"""Reduce the approved Tannery controller master through the shared block-face pipeline."""
from pathlib import Path
import sys
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[2] / 'art/textures'))
from pipeline import prepare, review_sheet, metrics

spec = {'id': 'tannery_front', 'kind': 'block', 'colors': 20}
source = Image.open(HERE / 'controller-front-master.png').convert('RGB').convert('RGBA')
result = prepare(source, spec)
result.save(HERE / 'tannery_front.png')
review_sheet([(spec, source, result)], HERE / 'controller-review.png')
print(metrics(result))
