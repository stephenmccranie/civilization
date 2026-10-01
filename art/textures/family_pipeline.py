"""Reference-driven block families: one master, shared palette, bounded edits, reusable faces."""
import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw
from pipeline import ROOT, RUNTIME, prepare, validate, pixels


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def palette_for(master, heat=False, accents=()):
    rgb = master.convert('RGB').resize((32, 32), Image.Resampling.BOX)
    if len(accents)>8 or any(len(c)!=3 or any(not 0<=v<=255 for v in c) for c in accents):
        raise ValueError('Family accents must contain at most eight RGB colors')
    count = 24-len(accents)
    quantized = rgb.quantize(colors=count, dither=Image.Dither.NONE)
    colors = quantized.getpalette()[:count*3]
    colors += colors[:3] * ((count*3-len(colors))//3)
    colors += [value for color in accents for value in color]
    if heat:
        colors += [65, 20, 8, 105, 28, 8, 155, 42, 8, 205, 66, 10,
                   235, 110, 18, 250, 155, 35, 255, 195, 70, 255, 225, 140]
    colors += colors[:3] * ((768 - len(colors)) // 3)
    result = Image.new('P', (1, 1))
    result.putpalette(colors)
    return result


def protected_edit(base, edited, regions):
    result = base.copy()
    for box in regions:
        if len(box) != 4 or not (0 <= box[0] < box[2] <= 32 and 0 <= box[1] < box[3] <= 32):
            raise ValueError('Invalid edit rectangle')
        result.paste(edited.crop(box), box[:2])
    return result


def request(family, face):
    spec = family['faces'][face]
    if spec['operation'] != 'edit':
        raise ValueError('Only edit faces need image generation; reuse faces are copied, masters already exist')
    reference = ROOT / family['faces'][spec['reference']]['source']
    prompt = ('Edit the supplied image, which is the exact material and style authority for this block. '
              + spec['instruction'] + ' ' + family['style']
              + ' Full square opaque edge-to-edge orthographic texture. No perspective, scene, labels or margin.')
    return {'prompt': prompt, 'referenced_image_paths': [str(reference.resolve())],
            'reference_sha256': digest(reference)}


def build(family, receipts):
    faces, sources = {}, {}
    master_spec = family['faces'][family['master']]
    master_path = ROOT / master_spec['source']
    master = Image.open(master_path).convert('RGBA')
    palette = palette_for(master, accents=family.get("accents", ()))
    pending = dict(family['faces'])
    while pending:
        progressed = False
        for name, spec in list(pending.items()):
            reference = spec.get('reference')
            if reference and reference not in faces:
                continue
            kind = spec['operation']
            if kind == 'reuse':
                faces[name] = faces[reference].copy()
            else:
                path = ROOT / spec['source']
                if kind == 'edit':
                    receipt = receipts[name]
                    reference_path = ROOT / family['faces'][reference]['source']
                    if receipt['reference_sha256'] != digest(reference_path) or receipt['output_sha256'] != digest(path):
                        raise ValueError(f'{name}: changed reference/output; redo or record this edit explicitly')
                elif kind != 'master':
                    raise ValueError('Unknown family operation')
                source = Image.open(path).convert('RGBA')
                chosen = palette_for(master, heat=True, accents=family.get("accents", ())) if spec.get('heat') else palette
                face = prepare(source, {'kind': 'block', 'colors': 32}, palette=chosen)
                faces[name] = protected_edit(faces[reference], face, spec['regions']) if kind == 'edit' else face
                sources[name] = {'path': str(path.relative_to(ROOT)), 'sha256': digest(path)}
            validate(faces[name], {'kind': 'block', 'colors': 32})
            del pending[name]
            progressed = True
        if not progressed:
            raise ValueError('Cyclic or missing face reference')
    # One deterministic tone curve for every face preserves protected-region equality.
    # Apply after material quantization: unique-color budgets remain unchanged.
    if 'tone' in family:
        tone = family['tone']
        floor, gamma = tone['floor'], tone['gamma']
        lut = [round(floor + (255 - floor) * (v / 255) ** gamma) for v in range(256)]
        for name, face in faces.items():
            red, green, blue, alpha = face.split()
            faces[name] = Image.merge('RGBA', (red.point(lut), green.point(lut), blue.point(lut), alpha))
    return faces, sources


def review(faces, target):
    rows=(len(faces)+3)//4
    base=rows*220
    sheet=Image.new('RGB',(800,base+360),'#25282c')
    draw=ImageDraw.Draw(sheet)
    for i,(name,im) in enumerate(faces.items()):
        x,y=(i%4)*200,(i//4)*220
        draw.text((x+8,y+8),name,fill='white')
        sheet.paste(im.resize((192,192),Image.Resampling.NEAREST),(x+4,y+25))
    draw.text((8,base+10),'Front / side / side wrap strip',fill='white')
    for i,name in enumerate(('front','side','side')):
        sheet.paste(faces[name].resize((192,192),Image.Resampling.NEAREST),(8+i*192,base+32))
    draw.text((8,base+240),'Native faces - protected casing remains identical',fill='white')
    for i,im in enumerate(faces.values()):sheet.paste(im,(12+i*60,base+270))
    sheet.save(target)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=('request', 'record', 'prepare', 'check', 'publish'))
    parser.add_argument('family')
    parser.add_argument('--face')
    parser.add_argument('--output', type=Path)
    parser.add_argument('--prompt', type=Path, help='Exact prompt actually sent, if different from the request template')
    args = parser.parse_args()
    family = json.loads((ROOT / 'block_families.json').read_text())[args.family]
    if args.command in ('publish', 'check') and family.get('publication_owner'):
        raise ValueError('This retained source family is superseded; use ' + family['publication_owner'])
    folder = ROOT / 'families' / args.family
    folder.mkdir(parents=True, exist_ok=True)
    receipt_path = folder / 'receipts.json'
    receipts = json.loads(receipt_path.read_text()) if receipt_path.exists() else {}
    if args.command == 'request':
        value = request(family, args.face)
        print(json.dumps(value, indent=2))
        return
    if args.command == 'record':
        value = request(family, args.face)
        if args.prompt:
            value['prompt'] = args.prompt.read_text(encoding='utf-8')
        destination = ROOT / family['faces'][args.face]['source']
        destination.write_bytes(args.output.read_bytes())
        value['output_sha256'] = digest(destination)
        receipts[args.face] = value
        receipt_path.write_text(json.dumps(receipts, indent=2) + '\n')
        return
    faces, sources = build(family, receipts)
    for name, face in faces.items():
        path = folder / (name + '.png')
        if args.command == 'prepare':
            face.save(path)
        else:
            with Image.open(path) as previous:
                if previous.mode != 'RGBA' or previous.size != face.size or previous.tobytes() != face.tobytes():
                    raise ValueError('Stale family candidate; prepare again')
    if args.command == 'prepare':
        review(faces, folder / 'review.png')
        (folder / 'report.json').write_text(json.dumps({'sources': sources, 'faces': {n: digest(folder / (n + '.png')) for n in faces}}, indent=2) + '\n')
    if args.command == 'publish':
        for name, face in faces.items():
            target = RUNTIME / 'block' / f'{args.family}_{name}.png'
            target.parent.mkdir(parents=True, exist_ok=True)
            face.save(target)
    print(f'{args.command}: {args.family}, {len(faces)} faces')


if __name__ == '__main__':
    main()
