"""Engine authoring recipe: native Blockbench projects, one shared geometry definition.

Blockout first; --textured uses the reviewed atlas. Does not publish runtime assets.
"""
import argparse
import base64
import json
import math
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / 'tools/modeling'))
from mcp import Client
from proof import evaluate, export

HERE = Path(__file__).resolve().parent
PARTS = []

def box(group, name, start, end, material='iron', angle=0, axis='x', pivot=None):
    PARTS.append(dict(group=group, name=name, start=start, end=end, material=material,
                      angle=angle, axis=axis, pivot=pivot or [0, 0, 0]))

def geometry():
    # Coordinates use Minecraft's 16 model units per block, controller at (0,0,0).
    for x in (-16, 0, 16):
        for z in (16, 32): box('bed', f'bed_{x}_{z}', [x, 0, z], [x+16, 8, z+16], 'steel')
    box('bed', 'copper_slab', [0,16,32], [16,24,48], 'copper')
    box('controller','plinth',[0,0,0],[16,3,16],'steel')
    box('controller','cabinet',[2,3,2],[14,13,14],'steel')
    box('controller','dial_panel',[3,4,1.8],[13,12,2],'dial')
    box('controller','fuel_flange',[0,5,5],[2,11,11],'brass')
    box('controller','oil_flange',[14,5,5],[16,11,11],'brass')
    box('controller','fuel_mouth',[-.1,6,6],[.1,10,10],'dark')
    box('controller','oil_mouth',[15.9,6,6],[16.1,10,10],'dark')
    # Crankcase owns the bearing; no pedestal intersects the flywheel's swept volume.
    box('cylinder','front_foot',[-1,8,18],[15,11,28],'steel')
    box('cylinder','front_support',[2,11,20],[12,21,26],'iron')
    box('cylinder','rear_foot',[2,8,36],[14,16,43],'steel')
    box('cylinder','crankcase',[0,20,16],[15,37,28],'iron')
    box('cylinder','case_hatch',[1,21,15.8],[14,36,16],'panel')
    box('cylinder','case_lid',[1,37,17],[14,38,27],'steel')
    box('cylinder','bearing_collar',[-2,27,21],[1,33,27],'brass')
    # Horizontal barrel, cylinder axis Z, shaft axis X.
    box('cylinder','barrel',[2,25,28],[14,35,44],'iron')
    box('cylinder','barrel_top',[4,35,28],[12,37,44],'iron')
    for z in (28,41):
        box('cylinder',f'band_{z}',[1.5,24.5,z],[14.5,35.5,z+2],'steel')
        box('cylinder',f'band_top_{z}',[3.5,35.5,z],[12.5,37.5,z+2],'steel')
    box('cylinder','hot_bulb',[5,27,44],[11,33,47],'brass')
    box('cylinder','burner_tip',[6,24,45],[10,27,47],'dark')
    box('cylinder','oiler',[5,38,21],[9,41,25],'brass')
    box('cylinder','exhaust',[10,37,28],[14,45,32],'iron')
    box('cylinder','exhaust_opening',[10.5,45,28.5],[13.5,45.2,31.5],'dark')
    box('shaft','shaft',[-9,28.5,22.5],[0,31.5,25.5],'steel')
    box('wheel','hub',[-13,27,21],[-7,33,27],'iron')
    box('wheel','hub_cap',[-14,28,22],[-13,32,26],'brass')
    box('wheel','spoke_vertical',[-12,17,22.75],[-9,43,25.25],'iron')
    box('wheel','spoke_horizontal',[-12,28.75,11],[-9,31.25,37],'iron')
    for i in range(8):
        angle=i*math.pi/4; cy=30+12.5*math.cos(angle); cz=24+12.5*math.sin(angle)
        # Alternate axis-aligned and 45-degree tangent segments; all legal Java rotations.
        if i%2==0:
            dy,dz=(1.5,5.3) if i%4==0 else (5.3,1.5)
            box('wheel',f'rim_{i}',[-13,cy-dy,cz-dz],[-8,cy+dy,cz+dz],'iron')
        else:
            rotation=45 if i in (1,5) else -45
            box('wheel',f'rim_{i}',[-13,cy-1.5,cz-5.3],[-8,cy+1.5,cz+5.3],
                'iron',rotation,'x',[-10.5,cy,cz])

