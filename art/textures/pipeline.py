"""Deterministic sprite preparation. Generated sources are references, never trusted grids."""
import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw, __version__ as pillow_version

ROOT = Path(__file__).resolve().parent
RUNTIME = ROOT.parents[1] / 'civilization-mod/src/main/resources/assets/civilization/textures'


def pixels(image):
    return list(image.get_flattened_data()) if hasattr(image, 'get_flattened_data') else list(image.getdata())


def metrics(image):
    image = image.convert('RGBA')
    values = pixels(image)
    return {'size': list(image.size), 'bbox': image.getchannel('A').point(lambda a: 255 if a >= 128 else 0).getbbox(),
            'opaque_colors': len({p[:3] for p in values if p[3] == 255}),
            'partial_alpha_pixels': sum(0 < p[3] < 255 for p in values)}


def prepare(source, spec, size=32, palette=None):
    image = source.convert('RGBA')
    if spec['kind'] not in ('item', 'block'):
        raise ValueError('kind must be item or block')
    if not 2 <= spec['colors'] <= 32:
        raise ValueError('colors must be 2..32')
    palette_colors = spec.get('palette_colors', spec['colors'])
    if not 2 <= palette_colors <= spec['colors']:
        raise ValueError('palette_colors must be 2..colors')
    alpha = image.getchannel('A')
    bbox = alpha.point(lambda a: 255 if a >= 128 else 0).getbbox()
    if bbox is None:
        raise ValueError('Empty silhouette')
    if spec['kind'] == 'item':
        if min(pixels(alpha)) == 255:
            raise ValueError('Item source needs real transparency; refusing to guess its background')
        if bbox[0] == 0 or bbox[1] == 0 or bbox[2] == image.width or bbox[3] == image.height:
            raise ValueError('Item touches source edge: possible clipping; regenerate with margin')
        extent = spec.get('extent', 28)
        if not 1 <= extent <= size - 4:
            raise ValueError('Item extent must leave at least two pixels of margin')
        image = image.crop(bbox)
        scale = extent / max(image.size)
        target = tuple(max(1, round(d * scale)) for d in image.size)
    else:
        if image.width != image.height or min(pixels(alpha)) != 255:
            raise ValueError('Block faces require square, fully opaque source images')
        target = (size, size)

    # Premultiply before area filtering so invisible RGB cannot create fringes.
    # We construct a new grid, not infer an exact grid from imperfect generated pixels.
    small = image.convert('RGBa').resize(target, Image.Resampling.BOX).convert('RGBA')
    coverage = small.getchannel('A').point(lambda a: 255 if a >= 128 else 0)
    visible = [p[:3] for p, a in zip(pixels(small), pixels(coverage)) if a]
    if not visible:
        raise ValueError('Silhouette vanished at target resolution')
    # Palette comes only from the subject: transparent background cannot steal a color.
    if palette is None:
        swatches = Image.new('RGB', (len(visible), 1))
        swatches.putdata(visible)
        palette = swatches.quantize(colors=palette_colors, method=Image.Quantize.MEDIANCUT,
                                    dither=Image.Dither.NONE)
    reduced = small.convert('RGB').quantize(palette=palette, dither=Image.Dither.NONE).convert('RGBA')
    reduced.putalpha(coverage)
    result = Image.new('RGBA', (size, size))
    result.paste(reduced, ((size - target[0]) // 2, (size - target[1]) // 2))
    # Optional authored pixel corrections are explicit and reproducible, never hidden heuristics.
    for edit in spec.get('pixels', []):
        x, y, rgba = edit
        if not (0 <= x < size and 0 <= y < size and len(rgba) == 4 and rgba[3] in (0, 255)):
            raise ValueError('Invalid authored pixel correction')
        result.putpixel((x, y), tuple(rgba))
    result.putdata([p if p[3] else (0, 0, 0, 0) for p in pixels(result)])
    validate(result, spec, size)
    return result


def validate(image, spec, size=32):
    data = metrics(image)
    if image.mode != 'RGBA' or image.size != (size, size):
        raise ValueError('Output must be native RGBA at target resolution')
    if data['partial_alpha_pixels'] or not data['bbox']:
        raise ValueError('Output must have nonempty, binary alpha')
    if data['opaque_colors'] > spec['colors']:
        raise ValueError('Palette limit exceeded')
    if spec['kind'] == 'item':
        x0, y0, x1, y1 = data['bbox']
        if min(x0, y0, size - x1, size - y1) < 2:
            raise ValueError('Item exceeds safe margins')
        if max(x1 - x0, y1 - y0) > spec.get('extent', 28):
            raise ValueError('Item exceeds declared extent')
    elif any(p[3] != 255 for p in pixels(image)):
        raise ValueError('Block face has transparency')
    return data


def review_sheet(results, destination):
    sheet = Image.new('RGB', (760, 340 * len(results)), '#24272b')
    draw = ImageDraw.Draw(sheet)
    for row, (spec, source, result) in enumerate(results):
        y = row * 340
        draw.text((12, y + 8), spec['id'] + ' | source / 32px grid / native on dark & light', fill='white')
        thumb = source.copy()
        thumb.thumbnail((250, 260))
        sheet.paste(thumb, (12, y + 40), thumb)
        enlarged = result.resize((256, 256), Image.Resampling.NEAREST)
        sheet.paste(enlarged, (282, y + 40), enlarged)
        for i in range(33):
            draw.line((282 + i * 8, y + 40, 282 + i * 8, y + 296), fill='#404348')
            draw.line((282, y + 40 + i * 8, 538, y + 40 + i * 8), fill='#404348')
        for x, color in ((574, '#17191c'), (646, '#c6c6c6')):
            draw.rectangle((x, y + 48, x + 55, y + 103), fill=color)
            sheet.paste(result, (x + 12, y + 60), result)
        draw.text((282, y + 310), json.dumps(metrics(result)), fill='white')
    sheet.save(destination)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=('prepare', 'check', 'publish'))
    args = parser.parse_args()
    manifest = json.loads((ROOT / 'pipeline.json').read_text())
    size = manifest['size']
    output = ROOT / 'candidates'
    output.mkdir(exist_ok=True)
    reports, results = {}, []
    for spec in manifest['items']:
        name = spec['id']
        if not name.replace('_', '').isalnum():
            raise ValueError('Invalid asset id')
        with Image.open(ROOT / spec['source']) as opened:
            source = opened.convert('RGBA')
        expected = prepare(source, spec, size)
        path = output / (name + '.png')
        if args.command == 'prepare':
            expected.save(path)
        with Image.open(path) as candidate:
            validate(candidate, spec, size)
            if candidate.tobytes() != expected.tobytes():
                raise ValueError(f'{name}: stale or unrecorded edits; encode edits in manifest and prepare again')
        reports[name] = {'source': metrics(source), 'output': metrics(expected),
                         'source_sha256': hashlib.sha256((ROOT / spec['source']).read_bytes()).hexdigest(),
                         'output_sha256': hashlib.sha256(path.read_bytes()).hexdigest()}
        results.append((spec, source, expected))
    # Validate the entire batch before replacing any runtime asset.
    if args.command == 'publish':
        for spec, _, result in results:
            target = RUNTIME / spec['kind'] / (spec['id'] + '.png')
            target.parent.mkdir(parents=True, exist_ok=True)
            result.save(target)
    if args.command == 'prepare':
        review_sheet(results, output / 'review.png')
        for kind in ('item', 'block'):
            group = [r for r in results if r[0]['kind'] == kind]
            for start in range(0, len(group), 4):
                review_sheet(group[start:start + 4], output / f'review-{kind}-{start // 4 + 1}.png')
        (output / 'report.json').write_text(json.dumps({'pillow': pillow_version, 'assets': reports}, indent=2) + '\n')
    print(json.dumps(reports, indent=2))


if __name__ == '__main__':
    main()
