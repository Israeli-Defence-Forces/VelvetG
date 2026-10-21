#!/usr/bin/env python3
"""Side-by-side RC1 installation with exact artifact pinning and durable recovery.
The build replaces EXPECTED_JAR_SHA256. This unpinned template refuses installation.
"""
import argparse
import contextlib
import copy
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import tempfile

EXPECTED_JAR_SHA256 = None
DEFAULT_JAR = None
EXPECTED_BASELINE_SHA256 = '7949d73d9be9eb74b662b2844faee2939c0463222c01b4a89378f19ddaa8bbc7'
SOURCE_HASHES = {EXPECTED_BASELINE_SHA256,
    '696bce5471d2db5ad0b0be567204efc9e65ebeb075688b10ac8555fc69689455',
    '3e4b0190e26b4a1540ac23ff018a8c8280b8ab447c34cce0fc5bf8cc6c2a6813'}
ID='VelvetG-CombatIQ-RC1'
VERSION='2.0.9.6-CombatIQ-RC1'
JAR='libraries/me/eldodebug/GlideClient/'+VERSION+'/GlideClient-'+VERSION+'.jar'
PROFILE='versions/'+ID+'/'+ID+'.json'
STATE='.combatiq-rc1'
RECEIPT=STATE+'/receipt.json'
BACKUPS=[STATE+'/rollback.jar',STATE+'/rollback.json',STATE+'/rollback.receipt.json']
OWNED=[JAR,PROFILE,RECEIPT]+BACKUPS


def encoded(value):return (json.dumps(value,indent=2,ensure_ascii=False)+'\n').encode('utf-8')
def sha(data):return hashlib.sha256(data).hexdigest()
def read(path):return path.read_bytes() if path.is_file() else None


def path(root,relative):
    parts=PurePosixPath(relative)
    if parts.is_absolute() or '..' in parts.parts or '\\' in relative:raise ValueError('Unsafe relative path')
    current=root
    for part in parts.parts:
        current=current/part
        if current.is_symlink():raise ValueError('Symlink in install/source path: '+relative)
    if not current.resolve().is_relative_to(root):raise ValueError('Path escapes game directory')
    return current


def durable(pathname,data):
    pathname.parent.mkdir(parents=True,exist_ok=True)
    fd,tmp=tempfile.mkstemp(prefix='.combatiq-',dir=pathname.parent)
    try:
        with os.fdopen(fd,'wb') as f:f.write(data);f.flush();os.fsync(f.fileno())
        os.replace(tmp,pathname)
    finally:
        if os.path.exists(tmp):os.unlink(tmp)


@contextlib.contextmanager
def locked(root):
    folder=path(root,STATE);folder.mkdir(parents=True,exist_ok=True)
    with path(root,STATE+'/lock').open('a+b') as f:
        if os.name=='nt':
            import msvcrt
            f.seek(0);f.write(b'0');f.flush();f.seek(0)
            try:msvcrt.locking(f.fileno(),msvcrt.LK_NBLCK,1)
            except OSError as e:raise RuntimeError('Another installation/recovery is running') from e
        else:
            import fcntl
            try:fcntl.flock(f,fcntl.LOCK_EX|fcntl.LOCK_NB)
            except BlockingIOError as e:raise RuntimeError('Another installation/recovery is running') from e
        try:yield
        finally:
            if os.name=='nt':f.seek(0);msvcrt.locking(f.fileno(),msvcrt.LK_UNLCK,1)
            else:fcntl.flock(f,fcntl.LOCK_UN)


def recovery(root):
    """Rollback an interrupted transaction, refusing to overwrite outside edits."""
    journal=path(root,STATE+'/journal.json');pending=path(root,STATE+'/pending')
    if not journal.exists():
        # No target mutation starts before journal publication.
        if pending.exists():shutil.rmtree(pending)
        return False
    doc=json.loads(journal.read_text())
    if doc.get('schema')!=1 or not isinstance(doc.get('entries'),list):raise ValueError('Invalid recovery journal')
    entries=doc['entries']
    if len({e['path'] for e in entries})!=len(entries):raise ValueError('Duplicate journal target')
    restores=[]
    for i,e in enumerate(entries):
        if e.get('path') not in OWNED:raise ValueError('Unexpected recovery target')
        before=e.get('before');after=e.get('after');current=read(path(root,e['path']))
        current_hash=sha(current) if current is not None else None
        if current_hash not in (before,after):raise ValueError('Outside edit detected; recovery refused for '+e['path'])
        data=read(pending/(str(i)+'.before')) if before is not None else None
        if (sha(data) if data is not None else None)!=before:raise ValueError('Corrupt recovery backup')
        restores.append((path(root,e['path']),data))
    for target,data in restores:
        if data is None:target.unlink(missing_ok=True)
        else:durable(target,data)
    journal.unlink();shutil.rmtree(pending)
    return True


