#!/usr/bin/env python3
"""Verify and reconstruct collector JAR parts. Never installs or executes client code."""
from pathlib import Path
import argparse, hashlib, json, re, zipfile

LIMIT=24*1024*1024

def restore(directory,output):
    directory=Path(directory);output=Path(output)
    with zipfile.ZipFile(directory/'VelvetG-DIAGNOSTICS.zip') as z:
        if z.getinfo('collection.json').file_size>2*1024*1024:raise ValueError('Oversized diagnostics')
        manifest=json.loads(z.read('collection.json'))
    if manifest.get('schema')!=1:raise ValueError('Unsupported collection schema')
    payloads=manifest['payloads']
    if not isinstance(payloads,list) or not 1<=len(payloads)<=20:raise ValueError('Invalid payload count')
    output.mkdir(parents=True,exist_ok=True)
    paths=[]
    for payload in payloads:
        name=payload['id']
        if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9_-]{0,48}',name):raise ValueError('Invalid payload ID')
        target=output/(name+'.jar');temporary=output/(name+'.jar.collecting')
        if target.exists() or temporary.exists():raise ValueError('Output already exists: '+target.name)
        parts=payload['parts']
        if not isinstance(parts,list) or not 1<=len(parts)<=64:raise ValueError('Invalid part count')
        if not 0<payload['size']<=1024*1024*1024:raise ValueError('Invalid payload size')
        digest=hashlib.sha256();total=0
        try:
            with temporary.open('xb') as sink:
                for index,part in enumerate(parts,1):
                    filename=part['archive']
                    if not re.fullmatch(r'VelvetG-'+re.escape(name)+r'-PART-[0-9]{2}\.zip',filename):raise ValueError('Invalid archive filename')
                    if part['index']!=index or part['entry']!='payload.bin' or not 0<part['size']<=LIMIT:raise ValueError('Invalid part metadata')
                    archive=directory/filename
                    if archive.stat().st_size>=32*1024*1024:raise ValueError('Part archive exceeds upload limit')
                    with zipfile.ZipFile(archive) as z:
                        entry=z.getinfo('payload.bin')
                        if entry.file_size!=part['size']:raise ValueError('Part size mismatch: '+filename)
                        data=z.read(entry)
                    if hashlib.sha256(data).hexdigest()!=part['sha256']:raise ValueError('Part hash mismatch: '+filename)
                    total+=len(data);digest.update(data);sink.write(data)
            if total!=payload['size'] or digest.hexdigest()!=payload['sha256']:raise ValueError('Whole-JAR hash mismatch')
            with zipfile.ZipFile(temporary) as z:
                if z.testzip() is not None:raise ValueError('Corrupt client JAR')
            temporary.rename(target);paths.append(target)
        except BaseException:
            temporary.unlink(missing_ok=True)
            raise
    return paths

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory',type=Path,help='Folder containing diagnostics and all part ZIPs')
    parser.add_argument('--output',type=Path,required=True,help='New output directory for verified JARs')
    args=parser.parse_args()
    try:
        for path in restore(args.directory,args.output):print('SHA-256 and ZIP verified: '+path.name)
    except (ValueError,KeyError,OSError,zipfile.BadZipFile) as error:parser.exit(1,'Collection not restored: '+str(error)+'\n')
