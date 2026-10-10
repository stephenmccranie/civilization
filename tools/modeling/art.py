"""Compact art workflow; existing schema-1 assets retain pipeline.py unchanged."""
import argparse
import hashlib
import json
import shutil
import subprocess
import sys
from pathlib import Path
from PIL import Image
import pipeline as legacy

ROOT = legacy.ROOT
RUNTIME = legacy.RUNTIME
QUESTIONS = ('character', 'materials', 'readability', 'function')


def read(path): return json.loads(path.read_text(encoding='utf-8'))
def write(path, data): legacy.write(path, data)
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def local(folder, path): return legacy.confined(folder, path)


def definition(asset, folder):
    if asset.get('schema') != 2: raise ValueError('Use pipeline.py for legacy assets')
    if asset['kind'] not in legacy.KINDS: raise ValueError('Unknown asset kind')
    if any(not asset['brief'].get(k) for k in ('purpose', 'target', 'constraints')):
        raise ValueError('Fill purpose, target and constraints')
    if not asset['references']: raise ValueError('Inspect and declare visual references')
    for ref in asset['references']:
        if not ref.get('role') or not local(ROOT, ref['path']).is_file():
            raise ValueError('Reference needs an existing file and a role')
    for generation in asset.get('generations', []):
        if sha(local(folder, generation['file'])) != generation['sha256']:
            raise ValueError('Recorded concept changed')
        for path, digest in generation['references'].items():
            if sha(local(ROOT, path)) != digest: raise ValueError('Generation reference changed')
    source = asset['source']
    if source['mode'] not in ('project', 'recipe') or not local(folder, source['path']).is_file():
        raise ValueError('Declare one existing authoritative project or recipe')
    density = asset['texels_per_block']
    if density != 64 and not asset.get('density_reason'):
        raise ValueError('New world assets use 64px; explain legacy or UI density exceptions')


def output_asset(asset):
    return dict(asset, sources=list(dict.fromkeys([asset['source']['path'], *asset['sources']])))


def uv_density(asset, folder):
    """Check actual native UV spans; explicitly named decals may fit their island."""
    if asset['kind'] == 'ui' and asset['texels_per_block'] is None: return
    density = asset['texels_per_block'] / 16
    exceptions = set(asset.get('decals', [])); used = set()
    models = asset.get('models', [])
    if asset['source']['path'].endswith('.bbmodel'):
        models = list(dict.fromkeys([asset['source']['path'], *models]))
    for model in models:
        data = read(local(folder, model))
        if isinstance(data.get('textures'), dict):
            # Native Java block models may reference existing pack materials without exporting them.
            materials = asset.get('material_textures', {})
            for index, cube in enumerate(data.get('elements', [])):
                size = [abs(b-a) for a, b in zip(cube['from'], cube['to'])]
                for face, entry in cube.get('faces', {}).items():
                    resource = entry.get('texture'); visited = set()
                    while resource and resource.startswith('#'):
                        if resource in visited: raise ValueError('Cyclic Java texture alias')
                        visited.add(resource); resource = data['textures'].get(resource[1:])
                    if not resource or resource not in materials:
                        raise ValueError(f'Declare source pixels for Java material {resource}')
                    source = materials[resource]
                    if source not in asset['sources']: raise ValueError('Java material pixels must be declared sources')
                    with Image.open(local(folder, source)) as image: width, height = image.size
                    axes = (0, 1) if face in ('north', 'south') else (2, 1) if face in ('east', 'west') else (0, 2)
                    uv = entry.get('uv', [0, 0, size[axes[0]], size[axes[1]]])
                    actual = (abs(uv[2]-uv[0])*width/16, abs(uv[3]-uv[1])*height/16)
                    if entry.get('rotation', 0) % 180 == 90: actual = actual[::-1]
                    expected = tuple(size[i]*density for i in axes)
                    if any(abs(a-b)>1.01 for a,b in zip(actual,expected)):
                        raise ValueError(f'UV density mismatch: {model}:element {index}/{face}: {actual} versus {expected}')
            if not data.get('elements'): raise ValueError('Java density check needs authored elements')
            continue
        for cube in data['elements']:
            if cube.get('type', 'cube') != 'cube': raise ValueError('Unsupported native element')
            size = [abs(b-a) for a, b in zip(cube['from'], cube['to'])]
            for face, entry in cube.get('faces', {}).items():
                if entry.get('texture') is None: continue
                key = f"{model}:{cube['name']}/{face}"
                if key in exceptions: used.add(key); continue
                uv = entry['uv']; texture = data['textures'][int(entry['texture'])]
                resolution = data['resolution']
                sx = texture['width'] / texture.get('uv_width', resolution['width'])
                sy = texture['height'] / texture.get('uv_height', resolution['height'])
                axes = (0, 1) if face in ('north', 'south') else (2, 1) if face in ('east', 'west') else (0, 2)
                actual = (abs(uv[2]-uv[0])*sx, abs(uv[3]-uv[1])*sy)
                if entry.get('rotation', 0) % 180 == 90: actual = actual[::-1]
                expected = tuple(size[i]*density for i in axes)
                if any(abs(a-b) > 1.01 for a, b in zip(actual, expected)):
                    raise ValueError(f'UV density mismatch: {key}: {actual} versus {expected}')
    if exceptions-used: raise ValueError(f'Unknown decal exceptions: {sorted(exceptions-used)}')
    if asset['kind'] in ('block', 'multiblock', 'animated_machine', 'entity') and not models:
        raise ValueError('Declare native models for density checks')


