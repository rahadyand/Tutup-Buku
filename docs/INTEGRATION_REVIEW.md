# Tinjauan integrasi P0 — M5

PRD25halaman dibaca kembali; baseline cf37dbea4e0de35b729808ef130e21c065c9f04a cocok dengan remote/main. Jumlah tes adalah inventaris bukti, bukan pengganti pemetaan perilaku.

| PRD | Perilaku aktual dan bukti | Status/batas |
|---|---|---|
| F01 / AT01–02 | AuthService+PBKDF2, NIM teks/trim, password confirmation/login/logout. LocalAccountTest; MilestoneTwoFxCheck; akun fiktif dari EXE hasil ekstraksi | Terverifikasi; recovery/enkripsi seluruh DB tidak dijanjikan |
| F02 / AT03–04 | AcademicService/SqliteRepository, aktif sebagai FK, semester lama tetap, SKS positif. AcademicPersistenceTest14, M2/M5GUI | Terverifikasi; aktif dipakai sebagai pilihan awal M5 |
| F03 | WorkspaceView dua ruang dashboard; tanpa semester bukan IP0. M2/M5GUI | Terverifikasi |
| F04–05 / AT05,07 | CommonMark+GFM, renderer aman, AssetStore lokal, pemisahan chapter/field owner. LearningPersistence/AssetsAndMarkdown/MarkdownReaderFxCheck/M3/M5GUI | Terverifikasi; EXE juga mengimpor PNG melalui file chooser asli dan merender setelah sumber dipindah |
| F06–07 / AT06 | Completion manual, note terpisah, count semua materi, bookmark revisi/rasio. LearningPersistence/M3/M5GUI | Terverifikasi; akun/materi/note/completion/gambar/resume juga diperiksa setelah EXE ditutup normal dan dibuka ulang |
| F08 / AT08 | CRUD/order/draft bobot flat, earned/max, missing≠0. GradePlanningPersistenceTest, GradeCalculatorTest, M4/M5GUI | Terverifikasi; grouped tetap P1 |
| F09 / SCN-02 | Satu mode aktif, inactive data tetap, actual terpisah estimasi. M4tests+dialog mode M5 | Terverifikasi; konfirmasi menjelaskan hasil yang ikut IP |
| F10 / MT01–09,12 | TargetPlanner, ScoreFraction/GradeCalculator,60→55→70, impossible/already/W0, konversi tanpa rounding. Domain dan M4GUI | Terverifikasi; tidak mengoptimalkan antar-course |
| F11 / AT09–10 | Snapshot/CAS/duplikasi/review/refresh/perbandingan. GradePlanningPersistence17, M4/M5GUI | Terverifikasi; tabel kini menampilkan Aktual/Asumsi; historis tetap ditandai |
| F12 / AT12, MT10,13 | IP semua course, valid SKS/efektif, D/E penyebut,36/11 dan30/11, target sebelum rounding | Terverifikasi domain+persistensi+GUI; bukan transkrip resmi |
| F13 / AT11, MT11 | Attendance assumed, underlying tetap, effectiveE/restore. Engine+GradePlanning+M4GUI | Terverifikasi; regresi M5 menjaga confirmed saat edit metadata saja |
| AT14 / kualitas data | SQL berparameter/owner, FK/transaksi/migrasi, input gagal tetap dan CAS. DatabaseSafety, LearningMigration, GradeMigration, semua suite GUI | Terverifikasi; tes selalu database/aset sementara |
| AT13 / offline/paket | Runtime/JAR/legal tersedia lokal; EXE hasil ekstraksi berjalan dengan JAVA_HOME/MAVEN_HOME kosong, PATHSystem32 dan proxy Java tak terhubung | Paket lokal terverifikasi; jaringan fisik diputus dan komputer kedua tetap pending pengguna |
| Keterbacaan | Ukuran jendela fisik target1366×768, skala proses1,25, screenshot setelah perbaikan panel baca | Terverifikasi pada proses uji; resolusi/skala Windows global tidak diubah, uji layar125% nyata pending |
| Responsivitas | CalculationPerformanceCheck,30×20, cold21,738ms;20sampel setelah3warmup: median5,606ms, max8,762ms | Di bawah1detik pada Windows11x64/Temurin21.0.13+9/12CPUlogis ini; render/setup/hash tidak masuk |

## Defek yang diperbaiki
1. Label akademik masih menyebut nilai belum tersedia → tautan instruksi menuju Simulasi Nilai.
2. Edit profil/akademik hilang pada navigasi → guard per form, Save/Buang/Batal; callback hanya berpindah setelah save berhasil.
3. Edit metadata course menghapus attendance.confirmed → pertahankan bila met sama; ubah met menjadi asumsi baru. Regresi persistence khusus.
4. Pilihan pembuka belajar/nilai mengabaikan aktif → preferensi dari profil, tanpa menghapus pilihan layar sebelumnya.
5. Save/refresh skenario mengembalikan course pertama → pertahankan course yang sedang dikerjakan. Ditemukan saat uji multi-course, bukan dari jumlah tes.
6. Status draf/peralihan mode/kelulusan/sumber hasil perbandingan kurang lengkap → label dan konfirmasi eksplisit, rumus bantuan, konteks/bobot selalu terlihat.
7. Reader hampir tidak mempunyai tinggi pada125% → panel catatan dapat dilipat, sidebar dapat digulir, header diringkas dan jendela mengikuti visual bounds.

Saat mengembangkan M5, tes juga menangkap nullable semester preference yang ter-unbox; diperbaiki sebelum regresi akhir. Helper tes menunggu node setelah pergantian tab sehingga tidak menganggap keterlambatan layout sebagai data hilang.

## Keputusan lingkup dan pemeriksaan tertunda
Simpan nilai/skenario tetap eksplisit sesuai instruksi M4; autosave materi tetap2detik. Retry memakai tombol Simpan yang sama setelah pesan gagal. Tidak ada perubahan schema/rumus/framework. F08G/F14/F15 dan semua P2 tetap backlog.

Pemeriksaan jendela paket menggunakan otomasi desktop pada data demo sendiri, dengan izin akses pengguna. Ini bukan usability oleh3–5mahasiswa. Uji komputer kedua, putus jaringan fisik, Windows125% nyata, SmartScreen/AV pada komputer penerima, serta usability memerlukan partisipasi pengguna. Kandidat lokal tidak sama dengan penerimaan akhir semua perangkat.
