"""Civic cabinet atlas reduction using the shared native block-face pipeline."""
from pathlib import Path
import json
import sys
from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(ROOT / 'art/textures'))
from pipeline import prepare
from family_pipeline import palette_for, protected_edit

faces = {}
for block, source in [('land_controller', 'land-master.png'), ('trade_counter', 'trade-master.png')]:
    atlas = Image.open(HERE / source).convert('RGB').convert('RGBA')
    assert atlas.width == atlas.height and atlas.width % 2 == 0
    n = atlas.width // 2
    master = atlas.crop((0, 0, n, n))
    accents = [[48,46,45],[78,76,72],[113,109,101],[157,151,136]]
    if block == "land_controller": accents += [[160,60,31],[192,77,38],[110,129,57],[126,146,66]]
    palette = palette_for(atlas, accents=accents)
    front = prepare(master, {'kind':'block', 'colors':24}, palette=palette)
    for face, x, y in [('front',0,0),('side',1,0),('top',0,1)]:
        tile = atlas.crop((x*n,y*n,(x+1)*n,(y+1)*n))
        result = prepare(tile, {'kind':'block', 'colors':24}, palette=palette)
        if face != 'front':
            result = protected_edit(front, result, [[2,2,30,30]])
            for box in ([0,0,5,5],[27,0,32,5],[0,27,5,32],[27,27,32,32]):
                result.paste(front.crop(box),box[:2])
        name = block+'_'+face
        result.save(HERE / (name+'.png'))
        faces[name] = result
    model = {'parent':'minecraft:block/cube','textures':{d:'civilization:block/'+block+'_'+face for d,face in [('particle','side'),('down','side'),('up','top'),('north','front'),('south','side'),('east','side'),('west','side')]}}
    if block == 'trade_counter':
        # Real one-pixel-deep tray, entirely within the existing cube collision.
        textures=model['textures']
        model={'parent':'minecraft:block/block','textures':textures,'elements':[]}
        for lo,hi in [([0,0,0],[16,15,16]),([0,15,0],[16,16,1]),([0,15,15],[16,16,16]),([0,15,1],[1,16,15]),([15,15,1],[16,16,15]),([7.5,15,1],[8.5,16,15])]:
            model['elements'].append({'from':lo,'to':hi,'faces':{d:{'texture':'#'+d} for d in ['north','south','east','west','up','down']}})
    (HERE / (block+'.json')).write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')

sheet=Image.new('RGB',(768,620),'#292b2e');draw=ImageDraw.Draw(sheet)
for row,block in enumerate(['land_controller','trade_counter']):
    for col,face in enumerate(['front','side','top']):
        im=faces[block+'_'+face];x=col*256;y=row*310
        draw.text((x+12,y+8),block+' '+face,fill='white')
        sheet.paste(im.resize((224,224),Image.Resampling.NEAREST),(x+12,y+30))
        sheet.paste(im,(x+12,y+266))
sheet.save(HERE/'texture-review.png')
