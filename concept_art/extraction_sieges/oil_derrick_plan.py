"""Wooden oil derrick construction geometry, exported into the mod."""
from isometric_plan import Plan

PLAN = Plan("Wooden oil derrick")


def cube(x, y, z, material="wood"):
    PLAN.box(material, x, y, z, w=.5, h=.5, d=.5)


def rail_x(start, end, y, z):
    x = start
    while x < end:
        width = 1 if x == int(x) and x + 1 <= end else .5
        PLAN.box("wood", x, y, z, w=width, h=.5, d=.5)
        x += width


def rail_z(x, y, start, end):
    z = start
    while z < end:
        depth = 1 if z == int(z) and z + 1 <= end else .5
        PLAN.box("wood", x, y, z, w=.5, h=.5, d=depth)
        z += depth


def perimeter_rails(lo, hi, y):
    for z in (lo, hi):
        rail_x(lo + .5, hi, y, z)
    for x in (lo, hi):
        rail_z(x, y, lo + .5, hi)


def deck(start, stop, hole_start, hole_stop, y):
    """One-block-wide wood slab walkway around an open tower center."""
    for x in range(start, stop):
        for z in range(start, stop):
            if hole_start <= x < hole_stop and hole_start <= z < hole_stop:
                continue
            PLAN.box("wood", x, y, z, h=.5)


def outside_railing(start, stop, y):
    """Posts and handrail outside the walking ring, not in its one-block path."""
    lo, hi = start - .5, stop
    mid = (start + stop) / 2 - .5
    for x, z in ((lo, lo), (lo, hi), (hi, lo), (hi, hi),
                 (mid, lo), (mid, hi), (lo, mid), (hi, mid)):
        for post_y in (y + .5, y + 1, y + 1.5):
            cube(x, post_y, z)
    perimeter_rails(lo, hi, y + 2)


# Four grounded wooden legs surround an open central well. The controller and
# future crude outlet sit outside the tower where a player can reach them.
for x in (0, 7):
    for z in (0, 7):
        PLAN.box("stone", x, -1, z)
for x in (3, 4, 5):
    PLAN.box("stone", x, -1, -1)
PLAN.box("controller", 4, 0, -1)
PLAN.box("iron", 5, 0, -1)  # reserved output-port position, not a new pipe mechanic

# Foot ties use beams instead of a filled plinth; the well remains visible.
perimeter_rails(0, 7.5, .5)

# Four stepped, tapered corner posts. The repeated middle width adds one tall
# structural bay without making the crown narrower. Every actual inward step
# uses eighth-cube connectors, never an impossible diagonal block.
edges = [(0, 7.5), (.5, 7), (1, 6.5), (1, 6.5), (1.5, 6), (2, 5.5)]
for stage, (lo, hi) in enumerate(edges):
    tapered = stage > 0 and (lo, hi) != edges[stage - 1]
    for x in (lo, hi):
        for z in (lo, hi):
            if tapered:
                previous_lo, previous_hi = edges[stage - 1]
                old_x = previous_lo if x == lo else previous_hi
                old_z = previous_lo if z == lo else previous_hi
                cube(old_x, stage * 5, old_z)
                cube(x, stage * 5, old_z)
                cube(x, stage * 5, z)
                cube(x, stage * 5 + .5, z)
            for y in range(stage * 5 + (1 if tapered else 0), stage * 5 + 5):
                PLAN.box("wood", x, y, z, w=.5, d=.5)
    # Two ties per five-block bay keep the tall frame visually braced. The
    # gallery decks remain outside the legs and retain their open centers.
    for offset in (1.5, 3.5):
        perimeter_rails(lo, hi, stage * 5 + offset)

# One working gallery at half-height and one smaller crown deck. Each has a
# one-block walkway just outside the local legs, carried by short outriggers.
for x in (1, 6.5):
    for z in (1, 6.5):
        rail_x(0 if x == 1 else 7, 1 if x == 1 else 8, 17, z)
        rail_z(x, 17, 0 if z == 1 else 7, 1 if z == 1 else 8)
deck(0, 8, 1, 7, 17.5)
outside_railing(0, 8, 17.5)
# The crown gallery is smaller than the middle gallery, but still projects a
# full block beyond the narrow upper posts. Short cantilevers carry each side.
for x in (2, 5.5):
    for z in (2, 5.5):
        PLAN.box("wood", 1 if x == 2 else 6, 29.5, z, w=1, h=.5, d=.5)
        PLAN.box("wood", x, 29.5, 1 if z == 2 else 6, w=.5, h=.5, d=1)
deck(1, 7, 2, 6, 30)
outside_railing(1, 7, 30)

# A narrow central iron string identifies the purpose. Its motion, hoist and
# actual oil transfer are future visual/gameplay work, not block requirements.
for y in range(0, 30):
    PLAN.box("iron", 4, y, 4, w=.5, d=.5)
