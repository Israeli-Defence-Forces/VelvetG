[CmdletBinding()]
param([ValidateSet('Install','Verify','Restore')][string]$Action='Install',[string]$GameDir='D:\Roaming\.minecraft',[string]$JavaPath='',[string]$SourceVersion='')
$ErrorActionPreference='Stop'
Set-StrictMode -Version 2
$root=$null
$package=$PSScriptRoot
$expected='3e4b0190e26b4a1540ac23ff018a8c8280b8ab447c34cce0fc5bf8cc6c2a6813'
$mcExpected='14f0d96d1a56fb4f5c3b2233d00699525893fe5ce3dcf181e7de59120595d298'
$oldCoordinate='me.eldodebug:GlideClient:2.0.9.6-lean-fixed-20261016'
$newRelease='2.0.9.6-lean-overhaul3'
$newId='VelvetG-Lean-Overhaul3'
$state=$null
$receiptPath=$null
function Hash([string]$p){return (Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash.ToLowerInvariant()}
function ReadJson([string]$p){return ([IO.File]::ReadAllText($p)|ConvertFrom-Json)}
function SetProp($obj,[string]$name,$value){$obj|Add-Member -MemberType NoteProperty -Name $name -Value $value -Force}
function WriteJson($obj,[string]$path){$text=$obj|ConvertTo-Json -Depth 100;[IO.File]::WriteAllText($path,$text,(New-Object Text.UTF8Encoding($false)))}
function RemoveEmpty([string]$path){if((Test-Path -LiteralPath $path -PathType Container) -and [IO.Directory]::GetFileSystemEntries($path).Length -eq 0){[IO.Directory]::Delete($path,$false)}}
function Resolve-VelvetJava([string]$requested){
 if(-not [string]::IsNullOrWhiteSpace($requested)){
  $candidate=[IO.Path]::GetFullPath($requested.Trim().Trim('"'))
  if(-not (Test-Path -LiteralPath $candidate -PathType Leaf)){throw 'Java executable not found. Pass -JavaPath with the full path to java.exe.'}
  return $candidate
 }
 if(-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)){
  foreach($name in @('java.exe','java')){
   # An obsolete or malformed JAVA_HOME must not prevent PATH discovery.
   try{
    $candidate=Join-Path ($env:JAVA_HOME.Trim().Trim('"')) ('bin/'+$name)
    if(Test-Path -LiteralPath $candidate -PathType Leaf){return [IO.Path]::GetFullPath($candidate)}
   }catch{continue}
  }
 }
 foreach($name in @('java.exe','java')){
  # A function or alias named Java is not an executable. PowerShell names ignore case.
  $command=Get-Command -Name $name -CommandType Application -ErrorAction SilentlyContinue|Select-Object -First 1
  if($null -ne $command -and -not [string]::IsNullOrWhiteSpace($command.Path)){return $command.Path}
 }
 throw 'Java 8 or newer was not found. Install Java or pass -JavaPath with the full path to java.exe.'
}
function Invoke-VelvetJava([string[]]$arguments){& $script:javaExecutable @arguments;if($LASTEXITCODE -ne 0){throw 'Java patch/integrity check failed; installation was not approved.'}}
# Local custom artifacts use the empty-URL convention used by locally installed Forge libraries.
# SHA-1/size/path describe the local file; no network location is invented.
function Assert-Receipt($r){
 if($r.schema -ne 2 -or $r.coordinate -ne ('me.eldodebug:GlideClient:'+$newRelease) -or $r.original -ne $baseline -or $r.library -ne $library -or $r.versionJson -ne $versionJson){throw 'Receipt target is not this installation.'}
 $backupRoot=[IO.Path]::GetFullPath($state)+[IO.Path]::DirectorySeparatorChar
 $fullBackup=[IO.Path]::GetFullPath([string]$r.backup)
 if(-not $fullBackup.StartsWith($backupRoot,[StringComparison]::OrdinalIgnoreCase) -or [IO.Path]::GetFileName($fullBackup) -ne 'original-client.jar'){throw 'Unsafe backup path in receipt.'}
 if($r.librarySha256 -notmatch '^[a-f0-9]{64}$' -or $r.versionSha256 -notmatch '^[a-f0-9]{64}$'){throw 'Invalid receipt hashes.'}
}
function Remove-Owned([string]$path,[string]$hash){
 if(Test-Path -LiteralPath $path){
  if((Hash $path) -ne $hash){throw 'Interrupted transaction contains changed files. Refusing removal.'}
  if((Get-Item -LiteralPath $path).IsReadOnly){throw 'An owned file is read-only. Clear read-only before retrying.'}
  Remove-Item -LiteralPath $path
 }
}
function Recover-Transaction {
 if(-not (Test-Path -LiteralPath $journalPath)){return}
 $j=ReadJson $journalPath;Assert-Receipt $j
 if((Hash $baseline) -ne $expected -or (Hash $j.backup) -ne $expected){throw 'Original/backup validation failed during recovery.'}
 if((Test-Path -LiteralPath $receiptPath) -and -not ($j.PSObject.Properties['operation'] -and $j.operation -eq 'Restore')){
  $r=ReadJson $receiptPath;Assert-Receipt $r
  if($r.librarySha256 -ne $j.librarySha256 -or $r.versionSha256 -ne $j.versionSha256 -or (Hash $library) -ne $r.librarySha256 -or (Hash $versionJson) -ne $r.versionSha256){throw 'Conflicting completed/interrupted receipts. No files removed.'}
 }else{
  if(Test-Path -LiteralPath $receiptPath){$existing=ReadJson $receiptPath;Assert-Receipt $existing;if($existing.librarySha256 -ne $j.librarySha256 -or $existing.versionSha256 -ne $j.versionSha256){throw 'Changed rollback receipt. No files removed.'}}
  # Validate BOTH files before removing either. Never discard unknown user changes.
  foreach($pair in @(@($library,$j.librarySha256),@($versionJson,$j.versionSha256))){if(Test-Path -LiteralPath $pair[0]){if((Hash $pair[0]) -ne $pair[1] -or (Get-Item -LiteralPath $pair[0]).IsReadOnly){throw 'Interrupted transaction has changed or read-only files. No files removed.'}}}
  Remove-Owned $library $j.librarySha256;Remove-Owned $versionJson $j.versionSha256
  if($j.PSObject.Properties['operation'] -and $j.operation -eq 'Restore' -and (Test-Path -LiteralPath $receiptPath)){Remove-Item -LiteralPath $receiptPath}
  RemoveEmpty (Split-Path -Parent $library);RemoveEmpty $versionDir
  Write-Host 'Recovered an interrupted installation. Verified originals/backups were retained.'
 }
 Remove-Item -LiteralPath $journalPath
}
$lock=$null;$stage=$null;$committed=$false;$created=New-Object 'System.Collections.Generic.List[string]';$createdDirs=New-Object 'System.Collections.Generic.List[string]'
try {
 if([string]::IsNullOrWhiteSpace($GameDir)){throw 'GameDir must be a non-empty directory path.'}
 $root=[IO.Path]::GetFullPath($GameDir.Trim().Trim('"'))
 $state=Join-Path $root 'velvetg-lean-overhaul3'
 $receiptPath=Join-Path $state 'receipt.json'
 $journalPath=Join-Path $state 'transaction.json'
 $baseline=Join-Path $root 'libraries/me/eldodebug/GlideClient/2.0.9.6-lean-fixed-20261016/GlideClient-2.0.9.6-lean-fixed-20261016.jar'
 $relative=('me/eldodebug/GlideClient/'+$newRelease+'/GlideClient-'+$newRelease+'.jar')
 $library=Join-Path (Join-Path $root 'libraries') $relative
 $versionDir=Join-Path (Join-Path $root 'versions') $newId
 $versionJson=Join-Path $versionDir ($newId+'.json')
 if(-not (Test-Path -LiteralPath $root -PathType Container)){throw 'Minecraft directory does not exist. Pass -GameDir with the actual directory.'}
 $script:javaExecutable=Resolve-VelvetJava $JavaPath
 $manifest=ReadJson (Join-Path $package 'package.json')
 foreach($file in $manifest.files.PSObject.Properties){$p=Join-Path $package $file.Name;if((Hash $p) -ne $file.Value){throw ('Package checksum mismatch: '+$file.Name)}}
 [IO.Directory]::CreateDirectory($state)|Out-Null
 # A crashed process releases this handle; the remaining empty lock file is reusable.
 $lock=[IO.File]::Open((Join-Path $state 'install.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::Write,[IO.FileShare]::None)
 $recovering=(Test-Path -LiteralPath $journalPath)
 Recover-Transaction
 if($recovering -and $Action -eq 'Restore' -and -not (Test-Path -LiteralPath $receiptPath)){Write-Host 'Interrupted rollback/install recovered; originals retained.';exit 0}
 if($Action -ne 'Install'){
  if(-not (Test-Path -LiteralPath $receiptPath)){throw 'No Overhaul3 receipt was found. Original installation was not changed.'}
  $r=ReadJson $receiptPath;Assert-Receipt $r
  if((Hash $r.original) -ne $expected -or (Hash $r.backup) -ne $expected){throw 'Original or backup hash mismatch. Refusing automatic changes.'}
  if((Hash $r.library) -ne $r.librarySha256 -or (Hash $r.versionJson) -ne $r.versionSha256){throw 'Installed patch or version metadata changed. Refusing automatic removal.'}
  Invoke-VelvetJava -arguments @('-cp',(Join-Path $package 'patcher.jar'),'VerifyPatch',$r.original,$r.library,(Join-Path $package 'entries.sha256'))
  $v=ReadJson $r.versionJson;$lib=@($v.libraries|Where-Object {$_.name -eq $r.coordinate})
  if($lib.Count -ne 1 -or $lib[0].downloads.artifact.sha1 -ne (Get-FileHash -LiteralPath $r.library -Algorithm SHA1).Hash.ToLowerInvariant() -or $lib[0].downloads.artifact.size -ne (Get-Item -LiteralPath $r.library).Length -or $lib[0].downloads.artifact.url -ne '' -or $lib[0].downloads.artifact.path -ne $relative -or -not $lib[0].PSObject.Properties['url'] -or $lib[0].url -ne ''){throw 'Launcher SHA-1/size metadata mismatch.'}
  if($Action -eq 'Restore'){
   # Only our exact, verified side-by-side files are removed. User configs, screenshots, and original libraries remain.
   if((Get-Item -LiteralPath $r.library).IsReadOnly -or (Get-Item -LiteralPath $r.versionJson).IsReadOnly){throw 'Installed files are read-only; no rollback files removed.'}
   # Persist rollback intent before deleting either file, so interrupted removal is recoverable.
   SetProp $r 'operation' 'Restore';$rollbackIntent=Join-Path $state ('restore-'+[Guid]::NewGuid().ToString('N')+'.json');WriteJson $r $rollbackIntent;[IO.File]::Move($rollbackIntent,$journalPath)
   Remove-Owned $r.library $r.librarySha256
   Remove-Owned $r.versionJson $r.versionSha256
   Remove-Item -LiteralPath $receiptPath -Force
   Remove-Item -LiteralPath $journalPath -Force
   RemoveEmpty (Split-Path -Parent $r.library)
   RemoveEmpty (Split-Path -Parent $r.versionJson)
   Write-Host 'Patch removed. Select your original launcher version. Backups and settings were retained.'
  }else{Write-Host 'Installation hashes and launcher metadata verified offline. No game launch was performed.'}
  exit 0
 }
 if(Test-Path -LiteralPath $receiptPath){throw 'Overhaul3 is already installed. Run VERIFY-INSTALLATION.bat or RESTORE-BACKUP.bat.'}
 $baseline=Join-Path $root 'libraries/me/eldodebug/GlideClient/2.0.9.6-lean-fixed-20261016/GlideClient-2.0.9.6-lean-fixed-20261016.jar'
 if(-not (Test-Path -LiteralPath $baseline) -or (Hash $baseline) -ne $expected){throw 'Installed Lean JAR is missing or differs from the collected build. No files changed.'}
 $versionMatches=@()
 foreach($folder in Get-ChildItem -LiteralPath (Join-Path $root 'versions') -Directory){$file=Join-Path $folder.FullName ($folder.Name+'.json');if(-not (Test-Path -LiteralPath $file)){continue};try{$v=ReadJson $file;if(@($v.libraries|Where-Object {$_.name -eq $oldCoordinate}).Count -gt 0){if(-not $SourceVersion -or $folder.Name -eq $SourceVersion){$versionMatches+=@{path=$file;value=$v}}}}catch{continue}}
 if($versionMatches.Count -ne 1){throw 'Exactly one matching launcher version is required. Use -SourceVersion with its folder name if several profiles use this JAR.'}
 $source=$versionMatches[0];$launchVersion=$source.value;$sourceHash=Hash $source.path
 $mc=$null
 foreach($folder in Get-ChildItem -LiteralPath (Join-Path $root 'versions') -Directory){$candidate=Join-Path $folder.FullName ($folder.Name+'.jar');if((Test-Path -LiteralPath $candidate) -and (Get-Item -LiteralPath $candidate).Length -eq 8461484 -and (Hash $candidate) -eq $mcExpected){$mc=$candidate;break}}
 if(-not $mc){throw 'Exact Minecraft 1.8.9 base JAR not found. Install/launch vanilla 1.8.9 first.'}
 $relative=('me/eldodebug/GlideClient/'+$newRelease+'/GlideClient-'+$newRelease+'.jar')
 $library=Join-Path (Join-Path $root 'libraries') $relative
 $versionDir=Join-Path (Join-Path $root 'versions') $newId
 $versionJson=Join-Path $versionDir ($newId+'.json')
 if((Test-Path -LiteralPath $library) -or (Test-Path -LiteralPath $versionDir)){throw 'Destination already exists. Refusing to overwrite it.'}
 [IO.Directory]::CreateDirectory($state)|Out-Null
 $stage=Join-Path $state ([Guid]::NewGuid().ToString('N'));[IO.Directory]::CreateDirectory($stage)|Out-Null
 $output=Join-Path $stage 'patched.jar'
 # Resolve only this version's declared dependencies and inherited parents.
 $dependencyPaths=New-Object 'System.Collections.Generic.List[string]'
 $pending=$launchVersion;$visited=New-Object 'System.Collections.Generic.HashSet[string]'
 for($depth=0;$depth -lt 16;$depth++){
  foreach($dep in $pending.libraries){
   if($dep.name -eq $oldCoordinate){continue}
   $relativeDependency=$null
   if($dep.PSObject.Properties['downloads'] -and $dep.downloads.PSObject.Properties['artifact'] -and $dep.downloads.artifact.PSObject.Properties['path']){$relativeDependency=$dep.downloads.artifact.path}else{
    $parts=$dep.name.Split(':');if($parts.Length -lt 3){continue}
    if(@($parts|Where-Object {$_ -notmatch '^[A-Za-z0-9_.-]+$'}).Count -gt 0){throw 'Unsupported dependency coordinate.'}
    $classifier='';if($parts.Length -gt 3){$classifier='-'+$parts[3]}
    $relativeDependency=$parts[0].Replace('.','/')+'/'+$parts[1]+'/'+$parts[2]+'/'+$parts[1]+'-'+$parts[2]+$classifier+'.jar'
   }
   $libraryRoot=[IO.Path]::GetFullPath((Join-Path $root 'libraries'))+[IO.Path]::DirectorySeparatorChar
   $depPath=[IO.Path]::GetFullPath((Join-Path $libraryRoot $relativeDependency))
   if(-not $depPath.StartsWith($libraryRoot,[StringComparison]::OrdinalIgnoreCase)){throw 'Dependency path is outside libraries.'}
   if(Test-Path -LiteralPath $depPath){$dependencyPaths.Add($depPath)}
  }
  if(-not $pending.PSObject.Properties['inheritsFrom']){break}
  $parent=[string]$pending.inheritsFrom
  if($parent -notmatch '^[A-Za-z0-9_. -]{1,180}$' -or -not $visited.Add($parent)){throw 'Unsupported or cyclic inherited version.'}
  $parentJson=Join-Path (Join-Path (Join-Path $root 'versions') $parent) ($parent+'.json')
  $pending=ReadJson $parentJson
  if($depth -eq 15){throw 'Inherited version chain exceeds safe depth.'}
 }
 $cpList=Join-Path $stage 'dependencies.txt';[IO.File]::WriteAllLines($cpList,@($dependencyPaths|Select-Object -Unique),(New-Object Text.UTF8Encoding($false)))
 $cpJar=Join-Path $stage 'classpath.jar'
 Invoke-VelvetJava -arguments @('-cp',(Join-Path $package 'patcher.jar'),'ClassPathJar',$cpList,$cpJar)
 $cp=(Join-Path $package 'patcher.jar')+[IO.Path]::PathSeparator+$baseline+[IO.Path]::PathSeparator+$mc+[IO.Path]::PathSeparator+$cpJar
 Invoke-VelvetJava -arguments @('-Xverify:all','-cp',$cp,'LeanPatch',$baseline,$mc,(Join-Path $package 'additions.jar'),$output)
 Invoke-VelvetJava -arguments @('-cp',(Join-Path $package 'patcher.jar'),'VerifyPatch',$baseline,$output,(Join-Path $package 'entries.sha256'))
 $backupDir=Join-Path $state ('backup-'+[DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,8));[IO.Directory]::CreateDirectory($backupDir)|Out-Null
 $backup=Join-Path $backupDir 'original-client.jar';[IO.File]::Copy($baseline,$backup,$false);[IO.File]::Copy($source.path,(Join-Path $backupDir 'original-version.json'),$false)
 if((Hash $backup) -ne $expected -or (Hash $baseline) -ne $expected -or (Hash $source.path) -ne $sourceHash -or (Hash (Join-Path $backupDir 'original-version.json')) -ne $sourceHash){throw 'Backup/source changed during installation.'}
 $coordinate='me.eldodebug:GlideClient:'+$newRelease
 $sha1=(Get-FileHash -LiteralPath $output -Algorithm SHA1).Hash.ToLowerInvariant();$size=(Get-Item -LiteralPath $output).Length
 foreach($lib in $launchVersion.libraries){if($lib.name -eq $oldCoordinate){SetProp $lib 'name' $coordinate;SetProp $lib 'downloads' ([PSCustomObject]@{artifact=[PSCustomObject]@{path=$relative;sha1=$sha1;size=$size;url=''}});SetProp $lib 'url' '';SetProp $lib 'sha1' $sha1;if($lib.PSObject.Properties['checksums']){SetProp $lib 'checksums' @($sha1)};if($lib.PSObject.Properties['size']){SetProp $lib 'size' $size};if($lib.PSObject.Properties['path']){SetProp $lib 'path' $relative}}}
 if(@($launchVersion.libraries|Where-Object {$_.name -eq $coordinate}).Count -ne 1){throw 'Ambiguous custom library declaration.'}
 SetProp $launchVersion 'id' $newId
 SetProp $launchVersion 'jar' ([IO.Path]::GetFileNameWithoutExtension($mc))
 $preparedJson=Join-Path $stage 'version.json';WriteJson $launchVersion $preparedJson
 $receipt=[PSCustomObject]@{schema=2;original=$baseline;backup=$backup;library=$library;librarySha256=(Hash $output);versionJson=$versionJson;versionSha256=(Hash $preparedJson);coordinate=$coordinate}
 Assert-Receipt $receipt
 # Journal exists before the first final-file move; recovery accepts only these exact hashes.
 $preparedJournal=Join-Path $stage 'transaction.json';WriteJson $receipt $preparedJournal;[IO.File]::Move($preparedJournal,$journalPath)
 $libraryDir=Split-Path -Parent $library
 if(-not (Test-Path -LiteralPath $libraryDir)){$createdDirs.Add($libraryDir)}
 $createdDirs.Add($versionDir)
 [IO.Directory]::CreateDirectory($libraryDir)|Out-Null;[IO.Directory]::CreateDirectory($versionDir)|Out-Null
 [IO.File]::Move($output,$library);$created.Add($library)
 [IO.File]::Move($preparedJson,$versionJson);$created.Add($versionJson)
 if((Hash $library) -ne $receipt.librarySha256 -or (Hash $versionJson) -ne $receipt.versionSha256){throw 'Final files failed transaction verification.'}
 $preparedReceipt=Join-Path $stage 'receipt.json';WriteJson $receipt $preparedReceipt;[IO.File]::Move($preparedReceipt,$receiptPath)
 $committed=$true;Remove-Item -LiteralPath $journalPath
 Write-Host 'Installed separate version VelvetG-Lean-Overhaul3. Select it in a new launcher installation. Your original profile is unchanged.'
 exit 0
}catch{
 $failure=$_.Exception.Message
 if(-not $committed){
  # Do not blindly remove moved files: another process/user may have changed one.
  if($lock -and $null -ne (Get-Variable -Name journalPath -ErrorAction SilentlyContinue) -and (Test-Path -LiteralPath $journalPath)){
   try{Recover-Transaction}catch{Write-Host 'Verified transaction cleanup could not complete. Journal retained; changed files were not discarded.'}
  }
  foreach($directory in $createdDirs){try{RemoveEmpty $directory}catch{}}
 }
 Write-Error ('VelvetG operation failed: '+$failure) -ErrorAction Continue

 exit 1
}finally{
 if($stage -and (Test-Path -LiteralPath $stage)){Remove-Item -LiteralPath $stage -Recurse -Force -ErrorAction SilentlyContinue}
 if($lock){$lock.Dispose();Remove-Item -LiteralPath (Join-Path $state 'install.lock') -Force -ErrorAction SilentlyContinue}
}
