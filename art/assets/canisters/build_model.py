"""One native cuboid recipe for all four canister items; no runtime renderer."""
import argparse
import base64
import json
import sys
from pathlib import Path

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
sys.path.insert(0,str(ROOT/'tools/modeling'))
from mcp import Client
from proof import evaluate, export

PARTS=[]
def box(name,start,end,material='iron',angle=0,pivot=None):
    PARTS.append(dict(name=name,start=start,end=end,material=material,angle=angle,pivot=pivot or [0,0,0]))

def geometry():
    box('body',[3.5,1.5,5],[12.5,10.5,11])
    box('folded_foot',[3.3,1,4.8],[12.7,1.75,11.2],'steel')
    box('shoulder_seam',[3.35,10,4.85],[12.65,10.5,11.15],'steel')
    box('shoulder_core',[3.5,10.5,6],[12.5,11.5,10])
    box('front_slope',[3.5,10.3964466,5.0428932],[12.5,11.1035534,6.4571068],'iron',-45,[8,10.75,5.75])
    box('rear_slope',[3.5,10.3964466,9.5428932],[12.5,11.1035534,10.9571068],'iron',45,[8,10.75,10.25])
    for x in (3.5,12.1): box('fold_'+str(x),[x,1.75,4.95],[x+.4,10,5.05],'steel')
    box('back_rib',[7.65,2.5,11],[8.35,9.5,11.15],'steel')
    box('handle_left',[4.5,11.5,7],[5.5,14,9],'steel')
    box('handle_right',[8,11.5,7],[9,14,9],'steel')
    box('handle_grip',[4.5,14,7],[9,15,9],'steel')
    box('filler_neck',[10,11.5,7],[12,12,9],'dark')
    box('brass_cap',[9.75,12,6.75],[12.25,13,9.25],'brass')
    box('cap_grip',[10.25,13,7.25],[11.75,13.5,8.75],'brass')
    box('enamel_label',[4.75,3,4.91],[11.25,9.5,4.96],'label')

DISPLAY={
 'gui':{'rotation':[20,225,0],'translation':[0,.3,0],'scale':[1.05,1.05,1.05]},
 'ground':{'translation':[0,2,0],'scale':[.6,.6,.6]},
 'fixed':{'rotation':[0,0,0],'scale':[.9,.9,.9]},
 'firstperson_righthand':{'rotation':[0,75,-5],'translation':[1.5,2.5,0],'scale':[.45,.45,.45]},
 'firstperson_lefthand':{'rotation':[0,-75,5],'translation':[1.5,2.5,0],'scale':[.45,.45,.45]},
 'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,1,0],'scale':[.55,.55,.55]},
 'thirdperson_lefthand':{'rotation':[0,0,0],'translation':[0,1,0],'scale':[.55,.55,.55]}}

def create(client,name,variant,textured):
    client.call('create_project',{'name':name,'format':'java_block'})
    data='data:image/png;base64,'+base64.b64encode((HERE/'model/canisters.png').read_bytes()).decode() if textured else ''
    cfg=json.dumps(dict(name=name,parts=PARTS,variant=variant,texture=data,display=DISPLAY)).replace('/','\\u002f')
    evaluate(client,r'''(() => {
      const cfg=CONFIG; Project.name=cfg.name;Project.texture_width=Project.texture_height=128;Project.box_uv=false;
      let data=cfg.texture;
      if(!data){const c=document.createElement('canvas');c.width=c.height=128;let ctx=c.getContext('2d');
        [['#5b6974',0,0],['#7f8b93',32,0],['#ad874d',64,0],['#30383d',96,0],['#808078',0,64],['#35383a',32,64],['#b08431',64,64],['#5d744d',96,64]].forEach(([color,x,y])=>{ctx.fillStyle=color;ctx.fillRect(x,y,32,32)});data=c.toDataURL();}
      const texture=new Texture({name:'canisters.png',folder:'item',namespace:'civilization'}).fromDataURL(data).add(false);
      const group=new Group({name:'canister'}).init();
      const tiles={iron:[0,0],steel:[32,0],brass:[64,0],dark:[96,0],label:[cfg.variant*32,64]};
      for(const p of cfg.parts){const cube=new Cube({name:p.name,from:p.start,to:p.end,origin:p.pivot,rotation:[p.angle,0,0],autouv:0,box_uv:false}).addTo(group).init();
        const [x,y,z]=p.end.map((n,i)=>n-p.start[i]);const dims={north:[x,y],south:[x,y],east:[z,y],west:[z,y],up:[x,z],down:[x,z]};
        for(const [face,[w,h]]of Object.entries(dims)){let [u,v]=tiles[p.material];const label=p.material==='label'&&face==='north';if(p.material==='label'&&!label)[u,v]=tiles.iron;
          cube.faces[face].texture=texture.uuid;cube.faces[face].uv=label?[u,v,u+32,v+32]:[u,v,u+Math.min(32,w*2),v+Math.min(32,h*2)];}
      }
      for(const [slot,settings]of Object.entries(cfg.display))Project.display_settings[slot]=new DisplaySlot(slot,settings);
      Canvas.updateAll();return Cube.all.length;
    })()'''.replace('CONFIG',cfg))

def views(client,folder,name):
    for view,pos in [('front',[0,8,-35]),('side',[35,8,0]),('back',[0,8,35]),('isometric',[25,22,-30])]:
        r=client.call('set_camera_angle',dict(position=pos,target=[0,8,0],projection='orthographic'))
        evaluate(client,"Preview.selected.camOrtho.zoom=.95; Preview.selected.camOrtho.updateProjectionMatrix(); true")
        r=client.call('capture_screenshot',{})
        for b in r.get('content',[]):
            if b['type']=='image':(folder/f'{name}-{view}.png').write_bytes(base64.b64decode(b['data']))

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--textured',action='store_true');args=parser.parse_args()
    folder=HERE/('model' if args.textured else 'blockout');folder.mkdir(exist_ok=True)
    geometry();client=Client()
    try:
        for i,name in enumerate(('empty_canister','crude_canister','fuel_canister','lubricant_canister')):
            if not args.textured and i!=2:continue
            create(client,name,i,args.textured)
            (folder/f'{name}.bbmodel').write_text(export(client,'project'),encoding='utf8')
            model=json.loads(export(client,'java_block'));model['parent']='minecraft:block/block';model['textures']['particle']='civilization:item/canisters';model['display']=DISPLAY
            (folder/f'{name}.json').write_text(json.dumps(model,indent=2)+'\n',encoding='utf8')
            views(client,folder,name)
        print('Native canisters:',folder)
    finally:client.close()

if __name__=='__main__':main()
