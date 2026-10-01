#!/usr/bin/env python3
"""Generate the review schematic for the compact Apogee Pyramid.

The output is deliberately plain and reproducible: a native Minecraft structure,
machine-readable block list, material schedule, plan/elevation sheets, and a
procedural isometric drawing. It does not use generated concept art.
"""

from __future__ import annotations

import argparse
import csv
import gzip
import hashlib
import json
import math
import struct
from collections import Counter
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[2]
CENTER = 15

parser = argparse.ArgumentParser()
parser.add_argument("--variant", choices=("v2", "v3", "v4"), default="v2")
VARIANT = parser.parse_args().variant
PYRAMID_SHELL = False

if VARIANT == "v2":
    SIZE = (31, 31, 31)
    LEVELS = [(1, 29, 1, 7), (6, 24, 8, 15), (11, 19, 16, 23)]
    STAIR_RUNS = [
        list(zip(range(0, 8), range(30, 22, -1))),
        list(zip(range(8, 16), range(24, 16, -1))),
        list(zip(range(16, 24), range(19, 11, -1))),
    ]
    DAIS_Y, PILLAR_Y0, RING_Y = 24, 25, 30
elif VARIANT == "v3":
    SIZE = (31, 36, 31)
    LEVELS = [(1, 29, 1, 7), (5, 25, 8, 14), (9, 21, 15, 21), (11, 19, 22, 28)]
    STAIR_RUNS = [
        list(zip(range(0, 8), range(30, 22, -1))),
        list(zip(range(8, 15), range(25, 18, -1))),
        list(zip(range(15, 22), range(21, 14, -1))),
        list(zip(range(22, 29), range(19, 12, -1))),
    ]
    DAIS_Y, PILLAR_Y0, RING_Y = 29, 30, 35
else:
    SIZE = (31, 31, 31)
    LEVELS = []
    STAIR_RUNS = []
    DAIS_Y, PILLAR_Y0, RING_Y = 24, 25, 30
    PYRAMID_SHELL = True

OUT = ROOT / "concept_art" / "schematics" / f"apogee_pyramid_{VARIANT}"

COLORS = {
    "minecraft:polished_deepslate": "#293039",
    "minecraft:deepslate_tiles": "#343b45",
    "minecraft:stone_bricks": "#9aa09b",
    "minecraft:smooth_stone": "#b9b9ae",
    "minecraft:stone_brick_stairs": "#969c97",
    "minecraft:cut_copper": "#b96f49",
    "minecraft:cut_copper_stairs": "#b96f49",
    "minecraft:oxidized_cut_copper": "#4d958b",
    "minecraft:quartz_pillar": "#ded9cb",
    "minecraft:copper_grate": "#a96643",
    "minecraft:sea_lantern": "#b7edf0",
}


def state(name: str, **properties: str) -> str:
    if not properties:
        return name
    values = ",".join(f"{key}={value}" for key, value in sorted(properties.items()))
    return f"{name}[{values}]"


blocks: dict[tuple[int, int, int], str] = {}


def put(x: int, y: int, z: int, block: str) -> None:
    blocks[(x, y, z)] = block


def hollow_tier(lo: int, hi: int, y0: int, y1: int) -> None:
    """Four perimeter walls with a complete public terrace on top."""
    for y in range(y0, y1):
        for x in range(lo, hi + 1):
            put(x, y, lo, "minecraft:stone_bricks")
            put(x, y, hi, "minecraft:stone_bricks")
        for z in range(lo + 1, hi):
            put(lo, y, z, "minecraft:stone_bricks")
            put(hi, y, z, "minecraft:stone_bricks")
    for x in range(lo, hi + 1):
        for z in range(lo, hi + 1):
            put(x, y1, z, "minecraft:smooth_stone")


