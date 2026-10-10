# VelvetG r8 / Violet: Codex transport instructions

This archive is a **source-and-patch development package**, not a self-contained
fully editable version of the historical GlideClient base. Its implementation
is in `source-patch/src/`, and the existing r8 build integrates that source
into an exact, checked historical r7 JAR. The build cannot work without that
binary foundation. Do not replace it with a random client JAR.

## Upload all three independent files to Codex (<32 MiB each)

- `VelvetG-r8-Codex-SOURCE-ONLY.zip`
- `VelvetG-r8-BASE-PART-1.zip`
- `VelvetG-r8-BASE-PART-2.zip`

1. Extract this source ZIP (preserve its root project folder).
2. From inside that folder, run:

   `python RESTORE-BASE.py /path/to/VelvetG-r8-BASE-PART-1.zip /path/to/VelvetG-r8-BASE-PART-2.zip`

   Alternatively, extract both part ZIPs into that project folder, creating
   `baseline-parts/`, and run `python RESTORE-BASE.py`.
3. This checks each binary piece plus the final JAR against the included
   `BASE-PARTS-MANIFEST.json` before writing the exact original baseline JAR.
4. Only after that run `BUILD.bat` on Windows, or `./BUILD.sh` on Linux/macOS.
   Both root build entry points have been corrected in this transport package
   to invoke their existing real builder under `source-patch/`.
5. The Java build uses pinned Minecraft 1.8.9 dependencies, requiring
   a JDK and access to the artifact URLs. A successful offline build does
   not imply a real in-game launch has been tested.

## Notes

- Generated files/caches (`build/`, `.gradle/`, `__pycache__/`, etc.) excluded.
- Preview screenshots and documentation are included for reference.
- The two BASE-PART archives are **binary data, not optional source files**;
  the exact JAR is required by `source-patch/RestoreBase.java`.
- This project does not contain a Gradle wrapper; it has a bespoke Python/
  PowerShell/Java build pipeline. Do not fabricate one while preparing input.
- If modifying VelvetG's inherited client beyond the Violet overlay,
  note that this ZIP lacks full original GlideClient source. The Java base
  comes from historical compiled bytecode.
