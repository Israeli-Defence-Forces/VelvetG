#!/usr/bin/env python3
"""Reproducible source additions + targeted ASM transforms against one exact private baseline."""
import os,pathlib,subprocess,sys,hashlib,zipfile,json,shutil
R=pathlib.Path(__file__).resolve().parent
BASE=pathlib.Path(sys.argv[1] if len(sys.argv)>1 else os.environ.get('BASELINE_JAR','original-client.jar')).resolve()
JDK=pathlib.Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else pathlib.Path(shutil.which('javac') or 'javac').resolve().parent
MC=pathlib.Path(os.environ.get('MINECRAFT_JAR','minecraft-1.8.9.jar')).resolve()
DEP=pathlib.Path(os.environ.get('DEPENDENCIES_DIR','libraries')).resolve()
W=R/'work';W.mkdir(exist_ok=True)
def run(args):subprocess.run([str(x) for x in args],check=True,cwd=R)
assert hashlib.sha256(BASE.read_bytes()).hexdigest()=='3e4b0190e26b4a1540ac23ff018a8c8280b8ab447c34cce0fc5bf8cc6c2a6813','Wrong baseline; refusing to patch'
deps=list(DEP.rglob('*.jar')); toolcp=os.pathsep.join([str(W/'tools'),str(BASE)]+[str(x) for x in deps])
run([JDK/'javac','--release','8','-proc:none','-cp',toolcp,'-d',W/'tools',*sorted((R/'tools').glob('*.java'))])
for src,out in [(BASE,'baseline-compile.jar'),(MC,'minecraft-compile.jar')]:
 target=W/out; marker=W/(out+'.sha256'); digest=hashlib.sha256(src.read_bytes()).hexdigest()
 if not target.exists() or not marker.exists() or marker.read_text()!=digest:
  run([JDK/'java','-cp',toolcp,'Remap','prefix',src,target]);marker.write_text(digest)
cp=os.pathsep.join([str(W/'baseline-compile.jar'),str(W/'minecraft-compile.jar')]+[str(x) for x in deps])
run([JDK/'javac','--release','8','-encoding','UTF-8','-proc:none','-cp',cp,'-d',W/'classes',*sorted((R/'src').rglob('*.java'))])
with zipfile.ZipFile(W/'additions-compile.jar','w') as z:
 for p in sorted((W/'classes').rglob('*.class')):z.write(p,p.relative_to(W/'classes').as_posix())
 for p in sorted((R/'resources').rglob('*')):
  if p.is_file():z.write(p,p.relative_to(R/'resources').as_posix())
run([JDK/'java','-cp',toolcp,'Remap','strip',W/'additions-compile.jar',W/'additions.jar'])
(W/'patched.jar').unlink(missing_ok=True)
run([JDK/'java','-cp',toolcp,'LeanPatch',BASE,MC,W/'additions.jar',W/'patched.jar'])
with zipfile.ZipFile(BASE) as a,zipfile.ZipFile(W/'patched.jar') as b:
 changed={n:hashlib.sha256(b.read(n)).hexdigest() for n in b.namelist() if n not in a.namelist() or a.read(n)!=b.read(n)}
 report={'baselineSha256':hashlib.sha256(BASE.read_bytes()).hexdigest(),'patchedSha256':hashlib.sha256((W/'patched.jar').read_bytes()).hexdigest(),'entries':changed,'preservedEntries':len(a.namelist())-len(set(changed)&set(a.namelist()))}
 (R/'reports/modified-files.json').write_text(json.dumps(report,indent=2)+'\n')
print('Build complete:',len(changed),'modified/new entries')
