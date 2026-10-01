"""Preview the uranium track motion; no Minecraft assets are exported yet.

Each track shoots out in two frames. Its stationary condensation-like trail
then fades over several frames. All three sources use this one generator with
different seeds, so their bursts never pulse in lockstep.
"""

from dataclasses import dataclass
from math import atan2, cos, hypot, sin
from pathlib import Path
from random import Random

from PIL import Image, ImageDraw


OUT = Path(__file__).parent
ROOT = OUT.parents[2]
FRAMES = 144
FRAME_MS = 85
SIZE = 96
SCALE = 3
SEEDS = (12, 159, 841)
KINDS = ("ore", "carried", "chest")


@dataclass(frozen=True)
class Track:
    start: int
    life: int
    points: tuple[tuple[float, float], ...]
    fork_at: int
    fork_tip: tuple[float, float]
    strength: float


def source_icon(kind: str) -> Image.Image:
    icon = Image.new("RGBA", (SIZE, SIZE))
    if kind in ("ore", "carried"):
        filename = "uranium_ore.png" if kind == "ore" else "raw_uranium.png"
        original = Image.open(ROOT / "art/assets/frontier_uranium" / filename).convert("RGBA")
        width = 40 if kind == "ore" else 38
        source = original.resize((width, width), Image.Resampling.NEAREST)
        icon.alpha_composite(source, ((SIZE - width) // 2, (SIZE - width) // 2))
    else:
        draw = ImageDraw.Draw(icon)
        draw.rectangle((25, 32, 70, 64), fill="#76502f", outline="#38281d", width=2)
        draw.rectangle((25, 32, 70, 41), fill="#89623c", outline="#38281d", width=2)
        draw.line((26, 43, 69, 43), fill="#312820", width=2)
        draw.rectangle((44, 39, 51, 48), fill="#aaa59b", outline="#555657", width=1)
    return icon


def edge_points(icon: Image.Image) -> list[tuple[float, float, float]]:
    alpha = icon.getchannel("A")
    points = []
    for y in range(1, SIZE - 1):
        for x in range(1, SIZE - 1):
            if alpha.getpixel((x, y)) < 128:
                continue
            dx = int(alpha.getpixel((x + 1, y)) < 128) - int(alpha.getpixel((x - 1, y)) < 128)
            dy = int(alpha.getpixel((x, y + 1)) < 128) - int(alpha.getpixel((x, y - 1)) < 128)
            if not (dx or dy):
                continue
            # Discard holes in item sprites; launch from the exterior contour.
            if (x - SIZE / 2) * dx + (y - SIZE / 2) * dy <= 0:
                continue
            magnitude = hypot(dx, dy)
            points.append((x + 1.2 * dx / magnitude,
                           y + 1.2 * dy / magnitude, atan2(dy, dx)))
    if not points:
        raise ValueError("source icon has no exterior edge")
    return points


def tracks(seed: int, icon: Image.Image) -> list[Track]:
    rng = Random(seed)
    origins = edge_points(icon)
    result = []
    # About 1.3 launches per second, with random gaps and occasional pairs.
    for start in rng.sample(range(FRAMES), 16):
        life = rng.randint(8, 15)
        origin_x, origin_y, normal = rng.choice(origins)
        angle = normal + rng.uniform(-0.4, 0.4)
        length = rng.uniform(13, 27)
        bend = rng.uniform(-0.18, 0.18)
        segments = rng.randint(6, 9)
        points = []
        for step in range(segments + 1):
            fraction = step / segments
            ray_angle = angle + bend * fraction
            offset = rng.uniform(-0.75, 0.75) if step not in (0, segments) else 0
            points.append((
                origin_x + (length * fraction + offset) * cos(ray_angle),
                origin_y + (length * fraction + offset) * sin(ray_angle),
            ))
        fork_at = rng.randint(2, segments - 2) if rng.random() < 0.28 else -1
        branch = angle + bend * fork_at / segments + rng.choice((-1, 1)) * rng.uniform(0.34, 0.8)
        fork_tip = (
            points[fork_at][0] + rng.uniform(4, 11) * cos(branch),
            points[fork_at][1] + rng.uniform(4, 11) * sin(branch),
        ) if fork_at >= 0 else (0, 0)
        result.append(Track(start, life, tuple(points), fork_at, fork_tip, rng.uniform(0.55, 1)))
    return result


def draw_track(layer: Image.Image, track: Track, age: int) -> None:
    # Head travels most of the path immediately, then finishes in frame two.
    progress = 0.64 if age == 0 else 1.0
    last = max(1, round((len(track.points) - 1) * progress))
    points = [(round(x * SCALE), round(y * SCALE)) for x, y in track.points[:last + 1]]
    linger = 1 if age < 2 else max(0, (track.life - age) / (track.life - 2))
    opacity = round(215 * track.strength * linger)
    if opacity < 5:
        return
    draw = ImageDraw.Draw(layer, "RGBA")
    draw.line(points, fill=(68, 69, 72, round(opacity * 0.42)), width=5)
    draw.line(points, fill=(215, 216, 214, opacity), width=2)
    if track.fork_at >= 0 and last >= track.fork_at and age >= 1:
        base = tuple(round(v * SCALE) for v in track.points[track.fork_at])
        tip = tuple(round(v * SCALE) for v in track.fork_tip)
        draw.line((base, tip), fill=(192, 194, 193, round(opacity * 0.68)), width=2)
    if age < 2:
        x, y = points[-1]
        radius = 3 if age == 0 else 2
        draw.ellipse((x - radius, y - radius, x + radius, y + radius),
                     fill=(244, 245, 242, round(215 * track.strength)))


def render(track_set: list[Track], index: int) -> Image.Image:
    layer = Image.new("RGBA", (SIZE * SCALE, SIZE * SCALE))
    for track in track_set:
        age = (index - track.start) % FRAMES
        if age < track.life:
            draw_track(layer, track, age)
    return layer.resize((SIZE, SIZE), Image.Resampling.LANCZOS)


def review_sheet(variants: list[list[Image.Image]], track_sets: list[list[Track]],
                 icons: list[Image.Image]) -> None:
    # Consecutive frames make shooting heads and lingering trails inspectable.
    cell = 160
    background = Image.new("RGB", (9 * cell, 3 * cell), "#24282a")
    draw = ImageDraw.Draw(background)
    for row, frames in enumerate(variants):
        track_set = track_sets[row]
        window_start = max(range(FRAMES), key=lambda start: sum(
            (track.start - start) % FRAMES < 9 for track in track_set))
        for column in range(9):
            tick = (window_start + column) % FRAMES
            sprite = frames[tick]
            panel = Image.new("RGBA", (136, 136), (48, 53, 55, 255))
            panel.alpha_composite(icons[row].resize((128, 128), Image.Resampling.NEAREST), (4, 4))
            large = sprite.resize((128, 128), Image.Resampling.NEAREST)
            panel.alpha_composite(large, (4, 4))
            background.paste(panel.convert("RGB"), (column * cell + 12, row * cell + 8))
            draw.text((column * cell + 14, row * cell + 145),
                      f"source {row + 1} / frame {tick}", fill="#c9c9c4")
    background.save(OUT / "frame-contact.png")


def preview(variants: list[list[Image.Image]], icons: list[Image.Image]) -> None:
    # Clean panels isolate this animation from static tracks in the concept art.
    palette = ((51, 55, 57), (84, 79, 72), (99, 71, 49))
    backdrop = Image.new("RGBA", (840, 282), (32, 34, 35, 255))
    painter = ImageDraw.Draw(backdrop)
    for index, color in enumerate(palette):
        left = index * 280
        painter.rectangle((left + 8, 8, left + 272, 274), fill=(*color, 255))
        painter.text((left + 20, 20), ("ORE", "CARRIED", "CHEST")[index], fill="#d4d1c9")
    frames = []
    for tick in range(FRAMES):
        image = backdrop.copy()
        for column, (variant, phase) in enumerate(zip(variants, (0, 17, 29))):
            icon = icons[column].resize((224, 224), Image.Resampling.NEAREST)
            image.alpha_composite(icon, (column * 280 + 28, 34))
            sprite = variant[(tick + phase) % FRAMES].resize((224, 224), Image.Resampling.NEAREST)
            image.alpha_composite(sprite, (column * 280 + 28, 34))
        frames.append(image.convert("RGB"))
    frames[0].save(OUT / "animation-preview.gif", save_all=True,
                   append_images=frames[1:], duration=FRAME_MS, loop=0, optimize=False)


def main() -> None:
    icons = [source_icon(kind) for kind in KINDS]
    track_sets = [tracks(seed, icon) for seed, icon in zip(SEEDS, icons)]
    variants = [[render(track_set, index) for index in range(FRAMES)] for track_set in track_sets]
    review_sheet(variants, track_sets, icons)
    preview(variants, icons)
    print(f"Previewed {FRAMES} frames across {len(SEEDS)} independent sources in {OUT}")


if __name__ == "__main__":
    main()
