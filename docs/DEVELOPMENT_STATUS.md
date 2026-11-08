# Status pengembangan — Milestone 2

Tanggal verifikasi: **8 November 2026, Asia/Jakarta**. Versi aplikasi **0.2.0-SNAPSHOT**. **M2 selesai; berhenti sebelum M3.** Tidak ada commit/push pada pekerjaan M2.

Status historis M1 dipertahankan di [MILESTONE_1_STATUS.md](MILESTONE_1_STATUS.md). PRD 25 halaman dan AGENTS/plan/checklist/arsitektur/status dibaca sebelum keputusan M2. Semua dependensi M1 tetap digunakan; tidak ada framework/layanan baru.

## Yang selesai

- Registrasi lokal NIM teks, trim, nol awal tetap, unik per perangkat; nama, konfirmasi password, minimum8 karakter. Program studi dilengkapi di profil. Tidak ada pola/panjang NIM dugaan.
- PBKDF2WithHmacSHA256 Java,600.000 iterasi, salt acak16byte, hash256bit, parameter disimpan. Verifikasi constant-time melalui MessageDigest.isEqual; buffer char[] dibersihkan. Password tidak disimpan/log sebagai plaintext.
- Login benar/salah dengan pesan generik, logout mengakhiri sesi. Profil nama/prodi editable dan NIM immutable melalui API/UI; prodi teks bebas.
- CRUD semester bernomor positif, tahun YYYY/YYYY berurutan dan periode Ganjil/Genap. Unik tahun/periode per akun. Pemilihan aktif eksplisit dan persisten; semester lama tidak dihapus.
- CRUD mata kuliah per semester: nama wajib, kode opsional, SKS positif BigDecimal, desimal koma/titik, kehadiran defaulttrue/asumsi belum diverifikasi, acuan default75%/12dari16 editable. Revision meningkat pada edit.
- Model Attendance M1 dipakai ulang; Course.withoutGrade menandai belum ada hasil sehingga tidak menghasilkan IP parsial. Mesin hitung dan tes numerik M1 tidak ditulis ulang.
- SQL terpisah melalui AccountRepository/AcademicRepository dan SqliteRepository. Query berparameter + owner_nim/ID; composite FK melindungi pemilikan referensi aktif dan course.
- SQLite schema1, initialization atomik, FK aktif, busy_timeout3detik, commit/rollback. CASCADE semester menghapus matkul/referensi aktif terkait saja; kegagalan cascade rollback seluruh perubahan.
- Database default %LOCALAPPDATA%/TutupBuku/tutup-buku.db (folder pengguna, bukan instalasi); override TUTUP_BUKU_DATA_DIR untuk demo. Semua tes memakai @TempDir atau in-memory.
- Database rusak, versi lebih baru, versi0 berisi tabel tak dikenal, atau gagal baca tidak dianggap database kosong dan tidak direset.
- JavaFX: registrasi/login, dashboard dua pilar setelah login, profil, semester/course, active marker, konfirmasi hapus dengan Batal default. Tombol simpan/hapus tetap terlihat, isi panel dapat digulir.
- Satu worker untuk hash/JDBC, status proses, UI dinonaktifkan selama operasi. Save gagal mempertahankan input; retry memakai tombol Simpan. Refresh daftar merupakan operasi terpisah, tidak mengaburkan keberhasilan commit.
- README, arsitektur, requirements, plan, backlog dan AGENTS diperbarui. Fitur tertunda tetap tercatat.

## Bukti pengujian otomatis

Lingkungan: Windows x64, Oracle JDK21.0.12, Maven Wrapper3.3.4/Maven3.9.11, JavaFX21.0.10, SQLite JDBC3.51.0.0, CommonMark0.24.0, JUnit5.12.2. Cache sudah tersedia; verifikasi menggunakan Maven offline.

Setelan perintah:
```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
$env:MAVEN_USER_HOME = "$PWD\.tools\maven-user"
```

| Perintah aktual | Hasil |
|---|---|
| `.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp clean verify` | BUILD SUCCESS;96tes,0failures,0errors,0skipped; JAR0.2.0-SNAPSHOT dibuat |
| `.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck' '-DreuseForks=false' test` | BUILD SUCCESS;3tes GUI,0failures,0errors,0skipped |
| `.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp javafx:run, dengan TUTUP_BUKU_DATA_DIR=$PWD/tmp/m2-launch-check` | Launcher dijalankan, database demo schema1 terbentuk; jendela berjudul Tutup Buku — Milestone 2 ditemukan (dalam keadaan minimized). Alur lengkap layar diverifikasi oleh GUI test terisolasi, bukan uji pengguna manual |
| git diff --check | Tidak ada whitespace error; warning konversi LF/CRLF dari konfigurasi Git Windows bukan kegagalan |
| Pemeriksaan source M1 terhadap HEAD | 20 berkas engine domain/service dan tes M1 diperiksa dan tetap sama; hanya fixture smoke GUI disesuaikan ke DashboardView setelah pemindahan shell |

