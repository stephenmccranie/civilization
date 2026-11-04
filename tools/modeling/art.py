"""Compact art workflow: concepts, texture masters and atlases, native exports, reviews and publication.

Existing schema-1 assets retain pipeline.py unchanged.
"""
import argparse
import hashlib
import json
import math
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
    if len(asset['references']) > 5: raise ValueError('Codex\'s image tool takes at most five references')
    for neighbour in asset.get('neighbours', []):
        if not local(ROOT, neighbour).is_file(): raise ValueError(f'Neighbour {neighbour} is missing')
    outputs = {local(RUNTIME, o['target']) for o in asset.get('outputs', [])}
    for ref in asset['references']:
        if not ref.get('role') or not local(ROOT, ref['path']).is_file():
            raise ValueError('Reference needs an existing file and a role')
        if local(ROOT, ref['path']) in outputs:
            raise ValueError(f'{ref["path"]} is this asset\'s own output; reference its kept copy in art/references instead')
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


def images(texture):
    """The images a layout fills, by path and size: its declared images, or the single atlas most layouts name."""
    return texture.get('images') or {texture['atlas']: texture['size']}


def image_of(texture, region): return region.get('image', texture.get('atlas'))


def layout(asset):
    """A texture layout reduces grid cells of one recorded master to the rectangles of its images. Panels and sprites take
    declared rectangles that never overlap within an image. An inset names the earlier panel it fills: that panel is drawn
    with an opening, and the inset's cell is reduced into the opening wherever the drawing put it."""
    texture = asset['texture']; cols, rows = texture['grid']; sizes = images(texture); regions = texture['regions']; order = list(regions)
    if texture.get('master') not in (None, *(g['file'] for g in asset['generations'])):
        raise ValueError('The texture master must be a recorded generation')
    ImageColor.getrgb(texture['ground'])
    if not 2 <= texture['colors'] <= 256: raise ValueError('A texture palette has 2 to 256 colours')
    placed = {}
    for name, region in regions.items():
        col, row = region['cell']
        if not (0 <= col < cols and 0 <= row < rows): raise ValueError(f'Texture region {name} takes a cell outside the master grid')
        image = image_of(texture, region)
        if image not in sizes: raise ValueError(f'Texture region {name} names no declared image')
        if 'in' in region:
            host = regions.get(region['in'])
            if (host is None or 'in' in host or host.get('sprite') or image_of(texture, host) != image
                    or order.index(region['in']) > order.index(name) or host['cell'] == region['cell'] or 'rect' in region):
                raise ValueError(f'Inset {name} must name an earlier panel of its image, from its own cell, and take no rectangle')
            continue
        x, y, w, h = region['rect']; width, height = sizes[image]
        if min(w, h) <= 0 or min(x, y) < 0 or x+w > width or y+h > height: raise ValueError(f'Texture region {name} lies outside its image')
        for other, (ox, oy, ow, oh) in placed.get(image, []):
            if x < ox+ow and ox < x+w and y < oy+oh and oy < y+h: raise ValueError(f'Texture regions {other} and {name} overlap')
        placed.setdefault(image, []).append((name, region['rect']))
    hosts = [r['in'] for r in regions.values() if 'in' in r]
    if len(hosts) != len(set(hosts)): raise ValueError('A panel holds one inset')


def master_prompt(texture):
    """The master sheet contract the atlas reduction relies on, stated from the layout ahead of the asset's cell descriptions."""
    cols, rows = texture['grid']; regions = texture['regions']; notes = {}
    for name, region in regions.items():
        if 'in' in region:
            note = (f'a panel reduced into the opening left in the {region["in"]} panel, for {name}: only what shows through the opening, '
                    'edge to edge, with no frame, rim, border or screws of its own, since the opening already frames it')
            host = notes.setdefault(tuple(regions[region['in']]['cell']), [])
            if 'with one opening' not in ' '.join(host): host.append('with one opening left as a clean rectangle of plain background, filled from another cell')
        else:
            fit = 'an icon reduced to fit within' if region.get('sprite') else 'a panel reduced to'
            note = f'{fit} {region["rect"][2]} by {region["rect"][3]} pixels for {name}'
        notes.setdefault(tuple(region['cell']), []).insert(0, note)
    cells = '\n'.join(f'Row {row+1}, column {col+1}: {", ".join(found)}.'
                      for (col, row), found in sorted(notes.items(), key=lambda item: item[0][::-1]))
    return (f'Draw a texture master sheet: a strict {cols} by {rows} grid of equal cells on a perfectly flat, solid {texture["ground"]} '
            'background, with no grid lines. Each cell listed below holds one subject, centred with a margin of plain background at least '
            '6% of the cell on every side; leave other cells empty. A panel is a flat rectangular material surface seen straight on, with no '
            'perspective, 3D form, cast shadow or lighting gradient; it is solid edge to edge, and the background shows inside it only '
            'where its cell asks for an opening. An icon is one whole object drawn as a pixel-art inventory sprite, with no cast shadow, '
            'floor or outline glow; its openings show the plain background. Icons in one sheet are drawn at one common scale, so things '
            'of the same size are drawn the same size. No text. Crisp and matte, with broad two-to-three-tone colour clusters and no '
            'dithering or noise. Each subject is reduced by area averaging to the pixel sizes listed: shape it to the largest one and draw '
            f'seams, edges and fasteners bold enough to survive.\n{cells}\n\n')


