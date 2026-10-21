#!/usr/bin/env python3
"""Reassemble ordered private artifact parts, verify SHA-256, then publish without overwrite."""
import argparse
import hashlib
import os
from pathlib import Path
import tempfile


def restore(parts, output, expected_sha256):
    if len(expected_sha256)!=64 or any(c not in '0123456789abcdef' for c in expected_sha256):
        raise ValueError('Exact expected SHA-256 required')
    if output.exists(): raise FileExistsError(output)
    resolved=[p.resolve(strict=True) for p in parts]
    if not resolved or len(set(resolved))!=len(resolved):raise ValueError('Require unique ordered parts')
    output.parent.mkdir(parents=True,exist_ok=True)
    hasher=hashlib.sha256()
    fd,name=tempfile.mkstemp(prefix='.combatiq-restore-',dir=output.parent)
    try:
        with os.fdopen(fd,'wb') as dest:
            for part in resolved:
                with part.open('rb') as source:
                    for block in iter(lambda:source.read(1024*1024),b''):
                        dest.write(block);hasher.update(block)
            dest.flush();os.fsync(dest.fileno())
        if hasher.hexdigest()!=expected_sha256:raise ValueError('Reconstructed SHA-256 mismatch; nothing published')
        os.link(name,output)
        return dict(sha256=hasher.hexdigest(),bytes=Path(name).stat().st_size)
    finally:Path(name).unlink(missing_ok=True)


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--out',type=Path,required=True)
    p.add_argument('--sha256',required=True);p.add_argument('parts',type=Path,nargs='+')
    a=p.parse_args();print(restore(a.parts,a.out,a.sha256))

if __name__=='__main__':main()
