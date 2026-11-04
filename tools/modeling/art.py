"""Compact art workflow: concepts, texture masters and atlases, native exports, reviews and publication.

Existing schema-1 assets retain pipeline.py unchanged.
"""
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import uuid
from pathlib import Path
from PIL import Image, ImageChops, ImageColor, ImageDraw, ImageFilter, ImageStat
import pipeline as legacy
import setup

# Art, references and authoring tools live in the main checkout; a worktree publishes into its own runtime resources.
ROOT = setup.ROOT
RUNTIME = legacy.RUNTIME
QUESTIONS = ('character', 'materials', 'readability', 'function')
KEY = 40  # How far a master pixel must lie from the ground colour to belong to a subject; generated grounds are only nearly flat.


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
            kept = kept_reference(path, digest)
            if not kept.is_file() or sha(kept) != digest: raise ValueError(f'Generation reference {path} is missing from {kept.parent}')
    source = asset['source']
    if source['mode'] not in ('project', 'recipe') or not local(folder, source['path']).is_file():
        raise ValueError('Declare one existing authoritative project or recipe')
    density = asset['texels_per_block']
    if density != 64 and not asset.get('density_reason'):
        raise ValueError('New world assets use 64px; explain legacy or UI density exceptions')
    if 'texture' in asset: layout(asset)


def layout(asset):
    """A texture layout reduces grid cells of one recorded master to disjoint rectangles of one atlas."""
    texture = asset['texture']; cols, rows = texture['grid']; width, height = texture['size']
    if texture.get('master') not in (None, *(g['file'] for g in asset['generations'])):
        raise ValueError('The texture master must be a recorded generation')
    ImageColor.getrgb(texture['ground'])
    if not 2 <= texture['colors'] <= 256: raise ValueError('A texture palette has 2 to 256 colours')
    placed = []
    for name, region in texture['regions'].items():
        (col, row), (x, y, w, h) = region['cell'], region['rect']
        if not (0 <= col < cols and 0 <= row < rows): raise ValueError(f'Texture region {name} takes a cell outside the master grid')
        if min(w, h) <= 0 or min(x, y) < 0 or x+w > width or y+h > height: raise ValueError(f'Texture region {name} lies outside the atlas')
        for other, (ox, oy, ow, oh) in placed:
            if x < ox+ow and ox < x+w and y < oy+oh and oy < y+h: raise ValueError(f'Texture regions {other} and {name} overlap')
        placed.append((name, region['rect']))


def master_prompt(texture):
    """The master sheet contract the atlas reduction relies on, stated from the layout ahead of the asset's cell descriptions."""
    cols, rows = texture['grid']; sizes = {}
    for name, region in texture['regions'].items():
        fit = 'an icon reduced to fit within' if region.get('sprite') else 'a panel reduced to'
        sizes.setdefault(tuple(region['cell']), []).append(f'{fit} {region["rect"][2]} by {region["rect"][3]} pixels for {name}')
    cells = '\n'.join(f'Row {row+1}, column {col+1}: {", ".join(found)}.'
                      for (col, row), found in sorted(sizes.items(), key=lambda item: item[0][::-1]))
    return (f'Draw a texture master sheet: a strict {cols} by {rows} grid of equal cells on a perfectly flat, solid {texture["ground"]} '
            'background, with no grid lines. Each cell listed below holds one subject, centred with a margin of plain background at least '
            '6% of the cell on every side; leave other cells empty. A panel is a flat rectangular material surface seen straight on, with no '
            'perspective, 3D form, cast shadow or lighting gradient. An icon is one whole object drawn as a pixel-art inventory sprite, with '
            'no cast shadow, floor or outline glow; its openings show the plain background. No text. Crisp and matte, with broad '
            'two-to-three-tone colour clusters and no dithering or noise. Each subject is reduced by area averaging to the pixel sizes '
            f'listed: shape it to the largest one and draw seams, edges and fasteners bold enough to survive.\n{cells}\n\n')


