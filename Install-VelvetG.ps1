param(
    [string]$MinecraftDir = (Join-Path $env:APPDATA '.minecraft'),
    [switch]$WithoutOptifine,
    [switch]$DoNotSelectProfile
)
$ErrorActionPreference = 'Stop'

function Read-ReleaseProperties {
    $values=@{}
    foreach($line in Get-Content -LiteralPath (Join-Path $PSScriptRoot 'RELEASE.properties') -Encoding UTF8){
        if($line -match '^\s*([^#=]+?)\s*=\s*(.*?)\s*$'){$values[$matches[1]]=$matches[2]}
    }
    return $values
}
$info=Read-ReleaseProperties
$release=$info['release'];$jarName=$info['jar'];$versionName=$info['profile']
if([string]::IsNullOrWhiteSpace($release)-or[string]::IsNullOrWhiteSpace($jarName)-or[string]::IsNullOrWhiteSpace($versionName)){throw 'RELEASE.properties is incomplete.'}
$libraryCoordinate='me.eldodebug:GlideClient:'+$release
$clientJar=Join-Path $PSScriptRoot $jarName
if(-not(Test-Path -LiteralPath $clientJar)){
    $builtJar=Join-Path $PSScriptRoot ("source-patch\built\"+$jarName)
    if(Test-Path -LiteralPath $builtJar){$clientJar=$builtJar}
    else{throw "$jarName has not been built yet. If this is the SOURCE package, run Install-VelvetG.bat (recommended) or BUILD.bat first."}
}

$Utf8NoBom=New-Object System.Text.UTF8Encoding -ArgumentList $false,$true
function Read-Utf8([string]$Path){return [IO.File]::ReadAllText($Path,$Utf8NoBom)}
function Write-Utf8Atomic([string]$Path,[string]$Text){
    $dir=Split-Path -Parent $Path;if(-not(Test-Path -LiteralPath $dir)){New-Item -ItemType Directory -Force -Path $dir|Out-Null}
    $tmp=$Path+'.velvetg.tmp';$replaceBackup=$Path+'.velvetg.replacebak'
    if(Test-Path -LiteralPath $tmp){Remove-Item -LiteralPath $tmp -Force}
    [IO.File]::WriteAllText($tmp,$Text,$Utf8NoBom)
    try{
        if(Test-Path -LiteralPath $Path){
            try{[IO.File]::Replace($tmp,$Path,$replaceBackup,$true);if(Test-Path -LiteralPath $replaceBackup){Remove-Item -LiteralPath $replaceBackup -Force}}
            catch{Move-Item -LiteralPath $tmp -Destination $Path -Force}
        }else{[IO.File]::Move($tmp,$Path)}
    }finally{if(Test-Path -LiteralPath $tmp){Remove-Item -LiteralPath $tmp -Force}}
}
function Assert-Replaceable([string]$Path){
    if(-not(Test-Path -LiteralPath $Path)){return}
    try{$s=[IO.File]::Open($Path,[IO.FileMode]::Open,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None);$s.Close()}
    catch{throw "VelvetG cannot safely update '$Path' because it is in use. Fully close Minecraft AND the launcher, then run the installer again."}
}

# Verify the release payload before touching .minecraft.
$payloadRoot=Split-Path -Parent $clientJar
$manifestPath=Join-Path $payloadRoot 'SHA256SUMS.txt'
if(-not(Test-Path -LiteralPath $manifestPath)){throw 'SHA256SUMS.txt is missing. Refusing to install an unverifiable release package.'}
$expected=$null
    foreach($line in Get-Content -LiteralPath $manifestPath -Encoding UTF8){if($line -match '^([0-9a-fA-F]{64})\s+\*?(.+)$' -and $matches[2].Trim() -eq $jarName){$expected=$matches[1].ToLowerInvariant();break}}
    if($null -eq $expected){throw "SHA256SUMS.txt does not contain $jarName."}
    $actual=(Get-FileHash -LiteralPath $clientJar -Algorithm SHA256).Hash.ToLowerInvariant()
if($actual -ne $expected){throw 'Release JAR failed SHA-256 verification. Re-extract or redownload the package.'}

$root=[IO.Path]::GetFullPath($MinecraftDir)
$libraryDir=Join-Path $root ('libraries\me\eldodebug\GlideClient\'+$release)
$installedJar=Join-Path $libraryDir $jarName
$launcherProfilesPath=Join-Path $root 'launcher_profiles.json'
$versionDir=Join-Path $root ('versions\'+$versionName)
$versionJson=Join-Path $versionDir ($versionName+'.json')
$installManifest=Join-Path $root 'velvetg-install.json'
$backupRoot=Join-Path $root ('velvetg-backups\'+(Get-Date -Format 'yyyyMMdd-HHmmss'))

foreach($path in @($installedJar,$versionJson,$launcherProfilesPath,$installManifest)){Assert-Replaceable $path}
New-Item -ItemType Directory -Force -Path $libraryDir,$versionDir|Out-Null

$backupMap=@{};$created=New-Object System.Collections.Generic.List[string]
foreach($target in @($installedJar,$versionJson,$launcherProfilesPath,$installManifest)){
    if(Test-Path -LiteralPath $target){
        $relative=$target.Substring($root.Length).TrimStart([char[]]@('\','/'));$destination=Join-Path $backupRoot $relative;$destinationDir=Split-Path -Parent $destination
        if(-not(Test-Path -LiteralPath $destinationDir)){New-Item -ItemType Directory -Force -Path $destinationDir|Out-Null}
        Copy-Item -LiteralPath $target -Destination $destination -Force;$backupMap[$target]=$destination
    }else{$created.Add($target)}
}

try{
    # Preserve data from older Velvet names. Existing VelvetG data always wins.
    $configDir=Join-Path $root 'config';New-Item -ItemType Directory -Force -Path $configDir|Out-Null
    $migrations=@{
      'velvet-1.3.0.properties'='velvetg-1.3.0.properties';'velvet-history-1.3.properties'='velvetg-history-1.3.properties';
      'velvet-player-data.properties'='velvetg-player-data.properties';'velvet-1.3-settings.properties'='velvetg-1.3-settings.properties';
      'velvet-stats-cache-v1.bin'='velvetg-stats-cache-v1.bin'
    }
    foreach($oldName in $migrations.Keys){
        $old=Join-Path $configDir $oldName;$new=Join-Path $configDir $migrations[$oldName]
        if((Test-Path -LiteralPath $old)-and-not(Test-Path -LiteralPath $new)){
            $created.Add($new)
            Copy-Item -LiteralPath $old -Destination $new -Force
        }
    }

    Copy-Item -LiteralPath $clientJar -Destination $installedJar -Force
    $sourceSha256=(Get-FileHash $clientJar -Algorithm SHA256).Hash.ToLowerInvariant();$installedSha256=(Get-FileHash $installedJar -Algorithm SHA256).Hash.ToLowerInvariant();$sourceSha1=(Get-FileHash $clientJar -Algorithm SHA1).Hash.ToLowerInvariant()
    if($sourceSha256 -ne $installedSha256){throw 'Installed VelvetG JAR checksum does not match the verified release JAR.'}

    $libraries=@(@{name=$libraryCoordinate;sha1=$sourceSha1},@{name='net.minecraft:launchwrapper:1.12'},@{name='org.ow2.asm:asm-all:5.0.3'})
    $arguments='--username ${auth_player_name} --version ${version_name} --gameDir ${game_directory} --assetsDir ${assets_root} --assetIndex ${assets_index_name} --uuid ${auth_uuid} --accessToken ${auth_access_token} --userProperties ${user_properties} --userType ${user_type} --tweakClass me.eldodebug.soar.injection.mixin.GlideTweaker'
    if(-not $WithoutOptifine){$libraries+=@{name='me.eldodebug:Optifine:Release';url='https://glideclient.github.io/data/libs/';sha1='0bb792be16ec78d8b62586cc4856950befb3391b'};$arguments+=' --tweakClass optifine.OptiFineForgeTweaker'}
    $profile=@{id=$versionName;inheritsFrom='1.8.9';jar='1.8.9';assets='1.8';type='release';mainClass='net.minecraft.launchwrapper.Launch';minecraftArguments=$arguments;minimumLauncherVersion=14;libraries=$libraries;javaVersion=@{component='jre-legacy';majorVersion=8}}
    Write-Utf8Atomic $versionJson ($profile|ConvertTo-Json -Depth 8)

    $launcherProfileChanged=$false
    if(Test-Path -LiteralPath $launcherProfilesPath){
        $lp=(Read-Utf8 $launcherProfilesPath)|ConvertFrom-Json
        if($null -eq $lp.profiles){$lp|Add-Member -NotePropertyName profiles -NotePropertyValue ([pscustomobject]@{})}
        $key='VelvetG';$now=(Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ss.fffZ')
        $existing=$lp.profiles.PSObject.Properties[$key]
        if($null -eq $existing){$entry=[pscustomobject]@{created=$now;icon='Grass';lastUsed=$now;lastVersionId=$versionName;name='VelvetG';type='custom'};$lp.profiles|Add-Member -NotePropertyName $key -NotePropertyValue $entry}
        else{$entry=$existing.Value;$entry|Add-Member -NotePropertyName lastVersionId -NotePropertyValue $versionName -Force;$entry|Add-Member -NotePropertyName name -NotePropertyValue 'VelvetG' -Force;$entry|Add-Member -NotePropertyName lastUsed -NotePropertyValue $now -Force}
        if(-not $DoNotSelectProfile){if($lp.PSObject.Properties['selectedProfile']){$lp.selectedProfile=$key}else{$lp|Add-Member -NotePropertyName selectedProfile -NotePropertyValue $key}}
        Write-Utf8Atomic $launcherProfilesPath ($lp|ConvertTo-Json -Depth 16);$launcherProfileChanged=$true
        $verifyLauncher=(Read-Utf8 $launcherProfilesPath)|ConvertFrom-Json
        $verifyEntry=$verifyLauncher.profiles.PSObject.Properties[$key]
        if($null -eq $verifyEntry -or $verifyEntry.Value.lastVersionId -ne $versionName){throw 'Launcher profile verification failed after write.'}
    }

    $written=(Read-Utf8 $versionJson)|ConvertFrom-Json;$entry=$written.libraries|Where-Object{$_.name -eq $libraryCoordinate}|Select-Object -First 1
    if($null -eq $entry){throw "Launcher profile '$versionJson' does not reference $libraryCoordinate"}
    if($entry.sha1 -and $entry.sha1.ToLowerInvariant() -ne $sourceSha1){throw "Launcher profile '$versionJson' has the wrong VelvetG JAR SHA-1."}

    $manifest=[ordered]@{release=$release;installedAt=(Get-Date).ToUniversalTime().ToString('o');jar=$installedJar;versionJson=$versionJson;launcherProfiles=$launcherProfilesPath;profile='VelvetG';sha256=$sourceSha256;backup=$backupRoot;hadJar=$backupMap.ContainsKey($installedJar);hadVersionJson=$backupMap.ContainsKey($versionJson);hadLauncherProfiles=$backupMap.ContainsKey($launcherProfilesPath);hadInstallManifest=$backupMap.ContainsKey($installManifest)}
    Write-Utf8Atomic $installManifest ($manifest|ConvertTo-Json -Depth 4)

    # Keep the newest five backups. They are recovery points, not a geological record.
    $backupParent=Join-Path $root 'velvetg-backups'
    if(Test-Path -LiteralPath $backupParent){Get-ChildItem -LiteralPath $backupParent -Directory|Sort-Object Name -Descending|Select-Object -Skip 5|Remove-Item -Recurse -Force -ErrorAction SilentlyContinue}

    Write-Host ''
    Write-Host 'VelvetG + Violet r8 installed successfully.' -ForegroundColor Green
    Write-Host "Profile:          $versionName"
    Write-Host "Coordinate:       $libraryCoordinate"
    Write-Host "SHA-256:          $sourceSha256"
    if($launcherProfileChanged){Write-Host 'Launcher entry:   VelvetG updated'}
    if($backupMap.Count -gt 0){Write-Host "Recovery backup:  $backupRoot"}
    Write-Host ''
    Write-Host 'Fully close and reopen the Minecraft Launcher before starting the game.' -ForegroundColor Yellow
}catch{
    Write-Warning 'Installation failed. Restoring files changed by this run.'
    foreach($path in $created){if(Test-Path -LiteralPath $path){Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue}}
    foreach($target in $backupMap.Keys){$src=$backupMap[$target];$dir=Split-Path -Parent $target;if(-not(Test-Path -LiteralPath $dir)){New-Item -ItemType Directory -Force -Path $dir|Out-Null};Copy-Item -LiteralPath $src -Destination $target -Force}
    throw
}
