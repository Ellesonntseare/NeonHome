param(
    [Parameter(Mandatory=$true)][string]$JdkHome,
    [Parameter(Mandatory=$true)][string]$AndroidJar,
    [Parameter(Mandatory=$true)][string]$BuildTools,
    [string]$BuildDir = "$PSScriptRoot/build-local",
    [string]$OutApk = "$PSScriptRoot/../NeonHome.apk"
)
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = $JdkHome
$env:PATH = "$JdkHome/bin;$env:PATH"
function Run([string]$Exe, [string[]]$Arguments) {
    & $Exe @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Exe failed ($LASTEXITCODE)" }
}
New-Item -ItemType Directory -Force $BuildDir,"$BuildDir/classes","$BuildDir/dex" | Out-Null
$BuildDir = (Resolve-Path $BuildDir).Path
$sourceRoot = "$PSScriptRoot/app/src/main"
[xml]$manifest = Get-Content "$sourceRoot/AndroidManifest.xml"
$manifest.manifest.SetAttribute('package','com.elles.neon')
$manifest.Save("$BuildDir/AndroidManifest.xml")
Run "$BuildTools/aapt2.exe" @('compile','--dir',"$sourceRoot/res",'-o',"$BuildDir/resources.zip")
Run "$BuildTools/aapt2.exe" @('link','-o',"$BuildDir/base.apk",'-I',$AndroidJar,'--manifest',"$BuildDir/AndroidManifest.xml",'--min-sdk-version','26','--target-sdk-version','35','--version-code','1','--version-name','0.1.0',"$BuildDir/resources.zip")
$sources = @(Get-ChildItem "$sourceRoot/java" -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
Run "$JdkHome/bin/javac.exe" (@('-encoding','UTF-8','-source','8','-target','8','-bootclasspath',$AndroidJar,'-d',"$BuildDir/classes") + $sources)
Run "$JdkHome/bin/jar.exe" @('cf',"$BuildDir/classes.jar",'-C',"$BuildDir/classes",'.')
Run "$JdkHome/bin/java.exe" @('-cp',"$BuildTools/lib/d8.jar",'com.android.tools.r8.D8','--lib',$AndroidJar,'--min-api','26','--output',"$BuildDir/dex", "$BuildDir/classes.jar")
Copy-Item "$BuildDir/base.apk" "$BuildDir/unsigned.apk" -Force
Run "$JdkHome/bin/jar.exe" @('uf',"$BuildDir/unsigned.apk",'-C',"$BuildDir/dex",'classes.dex')
Run "$BuildTools/zipalign.exe" @('-f','4',"$BuildDir/unsigned.apk", "$BuildDir/aligned.apk")
if (!(Test-Path "$BuildDir/debug.keystore")) {
    Run "$JdkHome/bin/keytool.exe" @('-genkeypair','-keystore',"$BuildDir/debug.keystore",'-storepass','android','-keypass','android','-alias','androiddebugkey','-dname','CN=Android Debug,O=Android,C=US','-keyalg','RSA','-keysize','2048','-validity','10000')
}
Run "$JdkHome/bin/java.exe" @('-jar',"$BuildTools/lib/apksigner.jar",'sign','--ks',"$BuildDir/debug.keystore",'--ks-pass','pass:android','--out',$OutApk,"$BuildDir/aligned.apk")
Run "$JdkHome/bin/java.exe" @('-jar',"$BuildTools/lib/apksigner.jar",'verify','--verbose',$OutApk)
Run "$BuildTools/aapt2.exe" @('dump','badging',$OutApk)
Write-Output "Built: $OutApk"
