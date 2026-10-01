"""Map generated panel details to named engine faces at native texture density."""
import importlib.util
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageEnhance

HERE=Path(__file__).resolve().parent
ROOT=next(p for p in HERE.parents if (p/'tools/modeling/studio.py').exists())


def fit_panel(tile,size,border=4):
    """Nine-slice the pixel-art panel: retain corner size as face proportions change."""
    w,h=size;b=min(border,w//3,h//3)
    if b<1:return tile.resize(size,Image.Resampling.NEAREST)
    out=Image.new('RGBA',size)
    sx=[0,b,tile.width-b,tile.width];sy=[0,b,tile.height-b,tile.height]
    dx=[0,b,w-b,w];dy=[0,b,h-b,h]
    for x in range(3):
        for y in range(3):
            patch=tile.crop((sx[x],sy[y],sx[x+1],sy[y+1]))
            out.paste(patch.resize((dx[x+1]-dx[x],dy[y+1]-dy[y]),Image.Resampling.NEAREST),(dx[x],dy[y]))
    return out


def decorate(folder,parts,regions,clay=False):
    from studio import face_dimensions
    spec=importlib.util.spec_from_file_location('sprite_pipeline',ROOT/'art/textures/pipeline.py')
    reducer=importlib.util.module_from_spec(spec);spec.loader.exec_module(reducer)
    source=Image.open(HERE/'surface-master.png').convert('RGBA')
    # Generated block sheet has a uniform near-opaque alpha (238..250), not cutouts.
    # Block surfaces are opaque: reject real holes, then retain authored RGB exactly.
    if source.getchannel('A').getextrema()[0]<230:raise ValueError('Unexpected transparent holes in block master')
    source.putalpha(255);half=source.width//2
    tiles=[reducer.prepare(source.crop((x*half,y*half,(x+1)*half,(y+1)*half)),{'kind':'block','colors':24}) for y in (0,1) for x in (0,1)]
    atlas=Image.new('RGBA',(512,512),(73,77,81,255));atlas.paste(Image.open(folder/'engine_atlas.png'),(0,0))
    px,py,row=0,130,0;receipt=[]
    for p in parts:
        name=p['name'];chosen={}
        if name=='front_cover':chosen={'north':0}
        elif name=='side_cover':chosen={'east':0}
        elif name=='barrel_wide':chosen={f:2 for f in ('east','west')}
        elif name in ('front_foot','rear_foot','case_lid','cabinet_lid','plinth'):chosen={'up':1}
        elif name.startswith('rim_face_') or name.startswith('spoke_'):chosen={f:4 for f in ('west','east')}
        for face,style in chosen.items():
            w,h=[max(1,math.ceil(v*2-1e-5)) for v in face_dimensions(p)[face]]
            if px+w+2>512:px=0;py+=row+2;row=0
            if py+h+2>512:raise ValueError('Surface atlas full')
            if style<4:
                tile=fit_panel(tiles[style],(w,h))
                if style!=1:tile=ImageEnhance.Brightness(tile).enhance(1.15)
            else:
                tile=Image.new('RGBA',(w,h),(90,94,98,255));d=ImageDraw.Draw(tile)
                d.line((0,0,w-1,0),fill=(163,166,165,255));d.line((0,0,0,h-1),fill=(133,139,139,255))
                d.line((0,h-1,w-1,h-1),fill=(48,52,55,255));d.line((w-1,0,w-1,h-1),fill=(61,65,68,255))
                if min(w,h)>4:d.line((2,2,min(w-3,5),2),fill=(179,180,171,255))
            if clay:tile=Image.new('RGBA',(w,h),(150,158,160,255))
            atlas.paste(tile,(px,py));key='surface_'+name+'_'+face
            regions[key]={'rect':[px,py,w,h]};p['faces'][face]=key
            receipt.append({'part':name,'face':face,'style':['access','steel','enamel','support','authored_edge'][style],'rect':[px,py,w,h]})
            px+=w+2;row=max(row,h)
    atlas.save(folder/'engine_atlas.png')
    (folder/'surface-map.json').write_text(json.dumps(receipt,indent=2),encoding='utf-8')
    atlas.resize((1024,1024),Image.Resampling.NEAREST).save(folder/'surface-atlas-review.png')
