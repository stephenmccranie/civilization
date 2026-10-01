import unittest
from PIL import Image, ImageDraw
from pipeline import prepare, validate


class TexturePipelineTests(unittest.TestCase):
    spec = {'kind': 'item', 'extent': 28, 'colors': 16}

    def source(self):
        image = Image.new('RGBA', (256, 256))
        draw = ImageDraw.Draw(image)
        draw.rectangle((40, 50, 209, 209), fill=(160, 110, 60, 253))
        draw.rectangle((80, 70, 180, 130), fill=(235, 220, 190, 255))
        return image

    def test_normalizes_margin_and_alpha(self):
        image = prepare(self.source(), self.spec)
        data = validate(image, self.spec)
        self.assertEqual(data['size'], [32, 32])
        self.assertEqual(data['partial_alpha_pixels'], 0)
        x0, y0, x1, y1 = data['bbox']
        self.assertEqual(max(x1 - x0, y1 - y0), 28)
        self.assertLessEqual(abs(x0 - (32 - x1)), 1)
        self.assertLessEqual(abs(y0 - (32 - y1)), 1)

    def test_deterministic(self):
        self.assertEqual(prepare(self.source(), self.spec).tobytes(), prepare(self.source(), self.spec).tobytes())

    def test_invisible_rgb_does_not_make_halos(self):
        a = self.source()
        b = a.copy()
        for y in range(b.height):
            for x in range(b.width):
                if b.getpixel((x, y))[3] == 0:
                    b.putpixel((x, y), (255, 0, 255, 0))
        self.assertEqual(prepare(a, self.spec).tobytes(), prepare(b, self.spec).tobytes())

    def test_rejects_background_and_clipped_subject(self):
        with self.assertRaises(ValueError):
            prepare(Image.new('RGBA', (256, 256), 'white'), self.spec)
        clipped = self.source()
        ImageDraw.Draw(clipped).rectangle((0, 50, 50, 200), fill='white')
        with self.assertRaises(ValueError):
            prepare(clipped, self.spec)

    def test_block_faces_fill_canvas(self):
        spec = {'kind': 'block', 'colors': 16}
        result = prepare(Image.new('RGB', (256, 256), 'brown'), spec)
        self.assertEqual(validate(result, spec)['bbox'], (0, 0, 32, 32))
        with self.assertRaises(ValueError):
            prepare(self.source(), spec)

    def test_rejects_corrupt_output(self):
        image = prepare(self.source(), self.spec)
        image.putpixel((4, 4), (100, 100, 100, 128))
        with self.assertRaises(ValueError):
            validate(image, self.spec)

    def test_hot_variant_changes_only_authored_pixels(self):
        source = Image.new('RGB', (256, 256), (40, 40, 40))
        cold_spec = {'kind': 'block', 'colors': 32, 'palette_colors': 24}
        hot_spec = dict(cold_spec, pixels=[[12, 20, [255, 160, 40, 255]]])
        cold, hot = prepare(source, cold_spec), prepare(source, hot_spec)
        changes = [(x, y) for y in range(32) for x in range(32) if cold.getpixel((x, y)) != hot.getpixel((x, y))]
        self.assertEqual(changes, [(12, 20)])
        validate(hot, hot_spec)

    def test_invalid_palette_budget(self):
        with self.assertRaises(ValueError):
            prepare(self.source(), dict(self.spec, palette_colors=17))


if __name__ == '__main__':
    unittest.main()
