"""Export the retired half-grid oil derrick plan as a local multiblock study.

The isometric plan is the geometry source. Run from the project root with the
workspace Python runtime; the output is deterministic and has no art dependency.
"""

from collections import defaultdict
from math import floor
from pathlib import Path
import runpy
import sys


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools/modeling"))
plan = runpy.run_path(str(ROOT / "concept_art/extraction_sieges/oil_derrick_plan.py"))["PLAN"]
runpy.run_path(str(ROOT / "tools/modeling/isometric_plan.py"))["validate"](plan)


def cells(p):
    return {(x, y, z)
            for x in range(round((p.x - floor(p.x)) * 2), round((p.x + p.w - floor(p.x)) * 2))
            for y in range(round((p.y - floor(p.y)) * 2), round((p.y + p.h - floor(p.y)) * 2))
            for z in range(round((p.z - floor(p.z)) * 2), round((p.z + p.d - floor(p.z)) * 2))}


def mask(occupied):
    return sum(1 << (x + 2 * y + 4 * z) for x, y, z in occupied)


def geometry(p):
    occupied = cells(p)
    if len(occupied) == 8:
        return 4, "DOWN", -1
    for side, axis, upper in (("DOWN", 1, False), ("UP", 1, True),
                              ("NORTH", 2, False), ("SOUTH", 2, True),
                              ("WEST", 0, False), ("EAST", 0, True)):
        other = [i for i in range(3) if i != axis]
        low = [0, 0, 0]
        high = [2, 2, 2]
        low[axis], high[axis] = (1, 2) if upper else (0, 1)
        for units in (2, 1, 3):
            for corner in range(4):
                a, b = low.copy(), high.copy()
                if units == 1:
                    i = other[corner // 2]
                    a[i], b[i] = (1, 2) if corner % 2 else (0, 1)
                if units == 3:
                    for bit, i in enumerate(other):
                        a[i], b[i] = (1, 2) if corner & (1 << bit) else (0, 1)
                candidate = {(x, y, z) for x in range(a[0], b[0])
                             for y in range(a[1], b[1]) for z in range(a[2], b[2])}
                if candidate == occupied:
                    return units, side, corner
    raise ValueError(f"Cannot encode {p}")


materials = {"wood": "planks", "controller": "controller", "iron": "iron", "stone": "stone"}
by_block = defaultdict(list)
for p in plan.pieces:
    key = (floor(p.x), floor(p.y), floor(p.z))
    by_block[key].append(p)

lines = [
    "package dev.civilization;",
    "",
    "import java.util.List;",
    "import net.minecraft.core.Direction;",
    "",
    "/** Generated from concept_art/extraction_sieges/oil_derrick_plan.py. */",
    "public final class OilDerrickStructure {",
    "    private OilDerrickStructure() {}",
    "    public static final List<MachineStructure.Part> PARTS = List.of(",
]
entries = []
for (x, y, z), pieces in sorted(by_block.items(), key=lambda item: (item[0][1], item[0][2], item[0][0])):
    union = mask(set().union(*(cells(p) for p in pieces if p is not None)))
    shared = len(pieces) > 1
    for p in pieces:
        if p is not None and p.material == "controller":
            continue
        material = materials.get(p.material, p.material)
        if (x, y, z) == (5, 0, -1):
            material = "output_port"
        units, side, corner = geometry(p)
        if material == "output_port":
            side = "EAST"
        entries.append(f'        new MachineStructure.Part({x-4}, {y}, {z+1}, "{material}", {units}, Direction.{side}, {corner}, 0, {union if shared else 0})')
lines.append(",\n".join(entries))
lines.extend(["    );", "}", ""])
out = ROOT / ".tools/modeling/legacy-derrick/OilDerrickStructure.java"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text("\n".join(lines), encoding="utf-8")
print(f"Exported {len(entries)} parts to {out}")
