# Tutup Buku — Milestone 1

Aplikasi desktop Java untuk final project PBO semester 3, mahasiswa Fakultas Teknologi Informasi dan Sains Data UNS. UI dan dokumentasi berbahasa Indonesia. Tenggat produk: **8 Desember 2026**.

Milestone ini menyediakan shell JavaFX dengan dua area setara (Modul Belajar dan Simulasi Nilai), contoh perencanaan bertahap, serta engine nilai/target/IP yang diuji otomatis. Data contoh di layar tidak disimpan. Akun, SQLite untuk data pengguna, editor materi dan skenario tersimpan masuk milestone berikutnya.

## Persiapan Windows

1. Pasang **JDK 21** (bukan hanya JRE). Lingkungan pengembangan terverifikasi: Oracle JDK **21.0.12**, Windows x64. Jangan memasukkan folder `bin` ke `JAVA_HOME`.
2. Buka PowerShell di folder proyek. Atur path sesuai instalasi lokal, misalnya:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
.\mvnw.cmd -version
```

Maven global tidak diperlukan. Wrapper resmi versi3.3.4 mengunduh Maven **3.9.11** dengan checksum SHA-256. Build pertama memerlukan internet untuk Maven/dependensi; setelah cache lengkap, core shell dan engine berjalan tanpa internet. Tidak ada API AI, hosting atau akun cloud.

## Build, test, run

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd javafx:run
```

`verify` menjalankan seluruh JUnit lalu membuat `target/tutup-buku-0.1.0-SNAPSHOT.jar`. JAR ini belum paket mandiri atau fat JAR; **jalankan lewat `javafx:run`**, bukan `java -jar`. Paket Windows dengan runtime dibuat pada M5. Tutup jendela untuk menghentikan `javafx:run`.

Untuk menjalankan satu kelompok tes:

```powershell
.\mvnw.cmd '-Dtest=TargetPlannerTest' test
```

Smoke test JavaFX opsional memerlukan desktop aktif. Test membuka jendela sementara, mencoba navigasi dan kehadiran, lalu menutupnya sendiri:

```powershell
.\mvnw.cmd '-Dtest=JavaFxSmokeCheck' test
```

Gambar hasil ada di `target/verification/javafx-shell.png`. Tes ini tidak termasuk 70 tes default agar build domain tidak membutuhkan GUI. Pada smoke test classpath, JavaFX mengeluarkan warning `classes were loaded from 'unnamed module'`; peluncuran aplikasi normal memakai plugin JavaFX.

Setelah perintah online di atas pernah berhasil, coba mode Maven offline:

```powershell
.\mvnw.cmd -o verify
.\mvnw.cmd -o javafx:run
```

Cache default Maven ada di `%USERPROFILE%\.m2`. Pada sesi Codex ini cache dibuat di proyek agar tidak mengubah folder pengguna. Untuk memakai ulang cache sesi ini, gunakan:

```powershell
$env:MAVEN_USER_HOME = "$PWD\.tools\maven-user"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o javafx:run
```

Jika cache tidak disertakan saat menyalin proyek, hilangkan `-o` untuk mengunduh sekali. Folder `.tools`, `.m2`, `tmp`, dan `target` adalah hasil lokal yang diabaikan Git; bukan bagian source. Wrapper ada di `.mvn/wrapper`, `mvnw.cmd`, dan `mvnw`.

## Mencoba shell

- `Buka modul belajar`: menampilkan jalur kuliah dan peminatan serta batas milestone. Reader/editor belum dibuat.
- `Buka simulasi nilai`: tekan `Jalankan contoh PRD`. Hasil berurutan: semua bebas60; Tugas terkunci80→UTS/UAS55; tambah UTS aktual30→UAS70.
- Hilangkan centang kehadiran: hasil contoh lengkap60/C menjadi efektifE. Centang lagi: kembaliC. Nilai asal tetap60/C. Kehadiran masih berlabel asumsi.

UI M1 memakai data contoh tetap untuk memudahkan pemeriksaan engine. Form komponen/target bebas dan akun belum tersedia. Kasus mustahil, konversi84,5, dan IP diuji melalui JUnit; lihat test dan laporan `target/surefire-reports`.

## Membaca kode

Urutan yang disarankan: `Score` → `AssessmentItem` + `ScenarioAssumptions` → `LetterGrade` → `GradeCalculator` → `TargetPlanner` → `SemesterGpaService` → test → UI.

```text
src/main/java/id/ac/uns/tutupbuku/
  domain/     model nilai immutable, hasil perhitungan dan status
  service/    rumus dan validasi murni; tanpa JavaFX, JDBC atau file
  ui/         Application dan contoh UI; memanggil service
src/test/java/id/ac/uns/tutupbuku/service/
  GradeCalculatorTest, TargetPlannerTest, SemesterGpaServiceTest
  StackCompatibilityTest (JDBC in-memory dan Markdown)
```

`record` adalah bentuk class Java untuk data immutable; constructor tetap memvalidasi input. `Optional.empty()` menyatakan nilai belum ada, sehingga berbeda dari skor0. Semua angka domain memakai `BigDecimal` dari String. Contoh alur dan penjelasan presisi ada di [ARCHITECTURE](docs/ARCHITECTURE.md).

`src/App.java` Hello World lama tetap utuh di luar source root Maven. README dan pengaturan VS Code lama disalin ke `docs/README-starter.md` dan `docs/vscode-settings-starter.json`. VS Code sekarang mengimpor `pom.xml`; tunggu impor Java selesai. Tidak ada data pengguna lama yang dihapus.

## Dokumen proyek

- [Checklist kebutuhan dan ID PRD](docs/REQUIREMENTS.md)
- [Milestone kecil hingga pengumpulan](docs/IMPLEMENTATION_PLAN.md)
- [Status dan bukti verifikasi aktual](docs/DEVELOPMENT_STATUS.md)
- [Seluruh backlog yang ditunda](docs/BACKLOG.md)
- [Penjelasan class dan keputusan teknis](docs/ARCHITECTURE.md)
- [Konvensi proyek](AGENTS.md)

## Stack dan referensi

Versi dikunci pada `pom.xml`: JavaFX21.0.10, SQLite JDBC3.51.0.0, CommonMark0.24.0+GFM tables, JUnit5.12.2. Plugin build juga dikunci. Ini pilihan implementasi yang kompatibel dengan JDK21 terpasang, bukan klaim versi terbaru. Tidak ada deviasi dari stack usulan PRD; integrasi data/reader ditunda sesuai batas M1.

Rujukan primer: [OpenJFX Maven](https://openjfx.io/openjfx-docs/#maven), [Maven Wrapper](https://maven.apache.org/tools/wrapper/), [CommonMark Java](https://github.com/commonmark/commonmark-java), [SQLite JDBC](https://github.com/xerial/sqlite-jdbc). CommonMark berlisensi BSD 2-Clause, SQLite JDBC Apache-2.0 (komponen SQLite memiliki pemberitahuan tersendiri), OpenJFX GPLv2 dengan Classpath Exception, JUnit EPL-2.0, Maven Apache-2.0. Sertakan pemberitahuan lisensi dependensi saat paket distribusi dibuat; konten contoh final harus mempunyai atribusi sendiri.
