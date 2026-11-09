# Tutup Buku — Milestone 3

Aplikasi desktop Java untuk final project PBO semester 3, mahasiswa Fakultas Teknologi Informasi dan Sains Data UNS. Tenggat produk: **8 Desember 2026**. UI dan penjelasan berbahasa Indonesia.

M3 menambahkan **materi Markdown, gambar lokal, progres manual, catatan dan lanjut membaca** di atas akun/akademik M2. Dua pintu dashboard tetap setara: Modul Belajar dan Simulasi Nilai. Mesin hitung M1 tetap; editor nilai dan skenario tersimpan masih M4.

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

Sesuaikan JAVA_HOME jika lokasi JDK berbeda; jangan tambahkan bin ke JAVA_HOME. Build pertama memerlukan internet untuk Maven/dependensi. Aplikasi inti M3 tidak memerlukan internet, email, akun cloud atau layanan berbayar.

Hasil build: `target/tutup-buku-0.3.0-SNAPSHOT.jar`. Ini belum paket mandiri/fat JAR: gunakan **javafx:run**, bukan java -jar. Paket Windows dengan runtime dan uji komputer kedua tetap M5.

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

Perintah ini hanya menghapus variabel lingkungan sesi, **tidak menghapus file database**. Jendela yang dibuka untuk verifikasi pengembang menggunakan tmp/m3-launch-check, bukan database AppData Anda. Tutup jendela itu sebelum membuka sesi normal.

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

Dashboard Modul Belajar sekarang membuka fitur M3. Simulasi Nilai mempunyai contoh 60→55→70 dan checkbox demo C→E→C dari M1; **contoh itu terpisah dari checkbox kehadiran course yang tersimpan**.

