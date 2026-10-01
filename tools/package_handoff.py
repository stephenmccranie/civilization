"""Export current development sources without changing Git or including local runtime data."""
from pathlib import Path
import datetime
import hashlib
import json
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
EXCLUDED = {'.git', '.codex', '.tools', '.gradle', '.dev-libs', '__pycache__',
            'build', 'run', 'runs', '.idea', 'saves', 'logs'}

def main():
    raw = subprocess.check_output(['git', 'ls-files', '-z', '--cached', '--others', '--exclude-standard'], cwd=ROOT)
    paths = sorted({p.decode('utf-8') for p in raw.split(b'\0') if p})
    selected = []
    for name in paths:
        rel = Path(name)
        path = ROOT / rel
        if not path.is_file() or any(part in EXCLUDED for part in rel.parts):
            continue
        if rel.parts[0] == 'analysis' or rel.name == 'dev.local.json' or rel.name.startswith('.env'):
            continue
        if path.is_symlink() or not path.resolve().is_relative_to(ROOT):
            raise ValueError(f'External/symlink source requires explicit handling: {name}')
        selected.append((rel.as_posix(), path))
    properties = dict(line.split('=', 1) for line in (ROOT/'civilization-mod/gradle.properties').read_text(encoding='utf-8').splitlines() if '=' in line)
    version = properties['mod_version']
    stamp = datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
    folder = ROOT/'.tools/handoffs'
    folder.mkdir(parents=True, exist_ok=True)
    out = folder/f'civilization-{version}-helicopter-source-{stamp}.zip'
    manifest = {'version': version, 'created': datetime.datetime.now().astimezone().isoformat(),
                'git_history_included': False, 'source': 'Current working files, including uncommitted work',
                'excluded': sorted(EXCLUDED | {'analysis', 'dev.local.json', '.env*'}), 'files': {}}
    with zipfile.ZipFile(out, 'x', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name, path in selected:
            content = path.read_bytes()
            manifest['files'][name] = {'sha256': hashlib.sha256(content).hexdigest(), 'bytes': len(content)}
            z.writestr(name, content)
        z.writestr('HANDOFF-MANIFEST.json', json.dumps(manifest, indent=2)+'\n')
        z.writestr('START-HERE.md', '# Civilization helicopter development handoff\n\nOpen [the setup and branch instructions](docs/collaboration.md) before editing.\n\nThis is source code and original art, not a Prism instance. No GitHub account is required for the local branch workflow.\n')
    # Verify the actual compressed contents against the manifest, including binary art.
    with zipfile.ZipFile(out) as z:
        for name, info in manifest['files'].items():
            if hashlib.sha256(z.read(name)).hexdigest() != info['sha256']:
                raise RuntimeError(f'Archive verification failed: {name}')
        required = ['civilization-mod/gradle/wrapper/gradle-wrapper.jar',
                    'civilization-mod/src/main/java/dev/civilization/BoatSystem.java',
                    'concept_art/helicopters/kestrel.png', 'concept_art/helicopters/meridian.png',
                    '.agents/skills/civilization-art/SKILL.md']
        for name in required:
            if name not in z.namelist():
                raise RuntimeError(f'Missing handoff requirement: {name}')
    digest = hashlib.sha256()
    with out.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024*1024), b''):
            digest.update(chunk)
    out.with_suffix('.zip.sha256').write_text(f'{digest.hexdigest()}  {out.name}\n', encoding='utf-8')
    print(json.dumps({'archive': str(out), 'files': len(selected), 'MiB': round(out.stat().st_size/1048576, 1), 'sha256': digest.hexdigest(), 'verified': True}, indent=2))

if __name__ == '__main__':
    main()