def conductor_run(lo: int, hi: int, y0: int, y1: int, next_lo: int, next_hi: int) -> None:
    # One legible copper spine on each wall.
    for y in range(y0, y1):
        put(CENTER, y, lo, "minecraft:cut_copper")
        put(CENTER, y, hi, "minecraft:cut_copper")
        put(lo, y, CENTER, "minecraft:cut_copper")
        put(hi, y, CENTER, "minecraft:cut_copper")
    # Carry each spine across the exposed terrace to the next tier.
    for z in range(lo, next_lo + 1):
        put(CENTER, y1, z, "minecraft:cut_copper")
    for z in range(next_hi, hi + 1):
        put(CENTER, y1, z, "minecraft:cut_copper")
    for x in range(lo, next_lo + 1):
        put(x, y1, CENTER, "minecraft:cut_copper")
    for x in range(next_hi, hi + 1):
        put(x, y1, CENTER, "minecraft:cut_copper")


# A dark one-block foundation makes the monument sit firmly in the square.
for x in range(SIZE[0]):
    for z in range(SIZE[2]):
        put(x, 0, z, "minecraft:polished_deepslate")

if PYRAMID_SHELL:
    # One continuous fourfold-symmetric pyramid. A two-block shell supports each
    # inward step while leaving the interior free for the later ascent design.
    for y in range(1, 24):
        inset = math.floor(y * 12 / 23)
        lo, hi = inset, SIZE[0] - 1 - inset
        for x in range(lo, hi + 1):
            for z in range(lo, hi + 1):
                if min(x - lo, hi - x, z - lo, hi - z) < 2:
                    put(x, y, z, "minecraft:stone_bricks")
        # Identical copper spines on all four faces preserve rotational symmetry.
        put(CENTER, y, lo, "minecraft:cut_copper")
        put(CENTER, y, hi, "minecraft:cut_copper")
        put(lo, y, CENTER, "minecraft:cut_copper")
        put(hi, y, CENTER, "minecraft:cut_copper")
else:
    # Each architectural level is smaller than the one below.
    for lo, hi, y0, y1 in LEVELS:
        hollow_tier(lo, hi, y0, y1)

    # A restrained dark course at the foot of each level.
    for lo, hi, y, _ in LEVELS:
        for x in range(lo, hi + 1):
            put(x, y, lo, "minecraft:deepslate_tiles")
            put(x, y, hi, "minecraft:deepslate_tiles")
        for z in range(lo + 1, hi):
            put(lo, y, z, "minecraft:deepslate_tiles")
            put(hi, y, z, "minecraft:deepslate_tiles")

    for index, (lo, hi, y0, y1) in enumerate(LEVELS):
        next_lo, next_hi = (LEVELS[index + 1][0], LEVELS[index + 1][1]) if index + 1 < len(LEVELS) else (12, 18)
        conductor_run(lo, hi, y0, y1, next_lo, next_hi)

# One uninterrupted five-wide processional ascent on the south face.
stair = state(
    "minecraft:stone_brick_stairs",
    facing="north",
    half="bottom",
    shape="straight",
    waterlogged="false",
)
copper_stair = state(
    "minecraft:cut_copper_stairs",
    facing="north",
    half="bottom",
    shape="straight",
    waterlogged="false",
)
def stair_run(points: list[tuple[int, int]]) -> None:
    for y, z in points:
        for x in range(CENTER - 2, CENTER + 3):
            # Cut a readable, walkable slot through the taller masonry.
            blocks.pop((x, y + 1, z), None)
            blocks.pop((x, y + 2, z), None)
            put(x, y, z, copper_stair if x == CENTER else stair)


for points in STAIR_RUNS:
    stair_run(points)

# Compact summit: an open transfer dais, four ceramic supports, one octagonal crown.
for x in range(12, 19):
    for z in range(12, 19):
        if x in (12, 18) or z in (12, 18):
            put(x, DAIS_Y, z, "minecraft:oxidized_cut_copper")
        else:
            put(x, DAIS_Y, z, "minecraft:copper_grate")
put(CENTER, DAIS_Y, CENTER, "minecraft:sea_lantern")

# The symmetric pyramid uses diagonal corner supports so an internal ascent can
# emerge through any cardinal side of the summit. Terrace variants retain their
# cardinal supports. Every support sits directly beneath a crown-ring block.
supports = (
    ((13, 13), (13, 17), (17, 13), (17, 17))
    if PYRAMID_SHELL
    else ((15, 12), (15, 18), (12, 15), (18, 15))
)
for x, z in supports:
    for y in range(PILLAR_Y0, RING_Y):
        put(x, y, z, state("minecraft:quartz_pillar", axis="y"))

