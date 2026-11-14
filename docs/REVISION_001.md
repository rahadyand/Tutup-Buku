# Revision 001 — akun, navigasi dan akademik

Status: implementasi dan verifikasi lokal selesai pada 14 November 2026; kandidat **0.5.2-rc1** untuk review pengguna. Baseline source `0c401ee94a9732701b89677998c65dfc89b08c93`. Instruksi revisi pengguna mengatasi konvensi UI/tahun PRD yang bertentangan. PRD25halaman, AGENTS, arsitektur, status dan source aktual dibaca. Draf audit/kit/academic yang sudah ada tidak diubah atau dimasukkan checkpoint ini.

## Permintaan → hasil

| Permintaan | Implementasi |
|---|---|
| Panduan NIM di Masuk/Daftar | NimGuidance menjelaskan L +01 Informatika/02 Sains Data +tahun2digit +identitas3digit; contoh dan respons input saat mengetik. Tidak melakukan lookup akun lain/verifikasi kampus, mengubah NIM atau profil otomatis, maupun menambah aturan login wajib. Akun0001/nol awal tetap bisa masuk. |
| Form akun terpusat | AuthView dibatasi lebar580, isi form520, NIM240, label/tombol terpusat. Form bergulir jika viewport pendek; tidak mengubah lebar global editor lain. |
| Tiga toggle password | PasswordInput menyinkronkan PasswordField/TextField dua arah, tersembunyi awal, reset saat sukses/keluar tab. Enter tetap submit pada representasi masked maupun visible. Hash/parameter dan pembersihan buffer AuthService tidak berubah. |
| Shortcut kiri atas dan panel | Satu pasang Modul Belajar/Simulasi Nilai tepat di bawah Keluar; tab Dashboard → Semester & Mata Kuliah → Profil. Shortcut membuka DashboardView/LearningView/GradeView yang sama melalui guard. Pair dalam dashboard dihapus, isi dua area tidak didesain ulang. |
| Header relevan | Nama/NIM, prodi, nomor semester aktif/tahun dan tagline. Profil/semester edit/aktivasi/hapus memuat header melalui worker. Tanpa aktif menampilkan pesan singkat, bukan instruksi panel internal. |
| Dua halaman akademik | Halaman1 form semester terpusat, tombol vertikal Jadikan Aktif/Simpan/Hapus. Jadikan Aktif langsung save+activate atomik, Simpan tidak mengganti aktif. Halaman2 daftar semester kiri/course kanan, return jelas, selected berbeda dengan [aktif]. Simpan course dekat form, lebar nama/kode/SKS lokal. |
| Kehadiran hanya di nilai | AcademicView tidak memiliki checkbox/acuan. saveCourseMetadata hanya mengubah nama/kode/SKS/revisi; course baru default met=true/confirmed=false. ActualGradePane memiliki checkbox, konfirmasi dan acuan; saveActual menyimpannya atomik dengan setting nilai. |
| Kompatibilitas dan guard | Schema4 transaksional; ID/course/materi/snapshot/referensi aktif tetap. Guard Simpan/Buang/Batal dipertahankan pada tab, shortcut, halaman, semester/course, logout dan close. Form gagal tetap ada; ID hasil commit disimpan sebelum refresh untuk mencegah retry membuat duplikat. |

## Strategi tahun/periode

