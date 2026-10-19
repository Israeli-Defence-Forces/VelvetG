"""Run identical isolated CPU/local-HTTP workloads; never interpret results as game FPS."""
import argparse, hashlib, json, os, pathlib, shutil, statistics, subprocess, tempfile
R=pathlib.Path(__file__).resolve().parents[1]
W=R/'work'
parser=argparse.ArgumentParser()
parser.add_argument('--profiles-only',action='store_true')
args=parser.parse_args()
java=pathlib.Path(os.environ.get('JAVA8',shutil.which('java') or 'java'))
jdk=pathlib.Path(os.environ['JAVA_HOME'])/'bin'
base=pathlib.Path(os.environ['BASELINE_JAR']).resolve()
mc=pathlib.Path(os.environ['MINECRAFT_JAR']).resolve()
deps=[str(p.resolve()) for p in pathlib.Path(os.environ['DEPENDENCIES_DIR']).rglob('*.jar')]
variants={'original':base}
previous=os.environ.get('PREVIOUS_PATCH_JAR')
if previous:variants['overhaul1']=pathlib.Path(previous).resolve()
variants['overhaul2']=(W/'patched.jar').resolve()
cp=os.pathsep.join([str(W/'patched.jar'),str(mc),*deps])
(W/'comparison-tests').mkdir(parents=True,exist_ok=True)
subprocess.run([str(jdk/'javac'),'--release','8','-proc:none','-cp',cp,'-d',str(W/'comparison-tests'),str(R/'tests/ComparisonBench.java'),str(R/'tests/ProfileCountBench.java')],check=True)
result={'runtime':'Java 8, Linux','kind':'synthetic CPU and local HTTP only; no Minecraft/native rendering/FPS','variants':{}}
with tempfile.TemporaryDirectory(prefix='VelvetG comparison ') as temp:
 for name,jar in variants.items():
  work=pathlib.Path(temp)/name;work.mkdir()
  classpath=os.pathsep.join([str(W/'comparison-tests'),str(jar),str(mc),*deps])
  def run(cls):
   process=subprocess.run([str(java),'-Xverify:all','-Djava.awt.headless=true','-cp',classpath,'me.eldodebug.soar.lean.'+cls],cwd=work,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=30)
   if process.returncode:raise RuntimeError(process.stdout)
   return process.stdout
  measured={'sha256':hashlib.sha256(jar.read_bytes()).hexdigest()}
  output=run('ProfileCountBench');measured['localHttpProfileRequestsForThreeSelections']=int(output.strip().split('=')[-1])
  if name!='original' and not args.profiles_only:
   series=[]
   for _ in range(3):series.append(float(run('ComparisonBench').strip().split('=')[-1]))
   measured['synthetic40RowNsPerCall']=series
   measured['medianSynthetic40RowNsPerCall']=statistics.median(series)
  result['variants'][name]=measured
print(json.dumps(result,indent=2))
(R/'reports/build-comparison.json').write_text(json.dumps(result,indent=2)+'\n')
