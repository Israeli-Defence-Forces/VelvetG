import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import unittest
from combat_analyzer import analyze

ROOT=Path(__file__).resolve().parent
class NativeRuntimeTests(unittest.TestCase):
    def test_mock_hooks_and_anonymous_packaging(self):
        javac=shutil.which('javac');java=shutil.which('java')
        if not javac or not java:self.skipTest('JDK required for native Java hook harness')
        version=subprocess.check_output([javac,'-version'],stderr=subprocess.STDOUT,text=True)
        major=int(re.search(r'javac (?:1\.)?(\d+)',version)[1])
        flags=['--release','8'] if major>8 else ['-source','8','-target','8']
        with tempfile.TemporaryDirectory() as d:
            d=Path(d);classes=d/'classes';classes.mkdir();home=d/'home';home.mkdir()
            sources=list((ROOT/'tests-v4').glob('*.java'))
            sources.append(ROOT/'tests/org/lwjgl/input/Keyboard.java')
            sources.extend((ROOT/'src/me/eldodebug/soar/combatiq').glob('*.java'))
            subprocess.run([javac,'-proc:none']+flags+['-d',str(classes)]+[str(p) for p in sources],check=True,capture_output=True,text=True)
            subprocess.run([java,'-Xverify:all','-Dvelvet.combatiq.enabled=true','-Duser.home='+str(home),
                            '-cp',str(classes),'HarnessV4'],check=True,capture_output=True,text=True,timeout=15)
            files=list((home/'VelvetG-CombatIQ').glob('*.csv'));self.assertEqual(len(files),1)
            a=analyze(files[0]);self.assertTrue(a['recording']['complete'])
            self.assertEqual(a['wtap']['sampled'],1)
            self.assertEqual([t['data'] for t in a['sprint']['transitions']],[0,1])
            self.assertEqual(a['packets']['5']['ATTACK'],1)
            self.assertEqual(a['packets']['5']['INTERACT'],1)
            self.assertEqual(a['packets']['6']['VELOCITY'],1)
            self.assertEqual(a['packets']['15']['ENTITY_STATUS'],1)
            self.assertNotIn('16',a['recording']['counts'])
            self.assertTrue(a['coverage']['actual_transport_write']=='NOT_INSTRUMENTED')
            self.assertGreaterEqual(len({t['thread'] for t in a['sprint']['transitions']}),1)
            self.assertNotIn('java.lang.Object',files[0].read_text())
            from combat_analyzer import read_trace
            events=read_trace(files[0])['events']
            client={e['thread'] for e in events if e['kind'] == 4}
            self.assertEqual(len(client),1)
            self.assertTrue(all(e['thread'] in client for e in events if e['kind'] in (1,2,3,9,10,11,12,13,14)))
            self.assertEqual(sum(e['kind']==12 for e in events),2)
            self.assertEqual(sum(e['kind']==13 for e in events),1)
    def test_output_creation_and_schema(self):
        # Reflection errors must remain detectable; malformed evidence is not a success.
        from combat_analyzer import read_trace
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'broken.csv';p.write_text('# bogus\nx\n')
            with self.assertRaises(ValueError):read_trace(p)

if __name__=='__main__':unittest.main()

