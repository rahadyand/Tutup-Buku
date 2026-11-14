# Checklist kebutuhan PRD v1.1

Seluruh 25 halaman dibaca pada 8 November 2026 dan ditinjau ulang untuk M2–M5. P0 adalah target produk akhir, bukan klaim milestone 1 selesai. `parsial` berarti hanya bagian domain yang dicakup M1. Aturan akademik ini mengikuti PRD, belum diverifikasi sebagai kebijakan resmi fakultas.

| ID fitur / rincian PRD | Prioritas | Perilaku yang wajib dipertahankan | Status / milestone |
|---|---|---|---|
| F01 / ACC-01, ACC-02 | P0 | NIM teks unik setelah trim, nol awal tetap, tanpa pola dugaan; password hash+salt+parameter; registrasi/konfirmasi, login/logout, isolasi akun; tanpa recovery universal | Selesai M2; isolasi materi/catatan ditambahkan M3 |
| F02 / ACA-01 | P0 | Nama/prodi editable; semester aktif milik akun; nomor/tahun unik pada alur Revision 001; arsip periode lama tetap terpisah; semester lama tetap; mata kuliah nama/SKS >0, kode opsional; konfirmasi hapus turunan | Selesai M2, termasuk persistensi dan GUI |
| F03 | P0 | Dashboard dua pintu setara Modul dan Simulasi; kosong bukan IP nol | Selesai M4, dua pilar terintegrasi |
| F04 / LEARN-01,02,03 | P0 | Materi bawaan disalin per akun dan materi pengguna; Markdown judul/teks/list/tabel/kode/gambar/link; editor+preview, aset PNG/JPEG disalin, rumus gambar, sumber/urutan/revisi | Selesai M3, persistence dan GUI |
| F05 / LEARN-01 | P0 | Kuliah→bab→materi terpisah dari peminatan→bidang→materi; satu pemilik, salinan independen | Selesai M3, persistence dan GUI |
| F06 / LEARN-04,05 | P0 | Selesai manual, progres dari jumlah materi; nol materi berlabel kosong; lanjut materi/posisi setelah restart; tangani materi dihapus/berubah | Selesai M3, persistence dan GUI |
| F07 / LEARN-06 | P0 | Catatan teks pribadi per akun/materi terpisah dari isi; konfirmasi hapus; gagal simpan mempertahankan edit | Selesai M3, persistence dan GUI |
| F08 / GRADE-01,02,03 | P0 | Nama komponen bebas tanpa UTS/UAS wajib; flat total tepat 100%; skor earned/max desimal; kosong ≠0; bobot0 informatif; draft invalid boleh disimpan, hitung ditahan | Selesai M4: domain, CRUD, draf dan GUI |
| F08G / GRADE-01 | P1 | Satu tingkat kelompok, induk dan anak masing-masing total100%; bobot efektif daun; gerbang normalisasi/konversi/solver/skenario flat telah lulus | Ditunda bersyarat |
| F09 / GRADE-03, SCN-01,02 | P0 | Huruf langsung atau detail; hanya satu mode aktif, data mode lain tetap; aktual/perkiraan terpisah; tanpa skor angka buatan dari huruf | Selesai M4: mode tersimpan dan GUI |
| F10 / PLAN-01 | P0 | Target angka atau batas bawah huruf; semua bebas diberi target normalisasi sama; aktual dan locked tetap; kalkulasi ulang; aman dibulatkan ke atas; mustahil/W=0 eksplisit | Selesai M4: target banyak komponen dan GUI |
| F11 / SCN-01,02 | P0 | CRUD/gandakan skenario, snapshot revisi, penanda kedaluwarsa, refresh eksplisit, banding dua skenario selaras | Selesai M4: snapshot/revisi/refresh/perbandingan |
| F12 / GPA-01,02 | P0 | Semua SKS/hasil efektif wajib; D/E masuk penyebut; kosong bukan0; target IP0–4 dibanding sebelum pembulatan; label simulasi jika ada asumsi nilai/kehadiran; kombinasi manual | Selesai M4: IP lengkap, pembanding dan GUI |
| F13 / GRADE-04 | P0 | Kehadiran awal true asumsi; false→E/0 sambil simpan asal, reversible; acuan editable awal75%/12 dari16; bukan absensi otomatis | Domain M1; checkbox/acuan tersimpan M2; nilai tersimpan/override GUI M4 selesai |
| F14 | P1 | Pencarian judul materi | Backlog |
| F15 | P1 | Backup/restore lokal melalui UI | Backlog |
| F16–F20 | P2 | PDF, online, optimasi kombinasi antarmatkul, IPK, kuis/AI (PRD tidak memetakan tiap nomor secara rinci) | Semua rincian di BACKLOG |

