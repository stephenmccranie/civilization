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

    def box(self, group, name, start, end, material='iron', angle=0, axis='x', pivot=None, faces=None, collide=True):
        """`angle` turns the box about `axis`, or is a triple of angles about x, y and z applied in that order round
        `pivot`, as Blockbench and GeckoLib turn cubes. `collide=False` marks surface detail, such as stones set into a
        wall, that adds no collision of its own."""
        if any(b <= a for a, b in zip(start, end)):
            raise ValueError(f'{name}: box must have positive volume')
        if any(p['name'] == name for p in self.parts):
            raise ValueError(f'Duplicate part: {name}')
        # Match the native project's coordinate precision before computing sizes/UVs.
        # Otherwise sqrt-based bevels change by 0.00001 units after save/reopen.
        snap=lambda values:[round(v,5) for v in values]
        rotation = [round(a, 4) for a in angle] if isinstance(angle, (list, tuple)) else [angle if a == axis else 0 for a in 'xyz']
        part = dict(group=group, name=name, start=snap(start), end=snap(end), material=material,
                    rotation=rotation, pivot=snap(pivot or [0, 0, 0]), faces=faces or {}, collide=collide)
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

    def claim(self, group, start, end):
        """Give `group` the solid geometry inside the box start..end: every unrotated colliding part crossing it is split,
        the inside piece moving to `group`; surface detail stays whole with its section. A machine's controller claims
        its own cell this way, so the controller block draws the body around it before any section is built."""
        inside = lambda p: ([max(a, s) for a, s in zip(p['start'], start)], [min(b, e) for b, e in zip(p['end'], end)])
        for part in list(self.parts):
            if part['group'] == group or any(part['rotation']) or not part['collide']: continue
            lo, hi = inside(part)
            if any(b <= a for a, b in zip(lo, hi)): continue
            self.parts.remove(part)
            pieces = [(lo, hi, group)]
            # The outside remainder as up to six boxes, peeled off one axis at a time.
            rest_lo, rest_hi = list(part['start']), list(part['end'])
            for axis in range(3):
                if rest_lo[axis] < lo[axis]:
                    a, b = list(rest_lo), list(rest_hi); b[axis] = lo[axis]; pieces.append((a, b, part['group'])); rest_lo[axis] = lo[axis]
                if rest_hi[axis] > hi[axis]:
                    a, b = list(rest_lo), list(rest_hi); a[axis] = hi[axis]; pieces.append((a, b, part['group'])); rest_hi[axis] = hi[axis]
            for i, (a, b, g) in enumerate(pieces):
                self.parts.append(dict(part, group=g, name=f"{part['name']}_{i}" if i else part['name']+'_claimed', start=a, end=b))


def machine_cells(parts, controller=(0, 0, 0), exclude=()):
    """Collision cells of a machine model whose groups name its sections (`section_<n>_...`): each part's volume, split
    over the block cells it crosses, as [section, box...] in cell-local units. Rotated parts are sliced into pieces at
    most 4 units long before taking bounds, so a diagonal brace stays close to its true shape. The controller's cell and
    any `exclude` cells (ports) hold their own blocks and take no part cells."""
    cells = {}
    skip = {tuple(controller), *map(tuple, exclude)}
    for part in parts:
        if not part['group'].startswith('section_') or not part.get('collide', True): continue
        section = int(part['group'].split('_')[1])
        counts = [max(1, math.ceil((b - a) / 4)) if any(part['rotation']) else 1 for a, b in zip(part['start'], part['end'])]
        steps = [[a + (b - a) * k / n for k in range(n + 1)] for a, b, n in zip(part['start'], part['end'], counts)]
        for i, j, k in itertools.product(*(range(n) for n in counts)):
            lo = [steps[0][i], steps[1][j], steps[2][k]]; hi = [steps[0][i + 1], steps[1][j + 1], steps[2][k + 1]]
            points = [turn(part, corner) for corner in itertools.product(*zip(lo, hi))]
            a = [min(p[d] for p in points) / 16 for d in range(3)]; b = [max(p[d] for p in points) / 16 for d in range(3)]
            for cell in itertools.product(*(range(math.floor(a[d] + 1e-6), math.ceil(b[d] - 1e-6)) for d in range(3))):
                if cell in skip: continue
                box = [round(max(0, a[d] - cell[d]), 5) for d in range(3)] + [round(min(1, b[d] - cell[d]), 5) for d in range(3)]
                if all(box[d + 3] > box[d] for d in range(3)): cells.setdefault(cell, []).append([section, *box])
    return [{'offset': list(c), 'boxes': boxes} for c, boxes in sorted(cells.items(), key=lambda e: (e[0][1], e[0][2], e[0][0]))]


def turn(part, point):
    """A point of a part after its cube rotation: about x, then y, then z, round its pivot."""
    q = [x - o for x, o in zip(point, part['pivot'])]
    for axis, angle in enumerate(part['rotation']):
        if not angle: continue
        u, v = (axis + 1) % 3, (axis + 2) % 3; c, s = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        q[u], q[v] = q[u] * c - q[v] * s, q[u] * s + q[v] * c
    return [x + o for x, o in zip(q, part['pivot'])]


def face_dimensions(part):
    x,y,z=[b-a for a,b in zip(part['start'],part['end'])]
    return {'north':(x,y),'south':(x,y),'east':(z,y),'west':(z,y),'up':(x,z),'down':(x,z)}