- UI baru meminta tahun tunggal empat digit, misalnya2026, serta nomor semester positif. Kombinasi nomor+tahun per akun tidak boleh dibuat ganda pada alur baru. Batas operasional1000–9998 menjaga rentang internal empat digit; tahun9999 belum didukung.
- Kolom internal academic_year tetap `2026/2027`; period juga tetap ada. Record baru mendapat periode dari paritas nomor sebagai metadata kompatibilitas, bukan input pengguna atau klaim aturan akademik. Periode record lama tidak ditebak ulang, termasuk saat nomornya diedit.
- Migrasi3→4 membangun ulang hanya tabel semesters, menyalin ID/kolom/sequence dan memberi flag legacy pada arsip. FK dimatikan sebelum transaksi rebuild, diperiksa sebelum commit dan diaktifkan kembali sesudahnya. Error rollback; tidak reset DB. Semua tabel turunan tetap mengacu ID yang sama.
- Constraint baru owner+nomor+rentang+periode mempertahankan dua arsip lama yang sama nomor/tahun tetapi berbeda periode. Alur baru memeriksa owner+nomor+tahun dalam transaksi. Arsip lama yang tidak mengubah identitas tetap bisa disimpan/diaktifkan masing-masing; label `arsip Ganjil/Genap #ID` membedakannya. Tidak ada merge/delete diam-diam.
- `Semester.year()`/`toString()` menampilkan tahun tunggal di seluruh aplikasi. Versi lama tidak mendukung schema4; cadangkan DB+assets saat aplikasi tertutup sebelum memperbarui. API range/periode lama dipertahankan untuk kompatibilitas/fixture historis, bukan form baru.

## Verifikasi aktual

Lingkungan: Windows11x64, Temurin21.0.13+9 portable, JavaFX21.0.10. Semua DB/aset tes di @TempDir, bukan DB pengguna.

Setup perintah PowerShell:
```powershell
$env:JAVA_HOME=(Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME="$PWD\.tools\maven-user"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp -Pwindows-package verify
```
Hasil **176tes,0failure/error/skipped**:163baseline+13RevisionOnePersistenceTest. Meliputi panduan kedua prodi dan login lama, tahun tunggal, duplikasi, create+activate, save tanpa mengubah aktif, rollback save/aktivasi, metadata/acuan kehadiran, owner, migrasi berhasil/gagal dengan course/materi/note/snapshot lama. Engine nilai/target/IP dan security tetap lulus. Verifikasi default diulang setelah perbaikan UI terakhir; hasil akhir dicatat juga pada DEVELOPMENT_STATUS.

GUI:
```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ReleaseHardeningFxCheck,RevisionOneFxCheck' '-DreuseForks=false' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=ReleaseHardeningFxCheck' verify
```
Sembilan kasus GUI terverifikasi lulus melalui run suite dan pengulangan terfokus. Run suite terakhir awalnya8pass/1error: tes hardening membaca node halaman sebelum layout selesai; helper tes diperbaiki untuk menunggu halaman dan pengulangan1tes lulus. Tidak menyebut run suite gagal itu sebagai BUILD SUCCESS. Tes Revision001 membuktikan visibility/sinkronisasi/reset, header setelah editprofil/editsemester/hapusaktif, shortcut dan urutan, validation/storage failure menjaga halaman1, save/proceed/aktivasi, pemuatan course semester lain, dan Cancel pada navigasi. Tes M2/M3/M4/M5 mempertahankan failure/guard, materi dan nilai/skenario.

Skala proses:
```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=RevisionOneFxCheck,MilestoneTwoFxCheck,MilestoneFiveFxCheck' '-DreuseForks=false' '-DargLine=-Dglass.win.uiScale=125%' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=RevisionOneFxCheck' '-DargLine=-Dglass.win.uiScale=125%' verify
```
Run4tes lulus; pengulangan khusus1tes dengan assertion tinggi label juga lulus. Tinjauan gambar sempat menemukan panduan NIM elipsis meskipun alur lulus; FormSupport.centeredPage kini menjaga tinggi preferensi form dan menggulirnya. Screenshot sesudah fix benar-benar ditinjau dan tidak lagi memotong panduan. RevisionOneFxCheck ukuran normal dijalankan kembali1tes lulus untuk screenshot source final. Ini **emulasi proses JavaFX**, bukan bukti skala Windows nyata. Jendela sasaran fisik1366×768; tidak ada perubahan DPI/resolusi global.

## Screenshot nyata

File lokal, tidak dimasukkan Git/runtime. Nama berikut berada di `target/verification/`:

| Layar | Normal | Emulasi125% |
|---|---|---|
| Login | r001-login-native.png | r001-login-125%.png |
| Registrasi | r001-register-native.png | r001-register-125%.png |
| Header/navigasi | r001-navigation-native.png | r001-navigation-125%.png |
| Semester | r001-semester-native.png | r001-semester-125%.png |
| Course | r001-courses-native.png | r001-courses-125%.png |

