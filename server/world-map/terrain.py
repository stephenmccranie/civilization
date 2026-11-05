"""Small Anvil reader and coarse surface renderer; never asks Minecraft for chunks."""
import gzip
import math
import struct
import zlib
from pathlib import Path
import json
from map_colors import surface_color

_world_file=Path(__file__).resolve().parent/'world.json'
WORLD_CONFIG=json.loads(_world_file.read_text()) if _world_file.exists() else {'scale':6,'height':2160,'min_y':-192}
WORLD_HEIGHT=int(WORLD_CONFIG['height'])
WORLD_MIN_Y=int(WORLD_CONFIG['min_y'])
WORLD_TOP=WORLD_MIN_Y+WORLD_HEIGHT-1
HEIGHTMAP_BITS=WORLD_HEIGHT.bit_length()


class NBT:
    def __init__(self, data):
        self.data, self.pos = data, 0

    def take(self, n):
        if n < 0 or self.pos + n > len(self.data):
            raise ValueError('Truncated NBT')
        p = self.pos
        self.pos += n
        return self.data[p:self.pos]

    def unpack(self, fmt):
        return struct.unpack(fmt, self.take(struct.calcsize(fmt)))[0]

    def string(self):
        return self.take(self.unpack('>H')).decode('utf-8', errors='replace')

    def value(self, tag, depth=0):
        if depth > 64:
            raise ValueError('NBT nesting limit')
        if tag in (1, 2, 3, 4, 5, 6):
            return self.unpack({1:'>b', 2:'>h', 3:'>i', 4:'>q', 5:'>f', 6:'>d'}[tag])
        if tag == 7:
            return self.take(self.unpack('>i'))
        if tag == 8:
            return self.string()
        if tag == 9:
            kind, count = self.unpack('>B'), self.unpack('>i')
            if not 0 <= count <= 2000000:
                raise ValueError('NBT list length')
            return [self.value(kind, depth+1) for _ in range(count)]
        if tag == 10:
            result = {}
            while True:
                kind = self.unpack('>B')
                if not kind:
                    return result
                name = self.string()
                result[name] = self.value(kind, depth+1)
        if tag in (11, 12):
            count = self.unpack('>i')
            fmt, size = ('>i', 4) if tag == 11 else ('>q', 8)
            return [v[0] for v in struct.iter_unpack(fmt, self.take(count*size))]
        raise ValueError('Unsupported NBT tag')

    def root(self):
        tag = self.unpack('>B')
        self.string()
        return self.value(tag)


def read_chunk(data, entry, path, x, z):
    """Decode one chunk from a region file read whole, in one sequential pass."""
    offset = (entry >> 8)*4096
    if offset+5 > len(data):
        raise ValueError('Chunk header missing')
    length, compression = struct.unpack_from('>IB', data, offset)
    if not 1 <= length <= 16*1024*1024:
        raise ValueError('Chunk length limit')
    if compression & 128:
        raw = (path.parent / f'c.{x}.{z}.mcc').read_bytes()
    else:
        raw = data[offset+5:offset+4+length]
        if len(raw) != length-1:
            raise ValueError('Chunk data truncated')
    compression &= 127
    if compression in (1, 2):
        d = zlib.decompressobj(31 if compression == 1 else 15)
        raw = d.decompress(raw, 32*1024*1024)
        if not d.eof:
            raise ValueError('Chunk decompression limit or incomplete data')
    elif compression != 3:
        raise ValueError('Unsupported chunk compression: '+str(compression))
    return NBT(raw).root()