def validate(asset, folder):
    definition(asset, folder)
    legacy.validate_outputs(output_asset(asset), folder)
    uv_density(asset, folder)


def snapshot(asset, folder):
    spec = {k: v for k, v in asset.items() if k != 'reviews'}
    paths = [local(ROOT, r['path']) for r in asset['references']]
    paths += [local(folder, p) for p in output_asset(asset)['sources'] + asset.get('models', []) + asset['gecko_manifests']]
    paths += [local(folder, o['source']) for o in asset['outputs']]
    paths += [local(folder, g['file']) for g in asset.get('generations', [])]
    return {'spec': hashlib.sha256(json.dumps(spec, sort_keys=True).encode()).hexdigest(),
            'files': {str(p): sha(p) for p in paths}}


def approved(asset, folder, stage):
    review = asset['reviews'].get(stage)
    if not review or review['decision'] != 'pass': raise ValueError(f'{stage}: needs visual review')
    if review['snapshot'] != snapshot(asset, folder): raise ValueError(f'{stage}: review is stale')
    for name, digest in review['evidence'].items():
        if sha(local(folder, name)) != digest: raise ValueError('Review evidence changed')


def receipt(asset, folder):
    return {'snapshot': snapshot(asset, folder),
            'outputs': {o['target']: sha(local(folder, o['source'])) for o in asset['outputs']}}


def runtime_matches(asset, folder):
    for name, digest in receipt(asset, folder)['outputs'].items():
        path = local(RUNTIME, name)
        if not path.is_file() or sha(path) != digest: raise ValueError('Preview/runtime differs from reviewed exports')


def preserve(folder, path):
    destination = folder / 'history' / sha(path) / path.name
    destination.parent.mkdir(parents=True, exist_ok=True)
    if not destination.exists(): shutil.copyfile(path, destination)
    return str(destination.relative_to(folder))


