"""One generated material master shared by every engine face and moving part."""
from pathlib import Path
import sys,json,hashlib,math
from PIL import Image
ROOT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT.parent))
from pipeline import prepare
DEST=ROOT.parents[2]/'civilization-mod/src/main/resources'
if '--check' not in sys.argv:
 raise SystemExit('Retired publisher: use tools/modeling/pipeline.py with art/assets/oil_engine. --check verifies the historical texture only.')
img=prepare(Image.open(ROOT/'iron-master.png'),{'kind':'block','colors':24})
out=DEST/'assets/civilization/textures/block/engine_iron.png'
if '--check' in sys.argv:
 assert Image.open(out).convert('RGBA').tobytes()==img.tobytes()
 print('Engine shared 32x32 material verified');sys.exit()
out.parent.mkdir(parents=True,exist_ok=True);img.save(out)
receipt={'master':'iron-master.png','master_sha256':hashlib.sha256((ROOT/'iron-master.png').read_bytes()).hexdigest(),'output':str(out),'sha256':hashlib.sha256(out.read_bytes()).hexdigest(),'size':list(img.size),'palette':24,'reuse':'Same master on every iron face; copper/steel reference active resource-pack materials.'}
(ROOT/'export-receipt.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf-8')
def save(path,d):
 p=DEST/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
def box(a,b,t='iron',rotation=None):
 d={'from':a,'to':b,'faces':{x:{'texture':'#'+t,'uv':[0,0,16,16]} for x in ['north','south','east','west','up','down']}}
 if rotation:d['rotation']=rotation
 return d
tex={'iron':'civilization:block/engine_iron','copper':'minecraft:block/copper_block','brass':'minecraft:block/gold_block','steel':'civilization:block/refinery_side','particle':'civilization:block/engine_iron'}
# Visible ports: left/right relative to controller; same physical faces as capabilities.
controller=[box([0,0,0],[16,8,16],'steel'),box([4,8,3],[12,12,9],'iron'),box([5,9,2],[11,11,3],'copper'),box([0,6,5],[2,10,11],'copper'),box([14,6,5],[16,10,11],'copper')]
cylinder=[box([2,3,1],[14,13,16]),box([4,1,1],[12,15,16]),box([1,4,0],[15,12,3]),box([5,15,8],[11,23,14],'copper'),box([3,-8,3],[13,3,14],'steel'),box([4,-8,2],[12,-5,15],'iron'),box([5,15,8],[11,16,14],'brass'),box([5,21,8],[11,23,14],'brass'),box([12,6,3],[15,8,14],'copper'),box([13,6,12],[15,18,14],'copper'),box([10,16,12],[15,18,14],'copper')]
# Stationary round rim, moving spokes are rendered by the controller.
wheel=[box([5,8,5],[11,13,11],'brass'),box([6,-8,6],[10,8,10],'steel'),box([3,-8,4],[13,-5,12],'iron')]
for a in range(0,360,45):
 # Four axis-aligned segments and four legal 45-degree block-model segments.
 angle=math.radians(a);yc=10.4+10*math.cos(angle);zc=8+10*math.sin(angle)
 if a%90==0:
  dy,dz=(1.4,4.4) if a%180==0 else (4.4,1.4)
  wheel.append(box([5,yc-dy,zc-dz],[11,yc+dy,zc+dz]))
 else:
  wheel.append(box([5,yc-1.4,zc-4.4],[11,yc+1.4,zc+4.4],rotation={'origin':[8,yc,zc],'axis':'x','angle':45 if a in (45,225) else -45}))
for name,elements in [('oil_engine',controller),('engine_cylinder',cylinder),('engine_flywheel',wheel)]:
 save('assets/civilization/models/block/'+name+'.json',{'parent':'minecraft:block/block','textures':tex,'elements':elements})
 save('assets/civilization/models/item/'+name+'.json',{'parent':'civilization:block/'+name})
 save('assets/civilization/blockstates/'+name+'.json',{'variants':{'facing='+f:{'model':'civilization:block/'+name,'y':y} for f,y in [('north',0),('east',90),('south',180),('west',270)]}})
 save('data/civilization/loot_table/blocks/'+name+'.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'civilization:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
for name,pattern,key in [('engine_cylinder',['III','P P','III'],{'I':{'item':'minecraft:iron_ingot'},'P':{'item':'civilization:machine_parts'}}),('engine_flywheel',[' S ','SPS',' S '],{'S':{'item':'civilization:steel_ingot'},'P':{'item':'civilization:machine_parts'}})]:
 save('data/civilization/recipe/'+name+'.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':key,'result':{'id':'civilization:'+name,'count':1}})
print('Exported shared material and engine models')
