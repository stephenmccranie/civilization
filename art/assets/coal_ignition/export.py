from pathlib import Path
from PIL import Image
import hashlib,json
root=Path(__file__).resolve().parents[3]
folder=Path(__file__).resolve().parent
source=folder/'master.png'
im=Image.open(source).convert('RGB')
atlas=Image.new('RGB',(128,64))
for i in range(2):
    frame=im.crop((i*im.width//2,0,(i+1)*im.width//2,im.height)).resize((64,64),Image.Resampling.BOX)
    atlas.paste(frame,(64*i,0))
out=root/'civilization-mod/src/main/resources/assets/civilization/textures/gui/ignition.png'
atlas.save(out)
(folder/'export-receipt.json').write_text(json.dumps({'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'output_sha256':hashlib.sha256(out.read_bytes()).hexdigest(),'size':[128,64],'frames':2,'resample':'BOX','review':'Matching cabinet frame, clear flint/steel silhouette and compact spark contact. Two poses of the same tool; used as the production button.'},indent=2)+'\n')
