"""Install the live server's code, services and settings from this checkout, or list where the server differs.

usage: python server/deploy.py check|install

The host comes from server/deploy.local.json, which is never committed:
    {"host": "user@address", "key": "~/.ssh/key-file", "map_port": 12345}
"""
import hashlib
import io
import json
import os
import subprocess
import sys
import tarfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
MAP_FILES = ('surface_server.py', 'terrain.py', 'map_colors.py', 'map_overviews.py', 'deposits.py', 'villages.py',
             'server_metrics.py', 'surface.html', 'map.js', 'colors.json', 'world.json')
# This checkout's file -> its live path.
FILES = {
    'minecraft/minecraft.service': '/etc/systemd/system/minecraft.service',
    'minecraft/minecraft-start': '/usr/local/libexec/minecraft-start',
    'minecraft/minecraft-worldgen-cap.service': '/etc/systemd/system/minecraft-worldgen-cap.service',
    'minecraft/minecraft-worldgen-cap': '/usr/local/sbin/minecraft-worldgen-cap',
    'minecraft/90-minecraft-swappiness.conf': '/etc/sysctl.d/90-minecraft-swappiness.conf',
    'minecraft/user_jvm_args.txt': '/srv/minecraft/user_jvm_args.txt',
    **{f'minecraft/{p.relative_to(HERE / "minecraft").as_posix()}': f'/srv/minecraft/world/{p.relative_to(HERE / "minecraft").as_posix()}'
       for p in sorted((HERE / 'minecraft' / 'datapacks').rglob('*')) if p.is_file()},
    'pregeneration/minecraft-pregenerate.service': '/etc/systemd/system/minecraft-pregenerate.service',
    'pregeneration/pregenerate.py': '/usr/local/libexec/minecraft-pregenerate',
    'pregeneration/pregen_log.py': '/usr/local/libexec/pregen_log.py',
    'pregeneration/radial.py': '/usr/local/libexec/radial.py',
    'world-map/minecraft-map.service': '/etc/systemd/system/minecraft-map.service',
    'world-map/nginx.conf': '/etc/nginx/conf.d/minecraft-map.conf',
    **{f'world-map/{name}': f'/opt/minecraft-map/{name}' for name in MAP_FILES},
}
EXECUTABLES = {'/usr/local/libexec/minecraft-start', '/usr/local/libexec/minecraft-pregenerate', '/usr/local/sbin/minecraft-worldgen-cap'}
# The keys this repository decides in files the game and its mods also write, each set on its one line.
SETTINGS = {
    '/srv/minecraft/server.properties': {
        'gamemode': 'survival', 'force-gamemode': 'false', 'level-type': 'server\\:terra_scale_4',
        'initial-enabled-packs': 'vanilla,file/terra_scale_4,file/civilization_terralith',
        # Terrain inference can hold the server thread past the watchdog's limit while a river region builds
        'max-tick-time': '-1', 'sync-chunk-writes': 'false'},
    '/srv/minecraft/config/DistantHorizons.toml': {
        'chunkGeneratorMode': '"INTERNAL_SERVER"', 'generatorPlan': '"CHUNKS_ONLY"', 'generationRequestRateLimit': '16',
        'maxGenerationRequestDistance': '256', 'maxSyncOnLoadRequestDistance': '256',
        'realTimeUpdateDistanceRadiusInChunks': '128', 'playerBandwidthLimit': '100',
        'numberOfThreads': '4', 'threadRunTimeRatio': '"0.75"', 'threadPriority': '3',
        'levelKeyPrefix': '"climate-scale4-20261102t022132z"'},
}
# Run as root on the server: reports each managed file's digest, mode and owner and each setting's value, and with
# INSTALL set, first writes the settings.
REMOTE = r'''
import hashlib, json, os, pwd, re, sys
files, settings, install = json.loads(REQUEST)
def line(path, key):
    found = [i for i, l in enumerate(lines[path]) if re.match(r'\s*' + re.escape(key) + r'\s*=', l)]
    if len(found) != 1: sys.exit(f'{path}: {key} is on {len(found)} lines, not one')
    return found[0]
lines = {path: open(path).read().split('\n') for path in settings}
for path, keys in settings.items():
    if install:
        for key, value in keys.items():
            i = line(path, key)
            lines[path][i] = re.sub(r'(=\s*).*$', lambda m: m.group(1) + value, lines[path][i], count=1)
        with open(path + '.new', 'w') as f: f.write('\n'.join(lines[path]))
        st = os.stat(path); os.chown(path + '.new', st.st_uid, st.st_gid); os.replace(path + '.new', path)
state = {}
for path in files:
    try:
        st = os.stat(path)
        state[path] = [hashlib.sha256(open(path, 'rb').read()).hexdigest(), st.st_mode & 0o777, pwd.getpwuid(st.st_uid).pw_name]
    except FileNotFoundError:
        state[path] = None
for path, keys in settings.items():
    for key in keys:
        state[path + ' ' + key] = lines[path][line(path, key)].split('=', 1)[1].strip()
print(json.dumps(state))
'''


