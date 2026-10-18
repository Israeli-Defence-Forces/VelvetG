[CmdletBinding()]
param([Parameter(Mandatory=$true)][int]$ProcessId,[ValidateSet('baseline','patched')][string]$Label='patched',[ValidateRange(10,600)][int]$DurationSeconds=60,[string]$OutputDirectory='')
$ErrorActionPreference='Stop'
try {
 if([string]::IsNullOrWhiteSpace($OutputDirectory)){$OutputDirectory=$PSScriptRoot}
 $dir=[IO.Path]::GetFullPath($OutputDirectory);[IO.Directory]::CreateDirectory($dir)|Out-Null
 $file=Join-Path $dir ($Label+'-'+[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')+'.csv')
 $samples=New-Object 'System.Collections.Generic.List[object]'
 $start=[Diagnostics.Stopwatch]::StartNew();$lastTime=0.0;$lastCpu=$null
 while($start.Elapsed.TotalSeconds -lt $DurationSeconds){
  $p=Get-Process -Id $ProcessId -ErrorAction Stop
  $now=$start.Elapsed.TotalSeconds;$cpu=$p.TotalProcessorTime.TotalSeconds
  if($null -ne $lastCpu){$samples.Add([PSCustomObject]@{elapsedSeconds=[Math]::Round($now,3);cpuPercentOneCore=[Math]::Round(100*($cpu-$lastCpu)/($now-$lastTime),2);workingSetMiB=[Math]::Round($p.WorkingSet64/1MB,2);privateMiB=[Math]::Round($p.PrivateMemorySize64/1MB,2);threads=$p.Threads.Count})}
  $lastCpu=$cpu;$lastTime=$now;Start-Sleep -Milliseconds 1000
 }
 $samples|Export-Csv -LiteralPath $file -NoTypeInformation -Encoding UTF8
 Write-Host 'CPU/memory samples saved. This does not measure FPS or GPU frame time. Export the in-client profiler separately.'
}catch{Write-Error $_.Exception.Message;exit 1}
