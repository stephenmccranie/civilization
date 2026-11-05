#!/usr/bin/python3
"""Coarse surface map, rendered only from saved chunks marked minecraft:full."""
import json
import logging
import os
import sqlite3
import struct
import threading
import time
import zlib
from concurrent.futures import FIRST_COMPLETED, ProcessPoolExecutor, wait
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import re
import stat
from terrain import read_chunk, render_topography, png, topographic_tiles
from map_overviews import OverviewCache
from map_colors import COLOR_VERSION
from PIL import Image
import deposits
import villages
import server_metrics

ROOT=Path('/srv/minecraft')
HERE=Path(__file__).resolve().parent
REGIONS=ROOT/'world/region'
STATE=Path('/var/lib/minecraft-map')
LOCK=threading.Lock()
SNAPSHOT=b'{}'
OVERVIEW_SNAPSHOT=b'{"tiles":[]}'
PATTERN=re.compile(r'r\.(-?\d+)\.(-?\d+)\.mca$')
TILE=re.compile(r'/tiles/r\.(-?[0-9]{1,8})\.(-?[0-9]{1,8})\.(?:png|relief\.png|elevation\.png|contours\.png|height\.bin)$')
OVERVIEW=re.compile(r'/overviews/([1-8])/r\.(-?[0-9]{1,8})\.(-?[0-9]{1,8})\.(?:png|relief\.png|elevation\.png|contours\.png)$')
# A stored region pass is reused only while every layer it produced is current.
VERSION=f'{COLOR_VERSION}:deposits-{deposits.VERSION}'
NO_HEIGHT=-32768
# Regions are independent, so a pass draws this many at once. The service's Nice=10
# gives Minecraft first claim on the CPU; its CPUQuota leaves room for these plus HTTP.
WORKERS=8
ROWS={}
# Regions whose relief, elevation and contours must be derived again: their own or a neighbour's heights
# changed. Startup derives every region again, which also recovers a pass interrupted before shading.
UNSHADED=set()
WORLD_BYTES=None
LOD_BYTES=None
WORLD_SIZE_UPDATED=None
DEPOSIT_SNAPSHOT=b'{"markers":[],"counts":{}}'
VILLAGE_SNAPSHOT=b'{"markers":[]}'
METRICS_SNAPSHOT=b'{"updated":null}'


def publish_metrics(value):
    global METRICS_SNAPSHOT
    with LOCK: METRICS_SNAPSHOT=json.dumps(value,separators=(',',':')).encode()


def measure_world_sizes(world):
    """Allocated bytes, without following symlinks or double-counting hardlinks."""
    world_bytes=lod_bytes=0
    linked=set()
    lod_names={'DistantHorizons.sqlite','DistantHorizons.sqlite-wal','DistantHorizons.sqlite-shm'}
    def failed(error):
        raise error
    for directory,dirs,files in os.walk(world,onerror=failed,followlinks=False):
        directory=Path(directory)
        paths=[directory]
        paths.extend(directory/name for name in files)
        paths.extend(directory/name for name in dirs if (directory/name).is_symlink())
        for path in paths:
            try: info=path.lstat()
            except FileNotFoundError: continue  # A live save may remove a file.
            if not stat.S_ISDIR(info.st_mode) and info.st_nlink>1:
                key=(info.st_dev,info.st_ino)
                if key in linked: continue
                linked.add(key)
            allocated=info.st_blocks*512
            if stat.S_ISREG(info.st_mode) and path.parent.name=='data' and path.name in lod_names:
                lod_bytes+=allocated
            else: world_bytes+=allocated
    return world_bytes,lod_bytes


def world_size_worker():
    """Metadata-only disk accounting, independent of HTTP and chunk rendering."""
    global WORLD_BYTES,LOD_BYTES,WORLD_SIZE_UPDATED
    while True:
        try:
            world_bytes,lod_bytes=measure_world_sizes(ROOT/'world')
            with LOCK: WORLD_BYTES,LOD_BYTES,WORLD_SIZE_UPDATED=world_bytes,lod_bytes,time.time()
        except OSError:
            logging.warning('World size refresh deferred; retaining last measurement')
        time.sleep(300)



