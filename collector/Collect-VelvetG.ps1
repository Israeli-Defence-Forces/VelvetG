#Requires -Version 5.1
[CmdletBinding()]
param(
    [string]$MinecraftDir = 'D:\Roaming\.minecraft',
    [string]$ClientJar = '',
    [string]$OutputParent = $PSScriptRoot
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root = [IO.Path]::GetFullPath($MinecraftDir)
if (-not (Test-Path -LiteralPath $root -PathType Container)) { throw 'Minecraft directory not found. Supply -MinecraftDir with the correct directory.' }
if (-not $ClientJar) { $ClientJar = Join-Path $root 'libraries/me/eldodebug/GlideClient/2.0.9.6-lean-fixed-20261016/GlideClient-2.0.9.6-lean-fixed-20261016.jar' }
$ClientJar = [IO.Path]::GetFullPath($ClientJar)
if (-not (Test-Path -LiteralPath $ClientJar -PathType Leaf)) { throw 'The specified Lean client JAR was not found. Supply -ClientJar with the installed client JAR. No older client was substituted.' }
$OutputParent=[IO.Path]::GetFullPath($OutputParent)
$prefix=$root.TrimEnd([IO.Path]::DirectorySeparatorChar)+[IO.Path]::DirectorySeparatorChar
if ($OutputParent.Equals($root,[StringComparison]::OrdinalIgnoreCase) -or $OutputParent.StartsWith($prefix,[StringComparison]::OrdinalIgnoreCase)) { throw 'Choose an output directory outside the Minecraft installation.' }
$out = Join-Path $OutputParent ('VelvetG-Collection-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,6))
[void][IO.Directory]::CreateDirectory($out)
$script:utf8 = New-Object Text.UTF8Encoding($false)
$script:notes = New-Object 'System.Collections.Generic.List[string]'
$script:payloads = New-Object 'System.Collections.Generic.List[object]'
$script:versions = New-Object 'System.Collections.Generic.List[object]'
$script:dependencies = New-Object 'System.Collections.Generic.List[object]'
$script:controls = New-Object 'System.Collections.Generic.List[object]'
$script:frames = New-Object 'System.Collections.Generic.List[string]'
$script:runtimeVersions = New-Object 'System.Collections.Generic.List[string]'
$script:jvmFlags = New-Object 'System.Collections.Generic.List[string]'
$script:seenVersions = @{}
$script:seenDependencies = @{}
$secretName = '(?i)(apikey|api[_-]?key|token|credential|account|session|secret|password|auth|friend|watchlist|history|notes|usercache|servers)'
function Hash-File([string]$Path) { return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant() }
function Add-TextEntry($Zip,[string]$Name,[string]$Value) {
    $entry = $Zip.CreateEntry($Name, [IO.Compression.CompressionLevel]::Optimal)
    $stream = $entry.Open()
    try { $bytes = $script:utf8.GetBytes($Value); $stream.Write($bytes,0,$bytes.Length) } finally { $stream.Dispose() }
}
function Zip-Open([string]$Path) {
    $stream = [IO.File]::Open($Path,[IO.FileMode]::CreateNew,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
    try { return New-Object IO.Compression.ZipArchive($stream,[IO.Compression.ZipArchiveMode]::Create,$false) }
    catch { $stream.Dispose(); throw }
}
function Add-Payload([string]$Path,[string]$Id) {
    # Binary is exact and private; refuse obvious bundled credential files rather than rewriting it.
    $check = [IO.Compression.ZipFile]::OpenRead($Path)
    try {
        foreach ($entry in $check.Entries) {
            if ($entry.FullName -match '(?i)(^|/)(launcher_accounts|accounts|apikey|api[_-]?key|credentials|secrets|tokens)\.(json|txt|key|properties)$') {
                throw 'Client JAR contains a possible bundled credential file. Binary collection stopped; do not upload this JAR before it is reviewed locally.'
            }
            # Check decompressed bytes without decoding images or changing the binary.
            $entryStream=$entry.Open()
            try {
                $scan=New-Object byte[] 65536; $tail=''
                while (($read=$entryStream.Read($scan,0,$scan.Length)) -gt 0) {
                    $text=$tail+[Text.Encoding]::ASCII.GetString($scan,0,$read)
                    if ([regex]::IsMatch($text,'pika_[A-Za-z0-9_-]{16,128}')) { throw 'A possible embedded Pika API key was detected. Do not upload this JAR; remove the embedded credential locally before collection.' }
                    $tail=$text.Substring([Math]::Max(0,$text.Length-160))
                }
            } finally { $entryStream.Dispose() }
        }
    } finally { $check.Dispose() }
    $before = Hash-File $Path
    $payloadStream = [IO.File]::Open($Path,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::Read)
    $parts = New-Object 'System.Collections.Generic.List[object]'
    try {
        $buffer = New-Object byte[] (24 * 1024 * 1024)
        $index = 0
        while ($payloadStream.Position -lt $payloadStream.Length) {
            $used = 0
            while ($used -lt $buffer.Length) {
                $n = $payloadStream.Read($buffer,$used,$buffer.Length-$used)
                if ($n -eq 0) { break }; $used += $n
            }
            $index++; $name = 'VelvetG-' + $Id + '-PART-' + $index.ToString('00') + '.zip'
            $zip = Zip-Open (Join-Path $out $name)
            try {
                $entry = $zip.CreateEntry('payload.bin',[IO.Compression.CompressionLevel]::NoCompression)
                $stream = $entry.Open()
                try { $stream.Write($buffer,0,$used) } finally { $stream.Dispose() }
                $algorithm = [Security.Cryptography.SHA256]::Create()
                try { $hash = [BitConverter]::ToString($algorithm.ComputeHash($buffer,0,$used)).Replace('-','').ToLowerInvariant() } finally { $algorithm.Dispose() }
                $part = [ordered]@{ archive=$name; entry='payload.bin'; index=$index; size=$used; sha256=$hash }
                Add-TextEntry $zip 'part.json' ($part | ConvertTo-Json -Depth 5)
                [void]$parts.Add($part)
            } finally { $zip.Dispose() }
        }
        $length = $payloadStream.Length
    } finally { $payloadStream.Dispose() }
    if ((Hash-File $Path) -ne $before) { throw 'The client JAR changed while collecting. Close Minecraft and retry. Do not upload the incomplete collection.' }
    [void]$script:payloads.Add([ordered]@{ id=$Id; size=$length; sha256=$before; parts=@($parts.ToArray()) })
}
function Add-Library($Library) {
    $name = [string]$Library.name
    if ($name -notmatch '^[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+:[A-Za-z0-9_.+-]+(?::[A-Za-z0-9_.-]+)?$' -or $script:seenDependencies.ContainsKey($name)) { return }
    $script:seenDependencies[$name] = $true
    $item = [ordered]@{ coordinate=$name }
    $bits = $name.Split(':'); $relative = $bits[0].Replace('.','/')+'/'+$bits[1]+'/'+$bits[2]+'/'+$bits[1]+'-'+$bits[2]
    if ($bits.Length -gt 3) { $relative += '-'+$bits[3] }; $relative += '.jar'
    $path = Join-Path (Join-Path $root 'libraries') $relative
    $item.present = Test-Path -LiteralPath $path -PathType Leaf
    if ($item.present) { $item.size=(Get-Item -LiteralPath $path).Length; $item.sha256=Hash-File $path }
    if ($Library.downloads -and $Library.downloads.artifact) {
        $a = $Library.downloads.artifact
        if ([string]$a.sha1 -match '^[a-fA-F0-9]{40}$') { $item.declaredSha1=[string]$a.sha1 }
        if ($a.size -is [ValueType]) { $item.declaredSize=$a.size }
    }
    if ($Library.natives) { $item.hasNatives=$true }
    [void]$script:dependencies.Add($item)
}
function Add-Version([string]$Path,[string]$Role,[int]$Depth=0) {
    if ($Depth -gt 4 -or $script:seenVersions.ContainsKey($Path)) { return }
    $script:seenVersions[$Path]=$true
    try { $data = Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json } catch { [void]$script:notes.Add('A launcher version JSON could not be parsed.'); return }
    $item = [ordered]@{ alias=('version-'+($script:versions.Count+1)); role=$Role; sha256=(Hash-File $Path) }
    if ([string]$data.mainClass -match '^[A-Za-z_$][A-Za-z0-9_.$]+$') { $item.mainClass=[string]$data.mainClass }
    if ($data.javaVersion -and $data.javaVersion.majorVersion -is [ValueType]) { $item.javaMajor=$data.javaVersion.majorVersion }
    $item.tweakClasses=@([regex]::Matches([string]$data.minecraftArguments,'--tweakClass\s+([A-Za-z_$][A-Za-z0-9_.$]+)') | ForEach-Object { $_.Groups[1].Value })
    $item.libraries=@($data.libraries | ForEach-Object { Add-Library $_; if ([string]$_.name -match '^[A-Za-z0-9_.:-]+$') { [string]$_.name } })
    [void]$script:versions.Add($item)
    $parent = [string]$data.inheritsFrom
    if ($parent -and $parent -match '^[A-Za-z0-9_.-]+$') {
        Add-Version (Join-Path (Join-Path (Join-Path $root 'versions') $parent) ($parent+'.json')) 'parent' ($Depth+1)
    }
    $versionJar = [IO.Path]::ChangeExtension($Path,'.jar')
    if (Test-Path -LiteralPath $versionJar -PathType Leaf) {
        $item.versionJarSize=(Get-Item -LiteralPath $versionJar).Length; $item.versionJarSha256=Hash-File $versionJar
        # Standard vanilla JARs are discoverable by hash; collect only custom version JARs.
        if ($Role -eq 'matching' -and $item.versionJarSha256 -ne $script:clientHash) { Add-Payload $versionJar ($item.alias+'-jar') }
    }
}
function Read-Controls($Node,[int]$Depth=0) {
    if ($null -eq $Node -or $Depth -gt 12 -or $script:controls.Count -ge 500) { return }
    if ($Node -is [Array]) { foreach ($v in $Node) { Read-Controls $v ($Depth+1) }; return }
    if ($Node -isnot [System.Management.Automation.PSCustomObject]) { return }
    foreach ($p in $Node.PSObject.Properties) {
        if ($p.Name -match $secretName) { continue }
        $v=$p.Value
        if ($p.Name -match '^(enabled|toggled|x|y|scale|width|height|opacity|alpha|backgroundOpacity|animationDuration|animations|lowPerformance|lowPerformanceMode|playerLimit|maxPlayers|showHeads|showHeader|showHud|showTab|sort|mode|period|fontSize|columnSpacing|autoApiKey|automaticApiKey|hideHudWithTab|renderDistance|maxFps|particles|useVbo|enableVsync|mipmapLevels|fboEnable)$') {
            if ($v -is [bool] -or $v -is [int] -or $v -is [long] -or $v -is [double] -or $v -is [decimal]) { [void]$script:controls.Add([ordered]@{control=$p.Name;value=$v}) }
            elseif ($v -is [string] -and $v -cmatch '^(ALL_MODES|SOLO|DOUBLES|QUAD|QUADS|LIFETIME|WEEKLY|MONTHLY|YEARLY|FKDR|WLR|WINS|FINAL_KILLS|BEDS|LEVEL|WINSTREAK|None|Slide|Zoom|Bounce|Pop)$') { [void]$script:controls.Add([ordered]@{control=$p.Name;value=$v}) }
        }
        Read-Controls $v ($Depth+1)
    }
}
function Read-Frames([string]$Path) {
    try {
        foreach ($line in (Get-Content -LiteralPath $Path -Tail 6000)) {
            if ($script:frames.Count -ge 1000) { break }
            if ($line -match '^\s*Java Version:\s*([0-9][0-9._+-]*)') { [void]$script:runtimeVersions.Add($Matches[1]) }
            if ($line -match '^\s*JVM Flags:') {
                foreach ($m in [regex]::Matches($line,'(?<!\S)(-Xm[sxn][0-9]+[kKmMgG]|-XX:[+-](?:UseG1GC|UseParallelGC|UseSerialGC|UseConcMarkSweepGC)|-XX:(?:MaxGCPauseMillis|G1NewSizePercent|G1ReservePercent|G1HeapRegionSize|ParallelGCThreads|ConcGCThreads|InitiatingHeapOccupancyPercent)=[0-9]+[kKmMgG]?)(?=\s|$)')) { [void]$script:jvmFlags.Add($m.Groups[1].Value) }
            }
            if ($line -match '^\s*at\s+([A-Za-z0-9_.$<>]+)\(([A-Za-z0-9_.$]+(?::\d+)?|Native Method|Unknown Source)\)') { [void]$script:frames.Add('at '+$Matches[1]+'('+$Matches[2]+')') }
            elseif ($line -match '(?:^|Caused by:\s+)((?:java|javax|org\.lwjgl|net\.minecraft|me\.eldodebug|me\.movi)\.[A-Za-z0-9_.$]*(?:Exception|Error))(?::|$)') { [void]$script:frames.Add($Matches[1]) }
            elseif ($line -match '(Bad type on operand stack|Bad local variable type|Inconsistent stackmap frames|sealing violation|No OpenGL context found|Pixel format not accelerated)') { [void]$script:frames.Add($Matches[1]) }
        }
    } catch { [void]$script:notes.Add('A log was unreadable; collection continued without that file.'); }
}
try {
    $script:clientHash=Hash-File $ClientJar
    Add-Payload $ClientJar 'client'
    $build=[IO.Path]::GetFileNameWithoutExtension($ClientJar)
    $versionRoot=Join-Path $root 'versions'
    if (Test-Path -LiteralPath $versionRoot) {
        foreach ($dir in (Get-ChildItem -LiteralPath $versionRoot -Directory | Select-Object -First 300)) {
            $json=Join-Path $dir.FullName ($dir.Name+'.json')
            if (-not (Test-Path -LiteralPath $json -PathType Leaf)) { continue }
            try { $raw=Get-Content -LiteralPath $json -Raw; if ($raw.Contains($build) -or $raw.Contains($build.Replace('GlideClient-',''))) { Add-Version $json 'matching' } }
            catch { [void]$script:notes.Add('A version file was unreadable.'); }
        }
    }
    if ($script:versions.Count -eq 0) { [void]$script:notes.Add('No matching launcher version JSON found. Launcher profile/account files were deliberately not collected.'); }
    $glide=Join-Path $root 'glide'
    $configCount=0
    if (Test-Path -LiteralPath $glide) {
        foreach ($file in (Get-ChildItem -LiteralPath $glide -File -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.Extension -eq '.json' -and $_.Length -le 1MB -and $_.FullName -notmatch $secretName -and $_.FullName -notmatch '(?i)[/\\](screenshots|cache|logs|backups)[/\\]' } | Select-Object -First 12)) {
            try { Read-Controls (Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json); $configCount++ } catch { [void]$script:notes.Add('A settings file could not be summarized.'); }
        }
    }
    foreach ($name in @('options.txt','optionsof.txt')) {
        $path=Join-Path $root $name
        if (Test-Path -LiteralPath $path -PathType Leaf) {
            foreach ($line in (Get-Content -LiteralPath $path)) {
                if ($line -cmatch '^(renderDistance|maxFps|enableVsync|useVbo|fboEnable|mipmapLevels|graphics|particles|clouds|of[A-Za-z]+):(true|false|[0-9]+(?:\.[0-9]+)?)$') { [void]$script:controls.Add([ordered]@{control=$Matches[1];value=$Matches[2]}) }
            }
        }
    }
    foreach ($base in @('crash-reports','logs')) {
        $path=Join-Path $root $base
        if (Test-Path -LiteralPath $path) {
            $files=@(Get-ChildItem -LiteralPath $path -File | Where-Object { $_.Name -eq 'latest.log' -or $_.Name -like 'crash-*-client.txt' -or $_.Name -like '*thread*dump*.txt' } | Sort-Object LastWriteTime -Descending | Select-Object -First 3)
            foreach ($file in $files) { Read-Frames $file.FullName }
        }
    }
    $gallery=Join-Path $glide 'screenshots';$screens=@(Get-ChildItem -LiteralPath $gallery -File -ErrorAction SilentlyContinue | Where-Object { $_.Extension -match '^\.(png|jpg|jpeg)$' })
    $total=0L;foreach($file in $screens) { $total+=$file.Length }
    $java=@()
    foreach ($cmd in @(Get-Command java -All -ErrorAction SilentlyContinue)) {
        if ($cmd.CommandType -ne 'Application') { continue }
        try {
            $v=[Diagnostics.FileVersionInfo]::GetVersionInfo($cmd.Source).ProductVersion
            if ($v -match '^[0-9][0-9._+a-zA-Z-]{0,39}$') { $java+=@{productVersion=$v} }
        } catch { }
    }
    $backupCounts=@()
    foreach($relative in @('glide/backups','backups','.velvetg-backups','libraries/me/eldodebug/GlideClient')) {
        $path=Join-Path $root $relative
        if(Test-Path -LiteralPath $path) { $backupCounts+=@{area=$relative;files=@(Get-ChildItem -LiteralPath $path -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 100).Count} }
    }
    $report=[ordered]@{ schema=1; collector='VelvetG Lean read-only collection'; client=@{size=(Get-Item -LiteralPath $ClientJar).Length;sha256=$script:clientHash}; payloads=@($script:payloads.ToArray()); versions=@($script:versions.ToArray()); libraries=@($script:dependencies.ToArray()); configuration=@{filesSummarized=$configCount;controls=@($script:controls.ToArray());policy='Allowlisted numeric/boolean controls and fixed enums only; raw configuration and arbitrary strings omitted.'}; screenshotInventory=@{images=$screens.Count;bytes=$total;imagesCollected=$false}; java=@{executables=$java;reportedVersions=@($script:runtimeVersions.ToArray() | Select-Object -Unique);safeJvmFlags=@($script:jvmFlags.ToArray() | Select-Object -Unique)}; backupInventory=$backupCounts; notes=@($script:notes.ToArray()); exclusions=@('accounts','launcher_profiles.json','launcher_accounts.json','API key files','raw configurations','chat and raw logs','personal paths and names','screenshots and resource packs') }
    $zip=Zip-Open (Join-Path $out 'VelvetG-DIAGNOSTICS.zip')
    try { Add-TextEntry $zip 'collection.json' ($report | ConvertTo-Json -Depth 24); Add-TextEntry $zip 'stack-frames.txt' (($script:frames.ToArray() | Select-Object -Unique) -join "`r`n") } finally { $zip.Dispose() }
    $hashes=@(Get-ChildItem -LiteralPath $out -File -Filter '*.zip' | ForEach-Object { (Hash-File $_.FullName)+'  '+$_.Name })
    [IO.File]::WriteAllLines((Join-Path $out 'SHA256SUMS.txt'),[string[]]$hashes,$script:utf8)
    Write-Host ('Collection complete: '+$out)
    Write-Host 'Upload VelvetG-DIAGNOSTICS.zip and ALL VelvetG-*-PART-*.zip files. Do not upload your key file or screenshot folder.'
    Write-Host 'Minecraft files were read only. No files were installed, patched or removed.'
} catch {
    Write-Error 'Collection incomplete. Do not upload this output directory. No installed Minecraft files were modified.'
    exit 1
}
