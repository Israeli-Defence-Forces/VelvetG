#!/usr/bin/env python3
"""Apply real Mixin transformations headlessly to pinned official Minecraft bytecode.
Requires Java 8 (LaunchWrapper 1.12 is not a Java 17/21 application). This is not a game launch.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile
from build_v4 import V3,NETTY_SHA256,NETTY_RELATIVE

ROOT=Path(__file__).resolve().parent
MINECRAFT='14f0d96d1a56fb4f5c3b2233d00699525893fe5ce3dcf181e7de59120595d298'
MIXINS=['CombatIQMinecraftMixin','CombatIQPlayerMixin','CombatIQHandlerMixin','CombatIQNetworkMixin','CombatIQQueuedWriteMixin','CombatIQModuleMixin']


def run(client,minecraft,libraries,java8,javac,abi_only=False):
    client_sha=hashlib.sha256(client.read_bytes()).hexdigest()
    if client_sha!=V3 and not abi_only:raise ValueError('Exact v3 baseline required unless explicitly --abi-only')
    if hashlib.sha256(minecraft.read_bytes()).hexdigest()!=MINECRAFT:raise ValueError('Unknown Minecraft binary')
    netty=libraries/NETTY_RELATIVE
    if hashlib.sha256(netty.read_bytes()).hexdigest()!=NETTY_SHA256:raise ValueError('Unknown Netty library')
    version=subprocess.check_output([str(java8),'-version'],stderr=subprocess.STDOUT,text=True)
    if 'version "1.8.' not in version:raise ValueError('Use Java 8 for LaunchWrapper/Mixin application')
    with tempfile.TemporaryDirectory() as d:
        d=Path(d);classes=d/'classes';classes.mkdir();library=d/'mixin-library.jar';api=d/'client-api.jar'
        # Extract only the existing open-source Mixin dependency, not proprietary client classes.
        with zipfile.ZipFile(client) as source,zipfile.ZipFile(library,'w') as dest:
            for name in source.namelist():
                if name.startswith(('org/spongepowered/','META-INF/services/org.spongepowered.')):dest.writestr(name,source.read(name))
        # Keep private API bytecode temporary and suppress baseline manifest discovery.
        with zipfile.ZipFile(client) as source,zipfile.ZipFile(api,'w') as dest:
            for name in source.namelist():
                if name.endswith('.class') and not name.startswith('org/spongepowered/'):dest.writestr(name,source.read(name))
        cp=os.pathsep.join(map(str,[classes,library,minecraft,api,*sorted(libraries.rglob('*.jar'))]))
        sources=list((ROOT/'src').rglob('*.java'))+list((ROOT/'native-module').rglob('*.java'))+list((ROOT/'integration-src').rglob('*.java'))
        compiler=subprocess.check_output([str(javac),'-version'],stderr=subprocess.STDOUT,text=True)
        flags=['-source','8','-target','8'] if compiler.startswith('javac 1.8') else ['--release','8']
        subprocess.run([str(javac),'-proc:none',*flags,'-cp',cp,'-d',str(classes),*map(str,sources)],check=True,capture_output=True,text=True)
        (classes/'mixins.combatiq.integration.json').write_text(json.dumps(dict(required=True,minVersion='0.7.10',
            package='me.eldodebug.soar.injection.mixin.mixins',compatibilityLevel='JAVA_8',mixins=['combatiq.'+m for m in MIXINS])))
        p=subprocess.run([str(java8),'-Xverify:all','-cp',cp,'tools.MixinApplicationCheck',str(minecraft),str(client)],capture_output=True,text=True,timeout=60)
        result=dict(client_sha256=client_sha,exact_v3_baseline=client_sha==V3,java=version.strip(),
                    transformation_passed=p.returncode==0,minecraft_launched=False,full_client_mixins_applied=False,
                    stdout=p.stdout,stderr=p.stderr,returncode=p.returncode)
        return result


def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ('client','minecraft','libraries','java8','javac','out'):p.add_argument('--'+name,type=Path,required=True)
    p.add_argument('--abi-only',action='store_true');a=p.parse_args()
    result=run(a.client,a.minecraft,a.libraries,a.java8,a.javac,a.abi_only)
    with a.out.open('x') as f:json.dump(result,f,indent=2)
    print(a.out)
    if not result['transformation_passed']:raise SystemExit(result['stdout']+result['stderr'])

if __name__=='__main__':main()