def overview_worker():
    global OVERVIEW_SNAPSHOT
    cache=OverviewCache(STATE)
    with LOCK: OVERVIEW_SNAPSHOT=json.dumps({'tiles':cache.rows},separators=(',',':')).encode()
    while True:
        try:
            with LOCK: snapshot=SNAPSHOT
            regions=json.loads(snapshot).get('regions',[])
            if regions:
                rebuilt=cache.refresh(regions)
                value=json.dumps({'tiles':cache.rows},separators=(',',':')).encode()
                with LOCK: OVERVIEW_SNAPSHOT=value
                if rebuilt: logging.info('Updated %s cached overview tiles',rebuilt)
        except Exception: logging.exception('Overview refresh failed; retaining previous tiles')
        time.sleep(15)


def summary(name,version,signature,full,tile,found,marks):
    """What the feeds need from one region; per-chunk detail stays in the database."""
    rx,rz=map(int,PATTERN.fullmatch(name).groups())
    return {'version':version,'signature':signature,'tile':[rx,rz,full,tile],
        'deposits':deposits.merge(row for rows in found.values() for row in rows),
        'villages':[marker for markers in marks.values() for marker in markers]}


def publish():
    global SNAPSHOT,DEPOSIT_SNAPSHOT,VILLAGE_SNAPSHOT
    try: job=json.loads((ROOT/'pregeneration/state.json').read_text())
    except (OSError,ValueError): job={'status':'unavailable'}
    # Publish only UI fields, never raw local job configuration or filesystem capacity.
    job={key:job[key] for key in ('status','phase','tile') if key in job}
    with LOCK: world_bytes,lod_bytes,size_updated=WORLD_BYTES,LOD_BYTES,WORLD_SIZE_UPDATED
    rows=list(ROWS.values())
    # A region is listed once shaded; its version is the time its layers were last written.
    coverage={'updated':time.time(),'regions':[r['tile'] for r in rows if r['tile'][2] and r['tile'][3]],
        'fullChunks':sum(r['tile'][2] for r in rows),'job':job,
        'worldBytes':world_bytes,'lodBytes':lod_bytes,'worldSizeUpdated':size_updated}
    found=deposits.snapshot(row for r in rows for row in r['deposits'])
    marks={'markers':sorted((m for r in rows for m in r['villages']),key=lambda m:m['id'])}
    encode=lambda value:json.dumps(value,separators=(',',':')).encode()
    coverage,found,marks=encode(coverage),encode(found),encode(marks)
    with LOCK: SNAPSHOT,DEPOSIT_SNAPSHOT,VILLAGE_SNAPSHOT=coverage,found,marks


def stamp(header,idx):
    """A chunk's location and save time in its region header change on every save."""
    return header[idx*4:idx*4+4]+header[4096+idx*4:4100+idx*4]


def decode(data,entry,path,x,z):
    """Every map layer of one saved chunk; None unless the chunk is fully generated."""
    chunk=read_chunk(data,entry,path,x,z)
    if (chunk.get('xPos'),chunk.get('zPos'))!=(x,z): raise ValueError('Chunk moved during read')
    if chunk.get('Status') not in ('minecraft:full','full'): return None
    pixels,heights=render_topography(chunk)
    return pixels,heights,deposits.extract(chunk),villages.extract(chunk)


def neighbourhood(name):
    """A region and the four regions whose shading reads its edge heights."""
    rx,rz=map(int,PATTERN.fullmatch(name).groups())
    return {f'r.{rx+dx}.{rz+dz}.mca' for dx,dz in ((0,0),(-1,0),(1,0),(0,-1),(0,1))}