Screenshot berasal dari JavaFX aplikasi dengan akun fiktif. Pemeriksaan desktop terpisah juga mengamati EXE hasil ZIP, bukan javafx:run.

## Paket Windows yang diuji

- ZIP: `releases/TutupBuku-0.5.2-rc1-20261114-111743/TutupBuku-0.5.2-rc1-windows-x64.zip`, **95.935.355byte**.
- Launcher distribusi: `releases/TutupBuku-0.5.2-rc1-20261114-111743/TutupBuku/TutupBuku.exe`.
- Launcher hasil ekstraksi yang diuji: `tmp/Uji Paket Ekstraksi 20261114-111905/TutupBuku/TutupBuku.exe`.
- SHA256 ZIP: `F85224558D7899366E198FBCFD1931578217D4C1AE010554C143AAEAC04462EC`.
- SHA256 JAR: `C5803896F0F840C71E514243E433EB9723E98A4B9BB7AA09420D64B4B035EFA8`.

Build-Windows -Offline -SkipBuild memakai JAR/runtime-libs dari build terverifikasi; tidak mengulang unduhan atau menimpa rilis lama. Kandidat percobaan111424 sebelum perbaikan label dipertahankan, **bukan paket yang diserahkan**. BUILD-INFO mencatat baseline0c401ee+working-tree changes=True karena Revision001 belum di-commit saat build; checkpoint revisi memuat source ini. Hash JAR mengikat payload yang diuji ke ZIP final.

Verify-WindowsPackage mencocokkan226file+manifest, forbidden0,17modul runtime. Pemeriksaan tambahan `python tmp/verify-r001-payload.py` mencocokkan seluruh227file ZIP dengan ekstraksi yang benar-benar diuji; JAR sama target build. Data fixture/log/audit tetap di tmp yang diabaikan Git.

EXE dijalankan di path berspasi, JAVA_HOME/MAVEN_HOME kosong dan PATH hanya System32, folder data demo terisolasi. Fixture akun/semester/course/skenario dibuat melalui service sebelum launcher; tidak diklaim dibuat melalui GUI paket. Melalui EXE: NIM/password demo, **Enter untuk login**, header aktual, halaman semester dan course, edit nama course lalu Simpan berhasil. Close normal **exit0**. Pembacaan SQLite read-only setelah tutup membuktikan nama baru/SKS/kehadiran/aktual8/10/skenario tetap; schema4, quick_check/FK sah. Log hanya warning unnamed JavaFX module; tidak muncul exception baru pada sesi ini.

## Batas dan review pengguna

Komputer kedua tanpa alat, offline fisik, Windows125% nyata dan usability mahasiswa **belum dilakukan**. H-02 historis JavaFXListView belum dinyatakan teratasi hanya karena tidak muncul dalam sesi ini. Tahun9999, merge arsip, fitur P1/P2, desain ulang isi dashboard dan draf akademik tidak dikerjakan. Panduan/README/AGENTS/arsitektur/checklist/status diperbarui; dokumentasi repository setelah build dapat lebih baru daripada salinan panduan di ZIP tanpa mengubah payload aplikasi.

Review: coba tiga toggle dan akun lama; aktifkan semester baru tanpa Save dahulu; buat semester lain dengan Save saja; pilih semester di kiri dan edit course; ubah kehadiran pada simulasi lalu pastikan edit metadata tidak meresetnya; batalkan navigasi dengan input belum tersimpan.

## Checkpoint dan kelanjutan

Commit/push normal ke origin/main hanya source/test/docs/config Revision001 setelah diff/staging diperiksa; receipt aktual disimpan pada `tmp/r001-checkpoint.txt` dan dilaporkan pada jawaban akhir. ZIP/runtime/DB/aset/log dan draf lama tidak di-stage. Setelah push terverifikasi, **berhenti untuk review pengguna**, tanpa mulai revisi lain.
