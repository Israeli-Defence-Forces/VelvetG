"""Exercise the shipped PowerShell collector with synthetic, private-data canaries."""
from pathlib import Path
import subprocess,os,tempfile,zipfile,json,hashlib,importlib.util,shutil
ROOT=Path(__file__).resolve().parents[1]
PS=ROOT/'tools/pwsh/pwsh'
SCRIPT=ROOT/'collector/Collect-VelvetG.ps1'
ENV=dict(os.environ,XDG_CACHE_HOME=str(ROOT/'cache'),XDG_CONFIG_HOME=str(ROOT/'config'),XDG_DATA_HOME=str(ROOT/'data'))
checks=0
def check(value,message):
 global checks
 if not value:raise AssertionError(message)
 checks+=1

def run(*args):
 return subprocess.run([str(PS),'-NoProfile','-File',str(SCRIPT),*map(str,args)],env=ENV,capture_output=True,text=True)

def snapshot(root):
 return {str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in root.rglob('*') if p.is_file()}

with tempfile.TemporaryDirectory(prefix='collector tests ',dir=ROOT) as td:
 base=Path(td);game=base/'Minecraft with spaces';game.mkdir();out=base/'Collection output';out.mkdir()
 target=game/'libraries/me/eldodebug/GlideClient/2.0.9.6-lean-fixed-20261016/GlideClient-2.0.9.6-lean-fixed-20261016.jar';target.parent.mkdir(parents=True)
 with zipfile.ZipFile(target,'w',zipfile.ZIP_STORED) as z:z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\n');z.writestr('assets/synthetic.bin',b'Z'*(24*1024*1024+121))
 canaries=['pika_SyntheticPrivateKey123456789','SyntheticPersonalName_CANARY','192.0.2.99','71a4d4c0-1b1d-4e4f-a9a5-0e782bb6252b','SyntheticPrivateServer_CANARY']
 ver=game/'versions/LeanSyntheticPersonalName_CANARY';ver.mkdir(parents=True)
 (ver/(ver.name+'.json')).write_text(json.dumps({'id':ver.name,'inheritsFrom':'1.8.9','mainClass':'net.minecraft.launchwrapper.Launch','minecraftArguments':'--username '+canaries[1]+' --accessToken '+canaries[0]+' --server '+canaries[2]+' --tweakClass me.eldodebug.soar.injection.mixin.GlideTweaker','libraries':[{'name':'me.eldodebug:GlideClient:2.0.9.6-lean-fixed-20261016','url':'https://example.invalid/?key='+canaries[0]}],'arguments':{'jvm':['-Dsecret='+canaries[0]]}}))
 parent=game/'versions/1.8.9';parent.mkdir(parents=True);(parent/'1.8.9.json').write_text(json.dumps({'id':'1.8.9','mainClass':'net.minecraft.client.main.Main','libraries':[]}))
 glide=game/'glide';glide.mkdir();screens=glide/'screenshots';screens.mkdir()
 for i in range(250):(screens/f'{canaries[1]} {i}.png').write_bytes(b'\x89PNG\r\n\x1a\ncorrupt-by-design')
 (glide/'settings.json').write_text(json.dumps({'scale':1.5,'enabled':True,'mode':'QUAD','period':'MONTHLY','apiKey':canaries[0],'account':{'scale':999,'username':canaries[1]},'notes':{'scale':777},'arbitrary':canaries[1]}))
 (glide/'apikey.txt').write_text(canaries[0]);(game/'launcher_accounts.json').write_text(json.dumps({'accessToken':canaries[0],'username':canaries[1]}));(game/'launcher_profiles.json').write_text(json.dumps({'lastUsed':canaries[1]}));(game/'servers.dat').write_bytes(canaries[4].encode())
 (game/'options.txt').write_text('maxFps:120\nrenderDistance:8\nenableVsync:false\nlastServer:'+canaries[4]+'\n')
 logs=game/'logs';logs.mkdir();(logs/'latest.log').write_text('[CHAT] '+canaries[0]+' '+canaries[1]+' '+canaries[2]+' '+canaries[3]+'\njava.lang.VerifyError: Bad type on operand stack '+canaries[0]+'\n at me.eldodebug.soar.scout.PikaApiClient.json(PikaApiClient.java:303)\nJava Version: 1.8.0_51, Oracle Corporation\nJVM Flags: 3 total; -Xmx3G -XX:+UseG1GC -Dprivate='+canaries[0]+'\n')
 before=snapshot(game);result=run('-MinecraftDir',game,'-OutputParent',out)
 check(result.returncode==0,'collector succeeds with spaces: '+result.stdout+'\n'+result.stderr)
 check(snapshot(game)==before,'every original file, screenshot and configuration remains byte-identical')
 dirs=list(out.iterdir());check(len(dirs)==1,'one unique collection directory');archives=sorted(dirs[0].glob('*.zip'));check(len(archives)==3,'large JAR creates two part ZIPs and one diagnostics ZIP')
 check(all(p.stat().st_size<32*1024*1024 for p in archives),'every upload archive stays below 32 MiB')
 with zipfile.ZipFile(dirs[0]/'VelvetG-DIAGNOSTICS.zip') as z:
  check(z.testzip() is None,'diagnostics ZIP integrity');check(set(z.namelist())=={'collection.json','stack-frames.txt'},'no raw user files included')
  content='\n'.join(z.read(n).decode() for n in z.namelist());report=json.loads(z.read('collection.json'))
  for secret in canaries:check(secret not in content,'private-data canary absent')
  check('PikaApiClient.json' in content,'relevant Java stack frame retained')
  check(report['java']['reportedVersions']==['1.8.0_51'],'crash Java version retained safely')
  check(report['java']['safeJvmFlags']==['-Xmx3G','-XX:+UseG1GC'],'only allowlisted JVM flags retained')
  check(report['screenshotInventory']['images']==250 and not report['screenshotInventory']['imagesCollected'],'all corrupt screenshots inventoried without copying or decoding')
  check(any(c['control']=='mode' and c['value']=='QUAD' for c in report['configuration']['controls']),'QUAD preserved literally')
  check(not any(c['value'] in [777,999] for c in report['configuration']['controls']),'sensitive parent sections excluded')
  check(report['versions'][0]['mainClass']=='net.minecraft.launchwrapper.Launch','bootstrap retained without account arguments')
  check(report['versions'][0]['tweakClasses']==['me.eldodebug.soar.injection.mixin.GlideTweaker'],'safe tweak classes retained')
  check(len(report['versions'])==2,'matching version and parent discovered')
 payload=report['payloads'][0];binary=bytearray()
 for part in payload['parts']:
  with zipfile.ZipFile(dirs[0]/part['archive']) as z:
   raw=z.read(part['entry']);check(len(raw)==part['size'] and hashlib.sha256(raw).hexdigest()==part['sha256'],'part size and hash valid');binary.extend(raw)
 check(bytes(binary)==target.read_bytes(),'parts reproduce exact installed JAR')
 check(hashlib.sha256(binary).hexdigest()==payload['sha256'],'whole-client hash matches')
 spec=importlib.util.spec_from_file_location('restore_collection',ROOT/'collector/RESTORE-COLLECTION.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
 restored=module.restore(dirs[0],base/'restored');check(restored[0].read_bytes()==target.read_bytes(),'shipped restorer reconstructs exact JAR')
 try:module.restore(dirs[0],base/'restored');raise AssertionError('overwrite was allowed')
 except ValueError:check(True,'restorer refuses to overwrite existing files')
 damaged=base/'damaged';shutil.copytree(dirs[0],damaged)
 first=payload['parts'][0]
 with zipfile.ZipFile(damaged/first['archive']) as z:raw=z.read('payload.bin')
 with zipfile.ZipFile(damaged/first['archive'],'w',zipfile.ZIP_STORED) as z:z.writestr('payload.bin',bytes([raw[0]^1])+raw[1:])
 try:module.restore(damaged,base/'failed-restore');raise AssertionError('tampered bytes were accepted')
 except ValueError:check(True,'restorer rejects tampered part hashes')
 check(not list((base/'failed-restore').glob('*')),'failed restore leaves no partial JAR')
 missing=run('-MinecraftDir',base/'missing','-OutputParent',out);check(missing.returncode!=0,'missing installation fails clearly')
 inside=run('-MinecraftDir',game,'-OutputParent',game/'should-not-be-created');check(inside.returncode!=0 and not (game/'should-not-be-created').exists(),'collector refuses output inside Minecraft')
 # Small client and empty gallery, damaged config and malformed launcher manifest are tolerated.
 for p in screens.iterdir():p.unlink()
 with zipfile.ZipFile(target,'w') as z:z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\n')
 (glide/'settings.json').write_text('{bad json');(parent/'1.8.9.json').write_text('{bad json')
 second=run('-MinecraftDir',game,'-OutputParent',out);check(second.returncode==0,'empty gallery and corrupt metadata do not crash collection')
 # Credential-bearing archives fail before any binary is emitted.
 with zipfile.ZipFile(target,'w') as z:z.writestr('apikey.txt',canaries[0])
 third=run('-MinecraftDir',game,'-OutputParent',out);check(third.returncode!=0,'obvious bundled credentials block binary export')
 latest=max(out.iterdir(),key=lambda p:p.stat().st_mtime_ns);check(not list(latest.glob('*PART*.zip')),'credential rejection emits no binary parts')
 # A key embedded in class/resource bytes is also rejected without reporting the value.
 with zipfile.ZipFile(target,'w') as z:z.writestr('assets/opaque.bin',b'X'*65530+canaries[0].encode()+b'Y')
 fourth=run('-MinecraftDir',game,'-OutputParent',out);check(fourth.returncode!=0,'embedded key crossing scan-buffer boundary blocks export')
 check(canaries[0] not in fourth.stdout+fourth.stderr,'detected key is not printed')
print(f'PASS: {checks} collector privacy, integrity, splitting, spaces, missing/corrupt inputs and read-only checks (PowerShell 7.4.13 Linux; Windows 5.1 untested)')
