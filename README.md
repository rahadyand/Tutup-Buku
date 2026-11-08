# Tutup Buku — Milestone 2

Aplikasi desktop Java untuk final project PBO semester 3, mahasiswa Fakultas Teknologi Informasi dan Sains Data UNS. Tenggat produk: **8 Desember 2026**. UI dan penjelasan berbahasa Indonesia.

M2 menyediakan **akun lokal, profil, CRUD semester dan mata kuliah, pilihan semester aktif, serta SQLite persisten**. Mesin hitung M1 dipertahankan. Dashboard tetap mempunyai dua pintu setara: Modul Belajar dan Simulasi Nilai. Materi/editor masih dijadwalkan M3; simulasi di layar masih contoh tetap M1, belum memakai mata kuliah yang disimpan. Komponen penilaian dan skenario tersimpan masuk M4.

## Setup Windows

JDK **21** diperlukan, bukan hanya JRE. Lingkungan terverifikasi: Oracle JDK21.0.12 Windows x64. Maven global tidak diperlukan; Wrapper3.3.4 mengunci Maven3.9.11 beserta SHA-256.

Buka PowerShell di folder proyek:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
.\mvnw.cmd javafx:run
```

Sesuaikan JAVA_HOME jika lokasi JDK berbeda; jangan tambahkan bin ke JAVA_HOME. Build pertama memerlukan internet untuk Maven/dependensi. Aplikasi inti M2 tidak memerlukan internet, email, akun cloud atau layanan berbayar.

Hasil build: `target/tutup-buku-0.2.0-SNAPSHOT.jar`. Ini belum paket mandiri/fat JAR: gunakan **javafx:run**, bukan java -jar. Paket Windows dengan runtime dan uji komputer kedua tetap M5.

## Memakai cache sesi pengembangan ini

Cache sesi Codex berada di proyek agar tidak menulis ke cache global. Untuk menjalankan sekarang tanpa mengunduh ulang:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
$env:MAVEN_USER_HOME = "$PWD\.tools\maven-user"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o javafx:run
```

Jika menyalin source tanpa cache, hilangkan -o pada build pertama. Folder .m2/, .tools/, target/, tmp/ dan database/log diabaikan Git; .mvn/wrapper/ serta skrip Wrapper tetap source yang dilacak.

## Lokasi data dan akun

Default Windows: **%LOCALAPPDATA%\TutupBuku\tutup-buku.db**. Database dibuat saat aplikasi dibuka, di luar folder instalasi. Jika LOCALAPPDATA tidak tersedia, fallback adalah user.home/AppData/Local/TutupBuku. Tidak ada login otomatis setelah restart: pilihan semester aktif tetap tersimpan, sesi login tidak.

Untuk mencoba pada folder demo terpisah:

```powershell
$env:TUTUP_BUKU_DATA_DIR = "$PWD\tmp\demo-m2"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o javafx:run
```

Untuk kembali ke lokasi normal pada PowerShell yang sama:

```powershell
Remove-Item Env:TUTUP_BUKU_DATA_DIR -ErrorAction SilentlyContinue
```

Perintah ini hanya menghapus variabel lingkungan sesi, **tidak menghapus file database**. Jendela yang dibuka untuk verifikasi pengembang menggunakan tmp/m2-launch-check, bukan database AppData Anda. Tutup jendela itu sebelum membuka sesi normal.

Password disimpan sebagai PBKDF2-HMAC-SHA256 bersalt acak, bukan plaintext. Hash256bit, salt16byte, 600.000 iterasi serta nama algoritme disimpan agar verifikasi dapat diulang. Ini memakai implementasi Java, sesuai pilihan PRD dan parameter [OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html). File database tidak dienkripsi seluruhnya; orang yang dapat mengakses file melalui akun Windows yang sama dapat membaca data profil/akademik. Tidak ada recovery password/email atau password universal.

## Alur mencoba M2 secara manual

1. Buka tab **Daftar**, isi NIM (misalnya 001234), nama, password minimal8 karakter dan konfirmasinya. NIM disimpan sebagai teks setelah trim; tidak diasumsikan panjang/pola kampus.
2. Setelah berhasil daftar, **Masuk**. Coba password salah terlebih dahulu: pesan generik muncul tanpa mengubah data.
3. Buka **Profil**, edit nama/program studi lalu Simpan. NIM tidak dapat diubah. Program studi teks bebas; boleh kosong sampai dilengkapi.
4. Buka **Semester & mata kuliah**, klik Semester baru. Isi nomor positif, tahun akademik seperti2026/2027 dan Ganjil/Genap, lalu Simpan semester. Tahun akhir harus tepat satu setelah tahun awal. Kombinasi tahun/periode unik dalam akun.
5. Pilih semester dan **Jadikan aktif**. Melihat semester lain tidak otomatis mengganti semester aktif.
6. Klik **Mata kuliah baru**. Isi nama, kode opsional dan SKS positif. Kosong/0/negatif ditolak; koma/titik desimal diterima. Checkbox kehadiran awal dicentang dan berlabel asumsi belum diverifikasi; acuan dapat diedit, tanpa absensi otomatis.
7. Pilih item pada daftar untuk mengedit, lalu Simpan. Jika simpan gagal, isian tetap ada: periksa penyimpanan dan klik **Simpan** lagi. Muat ulang daftar merupakan aksi terpisah.
8. Buat semester kedua, ubah semester aktif, lalu kembali ke semester pertama: mata kuliah lama tetap tersedia. Hapus memunculkan konfirmasi; Batal tidak mengubah data. Hapus semester menghapus mata kuliahnya; jika aktif, referensi aktif dikosongkan tanpa memilih pengganti otomatis.
9. Keluar, daftar akun kedua dan masuk: data akun pertama tidak terlihat.
10. Tutup aplikasi dan jalankan lagi. Login akun pertama: profil, semester, mata kuliah, kehadiran dan pilihan aktif tetap tersedia.

