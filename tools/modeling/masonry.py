"""Masonry for machine recipes: solid mortar masses faced with whole painted bricks and stones.

A recipe's texture catalogue names its painted pieces `<kind>_<tone>` (kinds `brick`, `half`, `rowlock` and `stone`),
each an object panel on mortar. Faces are laid in courses two units high on one grid, in running bond from each face's
left corner, so bonds line up round corners; a brick is cut to fit where a course meets an opening or an edge, and every
top is capped with bricks laid flat. Pieces are surface detail (`collide=False`); the mortar masses carry the collision.
Coordinates are model units; a face looks in direction `phi` and may tilt upward.
"""
import math
from collections import namedtuple

FRONT, BACK, WEST, EAST = -math.pi / 2, math.pi / 2, math.pi, 0  # directions a face can look in
COURSE = 2  # a brick course: a brick 1.75 high and a quarter-unit bed joint

Painted = namedtuple('Painted', 'region width height tone')


def wrap(degrees):
    return (degrees + 180) % 360 - 180


def frame(phi, tilt):
    """The unit vectors across, up and out of a face looking in direction `phi` and tilted `tilt` degrees upward."""
    t = math.radians(tilt)
    return ((-math.sin(phi), 0, math.cos(phi)), (-math.sin(t) * math.cos(phi), math.cos(t), -math.sin(t) * math.sin(phi)),
            (math.cos(t) * math.cos(phi), math.sin(t), math.cos(t) * math.sin(phi)))


def point(origin, phi, tilt, u, v, w=0):
    """The point u across, v up and w out from `origin` on a face looking in direction `phi`, tilted `tilt`."""
    U, V, N = frame(phi, tilt)
    return [o + u * a + v * b + w * c for o, a, b, c in zip(origin, U, V, N)]


def block(m, group, name, face, phi, tilt, depth, length, height, material, front=None, roll=0):
    """A box whose outer face is centred on `face`, looking in direction `phi`, tilted `tilt` degrees upward and turned
    `roll` degrees about its own axis. It is built with that face to the north, then turned round its centre. `front`
    is the face's texture offset inside the material; each side then takes the strip of that crop along its own edge,
    so the box's edges carry on its surface. Its back lies against the masonry and is never drawn."""
    n = frame(phi, tilt)[2]; c = [f - depth / 2 * k for f, k in zip(face, n)]
    faces = {'south': None}
    if front:
        (x, y), (w, h, d) = front, (round(v * 4) for v in (length, height, depth))
        faces.update(north=(material, x, y), up=(material, x, y), down=(material, x, y + h - d),
                     east=(material, x, y), west=(material, x + w - d, y))
    m.box(group, name, [c[0] - length / 2, c[1] - height / 2, c[2] - depth / 2], [c[0] + length / 2, c[1] + height / 2, c[2] + depth / 2],
          material, angle=[tilt, wrap(-90 - math.degrees(phi)), roll], pivot=c, faces=faces, collide=False)


def mass(m, group, name, lo, hi, material='mortar', holes=()):
    """A solid mass lo..hi carrying the collision, less the boxes `holes` such as a port's cell or a door's recess, in
    boxes no larger than its texture. The faces where its boxes meet are never drawn; those round a hole are."""
    cuts = []
    for d in range(3):
        edges = sorted({lo[d], hi[d], *(e for a, b in holes for e in (a[d], b[d]) if lo[d] < e < hi[d])})
        cuts.append([a + (b - a) * i / n for a, b in zip(edges, edges[1:]) for n in [max(1, math.ceil((b - a) / 32))] for i in range(n)] + [hi[d]])
    counts = [len(c) - 1 for c in cuts]

    def solid(index):
        if any(not 0 <= i < n for i, n in zip(index, counts)): return False
        centre = [(cuts[d][i] + cuts[d][i + 1]) / 2 for d, i in enumerate(index)]
        return not any(all(a[d] < centre[d] < b[d] for d in range(3)) for a, b in holes)
    for i in range(counts[0]):
        for j in range(counts[1]):
            for k in range(counts[2]):
                if not solid((i, j, k)): continue
                beside = {'west': (i - 1, j, k), 'east': (i + 1, j, k), 'down': (i, j - 1, k), 'up': (i, j + 1, k),
                          'north': (i, j, k - 1), 'south': (i, j, k + 1)}
                m.box(group, f'{name}_{i}_{j}_{k}', [cuts[0][i], cuts[1][j], cuts[2][k]], [cuts[0][i + 1], cuts[1][j + 1], cuts[2][k + 1]],
                      material, faces={face: None for face, index in beside.items() if solid(index)})


def minus(stretches, holes):
    """The parts of the (start, end) stretches no hole covers."""
    for h0, h1 in holes:
        stretches = [(a, b) for s, e in stretches for a, b in ((s, min(e, h0)), (max(s, h1), e)) if b - a > 1e-6]
    return stretches