## Pengujian otomatis

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd '-Dtest=LocalAccountTest,AcademicPersistenceTest,DatabaseSafetyTest' test
```

Default: **118 tes lulus** (96 M1/M2 dan22 M3), tanpa kegagalan/error/skipped pada verifikasi akhir. Tes persistence memakai SQLite nyata di @TempDir, bukan database pengguna.

Pengujian JavaFX opsional memerlukan desktop aktif:

```powershell
.\mvnw.cmd '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck' '-DreuseForks=false' test
```

Total **5 tes GUI lulus**: satu regresi shell M1, dua alur M2, satu reader aman dan satu alur lengkap M3. Fork terpisah diperlukan karena tiap kelas menutup toolkit JavaFX. Test M2 membuat data fiktif di @TempDir, memeriksa registrasi/login/profil/akademik/logout/restart, batal/hapus, isolasi akun, input tetap pada simpan gagal dan startup database rusak. Snapshot terdapat di target/verification/m2-login.png dan m2-academic.png; laporan XML/TXT di target/surefire-reports.

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
- [Status historis M2](docs/MILESTONE_2_STATUS.md)
- [Konvensi proyek](AGENTS.md)

Source starter M1 dan riwayat commit tidak ditulis ulang. Salinan README/settings starter tetap di docs. Checkpoint M2 sudah di-push atas permintaan pengguna sebelum M3: [0ca1b28](https://github.com/rahadyand/Tutup-Buku/commit/0ca1b2899bbfff9c057f20f0ba984a807e9a7e2a), branch main. Perubahan M3 tidak di-commit/push. Milestone berikutnya **M4 nilai dan skenario**, belum dimulai.

Stack tetap Java21, JavaFX21.0.10, SQLite JDBC3.51.0.0, CommonMark0.24.0+GFM tables dan JUnit5.12.2; tanpa framework baru. Referensi primer: [OpenJFX](https://openjfx.io/openjfx-docs/), [Maven Wrapper](https://maven.apache.org/tools/wrapper/), [SQLite FK](https://www.sqlite.org/foreignkeys.html), [SQLite user_version](https://www.sqlite.org/pragma.html#pragma_user_version), [CommonMark](https://github.com/commonmark/commonmark-java). CommonMark BSD2-Clause; SQLite JDBC Apache2.0; OpenJFX GPLv2+Classpath Exception; JUnit EPL2.0; Maven Apache2.0. Sertakan pemberitahuan lisensi komponen saat distribusi M5.

## Mencoba fitur M3
1. Masuk dengan akun lokal. Buat semester/course PBO melalui tab akademik jika belum ada, lalu buka Modul Belajar dan Muat ulang daftar.
2. Jalur **Kuliah**: pilih Semester dan Mata kuliah, isi nama bab lalu Tambah. Jalur **Peminatan**: pilih Peminatan, isi nama bidang lalu Tambah; bidang ini tidak bergantung semester/course.
3. Atau pilih course lalu **Salin5materi contoh (sekali)** dan konfirmasi:3pengantarPBO masuk course pilihan,2pengantar peminatan masuk bidang contoh. Ini koleksi asli yang singkat, bukan course lengkap. Tombol tidak menggandakan atau menimpa contoh pada login berikutnya.
4. Pilih bab/bidang dan **Materi baru**. Pada tab **Tulis**, isi judul, sumber opsional dan Markdown. Heading, list, bold/italic, tabel dan fenced code tersedia. Isi kosong boleh disimpan sebagai Draf; judul tetap wajib.
5. Buka **Pratinjau/Baca** untuk membaca. Kode ditampilkan sebagai teks; HTML/script raw tidak dieksekusi. Ringkasan ditulis manual sebagai heading `## Ringkasan`, bukan generator.
6. **Sisipkan gambar lokal** membuka chooser PNG/JPEG. File didekode, dibatasi10MB/16juta piksel, lalu disalin ke folder assets akun. Ref relatif dimasukkan ke markdown. Gambar asal boleh dipindah setelah salinan berhasil; rumus menggunakan gambar. Aset hilang/ditolak menampilkan placeholder bernama.
7. Isi **Catatan pribadi** yang terpisah dari markdown. Simpan materi & catatan atau tunggu autosave2detik. Status menunjukkan Belum tersimpan/Menyimpan/Tersimpan/Gagal menyimpan. Jika gagal, teks tetap ada; perbaiki masalah lalu klik Simpan lagi.
8. Tandai **Selesai** secara manual. Membuka materi tidak menandainya selesai. Progres course memakai semua materi dari semua bab:2selesai dari2+8materi berarti20%, bukan50%. Empty collections berlabel Belum ada materi.
9. Gunakan Naik/Turun untuk urutan bab/bidang/materi. Catatan dan reading revision tetap. Hapus memberi konfirmasi turunan yang ikut dihapus; aset tidak dibersihkan otomatis.
10. Gulir saat membaca, pindah layar/logout/tutup aplikasi, lalu buka lagi dan **Lanjutkan Membaca**. Posisi dipulihkan setelah render. Jika isi berubah, kembali ke awal; materi dihapus menghasilkan penjelasan dan daftar yang masih tersedia.
11. Meninggalkan editor yang belum tersimpan memunculkan **Simpan / Buang perubahan / Batal**, termasuk saat logout atau close. Simpan yang gagal tidak membawa Anda meninggalkan editor. Batal mempertahankan isian.
12. Buat akun kedua untuk memeriksa materi/catatan/progres terpisah; jika menyalin contoh, salinannya dapat diedit secara independen.

## Data M3 dan keamanan reader
Schema SQLite otomatis naik1→2 secara atomik, tanpa reset akun/profil/semester/course M2. Tidak ada seed otomatis saat login/migrasi. Aset berada pada dataDirectory/assets/{SHA256-NIM}/{UUID}.png/jpg; database menyimpan referensi relatif. Cadangan manual perlu menyalin DB **dan** assets. Tidak ada cleanup aset agresif; gambar yang tidak dirujuk bisa tersisa.

CommonMark+GFM tables tetap pustaka yang sama; tambahan runtime hanya javafx-web21.0.10. Script halaman WebView dimatikan dan tidak ada Java bridge. Gambar eksternal, file URL, traversal path dan aset akun lain ditolak. Link http/https dibuka di browser hanya setelah klik; koneksi internet mungkin diperlukan. Perintah internal pembacaan/restore scroll tidak pernah menerima kode dari markdown.

Tes baru: LearningMigrationTest2, LearningPersistenceTest12 dan AssetsAndMarkdownTest8. Tes GUI M3 mencakup autosave, perubahan belum tersimpan, save failure, progres/note/resume/restart, aman dari script dan link eksplisit. Snapshot: target/verification/m3-learning.png. Seluruh database dan aset tes terisolasi. Dialog file chooser pemilihan gambar, usability, skala125%, jaringan fisik diputus dan paket komputer kedua belum diuji manual; decoder/copy/ownership/render gambar diuji otomatis.
