[CmdletBinding()]
param([string]$GameDir='D:\Roaming\.minecraft',[string]$JavaPath='',[switch]$Online,[string]$Player='',[string]$OutputDirectory='')
$ErrorActionPreference='Stop'
Set-StrictMode -Version 2
$lines=New-Object 'System.Collections.Generic.List[string]'
$failed=$false
function Report([string]$line){$lines.Add($line);Write-Host $line}
function Run-Java([string[]]$Values){
 # ProcessStartInfo avoids Windows PowerShell 5.1 treating java -version stderr as an error.
 $info=New-Object Diagnostics.ProcessStartInfo
 $info.FileName=$script:java;$info.UseShellExecute=$false;$info.CreateNoWindow=$true
 $info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
 # These arguments are only owned package/file paths and fixed class names, never credentials.
 foreach($a in $Values){if($a.Contains('"')){throw 'Unsupported quote in executable argument.'}}
 $info.Arguments=(@($Values|ForEach-Object {'"'+$_.Replace('\"','\\"')+'"'}) -join ' ')
 $p=New-Object Diagnostics.Process;$p.StartInfo=$info
 [void]$p.Start();$stdout=$p.StandardOutput.ReadToEnd();$stderr=$p.StandardError.ReadToEnd();$p.WaitForExit()
 return [PSCustomObject]@{code=$p.ExitCode;text=$stdout+$stderr}
}
try{
 $root=[IO.Path]::GetFullPath($GameDir.Trim().Trim('"'))
 if(-not (Test-Path -LiteralPath $root -PathType Container)){throw 'Game directory missing.'}
 if([string]::IsNullOrWhiteSpace($OutputDirectory)){$OutputDirectory=Join-Path $PSScriptRoot ('smoke-results-'+[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,8))}
 $out=[IO.Path]::GetFullPath($OutputDirectory);[IO.Directory]::CreateDirectory($out)|Out-Null
 Report 'VelvetG Overhaul3 local smoke report. No game launch is automated.'
 $manifest=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'package.json') -Raw|ConvertFrom-Json
 foreach($f in $manifest.files.PSObject.Properties){if((Get-FileHash -LiteralPath (Join-Path $PSScriptRoot $f.Name) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $f.Value){throw 'Package integrity failed.'}}
 $tokens=$null;$errors=$null
 $tree=[Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'Manage-VelvetG.ps1'),[ref]$tokens,[ref]$errors)
 if($errors.Count -ne 0){throw 'Installer syntax failed.'}
 $definition=$tree.Find({param($n) $n -is [Management.Automation.Language.FunctionDefinitionAst] -and $n.Name -eq 'Resolve-VelvetJava'},$true)
 . ([ScriptBlock]::Create($definition.Extent.Text))
 $script:java=Resolve-VelvetJava $JavaPath
 $j=Run-Java -Values @('-version')
 if($j.code -ne 0){throw 'Java runtime could not execute.'}
 $match=[regex]::Match($j.text,'version "([0-9][0-9._+-]*)')
 if($match.Success){Report ('OFFLINE Java version: '+$match.Groups[1].Value);if($match.Groups[1].Value -notmatch '^1\.8\.'){Report 'ACTION: select maintained Java 8 for the game. Newer Java may run patch tools.'}}else{Report 'ACTION: Java version could not be identified.'}
 $shell=(Get-Process -Id $PID).Path
 # Capture verifier output locally but never include raw paths or exceptions in exported reports.
 $verify=& $shell -NoProfile -File (Join-Path $PSScriptRoot 'Manage-VelvetG.ps1') -Action Verify -GameDir $root -JavaPath $script:java 2>&1
 if($LASTEXITCODE -ne 0){throw 'Installed offline integrity/metadata checks failed. Run VERIFY-INSTALLATION.bat for local details.'}
 Report 'OFFLINE PASS: package, original/backup, installed JAR, entry hashes and local-only launcher metadata.'
 $r=Get-Content -LiteralPath (Join-Path $root 'velvetg-lean-overhaul3/receipt.json') -Raw|ConvertFrom-Json
 $check=Run-Java -Values @('-Xverify:all','-cp',(Join-Path $PSScriptRoot 'patcher.jar'),'OfflineSmoke',$r.library)
 if($check.code -ne 0){throw 'Installed Java/mode smoke check failed.'}
 Report 'OFFLINE PASS: installed QUAD, ALL_MODES, SOLO, DOUBLES and alias/invalid/null normalization.'
 $startup=Join-Path $root 'config/velvetg-startup.properties'
 if((Test-Path -LiteralPath $startup) -and (Get-Item -LiteralPath $startup).Length -le 65536){
  $timeline=New-Object 'System.Collections.Generic.List[string]';$timeline.Add('stage,durationMs,elapsedMs')
  $data=@{}
  foreach($line in [IO.File]::ReadLines($startup)){if($line -match '^(stage\.[0-9]{2}\.(?:name|durationMs|elapsedMs)|totalMs|startedUnixMs|release)=(.*)$'){$data[$matches[1]]=$matches[2]}}
  if($data['release'] -eq 'Overhaul3'){
   foreach($index in 0..63){$prefix='stage.'+$index.ToString('00')+'.';$name=$data[$prefix+'name'];$duration=$data[$prefix+'durationMs'];$elapsed=$data[$prefix+'elapsedMs']
    if($name -match '^[A-Za-z0-9_$.-]{1,80}$' -and $duration -match '^\d+(?:\.\d+)?$' -and $elapsed -match '^\d+(?:\.\d+)?$'){$timeline.Add($name+','+$duration+','+$elapsed)}}
   [IO.File]::WriteAllLines((Join-Path $out 'startup.csv'),$timeline)
   Report ('Startup export age seconds: '+[Math]::Round(([DateTime]::UtcNow-(Get-Item -LiteralPath $startup).LastWriteTimeUtc).TotalSeconds,1))
   Report 'COLLECTED: last exported Overhaul3 startup timeline; spans instrumented Minecraft.startGame and Glide checkpoints, not launcher authentication/downloads.'
  }else{Report 'PENDING: no Overhaul3 startup timeline yet.'}
 }else{Report 'PENDING: launch Overhaul3 once to collect its startup timeline.'}
 $frameFile=Join-Path $root 'config/velvetg-frame-samples.csv'
 if((Test-Path -LiteralPath $frameFile) -and (Get-Item -LiteralPath $frameFile).Length -le 1048576){
  $samples=@();$safe=New-Object 'System.Collections.Generic.List[string]';$safe.Add('sample,frameIntervalMs')
  foreach($line in [IO.File]::ReadLines($frameFile)){
   if($line -match '^([0-9]{1,5}),([0-9]+(?:\.[0-9]+)?)$' -and $samples.Count -lt 4096){$v=[double]::Parse($matches[2],[Globalization.CultureInfo]::InvariantCulture);if($v -gt 0 -and $v -lt 3600000){$samples+=,$v;$safe.Add($line)}}
  }
  if($samples.Count -gt 0){
   [Array]::Sort($samples);$count=$samples.Count;$mean=($samples|Measure-Object -Average).Average
   $tailCount=[Math]::Max(1,[int][Math]::Ceiling($count*.01));$tail=@($samples[($count-$tailCount)..($count-1)]);$tailMean=($tail|Measure-Object -Average).Average
   Report ('COLLECTED CPU-observed frame intervals: n={0}; average={1:F3} ms; p95={2:F3}; p99={3:F3}; slowest-1%-mean equivalent={4:F2} FPS.' -f $count,$mean,$samples[[int][Math]::Ceiling($count*.95)-1],$samples[[int][Math]::Ceiling($count*.99)-1],(1000/$tailMean))
   [IO.File]::WriteAllLines((Join-Path $out 'frames.csv'),$safe)
   Report ('Frame export age seconds: '+[Math]::Round(([DateTime]::UtcNow-(Get-Item -LiteralPath $frameFile).LastWriteTimeUtc).TotalSeconds,1))
   Report 'These are the last exported client-loop intervals, not GPU timings or proof of equivalent scenes. Export freshly for each run.'
  }else{Report 'PENDING: enable profiler, play a repeatable scene, then Export in /velvetlean.'}
 }else{Report 'PENDING: no frame export. Enable profiler, play a repeatable scene, then Export in /velvetlean.'}
 # Keep only exception-type and warning counts. Never copy raw logs, names, IPs, paths or messages.
 $log=Join-Path $root 'logs/latest.log';$exceptions=0;$warnings=0
 if(Test-Path -LiteralPath $log){
  $stream=[IO.File]::Open($log,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::ReadWrite)
  try{if($stream.Length -gt 8388608){[void]$stream.Seek(-8388608,[IO.SeekOrigin]::End)}
   $reader=New-Object IO.StreamReader($stream)
   while(($line=$reader.ReadLine()) -ne $null){if($line -match '(?:java\.[A-Za-z0-9_$.]*(?:Exception|Error)|VerifyError|NoClassDefFoundError|UnsatisfiedLinkError)'){$exceptions++};if($line -match "Can't keep up|OpenGL error|OutOfMemory|Unable to load"){$warnings++}}
  }finally{$stream.Dispose()}
  Report ('COLLECTED last-log warning counts: exception/error mentions='+$exceptions+'; performance/resource warnings='+$warnings+'. Historical text is not proof of an Overhaul3 failure.')
 }
 if($Online){
  if($Player -notmatch '^[A-Za-z0-9_]{3,16}$'){throw 'Online testing requires -Player with a valid Minecraft username.'}
  $onlineCheck=Run-Java -Values @('-cp',(Join-Path $PSScriptRoot 'patcher.jar'),'OnlineSmoke',(Join-Path $root 'glide/pika-api-key.txt'),$Player)
  if($onlineCheck.text -match 'HTTP_STATUS=([0-9]{3})'){
   $code=[int]$matches[1];Report ('ONLINE Java MONTHLY/QUAD HTTP status: '+$code+'. No response body, player name or credential exported.')
   if($code -eq 200){Report 'ONLINE PASS: selected Java runtime accepted an authenticated endpoint request.'}else{Report 'ONLINE ACTION: non-200 requires investigation. No redirects or automatic retries.';$failed=$true}
  }else{Report 'ONLINE INCONCLUSIVE: key-file, TLS, network or timeout failure. No raw error saved.';$failed=$true}
 }else{Report 'ONLINE SKIPPED. Explicit opt-in: TEST-OVERHAUL3-WINDOWS.bat -Online -Player YourName. The key is read locally, never passed as an argument.'}
 Report 'MANUAL PENDING: launcher selection/repair behavior, real Minecraft launch, native rendering, input, screenshots and matched-scene FPS checks. See WINDOWS-SMOKE-TEST.md.'
}catch{$failed=$true;Report 'FAILED: a smoke check could not complete. Run the offline verifier locally for details; raw exceptions were excluded for privacy.'}
finally{
 if($null -ne (Get-Variable -Name out -ErrorAction SilentlyContinue)){[IO.File]::WriteAllLines((Join-Path $out 'REPORT.txt'),$lines)}
}
if($failed){exit 1}else{exit 0}
