#!/usr/bin/env python3
"""Restore the exact VelvetG r7 base JAR from two independently uploadable ZIP files.

Usage (from project directory):
    python RESTORE-BASE.py /path/to/VelvetG-r8-BASE-PART-1.zip /path/to/VelvetG-r8-BASE-PART-2.zip
Or extract the two ZIPs to the project directory and run without arguments:
    python RESTORE-BASE.py

Python 3 standard library only. Verifies both shards and final JAR via SHA-256.
"""
from pathlib import Path
import hashlib, json, os, sys, zipfile

HERE=Path(__file__).resolve().parent
MANIFEST=json.loads((HERE/'BASE-PARTS-MANIFEST.json').read_text(encoding='utf-8'))

def digest(blob): return hashlib.sha256(blob).hexdigest()

def read_part(index, external):
    spec=MANIFEST['parts'][index]
    name=spec['name']
    archive_name=f'VelvetG-r8-BASE-PART-{index+1}.zip'
    candidates=([Path(external)] if external else [
        HERE/'baseline-parts'/name, HERE/archive_name, HERE.parent/archive_name,
        Path.cwd()/archive_name, Path.cwd()/'baseline-parts'/name,
    ])
    for candidate in candidates:
        if not candidate.is_file():
            continue
        if candidate.suffix.lower()=='.zip':
            with zipfile.ZipFile(candidate) as archive:
                data=archive.read('baseline-parts/'+name)
        else:
            data=candidate.read_bytes()
        if len(data)!=spec['size'] or digest(data)!=spec['sha256']:
            raise SystemExit(f'SHA-256/size mismatch for {name} in {candidate}')
        print(f'Verified part {index+1}: {len(data):,} bytes from {candidate}')
        return data
    raise SystemExit(f'Missing {name}. Supply the two BASE-PART ZIP paths or extract both ZIPs here.')

def main():
    if len(sys.argv) not in (1,3):
        raise SystemExit('Usage: python RESTORE-BASE.py [BASE-PART-1.zip BASE-PART-2.zip]')
    given=sys.argv[1:] if len(sys.argv)==3 else [None,None]
    dest=HERE/'GlideClient-VelvetG-2.0.9.6-Violet-r7.jar'
    if dest.is_file():
        good=(dest.stat().st_size==MANIFEST['baseline_size']
              and digest(dest.read_bytes())==MANIFEST['baseline_sha256'])
        if good:
            print(f'Existing baseline is already correct: {dest}')
            return
        raise SystemExit(f'Wrong existing baseline at {dest}. Refusing to overwrite; inspect it first.')
    parts=[read_part(i,given[i]) for i in range(2)]
    data=b''.join(parts)
    if len(data)!=MANIFEST['baseline_size'] or digest(data)!=MANIFEST['baseline_sha256']:
        raise SystemExit('Reconstructed baseline JAR failed integrity verification.')
    # Validate container format before touching build inputs.
    import io
    if not zipfile.is_zipfile(io.BytesIO(data)):
        raise SystemExit('Reconstructed baseline is not a valid JAR/ZIP file.')
    temp=dest.with_suffix('.jar.tmp')
    try:
        with temp.open('wb') as f: f.write(data)
        os.replace(temp,dest)
    finally:
        temp.unlink(missing_ok=True)
    print(f'Success: restored {dest.name} ({len(data):,} bytes)')
    print('Now run BUILD.bat on Windows or ./BUILD.sh on Linux/macOS.')

if __name__=='__main__':main()