def local():
    config = json.loads((HERE / 'deploy.local.json').read_text())
    return config, ['ssh', '-o', 'BatchMode=yes', '-o', 'IdentitiesOnly=yes', '-i', os.path.expanduser(config['key']), config['host']]


def content(source, config):
    return (HERE / source).read_bytes().replace(b'MAP_PORT', str(config['map_port']).encode())


def owner(path):
    return 'minecraft' if path.startswith('/srv/minecraft/') else 'root'


def remote(ssh, install):
    request = f'REQUEST = {json.dumps([list(FILES.values()), SETTINGS, install])!r}\n'
    result = subprocess.run(ssh + ['sudo python3 -I -'], input=request + REMOTE, capture_output=True, text=True)
    if result.returncode: sys.exit(result.stderr.strip() or 'remote check failed')
    return json.loads(result.stdout)


def differences(config, state):
    found = []
    for source, path in FILES.items():
        want = [hashlib.sha256(content(source, config)).hexdigest(), 0o755 if path in EXECUTABLES else 0o644, owner(path)]
        if state[path] != want: found.append(path if state[path] else path + ' (missing)')
    for path, keys in SETTINGS.items():
        found += [f'{path} {key} = {state[path + " " + key]}, not {value}' for key, value in keys.items()
                  if state[path + ' ' + key] != value]
    return found


def install(config, ssh, changed):
    archive = io.BytesIO()
    with tarfile.open(fileobj=archive, mode='w:gz') as tar:
        for source, path in FILES.items():
            if path not in changed: continue
            data = content(source, config)
            info = tarfile.TarInfo(path.lstrip('/'))
            info.size, info.mode = len(data), 0o755 if path in EXECUTABLES else 0o644
            info.uname = info.gname = owner(path)
            tar.addfile(info, io.BytesIO(data))
    subprocess.run(ssh + ['sudo', 'tar', '-C', '/', '-xzpf', '-', '--same-owner'], input=archive.getvalue(), check=True)
    if any(p.startswith('/etc/systemd/') for p in changed): subprocess.run(ssh + ['sudo', 'systemctl', 'daemon-reload'], check=True)
    if any(p.startswith('/etc/nginx/') for p in changed): subprocess.run(ssh + ['sudo nginx -t && sudo systemctl reload nginx'], check=True)
    if any(p.startswith('/etc/sysctl.d/') for p in changed): subprocess.run(ssh + ['sudo', 'sysctl', '--system'], check=True, capture_output=True)


def main():
    if len(sys.argv) != 2 or sys.argv[1] not in ('check', 'install'): sys.exit(__doc__)
    config, ssh = local()
    found = differences(config, remote(ssh, False))
    if sys.argv[1] == 'install' and found:
        install(config, ssh, {f.split(' ')[0] for f in found})
        found = differences(config, remote(ssh, True))
        print('Installed. Restart a changed service yourself when it suits the players; this never restarts one.')
    print('\n'.join(found) or 'The server matches this checkout.')
    sys.exit(1 if found else 0)


if __name__ == '__main__':
    main()
