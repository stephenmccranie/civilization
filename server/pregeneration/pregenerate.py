#!/usr/bin/python3
"""Persistent full chunks and DH LODs in circular bands of DH-sized tiles around 0,0."""
import json
import os
import re
import shutil
import signal
import subprocess
import time
from pathlib import Path
from pregen_log import LogTail
from radial import migrate,new_state,next_patch,advance,commands,TILE_SIZE,CHUNKY_RADIUS

ROOT=Path('/srv/minecraft')
STATE=ROOT/'pregeneration/state.json'
LOG=ROOT/'logs/latest.log'
PIPE='/run/minecraft/console'
POLL_SECONDS=0.25
# A failed chunk never completes, so its Chunky or DH job would otherwise wait forever.
FAILURES=('Pregen failed:','Task stopped for minecraft:overworld.','Integer must not be less than','Failed to load chunk')
# Server lines follow ']: ' directly, while chat starts with <name>, so chat never matches.
PRESENCE=re.compile(r'\]: (\w+) (joined|left) the game$')
ONLINE=re.compile(r'\]: There are \d+ of a max of \d+ players online:(.*)$')
stopped=False


def stop(*_):
    global stopped
    stopped=True


def command(s):
    fd=os.open(PIPE,os.O_WRONLY|os.O_NONBLOCK)
    try: os.write(fd,(s+'\n').encode())
    finally: os.close(fd)
    print('console: '+s,flush=True)


def pause():
    command('chunky pause minecraft:overworld')
    command('dh pregen stop')


def track_players(online,lines):
    """Who is online, from list replies and the join and leave lines since."""
    for line in lines:
        if listed:=ONLINE.search(line.rstrip()):
            online.clear()
            online.update(name.strip() for name in listed.group(1).split(',') if name.strip())
        elif m:=PRESENCE.search(line.rstrip()):
            (online.add if m.group(2)=='joined' else online.discard)(m.group(1))


def save(s):
    p=STATE.with_suffix('.tmp')
    p.write_text(json.dumps(s,indent=2)+'\n')
    p.replace(STATE)


def main():
    signal.signal(signal.SIGTERM,stop)
    signal.signal(signal.SIGINT,stop)
    s=migrate(json.loads(STATE.read_text())) if STATE.exists() else new_state()
    tail=LogTail(LOG)
    pid=None
    current='0'
    last_pid_check=0
    issued=False
    chunks_started=False
    ready=False
    last_probe=0
    online=set()
    try:
        while not stopped:
            if shutil.disk_usage(ROOT).free<50*1024**3:
                s['status']='paused: less than 50 GiB free'
                save(s)
                print(s['status'],flush=True)
                break
            now=time.monotonic()
            if now-last_pid_check>=1:
                try:
                    current=subprocess.check_output(['systemctl','show','minecraft','-p','MainPID','--value'],text=True,timeout=5).strip()
                    last_pid_check=now
                except (subprocess.SubprocessError,OSError):
                    if stopped: break
                    time.sleep(3)
                    continue
            if current=='0' or not LOG.exists() or not os.path.exists(PIPE):
                issued=False
                ready=False
                time.sleep(3)
                continue
            if current!=pid:
                pid=current
                issued=False
                ready=False
                last_probe=0
                tail.close()
            lines=tail.read()
            if any('Stopping the server' in line for line in lines):
                # Chunky stops its task as the server shuts down; that is no generation failure.
                issued=False
                ready=False
                online.clear()
                time.sleep(1)
                continue
            ready=ready or any('Done (' in line or ('There are ' in line and 'players online:' in line) for line in lines)
            if not ready:
                # Startup's "Done" line may be in yesterday's rotated log.
                # The console responds only once Minecraft is ready to execute commands.
                if time.monotonic()-last_probe>=10:
                    # The console has no reader for the moments a restarting server is down.
                    try: command('list')
                    except OSError: pass
                    last_probe=time.monotonic()
                time.sleep(1)
                continue
            track_players(online,lines)
            if online:
                # Players come first: Distant Horizons generates around them while pregeneration waits.
                if issued:
                    pause()
                    issued=False
                    s['status']='waiting while players are online'
                    save(s)
                    print(s['status'],flush=True)
                time.sleep(1)
                continue
            if issued and s['phase']=='chunks':
                expected=f"Task started in minecraft:overworld for the square region centered at {s['center'][0]}, {s['center'][1]} with radius {CHUNKY_RADIUS}."
                for index,line in enumerate(lines):
                    if expected in line:
                        chunks_started=True
                        lines=lines[index:]  # Ignore an old worker's stop before our start.
                        break
            if not issued:
                x,z=next_patch(s)
                if max(abs(x),abs(z))+TILE_SIZE//2>=29999984:
                    s['status']='paused at Minecraft world boundary'
                    save(s)
                    break
                track_players(online,tail.discard())
                if online: continue
                s['center']=[x,z]
                s['status']='running'
                save(s)
                for instruction in commands(s['phase'],x,z): command(instruction)
                issued=True
                chunks_started=False
                print(json.dumps(s),flush=True)
            elif s['phase']=='chunks' and any('A task was already started for this world.' in line for line in lines):
                # A paused task survives controller shutdown. Confirm only
                # after our replacement start explicitly requests it; never
                # continue an old selection or blindly confirm another action.
                command('chunky confirm')
            elif s['phase']=='chunks' and not chunks_started and any(
                    'Task already started for minecraft:overworld!' in line
                    or 'Task stopped for minecraft:overworld.' in line for line in lines):
                # Chunky pause returns before an in-flight terrain task drains.
                # Retry our selection after that old worker releases the world.
                issued=False
            elif s['phase']=='chunks' and chunks_started and any('Task finished for minecraft:overworld.' in line for line in lines):
                s['phase']='lods'
                save(s)
                issued=False
                continue
            elif s['phase']=='lods' and any('Pregen is complete' in line for line in lines):
                s['tile']+=1
                advance(s)
                s['phase']='chunks'
                save(s)
                issued=False
                continue
            elif failure:=next((line for line in lines if any(f in line for f in FAILURES)),None):
                s['status']='paused after generation failure: '+failure.rsplit(']: ',1)[-1].strip()
                save(s)
                print(s['status'],flush=True)
                break
            time.sleep(POLL_SECONDS)
    finally:
        tail.close()
        try: pause()
        except OSError: pass
        if s.get('status')=='running':
            s['status']='paused'
            save(s)


if __name__=='__main__': main()