def transaction(root,updates,fault=None):
    journal=path(root,STATE+'/journal.json');pending=path(root,STATE+'/pending')
    if journal.exists() or pending.exists():raise RuntimeError('Interrupted transaction found; run recover first')
    if any(name not in OWNED for name in updates):raise ValueError('Unexpected install target')
    pending.mkdir()
    entries=[]
    try:
        for i,(name,after) in enumerate(updates.items()):
            before=read(path(root,name))
            if before is not None:durable(pending/(str(i)+'.before'),before)
            if after is not None:durable(pending/(str(i)+'.after'),after)
            entries.append(dict(path=name,before=sha(before) if before is not None else None,
                                after=sha(after) if after is not None else None))
        durable(journal,encoded(dict(schema=1,entries=entries)))
        for i,e in enumerate(entries):
            target=path(root,e['path']);current=read(target)
            if (sha(current) if current is not None else None)!=e['before']:raise ValueError('Target changed during installation')
            if e['after'] is None:target.unlink(missing_ok=True)
            else:
                target.parent.mkdir(parents=True,exist_ok=True)
                os.replace(pending/(str(i)+'.after'),target)
            if fault:fault(i)
        # Verify durable target bytes BEFORE deleting the undo journal.
        for e in entries:
            data=read(path(root,e['path']))
            if (sha(data) if data is not None else None)!=e['after']:raise ValueError('Post-install verification failed')
        journal.unlink();shutil.rmtree(pending)
    except Exception:
        if journal.exists():recovery(root)
        elif pending.exists():shutil.rmtree(pending)
        raise


def receipt(root):
    data=read(path(root,RECEIPT))
    if data is None:
        if any(path(root,n).exists() for n in (JAR,PROFILE)):raise ValueError('Unknown RC1 installation; refusing overwrite')
        return None
    doc=json.loads(data)
    if doc.get('schema')!=1 or doc.get('id')!=ID:raise ValueError('Invalid installation receipt')
    for name,key in ((JAR,'jar_sha256'),(PROFILE,'profile_sha256')):
        value=read(path(root,name))
        if value is None or sha(value)!=doc.get(key):raise ValueError('Installed files do not match receipt')
    profile=json.loads(path(root,PROFILE).read_text(encoding='utf-8-sig'))
    matches=[x for x in profile.get('libraries',[]) if isinstance(x,dict) and x.get('name')=='me.eldodebug:GlideClient:'+VERSION]
    jar=path(root,JAR)
    if profile.get('id')!=ID or len(matches)!=1:raise ValueError('Installed launcher metadata mismatch')
    artifact=matches[0].get('downloads',{}).get('artifact',{})
    if artifact.get('path')!=JAR[len('libraries/'):] or artifact.get('size')!=jar.stat().st_size or artifact.get('sha1')!=hashlib.sha1(jar.read_bytes()).hexdigest():
        raise ValueError('Installed launcher artifact metadata mismatch')
    return doc


