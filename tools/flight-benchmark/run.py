"""Fresh-JVM, fresh-world-copy flight experiments. Baseline and user worlds are never opened for play."""
import argparse,configparser,csv,datetime,hashlib,json,math,os,pathlib,shutil,subprocess,sys,time,uuid,re,statistics,sqlite3
from corridor import AREA,ROOT,digest,check_region
MOD=ROOT/'civilization-mod'

def lod_cache_state(database):
    """Inspect only a closed benchmark database; rendering data and hashes differ."""
    with sqlite3.connect(database.resolve().as_uri()+'?mode=ro',uri=True) as db:
        return {'full_data_rows':db.execute('SELECT count(*) FROM FullData').fetchone()[0],
                'chunk_hash_rows':db.execute('SELECT count(*) FROM ChunkHash').fetchone()[0]}


def percent(values,p):
    v=sorted(values);x=(len(v)-1)*p;a=int(x);b=min(a+1,len(v)-1);return v[a]+(v[b]-v[a])*(x-a)

def summarize(out):
    rows=list(csv.DictReader((out/'ticks.csv').open()));rows=[r for r in rows if r['phase']=='measure']
    frames=[r for r in csv.DictReader((out/'frames.csv').open()) if r['phase']=='measure' and float(r['frame_interval_ms'])>0]
    assert rows and frames,'No measurement samples'
    dt=[float(r['tick_interval_ms'])/1000 for r in rows];duration=sum(dt)
    work=[float(r['tick_work_ms']) for r in rows];fi=[float(r['frame_interval_ms']) for r in frames]
    initial=[r for r in csv.DictReader((out/'ticks.csv').open()) if r['phase']!='measure'][-1]
    generated=int(rows[-1]['new_chunks'])-int(initial['new_chunks'])
    result={'run':out.name,'mode':json.loads((out/'manifest.json').read_text())['mode'],'target_bps':float(rows[0]['target_bps']),'measured_seconds':duration,'tick_samples':len(rows),'actual_wall_bps':sum(float(r['wall_bps'])*d for r,d in zip(rows,dt))/duration,'game_bps_mean':statistics.mean(float(r['game_bps']) for r in rows),'body_bps_mean':statistics.mean(float(r['body_bps']) for r in rows) if rows[0]['body_bps']!='NaN' else None,'tps':len(rows)/duration,'tick_work_p50_ms':percent(work,.5),'tick_work_p95_ms':percent(work,.95),'tick_work_p99_ms':percent(work,.99),'tick_work_max_ms':max(work),'tick_intervals_over_100ms':sum(d>.1 for d in dt),'hold_time_pct':100*sum(d for r,d in zip(rows,dt) if r['held']=='true')/duration,'pilot_detached_ticks':sum(r['pilot_attached']!='true' for r in rows),'process_cpu_mean_pct':sum(float(r['process_cpu_pct'])*d for r,d in zip(rows,dt))/duration,'host_cpu_mean_pct':sum(float(r['host_cpu_pct'])*d for r,d in zip(rows,dt))/duration,'fps_mean':1000*len(fi)/sum(fi),'fps_median':1000/percent(fi,.5),'fps_1pct_low':1000/statistics.mean(sorted(fi,reverse=True)[:max(1,math.ceil(len(fi)*.01))]),'frame_interval_p50_ms':percent(fi,.5),'frame_interval_p99_ms':percent(fi,.99),'heap_peak_mib':max(float(r['heap_mib']) for r in rows),'server_chunk_loads':int(rows[-1]['server_chunk_loads'])-int(initial['server_chunk_loads']),'new_chunks':generated,'c2me_iterators':int(rows[-1]['c2me_iterators'])-int(initial['c2me_iterators']),'warp_frame_pct':100*sum(r['warp']=='true' for r in frames)/len(frames),'sweep_loaded_mean_pct':sum(float(r['sweep_loaded_pct'])*d for r,d in zip(rows,dt))/duration,'target_fits_safety_snapshot':all(r['target_snapshot_fits']=='true' for r in rows),'camera_wall_bps':sum(float(r['camera_wall_bps'])*float(r['frame_interval_ms']) for r in frames)/sum(fi),'client_near_loaded_mean_pct':statistics.mean(float(r['client_near_loaded_pct']) for r in frames),'pregenerated_route_valid':generated==0}
    # Older captures did not record the gradual governor; missing telemetry is unknown, never zero.
    result['streaming_brake_sample_pct']=100*sum(d for r,d in zip(rows,dt) if r.get('streaming_brake')=='true')/duration if 'streaming_brake' in rows[0] else None
    result['streaming_brake_episodes']=sum(r['streaming_brake']=='true' and (i==0 or rows[i-1]['streaming_brake']!='true') for i,r in enumerate(rows)) if 'streaming_brake' in rows[0] else None
    # Averages can hide repeated safety stops. Preserve speed spread and stop episodes.
    body=[float(r['body_bps']) for r in rows if r['body_bps']!='NaN']
    result.update(hold_episodes=sum(r['held']=='true' and (i==0 or rows[i-1]['held']!='true') for i,r in enumerate(rows)),tick_interval_p99_ms=percent([d*1000 for d in dt],.99),tick_interval_max_ms=max(dt)*1000,frames_over_50ms=sum(v>50 for v in fi),frame_interval_max_ms=max(fi))
    for name,p in [('min',0),('p05',.05),('p50',.5),('p95',.95),('max',1)]:result['body_bps_'+name]=percent(body,p) if body else None
    if (out/'safety.csv').exists():
        safety=[r for r in csv.DictReader((out/'safety.csv').open()) if r['phase']=='measure']
        reasons={}
        for r in safety:
            if r['clear']=='false':reasons[r['observed_reason']]=reasons.get(r['observed_reason'],0)+1
        result.update(physics_safety_checks=len(safety),physics_safety_rejections=sum(reasons.values()),observed_safety_reasons=reasons)
    (out/'summary.json').write_text(json.dumps(result,indent=2));return result

