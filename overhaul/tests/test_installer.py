"""Exercise real PowerShell and Java 8 against a private temporary Minecraft layout."""
import pathlib,tempfile,shutil,subprocess,json,hashlib,os,unittest
R=pathlib.Path(__file__).resolve().parents[1]
PS=pathlib.Path(os.environ.get('POWERSHELL_EXECUTABLE',shutil.which('pwsh') or 'pwsh'))
JAVA=pathlib.Path(os.environ.get('JAVA8',shutil.which('java') or 'java'))
BASE=pathlib.Path(os.environ.get('BASELINE_JAR','original-client.jar'))
MC=pathlib.Path(os.environ.get('MINECRAFT_JAR','minecraft-1.8.9.jar'))
PACKAGE=pathlib.Path(os.environ.get('PACKAGE_DIR',str(R if (R/'package.json').exists() else R/'dist/VelvetG-Lean-Overhaul3-RELEASE-CANDIDATE')))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
class Installer(unittest.TestCase):
 def test_transaction(self):
  with tempfile.TemporaryDirectory(prefix='VelvetG space ! & ') as td:
   root=pathlib.Path(td)/'Minecraft game ! &';root.mkdir()
   lib=root/'libraries/me/eldodebug/GlideClient/2.0.9.6-lean-fixed-20261016/GlideClient-2.0.9.6-lean-fixed-20261016.jar';lib.parent.mkdir(parents=True);shutil.copyfile(BASE,lib)
   vanilla=root/'versions/1.8.9/1.8.9.jar';vanilla.parent.mkdir(parents=True);shutil.copyfile(MC,vanilla)
   dependencies=pathlib.Path(os.environ.get('DEPENDENCIES_DIR','libraries'))
   declared=[]
   for f in dependencies.rglob('*.jar'):
    relative=f.relative_to(dependencies);dest=root/'libraries'/relative;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(f,dest)
    declared.append({'name':'fixture:dependency:1','downloads':{'artifact':{'path':relative.as_posix()}}})
   (vanilla.parent/'1.8.9.json').write_text(json.dumps({'id':'1.8.9','libraries':declared}))
   original=root/'versions/original/original.json';original.parent.mkdir(parents=True)
   original.write_text(json.dumps({'id':'original','inheritsFrom':'1.8.9','mainClass':'net.minecraft.launchwrapper.Launch','minecraftArguments':'--tweakClass me.eldodebug.soar.injection.mixin.GlideTweaker','libraries':[{'name':'me.eldodebug:GlideClient:2.0.9.6-lean-fixed-20261016','checksums':['original-checksum'],'size':1,'path':'old-path'},{'name':'net.minecraft:launchwrapper:1.12'}]}))
   screen=root/'glide/screenshots/keep.png';screen.parent.mkdir(parents=True);screen.write_bytes(b'precious screenshot fixture')
   config=root/'glide/config.json';config.write_text('{"SnapTap":true,"secret":"DO_NOT_EXPORT"}')
   launcher=root/'launcher_profiles.json';launcher.write_text('{"personal":"DO_NOT_CHANGE"}')
   # A previous Overhaul1 installation is preserved beside the new version.
   old_version=root/'versions/VelvetG-Lean-Overhaul1/VelvetG-Lean-Overhaul1.json';old_version.parent.mkdir(parents=True)
   old_version.write_text(json.dumps({'id':'VelvetG-Lean-Overhaul1','libraries':[{'name':'me.eldodebug:GlideClient:2.0.9.6-lean-overhaul1'}]}))
   old_receipt=root/'velvetg-lean-overhaul/receipt.json';old_receipt.parent.mkdir(parents=True);old_receipt.write_text('{"canary":"keep previous installation"}')
   old_settings=root/'config/velvetg-lean.properties';old_settings.parent.mkdir(parents=True);old_settings.write_text('tabLimit=17\nunknownUserSetting=keep\n')
   previous_version=root/'versions/VelvetG-Lean-Overhaul2/VelvetG-Lean-Overhaul2.json';previous_version.parent.mkdir(parents=True)
   previous_version.write_text(json.dumps({'id':'VelvetG-Lean-Overhaul2','libraries':[{'name':'me.eldodebug:GlideClient:2.0.9.6-lean-overhaul2'}]}))
   previous_receipt=root/'velvetg-lean-overhaul2/receipt.json';previous_receipt.parent.mkdir(parents=True);previous_receipt.write_text('{"canary":"keep Overhaul2"}')
   previous_settings=root/'config/velvetg-lean-v2.properties';previous_settings.write_text('tabLimit=25\n')
   hashes={p:sha(p) for p in [lib,original,screen,config,launcher,old_version,old_receipt,old_settings,previous_version,previous_receipt,previous_settings]}
   def invoke(action,ok=True,extra=(),java_mode="explicit",package=PACKAGE):
    env=dict(os.environ);env.update(XDG_CACHE_HOME=str(root/'cache'),XDG_CONFIG_HOME=str(root/'psconfig'),XDG_DATA_HOME=str(root/'psdata'))
    java_args=['-JavaPath',str(JAVA)] if java_mode=='explicit' else []
    if java_mode!='explicit':
     env['PATH']=str(JAVA.resolve().parent)+os.pathsep+env.get('PATH','')
     env.pop('JAVA_HOME',None)
     if java_mode=='home':env['JAVA_HOME']=str(JAVA.resolve().parent.parent)
     elif java_mode=='stale-home':env['JAVA_HOME']=str(root/'missing Java home')
    r=subprocess.run([str(PS),'-NoProfile','-File',str(package/'Manage-VelvetG.ps1'),'-Action',action,'-GameDir',str(root),*java_args,*extra],env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    if ok:self.assertEqual(r.returncode,0,r.stdout)
    else:self.assertNotEqual(r.returncode,0,r.stdout)
    self.assertNotIn('DO_NOT_EXPORT',r.stdout);return r
   invoke('Install',java_mode='path');self.assertTrue((root/'velvetg-lean-overhaul3/receipt.json').exists());invoke('Verify',java_mode='home');invoke('Verify',java_mode='stale-home');invoke('Install',False)
   for p,h in hashes.items():self.assertEqual(sha(p),h)
   receipt=json.loads((root/'velvetg-lean-overhaul3/receipt.json').read_text());patched=pathlib.Path(receipt['library']);self.assertTrue(patched.exists())
   installed=json.loads(pathlib.Path(receipt['versionJson']).read_text());entry=next(x for x in installed['libraries'] if x['name']==receipt['coordinate']);self.assertEqual(entry['checksums'],[hashlib.sha1(patched.read_bytes()).hexdigest()]);self.assertEqual(entry['size'],patched.stat().st_size);self.assertEqual(entry['path'],entry['downloads']['artifact']['path']);self.assertEqual(entry['url'],'');self.assertEqual(entry['downloads']['artifact']['url'],'');self.assertEqual(installed['inheritsFrom'],'1.8.9');self.assertEqual(installed['jar'],'1.8.9')
   # Offline integrity detects damage and rollback refuses to discard changed user files.
   original_bytes=patched.read_bytes();patched.write_bytes(original_bytes+b'corruption');invoke('Verify',False);invoke('Restore',False);self.assertTrue(patched.exists());patched.write_bytes(original_bytes)
   # Read-only rollback is refused before removing either output.
   version=pathlib.Path(receipt['versionJson']);version.chmod(0o444);invoke('Restore',False);self.assertTrue(patched.exists());version.chmod(0o644)
   # The privacy-safe smoke kit really verifies this installed fixture, without online I/O.
   env=dict(os.environ);env.update(XDG_CACHE_HOME=str(root/'cache'),XDG_CONFIG_HOME=str(root/'psconfig'),XDG_DATA_HOME=str(root/'psdata'))
   results=root/'safe report'
   startup=root/'config/velvetg-startup.properties';startup.write_text('release=Overhaul3\nstartedUnixMs=1\ntotalMs=10\nstage.00.name=modManager\nstage.00.durationMs=10\nstage.00.elapsedMs=10\napiKey=SECRET_CANARY_NOT_EXPORTED\n')
   frames=root/'config/velvetg-frame-samples.csv';frames.write_text('sample,frameIntervalMs\n0,8.0\n1,10.0\nSECRET_CANARY_NOT_EXPORTED\n')
   log=root/'logs/latest.log';log.parent.mkdir();log.write_text('java.lang.VerifyError: SECRET_CANARY_NOT_EXPORTED\nusername: personal-data\n')
   smoke=subprocess.run([str(PS),'-NoProfile','-File',str(PACKAGE/'Test-Overhaul3.ps1'),'-GameDir',str(root),'-JavaPath',str(JAVA),'-OutputDirectory',str(results)],env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
   self.assertEqual(smoke.returncode,0,smoke.stdout)
   report=(results/'REPORT.txt').read_text();self.assertIn('OFFLINE PASS',report);self.assertIn('ONLINE SKIPPED',report)
   for f in results.iterdir():self.assertNotIn('SECRET_CANARY_NOT_EXPORTED',f.read_text());self.assertNotIn('personal-data',f.read_text())
   # Invalid local key format must fail in the selected Java runtime BEFORE any network request.
   (root/'glide/pika-api-key.txt').write_text('SECRET_CANARY_NOT_EXPORTED WITH SPACE\n')
   online_result=root/'safe online refusal'
   online=subprocess.run([str(PS),'-NoProfile','-File',str(PACKAGE/'Test-Overhaul3.ps1'),'-GameDir',str(root),'-JavaPath',str(JAVA),'-OutputDirectory',str(online_result),'-Online','-Player','FixtureUser'],env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
   self.assertNotEqual(online.returncode,0,online.stdout);self.assertNotIn('SECRET_CANARY',online.stdout)
   self.assertIn('ONLINE INCONCLUSIVE',(online_result/'REPORT.txt').read_text())
   invoke('Restore');self.assertFalse(patched.exists());self.assertFalse((root/'velvetg-lean-overhaul3/receipt.json').exists())
   for p,h in hashes.items():self.assertEqual(sha(p),h)
   invoke('Install',java_mode='home');invoke('Verify');invoke('Restore',java_mode='path')
   # A crashed process leaves a harmless reusable lock and journal; exact partial files recover.
   state=root/'velvetg-lean-overhaul3';journal=state/'transaction.json'
   (state/'install.lock').write_bytes(b'')
   patched.parent.mkdir(parents=True,exist_ok=True);patched.write_bytes(original_bytes);journal.write_text(json.dumps(receipt))
   invoke('Restore');self.assertFalse(patched.exists());self.assertFalse(journal.exists())
   # Altered orphan files must survive recovery refusal.
   patched.parent.mkdir(parents=True,exist_ok=True);patched.write_bytes(b'user modified orphan');journal.write_text(json.dumps(receipt))
   invoke('Install',False);self.assertEqual(patched.read_bytes(),b'user modified orphan');patched.unlink();journal.unlink()
   if patched.parent.exists():patched.parent.rmdir()
   # Missing original / metadata and unknown existing targets fail before final writes.
   lib.rename(lib.with_suffix('.saved'));invoke('Install',False);lib.with_suffix('.saved').rename(lib)
   original.rename(original.with_suffix('.saved'));invoke('Install',False);original.with_suffix('.saved').rename(original)
   patched.parent.mkdir(parents=True,exist_ok=True);patched.write_bytes(b'unknown existing version');invoke('Install',False);self.assertEqual(patched.read_bytes(),b'unknown existing version');patched.unlink();patched.parent.rmdir()
   # A damaged package fails checksum checks before patching.
   broken=root/'broken package';shutil.copytree(PACKAGE,broken)
   (broken/'patcher.jar').write_bytes(b'corrupt');invoke('Install',False,package=broken)
   shutil.copyfile(PACKAGE/'patcher.jar',broken/'patcher.jar')
   # A valid package with a deliberately wrong entry manifest fails AFTER patch generation.
   entries=broken/'entries.sha256';text=entries.read_text();entries.write_text(('0' if text[0]!='0' else '1')+text[1:])
   manifest=json.loads((broken/'package.json').read_text());manifest['files']['entries.sha256']=sha(entries);(broken/'package.json').write_text(json.dumps(manifest))
   invoke('Install',False,package=broken);self.assertFalse(patched.exists());self.assertFalse((state/'receipt.json').exists())
   for p,h in hashes.items():self.assertEqual(sha(p),h)
   # Target mismatch fails before backups or library creation.
   lib.write_bytes(b'wrong target');invoke('Install',False);self.assertFalse(patched.exists())
if __name__=='__main__':unittest.main()
