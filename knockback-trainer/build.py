#!/usr/bin/env python3
"""Exact RC1 -> side-by-side Knockback Trainer build. Original client entries preserved."""
import argparse,hashlib,json,os,shutil,struct,subprocess,sys,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT.parent/'combatiq/native-v4'))
from build_v4 import class_references,require
BASE='43f3a3f9b477986bf76b8ec8cc896df3ff1cc3e294d7526e16bc15fc128a3ac9'
MC='14f0d96d1a56fb4f5c3b2233d00699525893fe5ce3dcf181e7de59120595d298'
MIXINS=['TrainerMinecraftMixin','TrainerHandlerMixin','TrainerNetworkMixin','TrainerModuleMixin']
OWN=('me/eldodebug/soar/knockback/','me/eldodebug/soar/injection/mixin/mixins/knockback/')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def installer(digest,name):
 s=(ROOT.parent/'combatiq/native-v4/install_release.py').read_text()
 s=s.replace('EXPECTED_JAR_SHA256 = None','EXPECTED_JAR_SHA256 = '+repr(digest),1).replace('DEFAULT_JAR = None','DEFAULT_JAR = '+repr(name),1)
 s=s.replace("SOURCE_HASHES = {EXPECTED_BASELINE_SHA256,","SOURCE_HASHES = {EXPECTED_BASELINE_SHA256,\n    '"+BASE+"',")
 s=s.replace("ID='VelvetG-CombatIQ-RC1'","ID='VelvetG-Knockback-Trainer-RC1'").replace("VERSION='2.0.9.6-CombatIQ-RC1'","VERSION='2.0.9.6-Knockback-Trainer-RC1'").replace("STATE='.combatiq-rc1'","STATE='.knockback-trainer-rc1'")
 return s

def verify(base,out,classes,minecraft):
 compiled={p.relative_to(classes).as_posix():p.read_bytes() for p in classes.rglob('*.class') if p.relative_to(classes).as_posix().startswith(OWN)}
 require(compiled,'No trainer classes')
 with zipfile.ZipFile(base) as a,zipfile.ZipFile(out) as b,zipfile.ZipFile(minecraft) as mc:
  old=set(a.namelist());new=set(b.namelist());require(len(b.namelist())==len(new),'Duplicate output names')
  require(new-old==set(compiled),'Unexpected added classes/resources');require(not old-new,'Original entry removed')
  changed=[n for n in old if a.read(n)!=b.read(n)];require(changed==['mixins.soar.json'],'Unrelated RC1 bytes changed')
  require(b.testzip() is None,'Invalid output ZIP')
  available=new|set(mc.namelist())
  for n,raw in compiled.items():
   require(b.read(n)==raw,'Wrong compiled class');require(struct.unpack('>H',raw[6:8])[0]==52,'Not Java 8')
   for ref in class_references(raw):
    if not ref.startswith(('java/','javax/','sun/','com/sun/')):require(ref+'.class' in available,('Missing dependency',ref))
  cfg=json.loads(b.read('mixins.soar.json'))
  for m in MIXINS:require(cfg['mixins'].count('knockback.'+m)==1,'Missing/duplicate trainer Mixin')
  require(len(cfg['mixins'])==len(set(cfg['mixins'])),'Duplicate Mixin entries')
 return dict(preserved_rc1_entries=len(old)-1,changed_original_entries=changed,added_classes=len(compiled),entry_sha256={n:hashlib.sha256(v).hexdigest() for n,v in sorted(compiled.items())})

def main():
 p=argparse.ArgumentParser(description=__doc__)
 for arg in ('base','minecraft','out'):p.add_argument('--'+arg,type=Path,required=True)
 a=p.parse_args();require(sha(a.base)==BASE,'Exact working CombatIQ RC1 required');require(sha(a.minecraft)==MC,'Exact official Minecraft 1.8.9 required')
 for f in [a.out,a.out.with_suffix('.install.py'),a.out.with_suffix('.manifest.json')]:require(not f.exists(),('Refuse overwrite',f))
 classes=ROOT/'build/classes';shutil.rmtree(classes,ignore_errors=True);classes.mkdir(parents=True)
 javac=shutil.which('javac');require(javac,'JDK required');version=subprocess.check_output([javac,'-version'],stderr=subprocess.STDOUT,text=True)
 flags=['-source','8','-target','8'] if version.startswith('javac 1.8') else ['--release','8']
 sources=sorted(p for d in ('src','native','mixins') for p in (ROOT/d).rglob('*.java'))
 subprocess.run([javac,'-proc:none',*flags,'-cp',str(a.base),'-d',str(classes),*map(str,sources)],check=True)
 tmp=a.out.with_suffix('.building');created=[]
 try:
  with zipfile.ZipFile(a.base) as base,zipfile.ZipFile(tmp,'x',zipfile.ZIP_DEFLATED) as dest:
   cfg=json.loads(base.read('mixins.soar.json'));cfg['mixins']+=['knockback.'+m for m in MIXINS]
   entries={n:base.read(n) for n in base.namelist()};entries['mixins.soar.json']=(json.dumps(cfg,indent=2)+'\n').encode()
   entries.update({p.relative_to(classes).as_posix():p.read_bytes() for p in classes.rglob('*.class')})
   for name,raw in sorted(entries.items()):
    info=zipfile.ZipInfo(name,(1980,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;dest.writestr(info,raw)
  result=verify(a.base,tmp,classes,a.minecraft)
  source_hash=hashlib.sha256(b''.join(p.relative_to(ROOT).as_posix().encode()+b'\0'+p.read_bytes()+b'\0' for p in sources)).hexdigest()
  result.update(base_sha256=BASE,minecraft_sha256=MC,output_sha256=sha(tmp),source_sha256=source_hash,compiler=version.strip(),source_commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),minecraft_runtime_validated=False,windows_executed=False)
  script=installer(result['output_sha256'],a.out.name);result['installer_sha256']=hashlib.sha256(script.encode()).hexdigest()
  for target,content in [(a.out.with_suffix('.install.py'),script),(a.out.with_suffix('.manifest.json'),json.dumps(result,indent=2)+'\n')]:
   with target.open('x',encoding='utf-8',newline='\n') as f:created.append(target);f.write(content);f.flush();os.fsync(f.fileno())
  os.link(tmp,a.out);created.append(a.out);print(json.dumps(result,indent=2))
 except BaseException:
  for f in reversed(created):f.unlink()
  raise
 finally:tmp.unlink(missing_ok=True)
if __name__=='__main__':main()
