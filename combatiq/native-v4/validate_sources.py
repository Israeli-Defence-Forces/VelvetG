#!/usr/bin/env python3
"""Compile native sources and optional HUD against an explicit local client API.
This creates no patched client and does not prove Mixin application or GUI behavior.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import struct
import subprocess
import tempfile
from build_v4 import V3

ROOT=Path(__file__).resolve().parent


def validate(client,minecraft,abi_only=False):
    client_hash=hashlib.sha256(client.read_bytes()).hexdigest()
    if client_hash!=V3 and not abi_only:
        raise ValueError('Exact v3 baseline required; --abi-only permits an explicit alternate API compile check')
    javac=shutil.which('javac');java=shutil.which('java')
    if not java or not javac: raise RuntimeError('JDK required')
    version=subprocess.check_output([javac,'-version'],stderr=subprocess.STDOUT,text=True).strip()
    flags=['-source','8','-target','8'] if version.startswith('javac 1.8') else ['--release','8']
    with tempfile.TemporaryDirectory() as d:
        sources=list((ROOT/'src').rglob('*.java'))+list((ROOT/'native-module').rglob('*.java'))
        subprocess.run([javac,'-proc:none',*flags,'-cp',str(client),'-d',d,*map(str,sources)],check=True)
        target=subprocess.check_output([java,'-Xverify:all','-cp',d+os.pathsep+str(client),
                                       'tools.VerifyBindings',str(minecraft)],text=True)
        classes=list(Path(d).rglob('*.class'))
        if any(struct.unpack('>H',p.read_bytes()[6:8])[0]!=52 for p in classes):
            raise AssertionError('Non-Java8 class')
        return dict(compiler=version,client_sha256=client_hash,
                    baseline_identity_verified=client_hash==V3,abi_only=abi_only,
                    java_class_major=52,compiled_classes=len(classes),structural_verification=target.strip(),
                    minecraft_launched=False,mixin_applied=False,gui_persistence_tested=False)


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--client',type=Path,required=True);p.add_argument('--minecraft',type=Path,required=True)
    p.add_argument('--abi-only',action='store_true');p.add_argument('--out',type=Path,required=True)
    a=p.parse_args();r=validate(a.client,a.minecraft,a.abi_only)
    with a.out.open('x') as f:json.dump(r,f,indent=2)
    print(a.out)

if __name__=='__main__':main()