def install(game,jar,source_version,expected_sha=EXPECTED_JAR_SHA256,check=False,fault=None):
    root=game.resolve();root.mkdir(parents=True,exist_ok=True)
    if not isinstance(expected_sha,str) or not re.fullmatch('[0-9a-f]{64}',expected_sha):raise ValueError('Installer is not pinned by a verified build')
    payload=jar.read_bytes()
    if sha(payload)!=expected_sha:raise ValueError('Patched JAR SHA-256 mismatch')
    with locked(root):
        if path(root,STATE+'/journal.json').exists():raise RuntimeError('Run recover before installation')
        old=receipt(root)
        if old and old['jar_sha256']==expected_sha:return dict(status='already-installed',sha256=expected_sha)
        if not isinstance(source_version,str) or not re.fullmatch('[A-Za-z0-9_-][A-Za-z0-9_.-]*',source_version) or source_version==ID:
            raise ValueError('Select an existing original/v3/v4 source profile, not the RC1 target')
        original=path(root,'versions/'+source_version+'/'+source_version+'.json')
        doc=json.loads(original.read_text(encoding='utf-8-sig'))
        if doc.get('id')!=source_version:raise ValueError('Source profile ID mismatch')
        entries=[(i,x) for i,x in enumerate(doc.get('libraries',[])) if isinstance(x,dict) and x.get('name','').startswith('me.eldodebug:GlideClient:')]
        if len(entries)!=1:raise ValueError('Source profile must directly declare exactly one GlideClient library')
        index,library=entries[0]
        source_path=library.get('downloads',{}).get('artifact',{}).get('path')
        if not isinstance(source_path,str):raise ValueError('Source GlideClient has no explicit verified artifact path')
        source_jar=path(root,'libraries/'+source_path)
        if sha(source_jar.read_bytes()) not in SOURCE_HASHES:raise ValueError('Unknown source GlideClient hash')
        target=copy.deepcopy(doc);target['id']=ID
        target['libraries'][index]=dict(name='me.eldodebug:GlideClient:'+VERSION,downloads=dict(artifact=dict(
            path=JAR[len('libraries/'):],sha1=hashlib.sha1(payload).hexdigest(),size=len(payload),url='')))
        profile=encoded(target)
        previous={n:read(path(root,n)) for n in (JAR,PROFILE,RECEIPT)}
        rollback_hashes={n:sha(value) if value is not None else None for n,value in zip(BACKUPS,previous.values())}
        result=dict(schema=1,id=ID,jar_sha256=expected_sha,profile_sha256=sha(profile),can_rollback=True,rollback=rollback_hashes)
        if check:return dict(status='verified-no-changes',sha256=expected_sha)
        updates={JAR:payload,PROFILE:profile,RECEIPT:encoded(result)}
        updates.update(zip(BACKUPS,previous.values()))
        transaction(root,updates,fault)
        receipt(root)
        return dict(status='installed',sha256=expected_sha,version=ID)


def rollback(game,check=False):
    root=game.resolve()
    with locked(root):
        doc=receipt(root)
        if not doc or not doc.get('can_rollback'):raise ValueError('No one-step rollback available')
        values=[]
        for name in BACKUPS:
            value=read(path(root,name));expected=doc.get('rollback',{}).get(name,'missing')
            if (sha(value) if value is not None else None)!=expected:raise ValueError('Rollback backup missing/corrupt')
            values.append(value)
        if check:return dict(status='rollback-verified-no-changes')
        if values[2] is not None:
            previous=json.loads(values[2]);previous['can_rollback']=False;previous['rollback']={};values[2]=encoded(previous)
        updates=dict(zip((JAR,PROFILE,RECEIPT),values));updates.update((n,None) for n in BACKUPS)
        transaction(root,updates)
        receipt(root)
        return dict(status='rolled-back')


def uninstall(game,check=False):
    root=game.resolve()
    with locked(root):
        if not receipt(root):return dict(status='not-installed')
        if not check:transaction(root,{n:None for n in OWNED})
        return dict(status='uninstall-verified-no-changes' if check else 'uninstalled-original-preserved')


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('action',choices=['install','check','rollback','uninstall','recover'])
    p.add_argument('--game-dir',type=Path,required=True);p.add_argument('--jar',type=Path,default=Path(__file__).parent/DEFAULT_JAR if DEFAULT_JAR else None)
    p.add_argument('--source-version');p.add_argument('--check',action='store_true')
    a=p.parse_args()
    try:
        if a.action=='install':
            if not a.jar or not a.source_version:p.error('install requires --jar and --source-version')
            result=install(a.game_dir,a.jar,a.source_version,check=a.check)
        elif a.action=='rollback':result=rollback(a.game_dir,a.check)
        elif a.action=='uninstall':result=uninstall(a.game_dir,a.check)
        else:
            root=a.game_dir.resolve()
            with locked(root):result=dict(recovered=recovery(root)) if a.action=='recover' else receipt(root)
        print(json.dumps(result,indent=2))
    except (OSError,ValueError,RuntimeError,KeyError,TypeError) as error:p.exit(2,'REFUSED: '+str(error)+'\n')

if __name__=='__main__':main()