Rincian 96 tes:

- M1: GradeCalculatorTest43, TargetPlannerTest15, SemesterGpaServiceTest10, StackCompatibilityTest2 =70.
- M2: LocalAccountTest8, AcademicPersistenceTest13, DatabaseSafetyTest5 =26.

Cakupan M2 otomatis:

- NIM001234 tetap teks, trim dan duplikat ditolak, konfirmasi/minimum password, login benar/salah/logout, salt/hash berbeda meski password sama, tidak ada kolom plaintext, buffer dibersihkan.
- Profil terisolasi/tersimpan dan NIM tetap; NIM berisi kutip tidak dieksekusi sebagai SQL.
- Unik semester per akun, update duplikat tidak mengubah data lama, active selection persisten setelah koneksi ditutup/dibuka, perubahan aktif tidak menghapus course.
- Akun lain tidak dapat list/update/delete semester/course atau memilih semester aktif akun pertama; FK juga menolak referensi aktif silang melalui SQL langsung.
- SKS kosong/0/negatif/nonangka ditolak; desimal2,5 diterima. Nama wajib; kode opsional; edit kehadiran/acuan/revision tersimpan dan cocok dengan engine M1.
- Hapus course/semester terkait tidak menghapus semester/akun lain. Trigger kegagalan delete child membuktikan rollback semester/course/referensi aktif.
- Mode read-only membuktikan save gagal tidak mengubah profil lama; setelah penyebab diatasi, retry berhasil.
- File corrupt, unknown schema version dan tabel legacy tidak direset/ditimpa; koneksi tertutup menghasilkan error, bukan daftar kosong sukses.
- GUI: daftar/login/duplikat/password salah/profil/dua semester/course/SKS salah/kehadiran/logout/pindah akun/restart; batal dan konfirmasi hapus; trigger save gagal mempertahankan field dan tidak menampilkan sukses palsu; startup corrupt dapat dicoba ulang tanpa mengganti file.
- Regresi shell M1 tetap memeriksa60→55→70 dan C→E→C.

Laporan XML/TXT: target/surefire-reports. Snapshot scene: target/verification/m2-login.png, m2-academic.png, javafx-shell.png. Gambar login dan akademik ditinjau pada1080×720; tidak ada teks/tombol utama terpotong, form panjang memiliki scrollbar. Snapshot berasal dari scene test sendiri, tidak menangkap aplikasi pengguna lain.

GUI test pertama gagal karena fixture mencoba hapus tanpa memilih course. Langkah fixture diperbaiki, lalu seluruh3tes GUI diulang dan lulus pada kode/tata letak akhir. Warning JavaFX classpath unnamed module tetap muncul pada tes; tidak ada exception toolkit pada run akhir.

## Pemeriksaan manual dan yang belum diverifikasi

Peninjauan visual dilakukan pada snapshot scene; aplikasi juga dijalankan melalui Maven launcher dengan folder demo terisolasi. Alur tombol/form di atas adalah **tes GUI otomatis**, bukan klaim semua langkah diklik manual atau uji usability mahasiswa. Database AppData pengguna tidak dipakai untuk tes.

Belum dilakukan: uji perangkat kedua, paket jpackage/runtime, jaringan fisik benar-benar diputus, skala125%/1366×768, benchmark30×20, uji usability3–5mahasiswa, full P0 end-to-end, keamanan reader/asset M3 dan skenario M4. Login menggunakan hash aman, tetapi database belum dienkripsi seluruhnya dan tidak ada recovery/ganti password.

## Batas dan milestone berikutnya

M3 **belajar lokal**: jalur kuliah/peminatan, editor/reader CommonMark+tabel, aset lokal, ringkasan manual, progres manual, catatan dan resume. **Belum dimulai.** M4: editor assessment/aktual, nilai huruf/detail tersimpan, draf penilaian, skenario/revisi/perbandingan/manual targetIP. F08G/P1/P2 tetap backlog. Tidak ada AI, hosting, cloud atau paid service.