ring = []
for x in range(12, 19):
    for z in range(12, 19):
        edge = x in (12, 18) or z in (12, 18)
        corner_cut = (x in (12, 18) and z in (12, 13, 17, 18)) or (
            z in (12, 18) and x in (12, 13, 17, 18)
        )
        if edge and not corner_cut:
            ring.append((x, RING_Y, z))
for pos in ring:
    put(*pos, "minecraft:oxidized_cut_copper")
# Diagonal corner blocks make the square ring read as a simple octagon.
for x, z in ((13, 13), (13, 17), (17, 13), (17, 17)):
    put(x, RING_Y, z, "minecraft:oxidized_cut_copper")


def base_name(block: str) -> str:
    return block.split("[", 1)[0]


def shade(hex_color: str, factor: float) -> tuple[int, int, int]:
    value = hex_color.lstrip("#")
    rgb = tuple(int(value[i : i + 2], 16) for i in (0, 2, 4))
    return tuple(max(0, min(255, round(channel * factor))) for channel in rgb)


def font(size: int, bold: bool = False):
    candidates = [
        Path("C:/Windows/Fonts/seguisb.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf"),
        Path("C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size)
    return ImageFont.load_default()


def save_isometric() -> None:
    scale = 9
    ox, oy = 560, 270 if len(LEVELS) == 4 else 230
    image = Image.new("RGB", (1120, 800), "#eee9dc")
    draw = ImageDraw.Draw(image)
    draw.text((45, 34), "APOGEE PYRAMID — REVIEW SCHEMATIC", fill="#182331", font=font(28, True))
    draw.text(
        (47, 72),
        (
            f"{SIZE[0]} × {SIZE[2]} × {SIZE[1]} blocks  |  fourfold symmetric shell"
            if PYRAMID_SHELL
            else f"{SIZE[0]} × {SIZE[2]} × {SIZE[1]} blocks  |  {len(LEVELS)} levels  |  south entrance"
        ),
        fill="#4c5661",
        font=font(16),
    )

    def project(x: float, y: float, z: float) -> tuple[float, float]:
        return (ox + (x - z) * scale, oy + (x + z) * scale * 0.48 - y * scale)

    visible = []
    for (x, y, z), block in blocks.items():
        if (x, y + 1, z) not in blocks or (x + 1, y, z) not in blocks or (x, y, z + 1) not in blocks:
            visible.append((x, y, z, block))
    visible.sort(key=lambda value: (value[0] + value[2], value[1], value[0]))

    for x, y, z, block in visible:
        color = COLORS.get(base_name(block), "#cc00cc")
        p000 = project(x, y, z)
        p100 = project(x + 1, y, z)
        p001 = project(x, y, z + 1)
        p101 = project(x + 1, y, z + 1)
        p010 = project(x, y + 1, z)
        p110 = project(x + 1, y + 1, z)
        p011 = project(x, y + 1, z + 1)
        p111 = project(x + 1, y + 1, z + 1)
        if (x + 1, y, z) not in blocks:
            draw.polygon([p100, p101, p111, p110], fill=shade(color, 0.77), outline="#27313a")
        if (x, y, z + 1) not in blocks:
            draw.polygon([p001, p101, p111, p011], fill=shade(color, 0.62), outline="#27313a")
        if (x, y + 1, z) not in blocks:
            draw.polygon([p010, p110, p111, p011], fill=shade(color, 1.05), outline="#3d454c")

    # Review callouts are concise and describe build logic, not lore.
    notes = (
        [
            "1  One continuous square pyramid",
            "2  Identical slope on all faces",
            "3  Four symmetric conductor spines",
            "4  Interior ascent left unresolved",
            "5  Open 7×7 summit crown",
        ]
        if PYRAMID_SHELL
        else [
            f"1  {len(LEVELS)} tall hollow levels",
            "2  Five-wide recessed stair",
            "3  Four copper conductor spines",
            "4  Open 7×7 summit dais",
            "5  Four supports + one crown ring",
        ]
    )
    y = 610
    draw.rounded_rectangle((45, 590, 430, 752), 8, fill="#f8f4e9", outline="#9c8f77", width=2)
    for line in notes:
        draw.text((67, y), line, fill="#27313a", font=font(17))
        y += 27
    image.save(OUT / "isometric_review.png")


def plan_at(y: int) -> Image.Image:
    cell = 9
    margin = 22
    canvas = Image.new("RGB", (SIZE[0] * cell + margin * 2, SIZE[2] * cell + margin * 2), "#f5f0e3")
    draw = ImageDraw.Draw(canvas)
    for z in range(SIZE[2]):
        for x in range(SIZE[0]):
            block = blocks.get((x, y, z))
            if block:
                color = COLORS.get(base_name(block), "#cc00cc")
                box = (margin + x * cell, margin + z * cell, margin + (x + 1) * cell, margin + (z + 1) * cell)
                draw.rectangle(box, fill=color, outline="#62676a")
    # Cardinal cue makes stair orientation unambiguous.
    draw.text((margin + 3, 3), "N ↑", fill="#202b38", font=font(15, True))
    return canvas


def save_plan_sheet() -> None:
    if PYRAMID_SHELL:
        plans = [(f"COURSE / y={y}", y) for y in (0, 6, 12, 18, 23)]
    else:
        plans = [(f"LEVEL {index + 1} / y={level[3]}", level[3]) for index, level in enumerate(LEVELS)]
    plans.append((f"SUMMIT / y={DAIS_Y}", DAIS_Y))
    columns = 3 if len(plans) > 4 else 2
    rows = math.ceil(len(plans) / columns)
    sheet = Image.new("RGB", (columns * 410 + 60, rows * 405 + 110), "#e9e2d3")
    draw = ImageDraw.Draw(sheet)
    draw.text((34, 24), "APOGEE PYRAMID — PLAN VIEWS", fill="#182331", font=font(28, True))
    for index, (label, y) in enumerate(plans):
        px = 30 + (index % columns) * 410
        py = 82 + (index // columns) * 405
        plan = plan_at(y).resize((350, 350), Image.Resampling.NEAREST)
        sheet.paste(plan, (px, py))
        draw.text((px + 8, py + 354), label, fill="#283440", font=font(18, True))
    sheet.save(OUT / "plan_views.png")


def save_elevation_sheet() -> None:
    cell = 10
    panel_w, panel_h = SIZE[0] * cell + 52, SIZE[1] * cell + 80
    sheet = Image.new("RGB", (panel_w * 2 + 30, panel_h + 80), "#e9e2d3")
    draw = ImageDraw.Draw(sheet)
    draw.text((30, 24), "APOGEE PYRAMID — ELEVATIONS", fill="#182331", font=font(28, True))

    def elevation(axis: str) -> Image.Image:
        panel = Image.new("RGB", (panel_w, panel_h), "#f5f0e3")
        d = ImageDraw.Draw(panel)
        for horizontal in range(SIZE[0]):
            for y in range(SIZE[1]):
                candidates = []
                if axis == "south":
                    candidates = [blocks.get((horizontal, y, z)) for z in range(SIZE[2] - 1, -1, -1)]
                else:
                    candidates = [blocks.get((x, y, horizontal)) for x in range(SIZE[0] - 1, -1, -1)]
                block = next((candidate for candidate in candidates if candidate), None)
                if block:
                    color = COLORS.get(base_name(block), "#cc00cc")
                    x0 = 26 + horizontal * cell
                    y0 = 24 + (SIZE[1] - 1 - y) * cell
                    d.rectangle((x0, y0, x0 + cell, y0 + cell), fill=color, outline="#62676a")
        return panel

    south = elevation("south")
    east = elevation("east")
    sheet.paste(south, (20, 72))
    sheet.paste(east, (panel_w + 30, 72))
    south_label = "SOUTH — MATCHED SLOPE" if PYRAMID_SHELL else "SOUTH — PROCESSIONAL STAIR"
    east_label = "EAST — MATCHED SLOPE" if PYRAMID_SHELL else "EAST — CONDUCTOR SPINE"
    draw.text((40, panel_h + 78), south_label, fill="#283440", font=font(18, True))
    draw.text((panel_w + 50, panel_h + 78), east_label, fill="#283440", font=font(18, True))
    sheet.save(OUT / "elevations.png")


# Minimal NBT writer for the vanilla structure-template format.
TAG_END, TAG_BYTE, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 1, 3, 8, 9, 10


def nbt_string(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def named(tag_type: int, name: str, payload: bytes) -> bytes:
    return bytes([tag_type]) + nbt_string(name) + payload


def compound(entries: list[bytes]) -> bytes:
    return b"".join(entries) + bytes([TAG_END])


def int_list(values: tuple[int, ...]) -> bytes:
    return bytes([TAG_INT]) + struct.pack(">i", len(values)) + b"".join(struct.pack(">i", value) for value in values)


def parse_state(value: str) -> tuple[str, dict[str, str]]:
    if "[" not in value:
        return value, {}
    name, raw = value[:-1].split("[", 1)
    return name, dict(item.split("=", 1) for item in raw.split(","))


def save_structure() -> None:
    palette_values = sorted(set(blocks.values()))
    palette_index = {value: index for index, value in enumerate(palette_values)}
    palette_payload = []
    for value in palette_values:
        name, properties = parse_state(value)
        entries = [named(TAG_STRING, "Name", nbt_string(name))]
        if properties:
            property_entries = [named(TAG_STRING, key, nbt_string(val)) for key, val in sorted(properties.items())]
            entries.append(named(TAG_COMPOUND, "Properties", compound(property_entries)))
        palette_payload.append(compound(entries))
    palette = bytes([TAG_COMPOUND]) + struct.pack(">i", len(palette_payload)) + b"".join(palette_payload)

    block_payload = []
    for (x, y, z), value in sorted(blocks.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
        block_payload.append(
            compound(
                [
                    named(TAG_LIST, "pos", int_list((x, y, z))),
                    named(TAG_INT, "state", struct.pack(">i", palette_index[value])),
                ]
            )
        )
    encoded_blocks = bytes([TAG_COMPOUND]) + struct.pack(">i", len(block_payload)) + b"".join(block_payload)
    root = compound(
        [
            named(TAG_INT, "DataVersion", struct.pack(">i", 3955)),
            named(TAG_LIST, "size", int_list(SIZE)),
            named(TAG_LIST, "palette", palette),
            named(TAG_LIST, "blocks", encoded_blocks),
            named(TAG_LIST, "entities", bytes([TAG_COMPOUND]) + struct.pack(">i", 0)),
        ]
    )
    binary = bytes([TAG_COMPOUND]) + nbt_string("") + root
    with (OUT / f"apogee_pyramid_{VARIANT}.nbt").open("wb") as raw:
        with gzip.GzipFile(fileobj=raw, mode="wb", mtime=0) as handle:
            handle.write(binary)


def save_data() -> None:
    records = [
        {"x": x, "y": y, "z": z, "block": value}
        for (x, y, z), value in sorted(blocks.items(), key=lambda item: (item[0][1], item[0][2], item[0][0]))
    ]
    (OUT / "blocks.json").write_text(json.dumps({"size": SIZE, "blocks": records}, indent=2) + "\n", encoding="utf-8")
    with (OUT / "blocks.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=("x", "y", "z", "block"))
        writer.writeheader()
        writer.writerows(records)
    counts = Counter(base_name(value) for value in blocks.values())
    design_notes = (
        [
            "The exterior is one continuous fourfold-symmetric stepped pyramid rather than stacked terraces.",
            "The two-block masonry shell leaves a hollow interior; four internal progression stages and their ascent remain unresolved.",
            "The crown is an open 7x7 dais with four diagonal corner supports, leaving every cardinal stair approach clear.",
            "Palette and exact multiblock validation are candidates for review, not approved gameplay rules.",
        ]
        if PYRAMID_SHELL
        else [
            f"Exactly {len(LEVELS)} main masonry levels; summit machinery is not another architectural level.",
            f"All {len(LEVELS)} level interiors are hollow and intentionally unresolved for later functional rooms.",
            "The crown is an open 7x7 dais with four ceramic supports and one compact octagonal ring.",
            "Palette and exact multiblock validation are candidates for review, not approved gameplay rules.",
        ]
    )
    schedule = {
        "dimensions": {"width": SIZE[0], "height": SIZE[1], "depth": SIZE[2]},
        "orientation": (
            "fourfold symmetric; no exterior entrance selected"
            if PYRAMID_SHELL
            else "entrance and stair face south (+Z)"
        ),
        "total_blocks": sum(counts.values()),
        "materials": dict(sorted(counts.items())),
        "design_notes": design_notes,
    }
    (OUT / "material_schedule.json").write_text(json.dumps(schedule, indent=2) + "\n", encoding="utf-8")


def save_readme() -> None:
    counts = Counter(base_name(value) for value in blocks.values())
    rows = "\n".join(f"| `{name}` | {count:,} |" for name, count in sorted(counts.items()))
    level_sizes = ", ".join(f"{hi - lo + 1}×{hi - lo + 1}" for lo, hi, _, _ in LEVELS)
    concept_line = (
        "one continuous fourfold-symmetric masonry pyramid with a hollow interior and one compact electrical crown"
        if PYRAMID_SHELL
        else f"{len(LEVELS)} main masonry levels with one ascent and one compact electrical crown"
    )
    level_line = (
        "- Exterior: one continuous stepped slope, identical on all four faces\n- Interior: hollow; four progression stages and their route remain to be designed"
        if PYRAMID_SHELL
        else f"- Level sizes, low to high: {level_sizes}"
    )
    status_line = (
        "The exterior geometry is the **approved architectural direction**. Internal circulation, gameplay, recipe and implementation remain unresolved."
        if VARIANT == "v4"
        else "This is a **candidate for review**, not an approved multiblock or recipe."
    )
    orientation_line = "- Orientation: fourfold symmetric; no designated front" if PYRAMID_SHELL else "- Front: **south (+Z)**"
    plans_description = "sampled exterior courses and summit" if PYRAMID_SHELL else "the architectural levels and summit"
    text = f"""# Apogee Pyramid review schematic {VARIANT}

{status_line} It develops the selected concept as {concept_line}.

## Dimensions and reading

- Footprint: **{SIZE[0]}×{SIZE[2]} blocks**
- Built height: **{SIZE[1]} blocks** including the foundation layer and crown
{orientation_line}
{level_line}
- Summit: open 7×7 dais under a four-support octagonal crown
- Interiors: hollow and intentionally unplanned

## Files

- `apogee_pyramid_{VARIANT}.nbt`: vanilla Minecraft 1.21.1 structure template
- `isometric_review.png`: procedural overview
- `plan_views.png`: top-down plans for {plans_description}
- `elevations.png`: south and east elevations
- `blocks.csv` / `blocks.json`: exact block coordinates
- `material_schedule.json`: dimensions, counts and design assumptions

To inspect the NBT in a test world, copy it to `<world>/generated/civilization/structures/apogee_pyramid_{VARIANT}.nbt`, then load `civilization:apogee_pyramid_{VARIANT}` with a structure block. The file contains no entities and omits air, so placement does not intentionally clear unrelated blocks.

## Material schedule

| Block | Count |
| --- | ---: |
{rows}

The palette uses vanilla stand-ins so the shape can be judged independently from future custom blocks. The expensive visual language is concentrated in the four copper spines and the summit crown.
"""
    (OUT / "README.md").write_text(text, encoding="utf-8")


def save_manifest() -> None:
    files = {}
    for path in sorted(OUT.iterdir()):
        if path.name == "manifest.json" or not path.is_file():
            continue
        files[path.name] = hashlib.sha256(path.read_bytes()).hexdigest()
    manifest = {
        "schema": 1,
        "asset": f"apogee_pyramid_review_schematic_{VARIANT}",
        "generator": str(Path(__file__).relative_to(ROOT)).replace("\\", "/"),
        "image_generation_used": False,
        "status": "candidate_for_user_review",
        "files_sha256": files,
    }
    (OUT / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    save_data()
    save_structure()
    save_isometric()
    save_plan_sheet()
    save_elevation_sheet()
    save_readme()
    save_manifest()
    print(f"Generated {len(blocks):,} blocks in {OUT}")


if __name__ == "__main__":
    main()
