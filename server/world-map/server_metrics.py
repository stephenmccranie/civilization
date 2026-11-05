"""Small Linux host sampler; publish numbers only, never raw /proc data."""
import logging
from pathlib import Path
import time


def read_sample(proc=Path('/proc')):
    cpu=[int(v) for v in (proc/'stat').read_text().splitlines()[0].split()[1:9]]
    if len(cpu)<8:raise ValueError('Incomplete CPU counters')
    memory={row.split(':')[0]:int(row.split()[1])*1024
        for row in (proc/'meminfo').read_text().splitlines() if ':' in row}
    total=memory['MemTotal'];available=memory['MemAvailable']
    if not 0<=available<=total or total<=0:raise ValueError('Invalid memory counters')
    interfaces={}
    for row in (proc/'net/dev').read_text().splitlines()[2:]:
        name,values=row.split(':',1);name=name.strip();values=values.split()
        if name!='lo':interfaces[name]=(int(values[0]),int(values[8]))
    routes=[]
    for row in (proc/'net/route').read_text().splitlines()[1:]:
        fields=row.split()
        if fields[1]=='00000000' and int(fields[3],16)&1 and fields[0] in interfaces:
            routes.append((int(fields[6]),fields[0]))
    names=(min(routes)[1],) if routes else tuple(sorted(interfaces))
    return dict(clock=time.monotonic(),cpuTotal=sum(cpu),cpuIdle=cpu[3]+cpu[4],
        ramUsedBytes=total-available,ramTotalBytes=total,
        network={name:interfaces[name] for name in names})


def summarize(current,previous=None):
    cpu=rx=tx=None
    if previous is not None:
        total=current['cpuTotal']-previous['cpuTotal']
        idle=current['cpuIdle']-previous['cpuIdle']
        if total>0 and 0<=idle<=total:cpu=round(100*(total-idle)/total,1)
        elapsed=current['clock']-previous['clock']
        if elapsed>0 and current['network'].keys()==previous['network'].keys():
            deltas=[(value[0]-previous['network'][name][0],value[1]-previous['network'][name][1])
                for name,value in current['network'].items()]
            if deltas and all(a>=0 and b>=0 for a,b in deltas):
                rx=sum(a for a,_ in deltas)/elapsed
                tx=sum(b for _,b in deltas)/elapsed
    return dict(updated=time.time(),cpuPercent=cpu,ramUsedBytes=current['ramUsedBytes'],
        ramTotalBytes=current['ramTotalBytes'],rxBytesPerSecond=rx,txBytesPerSecond=tx)


def worker(publish):
    previous=None
    while True:
        try:
            current=read_sample()
            publish(summarize(current,previous))
            previous=current
        except (OSError,ValueError,KeyError,IndexError):
            logging.warning('Server metrics refresh deferred')
            previous=None
        time.sleep(5)
