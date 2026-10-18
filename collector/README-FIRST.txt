VELVETG LEAN - READ-ONLY COLLECTION REVISION 2 (not an installer)

1. Close Minecraft and the Minecraft launcher.
2. Extract this collector ZIP into a new folder. Do not run it from the ZIP viewer.
3. Double-click COLLECT-WINDOWS.bat. No administrator access, Python, Java installation or network is required.
4. The default game directory is D:\Roaming\.minecraft and the exact default client is GlideClient-2.0.9.6-lean-fixed-20261016.jar.
5. Upload VelvetG-DIAGNOSTICS.zip and EVERY VelvetG-*-PART-*.zip from the newly created VelvetG-Collection-* folder into the chat.
   Each JAR part archive is below 32 MiB. Upload all parts; they are not individually runnable.

Custom paths (PowerShell):
.\Collect-VelvetG.ps1 -MinecraftDir 'D:\A folder\.minecraft' -ClientJar 'D:\A folder\installed-client.jar'

What is collected:
- Exact client JAR bytes, split into 24 MiB payloads with per-part and whole-file SHA-256 checksums.
- A custom matching version JAR if present, distinct from the client, and required by its version.
- Sanitized matching launcher-version metadata and parent bootstrap/library identities and hashes.
- Only allowlisted numeric/boolean configuration controls and fixed enum values (including QUAD).
- Relevant exception class names and Java stack frames from recent crash/latest/thread-dump files.
- Screenshot count/total bytes only, backup-folder counts, and Java executable product-version metadata where available, crash-reported Java versions and allowlisted heap/GC flags.

What is excluded:
- Launcher profiles/accounts, access/session tokens, API key files, credentials, server lists, player histories/notes/watchlists, chat, personal paths and names, raw settings, raw log messages, resource packs and screenshot images.
- Arbitrary string settings, arbitrary log messages, launcher account arguments, URL credentials and URL query strings are never exported.
- JARs with obvious credential files or Pika-key-looking embedded bytes are rejected instead of altered. If collection fails, do not upload its partial output.

No installed file is patched, reset, deleted or replaced. Every screenshot is preserved; images are not decoded. A damaged or absent screenshot folder does not stop collection. There is no network/upload code; you choose the archives to upload privately. Do not publish your collected client binary in a public repository. Existing dependency licences have not yet been established for this installed build.

Verification performed here: 57 assertions using actual PowerShell 7.4.13 on Linux with synthetic game directories. These cover exact file preservation, paths with spaces, secret/personal-data canaries, split/restore hashes, corrupt settings, empty/250-corrupt-image galleries, missing installation, tampered parts and embedded-key rejection. Windows PowerShell 5.1 execution remains untested; the script targets its syntax and .NET ZIP APIs.

RESTORE-COLLECTION.py is for verifying/reassembling the uploaded collection on the analysis machine; you do not need Python to collect files on Windows. It never installs or runs the client.

REVISION 2 PATH FIX: The output directory is resolved inside the script body rather than in its parameter default. The no-argument batch wrapper explicitly supplies the collector directory. Default, empty, whitespace-only and trailing-dot output values were tested from a separate working directory. Use this package in a new extracted folder after a GetFullPath startup error.
