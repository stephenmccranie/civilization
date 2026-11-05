"""Region pass over real compressed Anvil fixtures; no game server or worlds."""
from concurrent.futures import ProcessPoolExecutor
import json
import os
from pathlib import Path
import sqlite3
import struct
import tempfile
import unittest
import zlib
import surface_server as app
import terrain
import villages


class Longs(list):
    pass


SAVES=iter(range(1,1<<30))


def nbt(value):
    """Minimal fixture writer for real compressed Anvil/NBT boundary coverage."""
    def string(v):
        b=v.encode();return struct.pack('>H',len(b))+b
    if isinstance(value,Longs):return 12,struct.pack('>i',len(value))+b''.join(struct.pack('>q',v) for v in value)
    if isinstance(value,str):return 8,string(value)
    if isinstance(value,int):return 3,struct.pack('>i',value)
    if isinstance(value,list):
        rows=[nbt(v) for v in value]
        return 9,bytes([rows[0][0] if rows else 10])+struct.pack('>i',len(rows))+b''.join(v for _,v in rows)
    return 10,b''.join(bytes([nbt(v)[0]])+string(k)+nbt(v)[1] for k,v in value.items())+b'\0'


def heightmap(y):
    """Every column's top block at y, packed as Minecraft packs dimension heights."""
    bits=terrain.HEIGHTMAP_BITS;per=64//bits;value=y+1-terrain.WORLD_MIN_Y
    word=sum(value<<(i*bits) for i in range(per))
    words=[word]*((256+per-1)//per)
    return Longs(v-(1<<64) if v>=1<<63 else v for v in words)


def chunk(cx,cz,status='minecraft:full',block='minecraft:grass_block',village=False,coal=False,y=64):
    sections=[dict(Y=y//16,block_states=dict(palette=[dict(Name=block)]),biomes=dict(palette=['minecraft:plains']))]
    if coal:sections.append(dict(Y=0,block_states=dict(palette=[dict(Name='civilization:coal_seam')]),biomes=dict(palette=['minecraft:plains'])))
    starts={}
    if village:
        starts['minecraft:village_plains']=dict(id='minecraft:village_plains',ChunkX=cx,ChunkZ=cz,
            Children=[{'BB':[cx*16,70,cz*16,cx*16+12,78,cz*16+12]}])
    return dict(Status=status,xPos=cx,zPos=cz,yPos=terrain.WORLD_MIN_Y//16,sections=sections,
        Heightmaps=dict(WORLD_SURFACE=heightmap(y),MOTION_BLOCKING_NO_LEAVES=heightmap(y)),
        structures=dict(starts=starts))


def write_region(path,chunks,corrupt=()):
    """chunks: (chunk, saved timestamp) pairs; corrupt chunk indices hold undecodable data."""
    header=bytearray(8192);body=bytearray();sector=2
    for c,saved in chunks:
        idx=(c['zPos']%32)*32+c['xPos']%32
        raw=b'not zlib' if idx in corrupt else zlib.compress(b'\x0a\0\0'+nbt(c)[1])
        payload=struct.pack('>IB',len(raw)+1,2)+raw
        sectors=(len(payload)+4095)//4096
        struct.pack_into('>I',header,idx*4,(sector<<8)|sectors)
        struct.pack_into('>I',header,4096+idx*4,saved)
        body+=payload+bytes(sectors*4096-len(payload));sector+=sectors
    path.write_bytes(header+body)
    # Distinct save times, as fixture writes can share one filesystem timestamp tick.
    t=next(SAVES)*10**9;os.utime(path,ns=(t,t))


class Idle:
    def submit(self,*_):
        raise AssertionError('Unchanged region reread')


class RegionPassTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pool=ProcessPoolExecutor(2)

    @classmethod
    def tearDownClass(cls):
        cls.pool.shutdown()

    def refresh(self,pool=None):
        return app.refresh(self.db,pool or self.pool)

    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        root=Path(self.temp.name)
        app.ROOT,app.STATE,app.REGIONS=root,root/'state',root/'region'
        (app.STATE/'tiles').mkdir(parents=True);app.REGIONS.mkdir()
        self.db=sqlite3.connect(app.STATE/'regions.sqlite')
        app.load(self.db)
        self.path=app.REGIONS/'r.-1.0.mca'

    def tearDown(self):
        self.db.close()
        self.temp.cleanup()

    def feeds(self):
        return [json.loads(v) for v in (app.SNAPSHOT,app.DEPOSIT_SNAPSHOT,app.VILLAGE_SNAPSHOT)]

    def test_whole_region_is_drawn_in_one_pass(self):
        # More chunks than one row band, plus a chunk that is not yet fully generated.
        chunks=[(chunk(-32+i%32,i//32),1) for i in range(300)]+[(chunk(-1,20,status='minecraft:features'),1)]
        write_region(self.path,chunks)
        self.assertEqual(self.refresh(),(301,1))
        coverage,_,_=self.feeds()
        self.assertEqual(coverage['regions'][0][:3],[-1,0,300])
        self.assertEqual(coverage['fullChunks'],300)
        tile=app.STATE/'tiles/r.-1.0.png'
        with app.Image.open(tile) as image:alpha=image.getchannel('A')
        for i in range(300):self.assertEqual(alpha.getpixel(((i%32)*4,(i//32)*4)),255,i)
        self.assertEqual(alpha.getpixel((31*4,20*4)),0)
        heights=struct.unpack('<16384h',tile.with_suffix('.height.bin').read_bytes())
        self.assertEqual(heights[0],64)
        self.assertEqual(heights[20*4*128+31*4],app.NO_HEIGHT)
        for suffix in ('.relief.png','.elevation.png','.contours.png'):self.assertTrue(tile.with_suffix(suffix).exists())

    def test_only_saved_chunks_are_redrawn_and_layers_follow(self):
        first=[(chunk(-32,0,coal=True),1),(chunk(-31,0),1)]
        write_region(self.path,first)
        self.refresh()
        self.assertEqual(self.refresh(Idle()),(0,0))
        # The second chunk is saved again with a village; only it is redrawn.
        write_region(self.path,[first[0],(chunk(-31,0,village=True),2)])
        self.assertEqual(self.refresh(),(1,1))
        coverage,found,marks=self.feeds()
        self.assertEqual(coverage['fullChunks'],2)
        self.assertEqual([(m['kind'],m['blocks']) for m in found['markers']],[('coal',4096)])
        self.assertEqual([(m['id'],m['x'],m['y'],m['z']) for m in marks['markers']],[('minecraft:village_plains:-31:0',-490,70,6)])
        # Restart rebuilds the same feeds from the stored passes alone.
        before=self.feeds()
        app.ROWS.clear();app.load(self.db)
        self.assertEqual(self.feeds()[1:],before[1:])
        self.assertEqual(self.feeds()[0]['regions'],before[0]['regions'])
        # A saved chunk that loses its village clears the marker.
        write_region(self.path,[first[0],(chunk(-31,0),3)])
        self.refresh()
        self.assertEqual(self.feeds()[2]['markers'],[])

    def test_shading_runs_across_region_edges(self):
        # The east edge of region -1 is shaded before region 0 exists, so it reads as level ground.
        write_region(self.path,[(chunk(-1,0),1)])
        self.refresh()
        version=app.ROWS[self.path.name]['tile'][3]
        def relief(name,x):
            with app.Image.open(app.STATE/'tiles'/name) as image:return image.getpixel((x,0))
        self.assertEqual(relief('r.-1.0.relief.png',127),relief('r.-1.0.relief.png',126))
        # Drawing the higher neighbour shades both sides of the shared edge as one slope.
        write_region(app.REGIONS/'r.0.0.mca',[(chunk(0,0,y=120),1)])
        self.refresh()
        self.assertNotEqual(relief('r.-1.0.relief.png',127),relief('r.-1.0.relief.png',126))
        self.assertNotEqual(relief('r.0.0.relief.png',0),relief('r.0.0.relief.png',1))
        self.assertGreater(app.ROWS[self.path.name]['tile'][3],version)
        self.assertEqual(self.refresh(Idle()),(0,0))

    def test_failed_chunk_is_retried_without_redrawing_the_rest(self):
        write_region(self.path,[(chunk(-32,0),1),(chunk(-31,0),1)],corrupt={1})
        with self.assertLogs(level='WARNING'):
            self.assertEqual(self.refresh(),(1,1))
        self.assertIsNone(app.ROWS[self.path.name]['signature'])
        # The next pass retries only the failed chunk.
        with self.assertLogs(level='WARNING') as logs:
            self.assertEqual(self.refresh(),(0,0))
        self.assertIn('1 chunks failed to decode, first chunk 1',logs.output[0])
        write_region(self.path,[(chunk(-32,0),1),(chunk(-31,0),1)])
        self.assertEqual(self.refresh(),(1,1))
        self.assertEqual(self.feeds()[0]['fullChunks'],2)
        self.assertIsNotNone(app.ROWS[self.path.name]['signature'])

    def test_truncated_and_deleted_regions(self):
        self.path.write_bytes(b'truncated')
        self.assertEqual(self.refresh(),(0,0))
        self.assertNotIn(self.path.name,app.ROWS)
        write_region(self.path,[(chunk(-32,0),1)])
        self.refresh()
        self.path.unlink()
        self.refresh()
        self.assertEqual(self.feeds()[0]['regions'],[])
        self.assertEqual(self.db.execute('SELECT count(*) FROM regions').fetchone(),(0,))

    def test_village_starts_only_and_negative_coordinates(self):
        c=chunk(-1,-33,village=True)
        marker=villages.extract(c)[0]
        self.assertEqual((marker['x'],marker['y'],marker['z']),(-10,70,-522))
        self.assertEqual(marker['id'],'minecraft:village_plains:-1:-33')
        start=c['structures']['starts'].pop('minecraft:village_plains')
        c['structures']['starts']['terralith:fortified_village']=dict(start,id='terralith:fortified_village')
        self.assertEqual(len(villages.extract(c)),1)
        for change in ('outpost','empty','wrong_chunk'):
            c=chunk(-1,-33,village=True);start=c['structures']['starts']['minecraft:village_plains']
            if change=='outpost':start['id']='minecraft:outpost'
            if change=='empty':start['Children']=[]
            if change=='wrong_chunk':start['ChunkX']=50
            self.assertEqual(villages.extract(c),[],change)


if __name__=='__main__':unittest.main()
