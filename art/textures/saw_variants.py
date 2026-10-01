"""Derive blade materials from one saw master; preserve the wooden handle and silhouette."""
from pathlib import Path
from PIL import Image
from pipeline import pixels
root=Path(__file__).resolve().parent
source=Image.open(root/'sources/iron_saw-v1.png').convert('RGBA')
for tier in ('stone','diamond'):
    out=[]
    for red,green,blue,alpha in pixels(source):
        # Neutral metal only; warm wood and brass remain byte-identical.
        if max(red,green,blue)-min(red,green,blue)<35 and blue>=red-8:
            value=(red+green+blue)/3
            if tier=='stone': red=green=blue=round(value*.62)
            else: red,green,blue=round(value*.30),round(value*.90),round(value*.88)
        out.append((red,green,blue,alpha))
    image=Image.new('RGBA',source.size);image.putdata(out);image.save(root/f'sources/{tier}_saw-v1.png')
