"""Render simple, offline multiblock plans from a short Python layout file.

Usage: python tools/modeling/isometric_plan.py path/to/layout.py path/to/output
The layout file defines PLAN = Plan('name') and calls PLAN.box(...).
"""

from collections import Counter
from dataclasses import dataclass
from pathlib import Path
import argparse
import math
import runpy

from PIL import Image, ImageDraw, ImageFont


# Diagram colors only. The plan describes geometry, not final texture art.
COLORS = {
    "brick": (171, 96, 72),
    "stone": (143, 149, 145),
    "steel": (95, 113, 124),
    "iron": (70, 77, 84),
    "copper": (177, 115, 72),
    "wood": (161, 124, 76),
    "glass": (126, 171, 178),
    "crude": (105, 66, 36),
    "controller": (207, 168, 88),
}


@dataclass(frozen=True)
class Piece:
    material: str
    x: float
    y: float
    z: float
    w: float
    h: float
    d: float


class Plan:
    def __init__(self, name):
        self.name = name
        self.pieces = []

    def box(self, material, x, y, z, w=1, h=1, d=1):
        """Place a full block, slab, beam or eighth cube; units are Minecraft blocks."""
        self.pieces.append(Piece(material, x, y, z, w, h, d))


def validate(plan):
    if not plan.pieces:
        raise ValueError("Plan has no pieces")
    occupied = set()
    for n, p in enumerate(plan.pieces, 1):
        values = (p.x, p.y, p.z, p.w, p.h, p.d)
        if p.material not in COLORS:
            raise ValueError(f"Piece {n}: unknown material {p.material!r}")
        if any(not math.isfinite(v) or v * 2 != round(v * 2) for v in values):
            raise ValueError(f"Piece {n}: coordinates and sizes must use the half-block grid")
        if (p.w, p.h, p.d).count(1) + (p.w, p.h, p.d).count(.5) != 3:
            raise ValueError(f"Piece {n}: dimensions must each be 1 or 0.5 blocks")
        if math.prod((p.w, p.h, p.d)) not in (1, .5, .25, .125):
            raise ValueError(f"Piece {n}: unsupported cut size")
        if any(math.floor(lo) != math.floor(lo + size - .001) for lo, size in zip((p.x, p.y, p.z), (p.w, p.h, p.d))):
            raise ValueError(f"Piece {n}: a piece may not straddle a full-block boundary")
        for ix in range(round(p.x * 2), round((p.x + p.w) * 2)):
            for iy in range(round(p.y * 2), round((p.y + p.h) * 2)):
                for iz in range(round(p.z * 2), round((p.z + p.d) * 2)):
                    cell = (ix, iy, iz)
                    if cell in occupied:
                        raise ValueError(f"Piece {n}: overlaps another piece at half-cell {cell}")
                    occupied.add(cell)


def shade(rgb, factor):
    return tuple(max(0, min(255, round(c * factor))) for c in rgb)


def project(x, y, z, sx, sz):
    return ((sx * x - sz * z) * .8660254, (sx * x + sz * z) * .5 - y)


def faces(piece, sx, sz):
    x, y, z, w, h, d = piece.x, piece.y, piece.z, piece.w, piece.h, piece.d
    xx = x + w if sx > 0 else x
    zz = z + d if sz > 0 else z
    top = [(x, y+h, z), (x+w, y+h, z), (x+w, y+h, z+d), (x, y+h, z+d)]
    xface = [(xx, y, z), (xx, y, z+d), (xx, y+h, z+d), (xx, y+h, z)]
    zface = [(x, y, zz), (x+w, y, zz), (x+w, y+h, zz), (x, y+h, zz)]
    for points, light in ((top, 1.17), (xface, .84), (zface, .98)):
        depth = sum(sx * a + sz * c + b * 1.4 for a, b, c in points) / 4
        yield depth, points, shade(COLORS[piece.material], light), piece.material


def font(size):
    path = Path("C:/Windows/Fonts/segoeui.ttf")
    return ImageFont.truetype(str(path), size) if path.exists() else ImageFont.load_default()


def draw_isometric(plan, sx, sz, title, size=(900, 700)):
    width, height = size
    image = Image.new("RGB", size, (236, 233, 223))
    draw = ImageDraw.Draw(image, "RGBA")
    draw.text((35, 22), title, font=font(28), fill=(38, 48, 53))
    all_faces = [face for piece in plan.pieces for face in faces(piece, sx, sz)]
    points = [project(*point, sx, sz) for _, polygon, _, _ in all_faces for point in polygon]
    left, right = min(p[0] for p in points), max(p[0] for p in points)
    top, bottom = min(p[1] for p in points), max(p[1] for p in points)
    scale = min(105, (width - 100) / max(1, right - left), (height - 170) / max(1, bottom - top))
    center_x = width / 2 - (left + right) * scale / 2
    center_y = 75 + (height - 170) / 2 - (top + bottom) * scale / 2
    for _, polygon, color, material in sorted(all_faces, key=lambda item: item[0]):
        pixel = [(round(center_x + u * scale), round(center_y + v * scale)) for u, v in (project(*point, sx, sz) for point in polygon)]
        draw.polygon(pixel, fill=(*color, 100) if material == "glass" else (*color, 255))
        draw.line(pixel + [pixel[0]], fill=(48, 53, 54), width=2, joint="curve")
    counts = Counter(p.material for p in plan.pieces)
    draw.text((35, height - 74), f"{len(plan.pieces)} pieces  |  " + "  ·  ".join(f"{key} {value}" for key, value in sorted(counts.items())), font=font(15), fill=(51, 57, 59))
    note = ("Fluid and moving pieces are visual-only overlays" if "visuals only" in plan.name
            else "Complete pieces at their exact half-block-grid positions")
    draw.text((35, height - 43), note, font=font(14), fill=(92, 97, 96))
    return image


