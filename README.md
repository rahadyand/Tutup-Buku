# Tutup Buku — Kandidat Windows 0.5.0-rc1

Aplikasi desktop Java untuk final project PBO semester 3, mahasiswa Fakultas Teknologi Informasi dan Sains Data UNS. Tenggat produk: **8 Desember 2026**. UI dan dokumentasi berbahasa Indonesia.

M1–M5 menyediakan akun lokal, profil/semester/mata kuliah, materi Markdown dan **nilai aktual, target banyak komponen, skenario tersimpan serta IP semester**. Dua area dashboard tetap **Modul Belajar** dan **Simulasi Nilai**. Tidak ada layanan cloud/AI.

## Setup, build, test dan run Windows

Perlu JDK **21**, bukan hanya JRE. Terverifikasi di Oracle JDK21.0.12 Windows x64. Maven global tidak diperlukan: Maven Wrapper3.3.4 mengunci Maven3.9.11 beserta checksum SHA-256.

Buka PowerShell pada folder proyek:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
.\mvnw.cmd javafx:run
```

Sesuaikan JAVA_HOME dengan lokasi JDK. Build pertama memerlukan internet untuk Maven/dependensi; alur inti aplikasi bekerja lokal. Hasil build `target/tutup-buku-0.5.0-rc1.jar` belum paket mandiri/fat JAR; jalankan melalui **javafx:run**, bukan java -jar. Paket Windows ber-runtime tersedia melalui script M5; pengujian komputer kedua tetap perlu hasil pengguna.

Cache sesi pengembangan ini berada di proyek:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
$env:MAVEN_USER_HOME = "$PWD\.tools\maven-user"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o clean verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o javafx:run
```

