param([string]$BaseJar='')
$ErrorActionPreference='Stop'
$root=$PSScriptRoot
$packageRoot=Split-Path $root -Parent

function Read-ReleaseProperties {
    $values=@{}
    foreach($line in Get-Content -LiteralPath (Join-Path $packageRoot 'RELEASE.properties') -Encoding UTF8){
        if($line -match '^\s*([^#=]+?)\s*=\s*(.*?)\s*$'){$values[$matches[1]]=$matches[2]}
    }
    return $values
}
$releaseInfo=Read-ReleaseProperties
$clientName=$releaseInfo['jar'];$baselineJarName=$releaseInfo['baselineJar'];$releaseLabel=$releaseInfo['label']
if([string]::IsNullOrWhiteSpace($clientName)-or[string]::IsNullOrWhiteSpace($baselineJarName)){throw 'RELEASE.properties is incomplete.'}

$javac=(Get-Command javac -ErrorAction Stop).Source
$jdkBin=Split-Path $javac -Parent
$java=Join-Path $jdkBin 'java.exe';$jar=Join-Path $jdkBin 'jar.exe'
$work=Join-Path $root 'work';$deps=Join-Path $work 'deps';$classes=Join-Path $work 'classes';$tools=Join-Path $work 'tools';$checks=Join-Path $work 'checks';$out=Join-Path $root 'built'
foreach($dir in @($work,$deps)){New-Item -ItemType Directory -Force -Path $dir|Out-Null}
# Preserve downloaded dependency cache, but never reuse stale generated tools/classes/output.
foreach($dir in @($tools,$classes,$checks,$out)){if(Test-Path -LiteralPath $dir){Remove-Item -LiteralPath $dir -Recurse -Force};New-Item -ItemType Directory -Force -Path $dir|Out-Null}

function Run-Java([string[]]$Arguments){& $java @Arguments;if($LASTEXITCODE -ne 0){throw "Java failed with exit code $LASTEXITCODE"}}
function Compile([string[]]$Arguments){& $javac -encoding UTF-8 @Arguments;if($LASTEXITCODE -ne 0){throw "Compilation failed with exit code $LASTEXITCODE. JDK 8 or newer is supported."}}
function Java8Flags(){
    # javac 8 predates --release and prints its version to stderr. Using PowerShell's
    # native redirection here turns that perfectly normal stderr output into a
    # NativeCommandError when $ErrorActionPreference is Stop, so query it through
    # ProcessStartInfo instead.
    $psi=New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName=$javac
    $psi.Arguments='-version'
    $psi.UseShellExecute=$false
    $psi.RedirectStandardOutput=$true
    $psi.RedirectStandardError=$true
    $psi.CreateNoWindow=$true
    $proc=[System.Diagnostics.Process]::Start($psi)
    $stdout=$proc.StandardOutput.ReadToEnd()
    $stderr=$proc.StandardError.ReadToEnd()
    $proc.WaitForExit()
    if($proc.ExitCode -ne 0){throw "Could not query javac version (exit code $($proc.ExitCode))."}
    $versionText=(($stdout+' '+$stderr).Trim())
    Write-Host "Detected $versionText"
    if($versionText -match '^javac\s+1\.8(?:\.|$)'){
        return @('-source','1.8','-target','1.8')
    }
    return @('--release','8')
}
function Assert-Hash([string]$Path,[string]$Expected,[string]$Algorithm='SHA1'){
    if([string]::IsNullOrWhiteSpace($Expected)){return}
    $actual=(Get-FileHash -LiteralPath $Path -Algorithm $Algorithm).Hash.ToLowerInvariant()
    if($actual -ne $Expected.ToLowerInvariant()){throw "$Algorithm mismatch for $Path (expected $Expected, got $actual)"}
}
function Fetch([string]$Url,[string]$Path,[string]$ExpectedHash='',[string]$Algorithm='SHA1'){
    $parent=Split-Path -Parent $Path;if(-not(Test-Path -LiteralPath $parent)){New-Item -ItemType Directory -Force -Path $parent|Out-Null}
    if(Test-Path -LiteralPath $Path){
        try{Assert-Hash $Path $ExpectedHash $Algorithm;return}catch{Remove-Item -LiteralPath $Path -Force}
    }
    $tmp=$Path+'.download';if(Test-Path -LiteralPath $tmp){Remove-Item -LiteralPath $tmp -Force}
    Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $tmp
    Assert-Hash $tmp $ExpectedHash $Algorithm
    Move-Item -LiteralPath $tmp -Destination $Path -Force
}

