# Apogee Pyramid review schematic v3

This is a **candidate for review**, not an approved multiblock or recipe. It develops the selected concept as 4 main masonry levels with one ascent and one compact electrical crown.

## Dimensions and reading

- Footprint: **31×31 blocks**
- Built height: **36 blocks** including the foundation layer and crown
- Front: **south (+Z)**
- Level sizes, low to high: 29×29, 21×21, 13×13, 9×9
- Summit: open 7×7 dais under a four-support octagonal crown
- Interiors: hollow and intentionally unplanned

## Files

- `apogee_pyramid_v3.nbt`: vanilla Minecraft 1.21.1 structure template
- `isometric_review.png`: procedural overview
- `plan_views.png`: top-down plans for the three terraces and summit
- `elevations.png`: south and east elevations
- `blocks.csv` / `blocks.json`: exact block coordinates
- `material_schedule.json`: dimensions, counts and design assumptions

To inspect the NBT in a test world, copy it to `<world>/generated/civilization/structures/apogee_pyramid_v3.nbt`, then load `civilization:apogee_pyramid_v3` with a structure block. The file contains no entities and omits air, so placement does not intentionally clear unrelated blocks.

## Material schedule

| Block | Count |
| --- | ---: |
| `minecraft:copper_grate` | 24 |
| `minecraft:cut_copper` | 142 |
| `minecraft:cut_copper_stairs` | 29 |
| `minecraft:deepslate_tiles` | 240 |
| `minecraft:oxidized_cut_copper` | 40 |
| `minecraft:polished_deepslate` | 956 |
| `minecraft:quartz_pillar` | 20 |
| `minecraft:sea_lantern` | 1 |
| `minecraft:smooth_stone` | 1,414 |
| `minecraft:stone_brick_stairs` | 116 |
| `minecraft:stone_bricks` | 1,248 |

The palette uses vanilla stand-ins so the shape can be judged independently from future custom blocks. The expensive visual language is concentrated in the four copper spines and the summit crown.
