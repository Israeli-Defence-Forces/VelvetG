"""Default/empty output path regression: launch collector outside its own directory."""
from pathlib import Path
import subprocess,os,tempfile,zipfile,json,hashlib,shutil
ROOT=Path(__file__).resolve().parents[1]
PS=Path(os.environ.get('POWERSHELL_EXECUTABLE') or shutil.which('pwsh') or shutil.which('powershell') or str(ROOT/'tools/pwsh/pwsh'))
ENV=dict(os.environ,XDG_CACHE_HOME=str(ROOT/'cache'),XDG_CONFIG_HOME=str(ROOT/'config'),XDG_DATA_HOME=str(ROOT/'data'))
checks=0
def check(test,message):
 global checks
 if not test:raise AssertionError(message)
 checks+=1

def snapshot(root):return {str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in root.rglob('*') if p.is_file()}
with tempfile.TemporaryDirectory(prefix='collector default path tests ',dir=ROOT) as td:
 base=Path(td);game=base/'Minecraft with spaces';jar=game/'libraries/me/eldodebug/GlideClient/2.0.9.6-lean-fixed-20261016/GlideClient-2.0.9.6-lean-fixed-20261016.jar';jar.parent.mkdir(parents=True)
 with zipfile.ZipFile(jar,'w') as z:z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\n')
 scriptdir=base/'Extracted collector with spaces';scriptdir.mkdir();script=scriptdir/'Collect-VelvetG.ps1';script.write_bytes((ROOT/'collector/Collect-VelvetG.ps1').read_bytes())
 cwd=base/'Unrelated working directory';cwd.mkdir();before=snapshot(game)
 def run(*extra):return subprocess.run([str(PS),'-NoProfile','-File',str(script),'-MinecraftDir',str(game),*map(str,extra)],env=ENV,cwd=cwd,capture_output=True,text=True)
 for extra in [(),('-OutputParent',''),('-OutputParent','  '),('-OutputParent',str(scriptdir)+os.sep+'.')]:
  existing=set(scriptdir.glob('VelvetG-Collection-*'));p=run(*extra)
  check(p.returncode==0,'default/empty output path resolves: '+p.stdout+p.stderr)
  created=set(scriptdir.glob('VelvetG-Collection-*'))-existing;check(len(created)==1,'output is next to copied script')
  folder=created.pop();check((folder/'VelvetG-DIAGNOSTICS.zip').is_file(),'collection finishes successfully')
  with zipfile.ZipFile(folder/'VelvetG-DIAGNOSTICS.zip') as z:check(json.loads(z.read('collection.json'))['collectorRevision']==2,'revision identifies corrected collector')
 check(not list(cwd.iterdir()),'unrelated process working directory is untouched')
 check(snapshot(game)==before,'Minecraft installation stays byte-identical')
 explicit=base/'Explicit output with spaces';explicit.mkdir();p=run('-OutputParent',explicit);check(p.returncode==0 and len(list(explicit.glob('VelvetG-Collection-*')))==1,'explicit override remains supported')
 bad=subprocess.run([str(PS),'-NoProfile','-File',str(script),'-MinecraftDir','   '],env=ENV,cwd=cwd,capture_output=True,text=True)
 check(bad.returncode!=0 and 'MinecraftDir is empty' in bad.stderr,'blank game path has labeled error, not generic GetFullPath error')
print(f'PASS: {checks} default/empty/explicit output path and unrelated-working-directory checks')
