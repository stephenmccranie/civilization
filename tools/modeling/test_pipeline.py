"""Synthetic review records here test bookkeeping, never stand in for real visual review."""
import contextlib
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
from PIL import Image
import pipeline as p

class PipelineTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(); self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name); self.folder = self.root / 'asset'; self.folder.mkdir()
        self.runtime = self.root / 'runtime'
        for name, value in [('ROOT', self.root), ('RUNTIME', self.runtime)]:
            mocked = patch.object(p, name, value); mocked.start(); self.addCleanup(mocked.stop)
        Image.new('RGBA', (32, 32), '#887766').save(self.root / 'reference.png')
        Image.new('RGBA', (32, 32), '#776655').save(self.folder / 'icon.png')
        (self.folder / 'master.txt').write_text('synthetic source')
        self.asset = {'schema': 1, 'kind': 'item', 'brief': {k:'Synthetic test' for k in
            ('description','purpose','silhouette','bounds','materials','interfaces','states','construction','invariants')},
            'references': [{'path':'reference.png','role':'synthetic test'}], 'sources':['master.txt'],
            'outputs':[{'source':'icon.png','target':'textures/item/test.png','format':'png','size':[32,32],'alpha':'opaque'}],
            'gecko_manifests':[], 'generations':[], 'reviews':[]}
        self.save()

    def save(self): p.write(self.folder / 'asset.json', self.asset)
    def command(self, command, *args):
        with patch.object(sys, 'argv', ['pipeline.py',command,str(self.folder),*args]), contextlib.redirect_stdout(io.StringIO()): p.main()

    def mockup(self):
        prompt = self.root / 'prompt.txt'; prompt.write_text('synthetic test prompt')
        self.command('record','--image',str(self.root/'reference.png'),'--prompt',str(prompt))
        self.review('mockup')

    def review(self, stage, decision='pass'):
        review = self.root / 'review.json'
        p.write(review, {'decision':decision, 'criteria':{k:'Synthetic assertion fixture' for k in p.CRITERIA[stage]},
                         'remaining_issues':[] if decision=='pass' else ['Synthetic defect']})
        self.command('review','--stage',stage,'--review',str(review),'--evidence','mockup-01.png')

    def test_full_item_lifecycle_and_stale_source(self):
        self.mockup(); self.review('model'); self.command('publish'); self.review('ingame'); self.command('check','--complete')
        self.assertTrue((self.runtime/'textures/item/test.png').exists())
        (self.folder/'master.txt').write_text('changed source')
        with self.assertRaisesRegex(ValueError,'stale'): self.command('check','--complete')

    def test_compact_stage_review_preserves_validation(self):
        self.mockup()
        review = self.root / 'compact-review.json'
        p.write(review, {stage: {'decision':'pass', 'criteria':{key:'Synthetic stage finding' for key in p.CRITERIA[stage]}, 'remaining_issues':[]} for stage in ('model','ingame')})
        self.command('review','--stage','model','--review',str(review),'--evidence','mockup-01.png')
        self.command('publish')
        self.command('review','--stage','ingame','--review',str(review),'--evidence','mockup-01.png')
        self.command('check','--complete')
        (self.folder/'master.txt').write_text('changed after compact review')
        with self.assertRaisesRegex(ValueError,'stale'): self.command('check','--complete')

    def test_cannot_publish_without_visual_review(self):
        with self.assertRaisesRegex(ValueError,'review'): self.command('publish')
        self.assertFalse(self.runtime.exists())

    def test_revise_invalidates_pass(self):
        self.mockup(); self.review('model'); self.review('mockup','revise')
        with self.assertRaisesRegex(ValueError,'review'): self.command('publish')

    def test_changed_evidence_invalidates_review(self):
        self.mockup(); self.review('model'); (self.folder/'mockup-01.png').write_bytes(b'changed')
        with self.assertRaisesRegex(ValueError,'evidence changed'): self.command('publish')

    def test_brief_change_requires_new_mockup(self):
        self.mockup(); self.asset=p.read(self.folder/'asset.json');self.asset['brief']['bounds']='Changed';self.save()
        with self.assertRaisesRegex(ValueError,'different brief'): self.review('mockup')

    def test_java_blocks_and_multiblock_bundle(self):
        for kind in ['block','multiblock']:
            self.asset['kind']=kind
            p.write(self.folder/'model.json', {'parent':'minecraft:block/cube_all','textures':{'all':'minecraft:block/stone'}})
            self.asset['outputs']=[{'source':'model.json','target':'models/block/test.json','format':'java_model'}]
            p.validate_outputs(self.asset,self.folder)

    def test_invalid_java_rotation(self):
        with self.assertRaisesRegex(ValueError,'rotation'):
            p.java_model({'elements':[{'from':[0,0,0],'to':[16,16,16],'rotation':{'axis':'x','angle':30}}]})

    def test_ui_dimensions(self):
        self.asset['kind']='ui';self.asset['outputs'][0]['size']=[128,128]
        with self.assertRaisesRegex(ValueError,'pixel size'): p.validate_outputs(self.asset,self.folder)

    def test_gecko_requires_manifest(self):
        p.write(self.folder/'geometry.json',{})
        self.asset['kind']='animated_machine';self.asset['outputs']=[{'source':'geometry.json','target':'geo/test.json','format':'gecko_geometry'}]
        with self.assertRaisesRegex(ValueError,'paired'): p.validate_outputs(self.asset,self.folder)

    def test_escape_and_duplicate_targets(self):
        self.asset['outputs'][0]['target']='../escape.png'
        with self.assertRaisesRegex(ValueError,'escapes'): p.validate_outputs(self.asset,self.folder)
        self.asset['outputs'][0]['target']='textures/item/test.png'
        self.asset['outputs'].append(dict(self.asset['outputs'][0]))
        with self.assertRaisesRegex(ValueError,'Duplicate'): p.validate_outputs(self.asset,self.folder)

if __name__ == '__main__': unittest.main()
