"""Village starts recorded in saved full chunks."""


def extract(chunk):
    markers=[]
    for start in chunk.get('structures',{}).get('starts',{}).values():
        kind=start.get('id','')
        if 'village' not in kind.split(':')[-1] or not start.get('Children'):
            continue
        cx,cz=start.get('ChunkX'),start.get('ChunkZ')
        if (cx,cz)!=(chunk.get('xPos'),chunk.get('zPos')):
            continue
        # The first jigsaw piece is the starting town-center piece, not an
        # average across distant paths and houses. Retain negative coordinates.
        box=start['Children'][0].get('BB',[])
        if len(box)!=6 or not all(isinstance(v,int) for v in box):
            continue
        markers.append(dict(id=f'{kind}:{cx}:{cz}',kind='village',
            x=(box[0]+box[3])/2,y=box[1],z=(box[2]+box[5])/2))
    return markers