def subject(crop, ground):
    """Mask of the pixels that differ from the flat ground."""
    difference = ImageChops.difference(crop, Image.new('RGB', crop.size, ground)).split()
    return ImageChops.lighter(ImageChops.lighter(*difference[:2]), difference[2]).point(lambda v: 255 if v > KEY else 0)


def atlas(asset, folder):
    """Reduce each region's master subject to its atlas rectangle with one shared palette, and draw the inspection sheet.

    A panel fills its rectangle. A sprite keeps its silhouette and proportions, centred in its rectangle on transparency."""
    texture = asset['texture']; regions = texture['regions']
    if not texture.get('master'): raise ValueError('Record a texture master first with texture --prompt')
    with Image.open(local(folder, texture['master'])) as opened: master = opened.convert('RGB')
    cols, rows = texture['grid']; found = {}
    for col, row in {tuple(r['cell']) for r in regions.values()}:
        cell = (col*master.width//cols, row*master.height//rows, (col+1)*master.width//cols, (row+1)*master.height//rows)
        mask = subject(master.crop(cell), texture['ground']); box = mask.getbbox()
        # A subject touching its cell edge has no margin to trust.
        if box is None or 0 in box[:2] or box[2] == mask.width or box[3] == mask.height:
            raise ValueError(f'Master cell {col},{row} has no subject inside a margin of plain {texture["ground"]}')
        if max((box[2]-box[0])/mask.width, (box[3]-box[1])/mask.height) < 0.5:
            raise ValueError(f'Master cell {col},{row} holds a subject under half its size; the sheet asks for it to fill the cell')
        found[col, row] = cell, mask, box
    tiles = {}
    for name, region in regions.items():
        (left, top, _, _), mask, box = found[tuple(region['cell'])]; width, height = region['rect'][2:]
        if region.get('sprite'):
            # Drop the rim blended with the ground, a quarter of an output pixel deep, then reduce premultiplied
            # so the ground cannot tint the edge.
            crop = master.crop((left+box[0], top+box[1], left+box[2], top+box[3])).convert('RGBA')
            scale = min(width/crop.width, height/crop.height); rim = max(2, round(0.25/scale))
            crop.putalpha(mask.filter(ImageFilter.MinFilter(2*rim+1)).crop(box))
            small = crop.convert('RGBa').resize((max(1, round(crop.width*scale)), max(1, round(crop.height*scale))), Image.Resampling.BOX).convert('RGBA')
            small.putalpha(small.getchannel('A').point(lambda a: 255 if a >= 128 else 0))
            tile = Image.new('RGBA', (width, height)); tile.paste(small, ((width-small.width)//2, (height-small.height)//2))
        else:
            inset = max(2, (box[2]-box[0])//60)  # Inside the antialiased rim.
            panel = (left+box[0]+inset, top+box[1]+inset, left+box[2]-inset, top+box[3]-inset)
            tile = master.crop(panel).resize((width, height), Image.Resampling.BOX).convert('RGBA')
        tiles[name] = tile
    # The palette comes from the visible subjects alone and applies without dithering; k-means refinement moves each
    # colour to its cluster's mean, so small accents such as brass are not averaged into the dominant material.
    # Unused space in an atlas of panels takes their mean colour; an atlas holding a sprite stays transparent around it.
    visible = [data[i:i+3] for data in (t.tobytes() for t in tiles.values()) for i in range(0, len(data), 4) if data[i+3]]
    swatch = Image.frombytes('RGB', (len(visible), 1), b''.join(visible))
    palette = swatch.quantize(colors=texture['colors'], method=Image.Quantize.MEDIANCUT, kmeans=16, dither=Image.Dither.NONE)
    transparent = any(r.get('sprite') for r in regions.values())
    image = Image.new('RGBA', tuple(texture['size']), (0, 0, 0, 0) if transparent else (*map(round, ImageStat.Stat(swatch).mean), 255))
    for name, tile in tiles.items(): image.alpha_composite(tile, tuple(regions[name]['rect'][:2]))
    coverage = image.getchannel('A')
    image = image.convert('RGB').quantize(palette=palette, dither=Image.Dither.NONE).convert('RGB')
    if transparent: image = Image.composite(image.convert('RGBA'), Image.new('RGBA', image.size), coverage)
    path = local(folder, texture['atlas'])
    if path.is_file(): preserve(folder, path)
    path.parent.mkdir(parents=True, exist_ok=True); image.save(path)
    subjects = [(c[0]+b[0], c[1]+b[1], c[0]+b[2], c[1]+b[3]) for c, _, b in found.values()]
    (folder/'review').mkdir(exist_ok=True); sheet(master, subjects, image, regions).save(folder/'review/texture.png')
    return path


def sheet(master, subjects, image, regions):
    """The subjects found on the master beside the atlas they reduce to, with its regions outlined and named,
    and the atlas at native and double size on dark and light grounds."""
    zoom = max(1, 512//image.height)
    right = Image.new('RGBA', (image.width*zoom, image.height*zoom), (32, 32, 32, 255))
    right.alpha_composite(image.convert('RGBA').resize(right.size, Image.Resampling.NEAREST)); draw = ImageDraw.Draw(right)
    for name, region in regions.items():
        x, y, w, h = (v*zoom for v in region['rect'])
        draw.rectangle((x, y, x+w-1, y+h-1), outline=(255, 0, 0)); draw.text((x+3, y+2), name, fill=(255, 255, 255), stroke_width=1, stroke_fill=(0, 0, 0))
    scale = right.height/master.height
    left = master.resize((round(master.width*scale), right.height)); draw = ImageDraw.Draw(left)
    for box in subjects: draw.rectangle(tuple(round(v*scale) for v in box), outline=(255, 0, 0), width=2)
    native = Image.new('RGBA', (image.width*3+32, max(right.height, image.height*4+32)), (32, 32, 32, 255))
    for row, ground in enumerate(((36, 37, 42, 255), (198, 198, 198, 255))):
        y = row*(image.height*2+16)
        native.paste(ground, (0, y, native.width, y+image.height*2+16))
        native.alpha_composite(image.convert('RGBA'), (8, y+8))
        native.alpha_composite(image.convert('RGBA').resize((image.width*2, image.height*2), Image.Resampling.NEAREST), (image.width+24, y+8))
    result = Image.new('RGB', (left.width+16+right.width+16+native.width, max(right.height, native.height)), (32, 32, 32))
    result.paste(left, (0, 0)); result.paste(right.convert('RGB'), (left.width+16, 0)); result.paste(native.convert('RGB'), (left.width+right.width+32, 0))
    return result


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


def generate(prompt, references, dest):
    """Draw an image with Codex's image tool on the owner's ChatGPT sign-in; setup.py installs the pinned CLI."""
    codex = setup.DEST / 'codex'
    # A plain mkdir inherits the workspace ACL. tempfile's private directory grants only each file's owner,
    # and the file's owner is Codex's sandbox user, so its image would be unreadable here.
    scratch = setup.DEST / f'image-{uuid.uuid4().hex}'
    scratch.mkdir()
    try:
        out = scratch / 'image.png'
        command = [str(codex / read(codex / 'codex-package.json')['entrypoint']), 'exec', '--skip-git-repo-check',
                   '--sandbox', 'workspace-write', '--cd', str(scratch), *(a for p in references for a in ('--image', str(p)))]
        request = ('Call your image generation tool once with exactly the prompt below, passing any attached images as references, '
                   f'and save the result as {out.name} in the current directory. Do nothing else.\n\n{prompt}')
        # The request goes on stdin because --image takes every following argument as another file.
        subprocess.run(command, input=request, text=True, encoding='utf-8', check=True)
        if not out.is_file(): raise ValueError('Codex saved no image')
        with Image.open(out) as opened: opened.verify()
        shutil.copyfile(out, dest)
    finally:
        shutil.rmtree(scratch)


def kept_reference(path, digest):
    """Generations keep the exact bytes of their references by content, so redrawing a referenced asset later
    leaves every earlier generation's provenance intact."""
    return ROOT / 'art/references' / (digest + Path(path).suffix.lower())


def record(asset, folder, stem, prompt, references, image=None):
    """Keep a generated, or supplied, image in the asset with its exact prompt and references; returns its file name."""
    references = {str(p): sha(p) for p in (local(ROOT, p) for p in references)}
    dest = folder / f'{stem}-{len(asset["generations"])+1:02}{image.suffix.lower() if image else ".png"}'
    if dest.exists(): raise ValueError(f'{dest.name} exists')
    if image:
        with Image.open(image) as opened: opened.verify()
        shutil.copyfile(image, dest)
    else: generate(prompt, [Path(p) for p in references], dest)
    for path, digest in references.items():
        kept = kept_reference(path, digest)
        if not kept.exists(): kept.parent.mkdir(parents=True, exist_ok=True); shutil.copyfile(path, kept)
    asset['generations'].append({'file': dest.name, 'sha256': sha(dest), 'prompt': prompt, 'references': references})
    return dest.name


def workspace(value):
    """Command-line paths name files in the main checkout, wherever the command runs."""
    return ROOT / value


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=('init', 'prepare', 'views', 'compare', 'record', 'texture', 'review', 'preview', 'publish', 'check', 'materials'))
    parser.add_argument('asset', type=workspace, nargs='?')
    parser.add_argument('--kind', choices=legacy.KINDS, default='block')
    parser.add_argument('--stage', choices=('build', 'verify'), default='build')
    parser.add_argument('--review', type=workspace)
    parser.add_argument('--evidence', nargs='+')
    parser.add_argument('--image', type=workspace); parser.add_argument('--prompt', type=workspace)
    parser.add_argument('--references', type=workspace, nargs='*', default=[])
    parser.add_argument('--model'); parser.add_argument('--target', type=float, nargs=3)
    parser.add_argument('--span', type=float); parser.add_argument('--complete', action='store_true')
    parser.add_argument('--images', nargs='+', help='Workspace image paths, optionally path|left,top,right,bottom')
    args = parser.parse_args()
    if args.command == 'materials':
        print(json.dumps(read(Path(__file__).with_name('materials.json')), indent=2)); return
    if args.asset is None: parser.error('Choose an asset directory')
    folder = local(ROOT, args.asset)
    file = folder / 'asset.json'
    if args.command == 'init':
        # A restart begins a fresh record; the previous one stays in history with the files it names.
        if file.exists(): print('Previous record kept as', preserve(folder, file))
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
        if 'texture' in asset: atlas(asset, folder)
        if asset['source']['mode'] == 'recipe':
            recipe = local(folder, asset['source']['path'])
            if recipe.suffix != '.py': raise ValueError('Recipe must be a Python file')
            # Recipes import the modelling tools of the checkout that runs them.
            subprocess.run([sys.executable, str(recipe)], cwd=folder, env=dict(os.environ, PYTHONPATH=str(Path(__file__).parent)), check=True)
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
        if not args.prompt: parser.error('record needs the exact --prompt, and --image for a supplied concept')
        print(record(asset, folder, 'concept', args.prompt.read_text(encoding='utf-8'), args.references, args.image))
        write(file, asset)
    elif args.command == 'texture':
        if 'texture' not in asset: raise ValueError('Declare the texture layout in asset.json first')
        if args.prompt:
            # A generated master gets the layout's sheet contract; a supplied one keeps the prompt that made it.
            prompt = args.prompt.read_text(encoding='utf-8')
            asset['texture']['master'] = record(asset, folder, 'master', prompt if args.image else master_prompt(asset['texture'])+prompt,
                                                args.references, args.image)
            write(file, asset)
        print(atlas(asset, folder))
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
