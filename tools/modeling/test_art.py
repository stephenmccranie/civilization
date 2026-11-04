"""Lifecycle and failure checks; synthetic images never certify artistic quality."""
import contextlib
import io
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from PIL import Image, ImageDraw
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

    def test_entity_requires_density_checked_native_model(self):
        self.asset['kind']='entity'
        art.definition(self.asset,self.folder)
        with self.assertRaisesRegex(ValueError,'Declare native models'):
            art.uv_density(self.asset,self.folder)
        model={'resolution':{'width':64,'height':64},'textures':[{'width':64,'height':64}],
               'elements':[{'name':'body','from':[0,0,0],'to':[16,16,16],
                            'faces':{'north':{'uv':[0,0,64,64],'texture':0}}}]}
        art.write(self.folder/'entity.bbmodel',model)
        self.asset['models']=['entity.bbmodel']
        art.uv_density(self.asset,self.folder)
        model['elements'][0]['faces']['north']['uv']=[0,0,32,32]
        art.write(self.folder/'entity.bbmodel',model)
        with self.assertRaisesRegex(ValueError,'density mismatch'):
            art.uv_density(self.asset,self.folder)

    def test_bundle_validates_before_copy_and_rejects_escape(self):
        self.asset['outputs'].append(dict(self.asset['outputs'][0], source='missing.png',target='textures/item/missing.png')); self.save()
        with self.assertRaises(FileNotFoundError): self.command('preview')
        self.assertFalse(self.runtime.exists())
        with self.assertRaisesRegex(ValueError,'escapes'): art.local(self.folder,'../outside')

    def java_model(self):
        self.asset['models']=['block.json'];self.asset['sources']=['icon.png'];self.asset['material_textures']={'minecraft:block/stone':'icon.png'}
        model={'textures':{'stone':'minecraft:block/stone','alias':'#stone'},'elements':[{'from':[0,16,0],'to':[16,16,16],'faces':{'up':{'texture':'#alias','uv':[0,0,16,16]}}}]}
        art.write(self.folder/'block.json',model);return model

    def test_java_texture_pixels_and_plane_density(self):
        model=self.java_model();art.uv_density(self.asset,self.folder)
        model['elements'][0]['faces']['up']['uv']=[0,0,8,8];art.write(self.folder/'block.json',model)
        with self.assertRaisesRegex(ValueError,'density mismatch'):art.uv_density(self.asset,self.folder)

    def test_java_material_source_required_and_confined(self):
        self.java_model();self.asset['sources']=[]
        with self.assertRaisesRegex(ValueError,'declared sources'):art.uv_density(self.asset,self.folder)
        self.asset['sources']=['../icon.png'];self.asset['material_textures']['minecraft:block/stone']='../icon.png'
        with self.assertRaisesRegex(ValueError,'escapes'):art.uv_density(self.asset,self.folder)

    def test_java_alias_cycle_and_missing_source(self):
        model=self.java_model();model['textures']['stone']='#alias';art.write(self.folder/'block.json',model)
        with self.assertRaisesRegex(ValueError,'Cyclic'):art.uv_density(self.asset,self.folder)
        model['textures']['stone']='minecraft:block/unknown';art.write(self.folder/'block.json',model)
        with self.assertRaisesRegex(ValueError,'source pixels'):art.uv_density(self.asset,self.folder)

    def test_java_default_uv_matches_partial_face(self):
        model=self.java_model();model['elements'][0]['to']=[8,16,16];model['elements'][0]['faces']['up'].pop('uv');art.write(self.folder/'block.json',model)
        art.uv_density(self.asset,self.folder)

    def textured(self):
        """A 2 by 1 master: a red panel with a dark top band and a blue panel, each inside a gray margin."""
        master=Image.new('RGB',(200,100),'#808080'); draw=ImageDraw.Draw(master)
        draw.rectangle((10,10,89,89),fill=(200,40,40)); draw.rectangle((10,10,89,29),fill=(110,20,20)); draw.rectangle((110,10,189,89),fill=(40,40,200))
        master.save(self.root/'supplied.png'); (self.root/'cells.txt').write_text('Red leather, then blue cloth.',encoding='utf-8')
        self.asset['texture']={'grid':[2,1],'ground':'#808080','atlas':'model/atlas.png','size':[16,8],'colors':4,
            'regions':{'red':{'cell':[0,0],'rect':[0,0,8,8]},'blue':{'cell':[1,0],'rect':[8,0,4,8]}}}
        self.asset['outputs']=[{'source':'model/atlas.png','target':'textures/item/test.png','format':'png','size':[16,8],'alpha':'opaque'}]
        self.save()

    def test_supplied_master_reduces_to_atlas_regions(self):
        self.textured(); self.command('texture','--prompt','cells.txt','--image','supplied.png')
        asset=art.read(self.folder/'asset.json')
        self.assertEqual(asset['texture']['master'],'master-01.png')
        self.assertEqual(asset['generations'][0]['prompt'],'Red leather, then blue cloth.')
        self.assertNotIn(b'\r',(self.folder/'asset.json').read_bytes())
        with Image.open(self.folder/'model/atlas.png') as atlas:
            self.assertEqual(atlas.size,(16,8)); self.assertLessEqual(len(atlas.getcolors()),4)
            band,red,blue=atlas.getpixel((4,0)),atlas.getpixel((4,6)),atlas.getpixel((10,4))
            self.assertGreater(red[0],150); self.assertLess(band[0],150); self.assertGreater(blue[2],150)
            self.assertIn(atlas.getpixel((14,4)),{atlas.getpixel((x,y)) for x in range(12) for y in range(8)})
        self.assertTrue((self.folder/'review/texture.png').is_file())

    def test_generated_master_states_the_layout_contract(self):
        self.textured(); codex=self.root/'.tools/codex'; codex.mkdir(parents=True)
        (codex/'codex-package.json').write_text('{"entrypoint":"codex.exe"}',encoding='utf-8')
        def draw(command,**kwargs):
            Image.open(self.root/'supplied.png').save(Path(command[command.index('--cd')+1])/'image.png')
        with patch.object(art.setup,'DEST',self.root/'.tools'), patch.object(art.subprocess,'run',draw):
            self.command('texture','--prompt','cells.txt','--references','asset/icon.png')
        prompt=art.read(self.folder/'asset.json')['generations'][0]['prompt']
        self.assertIn('strict 2 by 1 grid',prompt); self.assertIn('#808080',prompt)
        self.assertIn('Row 1, column 2: a panel reduced to 4 by 8 pixels for blue.',prompt); self.assertTrue(prompt.endswith('Red leather, then blue cloth.'))
        self.assertTrue((self.folder/'master-01.png').is_file()); self.assertEqual(list((self.root/'.tools').iterdir()),[codex])
        # Redrawing a referenced image later leaves the generation's kept reference, and so the record, valid.
        Image.new('RGBA',(64,64),'#112233').save(self.folder/'icon.png'); art.definition(art.read(self.folder/'asset.json'),self.folder)
        for kept in (self.root/'art/references').iterdir(): kept.unlink()
        with self.assertRaisesRegex(ValueError,'missing'): art.definition(art.read(self.folder/'asset.json'),self.folder)

    def test_sprite_keeps_its_silhouette_on_transparency(self):
        """A wide blue bar with a hole on a magenta key: proportions kept, the hole and surround transparent, no key tint."""
        master=Image.new('RGB',(400,400),'#ff00ff'); draw=ImageDraw.Draw(master)
        draw.rectangle((40,150,359,249),fill=(40,60,200)); draw.rectangle((180,180,219,219),fill='#ff00ff')
        master.putpixel((0,399),(238,44,235))  # A compression speck in the corner is not the subject touching its edge.
        master.save(self.root/'supplied.png'); (self.root/'cells.txt').write_text('A bar.',encoding='utf-8')
        self.asset['texture']={'grid':[1,1],'ground':'#ff00ff','atlas':'icon.png','size':[64,64],'colors':8,
            'regions':{'bar':{'cell':[0,0],'rect':[4,4,56,56],'sprite':True}}}
        self.save(); self.command('texture','--prompt','cells.txt','--image','supplied.png')
        with Image.open(self.folder/'icon.png') as icon:
            self.assertEqual(icon.mode,'RGBA'); box=icon.getchannel('A').getbbox()
            self.assertEqual((box[0],box[2]),(4,60)); self.assertLess(box[3]-box[1],22)
            self.assertEqual(icon.getpixel((32,32))[3],0); self.assertEqual(icon.getpixel((2,2)),(0,0,0,0))
            colours={c for _,c in icon.getcolors() if c[3]}
            self.assertTrue(all(b>150 and r<100 for r,g,b,a in colours))
        prompt=art.read(self.folder/'asset.json')['generations'][0]['prompt']
        self.assertEqual(prompt,'A bar.')
        self.assertIn('an icon reduced to fit within 56 by 56 pixels for bar',art.master_prompt(self.asset['texture']))
        small=Image.new('RGB',(400,400),'#ff00ff'); ImageDraw.Draw(small).rectangle((20,20,120,60),fill=(40,60,200)); small.save(self.root/'supplied.png')
        with self.assertRaisesRegex(ValueError,'under half'): self.command('texture','--prompt','cells.txt','--image','supplied.png')

    def test_images_share_a_panel_and_differ_by_inset(self):
        """A cold and a lit face take the same door panel; each paints its own window inset over it."""
        master=Image.new('RGB',(300,100),'#ff00ff'); draw=ImageDraw.Draw(master)
        for i,colour in enumerate([(30,30,40),(60,60,70),(240,140,30)]): draw.rectangle((i*100+10,10,i*100+89,89),fill=colour)
        master.save(self.root/'supplied.png'); (self.root/'cells.txt').write_text('Door, cold window, lit window.',encoding='utf-8')
        self.asset['texture']={'grid':[3,1],'ground':'#ff00ff','colors':8,'images':{'front.png':[16,16],'front_on.png':[16,16]},
            'regions':{'door':{'cell':[0,0],'image':'front.png','rect':[0,0,16,16]},'window':{'cell':[1,0],'image':'front.png','rect':[4,10,8,4]},
                       'door_on':{'cell':[0,0],'image':'front_on.png','rect':[0,0,16,16]},'window_on':{'cell':[2,0],'image':'front_on.png','rect':[4,10,8,4]}}}
        self.asset['outputs']=[{'source':'front.png','target':'textures/block/test.png','format':'png','size':[16,16],'alpha':'opaque'}]
        self.save(); self.command('texture','--prompt','cells.txt','--image','supplied.png')
        with Image.open(self.folder/'front.png') as cold, Image.open(self.folder/'front_on.png') as lit:
            changed={(x,y) for x in range(16) for y in range(16) if cold.getpixel((x,y))!=lit.getpixel((x,y))}
            self.assertEqual(changed,{(x,y) for x in range(4,12) for y in range(10,14)})
            self.assertGreater(lit.getpixel((6,11))[0],200); self.assertLess(cold.getpixel((6,11))[0],100)
        self.asset['texture']['regions']['window']['rect']=[12,10,8,4]
        with self.assertRaisesRegex(ValueError,'outside its image'): art.definition(self.asset,self.folder)
        self.asset['texture']['regions']['window']['rect']=[4,10,8,4]; self.asset['texture']['regions']['spill']={'cell':[1,0],'image':'front.png','rect':[0,0,16,16]}
        with self.assertRaisesRegex(ValueError,'overlap'): art.definition(self.asset,self.folder)

    def test_panel_without_margin_is_rejected(self):
        self.textured(); master=Image.open(self.root/'supplied.png'); ImageDraw.Draw(master).rectangle((110,0,189,89),fill=(40,40,200))
        master.save(self.root/'supplied.png')
        with self.assertRaisesRegex(ValueError,'margin'): self.command('texture','--prompt','cells.txt','--image','supplied.png')

    def test_own_output_is_not_a_reference(self):
        target=self.runtime/'textures/item/test.png'; target.parent.mkdir(parents=True); target.write_bytes(b'old sprite')
        self.asset['references'].append({'path':'runtime/textures/item/test.png','role':'previous sprite'})
        with self.assertRaisesRegex(ValueError,'own output'): art.definition(self.asset,self.folder)

    def test_texture_layout_is_checked_once_at_the_boundary(self):
        for change,message in [({'rect':[6,0,4,8]},'overlap'),({'cell':[2,0]},'master grid'),({'rect':[14,0,4,8]},'outside its image')]:
            self.textured(); self.asset['texture']['regions']['blue'].update(change)
            with self.assertRaisesRegex(ValueError,message): art.definition(self.asset,self.folder)
        self.textured(); self.asset['texture']['master']='other.png'
        with self.assertRaisesRegex(ValueError,'recorded'): art.definition(self.asset,self.folder)

    def test_prepare_builds_the_atlas_before_the_recipe(self):
        self.textured(); self.command('texture','--prompt','cells.txt','--image','supplied.png')
        (self.folder/'recipe.py').write_text('import json, pathlib, assets\nfrom PIL import Image\n'
            "regions = json.loads(pathlib.Path('asset.json').read_text(encoding='utf-8'))['texture']['regions']\n"
            "pathlib.Path('recipe-saw').write_text(str(Image.open('model/atlas.png').size) + ' ' + ','.join(regions))\n",encoding='utf-8')
        asset=art.read(self.folder/'asset.json'); asset['source']={'mode':'recipe','path':'recipe.py'}; art.write(self.folder/'asset.json',asset)
        (self.folder/'model/atlas.png').unlink(); self.command('prepare')
        self.assertEqual((self.folder/'recipe-saw').read_text(),'(16, 8) red,blue')
        self.assertIn('textures/item/test.png',art.read(self.folder/'prepare-receipt.json')['outputs'])

    def test_worktrees_share_the_main_checkout(self):
        main=self.root/'main'; main.mkdir()
        git=lambda *args: subprocess.run(['git','-c','user.name=t','-c','user.email=t@example.com',*args],cwd=main,check=True,capture_output=True)
        git('init','-q'); git('commit','-q','--allow-empty','-m','start'); git('worktree','add','-q',str(self.root/'tree'))
        (self.root/'tree/tools').mkdir()
        for path in (main,self.root/'tree/tools'): self.assertEqual(art.setup.main_checkout(path).resolve(),main.resolve())

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
