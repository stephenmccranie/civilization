"""Use the active pack's cobblestone, with only custom iron panels layered over it."""
import json
from pathlib import Path

MODELS = Path(__file__).resolve().parents[2] / 'civilization-mod/src/main/resources/assets/civilization/models/block'

def model(hot=False):
    faces = {side: {'texture': '#stone', 'uv': [0, 0, 16, 16], 'cullface': side}
             for side in ('north', 'south', 'east', 'west', 'up', 'down')}
    elements = [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': faces}]
    # Native texture coordinates mapped to the original face; no generated stone is used.
    for u1, v1, u2, v2 in ((5, 2.5, 11, 4.5), (3, 6, 13, 14)):
        elements.append({'from': [16-u2, 16-v2, -0.01], 'to': [16-u1, 16-v1, -0.01],
                         'faces': {'north': {'texture': '#ports', 'uv': [u1, v1, u2, v2], 'cullface': 'north'}}})
    return {'parent': 'minecraft:block/block', 'textures': {'particle': 'minecraft:block/cobblestone',
            'stone': 'minecraft:block/cobblestone', 'ports': 'civilization:block/kiln_front' + ('_on' if hot else '')}, 'elements': elements}

if __name__ == '__main__':
    raise SystemExit('Kiln models now belong to art/assets/thermal_controllers/export.py; publish through its asset manifest.')
    for hot in (False, True):
        (MODELS / ('brick_kiln' + ('_on' if hot else '') + '.json')).write_text(json.dumps(model(hot), indent=2)+'\n')
