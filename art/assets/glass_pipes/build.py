"""Native eight-sided sight-glass tube; static holes stay clear under shaders."""
from pathlib import Path
import json
import math
from PIL import Image

HERE = Path(__file__).resolve().parent
A = 4 * (math.sqrt(2) - 1)

def box(lo, hi, tex='steel', sides=('north','south','east','west','up','down'), rotation=None):
    result = {'from': lo, 'to': hi, 'faces': {d: {'texture': '#'+tex, 'uv': [0,0,16,16]} for d in sides}}
    if rotation:
        result['rotation'] = {'origin':[8,8,8], 'axis':'z', 'angle':rotation}
    return result

def tube(z0, z1, glass=True):
    result = []
    # Four 1.25-pixel inspection slits: about 19% of the octagonal perimeter.
    # Solid shoulders and bevels make the remaining surface a sturdy steel shell.
    width = .625 if glass else A
    tex = 'glass' if glass else 'steel'
    for y in (4,12):
        result.append(box([8-width,y-.01,z0],[8+width,y+.01,z1],tex,('up','down')))
        if glass:
            lo,hi=(4,4.25) if y==4 else (11.75,12)
            for a,b in ((8-A,8-width),(8+width,8+A)):
                result.append(box([a,lo,z0],[b,hi,z1]))
    for x in (4,12):
        result.append(box([x-.01,8-width,z0],[x+.01,8+width,z1],tex,('east','west')))
        if glass:
            lo,hi=(4,4.25) if x==4 else (11.75,12)
            for a,b in ((8-A,8-width),(8+width,8+A)):
                result.append(box([lo,a,z0],[hi,b,z1]))
    for y in (4,11.75):
        for angle in (-45,45):
            result.append(box([8-A,y,z0],[8+A,y+.25,z1],rotation=angle))
    return result

def model(name, elements):
    data = {'parent':'minecraft:block/block', 'render_type':'minecraft:cutout',
        'ambientocclusion':False,
        'textures':{'steel':'civilization:block/pipe_steel','glass':'civilization:block/pipe_glass',
                    'particle':'civilization:block/pipe_steel'}, 'elements':elements}
    (HERE / (name+'.json')).write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')

# Reuse the approved master, sampling its quiet center instead of repeating a
# whole block border on each narrow rib. No new independent texture style.
steel = Image.open(HERE/'steel-source.png').convert('RGBA')
steel.crop((8,8,24,24)).resize((32,32),Image.Resampling.NEAREST).save(HERE/'pipe_steel.png')
# Code-native glass edge glints: sparse intentional pixels, with real alpha-zero
# windows. This is a generic optical treatment, not generated painted glass.
glass = Image.new('RGBA',(32,32),(0,0,0,0))
for x,y in [(1,4),(1,5),(1,6),(2,7),(1,17),(1,18),(2,19),(30,11),(30,12),(29,13)]:
    glass.putpixel((x,y),(173,197,200,255))
glass.save(HERE/'pipe_glass.png')
model('fluid_pipe_arm',tube(0,8)+tube(0,.6,False))
model('fluid_pipe_open',tube(0,16)+tube(0,.6,False)+tube(15.4,16,False))
model('fluid_pipe_fitting',[box([4,4,4],[12,12,12])])
model('fluid_pipe_end',tube(7.4,8,False))
model('fluid_pipe_item',tube(0,16)+tube(0,.6,False)+tube(15.4,16,False))
directions = {'north':{},'east':{'y':90},'south':{'y':180},'west':{'y':270},'up':{'x':270},'down':{'x':90}}
axes = [('north','south'),('east','west'),('up','down')]
bends = [{a:'true',b:'true'} for i,axis in enumerate(axes) for other in axes[i+1:] for a in axis for b in other]
parts = [{'when':{d:'false' for d in directions},'apply':{'model':'civilization:block/fluid_pipe_open'}}]
parts += [{'when':{'OR':bends},'apply':{'model':'civilization:block/fluid_pipe_fitting'}}]
parts += [{'when':{d:'true'},'apply':{'model':'civilization:block/fluid_pipe_arm',**rotation}} for d,rotation in directions.items()]
parts += [{'when':{other:'true' if other==d else 'false' for other in directions},
           'apply':{'model':'civilization:block/fluid_pipe_end',**rotation}} for d,rotation in directions.items()]
(HERE/'fluid_pipe.json').write_text(json.dumps({'multipart':parts},indent=2)+'\n')
(HERE/'item.json').write_text(json.dumps({'parent':'civilization:block/fluid_pipe_item'},indent=2)+'\n')

assetfile=HERE/'asset.json'
asset=json.loads(assetfile.read_text(encoding='utf-8'))
asset['sources']=['build.py','preview.py','pipe.bbmodel','steel-source.png','concept-prompt.txt','steel-shell-prompt.txt']
asset['outputs']=[{'source':n+'.json','target':'models/block/'+n+'.json','format':'java_model'} for n in ('fluid_pipe_arm','fluid_pipe_open','fluid_pipe_fitting','fluid_pipe_end','fluid_pipe_item')]
asset['outputs'] += [{'source':n+'.png','target':'textures/block/'+n+'.png','format':'png','size':[32,32],'alpha':'transparent' if n=='pipe_glass' else 'opaque'} for n in ('pipe_steel','pipe_glass')]
asset['outputs'] += [{'source':'fluid_pipe.json','target':'blockstates/fluid_pipe.json','format':'json'}, {'source':'item.json','target':'models/item/fluid_pipe.json','format':'json'}]
assetfile.write_text(json.dumps(asset,indent=2)+'\n',encoding='utf-8')
print('Prepared octagonal pipe: eight-pixel bore envelope, real glass holes, four steel bevels.')
