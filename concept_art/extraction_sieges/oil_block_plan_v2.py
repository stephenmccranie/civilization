"""Oil extractor block-plan study 2. Concept only; not a runtime multiblock layout."""
from isometric_plan import Plan

PLAN = Plan("Block-built oil extractor · study 2")

# A low stone/brick perimeter frames the whole installation.
for x in range(-4, 10):
    for z in range(6):
        if x in (-4, 9) or z in (0, 5):
            PLAN.box("stone", x, 0, z)
            PLAN.box("brick", x, 1, z)

# The 2×2 well remains open through its whole masonry and steel collar.
for x in range(-3, 1):
    for z in range(1, 5):
        if x in (-3, 0) or z in (1, 4):
            PLAN.box("stone", x, 0, z)
            PLAN.box("brick", x, 1, z)
            # Full corner saddles take the post loads; the other collar segments
            # are half-height, leaving the well visibly open below the tower.
            PLAN.box("steel", x, 2, z, h=1 if x in (-3, 0) and z in (1, 4) else .5)

# Two long low walls and a solid central footing bring the bed to the counterweight end.
for x in range(1, 6):
    for z in (1, 4):
        PLAN.box("stone", x, 0, z)
        PLAN.box("brick", x, 1, z)
        PLAN.box("steel", x, 2, z, h=.5)
for x in (2, 3):
    for z in (2, 3):
        PLAN.box("stone", x, 0, z)
        PLAN.box("brick", x, 1, z)
        PLAN.box("steel", x, 2, z)

# The square right-hand plinth carries the open block-built frame.
for x in range(6, 10):
    for z in range(1, 5):
        if x != 9:  # the outer perimeter already supplies these lower two courses
            PLAN.box("stone", x, 0, z)
            PLAN.box("brick", x, 1, z)
        PLAN.box("steel", x, 2, z)

# Exposed half-slab coping on the front, back and left edge of the bed.
for x in range(-4, 10):
    for z in (0, 5):
        if not (x == 0 and z == 0):  # leave the controller's full-block space clear
            PLAN.box("stone", x, 2, z, h=.5)
for z in range(1, 5):
    PLAN.box("stone", -4, 2, z, h=.5)

# Full feet carry four slender quarter-beam uprights over the well.
for x in (-3, 0):
    for z in (1, 4):
        PLAN.box("steel", x, 3, z)
        outside_x = x if x == -3 else x + .5
        outside_z = z if z == 1 else z + .5
        for y in range(4, 9):
            PLAN.box("steel", outside_x, y, outside_z, w=.5, d=.5)
for x in range(-3, 1):
    for z in range(1, 5):
        if x in (-3, 0) or z in (1, 4):
            # Solid corner bearings and slab-span roof have clear load paths.
            PLAN.box("steel", x, 9, z, h=1 if x in (-3, 0) and z in (1, 4) else .5)

# A slim square-section pump rod hangs within the well; no round or custom geometry.
for y in range(3, 8):
    PLAN.box("iron", -2, y, 2, w=.5, d=.5)

# Solid central feet, four quarter-beam columns and a thin bearing deck.
for x in (2, 3):
    for z in (2, 3):
        PLAN.box("steel", x, 3, z)
        edge_x = x if x == 2 else x + .5
        edge_z = z if z == 2 else z + .5
        for y in range(4, 7):
            PLAN.box("steel", edge_x, y, edge_z, w=.5, d=.5)
        PLAN.box("steel", edge_x, 7, edge_z, w=.5, h=.5, d=.5)
        PLAN.box("steel", x, 7.5, z, h=.5)

# Two narrow cross-ties stiffen the bent without filling its middle.
for x in (2, 3):
    PLAN.box("iron", x, 6, 2.5, h=.5, d=.5)

# The long level beam is genuinely half-height; paired edge ribs widen its
# apparent span without restoring the old full-cube silhouette.
for x in range(-2, 8):
    PLAN.box("iron", x, 8, 2, h=.5)
    PLAN.box("iron", x, 8, 3, h=.5, d=.5)
    PLAN.box("iron", x, 8, 1.5, h=.5, d=.5)

# Bearing keys land only at support junctions, as visible eighth cubes.
for x, z in ((-2, 2), (2.5, 2), (7, 2)):
    PLAN.box("copper", x, 8.5, z, w=.5, h=.5, d=.5)

# The right square window keeps a full lower sill, but its side members are
# quarter-beam posts and the top rail is made of half-height slabs.
for z in range(1, 5):
    PLAN.box("iron", 7, 3, z)
    PLAN.box("iron", 7, 6, z, h=.5)
for z in (1, 4.5):
    for y in (4, 5):
        PLAN.box("iron", 7, y, z, w=.5, d=.5)
# A square, slender connector joins the open frame to the level beam.
PLAN.box("iron", 7, 6.5, 2, w=.5, h=.5, d=1)
PLAN.box("iron", 7, 7, 2, w=.5, d=.5)
PLAN.box("copper", 7.5, 7.5, 2, w=.5, h=.5, d=.5)

# Ground-level cabinet and a short block-section output channel.
PLAN.box("controller", 0, 2, 0)
for x in (10, 11):
    PLAN.box("stone", x, 0, 2)
    PLAN.box("brick", x, 1, 2)
PLAN.box("iron", 10, 2, 2)
PLAN.box("copper", 11, 2, 2)

# Sparse eighth-cube contact markers on the top collar, each resting on steel.
for x, z in ((-3, 1), (0.5, 1), (-3, 4.5), (0.5, 4.5)):
    PLAN.box("copper", x, 10, z, w=.5, h=.5, d=.5)
