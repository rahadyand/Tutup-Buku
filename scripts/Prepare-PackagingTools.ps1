param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolRoot = Join-Path $projectRoot '.tools'
New-Item -ItemType Directory -Path $toolRoot -Force | Out-Null
function Get-VerifiedArchive($Name, $Url, $Checksum, $ExpectedFolder) {
    $archive = Join-Path $toolRoot $Name
    if (!(Test-Path -LiteralPath $archive)) { Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $archive }
    if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ine $Checksum) {
        throw "Checksum tidak cocok: $Name. File dipertahankan untuk diperiksa; tidak diekstrak."
    }
    if (!(Test-Path -LiteralPath (Join-Path $toolRoot $ExpectedFolder))) { Expand-Archive -LiteralPath $archive -DestinationPath $toolRoot }
}
# Versi dan checksum dikunci, bukan endpoint latest. Tidak mengubah Java/PATH global.
Get-VerifiedArchive 'OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip' 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip' 'a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9' 'jdk-21.0.13+9'
Get-VerifiedArchive 'openjfx-21.0.10_windows-x64_bin-sdk.zip' 'https://download2.gluonhq.com/openjfx/21.0.10/openjfx-21.0.10_windows-x64_bin-sdk.zip' '8e87151957acf82205e85087071cc4393769a82e672bacebe63d945b718e7e55' 'javafx-sdk-21.0.10'
Write-Output 'Temurin21.0.13+9 dan legal OpenJFX21.0.10 siap di .tools; tidak masuk Git.'
