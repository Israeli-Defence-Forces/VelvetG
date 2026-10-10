#!/usr/bin/env python3
"""Run real dependency-independent Java 8 checks; does not validate Minecraft integration."""
import importlib.util
import os
import shutil
from pathlib import Path

root = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("velvet_builder", root / "BUILD.py")
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)
javac, java = shutil.which("javac"), shutil.which("java")
if not javac or not java:
    raise SystemExit("JDK 8 or newer is required; ensure javac and java are on PATH")
output = root / "work" / "reliability-checks"
builder.clean_dir(output)
sources = [root / "src" / "me" / "movi" / "violet" / (name + ".java")
           for name in ("VioletEngine", "VioletDiagnostics", "SoupConfirmation", "BlockFilter")]
sources += [root / "tests" / (name + ".java") for name in ("EngineChecks", "ReliabilityChecks")]
builder.run([javac, "-encoding", "UTF-8", *builder.java8_flags(javac), "-d", output, *sources])
for target in ("EngineChecks", "ReliabilityChecks"):
    builder.run([java, "-Xverify:all", "-cp", output, target])
print("Dependency-independent checks passed. Full BUILD and live game validation are separate.")
