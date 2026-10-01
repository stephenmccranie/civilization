"""Compare generated sprite masters at their real target size; never publishes assets."""
import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw
from pipeline import prepare, metrics

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('sources', nargs='+', type=Path)
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    modes = ('direct-12', 'direct-16', 'direct-24', 'via64-16')
    sheet = Image.new('RGB', (1060, 250 * len(args.sources)), '#24272b')
    draw = ImageDraw.Draw(sheet)
    report = []
    for row, path in enumerate(args.sources):
        source = Image.open(path).convert('RGBA')
        y = row * 250
        draw.text((10, y+8), path.stem, fill='white')
        thumb = source.copy(); thumb.thumbnail((180, 185))
        sheet.paste(thumb, (10, y+32), thumb)
        entry = {'source': str(path), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(), 'variants': {}}
        for col, mode in enumerate(modes):
            colors = int(mode.split('-')[1])
            master = source
            if mode.startswith('via64'):
                master = prepare(source, {'kind':'item', 'extent':56, 'colors':32}, 64)
            result = prepare(master, {'kind':'item', 'extent':28, 'colors':colors})
            filename = f'{row}-{mode}.png'; result.save(args.output / filename)
            x = 210 + col*210
            draw.text((x,y+8),mode,fill='white')
            zoom = result.resize((160,160),Image.Resampling.NEAREST)
            sheet.paste(zoom,(x,y+30),zoom)
            for dx,color in [(12,'#17191c'),(82,'#c6c6c6')]:
                draw.rectangle((x+dx,y+198,x+dx+42,y+240),fill=color)
                sheet.paste(result,(x+dx+5,y+203),result)
            entry['variants'][mode]={'file':filename,**metrics(result)}
        report.append(entry)
    sheet.save(args.output/'comparison.png')
    (args.output/'comparison.json').write_text(json.dumps(report,indent=2)+'\n')
    print(args.output/'comparison.png')

if __name__ == '__main__': main()
