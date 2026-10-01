"""Reduce the inspected four-cell generated master with the shared sprite pipeline."""
import hashlib
import importlib.util
import json
from pathlib import Path
from PIL import Image, ImageEnhance

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
spec=importlib.util.spec_from_file_location('sprite_pipeline',ROOT/'art/textures/pipeline.py')
pipeline=importlib.util.module_from_spec(spec);spec.loader.exec_module(pipeline)
master=Image.open(HERE/'texture-master.png').convert('RGBA')
half=master.width//2
boxes=[(0,0,half,half),(half,0,master.width,half),(0,half,half,master.height),(half,half,master.width,master.height)]
tiles=[]
for box in boxes:
    crop=master.crop(box)
    if crop.width!=crop.height: raise ValueError('Expected square master quadrants')
    tiles.append(pipeline.prepare(crop,{'kind':'block','colors':32}))
folder=HERE/'model';folder.mkdir(exist_ok=True)
atlas=Image.new('RGBA',(128,128),(87,100,106,255))
for i,tile in enumerate(tiles):atlas.paste(tile,(i*32,0))
# Assembly casing uses the same project surface as the real construction blocks.
atlas.paste(Image.open(ROOT/'civilization-mod/src/main/resources/assets/civilization/textures/block/refinery_side.png').convert('RGBA'),(0,32))
dark=ImageEnhance.Brightness(tiles[0]).enhance(.45);atlas.paste(dark,(32,32))
# Plain machined steel must never sample rivets from the access-panel tile.
atlas.paste(ImageEnhance.Brightness(tiles[0]).enhance(1.15),(64,32))
atlas.save(folder/'engine_atlas.png')
atlas.resize((768,768),Image.Resampling.NEAREST).save(HERE/'texture-review.png')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
receipt={'master':'texture-master.png','sha256':sha(HERE/'texture-master.png'),'prompt':(HERE/'texture-prompt-03.txt').read_text(),
 'original':'C:/Users/admin/.codex/generated_images/01a08a58-4280-7292-9a4b-a14f940d3cfc/exec-3519f699-b634-466a-9511-c1d3c108aa82.png',
 'references':{'texture-master-02.png':sha(HERE/'texture-master-02.png')},'source_size':list(master.size),'crops':boxes,
 'export':'model/engine_atlas.png','export_sha256':sha(folder/'engine_atlas.png'),'tile_size':32,
 'review':'Third referenced master gives quieter medium blue-gray iron; plain steel has a dedicated derived tile. Assembly bed uses actual refinery casing texture; copper slab removed.'}
(HERE/'texture-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
print('Prepared atlas',atlas.size)