def packed(data, index, bits):
    per_long = 64 // bits
    return ((data[index // per_long] & ((1 << 64)-1)) >> ((index % per_long)*bits)) & ((1 << bits)-1)


def palette(container, index, minimum):
    values = container.get('palette', [])
    if not values:
        return None
    if len(values) == 1:
        return values[0]
    i = packed(container['data'], index, max(minimum, (len(values)-1).bit_length()))
    return values[i] if i < len(values) else None


def render_topography(chunk):
    """Sixteen surface colors and ground heights of one full chunk."""
    sections = {s['Y']:s for s in chunk.get('sections', [])}
    heights = chunk.get('Heightmaps', {}).get('WORLD_SURFACE')
    if not heights or not sections:
        raise ValueError('Full chunk has no surface heightmap')
    # Heightmap bit packing depends on the dimension height, including empty space.
    min_y = chunk.get('yPos', WORLD_MIN_Y//16)*16
    bits = HEIGHTMAP_BITS
    pixels = bytearray()
    ground = chunk.get('Heightmaps', {}).get('MOTION_BLOCKING_NO_LEAVES', heights)
    elevations = []
    for z in (2, 6, 10, 14):
        for x in (2, 6, 10, 14):
            height = packed(heights, z*16+x, bits)+min_y-1
            sec = sections.get(height//16, {})
            block = palette(sec.get('block_states', {}), (height & 15)*256+z*16+x, 4)
            biome = palette(sec.get('biomes', {}), ((height & 15)//4)*16+(z//4)*4+x//4, 1)
            pixels.extend((*surface_color(block, biome, height), 255))
            elevations.append(packed(ground, z*16+x, bits)+min_y-1)
    return bytes(pixels), elevations


def topographic_tiles(rgba, heights, width=128):
    """Precompute all display layers once. heights is (width+2)^2: the tile's heights inside a one-pixel border
    from its neighbouring tiles, so slopes and contours run on across tile edges; unknown heights never create cliffs."""
    shaded, elevation, contours = bytearray(rgba), bytearray(len(rgba)), bytearray(len(rgba))
    stops = [(-192,(24,62,107)),(62,(55,111,155)),(63,(135,168,108)),
             (200,(185,190,124)),(500,(171,147,103)),(1000,(189,184,176)),(WORLD_TOP,(247,245,239))]
    padded = width+2
    def height(x,z,fallback):
        h=heights[(z+1)*padded+x+1]
        return h if h != -32768 else fallback
    for z in range(width):
        for x in range(width):
            i=z*width+x; offset=i*4; h=heights[(z+1)*padded+x+1]
            if h == -32768 or not rgba[offset+3]: continue
            left,right=height(x-1,z,h),height(x+1,z,h)
            up,down=height(x,z-1,h),height(x,z+1,h)
            dx=(right-left)/8; dz=(down-up)/8
            light=(.5*dx+.5*dz+.70710678)/math.sqrt(dx*dx+dz*dz+1)
            shade=max(.55,min(1.2,.65+.5*light))
            shaded[offset:offset+3]=bytes(min(255,round(v*shade)) for v in rgba[offset:offset+3])
            for (a,ca),(b,cb) in zip(stops,stops[1:]):
                if h <= b:
                    t=max(0,min(1,(h-a)/(b-a)))
                    color=tuple(round(ca[k]+t*(cb[k]-ca[k])) for k in range(3))
                    break
            else: color=stops[-1][1]
            elevation[offset:offset+4]=bytes((*color,255))
            # Compare adjacent samples, excluding unknown areas.
            if h//50 != right//50 or h//50 != down//50:
                major=h//200 != right//200 or h//200 != down//200
                contours[offset:offset+4]=bytes((31,27,22,175 if major else 100))
    return png(shaded,width), png(elevation,width), png(contours,width)


def png(pixels, width=128):
    def part(kind, data):
        return struct.pack('>I', len(data))+kind+data+struct.pack('>I', zlib.crc32(kind+data))
    rows = b''.join(b'\x00'+pixels[y*width*4:(y+1)*width*4] for y in range(width))
    return b'\x89PNG\r\n\x1a\n'+part(b'IHDR', struct.pack('>IIBBBBB',width,width,8,6,0,0,0))+part(b'IDAT',zlib.compress(rows,3))+part(b'IEND',b'')