## Aturan lintas fitur

- Offline untuk alur inti dan aset; hanya link luar atas klik membuka browser dan perlu internet.
- Ringkasan manual sebagai heading biasa, bukan generator; kode materi tidak dieksekusi.
- Gagal simpan jangan reset data/editor atau tampil sukses palsu. Transaksi, FK, query pemilik, raw HTML/script nonaktif.
- Data di folder pengguna, bukan folder instalasi; UI tidak membeku; akses keyboard; target 1366×768/skala125%; paket Windows mandiri diuji komputer kedua / M5.
- Skenario lama boleh dibaca sebagai snapshot, bukan IP terkini; perubahan aktual/bobot/SKS perlu revisi.

## Override pengguna Revision 001

Aturan UI/NIM/tahun navigasi terbaru menggantikan PRD hlm6/15 pada bagian yang bertentangan: NIM berpanduanL01/L02 tanpa memblokir akun lama, akun terpusat+passwordtoggle, header kontekstual, shortcutdi bawahKeluar, tabDashboard/Akademik/Profil, dua halaman akademik, tahun tunggal tanpa periodeselector. Metadata kehadiran hanya diedit pada simulasi. Schema 4 menjaga seluruh ID dan arsip; rincian/bukti di REVISION_001. Tabel/milestone di bawah yang menyebut tahun/periode adalah riwayat sebelum revisi.

## Jenis keputusan

**Disepakati:** semua perilaku P0 di atas, ambang nilai persis PRD hlm10, D/E tidak lulus, target banyak komponen, kehadiran default asumsi, dua pilar setara.

**Rekomendasi teknis yang dipilih:** JavaFX/Maven/SQLite JDBC/CommonMark; lapisan sederhana; BigDecimal; Maven Wrapper. Versi setelah inspeksi: JDK21.0.12 tersedia; JavaFX21.0.10, Maven3.9.11, SQLite JDBC3.51.0.0, CommonMark0.24.0 beserta ekstensi tabel. Pasangan diuji melalui build/smoke test, bukan dianggap sudah cocok dari PRD.

**Asumsi operasional:** password minimal 8 karakter dan prodi teks bebas sudah diterapkan M2. M3 memakai 3 materi PBO+2 peminatan asli, autosave2detik, rasio gulir0–1; minimum jendela760×480 pada M5; ukuran awal mengikuti visual bounds. Jendela target1366×768 dan skala proses125% diperiksa; Windows125% nyata tetap gerbang pengguna. M2 memilih PBKDF2-HMAC-SHA256 600.000 iterasi, salt acak 16 byte, hash 256 bit; seluruh parameter disimpan.

## Pemetaan penerimaan

M1: MT01–MT13 pada domain, AT08/11/12 bagian logika; flat setara contoh MT14 diuji, grouped sendiri belum aktif. M2: AT01–04 pada akun/akademik tersedia; isolasi materi/nilai/catatan lengkap baru dapat diuji M3–M4. M3: AT05–07. M4: AT09–12 end-to-end. M5: AT13–14, persistensi/restart dan uji pengguna. Hasil aktual terdapat di DEVELOPMENT_STATUS, bukan tabel rencana ini.

