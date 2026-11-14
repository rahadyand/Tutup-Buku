# Ponytail FULL — perawatan setelah Revision 002

14 November2026. Baseline main4dac60cfa05c2c2af1efd865b413aacb443e1442, kandidat0.5.3-rc1. Instruksi skill FULL dibaca dari C:/Users/Lenovo/.agents/skills/ponytail/SKILL.md. PRD25halaman, AGENTS, arsitektur, status, Revision001/002 dan source terkait ditinjau. Ini pemeriksaan terfokus, bukan klaim audit seluruh repository atau izin memulai dashboard.

## Dua perubahan yang dipilih

1. **Hapus jalur panduan NIM usang.** Pencarian seluruh src/docs menemukan EXAMPLE tanpa pemanggil dan describe hanya di tes R001. AuthView sudah memakai welcome sejak R002. Hapus constant/method tersebut, pertahankan welcome persis; tes R001 tetap memeriksa login legacy/nol awal, dan RevisionTwoGuidanceTest+GUI tetap menguji pesan yang benar-benar tampil. Ini menghindari dua definisi panduan NIM yang dapat berbeda tanpa manfaat pengguna. Tidak ada kontrak SDK eksternal yang didokumentasikan untuk aplikasi desktop ini.
2. **Validasi metadata Course sekali.** saveCourseMetadata tadinya membentuk objek identik di INSERT dan UPDATE. Konstruksi dipindah tepat setelah requireSemester di transaksi yang sama; kedua cabang memakai nilai tervalidasi yang sama. Mengurangi risiko nama/kode/SKS divalidasi berbeda. Prepared SQL, WHEREowner+semester+ID, revisi, readback, rollback dan pesan error tidak berubah. Tidak memakai objek default untuk menimpa kehadiran.

Jangkauan ditelusuri sebelum edit: AuthView/NimGuidance serta dua tes guidance/login; AcademicView→AcademicService→AcademicRepository→SqliteRepository, constructor Course dan seluruh caller/fixture saveCourseMetadata. Build/version mengikuti pom, judul kandidat dan script. Tidak menambah dependency, abstraction atau framework.

## Yang sengaja dipertahankan

- Engine BigDecimal/ScoreFraction, bobot tepat100, missing≠0, skor earned/max, batas84,5=A-, D/E gagal, target60→55→70, mustahil/W=0, kehadiranE dengan hasil asal, IP seluruh course. Tidak mengedit kode kalkulasi; regresi lama tetap oracle hasil angka eksplisit/independen.
- Jalur saveCourse lama masih dipakai fixture/API kompatibilitas; tidak digabung dengan metadata-only karena boleh mengubah kehadiran. Semester range/periode legacy dan jalur save+activate berbeda maksud, tidak digabung.
- Mapping live Course dan snapshot sengaja berbeda: kolom/revisi/data historis tidak boleh tertukar. Tidak membuat mapper/repository generik hanya demi menghapus baris.
- Binder query belajar dan nilai mempunyai jenis input serta cleanup yang berbeda; tidak diseragamkan dalam pass kecil ini. Transaksi/migrasi, ownership+FK, hash600k, CAS, autosave/bookmark ordering dan guard tetap.
- DashboardView/CalculationDemoView default adalah fixture shell M1 yang didokumentasikan dan dipakai JavaFxSmokeCheck, sehingga bukan dead code. Tidak menghapus konsep OOP/interface/lapisan atau merapikan formatting seluruh proyek.
- H-02 dibaca dari KNOWN_ISSUES: exception internal JavaFXListView pada run kedua paket0.5.1, pemicu belum pasti. Tidak direproduksi/diperbaiki pada pass ini dan tidak ditandai selesai.

## Verifikasi aktual sejauh ini

Baseline sebelum edit: **177tes lulus**. Fokus sesudah edit: `-Dtest=RevisionOnePersistenceTest,RevisionTwoGuidanceTest` **16tes lulus** (15+1),0failures/errors/skipped.

Dua kasus baru menutup gap metadata-only: validasi blank name/SKS0/negatif/null untuk create+edit tanpa mengubah record/revisi; trigger AFTER INSERT/UPDATE memaksa kegagalan, kemudian membuktikan rollback course+aktual+snapshot, retry, kehadiran/acuan tetap, revisi/basis usang setelah edit sukses. Semua memakai @TempDir, bukan database AppData pengguna.

Regresi final pada Temurin21.0.13+9/JavaFX21.0.10, cache lokal offline:
```powershell
$env:JAVA_HOME = (Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME = "$PWD\.tools\maven-user"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp -Pwindows-package verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ReleaseHardeningFxCheck,RevisionOneFxCheck,RevisionTwoFxCheck' '-DreuseForks=false' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=RevisionTwoFxCheck' '-DargLine=-Dglass.win.uiScale=125%' verify
```
**179tes default lulus**, **10kasus GUI lulus**, **1pengulangan skala proses125% lulus**; semuanya0failures/errors/skipped. Seluruh uji perhitungan eksplisit/independen lama ikut default, tanpa mengubah produksi/expected result engine. GUI mempertahankan validasi, guard, failure/retry, nilai/attendance, belajar/assets/notes/resume, profil/dropdown, greeting/privacy/password dan posisi navigasi R002.

