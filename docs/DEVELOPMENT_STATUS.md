# Status pengembangan — Milestone 4

Tanggal: **9 November 2026, Asia/Jakarta**. Aplikasi **0.4.0-SNAPSHOT**, skema **3**. **M4 selesai dan terverifikasi; berhenti sebelum M5.**

## Checkpoint sebelum M4

M3 di-commit sebagai **04d580d61473596f9a39f95dafee21c8a6265dcf** — feat: complete milestone 3 learning modules and reading progress — pada [rahadyand/Tutup-Buku](https://github.com/rahadyand/Tutup-Buku), branch **main**. Push berhasil dan git ls-remote cocok sebelum implementasi M4. Status historis M3 tetap pada [MILESTONE_3_STATUS.md](MILESTONE_3_STATUS.md); klaim belum di-commit di dokumen historis tersebut merekam waktu sebelum checkpoint.

Sebelum checkpoint M4, fetch memverifikasi main lokal/remote sama dengan M3 (0 tertinggal/0 mendahului). Origin tetap https://github.com/rahadyand/Tutup-Buku.git. Autentikasi Git Credential Manager tersedia. Commit M4 diminta dengan pesan **feat: complete milestone 4 grade planning and saved scenarios** setelah verifikasi di bawah; hash hasil/push remote dilaporkan pada jawaban akhir dan dapat diperiksa melalui git log -1 serta git ls-remote origin refs/heads/main. Hash tidak ditanam di berkas commit itu sendiri.

## Implementasi selesai

- CRUD/urutan komponen flat per course, nama bebas tanpa UTS/UAS wajib. Earned/max desimal, maksimum boleh disiapkan sebelum earned. Nilai kosong tetap berbeda dari0, bobot0 informatif.
- Bobot total harus tepat100%; draf invalid tetap tersimpan dan hasil detail ditahan. Tidak ada normalisasi bobot diam-diam.
- Mode aktual DETAIL/DIRECT, huruf aktual terpisah; pergantian mode tidak menghapus data inactive. Perhitungan hanya memakai mode aktif.
- Target angka atau minimum huruf, aktual/kunci/bebas jelas. TargetPlanner M1 memberi saran normalisasi sama, aman2desimal ke atas. Impossible/raw>100, sudah tercapai, W=0 dan draf tidak sah dijelaskan.
- Skenario CRUD/nama/duplikasi/open/save/delete per owner/semester. Estimasi huruf dan kunci tidak pernah ditulis ke aktual. Duplikasi snapshot mempunyai baris/ID independen.
- Snapshot metadata/SKS/revisi/komponen/earned-max/kehadiran aktual, target/mode/estimasi/kunci/kehadiran asumsi dan saran saat save.
- Course revision naik bersama mutasi aktual/komponen/order/akademik. Revisi dan set ID dibandingkan untuk menandai usang; hasil lama selalu historis. Review menjelaskan perubahan, refresh hanya draft sampai Simpan. Kunci kini aktual/terhapus dibuang; target/kunci unknown lain dipertahankan.
- IP lengkap melalui SemesterGpaService M1, SKS D/E tetap masuk penyebut; unknown menghalangi IP. Target0–4, selisih dan tercapai dibanding sebelum pembulatan. Label simulasi bila asumsi tersisa.
- Perbandingan2snapshot tersimpan: union course, SKS kedua sisi, huruf efektif dan IP. Basis usang/berbeda ditandai.
- Kehadiran default assumed-met; actual dapat diverifikasi. Uncheck → efektif E/0 dengan underlying tetap; check memulihkan.
- Migrasi aditif2→3 transaksional, preservasi M2/M3 dan aset. Query berparameter/owner+ID, composite FK, CAS version saat save skenario dan rollback header+children.
- UI aktual dan skenario terpisah, diproses lewat UiTasks. Gagal simpan mempertahankan isian, Save/Buang/Batal digabung dengan guard belajar pada navigasi/logout/close. Konfirmasi hapus menjelaskan cascade/snapshot historis.
- Engine/perhitungan M1 tidak ditulis ulang; tanpa dependency aplikasi/framework/online baru. README, arsitektur, checklist, plan, backlog dan AGENTS dimutakhirkan. Formatter lokal hanya di .tools, diabaikan Git.

## Perintah dan hasil verifikasi akhir

PowerShell menggunakan:
```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
$env:MAVEN_USER_HOME = "$PWD\.tools\maven-user"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp clean verify
```

**BUILD SUCCESS; 137 tes, 0 failures, 0 errors, 0 skipped.** Build bersih mengompilasi71file aplikasi dan18file tes; JAR target/tutup-buku-0.4.0-SNAPSHOT.jar. Rincian:
- M1: GradeCalculatorTest43, TargetPlannerTest15, SemesterGpaServiceTest10, StackCompatibilityTest2 =70.
- M2: AcademicPersistenceTest13, LocalAccountTest8, DatabaseSafetyTest5 =26.
- M3: LearningPersistenceTest12, AssetsAndMarkdownTest8, LearningMigrationTest2 =22.
- M4: GradePlanningPersistenceTest17, GradeMigrationTest2 =19.

Pengujian M4 mencakup CRUD/order/draft bobot99,99/100,01,8/10+35/40, missing vs0,60→55→70 tanpa mengubah aktual,84,5 A-, mustahil/already/W=0, dua mode, estimasi vs aktual, attendance restore, persist/reopen/duplikasi independen, isolasi seluruh operasi, review/refresh SKS/actual/course baru/hapus, CAS conflict, rollback save+retry, cascade delete. Migrasi menjaga login/profil/semester aktif/course/materi/note/completion/bookmark/marker/aset, idempotensi dan rollback DDL/version.

Contoh PRD terintegrasi: SKS3,3,3,2 dengan A,A,C,B menghasilkan36/11, tampilan **3,27**; C gagal kehadiran menghasilkan30/11, tampilan **2,73**. Target3,2727 vs3,2728 dibanding tanpa pembulatan tampilan. Unknown course menghalangi seluruh IP; D masuk penyebut.

GUI opt-in:
```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck' '-DreuseForks=false' verify
```

**BUILD SUCCESS; 6 tes, 0 failures/errors/skipped.**
- M1shell1, M2akun/akademik/errorstartup2, readeraman1, M3alurbelajar1, M4nilai/skenario1.
- M4 meluncurkan aplikasi asli: tambah/urut/hapus komponen,60→55→70, save/open/duplicate, basis usang+dialog refresh, mode langsung/estimasi A, IP4→0 kehadiran→restore, banding, restart/persist.
- Trigger gagal simpan aktual dan skenario disisipkan hanya pada database sementara; input tetap, navigasi Batal tetap, retry setelah trigger dilepas berhasil.
- Screenshot dibuat dan ditinjau pada target/verification/m4-targets.png, m4-comparison.png, m4-scenarios.png. Teks dan tabel dapat dibaca pada viewport1280×820; form panjang digulir.
- XML/TXT hasil pada target/surefire-reports. Warning JavaFX unnamed module dan unchecked cast tes dicatat; tidak menggagalkan build.
- Semua database/aset pengujian di @TempDir, bukan database AppData pengguna.

Peluncuran normal:
```powershell
$env:TUTUP_BUKU_DATA_DIR = "$PWD\tmp\m4-launch-check"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp javafx:run
```
Jendela M4 dibuka melalui jalur run normal pada data demo terisolasi. Tutup jendela sebelum mencoba sesi/lokasi lain. Langkah manual rinci terdapat pada README.

Git: git diff --check diperiksa tanpa masalah whitespace; daftar berkas/pola rahasia umum serta ignore outputs/caches/tools/databases/log/secrets/aset runtime diperiksa. Maven Wrapper dan .mvn tetap dilacak. File starter pengguna tidak diubah. Commit/push tanpa force/rewrite atau perubahan visibility.

## Batas verifikasi dan pekerjaan berikutnya

Interaksi GUI di atas **otomatis**, dilengkapi tinjauan visual gambar; bukan uji usability manual mahasiswa. Belum diverifikasi: jaringan fisik diputus, UI1366×768/skala125%, benchmark30course×20komponen, komputer kedua, paket jpackage/runtime/installer/lisensi distribusi, usability3–5mahasiswa. Tidak ada klaim tes tersebut sudah lulus.

Grouped weighting, backup/restore UI, pencarian P1, AI/kuis/PDF, IPK dan optimasi antarmatkul tetap pada [BACKLOG.md](BACKLOG.md). Nilai/skenario memakai Simpan eksplisit; autosave nilai dan undo/delete recovery belum dibuat. Snapshot historis tetap dapat dibaca, tetapi harus direfresh dan disimpan sebelum dianggap terkini.

Berikutnya **M5 integrasi, distribusi Windows dan uji pengguna**, hanya setelah pengguna mencoba M4 dan meminta lanjut.
