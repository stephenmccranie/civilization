from pathlib import Path
import json
r=Path('civilization-mod/runs/test-campus')
w=r/'compact-campus'
assert not (w/'level.dat').exists(), 'Preserve existing compact world'
settings={'biome':'minecraft:river','lakes':False,'features':False,'layers':[{'height':1,'block':'minecraft:bedrock'},{'height':124,'block':'minecraft:stone'},{'height':2,'block':'minecraft:dirt'},{'height':1,'block':'minecraft:grass_block'}],'structure_overrides':[]}
(r/'server.properties').write_text('level-name=compact-campus\nlevel-seed=192506\nlevel-type=minecraft:flat\ngenerator-settings='+json.dumps(settings,separators=(',',':'))+'\nserver-ip=127.0.0.1\nserver-port=25569\nonline-mode=false\nview-distance=8\nsimulation-distance=6\nspawn-protection=0\ngamemode=creative\ndifficulty=normal\nenable-command-block=true\nmax-tick-time=180000\n',encoding='utf-8')
pack=w/'datapacks/campus_geography';(pack/'data/civilization/tags/worldgen/biome').mkdir(parents=True,exist_ok=True)
(pack/'pack.mcmeta').write_text(json.dumps({'pack':{'pack_format':48,'description':'Compact campus woodland growth in the flat river biome'}}),encoding='utf-8')
(pack/'data/civilization/tags/worldgen/biome/woodland_regions.json').write_text(json.dumps({'replace':False,'values':['minecraft:river']}),encoding='utf-8')
