#!/usr/bin/env python3
"""Incremental owner-only v4 build from the exact working v3 JAR. Baseline is untouched."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import sys
import zipfile

ROOT=Path(__file__).resolve().parent
V3='7949d73d9be9eb74b662b2844faee2939c0463222c01b4a89378f19ddaa8bbc7'
BRIDGE='me/eldodebug/soar/combatiq/'
MIXINS='me/eldodebug/soar/injection/mixin/mixins/combatiq/'
ADDED_MIXINS=['CombatIQPlayerMixin','CombatIQMinecraftMixin','CombatIQHandlerMixin']

def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()

def verify(v3, out, classes):
    expected={p.relative_to(classes).as_posix() for p in classes.rglob('*.class')
              if p.relative_to(classes).as_posix().startswith((BRIDGE,MIXINS))}
    with zipfile.ZipFile(v3) as a,zipfile.ZipFile(out) as b:
        old=set(a.namelist());new=set(b.namelist())
        assert len(b.namelist())==len(new),'Duplicate archive names'
        assert new-old==expected-old
        assert not old-new
        changed={n for n in old if a.read(n)!=b.read(n)}
        allowed=(expected & old) | {'mixins.soar.json'}
        assert changed <= allowed, ('Unrelated original entry changed', changed-allowed)
        assert BRIDGE+'CombatIQNative.class' in changed, 'Native bridge was not replaced'
        assert all(b.read(n)==(classes/n).read_bytes() for n in expected), 'Compiled class mismatch'
        assert b.testzip() is None
        for n in expected:
            raw=b.read(n)
            assert raw[:4]==b'\xca\xfe\xba\xbe' and struct.unpack('>H',raw[6:8])[0]==52
            # Every known telemetry/mixin compiler-generated sibling in constants
            # must be packaged. This includes named inner Layout, not just $1/$2.
            for ref in set(re.findall(rb'me/eldodebug/soar/(?:combatiq|injection/mixin/mixins/combatiq)/[\w$/]+',raw)):
                name=ref.decode()+'.class'
                assert name in new,('Missing referenced telemetry class',name)
        cfg=json.loads(b.read('mixins.soar.json'))
        for n in ADDED_MIXINS: assert cfg['mixins'].count('combatiq.'+n)==1
    return dict(changed=sorted(changed),added=sorted(new-old),unchanged_original_entries=len(old)-len(changed),java_class_major=52)

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--v3',type=Path,required=True)
    p.add_argument('--minecraft',type=Path,required=True)
    p.add_argument('--out',type=Path,required=True)
    p.add_argument('--native-module',action='store_true',help='Experimental native HUD adapter; live baseline validation required')
    a=p.parse_args()
    if a.native_module: ADDED_MIXINS.append('CombatIQModuleMixin')
    if digest(a.v3)!=V3: sys.exit('REFUSED: expected exact v3 JAR hash')
    if a.out.exists(): sys.exit('REFUSED: output already exists')
    java=shutil.which('java');javac=shutil.which('javac')
    if not java or not javac: sys.exit('JDK required (javac/java)')
    version=subprocess.check_output([javac,'-version'],stderr=subprocess.STDOUT,text=True)
    major=int(re.search(r'javac (?:1\.)?(\d+)',version)[1])
    flags=['--release','8'] if major>8 else ['-source','8','-target','8']
    classes=ROOT/'build-v4'
    if classes.exists(): shutil.rmtree(classes)
    classes.mkdir()
    sources=[x for x in (ROOT/'src').rglob('*.java') if x.name!='PatchMixins.java']
    if a.native_module: sources.extend((ROOT/'native-module').rglob('*.java'))
    subprocess.run([javac,'-proc:none']+flags+['-cp',str(a.v3),'-d',str(classes)]+[str(x) for x in sources],check=True)
    cp=str(classes)+os.pathsep+str(a.v3)
    subprocess.run([java,'-Xverify:all','-cp',cp,'tools.VerifyBindings',str(a.minecraft)],check=True)
    compiled={x.relative_to(classes).as_posix():x.read_bytes() for x in classes.rglob('*.class')
              if x.relative_to(classes).as_posix().startswith((BRIDGE,MIXINS))}
    tmp=a.out.with_name(a.out.name+'.building')
    if tmp.exists(): sys.exit('REFUSED: stale building file; inspect it first')
    try:
        with zipfile.ZipFile(a.v3) as old,zipfile.ZipFile(tmp,'x',zipfile.ZIP_DEFLATED) as dest:
            config=json.loads(old.read('mixins.soar.json'))
            config['mixins'].extend('combatiq.'+n for n in ADDED_MIXINS)
            for n in old.namelist():
                raw=compiled.pop(n,old.read(n))
                if n=='mixins.soar.json':raw=(json.dumps(config,indent=2)+'\n').encode()
                e=zipfile.ZipInfo(n,(1980,1,1,0,0,0));e.compress_type=zipfile.ZIP_DEFLATED
                dest.writestr(e,raw)
            for n,raw in sorted(compiled.items()):
                e=zipfile.ZipInfo(n,(1980,1,1,0,0,0));e.compress_type=zipfile.ZIP_DEFLATED;dest.writestr(e,raw)
        result=verify(a.v3,tmp,classes)
        result.update(input_v3_sha256=V3,output_sha256=digest(tmp),compiler=version.strip(),
                      minecraft_launch_tested=False,mixin_application_tested=False,transport_write_instrumented=False)
        # Link then remove temp: refuses replacing an output created concurrently.
        os.link(tmp,a.out);tmp.unlink()
        manifest=a.out.with_suffix('.manifest.json')
        with manifest.open('x',encoding='utf-8') as f:json.dump(result,f,indent=2)
        print(json.dumps(result,indent=2))
    finally:
        if tmp.exists():tmp.unlink()

if __name__=='__main__': main()

