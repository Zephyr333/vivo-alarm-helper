$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$androidSdk = 'D:\DevTools\Android\Sdk'
$androidJar = Join-Path $androidSdk 'platforms\android-35\android.jar'
$buildTools = Join-Path $androidSdk 'build-tools\35.0.0'
$aapt2 = Join-Path $buildTools 'aapt2.exe'
$aapt = Join-Path $buildTools 'aapt.exe'
$d8 = Join-Path $buildTools 'd8.bat'
$zipalign = Join-Path $buildTools 'zipalign.exe'
$apksigner = Join-Path $buildTools 'apksigner.bat'
$buildDir = Join-Path $projectRoot 'build'
$classesDir = Join-Path $buildDir 'classes'
$testClassesDir = Join-Path $buildDir 'test-classes'
$dexDir = Join-Path $buildDir 'dex'
$classesJar = Join-Path $buildDir 'classes.jar'
$compiledResources = Join-Path $buildDir 'resources.zip'
$unsignedApk = Join-Path $buildDir 'unsigned.apk'
$alignedApk = Join-Path $buildDir 'aligned.apk'
$outputApk = Join-Path $projectRoot 'VivoAlarmHelper.apk'
$keyStore = Join-Path $projectRoot 'debug.keystore'

if (-not (Test-Path -LiteralPath $androidJar)) {
    throw "Android platform 35 not found: $androidJar"
}

if (Test-Path -LiteralPath $buildDir) {
    Get-ChildItem -LiteralPath $buildDir -File -Recurse -Force |
        ForEach-Object { [System.IO.File]::Delete($_.FullName) }
}
New-Item -ItemType Directory -Force -Path $classesDir, $testClassesDir, $dexDir | Out-Null

& (Join-Path $projectRoot 'tests\verify-source.ps1')
if ($LASTEXITCODE -ne 0) { throw 'source regression checks failed' }

& $aapt2 compile --dir (Join-Path $projectRoot 'res') -o $compiledResources
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile failed' }

& $aapt2 link -o $unsignedApk -I $androidJar --manifest (Join-Path $projectRoot 'AndroidManifest.xml') $compiledResources
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link failed' }

$javaFiles = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src') -Recurse -Filter '*.java' | ForEach-Object FullName
& javac -encoding UTF-8 -source 8 -target 8 -classpath $androidJar -d $classesDir $javaFiles
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

$testFiles = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'tests') -Filter '*.java' | ForEach-Object FullName
& javac -encoding UTF-8 -source 8 -target 8 -classpath $classesDir -d $testClassesDir $testFiles
if ($LASTEXITCODE -ne 0) { throw 'test javac failed' }
& java -classpath "$classesDir;$testClassesDir" com.example.vivoalarmhelper.TimeTextTest
if ($LASTEXITCODE -ne 0) { throw 'unit tests failed' }
& java -classpath "$classesDir;$testClassesDir" com.example.vivoalarmhelper.AccessibilityServiceIdsTest
if ($LASTEXITCODE -ne 0) { throw 'accessibility service id tests failed' }
& java -classpath "$classesDir;$testClassesDir" com.example.vivoalarmhelper.AccessibilityStatusTest
if ($LASTEXITCODE -ne 0) { throw 'accessibility status tests failed' }
& java -classpath "$classesDir;$testClassesDir" com.example.vivoalarmhelper.PendingExecutionPolicyTest
if ($LASTEXITCODE -ne 0) { throw 'pending execution policy tests failed' }
& java -classpath "$classesDir;$testClassesDir" com.example.vivoalarmhelper.SequenceOffsetsTest
if ($LASTEXITCODE -ne 0) { throw 'profile timing tests failed' }

& jar --create --file $classesJar -C $classesDir .
if ($LASTEXITCODE -ne 0) { throw 'jar failed' }

& $d8 --lib $androidJar --min-api 27 --output $dexDir $classesJar
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

Push-Location $dexDir
try {
    & $aapt add $unsignedApk 'classes.dex'
} finally {
    Pop-Location
}
if ($LASTEXITCODE -ne 0) { throw 'aapt2 add classes.dex failed' }

& $zipalign -f 4 $unsignedApk $alignedApk
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

if (-not (Test-Path -LiteralPath $keyStore)) {
    & keytool -genkeypair -keystore $keyStore -storepass android -keypass android -alias vivoalarmhelper -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Vivo Alarm Helper Test,O=Local Test,C=CN'
    if ($LASTEXITCODE -ne 0) { throw 'keytool failed' }
}

& $apksigner sign --v4-signing-enabled false --ks $keyStore --ks-pass pass:android --key-pass pass:android --ks-key-alias vivoalarmhelper --out $outputApk $alignedApk
if ($LASTEXITCODE -ne 0) { throw 'apksigner failed' }

& $apksigner verify --verbose --print-certs $outputApk
if ($LASTEXITCODE -ne 0) { throw 'APK verification failed' }

Get-Item -LiteralPath $outputApk | Select-Object FullName, Length, LastWriteTime

# OneDrive may protect inherited directories from deletion. Remove generated
# files while retaining the ignored empty directory tree for the next build.
Get-ChildItem -LiteralPath $buildDir -File -Recurse -Force |
    ForEach-Object { [System.IO.File]::Delete($_.FullName) }
