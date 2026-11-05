import json
import os
import math
from pathlib import Path
import tempfile
import unittest
from types import SimpleNamespace
from unittest.mock import patch
import radial
import pregenerate
from pregen_log import LogTail


def distance(tile):return (2*tile[0]+1)**2+(2*tile[1]+1)**2


def run_controller(tail,record):
    """Run the controller against scripted log reads; return its saved checkpoint."""
    with tempfile.TemporaryDirectory() as temp:
        root=Path(temp);state=root/'state.json';log=root/'latest.log';log.touch()
        original_exists=os.path.exists
        with patch.multiple(pregenerate,ROOT=root,STATE=state,LOG=log,stopped=False),\
             patch('pregenerate.signal.signal'),patch('pregenerate.LogTail',tail),\
             patch('pregenerate.os.path.exists',side_effect=lambda p:True if str(p)==pregenerate.PIPE else original_exists(p)),\
             patch('pregenerate.shutil.disk_usage',return_value=SimpleNamespace(free=1024**4)),\
             patch('pregenerate.subprocess.check_output',return_value='123'),\
             patch('pregenerate.time.sleep'),patch('pregenerate.command',record):
            pregenerate.main()
        return json.loads(state.read_text())


class RadialTest(unittest.TestCase):
    def test_complete_symmetric_disk(self):
        tiles=[p for band in range(40) for p in radial.band_tiles(band)]
        self.assertEqual(len(tiles),len(set(tiles)))
        expected={(x,z) for x in range(-40,40) for z in range(-40,40) if distance((x,z))<4*40**2}
        self.assertEqual(set(tiles),expected)
        for band in range(40):
            ring=radial.band_tiles(band)
            self.assertTrue(all(4*band**2<=distance(p)<4*(band+1)**2 for p in ring))
            self.assertEqual(sum(2*x+1 for x,z in ring),0)
            self.assertEqual(sum(2*z+1 for x,z in ring),0)

    def test_arc_order_keeps_neighbouring_tiles_together(self):
        arc=radial.band_tiles(15)
        travel=lambda points:sum(math.hypot(b[0]-a[0],b[1]-a[1]) for a,b in zip(points,points[1:]))
        self.assertLess(travel(arc),travel(sorted(arc,key=distance))/5)

    def test_v3_checkpoint_resumes_at_first_tile_beyond_completed_radius(self):
        old=dict(version=3,tileSize=256,band=62,offset=98,tile=3700,phase='chunks',status='running',
            legacyCompletedTiles=535,legacyCheckpoint=dict(tile=535),center=[-128,-16000])
        state=radial.migrate(old)
        self.assertEqual(state,dict(radial.new_state(),tile=3700,status='running',completeRadius=62*256-182))
        # Any point within the radius lies in a 256-block patch centred in an earlier band.
        for x in range(-15700,15700,97):
            for z in range(-15700,15700,89):
                if math.hypot(x,z)<=state['completeRadius']:
                    self.assertLess(math.hypot(x//256*256+128,z//256*256+128),62*256)
        centre=radial.next_patch(state)
        x,z=radial.band_tiles(state['band'])[state['offset']]
        self.assertEqual(centre,(x*1024+512,z*1024+512))
        self.assertGreater(radial.reach(x,z),state['completeRadius'])
        skipped=[p for band in range(state['band']) for p in radial.band_tiles(band)]+list(
            radial.band_tiles(state['band'])[:state['offset']])
        self.assertTrue(all(radial.reach(*p)<=state['completeRadius'] for p in skipped))
        self.assertEqual(radial.migrate(json.loads(json.dumps(state))),state)
        for bad in (dict(state,phase='unknown'),dict(state,offset=-1),dict(old,version=2),
                dict(old,pendingBandTiles=[[0,0]])):
            with self.assertRaises(ValueError):radial.migrate(bad)

    def test_chunky_selection_matches_dh_footprint_with_shared_boundary(self):
        for x,z in [(-3,-3),(-1,0),(0,0),(3,4)]:
            cx,cz=x*1024+512,z*1024+512
            # Chunky 1.4.23 Selection uses ceil(radius/16) chunks; DH counts its radius in chunks.
            chunky=-(-radial.CHUNKY_RADIUS//16)
            self.assertEqual(chunky,radial.DH_RADIUS)
            self.assertEqual((cx//16-chunky,cx//16+chunky),(x*64,x*64+64))
            self.assertEqual((cz//16-chunky,cz//16+chunky),(z*64,z*64+64))
            self.assertEqual((cx+1024)//16-chunky,cx//16+chunky)
            corners=[math.hypot(a,b) for a in (x*1024,x*1024+1040) for b in (z*1024,z*1024+1040)]
            self.assertEqual(radial.reach(x,z),max(corners))
            self.assertIn(f'square {cx} {cz} 511',radial.commands('chunks',cx,cz)[2])
            self.assertEqual(radial.commands('lods',cx,cz),[f'dh pregen start minecraft:overworld {cx} {cz} 32'])

    def test_controller_checkpoints_chunks_before_lods_then_advances(self):
        issued=[]
        issued_steps=[]
        class Tail:
            progress=0
            def __init__(self,*_):self.step=0
            def read(self):
                rows=[['Done ('],['Task already started for minecraft:overworld!'],
                    ['Task stopped for minecraft:overworld.'],['A task was already started for this world.'],
                    ['Task finished for minecraft:overworld.'],  # unrelated old selection
                    ['Task stopped for minecraft:overworld.','Task started in minecraft:overworld for the square region centered at -512, -512 with radius 511.'],
                    ['Task finished for minecraft:overworld.'],[],['Pregen is complete']]
                result=rows[self.step];self.step+=1
                Tail.progress=self.step
                if self.step==9:pregenerate.stopped=True
                return result
            def close(self):pass
            def discard(self):return []
        def record(command):
            issued.append(command);issued_steps.append(Tail.progress)
        saved=run_controller(Tail,record)
        self.assertEqual((saved['tile'],saved['phase'],saved['offset']),(1,'chunks',1))
        self.assertEqual(issued[:3],radial.commands('chunks',-512,-512))
        self.assertEqual(issued[3:6],radial.commands('chunks',-512,-512))
        self.assertEqual(issued[6],'chunky confirm')
        self.assertEqual(issued[7],radial.commands('lods',-512,-512)[0])
        self.assertEqual(issued_steps[7],8)
        self.assertEqual(issued[-2:],['chunky pause minecraft:overworld','dh pregen stop'])

    def test_failed_chunk_pauses_instead_of_waiting_forever(self):
        issued=[]
        class Tail:
            def __init__(self,*_):self.rows=[['Done ('],
                ['Task started in minecraft:overworld for the square region centered at -512, -512 with radius 511.'],
                ['[02Nov2026 15:29:40.152] [Server thread/ERROR] [net.minecraft.server.MinecraftServer/]: Failed to load chunk -383,903']]
            def read(self):return self.rows.pop(0)
            def close(self):pass
            def discard(self):return []
        saved=run_controller(Tail,issued.append)
        self.assertEqual(saved['status'],'paused after generation failure: Failed to load chunk -383,903')
        self.assertEqual((saved['tile'],saved['phase'],saved['center']),(0,'chunks',[-512,-512]))
        self.assertEqual(issued[-2:],['chunky pause minecraft:overworld','dh pregen stop'])

    def test_players_online_hold_generation_until_the_server_is_empty(self):
        issued=[]
        start='Task started in minecraft:overworld for the square region centered at -512, -512 with radius 511.'
        server=lambda text:f'[05Nov2026 07:50:00.000] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: {text}'
        class Tail:
            def __init__(self,*_):self.rows=[[server('Done (3.9s)! For help, type "help"')],[server(start)],
                [server('19BitPrice joined the game'),server('<19BitPrice> Boaty left the game')],[],
                [server('There are 1 of a max of 20 players online: 19BitPrice')],[server('19BitPrice left the game')],[],
                [server(start)]]
            def read(self):
                if not self.rows:pregenerate.stopped=True;return []
                return self.rows.pop(0)
            def close(self):pass
            def discard(self):return []
        saved=run_controller(Tail,issued.append)
        chunks=radial.commands('chunks',-512,-512)
        self.assertEqual(issued[:3],chunks)
        # A join pauses both generators at once; chat that reads like a leave changes nothing.
        self.assertEqual(issued[3:5],['chunky pause minecraft:overworld','dh pregen stop'])
        # Once the last player leaves, the same tile is selected again.
        self.assertEqual(issued[5:8],chunks)
        self.assertEqual((saved['tile'],saved['phase'],saved['status']),(0,'chunks','paused'))

    def test_server_shutdown_is_no_generation_failure(self):
        issued=[]
        class Tail:
            def __init__(self,*_):self.rows=[['Done ('],
                ['Task started in minecraft:overworld for the square region centered at -512, -512 with radius 511.'],
                ['Stopping the server'],['Task stopped for minecraft:overworld.'],['Done ('],
                ['Task started in minecraft:overworld for the square region centered at -512, -512 with radius 511.']]
            def read(self):
                if not self.rows:pregenerate.stopped=True;return []
                return self.rows.pop(0)
            def close(self):pass
            def discard(self):return []
        saved=run_controller(Tail,issued.append)
        self.assertEqual(saved['status'],'paused')
        # After the restart's probes, the same tile is selected again.
        self.assertEqual([c for c in issued if c!='list'][3:6],radial.commands('chunks',-512,-512))

    def test_rotated_log_does_not_replay_completion(self):
        with tempfile.TemporaryDirectory() as temp:
            path=Path(temp)/'latest.log';path.write_text('old completion\n')
            tail=LogTail(path);tail.discard()
            try:
                path.rename(path.with_suffix('.old'));path.write_text('new progress\n')
                self.assertEqual(tail.read(),['new progress\n'])
                self.assertEqual(tail.read(),[])
            finally:tail.close()


if __name__=='__main__':unittest.main()
