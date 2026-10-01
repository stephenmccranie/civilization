"""Small native Blockbench authoring helpers, not another model format/exporter.

Coordinates: Minecraft model units (16/block). UV density: pixels/model unit.
The caller owns shape design, atlas rectangles, assembly offsets and review.
"""
import base64
import hashlib
import itertools
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw
from proof import evaluate, export


class Model:
    def __init__(self):
        self.parts = []

    def box(self, group, name, start, end, material='iron', angle=0, axis='x', pivot=None, faces=None):
        if any(b <= a for a, b in zip(start, end)):
            raise ValueError(f'{name}: box must have positive volume')
        if any(p['name'] == name for p in self.parts):
            raise ValueError(f'Duplicate part: {name}')
        # Match the native project's coordinate precision before computing sizes/UVs.
        # Otherwise sqrt-based bevels change by 0.00001 units after save/reopen.
        snap=lambda values:[round(v,5) for v in values]
        part = dict(group=group, name=name, start=snap(start), end=snap(end), material=material,
                    angle=angle, axis=axis, pivot=snap(pivot or [0, 0, 0]), faces=faces or {})
        self.parts.append(part)
        return part

    def casting(self, group, name, u, v, start, end, radius, bevel, material='iron', axis='z'):
        """Legal cuboid approximation; tiny cap offsets avoid coplanar flicker.

        Java block models cannot use polygon end caps. The 0.002-unit offsets
        separate overlapping cap surfaces, not visible mechanical layers.
        """
        if axis not in ('x','z') or not 0 < bevel < radius or end-start <= .024:
            raise ValueError('Casting needs X/Z axis, valid bevel and positive depth')
        a = radius - bevel
        point = lambda x,y,z: [x,y,z] if axis == 'z' else [z,x,y]
        for i,(suffix, dx, dy) in enumerate([('wide',radius,a),('tall',a,radius)]):
            inset=i*.002
            self.box(group,name+'_'+suffix,point(u-dx,v-dy,start+inset),point(u+dx,v+dy,end-inset),material)
        c=(radius+3*a)/4
        length=bevel*math.sqrt(2); thickness=bevel/math.sqrt(2)
        for i,(sx,sy) in enumerate([(-1,-1),(-1,1),(1,-1),(1,1)]):
            cx=u+sx*c;cy=v+sy*c
            inset=(i+2)*.002
            self.box(group,f'{name}_bevel_{sx}_{sy}',point(cx-length/2,cy-thickness/2,start+inset),
                     point(cx+length/2,cy+thickness/2,end-inset),material,-45 if sx*sy>0 else 45,
                     axis,point(cx,cy,(start+end)/2))


def face_dimensions(part):
    x,y,z=[b-a for a,b in zip(part['start'],part['end'])]
    return {'north':(x,y),'south':(x,y),'east':(z,y),'west':(z,y),'up':(x,z),'down':(x,z)}


def bounds(parts):
    """Bounds after each cuboid's native single-axis rotation; no bone animation."""
    points=[]
    for p in parts:
        axis='xyz'.index(p['axis']);u,v=(axis+1)%3,(axis+2)%3
        a=math.radians(p['angle']);c,s=math.cos(a),math.sin(a)
        for corner in itertools.product(*zip(p['start'],p['end'])):
            q=[x-o for x,o in zip(corner,p['pivot'])]
            q[u],q[v]=q[u]*c-q[v]*s,q[u]*s+q[v]*c
            points.append([x+o for x,o in zip(q,p['pivot'])])
    if not points:raise ValueError('No geometry')
    return [[min(p[i] for p in points) for i in range(3)],[max(p[i] for p in points) for i in range(3)]]


def uv_faces(part, regions, density=2):
    """Never silently compress a long surface into a small material tile."""
    result={}
    for face,(w,h) in face_dimensions(part).items():
        material=part.get('faces',{}).get(face,part['material'])
        r=regions[material];u,v,rw,rh=r['rect']
        if r.get('fit',False):
            width,height=rw,rh  # Explicit authored decal; exempt from material density.
        else:
            width,height=w*density,h*density
            if width>rw+1e-6 or height>rh+1e-6:
                raise ValueError(f"{part['name']}/{face}: {width:g}x{height:g}px exceeds {material} {rw}x{rh}; enlarge region or split part")
        result[face]=[u,v,u+width,v+height]
    return result


