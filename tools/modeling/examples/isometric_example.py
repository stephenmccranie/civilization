"""Small geometry proof, not an accepted oil or coal extractor design."""
from isometric_plan import Plan

PLAN = Plan("Construction geometry example")

for x in range(-2, 3):
    for z in range(3):
        if x in (-2, 2) or z in (0, 2):
            PLAN.box("brick", x, 0, z)

for x in (-2, 2):
    for z in (0, 2):
        for y in range(1, 4):
            PLAN.box("steel", x, y, z)
        PLAN.box("steel", x, 4, z, h=.5)  # cap slab

for x in range(-1, 2):
    PLAN.box("iron", x, 3.5, 0, h=.5, d=.5)  # horizontal quarter beam

PLAN.box("controller", 0, 1, 0)
PLAN.box("copper", 0, 1, 1, w=.5, h=.5, d=.5)  # eighth cube
