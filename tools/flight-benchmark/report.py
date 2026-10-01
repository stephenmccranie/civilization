"""Aggregate finished runs without silently mixing smoke runs or differing settings."""

import argparse,html,json,pathlib,statistics

from corridor import AREA



def report(label):

    runs=[]

    for f in sorted((AREA/'results').glob('*/summary.json')):

        if label not in f.parent.name:continue

        s=json.loads(f.read_text());m=json.loads((f.parent/'manifest.json').read_text());s['order']='directional' if m['directional'] else 'spiral';runs.append((s,m,f.parent))

    assert runs,'No matching finished runs'

    columns=[('Run','run'),('Mode','mode'),('Order','order'),('Target b/s','target_bps'),('Actual b/s','actual_wall_bps'),('Camera b/s','camera_wall_bps'),('TPS','tps'),('Tick p95 ms','tick_work_p95_ms'),('Tick p99 ms','tick_work_p99_ms'),('CPU %','process_cpu_mean_pct'),('FPS','fps_mean'),('1% low FPS','fps_1pct_low'),('Frame p99 ms','frame_interval_p99_ms'),('Hold sample %','hold_time_pct'),('Hold episodes','hold_episodes'),('Readiness brake sample %','streaming_brake_sample_pct'),('Body p05 b/s','body_bps_p05'),('Body p95 b/s','body_bps_p95'),('Client near loaded %','client_near_loaded_mean_pct'),('New chunks','new_chunks')]

    def value(v):return f'{v:.2f}' if isinstance(v,float) else html.escape(str(v))

    table='<table><thead><tr>'+''.join('<th>'+c[0]+'</th>' for c in columns)+'</tr></thead><tbody>'

    for s,m,path in runs:

        table+=('<tr style="background:#50272b">' if s.get('client_near_loaded_mean_pct',100)<99 else '<tr>')+''.join('<td>'+value(s.get(key) if s.get(key) is not None else 'n/a')+'</td>' for _,key in columns)+'</tr>'

    table+='</tbody></table>'

    receipts='<ul>'+''.join('<li>'+html.escape(p.name)+': <a href="results/'+p.name+'/ticks.csv">ticks</a> Ã‚Â· <a href="results/'+p.name+'/frames.csv">frames</a> Ã‚Â· <a href="results/'+p.name+'/profile.jfr">JFR</a> Ã‚Â· <a href="results/'+p.name+'/manifest.json">manifest</a> Ã‚Â· <a href="results/'+p.name+'/loaded-mods.json">loaded mods</a> | <a href="results/'+p.name+'/flight.png">screenshot</a>'+(' | <a href="results/'+p.name+'/safety.csv">physics safety</a>' if (p/'safety.csv').exists() else '')+'</li>' for _,_,p in runs)+'</ul>'

    groups={}

    for s,m,p in runs:groups.setdefault((s['mode'],s['target_bps'],m['directional']),[]).append(s)

    aggregates=[]

    for (mode,speed,directional),values in groups.items():

        item={'mode':mode,'target_bps':speed,'directional':directional,'repeats':len(values)}

        for k in ('actual_wall_bps','tps','tick_work_p95_ms','process_cpu_mean_pct','fps_mean','fps_1pct_low','hold_time_pct'):

            item[k]={'mean':statistics.mean(v[k] for v in values),'min':min(v[k] for v in values),'max':max(v[k] for v in values)}

        aggregates.append(item)

    data={'label':label,'runs':[s for s,_,_ in runs],'groups':aggregates,'source_fingerprints':sorted({m['source_sha256'] for _,m,_ in runs}),'config_fingerprints_match':all(m['config_sha256']==runs[0][1]['config_sha256'] for _,m,_ in runs),'mod_fingerprints_match':all(m['mods']==runs[0][1]['mods'] for _,m,_ in runs)}

    (AREA/(label+'-report.json')).write_text(json.dumps(data,indent=2))

    fingerprint_notice='<p>Source variants: '+str(len(data['source_fingerprints']))+'; configuration fingerprints match: '+str(data['config_fingerprints_match'])+'; mod fingerprints match: '+str(data['mod_fingerprints_match'])+'. Check manifests before attributing differences to one setting.</p>'

    text='<html><meta charset="utf-8"><title>Flight benchmark</title><style>body{font:15px system-ui;margin:32px;background:#111923;color:#e9eff5}table{border-collapse:collapse;font-size:13px}td:first-child{max-width:190px;overflow-wrap:anywhere}td,th{padding:9px;border:1px solid #3c4c5d;text-align:right}th{background:#243446}a{color:#8bc8ff}p{max-width:1000px}li{margin:12px 0}</style><h1>Civilization flight benchmark</h1><p>Pregenerated superflat route: 262,144 blocks long, 2,048 wide, plus 1,024-block end buffers. Ground y=63; flight y=128. Main-client enabled mods, Photon, resource packs and graphics settings. Each trial starts a fresh JVM and world copy, including a prepared uniform DH cache. OS disk cache is uncontrolled.</p><p>Streaming mode moves a spectator by target/20 each server tick; it measures streaming capacity, not airship physics. Airship mode uses the real controller and records actual speed and safety holds. JFR and per-frame telemetry run in every trial. FPS uses frame intervals; 1% low is the reciprocal of the mean slowest 1% of intervals. CPU is Java process CPU normalized over all logical processors.</p>'+fingerprint_notice+table+'<p>Body-speed percentiles and hold episodes expose stop/reaccelerate cycles that average TPS or FPS can hide. When present, safety.csv records individual physics checks and the observed rejection reason; it does not alter safety decisions.</p>'+'<p>Do not interpret a fast streaming test as proof an airship sustains that speed. Terrain is deliberately simple; these results do not predict forest/world-generation costs. Only runs with zero new chunks qualify as pregenerated-route tests. Red rows have incomplete nearby client chunk coverage: a fast camera and good FPS do not establish usable streaming at that speed. Prebuilt LODs can keep terrain visible while ordinary chunks lag behind. The development JVM uses a 6 GiB heap and the project JDK, not Prism&#39;s launcher JVM. Shader and mod caches outside the fresh world may warm between JVMs; repeat and reverse A/B order.</p><h2>Evidence</h2>'+receipts+'</html>'

    output=AREA/(label+'-report.html');output.write_text(text,encoding='utf8');print(output)



if __name__=='__main__':

    p=argparse.ArgumentParser();p.add_argument('--label',default='fullmods');report(p.parse_args().label)

