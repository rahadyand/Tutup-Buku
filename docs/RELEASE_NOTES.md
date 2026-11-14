# Kandidat0.5.4-rc1 — perawatan internal

Menghapus panduan NIM pra-Revision002 yang tidak lagi dipanggil aplikasi dan menyatukan validasi metadata Course sebelum cabang tambah/edit. Tampilan/alur Revision002, schema4, engine, hash password dan pemisahan aktual/skenario tetap. Query edit metadata tetap tidak menyentuh kehadiran.

Tidak ada fitur baru atau perubahan dashboard. Hasil tes, paket dan batas verifikasi terbaru di PONYTAIL_FULL_REVIEW.md repository. Gerbang lapangan dan H-02 historis tetap pending.

## Riwayat kandidat sebelumnya

# Kandidat0.5.3-rc1 — Revision 002

Form akun tetap terpusat: NIM200px, password260px dan nama300px; judul36px dan deskripsi terbungkus. Sapaan daftar mengikuti input nama/NIM L01/L02 dan tahun; login tidak membocorkan nama profil sebelum autentikasi.

Profil terpusat dengan dropdown Informatika/Sains Data. Prodi lama kosong/tidak dikenal tidak ditimpa saat membuka halaman. Semester90px/tahun110px, aksi170px; Semester Baru di bawah Hapus/petunjuk dan Mata Kuliah Baru di bawah status simpan. Navigasi kini kanan atas: Keluar lalu dua shortcut. Guard, transaksi, attendance, engine dan schema4 dipertahankan.

Bukti terbaru di REVISION_002.md repository. Kandidat masih memerlukan komputer kedua, jaringan fisik terputus, skala Windows125% nyata dan usability. H-02 historis belum diklaim selesai. Cadangkan DB+assets saat aplikasi tertutup sebelum memperbarui.

## Riwayat sebelum Revision 002

# Kandidat0.5.2-rc1 — Revision 001

Form Login/Daftar terpusat, panduan NIM Informatika/Sains Data tanpa aturan login baru, toggle tiga password dan reset visibility. Shortcut dipindahkan ke bawah Keluar; urutan Dashboard/Akademik/Profil. Header menampilkan profil dan semester aktif sebenarnya.

Akademik dua halaman; tahun tunggal4 digit, save+activate atomik, simpan saja tidak mengubah aktif. Course metadata tidak menyentuh kehadiran; acuan dipindahkan ke nilai aktual. Migrasi3→4 mempertahankan ID/data/periode arsip tanpa merge; arsip diberi periode+#ID agar tidak tertukar. Versi sebelumnya tidak dapat membuka schema 4. Cadangkan DB+assets dahulu saat app tertutup.

Batas: tahun1000–9998, hanyaWindowsx64, penerimaan perangkat kedua/offline fisik/Windows125% nyata/usability masih pending. H-02 historis JavaFXListView belum diklaim teratasi. Bukti tes/paket terbaru di REVISION_001.md repository.

## Riwayat sebelum Revision 001

# Catatan kandidat rilis 0.5.1-rc1

Hardening sesudah M5: layar Semester & mata kuliah dimuat ulang setelah guard ketika dibuka kembali. Kehadiran yang baru disimpan melalui Simulasi Nilai kini terbaca sebelum edit nama/SKS, sehingga form lama tidak menimpanya. Regresi lintas layar membuktikan kehadiran false/confirmed true tetap tersimpan setelah metadata berubah.

Engine/skema/dependency tetap. Regresi baru:163tes default,8GUI+1benchmark, serta1pengulangan skala proses125% lulus. Ada oracle hitung independen, uji revisi bobot/order dan integrasi generasi draft dengan DB. Audit dan bahan laporan/OOP/presentasi/kit penerimaan tersedia di repository. Penerimaan manusia belum dinyatakan selesai.

## Fondasi M5 yang dipertahankan

M5 menjaga lingkup P0: akun lokal, akademik multi-semester, dua jalur materi, Markdown/gambar/catatan/completion/resume, aktual/target/skenario/IP. Skema tetap3 dan engine nilai M1 tetap.

Perbaikan integrasi:
- Edit profil/akademik mendapat Simpan/Buang/Batal. Label akademik yang tertinggal dari M2 diperbarui.
- Edit nama/kode/SKS tidak membatalkan verifikasi kehadiran bila centangnya tidak berubah.
- Pilihan pembuka mengikuti semester aktif bila belum ada pilihan layar sebelumnya.
- Save/refresh skenario mempertahankan course yang sedang diedit.
- Status draf terlihat; pergantian mode dikonfirmasi; hasil menampilkan lulus/tidak lulus; perbandingan menyebut Aktual/Asumsi.
- Sidebar belajar digulir, jendela menyesuaikan area layar, bobot/konteks lebih mudah terlihat. Selisih IP panjang memakai tanda≈, keputusan tetap tidak dibulatkan.
- Launcher Windows dengan runtime Temurin21.0.13+9, JavaFX21.0.10, dependensi dan lisensi. Tidak perlu Java/Maven/IDE penerima.

## Batas yang tetap berlaku
- Windows x64; kandidat belum ditandatangani. App-image ZIP, bukan installer; seluruh folder harus diekstrak bersama.
- Pengujian komputer kedua, jaringan fisik diputus, skala Windows125% nyata dan usability3–5mahasiswa perlu hasil pengguna. Pengujian skala proses JavaFX tidak menggantikan perubahan DPI Windows nyata.
- Tidak ada recovery password, enkripsi seluruh DB, sinkronisasi, autosave nilai/skenario, undo/delete recovery atau dukungan beberapa instance yang lengkap.
- Aset tak terpakai sengaja tidak dibersihkan; cadangan manual perlu DB+assets.
- Grouped weighting/pencarian/backup-restore UI P1 serta AI/kuis/PDF/IPK/optimasi antarmatkul P2 tetap tertunda.

Bukti terukur dan keputusan kesiapan ada di DEVELOPMENT_STATUS/RELEASE_ACCEPTANCE repository. Kandidat ini disediakan untuk validasi, bukan klaim seluruh pengujian lapangan selesai.
