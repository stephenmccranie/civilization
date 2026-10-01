"""Shared engine metals plus one referenced label family, downscaled through the existing pipeline."""
import hashlib
import importlib.util
import json
from pathlib import Path
from PIL import Image, ImageEnhance

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
spec=importlib.util.spec_from_file_location('sprite_pipeline',ROOT/'art/textures/pipeline.py')
pipeline=importlib.util.module_from_spec(spec);spec.loader.exec_module(pipeline)
def tiles(path):
    image=Image.open(path).convert('RGBA');w,h=image.size
    crops=[(0,0,w//2,h//2),(w//2,0,w,h//2),(0,h//2,w//2,h),(w//2,h//2,w,h)]
    return [pipeline.prepare(image.crop(crop),{'kind':'block','colors':32}) for crop in crops],crops
metal_path=HERE/'metal-master.png';label_path=HERE/'label-master.png'
metals,_=tiles(metal_path);labels,crops=tiles(label_path)
atlas=Image.new('RGBA',(128,128),(78,91,101,255))
for i,tile in enumerate([metals[0],ImageEnhance.Brightness(metals[0]).enhance(1.15),metals[2],ImageEnhance.Brightness(metals[0]).enhance(.45)]):atlas.paste(tile,(i*32,0))
for i,tile in enumerate(labels):atlas.paste(tile,(i*32,64))
folder=HERE/'model';folder.mkdir(exist_ok=True);atlas.save(folder/'canisters.png')
atlas.resize((768,768),Image.Resampling.NEAREST).save(HERE/'texture-review.png')
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
receipt={'label_master':'label-master.png','label_hash':sha(label_path),'prompt':(HERE/'label-prompt.txt').read_text(encoding='utf8'),
 'original':'C:/Users/admin/.codex/generated_images/01a08a58-4280-7292-9a4b-a14f940d3cfc/exec-745e969a-ef7f-41e0-a343-624dde5eede5.png',
 'references':{'mockup-01.png':sha(HERE/'mockup-01.png')},'crops':crops,'tile_size':32,
 'metal_master':'metal-master.png','metal_hash':sha(metal_path),'metal_provenance':'../oil_engine/texture-receipt.json',
 'export':'model/canisters.png','export_hash':sha(folder/'canisters.png')}
(HERE/'texture-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf8')
print('Prepared canister atlas',atlas.size)
