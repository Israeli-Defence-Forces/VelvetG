import pathlib,subprocess,os,zipfile,hashlib,json,shutil
R=pathlib.Path(__file__).resolve().parent;W=R/'work'
MC=pathlib.Path(os.environ.get('MINECRAFT_JAR','minecraft-1.8.9.jar')).resolve()
BASE=pathlib.Path(os.environ.get('BASELINE_JAR','original-client.jar')).resolve()
JDK=pathlib.Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else pathlib.Path(shutil.which('javac') or 'javac').resolve().parent
JAVA8=pathlib.Path(os.environ.get('JAVA8',shutil.which('java') or 'java'))
deps=[str(x.resolve()) for x in pathlib.Path(os.environ.get('DEPENDENCIES_DIR','libraries')).rglob('*.jar')];cp=os.pathsep.join([str(W/'patched.jar'),str(MC)]+deps)
def run(a):
 p=subprocess.run([str(x) for x in a],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,cwd=W/'test-run')
 if p.returncode:
  print(p.stdout,flush=True);raise SystemExit('Check failed: '+str(a[-1]))
 return p.stdout

(W/'test-run').mkdir(exist_ok=True);(W/'tests').mkdir(exist_ok=True)
print(run([JDK/'javac','--release','8','-proc:none','-cp',cp,'-d',W/'tests',*sorted((R/'tests').glob('*.java'))]))
logs=[]
logs.append(run([JAVA8,'-Xverify:all','-cp',str(W/'tests')+os.pathsep+cp,'me.eldodebug.soar.lean.tests.StructuralChecks',W/'patched.jar',BASE]));print(logs[-1])
for baseline in [BASE,W/'patched.jar']:
 c=os.pathsep.join([str(W/'tests'),str(baseline),str(MC)]+deps)
 logs.append(run([JAVA8,'-Xverify:all','-cp',c,'me.eldodebug.soar.lean.tests.StartupClassChecks',baseline]));print(baseline.name,logs[-1])
logs.append(run([JAVA8,'-Xverify:all','-Djava.awt.headless=true','-cp',str(W/'tests')+os.pathsep+cp,'me.eldodebug.soar.lean.KernelChecks']));print(logs[-1]);logs.append(run([JAVA8,'-Xverify:all','-Djava.awt.headless=true','-cp',str(W/'tests')+os.pathsep+cp,'me.eldodebug.soar.lean.SchedulerChecks']));print(logs[-1]);logs.append(run([JAVA8,'-Xverify:all','-Djava.awt.headless=true','-cp',str(W/'tests')+os.pathsep+cp,'me.eldodebug.soar.lean.CacheChecks']));print(logs[-1]);logs.append(run([JAVA8,'-Xverify:all','-cp',str(W/'tests')+os.pathsep+cp,'me.eldodebug.soar.lean.ConfigWriterChecks']));print(logs[-1]);logs.append(run([JAVA8,'-Xverify:all','-cp',str(W/'tests')+os.pathsep+cp,'me.eldodebug.soar.discord.ipc.entities.pipe.DiscordChecks']));print(logs[-1]);logs.append(run([JAVA8,'-Xverify:all','-cp',str(W/'tests')+os.pathsep+cp,'me.eldodebug.soar.lean.ProfileCancelChecks']));print(logs[-1]);bench=run([JAVA8,'-Xverify:all','-cp',str(W/'tests')+os.pathsep+cp,'me.eldodebug.soar.lean.OfflineBench']);print(bench);(R/'reports/offline-benchmark.txt').write_text(bench);(R/'reports/test-results.txt').write_text('\n'.join(logs))
