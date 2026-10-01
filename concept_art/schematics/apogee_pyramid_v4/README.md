# Apogee Pyramid review schematic v4

The exterior geometry is the **approved architectural direction**. Internal circulation, gameplay, recipe and implementation remain unresolved. It develops the selected concept as one continuous fourfold-symmetric masonry pyramid with a hollow interior and one compact electrical crown.

## Dimensions and reading

- Footprint: **31×31 blocks**
- Built height: **31 blocks** including the foundation layer and crown
- Orientation: fourfold symmetric; no designated front
- Exterior: one continuous stepped slope, identical on all four faces
- Interior: hollow; four progression stages and their route remain to be designed
- Summit: open 7×7 dais under a four-support octagonal crown
- Interiors: hollow and intentionally unplanned

## Files

- `apogee_pyramid_v4.nbt`: vanilla Minecraft 1.21.1 structure template
- `isometric_review.png`: procedural overview
- `plan_views.png`: top-down plans for sampled exterior courses and summit
- `elevations.png`: south and east elevations
- `blocks.csv` / `blocks.json`: exact block coordinates
- `material_schedule.json`: dimensions, counts and design assumptions

To inspect the NBT in a test world, copy it to `<world>/generated/civilization/structures/apogee_pyramid_v4.nbt`, then load `civilization:apogee_pyramid_v4` with a structure block. The file contains no entities and omits air, so placement does not intentionally clear unrelated blocks.

## Material schedule

| Block | Count |
| --- | ---: |
| `minecraft:copper_grate` | 24 |
| `minecraft:cut_copper` | 92 |
| `minecraft:oxidized_cut_copper` | 40 |
| `minecraft:polished_deepslate` | 961 |
| `minecraft:quartz_pillar` | 20 |
| `minecraft:sea_lantern` | 1 |
| `minecraft:stone_bricks` | 3,116 |

The palette uses vanilla stand-ins so the shape can be judged independently from future custom blocks. The expensive visual language is concentrated in the four copper spines and the summit crown.
