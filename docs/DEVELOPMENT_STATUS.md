# Status pengembangan — M5 kandidat Windows

Tanggal **9 November 2026, Asia/Jakarta**. Versi **0.5.0-rc1**, schema **3**. **Kandidat lokal M5 siap untuk penerimaan pengguna; gerbang lapangan tetap pending.** Tidak memulai fitur baru/laporan/PPT.

## Baseline dan lingkup

main/origin https://github.com/rahadyand/Tutup-Buku.git dan hash **cf37dbea4e0de35b729808ef130e21c065c9f04a** diperiksa sebelum perubahan. Working tree awal bersih. Baseline137tes diulang dan lulus. Seluruh PRD25halaman, AGENTS, checklist, plan, README, arsitektur/status dibaca kembali. Riwayat M4 disimpan di MILESTONE_4_STATUS.

Pemetaan perilaku/test/pendidikan lapangan ada di [INTEGRATION_REVIEW.md](INTEGRATION_REVIEW.md) dan REQUIREMENTS. F01–F13 tetap lingkup, F08G/F14/F15 bersyarat dan seluruh P2 tetap backlog. Jumlah tes bukan bukti tunggal; perjalanan multi-course dan paket nyata diperiksa.

## Defek/perbaikan

- Label akademik yang masih menyebut nilai belum tersedia diperbaiki.
- Profil dan dua form akademik mendapat Simpan/Buang/Batal; navigasi baru diteruskan setelah save berhasil.
- Edit metadata course mempertahankan attendance.confirmed bila met tidak berubah; perubahan met menjadi asumsi baru.
- Pilihan awal belajar/nilai mengikuti semester aktif jika belum ada pilihan tampilan.
- Save/refresh skenario mempertahankan course terpilih; ditemukan lewat alur multi-course.
- Status Belum disimpan, konfirmasi mode, Lulus/Tidak lulus, sumber Aktual/Asumsi di perbandingan, bantuan rumus dan konteks/bobot ditambahkan.
- Pada125% area reader hampir hilang karena note; note kini panel lipat, sidebar scroll, header ringkas dan window menyesuaikan visual bounds. Selisih IP panjang memakai≈6desimal, tanpa mengubah keputusan engine.
- Main Launcher non-Application, profil Maven runtime dan script Windows. Java21/JavaFX21 tetap, tanpa framework/dependency aplikasi baru; dependency-plugin3.8.1 hanya profil build distribusi.
- Tidak mengubah rumus M1 atau schema. Berkas pengguna/starter dipertahankan, tidak memakai DB nyata atau menghapus artefak rilis lama.

## Verifikasi otomatis akhir

PowerShell memakai:
```powershell
$env:JAVA_HOME = (Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME = "$PWD\.tools\maven-user"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp -Pwindows-package verify
```
Dijalankan melalui **scripts/Build-Windows.ps1 -Offline**. **BUILD SUCCESS;138tes,0failures/errors/skipped.** M1=70, M2=27 (AcademicPersistence14), M3=22, M4=19. Kompilasi72class source dan21file test (termasuk helper/opt-in checks); tidak memasukkan test-only ke aplikasi.

```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck' '-DreuseForks=false' verify
```
**BUILD SUCCESS;7tes,0failures/errors/skipped.** M1shell1, M2GUI2, readeraman1, M3belajar1, M4nilai1, M5terhubung1.

M5FxCheck: register akun baru, semester/course/aktif, guard profil/akademik+Batal/Buang, import/copy aset lewat LearningService yang sama dengan UI lalu pindah file asal, Markdown/tabel/kode/gambar/note/completion, empat course dan IP3,27, konfirmasi mode/estimasi/label lulus, save mempertahankan course, ubah SKS4 dan review/refresh, batal hapus, akun kedua kosong, restart/login/resume dengan note/completion/rasio≈0,6. File chooser native diuji terpisah pada paket, bukan diklaim dari service import dalam tes ini.

```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=MilestoneFiveFxCheck' '-DargLine=-Dglass.win.uiScale=125%' verify
```
**1tes lulus**, diulang setelah perbaikan reader. outputScale=1,25; jendela target fisik1366×768 (konten≈1080×584 logical pixels), desktop fisik1920×1080 dengan Screen logical1536×864 pada emulasi. Pengujian normal juga menargetkan jendela1366×768. **Resolusi/skala Windows global tidak diubah.** Screenshot native/125% di target/verification/m5-learning-*.png, m5-grades-*.png, m5-academic-*.png ditinjau; bacaan, status dan tombol dapat dicapai. Panel panjang digulir, note dilipat.

```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=MilestoneFiveFxCheck,CalculationPerformanceCheck' '-DreuseForks=false' verify
```
**2tes lulus.** Pengukuran terakhir diulang dengan parameter yang sama dan '-Dtest=CalculationPerformanceCheck' verify (**1tes lulus**); median memakai rata-rata dua sampel tengah. Benchmark: Windows11amd64, Temurin21.0.13+9-LTS,12CPUlogis.30course×20komponen,2aktual85/100 per course dan18bebas, bobot5%. Mengukur TargetPlanner+GradeCalculator+IP+review SQLite, bukan setup/hash/render.
- cold **21,738ms**
-3warmup,20sampel: min4,023ms; median **5,606ms**; p95 7,879ms; max **8,762ms**
- Target<1detik tercapai pada mesin ini, bukan klaim semua komputer.

Seluruh DB/aset tes di @TempDir. XML/TXT pada target/surefire-reports dan performance/layout.txt pada target/verification. Warning JavaFX unnamed module, unchecked cast helper tes, serta jlink compress=2 deprecated tercatat; tidak menggagalkan pemeriksaan. Error awal selama pengembangan (nullable preference, selection reset, sinkronisasi layout helper) diperbaiki sebelum hasil akhir.

