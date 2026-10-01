"""Report observational chunk-stage timings from a completed flight benchmark."""
import csv,json,collections,pathlib,sys
from run import percent

def report(root):
    manifest=json.loads((root/'manifest.json').read_text())
    start=(10+manifest['moving_warmup_seconds'])*1000
    end=start+manifest['measurement_seconds']*1000
    traces=list(csv.DictReader((root/'chunk-latency.csv').open()))
    cohort=[r for r in traces if r['request_ms'] and start<=float(r['request_ms'])<end]
    pairs=[('request_ms','nbt_start_ms'),('nbt_start_ms','nbt_ready_ms'),('nbt_ready_ms','decode_start_ms'),('decode_start_ms','decode_end_ms'),('decode_end_ms','load_ms'),('load_ms','snapshot_ms'),('request_ms','snapshot_ms')]
    if traces and 'init_light_start_ms' in traces[0]:
        pairs += [('decode_end_ms','init_light_start_ms'),('init_light_start_ms','init_light_end_ms'),('init_light_end_ms','light_start_ms'),('light_start_ms','light_end_ms'),('light_end_ms','spawn_start_ms'),('spawn_start_ms','spawn_end_ms'),('spawn_end_ms','full_create_ms'),('full_create_ms','publish_start_ms'),('publish_start_ms','load_ms')]
    if traces and 'init_pre_start_ms' in traces[0]:
        pairs += [('init_light_start_ms','init_pre_start_ms'),('init_pre_start_ms','init_pre_end_ms'),('init_pre_end_ms','init_light_end_ms'),('light_start_ms','light_pre_start_ms'),('light_pre_start_ms','light_pre_end_ms'),('light_pre_end_ms','light_end_ms')]
    stages={}
    for a,b in pairs:
        vals=[float(r[b])-float(r[a]) for r in cohort if r[a] and r[b] and float(r[b])>=float(r[a])]
        stages[a+' -> '+b]={'observations':len(vals),'p50_ms':percent(vals,.5) if vals else None,'p95_ms':percent(vals,.95) if vals else None,'max_ms':max(vals,default=None)}
    blockers=[r for r in csv.DictReader((root/'chunk-blockers.csv').open()) if r['phase']=='measure']
    counts=collections.Counter(r['state'] for r in blockers)
    ages=[float(r['request_age_ms']) for r in blockers if float(r['request_age_ms'])>=0]
    result={'run':root.name,'requested_cohort':len(cohort),'no_snapshot_by_finish':sum(not r['snapshot_ms'] for r in cohort),'nbt_started_before_corridor_request':sum(bool(r['nbt_start_ms']) and float(r['nbt_start_ms'])<float(r['request_ms']) for r in cohort),'stages':stages,'blocker_observations':len(blockers),'blocker_states':dict(counts),'blocker_age_p50_ms':percent(ages,.5) if ages else None,'blocker_age_p95_ms':percent(ages,.95) if ages else None,'limits':(root/'chunk-latency-limits.txt').read_text(),'caveats':'First observations per coordinate, not per reload. Missing endpoints are censored, not zero latency. Stage quantiles use different completed subsets and cannot be summed. Snapshot timing is tick-sampled. NBT callback order can race decode; negative intervals are excluded. Blocker counts repeat chunks across ticks and are not CPU time.'}
    if (root/'light-batches.csv').exists():
        batches=[float(r['duration_ms']) for r in csv.DictReader((root/'light-batches.csv').open()) if start/1000<=float(r['elapsed_s'])<end/1000]
        result['light_batches']={'count':len(batches),'total_ms':sum(batches),'p50_ms':percent(batches,.5) if batches else None,'p95_ms':percent(batches,.95) if batches else None,'max_ms':max(batches,default=None)}
    (root/'chunk-latency-summary.json').write_text(json.dumps(result,indent=2),encoding='utf8')
    return result
if __name__=='__main__':print(json.dumps(report(pathlib.Path(sys.argv[1])),indent=2))