def prepare_client():
    client=AREA/'client';client.mkdir(parents=True,exist_ok=True)
    cfg=json.loads((MOD/'dev.local.json').read_text());prism=pathlib.Path(cfg['prismInstance'])/'minecraft'
    # Compile-time/runtime dependencies already supplied by ModDev must not be duplicated in mods/.
    runtime_prefixes=('civilization-','geckolib-','sable-')
    mods=client/'mods';mods.mkdir(exist_ok=True)
    assert mods.resolve()==(AREA/'client/mods').resolve()
    for f in mods.glob('*.jar'):f.unlink()
    for f in (prism/'mods').glob('*.jar'):
        if not f.name.startswith(runtime_prefixes):shutil.copy2(f,mods/f.name)
    for name in ('shaderpacks','resourcepacks'):
        dest=client/name;dest.mkdir(exist_ok=True)
        for f in (prism/name).glob('*'):
            if f.is_file():shutil.copy2(f,dest/f.name)
            elif f.is_dir():shutil.copytree(f,dest/f.name,dirs_exist_ok=True)
    config=client/'config';shutil.copytree(prism/'config',config,dirs_exist_ok=True)
    (config/'fml.toml').write_text('earlyWindowControl = false\n earlyWindowWidth = 1920\n earlyWindowHeight = 1080\n')
    options=(prism/'options.txt').read_text(encoding='utf8')
    overrides={'fullscreen':'false','overrideWidth':'1920','overrideHeight':'1080','pauseOnLostFocus':'false','tutorialStep':'none'}
    lines=[l for l in options.splitlines() if l.split(':',1)[0] not in overrides];lines += [k+':'+v for k,v in overrides.items()];(client/'options.txt').write_text('\n'.join(lines)+'\n',encoding='utf8')
    # Verify the shared Sable and GeckoLib artifacts match the main client byte-for-byte.
    for prefix in ('sable-','geckolib-'):
        source=next((prism/'mods').glob(prefix+'*.jar'));local=MOD/'.dev-libs'/source.name
        assert local.exists() and digest(local)==digest(source),'Runtime dependency differs: '+source.name
    return client,cfg

def client_heap_mib(cfg):
    instance=pathlib.Path(cfg['prismInstance'])
    local=configparser.ConfigParser();local.read(instance/'instance.cfg',encoding='utf8')
    if local.getboolean('General','OverrideMemory',fallback=False):
        return local.getint('General','MaxMemAlloc')
    global_settings=configparser.ConfigParser();global_settings.read(instance.parent.parent/'prismlauncher.cfg',encoding='utf8')
    return global_settings.getint('General','MaxMemAlloc',fallback=4096)


