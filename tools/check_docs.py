"""Check active Markdown local links and the status build version. No game/runtime dependencies."""
from pathlib import Path
import re
import subprocess
import sys
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]


def main():
    listed = subprocess.check_output(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard", "-z"],
        cwd=ROOT,
    ).decode("utf-8").split("\0")
    files = sorted({ROOT / name for name in listed if name.endswith(".md")
                    and not name.startswith("docs/archive/") and (ROOT / name).is_file()})
    errors = []
    links = 0
    for path in files:
        text = path.read_text(encoding="utf-8-sig")
        # Code examples are not links. Frozen snapshots intentionally retain original context.
        text = re.sub(r"(?ms)^```.*?^```[^\n]*", "", text)
        for match in re.finditer(r"!?\[[^\]\n]*\]\(([^)\n]+)\)", text):
            target = match.group(1).strip()
            if target.startswith("<"):
                target = target[1:target.index(">")]
            else:
                target = re.split(r'\s+["\']', target, maxsplit=1)[0]
            parsed = urlsplit(target)
            if parsed.scheme or parsed.netloc or not parsed.path:
                continue
            links += 1
            dest = path.parent / unquote(parsed.path)
            if not dest.exists():
                errors.append(f"{path.relative_to(ROOT)}: missing target {target}")
    properties = (ROOT / "civilization-mod/gradle.properties").read_text(encoding="utf-8")
    version = re.search(r"(?m)^mod_version\s*=\s*(\S+)", properties).group(1)
    status = (ROOT / "docs/status.md").read_text(encoding="utf-8")
    if f"Current build: **{version}**" not in status:
        errors.append(f"docs/status.md: current build must match gradle.properties ({version})")
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"Documentation OK: {len(files)} Markdown files, {links} local link targets, build {version}.")
    print("Archive links, URL availability, and heading anchors are not checked.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