def create(client,name,parts,atlas,regions,offset=(0,0,0),gecko=True,pivots=None,density=2):
    image=Image.open(atlas)
    for region in regions.values():
        u,v,w,h=region['rect']
        if min(u,v)<0 or min(w,h)<=0 or u+w>image.width or v+h>image.height:
            raise ValueError('Atlas region outside texture')
    pieces=[dict(p,uvs=uv_faces(p,regions,density)) for p in parts]
    cfg=dict(name=name,pieces=pieces,offset=offset,pivots=pivots or {},size=image.size,texture_name=Path(atlas).name,
             texture='data:image/png;base64,'+base64.b64encode(Path(atlas).read_bytes()).decode(),gecko=gecko)
    client.call('create_project',{'name':name,'format':'geckolib_model' if gecko else 'java_block'})
    script='''(() => {
      const c=CONFIG;
      Project.name=c.name; Project.geometry_name=c.name; Project.box_uv=false;
      Project.texture_width=c.size[0]; Project.texture_height=c.size[1];
      if(c.gecko) Project.geckolib_model_type='block';
      const t=new Texture({name:c.texture_name,folder:'block',namespace:'civilization'}).fromDataURL(c.texture).add(false);
      const groups={};const sub=v=>v.map((n,i)=>n-c.offset[i]);
      for(const p of c.pieces){
        if(!groups[p.group]) groups[p.group]=new Group({name:p.group,origin:sub(c.pivots[p.group]||c.offset)}).init();
        const rotation=[0,0,0];rotation['xyz'.indexOf(p.axis)]=p.angle;
        const cube=new Cube({name:p.name,from:sub(p.start),to:sub(p.end),origin:sub(p.pivot),rotation,autouv:0,box_uv:false}).addTo(groups[p.group]).init();
        for(const [face,uv] of Object.entries(p.uvs)){cube.faces[face].texture=t.uuid;cube.faces[face].uv=uv;}
      }
      Canvas.updateAll();return {cubes:Cube.all.length,bones:Group.all.length};
    })()'''
    return evaluate(client,script.replace('CONFIG',json.dumps(cfg).replace('/','\\u002f')))


def save(client,folder,name,gecko=True):
    folder=Path(folder);folder.mkdir(parents=True,exist_ok=True)
    (folder/f'{name}.bbmodel').write_text(export(client,'project'),encoding='utf-8')
    data=json.loads(export(client,'bedrock' if gecko else 'java_block'))
    if not gecko:
        data['parent']='minecraft:block/block';data['textures']['particle']=next(iter(data['textures'].values()))
    (folder/f'{name}{".geo" if gecko else ""}.json').write_text(json.dumps(data,indent=2),encoding='utf-8')
    if gecko:
        a=evaluate(client,'Animator.buildFile(null, Animation.all.map(a=>a.name))')
        (folder/f'{name}.animation.json').write_text(json.dumps(a,indent=2),encoding='utf-8')


def capture(client,path,position,target,span=70,size=1000):
    """Offscreen native renderer: fixed camera, scale and resolution, no window resize."""
    cfg=json.dumps(dict(position=position,target=target,span=span,size=size))
    script='''(() => {
      const c=CONFIG;const p=new Preview({id:'civilization_studio',offscreen:true});
      const hidden=Canvas.scene.children.filter(o=>['grid_group','outline_group'].includes(o.name)).map(o=>[o,o.visible]);
      try {
        hidden.forEach(([o])=>o.visible=false);
        p.setProjectionMode(true);p.resize(c.size,c.size);
        p.camOrtho.zoom=(c.size/40)/c.span;
        p.camOrtho.position.fromArray(c.position);p.controls.target.fromArray(c.target);
        p.camOrtho.lookAt(...c.target);p.camOrtho.updateProjectionMatrix();
        p.render();return p.canvas.toDataURL('image/png');
      } finally {hidden.forEach(([o,v])=>o.visible=v);p.delete();}
    })()'''
    data=evaluate(client,script.replace('CONFIG',cfg))
    Path(path).parent.mkdir(parents=True,exist_ok=True)
    Path(path).write_bytes(base64.b64decode(data.split(',',1)[1]))
    Path(path).with_suffix('.view.json').write_text(json.dumps(dict(position=position,target=target,span=span,size=size,
        renderer='Blockbench offscreen Preview',sha256=hashlib.sha256(Path(path).read_bytes()).hexdigest()),indent=2),encoding='utf-8')


def contact_sheet(paths,output,labels=None,cell=420):
    """Layout existing native renders; never synthesize missing model views."""
    paths=list(map(Path,paths));cols=3;rows=math.ceil(len(paths)/cols)
    sheet=Image.new('RGB',(cols*cell,rows*(cell+30)),(222,219,207));d=ImageDraw.Draw(sheet)
    for i,path in enumerate(paths):
        im=Image.open(path).convert('RGBA');im.thumbnail((cell,cell))
        x=(i%cols)*cell;y=(i//cols)*(cell+30)
        sheet.paste(im,(x+(cell-im.width)//2,y+30+(cell-im.height)//2),im)
        d.text((x+12,y+8),(labels or [p.stem for p in paths])[i],fill=(35,40,43))
    sheet.save(output)