def draw_region(path,tile,rx,rz,old,found,marks,current):
    """Redraw one region's chunks saved since its last pass and write its colours and heights; runs in a worker."""
    # One sequential read. Minecraft may rewrite the file meanwhile; a chunk read
    # inconsistently fails to decode and keeps its old stamp for the next pass.
    data=path.read_bytes()
    if len(data)<8192: return None
    if current:
        with Image.open(tile) as image: rgba=bytearray(image.tobytes())
        heights=list(struct.unpack('<16384h',tile.with_suffix('.height.bin').read_bytes()))
    else:
        rgba=bytearray(128*128*4)
        heights=[NO_HEIGHT]*(128*128)
    header=bytearray(data[:8192])
    drawn=errors=0
    failure=None
    for idx in range(1024):
        if stamp(data,idx)==stamp(old,idx): continue
        entry=struct.unpack_from('>I',data,idx*4)[0]
        try:
            layers=decode(data,entry,path,rx*32+idx%32,rz*32+idx//32) if entry>>8>=2 and entry&255 else None
        except (OSError,ValueError,KeyError,IndexError,struct.error,zlib.error) as error:
            header[idx*4:idx*4+4]=old[idx*4:idx*4+4]
            header[4096+idx*4:4100+idx*4]=old[4096+idx*4:4100+idx*4]
            failure=failure or f'chunk {idx}: {error}'
            errors+=1
            continue
        pixels,values,resources,starts=layers or (bytes(64),[NO_HEIGHT]*16,[],[])
        x,z=(idx%32)*4,(idx//32)*4
        for row in range(4):
            i=(z+row)*128+x
            rgba[i*4:i*4+16]=pixels[row*16:(row+1)*16]
            heights[i:i+4]=values[row*4:(row+1)*4]
        for index,value in ((found,resources),(marks,starts)):
            if value: index[idx]=value
            else: index.pop(idx,None)
        drawn+=1
    wrote=drawn>0 or not current
    if wrote:
        write(tile,png(rgba))
        write(tile.with_suffix('.height.bin'),struct.pack('<16384h',*heights))
    # Full chunks always draw an opaque sample at their corner.
    full=sum(rgba[((idx//32)*512+(idx%32)*4)*4+3]!=0 for idx in range(1024))
    if errors: failure=f'{errors} chunks failed to decode, first {failure}'
    return bytes(header),found,marks,full,wrote,drawn,failure


def shade_region(tile,rx,rz):
    """Write one region's relief, elevation and contours from its heights inside a one-pixel border of its
    neighbours' heights, and return the new tile version; runs in a worker."""
    with Image.open(tile) as image: rgba=image.tobytes()
    padded=[NO_HEIGHT]*(130*130)
    own=struct.unpack('<16384h',tile.with_suffix('.height.bin').read_bytes())
    for z in range(128): padded[(z+1)*130+1:(z+1)*130+129]=own[z*128:(z+1)*128]
    for dx,dz in ((-1,0),(1,0),(0,-1),(0,1)):
        try: other=struct.unpack('<16384h',(tile.parent/f'r.{rx+dx}.{rz+dz}.height.bin').read_bytes())
        except FileNotFoundError: continue
        for k in range(128):
            if dx: padded[(k+1)*130+(0 if dx<0 else 129)]=other[k*128+(127 if dx<0 else 0)]
            else: padded[(0 if dz<0 else 129)*130+k+1]=other[(127 if dz<0 else 0)*128+k]
    relief,elevation,contours=topographic_tiles(rgba,padded)
    write(tile.with_suffix('.elevation.png'),elevation)
    write(tile.with_suffix('.contours.png'),contours)
    write(tile.with_suffix('.relief.png'),relief)
    return tile.with_suffix('.relief.png').stat().st_mtime_ns


def write(target,content):
    temp=target.with_suffix(target.suffix+'.tmp')
    temp.write_bytes(content)
    temp.replace(target)


def load(db):
    """Open the region table and rebuild every feed from it, as on each start."""
    db.execute('PRAGMA journal_mode=WAL')
    db.execute('PRAGMA synchronous=NORMAL')
    db.execute('CREATE TABLE IF NOT EXISTS regions(name TEXT PRIMARY KEY,version TEXT,signature TEXT,'
        'header BLOB,deposits TEXT,villages TEXT,full INT,tile INT)')
    ROWS.clear()
    for name,version,signature,full,tile,found,marks in db.execute(
            'SELECT name,version,signature,full,tile,deposits,villages FROM regions'):
        ROWS[name]=summary(name,version,json.loads(signature),full,tile,
            {int(k):v for k,v in json.loads(found).items()},{int(k):v for k,v in json.loads(marks).items()})
    UNSHADED.clear();UNSHADED.update(ROWS)
    publish()


def refresh(db,pool):
    """One pass over the saved regions, nearest first, so the map fills outward like the world."""
    paths=sorted((p for p in REGIONS.glob('r.*.*.mca') if PATTERN.fullmatch(p.name)),
        key=lambda p:sum(int(v)**2 for v in PATTERN.fullmatch(p.name).groups()))
    for name in set(ROWS)-{p.name for p in paths}:
        db.execute('DELETE FROM regions WHERE name=?',(name,))
        del ROWS[name]
        UNSHADED.update(neighbourhood(name))
    db.commit()
    pending={}
    drawn=regions=0

    def commit(done):
        nonlocal drawn,regions
        for future in done:
            name,signature=pending.pop(future)
            try: result=future.result()
            except Exception:
                logging.exception('Region refresh failed: %s',name)
                continue
            if result is None: continue
            header,found,marks,full,wrote,count,failure=result
            if failure:
                # No signature, so the next pass retries the failed chunks.
                logging.warning('%s: %s',name,failure)
                signature=None
            if wrote: UNSHADED.update(neighbourhood(name))
            # The tile keeps its last shaded version until shading below writes its layers again.
            tile=ROWS[name]['tile'][3] if name in ROWS else 0
            db.execute('INSERT OR REPLACE INTO regions VALUES (?,?,?,?,?,?,?,?)',(name,VERSION,
                json.dumps(signature),header,json.dumps(found),json.dumps(marks),full,tile))
            db.commit()
            ROWS[name]=summary(name,VERSION,signature,full,tile,found,marks)
            publish()
            drawn+=count;regions+=count>0

    for path in paths:
        try: st=path.stat()
        except FileNotFoundError: continue  # Deleted since the listing; the next pass prunes it.
        signature=[st.st_mtime_ns,st.st_size]
        known=ROWS.get(path.name)
        current=known is not None and known['version']==VERSION
        if current and known['signature']==signature: continue
        if current:
            old,found,marks=db.execute('SELECT header,deposits,villages FROM regions WHERE name=?',(path.name,)).fetchone()
            found={int(k):v for k,v in json.loads(found).items()}
            marks={int(k):v for k,v in json.loads(marks).items()}
        else: old,found,marks=bytes(8192),{},{}
        if len(pending)>=2*WORKERS: commit(wait(pending,return_when=FIRST_COMPLETED).done)
        rx,rz=map(int,PATTERN.fullmatch(path.name).groups())
        future=pool.submit(draw_region,path,STATE/'tiles'/f'r.{rx}.{rz}.png',rx,rz,old,found,marks,current)
        pending[future]=(path.name,signature)
    commit(wait(pending).done)
    # Shading reads neighbours' heights, so it runs once every region of the pass is drawn.
    UNSHADED.intersection_update(ROWS)
    shading={}
    for name in UNSHADED:
        rx,rz=map(int,PATTERN.fullmatch(name).groups())
        shading[pool.submit(shade_region,STATE/'tiles'/f'r.{rx}.{rz}.png',rx,rz)]=name
    for future in wait(shading).done:
        name=shading[future]
        try: version=future.result()
        except Exception:
            logging.exception('Region shading failed: %s',name)
            continue
        UNSHADED.discard(name)
        ROWS[name]['tile'][3]=version
        db.execute('UPDATE regions SET tile=? WHERE name=?',(version,name))
    db.commit()
    publish()
    return drawn,regions


def worker():
    STATE.mkdir(exist_ok=True)
    (STATE/'tiles').mkdir(exist_ok=True)
    with sqlite3.connect(STATE/'regions.sqlite') as db,ProcessPoolExecutor(WORKERS) as pool:
        load(db)
        while True:
            start=time.monotonic()
            drawn,regions=refresh(db,pool)
            if drawn: logging.info('Drew %s chunks in %s regions; pass %.1fs',drawn,regions,time.monotonic()-start)
            time.sleep(max(1,30-(time.monotonic()-start)))


class Handler(BaseHTTPRequestHandler):
    server_version='Map'
    sys_version=''

    def setup(self):
        self.request.settimeout(5)
        super().setup()

    def do_HEAD(self):
        self.do_GET()

    def do_GET(self):
        path=self.path.split('?')[0]
        if path=='/api/coverage':
            with LOCK: data=SNAPSHOT
            mime='application/json'
        elif path=='/api/overviews':
            with LOCK: data=OVERVIEW_SNAPSHOT
            mime='application/json'
        elif path=='/api/players':
            try:
                value=json.loads((ROOT/'map-players.json').read_text())
                value['stale']=not 0 <= time.time()-float(value['updated']) <= 15
                if value['stale']: value['players']=[]
                value={key:value[key] for key in ('players','stale','updated')}
                value['players']=[{key:player[key] for key in ('uuid','name','dimension','x','y','z','yaw') if key in player}
                    for player in value['players']]
            except (OSError,ValueError,KeyError,TypeError):
                value={'players':[],'stale':True,'updated':None}
            data=json.dumps(value,separators=(',',':')).encode()
            mime='application/json'
        elif path=='/api/deposits':
            with LOCK:data=DEPOSIT_SNAPSHOT
            mime='application/json'
        elif path=='/api/villages':
            with LOCK:data=VILLAGE_SNAPSHOT
            mime='application/json'
        elif path=='/api/metrics':
            with LOCK:data=METRICS_SNAPSHOT
            mime='application/json'
        elif path in ('/','/index.html'):
            data=(HERE/'surface.html').read_bytes()
            mime='text/html; charset=utf-8'
        elif path=='/map.js':
            data=(HERE/'map.js').read_bytes()
            mime='text/javascript; charset=utf-8'
        elif TILE.fullmatch(path) or OVERVIEW.fullmatch(path):
            p=(STATE/path[1:]).resolve()
            if not p.is_relative_to(STATE.resolve()):
                self.send_error(404)
                return
            try: data=p.read_bytes()
            except (FileNotFoundError, PermissionError):
                self.send_error(404)
                return
            mime='application/octet-stream' if path.endswith('.bin') else 'image/png'
        else:
            self.send_error(404)
            return
        self.send_response(200)
        self.send_header('Content-Type',mime)
        self.send_header('Content-Length',str(len(data)))
        self.send_header('Cache-Control','public, max-age=86400' if path.startswith(('/tiles/','/overviews/')) else 'no-store')
        self.send_header('X-Content-Type-Options','nosniff')
        self.end_headers()
        try:
            if self.command!='HEAD': self.wfile.write(data)
        except (BrokenPipeError,ConnectionResetError): pass

    def log_message(self,*_): pass


class BoundedHTTPServer(ThreadingHTTPServer):
    """Bound connection threads even if the local proxy is misconfigured."""
    daemon_threads=True
    request_queue_size=32

    def __init__(self,*args,**kwargs):
        self.slots=threading.BoundedSemaphore(32)
        super().__init__(*args,**kwargs)

    def process_request(self,request,client_address):
        if not self.slots.acquire(blocking=False):
            self.shutdown_request(request)
            return
        try: super().process_request(request,client_address)
        except BaseException:
            self.slots.release()
            raise

    def process_request_thread(self,request,client_address):
        try: super().process_request_thread(request,client_address)
        finally: self.slots.release()


if __name__=='__main__':
    logging.basicConfig(level=logging.INFO,format='%(message)s')
    threading.Thread(target=world_size_worker,daemon=True).start()
    threading.Thread(target=worker,daemon=True).start()
    threading.Thread(target=overview_worker,daemon=True).start()
    threading.Thread(target=server_metrics.worker,args=(publish_metrics,),daemon=True).start()
    BoundedHTTPServer(('127.0.0.1',8080),Handler).serve_forever()
