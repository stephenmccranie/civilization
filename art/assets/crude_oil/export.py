"""Export a seamless 64 px crude-oil atlas with slow, looping surface motion."""
from pathlib import Path
import math
from PIL import Image

ROOT = Path(__file__).resolve().parent
TARGET = ROOT.parents[2] / "civilization-mod/src/main/resources/assets/civilization/textures/block/crude_oil.png"
SIZE = 64
FRAMES = 8


def periodic(master):
    source = master.convert("RGB").resize((SIZE, SIZE), Image.Resampling.LANCZOS)
    pixels = source.load()
    # Bring opposite edges together without hiding the broad marks in the center.
    for axis in (0, 1):
        original = source.copy().load()
        for y in range(SIZE):
            for x in range(SIZE):
                coordinate = x if axis == 0 else y
                distance = min(coordinate, SIZE - 1 - coordinate)
                if distance >= 8:
                    continue
                other_x, other_y = (SIZE - 1 - x, y) if axis == 0 else (x, SIZE - 1 - y)
                a, b = original[x, y], original[other_x, other_y]
                weight = .5 * (1 - distance / 8) ** 2
                pixels[x, y] = tuple(round(a[c] * (1 - weight) + b[c] * weight) for c in range(3))
    return source


def sample(pixels, x, y):
    x %= SIZE
    y %= SIZE
    x0, y0 = math.floor(x), math.floor(y)
    fx, fy = x - x0, y - y0
    colors = (pixels[x0 % SIZE, y0 % SIZE], pixels[(x0 + 1) % SIZE, y0 % SIZE],
              pixels[x0 % SIZE, (y0 + 1) % SIZE], pixels[(x0 + 1) % SIZE, (y0 + 1) % SIZE])
    return tuple(round(colors[0][c] * (1 - fx) * (1 - fy)
                       + colors[1][c] * fx * (1 - fy)
                       + colors[2][c] * (1 - fx) * fy
                       + colors[3][c] * fx * fy) for c in range(3))


def main():
    base = periodic(Image.open(ROOT / "master.png"))
    base.save(ROOT / "native-review.png")
    pixels = base.load()
    atlas = Image.new("RGB", (SIZE, SIZE * FRAMES))
    for frame in range(FRAMES):
        phase = frame * 2 * math.pi / FRAMES
        tile = Image.new("RGB", (SIZE, SIZE))
        out = tile.load()
        for y in range(SIZE):
            for x in range(SIZE):
                # Periodic, subpixel eddies return to their starting pose without a jump.
                sx = x + 2.4 * math.sin(2 * math.pi * y / SIZE + phase)
                sy = y + 2.4 * math.cos(2 * math.pi * x / SIZE + phase)
                out[x, y] = sample(pixels, sx, sy)
        atlas.paste(tile, (0, frame * SIZE))
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(ROOT / "atlas.png")
    atlas.save(TARGET)
    (TARGET.parent / "crude_oil.png.mcmeta").write_text(
        '{"animation":{"frametime":8,"interpolate":true}}\n', encoding="utf-8")


if __name__ == "__main__":
    main()
