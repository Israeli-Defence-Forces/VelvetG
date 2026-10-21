#!/usr/bin/env python3
"""Package a verified owner-only RC1 binary, tools and evidence without baseline inputs."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import zipfile
from build_v4 import V3,MINECRAFT,pinned_installer,require
ROOT=Path(__file__).resolve().parent


def sha(raw):return hashlib.sha256(raw).hexdigest()


def archive(path,entries):
    with zipfile.ZipFile(path,'x',zipfile.ZIP_DEFLATED) as z:
        for name,raw in sorted(entries.items()):
            item=zipfile.ZipInfo(name,(1980,1,1,0,0,0));item.compress_type=zipfile.ZIP_DEFLATED;item.external_attr=0o100644<<16
            z.writestr(item,raw)


def analyzer(path):
    entries={p.name:p.read_bytes() for p in (ROOT/'combat_analyzer.py',ROOT/'calibrate.py')}
    entries.update({'analytics/'+p.name:p.read_bytes() for p in (ROOT/'analytics').glob('*.py')})
    entries['__main__.py']=b'''import sys
if len(sys.argv)>1 and sys.argv[1]=='calibrate':
    del sys.argv[1]
    from calibrate import main
else:
    from combat_analyzer import main
main()
'''
    archive(path,entries)


def verify_folder(folder):
    doc=json.loads((folder/'CONTENTS.json').read_text())
    require(doc['schema']==1,'Unsupported package manifest')
    expected=set(doc['sha256'])|{'CONTENTS.json','SHA256SUMS.txt','VERIFY.py'}
    require({p.relative_to(folder).as_posix() for p in folder.rglob('*') if p.is_file()}==expected,'Unexpected/missing package file')
    for name,digest in doc['sha256'].items():
        require(not Path(name).is_absolute() and '..' not in Path(name).parts,'Unsafe manifest path')
        require(sha((folder/name).read_bytes())==digest,('Artifact checksum mismatch',name))
    return doc


def bundle(jar,kernel,evidence,readme,out):
    require(re.fullmatch(r'[A-Za-z0-9_.-]+\.jar',jar.name),'Unsafe JAR name')
    manifest=jar.with_suffix('.manifest.json');installer=jar.with_suffix('.install.py')
    build=json.loads(manifest.read_text())
    require(build['input_v3_sha256']==V3 and build['input_minecraft_sha256']==MINECRAFT,'Unverified native build inputs')
    require(build['native_module_included'] and build['dependency_closure_checked'],'Incomplete native build')
    require(build['output_sha256']==sha(jar.read_bytes()),'JAR hash mismatch')
    expected=pinned_installer(build['output_sha256'],jar.name).encode()
    require(installer.read_bytes()==expected and sha(expected)==build['installer_sha256'],'Wrong installer pin')
    entries={jar.name:jar.read_bytes(),manifest.name:manifest.read_bytes(),installer.name:expected,
             'combatiq-kernel.jar':kernel.read_bytes(),'README.txt':readme.read_bytes()}
    with tempfile.TemporaryDirectory() as d:
        pyz=Path(d)/'CombatIQ-Analyzer.pyz';analyzer(pyz);entries[pyz.name]=pyz.read_bytes()
        subprocess.run([sys.executable,str(pyz),'--help'],check=True,capture_output=True)
        subprocess.run([sys.executable,str(pyz),'calibrate','--help'],check=True,capture_output=True)
    for p in sorted(evidence.rglob('*')):
        if p.is_file():entries['validation/'+p.relative_to(evidence).as_posix()]=p.read_bytes()
    for action in ('install','check','rollback','uninstall','recover'):
        entries[action.upper()+'.cmd']=('@echo off\r\npy -3 "%~dp0'+installer.name+'" '+action+' %*\r\nexit /b %errorlevel%\r\n').encode()
    hashes={n:sha(raw) for n,raw in sorted(entries.items())}
    entries['CONTENTS.json']=(json.dumps(dict(schema=1,release='CombatIQ-RC1',source_commit=build['source_commit'],
        minecraft_runtime_validation='UNVERIFIED',windows_os_validation='UNVERIFIED',sha256=hashes),indent=2)+'\n').encode()
    # Standalone verifier also pins the manifest itself, so edits to CONTENTS fail.
    pin=sha(entries['CONTENTS.json'])
    entries['VERIFY.py']=('''import hashlib,json
from pathlib import Path
r=Path(__file__).resolve().parent
if hashlib.sha256((r/'CONTENTS.json').read_bytes()).hexdigest()!=%r:raise SystemExit('FAIL: manifest altered')
d=json.loads((r/'CONTENTS.json').read_text())
expected=set(d['sha256'])|{'CONTENTS.json','SHA256SUMS.txt','VERIFY.py'}
if {p.relative_to(r).as_posix() for p in r.rglob('*') if p.is_file()}!=expected:raise SystemExit('FAIL: unexpected/missing file')
for n,h in d['sha256'].items():
 p=r/n
 if hashlib.sha256(p.read_bytes()).hexdigest()!=h:raise SystemExit('FAIL: '+n)
print('PASS: all %%d payload artifacts verified; Minecraft runtime remains UNVERIFIED'%%len(d['sha256']))
'''%pin).encode()
    # SHA256SUMS covers all payloads, verifier and CONTENTS; only excludes itself.
    entries['SHA256SUMS.txt']=''.join(sha(raw)+'  '+n+'\n' for n,raw in sorted(entries.items())).encode()
    with tempfile.TemporaryDirectory() as d:
        folder=Path(d)
        for n,raw in entries.items():
            target=folder/n;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(raw)
        verify_folder(folder)
        subprocess.run([sys.executable,str(folder/'VERIFY.py')],check=True)
    archive(out,entries)
    with zipfile.ZipFile(out) as z:
        require(z.testzip() is None,'Release ZIP corrupt')
        require(set(z.namelist())==set(entries),'Release ZIP incomplete')
        require(all(z.read(n)==raw for n,raw in entries.items()),'Release ZIP content mismatch')
    out.with_suffix(out.suffix+'.sha256').write_text(sha(out.read_bytes())+'  '+out.name+'\n')
    return dict(path=str(out),bytes=out.stat().st_size,sha256=sha(out.read_bytes()),payload_artifacts=len(hashes))


def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('jar','kernel','evidence','readme','out'):p.add_argument('--'+name,type=Path,required=True)
    a=p.parse_args();print(json.dumps(bundle(a.jar,a.kernel,a.evidence,a.readme,a.out),indent=2))

if __name__=='__main__':main()