## Kriteria M2 yang diverifikasi
- ACC-01: NIM teks unik setelah trim, registrasi/konfirmasi, hash bersalt, login benar/salah, logout. Password minimal 8 karakter mengikuti asumsi PRD; tidak ada email/cloud/recovery universal.
- ACC-02: kepemilikan semester, course, profil dan referensi aktif dibatasi akun; ID akun lain ditolak oleh service/repository/FK. Materi/catatan pengguna ditambahkan M3; nilai pengguna sudah diisolasi M4.
- ACA-01: CRUD semester/mata kuliah, nama/prodi editable, NIM immutable pada API profil; unik tahun/periode per akun; active semester bertahan setelah restart. SKS positif, termasuk desimal; kode opsional.
- GRADE-04 bagian M2: default kehadiran true/confirmed=false dan acuan editable awal 75% atau 12/16; tidak ada kalkulasi absensi. Model Attendance dan adapter CourseResult kompatibel engine M1.
- Kualitas M2: schema versi 1, FK aktif, query berparameter, transaksi cascade, lokasi AppData, worker UI, retry dan input tetap saat simpan gagal, konfirmasi hapus dengan Batal sebagai tombol default.
- Rekomendasi teknis: interface AccountRepository/AcademicRepository dengan satu SqliteRepository; tanpa ORM/framework baru. Referensi aktif memakai tabel active_semesters agar FK memastikan semester milik akun.
- Asumsi operasional: tahun YYYY/YYYY berurutan, prodi boleh kosong sebelum dilengkapi, pemilihan aktif eksplisit; hapus semester aktif mengosongkan referensi aktif. Form akademik disimpan melalui tombol Simpan; autosave materi tetap untuk M3.

## Kriteria M3 yang diverifikasi
- LEARN-01/F04–F05: kuliah→semester→course→bab→materi dan peminatan→bidang→materi terpisah. CHECK tepat satu lokasi, composite FK owner/parent, CRUD dan tombol urut Naik/Turun; salinan contoh tiap akun independen.
- LEARN-02: CommonMark0.24.0+GFM table, heading/paragraf/list/bold/italic/kode/gambar/link. Tulis dan Pratinjau/Baca; kode tidak dijalankan. HTML raw di-escape, script halaman disabled; URL gambar eksternal/file dan link selain http/https ditolak.
- Aset PNG/JPEG didekode/divalidasi dan disalin ke dataDirectory/assets/pemilik dengan referensi relatif; gambar asal boleh pindah. Missing/invalid references menjadi placeholder bernama. Tidak ada cleanup agresif aset.
- LEARN-03: judul wajib, sumber opsional, urutan/revisi/timestamp, isi kosong berlabel Draf. Simpan eksplisit dan autosave2detik; status Belum tersimpan/Menyimpan/Tersimpan/Gagal menyimpan. Save lama ditolak CAS; simpan gagal tidak mengosongkan buffer. Simpan/Buang/Batal pada navigasi/logout/close.
- Koleksi asli5materi:3PBO pada course pilihan dan2peminatan. Tombol salin sekali per akun, dalam transaksi dengan marker; tidak menyalin pada setiap login/migrasi, tidak menimpa edit.
- LEARN-04/F06: selesai/incomplete manual; membuka materi tidak menyelesaikannya. Course menghitung total materi seluruh bab; contoh2selesai dari10=20%, bukan50%. Empty state tanpa pembagian nol.
- LEARN-05/F06: posisi gulir0–1 dan revisi per akun; Lanjutkan Membaca memulihkan setelah render. Revisi berubah→awal; materi dihapus→penjelasan dan daftar jalur/folder yang masih tersedia.
- LEARN-06/F07: satu catatan teks pribadi terpisah per materi, persistensi dan konfirmasi hapus; input dipertahankan saat gagal. Ringkasan tetap heading manual dalam markdown.
- Migrasi schema1→2 mempertahankan akun/profil/semester/course/active selection M2. Seluruh tes memakai database/aset sementara; full usability dan paket Windows tetap M5.
- Keputusan implementasi: batas gambar10MB/16juta piksel, nama file UUID dan folder hash NIM; material+note disimpan atomik; reorder tidak menaikkan content revision. Aset yang tidak lagi dirujuk sengaja tidak dibersihkan pada M3.

## Kriteria M4 dan bukti

