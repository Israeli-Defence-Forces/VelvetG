#!/usr/bin/env python3
"""Cross-platform r8 builder. Uses pinned 1.8.9 metadata and verifies every downloaded dependency."""
from __future__ import annotations
import hashlib, json, os, shutil, subprocess, sys, urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PACKAGE = ROOT.parent
WORK = ROOT / "work"
DEPS = WORK / "deps"
CLASSES = WORK / "classes"
TOOLS = WORK / "tools"
CHECKS = WORK / "checks"
OUT = ROOT / "built"


def props(path: Path):
    out = {}
    for raw in path.read_text(encoding="utf-8-sig").splitlines():
        raw = raw.strip()
        if not raw or raw.startswith("#") or "=" not in raw:
            continue
        k, v = raw.split("=", 1)
        out[k.strip()] = v.strip()
    return out


def run(cmd, *, cwd=None):
    print("+", " ".join(map(str, cmd)))
    subprocess.run([str(x) for x in cmd], cwd=cwd, check=True)


def sha(path: Path, algo="sha1"):
    h = hashlib.new(algo)
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def fetch(url: str, path: Path, expected_hash: str = "", algo: str = "sha1"):
    path.parent.mkdir(parents=True, exist_ok=True)
    expected_hash = expected_hash.lower().strip()
    if path.exists():
        if not expected_hash or sha(path, algo) == expected_hash:
            return
        path.unlink()
    tmp = path.with_suffix(path.suffix + ".download")
    tmp.unlink(missing_ok=True)
    req = urllib.request.Request(url, headers={"User-Agent": "VelvetG-r8-builder"})
    try:
        with urllib.request.urlopen(req, timeout=30) as src, tmp.open("wb") as dst:
            shutil.copyfileobj(src, dst)
        if expected_hash and sha(tmp, algo) != expected_hash:
            raise RuntimeError(f"{algo.upper()} mismatch for {path.name}")
        os.replace(tmp, path)
    finally:
        tmp.unlink(missing_ok=True)



def java8_flags(javac):
    """Use JDK 8 syntax on javac 8; use --release 8 when the compiler supports it."""
    probe = subprocess.run([str(javac), "-version"], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, check=True)
    version = probe.stdout.strip()
    if version.startswith("javac 1.8"):
        return ["-source", "1.8", "-target", "1.8"]
    return ["--release", "8"]

def clean_dir(path: Path):
    if path.exists():
        shutil.rmtree(path)
    path.mkdir(parents=True)


