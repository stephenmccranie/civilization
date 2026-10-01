"""Prepare a new superflat testing campus and its biome-specific annex datapack.
Run from the project root, then run the Gradle runCampusServer task.
Refuses to replace an existing generated campus.
"""
from pathlib import Path
import json
r=Path('civilization-mod/runs/test-campus');r.mkdir(parents=True,exist_ok=True)
if (r/'campus/level.dat').exists():raise SystemExit('Campus already exists; preserve it and choose another development output before rebuilding.')
settings=lambda biome:{'biome':biome,'lakes':False,'features':False,'layers':[{'height':1,'block':'minecraft:bedrock'},{'height':124,'block':'minecraft:stone'},{'height':2,'block':'minecraft:dirt'},{'height':1,'block':'minecraft:grass_block'}],'structure_overrides':[]}
(r/'server.properties').write_text('level-name=campus\nlevel-seed=192506\nlevel-type=minecraft:flat\ngenerator-settings='+json.dumps(settings('minecraft:river'),separators=(',',':'))+'\nserver-ip=127.0.0.1\nserver-port=25569\nonline-mode=false\nview-distance=8\nsimulation-distance=6\nspawn-protection=0\ngamemode=creative\ndifficulty=normal\nenable-command-block=true\nmax-tick-time=180000\n',encoding='utf-8')
(r/'eula.txt').write_text('eula=true\n',encoding='utf-8')
pack=r/'campus/datapacks/testing_annexes';(pack/'data/civilization_test/dimension').mkdir(parents=True,exist_ok=True)
(pack/'pack.mcmeta').write_text(json.dumps({'pack':{'pack_format':48,'description':'Civilization test campus: superflat resource and forest annexes'}}),encoding='utf-8')
for name,biome in [('coal','minecraft:windswept_hills'),('oil','minecraft:plains'),('forest','minecraft:forest')]:
 (pack/('data/civilization_test/dimension/'+name+'.json')).write_text(json.dumps({'type':'minecraft:overworld','generator':{'type':'minecraft:flat','settings':settings(biome)}}),encoding='utf-8')
