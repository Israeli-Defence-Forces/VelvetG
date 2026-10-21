#!/usr/bin/env python3
"""Synthetic trainer callback/snapshot cost; not game FPS, terrain, GPU or audio cost."""
import argparse,json,shutil,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
 javac=shutil.which('javac');java=shutil.which('java');v=subprocess.check_output([javac,'-version'],stderr=subprocess.STDOUT,text=True)
 flags=['-source','8','-target','8'] if v.startswith('javac 1.8') else ['--release','8']
 with tempfile.TemporaryDirectory() as d:
  sources=list((ROOT/'src').rglob('*.java'))+list((ROOT/'tests').rglob('*.java'))
  subprocess.run([javac,*flags,'-d',d,*map(str,sources)],check=True,capture_output=True)
  runs=[]
  for mode in ('disabled','native-bridge','core-max-history'):
   for repeat in range(3):
    r=subprocess.run([java,'-Xverify:all','-cp',d,'BenchTrainer',mode],check=True,capture_output=True,text=True,timeout=30)
    run=json.loads(r.stdout);run['repeat']=repeat;runs.append(run)
 result=dict(scope='SYNTHETIC_NOT_MINECRAFT',compiler=v.strip(),runs=runs,limitations=['Native bridge uses fake world/collision/sound objects','Core mode uses maximum 600-sample immutable graph snapshots','No NanoVG/GPU/audio hardware/game FPS or frame-time measurement','Tight loop with 1000 warmup + 5000 timed batches; allocation includes instrumentation'])
 a.out.write_text(json.dumps(result,indent=2)+'\n');print(a.out)
if __name__=='__main__':main()
