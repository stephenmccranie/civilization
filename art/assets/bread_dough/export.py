"""Reduce the approved bread_dough master through the shared item pipeline."""
from pathlib import Path
import sys
import json
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[2] / 'art/textures'))
from pipeline import prepare, review_sheet, metrics

spec = {'id': 'bread_dough', 'kind': 'item', 'extent': 28, 'colors': 16}
source = Image.open(HERE / 'mockup-01.png').convert('RGBA')
result = prepare(source, spec)
result.save(HERE / 'bread_dough.png')
review_sheet([(spec, source, result)], HERE / 'sprite-review.png')
(HERE / 'bread_dough.json').write_text(json.dumps({'parent': 'minecraft:item/generated',
    'textures': {'layer0': 'civilization:item/bread_dough'}}, indent=2) + '\n')
print(metrics(result))