def attached(asset):
    """The asset's references in the order they are attached to a generation, each with the role it plays."""
    lines = '\n'.join(f'{i}. {Path(r["path"]).name}: {r["role"]}' for i, r in enumerate(asset['references'], 1))
    return f'The attached images are, in order:\n{lines}\n\n'


def subject(crop, ground):
    """Mask of the pixels that differ from the flat ground, opened so isolated compression specks drop out."""
    difference = ImageChops.difference(crop, Image.new('RGB', crop.size, ground)).split()
    mask = ImageChops.lighter(ImageChops.lighter(*difference[:2]), difference[2]).point(lambda v: 255 if v > KEY else 0)
    return mask.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.MaxFilter(3))


def subjects(master, ground, cols, rows):
    """Each cell's subject as its mask and box on the master: the connected regions that differ from the ground, each
    given to the cell holding its centre, so a panel the generator drew a little past its cell's edge stays whole and its
    neighbour's cell never sees the overflow. Regions are found on a quarter-size copy, where every master pixel's block
    keeps it, and then taken back at full size."""
    mask = subject(master, ground); width, height = -(-mask.width//4)*4, -(-mask.height//4)*4
    padded = Image.new('L', (width, height)); padded.paste(mask)
    small = padded.reduce(4).point(lambda v: 255 if v else 0); w, h = small.size; seen = bytearray(small.tobytes())
    labels = bytearray(w*h); owner = {}
    for start in range(w*h):
        if not seen[start] or labels[start]: continue
        label = len(owner)+1
        if label > 255: raise ValueError('The master holds too many separate specks to read; draw it on a cleaner ground')
        labels[start] = label; stack = [start]; x0, y0, x1, y1 = w, h, 0, 0
        while stack:
            i = stack.pop(); x, y = i % w, i//w; x0, y0, x1, y1 = min(x0, x), min(y0, y), max(x1, x+1), max(y1, y+1)
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    j = i+dy*w+dx
                    if 0 <= x+dx < w and 0 <= y+dy < h and seen[j] and not labels[j]: labels[j] = label; stack.append(j)
        owner[label] = (min(cols-1, (x0+x1)*2*cols//mask.width), min(rows-1, (y0+y1)*2*rows//mask.height))
    full = Image.frombytes('L', (w, h), bytes(labels)).resize((width, height), Image.Resampling.NEAREST).crop((0, 0, *mask.size))
    found = {}
    for cell in set(owner.values()):
        own = ImageChops.multiply(mask, full.point([255 if owner.get(v) == cell else 0 for v in range(256)]))
        found[cell] = own, own.getbbox()
    return found


def opening(mask, box):
    """The opening drawn inside a panel: plain ground enclosed by it, in master coordinates, or None. The panel's rim is
    left out so rounded corners are not openings, and specks are dropped."""
    m = max(4, round(0.03*min(box[2]-box[0], box[3]-box[1])))
    inner = (box[0]+m, box[1]+m, box[2]-m, box[3]-m)
    found = ImageChops.invert(mask.crop(inner)).filter(ImageFilter.MinFilter(5)).filter(ImageFilter.MaxFilter(5)).getbbox()
    return found and (inner[0]+found[0], inner[1]+found[1], inner[0]+found[2], inner[1]+found[3])


def atlas(asset, folder):
    """Reduce each region's master subject into its image with one palette shared by all the layout's images, write the
    images, and draw the inspection sheet.

    A panel fills its rectangle. An inset fills the opening drawn in its host panel. Sprites keep their silhouettes and
    share one scale, so things drawn the same size come out the same size, each centred in its rectangle on transparency."""
    texture = asset['texture']; regions = texture['regions']
    if not texture.get('master'): raise ValueError('Record a texture master first with texture --prompt')
    with Image.open(local(folder, texture['master'])) as opened: master = opened.convert('RGB')
    cols, rows = texture['grid']; found = subjects(master, texture['ground'], cols, rows)
    for col, row in {tuple(r['cell']) for r in regions.values()}:
        box = found.get((col, row), (None, None))[1]
        # A subject cut by the master's edge has no margin to trust.
        if box is None or 0 in box[:2] or box[2] == master.width or box[3] == master.height:
            raise ValueError(f'Master cell {col},{row} has no subject inside a margin of plain {texture["ground"]}')
        if max((box[2]-box[0])*cols/master.width, (box[3]-box[1])*rows/master.height) < 0.5:
            raise ValueError(f'Master cell {col},{row} holds a subject under half its size; the sheet asks for it to fill the cell')
    def panel(region):
        box = found[tuple(region['cell'])][1]; rim = max(2, (box[2]-box[0])//60)  # Inside the antialiased rim.
        return (box[0]+rim, box[1]+rim, box[2]-rim, box[3]-rim)
    sprites = [n for n, r in regions.items() if r.get('sprite')]
    scale = min((min(r['rect'][2]/(b[2]-b[0]), r['rect'][3]/(b[3]-b[1])) for r in map(regions.get, sprites)
                 for _, b in [found[tuple(r['cell'])]]), default=None)
    filled = {r['in'] for r in regions.values() if 'in' in r}; placed = {}; openings = []
    for name, region in regions.items():
        mask, box = found[tuple(region['cell'])]
        if region.get('sprite'):
            # Drop the rim blended with the ground, a quarter of an output pixel deep, then reduce premultiplied
            # so the ground cannot tint the edge.
            width, height = region['rect'][2:]
            crop = master.crop(box).convert('RGBA'); rim = max(2, round(0.25/scale)); pad = (box[0]-rim, box[1]-rim, box[2]+rim, box[3]+rim)
            crop.putalpha(mask.crop(pad).filter(ImageFilter.MinFilter(2*rim+1)).crop((rim, rim, rim+crop.width, rim+crop.height)))
            small = crop.convert('RGBa').resize((max(1, round(crop.width*scale)), max(1, round(crop.height*scale))), Image.Resampling.BOX).convert('RGBA')
            small.putalpha(small.getchannel('A').point(lambda a: 255 if a >= 128 else 0))
            tile = Image.new('RGBA', (width, height)); tile.paste(small, ((width-small.width)//2, (height-small.height)//2))
            placed[name] = tile, tuple(region['rect'][:2])
        elif 'in' in region:
            host = regions[region['in']]; hmask, hbox = found[tuple(host['cell'])]
            hole = opening(hmask, hbox)
            if hole is None: raise ValueError(f'The {region["in"]} panel has no opening drawn for {name}')
            p = panel(host); x, y, w, h = host['rect']; sx, sy = w/(p[2]-p[0]), h/(p[3]-p[1])
            # Cover every pixel the opening touches, so no ground-tinted edge survives.
            x0, y0 = math.floor(x+(hole[0]-p[0])*sx), math.floor(y+(hole[1]-p[1])*sy)
            x1, y1 = math.ceil(x+(hole[2]-p[0])*sx), math.ceil(y+(hole[3]-p[1])*sy)
            placed[name] = master.crop(panel(region)).resize((x1-x0, y1-y0), Image.Resampling.BOX).convert('RGBA'), (x0, y0)
            openings.append(hole)
        else:
            if name not in filled and opening(mask, box):
                raise ValueError(f'The {name} panel shows the background inside it; draw it solid or declare an inset to fill its opening')
            placed[name] = master.crop(panel(region)).resize(tuple(region['rect'][2:]), Image.Resampling.BOX).convert('RGBA'), tuple(region['rect'][:2])
    # Regions paint in declared order, so an inset covers its host's opening. The palette comes from the visible result,
    # each region weighted equally, so a small vent, window or icon keeps its colours beside a large panel; it applies
    # without dithering, and k-means refinement moves each colour to its cluster's mean. Unused space in an image of
    # panels takes the mean colour; an image holding a sprite stays transparent around it.
    composed = {}
    for target, size in images(texture).items():
        image = Image.new('RGBA', tuple(size))
        for name, region in regions.items():
            if image_of(texture, region) == target: image.alpha_composite(*placed[name])
        composed[target] = image
    parts = []
    for name, region in regions.items():
        tile, (x, y) = placed[name]; data = composed[image_of(texture, region)].crop((x, y, x+tile.width, y+tile.height)).tobytes()
        parts.append([data[i:i+3] for i in range(0, len(data), 4) if data[i+3]])
    most = max(len(part) for part in parts)
    visible = [pixel for part in parts if part for pixel in part*(most//len(part))]
    swatch = Image.frombytes('RGB', (len(visible), 1), b''.join(visible))
    palette = swatch.quantize(colors=texture['colors'], method=Image.Quantize.MEDIANCUT, kmeans=16, dither=Image.Dither.NONE)
    written = {}
    for target, image in composed.items():
        transparent = any(r.get('sprite') for r in regions.values() if image_of(texture, r) == target)
        coverage = image.getchannel('A')
        if not transparent: image = Image.alpha_composite(Image.new('RGBA', image.size, (*map(round, ImageStat.Stat(swatch).mean), 255)), image)
        image = image.convert('RGB').quantize(palette=palette, dither=Image.Dither.NONE).convert('RGB')
        if transparent: image = Image.composite(image.convert('RGBA'), Image.new('RGBA', image.size), coverage)
        path = local(folder, target)
        if path.is_file(): preserve(folder, path)
        path.parent.mkdir(parents=True, exist_ok=True); image.save(path); written[target] = image
    boxes = [found[tuple(r['cell'])][1] for r in regions.values()]
    neighbours = [Image.open(local(ROOT, n)).convert('RGBA') for n in asset.get('neighbours', [])]
    (folder/'review').mkdir(exist_ok=True); sheet(master, boxes, openings, written, placed, texture, neighbours).save(folder/'review/texture.png')
    return [local(folder, target) for target in written]


def sheet(master, boxes, openings, written, placed, texture, neighbours):
    """The subjects (red) and openings (cyan) found on the master; the images they reduce to, side by side with their
    regions outlined and named; and a lineup of the images beside their neighbours at native and triple size on dark and
    light grounds."""
    offsets, x = {}, 0
    for target, image in written.items(): offsets[target] = x; x += image.width + 8
    image = Image.new('RGBA', (x-8, max(i.height for i in written.values())))
    for target, part in written.items(): image.paste(part.convert('RGBA'), (offsets[target], 0))
    zoom = max(1, 512//image.height)
    right = Image.new('RGBA', (image.width*zoom, image.height*zoom), (32, 32, 32, 255))
    right.alpha_composite(image.resize(right.size, Image.Resampling.NEAREST)); draw = ImageDraw.Draw(right)
    for name, region in texture['regions'].items():
        tile, (rx, ry) = placed[name]
        x, y, w, h = (v*zoom for v in (rx+offsets[image_of(texture, region)], ry, *tile.size))
        draw.rectangle((x, y, x+w-1, y+h-1), outline=(255, 0, 0)); draw.text((x+3, y+2), name, fill=(255, 255, 255), stroke_width=1, stroke_fill=(0, 0, 0))
    scale = right.height/master.height
    left = master.resize((round(master.width*scale), right.height)); draw = ImageDraw.Draw(left)
    for box in boxes: draw.rectangle(tuple(round(v*scale) for v in box), outline=(255, 0, 0), width=2)
    for box in openings: draw.rectangle(tuple(round(v*scale) for v in box), outline=(0, 255, 255), width=2)
    # Opaque square faces may repeat across neighbouring blocks, so the lineup also shows each tiled two by two.
    tiles = []
    for part in (i.convert('RGBA') for i in written.values()):
        if part.width == part.height and part.getchannel('A').getextrema()[0] == 255:
            tiled = Image.new('RGBA', (part.width*2, part.height*2))
            for dx in (0, part.width):
                for dy in (0, part.height): tiled.paste(part, (dx, dy))
            tiles.append(tiled)
    parts = [i.convert('RGBA') for i in written.values()] + neighbours + tiles
    zoom3 = 3 if max(p.height for p in parts) <= 128 else 1
    lineup = Image.new('RGBA', (sum(p.width*(1+zoom3)+24 for p in parts)+8, 2*(max(p.height for p in parts)*zoom3+16)))
    for row, ground in enumerate(((36, 37, 42, 255), (198, 198, 198, 255))):
        y = row*lineup.height//2; lineup.paste(ground, (0, y, lineup.width, y+lineup.height//2)); x = 8
        for part in parts:
            lineup.alpha_composite(part, (x, y+8)); x += part.width+8
            lineup.alpha_composite(part.resize((part.width*zoom3, part.height*zoom3), Image.Resampling.NEAREST), (x, y+8)); x += part.width*zoom3+16
    top = Image.new('RGB', (left.width+16+right.width, right.height), (32, 32, 32))
    top.paste(left, (0, 0)); top.paste(right.convert('RGB'), (left.width+16, 0))
    result = Image.new('RGB', (max(top.width, lineup.width), top.height+16+lineup.height), (32, 32, 32))
    result.paste(top, (0, 0)); result.paste(lineup.convert('RGB'), (0, top.height+16))
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
    # Java models measure this asset's own textures and any pack copies it declares; other materials belong to the pack
    # or to other assets.
    own = {f'civilization:{o["target"][len("textures/"):-len(".png")]}': o['source'] for o in asset['outputs']
           if o['target'].startswith('textures/') and o['target'].endswith('.png')}
    for model in models:
        data = read(local(folder, model))
        if isinstance(data.get('textures'), dict):
            materials = {**own, **asset.get('material_textures', {})}
            def pixels(resource):
                visited = set()
                while resource and resource.startswith('#'):
                    if resource in visited: raise ValueError('Cyclic Java texture alias')
                    visited.add(resource); resource = data['textures'].get(resource[1:])
                if resource not in materials: return None
                if resource not in own and materials[resource] not in asset['sources']:
                    raise ValueError('Java material pixels must be declared sources')
                with Image.open(local(folder, materials[resource])) as image: return image.size
            if not data.get('elements'):
                # A parent template such as cube or orientable spreads each texture over a whole face.
                for resource in data['textures'].values():
                    found = pixels(resource)
                    if found and found != (asset['texels_per_block'],)*2:
                        raise ValueError(f'UV density mismatch: {model}: whole face {resource} is {found}')
                continue
            for index, cube in enumerate(data['elements']):
                size = [abs(b-a) for a, b in zip(cube['from'], cube['to'])]
                for face, entry in cube.get('faces', {}).items():
                    found = pixels(entry.get('texture'))
                    if found is None: continue
                    width, height = found
                    axes = (0, 1) if face in ('north', 'south') else (2, 1) if face in ('east', 'west') else (0, 2)
                    uv = entry.get('uv', [0, 0, size[axes[0]], size[axes[1]]])
                    actual = (abs(uv[2]-uv[0])*width/16, abs(uv[3]-uv[1])*height/16)
                    if entry.get('rotation', 0) % 180 == 90: actual = actual[::-1]
                    expected = tuple(size[i]*density for i in axes)
                    if any(abs(a-b)>1.01 for a,b in zip(actual,expected)):
                        raise ValueError(f'UV density mismatch: {model}:element {index}/{face}: {actual} versus {expected}')
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


def record(asset, folder, stem, prompt, image=None):
    """Keep a generated, or supplied, image in the asset with its exact prompt and its declared references; returns its
    file name. A generation is told each reference's role ahead of the prompt."""
    references = {str(p): sha(p) for p in (local(ROOT, r['path']) for r in asset['references'])}
    if not image: prompt = attached(asset) + prompt
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
    parser.add_argument('--evidence', type=workspace, nargs='+')
    parser.add_argument('--image', type=workspace); parser.add_argument('--prompt', type=workspace)
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
        print(record(asset, folder, 'concept', args.prompt.read_text(encoding='utf-8'), args.image))
        write(file, asset)
    elif args.command == 'texture':
        if 'texture' not in asset: raise ValueError('Declare the texture layout in asset.json first')
        if args.prompt:
            # A generated master gets the layout's sheet contract; a supplied one keeps the prompt that made it.
            prompt = args.prompt.read_text(encoding='utf-8')
            asset['texture']['master'] = record(asset, folder, 'master', prompt if args.image else master_prompt(asset['texture'])+prompt, args.image)
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
        # Evidence may come from anywhere in the workspace, such as a scene's screenshots; the record keeps its own copy.
        evidence = [local(ROOT, p) for p in args.evidence]
        for path in evidence:
            with Image.open(path) as image: image.verify()
        evidence = {preserve(folder, p): sha(p) for p in evidence}
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