Hilangkan -o pada unduhan pertama jika cache belum ada. Git mengabaikan .m2/, .tools/, target/, tmp/, database, aset pengguna, log dan rahasia; **.mvn/wrapper/** dan skrip Wrapper tetap dilacak. Tidak perlu alat format lokal untuk membangun aplikasi.

## Data lokal dan migrasi

Default **%LOCALAPPDATA%\TutupBuku\tutup-buku.db**; fallback user.home/AppData/Local/TutupBuku. Demo terisolasi:

```powershell
$env:TUTUP_BUKU_DATA_DIR = "$PWD\tmp\demo-m4"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o javafx:run
```

Untuk kembali ke lokasi biasa, `Remove-Item Env:TUTUP_BUKU_DATA_DIR -ErrorAction SilentlyContinue` hanya menghapus variabel sesi, tidak menghapus file. Tes selalu menggunakan database/aset sementara, bukan database AppData Anda.

Skema otomatis dimigrasikan **1→2→3** secara aditif dan transaksional per versi. M4 tidak mengubah file gambar/bacaan M3. Database rusak, versi tak dikenal, dan gagal migrasi dilaporkan **tanpa reset**. Tutup aplikasi sebelum menyalin cadangan manual, lalu salin database **beserta folder assets**.

Password memakai PBKDF2-HMAC-SHA256 bawaan Java, 600.000 iterasi, salt acak16byte, hasil256bit; parameter tersimpan. NIM adalah teks yang di-trim, nol awal tetap. Database tidak dienkripsi seluruhnya. Tidak ada email, recovery universal atau login otomatis setelah restart.

## Mencoba akun, profil dan akademik (M2)

1. Daftar NIM, nama, password minimal8karakter dan konfirmasi. Coba NIM sama dan password login salah untuk melihat validasi.
2. Masuk; edit nama/prodi di **Profil**. NIM tetap. Prodi boleh kosong sampai dilengkapi.
3. **Semester & mata kuliah** → Semester baru. Nomor positif, tahun YYYY/YYYY berurutan, Ganjil/Genap. Tahun/periode unik per akun.
4. **Jadikan aktif** menyimpan referensi aktif; semester lama tidak hilang. Layar perencanaan dapat melihat semester mana pun tanpa mengubah referensi ini.
5. Buat mata kuliah: nama dan SKS positif, kode opsional, acuan kehadiran dapat diedit. Centang awal berarti asumsi belum terverifikasi.
6. Edit/hapus memakai pilihan daftar. Hapus selalu dikonfirmasi dan menjelaskan data turunan. Keluar/masuk akun lain untuk memeriksa isolasi; restart untuk memeriksa persistensi.

## Mencoba Modul Belajar (M3)

1. Pilih Kuliah → Semester → Mata kuliah → tambah bab; atau Peminatan → tambah bidang tanpa ketergantungan semester.
2. **Salin 5 materi contoh (sekali)** menyalin3pengantar PBO ke course pilihan dan2peminatan, setelah konfirmasi. Koleksi asli singkat, bukan course lengkap; tidak ada seed otomatis saat login/migrasi.
3. Buat materi, judul wajib dan sumber opsional. Tab Tulis mendukung Markdown heading/list/bold/italic/tabel/fenced code/link. Isi kosong boleh Draf.
4. Pratinjau/Baca menampilkan kode sebagai teks. Ringkasan manual memakai heading `## Ringkasan`.
5. **Sisipkan gambar lokal** mengimpor PNG/JPEG (≤10MB, ≤16juta piksel) ke assets/pemilik/UUID. Rumus memakai gambar; file asli dapat dipindah setelah salinan berhasil.
6. Catatan pribadi terpisah dari isi. Simpan eksplisit atau autosave2detik. Jika gagal, teks tetap; Simpan dapat dicoba lagi.
7. Selesai ditandai manual. Progres course menghitung semua materi, bukan rata-rata bab. Membuka bacaan tidak menyelesaikannya.
8. Gulir, pindah/logout/restart, lalu **Lanjutkan Membaca**. Isi berubah → awal; bacaan dihapus → penjelasan. Naik/Turun mengatur urutan.
9. Navigasi/logout/close yang meninggalkan edit belum tersimpan menawarkan Simpan/Buang/Batal. Hapus catatan/materi meminta konfirmasi.

HTML raw di-escape, script halaman WebView disabled, tanpa Java bridge. Gambar hanya aset lokal milik akun; traversal/file URL/gambar luar ditolak. Link http/https dibuka di browser setelah klik, sehingga link luar bisa memerlukan internet. Aset tak terpakai tidak dihapus otomatis.

## Mencoba Simulasi Nilai (M4)

1. Buka **Simulasi nilai**, pilih semester. **Komponen & aktual** → pilih mata kuliah.
2. Tambah Tugas20%, UTS30%, UAS50% dengan perolehan kosong. Nama bebas; UTS/UAS tidak wajib. Maksimum boleh disiapkan lebih dahulu. Simpan tiap komponen; Naik/Turun/Hapus tersedia.
3. Bobot salah tetap boleh disimpan sebagai **draf**, tetapi total harus tepat100% sebelum hasil detail dihitung. 99,99/100,01 tidak dinormalisasi.
4. **Skenario, target & IP** → Skenario baru, beri nama, target IP0–4, target detail angka60 atau huruf minimum C. **Hitung target & IP**: semua komponen bebas mendapat saran60 (skala normalisasi0–100).
5. Centang Kunci asumsi Tugas, isi80, Hitung: saran UTS/UAS55. **Simpan skenario**.
6. Kembali **Komponen & aktual**, pilih UTS, isi perolehan30/maksimum100, Simpan. Aktual tidak pernah berasal dari saran solver.
7. Kembali skenario, Hitung: **USANG/PERLU DITINJAU**. **Tinjau/perbarui basis** menjelaskan perubahan; konfirmasi **Perbarui draf** → saran UAS70. Tinjau lalu **Simpan**. Kunci untuk komponen yang kini aktual/terhapus dibuang saat refresh; target dan kunci lain tetap.
8. Actual earned/max misalnya8/10,35/40,84,5/100 didukung. Kosong berarti belum diketahui;0 adalah nilai aktual.84,5 tetap A-.
9. **Mode penilaian aktif** memilih Detail atau Huruf langsung. Huruf aktual dimasukkan pada tab aktual, estimasi pada skenario. Data mode tidak aktif tetap disimpan, hanya mode aktif dihitung. Huruf aktual tersedia mengalahkan estimasi huruf dalam mode langsung.
10. Kehadiran awal asumsi terpenuhi. Uncheck skenario memaksa efektif E/0 tanpa menghapus hasil asal; check kembali memulihkannya. Tab aktual juga menyediakan penanda bahwa kehadiran telah diverifikasi.
11. Coba target mustahil, kontribusi sudah cukup atau semua komponen terkunci (W=0). Pesan menjelaskan keadaan; kebutuhan>100 tidak dipotong menjadi100. Skor/bobot aktual tidak diubah.
12. IP hanya muncul jika **semua** mata kuliah snapshot memiliki SKS dan huruf efektif. Mata kuliah belum lengkap menghalangi IP; D/E tetap masuk penyebut. Label simulasi dipakai jika masih ada asumsi nilai/kehadiran.
13. Duplikasi skenario menghasilkan salinan independen; ubah nama/estimasi/target, Simpan, buka lagi. Hapus skenario tidak menghapus aktual.
14. Pilih skenario pembanding dan **Bandingkan tersimpan**. Tabel menampilkan nama/SKS/huruf efektif dan kedua IP; perubahan harus disimpan dahulu. Basis berbeda/usang diberi peringatan historis.
15. Restart/login lagi: aktual, mode, target, kunci, snapshot dan skenario tetap. Jika simpan gagal, form tetap dan navigasi dengan Simpan yang gagal ditahan.

Target huruf berarti **minimum ambang bawah** PRD, bukan janji mendapat tepat huruf tersebut. Saran aman dibulatkan ke atas2desimal; konversi huruf dan keputusan target IP tidak memakai pembulatan tampilan. Target IP bersifat pembanding manual, tanpa pencarian kombinasi antar-matkul/IPK.

Snapshot yang usang tetap dapat dibaca/digandakan sebagai historis. Refresh mengubah draf saja; tidak mengganti snapshot tersimpan sampai Simpan. Menghapus course mempertahankan snapshot lama bertanda usang; menghapus semester menghapus semua skenario semester itu setelah konfirmasi.

## Tes dan bukti

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd '-Dtest=GradePlanningPersistenceTest,GradeMigrationTest' test
.\mvnw.cmd '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck' '-DreuseForks=false' verify
```

Tambahkan parameter cache di atas jika memakai cache sesi ini. Default **138 tes**:137 baseline +1regresi verifikasi kehadiran. GUI opt-in **7 tes**; fork terpisah karena tiap kelas menutup toolkit. Hasil final dan perintah persis dicatat pada [status pengembangan](docs/DEVELOPMENT_STATUS.md). Laporan di target/surefire-reports; gambar di target/verification. Tes GUI meluncurkan aplikasi asli dengan SQLite sementara, termasuk kegagalan simpan yang sengaja disisipkan.

Pemeriksaan otomatis dan tinjauan gambar bukan uji usability mahasiswa. Ukuran jendela target1366×768 dan skala proses JavaFX125% diperiksa tanpa mengubah Windows; benchmark30×20 diukur dan paket mandiri diuji terpisah. Komputer kedua, skala Windows125% nyata, jaringan fisik diputus dan usability masih perlu hasil pengguna. Warning JavaFX unnamed module/unchecked cast pada kode tes dicatat; bukan kegagalan tes.

## Membaca kode

- `domain`: model immutable dan validasi; AssessmentComponent/ActualCourse untuk aktual, ScenarioCourse/SemesterScenario untuk snapshot dan asumsi.
- `service`: GradeCalculator, TargetPlanner dan SemesterGpaService M1 tetap; GradePlanningService mengoordinasikan evaluasi, review/refresh dan repository. AuthService/AcademicService/LearningService tetap.
- `persistence`: SqliteGradeRepository menyimpan data aktual dan snapshot pada tabel terpisah, query berparameter + owner dari UserSession; GradeSchema migrasi2→3; SqliteDatabase transaksi/rollback.
- `ui`: GradeView, ActualGradePane, ScenarioPane dan GradeForms; UiTasks memindahkan pekerjaan JDBC/hash ke worker. Handler hanya menangkap input/memanggil service/menampilkan hasil.

`record` adalah class data immutable. `Optional.empty()` berarti belum ada nilai, bukan0. Semua nilai/bobot/SKS memakai BigDecimal. Tidak ada Spring, ORM, FXML atau dependency aplikasi baru pada M4.

## Dokumen dan checkpoint

- [Checklist ID kebutuhan PRD](docs/REQUIREMENTS.md)
- [Rencana milestone](docs/IMPLEMENTATION_PLAN.md)
- [Status dan verifikasi](docs/DEVELOPMENT_STATUS.md)
- [Arsitektur dan class](docs/ARCHITECTURE.md)
- [Backlog](docs/BACKLOG.md)
- [Status historis M1](docs/MILESTONE_1_STATUS.md), [M2](docs/MILESTONE_2_STATUS.md), [M3](docs/MILESTONE_3_STATUS.md)
- [Konvensi proyek](AGENTS.md)

Checkpoint M4: [cf37dbe](https://github.com/rahadyand/Tutup-Buku/commit/cf37dbea4e0de35b729808ef130e21c065c9f04a). Checkpoint M3: [04d580d](https://github.com/rahadyand/Tutup-Buku/commit/04d580d61473596f9a39f95dafee21c8a6265dcf), main. M4 sudah di-push dan hash baseline diperiksa sebelum M5; checkpoint M5 dilakukan setelah gerbang lokal lulus, dengan hasil pada DEVELOPMENT_STATUS/Git. M5 menyiapkan kandidat lokal dan lembar penerimaan; tidak melanjutkan fitur baru atau laporan akademik otomatis. Starter lama `src/App.java` dan salinan konfigurasi awal tetap dipertahankan.

Stack terkunci Java21, JavaFX21.0.10, SQLite JDBC3.51.0.0, CommonMark0.24.0+GFM tables, JUnit5.12.2. Lisensi dan rujukan: [OpenJFX](https://openjfx.io/openjfx-docs/) GPLv2+Classpath Exception, [Maven Wrapper](https://maven.apache.org/tools/wrapper/) Apache2.0, [SQLite JDBC](https://github.com/xerial/sqlite-jdbc) Apache2.0, [CommonMark](https://github.com/commonmark/commonmark-java) BSD2-Clause, JUnit EPL2.0. Sertakan pemberitahuan lisensi saat distribusi M5.

## Paket Windows M5

Penerima mengekstrak seluruh ZIP lalu membuka **TutupBuku/TutupBuku.exe**. Java, Maven dan IDE tidak diperlukan. Data normal tetap di %LOCALAPPDATA%/TutupBuku; folder paket boleh diganti tanpa memindahkan data. Override TUTUP_BUKU_DATA_DIR tetap untuk demo terisolasi.

[Petunjuk pengguna](docs/USER_GUIDE.md), [catatan rilis/batas](docs/RELEASE_NOTES.md), [checklist penerimaan](docs/RELEASE_ACCEPTANCE.md), [demo9menit](docs/DEMO_SCRIPT.md), [inventaris lisensi](docs/THIRD_PARTY_NOTICES.md) dan [tinjauan integrasi](docs/INTEGRATION_REVIEW.md).

Untuk menghasilkan paket dari source pada Windows x64:
```powershell
.\scripts\Prepare-PackagingTools.ps1
.\scripts\Build-Windows.ps1
# Setelah cache terisi, build berikutnya dapat tanpa unduhan:
.\scripts\Build-Windows.ps1 -Offline
```

Script mengunci Temurin21.0.13+9 dan SDK OpenJFX21.0.10 beserta SHA256 pada .tools, tidak mengubah Java global. Internet hanya diperlukan untuk persiapan alat/dependency pertama. Runtime Temurin dipakai untuk distribusi; Java aplikasi tetap21. Perbedaan patch dari OracleJDK21.0.12 baseline terdokumentasi dan seluruh regresi diulang pada Temurin.

Build-Windows memakai profil Maven windows-package (dependency-plugin3.8.1, runtime scope), menjalankan verify, membuat runtime jlink dan app-image jpackage, menyalin guide/legal, mengaudit file, lalu ZIP+checksum. Folder keluaran **releases/TutupBuku-0.5.0-rc1-TANGGAL-WAKTU/** selalu baru, tidak menghapus/menimpa rilis sebelumnya. releases/ diabaikan Git. Tidak ada upload GitHub Release otomatis.

`-SkipBuild` hanya untuk mengemas ulang dokumentasi/source yang sudah diverifikasi dengan JAR/runtime-libs yang tersedia; bukan pengganti tes. Lihat BUILD-INFO dan SHA256SUMS dalam image, serta .zip.sha256 di sebelah ZIP. Sumber/tes/JDK pengembang/cache/database/aset pengguna tidak dibawa.

Uji khusus:
```powershell
.\mvnw.cmd '-Dtest=CalculationPerformanceCheck' test
.\mvnw.cmd '-Dtest=MilestoneFiveFxCheck' '-DargLine=-Dglass.win.uiScale=125%' verify
```
Tambahkan JAVA_HOME/MAVEN_USER_HOME dan parameter cache seperti bagian setup. Skala di atas berlaku pada proses tes, bukan pengaturan layar global. Bukti terakhir, lokasi artefak dan kondisi pemeriksaan launcher dicatat di DEVELOPMENT_STATUS.

Audit ulang ZIP tanpa mengubah paket:
```powershell
.\scripts\Verify-WindowsPackage.ps1 -Zip 'releases\NAMA_FOLDER\TutupBuku-0.5.0-rc1-windows-x64.zip'
# Tambahkan -Launch untuk menjalankan EXE ekstraksi pada data sementara baru,
# JAVA_HOME/MAVEN_HOME kosong dan PATH terbatas. Tutup jendela agar script selesai.
```
Script mengekstrak ke folder berspasi baru, memeriksa manifest hash/isi/JAR/runtime. Jangan menganggap audit ini menggantikan pengujian GUI, jaringan fisik atau komputer kedua.
