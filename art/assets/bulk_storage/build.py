"""Native controller models and full cut-block assemblies; reuse the refinery material family."""
from pathlib import Path
import sys,json
from PIL import Image,ImageEnhance
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
sys.path.insert(0,str(ROOT/'tools/modeling'))
from mcp import Client
from studio import Model,create,save,capture,contact_sheet
atlas=Image.new('RGBA',(128,128));side=Image.open(ROOT/'art/assets/industrial_steel/refinery_side.png').convert('RGBA')
side=side.resize((32,32),Image.Resampling.NEAREST)
regions={}
for i,(name,factor) in enumerate([('steel',1),('dark',.48),('rim',1.3),('brass',1)]):
    tile=ImageEnhance.Brightness(side).enhance(factor)
    if name=='brass':
        tile=tile.convert('L').convert('RGBA');px=tile.load()
        for y in range(32):
            for x in range(32):
                v=px[x,y][0];px[x,y]=(min(255,int(v*1.65)),min(255,int(v*1.18)),int(v*.48),255)
    for y in range(0,64,32):
        for x in range(0,64,32):atlas.paste(tile,((i%2)*64+x,(i//2)*64+y))
    regions[name]={'rect':[(i%2)*64,(i//2)*64,64,64]}
atlas.save(HERE/'bulk_steel.png')
client=Client();outputs=[];sources=['build.py','bulk_steel.png'];views=[]
def box(m,n,a,b,mat='steel'):m.box('shell',n,a,b,mat)
for liquid,name in [(False,'coal_bunker'),(True,'cargo_tank')]:
    m=Model();box(m,'controller casting',[0,0,0],[16,16,16])
    box(m,'fitting plate',[3,3,-.5],[13,13,0],'dark')
    if liquid:
        m.casting('shell','brass socket',8,8,-2.5,-.51,3,1,'brass')
        box(m,'open socket',[6.5,6.5,-2.6],[9.5,9.5,-2.51],'dark')
    else:
        box(m,'loading recess',[4,4,-.6],[12,11,-.51],'dark')
        for i,(a,b) in enumerate([([3.5,3.5,-1],[4.5,11.5,-.6]),([11.5,3.5,-1],[12.5,11.5,-.6]),([4.5,10.5,-1],[11.5,11.5,-.6]),([4.5,3.5,-1],[11.5,4.5,-.6])]):box(m,'hatch edge '+str(i),a,b,'rim')
        box(m,'latch',[7,12,-1],[9,14,-.51],'brass')
    for x in [3.5,11.5]:box(m,'fastener '+str(x),[x,3.5,-.65],[x+1,4.5,-.51],'rim')
    create(client,name,m.parts,HERE/'bulk_steel.png',regions,gecko=False);save(client,HERE,name,False)
    sources.append(name+'.bbmodel');outputs.extend([{'source':name+'.json','target':'models/block/'+name+'.json','format':'java_model'}])
    for folder,data in [('blockstates',{'variants':{'facing='+d:{'model':'civilization:block/'+name,'y':r} for d,r in [('north',0),('east',90),('south',180),('west',270)]}}),('models/item',{'parent':'civilization:block/'+name})]:
        fn=name+('-state' if folder=='blockstates' else '-item')+'.json';(HERE/fn).write_text(json.dumps(data,indent=2),encoding='utf-8');outputs.append({'source':fn,'target':folder+'/'+name+'.json','format':'json'})
    for y in range(3):
        for z in range(5):
            for x in range(-1,2):
                if (x,y,z)==(0,0,0):continue
                a=[x*16,y*16,z*16];b=[v+16 for v in a];mat='steel'
                if not liquid:
                    if y==0:
                        if x==0 and 0<z<4:b[1]-=8
                    elif x==0 and 0<z<4:continue
                    elif y==2:
                        b[1]-=8
                        if not (x!=0 and z in [0,4]):
                            if x<0:b[0]-=8
                            elif x>0:a[0]+=8
                            if z==0:b[2]-=8
                            elif z==4:a[2]+=8
                    elif x and 0<z<4:
                        if x<0:b[0]-=8
                        else:a[0]+=8
                    elif x==0:
                        if z==0:b[2]-=8
                        else:a[2]+=8
                else:
                    if y==1:
                        if x==0 and 0<z<4:continue
                        elif x==0 and z==0:
                            # Window rendered as empty border; vanilla glass is pack-dependent in game.
                            for n,(wa,wb) in enumerate([([0,16,0],[1,32,16]),([15,16,0],[16,32,16]),([1,16,0],[15,17,16]),([1,31,0],[15,32,16])]):box(m,'window '+str(n),wa,wb,'rim')
                            continue
                    else:
                        if y==0:a[1]+=8
                        else:b[1]-=8
                        if x<0:a[0]+=8
                        elif x>0:b[0]-=8
                        if y==0 and z in [1,3]:a=[x*16,0,z*16];b=[x*16+16,16,z*16+16]
                box(m,f'shell {x} {y} {z}',a,b,mat)
    create(client,name+'_assembly',m.parts,HERE/'bulk_steel.png',regions);save(client,HERE,name+'_assembly')
    sources.append(name+'_assembly.bbmodel')
    for suffix,cam in [('front',[-90,80,-110]),('back',[100,75,160])]:
        path=HERE/(name+'-'+suffix+'.png');capture(client,path,cam,[8,18,38],110);views.append(path)
outputs.append({'source':'bulk_steel.png','target':'textures/block/bulk_steel.png','format':'png','size':[128,128],'alpha':'opaque'})
contact_sheet(views,HERE/'model-review.png')
p=HERE/'asset.json';asset=json.loads(p.read_text(encoding='utf-8'));asset['sources']=sources;asset['outputs']=outputs;p.write_text(json.dumps(asset,indent=2),encoding='utf-8')
print('Saved native controller models and complete assembly views.')
