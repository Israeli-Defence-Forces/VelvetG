#!/usr/bin/env python3
"""Compile/test the standalone kernel on JDK 8+; no game JAR or downloaded dependency."""
import argparse
import hashlib
import json
import os
import pathlib
import re
import shutil
import struct
import subprocess
import sys
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent

def executable(name):
    suffix = ".exe" if os.name == "nt" else ""
    if os.environ.get("JAVA_HOME"):
        candidate = pathlib.Path(os.environ["JAVA_HOME"]) / "bin" / (name + suffix)
        if candidate.is_file():
            return str(candidate)
    found = shutil.which(name)
    if not found:
        raise SystemExit("Missing " + name + "; install a JDK (a JRE cannot compile).")
    return found

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--compile-only", action="store_true")
    args = parser.parse_args()
    build = ROOT / "build"
    classes = build / "classes"
    if classes.exists():
        shutil.rmtree(classes)
    classes.mkdir(parents=True)
    javac, java = executable("javac"), executable("java")
    version = subprocess.run([javac, "-version"], check=True, stdout=subprocess.PIPE,
                             stderr=subprocess.STDOUT, universal_newlines=True).stdout.strip()
    match = re.search(r"javac (?:1\.)?(\d+)", version)
    if not match:
        raise SystemExit("Unrecognized compiler version: " + version)
    major = int(match.group(1))
    target = ["-source", "8", "-target", "8"] if major == 8 else ["--release", "8"]
    sources = sorted((ROOT / "src" / "main" / "java").rglob("*.java"))
    tests = sorted((ROOT / "src" / "test" / "java").rglob("*.java"))
    subprocess.run([javac] + target + ["-encoding", "UTF-8", "-Xlint:all", "-d", str(classes)]
                   + [str(p) for p in sources + tests], check=True)
    for path in classes.rglob("*.class"):
        if struct.unpack(">H", path.read_bytes()[6:8])[0] != 52:
            raise SystemExit("Non-Java-8 bytecode: " + str(path))
    result = {"compiler": version, "class_major": 52, "game_integration_tested": False,
              "performance_measured": False, "tests_executed": False}
    if not args.compile_only:
        run = subprocess.run([java, "-Xverify:all", "-cp", str(classes),
                              "me.movi.combatiq.CombatIQTest"], check=True,
                             stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                             universal_newlines=True)
        print(run.stdout, end="")
        (build / "test-results.txt").write_text(run.stdout, encoding="utf-8")
        subprocess.run([sys.executable, str(ROOT / "experiments" / "protocol.py"),
                        "--self-test"], check=True)
        result["protocol_tests_executed"] = True
        result["tests_executed"] = True
        result["test_output"] = run.stdout
    artifact = build / "combatiq-kernel.jar"
    with zipfile.ZipFile(str(artifact), "w", zipfile.ZIP_DEFLATED) as jar:
        manifest = zipfile.ZipInfo("META-INF/MANIFEST.MF", (1980, 1, 1, 0, 0, 0))
        jar.writestr(manifest, "Manifest-Version: 1.0\nMain-Class: me.movi.combatiq.CombatIQ\n\n")
        for path in sorted(classes.rglob("*.class")):
            if path.name.startswith("CombatIQTest"):
                continue
            entry = zipfile.ZipInfo(path.relative_to(classes).as_posix(), (1980, 1, 1, 0, 0, 0))
            jar.writestr(entry, path.read_bytes())
    result["artifact_sha256"] = hashlib.sha256(artifact.read_bytes()).hexdigest()
    (build / "validation.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print("Artifact:", artifact)
    print("This is a telemetry kernel/replay tool, not an installable Minecraft mod.")

if __name__ == "__main__":
    main()
