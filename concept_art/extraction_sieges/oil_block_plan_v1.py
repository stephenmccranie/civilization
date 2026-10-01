"""Oil extractor block-plan study. Concept only; not a runtime multiblock layout."""
from isometric_plan import Plan

PLAN = Plan("Block-built oil extractor · study 1")

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
            PLAN.box("steel", x, 2, z)

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

# Four tall posts and a square upper collar over the well.
for x in (-3, 0):
    for z in (1, 4):
        for y in range(3, 9):
            PLAN.box("steel", x, y, z)
for x in range(-3, 1):
    for z in range(1, 5):
        if x in (-3, 0) or z in (1, 4):
            PLAN.box("steel", x, 9, z)

# A slim square-section pump rod hangs within the well; no round or custom geometry.
for y in range(3, 8):
    PLAN.box("iron", -2, y, 2, w=.5, d=.5)

# The middle bent supports meet a broad bearing deck beneath the long level beam.
for x in (2, 3):
    for z in (2, 3):
        for y in range(3, 7):
            PLAN.box("steel", x, y, z)
        PLAN.box("steel", x, 7, z)

# One level beam; a line of quarter beams gives its near edge a half-block profile.
for x in range(-2, 8):
    PLAN.box("iron", x, 8, 2)
    PLAN.box("iron", x, 8, 3, h=.5, d=.5)

# The large 4×4 square counterweight window is supported by the right plinth.
for z in range(1, 5):
    for y in range(3, 7):
        if z in (1, 4) or y in (3, 6):
            PLAN.box("iron", 7, y, z)
PLAN.box("copper", 7, 7, 2)  # square vertical bearing between frame and beam

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
