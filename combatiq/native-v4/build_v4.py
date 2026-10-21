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
MINECRAFT='14f0d96d1a56fb4f5c3b2233d00699525893fe5ce3dcf181e7de59120595d298'
BRIDGE='me/eldodebug/soar/combatiq/'
MIXINS='me/eldodebug/soar/injection/mixin/mixins/combatiq/'
NETTY_SHA256='50510e9c9874b539b98ef931fe7eeeb002f6b5b1976ea37669b9493426294561'
NETTY_RELATIVE='io/netty/netty-all/4.0.23.Final/netty-all-4.0.23.Final.jar'
ADDED_MIXINS=['CombatIQPlayerMixin','CombatIQMinecraftMixin','CombatIQHandlerMixin','CombatIQNetworkMixin','CombatIQQueuedWriteMixin']

def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()

class VerificationError(ValueError):
    pass


def require(condition, message):
    # This is a release gate, so it must remain active under python -O.
    if not condition:
        raise VerificationError(str(message))


def class_references(raw):
    """Parse class and member descriptor constants, rather than scanning binary text."""
    require(raw[:4] == b'\xca\xfe\xba\xbe', 'Invalid class magic')
    require(len(raw) >= 10, 'Truncated class header')
    count=struct.unpack_from('>H',raw,8)[0];cp={};descriptors=[];pos=10;i=1
    try:
        while i<count:
            tag=raw[pos];pos+=1
            if tag==1:
                length=struct.unpack_from('>H',raw,pos)[0];pos+=2
                cp[i]=(tag,raw[pos:pos+length].decode('utf-8',errors='replace'));pos+=length
            elif tag in (7,8,16,19,20):
                value=struct.unpack_from('>H',raw,pos)[0];cp[i]=(tag,value);pos+=2
                if tag==16:descriptors.append(value)
            elif tag in (3,4,9,10,11,12,17,18):
                if tag==12:descriptors.append(struct.unpack_from('>H',raw,pos+2)[0])
                pos+=4
            elif tag in (5,6):pos+=8;i+=1
            elif tag==15:pos+=3
            else:raise VerificationError('Unknown constant-pool tag '+str(tag))
            require(pos<=len(raw),'Truncated constant pool')
            i+=1
        refs=set()
        for tag,value in cp.values():
            if tag==7:
                name=cp[value][1]
                if name.startswith('['):refs.update(re.findall(r'L([^;]+);',name))
                else:refs.add(name)
        for index in descriptors:refs.update(re.findall(r'L([^;]+);',cp[index][1]))
        return refs
    except (IndexError,KeyError,struct.error) as error:
        raise VerificationError('Malformed class constant pool') from error


def verify(v3, out, classes, minecraft=None, required_mixins=None, dependencies=()):
    required_mixins=ADDED_MIXINS if required_mixins is None else required_mixins
    expected={p.relative_to(classes).as_posix() for p in classes.rglob('*.class')
              if p.relative_to(classes).as_posix().startswith((BRIDGE,MIXINS))}
    require(expected,'No compiled native classes')
    with zipfile.ZipFile(v3) as a,zipfile.ZipFile(out) as b:
        old=set(a.namelist());new=set(b.namelist())
        require(len(a.namelist())==len(old),'Duplicate baseline archive names')
        require(len(b.namelist())==len(new),'Duplicate output archive names')
        require(new-old==expected-old,'Unexpected or missing output entries')
        require(not old-new,'Original entry removed')
        changed={n for n in old if a.read(n)!=b.read(n)}
        allowed=(expected & old) | {'mixins.soar.json'}
        require(changed <= allowed,('Unrelated original entry changed', changed-allowed))
        require(BRIDGE+'CombatIQNative.class' in changed,'Native bridge was not replaced')
        require(all(b.read(n)==(classes/n).read_bytes() for n in expected),'Compiled class mismatch')
        require(b.testzip() is None,'Corrupt archive')
        available=new.copy()
        if minecraft is not None:
            with zipfile.ZipFile(minecraft) as mc:available.update(mc.namelist())
            for dependency in dependencies:
                with zipfile.ZipFile(dependency) as lib:available.update(lib.namelist())
        for n in expected:
            raw=b.read(n)
            require(len(raw)>=10 and struct.unpack('>H',raw[6:8])[0]==52,('Non-Java8 class',n))
            for ref in class_references(raw):
                own=ref.startswith((BRIDGE,MIXINS))
                if own or minecraft is not None and not ref.startswith(('java/','javax/','sun/','com/sun/')):
                    require(ref+'.class' in available,('Missing class dependency',n,ref))
        cfg=json.loads(b.read('mixins.soar.json'))
        package=cfg.get('package',MIXINS[:-len('combatiq/')].rstrip('/').replace('/','.')).replace('.','/')+'/'
        configured=[]
        for section in ('mixins','client','server'):
            entries=cfg.get(section,[])
            require(isinstance(entries,list),('Invalid Mixin configuration',section))
            for name in entries:
                require(isinstance(name,str) and '/' not in name and '..' not in name,'Invalid Mixin name')
                configured.append(name)
                require(package+name.replace('.','/')+'.class' in new,('Missing configured Mixin class',name))
        require(len(configured)==len(set(configured)),'Duplicate Mixin configuration entries')
        for n in required_mixins:
            require(configured.count('combatiq.'+n)==1,('Required Mixin not configured',n))
            require(MIXINS+n+'.class' in expected,('Required Mixin not compiled',n))
    return dict(changed=sorted(changed),added=sorted(new-old),unchanged_original_entries=len(old)-len(changed),
                java_class_major=52,dependency_closure_checked=minecraft is not None,
                entry_sha256={n:hashlib.sha256((classes/n).read_bytes()).hexdigest() for n in sorted(expected)})


