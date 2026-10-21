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
                z.writestr('mixins.soar.json',json.dumps({'mixins':[]}))
                for name,raw in expected.items(): z.writestr(name,raw)
            result=verify(baseline,patched,classes,required_mixins=[])
            self.assertEqual(result['java_class_major'],52)
            with zipfile.ZipFile(patched,'w') as z:
                z.writestr('unchanged.txt','preserve')
                z.writestr('mixins.soar.json',json.dumps({'mixins':[]}))
                for name,raw in expected.items():
                    if not name.endswith('Snapshot.class'):z.writestr(name,raw)
            with self.assertRaises(ValueError):verify(baseline,patched,classes,required_mixins=[])

    def test_connection_identity_motion_mouse_and_export_integration(self):
        from combat_analyzer import analyze
        import json
        with tempfile.TemporaryDirectory() as d:
            subprocess.run([self.java,'-Xverify:all','-Dvelvet.combatiq.enabled=true',
                '-Dvelvet.combatiq.exportDir='+d,'-cp',str(self.classes),'IntegrationV3'],
                check=True,capture_output=True,text=True,timeout=15)
            files=list(Path(d).glob('*.csv'));self.assertEqual(len(files),1)
            self.assertFalse(list(Path(d).glob('*.partial')))
            t=read_trace(files[0]);self.assertEqual(t['schema'],3);self.assertTrue(t['complete'])
            events=t['events']
            network=[e for e in events if e['kind']==26]
            self.assertEqual(len(network),2)
            self.assertNotEqual(network[0]['connection'],network[1]['connection'])
            packet=[e for e in events if e['kind'] in (5,23,24,26) and e['epoch']==network[0]['epoch']]
            self.assertEqual(len({e['packetId'] for e in packet}),1)
            self.assertTrue(all(e['packetId']>0 for e in packet))
            self.assertTrue(any(e['kind']==17 and e['y']==.4 for e in events))
            self.assertTrue(any(e['kind']==18 and e['x']==1 for e in events))
            self.assertTrue(any(e['kind']==21 and e['x']==3 for e in events))
            self.assertEqual([e['data'] for e in events if e['kind']==19],[1,3])
            self.assertTrue(any(e['kind']==22 and e['phase']==5 for e in events))
            self.assertTrue(any(e['kind']==22 and e['phase']==6 for e in events))
            report=analyze(files[0]);self.assertLess(report['normalized_event_count'],len(events))
            self.assertEqual(len(report['analyzers']['hit_select']['observations']),1)
            exports=list(Path(d).glob('combatiq-report-*.json'));self.assertEqual(len(exports),1)
            self.assertEqual(json.loads(exports[0].read_text())['server_outcome'],'UNKNOWN')

    def test_rotation_is_replayable_with_checksums(self):
        from combat_analyzer import read_session,analyze
        with tempfile.TemporaryDirectory() as d:
            subprocess.run([self.java,'-Dvelvet.combatiq.enabled=true','-Dvelvet.combatiq.rotate=true',
                '-Dvelvet.combatiq.maxBytes=65536','-Dvelvet.combatiq.exportDir='+d,
                '-cp',str(self.classes),'StressV4','concurrent','1500'],check=True,capture_output=True,text=True,timeout=15)
            files=list(Path(d).glob('*.csv'));self.assertGreater(len(files),1)
            self.assertTrue(all(p.stat().st_size<=65536 for p in files))
            t=read_session(list(reversed(files)));self.assertTrue(t['complete']);self.assertEqual(t['loss'],0)
            self.assertEqual(sum(e['kind']==5 for e in t['events']),3000)
            with self.assertRaises(ValueError):read_session(files[1:])
            p=files[0];p.write_bytes(p.read_bytes().replace(b',5,13,',b',5,14,',1))
            with self.assertRaises(ValueError):read_trace(p)

    def test_duration_cap_finishes_without_gameplay_calls(self):
        t,raw,status=self.run_capture('duration',100,properties=['-Dvelvet.combatiq.durationSeconds=1'])
        self.assertTrue(t['capped']);self.assertTrue(raw.endswith(b'# END\n'))

    def test_real_mid_write_failure_preserves_partial_output(self):
        if os.name!='posix':self.skipTest('POSIX file quota simulation; Windows live storage validation separate')
        import resource,signal
        def quota():
            signal.signal(signal.SIGXFSZ,signal.SIG_IGN)
            resource.setrlimit(resource.RLIMIT_FSIZE,(4096,4096))
        with tempfile.TemporaryDirectory() as d:
            p=subprocess.run([self.java,'-XX:-UsePerfData','-Dvelvet.combatiq.enabled=true',
                '-Dvelvet.combatiq.exportDir='+d,'-cp',str(self.classes),'StressV4','concurrent','1000'],
                preexec_fn=quota,check=True,capture_output=True,text=True,timeout=15)
            self.assertIn('recorder stopped:',p.stdout)
            self.assertFalse(list(Path(d).glob('*.csv')))
            partial=list(Path(d).glob('*.partial'));self.assertEqual(len(partial),1)
            self.assertFalse(read_trace(partial[0])['complete'])

    def test_abrupt_jvm_exit_leaves_identifiable_partial_file(self):
        import time
        with tempfile.TemporaryDirectory() as d:
            p=subprocess.Popen([self.java,'-Dvelvet.combatiq.enabled=true',
                '-Dvelvet.combatiq.exportDir='+d,'-cp',str(self.classes),'StressV4','pause','100'],
                stdout=subprocess.PIPE,stderr=subprocess.PIPE)
            try:
                deadline=time.monotonic()+5
                while time.monotonic()<deadline:
                    partial=list(Path(d).glob('*.partial'))
                    if partial and partial[0].stat().st_size>0:break
                    time.sleep(.01)
                p.kill();p.communicate(timeout=5)
                partial=list(Path(d).glob('*.partial'));self.assertEqual(len(partial),1)
                self.assertFalse(read_trace(partial[0])['complete'])
                self.assertFalse(list(Path(d).glob('*.csv')))
            finally:
                if p.poll() is None:p.kill();p.communicate(timeout=5)

    def test_all_bridge_siblings_are_java8(self):
        siblings=list((self.classes/'me/eldodebug/soar/combatiq').glob('*.class'))
        self.assertGreaterEqual(len(siblings),6)
        self.assertTrue(all(struct.unpack('>H',p.read_bytes()[6:8])[0]==52 for p in siblings))
