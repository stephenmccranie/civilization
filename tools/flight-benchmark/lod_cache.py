"""Pinned DH 3.3.1 uniform-flat fixture. Never opens or edits a user save.

The DTO stores horizontal coordinates/detail in SQL, not its column blobs. Identical
flat columns stay identical when downsampled. Replicate those validated columns at
each tree level, including edge data; no DH world-generation work is timed.
"""
import base64,json,math,pathlib,sqlite3,subprocess
from corridor import AREA,ROOT,digest

TEMPLATE=pathlib.Path(__file__).with_name('flat-lod-template.json')

def decode_columns(data,count):
    p=0
    def var():
        nonlocal p
        value=0;shift=0
        while True:
            b=data[p];p+=1;value|=(b&127)<<shift
            if b<128:return value
            shift+=7
            assert shift<35
    sizes=[var() for _ in range(count)];flags=[var() for _ in range(sum(sizes))]
    heights=[var() for _ in flags];bottoms=[];previous=0
    for f,h in zip(flags,heights):
        bottom=previous-h
        if f&1:
            v=var();bottom+=(v>>1)^-(v&1)
        bottoms.append(bottom);previous=bottom
    lights=[var() if f&2 else 0 for f in flags];columns=[];i=0
    for size in sizes:
        columns.append(tuple(zip([f>>2 for f in flags[i:i+size]],heights[i:i+size],bottoms[i:i+size],lights[i:i+size])));i+=size
    assert p==len(data)
    return columns

def validated_template():
    fixture=json.loads(TEMPLATE.read_text());row={k:bytes.fromhex(v['hex']) if isinstance(v,dict) else v for k,v in fixture['row'].items()}
    cfg=json.loads((ROOT/'civilization-mod/dev.local.json').read_text())
    jar=pathlib.Path(cfg['prismInstance'])/'minecraft/mods/DistantHorizons-3.3.1-1.21.1-fabric-neoforge.jar'
    assert digest(jar)==fixture['dh_sha256'],'Rebuild the LOD fixture when DH changes'
    assert json.loads((AREA/'baseline/CORRIDOR_READY.json').read_text())['template_sha256']==fixture['chunk_template_sha256']
    keys=[k for k,v in row.items() if isinstance(v,bytes)]
    decoded=subprocess.run([str(pathlib.Path(cfg['javaHome'])/'bin/java.exe'),'-cp',str(jar),str(TEMPLATE.with_name('DecodeZstd.java'))],input='\n'.join(base64.b64encode(row[k]).decode() for k in keys),capture_output=True,text=True,check=True)
    blobs={k:base64.b64decode(v) for k,v in zip(keys,decoded.stdout.splitlines())}
    assert blobs['ColumnGenerationStep']==bytes([9])*4096
    assert blobs['ColumnWorldCompressionMode']==bytes([1])*4096
    assert set(decode_columns(blobs['Data'],62*62))=={((0,257,128,15),(1,127,1,15),(4,1,0,0))}
    for name in ('North','South','East','West'):
        assert set(decode_columns(blobs[name+'AdjData'],64))=={((0,257,128,15),(1,1,127,15),(2,2,125,0),(3,124,1,0),(4,1,0,0))}
    for block in (b'minecraft:plains',b'minecraft:grass_block',b'minecraft:dirt',b'minecraft:stone',b'minecraft:bedrock'):
        assert block in blobs['Mapping']
    assert row['DataFormatVersion']==2 and row['CompressionMode']==4
    return fixture,row

def build():
    fixture,row=validated_template();base=AREA/'baseline';spec=json.loads((base/'CORRIDOR_READY.json').read_text())
    target=base/'data/DistantHorizons.sqlite';target.parent.mkdir(exist_ok=True)
    identity={'schema':1,'fixture_sha256':digest(TEMPLATE),'dh_sha256':fixture['dh_sha256'],'x_min':-4096,'x_max_exclusive':spec['length_blocks']+4096,'z_min':-4096,'z_max_exclusive':4096,'levels':list(range(13)),'method':'Validated uniform DH columns replicated at all tree levels; fresh DB copy per run'}
    marker=base/'LOD_READY.json'
    if target.exists() and marker.exists():
        saved=json.loads(marker.read_text());assert all(saved[k]==v for k,v in identity.items());assert digest(target)==saved['database_sha256'];return saved
    temp=target.with_suffix('.building')
    if temp.exists():temp.unlink()  # exact, benchmark-owned scratch file
    c=sqlite3.connect(temp)
    for statement in fixture['schema']:c.execute(statement)
    c.executemany('INSERT INTO Schema VALUES (?,?,?)',fixture['schema_rows'])
    keys=list(row);query='INSERT INTO FullData ('+','.join(keys)+') VALUES ('+','.join('?' for _ in keys)+')'
    total=0
    for level in identity['levels']:
        size=64<<level
        xs=range(identity['x_min']//size,math.ceil(identity['x_max_exclusive']/size));zs=range(identity['z_min']//size,math.ceil(identity['z_max_exclusive']/size))
        def rows():
            for x in xs:
                for z in zs:
                    yield tuple(level if k=='DetailLevel' else x if k=='PosX' else z if k=='PosZ' else row[k] for k in keys)
        c.executemany(query,rows());c.commit();total+=len(xs)*len(zs)
        print(f'LOD level {level}: {len(xs)*len(zs):,} complete sections',flush=True)
    assert c.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
    assert c.execute('SELECT count(*) FROM FullData').fetchone()[0]==total
    c.close();temp.replace(target)
    identity.update(rows=total,bytes=target.stat().st_size,database_sha256=digest(target))
    marker.write_text(json.dumps(identity,indent=2));return identity

if __name__=='__main__':print(json.dumps(build(),indent=2))
