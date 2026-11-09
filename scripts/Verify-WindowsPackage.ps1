param([Parameter(Mandatory=$true)][string]$Zip, [switch]$Launch)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$Zip = (Resolve-Path -LiteralPath $Zip).Path
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$extraction = Join-Path $projectRoot "tmp/Uji Paket Ekstraksi $stamp"
if (Test-Path -LiteralPath $extraction) { throw 'Folder uji sudah ada; tidak ditimpa.' }
Expand-Archive -LiteralPath $Zip -DestinationPath $extraction
$image = (Resolve-Path -LiteralPath (Join-Path $extraction 'TutupBuku')).Path
$forbidden = @(Get-ChildItem $image -Recurse -File | Where-Object { $_.FullName -match '\\(\.git|\.m2|\.tools|target|assets|secrets)\\|\.(db|sqlite|sqlite3)(-wal|-shm|-journal)?$|\.(log|pem|key|pfx|java|class)$|\\\.env(\.|$)' })
if ($forbidden.Count) { throw "Berkas tidak sesuai dalam paket: $($forbidden.FullName -join ', ')" }
$manifest = Join-Path $image 'SHA256SUMS.txt'
$verified = 0
foreach ($line in Get-Content -LiteralPath $manifest) {
    if ($line -notmatch '^([0-9A-Fa-f]{64})  (.+)$') { throw 'Format manifest hash tidak sah.' }
    $expected = $Matches[1]; $relative = $Matches[2]
    $file = [IO.Path]::GetFullPath((Join-Path $image $relative))
    if (!$file.StartsWith($image + [IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw 'Path manifest keluar dari image.' }
    if ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ine $expected) { throw "Hash berbeda: $relative" }
    $verified++
}
if (@(Get-ChildItem $image -Recurse -File).Count -ne $verified + 1) { throw 'Jumlah berkas paket tidak cocok manifest.' }
$zipHash = (Get-FileHash -LiteralPath $Zip -Algorithm SHA256).Hash
if (Test-Path -LiteralPath "$Zip.sha256") { if ((Get-Content -LiteralPath "$Zip.sha256" -Raw).Trim().Split(' ')[0] -ine $zipHash) { throw 'Checksum ZIP tidak cocok.' } }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$appJar = Get-ChildItem (Join-Path $image 'app') -Filter 'tutup-buku-*.jar' | Select-Object -First 1
$jar = [IO.Compression.ZipFile]::OpenRead($appJar.FullName)
try {
    if (@($jar.Entries | Where-Object { $_.FullName -match '(^org/junit/|Test\.class$|Check\.class$|\.java$|\.(db|sqlite)$|^assets/)' }).Count) { throw 'JAR aplikasi membawa source/test/data yang tidak sesuai.' }
} finally { $jar.Dispose() }
$report = @("ZIP=$Zip", "SHA256=$zipHash", "Extraction=$extraction", "Verified files=$verified; forbidden files=0", 'Runtime modules:')
$report += @(& (Join-Path $image 'runtime/bin/java.exe') --list-modules)
if ($LASTEXITCODE -ne 0) { throw 'Runtime bawaan tidak berjalan.' }
$report | Set-Content -LiteralPath (Join-Path $extraction 'verification.txt') -Encoding UTF8
$report | Write-Output
if ($Launch) {
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = Join-Path $image 'TutupBuku.exe'; $start.WorkingDirectory = $image; $start.UseShellExecute = $false
    $start.EnvironmentVariables['TUTUP_BUKU_DATA_DIR'] = Join-Path $projectRoot "tmp/package-data-$stamp"
    $start.EnvironmentVariables['JAVA_HOME'] = ''; $start.EnvironmentVariables['MAVEN_HOME'] = ''; $start.EnvironmentVariables['PATH'] = "$env:SystemRoot\System32"
    # Hanya properti proses tes, bukan perubahan jaringan/firewall Windows.
    $start.EnvironmentVariables['JAVA_TOOL_OPTIONS'] = '-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=9 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=9'
    $process = [Diagnostics.Process]::Start($start); Write-Output "Launcher PID=$($process.Id); tunggu sampai jendela ditutup."
    $process.WaitForExit(); Write-Output "Launcher exit=$($process.ExitCode)"
    if ($process.ExitCode -ne 0) { throw 'Launcher gagal; simpan bukti error dan folder uji.' }
}
