# Status pengembangan

Tanggal: 8 November 2026 (Asia/Jakarta). **M1 Fondasi selesai; berhenti pada batas milestone.**

## Selesai

- Inspeksi workspace: starter Hello World `src/App.java`, README dan pengaturan VS Code dipertahankan (salinan README/settings di docs).
- PRD dibaca lengkap 25 halaman; halaman tabel nilai/solver/contoh juga ditinjau sebagai gambar.
- JDK Oracle21.0.12 Windows x64 terdeteksi; Maven tidak tersedia di PATH.
- Checklist kebutuhan, plan, backlog dan konvensi proyek disiapkan.
- Maven Wrapper resmi 3.3.4 mengunci Maven 3.9.11 beserta SHA-256; JDK release21, JavaFX21.0.10, SQLite JDBC3.51.0.0, CommonMark0.24.0+tables, JUnit5.12.2 dan plugin build dikunci.
- Shell JavaFX bahasa Indonesia dengan dua pintu setara: Modul Belajar (placeholder) dan Simulasi Nilai (contoh tetap PRD). Nilai60→55→70 dan checkbox kehadiran dapat dicoba.
- Model immutable: Score, AssessmentItem, ScenarioAssumptions, LetterGrade, Attendance serta hasil nilai/target/mata kuliah/IP.
- GradeCalculator, TargetPlanner, SemesterGpaService dan DecimalInput murni; aktual/asumsi terpisah, bobot total tepat100, missing≠0, override kehadiran reversible, IP lengkap wajib.
- Pecahan internal BigDecimal mempertahankan keputusan ambang eksak. Saran aman CEILING2desimal; tampilan IP2desimal tidak dipakai memutuskan target.
- README setup/build/test/run, penjelasan class dan salinan konfigurasi awal tersedia.
- Checkpoint Git lokal M1: repository baru diinisialisasi karena sebelumnya belum ada `.git`. Aturan ignore meliputi hasil build, `.m2/`, `.tools/`, database/journal SQLite, log dan kredensial lokal; `.mvn/` tetap dilacak. Starter `src/App.java` dipertahankan sebagai berkas untracked di luar checkpoint. Tidak ada push atau perubahan logika aplikasi dalam pekerjaan checkpoint ini.

## Verifikasi

- `java -version` dan `javac -version` melalui path JDK: 21.0.12.
- Maven pertama diunduh dari Maven Central ke `.tools`; DNS sandbox awalnya gagal, unduhan diulang dengan izin jaringan. Tidak ada perubahan instalasi global. Cache lokal `.m2/repository` dan `.tools/maven-user` diabaikan Git.
- Semua perintah di bawah memakai `$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.12'` dan `$env:MAVEN_USER_HOME="$PWD\.tools\maven-user"`.

| Perintah / pemeriksaan | Hasil aktual |
|---|---|
| `.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -B -ntp clean verify` | BUILD SUCCESS; 70 tests, failures0, errors0, skipped0; JAR terbentuk |
| `.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp verify` setelah perubahan akhir UI | BUILD SUCCESS; 70 tests, failures0, errors0, skipped0; JAR diperbarui |
| `.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -B -ntp javafx:run` | Jendela Windows “Tutup Buku — Milestone 1” terbuka; teks contoh60/55/70 ditemukan melalui accessibility |
| `.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck' test` | BUILD SUCCESS; 1 test, failures0, errors0, skipped0; Stage terbuka, kedua tombol navigasi dan hitung dipicu, checkbox C→E→C dan nilai asal60/C diverifikasi |
| Snapshot scene dari smoke test | `target/verification/javafx-shell.png` ditinjau: judul, dua pintu, contoh, checkbox dan hasil terbaca, tidak terpotong pada1000×690 |

70 tes default: GradeCalculatorTest43, TargetPlannerTest15, SemesterGpaServiceTest10, StackCompatibilityTest2. Laporan XML/TXT aktual ada di `target/surefire-reports`; smoke test GUI1 dijalankan terpisah, tidak masuk total70.

MT01–13 pada domain dicakup:8/10=80;35/40=87,5;60→55→70;153,333… mustahil/maks44;target minimum0/minal72;W=0 tercapai/belum;99,99/100,01 ditolak;seluruh ambang55/60/65/70/75/80/85 termasuk84,5 A-;IP36/11 dan30/11;huruf langsung/attendance;56,67 aman vs56,66→69,996/C+;semester kosong/SKS salah/hasil hilang. Tambahan: nol aktual vs kosong, bobot0, immutable assumptions, konflik aktual/asumsi, ID duplikat/hilang, koma desimal, presisi pecahan dan target sedikit di atas batas. MT14 hanya contoh flat setara84,5/16,9, **bukan implementasi grouped**.

Smoke JDBC hanya database in-memory dan query berparameter mempertahankan NIM teks. Smoke CommonMark hanya render tabel/kode/gambar/link dan escape HTML. Native driver Windows termuat. Ini tidak membuktikan persistence pengguna atau reader lengkap.

Catatan verifikasi: percobaan GUI test pertama menutup toolkit terlalu dini dan mencetak exception walau assertion lulus; urutan startup/shutdown test diperbaiki lalu diulang. Run terakhir tidak mencetak exception, hanya warning JavaFX classpath `unnamed module` pada test. Screenshot helper desktop tidak dipakai sebagai bukti visual karena jendela tertutup aplikasi lain; bukti visual final berasal dari snapshot scene milik test sendiri. Ini bukan uji usability manual pengguna.

## Sisa / batas

Seluruh M2–M5 belum dikerjakan. Tidak ada login/hash password, akun tersimpan, schema/repository SQLite, editor Markdown/aset, catatan/resume atau skenario tersimpan pada M1. Tidak ada grouped weighting. AT end-to-end, ketahanan restart data, kegagalan simpan, jaringan benar-benar diputus,1366×768/125%,benchmark30×20,komputer kedua dan paket jpackage belum diuji. Build offline berarti dependensi tersedia di cache; belum mengklaim seluruh produk P0 offline selesai.

Milestone berikutnya **M2 akun/akademik lokal**: PBKDF2 bersalt beserta parameter yang diverifikasi saat implementasi, NIM teks unik/isolasi pemilik, profil, semester aktif dan CRUD mata kuliah, SQLite transaksi/query berparameter, AT01–04 dan restart. Menunggu pengguna mencoba dan memahami M1; tidak dimulai otomatis.