Tombol simpan/hapus berada di bagian bawah panel; isi form panjang dapat digulir. Error penyimpanan ditampilkan, tanpa reset data atau status sukses palsu. Operasi hash/JDBC berjalan pada worker; selama proses, form dinonaktifkan dan penutupan jendela menunggu proses selesai.

Dashboard Modul Belajar berlabel belum tersedia. Simulasi Nilai mempunyai contoh 60→55→70 dan checkbox demo C→E→C dari M1; **contoh itu terpisah dari checkbox kehadiran course yang tersimpan**.

## Pengujian otomatis

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd '-Dtest=LocalAccountTest,AcademicPersistenceTest,DatabaseSafetyTest' test
```

Default: **96 tes lulus** (70 M1 dan26 M2), tanpa kegagalan/error/skipped pada verifikasi akhir. Tes persistence memakai SQLite nyata di @TempDir, bukan database pengguna.

Pengujian JavaFX opsional memerlukan desktop aktif:

```powershell
.\mvnw.cmd '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck' '-DreuseForks=false' test
```

Total **3 tes GUI lulus**: satu regresi shell M1 dan dua alur M2. Fork terpisah diperlukan karena tiap kelas menutup toolkit JavaFX. Test M2 membuat data fiktif di @TempDir, memeriksa registrasi/login/profil/akademik/logout/restart, batal/hapus, isolasi akun, input tetap pada simpan gagal dan startup database rusak. Snapshot terdapat di target/verification/m2-login.png dan m2-academic.png; laporan XML/TXT di target/surefire-reports.

Tambahkan '-Dmaven.repo.local=.m2/repository' dan -o untuk memakai cache lokal sesi ini. Warning JavaFX `classes were loaded from 'unnamed module'` pada test classpath tercatat; pemeriksaan akhir tidak mencetak exception toolkit. GUI test otomatis dan peninjauan gambar **bukan uji usability manual mahasiswa**. Pengujian jaringan benar-benar diputus, skala125%, komputer kedua, paket mandiri dan benchmark tetap belum dilakukan.

## Membaca kode

- domain/: Score/AssessmentItem/LetterGrade/Attendance serta UserProfile/Semester/Course.
- service/: kalkulasi M1 tetap murni; AuthService dan AcademicService memakai interface repository, UserSession menyimpan NIM sesi.
- security/: PasswordHasher dan PasswordHash.
- persistence/: AccountRepository/AcademicRepository, SqliteRepository (SQL berparameter), SqliteDatabase (FK/transaksi/schema versi1), AppPaths.
- ui/: TutupBukuApplication, AuthView, WorkspaceView, AcademicView, DashboardView, UiTasks.
- src/test/: kalkulasi M1, integration SQLite dan opt-in GUI.

`record` adalah class data immutable; constructor memvalidasi input. `Optional.empty()` berarti belum ada nilai/referensi, bukan angka0. SKS disimpan sebagai teks desimal dari BigDecimal; kehadiran memakai model M1. Referensi aktif memakai FK owner+semester agar tidak menunjuk semester akun lain.

## Dokumentasi dan batas milestone

- [Kebutuhan dan ID PRD](docs/REQUIREMENTS.md)
- [Rencana milestone](docs/IMPLEMENTATION_PLAN.md)
- [Status pengembangan/bukti verifikasi](docs/DEVELOPMENT_STATUS.md)
- [Arsitektur dan penjelasan class](docs/ARCHITECTURE.md)
- [Backlog lengkap](docs/BACKLOG.md)
- [Status historis M1](docs/MILESTONE_1_STATUS.md)
- [Konvensi proyek](AGENTS.md)

Source starter M1 dan riwayat commit tidak ditulis ulang. Salinan README/settings starter tetap di docs. Perubahan M2 tidak di-commit atau di-push otomatis. Milestone berikutnya **M3 belajar lokal**, belum dimulai.

Stack tetap Java21, JavaFX21.0.10, SQLite JDBC3.51.0.0, CommonMark0.24.0+GFM tables dan JUnit5.12.2; tanpa framework baru. Referensi primer: [OpenJFX](https://openjfx.io/openjfx-docs/), [Maven Wrapper](https://maven.apache.org/tools/wrapper/), [SQLite FK](https://www.sqlite.org/foreignkeys.html), [SQLite user_version](https://www.sqlite.org/pragma.html#pragma_user_version), [CommonMark](https://github.com/commonmark/commonmark-java). CommonMark BSD2-Clause; SQLite JDBC Apache2.0; OpenJFX GPLv2+Classpath Exception; JUnit EPL2.0; Maven Apache2.0. Sertakan pemberitahuan lisensi komponen saat distribusi M5.
