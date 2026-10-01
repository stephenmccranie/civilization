"""Engine as the worked example for the native modeling studio. No runtime publication."""
import argparse
import importlib.util
import json
import math
from pathlib import Path
import sys
from PIL import Image, ImageDraw, ImageEnhance

HERE=Path(__file__).resolve().parent
ROOT=next(p for p in HERE.parents if (p/'tools/modeling/studio.py').is_file())
sys.path.insert(0,str(ROOT/'tools/modeling'))
from mcp import Client
from proof import evaluate
from studio import Model, create, save, capture, contact_sheet, bounds
from surface_textures import decorate

REGIONS={name:{'rect':[i%4*64,i//4*64,64,64]} for i,name in enumerate(['iron','steel','brass','dark','casing','panel','dial','machined'])}
REGIONS['panel']['fit']=True
REGIONS['dial']['fit']=True


def atlas(folder,clay=False):
    spec=importlib.util.spec_from_file_location('sprite_pipeline',ROOT/'art/textures/pipeline.py')
    reducer=importlib.util.module_from_spec(spec);spec.loader.exec_module(reducer)
    source=Image.open(HERE/'material-master.png').convert('RGBA');half=source.width//2
    tiles=[reducer.prepare(source.crop((x*half,y*half,(x+1)*half,(y+1)*half)),{'kind':'block','colors':16 if (x,y)!=(1,1) else 24}) for y in (0,1) for x in (0,1)]
    materials={'iron':tiles[0],'steel':tiles[1],'brass':tiles[2],'dark':ImageEnhance.Brightness(tiles[0]).enhance(.55),
               'casing':Image.open(ROOT/'civilization-mod/src/main/resources/assets/civilization/textures/block/refinery_side.png').convert('RGBA'),
               'panel':tiles[0],'dial':tiles[3],'machined':tiles[1]}
    image=Image.new('RGBA',(256,128),(100,110,114,255))
    for name,tile in materials.items():
        x,y,_,_=REGIONS[name]['rect']
        if clay: tile=Image.new('RGBA',(32,32),(150,158,160,255))
        for dx in (0,32):
            for dy in (0,32):image.paste(tile,(x+dx,y+dy))
    # Panels are decals with explicit fit; sample one complete motif, not repeated motifs.
    REGIONS['panel']['rect']=[64,64,32,32]
    REGIONS['dial']['rect']=[128,64,32,32]
    folder.mkdir(parents=True,exist_ok=True);image.save(folder/'engine_atlas.png')


def geometry():
    m=Model();box=m.box;casting=m.casting
    for x in (-16,0,16):
        for z in (0,16,32):
            if x==0 and z==0:continue
            box('bed',f'bed_{x}_{z}',[x,0,z],[x+16,8,z+16],'casing')
    box('controller','plinth',[0,0,0],[16,8,16],'steel')
    box('controller','cabinet',[1,10,3],[15,18,16])
    box('controller','cabinet_foot',[0,8,2],[16,10,16],'iron')
    casting('controller','dial_mount',6.75,12.25,2,3,3.75,1,'brass')
    box('controller','dial_face',[3.5,9,1.9],[10,15.5,2],'iron',faces={'north':'dial'})
    box('controller','lever_slot',[11,9,2],[12.5,14,3],'dark')
    box('controller','lever_handle',[11.2,11,1],[12.3,13,2.5],'brass',-22.5,'x',[11.75,11,2.5])
    box('controller','fuel_flange',[5,5,-.25],[11,11,.5],'brass')
    box('controller','fuel_mouth',[6,6,-.3],[10,10,-.25],'dark')
    box('controller','oil_flange',[5,-.25,5],[11,.25,11],'brass')
    box('controller','oil_mouth',[6,-.3,6],[10,-.25,10],'dark')
    casting('cylinder','crankcase',8,25,16,27,7.5,2)
    box('cylinder','case_lid',[2,32.5,17],[14,33.5,25],'steel')
    box('cylinder','front_cover_rim',[2,19,15],[14,30.5,16],'steel')
    box('cylinder','front_cover',[3,20,14.6],[13,29.5,15],'iron')
    for x in (3.5,11.5):
        for y in (20.5,28):box('cylinder',f'cover_fastener_{x}_{y}',[x,y,14.2],[x+1,y+1,14.6],'dark')
    box('cylinder','side_cover_rim',[15.5,20,18],[16,29,25],'steel')
    box('cylinder','side_cover',[16,21,19],[16.3,28,24],'iron')
    for z in (19.5,23):
        for y in (21.5,26.5):box('cylinder',f'side_fastener_{z}_{y}',[16.3,y,z],[16.6,y+1,z+1],'steel')
    casting('cylinder','bearing_housing',25,22,-2,1,3.5,1,'steel','x')
    casting('cylinder','bearing_collar',25,22,-3,-2,2.75,.75,'brass','x')
    casting('cylinder','barrel',8,25,26,44,5.5,1.5)
    for z in (27,41):
        casting('cylinder',f'flange_{z}',8,25,z,z+1.5,6.5,1.75,'steel')
    casting('cylinder','end_cap',8,25,43.5,44.5,5,1.5)
    for x in (4,11):
        for y in (21,28):box('cylinder',f'head_bolt_{x}_{y}',[x,y,44.5],[x+1,y+1,45],'steel')
    for z in (27,41):
        for y in (21,28):box('cylinder',f'flange_stud_{z}_{y}',[1.1,y,z+.1],[1.6,y+1,z+1.1],'machined')
    casting('cylinder','hot_bulb',8,25,44.5,46,2.5,.75,'brass')
    box('cylinder','burner_tip',[7,19,42],[9,22.5,44],'iron')
    box('cylinder','burner_nozzle',[7.5,22,43],[8.5,23,44.5],'brass')
    box('cylinder','oiler_stem',[5,30.5,31],[8,33,34])
    box('cylinder','oiler_cup',[4.5,33,30.5],[8.5,34,34.5],'brass')
    box('cylinder','oiler_cap',[5.5,34,31.5],[7.5,35,33.5],'brass')
    box('cylinder','exhaust',[9,30.5,37],[13,39,41])
    box('cylinder','stack_collar',[8.75,36,36.75],[13.25,37,41.25],'brass')
    # Hollow stack, not a dark patch floating above a solid cap.
    for n,lo,hi in [('left',[8.75,39,36.75],[9.5,40,41.25]),('right',[12.5,39,36.75],[13.25,40,41.25]),('front',[9.5,39,36.75],[12.5,40,37.5]),('rear',[9.5,39,40.5],[12.5,40,41.25])]:box('cylinder','stack_lip_'+n,lo,hi,'steel')
    box('cylinder','stack_interior',[9.5,39.01,37.5],[12.5,39.1,40.5],'dark')
    box('shaft','shaft',[-9,23.5,20.5],[0,26.5,23.5],'steel')
    casting('wheel','hub',25,22,-13,-7,3.25,1,'iron','x')
    casting('wheel','hub_cap',25,22,-14,-13,2.5,.75,'brass','x')
    box('wheel','hub_key',[-14.1,24.25,21.25],[-14,25.75,22.75],'dark')
    box('wheel','spoke_vertical',[-11.5,12,20.8],[-9.5,38,23.2])
    box('wheel','spoke_horizontal',[-11.5,23.8,9],[-9.5,26.2,35])
    for i in range(8):
        a=i*math.pi/4;cy=25+13*math.cos(a);cz=22+13*math.sin(a)
        half=14*math.tan(math.pi/8)
        dy,dz=(1,half) if i%4==0 else (half,1)
        angle=0
        if i%2:dy,dz=1,half;angle=45 if i in (1,5) else -45
        box('wheel',f'rim_{i}',[-13,cy-dy,cz-dz],[-8,cy+dy,cz+dz],'iron',angle,'x',[-10.5,cy,cz])
        box('wheel',f'rim_face_{i}',[-13.15,cy-dy,cz-dz],[-13.01,cy+dy,cz+dz],'steel',angle,'x',[-10.5,cy,cz])
    for part in m.parts:
        if part['group'] in ('cylinder','wheel','shaft'):
            def lengthen(z):
                return z-6 if z<=26 else 20+(z-26)*24/18 if z<44 else z
            for key in ('start','end','pivot'):
                part[key][2]=round(part[key][2]-6 if part['group'] in ('wheel','shaft') else lengthen(part[key][2]),5)
    # Extend the enclosed crankcase over the integrated control face.
    for part in m.parts:
        if part['name'].startswith('crankcase_'):
            part['start'][2]-=6
        elif part['name'] in ('front_cover','front_cover_rim') or part['name'].startswith('cover_fastener_'):
            for key in ('start','end','pivot'):part[key][2]-=6
        if part['group'] in ('cylinder','wheel','shaft'):
            for key in ('start','end','pivot'):part[key][1]-=1
    # Split only at the construction anchor; together these form one quiet cast bed.
    box('cylinder','bed_skirt',[0,8,16],[16,10,46],'iron')
    box('cylinder','bed_body',[1,10,16],[15,18,44],'iron')
    box('cylinder','bed_crown',[3,18,16],[13,20,43],'iron')
    for x in (1,13.5):
        for z in (4,43):
            group='controller' if z==4 else 'cylinder'
            box(group,f'bed_anchor_{x}_{z}',[x,10,z],[x+1.5,10.75,z+1.5],'steel')
    return m


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--clay',action='store_true');args=parser.parse_args()
    folder=HERE/('blockout' if args.clay else 'model');atlas(folder,args.clay)
    model=geometry();decorate(folder,model.parts,REGIONS,args.clay);c=Client()
    try:
        wheel=[p for p in model.parts if p['group']=='wheel']
        wheel_bounds=bounds(wheel)
        # The octagon's corner radius is the worst sweep, not its stopped height.
        swept_radius=14/math.cos(math.pi/8)
        assert 24-swept_radius>8, 'Wheel strikes the bed during rotation'
        assert wheel_bounds[1][0]<-3, 'Wheel strikes the bearing housing'
        (folder/'geometry-check.json').write_text(json.dumps({'parts':len(model.parts),'bounds':bounds(model.parts),
            'wheel_bounds':wheel_bounds,'minimum_bed_clearance_units':24-swept_radius-8,
            'barrel_length_diameter_ratio':24/11,'texels_per_block':32},indent=2),encoding='utf-8')
        create(c,'oil_engine_study',model.parts,folder/'engine_atlas.png',REGIONS,pivots={'wheel':[-10.5,24,16]})
        c.call('create_animation',{'name':'oil_engine.cycle','loop':True,'animation_length':1.5,'bones':{'wheel':[{'time':t,'rotation':[t*240,0,0]} for t in (0,.375,.75,1.125,1.5)]}})
        save(c,folder,'assembly')
        views=[('front',[8,24,-90]),('back',[8,24,130]),('side',[-110,24,24]),('opposite',[110,24,24]),('isometric',[-80,64,-70]),('rear',[-90,65,120])]
        files=[]
        for name,pos in views:
            path=folder/f'{name}.png';capture(c,path,pos,[5,20,24],span=75);files.append(path)
        contact_sheet(files,folder/'views.png')
        poses=[]
        for angle in (0,22.5,45):
            evaluate(c,f"Group.all.find(g=>g.name==='wheel').rotation[0]={angle};Canvas.updateAll();true")
            path=folder/f'pose-{angle}.png';capture(c,path,[-90,65,120],[5,20,24],span=75);poses.append(path)
        contact_sheet(poses,folder/'motion.png')
        evaluate(c,"Group.all.find(g=>g.name==='wheel').rotation[0]=0;Canvas.updateAll();true")
        if not args.clay:
            for name,groups,offset in [('oil_engine',['controller'],[0,0,0]),('engine_cylinder',['cylinder'],[0,16,16]),('engine_flywheel',['wheel','shaft'],[-16,16,16]),('engine_flywheel_linked',['shaft'],[-16,16,16])]:
                create(c,name,[p for p in model.parts if p['group'] in groups],folder/'engine_atlas.png',REGIONS,offset,False)
                save(c,folder,name,False)
            create(c,'oil_engine_wheel',[p for p in model.parts if p['group']=='wheel'],folder/'engine_atlas.png',REGIONS,[8,0,8],True,{'wheel':[-10.5,24,16]})
            c.call('create_animation',{'name':'oil_engine_wheel.cycle','loop':True,'animation_length':1.5,'bones':{'wheel':[{'time':t,'rotation':[t*240,0,0]} for t in (0,.375,.75,1.125,1.5)]}})
            save(c,folder,'oil_engine_wheel')
        print(json.dumps({'parts':len(model.parts),'folder':str(folder),'density':'32px per block; decals explicitly fitted'}))
    finally:c.close()

if __name__=='__main__':main()
