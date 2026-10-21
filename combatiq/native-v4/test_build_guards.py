import hashlib
import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from build_v4 import class_references,VerificationError,pinned_installer
from restore_parts import restore

class BuildGuards(unittest.TestCase):
    def test_verification_is_not_disabled_by_python_optimization(self):
        root=Path(__file__).parent
        p=subprocess.run([sys.executable,'-O','-c',"from build_v4 import require; require(False,'required guard')"],cwd=root,capture_output=True,text=True)
        self.assertNotEqual(p.returncode,0);self.assertIn('required guard',p.stderr)

    def test_malformed_class_pool_refused(self):
        for raw in [b'',b'not-a-class',b'\xca\xfe\xba\xbe\0\0\0\x34\0\x02\xff']:
            with self.assertRaises(VerificationError):class_references(raw)

    def test_new_installer_does_not_reuse_historical_v4_script(self):
        source=pinned_installer('a'*64,'My-CombatIQ.jar');scope={'__name__':'generated'}
        exec(compile(source,'generated.py','exec'),scope)
        self.assertEqual(scope['EXPECTED_JAR_SHA256'],'a'*64)
        self.assertEqual(scope['DEFAULT_JAR'],'My-CombatIQ.jar')
        self.assertIn('transaction',scope);self.assertIn('recovery',scope);self.assertIn('uninstall',scope)
        self.assertNotIn('CURRENT_SHA',scope)
        with self.assertRaises(ValueError):pinned_installer('a'*64,'../escape.jar')

    def test_private_parts_reconstruct_and_verify_before_publication(self):
        with tempfile.TemporaryDirectory() as d:
            d=Path(d);parts=[d/'a.bin',d/'b.bin'];parts[0].write_bytes(b'first');parts[1].write_bytes(b'second')
            out=d/'baseline.jar';expected=hashlib.sha256(b'firstsecond').hexdigest()
            with self.assertRaises(ValueError):restore(list(reversed(parts)),out,expected)
            self.assertFalse(out.exists())
            restore(parts,out,expected);self.assertEqual(out.read_bytes(),b'firstsecond')
            with self.assertRaises(FileExistsError):restore(parts,out,expected)
            self.assertFalse(list(d.glob('.combatiq-restore-*')))