def draw_layers(plan):
    min_x = math.floor(min(p.x for p in plan.pieces))
    max_x = math.ceil(max(p.x + p.w for p in plan.pieces))
    min_z = math.floor(min(p.z for p in plan.pieces))
    max_z = math.ceil(max(p.z + p.d for p in plan.pieces))
    min_y = math.floor(min(p.y for p in plan.pieces))
    max_y = math.ceil(max(p.y + p.h for p in plan.pieces))
    cell = 26
    panel_w = max(260, (max_x - min_x) * 2 * cell + 65)
    panel_h = max(240, (max_z - min_z) * 2 * cell + 95)
    slices = [(y, half) for y in range(min_y, max_y) for half in (0, 1)
              if any(p.y <= y + half/2 < p.y + p.h for p in plan.pieces)]
    columns = min(3, len(slices))
    rows = math.ceil(len(slices) / columns)
    image = Image.new("RGB", (columns * panel_w + 30, rows * panel_h + 84), (236, 233, 223))
    draw = ImageDraw.Draw(image)
    layer_kind = "state layers" if "visuals only" in plan.name else "construction layers"
    draw.text((24, 19), f"{plan.name}  /  {layer_kind}", font=font(25), fill=(38, 48, 53))
    draw.text((24, 52), "Each large square is one block; its four cells show the lower or upper half. Empty cells are air.", font=font(13), fill=(81, 90, 88))
    for index, (y, half) in enumerate(slices):
        ox = 15 + (index % columns) * panel_w
        oy = 78 + (index // columns) * panel_h
        draw.rounded_rectangle((ox, oy, ox + panel_w - 9, oy + panel_h - 9), radius=7, fill=(249, 247, 240), outline=(176, 181, 177), width=2)
        draw.text((ox + 15, oy + 12), f"Y {y}  ·  {'lower' if half == 0 else 'upper'} half", font=font(17), fill=(43, 54, 58))
        for bx in range(min_x, max_x):
            px = ox + 42 + (bx - min_x) * 2 * cell
            draw.text((px + 2 * cell / 2 - 5, oy + 46), str(bx), font=font(13), fill=(54, 64, 64))
        for bz in range(min_z, max_z):
            draw.text((ox + 17, oy + 68 + (bz - min_z) * 2 * cell + cell - 8), str(bz), font=font(13), fill=(54, 64, 64))
            for bx in range(min_x, max_x):
                for hz in range(2):
                    for hx in range(2):
                        px = ox + 42 + (bx - min_x) * 2 * cell + hx * cell
                        py = oy + 68 + (bz - min_z) * 2 * cell + hz * cell
                        occupant = next((p for p in plan.pieces if p.x <= bx + hx/2 < p.x + p.w and p.y <= y + half/2 < p.y + p.h and p.z <= bz + hz/2 < p.z + p.d), None)
                        color = COLORS[occupant.material] if occupant else (234, 232, 225)
                        draw.rectangle((px, py, px + cell, py + cell), fill=color, outline=(174, 174, 165), width=1)
                px = ox + 42 + (bx - min_x) * 2 * cell
                py = oy + 68 + (bz - min_z) * 2 * cell
                draw.rectangle((px, py, px + 2*cell, py + 2*cell), outline=(77, 85, 84), width=2)
    return image


def render(plan, directory):
    validate(plan)
    directory = Path(directory)
    directory.mkdir(parents=True, exist_ok=True)
    front = draw_isometric(plan, 1, -1, f"{plan.name}  /  front isometric")
    rear = draw_isometric(plan, -1, 1, f"{plan.name}  /  rear isometric")
    front.save(directory / "front.png")
    rear.save(directory / "rear.png")
    draw_layers(plan).save(directory / "layers.png")
    sheet = Image.new("RGB", (1800, 700))
    sheet.paste(front, (0, 0)); sheet.paste(rear, (900, 0))
    sheet.save(directory / "isometric-sheet.png")
    print(f"Wrote {directory}: isometric-sheet.png, front.png, rear.png, layers.png")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("layout", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    plan = runpy.run_path(str(args.layout)).get("PLAN")
    if not isinstance(plan, Plan) and not (hasattr(plan, "pieces") and hasattr(plan, "name")):
        raise ValueError("Layout script must define PLAN = Plan('name')")
    render(plan, args.output)


if __name__ == "__main__":
    main()
