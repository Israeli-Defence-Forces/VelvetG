#!/usr/bin/env python3
"""Consolidate the installable trainer, exact installer, source and validation evidence."""
import argparse,hashlib,json,sys,tempfile,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT.parent/'combatiq/native-v4'))
from release_bundle import archive
from build_v4 import require
def sha(b):return hashlib.sha256(b).hexdigest()
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--jar',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
 build=json.loads(a.jar.with_suffix('.manifest.json').read_text());require(sha(a.jar.read_bytes())==build['output_sha256'],'JAR hash mismatch')
 script=a.jar.with_suffix('.install.py').read_bytes();require(sha(script)==build['installer_sha256'],'Installer mismatch')
 entries={a.jar.name:a.jar.read_bytes(),a.jar.with_suffix('.manifest.json').name:a.jar.with_suffix('.manifest.json').read_bytes(),a.jar.with_suffix('.install.py').name:script,'README.md':(ROOT/'README.md').read_bytes()}
 for action in ('install','check','rollback','uninstall','recover'):
  entries[action.upper()+'.cmd']=('@echo off\r\npy -3 "%~dp0'+a.jar.with_suffix('.install.py').name+'" '+action+' %*\r\nexit /b %errorlevel%\r\n').encode()
 for f in (ROOT/'reports').rglob('*'):
  if f.is_file():entries['validation/'+f.relative_to(ROOT/'reports').as_posix()]=f.read_bytes()
 for f in ROOT.rglob('*'):
  if f.is_file() and not any(x in f.relative_to(ROOT).parts for x in ('build','__pycache__','reports')) and f.suffix in ('.py','.java','.md'):
   entries['source/knockback-trainer/'+f.relative_to(ROOT).as_posix()]=f.read_bytes()
 for name in ('build_v4.py','install_release.py','test_native_gui_contract.py'):
  entries['source/combatiq/native-v4/'+name]=(ROOT.parent/'combatiq/native-v4'/name).read_bytes()
 content=dict(version='Knockback Trainer RC1',source_commit=build['source_commit'],minecraft_runtime_validated=False,sha256={n:sha(b) for n,b in sorted(entries.items())})
 entries['CONTENTS.json']=(json.dumps(content,indent=2)+'\n').encode();pin=sha(entries['CONTENTS.json'])
 entries['VERIFY.py']=('''import hashlib,json
from pathlib import Path
root=Path(__file__).resolve().parent
if hashlib.sha256((root/'CONTENTS.json').read_bytes()).hexdigest()!=%r:raise SystemExit('FAIL: manifest modified')
d=json.loads((root/'CONTENTS.json').read_text())
for n,h in d['sha256'].items():
 if hashlib.sha256((root/n).read_bytes()).hexdigest()!=h:raise SystemExit('FAIL: '+n)
print('PASS: %%d payload artifacts verified; Minecraft runtime remains unverified'%%len(d['sha256']))
'''%pin).encode()
 entries['SHA256SUMS.txt']=''.join(sha(b)+'  '+n+'\n' for n,b in sorted(entries.items())).encode()
 archive(a.out,entries)
 with zipfile.ZipFile(a.out) as z:
  require(z.testzip() is None,'Corrupt release ZIP');require(all(z.read(n)==b for n,b in entries.items()),'ZIP content mismatch')
 a.out.with_suffix('.zip.sha256').write_text(sha(a.out.read_bytes())+'  '+a.out.name+'\n')
 print(json.dumps(dict(path=str(a.out),bytes=a.out.stat().st_size,sha256=sha(a.out.read_bytes()),payload_artifacts=len(content['sha256'])),indent=2))
if __name__=='__main__':main()
