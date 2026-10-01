"""Lifecycle and failure checks; synthetic images never certify artistic quality."""
import contextlib
import io
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from PIL import Image
import art


class ArtTests(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory(); self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name); self.folder = self.root/'asset'; self.folder.mkdir()
        self.runtime = self.root/'runtime'
        for module in (art, art.legacy):
            for key, value in [('ROOT', self.root), ('RUNTIME', self.runtime)]:
                patcher = patch.object(module, key, value); patcher.start(); self.addCleanup(patcher.stop)
        Image.new('RGBA', (64,64), '#778899').save(self.folder/'icon.png')
        self.asset = {'schema':2, 'kind':'item', 'brief':dict.fromkeys(('purpose','target','constraints'),'fixture'),
            'references':[{'path':'asset/icon.png','role':'material'}], 'texels_per_block':64,
            'source':{'mode':'project','path':'icon.png'}, 'sources':[], 'models':[],
            'outputs':[{'source':'icon.png','target':'textures/item/test.png','format':'png','size':[64,64]}],
            'gecko_manifests':[], 'generations':[], 'reviews':{}}
        self.save()

    def save(self): art.write(self.folder/'asset.json', self.asset)
    def command(self, command, *args):
        with patch.object(sys,'argv',['art.py',command,str(self.folder),*args]), contextlib.redirect_stdout(io.StringIO()): art.main()
    def review(self, stage, decision='pass'):
        if stage == 'build': self.command('prepare')
        art.write(self.folder/'findings.json', {'decision':decision,'criteria':dict.fromkeys(art.QUESTIONS,'fixture observation'),
                 'defects':[] if decision=='pass' else ['visible defect']})
        self.command('review','--stage',stage,'--review',str(self.folder/'findings.json'),'--evidence','icon.png')

    def test_no_generation_lifecycle_and_stale_source(self):
        self.command('prepare'); self.review('build'); self.command('preview'); self.review('verify')
        self.command('publish'); self.command('check','--complete')
        self.assertEqual((self.runtime/'textures/item/test.png').read_bytes(), (self.folder/'icon.png').read_bytes())
        Image.new('RGBA',(64,64),'#223344').save(self.folder/'icon.png')
        with self.assertRaisesRegex(ValueError,'stale'): self.command('publish')

    def test_final_publish_requires_game_review_and_preserves_previous_runtime(self):
        self.review('build')
        with self.assertRaisesRegex(ValueError,'verify'): self.command('publish')
        previous=self.runtime/'textures/item/test.png'; previous.parent.mkdir(parents=True); previous.write_bytes(b'old asset')
        self.command('preview')
        saved=art.read(self.folder/'preview-backup.json')['textures/item/test.png']
        self.assertEqual((self.folder/saved).read_bytes(),b'old asset')
        previous.write_bytes(b'wrong preview')
        with self.assertRaisesRegex(ValueError,'differs'): self.review('verify')

    def test_revoked_review_blocks_preview(self):
        self.review('build'); self.review('build','revise')
        with self.assertRaisesRegex(ValueError,'visual review'): self.command('preview')

    def test_native_density_and_named_decals(self):
        model={'resolution':{'width':64,'height':64},'textures':[{'width':64,'height':64}],
               'elements':[{'name':'body','from':[0,0,0],'to':[16,16,16],
                            'faces':{'north':{'uv':[0,0,64,64],'texture':0}}}]}
        art.write(self.folder/'model.bbmodel',model); self.asset['models']=['model.bbmodel']
        art.uv_density(self.asset,self.folder)
        model['elements'][0]['faces']['north']['uv']=[0,0,32,32]; art.write(self.folder/'model.bbmodel',model)
        with self.assertRaisesRegex(ValueError,'density mismatch'): art.uv_density(self.asset,self.folder)
        self.asset['decals']=['model.bbmodel:body/north']; art.uv_density(self.asset,self.folder)
        self.asset['decals'].append('model.bbmodel:missing/up')
        with self.assertRaisesRegex(ValueError,'Unknown decal'): art.uv_density(self.asset,self.folder)

    def test_bundle_validates_before_copy_and_rejects_escape(self):
        self.asset['outputs'].append(dict(self.asset['outputs'][0], source='missing.png',target='textures/item/missing.png')); self.save()
        with self.assertRaises(FileNotFoundError): self.command('preview')
        self.assertFalse(self.runtime.exists())
        with self.assertRaisesRegex(ValueError,'escapes'): art.local(self.folder,'../outside')

    def test_failed_copy_restores_bundle(self):
        first=self.runtime/'textures/item/test.png'; second=self.runtime/'textures/item/other.png'
        first.parent.mkdir(parents=True); first.write_bytes(b'original')
        self.asset['outputs'].append(dict(self.asset['outputs'][0],target='textures/item/other.png'))
        original=Path.write_bytes
        def fail(path, data):
            if path == second: raise OSError('simulated write failure')
            return original(path,data)
        with patch.object(Path,'write_bytes',fail), self.assertRaises(OSError): art.copy_bundle(self.asset,self.folder)
        self.assertEqual(first.read_bytes(),b'original'); self.assertFalse(second.exists())


if __name__ == '__main__': unittest.main()
