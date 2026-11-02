"""Prepare reviewed client release assets locally; never tag, push or publish."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tomllib
import zipfile

import build_player_pack as pack


def prepare(jar, output, check_downloads=False):
    index, _ = pack.inputs()
    tag = index['versionId']
    if not re.fullmatch(r'playtest-\d+\.\d+\.\d+-\d+', tag):
        raise ValueError('Expected a versioned playtest tag')
    with zipfile.ZipFile(jar) as archive:
        metadata = tomllib.loads(archive.read('META-INF/neoforge.mods.toml').decode())
    mods = metadata['mods']
    if len(mods) != 1 or mods[0]['modId'] != 'civilization':
        raise ValueError('Expected the Civilization mod')
    version = mods[0]['version']
    properties = (pack.ROOT / 'civilization-mod/gradle.properties').read_text(encoding='utf-8')
    if not re.search(r'^mod_version=' + re.escape(version) + r'\s*$', properties, re.MULTILINE):
        raise ValueError('JAR does not match the authoritative project version')
    if not re.fullmatch(r'\d+\.\d+\.\d+-dev', version) or not tag.startswith('playtest-' + version.removesuffix('-dev') + '-'):
        raise ValueError('Tag/mod version mismatch')
    entries = [item for item in index['files'] if Path(item['path']).name.startswith('civilization-')]
    if len(entries) != 1:
        raise ValueError('Expected one Civilization download')
    pack.verify_file(jar, entries[0])
    url = f'https://github.com/stephenmccranie/civilization/releases/download/{tag}/civilization-{version}.jar'
    if entries[0]['downloads'] != [url]:
        raise ValueError('Manifest must point at this release JAR')
    output.mkdir(parents=True, exist_ok=True)
    command = [sys.executable, str(pack.ROOT / 'tools/build_player_pack.py'), '--output', str(output)]
    if check_downloads:
        command += ['--check-downloads', '--local-civilization', str(jar)]
    subprocess.run(command, check=True)
    destination = output / f'civilization-{version}.jar'
    if jar.resolve() != destination.resolve():
        shutil.copyfile(jar, destination)
    manifest = dict(schemaVersion=1, channel='playtest', releaseTag=tag, version=version,
                    protocol='3/0.35.0-dev', minecraftVersion=index['dependencies']['minecraft'],
                    minimumNeoForgeVersion=index['dependencies']['neoforge'], downloadUrl=url,
                    sha256=hashlib.sha256(jar.read_bytes()).hexdigest(), size=jar.stat().st_size)
    (output / 'civilization-update.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    installer = output / 'Civilization-client-updater-windows.zip'
    with zipfile.ZipFile(installer, 'w', zipfile.ZIP_DEFLATED) as archive:
        for path in sorted((pack.SOURCE / 'overrides/civilization-updater').iterdir()):
            info = zipfile.ZipInfo('civilization-updater/' + path.name, date_time=(2026, 10, 31, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, path.read_bytes())
    for path in (destination, installer, output / 'civilization-update.json'):
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        path.with_name(path.name + '.sha256').write_text(f'{digest}  {path.name}\n', encoding='utf-8')
    print(f'Prepared {tag} in {output}; nothing published')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--output', type=Path, default=pack.ROOT / '.tools/client-release')
    parser.add_argument('--check-downloads', action='store_true')
    args = parser.parse_args()
    prepare(args.jar, args.output, args.check_downloads)
