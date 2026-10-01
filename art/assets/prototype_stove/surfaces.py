"""Measured stove face islands: construction accents over quiet generated metal.

Geometry stays in build.py. Every ordinary face retains four pixels per model unit.
Small authored accents go through the shared explicit-pixels palette exporter.
"""
from pathlib import Path
import hashlib,json,math
from PIL import Image,ImageDraw
from textures import prepare
from studio import face_dimensions
HERE=Path(__file__).resolve().parent

def decorate(parts):
    spec=json.loads((HERE/'surface-spec.json').read_text(encoding='utf-8'))
    palette={k:tuple(v) for k,v in spec['palette'].items()};density=spec['density']
    atlas=Image.open(HERE/'prototype_stove_iron.png').convert('RGBA')
    tiles={name:atlas.crop((i*64,0,(i+1)*64,64)) for i,name in enumerate(['iron','plain','steel','brass','vent'])}
    regions={name:{'rect':[i*64,0,64,64],**({'fit':True} if name=='vent' else {})} for i,name in enumerate(tiles)}
    records=[];bindings=[];px=0;py=68;row=0

    def island(name,base,w,h,marks):
        nonlocal px,py,row
        if px+w+2>atlas.width:px=0;py+=row+2;row=0
        if py+h+2>atlas.height:raise ValueError('Stove face atlas is full')
        # Native pixel corrections are applied to a 64px canvas by the shared exporter.
        canvas=Image.new('RGBA',(64,64));d=ImageDraw.Draw(canvas)
        marks(d,w,h)
        edits=[[x,y,list(canvas.getpixel((x,y)))] for y in range(h) for x in range(w) if canvas.getpixel((x,y))[3]]
        exported=prepare(tiles[base],{'kind':'block','colors':32,'palette_colors':20,'pixels':edits},size=64)
        atlas.paste(exported.crop((0,0,w,h)),(px,py));regions[name]={'rect':[px,py,w,h]}
        records.append({'name':name,'base':base,'rect':[px,py,w,h],'pixels':edits})
        px+=w+2;row=max(row,h);return name

    def border(d,w,h,inset=0):
        a=inset;b=w-1-inset;c=h-1-inset
        d.line((a,a,b,a),fill=palette['edge']);d.line((a,a,a,c),fill=palette['plate'])
        d.line((a,c,b,c),fill=palette['shade']);d.line((b,a,b,c),fill=palette['seam'])

    def stud(d,x,y):
        d.rectangle((x,y,x+1,y+1),fill=palette['stud']);d.point((x+1,y+1),fill=palette['stud_shadow'])

    def housing(d,w,h,front=False):
        border(d,w,h);d.rectangle((2,2,w-3,h-3),outline=palette['seam'])
        for x in [4,w-6]:
            for y in [4,h-6]:stud(d,x,y)
        if front:
            # Coordinates are front-view texels: U increases toward the cook's right.
            x,y,X,Y=spec['vent_surround'];d.rectangle((x,y,X,Y),fill=palette['plate'])
            d.rectangle((x,y,X,Y),outline=palette['shade']);d.line((x+1,y+1,X-1,y+1),fill=palette['edge'])
            for left in [8,21]:
                for top in [9,14]:
                    d.rectangle((left-1,top-1,left+10,top+2),outline=palette['shade'])
                    d.line((left,top-1,left+9,top-1),fill=palette['edge'])
        # A few interrupted rubbed edges, never a global noise pass.
        d.line((9,0,15,0),fill=palette['rub']);d.line((w-16,0,w-12,0),fill=palette['rub'])

    def cooktop(d,w,h):
        border(d,w,h,1)
        for x in [4,w-6]:
            for y in [4,h-6]:stud(d,x,y)
        d.line((12,1,20,1),fill=palette['rub']);d.line((w-2,20,w-2,26),fill=palette['edge'])

    def well(d,w,h):
        # Narrow visible seasoned lip; quieter center keeps the food cues readable.
        border(d,w,h);d.rectangle((2,2,w-3,h-3),outline=palette['shade'])
        d.line((7,h-5,12,h-5),fill=palette['seam']);d.line((w-10,5,w-6,5),fill=palette['seam'])

    def cookware(d,w,h):
        if h>w:
            d.line((0,0,0,h-1),fill=palette['edge']);d.line((w-1,0,w-1,h-1),fill=palette['shade'])
            if h>8:d.line((0,3,0,7),fill=palette['rub'])
        else:
            d.line((0,0,w-1,0),fill=palette['edge']);d.line((0,h-1,w-1,h-1),fill=palette['shade'])
            if w>8:d.line((3,0,7,0),fill=palette['rub'])

    def support(d,w,h):
        if h>w:
            d.line((0,0,0,h-1),fill=palette['plate']);d.line((w-1,0,w-1,h-1),fill=palette['shade'])
            d.line((0,5,0,min(h-2,11)),fill=palette['edge'])
            if w>=5 and h>16:stud(d,2,2)
        else:
            d.line((0,0,w-1,0),fill=palette['edge']);d.line((0,h-1,w-1,h-1),fill=palette['shade'])
            if w>12:d.line((4,0,10,0),fill=palette['rub'])

    # A coherent 12px dial face is cropped onto the two overlapping main castings.
    dial=Image.new('RGBA',(12,12),palette['seam']);d=ImageDraw.Draw(dial)
    d.ellipse((0,0,11,11),fill=palette['black'])
    for angle in spec['dial_tick_degrees']:
        r=math.radians(angle)
        d.line((round(5.5+4.2*math.sin(r)),round(5.5-4.2*math.cos(r)),round(5.5+5.2*math.sin(r)),round(5.5-5.2*math.cos(r))),fill=palette['stud'])

    for part in parts:
        name=part['name'];selected={}
        if name=='heating_head':selected={face:'front' if face=='north' else 'housing' for face in ['north','south','east','west']}
        elif name=='cooktop':selected={'up':'cooktop','north':'support','south':'support','east':'support','west':'support'}
        elif name=='pan_base':selected={'up':'well'}
        elif name.startswith('rim_'):selected={face:'cookware' for face in ['north','south','east','west','up']}
        elif name=='handle':selected={face:'cookware' for face in ['north','south','east','west','up']}
        elif name.startswith(('leg_','brace_','foot_')) or name=='head_lower_lip':selected={face:'support' for face in ['north','south','east','west']}
        elif name in ['dial_wide','dial_tall']:selected={'north':'dial'}
        for face,style in selected.items():
            w,h=[max(1,math.ceil(v*density-1e-5)) for v in face_dimensions(part)[face]]
            if style=='dial':
                left=round((5-part['end'][0])*density);top=round((13-part['end'][1])*density)
                crop=dial.crop((left,top,left+w,top+h))
                marks=lambda d,w,h,crop=crop:[d.point((x,y),fill=crop.getpixel((x,y))) for y in range(h) for x in range(w)]
            else:
                marks={'front':lambda d,w,h:housing(d,w,h,True),'housing':housing,'cooktop':cooktop,'well':well,'cookware':cookware,'support':support}[style]
            base='iron' if style in ['housing','front'] else 'plain' if style in ['well','cookware','dial'] else 'steel'
            key=f'{style}_{base}_{w}x{h}'
            if key not in regions:island(key,base,w,h,marks)
            part['faces'][face]=key
            bindings.append({'part':name,'face':face,'style':style,'island':key})
    atlas.save(HERE/'prototype_stove_iron.png')
    (HERE/'surface-map.json').write_text(json.dumps({'islands':records,'faces':bindings},separators=(',',':'))+'\n',encoding='utf-8')
    atlas.resize((1024,512),Image.Resampling.NEAREST).save(HERE/'evidence/surface-atlas.png')
    def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
    receipt=json.loads((HERE/'texture-receipt.json').read_text(encoding='utf-8'))
    receipt['method']+=' Face-specific fitted seams, fastening, vent, dial and cookware accents use measured native islands and explicit-pixels recipes.'
    receipt['surface_map']=sha(HERE/'surface-map.json');receipt['surface_spec']=sha(HERE/'surface-spec.json')
    receipt['outputs']['prototype_stove_iron.png']=sha(HERE/'prototype_stove_iron.png')
    (HERE/'texture-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8')
    return regions
