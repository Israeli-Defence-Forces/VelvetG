#!/usr/bin/env python3
"""Apply RC1's six additive Mixins together with all four trainer Mixins headlessly."""
import argparse,hashlib,json,os,subprocess,tempfile,zipfile
from pathlib import Path
from build import BASE,MC,ROOT,MIXINS,require
RC1_MIXINS=['CombatIQMinecraftMixin','CombatIQPlayerMixin','CombatIQHandlerMixin','CombatIQNetworkMixin','CombatIQQueuedWriteMixin','CombatIQModuleMixin']
def run(base,minecraft,libraries,java8,javac):
 require(hashlib.sha256(base.read_bytes()).hexdigest()==BASE,'Exact RC1 required')
 require(hashlib.sha256(minecraft.read_bytes()).hexdigest()==MC,'Exact Minecraft required')
 with tempfile.TemporaryDirectory() as d:
  d=Path(d);classes=d/'classes';classes.mkdir();api=d/'api.jar';library=d/'mixin.jar'
  with zipfile.ZipFile(base) as z,zipfile.ZipFile(api,'w') as a,zipfile.ZipFile(library,'w') as b:
   for name in z.namelist():
    if name.startswith(('org/spongepowered/','META-INF/services/org.spongepowered.')):b.writestr(name,z.read(name))
    elif name.endswith('.class'):a.writestr(name,z.read(name))
  cp=os.pathsep.join(map(str,[classes,library,minecraft,api,*sorted(libraries.rglob('*.jar'))]))
  sources=[p for name in ('src','native','mixins','integration') for p in (ROOT/name).rglob('*.java')]
  subprocess.run([str(javac),'--release','8','-proc:none','-cp',cp,'-d',str(classes),*map(str,sources)],check=True,capture_output=True,text=True)
  cfg=dict(required=True,minVersion='0.7.10',package='me.eldodebug.soar.injection.mixin.mixins',compatibilityLevel='JAVA_8',mixins=['combatiq.'+m for m in RC1_MIXINS]+['knockback.'+m for m in MIXINS])
  (classes/'mixins.combatiq.integration.json').write_text(json.dumps(cfg))
  p=subprocess.run([str(java8),'-Xverify:all','-cp',cp,'tools.TrainerMixinCheck',str(minecraft),str(base)],capture_output=True,text=True,timeout=60)
  return dict(passed=p.returncode==0,returncode=p.returncode,rc1_mixins=6,trainer_mixins=4,base_sha256=BASE,minecraft_sha256=MC,minecraft_launched=False,all_original_client_mixins_applied=False,stdout=p.stdout,stderr=p.stderr)
def main():
 p=argparse.ArgumentParser(description=__doc__)
 for n in ('base','minecraft','libraries','java8','javac','out'):p.add_argument('--'+n,type=Path,required=True)
 a=p.parse_args();result=run(a.base,a.minecraft,a.libraries,a.java8,a.javac);a.out.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2));require(result['passed'],'Mixin application failed')
if __name__=='__main__':main()
