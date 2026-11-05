"""Circular bands of DH-sized tiles around 0,0, swept in arc order."""
from functools import lru_cache
from math import atan2,hypot,isqrt

# The installed DH pregen command's minimum radius is 32 chunks (a 65x65-chunk
# square). Tiles match that footprint, so Chunky generates every chunk that DH
# then indexes instead of DH generating ahead of the front on its own.
DH_RADIUS=32
TILE_SIZE=DH_RADIUS*32
# Chunky rounds a square radius up to whole chunks: 511 selects the same 65x65
# chunks, sharing one boundary chunk with the next tile.
CHUNKY_RADIUS=TILE_SIZE//2-1
VERSION=4


def new_state():
    return dict(version=VERSION,tileSize=TILE_SIZE,band=0,offset=0,tile=0,phase='chunks',
        status='paused',completeRadius=0)


@lru_cache(maxsize=4)
def band_tiles(band):
    """Tiles whose centres lie in one Euclidean annulus, sorted by angle."""
    inner,outer=4*band*band,4*(band+1)*(band+1)
    tiles=[]
    for x in range(-band-1,band+1):
        xx=(2*x+1)**2
        if xx>=outer:continue
        minimum=max(0,inner-xx)
        lo=isqrt(minimum)
        if lo*lo<minimum:lo+=1
        lo=max(1,lo)
        if lo%2==0:lo+=1
        hi=isqrt(outer-1-xx)
        for odd_z in range(lo,hi+1,2):
            tiles.append((x,(odd_z-1)//2))
            tiles.append((x,(-odd_z-1)//2))
    return tuple(sorted(tiles,key=lambda p:(atan2(2*p[1]+1,2*p[0]+1),(2*p[0]+1)**2+(2*p[1]+1)**2)))


def reach(x,z):
    """Farthest block distance from 0,0 inside tile x,z, including its shared boundary chunk."""
    far=lambda i:max(abs(i*TILE_SIZE),abs((i+1)*TILE_SIZE+16))
    return hypot(far(x),far(z))


def migrate(state):
    if state.get('version')==VERSION:
        if state.get('tileSize')!=TILE_SIZE:raise ValueError('Unsupported tile size')
        if any(not isinstance(state.get(k),int) or state[k]<0
                for k in ('band','offset','tile','completeRadius')):
            raise ValueError('Invalid radial checkpoint counters')
        if state.get('phase') not in ('chunks','lods') or state['offset']>=len(band_tiles(state['band'])):
            raise ValueError('Invalid radial checkpoint phase/cursor')
        return state
    if state.get('version')==3 and state.get('tileSize')==256 and 'pendingBandTiles' not in state:
        # Every 256-block patch centred inside the current band's inner radius has
        # both phases complete, and any point within that radius less the patch
        # half-diagonal lies in such a patch.
        return dict(new_state(),tile=state['tile'],status=state['status'],
            completeRadius=state['band']*256-182)
    raise ValueError('Unsupported pregeneration checkpoint')


def advance(state):
    state['offset']+=1
    if state['offset']>=len(band_tiles(state['band'])):
        state['band']+=1
        state['offset']=0


def next_patch(state):
    while True:
        x,z=band_tiles(state['band'])[state['offset']]
        if reach(x,z)>state['completeRadius']:
            return x*TILE_SIZE+TILE_SIZE//2,z*TILE_SIZE+TILE_SIZE//2
        advance(state)


def commands(phase,x,z):
    if phase=='chunks':
        return ['chunky world minecraft:overworld','chunky pattern spiral',
            f'chunky start minecraft:overworld square {x} {z} {CHUNKY_RADIUS}']
    if phase=='lods':return [f'dh pregen start minecraft:overworld {x} {z} {DH_RADIUS}']
    raise ValueError('Unknown phase')
