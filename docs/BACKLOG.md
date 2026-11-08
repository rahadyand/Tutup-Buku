# Backlog yang dipertahankan

M2 akun/profil/semester/mata kuliah dan SQLite selesai. P0 yang belum dibuat **bukan fitur opsional**: materi, editor, aset, ringkasan manual, progres, resume, catatan (M3); CRUD nilai, skenario tersimpan, revisi, perbandingan/manual target IP (M4); penanganan gagal simpan, distribusi offline dan uji Windows (M5). Rincian lengkap di REQUIREMENTS.

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

M2 belum menambahkan ganti password atau recovery lokal; fitur akun lanjutan tetap backlog. Form penilaian, nilai aktual, draf assessment dan skenario tersimpan masih M4. Konfirmasi hapus M2 mencakup data akademik yang saat ini ada; saat M3/M4 menambah turunan, migrasi dan pesan konfirmasi harus ikut diperluas.
