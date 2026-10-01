import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from PIL import Image
import family_pipeline as families
from pipeline import pixels


class FamilyTests(unittest.TestCase):
    def test_family_accents_share_palette_and_keep_heat_budget(self):
        master=Image.new('RGB',(128,128),'#445566')
        accents=[[222,211,182],[181,145,87]]
        cold=families.palette_for(master,accents=accents).getpalette()
        hot=families.palette_for(master,heat=True,accents=accents).getpalette()
        self.assertEqual(cold[:72],hot[:72])
        self.assertEqual(cold[66:72],[222,211,182,181,145,87])
        with self.assertRaises(ValueError):families.palette_for(master,accents=[[0,0,0]]*9)

    def test_protected_regions_are_exact(self):
        base = Image.new('RGBA', (32, 32), (70, 35, 20, 255))
        changed = Image.new('RGBA', (32, 32), (255, 150, 40, 255))
        result = families.protected_edit(base, changed, [[9, 15, 23, 26]])
        for y in range(32):
            for x in range(32):
                self.assertEqual(result.getpixel((x, y)), changed.getpixel((x, y)) if 9 <= x < 23 and 15 <= y < 26 else base.getpixel((x, y)))

    def test_bad_regions_fail(self):
        image = Image.new('RGBA', (32, 32))
        with self.assertRaises(ValueError):
            families.protected_edit(image, image, [[-1, 0, 2, 2]])

    def test_reference_change_invalidates_family(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            Image.new('RGB', (64, 64), '#553322').save(root / 'front.png')
            Image.new('RGB', (64, 64), '#774422').save(root / 'side.png')
            family = {'master': 'front', 'faces': {
                'front': {'operation': 'master', 'source': 'front.png'},
                'side': {'operation': 'edit', 'source': 'side.png', 'reference': 'front', 'regions': [[4, 4, 28, 28]]},
                'top': {'operation': 'reuse', 'reference': 'side'}}}
            receipts = {'side': {'reference_sha256': families.digest(root / 'front.png'), 'output_sha256': families.digest(root / 'side.png')}}
            with patch.object(families, 'ROOT', root):
                faces, _ = families.build(family, receipts)
                self.assertEqual(faces['top'].tobytes(), faces['side'].tobytes())
                self.assertTrue(set(pixels(faces['side'])).issubset(set(pixels(faces['front']))))
                Image.new('RGB', (64, 64), '#998877').save(root / 'front.png')
                with self.assertRaisesRegex(ValueError, 'changed reference'):
                    families.build(family, receipts)

    def test_real_kiln_casing_and_masonry_are_protected(self):
        family = json.loads((families.ROOT / 'block_families.json').read_text())['kiln']
        receipts = json.loads((families.ROOT / 'families/kiln/receipts.json').read_text())
        faces, _ = families.build(family, receipts)
        for name in ('side', 'front_on'):
            regions = family['faces'][name]['regions']
            for y in range(32):
                for x in range(32):
                    if not any(a <= x < c and b <= y < d for a, b, c, d in regions):
                        self.assertEqual(faces[name].getpixel((x, y)), faces['front'].getpixel((x, y)))


if __name__ == '__main__':
    unittest.main()
