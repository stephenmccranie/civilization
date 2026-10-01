"""Industrial steel: one body and referenced generated controller/fitting atlases."""
from pathlib import Path
import json
import sys
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[2] / 'art/textures'))
from pipeline import prepare, review_sheet
from family_pipeline import protected_edit, palette_for

def reduce(name, colors=24):
    source = Image.open(HERE / name).convert('RGB').convert('RGBA')
    return source, prepare(source, {'kind': 'block', 'colors': colors})

master, steel = reduce('steel-master.png')
palette = palette_for(master, heat=True, accents=[
    [239,224,192], [204,185,144], [178,139,66], [218,174,76],
    [116,91,45], [70,59,36], [38,37,33], [54,52,48]])
pump_source, pump = reduce('pump-master.png', 32)
faces = {'refinery_side': steel, 'refinery_top': steel.copy(),
         'oil_pump_front': protected_edit(steel, pump, [[8, 3, 24, 17], [4, 17, 28, 28]])}
def tile(file, x, y):
    atlas = Image.open(HERE / file).convert('RGB').convert('RGBA')
    assert atlas.width == atlas.height and atlas.width % 2 == 0
    half = atlas.width // 2
    source = atlas.crop((x*half, y*half, (x+1)*half, (y+1)*half))
    face = prepare(source, {'kind': 'block', 'colors': 32}, palette=palette)
    face = protected_edit(steel, face, [[2,2,30,30]])
    # Generated atlas borders can drift; actual plate corners stay identical.
    for box in ([2,2,7,7], [25,2,30,7], [2,25,7,30], [25,25,30,30]):
        face.paste(steel.crop(box), box[:2])
    return face

faces['refinery_heater'] = tile('controllers-master.png', 0, 0)
faces['column_front'] = tile('controllers-master.png', 1, 0)
faces['condenser_front'] = tile('controllers-master.png', 0, 1)
faces['fuel_tank_front'] = tile('controllers-master.png', 1, 1)
faces['refinery_fins'] = tile('fittings-master.png', 0, 1)
faces['coal_drill_front'] = tile('fittings-master.png', 1, 1)
# Only the inspection window changes; the generator cannot brighten the casing.
faces['refinery_heater_cold'] = protected_edit(faces['refinery_heater'],
    tile('fittings-master.png', 0, 0), [[11,18,21,23]])
rows = []
for name, face in faces.items():
    face = face.convert('RGB').quantize(palette=palette, dither=Image.Dither.NONE).convert('RGBA')
    # Photon darkens shaded metal substantially. One shared curve preserves all
    # protected borders and state equality while keeping the steel readable.
    lut = [round(12 + 243 * (v / 255) ** 0.75) for v in range(256)]
    r, g, b, a = face.split()
    face = Image.merge('RGBA', (r.point(lut), g.point(lut), b.point(lut), a))
    face.save(HERE / f'{name}.png')
    rows.append(({'id': name, 'kind': 'block'}, master if name != 'oil_pump_front' else pump_source, face))
review_sheet(rows, HERE / 'texture-review.png')
models = {'oil_pump': 'oil_pump_front', 'oil_refinery': 'refinery_heater_cold',
    'oil_refinery_on': 'refinery_heater', 'distillation_column': 'column_front',
    'condenser': 'condenser_front', 'fuel_tank': 'fuel_tank_front', 'coal_drill': 'coal_drill_front'}
for name, front in models.items():
    model = {'parent': 'minecraft:block/orientable', 'textures': {
        'side': 'civilization:block/refinery_side', 'top': 'civilization:block/refinery_top',
        'front': 'civilization:block/'+front}}
    (HERE / f'{name}.json').write_text(json.dumps(model, indent=2)+'\n')
asset_file = HERE / 'asset.json'
asset = json.loads(asset_file.read_text(encoding='utf-8'))
asset['sources'] = ['export.py'] + [f'{name}-{suffix}' for name in ('steel', 'pump', 'controllers', 'fittings') for suffix in ('master.png', 'prompt.txt')]
# Older helm/guardrail models sample a brass texel in this sheet. Keep that
# sampler stable; no industrial controller references the old face anymore.
asset['sources'].append('references/legacy-front.png')
(HERE / 'refinery_front.png').write_bytes((HERE / 'references/legacy-front.png').read_bytes())
asset['outputs'] = [{'source': f'{n}.png', 'target': f'textures/block/{n}.png', 'format': 'png', 'size': [32,32], 'alpha': 'opaque'} for n in faces]
asset['outputs'] += [{'source': f'{n}.json', 'target': f'models/block/{n}.json', 'format': 'java_model'} for n in models]
asset['outputs'].append({'source': 'refinery_front.png', 'target': 'textures/block/refinery_front.png', 'format': 'png', 'size': [32,32], 'alpha': 'opaque'})
asset_file.write_text(json.dumps(asset, indent=2)+'\n', encoding='utf-8')
print('Prepared shared steel and all six original industrial controller faces.')
