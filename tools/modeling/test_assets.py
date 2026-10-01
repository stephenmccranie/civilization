"""Fast checks for the export gate's failure cases; no Minecraft or editor startup."""
import json
import tempfile
import unittest
from pathlib import Path
from PIL import Image
from assets import validate

class AssetChecks(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / 'source.bbmodel').write_text('{}')
        Image.new('RGBA', (64, 64)).save(self.root / 'atlas.png')
        self.geometry = {'minecraft:geometry': [{'description': {'texture_width': 64, 'texture_height': 64},
            'bones': [{'name': 'wheel', 'pivot': [0, 0, 0], 'cubes': [{'origin': [0, 0, 0], 'size': [16, 16, 16],
                'uv': {'north': {'uv': [0, 0], 'uv_size': [32, 32]}}}]}]}]}
        self.animation = {'animations': {'cycle': {'animation_length': 2, 'bones': {'wheel': {}}}}}
        self.manifest = {'id': 'fixture', 'project': 'source.bbmodel', 'geometry': 'model.json',
            'animation': 'animation.json', 'texture': 'atlas.png', 'required_animations': ['cycle']}

    def check(self):
        for name, value in [('manifest.json', self.manifest), ('model.json', self.geometry), ('animation.json', self.animation)]:
            (self.root / name).write_text(json.dumps(value))
        return validate(self.root / 'manifest.json')

    def test_valid(self):
        self.assertEqual(self.check()[2]['cubes'], 1)

    def test_missing_texture(self):
        (self.root / 'atlas.png').unlink()
        with self.assertRaisesRegex(ValueError, 'Missing asset'): self.check()

    def test_uv_outside_atlas(self):
        self.geometry['minecraft:geometry'][0]['bones'][0]['cubes'][0]['uv']['north']['uv'] = [50, 0]
        with self.assertRaisesRegex(ValueError, 'UV outside'): self.check()

    def test_hierarchy_cycle(self):
        self.geometry['minecraft:geometry'][0]['bones'][0]['parent'] = 'wheel'
        with self.assertRaisesRegex(ValueError, 'hierarchy'): self.check()

    def test_animation_missing_bone(self):
        self.animation['animations']['cycle']['bones'] = {'typo': {}}
        with self.assertRaisesRegex(ValueError, 'Unknown bone'): self.check()

    def test_nonfinite_geometry(self):
        self.geometry['minecraft:geometry'][0]['bones'][0]['pivot'][0] = float('nan')
        with self.assertRaisesRegex(ValueError, 'finite'): self.check()

    def test_path_escape(self):
        self.manifest['texture'] = '../outside.png'
        with self.assertRaisesRegex(ValueError, 'escapes'): self.check()

if __name__ == '__main__': unittest.main()