def copy_bundle(asset, folder):
    """Read/backup before mutation and roll back a failed bundle write."""
    bundle = [(local(RUNTIME, o['target']), local(folder, o['source']).read_bytes()) for o in asset['outputs']]
    previous = {p: p.read_bytes() if p.exists() else None for p, _ in bundle}
    backup = {str(p.relative_to(RUNTIME)).replace('\\', '/'): preserve(folder, p)
              for p, data in previous.items() if data is not None}
    old = folder/'preview-backup.json'
    if old.exists(): preserve(folder, old)
    write(old, backup)
    try:
        for path, data in bundle:
            path.parent.mkdir(parents=True, exist_ok=True); path.write_bytes(data)
    except OSError:
        for path, data in previous.items():
            if data is None: path.unlink(missing_ok=True)
            else: path.write_bytes(data)
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=('init', 'prepare', 'views', 'compare', 'record', 'review', 'preview', 'publish', 'check', 'materials'))
    parser.add_argument('asset', type=Path, nargs='?')
    parser.add_argument('--kind', choices=legacy.KINDS, default='block')
    parser.add_argument('--stage', choices=('build', 'verify'), default='build')
    parser.add_argument('--review', type=Path)
    parser.add_argument('--evidence', nargs='+')
    parser.add_argument('--image', type=Path); parser.add_argument('--prompt', type=Path)
    parser.add_argument('--references', type=Path, nargs='*', default=[])
    parser.add_argument('--model'); parser.add_argument('--target', type=float, nargs=3)
    parser.add_argument('--span', type=float); parser.add_argument('--complete', action='store_true')
    parser.add_argument('--images', nargs='+', help='Workspace image paths, optionally path|left,top,right,bottom')
    args = parser.parse_args()
    if args.command == 'materials':
        print(json.dumps(read(Path(__file__).with_name('materials.json')), indent=2)); return
    if args.asset is None: parser.error('Choose an asset directory')
    folder = args.asset.resolve(); local(ROOT, str(folder))
    file = folder / 'asset.json'
    if args.command == 'init':
        if file.exists(): raise ValueError('Asset exists; preserve its schema and history')
        folder.mkdir(parents=True, exist_ok=True)
        write(file, {'schema': 2, 'kind': args.kind,
            'brief': {'purpose': '', 'target': '', 'constraints': ''},
            'references': [], 'texels_per_block': 64,
            'source': {'mode': 'project', 'path': ''}, 'sources': [], 'models': [],
            'outputs': [], 'gecko_manifests': [], 'generations': [], 'reviews': {}})
        print(file); return
    asset = read(file)
    definition(asset, folder)
    if args.command == 'prepare':
        previous = folder/'prepare-receipt.json'
        if previous.exists():
            try:
                validate(asset, folder)
                if read(previous) == receipt(asset, folder):
                    print('Prepared inputs and outputs unchanged; reused validated exports.'); return
            except (ValueError, OSError): pass
        for output in asset['outputs']:
            previous = local(folder, output['source'])
            if previous.is_file(): preserve(folder, previous)
        if asset['source']['mode'] == 'recipe':
            recipe = local(folder, asset['source']['path'])
            if recipe.suffix != '.py': raise ValueError('Recipe must be a Python file')
            subprocess.run([sys.executable, str(recipe)], cwd=folder, check=True)
        elif asset['source']['path'].endswith('.bbmodel'):
            subprocess.run([sys.executable, str(Path(__file__).with_name('export_project.py')),
                            str(folder/'export'), '--name', asset.get('resource_name', folder.name),
                            '--project', str(local(folder, asset['source']['path'])), '--replace'], check=True)
        validate(asset, folder); write(folder / 'prepare-receipt.json', receipt(asset, folder))
    elif args.command == 'views':
        if not args.model or args.target is None or args.span is None: parser.error('views needs --model, --target and --span')
        subprocess.run([sys.executable, str(Path(__file__).with_name('review_model.py')), str(local(folder, args.model)),
                        '--output', str(folder/'review'), '--target', *map(str, args.target), '--span', str(args.span)], check=True)
    elif args.command == 'compare':
        if not args.images or len(args.images) < 2: parser.error('compare needs at least two --images')
        images = []
        for value in args.images:
            path, sep, crop = value.partition('|'); images.append(str(local(ROOT, path))+(sep+crop if sep else ''))
        sheet = folder/'review/comparison.png'
        if sheet.exists(): preserve(folder, sheet)
        subprocess.run([sys.executable, str(Path(__file__).with_name('compare.py')), str(sheet), *images], check=True)
    elif args.command == 'record':
        if not args.image or not args.prompt: parser.error('record needs --image and exact --prompt')
        with Image.open(args.image) as image: image.verify()
        dest = folder / f'concept-{len(asset["generations"])+1:02}{args.image.suffix.lower()}'
        if dest.exists(): raise ValueError('Concept exists')
        shutil.copyfile(args.image, dest)
        asset['generations'].append({'file': dest.name, 'sha256': sha(dest),
            'prompt': args.prompt.read_text(encoding='utf-8'),
            'references': {str(p.resolve()): sha(p) for p in args.references}})
        write(file, asset)
    elif args.command == 'review':
        validate(asset, folder)
        if read(folder/'prepare-receipt.json') != receipt(asset, folder): raise ValueError('Prepare exports after source changes')
        if not args.review or not args.evidence: parser.error('review needs --review and --evidence')
        findings = read(args.review)
        if findings['decision'] not in ('pass', 'revise'): raise ValueError('Use pass or revise')
        if any(not findings['criteria'].get(q) for q in QUESTIONS): raise ValueError(f'Answer {QUESTIONS} briefly')
        if findings['decision'] == 'pass' and findings['defects']: raise ValueError('Fix blocking defects')
        if args.stage == 'verify': approved(asset, folder, 'build'); runtime_matches(asset, folder)
        for path in args.evidence:
            with Image.open(local(folder, path)) as image: image.verify()
        evidence = {preserve(folder, local(folder, p)): sha(local(folder, p)) for p in args.evidence}
        if args.stage in asset['reviews']: preserve(folder, file)
        asset['reviews'][args.stage] = dict(findings, snapshot=snapshot(asset, folder), evidence=evidence)
        write(file, asset)
    else:
        validate(asset, folder); approved(asset, folder, 'build')
        if args.command == 'publish' or args.complete: approved(asset, folder, 'verify')
        result = receipt(asset, folder)
        if args.command in ('preview', 'publish'):
            # Validate the entire bundle before any mutation; preserve replaced runtime files.
            copy_bundle(asset, folder)
            write(folder/f'{args.command}-receipt.json', result)
        else:
            runtime_matches(asset, folder)
            name = 'publish' if args.complete else 'preview'
            if read(folder/f'{name}-receipt.json') != result: raise ValueError('Receipt is stale')
    print('Art workflow:', args.command, 'OK; visual acceptance requires inspected evidence.')


if __name__ == '__main__': main()
