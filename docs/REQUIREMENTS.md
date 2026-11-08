# Checklist kebutuhan PRD v1.1

Seluruh 25 halaman dibaca pada 8 November 2026 dan ditinjau ulang untuk M2. P0 adalah target produk akhir, bukan klaim milestone 1 selesai. `parsial` berarti hanya bagian domain yang dicakup M1. Aturan akademik ini mengikuti PRD, belum diverifikasi sebagai kebijakan resmi fakultas.

| ID fitur / rincian PRD | Prioritas | Perilaku yang wajib dipertahankan | Status / milestone |
|---|---|---|---|
| F01 / ACC-01, ACC-02 | P0 | NIM teks unik setelah trim, nol awal tetap, tanpa pola dugaan; password hash+salt+parameter; registrasi/konfirmasi, login/logout, isolasi akun; tanpa recovery universal | Selesai M2; isolasi materi/catatan menunggu M3 |
| F02 / ACA-01 | P0 | Nama/prodi editable; semester aktif milik akun; tahun/periode unik; semester lama tetap; mata kuliah nama/SKS >0, kode opsional; konfirmasi hapus turunan | Selesai M2, termasuk persistensi dan GUI |
| F03 | P0 | Dashboard dua pintu setara Modul dan Simulasi; kosong bukan IP nol | Dashboard setelah login M2; integrasi dua pilar penuh M3–M4 |
| F04 / LEARN-01,02,03 | P0 | Materi bawaan disalin per akun dan materi pengguna; Markdown judul/teks/list/tabel/kode/gambar/link; editor+preview, aset PNG/JPEG disalin, rumus gambar, sumber/urutan/revisi | Belum / M3 |
| F05 / LEARN-01 | P0 | Kuliah→bab→materi terpisah dari peminatan→bidang→materi; satu pemilik, salinan independen | Belum / M3 |
| F06 / LEARN-04,05 | P0 | Selesai manual, progres dari jumlah materi; nol materi berlabel kosong; lanjut materi/posisi setelah restart; tangani materi dihapus/berubah | Belum / M3 |
| F07 / LEARN-06 | P0 | Catatan teks pribadi per akun/materi terpisah dari isi; konfirmasi hapus; gagal simpan mempertahankan edit | Belum / M3 |
| F08 / GRADE-01,02,03 | P0 | Nama komponen bebas tanpa UTS/UAS wajib; flat total tepat 100%; skor earned/max desimal; kosong ≠0; bobot0 informatif; draft invalid boleh disimpan, hitung ditahan | Domain / M1; CRUD / M4 |
| F08G / GRADE-01 | P1 | Satu tingkat kelompok, induk dan anak masing-masing total100%; bobot efektif daun; gerbang normalisasi/konversi/solver/skenario flat telah lulus | Ditunda bersyarat |
| F09 / GRADE-03, SCN-01,02 | P0 | Huruf langsung atau detail; hanya satu mode aktif, data mode lain tetap; aktual/perkiraan terpisah; tanpa skor angka buatan dari huruf | Domain / M1; mode tersimpan / M4 |
| F10 / PLAN-01 | P0 | Target angka atau batas bawah huruf; semua bebas diberi target normalisasi sama; aktual dan locked tetap; kalkulasi ulang; aman dibulatkan ke atas; mustahil/W=0 eksplisit | Domain / M1; form / M4 |
| F11 / SCN-01,02 | P0 | CRUD/gandakan skenario, snapshot revisi, penanda kedaluwarsa, refresh eksplisit, banding dua skenario selaras | Belum / M4 |
| F12 / GPA-01,02 | P0 | Semua SKS/hasil efektif wajib; D/E masuk penyebut; kosong bukan0; target IP0–4 dibanding sebelum pembulatan; label simulasi jika ada asumsi nilai/kehadiran; kombinasi manual | Domain / M1; UI/simpan / M4 |
| F13 / GRADE-04 | P0 | Kehadiran awal true asumsi; false→E/0 sambil simpan asal, reversible; acuan editable awal75%/12 dari16; bukan absensi otomatis | Domain M1; checkbox/acuan tersimpan M2; nilai tersimpan M4 |
| F14 | P1 | Pencarian judul materi | Backlog |
| F15 | P1 | Backup/restore lokal melalui UI | Backlog |
| F16–F20 | P2 | PDF, online, optimasi kombinasi antarmatkul, IPK, kuis/AI (PRD tidak memetakan tiap nomor secara rinci) | Semua rincian di BACKLOG |

