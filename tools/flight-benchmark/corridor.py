"""Offline deterministic superflat fixture, cloned from a Minecraft-generated FULL chunk.
Never run against a live or user world. Each region is atomically published and resumable.
"""
import argparse, concurrent.futures, gzip, hashlib, json, math, os, pathlib, shutil, struct, time, zlib
ROOT=pathlib.Path(__file__).resolve().parents[2]
AREA=ROOT/'civilization-mod/runs/flight-benchmark'

def parse(data):
    offsets={}; pos=0
    def read(n):
        nonlocal pos
        v=data[pos:pos+n];pos+=n;return v
    def unpack(fmt):return struct.unpack(fmt,read(struct.calcsize(fmt)))[0]
    def string():return read(unpack('>H')).decode('utf8')
    def payload(t,path):
        offsets[path]=pos
        if t in (1,2,3,4,5,6):return unpack({1:'>b',2:'>h',3:'>i',4:'>q',5:'>f',6:'>d'}[t])
        if t==8:return string()
        if t in (7,11,12):return read(unpack('>i')*{7:1,11:4,12:8}[t])
        if t==9:
            subtype=unpack('>b');return [payload(subtype,path+(str(i),)) for i in range(unpack('>i'))]
        if t==10:
            result={}
            while (subtype:=unpack('>b')):
                name=string();result[name]=payload(subtype,path+(name,))
            return result
        raise ValueError(t)
    assert unpack('>b')==10;string(); result=payload(10,());assert pos==len(data)
    return result,offsets

def digest(p):return hashlib.sha256(pathlib.Path(p).read_bytes()).hexdigest()

def region(args):
    rx,rz,folder,raw,ox,oz=args
    target=pathlib.Path(folder)/f'r.{rx}.{rz}.mca'
    if target.exists():
        # A complete region is the atomic unit of resumability; never trust a temporary file.
        check_region(target,[(rx*32,rz*32),(rx*32+31,rz*32+31)])
        return target.stat().st_size
    header=bytearray(8192);chunks=[];sector=2;chunk=bytearray(raw)
    for z in range(32):
        for x in range(32):
            struct.pack_into('>i',chunk,ox,rx*32+x);struct.pack_into('>i',chunk,oz,rz*32+z)
            compressed=zlib.compress(chunk,1);length=len(compressed)+1;count=(length+4+4095)//4096
            assert count<256
            struct.pack_into('>I',header,4*(x+z*32),(sector<<8)|count)
            chunks.append(struct.pack('>I',length)+b'\x02'+compressed+b'\0'*(count*4096-length-4));sector+=count
    temp=target.with_suffix('.tmp')
    with temp.open('wb') as f:f.write(header);f.writelines(chunks)
    temp.replace(target)
    return sector*4096

def check_region(path,coords):
    with pathlib.Path(path).open('rb') as f:
        header=f.read(4096)
        for x,z in coords:
            loc=struct.unpack_from('>I',header,4*((x%32)+(z%32)*32))[0];assert loc>>8>=2
            f.seek((loc>>8)*4096);n=struct.unpack('>I',f.read(4))[0];assert f.read(1)==b'\x02'
            tags,_=parse(zlib.decompress(f.read(n-1)))
            assert (tags['xPos'],tags['zPos'],tags['Status'])==(x,z,'minecraft:full')
            assert tags.get('isLightOn')==1

def build(length=262144,width=2048,workers=4):
    assert length>=262144 and length%512==0 and width>=2048 and width%1024==0
    template=AREA/'seed/benchmark-template';raw=gzip.decompress((template/'flat-template.nbt').read_bytes());tags,offsets=parse(raw)
    assert tags['Status']=='minecraft:full' and tags['isLightOn']==1
    assert not tags['block_entities'] and not tags['structures']['starts'] and not tags['structures']['References']
    assert not tags.get('block_ticks') and not tags.get('fluid_ticks')
    # Strip time/history dependence. Sections, biomes, heightmaps and light are identical in a structure-free flat world.
    raw=bytearray(raw)
    for name in ('LastUpdate','InhabitedTime'):struct.pack_into('>q',raw,offsets[(name,)],0)
    base=AREA/'baseline';base.mkdir(parents=True,exist_ok=True);folder=base/'region';folder.mkdir(exist_ok=True)
    spec={'schema':1,'length_blocks':length,'width_blocks':width,'x_min':-1024,'x_max_exclusive':length+1024,'z_min':-width//2,'z_max_exclusive':width//2,'template_sha256':digest(template/'flat-template.nbt'),'seed':192506,'surface_y':63,'method':'Minecraft FULL flat chunk replicated offline; coordinate tags rewritten; structures/features disabled'}
    identity=base/'BUILD_ID.json'
    if identity.exists():assert json.loads(identity.read_text())==spec,'Existing baseline differs; refusing overwrite'
    else:identity.write_text(json.dumps(spec,indent=2))
    shutil.copy2(template/'level.dat',base/'level.dat')
    regions=[(x,z) for x in range(-2,length//512+2) for z in range(-width//1024,width//1024)]
    assert shutil.disk_usage(base).free>sum(not (folder/f'r.{x}.{z}.mca').exists() for x,z in regions)*4202496+2_000_000_000
    start=time.monotonic();total=0
    tasks=((x,z,str(folder),bytes(raw),offsets[('xPos',)],offsets[('zPos',)]) for x,z in regions)
    with concurrent.futures.ProcessPoolExecutor(max_workers=workers) as pool:
        for i,size in enumerate(pool.map(region,tasks,chunksize=4),1):
            total+=size
            if i%32==0 or i==len(regions):print(f'CORRIDOR {i}/{len(regions)} regions; {i*1024:,} FULL chunks; {time.monotonic()-start:.1f}s',flush=True)
    # Read back all region headers and diagonal endpoints, including negative coordinates and final corridor edge.
    for x,z in regions:check_region(folder/f'r.{x}.{z}.mca',[(x*32,z*32),(x*32+31,z*32+31)])
    spec.update(regions=len(regions),chunks=len(regions)*1024,bytes=total,verified_region_endpoints=len(regions)*2)
    (base/'CORRIDOR_READY.json').write_text(json.dumps(spec,indent=2));print(json.dumps(spec,indent=2))

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--workers',type=int,default=4);a=ap.parse_args();build(workers=a.workers)