14snapshot root (normal1366×768 dan emulasi125%, enam layar+aksi semester) **identik SHA256** dengan sebelum edit. Bukti di `tmp/ponytail-ui-comparison.json`, salinan baru `target/verification/ponytail-full`, sebelum edit `tmp/ponytail-ui-before-20261114-124651`. Login/profil/course ditinjau visual. Ini bukti layar yang dicakup, bukan semua keadaan UI; judul OS berubah nomor kandidat saja. Tidak mengubah skala/resolusi/jaringan Windows. Jendela1024 dan benchmark tidak diulang karena UI/engine tidak diubah; bukti R002 sebelumnya tidak dilabeli sebagai hasil baru.

## Paket Windows0.5.4-rc1 dan EXE ekstraksi

Kode aplikasi berubah, sehingga kandidat baru dibuat satu kali; paket sebelumnya tidak dihapus/ditimpa.
```powershell
.\scripts\Build-Windows.ps1 -Offline -SkipBuild
.\scripts\Verify-WindowsPackage.ps1 -Zip 'releases/TutupBuku-0.5.4-rc1-20261114-124851/TutupBuku-0.5.4-rc1-windows-x64.zip'
python tmp/verify-ponytail-payload.py
```
SkipBuild memakai JAR/runtime-libs dari verify di atas, bukan melewati tes. Runtime pinned/dependency/schema tetap; tidak ada unduhan/dependency baru. BUILD-INFO mencatat baseline4dac60c+working-tree changes karena source belum di-checkpoint saat build; hash JAR mengikat payload ke build teruji.

- ZIP: `releases/TutupBuku-0.5.4-rc1-20261114-124851/TutupBuku-0.5.4-rc1-windows-x64.zip` (**95.937.198byte**).
- Launcher distribusi: `releases/TutupBuku-0.5.4-rc1-20261114-124851/TutupBuku/TutupBuku.exe`.
- Launcher diuji: `tmp/Uji Paket Ekstraksi 20261114-124934/TutupBuku/TutupBuku.exe`.
- SHA256ZIP **445FC5DA871AD3679A73DC55FCC7B541B107713D02C77B5E2A0A0EDBCEB3EF95**.
- SHA256JAR **62AB91A579ABE6BFA0FBABC6CCCA304B9DE2C0808D036DA11B887A12EA01639F** sama target build.

Audit226hash+manifest, forbidden0, runtime17modul lulus; semua **227file ZIP cocok hash dengan ekstraksi yang diuji**. ZIP/JAR tidak diregenerasi sesudah tes.

Dua sesi EXE berjalan di path berspasi, JAVA_HOME/MAVEN_HOME kosong dan PATH hanyaSystem32 dengan runtime bawaan. Data fiktif di `tmp/ponytail-package-data-20261114-124935`, bukan AppData nyata. Akun/semester/course/komponen8/10/snapshot awal dibuat via service fixture; tidak diklaim dibuat lewat GUI paket. Interaksi desktop: sapaan NIM tetap, Enterlogin, halaman semester/course, pilih/edit nama→Simpan, status berhasil, close **exit0**; buka EXE sama/login, nama PBO Perawatan054 tetap terbaca di pilihan course, close **exit0**. Read-onlyDB sesudah tutup membuktikan schema4/quick_check/FK sah serta profil,semester aktif,course3SKS,kehadiran,aktual8/10 dan snapshot tetap. Seluruh editor nilai/belajar tidak diulang pada EXE; cakupan tersebut berasal dari suite GUI source.

Kedua log EXE hanya warning JavaFX unnamed module, tanpa exception baru. Dua error alat “coordinate input geometry is unavailable” dipulihkan dengan memilih ulang jendela+capture screenshot; bukan perubahan aplikasi/data. Warning helper unchecked dan jlinkcompress2 juga nonfatal. H-02 tidak dinyatakan teratasi.

Bukti lokal tambahan: `tmp/ponytail-{baseline,focused,verify,gui,scale,package,package-audit}.log`, `tmp/ponytail-exe{1,2}-{stderr.log,result.txt}`, `tmp/ponytail-final-evidence.json` dan target/surefire-reports. Folder generated diabaikan Git; panduan paket disalin saat build, catatan bukti repository ini lebih baru tanpa mengubah payload.

## Checkpoint dan batas

Diff/ignore/staging diperiksa sebelum commit, Wrapper tetap tracked. Hanya source/test/config/docs perawatan disertakan; paket/runtime,DB/assets/log/cache/secrets serta draf untracked HUMAN_ACCEPTANCE_KIT/MORNING_CHECKLIST/P0_AUDIT/docs/academic tidak dimasukkan. Commit `refactor: simplify NIM guidance and course metadata validation`; push normal origin/main dan hash remote harus sama, receipt aktual di `tmp/ponytail-checkpoint.txt` serta jawaban akhir. Tidak force-push/rewrite/publikasi GitHub Release. Berhenti setelah perawatan ini, tidak memulai dashboard.

Ini bukan audit keamanan/review semua jalur atau uji lapangan. Komputer kedua, offline fisik, DPI125% Windows nyata dan usability mahasiswa belum diperiksa. Isolasi akun bukan enkripsi DB; multi-instance/orphanassets dan H-02 tetap batas historis. Menghapus method internal usang dapat memerlukan penyesuaian kode eksternal tak terdokumentasi yang langsung memanggilnya; seluruh pemanggil dalam repository sudah diperiksa/kompilasi. Tidak menambah fitur baru atau mengurangi pengamanan untuk mengejar jumlah baris.
