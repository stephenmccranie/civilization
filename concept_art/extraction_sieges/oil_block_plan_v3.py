"""Oil-lift extractor study. PLAN is placed structure; OPERATING_PLAN adds visual-only motion/fluid."""

from isometric_plan import Plan


PLAN = Plan("Oil-lift extractor · placed structure")

# Stone and fired brick make a grounded installation around a 2×2 open well.
for x in range(-4, 10):
    for z in range(6):
        if x in (-4, 9) or z in (0, 5):
            PLAN.box("stone", x, 0, z)
            PLAN.box("brick", x, 1, z)

for x in range(-3, 1):
    for z in range(1, 5):
        if x in (-3, 0) or z in (1, 4):
            PLAN.box("stone", x, 0, z)
            PLAN.box("brick", x, 1, z)
            PLAN.box("steel", x, 2, z, h=1 if x in (-3, 0) and z in (1, 4) else .5)

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

for x in range(6, 10):
    for z in range(1, 5):
        if x != 9:
            PLAN.box("stone", x, 0, z)
            PLAN.box("brick", x, 1, z)
        PLAN.box("steel", x, 2, z)

for x in range(-4, 10):
    for z in (0, 5):
        if not (x == 0 and z == 0):
            PLAN.box("stone", x, 2, z, h=.5)
for z in range(1, 5):
    PLAN.box("stone", -4, 2, z, h=.5)

# The narrow four-post tower guides the moving square plunger; full feet are
# retained only where the posts meet the well collar.
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
            PLAN.box("steel", x, 9, z, h=1 if x in (-3, 0) and z in (1, 4) else .5)
for x, z in ((-2, 3), (-1, 2)):
    for y in range(3, 8):
        PLAN.box("iron", x, y, z, w=.5, d=.5)

# One glass-fronted crude riser climbs beside the well. Its hidden back half
# is empty in the construction plan; the operating view shows crude there.
PLAN.box("steel", -1, 2.5, 1, h=.5)
for y in range(3, 8):
    PLAN.box("glass", -1, y, 1, d=.5)
PLAN.box("glass", -1, 8, 1, h=.5)

# The middle bent supports a FIXED overhead crude header, not a rotating beam.
for x in (2, 3):
    for z in (2, 3):
        PLAN.box("steel", x, 3, z)
        edge_x = x if x == 2 else x + .5
        edge_z = z if z == 2 else z + .5
        for y in range(4, 7):
            PLAN.box("steel", edge_x, y, edge_z, w=.5, d=.5)
        PLAN.box("steel", edge_x, 7, edge_z, w=.5, h=.5, d=.5)
        PLAN.box("steel", x, 7.5, z, h=.5)
for x in (2, 3):
    PLAN.box("iron", x, 6, 2.5, h=.5, d=.5)

# Bottom, side and top pieces leave an honest half-block-height channel. Small
# glass front windows repeat every few blocks; the rest is sturdy iron.
for x in range(-2, 8):
    if x not in (-1, 7):
        PLAN.box("iron", x, 8, 2, h=.5)
    if x != -1:  # the riser elbow fills this half-block instead
        PLAN.box("glass" if x in (-2, 0, 2, 4, 6, 7) else "iron", x, 8, 1.5, d=.5)
    PLAN.box("iron", x, 8, 3, d=.5)
    if x != 0:  # the tower's side cap already closes this segment
        PLAN.box("iron", x, 9, 2, h=.5)

# The old abstract counterweight is now an output sight chamber. The front
# glass occupies half a block in depth, leaving a genuine interior for oil.
for z in range(1, 5):
    PLAN.box("iron", 7, 3, z)
    if z != 2:  # header downcomer enters through the crown
        PLAN.box("iron", 7, 6, z, h=.5)
for z in (1, 4.5):
    for y in (4, 5):
        PLAN.box("iron", 7, y, z, w=.5, d=.5)
for y in (4, 5):
    for z in (2, 3):
        PLAN.box("glass", 7.5, y, z, w=.5)
# A narrow glass downcomer shares an iron structural rib.
for y in (7,):
    PLAN.box("glass", 7.5, y, 2, w=.5)
    PLAN.box("iron", 7, y, 3, w=.5, d=.5)
PLAN.box("glass", 7.5, 6.5, 2, w=.5, h=.5)

# A short lower outlet exits the chamber to the established pipe interface.
for x in (8, 9, 10):
    if x in (9, 10):
        PLAN.box("glass", x, 3, 2, d=.5)
        PLAN.box("iron", x, 3, 2.5, d=.5)
    else:
        PLAN.box("iron", x, 3, 2)
PLAN.box("controller", 0, 2, 0)
for x in (10, 11):
    PLAN.box("stone", x, 0, 2)
    PLAN.box("brick", x, 1, 2)
PLAN.box("iron", 10, 2, 2)
PLAN.box("copper", 11, 2, 2)

# This separate diagram is visual feedback, not a list of blocks to place.
# It illustrates one pumping instant and a half-full existing output buffer.
OPERATING_PLAN = Plan("Oil-lift extractor · pumping view (visuals only)")
from dataclasses import replace

def filled_window(p):
    if p.material != "glass":
        return False
    riser = p.x == -1 and p.z == 1 and 3 <= p.y <= 8
    header = p.y == 8 and p.z == 1.5
    chamber = p.x == 7.5 and p.y == 4 and p.z in (2, 3)
    outlet = p.y == 3 and p.z == 2 and p.x in (9, 10)
    return riser or header or chamber or outlet

OPERATING_PLAN.pieces = [replace(p, material="crude") if filled_window(p) else p
                         for p in PLAN.pieces]
OPERATING_PLAN.box("iron", -1, 6.5, 3, h=.5)  # plunger near its upper stroke
OPERATING_PLAN.box("iron", -1, 7, 3, w=.5, d=.5)
OPERATING_PLAN.box("copper", -.5, 7, 3, w=.5, h=.5, d=.5)
# The front crosshead makes that stroke visible without moving any placed block.
OPERATING_PLAN.box("iron", -2, 6, 1.5, h=.5, d=.5)
OPERATING_PLAN.box("copper", -2, 6.5, 1.5, w=.5, h=.5, d=.5)
for y in (3, 4, 5, 6, 7):
    OPERATING_PLAN.box("crude", -1, y, 1.5, d=.5)
for x in range(-2, 8):
    OPERATING_PLAN.box("crude", x, 8.5, 2, h=.5)
# The two omitted header floor cells make face-connected riser/downcomer turns.
for x in (-1, 7):
    OPERATING_PLAN.box("crude", x, 8, 2, h=.5)
OPERATING_PLAN.box("crude", 7, 7, 2, w=.5)
OPERATING_PLAN.box("crude", 7, 6.5, 2, w=.5, h=.5)
for y in (4,):
    for z in (2, 3):
        OPERATING_PLAN.box("crude", 7, y, z, w=.5)
