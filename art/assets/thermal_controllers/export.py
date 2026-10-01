"""Original T1 hardware, pack-aware masonry and protected fire-window variants."""
from pathlib import Path
import json
import sys
from PIL import Image, ImageDraw
HERE=Path(__file__).resolve().parent
sys.path.insert(0,str(HERE.parents[2]/'art/textures'))
from pipeline import prepare
from family_pipeline import palette_for, protected_edit

def tile(file,x,y):
    im=Image.open(HERE/file).convert('RGB').convert('RGBA')
    assert im.width==im.height and im.width%2==0
    n=im.width//2
    return im.crop((x*n,y*n,(x+1)*n,(y+1)*n))

iron=palette_for(tile('plates-master.png',0,0),heat=True,accents=[[231,218,183],[195,160,85],[139,105,51],[83,66,40]])
copper_sample=Image.new('RGBA',(1254,627))
copper_sample.paste(tile('plates-master.png',1,1).resize((627,627)),(0,0))
copper_sample.paste(tile('surfaces-master.png',1,1).resize((627,627)),(627,0))
copper=palette_for(copper_sample,heat=True,accents=[[24,24,25],[49,49,51],[76,76,78],[112,110,108],[169,163,146],[202,158,76]])
faces={}
def face(name,file,x,y,palette):
    faces[name]=prepare(tile(file,x,y),{'kind':'block','colors':32},palette=palette)

for name,x,y in [('kiln',0,0),('foundry',1,0),('smithy',0,1),('works',1,1)]:
    face(name+'_front','plates-master.png',x,y,copper if name=='works' else iron)
for name,x,y in [('stove_front',0,0),('stove_side',1,0),('works_side',1,1)]:
    face(name,'surfaces-master.png',x,y,copper if name=='works_side' else iron)
face('stove_top','stove-top-fixed-master.png',0,1,iron)
for name,base in [('stove_side','stove_front'),('stove_top','stove_front'),('works_side','works_front')]:
    faces[name]=protected_edit(faces[base],faces[name],[[2,2,30,30]])
    for box in ([0,0,4,4],[28,0,32,4],[0,28,4,32],[28,28,32,32]):
        faces[name].paste(faces[base].crop(box),box[:2])
faces['works_top']=faces['works_side'].copy()
for name,x,y,box in [('kiln',0,0,[11,27,21,29]),('foundry',1,0,[11,26,20,28]),('works',1,1,[12,27,20,29])]:
    hot=prepare(tile('plates-hot-master.png',x,y),{'kind':'block','colors':32},palette=copper if name=='works' else iron)
    faces[name+'_front_on']=protected_edit(faces[name+'_front'],hot,[box])
hot=prepare(tile('surfaces-hot-master.png',0,0),{'kind':'block','colors':32},palette=iron)
faces['stove_front_on']=protected_edit(faces['stove_front'],hot,[[7,18,25,26]])

# Shared restrained lift keeps metal readable under Photon without emissive casing.
lut=[round(10+245*(v/255)**.83) for v in range(256)]
for name,im in faces.items():
    faces[name]=im.convert('RGB').point(lut*3).convert('RGBA')
    faces[name].save(HERE/(name+'.png'))

def save(name,model):(HERE/(name+'.json')).write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
def masonry(name,material,door,box):
    x1,y1,x2,y2=box
    return {'parent':'minecraft:block/block','textures':{'particle':material,'body':material,'door':'civilization:block/'+door},'elements':[
        {'from':[0,0,0],'to':[16,16,16],'faces':{d:{'texture':'#body','cullface':d} for d in ['north','south','east','west','up','down']}},
        {'from':[x1,y1,-.02],'to':[x2,y2,-.02],'faces':{'north':{'texture':'#door','uv':[0,0,16,16],'cullface':'north'}}}]}
for hot in [False,True]:
    suffix='_on' if hot else ''
    save('brick_kiln'+suffix,masonry('kiln','minecraft:block/cobblestone','kiln_front'+suffix,[2,1,14,15]))
    save('foundry'+suffix,masonry('foundry','minecraft:block/bricks','foundry_front'+suffix,[2,1,14,15]))
    for model,prefix in [('cooking_station','stove'),('fertilizer_retort','works')]:
        save(model+suffix,{'parent':'minecraft:block/orientable','textures':{'front':'civilization:block/'+prefix+'_front'+suffix,'side':'civilization:block/'+prefix+'_side','top':'civilization:block/'+prefix+'_top'}})
save('smithy',masonry('smithy','minecraft:block/bricks','smithy_front',[2,1,14,14]))

names=list(faces)
sheet=Image.new('RGB',(800,((len(names)+3)//4)*246),'#292b2e');draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    x=i%4*200;y=i//4*246
    draw.text((x+8,y+5),name,fill='white');sheet.paste(faces[name].resize((192,192),Image.Resampling.NEAREST),(x+4,y+22));sheet.paste(faces[name],(x+8,y+216))
sheet.save(HERE/'texture-review.png')