def main():
    info = props(PACKAGE / "RELEASE.properties")
    client_name = info.get("jar")
    baseline_name = info.get("baselineJar")
    label = info.get("label", "r8")
    if not client_name or not baseline_name:
        raise SystemExit("RELEASE.properties is incomplete")

    javac = shutil.which("javac")
    java = shutil.which("java")
    jar = shutil.which("jar")
    if not all((javac, java, jar)):
        raise SystemExit("JDK tools javac/java/jar are required (JDK 8 or newer)")

    WORK.mkdir(parents=True, exist_ok=True)
    DEPS.mkdir(parents=True, exist_ok=True)  # dependency cache survives clean builds
    for d in (TOOLS, CLASSES, CHECKS, OUT):
        clean_dir(d)

    bundled = PACKAGE / baseline_name
    base_arg = sys.argv[1] if len(sys.argv) > 1 else ""
    if base_arg:
        base = Path(base_arg).resolve()
        expected = (ROOT / "BASE-SHA256.txt").read_text().strip().lower()
        if not base.exists() or sha(base, "sha256") != expected:
            raise SystemExit("Use the exact original GameplayFixes-PerfPatch base JAR")
    else:
        if not bundled.exists():
            raise SystemExit(f"Keep source-patch beside {baseline_name}")
        run([javac, "-proc:none", "-d", TOOLS, ROOT / "RestoreBase.java"])
        base = WORK / "restored-base.jar"
        run([java, "-cp", TOOLS, "RestoreBase", bundled, ROOT / "base-overrides.zip", ROOT / "base-index.sha256", base])

    metadata_path = ROOT / "minecraft-1.8.9.json"
    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
    if metadata.get("id") != "1.8.9":
        raise RuntimeError("Pinned metadata is not Minecraft 1.8.9")
    mc = WORK / "minecraft.jar"
    client = metadata["downloads"]["client"]
    fetch(client["url"], mc, client["sha1"], "sha1")

    dep_jars = []
    for lib in metadata.get("libraries", []):
        art = lib.get("downloads", {}).get("artifact")
        if not art:
            continue
        # Preserve Mojang's relative artifact path so duplicate basenames cannot collide.
        dst = DEPS / Path(art["path"])
        fetch(art["url"], dst, art.get("sha1", ""), "sha1")
        dep_jars.append(dst)

    extras = [
        ("https://libraries.minecraft.net/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar",
         DEPS / "net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar",
         "57f402b626d16cc2705bf2a37add7adbb074f0ca3b3102fa6e23aa303dae682f"),
        ("https://repo.maven.apache.org/maven2/org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar",
         DEPS / "org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar",
         "09d44e6adddaab9fd89ea3164c439f71c755769fa8d3a940352ab3d2003b98ff"),
    ]
    for url, dst, digest in extras:
        fetch(url, dst, digest, "sha256")
        dep_jars.append(dst)

    target8 = java8_flags(javac)
    tool_sources = [ROOT / x for x in ("BytecodeIO.java", "VioletPatch.java", "Remap.java", "UnsignedJar.java")]
    run([javac, "-encoding", "UTF-8", "-proc:none", "-cp", base, "-d", TOOLS, *tool_sources])
    tool_cp = os.pathsep.join((str(TOOLS), str(base)))
    mc_compile, base_compile, mc_test = WORK / "minecraft-compile.jar", WORK / "base-compile.jar", WORK / "minecraft-test.jar"
    run([java, "-cp", TOOLS, "UnsignedJar", mc, mc_test])
    run([java, "-cp", tool_cp, "Remap", "prefix", mc, mc_compile])
    run([java, "-cp", tool_cp, "Remap", "prefix", base, base_compile])
    dep_cp = os.pathsep.join(map(str, dep_jars))
    compile_cp = os.pathsep.join((str(base_compile), str(mc_compile), dep_cp))

    sources = sorted((ROOT / "src").rglob("*.java"))
    run([javac, "-encoding", "UTF-8", *target8, "-proc:none", "-cp", compile_cp, "-d", CLASSES, *sources])
    new_compile = WORK / "new-compile.jar"
    run([jar, "cfM", new_compile, "-C", CLASSES, "."])
    resources = ROOT / "resources"
    if resources.exists():
        run([jar, "uf", new_compile, "-C", resources, "."])
    new_runtime = WORK / "new-runtime.jar"
    run([java, "-cp", tool_cp, "Remap", "strip", new_compile, new_runtime])

    target = OUT / client_name
    patch_cp = os.pathsep.join((tool_cp, str(mc), dep_cp))
    run([java, "-cp", patch_cp, "VioletPatch", base, mc, new_runtime, target, label])

    assist_classes = WORK / "assist-checks"
    clean_dir(assist_classes)
    assist_src = ROOT / "tests" / "me" / "movi" / "violet" / "AssistChecks.java"
    run([javac, "-encoding", "UTF-8", *target8, "-proc:none", "-cp", os.pathsep.join((str(new_compile), compile_cp)), "-d", assist_classes, assist_src])
    assist_compile = WORK / "assist-checks-compile.jar"
    run([jar, "cfM", assist_compile, "-C", assist_classes, "."])
    assist_runtime = WORK / "assist-checks-runtime.jar"
    run([java, "-cp", tool_cp, "Remap", "strip", assist_compile, assist_runtime])

    test_sources = sorted((ROOT / "tests").glob("*.java"))
    test_cp = os.pathsep.join((str(target), str(mc_test), dep_cp))
    run([javac, "-encoding", "UTF-8", *target8, "-proc:none", "-cp", test_cp, "-d", CHECKS, ROOT / "VioletVerify.java", *test_sources])
    runtime_cp = os.pathsep.join((str(CHECKS), str(assist_runtime), str(target), str(mc_test), dep_cp))
    run([java, "-Xverify:all", "-cp", runtime_cp, "VioletVerify", target, base])
    run([java, "-Xverify:all", "-cp", runtime_cp, "EngineChecks"])
    run([java, "-Xverify:all", "-cp", runtime_cp, "ReliabilityChecks"])
    run([java, "-Xverify:all", "-cp", runtime_cp, "AwarenessChecks"])
    run([java, "-Xverify:all", "-cp", runtime_cp, "VisualChecks"])
    preview_dir = WORK / "previews"
    clean_dir(preview_dir)
    run([java, "-Xverify:all", "-cp", runtime_cp, "NativeChecks", preview_dir])
    run([java, "-Xverify:all", "-cp", runtime_cp, "me.movi.violet.AssistChecks"])

    for name in ("Install-VelvetG.bat", "Install-VelvetG.ps1", "Uninstall-VelvetG.ps1", "README-FIRST.txt", "RELEASE-NOTES.md", "VALIDATION.txt", "RELEASE.properties", "AUDIT-FIX-MATRIX.md", "CHANGELOG-INTEGRATION.md", "INTEGRATION-NOTES.md", "TESTING-REPORT.md", "AUDIT-INTEGRATION.md", "DEPENDENCY-NOTICES-INVENTORY.txt"):
        shutil.copy2(PACKAGE / name, OUT / name)
    shutil.copytree(preview_dir, OUT / "validation-previews", dirs_exist_ok=True)
    with (OUT / "VALIDATION.txt").open("a", encoding="utf-8") as f:
        f.write("\nLOCAL BUILD PIPELINE: PASS\nVioletVerify, EngineChecks, ReliabilityChecks, AwarenessChecks, VisualChecks, NativeChecks and AssistChecks passed while producing this built directory. A live Minecraft/OptiFine launch is still required.\n")
    lines = []
    for f in sorted(p for p in OUT.iterdir() if p.is_file() and p.name != "SHA256SUMS.txt"):
        lines.append(f"{sha(f, 'sha256')}  {f.name}")
    (OUT / "SHA256SUMS.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Built and offline-checked {target}. Live Minecraft/graphics testing is still required.")


if __name__ == "__main__":
    main()
