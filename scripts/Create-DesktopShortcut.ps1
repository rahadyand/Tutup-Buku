param(
    [string]$ApplicationDirectory = $PSScriptRoot,
    [string]$DesktopDirectory = ''
)
$ErrorActionPreference = 'Stop'
if ($env:OS -ne 'Windows_NT') { throw 'Shortcut ini hanya dapat dibuat pada Windows.' }
$image = (Resolve-Path -LiteralPath $ApplicationDirectory).Path
$launcher = Join-Path $image 'TutupBuku.exe'
foreach ($required in @('TutupBuku.exe', 'app/TutupBuku.cfg', 'runtime/bin/java.exe', 'SHA256SUMS.txt')) {
    if (!(Test-Path -LiteralPath (Join-Path $image $required) -PathType Leaf)) {
        throw "Paket tidak lengkap: $required. Pilih folder app-image TutupBuku yang telah diverifikasi."
    }
}
$manifestLine = @(Get-Content -LiteralPath (Join-Path $image 'SHA256SUMS.txt') | Where-Object { $_ -match '^[0-9A-Fa-f]{64}  TutupBuku\.exe$' })
if ($manifestLine.Count -ne 1 -or (Get-FileHash -LiteralPath $launcher -Algorithm SHA256).Hash -ine $manifestLine[0].Substring(0,64)) {
    throw 'Checksum launcher tidak cocok. Ekstrak ulang paket yang terverifikasi; shortcut tidak diubah.'
}
if (!$DesktopDirectory) { $DesktopDirectory = [Environment]::GetFolderPath('Desktop') }
if (!$DesktopDirectory -or !(Test-Path -LiteralPath $DesktopDirectory -PathType Container)) {
    throw 'Desktop pengguna tidak tersedia. Jalankan kembali dari sesi Windows pengguna yang memiliki Desktop.'
}
$desktop = (Resolve-Path -LiteralPath $DesktopDirectory).Path
$shortcutPath = Join-Path $desktop 'Tutup Buku.lnk'
$shell = New-Object -ComObject WScript.Shell
$created = $false
try {
    $shortcut = $shell.CreateShortcut($shortcutPath)
    if (Test-Path -LiteralPath $shortcutPath) {
        if ($shortcut.TargetPath -ine $launcher -or $shortcut.WorkingDirectory -ine $image -or $shortcut.Arguments -ne '') {
            throw "Shortcut Tutup Buku sudah ada dengan konfigurasi berbeda (target: $($shortcut.TargetPath)). Tidak ditimpa. Simpan/ubah nama shortcut lama sebelum memasang paket baru."
        }
    } else {
        $shortcut.TargetPath = $launcher
        $shortcut.WorkingDirectory = $image
        $shortcut.Arguments = ''
        $shortcut.IconLocation = "$launcher,0"
        $shortcut.Description = 'Tutup Buku - Modul Belajar dan Simulasi Nilai'
        $shortcut.Save()
        $created = $true
    }
    $verified = $shell.CreateShortcut($shortcutPath)
    if ($verified.TargetPath -ine $launcher -or $verified.WorkingDirectory -ine $image -or $verified.Arguments -ne '') {
        throw 'Verifikasi shortcut gagal. Buka TutupBuku.exe dari folder paket dan periksa pengaturan shortcut.'
    }
    [PSCustomObject]@{
        Shortcut = $shortcutPath; Target = $verified.TargetPath
        WorkingDirectory = $verified.WorkingDirectory; Icon = $verified.IconLocation
        Created = $created; ConfigurationVerified = $true
        DoubleClickVerified = $false
    }
} finally {
    if ($null -ne $verified) { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($verified) }
    if ($null -ne $shortcut) { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($shortcut) }
    [void][Runtime.InteropServices.Marshal]::ReleaseComObject($shell)
}
