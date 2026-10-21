import contextlib
import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import upgrade_v4 as u

class UpgradeV4Tests(unittest.TestCase):
    @contextlib.contextmanager
    def fixture(self):
        with tempfile.TemporaryDirectory() as d:
            d=Path(d);game=d/'game';base,lib,meta=u.paths(game)
            for p in (base,lib,meta):p.parent.mkdir(parents=True,exist_ok=True)
            base.write_bytes(b'synthetic baseline');lib.write_bytes(b'synthetic previous')
            new=d/'v4.jar';new.write_bytes(b'synthetic current')
            doc={'id':u.ID,'libraries':[{'name':'me.eldodebug:GlideClient:'+u.VER,
                 'downloads':{'artifact':{'path':u.ARTIFACT_PATH,'sha1':u.digest(lib,'sha1'),'size':lib.stat().st_size}}}]}
            meta.write_text(json.dumps(doc));original_meta=meta.read_bytes()
            with patch.multiple(u,BASE_SHA=u.digest(base),PREVIOUS_SHA=u.digest(lib),CURRENT_SHA=u.digest(new)):
                yield game,base,lib,meta,new,original_meta
    def test_check_upgrade_idempotence_rollback(self):
        with self.fixture() as (game,base,lib,meta,new,original_meta):
            old=lib.read_bytes();baseline=base.read_bytes()
            u.upgrade(game,new,True);self.assertEqual(lib.read_bytes(),old)
            u.upgrade(game,new,False);self.assertEqual(lib.read_bytes(),new.read_bytes())
            u.upgrade(game,new,True)
            u.rollback(game,True);self.assertEqual(lib.read_bytes(),new.read_bytes())
            u.rollback(game,False);self.assertEqual(lib.read_bytes(),old)
            self.assertEqual(meta.read_bytes(),original_meta);self.assertEqual(base.read_bytes(),baseline)
    def test_unknown_binary(self):
        with self.fixture() as (game,base,lib,meta,new,_):
            lib.write_bytes(b'unknown')
            with self.assertRaises(ValueError):u.upgrade(game,new,False)
            self.assertEqual(lib.read_bytes(),b'unknown')
    def test_unknown_baseline(self):
        with self.fixture() as (game,base,lib,meta,new,_):
            base.write_bytes(b'unknown baseline')
            with self.assertRaises(ValueError):u.upgrade(game,new,False)
    def test_bad_payload(self):
        with self.fixture() as (game,base,lib,meta,new,_):
            new.write_bytes(b'bad')
            with self.assertRaises(ValueError):u.upgrade(game,new,False)
    def test_backup_collision_preserved(self):
        with self.fixture() as (game,base,lib,meta,new,_):
            b=lib.with_name(lib.name+'.combatiq-v3-backup');b.write_bytes(b'preserve')
            with self.assertRaises(FileExistsError):u.upgrade(game,new,False)
            self.assertEqual(b.read_bytes(),b'preserve')
    def test_launcher_metadata(self):
        with self.fixture() as (game,base,lib,meta,new,_):
            doc=json.loads(meta.read_text());doc['libraries'][0]['downloads']['artifact']['size']=0
            meta.write_text(json.dumps(doc))
            with self.assertRaises(ValueError):u.upgrade(game,new,False)
    def test_failure_between_replaces_restores(self):
        with self.fixture() as (game,base,lib,meta,new,original_meta):
            old=lib.read_bytes();real=u.os.replace
            def fail(source,target):
                if str(source).endswith('.combatiq-v4-staged') and Path(target)==meta:
                    raise OSError('injected second replace failure')
                return real(source,target)
            with patch.object(u.os,'replace',side_effect=fail):
                with self.assertRaises(OSError):u.upgrade(game,new,False)
            self.assertEqual(lib.read_bytes(),old);self.assertEqual(meta.read_bytes(),original_meta)
    def test_rollback_metadata_tamper_refused(self):
        with self.fixture() as (game,base,lib,meta,new,_):
            u.upgrade(game,new,False)
            bak=meta.with_name(meta.name+'.combatiq-v3-backup');doc=json.loads(bak.read_text())
            doc['libraries'][0]['downloads']['artifact']['path']='bad';bak.write_text(json.dumps(doc))
            with self.assertRaises(ValueError):u.rollback(game,False)
            self.assertEqual(lib.read_bytes(),new.read_bytes())

if __name__=='__main__':unittest.main()
