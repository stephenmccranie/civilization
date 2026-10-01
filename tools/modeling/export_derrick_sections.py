"""Partition the approved derrick into repairable panels without changing its silhouette.

Uses the local native authoring source; checked-in exports are sufficient at runtime.
"""
import importlib.util, itertools, json, math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'art/assets/oil_derrick/studio'
OUT = ROOT / 'civilization-mod/src/main/resources'

def section(name, y):
    if name == 'feet': return 0
    if name.startswith('middle_'): return 21
    if name.startswith('crown_'): return 22
    if name in ('pump_string', 'crosshead', 'oil_line'): return 23
    if name == 'controller': return -1
    tokens = name.split('_')
    if tokens[0] == 'leg':
        band = max(0, min(4, int((y - 28) // 72)))
        side = (int(tokens[1]) > 0) * 2 + (int(tokens[2]) > 0)
    else:
        band = min(4, int(tokens[2] if tokens[0] == 'tie' else tokens[1]))
        axis, sign = (tokens[1], tokens[3]) if tokens[0] == 'tie' else (tokens[2], tokens[3])
        side = (axis == 'z') * 2 + (int(sign) > 0)
    return 1 + band * 4 + side

def main():
    spec = importlib.util.spec_from_file_location('derrick_source', SOURCE / 'build.py')
    source = importlib.util.module_from_spec(spec); spec.loader.exec_module(source)
    beams = []; original = source.beam
    def beam(m, name, *args, **kwargs):
        old = len(source.BEAMS); original(m, name, *args, **kwargs)
        if len(source.BEAMS) > old: beams.append((name, *source.BEAMS[-1]))
    source.beam = beam; model = source.geometry()
    cells = {}
    def add(points, part):
        lo = [min(p[i] for p in points)/16 for i in range(3)]
        hi = [max(p[i] for p in points)/16 for i in range(3)]
        for c in itertools.product(*(range(math.floor(lo[i]+1e-6), math.ceil(hi[i]-1e-6)) for i in range(3))):
            if c in ((0,0,0), (1,0,0)): continue
            box = [round(max(0,lo[i]-c[i]),5) for i in range(3)] + [round(min(1,hi[i]-c[i]),5) for i in range(3)]
            if all(box[i+3] > box[i] for i in range(3)): cells.setdefault(c,[]).append([part,*box])
    for name,a,length,w,d,rot in beams:
        count = math.ceil(length/4); cubes = math.ceil(length/16)
        for j in range(count):
            mid = a[1] + (min(cubes-1,int((j+.5)*cubes/count))+.5)*length/cubes
            part = section(name, source.transform([a[0],mid,a[2]],a,rot)[1])
            lo = [a[0]-w/2,a[1]+j*length/count,a[2]-d/2]
            hi = [a[0]+w/2,a[1]+(j+1)*length/count,a[2]+d/2]
            add([source.transform(p,a,rot) for p in itertools.product(*zip(lo,hi))],part)
    for p in model.parts:
        if p['group']=='feet': add(list(itertools.product(*zip(p['start'],p['end']))),0)
    data = [{'offset':list(c),'boxes':boxes} for c,boxes in sorted(cells.items(),key=lambda e:(e[0][1],e[0][2],e[0][0]))]
    (OUT/'data/civilization/derrick_sections.json').write_text(json.dumps(data,separators=(',',':')),encoding='utf-8')
    geo = json.loads((SOURCE/'model/oil_derrick.geo.json').read_text())
    bones = geo['minecraft:geometry'][0]['bones']; children = []
    for bone in bones:
        groups = {}
        for cube in bone.pop('cubes',[]):
            # X is mirrored in the native export; vertical coordinates are unchanged.
            p = [cube['origin'][i]+cube['size'][i]/2 for i in range(3)]
            y = source.transform(p,bone.get('pivot',[0,0,0]),bone.get('rotation',[0,0,0]))[1]
            part = section(bone['name'],y)
            if part < 0: bone.setdefault('cubes',[]).append(cube)
            else: groups.setdefault(part,[]).append(cube)
        for part,cubes in groups.items():
            children.append({'name':f'section_{part}_{bone["name"]}','parent':bone['name'],'pivot':bone.get('pivot',[0,0,0]),'cubes':cubes})
    bones.extend(children)
    (OUT/'assets/civilization/geo/oil_derrick.geo.json').write_text(json.dumps(geo,indent=2)+'\n',encoding='utf-8')
    assert {b[0] for c in data for b in c['boxes']} == set(range(24))
    print(f'Exported 24 native sections and {len(data)} collision cells')

if __name__ == '__main__': main()
