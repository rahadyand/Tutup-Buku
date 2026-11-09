# Checklist penerimaan kandidat Windows

Gunakan akun/data fiktif. Catat tanggal, versi, Windows/arsitektur, resolusi/skala, lokasi ekstraksi, hasil dan pesan error. Jangan pakai data akademik teman tanpa persetujuan.

## Gerbang pengembang
- [ ] Baseline M4 hash dan regresi cocok.
- [ ] Regresi seluruh domain/persistence/migrasi dan tes GUI lulus setelah perbaikan.
- [ ] Alur terhubung: register → akademik → materi/gambar/note/completion → restart/resume → aktual/target/skenario/IP → perubahan basis → akun lain.
- [ ] Tinjau layar1366×768 dan emulasi skala JavaFX125%; tulis kondisi persis, bukan klaim skala Windows nyata.
- [ ] Benchmark30course×20item diukur dan kondisi dicatat.
- [ ] Paket runtime/lisensi lengkap; tidak ada DB/aset pribadi/secrets/cache/Git/source/test-only files.
- [ ] ZIP diekstrak ke direktori lain dengan spasi; **EXE hasil ekstraksi** berjalan tanpa Java/Maven/PATH pengembang.
- [ ] Save/reopen paket mempertahankan data di folder data terisolasi, bukan instalasi.
- [ ] Diff/checkpoint/push main cocok; tidak mengunggah ZIP/binary runtime.

Status dan hasil gerbang pengembang diisi pada DEVELOPMENT_STATUS repository; kotak di sini adalah lembar uji yang dapat disalin untuk setiap perangkat.

## Uji pengguna yang masih diperlukan
1. **Komputer Windows x64 kedua tanpa Java/IDE:** ekstrak ZIP, buka TutupBuku.exe, daftar/login. Verifikasi tidak ada dependensi dari komputer pengembang.
2. **Offline fisik:** putus jaringan sendiri, bukan lewat aplikasi/pengembang. Jalankan paket, login, baca gambar/tabel/kode, edit note, tandai selesai, target/simpan skenario/IP. Klik link luar: browser boleh gagal koneksi, aplikasi tetap hidup. Sambungkan jaringan kembali setelah selesai.
3. **Layar1366×768 dan Windows125%:** atur sendiri jika bersedia, tutup/buka aplikasi. Pastikan semua tombol dicapai dengan scroll/Tab, field/status dapat dibaca, dialog dapat dibatalkan. Catat skala asli dan kembalikan sesuai preferensi Anda.
4. **Persistensi:** simpan materi/note/completion/posisi serta skenario, tutup seluruh aplikasi, buka kembali dan login. Bandingkan hasil; pilihan semester aktif tetap, materi semester lain tidak hilang.
5. **Isolasi:** akun kedua kosong/terpisah; kembali akun pertama data tetap. Coba NIM dengan nol awal dan duplicate NIM.
6. **Aksi batal:** batal hapus course/semester/materi/skenario; edit lalu Batal pada navigasi. Pastikan data/form tidak berubah.
7. **Usability3–5mahasiswa:** minta membaca/menandai materi dan membuat satu skenario tanpa petunjuk langkah demi langkah. Catat keberhasilan, waktu, kebingungan dan komentar nyata. Tidak menebak kepuasan/manfaat belajar.

## Hasil per perangkat/peserta
| Pemeriksaan | Hasil nyata / bukti | Lulus / gagal / belum |
|---|---|---|
| Perangkat/Windows/resolusi/skala | Isi setelah diuji | Belum |
| Ekstrak/run tanpa Java | Isi setelah diuji | Belum |
| Offline fisik | Isi setelah diuji | Belum |
| Materi/gambar/note/resume | Isi setelah diuji | Belum |
| Target/skenario/IP | Isi setelah diuji | Belum |
| Restart/isolasi/batal | Isi setelah diuji | Belum |
| Usability peserta anonim | Isi setelah diuji | Belum |

Siap sebagai kandidat lokal setelah gerbang pengembang lulus. Siap untuk pengumpulan akhir setelah pengujian perangkat/jaringan/skala/pengguna di atas ditinjau dan masalah kritis diselesaikan.
