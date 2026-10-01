"""Editable native stove shell and reproducible continuous-cue audio sources."""
from pathlib import Path
import sys,json,shutil
from PIL import Image,ImageDraw
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
sys.path.insert(0,str(ROOT/'tools/modeling'))
from mcp import Client
from studio import Model,create,save,capture,contact_sheet

def model():
    from textures import export_textures
    export_textures()
    m=Model()
    m.box('burner','heating_head',[0,9,0],[16,14,16])
    for x in [0,14.5]:
        for z in [0,14.5]:
            m.box('stand',f'leg_{x}_{z}',[x,.75,z],[x+1.5,9,z+1.5],'steel')
            m.box('stand',f'foot_{x}_{z}',[max(0,x-.25),0,max(0,z-.25)],[min(16,x+1.75),.75,min(16,z+1.75)],'steel')
    for z in [0,14.5]:m.box('stand',f'brace_cross_{z}',[1.5,2,z],[14.5,2.75,z+1.5],'steel')
    for x in [0,14.5]:m.box('stand',f'brace_depth_{x}',[x,2,1.5],[x+1.5,2.75,14.5],'steel')
    m.box('burner','cooktop',[0,14,0],[16,14.5,16],'steel')
    m.box('burner','head_lower_lip',[0,8.75,0],[16,9,16],'steel')
    # Four fine air slots rather than a hinged baking/oven panel.
    for x in [2,5.25]:
        for y in [10,11.25]:m.box('controls',f'air_slot_{x}_{y}',[x,y,-.035],[x+2.5,y+.5,-.015],'plain',faces={'north':'vent'})
    m.box('skillet','pan_base',[2,14.5,2],[13,15.6,13],'plain')
    for name,a,b in [('left',[2,15.6,2],[2.7,17.1,13]),('right',[12.3,15.6,2],[13,17.1,13]),('front',[2.7,15.6,2],[12.3,17.1,2.7]),('rear',[2.7,15.6,12.3],[12.3,17.1,13])]:m.box('skillet','rim_'+name,a,b,'plain')
    m.box('skillet','handle',[13,15.6,6],[16,16.5,8.5],'plain')
    m.casting('controls','dial_rim',12.5,11.5,-.6,0,1.8,.4,material='brass',axis='z')
    m.casting('controls','dial',12.5,11.5,-.7,-.6,1.5,.4,material='plain',axis='z')
    # Preserve the existing cook-right dial/handle coordinates used by the renderer.
    for part in m.parts:
        left,right=part['start'][0],part['end'][0];part['start'][0]=16-right;part['end'][0]=16-left;part['pivot'][0]=16-part['pivot'][0]
        if part['axis'] in ('y','z'):part['angle']=-part['angle']
    client=Client()
    try:
        from surfaces import decorate
        regions=decorate(m.parts)
        create(client,'prototype_stove_surface',m.parts,HERE/'prototype_stove_iron.png',regions,gecko=False,density=4)
        # Judge the complete assembly with neutral surfaces before its textured views.
        import base64,io
        from proof import evaluate
        neutral=Image.new('RGBA',(512,256),(105,108,111,255));buf=io.BytesIO();neutral.save(buf,format='PNG')
        def texture(data):
            url='data:image/png;base64,'+base64.b64encode(data).decode()
            evaluate(client,'(() => {Texture.all[0].fromDataURL('+json.dumps(url).replace('/','\\u002f')+'); Canvas.updateAll();return true;})()')
        views=[('front',[8,12,-40]),('back',[8,12,45]),('left',[-40,12,8]),('right',[45,12,8]),('front_threequarter',[37,35,-38]),('rear_threequarter',[-30,35,40])]
        for stage,data in [('blockout',buf.getvalue()),('model',(HERE/'prototype_stove_iron.png').read_bytes())]:
            texture(data);paths=[]
            for name,pos in views:
                path=HERE/'evidence'/f'surface-{stage}-{name}.png';capture(client,path,[pos[0]-8,pos[1],pos[2]-8],[0,8,0],span=25,size=600);paths.append(path)
            contact_sheet(paths,HERE/f'evidence/surface-{stage}-sheet.png',cell=320)
        save(client,HERE/'model','prototype_stove',gecko=False)
    finally:client.close()
    shell=json.loads((HERE/'model/prototype_stove.json').read_text(encoding='utf-8'))
    shell['display']={'gui':{'rotation':[30,225,0],'translation':[0,-1,0],'scale':[.55,.55,.55]},'fixed':{'rotation':[0,180,0],'scale':[.5,.5,.5]},'firstperson_righthand':{'rotation':[0,45,0],'scale':[.35,.35,.35]},'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375,.375,.375]}}
    (HERE/'prototype_stove.json').write_text(json.dumps(shell,indent=2)+'\n',encoding='utf-8')

def sound():
    import numpy as np,soundfile as sf
    # Seeded filtered noise and sparse short crackles, crossfaded at both ends.
    rate=22050;n=rate*4;rng=np.random.default_rng(94017)
    for name,cut,crack in [('wet',300,.06),('sizzle',1700,.14),('char',4000,.34)]:
        noise=rng.normal(0,1,n);freq=np.fft.rfftfreq(n,1/rate)
        shaped=np.fft.irfft(np.fft.rfft(noise)*(freq/(freq+80))/(1+(freq/cut)**4),n)
        t=np.arange(n)/rate;shaped*=.4+.07*np.sin(t*13)+.035*np.sin(t*37)
        for at in rng.integers(100,n-400,int(18+crack*130)):
            k=np.arange(300);shaped[at:at+300]+=crack*rng.normal(0,1,300)*np.exp(-k/34)
        cross=512;shaped[:cross]*=np.linspace(0,1,cross);shaped[-cross:]*=np.linspace(1,0,cross)
        shaped=np.clip(shaped,-.85,.85)
        sf.write(HERE/f'stove_{name}.wav',shaped,rate,subtype='PCM_16')
        sf.write(HERE/f'stove_{name}.ogg',shaped,rate,format='OGG',subtype='VORBIS')

if __name__=='__main__':
    import argparse
    parser=argparse.ArgumentParser();parser.add_argument('--audio',action='store_true');args=parser.parse_args()
    model()
    if args.audio:sound()
