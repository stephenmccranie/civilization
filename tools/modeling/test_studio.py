"""Focused regression checks for silent UV distortion and invalid atlas use."""
import unittest
from studio import machine_cells, Model, uv_faces, bounds

class StudioTests(unittest.TestCase):
    def test_density_and_long_face_rejection(self):
        m=Model();p=m.box('body','barrel',[0,0,0],[11,11,18])
        r={'iron':{'rect':[0,0,64,64]}}
        self.assertEqual(uv_faces(p,r)['east'],[0,0,36,22])
        with self.assertRaises(ValueError):uv_faces(p,{'iron':{'rect':[0,0,32,32]}})

    def test_64px_faces_keep_density_and_reject_small_tiles(self):
        p=Model().box('body','block',[0,0,0],[16,5,16])
        r={'iron':{'rect':[64,0,64,64]}}
        self.assertEqual(uv_faces(p,r,density=4)['up'],[64,0,128,64])
        self.assertEqual(uv_faces(p,r,density=4)['north'],[64,0,128,20])
        with self.assertRaises(ValueError):uv_faces(p,{'iron':{'rect':[0,0,32,32]}},density=4)

    def test_only_explicit_decal_fits(self):
        p=Model().box('body','panel',[0,0,0],[6,5,1],faces={'north':'dial'})
        r={'iron':{'rect':[0,0,64,64]},'dial':{'rect':[64,0,32,32],'fit':True}}
        self.assertEqual(uv_faces(p,r)['north'],[64,0,96,32])
        self.assertEqual(uv_faces(p,r)['south'],[0,0,12,10])

    def test_offset_and_turned_islands_stay_inside_their_region(self):
        p=Model().box('body','leg',[0,0,0],[2,6,2],'wood',faces={'east':('wood',4,8,90),'west':('wood',0,16)})
        r={'wood':{'rect':[10,20,32,40]}}
        self.assertEqual(uv_faces(p,r,density=4)['east'],[14,28,38,36])
        self.assertEqual(uv_faces(p,r,density=4)['west'],[10,36,18,60])
        with self.assertRaises(ValueError):uv_faces(Model().box('body','tall',[0,0,0],[2,6,2],'wood',faces={'east':('wood',12,0,90)}),r,density=4)

    def test_unique_names_and_volume(self):
        m=Model();m.box('a','part',[0,0,0],[1,1,1])
        with self.assertRaises(ValueError):m.box('b','part',[0,0,0],[1,1,1])
        with self.assertRaises(ValueError):m.box('a','empty',[0,0,0],[0,1,1])

    def test_bounds_include_rotation(self):
        m=Model();m.box('a','rotated',[-1,-1,-1],[1,1,1],angle=45,axis='z')
        lo,hi=bounds(m.parts)
        self.assertAlmostEqual(hi[0],2**.5)
        self.assertAlmostEqual(lo[1],-2**.5)
        self.assertEqual(hi[2],1)

    def test_compound_rotation_turns_x_then_y(self):
        m=Model();m.box('a','tilted',[0,0,0],[2,1,1],angle=[90,90,0])
        lo,hi=bounds(m.parts)
        for got,want in zip(lo+hi,[0,-1,-2,1,0,0]):self.assertAlmostEqual(got,want)

    def test_controller_claims_its_cell_and_takes_no_part_cells(self):
        m=Model();m.box('section_0_wall','wall',[-16,0,0],[32,16,48]);m.box('section_0_wall','stone',[-2,2,-1],[2,4,0],collide=False)
        m.claim('controller',[0,0,0],[16,16,16])
        self.assertEqual(next(p['group'] for p in m.parts if p['name']=='stone'),'section_0_wall')
        self.assertEqual([(p['start'],p['end']) for p in m.parts if p['group']=='controller'],[([0,0,0],[16,16,16])])
        cells=machine_cells(m.parts)
        self.assertNotIn([0,0,0],[c['offset'] for c in cells])
        self.assertAlmostEqual(sum((b[4]-b[1])*(b[5]-b[2])*(b[6]-b[3]) for c in cells for b in c['boxes']),8)

    def test_rotated_parts_are_sliced_across_cells(self):
        m=Model();m.box('section_1_brace','brace',[0,16,8],[4,40,12],angle=45,axis='z',pivot=[2,16,10])
        offsets=sorted(tuple(c['offset']) for c in machine_cells(m.parts))
        self.assertIn((-1,1,0),offsets);self.assertNotIn((1,1,0),offsets)
        self.assertTrue(all(b[0]==1 for c in machine_cells(m.parts) for b in c['boxes']))

if __name__=='__main__':unittest.main()
