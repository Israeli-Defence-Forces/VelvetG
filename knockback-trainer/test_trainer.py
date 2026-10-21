import hashlib,importlib.util,json,os,shutil,subprocess,sys,tempfile,unittest,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
class TrainerTests(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.java=shutil.which('java');cls.javac=shutil.which('javac')
  if not cls.java or not cls.javac:raise unittest.SkipTest('JDK required')
  cls.tmp=tempfile.TemporaryDirectory();cls.classes=Path(cls.tmp.name)/'classes';cls.classes.mkdir()
  version=subprocess.check_output([cls.javac,'-version'],stderr=subprocess.STDOUT,text=True)
  cls.flags=['-source','8','-target','8'] if version.startswith('javac 1.8') else ['--release','8']
  sources=list((ROOT/'src').rglob('*.java'))+list((ROOT/'tests').rglob('*.java'))
  subprocess.run([cls.javac,*cls.flags,'-d',str(cls.classes),*map(str,sources)],check=True,capture_output=True)
 @classmethod
 def tearDownClass(cls):
  if hasattr(cls,'tmp'):cls.tmp.cleanup()
 def run_harness(self,name):
  p=subprocess.run([self.java,'-Xverify:all','-cp',str(self.classes),name],capture_output=True,text=True,timeout=15)
  self.assertEqual(p.returncode,0,p.stdout+p.stderr)
  print(p.stdout.strip());self.assertIn('PASS:',p.stdout)
 def test_timing_stats_ground_and_uncertainty(self):self.run_harness('EngineHarness')
 def test_native_bridge_without_csv(self):self.run_harness('RuntimeHarness')
 def test_passive_hot_paths_have_no_file_input_or_packet_mutation(self):
  source='\n'.join(p.read_text() for folder in ('src','native','mixins') for p in (ROOT/folder).rglob('*.java'))
  for forbidden in ['java.io.','java.nio.file','setKeyBindState','sendPacket','writeAndFlush','Thread.sleep','Runtime.getRuntime','getDeclaredField("motion']:
   self.assertNotIn(forbidden,source)
 def test_all_core_classes_java8(self):
  classes=list(self.classes.rglob('*.class'));self.assertGreater(len(classes),10)
  self.assertTrue(all(int.from_bytes(p.read_bytes()[6:8],'big')==52 for p in classes))
