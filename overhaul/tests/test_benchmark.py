"""Validate the process sampler against an owned dummy process, not Minecraft."""
import os,pathlib,subprocess,sys,tempfile,unittest,csv,shutil
R=pathlib.Path(__file__).resolve().parents[1]
class ProcessSampler(unittest.TestCase):
 def test_sampler(self):
  ps=os.environ.get('POWERSHELL_EXECUTABLE',shutil.which('pwsh') or 'pwsh')
  script=R/'installer/RECORD-BENCHMARK.ps1'
  if not script.exists():script=R/'RECORD-BENCHMARK.ps1'
  process=subprocess.Popen([sys.executable,'-c','import time; time.sleep(30)'])
  try:
   with tempfile.TemporaryDirectory(prefix='samples with spaces ') as td:
    env=dict(os.environ);env.update(XDG_CACHE_HOME=td,XDG_CONFIG_HOME=td,XDG_DATA_HOME=td)
    run=subprocess.run([ps,'-NoProfile','-File',str(script),'-ProcessId',str(process.pid),'-Label','baseline','-DurationSeconds','10','-OutputDirectory',td],env=env,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    self.assertEqual(run.returncode,0,run.stdout);files=list(pathlib.Path(td).glob('baseline-*.csv'));self.assertEqual(len(files),1)
    with files[0].open(encoding='utf-8-sig') as f:rows=list(csv.DictReader(f))
    self.assertGreaterEqual(len(rows),8);self.assertIn('workingSetMiB',rows[0]);self.assertNotIn('commandLine',rows[0])
  finally:
   process.terminate();process.wait()
if __name__=='__main__':unittest.main()