def bond(a, b, parity):
    """Running bond along a..b: joints every four units from the face's corner, offset two on odd courses. Returns each
    brick's place as (start, end); a place cut short by the stretch's end takes a brick cut to fit, and one under three
    quarters of a unit is left to the joint."""
    places, edge = [], 2 * parity + 4 * math.floor((a - 2 * parity) / 4)
    while edge < b - 1e-6:
        s, e = max(edge, a), min(edge + 4, b)
        if e - s >= .75: places.append((s, e))
        edge += 4
    return places


# Each vertical side of a box: the direction it looks in, its left corner seen from outside and the axis it runs along.
SIDES = {'front': (FRONT, lambda lo, hi: [lo[0], lo[1], lo[2]], 0), 'back': (BACK, lambda lo, hi: [hi[0], lo[1], hi[2]], 0),
         'west': (WEST, lambda lo, hi: [lo[0], lo[1], hi[2]], 2), 'east': (EAST, lambda lo, hi: [hi[0], lo[1], lo[2]], 2)}
plain = lambda *place: None


class Mason:
    """Lays one machine's masonry from its texture regions, choosing tones by weight from `tones` with `rng`. A `stain`
    callback names the tone a piece at a place takes, such as `soot`, or None for an ordinary piece."""

    def __init__(self, regions, tones, rng):
        self.pieces = {}
        for name, region in regions.items():
            kind, _, tone = name.partition('_')
            if kind in ('brick', 'half', 'rowlock', 'stone'):
                self.pieces.setdefault(kind, []).append(Painted(name, region['rect'][2] / 4, region['rect'][3] / 4, tone))
        self.tones, self.rng = tones, rng
        self.ring_depth = self.pieces['rowlock'][0].height if 'rowlock' in self.pieces else 0

    def piece(self, kind, last, stain=None):
        """A painted piece of `kind` in the `stain` tone where the kind has one, else never the colour of the one laid
        before it."""
        pieces = self.pieces[kind]
        if any(p.tone == stain for p in pieces): return self.rng.choice([p for p in pieces if p.tone == stain])
        tones = sorted({p.tone for p in pieces if p.tone in self.tones and p.tone != last} or {last})
        tone = self.rng.choices(tones, [self.tones[t] for t in tones])[0]
        return self.rng.choice([p for p in pieces if p.tone == tone])

    def lay(self, m, group, name, centre, phi, tilt, place, last, stain=None):
        """One brick filling a place `place` units long on a face, a quarter-unit joint shared either side: a whole
        brick, a half brick, or either cut to fit, showing the painted brick's end that keeps its outline. Returns its
        colour."""
        kind = 'brick' if place > 2.01 else 'half'
        brick = self.piece(kind, last, stain); length = min(brick.width, place - .25); thick = self.rng.choice([.25, .5])
        block(m, group, name, [c + thick * n for c, n in zip(centre, frame(phi, tilt)[2])], phi, tilt, thick, length, brick.height,
              brick.region, (0, 0))
        return brick.tone

    def wall(self, m, group, name, origin, phi, rows, span, stain=plain, tilt=0):
        """Bricks laid on a face looking in direction `phi` and tilted `tilt` degrees up, from `origin` at the face's
        left corner: courses `rows` (first, end), course k lying 2k to 2k + 1.75 up the face, each in running bond.
        `span(y)` gives the stretches across of the course at height y that take brick, so openings, arches and gables
        leave the right places bare; `stain(u, y)` tones bricks by place."""
        for k in range(*rows):
            y = origin[1] + COURSE * k + 1; last = None
            for a, b in span(y):
                for s, e in bond(a, b, k % 2):
                    last = self.lay(m, group, f'{name}_{k}_{s:g}', point(origin, phi, tilt, (s + e) / 2, COURSE * k + .875), phi, tilt,
                                    e - s, last, stain((s + e) / 2, y))

    def face(self, m, group, name, side, lo, hi, foot, ref=None, holes=lambda y: [], stain=plain):
        """Bricks on one side of the mass lo..hi, on the grid of the box `ref` whose side lies in the same plane: courses
        count from the footing's top `foot` and bonds from that box's corner, so masses built against and upon each
        other read as one wall. `holes(y)` gives stretches along the side's axis, in model units, that take no brick."""
        phi, corner, axis = SIDES[side]; ref = ref or (lo, hi); origin = corner(*ref); origin[1] = foot
        left = {'front': lambda a, b: (a - ref[0][0], b - ref[0][0]), 'back': lambda a, b: (ref[1][0] - b, ref[1][0] - a),
                'west': lambda a, b: (ref[1][2] - b, ref[1][2] - a), 'east': lambda a, b: (a - ref[0][2], b - ref[0][2])}[side]
        self.wall(m, group, name, origin, phi, (round((lo[1] - foot) / COURSE), round((hi[1] - foot) / COURSE)),
                  lambda y: minus([left(lo[axis], hi[axis])], [left(a, b) for a, b in holes(y)]), stain)

    def stones(self, m, group, name, lo, hi, sides):
        """A course of footing stones on each named side of the mass lo..hi, as many as fill it, cut to share it evenly."""
        stone0 = self.pieces['stone'][0]
        for side in sides:
            phi, corner, axis = SIDES[side]; length = hi[axis] - lo[axis]
            count = math.ceil(length / (stone0.width + .25)); place = length / count; last = None
            for j in range(count):
                stone = self.piece('stone', last); last = stone.tone; thick = self.rng.choice([.25, .5])
                block(m, group, f'{name}_{side}_{j}', point(corner(lo, hi), phi, 0, (j + .5) * place, (hi[1] - lo[1] - .25) / 2, thick),
                      phi, 0, thick, min(stone.width, place - .25), stone.height, stone.region, (0, 0))

    def brickwork(self, m, group, name, lo, hi, sides, holes=lambda side, y: [], stain=lambda side, u, y: None, top=True):
        """A solid mortar mass lo..hi carrying the collision, its named sides laid with brick, less each side's holes
        measured across from that side's left corner, and its top capped with a course of bricks laid flat."""
        m.box(group, name, lo, hi, 'mortar')
        for side in sides:
            phi, corner, axis = SIDES[side]; length = hi[axis] - lo[axis]
            self.wall(m, group, f'{name}_{side}', corner(lo, hi), phi, (0, round((hi[1] - lo[1]) / COURSE)),
                      lambda y, side=side, length=length: minus([(0, length)], holes(side, y)), lambda u, y, side=side: stain(side, u, y))
        if top: self.cap(m, group, f'{name}_top', lo, hi, stain=lambda u, y: stain('top', u, y))

    def cap(self, m, group, name, lo, hi, stain=plain):
        """The top of the box lo..hi laid with bricks flat, in courses running across it from front to back."""
        self.wall(m, group, name, [lo[0], hi[1], lo[2]], FRONT, (0, int((hi[2] - lo[2] + .25) // COURSE)),
                  lambda y: [(0, hi[0] - lo[0])], stain, tilt=90)

    def footing(self, m, group, name, lo, hi, sides, top=False):
        """A fieldstone footing course on the mortar mass lo..hi, its named sides laid with stones as many as fill each
        side, cut to share it evenly; with `top`, its top is laid with a row of flat stones too."""
        m.box(group, name, lo, hi, 'mortar')
        stone0 = self.pieces['stone'][0]
        rows = [(*SIDES[side], hi[1] - lo[1] - .25, 0) for side in sides]
        if top: rows.append((FRONT, lambda lo, hi: [lo[0], hi[1], lo[2]], 0, stone0.height, 90))
        for i, (phi, corner, axis, tall, tilt) in enumerate(rows):
            length = hi[axis] - lo[axis]; count = math.ceil(length / (stone0.width + .25)); place = length / count; last = None
            for j in range(count):
                stone = self.piece('stone', last); last = stone.tone; thick = self.rng.choice([.25, .5])
                block(m, group, f'{name}_{i}_{j}', point(corner(lo, hi), phi, tilt, (j + .5) * place, tall / 2 + (.125 if tilt else 0), thick),
                      phi, tilt, thick, min(stone.width, place - .25), stone.height, stone.region, (0, 0))

    def ring(self, m, group, name, centre, inner, z, phi, spread, stain=lambda theta: None):
        """An arch ring of bricks on end round `centre` (x, y) from radius `inner`, spreading `spread` radians either
        side of the vertical, standing three quarters of a unit proud of the face at z that looks in direction `phi`,
        clear of the bricks round it."""
        mid = inner + self.ring_depth / 2; count = max(1, round(2 * spread * mid / COURSE)); step = 2 * spread / count; last = None
        for i in range(count):
            theta = -spread + step * (i + .5); brick = self.piece('rowlock', last, stain(theta)); last = brick.tone
            face = [centre[0] + mid * math.sin(theta), centre[1] + mid * math.cos(theta), z + .75 * math.sin(phi)]
            block(m, group, f'{name}_{i}', face, phi, 0, 1.5, brick.width, brick.height, brick.region, (0, 0), roll=-math.degrees(theta))


def segmental_arch(x0, x1, top, rise):
    """Centre (x, y), inner radius and spread of a segmental arch of `rise` springing from an opening's top corners."""
    span = (x1 - x0) / 2
    inner = (span ** 2 + rise ** 2) / (2 * rise)
    return ((x0 + x1) / 2, top + rise - inner), inner, math.asin(span / inner)


def arch_holes(centre, inner, outer, spread, y, x0=0):
    """Where a course at height y crosses an arch ring between the radial lines through its springing, as stretches
    across from a face corner at x0."""
    (ax, ay), dy = centre, y - centre[1]
    if not 0 < dy < outer: return []
    wo = min(math.sqrt(outer ** 2 - dy ** 2), dy * math.tan(spread) + 1); wi = math.sqrt(max(0, inner ** 2 - dy ** 2))
    return [(ax - wo - x0, ax - wi - x0), (ax + wi - x0, ax + wo - x0)] if wo > wi else []
