"""Focused regression checks for silent UV distortion and invalid atlas use."""
import unittest
from studio import Model, uv_faces, bounds

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

if __name__=='__main__':unittest.main()
