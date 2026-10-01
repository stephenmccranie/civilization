"""Metalworking progression resources. Run after generate_refinery_models.py."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]/'civilization-mod/src/main/resources'
def put(path,value):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def craft(name,pattern,keys,count=1):
 put('data/civilization/recipe/'+name+'.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{k:{'item':v if ':' in v else 'minecraft:'+v} for k,v in keys.items()},'result':{'id':'civilization:'+name,'count':count}})
craft('foundry',['BBB','BFB','BBB'],{'B':'bricks','F':'furnace'})
craft('machine_parts',['SC','CS'],{'S':'civilization:steel_ingot','C':'copper_ingot'},2)
craft('industrial_casing',['SCS','C C','SCS'],{'S':'civilization:steel_ingot','C':'copper_ingot'},8)
# Preserve familiar controller layouts, but gate the common assembly part behind steel.
for name in ['oil_pump','oil_refinery','coal_drill','distillation_column','condenser']:
 p=ROOT/('data/civilization/recipe/'+name+'.json');d=json.loads(p.read_text(encoding='utf-8'))
 for v in d['key'].values():
  if v.get('item') in ['minecraft:hopper','civilization:mineral_coal']:v['item']='civilization:machine_parts'
 put(p.relative_to(ROOT),d)
for metal in ['iron','copper','gold']:
 for source in ['raw_'+metal,metal+'_ore','deepslate_'+metal+'_ore']+(['nether_gold_ore'] if metal=='gold' else []):
  put('data/civilization/recipe/foundry_'+source+'.json',{'type':'civilization:foundry','ingredient':{'item':'minecraft:'+source},'result':{'id':'minecraft:'+metal+'_ingot','count':1},'cookingtime':200,'experience':(1.0 if metal=='gold' else 0.7)})
 # Disable both raw material and equipment-recycling furnace routes.
 for suffix in ['_from_raw_'+metal,'_from_smelting_raw_'+metal,'_from_blasting_raw_'+metal,'_from_smelting_'+metal+'_ore','_from_blasting_'+metal+'_ore','_from_smelting_deepslate_'+metal+'_ore','_from_blasting_deepslate_'+metal+'_ore','_from_blasting']:
  put('data/minecraft/recipe/'+metal+'_ingot'+suffix+'.json',{'neoforge:conditions':[{'type':'neoforge:false'}]})
 for method in ['smelting','blasting']:
  put('data/minecraft/recipe/'+metal+'_nugget_from_'+method+'.json',{'neoforge:conditions':[{'type':'neoforge:false'}]})
for method in ['smelting','blasting']:
 put('data/minecraft/recipe/gold_ingot_from_'+method+'_nether_gold_ore.json',{'neoforge:conditions':[{'type':'neoforge:false'}]})
put('data/civilization/recipe/foundry_steel.json',{'type':'civilization:foundry','ingredient':{'item':'minecraft:iron_ingot'},'result':{'id':'civilization:steel_ingot','count':1},'cookingtime':800,'experience':0.35})
# Foundry controller models are owned by art/assets/thermal_controllers/export.py.
put('assets/civilization/blockstates/foundry.json',{'variants':{'facing='+d+',working='+str(on).lower():{'model':'civilization:block/foundry'+('_on' if on else ''),'y':angle} for d,angle in [('north',0),('east',90),('south',180),('west',270)] for on in [False,True]}})
put('assets/civilization/models/item/foundry.json',{'parent':'civilization:block/foundry'})
put('data/civilization/loot_table/blocks/foundry.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'civilization:foundry'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
# Steel and Machine Parts art are owned by art/assets/<item>/asset.json.
# Do not regenerate their retired placeholder models here.
p=ROOT/'data/minecraft/tags/block/mineable/pickaxe.json';d=json.loads(p.read_text(encoding='utf-8'))
if 'civilization:foundry' not in d['values']:d['values'].append('civilization:foundry')
put(p.relative_to(ROOT),d)
p=ROOT/'assets/civilization/lang/en_us.json';d=json.loads(p.read_text(encoding='utf-8'));d.update({'block.civilization.foundry':'Foundry','item.civilization.steel_ingot':'Steel Ingot','item.civilization.machine_parts':'Machine Parts','tooltip.civilization.foundry_input':'Ore -> ingots. Iron -> steel.','tooltip.civilization.foundry_fuel':'Fuel: Mineral Coal'})
for k,v in list(d.items()):
 if 'tooltip.civilization.kiln_' in k and k.replace('kiln_','foundry_') not in d:d[k.replace('kiln_','foundry_')]=v
put(p.relative_to(ROOT),d)