- F08/GRADE-01–03: CRUD+urutan komponen bebas, flat dan earned/max desimal; optional maksimum sebelum nilai. Draf total99,99/100,01 boleh disimpan tetapi tidak dikalkulasi; bobot0 informatif.
- F09/SCN-01–02: aktual vs estimasi terpisah; satu mode aktif, inactive detail/huruf tidak dihapus. Huruf langsung tanpa angka rekaan.
- F10/PLAN-01: target angka atau ambang minimum huruf; bebas/kunci/aktual jelas. Contoh60→55→70 melalui service dan JavaFX; impossible/already/W0/draft invalid dan attendance dijelaskan.
- F11: CRUD/nama/gandakan/open/save/delete snapshot per owner/semester. Revisi actual/bobot/SKS/set course menandai usang; review/refresh eksplisit menghasilkan draft sebelum save. Snapshot lama tidak diam-diam diubah. Perbandingan dua skenario menandai basis usang/tidak kompatibel.
- F12/GPA-01–02: IP penuh dari semua efektif+SKS, tidak parsial; D/E masuk penyebut. Target0–4 dan delta dibanding sebelum pembulatan; label asumsi/historis. Contoh36/11 dan30/11 diuji integrasi.
- F13/GRADE-04: asumsi awal, effective E/0 dengan hasil asal tetap, reversible; verifikasi kehadiran aktual opsional.
- Kualitas: migrasi2→3 transaksional menjaga M2/M3, rollback saat gagal; SQL owner+ID dan prepared statements, CAS skenario, pemisahan persistence/service/UI. Database/aset tes sementara.
- Gerbang bukti: GradePlanningPersistenceTest17, GradeMigrationTest2, seluruh regresi118, MilestoneFourFxCheck1 +5GUI regresi. Detail perintah/hasil akhir di DEVELOPMENT_STATUS.
- Tetap tertunda: F08G grouped P1, IPK, optimasi antar-course, distribusi/usability/benchmark M5. Selesainya fitur M4 tidak menyatakan seluruh nonfungsional produk final selesai.

## Tinjauan penerimaan M5 (9 November 2026)

Pemetaan per kebutuhan/tes dan defek terkonfirmasi ada di [INTEGRATION_REVIEW.md](INTEGRATION_REVIEW.md); ini membedakan bukti perilaku dari jumlah tes.

| Kelompok | Diimplementasikan | Bukti yang dijalankan | Masih pending |
|---|---|---|---|
| F01–F03 / AT01–04 | Akun/profil/aktif/akademik/dashboard; guard edit diperluas | Regresi unit/SQLite dan M2/M5GUI; registrasi/login paket nyata setelah restart | Usability oleh mahasiswa |
| F04–F07 / AT05–07 | Dua jalur, Markdown aman, gambar/note/completion/resume | M3/M5GUI + decoder/owner; paket mengimpor PNG via chooser asli, close/reopen menampilkan gambar/note/completion/resume | Uji pada komputer kedua; jaringan fisik diputus |
| F08–F13 / AT08–12, MT01–13 | Flat/aktual/target/snapshot/revisi/banding/IP/kehadiran | Regresi domain/persistensi dan M4/M5GUI, termasuk multi-course dan perubahan SKS | Pengujian paket oleh pengguna pada perangkat kedua |
| AT14 | Rollback/input tetap/retry/guard/migrasi | DatabaseSafety/LearningMigration/GradeMigration dan error nyata melalui trigger pada suite GUI | Perilaku disk/antivirus berbeda di perangkat lain |
| AT13 / distribusi | ZIP app-image + runtime/deps/legal/panduan; data di luar instalasi | EXE hasil ekstraksi di path berspasi, JAVA_HOME/MAVEN_HOME kosong dan PATHSystem32; normal close/reopen exit0; audit hash/file | **Putus jaringan fisik, link luar gagal wajar, komputer kedua** |
| Responsivitas | Worker dan engine dipertahankan |30×20; cold21,738ms, median5,606ms, max8,762ms pada mesin ini | Benchmark perangkat penerima |
| Keterbacaan | Sidebar scroll, note lipat, header/window adaptif | Jendela targetfisik1366×768; prosesJavaFX1,25; screenshot ditinjau, tanpa ubah Windows | Windows1366×768/125% nyata dan uji keyboard mahasiswa |

Default138tes +7GUI +benchmark1 lulus; tes skala125% mengulang alurM5 secara terpisah. F08G/MT14 grouped tidak diaktifkan; semua P1/P2 tetap deferred. Simpan nilai/skenario eksplisit mengikuti instruksi pengguna M4; autosave materi2detik tetap. Retry menggunakan Simpan yang sama sesudah pesan gagal.

Kandidat lokal tersedia untuk penerimaan; jangan menyatakan seluruh P0 penerimaan lapangan selesai sampai AT13 dan pemeriksaan pengguna tercatat. Lembar pelaksanaan: [RELEASE_ACCEPTANCE.md](RELEASE_ACCEPTANCE.md).
