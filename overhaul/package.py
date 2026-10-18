import pathlib,zipfile,json,hashlib,shutil
R=pathlib.Path(__file__).resolve().parent;D=R/'dist/VelvetG-Lean-Overhaul1';
if D.exists():shutil.rmtree(D)
D.mkdir(parents=True,exist_ok=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
for p in (R/'installer').iterdir():
 if p.is_file():shutil.copy2(p,D/p.name)
with zipfile.ZipFile(D/'patcher.jar','w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted((R/'work/tools').glob('*.class')):z.write(p,p.name)
shutil.copy2(R/'work/additions.jar',D/'additions.jar')
report=json.loads((R/'reports/modified-files.json').read_text());(D/'entries.sha256').write_text(''.join(h+'  '+n+'\n' for n,h in sorted(report['entries'].items())))
for folder in ['src','tools','tests','reports','resources']:
 shutil.copytree(R/folder,D/folder,dirs_exist_ok=True,ignore=shutil.ignore_patterns('__pycache__','*.pyc'))
for name in ['README.md','LICENSE-NEW-CODE.txt','build.py','run-tests.py','package.py']:
 if (R/name).exists():shutil.copy2(R/name,D/name)
manifest={'schema':1,'baselineSha256':report['baselineSha256'],'files':{p.name:sha(p) for p in D.iterdir() if p.is_file() and p.name!='package.json'}}
(D/'package.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(D)
