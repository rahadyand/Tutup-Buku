# Backlog yang dipertahankan

M1–M4 selesai: akun/akademik, belajar lokal, CRUD nilai, target, snapshot skenario/revisi/perbandingan/manual IP. Pemulihan gagal simpan sudah diuji per area. M5 menyiapkan kandidat ZIP Windows dengan runtime, integrasi, benchmark dan uji lokal. Gerbang produk yang masih wajib: penerimaan komputer kedua/offline fisik/Windows125% nyata/usability; bukan fitur opsional. Rincian di REQUIREMENTS.

P1 bersyarat: F08G pengelompokan satu tingkat (hanya setelah engine dan skenario flat stabil), F14 pencarian judul, F15 backup/restore UI. Jika tak masuk sebelum pembekuan, tetap prioritas versi berikutnya.

Seluruh pengembangan PRD hlm24 dan pembatasan hlm4/19 dipertahankan:

- Materi/editor: editor visual, rumus LaTeX, pencarian isi, bookmark, target/tenggat belajar, bab peminatan, materi ditautkan lintas konteks, statistik progres rinci.
- Portabilitas: ekspor PDF, impor PDF dengan pemeriksaan, paket modul beserta gambar, berbagi tautan, ekspor/cetak simulasi.
- Evaluasi: kuis manual+kunci/pembahasan, format soal tambahan, evaluasi pemahaman; editor ringkasan khusus jika diperlukan. Ringkasan manual di isi adalah P0.
- AI opsional: ringkasan/kuis/bantuan belajar; perlu evaluasi biaya, izin konten, kualitas dan privasi. Tidak ada integrasi AI pada proyek saat ini.
- Akademik: IPK lintas semester, pencarian otomatis kombinasi antarmatkul untuk target IP, optimasi batas kemampuan per komponen, kebijakan per matkul, aturan pembulatan lain, bonus skor.
- Aturan khusus: nilai ujian minimum, absensi per tanggal dan otomatis, matkul non-IP/lulus-gagal, pengulangan, kelompok lebih kompleks.
- Online/akun: sinkronisasi, akun/email/recovery, pembaruan materi, kolaborasi pengajar/mahasiswa, berbagi skenario tanpa data sensitif; enkripsi seluruh database belum dijanjikan.
- Platform/pengalaman: OS lain/mobile, tema gelap, dashboard lebih kaya, aksesibilitas lanjutan, profil lintas perangkat, video tertanam. Paket installer EXE opsional; app-image Windows dengan runtime adalah rencana utama.

F16–F20 adalah kelompok ID P2 dari PRD; dokumen tidak memberi pemetaan satu-ke-satu tiap nomor. Jangan mengarang pemetaan rinci. Target banyak komponen dalam satu matkul tetap P0; yang ditunda adalah optimasi otomatis antar-matkul.

M2 belum menambahkan ganti password atau recovery lokal; fitur akun lanjutan tetap backlog. Form penilaian, nilai aktual, draf assessment dan skenario tersimpan sudah dibuat M4. Konfirmasi hapus M2 mencakup data akademik yang saat ini ada; M3 sudah memperluas cascade dan pesan konfirmasi untuk bab/materi/catatan/progres. M4 menambah cascade assessment/skenario: hapus semester menghapus skenarionya; hapus course mempertahankan snapshot historis berlabel usang.

M3 selesai: Markdown/tabel/kode, gambar lokal terkelola, Ringkasan manual, progres, catatan, resume, dan5materi contoh. Yang tetap ditunda: cleanup/garbage collection aset, paket modul portabel melalui UI, full-text search/fitur P1/P2. Aset tidak dihapus otomatis ketika referensinya hilang agar save gagal tidak menghilangkan gambar. Tidak ada LaTeX, PDF, video tertanam, AI atau kuis pada M3.

M4 selesai: flat saja; grouped weighting tetap P1 bersyarat. Selanjutnya M5 (hanya atas permintaan): distribusi Windows+runtime dan lisensi, uji komputer kedua, benchmark30course×20komponen, UI1366×768/125%, jaringan fisik diputus, usability3–5orang dan finalisasi bukti laporan. Ekspor/banding lebih dari2skenario, autosave nilai, undo/delete recovery dan penyuntingan bersamaan lintas beberapa instance aplikasi bukan lingkup M4. Simpan skenario bersifat eksplisit dan CAS menolak save versi lama.

## Sesudah kandidat M5
- **Menunggu pengguna:** komputer kedua tanpa Java/IDE, putus jaringan fisik dan link luar, Windows1366×768/125% nyata, usability3–5mahasiswa. Gunakan RELEASE_ACCEPTANCE, jangan menulis hasil fiktif.
- Jika ditemukan masalah nyata: prioritas pertama crash/data hilang/rumus/akses tombol, kemudian kebingungan alur. Kembangkan perbaikan hanya atas tindak lanjut pengguna.
- Distribusi lokal sudah app-imageZIP, belum installer, belum ditandatangani digital. Penerimaan SmartScreen/antivirus/perizinan pada perangkat lain belum dibuktikan.
- Manual backup DB+assets tetap perlu sebelum migrasi/pengumpulan. Jangan mengubah kebijakan cleanup atau recovery tanpa lingkup baru.
- Source/test/docs/config M5 di-checkpoint; ZIP/runtime/hasil audit tetap lokal di releases/tmp/target yang diabaikan Git. Tidak ada publikasi GitHub Release otomatis.
