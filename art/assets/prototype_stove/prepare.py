"""Refresh stove export metadata without replacing its current brief or other assets."""
from pathlib import Path
import json
HERE=Path(__file__).resolve().parent

def write(p,data):p.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
stove=json.loads((HERE/'asset.json').read_text(encoding='utf-8'))
write(HERE/'blockstates.json',{'variants':{f'facing={f},lit={lit}':{'model':'civilization:block/prototype_stove',**({'y':yaw} if yaw else {})} for f,yaw in [('north',0),('east',90),('south',180),('west',270)] for lit in ['true','false']}})
write(HERE/'item.json',{'parent':'civilization:block/prototype_stove'})
stove['sources']=['prepare.py','build.py','textures.py','surfaces.py','surface-spec.json','surface-map.json','texture-spec.json','material-master-03.png','material-generations.json','material-prompt.txt','material-detail-prompt.txt','material-quiet-prompt.txt','surface-prompt.txt','model/prototype_stove.bbmodel','prototype_stove_iron.png','stove_wet.wav','stove_sizzle.wav','stove_char.wav']
stove['outputs']=[{'source':'prototype_stove.json','target':'models/block/prototype_stove.json','format':'java_model'},{'source':'blockstates.json','target':'blockstates/prototype_stove.json','format':'json'},{'source':'item.json','target':'models/item/prototype_stove.json','format':'java_model'}]
for name,size in [('prototype_stove_iron',[512,256]),('prototype_stove_brass',[64,64]),('prototype_stove_ember',[64,64])]:
    stove['outputs'].append({'source':name+'.png','target':'textures/block/'+name+'.png','format':'png','size':size,'alpha':'opaque'})
write(HERE/'asset.json',stove)
