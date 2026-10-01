"""Inspect the actual generated tube in Blockbench, retaining an editable source."""
import base64
import json
from pathlib import Path
import sys
HERE=Path(__file__).resolve().parent
sys.path.insert(0,str(HERE.parents[2]/'tools/modeling'))
from mcp import Client
from proof import evaluate, export

client=Client()
try:
    client.call('create_project',{'name':'civilization_glass_pipe','format':'java_block'})
    model=json.loads((HERE/'fluid_pipe_item.json').read_text())
    textures={name:'data:image/png;base64,'+base64.b64encode((HERE/f'pipe_{name}.png').read_bytes()).decode() for name in ('steel','glass')}
    cfg=json.dumps({'elements':model['elements'],'textures':textures}).replace('/','\\/')
    evaluate(client,'''(()=>{const cfg=CONFIG;Project.texture_width=Project.texture_height=32;
      const textures={}; for(const [name,data] of Object.entries(cfg.textures))textures[name]=new Texture({name:name+'.png'}).fromDataURL(data).add(false);
      for(const e of cfg.elements){const rotation=e.rotation;const cube=new Cube({name:'pipe_facet',from:e.from,to:e.to,origin:rotation?rotation.origin:[8,8,8],rotation:rotation?[0,0,rotation.angle]:[0,0,0],autouv:0,box_uv:false}).init();
        for(const [side,face]of Object.entries(cube.faces)){if(!e.faces[side]){face.texture=null;continue;}face.texture=textures[e.faces[side].texture.slice(1)].uuid;face.uv=e.faces[side].uv.map(v=>v*2);}}
      Canvas.updateAll();return Cube.all.length;})()'''.replace('CONFIG',cfg))
    (HERE/'pipe.bbmodel').write_text(export(client,'project'),encoding='utf-8')
    for name,pos in [('model-end',[14,18,-35]),('model-side',[35,18,-12])]:
        client.call('set_camera_angle',{'position':pos,'target':[0,8,0],'projection':'orthographic'})
        evaluate(client,'Preview.selected.camOrtho.zoom=1.1; Preview.selected.camOrtho.updateProjectionMatrix(); true')
        r=client.call('capture_screenshot',{})
        for b in r.get('content',[]):
            if b['type']=='image':(HERE/f'{name}.png').write_bytes(base64.b64decode(b['data']))
finally:client.close()
