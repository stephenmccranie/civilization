"""Make an evidence sheet from existing art/model views; never alters source images.

Inputs are path or path|left,top,right,bottom (explicit pixel crop). Choose comparable
cameras first: this tool aligns frames, not geometry, and does not judge fidelity.
"""
import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageOps, ImageDraw


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('output', type=Path)
    parser.add_argument('inputs', nargs='+')
    parser.add_argument('--labels', nargs='+')
    args = parser.parse_args()
    if args.labels and len(args.labels) != len(args.inputs):
        parser.error('Supply one label per input')
    sheet = Image.new('RGB', (600 * len(args.inputs), 650), '#ddd8cd')
    draw = ImageDraw.Draw(sheet)
    records = []
    for index, value in enumerate(args.inputs):
        name, sep, crop_text = value.partition('|')
        path = Path(name)
        with Image.open(path) as source:
            crop = list(map(int, crop_text.split(','))) if sep else [0, 0, *source.size]
            if len(crop) != 4 or not (0 <= crop[0] < crop[2] <= source.width and 0 <= crop[1] < crop[3] <= source.height):
                parser.error(f'Invalid crop for {path}')
            frame = ImageOps.contain(source.crop(crop).convert('RGBA'), (580, 600), Image.Resampling.LANCZOS)
        sheet.paste(frame, (index * 600 + (600-frame.width)//2, 35+(600-frame.height)//2), frame)
        label = args.labels[index] if args.labels else path.stem
        draw.text((index * 600 + 16, 12), label, fill='#272c2c')
        records.append({'source': str(path.resolve()), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(), 'crop': crop, 'label': label})
    args.output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(args.output)
    args.output.with_suffix('.json').write_text(json.dumps({'images': records}, indent=2)+'\n', encoding='utf8')
    print(args.output)


if __name__ == '__main__':
    main()
