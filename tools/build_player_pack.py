"""Build a download-only Prism/Modrinth pack from reviewed, public inputs (Python 3.11+)."""
import argparse
import concurrent.futures
import hashlib
import ipaddress
import json
from pathlib import Path, PurePosixPath
import re
import tomllib
import urllib.parse
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'modpack'
OVERRIDES = {
    'config/DistantHorizons.toml', 'config/iris.properties', 'config/grassiergrass-client.toml', 'options.txt',
    'shaderpacks/photon_v1.3b.zip.txt',
}
USER_AGENT = 'Civilization-playtest-pack/1 (github.com/stephenmccranie/civilization)'


def safe_path(value):
    path = PurePosixPath(value)
    if path.is_absolute() or '..' in path.parts or '\\' in value or ':' in value:
        raise ValueError(f'Unsafe pack path: {value}')


def public_text(data):
    text = data.decode('utf-8')
    for match in re.finditer(r'(?<![\w.])(?:\d{1,3}\.){3}\d{1,3}(?![\w.])', text):
        try:
            ipaddress.IPv4Address(match.group())
        except ipaddress.AddressValueError:
            continue
        raise ValueError('IP address literal in public pack input')
    if re.search(r'(?i)(?:(?<![a-z])[a-z]:[\\/]|-javaagent:|servers\.dat|accessToken|clientToken)', text):
        raise ValueError('Private or machine-specific data in pack input')


def inputs():
    raw = (SOURCE / 'modrinth.index.json').read_bytes()
    public_text(raw)
    index = json.loads(raw)
    if index['formatVersion'] != 1 or index['game'] != 'minecraft':
        raise ValueError('Unsupported pack format')
    if set(index['dependencies']) != {'minecraft', 'neoforge'}:
        raise ValueError('Expected Minecraft and NeoForge dependencies')
    actual = {p.relative_to(SOURCE / 'overrides').as_posix()
              for p in (SOURCE / 'overrides').rglob('*') if p.is_file()}
    if actual != OVERRIDES:
        raise ValueError(f'Unreviewed or missing overrides: {actual ^ OVERRIDES}')
    paths = set()
    for item in index['files']:
        path = item['path']
        safe_path(path)
        if path in paths or path.split('/')[0] not in {'mods', 'shaderpacks', 'resourcepacks'}:
            raise ValueError(f'Unexpected or duplicate download: {path}')
        paths.add(path)
        if item['env'] != {'client': 'required', 'server': 'unsupported'}:
            raise ValueError('This is a client pack, not a dedicated server export')
        for kind, length in [('sha1', 40), ('sha512', 128)]:
            if not re.fullmatch('[0-9a-f]{' + str(length) + '}', item['hashes'][kind]):
                raise ValueError(f'Invalid {kind}: {path}')
        if item['fileSize'] <= 0 or not item['downloads']:
            raise ValueError(f'Missing download metadata: {path}')
        for url in item['downloads']:
            parsed = urllib.parse.urlsplit(url)
            if parsed.scheme != 'https' or parsed.hostname not in {'cdn.modrinth.com', 'github.com'} or parsed.username or parsed.password:
                raise ValueError(f'Unreviewed download host: {path}')
    output = {'modrinth.index.json': raw}
    for name in sorted(actual):
        content = (SOURCE / 'overrides' / name).read_bytes()
        public_text(content)
        if name.endswith('.toml'):
            tomllib.loads(content.decode('utf-8'))
        output['overrides/' + name] = content
    return index, output


def verify_file(path, item):
    data = path.read_bytes()
    if len(data) != item['fileSize'] or any(hashlib.new(k, data).hexdigest() != v for k, v in item['hashes'].items()):
        raise ValueError(f'Download hash/size mismatch: {item["path"]}')
    with zipfile.ZipFile(path) as archive:
        if archive.testzip() is not None:
            raise ValueError(f'Corrupt archive: {item["path"]}')


def check_downloads(index, cache, local_civilization):
    cache.mkdir(parents=True, exist_ok=True)
    def check(item):
        name = PurePosixPath(item['path']).name
        if name.startswith('civilization-') and local_civilization:
            path = local_civilization
        else:
            path = cache / (item['hashes']['sha1'] + '-' + name)
            if not path.exists():
                req = urllib.request.Request(item['downloads'][0], headers={'User-Agent': USER_AGENT})
                with urllib.request.urlopen(req, timeout=90) as response:
                    content = response.read()
                temporary = path.with_suffix(path.suffix + '.partial')
                temporary.write_bytes(content)
                verify_file(temporary, item)
                temporary.replace(path)
        verify_file(path, item)
        print('Verified', item['path'], flush=True)
        return item['path'], path
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        return dict(pool.map(check, index['files']))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=ROOT / '.tools/player-pack-build')
    parser.add_argument('--check-downloads', action='store_true')
    parser.add_argument('--cache', type=Path, default=ROOT / '.tools/player-pack-cache')
    parser.add_argument('--local-civilization', type=Path, help='Validate an unpublished release candidate JAR locally')
    args = parser.parse_args()
    index, contents = inputs()
    if args.check_downloads:
        check_downloads(index, args.cache, args.local_civilization)
    args.output.mkdir(parents=True, exist_ok=True)
    target = args.output / ('Civilization-' + index['versionId'] + '.mrpack')
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, data in sorted(contents.items()):
            info = zipfile.ZipInfo(name, date_time=(2026, 10, 31, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, data)
    with zipfile.ZipFile(target) as archive:
        if archive.testzip() is not None or set(archive.namelist()) != set(contents):
            raise ValueError('Pack archive verification failed')
    digest = hashlib.sha256(target.read_bytes()).hexdigest()
    target.with_suffix('.mrpack.sha256').write_text(f'{digest}  {target.name}\n', encoding='utf-8')
    print(f'Built {target.name}: {target.stat().st_size:,} bytes, {len(index["files"])} downloads; no bundled third-party archives')
    print('SHA-256', digest)


if __name__ == '__main__':
    main()
