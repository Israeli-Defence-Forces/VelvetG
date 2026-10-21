import os
from pathlib import Path
import shutil
import struct
import subprocess
import tempfile
import unittest
from combat_analyzer import read_trace

ROOT=Path(__file__).resolve().parent

class RecorderLimits(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.java=shutil.which('java');javac=shutil.which('javac')
        if not cls.java or not javac: raise unittest.SkipTest('JDK required')
        cls.tmp=tempfile.TemporaryDirectory();cls.classes=Path(cls.tmp.name)/'classes';cls.classes.mkdir()
        sources=list((ROOT/'tests-v4').glob('*.java'))+[ROOT/'tests/org/lwjgl/input/Keyboard.java']
        sources+=list((ROOT/'src/me/eldodebug/soar/combatiq').glob('*.java'))
        version=subprocess.check_output([javac,'-version'],stderr=subprocess.STDOUT,text=True)
        flags=['-source','8','-target','8'] if version.startswith('javac 1.8') else ['--release','8']
        subprocess.run([javac,*flags,'-d',str(cls.classes),*map(str,sources)],check=True,capture_output=True)

    @classmethod
    def tearDownClass(cls):
        if hasattr(cls,'tmp'):cls.tmp.cleanup()

    def run_capture(self,mode='concurrent',count=1000,properties=()):
        with tempfile.TemporaryDirectory() as d:
            export=Path(d)/'export'
            proc=subprocess.run([self.java,'-Xverify:all','-Dvelvet.combatiq.enabled=true',
                 '-Dvelvet.combatiq.exportDir='+str(export),*properties,'-cp',str(self.classes),
                 'StressV4',mode,str(count)],check=True,capture_output=True,text=True,timeout=15)
            files=list(export.glob('*.csv'));self.assertEqual(len(files),1)
            raw=files[0].read_bytes();return read_trace(files[0]),raw,proc.stdout

    def test_concurrent_exact_accounting_and_sequences(self):
        t,raw,status=self.run_capture()
        packets=[e for e in t['events'] if e['kind']==5]
        self.assertEqual(len(packets)+t['loss'],2000)
        self.assertEqual(len({e['thread'] for e in packets}),2)
        seq=[e['sequence'] for e in t['events']]
        self.assertEqual(seq,sorted(set(seq)))
        self.assertTrue(raw.endswith(b'# END\n'))

    def test_deterministic_queue_overflow(self):
        t,raw,status=self.run_capture('overflow',25000)
        self.assertGreater(t['loss'],0)
        self.assertEqual(sum(e['kind']==5 for e in t['events'])+t['loss'],25000)
        self.assertIn('EVENT_LOSS',t['reasons'])

    def test_row_cap_is_exact(self):
        t,raw,status=self.run_capture(properties=['-Dvelvet.combatiq.maxRows=12'])
        self.assertEqual(len(t['events']),12)
        self.assertTrue(t['capped'])
        self.assertTrue(raw.endswith(b'# END\n'))

    def test_size_cap_and_footer(self):
        t,raw,status=self.run_capture(count=30000,properties=['-Dvelvet.combatiq.maxBytes=65536'])
        self.assertLessEqual(len(raw),65536)
        self.assertTrue(t['capped']);self.assertGreater(t['loss'],0)
        self.assertTrue(raw.endswith(b'# END\n'))

    def test_remapped_forward_ignores_w(self):
        t,raw,status=self.run_capture('remap')
        edges=[e for e in t['events'] if e['kind'] in (1,2)]
        self.assertEqual([e['data'] for e in edges],[33,33,33])
        self.assertEqual([e['kind'] for e in edges],[2,1,2])

    def test_disk_error_does_not_escape_game_callback(self):
        with tempfile.TemporaryDirectory() as d:
            blocked=Path(d)/'blocked';blocked.write_text('file prevents mkdir')
            p=subprocess.run([self.java,'-Dvelvet.combatiq.enabled=true',
                 '-Dvelvet.combatiq.exportDir='+str(blocked/'child'),'-cp',str(self.classes),
                 'StressV4','concurrent','100'],check=True,capture_output=True,text=True,timeout=15)
            self.assertIn('recorder stopped:',p.stdout)
            self.assertEqual(blocked.read_text(),'file prevents mkdir')

    def test_restart_and_repeated_enable(self):
        with tempfile.TemporaryDirectory() as d:
            subprocess.run([self.java,'-Dvelvet.combatiq.enabled=true',
                 '-Dvelvet.combatiq.exportDir='+d,'-cp',str(self.classes),
                 'StressV4','lifecycle','1'],check=True,capture_output=True,text=True,timeout=15)
            files=sorted(Path(d).glob('*.csv'));self.assertEqual(len(files),2)
            traces=[read_trace(p) for p in files]
            self.assertTrue(all(t['complete'] for t in traces))
            self.assertEqual([t['events'][0]['sequence'] for t in traces],[1,1])
            self.assertGreater(traces[1]['events'][0]['epoch'],traces[0]['events'][0]['epoch'])

    def test_archive_verification_preserves_unrelated_and_all_siblings(self):
        import json
        import zipfile
        from build_v4 import verify,BRIDGE,ADDED_MIXINS
        classes=self.classes
        expected={p.relative_to(classes).as_posix():p.read_bytes()
                  for p in (classes/'me/eldodebug/soar/combatiq').glob('*.class')}
        with tempfile.TemporaryDirectory() as d:
            baseline=Path(d)/'baseline.jar';patched=Path(d)/'patched.jar'
            with zipfile.ZipFile(baseline,'w') as z:
                z.writestr('unchanged.txt','preserve')
                z.writestr(BRIDGE+'CombatIQNative.class',b'old')
                z.writestr('mixins.soar.json','{"mixins":[]}')
            with zipfile.ZipFile(patched,'w') as z:
                z.writestr('unchanged.txt','preserve')
                z.writestr('mixins.soar.json',json.dumps({'mixins':['combatiq.'+n for n in ADDED_MIXINS]}))
                for name,raw in expected.items(): z.writestr(name,raw)
            result=verify(baseline,patched,classes)
            self.assertEqual(result['java_class_major'],52)
            with zipfile.ZipFile(patched,'w') as z:
                z.writestr('unchanged.txt','preserve')
                z.writestr('mixins.soar.json',json.dumps({'mixins':['combatiq.'+n for n in ADDED_MIXINS]}))
                for name,raw in expected.items():
                    if not name.endswith('Snapshot.class'):z.writestr(name,raw)
            with self.assertRaises(AssertionError):verify(baseline,patched,classes)

    def test_all_bridge_siblings_are_java8(self):
        siblings=list((self.classes/'me/eldodebug/soar/combatiq').glob('*.class'))
        self.assertGreaterEqual(len(siblings),6)
        self.assertTrue(all(struct.unpack('>H',p.read_bytes()[6:8])[0]==52 for p in siblings))
