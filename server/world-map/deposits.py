"""Sparse resource markers from saved blocks; no Minecraft/model generation requests."""
from collections import Counter

VERSION=2
KINDS={'civilization:coal_seam':'coal','civilization:coal_workface':'coal',
       'civilization:reservoir_oil':'oil','civilization:oil_seep':'oil'}
# 256-block cells divide the 512-block region, so a cell never spans two regions.
CELL=256

def extract(chunk):
    rows={};ox=chunk['xPos']*16;oz=chunk['zPos']*16
    for section in chunk.get('sections',[]):
        states=section.get('block_states',{});palette=states.get('palette',[])
        wanted={i:p['Name'] for i,p in enumerate(palette) if p['Name'] in KINDS}
        if not wanted:continue
        data=states.get('data',[]);bits=max(4,(len(palette)-1).bit_length());per=64//bits;mask=(1<<bits)-1
        if len(palette)>1 and len(data)<(4096+per-1)//per:raise ValueError('Incomplete resource block states')
        for i in range(4096):
            index=(data[i//per]>>(i%per*bits))&mask if data else 0
            if index not in wanted:continue
            name=wanted[index];kind=KINDS[name]
            x=ox+(i&15);z=oz+((i>>4)&15);y=section['Y']*16+(i>>8)
            key=f'{kind}:{x//CELL}:{z//CELL}'
            row=rows.setdefault(key,dict(id=key,kind=kind,count=0,sumX=0,sumZ=0,minY=32767,maxY=-32768,seep=None))
            if name=='civilization:oil_seep':row['seep']=[x,y,z];continue
            row['count']+=1;row['sumX']+=x;row['sumZ']+=z;row['minY']=min(row['minY'],y);row['maxY']=max(row['maxY'],y)
    return list(rows.values())

def merge(rows):
    """Combine observations of the same cell, such as one per saved chunk."""
    groups={}
    for source in rows:
        row=groups.get(source['id'])
        if row is None:groups[source['id']]=dict(source);continue
        for key in ('count','sumX','sumZ'):row[key]+=source[key]
        row['minY']=min(row['minY'],source['minY']);row['maxY']=max(row['maxY'],source['maxY'])
        if source['seep'] is not None:row['seep']=source['seep']
    return list(groups.values())

def snapshot(rows):
    markers=[]
    for row in merge(rows):
        count=row['count'];anchor=row['seep'];x,z=(anchor[0],anchor[2]) if anchor else (row['sumX']/count,row['sumZ']/count)
        markers.append(dict(id=row['id'],kind=row['kind'],x=round(x,1),z=round(z,1),
            y=anchor[1] if anchor else (row['minY']+row['maxY'])//2,blocks=count,
            minY=row['minY'] if count else None,maxY=row['maxY'] if count else None))
    markers.sort(key=lambda r:r['id'])
    return dict(markers=markers,counts=dict(Counter(m['kind'] for m in markers)),
                source='Observed resource blocks in saved full chunks; fields may be partially generated.')