def pinned_installer(output_sha, artifact_name='CombatIQ-RC1.jar'):
    if not re.fullmatch(r'[0-9a-f]{64}', output_sha):
        raise ValueError('Require actual built artifact SHA-256')
    if not re.fullmatch(r'[A-Za-z0-9_.-]+\.jar', artifact_name):
        raise ValueError('Artifact filename must be a simple .jar name')
    source=(ROOT/'install_release.py').read_text(encoding='utf-8')
    require(source.count('EXPECTED_JAR_SHA256 = None')==1,'Invalid installer template')
    return source.replace('EXPECTED_JAR_SHA256 = None',"EXPECTED_JAR_SHA256 = '"+output_sha+"'",1).replace('DEFAULT_JAR = None','DEFAULT_JAR = '+repr(artifact_name),1)


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--v3',type=Path,required=True)
    p.add_argument('--minecraft',type=Path,required=True)
    p.add_argument('--netty',type=Path,help='Official Minecraft 1.8.9 Netty library; auto-detected beside minecraft.jar under deps/')
    p.add_argument('--out',type=Path,required=True)
    p.add_argument('--native-module',action='store_true',help='Experimental native HUD adapter; live baseline validation required')
    a=p.parse_args()
    added_mixins=ADDED_MIXINS+(['CombatIQModuleMixin'] if a.native_module else [])
    netty=a.netty or a.minecraft.parent/'deps'/NETTY_RELATIVE
    if not netty.is_file() or digest(netty)!=NETTY_SHA256:sys.exit('REFUSED: exact Minecraft Netty library required (--netty)')
    if digest(a.v3)!=V3: sys.exit('REFUSED: expected exact v3 JAR hash')
    if digest(a.minecraft)!=MINECRAFT: sys.exit('REFUSED: expected exact official Minecraft 1.8.9 JAR hash')
    if a.out.exists() or a.out.with_suffix('.manifest.json').exists() or a.out.with_suffix('.install.py').exists():
        sys.exit('REFUSED: output/manifest/installer already exists')
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
    subprocess.run([javac,'-proc:none']+flags+['-cp',str(a.v3)+os.pathsep+str(netty),'-d',str(classes)]+[str(x) for x in sources],check=True)
    cp=str(classes)+os.pathsep+str(a.v3)
    subprocess.run([java,'-Xverify:all','-cp',cp,'tools.VerifyBindings',str(a.minecraft)],check=True)
    compiled={x.relative_to(classes).as_posix():x.read_bytes() for x in classes.rglob('*.class')
              if x.relative_to(classes).as_posix().startswith((BRIDGE,MIXINS))}
    tmp=a.out.with_name(a.out.name+'.building')
    if tmp.exists(): sys.exit('REFUSED: stale building file; inspect it first')
    try:
        with zipfile.ZipFile(a.v3) as old,zipfile.ZipFile(tmp,'x',zipfile.ZIP_DEFLATED) as dest:
            config=json.loads(old.read('mixins.soar.json'))
            config['mixins'].extend('combatiq.'+n for n in added_mixins)
            for n in old.namelist():
                raw=compiled.pop(n,old.read(n))
                if n=='mixins.soar.json':raw=(json.dumps(config,indent=2)+'\n').encode()
                e=zipfile.ZipInfo(n,(1980,1,1,0,0,0));e.compress_type=zipfile.ZIP_DEFLATED
                dest.writestr(e,raw)
            for n,raw in sorted(compiled.items()):
                e=zipfile.ZipInfo(n,(1980,1,1,0,0,0));e.compress_type=zipfile.ZIP_DEFLATED;dest.writestr(e,raw)
        result=verify(a.v3,tmp,classes,a.minecraft,added_mixins,[netty])
        source_files=sorted(list((ROOT/'src').rglob('*.java'))+list((ROOT/'native-module').rglob('*.java')))
        source_hash=hashlib.sha256(b''.join(x.relative_to(ROOT).as_posix().encode()+b'\0'+x.read_bytes()+b'\0' for x in source_files)).hexdigest()
        try: source_commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True,stderr=subprocess.DEVNULL).strip()
        except (OSError,subprocess.CalledProcessError): source_commit=None
        result.update(build_version='RC1',source_commit=source_commit,source_sha256=source_hash,
                      input_netty_sha256=digest(netty),input_minecraft_sha256=digest(a.minecraft),input_v3_sha256=V3,output_sha256=digest(tmp),compiler=version.strip(),
                      minecraft_launch_tested=False,mixin_application_tested=False,transport_write_instrumented=False,
                      channel_write_invocation_instrumented=True,
                      native_module_included=a.native_module)
        installer=a.out.with_suffix('.install.py')
        installer_source=pinned_installer(result['output_sha256'],a.out.name)
        result['installer_sha256']=hashlib.sha256(installer_source.encode('utf-8')).hexdigest()
        manifest=a.out.with_suffix('.manifest.json')
        # Exclusive publication; clean up only files this process actually created.
        # The JAR is published last, so a sidecar failure cannot leave a usable
        # client without its exact installer and manifest.
        created=[]
        try:
            with manifest.open('x',encoding='utf-8') as f:
                created.append(manifest);json.dump(result,f,indent=2);f.flush();os.fsync(f.fileno())
            with installer.open('x',encoding='utf-8',newline='\n') as f:
                created.append(installer);f.write(installer_source);f.flush();os.fsync(f.fileno())
            os.link(tmp,a.out);created.append(a.out);tmp.unlink()
        except BaseException:
            for path in reversed(created):path.unlink()
            raise
        print(json.dumps(result,indent=2))
    finally:
        if tmp.exists():tmp.unlink()

if __name__=='__main__': main()

