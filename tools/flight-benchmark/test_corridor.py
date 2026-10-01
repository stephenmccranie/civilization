import pathlib,struct,tempfile,unittest
from corridor import parse,region,check_region
from run import percent

def named(t,n,p):
    b=n.encode();return bytes([t])+struct.pack('>H',len(b))+b+p

class CorridorTests(unittest.TestCase):
    def test_coordinate_rewrite_negative_regions_and_all_entries(self):
        raw=b'\x0a\0\0'+named(3,'xPos',struct.pack('>i',0))+named(3,'zPos',struct.pack('>i',0))+named(8,'Status',struct.pack('>H',14)+b'minecraft:full')+named(1,'isLightOn',b'\x01')+b'\0'
        _,off=parse(raw)
        with tempfile.TemporaryDirectory() as d:
            args=(-1,2,d,raw,off[('xPos',)],off[('zPos',)])
            size=region(args);p=pathlib.Path(d)/'r.-1.2.mca'
            check_region(p,[(x,z) for x in range(-32,0) for z in range(64,96)])
            self.assertEqual(size,region(args))
            with self.assertRaises(AssertionError):check_region(p,[(0,64)])
    def test_percentiles(self):
        self.assertEqual(percent([100,0],.95),95)
        self.assertEqual(percent([8],.99),8)

if __name__=='__main__':unittest.main()
