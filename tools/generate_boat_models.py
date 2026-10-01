from pathlib import Path
import json
import copy
r=Path('civilization-mod/src/main/resources')
def put(p,d):
 p=r/p;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
def box(a,b,t):return {'from':a,'to':b,'faces':{d:{'texture':'#'+t} for d in ['north','south','east','west','up','down']}}
# One shared set of pack-aware wood, steel and brass surfaces across both console halves.
def elem(a,b,t,rotation=None):
 e=box(a,b,t)
 if t=='brass':
  for f in e['faces'].values():f['uv']=[4.5,2.5,5,3]
 if rotation:e['rotation']=copy.deepcopy(rotation)
 return e
tex={'wood':'minecraft:block/spruce_planks','metal':'civilization:block/refinery_side','brass':'civilization:block/refinery_front','gauge':'civilization:block/refinery_front','particle':'minecraft:block/spruce_planks'}
def model(name,e):put(Path('assets/civilization/models/block/'+name+'.json'),{'parent':'minecraft:block/block','textures':tex,'elements':e})
base=[elem([0,0,2],[16,2,14],'metal'),elem([1,2,3],[15,11,13],'wood'),elem([0,11,1],[16,13,15],'wood'),elem([0,11,14],[16,12,15],'brass')]
# Upright octagonal wheel, eight spokes/handgrips and brass hub on a low wooden console.
wheel=base+[elem([6,12,5],[10,17,8],'metal')]
for angle in [0,45]:
 rot={'origin':[8,15,11],'axis':'z','angle':angle}
 for a,b in [([1,12.2,10],[2.5,17.8,12]),([13.5,12.2,10],[15,17.8,12]),([5.2,8,10],[10.8,9.5,12]),([5.2,20.5,10],[10.8,22,12])]:wheel.append(elem(a,b,'wood',rot))
 for a,b in [([7.4,7,10.5],[8.6,23,11.5]),([0,14.4,10.5],[16,15.6,11.5])]:wheel.append(elem(a,b,'brass',rot))
wheel.append(elem([6,13,9],[10,17,13],'brass'))
# Lift the wheel above the counter so its lower rim is visible.
for e in wheel[len(base)+1:]:
 for key in ['from','to']:e[key][1]+=4
 if 'rotation' in e:e['rotation']['origin'][1]+=4
wheel[len(base)]['to'][1]=21
model('boat_helm',wheel)
for gear,angle in [(0,45),(1,22.5),(2,0),(3,-45)]:
 panel=base+[elem([2,13,3],[8,18,7],'brass'),elem([9,13,4],[14,14,13],'metal')]
 gauge=elem([3,13.5,7],[7,17.5,7.2],'gauge');gauge['faces']['south']['uv']=[2,2,9.5,9.5];panel.append(gauge)
 lever=elem([10.5,13,7],[12.5,20,9],'brass',{'origin':[11.5,13,8],'axis':'x','angle':angle});panel.append(lever)
 panel.append(elem([9.5,19,6.5],[13.5,21,9.5],'wood',{'origin':[11.5,13,8],'axis':'x','angle':angle}))
 for z in [4,6,10,12]:panel.append(elem([9,14, z],[10,14.3,z+.5],'brass'))
 model('boat_panel_'+str(gear),panel)
put(Path('assets/civilization/blockstates/boat_helm.json'),{'variants':{'facing='+d+',panel='+str(panel).lower()+',gear='+str(g):{'model':'civilization:block/'+('boat_panel_'+str(g) if panel else 'boat_helm'),'y':y} for d,y in [('north',0),('east',90),('south',180),('west',270)] for panel in [False,True] for g in range(4)}})
# Combined inventory model shows the entire console, centered and scaled to the item frame.
import copy
combined=copy.deepcopy(wheel)
for e in copy.deepcopy(panel):
 for key in ['from','to']:e[key][0]+=16
 if 'rotation' in e:e['rotation']['origin'][0]+=16
 combined.append(e)
put(Path('assets/civilization/models/item/boat_helm.json'),{'parent':'minecraft:block/block','textures':tex,'elements':combined,'display':{'gui':{'rotation':[25,225,0],'translation':[-4,-2,0],'scale':[.38,.38,.38]},'ground':{'scale':[.25,.25,.25]},'fixed':{'scale':[.4,.4,.4]}}})
put(Path('data/civilization/recipe/boat_helm.json'),{'type':'minecraft:crafting_shaped','pattern':['SPS','PWP','S S'],'key':{'S':{'item':'civilization:steel_ingot'},'P':{'item':'civilization:machine_parts'},'W':{'tag':'minecraft:planks'}},'result':{'id':'civilization:boat_helm','count':1}})
put(Path('data/civilization/loot_table/blocks/boat_helm.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'civilization:boat_helm'}],'conditions':[{'condition':'minecraft:survives_explosion'},{'condition':'minecraft:block_state_property','block':'civilization:boat_helm','properties':{'panel':'false'}}]}]})
p=Path('assets/civilization/lang/en_us.json');d=json.loads((r/p).read_text(encoding='utf-8'));d['block.civilization.boat_helm']='Small Boat Helm';d['material.civilization.planks']='Wooden planks';put(p,d)
p=Path('data/minecraft/tags/block/mineable/axe.json');d=json.loads((r/p).read_text(encoding='utf-8')) if (r/p).exists() else {'replace':False,'values':[]};d['values']=list(dict.fromkeys(d['values']+['civilization:boat_helm']));put(p,d)
