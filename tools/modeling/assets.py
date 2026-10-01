"""Validate native GeckoLib exports and publish them without interpreting .bbmodel.

python tools/modeling/assets.py art/models/<asset>/manifest.json [--check]
"""
import argparse
import hashlib
import json
import math
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
RUNTIME = ROOT / 'civilization-mod/src/main/resources/assets/civilization'

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def vector(value, length, label):
    if not isinstance(value, list) or len(value) != length or any(not isinstance(x, (int, float)) or not math.isfinite(x) for x in value):
        raise ValueError(f'{label}: expected {length} finite numbers')

def confined(parent, relative):
    path = (parent / relative).resolve()
    if not path.is_relative_to(parent.resolve()):
        raise ValueError(f'Path escapes its asset directory: {relative}')
    return path

def validate(manifest_path):
    manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
    directory = manifest_path.resolve().parent
    files = {key: confined(directory, manifest[key]) for key in ('project', 'geometry', 'animation', 'texture')}
    for file in files.values():
        if not file.is_file():
            raise ValueError(f'Missing asset: {file}')
    # Read project only as an opaque editable source. Native Blockbench codecs own its format.
    geometry = json.loads(files['geometry'].read_text(encoding='utf-8'))['minecraft:geometry']
    if len(geometry) != 1:
        raise ValueError('One geometry per machine asset is required')
    model = geometry[0]
    desc = model['description']
    with Image.open(files['texture']) as texture:
        width, height = texture.size
        if (width, height) != (desc['texture_width'], desc['texture_height']):
            raise ValueError('Texture dimensions do not match exported geometry')
    bones = model['bones']
    names = [bone['name'] for bone in bones]
    if len(names) != len(set(names)):
        raise ValueError('Duplicate bone names')
    parents = {bone['name']: bone.get('parent') for bone in bones}
    count = 0
    for bone in bones:
        ancestors = {bone['name']}
        parent = bone.get('parent')
        while parent:
            if parent not in parents or parent in ancestors:
                raise ValueError(f'Invalid bone hierarchy at {bone["name"]}')
            ancestors.add(parent)
            parent = parents[parent]
        vector(bone.get('pivot', [0, 0, 0]), 3, 'pivot')
        for cube in bone.get('cubes', []):
            count += 1
            vector(cube['origin'], 3, 'cube origin')
            vector(cube['size'], 3, 'cube size')
            if min(cube['size']) <= 0:
                raise ValueError('Zero/negative cube dimensions')
            uv = cube['uv']
            if not isinstance(uv, dict):
                raise ValueError('Use per-face UVs for our explicit texel-density pipeline')
            for face in uv.values():
                vector(face['uv'], 2, 'face UV')
                vector(face['uv_size'], 2, 'face UV size')
                u, v = face['uv']; du, dv = face['uv_size']
                if min(u, u + du) < 0 or min(v, v + dv) < 0 or max(u, u + du) > width or max(v, v + dv) > height:
                    raise ValueError(f'UV outside {width}x{height} atlas on {bone["name"]}')
    animations = json.loads(files['animation'].read_text(encoding='utf-8'))['animations']
    for name, animation in animations.items():
        if not set(animation.get('bones', {})).issubset(names):
            raise ValueError(f'Unknown bone in {name}')
        if animation.get('animation_length', 0) <= 0:
            raise ValueError(f'Animation must have a positive duration: {name}')
    for required in manifest.get('required_animations', []):
        if required not in animations:
            raise ValueError(f'Missing animation: {required}')
    return manifest, files, {'bones': len(bones), 'cubes': count, 'atlas': [width, height], 'animations': list(animations)}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('manifest', type=Path)
    parser.add_argument('--check', action='store_true', help='Verify runtime files and receipt without writing')
    args = parser.parse_args()
    manifest, files, summary = validate(args.manifest)
    asset_id = manifest['id']
    if not asset_id or any(c not in 'abcdefghijklmnopqrstuvwxyz0123456789_/' for c in asset_id):
        raise ValueError('Invalid resource id')
    outputs = {'geometry': RUNTIME / f'geo/{asset_id}.geo.json',
               'animation': RUNTIME / f'animations/{asset_id}.animation.json',
               'texture': RUNTIME / f'textures/entity/{asset_id}.png'}
    receipt = {'id': asset_id, 'sources': {key: digest(path) for key, path in files.items()},
               'outputs': {key: str(path.relative_to(ROOT)).replace('\\', '/') for key, path in outputs.items()},
               'validation': summary}
    receipt_path = args.manifest.resolve().with_name('export-receipt.json')
    if args.check:
        if not receipt_path.exists() or json.loads(receipt_path.read_text()) != receipt:
            raise ValueError('Export receipt is stale; export the current editor project and publish again')
        for key, target in outputs.items():
            if not target.exists() or digest(target) != digest(files[key]):
                raise ValueError(f'Runtime asset differs from native export: {target}')
    else:
        for key, target in outputs.items():
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(files[key].read_bytes())
        receipt_path.write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(summary))

if __name__ == '__main__':
    main()
