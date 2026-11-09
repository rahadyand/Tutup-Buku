# Rencana milestone kecil

Tenggat 8 Desember 2026. Jadwal berikut target kerja satu pengembang; rekan menyiapkan laporan berdasarkan bukti nyata. Setiap milestone harus bisa dibuka dan diuji sebelum lanjut.

| Milestone | Target | Hasil runnable dan gerbang |
|---|---|---|
| M1 Fondasi (selesai) | 8–10 Nov | Maven Wrapper, shell JavaFX dua pintu, model nilai dan kalkulasi murni, MT01–13, smoke JavaFX/SQLite/Markdown. Tidak ada login/editor/simpan skenario |
| M2 Akun & akademik lokal (selesai) | 11–17 Nov | SQLite schema/repository, PBKDF2 bersalt+parameter, akun terisolasi, profil, semester aktif, CRUD mata kuliah tanpa assessment; AT01–04 dan restart |
| M3 Belajar lokal (selesai) | 18–21 Nov | Jalur kuliah/peminatan, CommonMark+table reader/editor aman, salin aset, konten contoh, selesai/catatan/resume; AT05–07 offline/restart |
| M4 Nilai & skenario (selesai) | 22–30 Nov | Form flat, huruf/detail, aktual vs asumsi, kehadiran, simpan/gandakan/revisi/banding skenario, IP lengkap dan pembanding manual; AT08–12 terintegrasi |
| M5 Integrasi & distribusi | 1–5 Des | Error simpan AT14, semua P0 offline AT13, UI1366×768/125%, ukur30×20<1detik, paket jpackage runtime Windows, komputer kedua, 3–5 uji pengguna dan bukti |
| Pembekuan & submission | 6–8 Des | Bekukan fitur, latihan demo7–10menit, sinkron laporan/PPT dengan hasil nyata, source+paket+petunjuk+cadangan |

M1 berhenti setelah build/test dan peluncuran shell diverifikasi. M1–M4 telah diimplementasikan berurutan atas permintaan pengguna; pekerjaan berhenti setelah M4. M3 dan M4 sama penting untuk produk final; urutan dipakai agar fondasi data tersedia. F08G/F14/F15 hanya setelah seluruh P0 stabil dan masih ada waktu, jika tidak pindahkan ke versi berikutnya.

## Bentuk kode M1

- `domain`: Score, AssessmentItem (aktual), ScenarioAssumptions (locked terpisah), LetterGrade, Attendance, GradeResult, CourseResult, TargetPlan, GpaResult.
- `service`: validasi flat, GradeCalculator, TargetPlanner, SemesterGpaService, DecimalInput. Tidak mengakses UI/database.
- `ui`: Application dan dashboard sederhana, tombol contoh memanggil service.
- M2 menambahkan repository SQLite, auth dan data akademik. CommonMark tetap hanya smoke test sampai M3.

Versi dikunci pada pom dan wrapper. Tidak ada Spring, ORM, server, AI, layanan berbayar, atau parser Markdown buatan sendiri. Starter lama dipertahankan; source root Maven memakai src/main/java dan src/test/java.

## Hasil M2
Akun/profil/semester/mata kuliah persisten, schema versi 1, PBKDF2 bersalt, query pemilik, transaksi hapus dan GUI bekerja. Nomor semester positif; tahun akademik YYYY/YYYY berurutan dipilih sebagai validasi operasional. Semester pertama tidak otomatis aktif: pengguna memilih Jadikan aktif. Penghapusan semester aktif mengosongkan referensi aktif, tanpa memilih semester lain diam-diam. Komponen/nilai tersimpan dan draf penilaian tetap M4.

Checkpoint M2 telah dibuat dan di-push atas permintaan pengguna sebelum M3: 0ca1b2899bbfff9c057f20f0ba984a807e9a7e2a pada main, https://github.com/rahadyand/Tutup-Buku. Hash remote telah diperiksa.

## Hasil M3
Dua jalur belajar, CRUD/urutan bab/bidang/materi, editor CommonMark, preview aman, impor gambar terkelola, autosave/simpan eksplisit, progres manual, catatan, resume dan koleksi5contoh sudah diimplementasikan. Skema naik1→2 tanpa menghapus data M2. Materi contoh disalin sekali per akun atas klik pengguna setelah memilih course; tidak membuat course/semester atau menimpa data saat login.

M3 di-checkpoint sebelum M4: 04d580d61473596f9a39f95dafee21c8a6265dcf, main; push dan hash remote diverifikasi.

## Hasil M4
Editor flat/aktual, draft bobot, mode huruf/detail, target multi-komponen, snapshot skenario/revisi/review/refresh/duplikasi/perbandingan dan IP lengkap telah diimplementasikan. Skema naik2→3 tanpa reset M2/M3; engine M1 tetap. Pengujian service/persistensi/migrasi dan JavaFX dicatat pada DEVELOPMENT_STATUS. Commit/push M4 hanya setelah verifikasi akhir sesuai permintaan pengguna.

Milestone berikutnya **M5 integrasi dan distribusi**: uji1366×768/125%, benchmark30×20, paket jpackage beserta lisensi, komputer kedua/offline fisik, usability3–5mahasiswa dan bukti laporan. Belum dimulai; jangan lanjut otomatis.
