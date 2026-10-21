import copy
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import install_release as inst


class ReleaseInstallerTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.base=Path(self.tmp.name)
        self.game=self.base/'custom game directory';self.game.mkdir()
        self.source=b'known source binary';self.binary=b'new compiled artifact';self.jar=self.base/'new.jar';self.jar.write_bytes(self.binary)
        self.source_id='Original-VelvetG'
        self.source_relative='me/eldodebug/GlideClient/exact/source.jar'
        self.original_jar=self.game/'libraries'/self.source_relative;self.original_jar.parent.mkdir(parents=True);self.original_jar.write_bytes(self.source)
        self.profile=self.game/'versions'/self.source_id/(self.source_id+'.json');self.profile.parent.mkdir(parents=True)
        self.original=dict(id=self.source_id,mainClass='original.Main',libraries=[dict(name='me.eldodebug:GlideClient:exact',downloads=dict(artifact=dict(path=self.source_relative))),dict(name='unrelated:keep:1')])
        self.profile.write_bytes(inst.encoded(self.original))
        self.original_bytes=self.profile.read_bytes()
        self.known=patch.object(inst,'SOURCE_HASHES',{inst.sha(self.source)});self.known.start()

    def tearDown(self):self.known.stop();self.tmp.cleanup()
    def install(self,payload=None,**kwargs):
        if payload is not None:self.jar.write_bytes(payload)
        return inst.install(self.game,self.jar,self.source_id,inst.sha(self.jar.read_bytes()),**kwargs)
    def unchanged_original(self):
        self.assertEqual(self.original_jar.read_bytes(),self.source)
        self.assertEqual(self.profile.read_bytes(),self.original_bytes)

    def test_clean_install_custom_path_and_metadata(self):
        self.install();self.unchanged_original();receipt=inst.receipt(self.game)
        self.assertEqual(receipt['jar_sha256'],inst.sha(self.binary))
        doc=json.loads((self.game/inst.PROFILE).read_text())
        self.assertEqual(doc['mainClass'],'original.Main')
        self.assertEqual(doc['libraries'][1],dict(name='unrelated:keep:1'))

    def test_preflight_and_idempotence(self):
        self.install(check=True);self.assertFalse((self.game/inst.JAR).exists())
        self.install();self.assertEqual(self.install()['status'],'already-installed');self.unchanged_original()

    def test_upgrade_and_one_step_rollback(self):
        self.install();original_new=(self.game/inst.PROFILE).read_bytes()
        self.install(b'newer artifact')
        inst.rollback(self.game,check=True);self.assertEqual((self.game/inst.JAR).read_bytes(),b'newer artifact')
        inst.rollback(self.game)
        self.assertEqual((self.game/inst.JAR).read_bytes(),self.binary)
        self.assertEqual((self.game/inst.PROFILE).read_bytes(),original_new)
        with self.assertRaises(ValueError):inst.rollback(self.game)
        self.unchanged_original()

    def test_first_install_rollback_and_uninstall(self):
        self.install();inst.rollback(self.game);self.assertIsNone(inst.receipt(self.game))
        self.install();inst.uninstall(self.game,True);self.assertIsNotNone(inst.receipt(self.game))
        inst.uninstall(self.game);self.assertIsNone(inst.receipt(self.game));self.unchanged_original()

    def test_unknown_original_and_unknown_target_refused(self):
        self.original_jar.write_bytes(b'unknown')
        with self.assertRaises(ValueError):self.install()
        self.original_jar.write_bytes(self.source)
        target=self.game/inst.JAR;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(b'unowned')
        with self.assertRaises(ValueError):self.install()
        self.assertEqual(target.read_bytes(),b'unowned')

    def test_corrupt_payload_and_unpinned_template(self):
        with self.assertRaises(ValueError):inst.install(self.game,self.jar,self.source_id,'0'*64)
        with self.assertRaises(ValueError):inst.install(self.game,self.jar,self.source_id)
        self.unchanged_original()

    def test_failure_between_replacements_restores_entire_install(self):
        self.install();before={n:inst.read(self.game/n) for n in inst.OWNED}
        def fail(index):
            if index==1:raise OSError('injected disk-full / failed replace')
        with self.assertRaises(OSError):self.install(b'newer',fault=fail)
        self.assertEqual(before,{n:inst.read(self.game/n) for n in inst.OWNED})
        self.assertFalse((self.game/inst.STATE/'journal.json').exists());self.unchanged_original()

    def test_power_loss_recovery_at_every_replacement(self):
        class Crash(BaseException):pass
        self.install();before={n:inst.read(self.game/n) for n in inst.OWNED}
        for cut in range(len(inst.OWNED)):
            def stop(index):
                if index==cut:raise Crash()
            with self.assertRaises(Crash):self.install(b'newer',fault=stop)
            self.assertTrue((self.game/inst.STATE/'journal.json').exists())
            with inst.locked(self.game):self.assertTrue(inst.recovery(self.game))
            self.assertEqual(before,{n:inst.read(self.game/n) for n in inst.OWNED})
        self.unchanged_original()

    def test_outside_edit_and_corrupt_backup_preserved(self):
        class Crash(BaseException):pass
        self.install()
        def stop(index):raise Crash()
        with self.assertRaises(Crash):self.install(b'newer',fault=stop)
        target=self.game/inst.JAR;target.write_bytes(b'outside edit')
        with inst.locked(self.game):
            with self.assertRaises(ValueError):inst.recovery(self.game)
        self.assertEqual(target.read_bytes(),b'outside edit');self.unchanged_original()

    def test_unknown_installed_bytes_and_backup_corruption_refused(self):
        self.install();self.install(b'newer')
        (self.game/inst.BACKUPS[0]).write_bytes(b'corrupt')
        with self.assertRaises(ValueError):inst.rollback(self.game)
        (self.game/inst.JAR).write_bytes(b'unknown')
        with self.assertRaises(ValueError):inst.uninstall(self.game)
        self.unchanged_original()

    def test_symlink_and_traversal_refused(self):
        with self.assertRaises(ValueError):inst.path(self.game,'../original')
        target=self.game/inst.JAR;target.parent.mkdir(parents=True,exist_ok=True)
        try:target.symlink_to(self.original_jar)
        except (NotImplementedError,OSError):self.skipTest('Symlink unsupported')
        with self.assertRaises(ValueError):self.install()
        self.unchanged_original()
