# Apogee Pyramid review schematic v1

This is a **candidate for review**, not an approved multiblock or recipe. It simplifies the selected concept to three main masonry levels with one ascent and one compact electrical crown.

## Dimensions and reading

- Footprint: **37×37 blocks**
- Built height: **23 blocks** above the foundation layer
- Front: **south (+Z)**
- Levels: 35×35 lower terrace, 23×23 middle terrace, 11×11 upper terrace
- Summit: open 7×7 dais under a four-support octagonal crown
- Interiors: hollow and intentionally unplanned

## Files

- `apogee_pyramid_v1.nbt`: vanilla Minecraft 1.21.1 structure template
- `isometric_review.png`: procedural overview
- `plan_views.png`: top-down plans for the three terraces and summit
- `elevations.png`: south and east elevations
- `blocks.csv` / `blocks.json`: exact block coordinates
- `material_schedule.json`: dimensions, counts and design assumptions

To inspect the NBT in a test world, copy it to `<world>/generated/civilization/structures/apogee_pyramid_v1.nbt`, then load `civilization:apogee_pyramid_v1` with a structure block. The file contains no entities and omits air, so placement does not intentionally clear unrelated blocks.

## Material schedule

| Block | Count |
| --- | ---: |
| `minecraft:copper_grate` | 24 |
| `minecraft:cut_copper` | 117 |
| `minecraft:cut_copper_stairs` | 17 |
| `minecraft:deepslate_tiles` | 240 |
| `minecraft:oxidized_cut_copper` | 40 |
| `minecraft:polished_deepslate` | 1,364 |
| `minecraft:quartz_pillar` | 16 |
| `minecraft:sea_lantern` | 1 |
| `minecraft:smooth_stone` | 1,792 |
| `minecraft:stone_brick_stairs` | 68 |
| `minecraft:stone_bricks` | 744 |

The palette uses vanilla stand-ins so the shape can be judged independently from future custom blocks. The expensive visual language is concentrated in the four copper spines and the summit crown.