$bundled=Join-Path $packageRoot $baselineJarName
if([string]::IsNullOrWhiteSpace($BaseJar)){
    if(-not(Test-Path -LiteralPath $bundled)){throw "Keep source-patch beside $baselineJarName, or use -BaseJar with the exact original GameplayFixes-PerfPatch JAR."}
    Compile -Arguments @('-proc:none','-d',$tools,(Join-Path $root 'RestoreBase.java'))
    $BaseJar=Join-Path $work 'restored-base.jar'
    Run-Java -Arguments @('-cp',$tools,'RestoreBase',$bundled,(Join-Path $root 'base-overrides.zip'),(Join-Path $root 'base-index.sha256'),$BaseJar)
}else{
    $expected=(Get-Content (Join-Path $root 'BASE-SHA256.txt') -Raw).Trim()
    if(-not(Test-Path -LiteralPath $BaseJar)){throw 'Base JAR does not exist.'}
    if((Get-FileHash $BaseJar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected){throw 'Use the exact original attached GameplayFixes-PerfPatch JAR.'}
}

[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12
$metadataPath=Join-Path $root 'minecraft-1.8.9.json'
$metadata=Get-Content -LiteralPath $metadataPath -Raw -Encoding UTF8 | ConvertFrom-Json
if($metadata.id -ne '1.8.9'){throw 'Pinned metadata is not Minecraft 1.8.9.'}
$mc=Join-Path $work 'minecraft.jar';Fetch $metadata.downloads.client.url $mc $metadata.downloads.client.sha1 'SHA1'
$dependencyFiles=New-Object System.Collections.Generic.List[string]
foreach($library in $metadata.libraries){
    $artifact=$library.downloads.artifact;if($null -eq $artifact){continue}
    $relative=$artifact.path -replace '/', [IO.Path]::DirectorySeparatorChar
    $destination=Join-Path $deps $relative
    Fetch $artifact.url $destination $artifact.sha1 'SHA1'
    $dependencyFiles.Add($destination)
}
$extras=@(
    @('https://libraries.minecraft.net/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar','net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar','57f402b626d16cc2705bf2a37add7adbb074f0ca3b3102fa6e23aa303dae682f'),
    @('https://repo.maven.apache.org/maven2/org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar','org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar','09d44e6adddaab9fd89ea3164c439f71c755769fa8d3a940352ab3d2003b98ff')
)
foreach($extra in $extras){
    $destination=Join-Path $deps ($extra[1] -replace '/', [IO.Path]::DirectorySeparatorChar)
    Fetch $extra[0] $destination $extra[2] 'SHA256'
    $dependencyFiles.Add($destination)
}

Compile -Arguments @('-proc:none','-cp',$BaseJar,'-d',$tools,(Join-Path $root 'BytecodeIO.java'),(Join-Path $root 'VioletPatch.java'),(Join-Path $root 'Remap.java'),(Join-Path $root 'UnsignedJar.java'))
$toolCp=$tools+';'+$BaseJar
$mcCompile=Join-Path $work 'minecraft-compile.jar';$baseCompile=Join-Path $work 'base-compile.jar';$mcTest=Join-Path $work 'minecraft-test.jar'
Run-Java -Arguments @('-cp',$tools,'UnsignedJar',$mc,$mcTest)
Run-Java -Arguments @('-cp',$toolCp,'Remap','prefix',$mc,$mcCompile)
Run-Java -Arguments @('-cp',$toolCp,'Remap','prefix',$BaseJar,$baseCompile)
$dependencyCp=($dependencyFiles -join ';')
$compileCp=$baseCompile+';'+$mcCompile+';'+$dependencyCp
$sources=@(Get-ChildItem (Join-Path $root 'src') -Recurse -Filter '*.java'|ForEach-Object{$_.FullName})
Compile -Arguments ((Java8Flags)+@('-proc:none','-cp',$compileCp,'-d',$classes)+$sources)
$newCompile=Join-Path $work 'new-compile.jar'; & $jar cfM $newCompile -C $classes .
if($LASTEXITCODE -ne 0){throw 'Packaging classes failed.'}
$newRuntime=Join-Path $work 'new-runtime.jar'
Run-Java -Arguments @('-cp',$toolCp,'Remap','strip',$newCompile,$newRuntime)
$target=Join-Path $out $clientName
Run-Java -Arguments @('-cp',($toolCp+';'+$mc+';'+$dependencyCp),'VioletPatch',$BaseJar,$mc,$newRuntime,$target,$releaseLabel)

$assistClasses=Join-Path $work 'assist-checks'
if(Test-Path -LiteralPath $assistClasses){Remove-Item -LiteralPath $assistClasses -Recurse -Force}
New-Item -ItemType Directory -Force -Path $assistClasses|Out-Null
Compile -Arguments ((Java8Flags)+@('-proc:none','-cp',($newCompile+';'+$compileCp),'-d',$assistClasses,(Join-Path $root 'tests/me/movi/violet/AssistChecks.java')))
$assistCompile=Join-Path $work 'assist-checks-compile.jar'; & $jar cfM $assistCompile -C $assistClasses .
if($LASTEXITCODE -ne 0){throw 'Packaging assist checks failed.'}
$assistRuntime=Join-Path $work 'assist-checks-runtime.jar'
Run-Java -Arguments @('-cp',$toolCp,'Remap','strip',$assistCompile,$assistRuntime)
$runtimeCp=$checks+';'+$assistRuntime+';'+$target+';'+$mcTest+';'+$dependencyCp
$testSources=@(Get-ChildItem (Join-Path $root 'tests') -Filter '*.java'|ForEach-Object{$_.FullName})
Compile -Arguments ((Java8Flags)+@('-proc:none','-cp',($target+';'+$mcTest+';'+$dependencyCp),'-d',$checks,(Join-Path $root 'VioletVerify.java'))+$testSources)
Run-Java -Arguments @('-Xverify:all','-cp',$runtimeCp,'VioletVerify',$target,$BaseJar)
Run-Java -Arguments @('-Xverify:all','-cp',$runtimeCp,'StartupClassChecks',$target)
Run-Java -Arguments @('-Xverify:all','-cp',$runtimeCp,'ScoutFrameChecks',$target,$BaseJar)
Run-Java -Arguments @('-Xverify:all','-cp',$runtimeCp,'EngineChecks')
Run-Java -Arguments @('-Xverify:all','-cp',$runtimeCp,'ReliabilityChecks')
Run-Java -Arguments @('-Xverify:all','-cp',$runtimeCp,'AwarenessChecks')
Run-Java -Arguments @('-Xverify:all','-cp',$runtimeCp,'VisualChecks')
$previewDir=Join-Path $work 'previews'
if(Test-Path -LiteralPath $previewDir){Remove-Item -LiteralPath $previewDir -Recurse -Force}
Run-Java -Arguments @('-Xverify:all','-cp',$runtimeCp,'NativeChecks',$previewDir)
Run-Java -Arguments @('-Xverify:all','-cp',$runtimeCp,'me.movi.violet.AssistChecks')

foreach($name in @('Install-VelvetG.bat','Install-VelvetG.ps1','Uninstall-VelvetG.ps1','README-FIRST.txt','RELEASE-NOTES.md','VALIDATION.txt','RELEASE.properties','AUDIT-FIX-MATRIX.md','CHANGELOG-INTEGRATION.md','INTEGRATION-NOTES.md','TESTING-REPORT.md','AUDIT-INTEGRATION.md','DEPENDENCY-NOTICES-INVENTORY.txt')){Copy-Item (Join-Path $packageRoot $name) $out -Force}
if(Test-Path -LiteralPath $previewDir){Copy-Item -LiteralPath $previewDir -Destination (Join-Path $out 'validation-previews') -Recurse -Force}
Add-Content -LiteralPath (Join-Path $out 'VALIDATION.txt') -Value "`r`nLOCAL BUILD PIPELINE: PASS`r`nVioletVerify, StartupClassChecks, ScoutFrameChecks, EngineChecks, ReliabilityChecks, AwarenessChecks, VisualChecks, NativeChecks and AssistChecks passed while producing this built directory. A live Minecraft/OptiFine launch is still required.`r`n" -Encoding UTF8
$hashLines=@()
foreach($file in Get-ChildItem $out -File){$hash=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant();$hashLines+=("$hash  "+$file.Name)}
[IO.File]::WriteAllLines((Join-Path $out 'SHA256SUMS.txt'),$hashLines,(New-Object System.Text.UTF8Encoding($false)))
Write-Host "Built and offline-checked $target. Live Minecraft/graphics testing is still required." -ForegroundColor Green
