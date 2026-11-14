param(
    [string]$JdkHome = '',
    [string]$JavaFxSdkHome = '',
    [switch]$Offline,
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
if (!$JdkHome) { $JdkHome = Join-Path $projectRoot '.tools/jdk-21.0.13+9' }
if (!$JavaFxSdkHome) { $JavaFxSdkHome = Join-Path $projectRoot '.tools/javafx-sdk-21.0.10' }
$JdkHome = (Resolve-Path -LiteralPath $JdkHome).Path
$JavaFxSdkHome = (Resolve-Path -LiteralPath $JavaFxSdkHome).Path
foreach ($tool in @('java.exe','jlink.exe','jpackage.exe')) { if (!(Test-Path -LiteralPath (Join-Path $JdkHome "bin/$tool"))) { throw "$tool tidak ada; jalankan Prepare-PackagingTools.ps1." } }
$jdkRelease = Get-Content (Join-Path $JdkHome 'release') -Raw
if ($jdkRelease -notmatch 'IMPLEMENTOR="Eclipse Adoptium"' -or $jdkRelease -notmatch 'JAVA_VERSION="21\.0\.13"') { throw 'Gunakan Temurin21.0.13 portable yang terdokumentasi agar runtime/legal paket konsisten.' }
$releaseVersion = '0.5.4-rc1'
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$release = Join-Path $projectRoot "releases/TutupBuku-$releaseVersion-$stamp"
if (Test-Path -LiteralPath $release) { throw 'Folder rilis sudah ada; tidak ditimpa. Jalankan lagi pada detik berbeda.' }
$work = Join-Path $projectRoot "tmp/package-$stamp"
if (Test-Path -LiteralPath $work) { throw 'Folder staging sudah ada; tidak ditimpa.' }
New-Item -ItemType Directory -Path $release,$work | Out-Null
Push-Location $projectRoot
$previousJava = $env:JAVA_HOME; $previousMavenHome = $env:MAVEN_USER_HOME
try {
    $env:JAVA_HOME = $JdkHome; $env:MAVEN_USER_HOME = Join-Path $projectRoot '.tools/maven-user'
    if (!$SkipBuild) {
        $buildArguments = @('-Dmaven.repo.local=.m2/repository','-B','-ntp','-Pwindows-package','verify')
        if ($Offline) { $buildArguments = @('-o') + $buildArguments }
        & (Join-Path $projectRoot 'mvnw.cmd') @buildArguments
        if ($LASTEXITCODE -ne 0) { throw 'Build/test gagal; paket tidak dibuat.' }
    }
    $inputFolder = Join-Path $work 'input'; New-Item -ItemType Directory -Path $inputFolder | Out-Null
    $applicationJar = Join-Path $projectRoot "target/tutup-buku-$releaseVersion.jar"
    Copy-Item -LiteralPath $applicationJar -Destination $inputFolder
    $libraries = @(Get-ChildItem (Join-Path $projectRoot 'target/runtime-libs') -Filter '*.jar')
    if ($libraries.Count -ne 13) { throw 'Daftar dependency runtime berubah; tinjau pom dan script sebelum mengemas.' }
    foreach ($library in $libraries) {
        if ($library.Name -notmatch '^(javafx-(base|graphics|controls|web|media)-21\.0\.10(-win)?|commonmark(-ext-gfm-tables)?-0\.24\.0|sqlite-jdbc-3\.51\.0\.0)\.jar$') { throw "Dependency tak terduga: $($library.Name)" }
        Copy-Item -LiteralPath $library.FullName -Destination $inputFolder
    }
    $runtime = Join-Path $work 'runtime'
    # jdeps mengidentifikasi modul dasar; crypto/charset/localedata/unsupported.desktop menjaga dukungan runtime desktop.
    $modules = 'java.base,java.desktop,java.net.http,java.sql,jdk.jfr,jdk.jsobject,jdk.unsupported,jdk.xml.dom,jdk.unsupported.desktop,jdk.crypto.ec,jdk.charsets,jdk.localedata'
    & (Join-Path $JdkHome 'bin/jlink.exe') --add-modules $modules --strip-debug --no-header-files --no-man-pages --compress=2 --include-locales=en,id --output $runtime
    if ($LASTEXITCODE -ne 0) { throw 'jlink gagal.' }
    & (Join-Path $JdkHome 'bin/jpackage.exe') --type app-image --name TutupBuku --app-version 0.5.4 --vendor 'Tim Tutup Buku' --description 'Modul Belajar dan Simulasi Nilai lokal' --input $inputFolder --main-jar "tutup-buku-$releaseVersion.jar" --main-class id.ac.uns.tutupbuku.ui.Launcher --runtime-image $runtime --dest $release
    if ($LASTEXITCODE -ne 0) { throw 'jpackage gagal; artefak sebelumnya tidak dihapus.' }
    $image = Join-Path $release 'TutupBuku'; $guideFolder = Join-Path $image 'panduan'
    New-Item -ItemType Directory -Path $guideFolder | Out-Null
    foreach ($document in @('USER_GUIDE.md','RELEASE_NOTES.md','RELEASE_ACCEPTANCE.md','DEMO_SCRIPT.md','THIRD_PARTY_NOTICES.md')) { Copy-Item -LiteralPath (Join-Path $projectRoot "docs/$document") -Destination $guideFolder }
    $legal = Join-Path $image 'licenses'; New-Item -ItemType Directory -Path $legal | Out-Null
    foreach ($module in @('javafx.base','javafx.graphics','javafx.controls','javafx.web','javafx.media')) { Copy-Item -LiteralPath (Join-Path $JavaFxSdkHome "legal/$module") -Destination $legal -Recurse }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    foreach ($library in $libraries | Where-Object { $_.Name -notlike 'javafx-*' }) {
        $archive = [IO.Compression.ZipFile]::OpenRead($library.FullName)
        try {
            foreach ($entry in $archive.Entries | Where-Object { $_.Name -match '^(?i)(LICENSE.*|NOTICE.*|COPYING.*)$' }) {
                $destination = Join-Path $legal ($library.BaseName + '-' + $entry.Name)
                [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$destination,$false)
            }
        } finally { $archive.Dispose() }
    }
    $sourceRevision = 'source archive, Git metadata unavailable'; $sourceDirty = 'unknown'
    if ((Get-Command git -ErrorAction SilentlyContinue) -and (Test-Path -LiteralPath (Join-Path $projectRoot '.git'))) {
        $sourceRevision = git rev-parse HEAD
        $sourceDirty = @(git status --porcelain).Count -gt 0
    }
    @("Tutup Buku $releaseVersion","Source baseline=$sourceRevision; working-tree changes=$sourceDirty",'Temurin21.0.13+9; JavaFX21.0.10; schema4',"Runtime modules=$modules",'Build input hashes:') + @(Get-ChildItem $inputFolder -File | ForEach-Object { "$((Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash)  $($_.Name)" }) | Set-Content -LiteralPath (Join-Path $image 'BUILD-INFO.txt') -Encoding UTF8
    $forbidden = @(Get-ChildItem $image -Recurse -File | Where-Object { $_.FullName -match '\\(\.git|\.m2|\.tools|target|assets|secrets)\\|\.(db|sqlite|sqlite3|log|pem|key|pfx|java|class)$|\\\.env(\.|$)' })
    if ($forbidden.Count -gt 0) { throw "Artefak terlarang dalam paket: $($forbidden.FullName -join ', ')" }
    $checksums = @(Get-ChildItem $image -Recurse -File | ForEach-Object { "$((Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash)  $($_.FullName.Substring($image.Length+1))" })
    $checksums | Set-Content -LiteralPath (Join-Path $image 'SHA256SUMS.txt') -Encoding UTF8
    $zip = Join-Path $release "TutupBuku-$releaseVersion-windows-x64.zip"
    Compress-Archive -LiteralPath $image -DestinationPath $zip -CompressionLevel Optimal
    "$((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash)  $([IO.Path]::GetFileName($zip))" | Set-Content -LiteralPath "$zip.sha256" -Encoding UTF8
    Write-Output "ZIP: $zip"; Write-Output "Launcher: $(Join-Path $image 'TutupBuku.exe')"
} finally { $env:JAVA_HOME=$previousJava; $env:MAVEN_USER_HOME=$previousMavenHome; Pop-Location }
