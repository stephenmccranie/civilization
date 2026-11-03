"""Check that Markdown links reach versioned files and headings, and that text is not mis-encoded."""
from pathlib import Path
import re
import subprocess
import sys
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
MOJIBAKE = re.compile(r"[ÃÂ]|â€")


def anchors(text):
    """GitHub heading slugs plus explicit HTML ids."""
    found, seen = set(), {}
    for heading in re.findall(r"(?m)^#{1,6}\s+(.+?)\s*#*\s*$", text):
        slug = re.sub(r"[^\w\- ]", "", heading.strip().lower()).replace(" ", "-")
        count = seen.get(slug, 0)
        seen[slug] = count + 1
        found.add(slug if count == 0 else f"{slug}-{count}")
    found.update(re.findall(r'<a\s+id="([^"]+)"', text))
    return found


def main():
    # Tracked and new unignored files: what GitHub will show. Local-only folders such as art/ are excluded.
    listed = subprocess.check_output(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard", "-z"], cwd=ROOT,
    ).decode("utf-8").split("\0")
    versioned = {ROOT / name for name in listed if name and (ROOT / name).is_file()}
    folders = {parent for path in versioned for parent in path.parents}
    docs = sorted(path for path in versioned if path.suffix == ".md")
    texts = {path: path.read_text(encoding="utf-8") for path in docs}
    errors = []
    for path, text in texts.items():
        for number, line in enumerate(text.splitlines(), 1):
            if MOJIBAKE.search(line):
                errors.append(f"{path.relative_to(ROOT)}:{number}: mis-encoded text")
        prose = re.sub(r"(?ms)^```.*?^```[^\n]*", "", text)
        for match in re.finditer(r"!?\[[^\]\n]*\]\(([^)\n]+)\)", prose):
            target = re.split(r'\s+["\']', match.group(1).strip().strip("<>"), maxsplit=1)[0]
            parsed = urlsplit(target)
            if parsed.scheme or parsed.netloc:
                continue
            dest = (path.parent / unquote(parsed.path)).resolve() if parsed.path else path
            if dest not in versioned and dest not in folders:
                errors.append(f"{path.relative_to(ROOT)}: {target} is not a versioned file")
            elif parsed.fragment and dest in texts and parsed.fragment not in anchors(texts[dest]):
                errors.append(f"{path.relative_to(ROOT)}: {target} has no such heading")
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"Documentation OK: {len(docs)} Markdown files.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
