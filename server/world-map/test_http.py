"""Focused HTTP boundary checks; no game server, worlds or renderer workers."""
import http.client
import json
from pathlib import Path
import socket
import subprocess
import tempfile
import threading
import time
import unittest
import surface_server as app


class HttpBoundaryTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory()
        app.ROOT=app.HERE=app.STATE=Path(self.temp.name)
        (app.HERE/'surface.html').write_text('<html>map</html>')
        (app.HERE/'map.js').write_text('/* map */')
        (app.STATE/'tiles').mkdir()
        (app.STATE/'tiles/r.0.0.png').write_bytes(b'png')
        app.SNAPSHOT=b'{"regions":[]}'
        self.server=app.BoundedHTTPServer(('127.0.0.1',0),app.Handler)
        self.thread=threading.Thread(target=self.server.serve_forever,daemon=True)
        self.thread.start()

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()
        self.temp.cleanup()

    def request(self,path,method='GET'):
        c=http.client.HTTPConnection(*self.server.server_address,timeout=3)
        try:
            c.request(method,path)
            r=c.getresponse()
            return r.status,dict(r.getheaders()),r.read()
        finally:c.close()

    def test_allowlist_and_methods(self):
        for path in ('/','/index.html','/map.js','/api/coverage','/api/overviews','/api/players','/api/deposits','/api/villages','/api/metrics','/tiles/r.0.0.png?v=1'):
            self.assertEqual(self.request(path)[0],200,path)
        for path in ('/server.properties','/surfaces.sqlite','/.env','/surface_server.py','/tiles/../surface.html','/tiles/%2e%2e/surface.html','/api/admin','/tiles/r.'+'9'*100+'.0.png'):
            self.assertEqual(self.request(path)[0],404,path)
        status,headers,body=self.request('/','HEAD')
        self.assertEqual((status,body),(200,b''))
        self.assertNotIn('Python',headers['Server'])
        self.assertEqual(self.request('/','POST')[0],501)

    def test_symlink_cannot_escape_cache(self):
        outside=Path(self.temp.name).parent/('map-boundary-'+Path(self.temp.name).name)
        outside.write_bytes(b'private')
        try:
            link=app.STATE/'tiles/r.1.1.png'
            link.symlink_to(outside)
            self.assertEqual(self.request('/tiles/r.1.1.png')[0],404)
        finally:outside.unlink()

    def test_public_feeds_exclude_private_fields(self):
        (app.ROOT/'pregeneration').mkdir()
        (app.ROOT/'pregeneration/state.json').write_text(json.dumps(dict(status='paused',phase='chunks',tile=281,secret='private',path='/private')))
        app.ROWS.clear()
        app.publish()
        data=json.loads(self.request('/api/coverage')[2])
        self.assertEqual(data['job'],dict(status='paused',phase='chunks',tile=281))
        self.assertNotIn('freeBytes',data)
        player=dict(uuid='test',name='<script>alert(1)</script>',dimension='minecraft:overworld',x=0,y=80,z=0,yaw=0,ip='private')
        feed=app.ROOT/'map-players.json'
        feed.write_text(json.dumps(dict(updated=time.time(),players=[player],secret='private')))
        data=json.loads(self.request('/api/players')[2])
        self.assertNotIn('secret',data)
        self.assertNotIn('ip',data['players'][0])
        feed.write_text(json.dumps(dict(updated=0,players=[player])))
        self.assertEqual(json.loads(self.request('/api/players')[2])['players'],[])

    def test_connection_threads_are_bounded(self):
        clients=[]
        try:
            for _ in range(40):
                s=socket.create_connection(self.server.server_address,timeout=2)
                clients.append(s)
                s.sendall(b'GET / HTTP/1.1\r\n')
            deadline=time.monotonic()+2
            while self.server.slots._value and time.monotonic()<deadline:time.sleep(.01)
            self.assertEqual(self.server.slots._value,0)
        finally:
            for s in clients:s.close()
        deadline=time.monotonic()+2
        while self.server.slots._value!=32 and time.monotonic()<deadline:time.sleep(.01)
        self.assertEqual(self.server.slots._value,32)
        self.assertEqual(self.request('/')[0],200)

    def test_world_and_lod_sizes_are_disjoint_across_dimensions(self):
        world=app.ROOT/'world'
        lod_paths=[]
        for dimension in ('','DIM-1','DIM1'):
            data=world/dimension/'data';data.mkdir(parents=True)
            for suffix in ('','-wal','-shm'):
                p=data/('DistantHorizons.sqlite'+suffix)
                p.write_bytes(b'x'*4096);lod_paths.append(p)
        region=world/'region';region.mkdir()
        (region/'r.0.0.mca').write_bytes(b'y'*12288)
        # Hardlinks count once, symlink targets outside the world count zero.
        (region/'copy.mca').hardlink_to(region/'r.0.0.mca')
        outside=app.ROOT/'outside';outside.write_bytes(b'z'*65536)
        (world/'linked').symlink_to(outside)
        world_bytes,lod_bytes=app.measure_world_sizes(world)
        expected_lods=sum(p.stat().st_blocks*512 for p in lod_paths)
        total=int(subprocess.check_output(['du','-s','-B1',str(world)]).split()[0])
        self.assertEqual(lod_bytes,expected_lods)
        self.assertEqual(world_bytes+lod_bytes,total)
        self.assertGreater(world_bytes,0)


if __name__=='__main__':unittest.main()
