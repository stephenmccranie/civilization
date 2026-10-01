"""Publish three native 32px sprites from the reviewed frontier mockups."""
from pathlib import Path
import sys
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[2] / 'art/textures'))
from pipeline import prepare, review_sheet, metrics

runtime = HERE.parents[2] / 'civilization-mod/src/main/resources/assets/civilization/textures'
sheet = Image.open(HERE / 'instrument-and-ore-master.png').convert('RGBA')
raw = Image.open(HERE / 'raw-uranium-master.png').convert('RGBA')
instrument = sheet.crop((0, 0, 887, 887))
# The concept sheet includes a drawn comparison frame. Strip only that margin;
# the dark cast-metal silhouette and probe remain the image source.
for y in range(instrument.height):
    for x in range(instrument.width):
        if x < 56 or x >= 831 or y < 56 or y >= 827:
            instrument.putpixel((x, y), (0, 0, 0, 0))
ore = sheet.crop((936, 48, 1724, 836))
ore.putalpha(255)
sources = {
    'geiger_counter': (instrument, 'item', 16),
    'uranium_ore': (ore, 'block', 24),
    'raw_uranium': (raw, 'item', 16),
}
review = []
for name, (source, kind, colors) in sources.items():
    spec = {'id': name, 'kind': kind, 'extent': 28, 'colors': colors}
    output = prepare(source, spec)
    output.save(HERE / f'{name}.png')
    target = runtime / kind / f'{name}.png'
    target.parent.mkdir(parents=True, exist_ok=True)
    output.save(target)
    review.append((spec, source, output))
    print(name, metrics(output))
review_sheet(review, HERE / 'sprite-review.png')
