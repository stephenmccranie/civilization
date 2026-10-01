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


def casting(group, name, u, v, start, end, radius, bevel, material='iron', axis='z'):
    """Octagonal prism, decomposed into overlapping legal Java cuboids.

    The four corner strips fill bevel triangles without projecting beyond the
    octagon. Useful for castings/flanges; no smooth mesh or renderer required.
    """
    a=radius-bevel
    point=lambda x,y,z: [x,y,z] if axis=='z' else [z,x,y]
    for suffix, dx, dy in [('wide',radius,a),('tall',a,radius)]:
        box(group,name+'_'+suffix,point(u-dx,v-dy,start),point(u+dx,v+dy,end),material)
    c=(radius+3*a)/4
    length=bevel*math.sqrt(2); thickness=bevel/math.sqrt(2)
    for sx,sy in [(-1,-1),(-1,1),(1,-1),(1,1)]:
        cx=u+sx*c;cy=v+sy*c
        box(group,f'{name}_bevel_{sx}_{sy}',point(cx-length/2,cy-thickness/2,start),
            point(cx+length/2,cy+thickness/2,end),material,-45 if sx*sy>0 else 45,
            axis,point(cx,cy,(start+end)/2))

def geometry():
    # Coordinates use Minecraft's 16 model units per block, controller at (0,0,0).
    for x in (-16, 0, 16):
        for z in (16, 32): box('bed', f'bed_{x}_{z}', [x, 0, z], [x+16, 8, z+16], 'casing')
    box('controller','plinth',[0,0,0],[16,3,16],'steel')
    box('controller','cabinet',[2,3,2],[14,13,14],'steel')
    box('controller','dial_panel',[3,4,1.8],[13,12,2],'dial')
    box('controller','fuel_flange',[0,5,5],[2,11,11],'brass')
    box('controller','oil_flange',[14,5,5],[16,11,11],'brass')
    box('controller','fuel_mouth',[-.1,6,6],[.1,10,10],'dark')
    box('controller','oil_mouth',[15.9,6,6],[16.1,10,10],'dark')
    # Crankcase owns the bearing; no pedestal intersects the flywheel's swept volume.
    box('cylinder','front_foot',[0,8,17],[16,11,27],'steel')
    box('cylinder','front_support',[2,11,18],[14,18,25],'iron')
    box('cylinder','front_ankle',[1.5,11,17.5],[14.5,12,25.5],'brass')
    box('cylinder','rear_foot',[0,8,38],[16,11,47],'steel')
    box('cylinder','rear_support',[3,11,39],[13,20,45],'iron')
    box('cylinder','rear_ankle',[2.5,11,38.5],[13.5,12,45.5],'brass')
    casting('cylinder','crankcase',8,25,16,27,8,2)
    box('cylinder','case_hatch',[2,19,15.4],[14,31,16],'panel')
    box('cylinder','case_lid',[2,33,17],[14,34,25],'steel')
    casting('cylinder','bearing_housing',25,22,-2,1,4,1,'steel','x')
    casting('cylinder','bearing_collar',25,22,-3,-2,3,1,'brass','x')
    # Horizontal barrel, cylinder axis Z, shaft axis X.
    casting('cylinder','barrel',8,25,26,44,6,1.75)
    for z in (27,41):
        casting('cylinder',f'flange_{z}',8,25,z,z+2,6.75,2,'steel')
    casting('cylinder','end_cap',8,25,43,44.5,5,1.5,'iron')
    casting('cylinder','hot_bulb',8,25,44.5,46,3,1,'brass')
    box('cylinder','burner_tip',[6.5,19,45],[9.5,24,47],'dark')
    box('cylinder','burner_nozzle',[7,23,44.5],[9,24,46],'brass')
    box('cylinder','oiler_stem',[5,31,31],[8,33,34],'iron')
    box('cylinder','oiler_cup',[4.5,33,30.5],[8.5,34.5,34.5],'brass')
    box('cylinder','oiler_cap',[5.5,34.5,31.5],[7.5,35.5,33.5],'brass')
    box('cylinder','exhaust',[9,31,37],[13,40,41],'iron')
    box('cylinder','stack_collar',[8.75,37,36.75],[13.25,38,41.25],'brass')
    box('cylinder','exhaust_lip',[8.75,40,36.75],[13.25,40.75,41.25],'steel')
    box('cylinder','exhaust_opening',[9.5,40.76,37.5],[12.5,40.8,40.5],'dark')
    box('shaft','shaft',[-9,23.5,20.5],[0,26.5,23.5],'steel')
    casting('wheel','hub',25,22,-13,-7,3.5,1,'iron','x')
    casting('wheel','hub_cap',25,22,-14,-13,2.5,.75,'brass','x')
    box('wheel','hub_center',[-14.1,24,21],[-14,26,23],'dark')
    box('wheel','spoke_vertical',[-12,12,20.75],[-9,38,23.25],'iron')
    box('wheel','spoke_horizontal',[-12,23.75,9],[-9,26.25,35],'iron')
    for i in range(8):
        angle=i*math.pi/4; cy=25+12.5*math.cos(angle); cz=22+12.5*math.sin(angle)
        # Alternate axis-aligned and 45-degree tangent segments; all legal Java rotations.
        if i%2==0:
            dy,dz=(1.5,5.798989873) if i%4==0 else (5.798989873,1.5)
            box('wheel',f'rim_{i}',[-13,cy-dy,cz-dz],[-8,cy+dy,cz+dz],'iron')
        else:
            rotation=45 if i in (1,5) else -45
            box('wheel',f'rim_{i}',[-13,cy-1.5,cz-5.798989873],[-8,cy+1.5,cz+5.798989873],
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
        const colors=['#68787d','#849294','#ab8750','#697b82','#647978','#333b3f','#849294'];
        colors.forEach((v,i)=>{ctx.fillStyle=v;ctx.fillRect((i%4)*32,Math.floor(i/4)*32,32,32)});
        data=c.toDataURL();
      }
      const texture=new Texture({name:'engine_atlas.png',folder:'block',namespace:'civilization'}).fromDataURL(data).add(false);
      const groups={};
      for (const part of cfg.pieces) {
        if (!groups[part.group]) groups[part.group]=new Group({name:part.group,origin:part.group==='wheel'?[-10.5-cfg.offset[0],25-cfg.offset[1],22-cfg.offset[2]]:[0,0,0]}).init();
        const sub=v=>v.map((n,i)=>n-cfg.offset[i]);
        const rot=[0,0,0]; rot['xyz'.indexOf(part.axis)]=part.angle;
        const cube=new Cube({name:part.name,from:sub(part.start),to:sub(part.end),origin:sub(part.pivot),rotation:rot,autouv:0,box_uv:false}).addTo(groups[part.group]).init();
        const slots={iron:[0,0],steel:[64,32],brass:[64,0],dial:[96,0],casing:[0,32],dark:[32,32],panel:[32,0]};
        const [u,v]=slots[part.material]; const [x,y,z]=part.end.map((n,i)=>n-part.start[i]);
        const dims={north:[x,y],south:[x,y],east:[z,y],west:[z,y],up:[x,z],down:[x,z]};
        for (const [face,[w,h]] of Object.entries(dims)) {
          cube.faces[face].texture=texture.uuid;
          const panel=['dial','panel'].includes(part.material);
          const full=panel && face==='north';
          const plain=panel && !full ? [64,32] : [u,v];
          cube.faces[face].uv=full ? [u,v,u+32,v+32] : [plain[0],plain[1],plain[0]+Math.min(32,w*2),plain[1]+Math.min(32,h*2)];
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
    for name,pos in [('front',[8,32,-85]),('back',[8,32,120]),('side',[-100,32,24]),('isometric',[-75,65,-65]),('rear-isometric',[-80,60,100])]:
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
        for angle in (0,22.5,45):
            evaluate(client,f"Group.all.find(g=>g.name==='wheel').rotation[0]={angle}; Canvas.updateAll(); true")
            r=client.call('set_camera_angle',dict(position=[-80,60,100],target=[4,21,24],projection='orthographic'))
            for b in r.get('content',[]):
                if b['type']=='image': (folder/f'pose-{angle}.png').write_bytes(base64.b64decode(b['data']))
        evaluate(client,"Group.all.find(g=>g.name==='wheel').rotation[0]=0; Canvas.updateAll(); true")
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

