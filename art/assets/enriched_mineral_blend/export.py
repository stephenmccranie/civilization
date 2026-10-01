"""Reduce the approved enriched_mineral_blend master through the shared item pipeline."""
from pathlib import Path
import sys
import json
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[2] / 'art/textures'))
from pipeline import prepare, review_sheet, metrics

spec = {'id': 'enriched_mineral_blend', 'kind': 'item', 'extent': 28, 'colors': 16}
source = Image.open(HERE / 'mockup-01.png').convert('RGBA')
result = prepare(source, spec)
result.save(HERE / 'enriched_mineral_blend.png')
review_sheet([(spec, source, result)], HERE / 'sprite-review.png')
(HERE / 'enriched_mineral_blend.json').write_text(json.dumps({'parent': 'minecraft:item/generated',
    'textures': {'layer0': 'civilization:item/enriched_mineral_blend'}}, indent=2) + '\n')
print(metrics(result))
