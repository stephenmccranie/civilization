"""Cached map pyramid built from rendered PNGs, never Minecraft chunk files."""
import json
import time
from pathlib import Path
from PIL import Image

LAYERS=('plain','relief','elevation','contours')
RENDER_VERSION=3
OVERVIEW_SIZE=256


def downsample(source,layer):
    """Keep more real detail with clean Lanczos; preserve area-based coverage."""
    filter=Image.Resampling.LANCZOS if layer in ('plain','relief') else Image.Resampling.BOX
    coverage=source.getchannel('A').resize((OVERVIEW_SIZE,OVERVIEW_SIZE),Image.Resampling.BOX)
    result=source.resize((OVERVIEW_SIZE,OVERVIEW_SIZE),filter)
    result.putalpha(coverage)
    coverage.close()
    return result


class OverviewCache:
    def __init__(self,state,max_level=8):
        self.state=Path(state)
        self.root=self.state/'overviews'
        self.root.mkdir(exist_ok=True)
        self.index_path=self.root/'index.json'
        self.max_level=max_level
        try: self.index=json.loads(self.index_path.read_text())
        except (OSError,ValueError): self.index={}
        self.rows=[]
        # Serve the existing catalog while a new processing version rebuilds its tiles.
        for key,value in self.index.items():
            try: level,x,z=map(int,key.split(':'))
            except (ValueError,AttributeError): continue
            if 1<=level<=max_level and all(self.image_path(level,x,z,layer).exists() for layer in LAYERS):
                self.rows.append([level,x,z,value['version']])

    def image_path(self,level,x,z,layer):
        suffix='.png' if layer=='plain' else f'.{layer}.png'
        root=self.state/'tiles' if level==0 else self.root/str(level)
        return root/f'r.{x}.{z}{suffix}'

    def refresh(self,regions):
        children={(int(row[0]),int(row[1])):row[3] for row in regions if row[2]}
        rows=[]; live=set(); rebuilt=0
        for level in range(1,self.max_level+1):
            parents={}
            for (x,z),version in children.items():
                parents.setdefault((x//2,z//2),[]).append([x,z,version])
            next_children={}
            for (x,z),items in sorted(parents.items()):
                signature=sorted(items)
                key=f'{level}:{x}:{z}'; live.add(key)
                cached=self.index.get(key)
                ready=all(self.image_path(level,x,z,layer).exists() for layer in LAYERS)
                if not cached or cached.get('renderer')!=RENDER_VERSION or cached['children']!=signature or not ready:
                    (self.root/str(level)).mkdir(exist_ok=True)
                    for layer in LAYERS:
                        # Filter the stitched image so its internal child boundaries stay seamless.
                        child_size=128 if level==1 else OVERVIEW_SIZE
                        with Image.new('RGBA',(child_size*2,child_size*2)) as stitched:
                            for cx,cz,_ in signature:
                                with Image.open(self.image_path(level-1,cx,cz,layer)) as source:
                                    stitched.paste(source,((cx-x*2)*child_size,(cz-z*2)*child_size))
                            target=self.image_path(level,x,z,layer)
                            temp=target.with_suffix('.tmp')
                            with downsample(stitched,layer) as output:
                                output.save(temp,format='PNG',compress_level=2)
                            temp.replace(target)
                    cached={'children':signature,'version':time.time_ns(),'renderer':RENDER_VERSION,'pixels':OVERVIEW_SIZE}
                    self.index[key]=cached
                    rebuilt+=1
                next_children[x,z]=cached['version']
                rows.append([level,x,z,cached['version']])
            children=next_children
        self.index={k:v for k,v in self.index.items() if k in live}
        temp=self.index_path.with_suffix('.tmp')
        temp.write_text(json.dumps(self.index,separators=(',',':')))
        temp.replace(self.index_path)
        self.rows=rows
        return rebuilt