def bounds(parts):
    """Bounds after each cuboid's native rotation; no bone animation."""
    points=[turn(p,corner) for p in parts for corner in itertools.product(*zip(p['start'],p['end']))]
    if not points:raise ValueError('No geometry')
    return [[min(p[i] for p in points) for i in range(3)],[max(p[i] for p in points) for i in range(3)]]


def face_spec(part, face):
    """A face's material, the offset of its island inside that region, and its quarter turn: a material name takes the
    region's corner; (material, x, y) an offset part of it; (material, x, y, 90) turns the island so grain painted
    along the region follows an upright piece."""
    spec=part.get('faces',{}).get(face,part['material'])
    return (spec,0,0,0) if isinstance(spec,str) else (*spec,0)[:4]


def uv_faces(part, regions, density=2):
    """Never silently compress a long surface into a small material tile."""
    result={}
    for face,(w,h) in face_dimensions(part).items():
        material,x,y,turn=face_spec(part,face)
        r=regions[material];u,v,rw,rh=r['rect']
        if r.get('fit',False):
            width,height=rw,rh  # Explicit authored decal; exempt from material density.
        else:
            width,height=(h*density,w*density) if turn%180 else (w*density,h*density)
            if x+width>rw+1e-6 or y+height>rh+1e-6:
                raise ValueError(f"{part['name']}/{face}: {width:g}x{height:g}px at {x},{y} exceeds {material} {rw}x{rh}; enlarge region or split part")
            if r.get('scatter') and face_spec(part,face)[1:3]==(0,0):
                # A tiling material shows a different, stable part of its region on each face, so neighbouring
                # faces do not repeat one corner; offsets stay on whole pixels inside the region.
                seed=int(hashlib.sha256(f"{part['name']}/{face}".encode()).hexdigest()[:8],16)
                x=(seed%1000)/1000*(rw-width)//1;y=(seed//1000%1000)/1000*(rh-height)//1
        result[face]=[u+x,v+y,u+x+width,v+y+height]
    return result


def create(client,name,parts,atlas,regions,offset=(0,0,0),gecko=True,pivots=None,density=2):
    image=Image.open(atlas)
    for region in regions.values():
        u,v,w,h=region['rect']
        if min(u,v)<0 or min(w,h)<=0 or u+w>image.width or v+h>image.height:
            raise ValueError('Atlas region outside texture')
    pieces=[dict(p,uvs=uv_faces(p,regions,density),turns={f:face_spec(p,f)[3] for f in face_dimensions(p)}) for p in parts]
    cfg=dict(name=name,pieces=pieces,offset=offset,pivots=pivots or {},size=image.size,texture_name=Path(atlas).name,
             texture='data:image/png;base64,'+base64.b64encode(Path(atlas).read_bytes()).decode(),gecko=gecko)
    client.call('create_project',{'name':name,'format':'geckolib_model' if gecko else 'java_block'})
    script='''(() => {
      const c=JSON.parse(globalThis.civilizationModelConfig);
      Project.name=c.name; Project.geometry_name=c.name; Project.box_uv=false;
      Project.texture_width=c.size[0]; Project.texture_height=c.size[1];
      if(c.gecko) Project.geckolib_model_type='block';
      const t=new Texture({name:c.texture_name,folder:'block',namespace:'civilization'}).fromDataURL(c.texture).add(false);
      const groups={};const sub=v=>v.map((n,i)=>n-c.offset[i]);
      for(const p of c.pieces){
        if(!groups[p.group]) groups[p.group]=new Group({name:p.group,origin:sub(c.pivots[p.group]||c.offset)}).init();
        const cube=new Cube({name:p.name,from:sub(p.start),to:sub(p.end),origin:sub(p.pivot),rotation:p.rotation,autouv:0,box_uv:false}).addTo(groups[p.group]).init();
        for(const [face,uv] of Object.entries(p.uvs)){cube.faces[face].texture=t.uuid;cube.faces[face].uv=uv;cube.faces[face].rotation=p.turns[face];}
      }
      Canvas.updateAll();return {cubes:Cube.all.length,bones:Group.all.length};
    })()'''
    # Large literal objects can exhaust the editor's JavaScript parser.
    # Transfer bounded string chunks, then parse the configuration once.
    payload=json.dumps(cfg)
    evaluate(client,"globalThis.civilizationModelConfig='';true")
    try:
        for start in range(0,len(payload),60000):
            chunk=json.dumps(payload[start:start+60000]).replace('/', '\\u002f')
            evaluate(client,'globalThis.civilizationModelConfig+='+chunk+';true')
        return evaluate(client,script)
    finally:
        evaluate(client,'delete globalThis.civilizationModelConfig;true')


def save(client,folder,name,gecko=True):
    folder=Path(folder);folder.mkdir(parents=True,exist_ok=True)
    (folder/f'{name}.bbmodel').write_text(export(client,'project'),encoding='utf-8', newline='\n')
    data=json.loads(export(client,'bedrock' if gecko else 'java_block'))
    if not gecko:
        data['parent']='minecraft:block/block';data['textures']['particle']=next(iter(data['textures'].values()))
    (folder/f'{name}{".geo" if gecko else ""}.json').write_text(json.dumps(data,indent=2),encoding='utf-8', newline='\n')
    if gecko:
        a=evaluate(client,'Animator.buildFile(null, Animation.all.map(a=>a.name))')
        (folder/f'{name}.animation.json').write_text(json.dumps(a,indent=2),encoding='utf-8', newline='\n')


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
        renderer='Blockbench offscreen Preview',sha256=hashlib.sha256(Path(path).read_bytes()).hexdigest()),indent=2),encoding='utf-8', newline='\n')


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
