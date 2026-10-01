"""Build only transparent wear marks, aligned to the installed Faithful 32x silhouettes.

Faithful pixels are read as a placement mask and are never copied into the output.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import random
from pathlib import Path
from zipfile import ZipFile

from PIL import Image


ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / "civilization-mod/src/main/resources/assets/civilization"
TIERS = ("wooden", "stone", "iron", "golden", "diamond", "netherite")
SHAPES = ("sword", "pickaxe", "axe", "shovel", "hoe")
ARMORS = ("leather", "chainmail", "iron", "golden", "diamond", "netherite")
PIECES = ("helmet", "chestplate", "leggings", "boots")
ITEMS = [f"{tier}_{shape}" for tier in TIERS for shape in SHAPES]
ITEMS += [f"{material}_{piece}" for material in ARMORS for piece in PIECES]
ITEMS += ["turtle_helmet", "shears", "flint_and_steel", "bow", "crossbow", "trident", "mace", "elytra", "fishing_rod", "brush", "carrot_on_a_stick", "warped_fungus_on_a_stick"]
CUSTOM = ("stone_saw", "iron_saw", "diamond_saw")


def palette(name: str) -> tuple[tuple[int, int, int], tuple[int, int, int]]:
    if name.startswith(("diamond_", "turtle_")):
        return (49, 99, 103), (118, 189, 186)
    if name.startswith("netherite_"):
        return (42, 42, 51), (113, 99, 100)
    if name.startswith("golden_"):
        return (118, 80, 31), (189, 143, 62)
    if name.startswith(("wooden_", "leather_", "fishing_", "carrot_", "warped_")):
        return (93, 65, 43), (151, 111, 68)
    if name.startswith("stone_"):
        return (67, 71, 70), (135, 137, 127)
    return (111, 60, 34), (154, 94, 55)


def source_image(archive: ZipFile, name: str) -> Image.Image | None:
    if name in CUSTOM:
        path = ASSETS / "textures/item" / f"{name}.png"
        return Image.open(path).convert("RGBA")
    sprite = "crossbow_standby" if name == "crossbow" else name
    path = f"assets/minecraft/textures/item/{sprite}.png"
    try:
        return Image.open(io.BytesIO(archive.read(path))).convert("RGBA")
    except KeyError:
        return None


def wear_variant(name: str, source: Image.Image, variant: int) -> Image.Image:
    """Two connected nicks/scuffs per layer; every painted pixel belongs to the base sprite."""
    opaque = {(x, y) for y in range(32) for x in range(32) if source.getpixel((x, y))[3] > 200}
    rng = random.Random(int.from_bytes(hashlib.sha256(f"{name}:{variant}".encode()).digest()[:8], "big"))
    image = Image.new("RGBA", (32, 32))
    dark, light = palette(name)
    brightness = {point: sum(source.getpixel(point)[:3]) / 3 for point in opaque}
    threshold = sorted(brightness.values())[int((len(brightness) - 1) * 0.55)]
    # Wear on the material's lit face reads clearly; dark handles and outlines
    # otherwise swallow most randomly placed marks on thin tools.
    anchors = [point for point in opaque if brightness[point] >= threshold]
    if len(anchors) < 8:
        anchors = list(opaque)
    rng.shuffle(anchors)
    used: set[tuple[int, int]] = set()
    for motif in range(2):
        for x, y in anchors:
            if any(max(abs(x - px), abs(y - py)) < 5 for px, py in used):
                continue
            # A short directional scratch or an L-shaped patch reads as wear at 32 px.
            direction = rng.choice(((1, 0), (0, 1), (1, 1), (-1, 1)))
            dx, dy = direction
            shape = [(0, 0), (dx, dy), (2 * dx, 2 * dy)]
            if motif == 1:
                shape += [(dx - dy, dy + dx), (2 * dx - dy, 2 * dy + dx)]
            elif rng.random() < 0.5:
                shape += [(dx - dy, dy + dx)]
            pixels = [(x + ox, y + oy) for ox, oy in shape if (x + ox, y + oy) in opaque]
            if len(pixels) < 2:
                continue
            color = dark if motif == 1 else light
            for j, point in enumerate(pixels):
                image.putpixel(point, (*color, 240 if j < 3 else 205))
            used.update(pixels)
            break
    if not used and opaque:
        image.putpixel(next(iter(opaque)), (*dark, 230))
    return image


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", type=Path, required=True, help="Installed Faithful 32x ZIP; never copied into the mod")
    args = parser.parse_args()
    texture_dir = ASSETS / "textures/item/grade_wear"
    model_dir = ASSETS / "models/item/grade_wear"
    texture_dir.mkdir(parents=True, exist_ok=True)
    model_dir.mkdir(parents=True, exist_ok=True)
    built = []
    with ZipFile(args.pack) as archive:
        for name in ITEMS + list(CUSTOM):
            source = source_image(archive, name)
            if source is None or source.size != (32, 32):
                continue
            namespace = "civilization" if name in CUSTOM else "minecraft"
            key = f"{namespace}_{name}"
            for old_band in range(1, 5):
                for folder, suffix in ((texture_dir, ".png"), (model_dir, ".json")):
                    (folder / f"{key}_{old_band}{suffix}").unlink(missing_ok=True)
            variants = set()
            for variant in range(16):
                image = wear_variant(key, source, variant)
                painted = {(x, y) for y in range(32) for x in range(32) if image.getpixel((x, y))[3]}
                assert painted and all(source.getpixel(point)[3] > 200 for point in painted)
                signature = image.tobytes()
                assert signature not in variants, f"Duplicate wear variant: {key} {variant}"
                variants.add(signature)
                stem = f"{key}_v{variant:02d}"
                image.save(texture_dir / f"{stem}.png")
                (model_dir / f"{stem}.json").write_text(json.dumps({
                    "parent": "minecraft:item/generated",
                    "textures": {"layer0": f"civilization:item/grade_wear/{stem}"},
                }, indent=2) + "\n", encoding="utf-8")
            built.append(f"{namespace}:{name}")
    (Path(__file__).parent / "items.json").write_text(json.dumps(built, indent=2) + "\n", encoding="utf-8")
    print(f"Created sixteen composable wear variants for {len(built)} equipment items; no Faithful source pixels bundled.")


if __name__ == "__main__":
    main()
