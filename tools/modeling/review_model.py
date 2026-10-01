"""Open an editable model in a new Blockbench tab and capture consistent native views.

python tools/modeling/review_model.py MODEL.bbmodel --output FOLDER --target 5 20 24 --span 75
This reads source; it never overwrites models or publishes runtime assets.
"""
import argparse
import hashlib
import json
from pathlib import Path
from mcp import Client
from proof import evaluate
from studio import capture, contact_sheet

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('model',type=Path);p.add_argument('--output',type=Path,required=True)
    p.add_argument('--target',nargs=3,type=float,required=True);p.add_argument('--span',type=float,required=True)
    p.add_argument('--size',type=int,default=1000);args=p.parse_args()
    if not 100<=args.size<=4096 or args.span<=0:raise ValueError('Invalid view size/span')
    source=json.loads(args.model.read_text(encoding='utf-8'))
    fmt=source['meta']['model_format']
    if fmt not in ('java_block','geckolib_model'):raise ValueError('Use native Java or GeckoLib source')
    args.output.mkdir(parents=True,exist_ok=True)
    c=Client()
    try:
        c.call('create_project',{'name':args.model.stem+'_review','format':fmt})
        evaluate(c,'Codecs.project.parse('+json.dumps(source).replace('/','\\u002f')+');true')
        cameras={'front':[0,0,-120],'back':[0,0,120],'side':[-120,0,0],'opposite':[120,0,0],
                 'isometric':[-85,45,-95],'rear':[-95,45,95]}
        paths=[]
        for name,delta in cameras.items():
            path=args.output/f'{name}.png'
            capture(c,path,[a+b for a,b in zip(args.target,delta)],args.target,args.span,args.size);paths.append(path)
        contact_sheet(paths,args.output/'views.png')
        (args.output/'source.json').write_text(json.dumps({'model':str(args.model.resolve()),
            'sha256':hashlib.sha256(args.model.read_bytes()).hexdigest(),'format':fmt},indent=2),encoding='utf-8')
        print(args.output/'views.png')
    finally:c.close()

if __name__=='__main__':main()
