param([string]$MinecraftDir = (Join-Path $env:APPDATA '.minecraft'))
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath($MinecraftDir)
$manifestPath=Join-Path $root 'velvetg-install.json'
$launcherProfilesPath=Join-Path $root 'launcher_profiles.json'
$Utf8NoBom=New-Object System.Text.UTF8Encoding -ArgumentList $false,$true
function Read-Utf8([string]$Path){return [IO.File]::ReadAllText($Path,$Utf8NoBom)}
function Write-Utf8Atomic([string]$Path,[string]$Text){
    $dir=Split-Path -Parent $Path;if(-not(Test-Path -LiteralPath $dir)){New-Item -ItemType Directory -Force -Path $dir|Out-Null}
    $tmp=$Path+'.velvetg.tmp';if(Test-Path -LiteralPath $tmp){Remove-Item -LiteralPath $tmp -Force}
    [IO.File]::WriteAllText($tmp,$Text,$Utf8NoBom)
    try{if(Test-Path -LiteralPath $Path){try{[IO.File]::Replace($tmp,$Path,$null,$true)}catch{Move-Item -LiteralPath $tmp -Destination $Path -Force}}else{[IO.File]::Move($tmp,$Path)}}finally{if(Test-Path -LiteralPath $tmp){Remove-Item -LiteralPath $tmp -Force}}
}
function Backup-Relative([string]$BackupRoot,[string]$Target){
    if([string]::IsNullOrWhiteSpace($BackupRoot)){return $null}
    $relative=$Target.Substring($root.Length).TrimStart([char[]]@('\\','/'))
    return (Join-Path $BackupRoot $relative)
}
function Restore-Or-Remove([string]$Target,[bool]$HadOriginal,[string]$BackupRoot){
    $backup=Backup-Relative $BackupRoot $Target
    if($HadOriginal -and $backup -and (Test-Path -LiteralPath $backup)){
        $dir=Split-Path -Parent $Target;if(-not(Test-Path -LiteralPath $dir)){New-Item -ItemType Directory -Force -Path $dir|Out-Null}
        Copy-Item -LiteralPath $backup -Destination $Target -Force
    }elseif(Test-Path -LiteralPath $Target){Remove-Item -LiteralPath $Target -Force}
}

if(Test-Path -LiteralPath $manifestPath){
    $m=(Read-Utf8 $manifestPath)|ConvertFrom-Json
    $jar=[string]$m.jar
    $versionJson=if($m.versionJson){[string]$m.versionJson}else{Join-Path $root 'versions\VelvetG\VelvetG.json'}
    $lp=if($m.launcherProfiles){[string]$m.launcherProfiles}else{$launcherProfilesPath}
    $backupRoot=[string]$m.backup
    Restore-Or-Remove $jar ([bool]$m.hadJar) $backupRoot
    Restore-Or-Remove $versionJson ([bool]$m.hadVersionJson) $backupRoot
    if([bool]$m.hadLauncherProfiles){
        $backup=Backup-Relative $backupRoot $lp
        if($backup -and (Test-Path -LiteralPath $backup)){Copy-Item -LiteralPath $backup -Destination $lp -Force}
    }elseif(Test-Path -LiteralPath $lp){
        # We created the launcher file. Remove only our entry instead of deleting an
        # installation file the launcher/user may have modified after install.
        $safety=$lp+'.velvetg-uninstall-backup';Copy-Item -LiteralPath $lp -Destination $safety -Force
        $obj=(Read-Utf8 $lp)|ConvertFrom-Json
        if($obj.profiles -and $obj.profiles.PSObject.Properties['VelvetG']){$obj.profiles.PSObject.Properties.Remove('VelvetG')}
        if($obj.PSObject.Properties['selectedProfile'] -and $obj.selectedProfile -eq 'VelvetG'){$obj.selectedProfile=$null}
        Write-Utf8Atomic $lp ($obj|ConvertTo-Json -Depth 16)
    }
    $oldManifest=Backup-Relative $backupRoot $manifestPath
    if([bool]$m.hadInstallManifest -and $oldManifest -and (Test-Path -LiteralPath $oldManifest)){
        Copy-Item -LiteralPath $oldManifest -Destination $manifestPath -Force
    }elseif(Test-Path -LiteralPath $manifestPath){Remove-Item -LiteralPath $manifestPath -Force}
    Write-Host 'VelvetG install files were removed/restored. User settings and caches were intentionally kept.' -ForegroundColor Green
    exit 0
}

# Fallback for installs made before the r8 manifest existed.
$versionJson=Join-Path $root 'versions\VelvetG\VelvetG.json'
if(Test-Path -LiteralPath $versionJson){Remove-Item -LiteralPath $versionJson -Force}
if(Test-Path -LiteralPath $launcherProfilesPath){
    $backup=$launcherProfilesPath+'.velvetg-uninstall-backup';Copy-Item -LiteralPath $launcherProfilesPath -Destination $backup -Force
    $lp=(Read-Utf8 $launcherProfilesPath)|ConvertFrom-Json
    if($lp.profiles -and $lp.profiles.PSObject.Properties['VelvetG']){$lp.profiles.PSObject.Properties.Remove('VelvetG')}
    if($lp.PSObject.Properties['selectedProfile'] -and $lp.selectedProfile -eq 'VelvetG'){$lp.selectedProfile=$null}
    Write-Utf8Atomic $launcherProfilesPath ($lp|ConvertTo-Json -Depth 16)
}
Write-Host 'VelvetG launcher entry/version file removed. No r8 install manifest was present, so an older client library JAR was not guessed at or deleted.' -ForegroundColor Yellow
