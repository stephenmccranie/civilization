"""Download pinned authoring tools into ignored local storage. Does not launch apps."""
import hashlib
import json
from pathlib import Path
import shutil
from urllib.request import urlopen
import zipfile

ROOT = Path(__file__).resolve().parents[2]
DEST = ROOT / '.tools/modeling'

def sha(data):
    return hashlib.sha256(data).hexdigest()

def main():
    DEST.mkdir(parents=True, exist_ok=True)
    pins = json.loads(Path(__file__).with_name('pins.json').read_text())
    receipt = {}
    for name, spec in pins.items():
        if not isinstance(spec, dict):
            continue
        target = DEST / spec['file']
        data = target.read_bytes() if target.exists() else urlopen(spec['url'], timeout=90).read()
        if sha(data) != spec['sha256']:
            raise SystemExit(f'{name}: checksum mismatch; inspect before changing pins')
        if not target.exists():
            target.write_bytes(data)
        receipt[name] = {'file': str(target), 'sha256': sha(data)}
        if target.suffix == '.zip' and not (DEST / name).exists():
            # Archives unpack into a folder named after their pin; the rename keeps an interrupted unpack from looking installed.
            partial = DEST / (name + '.partial')
            if partial.exists():
                shutil.rmtree(partial)
            with zipfile.ZipFile(target) as archive:
                archive.extractall(partial)
            partial.rename(DEST / name)
    # Upstream binds all interfaces despite logging localhost. Keep this developer tool local.
    source = (DEST / 'mcp.upstream.js').read_text(encoding='utf-8')
    old = 'a.listen(t,()=>{console.log(`[MCP] Server listening'
    new = 'a.listen(t,"127.0.0.1",()=>{console.log(`[MCP] Server listening'
    if source.count(old) != 1:
        raise SystemExit('MCP loopback patch no longer matches the pinned artifact')
    patched = source.replace(old, new)
    (DEST / 'mcp.js').write_text(patched, encoding='utf-8', newline='')
    receipt['mcp']['local_patch'] = 'Bind listener to 127.0.0.1 instead of all interfaces'
    receipt['mcp']['patched_sha256'] = sha(patched.encode())
    (DEST / 'setup-receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print('Pinned Blockbench, MCP, GeckoLib editor and Codex tools ready:', DEST)

if __name__ == '__main__':
    main()