def run(mode,speed,seconds,warmup,directional,label,obstacle=0,dh_threads=0,heap_gib=None,dh_cache_from=None,turn_pulse=False):
    assert mode in ('stream','airship') and 0<speed<=5000 and 0<seconds<=120 and warmup>=0 and speed*(seconds+warmup)<=250000
    from lod_cache import build as prepare_lods
    lod_spec=prepare_lods()
    base=AREA/'baseline';spec=json.loads((base/'CORRIDOR_READY.json').read_text());client,cfg=prepare_client()
    heap_mib=heap_gib*1024 if heap_gib is not None else client_heap_mib(cfg)
    assert heap_mib>=512
    if dh_threads:
        assert 1<=dh_threads<=16
        config=client/'config/DistantHorizons.toml'
        changed,count=re.subn(r'(?m)^(\s*numberOfThreads\s*=\s*)\d+',lambda m:m[1]+str(dh_threads),config.read_text(encoding='utf8'))
        assert count==1,'Cannot locate DH worker count'
        config.write_text(changed,encoding='utf8')
    name=datetime.datetime.now().strftime('%Y%m%d-%H%M%S')+'-'+label+'-'+mode+'-'+str(speed)+'-'+uuid.uuid4().hex[:6]
    out=AREA/'results'/name;out.mkdir(parents=True)
    world=client/'saves'/name;world.mkdir(parents=True);(world/'region').mkdir()
    end=math.ceil((speed*(seconds+warmup)+2048)/512)
    regions=[f for f in (base/'region').glob('*.mca') if mode=='airship' or -2<=int(f.name.split('.')[1])<=end]
    print(f'PREPARE {name}: copying {len(regions)} regions',flush=True)
    shutil.copy2(base/'level.dat',world/'level.dat')
    (world/'data').mkdir();shutil.copyfile(base/'data/DistantHorizons.sqlite',world/'data/DistantHorizons.sqlite')
    assert digest(world/'data/DistantHorizons.sqlite')==lod_spec['database_sha256']
    cache_source=None
    if dh_cache_from is not None:
        prior=AREA/'results'/dh_cache_from
        assert prior.resolve().parent==(AREA/'results').resolve() and (prior/'COMPLETE.txt').exists()
        prior_manifest=json.loads((prior/'manifest.json').read_text())
        cache_source=pathlib.Path(prior_manifest['world'])/'data/DistantHorizons.sqlite'
        assert cache_source.resolve().parent.parent.parent==(client/'saves').resolve()
        assert prior_manifest['lod_baseline']['database_sha256']==lod_spec['database_sha256']
        # SQLite backup includes any committed WAL pages and does not edit the source.
        with sqlite3.connect(cache_source.resolve().as_uri()+'?mode=ro',uri=True) as source, sqlite3.connect(world/'data/DistantHorizons.sqlite') as dest:
            source.backup(dest)
    for f in regions:shutil.copyfile(f,world/'region'/f.name)
    (world/'BENCHMARK_COPY.json').write_text(json.dumps({'baseline':str(base),'manifest':digest(base/'CORRIDOR_READY.json'),'run':name}))
    # Verify loaded-route endpoints in the physical run copy before launch.
    for x,z in [(0,0),(int(speed*(seconds+warmup))//16,0),(-64,-64)]:check_region(world/'region'/f'r.{x//32}.{z//32}.mca',[(x,z)])
    settings=out/'settings';shutil.copytree(client/'config',settings);shutil.copy2(client/'options.txt',settings/'options.txt')
    sources=hashlib.sha256()
    for f in sorted([f for f in (MOD/'src').rglob('*') if f.is_file()]+[MOD/'build.gradle',MOD/'gradle.properties']):sources.update(str(f.relative_to(MOD)).encode());sources.update(f.read_bytes())
    manifest={'schema':1,'mode':mode,'speed':speed,'obstacle_x':obstacle,'dh_threads_override':dh_threads or None,'measurement_seconds':seconds,'moving_warmup_seconds':warmup,'stationary_settle_seconds':10,'directional':directional,'world':str(world),'baseline':spec,'lod_baseline':lod_spec,'source_sha256':sources.hexdigest(),'harness_sha256':{f.name:digest(f) for f in pathlib.Path(__file__).parent.glob('*.py')},'build_properties':(MOD/'gradle.properties').read_text(),'mods':{f.name:digest(f) for f in (client/'mods').glob('*.jar')},'main_client_mods':{f.name:digest(f) for f in (pathlib.Path(cfg['prismInstance'])/'minecraft/mods').glob('*.jar')},'shaderpacks':{str(f.relative_to(client/'shaderpacks')):digest(f) for f in (client/'shaderpacks').rglob('*') if f.is_file()},'resourcepacks':{str(f.relative_to(client/'resourcepacks')):digest(f) for f in (client/'resourcepacks').rglob('*') if f.is_file()},'config_sha256':{str(f.relative_to(settings)):digest(f) for f in settings.rglob('*') if f.is_file()},'logical_cpus':os.cpu_count(),'jvm_heap_gib':heap_mib/1024,'resolution':[1920,1080],'settings_source':'main Prism client; only window visibility, resolution, pause-on-focus-loss and tutorial differ','cache_state':'fresh JVM; fresh copy of pregenerated Minecraft and DH data; operating-system disk cache uncontrolled','jfr':'profile enabled in every run','clock':'movement target is blocks per game second; actual wall speed and TPS measured independently','timestamp_utc':datetime.datetime.now(datetime.timezone.utc).isoformat()}
    manifest['turn_pulse']=turn_pulse
    manifest['dh_cache_from']=dh_cache_from
    if cache_source is not None:
        manifest['cache_state']='fresh JVM and Minecraft baseline; DH database retained from named completed traversal; OS cache uncontrolled'
    manifest['dh_cache_before']=lod_cache_state(world/'data/DistantHorizons.sqlite')
    manifest['dh_cache_note']='Prepared renderable LOD data; missing chunk hashes can require conversion on first load. Counts do not prove hash matches.'
    (out/'manifest.json').write_text(json.dumps(manifest,indent=2))
    env=os.environ.copy();env['JAVA_HOME']=cfg['javaHome']
    command=[str(MOD/'gradlew.bat'),'--console=plain',f'-PbenchWorld={name}',f'-PbenchMode={mode}',f'-PbenchSpeed={speed}',f'-PbenchSeconds={seconds}',f'-PbenchWarmup={warmup}',f'-PbenchDirectional={str(directional).lower()}',f'-PbenchOutput={out}',f'-PbenchObstacle={obstacle}',f'-PbenchTurn={str(turn_pulse).lower()}',f'-PbenchHeap={heap_mib}M','runFlightBenchmark']
    print('RUN '+name,flush=True)
    with (out/'console.log').open('w',encoding='utf8') as log:result=subprocess.run(command,cwd=MOD,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=540)
    if result.returncode or not (out/'COMPLETE.txt').exists() or not (out/'frames.csv').exists():raise RuntimeError(f'Benchmark failed: {out}/console.log')
    screenshot=client/'screenshots/flight-benchmark.png'
    if screenshot.exists():shutil.copy2(screenshot,out/'flight.png')
    (out/'dh-cache-after.json').write_text(json.dumps(lod_cache_state(world/'data/DistantHorizons.sqlite'),indent=2))
    summary=summarize(out);print(json.dumps(summary,indent=2),flush=True);return summary

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--mode',choices=['stream','airship'],default='stream');ap.add_argument('--speeds',default='200,1000,5000');ap.add_argument('--seconds',type=float,default=20);ap.add_argument('--warmup',type=float,default=5);ap.add_argument('--repeats',type=int,default=2);ap.add_argument('--directional',choices=['on','off'],default='on');ap.add_argument('--obstacle',type=int,default=0);ap.add_argument('--dh-threads',type=int,default=0);ap.add_argument('--heap-gib',type=int,choices=range(2,17),default=None);ap.add_argument('--label',default='baseline');ap.add_argument('--cache-pair',action='store_true',help='Follow each fresh trial with a fresh-JVM trial retaining only its DH database');ap.add_argument('--turn-pulse',action='store_true',help='Two short opposing steering pulses during measurement');a=ap.parse_args()
    assert re.fullmatch(r'[A-Za-z0-9_-]+',a.label)
    # One shared hidden client directory: prevent concurrent benchmark runners.
    import msvcrt
    AREA.mkdir(parents=True,exist_ok=True)
    with (AREA/'runner.lock').open('a+b') as lock:
        if lock.tell()==0:lock.write(b'0');lock.flush()
        lock.seek(0)
        try:msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        except OSError:raise SystemExit('Another flight benchmark runner is active')
        try:
            for repeat in range(a.repeats):
                for speed in map(int,a.speeds.split(',')):
                    first=run(a.mode,speed,a.seconds,a.warmup,a.directional=='on',a.label+'-r'+str(repeat+1),a.obstacle,a.dh_threads,a.heap_gib,turn_pulse=a.turn_pulse)
                    if a.cache_pair:
                        run(a.mode,speed,a.seconds,a.warmup,a.directional=='on',a.label+'-repeat-r'+str(repeat+1),a.obstacle,a.dh_threads,a.heap_gib,first['run'],turn_pulse=a.turn_pulse)
        finally:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)
