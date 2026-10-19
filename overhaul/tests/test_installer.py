"""Exercise real PowerShell and Java 8 against a private temporary Minecraft layout."""
import pathlib,tempfile,shutil,subprocess,json,hashlib,os,unittest
R=pathlib.Path(__file__).resolve().parents[1]
PS=pathlib.Path(os.environ.get('POWERSHELL_EXECUTABLE',shutil.which('pwsh') or 'pwsh'))
JAVA=pathlib.Path(os.environ.get('JAVA8',shutil.which('java') or 'java'))
BASE=pathlib.Path(os.environ.get('BASELINE_JAR','original-client.jar'))
MC=pathlib.Path(os.environ.get('MINECRAFT_JAR','minecraft-1.8.9.jar'))
PACKAGE=pathlib.Path(os.environ.get('PACKAGE_DIR',str(R if (R/'package.json').exists() else R/'dist/VelvetG-Lean-Overhaul2')))
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
   hashes={p:sha(p) for p in [lib,original,screen,config,launcher,old_version,old_receipt,old_settings]}
   def invoke(action,ok=True,extra=(),java_mode="explicit"):
    env=dict(os.environ);env.update(XDG_CACHE_HOME=str(root/'cache'),XDG_CONFIG_HOME=str(root/'psconfig'),XDG_DATA_HOME=str(root/'psdata'))
    java_args=['-JavaPath',str(JAVA)] if java_mode=='explicit' else []
    if java_mode!='explicit':
     env['PATH']=str(JAVA.resolve().parent)+os.pathsep+env.get('PATH','')
     env.pop('JAVA_HOME',None)
     if java_mode=='home':env['JAVA_HOME']=str(JAVA.resolve().parent.parent)
     elif java_mode=='stale-home':env['JAVA_HOME']=str(root/'missing Java home')
    r=subprocess.run([str(PS),'-NoProfile','-File',str(PACKAGE/'Manage-VelvetG.ps1'),'-Action',action,'-GameDir',str(root),*java_args,*extra],env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    if ok:self.assertEqual(r.returncode,0,r.stdout)
    else:self.assertNotEqual(r.returncode,0,r.stdout)
    self.assertNotIn('DO_NOT_EXPORT',r.stdout);return r
   invoke('Install',java_mode='path');self.assertTrue((root/'velvetg-lean-overhaul2/receipt.json').exists());invoke('Verify',java_mode='home');invoke('Verify',java_mode='stale-home');invoke('Install',False)
   for p,h in hashes.items():self.assertEqual(sha(p),h)
   receipt=json.loads((root/'velvetg-lean-overhaul2/receipt.json').read_text());patched=pathlib.Path(receipt['library']);self.assertTrue(patched.exists())
   installed=json.loads(pathlib.Path(receipt['versionJson']).read_text());entry=next(x for x in installed['libraries'] if x['name']==receipt['coordinate']);self.assertEqual(entry['checksums'],[hashlib.sha1(patched.read_bytes()).hexdigest()]);self.assertEqual(entry['size'],patched.stat().st_size);self.assertEqual(entry['path'],entry['downloads']['artifact']['path'])
   # Offline integrity detects damage and rollback refuses to discard changed user files.
   original_bytes=patched.read_bytes();patched.write_bytes(original_bytes+b'corruption');invoke('Verify',False);invoke('Restore',False);self.assertTrue(patched.exists());patched.write_bytes(original_bytes)
   invoke('Restore');self.assertFalse(patched.exists());self.assertFalse((root/'velvetg-lean-overhaul2/receipt.json').exists())
   for p,h in hashes.items():self.assertEqual(sha(p),h)
   invoke('Install',java_mode='home');invoke('Verify');invoke('Restore',java_mode='path')
   # Target mismatch fails before backups or library creation.
   lib.write_bytes(b'wrong target');invoke('Install',False);self.assertFalse(patched.exists())
if __name__=='__main__':unittest.main()
