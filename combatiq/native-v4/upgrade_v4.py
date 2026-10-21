#!/usr/bin/env python3
"""Fail-closed upgrade of ONLY an already-installed exact CombatIQ v3 profile to v4.
Requires an unchanged pinned historical baseline, exact v3 experimental install, and exact v4 JAR.
Never modifies another Minecraft version.
"""
import argparse
import hashlib
import json
import os
import shutil
import sys
from pathlib import Path

BASE_SHA = '3e4b0190e26b4a1540ac23ff018a8c8280b8ab447c34cce0fc5bf8cc6c2a6813'
PREVIOUS_SHA = '7949d73d9be9eb74b662b2844faee2939c0463222c01b4a89378f19ddaa8bbc7'
CURRENT_SHA = '696bce5471d2db5ad0b0be567204efc9e65ebeb075688b10ac8555fc69689455'
VER = '2.0.9.6-CombatIQ-Experimental'
ID = 'VelvetG-CombatIQ-Experimental'
ARTIFACT_PATH = 'me/eldodebug/GlideClient/' + VER + '/GlideClient-' + VER + '.jar'


def digest(path, alg='sha256'):
    hasher = hashlib.new(alg)
    with path.open('rb') as inp:
        for chunk in iter(lambda: inp.read(1024 * 1024), b''):
            hasher.update(chunk)
    return hasher.hexdigest()


def paths(game):
    root = game / 'libraries' / 'me' / 'eldodebug' / 'GlideClient'
    original = root / '2.0.9.6-lean-fixed-20261016' / 'GlideClient-2.0.9.6-lean-fixed-20261016.jar'
    lib = root / VER / ('GlideClient-' + VER + '.jar')
    version = game / 'versions' / ID / (ID + '.json')
    return original, lib, version


def validate(game, patched, rollback=False):
    original, lib, version = paths(game)
    if not original.is_file() or digest(original) != BASE_SHA:
        raise ValueError('Original historical baseline missing or hash mismatch. No changes made.')
    if not lib.is_file() or not version.is_file():
        raise ValueError('CombatIQ experimental installation missing. No changes made.')
    existing = digest(lib)
    if existing not in (PREVIOUS_SHA, CURRENT_SHA):
        raise ValueError('Experimental JAR is neither known v3 nor v4; refusing overwrite.')
    doc = json.loads(version.read_text(encoding='utf-8-sig'))
    if doc.get('id') != ID:
        raise ValueError('Unexpected launcher version identifier.')
    match = [x for x in doc.get('libraries', [])
             if isinstance(x, dict) and x.get('name') == 'me.eldodebug:GlideClient:' + VER]
    if len(match) != 1:
        raise ValueError('Ambiguous or missing CombatIQ library in launcher version JSON.')
    artifact = match[0].get('downloads', {}).get('artifact', {})
    if artifact.get('path') != ARTIFACT_PATH or artifact.get('sha1') != digest(lib, 'sha1') or artifact.get('size') != lib.stat().st_size:
        raise ValueError('Launcher metadata does not match installed experimental JAR.')
    if not rollback and (not patched.is_file() or digest(patched) != CURRENT_SHA):
        raise ValueError('Expected exact v4 JAR but supplied file is missing or hash mismatch.')
    return lib, version, doc, artifact, existing


def upgrade(game, patched, check):
    lib, version, doc, artifact, existing = validate(game, patched)
    if existing == CURRENT_SHA:
        print('PASS: v4 is already installed and launcher metadata verified.')
        return
    lib_bak = lib.with_name(lib.name + '.combatiq-v3-backup')
    version_bak = version.with_name(version.name + '.combatiq-v3-backup')
    staged_lib = lib.with_name(lib.name + '.combatiq-v4-staged')
    staged_ver = version.with_name(version.name + '.combatiq-v4-staged')
    if any(x.exists() for x in (lib_bak, version_bak, staged_lib, staged_ver)):
        raise FileExistsError('v4 upgrade/backup files already present: inspect before retrying')
    if check:
        print('PASS: exact v3 installation, launcher metadata and v4 JAR verified; no changes made.')
        return
    original_json = version.read_bytes()
    artifact['sha1'] = digest(patched, 'sha1')
    artifact['size'] = patched.stat().st_size
    new_json = (json.dumps(doc, indent=2, ensure_ascii=False) + '\n').encode('utf-8')
    try:
        shutil.copy2(lib, lib_bak)
        version_bak.write_bytes(original_json)
        shutil.copy2(patched, staged_lib)
        staged_ver.write_bytes(new_json)
        if digest(staged_lib) != CURRENT_SHA:
            raise ValueError('Staged JAR verification failed')
        os.replace(staged_lib, lib)
        os.replace(staged_ver, version)
        validate(game, patched)
    except Exception:
        if lib_bak.is_file() and digest(lib_bak) == PREVIOUS_SHA:
            os.replace(lib_bak, lib)
        if version_bak.is_file():
            os.replace(version_bak, version)
        raise
    finally:
        staged_lib.unlink(missing_ok=True)
        staged_ver.unlink(missing_ok=True)
    print('PASS: CombatIQ v3 -> v4 upgraded, launcher metadata updated. Original VelvetG untouched.')
    print('Backup copies:', lib_bak, version_bak)


def rollback(game, check):
    # For rollback, verify installed v4 metadata, no need for the v4 source package.
    lib, version, doc, artifact, existing = validate(game, Path('__unused__'), rollback=True)
    if existing != CURRENT_SHA:
        raise ValueError('Rollback expects exact v4 installed.')
    lib_bak = lib.with_name(lib.name + '.combatiq-v3-backup')
    version_bak = version.with_name(version.name + '.combatiq-v3-backup')
    if not lib_bak.is_file() or digest(lib_bak) != PREVIOUS_SHA or not version_bak.is_file():
        raise ValueError('Verified v3 backups not found.')
    backup = json.loads(version_bak.read_text(encoding='utf-8-sig'))
    m = [x for x in backup.get('libraries', []) if isinstance(x, dict) and x.get('name') == 'me.eldodebug:GlideClient:' + VER]
    if (len(m) != 1 or backup.get('id') != ID or
        m[0].get('downloads', {}).get('artifact', {}).get('sha1') != digest(lib_bak, 'sha1') or
        m[0].get('downloads', {}).get('artifact', {}).get('path') != ARTIFACT_PATH or
        m[0].get('downloads', {}).get('artifact', {}).get('size') != lib_bak.stat().st_size):
        raise ValueError('v3 backup launcher metadata invalid')
    if check:
        print('PASS: v3 rollback backups valid; no files modified.')
        return
    os.replace(lib_bak, lib)
    os.replace(version_bak, version)
    print('PASS: rolled experimental profile back to v3; original VelvetG untouched.')


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--game-dir', type=Path, required=True)
    p.add_argument('--patched-jar', type=Path, default=Path(__file__).parent / 'GlideClient-2.0.9.6-CombatIQ-v4.jar')
    p.add_argument('--check', action='store_true')
    p.add_argument('--rollback', action='store_true')
    a = p.parse_args()
    try:
        if a.rollback: rollback(a.game_dir, a.check)
        else: upgrade(a.game_dir, a.patched_jar, a.check)
    except (OSError, ValueError, KeyError, TypeError) as ex:
        print('REFUSED:', ex, file=sys.stderr)
        sys.exit(2)

if __name__ == '__main__':
    main()