def create(client, name, pieces, offset, gecko, textured):
    client.call('create_project', {'name':name,'format':'geckolib_model' if gecko else 'java_block'})
    atlas = (HERE/'model/engine_atlas.png') if textured else None
    data = 'data:image/png;base64,'+base64.b64encode(atlas.read_bytes()).decode() if atlas else ''
    script = r'''(() => {
      const cfg = CONFIG;
      Project.name=cfg.name; Project.geometry_name=cfg.name; Project.box_uv=false;
      Project.texture_width=128; Project.texture_height=128;
      if (cfg.gecko) Project.geckolib_model_type='block';
      let data=cfg.texture;
      if (!data) {
        const c=document.createElement('canvas'); c.width=c.height=128; const ctx=c.getContext('2d');
        const colors=['#68787d','#849294','#ab8750','#697b82','#986345','#333b3f'];
        colors.forEach((v,i)=>{ctx.fillStyle=v;ctx.fillRect((i%4)*32,Math.floor(i/4)*32,32,32)});
        data=c.toDataURL();
      }
      const texture=new Texture({name:'engine_atlas.png',folder:'block',namespace:'civilization'}).fromDataURL(data).add(false);
      const groups={};
      for (const part of cfg.pieces) {
        if (!groups[part.group]) groups[part.group]=new Group({name:part.group,origin:part.group==='wheel'?[-10.5-cfg.offset[0],30-cfg.offset[1],24-cfg.offset[2]]:[0,0,0]}).init();
        const sub=v=>v.map((n,i)=>n-cfg.offset[i]);
        const rot=[0,0,0]; rot['xyz'.indexOf(part.axis)]=part.angle;
        const cube=new Cube({name:part.name,from:sub(part.start),to:sub(part.end),origin:sub(part.pivot),rotation:rot,autouv:0,box_uv:false}).addTo(groups[part.group]).init();
        const slots={iron:[0,0],steel:[32,0],brass:[64,0],dial:[96,0],copper:[0,32],dark:[32,32],panel:[32,0]};
        const [u,v]=slots[part.material]; const [x,y,z]=part.end.map((n,i)=>n-part.start[i]);
        const dims={north:[x,y],south:[x,y],east:[z,y],west:[z,y],up:[x,z],down:[x,z]};
        for (const [face,[w,h]] of Object.entries(dims)) {
          cube.faces[face].texture=texture.uuid;
          cube.faces[face].uv=['dial','panel'].includes(part.material)?[u,v,u+32,v+32]:[u,v,u+Math.min(32,w*2),v+Math.min(32,h*2)];
        }
      }
      Canvas.updateAll(); return {cubes:Cube.all.length,bones:Group.all.length};
    })()'''
    config=json.dumps(dict(name=name,pieces=pieces,offset=offset,gecko=gecko,texture=data)).replace('/','\\u002f')
    evaluate(client,script.replace('CONFIG',config))

def save(client, folder, name, gecko):
    (folder/f'{name}.bbmodel').write_text(export(client,'project'),encoding='utf-8')
    data=json.loads(export(client,'bedrock' if gecko else 'java_block'))
    if not gecko:
        data['parent']='minecraft:block/block'
        data['textures']['particle']='civilization:block/engine_atlas'
    (folder/f'{name}{".geo" if gecko else ""}.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    if gecko:
        (folder/f'{name}.animation.json').write_text(json.dumps(evaluate(client,'Animator.buildFile(null, Animation.all.map(a=>a.name))'),indent=2))

def views(client, folder, prefix):
    for name,pos in [('front',[8,32,-85]),('back',[8,32,120]),('side',[-100,32,24]),('isometric',[-75,65,-65])]:
        r=client.call('set_camera_angle',dict(position=pos,target=[4,21,24],projection='orthographic'))
        for b in r.get('content',[]):
            if b['type']=='image': (folder/f'{prefix}-{name}.png').write_bytes(base64.b64decode(b['data']))

def main():
    parser=argparse.ArgumentParser(); parser.add_argument('--textured',action='store_true'); args=parser.parse_args()
    folder=HERE/('model' if args.textured else 'blockout');folder.mkdir(exist_ok=True)
    geometry();client=Client()
    try:
        create(client,'oil_engine_assembly',PARTS,[0,0,0],True,args.textured)
        client.call('create_animation',{'name':'oil_engine.cycle','loop':True,'animation_length':1.5,
            'bones':{'wheel':[{'time':t,'rotation':[t*240,0,0]} for t in [0,.375,.75,1.125,1.5]]}})
        save(client,folder,'assembly',True);views(client,folder,'assembly')
        # Native formats own each export; assembly source remains open and editable.
        if args.textured:
            for name,groups,offset in [('oil_engine',['controller'],[0,0,0]),('engine_cylinder',['cylinder'],[0,16,16]),
                                       ('engine_flywheel',['wheel','shaft'],[-16,16,16]),('engine_flywheel_linked',['shaft'],[-16,16,16])]:
                create(client,name,[p for p in PARTS if p['group'] in groups],offset,False,True)
                save(client,folder,name,False)
            create(client,'oil_engine_wheel',[p for p in PARTS if p['group']=='wheel'],[8,0,8],True,True)
            client.call('create_animation',{'name':'oil_engine_wheel.cycle','loop':True,'animation_length':1.5,
                'bones':{'wheel':[{'time':t,'rotation':[t*240,0,0]} for t in [0,.375,.75,1.125,1.5]]}})
            save(client,folder,'oil_engine_wheel',True)
        print('Native exports:',folder)
    finally: client.close()

if __name__=='__main__':main()

