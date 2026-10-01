"""Original 64px materials from the inspected generated master, shared palette export."""
from pathlib import Path
import hashlib,json,sys
from PIL import Image,ImageDraw
HERE=Path(__file__).resolve().parent
sys.path.insert(0,str(HERE.parents[2]/'art/textures'))
from pipeline import prepare

def export_textures():
    master=Image.open(HERE/'material-master-03.png').convert('RGBA');half=master.width//2
    spec=json.loads((HERE/'texture-spec.json').read_text(encoding='utf-8'))
    tiles=[]
    for name,(x,y) in zip(['iron','plain','steel','brass'],[(0,0),(half,0),(0,half),(half,half)]):
        tile=prepare(master.crop((x,y,x+half,y+half)),spec[name],size=64);tiles.append(tile)
    atlas=Image.new('RGBA',(512,256),(40,43,45,255))
    for i,tile in enumerate(tiles):atlas.paste(tile,(i*64,0))
    # Authored slot decal and local fire state, fully specified in the pixels recipe.
    cold=prepare(master.crop((half,0,half*2,half)),spec['vent'],size=64)
    atlas.paste(cold,(256,0));atlas.save(HERE/'prototype_stove_iron.png')
    tiles[3].save(HERE/'prototype_stove_brass.png')
    warm=prepare(master.crop((half,0,half*2,half)),spec['ember'],size=64)
    warm.save(HERE/'prototype_stove_ember.png')
    sheet=Image.new('RGBA',(640,250),(224,225,226,255));d=ImageDraw.Draw(sheet)
    for i,(name,tile) in enumerate(zip(['iron','seasoned iron','steel','brass','cold slot','lit slot'],tiles+[cold,warm])):
        x=i*106;d.text((x+4,4),name,fill=(25,25,25));sheet.paste(tile,(x+4,24));sheet.paste(tile.resize((96,96),Image.Resampling.NEAREST),(x+4,110))
    sheet.save(HERE/'evidence/surface-materials.png')
    def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
    receipt={'method':'Built-in referenced material generation; equal-quarter crops; shared BOX/palette exporter at 64px, no dithering; explicit vent-state pixel recipe. Native model uses four pixels per unit.',
             'master':sha(HERE/'material-master-03.png'),'spec':sha(HERE/'texture-spec.json'),
             'outputs':{name:sha(HERE/name) for name in ['prototype_stove_iron.png','prototype_stove_brass.png','prototype_stove_ember.png']}}
    (HERE/'texture-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__':export_textures()