## Paket Windows dan uji launcher nyata

Persiapan:
```powershell
.\scripts\Prepare-PackagingTools.ps1
.\scripts\Build-Windows.ps1 -Offline
.\scripts\Verify-WindowsPackage.ps1 -Zip 'releases\TutupBuku-0.5.0-rc1-20261109-145254\TutupBuku-0.5.0-rc1-windows-x64.zip'
```

Temurin portable21.0.13+9 resmi:
SHA256 a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9.
SDK OpenJFX21.0.10 untuk legal:
SHA256 8e87151957acf82205e85087071cc4393769a82e672bacebe63d945b718e7e55.
Tidak mengganti Java/PATH global. Runtime Temurin dipilih untuk distribusi beserta legal; semua regresi diulang pada vendor/patch ini.

Build memakai runtime scope13JAR yang dikenali, jlink17modul JDK (termasuk transitif) dan jpackage app-image. Legal JDK/OpenJFX/CommonMark/SQLite, panduan, release notes, checklist/demo dan hash disertakan. Penerima tidak membutuhkan Java/Maven/IDE. Tidak ada DB/aset pribadi, cache, Git, source atau JUnit/test-only dalam image.

Paket pertama **95.921.088byte**, SHA256 **7869b76414071af8bee5c6f071ee09469aca8a517765542058120846e8158812**. Audit Verify-WindowsPackage: **226file hash cocok** +manifest, forbidden0; runtime bawaan --list-modules berhasil dan JAR aplikasi tanpa source/test/data. Artefak pertama dipertahankan. Folder final bertimestamp terbaru dibuat setelah checkpoint untuk menyertakan dokumentasi/hash source terbaru; payload kode harus identik dengan JAR yang diuji.

JAR aplikasi yang dipakai/dibuktikan pada paket:
**2b954f23210ce7c2f63c1c8c5ed18c7866f5afaa544983427479f27d1c914508**.
Sama dengan target/tutup-buku-0.5.0-rc1.jar. BUILD-INFO dan SHA256SUMS mengikat input/payload; hash dan lokasi paket final dilaporkan pada jawaban akhir.

Pemeriksaan EXE hasil ZIP (bukan javafx:run):
- Ekstraksi ke **tmp/Uji Paket Folder Dengan Spasi/TutupBuku/TutupBuku.exe**.
- Child process: JAVA_HOME/MAVEN_HOME kosong, PATH hanya WindowsSystem32, properti proxy Java ke127.0.0.1:9. Tidak mengubah jaringan/firewall Windows, browser atau pengaturan global.
- Folder data **tmp/packaged-demo** di luar instalasi; tidak menyentuh AppData pengguna.
- Otomasi desktop dengan izin pengguna: daftar NIMfiktif0005/login, peminatan/bidang, materi Bacaan PaketM5, Markdown/tabel/kode, **file chooser PNG native**, note, selesai, pratinjau gambar biru.
- Tutup normal → **exit0**. Pindahkan sumber PNG buatan penguji tanpa menghapus aset terkelola. Buka EXE yang sama lagi, login, Lanjutkan Membaca: gambar/tabel/kode/note/completion1/1 dan akun tetap; gambar sumber sudah pindah. Tutup normal lagi → **exit0**.
- Ini uji paket otomatis/visual pada mesin pengembang, bukan uji usability mahasiswa. Nilai/skenario lengkap diverifikasi pada suite M4/M5 dengan SQLite sementara; tidak mengklaim seluruh perjalanan itu diulang manual pada paket.

Script menghasilkan folder timestamp baru dan menolak overwrite. ZIP/runtime tetap lokal dalam releases/ (ignored). Tidak ada GitHub Release otomatis.

## Dokumentasi dan checkpoint

README, requirements, arsitektur, plan, backlog, AGENTS, status historis M4, USER_GUIDE, RELEASE_NOTES, RELEASE_ACCEPTANCE, DEMO_SCRIPT dan THIRD_PARTY_NOTICES diperbarui. Demo9menit adalah langkah yang dapat diulang, bukan hasil pengukuran peserta.

Diff/ignore/daftar file/pola rahasia umum ditinjau sebelum staging; .mvn/Wrapper tetap dilacak. Commit diminta **feat: prepare milestone 5 Windows release candidate**, main pada repository yang sama. Hash/push diverifikasi dan dilaporkan di jawaban akhir; lihat git log -1/git ls-remote untuk hash commit yang memuat status ini.

## Pemeriksaan yang masih memerlukan pengguna

- Komputer Windows x64 kedua tanpa Java/IDE; termasuk MSVC/SmartScreen/AV/izin pada perangkat penerima.
- Jaringan **fisik** diputus dan link luar gagal wajar. Proxy proses bukan pengganti tes ini.
- Windows display1366×768/skala125% **nyata**; emulasi proses/window bukan perubahan layar global.
- Usability3–5mahasiswa: keberhasilan/waktu/kebingungan nyata tanpa bantuan langkah demi langkah.
- Verifikasi data normal AppData pada perangkat pengguna; pengujian pengembang sengaja memakai override demo agar tidak menyentuh database nyata.

Kandidat lokal siap dicoba, **penerimaan akhir pengumpulan belum dinyatakan selesai**. Ikuti RELEASE_ACCEPTANCE, laporkan masalah nyata sebelum pembekuan. Grouped/PDF/AI/kuis/backupUI/IPK/optimasi dan semua penundaan tetap BACKLOG. Tidak melanjutkan milestone/fitur/laporan/PPT otomatis.
