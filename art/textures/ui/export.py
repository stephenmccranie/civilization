"""Rebuild the shared UI texture and its source receipt. High-resolution source slices preserve the authored metalwork."""
from pathlib import Path
import hashlib, io, json, sys
from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
source = HERE / 'cabinet-master.png'
out = ROOT / 'civilization-mod/src/main/resources/assets/civilization/textures/gui/cabinet.png'
out.parent.mkdir(parents=True, exist_ok=True)
# UI is not a 32-color world sprite: retain the generated bevel and rivet shading.
im = Image.open(source).convert('RGB').resize((512, 512), Image.Resampling.BOX)
encoded = io.BytesIO()
im.save(encoded, format='PNG')
pixels = encoded.getvalue()
receipt = {'source': str(source.relative_to(ROOT)), 'source_sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
           'reference_sha256': hashlib.sha256((ROOT/'concept_art/11_ui_coal_machine.png').read_bytes()).hexdigest(),
           'prompt_sha256': hashlib.sha256((HERE/'prompt.md').read_bytes()).hexdigest(),
           'source_size': list(Image.open(source).size),
           'output': str(out.relative_to(ROOT)), 'output_sha256': hashlib.sha256(pixels).hexdigest(),
           'size': [512,512], 'slice_border_source': 48, 'slice_border_gui': 18, 'outset_gui': 3, 'filter': 'BOX',
           'colors': 'RGB, no palette reduction', 'texels_per_gui_pixel': 48/18}

record = json.dumps(receipt, indent=2)+'\n'
if '--check' in sys.argv:
    assert out.read_bytes() == pixels, 'Stale UI export; run export.py'
    assert (HERE/'receipt.json').read_text(encoding='utf-8') == record, 'Stale UI receipt; run export.py'
    print('UI source, reference, prompt and exported pixels verified.')
else:
    out.write_bytes(pixels)
    (HERE / 'receipt.json').write_text(record, encoding='utf-8')
    print(receipt['output'])
