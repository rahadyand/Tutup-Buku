# Rencana milestone kecil

Tenggat 8 Desember 2026. Jadwal berikut target kerja satu pengembang; rekan menyiapkan laporan berdasarkan bukti nyata. Setiap milestone harus bisa dibuka dan diuji sebelum lanjut.

| Milestone | Target | Hasil runnable dan gerbang |
|---|---|---|
| M1 Fondasi (selesai) | 8–10 Nov | Maven Wrapper, shell JavaFX dua pintu, model nilai dan kalkulasi murni, MT01–13, smoke JavaFX/SQLite/Markdown. Tidak ada login/editor/simpan skenario |
| M2 Akun & akademik lokal (selesai) | 11–17 Nov | SQLite schema/repository, PBKDF2 bersalt+parameter, akun terisolasi, profil, semester aktif, CRUD mata kuliah tanpa assessment; AT01–04 dan restart |
| M3 Belajar lokal | 18–21 Nov | Jalur kuliah/peminatan, CommonMark+table reader/editor aman, salin aset, konten contoh, selesai/catatan/resume; AT05–07 offline/restart |
| M4 Nilai & skenario | 22–30 Nov | Form flat, huruf/detail, aktual vs asumsi, kehadiran, simpan/gandakan/revisi/banding skenario, IP lengkap dan pembanding manual; AT08–12 terintegrasi |
| M5 Integrasi & distribusi | 1–5 Des | Error simpan AT14, semua P0 offline AT13, UI1366×768/125%, ukur30×20<1detik, paket jpackage runtime Windows, komputer kedua, 3–5 uji pengguna dan bukti |
| Pembekuan & submission | 6–8 Des | Bekukan fitur, latihan demo7–10menit, sinkron laporan/PPT dengan hasil nyata, source+paket+petunjuk+cadangan |

M1 berhenti setelah build/test dan peluncuran shell diverifikasi. M2 telah diimplementasikan atas permintaan pengguna; berhenti sebelum M3. M3 dan M4 sama penting untuk produk final; urutan dipakai agar fondasi data tersedia. F08G/F14/F15 hanya setelah seluruh P0 stabil dan masih ada waktu, jika tidak pindahkan ke versi berikutnya.

## Bentuk kode M1

- `domain`: Score, AssessmentItem (aktual), ScenarioAssumptions (locked terpisah), LetterGrade, Attendance, GradeResult, CourseResult, TargetPlan, GpaResult.
- `service`: validasi flat, GradeCalculator, TargetPlanner, SemesterGpaService, DecimalInput. Tidak mengakses UI/database.
- `ui`: Application dan dashboard sederhana, tombol contoh memanggil service.
- M2 menambahkan repository SQLite, auth dan data akademik. CommonMark tetap hanya smoke test sampai M3.

Versi dikunci pada pom dan wrapper. Tidak ada Spring, ORM, server, AI, layanan berbayar, atau parser Markdown buatan sendiri. Starter lama dipertahankan; source root Maven memakai src/main/java dan src/test/java.

## Hasil M2
Akun/profil/semester/mata kuliah persisten, schema versi 1, PBKDF2 bersalt, query pemilik, transaksi hapus dan GUI bekerja. Nomor semester positif; tahun akademik YYYY/YYYY berurutan dipilih sebagai validasi operasional. Semester pertama tidak otomatis aktif: pengguna memilih Jadikan aktif. Penghapusan semester aktif mengosongkan referensi aktif, tanpa memilih semester lain diam-diam. Komponen/nilai tersimpan dan draf penilaian tetap M4.

Milestone berikutnya M3 belajar lokal: dua jalur materi, CommonMark/table, aset PNG/JPEG, ringkasan manual, progres, catatan dan resume. Tidak dimulai dalam pekerjaan M2.
