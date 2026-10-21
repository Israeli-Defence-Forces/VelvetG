#!/usr/bin/env python3
"""Repeatable mock JVM callback/replay benchmark. No Minecraft frame or FPS measurements."""
import argparse
import json
import os
from pathlib import Path
try:
    import resource
except ImportError:
    resource=None
import shutil
import subprocess
import tempfile
import time
from combat_analyzer import analyze, read_trace

ROOT=Path(__file__).resolve().parent


def benchmark(batches=5000,repeats=3):
    java=shutil.which('java');javac=shutil.which('javac')
    if not java or not javac: raise RuntimeError('JDK required')
    if not 100<=batches<=10000 or not 1<=repeats<=10: raise ValueError('Bounded benchmark sizes required')
    version=subprocess.check_output([javac,'-version'],stderr=subprocess.STDOUT,text=True).strip()
    flags=['-source','8','-target','8'] if version.startswith('javac 1.8') else ['--release','8']
    runs=[]
    with tempfile.TemporaryDirectory() as d:
        d=Path(d);classes=d/'classes';classes.mkdir()
        sources=list((ROOT/'tests-v4').glob('*.java'))+[ROOT/'tests/org/lwjgl/input/Keyboard.java']
        sources+=list((ROOT/'src/me/eldodebug/soar/combatiq').glob('*.java'))
        subprocess.run([javac,*flags,'-d',str(classes),*map(str,sources)],check=True,capture_output=True)
        for mode in ('disabled','enabled-idle','recording','compact-hud','expanded-diagnostics','deep-diagnostics'):
            for repeat in range(repeats):
                export=d/(mode+str(repeat));export.mkdir()
                before=resource.getrusage(resource.RUSAGE_CHILDREN) if resource else None;start=time.perf_counter()
                p=subprocess.run([java,'-Dvelvet.combatiq.enabled='+str(mode not in ('disabled','enabled-idle')).lower(),
                    '-Dvelvet.combatiq.exportDir='+str(export),'-cp',str(classes),'BenchV4',mode,str(batches)],
                    check=True,capture_output=True,text=True,timeout=30)
                elapsed=time.perf_counter()-start;after=resource.getrusage(resource.RUSAGE_CHILDREN) if resource else None
                run=dict(mode=mode,repeat=repeat,wall_seconds_including_JVM=elapsed,
                         child_cpu_seconds=after.ru_utime+after.ru_stime-before.ru_utime-before.ru_stime if resource else None,
                         timing=json.loads(p.stdout),serialized_rows=0,event_loss=0,bytes=0)
                files=list(export.glob('*.csv'))
                if files:
                    t=read_trace(files[0]);run.update(serialized_rows=len(t['events']),event_loss=t['loss'],bytes=files[0].stat().st_size)
                    start=time.perf_counter();a=analyze(files[0]);run['offline_analysis_seconds']=time.perf_counter()-start
                    run['recording_complete']=t['complete']
                runs.append(run)
    return dict(kind='SYNTHETIC_CALLBACK_BATCHES_NOT_GAMEPLAY',compiler=version,runs=runs,
                limitations=['One JVM per run; warmup is limited; timing includes nanoTime measurement overhead',
                             'HUD modes consume immutable snapshots; no NanoVG rendering or native layout timing',
                             'No frame times, FPS, GPU cost or gameplay allocation rate measured; allocation is producer thread only',
                             'Same offered callbacks in all modes; deep mode additionally retains unrelated numeric entity teleports',
                             'CPU includes startup/shutdown; loss and serialized rows include warmup'])


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--out',type=Path,required=True)
    p.add_argument('--batches',type=int,default=5000);p.add_argument('--repeats',type=int,default=3)
    a=p.parse_args();r=benchmark(a.batches,a.repeats)
    with a.out.open('x') as f:json.dump(r,f,indent=2,allow_nan=False)
    print(a.out)

if __name__=='__main__':main()