## Aturan lintas fitur

- Offline untuk alur inti dan aset; hanya link luar atas klik membuka browser dan perlu internet.
- Ringkasan manual sebagai heading biasa, bukan generator; kode materi tidak dieksekusi.
- Gagal simpan jangan reset data/editor atau tampil sukses palsu. Transaksi, FK, query pemilik, raw HTML/script nonaktif.
- Data di folder pengguna, bukan folder instalasi; UI tidak membeku; akses keyboard; target 1366×768/skala125%; paket Windows mandiri diuji komputer kedua / M5.
- Skenario lama boleh dibaca sebagai snapshot, bukan IP terkini; perubahan aktual/bobot/SKS perlu revisi.

## Jenis keputusan

**Disepakati:** semua perilaku P0 di atas, ambang nilai persis PRD hlm10, D/E tidak lulus, target banyak komponen, kehadiran default asumsi, dua pilar setara.

**Rekomendasi teknis yang dipilih:** JavaFX/Maven/SQLite JDBC/CommonMark; lapisan sederhana; BigDecimal; Maven Wrapper. Versi setelah inspeksi: JDK21.0.12 tersedia; JavaFX21.0.10, Maven3.9.11, SQLite JDBC3.51.0.0, CommonMark0.24.0 beserta ekstensi tabel. Pasangan diuji melalui build/smoke test, bukan dianggap sudah cocok dari PRD.

**Asumsi operasional:** password minimal 8 karakter dan prodi teks bebas sudah diterapkan M2. Untuk nanti: 3 materi PBO+2 peminatan hanya rencana demo; autosave 2 detik, rasio gulir 0–1, minimum jendela 1024×768. M2 memilih PBKDF2-HMAC-SHA256 600.000 iterasi, salt acak 16 byte, hash 256 bit; seluruh parameter disimpan.

## Pemetaan penerimaan

M1: MT01–MT13 pada domain, AT08/11/12 bagian logika; flat setara contoh MT14 diuji, grouped sendiri belum aktif. M2: AT01–04 pada akun/akademik tersedia; isolasi materi/nilai/catatan lengkap baru dapat diuji M3–M4. M3: AT05–07. M4: AT09–12 end-to-end. M5: AT13–14, persistensi/restart dan uji pengguna. Hasil aktual terdapat di DEVELOPMENT_STATUS, bukan tabel rencana ini.

## Kriteria M2 yang diverifikasi
- ACC-01: NIM teks unik setelah trim, registrasi/konfirmasi, hash bersalt, login benar/salah, logout. Password minimal 8 karakter mengikuti asumsi PRD; tidak ada email/cloud/recovery universal.
- ACC-02: kepemilikan semester, course, profil dan referensi aktif dibatasi akun; ID akun lain ditolak oleh service/repository/FK. Materi/catatan/nilai pengguna belum dibuat.
- ACA-01: CRUD semester/mata kuliah, nama/prodi editable, NIM immutable pada API profil; unik tahun/periode per akun; active semester bertahan setelah restart. SKS positif, termasuk desimal; kode opsional.
- GRADE-04 bagian M2: default kehadiran true/confirmed=false dan acuan editable awal 75% atau 12/16; tidak ada kalkulasi absensi. Model Attendance dan adapter CourseResult kompatibel engine M1.
- Kualitas M2: schema versi 1, FK aktif, query berparameter, transaksi cascade, lokasi AppData, worker UI, retry dan input tetap saat simpan gagal, konfirmasi hapus dengan Batal sebagai tombol default.
- Rekomendasi teknis: interface AccountRepository/AcademicRepository dengan satu SqliteRepository; tanpa ORM/framework baru. Referensi aktif memakai tabel active_semesters agar FK memastikan semester milik akun.
- Asumsi operasional: tahun YYYY/YYYY berurutan, prodi boleh kosong sebelum dilengkapi, pemilihan aktif eksplisit; hapus semester aktif mengosongkan referensi aktif. Form akademik disimpan melalui tombol Simpan; autosave materi tetap untuk M3.
