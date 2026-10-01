"""Generate refinery geometry using the published reference-driven refinery texture family.
Raster masters and export receipts live in art/textures; this script never edits them. Run from the project root after editing this source.
"""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]/'civilization-mod/src/main/resources'
def put(path,value):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def element(a,b,tex):return {'from':a,'to':b,'faces':{side:{'texture':'#'+tex} for side in ['north','south','east','west','up','down']}}
textures={'body':'civilization:block/refinery_side','trim':'civilization:block/refinery_side','dark':'minecraft:block/gray_concrete','dial':'minecraft:block/white_concrete','needle':'minecraft:block/black_concrete','hot':'minecraft:block/orange_terracotta','brick':'minecraft:block/bricks','particle':'civilization:block/refinery_side'}
def model(name,elements,tex=None):
 # Reviewed controller models are owned by the industrial steel asset pipeline.
 if name in ('oil_pump','oil_refinery','oil_refinery_on','distillation_column','condenser','fuel_tank','coal_drill'):return
 put(Path('assets/civilization/models/block')/(name+'.json'),{'parent':'minecraft:block/block','textures':tex or textures,'elements':elements})
def item(name):put(Path('assets/civilization/models/item')/(name+'.json'),{'parent':'civilization:block/'+name})
def drop(name):put(Path('data/civilization/loot_table/blocks')/(name+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'civilization:'+name}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
def blockstate(name,directional=False):put(Path('assets/civilization/blockstates')/(name+'.json'),{'variants':{('facing='+d if directional else ''):{'model':'civilization:block/'+name,**({'y':y} if directional else {})} for d,y in ([('north',0),('east',90),('south',180),('west',270)] if directional else [('',0)])}})
def recipe(name,pattern,keys,count=1):put(Path('data/civilization/recipe')/(name+'.json'),{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{k:{'item':v if ':' in v else 'minecraft:'+v} for k,v in keys.items()},'result':{'id':'civilization:'+name,'count':count}})
# Smooth pack-matched panels, subtle copper seams. Full cubic bounds keep casing cuttable.
casing=[element([0,0,0],[16,16,16],'body')]
model('industrial_casing',casing);item('industrial_casing');blockstate('industrial_casing');drop('industrial_casing')
# The generated face is authoritative; shallow modeled trim provides actual relief.
for name in ['oil_refinery','distillation_column','condenser','fuel_tank','oil_pump','coal_drill']:
 face='refinery_heater_cold' if name=='oil_refinery' else 'refinery_front'
 tex={**textures,'front':'civilization:block/'+face}
 e=[element([0,0,0],[16,16,16],'body')]
 e[0]['faces']['north']={'texture':'#front'}
 model(name,e,tex);item(name);blockstate(name,True);drop(name)
model('cooling_grille',[element([0,0,0],[16,16,16],'fins')],{**textures,'fins':'civilization:block/refinery_fins'});item('cooling_grille');blockstate('cooling_grille');drop('cooling_grille')
flue=[element([4,0,4],[6,16,12],'body'),element([10,0,4],[12,16,12],'body'),element([6,0,4],[10,16,6],'body'),element([6,0,10],[10,16,12],'body')]
for low,high in [(0,2),(14,16)]:
 for a,b in [([2,low,2],[6,high,14]),([10,low,2],[14,high,14]),([6,low,2],[10,high,6]),([6,low,10],[10,high,14])]:flue.append(element(a,b,'trim'))
model('refinery_flue',flue);item('refinery_flue');blockstate('refinery_flue');drop('refinery_flue')
recipe('cooling_grille',['III','CCC','III'],{'I':'iron_nugget','C':'civilization:industrial_casing'},3)
recipe('refinery_flue',['C C','C C'],{'C':'civilization:industrial_casing'},4)
# Open guardrail shares the steel family; the handrail samples its existing brass accent.
railtex={**textures,'brass':'civilization:block/refinery_front'}
def brass(a,b):
 e=element(a,b,'brass')
 for face in e['faces'].values():face['uv']=[4,3,4.5,3.5]
 return e
post=[element([7,0,7],[9,15,9],'body'),element([5,0,5],[11,1,11],'body'),brass([6.5,14,6.5],[9.5,16,9.5])]
arm=[element([7.25,7,0],[8.75,8.5,7],'body'),brass([6.5,14,0],[9.5,16,7])]
model('industrial_guardrail_post',post,railtex);model('industrial_guardrail_arm',arm,railtex)
model('industrial_guardrail',post+arm+[element([7.25,7,9],[8.75,8.5,16],'body'),brass([6.5,14,9],[9.5,16,16])],railtex)
item('industrial_guardrail');drop('industrial_guardrail')
put(Path('assets/civilization/blockstates/industrial_guardrail.json'),{'multipart':[{'apply':{'model':'civilization:block/industrial_guardrail_post'}}]+[{'when':{d:'true'},'apply':{'model':'civilization:block/industrial_guardrail_arm','y':y}} for d,y in [('north',0),('east',90),('south',180),('west',270)]]})
recipe('industrial_guardrail',['CCC','I I','III'],{'C':'copper_ingot','I':'iron_ingot'},16)
# Only the inspection slit changes when firing; every casing pixel remains identical.
model('oil_refinery_on',[{**element([0,0,0],[16,16,16],'body'),'faces':{**element([0,0,0],[16,16,16],'body')['faces'],'north':{'texture':'#front'}}}],{**textures,'front':'civilization:block/refinery_heater'})
put(Path('assets/civilization/blockstates/oil_refinery.json'),{'variants':{'facing='+d+',working='+str(on).lower():{'model':'civilization:block/oil_refinery'+('_on' if on else ''),'y':angle} for d,angle in [('north',0),('east',90),('south',180),('west',270)] for on in [False,True]}})
# Flange with four exposed bolts and inset bore.
e=[element([0,0,0],[16,16,16],'body'),element([3,3,-1],[13,13,0],'trim'),element([5,5,-1.1],[11,11,-1],'dark')]
for x in [3.5,11]:
 for y in [3.5,11]:e.append(element([x,y,-1.3],[x+1.5,y+1.5,-1],'body'))
model('refinery_port',e);item('refinery_port');drop('refinery_port')
put(Path('assets/civilization/blockstates/refinery_port.json'),{'variants':{'facing='+d:{'model':'civilization:block/refinery_port',**r} for d,r in {'north':{},'east':{'y':90},'south':{'y':180},'west':{'y':270},'up':{'x':270},'down':{'x':90}}.items()}})
# Pipe geometry, glass and item/blockstate exports belong to art/assets/glass_pipes.
drop('fluid_pipe')
recipe('industrial_casing',['ICI','C C','ICI'],{'I':'iron_ingot','C':'copper_ingot'},8)
recipe('fluid_pipe',['ICI','   ','ICI'],{'I':'iron_nugget','C':'copper_ingot'},8)
recipe('refinery_port',[' I ','ICI',' I '],{'I':'iron_ingot','C':'copper_ingot'},2)
recipe('distillation_column',['ICI','CHC','ICI'],{'I':'iron_ingot','C':'copper_ingot','H':'hopper'})
recipe('condenser',['ICI','BHB','ICI'],{'I':'iron_ingot','C':'copper_ingot','B':'iron_bars','H':'hopper'})
# Canister models are owned by art/assets/canisters and tools/modeling/pipeline.py.
# Faceted sulfur crystals, rather than an unrelated reused food/tool icon.
put(Path('assets/civilization/models/item/sulfur.json'),{'parent':'minecraft:block/block','textures':{'crystal':'minecraft:block/yellow_concrete','particle':'minecraft:block/yellow_concrete'},'elements':[element([3,2,5],[8,7,11],'crystal'),element([8,2,6],[13,5,10],'crystal'),element([5,7,7],[7,10,9],'crystal')]})
put(Path('assets/civilization/models/item/enriched_mineral_blend.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'civilization:item/mineral_blend'}})
put(Path('data/civilization/recipe/enriched_mineral_blend.json'),{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'minecraft:gravel'},{'item':'civilization:sulfur'}],'result':{'id':'civilization:enriched_mineral_blend','count':1}})
put(Path('data/civilization/recipe/retort_enriched_fertilizer.json'),{'type':'civilization:retort','ingredient':{'item':'civilization:enriched_mineral_blend'},'result':{'id':'civilization:fertilizer','count':8},'cookingtime':400,'experience':0.35})
langpath=ROOT/'assets/civilization/lang/en_us.json';lang=json.loads(langpath.read_text(encoding='utf-8'))
for name,label in {'industrial_guardrail':'Industrial Guardrail','oil_refinery':'Fired Heater','distillation_column':'Distillation Column','condenser':'Air-Cooled Condenser','cooling_grille':'Cooling Grille','refinery_flue':'Refinery Flue','industrial_casing':'Industrial Casing','fluid_pipe':'Fluid Pipe','refinery_port':'Refinery Port'}.items():lang['block.civilization.'+name]=label
for name,label in {'lubricant_canister':'Lubricating Oil Canister','sulfur':'Sulfur','enriched_mineral_blend':'Sulfur Blend'}.items():lang['item.civilization.'+name]=label
for name,label in {'heated_crude':'Heated Crude','distillate_vapor':'Distillate Vapor','lubricating_oil':'Lubricating Oil'}.items():lang['fluid_type.civilization.'+name]=label
for name,label in {'guardrail':'Industrial guardrail','cooling':'Cooling grille','flue':'Refinery flue','casing':'Industrial casing','input_port':'Refinery port (intake)','output_port':'Refinery port (output)','aux_port':'Refinery port (oil output)','ladder':'Ladder','bars':'Iron bars'}.items():
 for suffix,shape in [('', ''),('_half',' half'),('_quarter',' beam'),('_eighth',' eighth')]:lang['material.civilization.'+name+suffix]=label+shape
langpath.write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')


# Apply the authoritative metalworking gates after base industrial recipes.
import runpy
runpy.run_path(str(Path(__file__).with_name("generate_metalworking.py")))
