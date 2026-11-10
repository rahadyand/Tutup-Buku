# Panduan Tutup Buku 0.5.1-rc1 — Windows x64

## Membuka paket
1. Ekstrak seluruh ZIP memakai **Extract All**, misalnya ke `D:\Aplikasi Saya\TutupBuku`. Jangan menjalankan EXE langsung dari tampilan ZIP.
2. Buka folder hasil ekstraksi **TutupBuku**, lalu klik **TutupBuku.exe**. Java, Maven dan IDE tidak perlu dipasang. Folder runtime dan app harus tetap bersama EXE.
3. Simpan folder paket pada lokasi yang boleh Anda baca. Data ditulis ke **%LOCALAPPDATA%\TutupBuku**, bukan folder paket.
4. Daftar akun lokal: NIM, nama, password minimal8karakter dan konfirmasi; lalu Masuk. NIM teks mempertahankan nol awal. Tidak ada recovery password universal/email.

Paket kandidat belum ditandatangani digital. Bila Windows memblokir, catat pesan dan minta bantuan pengembang; jangan mengubah pengaturan keamanan untuk pengujian ini.

## Profil, semester dan mata kuliah
- Profil: ubah nama dan program studi, kemudian Simpan. NIM tetap.
- Semester & mata kuliah: Semester baru → nomor positif, tahun berurutan misalnya2026/2027, Ganjil/Genap → Simpan. Kombinasi tahun/periode unik dalam akun.
- Jadikan aktif menetapkan semester pembuka untuk kedua ruang dashboard saat login berikutnya. Semester lama tetap ada; pilihan layar dapat diganti tanpa mengubah referensi aktif.
- Tambah mata kuliah dengan nama/SKS positif; kode opsional. Acuan kehadiran dapat diedit. Penanda awal adalah asumsi.
- Perubahan form belum tersimpan ditahan oleh pilihan Simpan/Buang/Batal saat berpindah. Hapus meminta konfirmasi turunan; Batal menjaga data.

## Modul Belajar
1. Pilih Kuliah → semester/course → tambah bab; atau Peminatan → bidang yang terpisah dari semester.
2. Salin5materi contoh jika diinginkan (sekali per akun), atau Materi baru. Konten contoh asli singkat dan dapat diedit.
3. Isi judul wajib, sumber opsional, Markdown. Gunakan heading/list/tabel/fenced code. Ringkasan adalah heading manual `## Ringkasan`.
4. Sisipkan gambar lokal PNG/JPEG; salinan dikelola aplikasi. File asal tidak diperlukan setelah impor. Rumus menggunakan gambar. Batas10MB/16juta piksel.
5. Pratinjau/Baca menampilkan materi. Kode/HTML/script tidak dijalankan. Link http/https dibuka browser setelah klik dan dapat memerlukan internet.
6. Buka panel **Catatan pribadi** untuk mengedit note; panel dapat dilipat agar area baca tetap luas. Catatan terpisah dari isi. Simpan eksplisit atau tunggu autosave2detik. Status selesai ditandai manual, bukan bukti penguasaan materi.
7. Gulir, keluar/tutup, buka kembali dan Lanjutkan Membaca. Revisi isi berubah → awal; materi dihapus → penjelasan. Sidebar panjang dapat digulir.

## Simulasi Nilai
1. Komponen & aktual: pilih course, tambah nama/bobot/perolehan/maksimum. UTS/UAS tidak wajib. Kosong berarti belum diketahui;0 adalah nilai aktual. Bobot0 informatif.
2. Bobot harus tepat100% untuk hasil detail. Struktur salah boleh disimpan sebagai draf, tanpa normalisasi otomatis. Isi earned/max seperti8/10 atau35/40.
3. Mode aktif Detail atau Huruf langsung. Perubahan mode dikonfirmasi; data mode lain tetap tersimpan. Huruf aktual hanya dimasukkan pada tab aktual, estimasi pada skenario.
4. Skenario baru → nama/target IP0–4 → course → minimal target angka/huruf. Hitung target & IP, kemudian Simpan skenario.
5. Contoh:20/30/50%, target60 → semua saran60. Kunci tugas80 → dua saran55. Isi UTS aktual30 di tab aktual → buka skenario, Tinjau/perbarui basis → UAS70. Refresh hanya draf sampai Simpan.
6. Saran adalah normalisasi0–100, bukan perolehan mentah. Angka minimum aman dibulatkan ke atas2desimal. Target mustahil tidak dipotong menjadi100; W=0 tidak dibagi.
7. Kehadiran awal asumsi terpenuhi. Tidak terpenuhi → efektif E/0; hasil asal tetap dan pulih saat dicentang kembali. Verifikasi kehadiran aktual tersedia terpisah.
8. Semua course harus memiliki SKS dan hasil efektif sebelum IP tampil. D/E tetap dihitung. IP Simulasi berarti masih ada asumsi; angka ini bukan transkrip resmi kampus.
9. Duplikasi/edit/save/open/delete skenario terpisah dari aktual. Perubahan bobot/nilai/SKS/daftar course menandai basis usang; jangan menganggap snapshot historis sebagai hasil terkini.
10. Simpan dua skenario lalu Bandingkan tersimpan: course, SKS, huruf/status sumber dan kedua IP. Target IP adalah pembanding manual; tidak ada optimasi antarmatkul/IPK.

## Penyimpanan dan masalah
- Tutup aplikasi sebelum membuat cadangan manual; salin **tutup-buku.db dan assets** bersama. Jangan hanya menyalin ZIP aplikasi sebagai cadangan data.
- Gagal simpan: isian tetap ada. Periksa ruang/izin folder data, lalu tekan Simpan lagi. Jangan menghapus database untuk mengatasi error.
- Versi database tak dikenal/rusak/gagal migrasi dilaporkan tanpa reset. Catat pesan; simpan cadangan file sebelum meminta bantuan.
- Membuka dua instance pada akun/data sama tidak dianjurkan. Save skenario/materi versi lama ditolak agar tidak menimpa perubahan baru.
- Memperbarui paket: tutup aplikasi, ekstrak ke folder paket baru. Data normal tetap berada di AppData. Jangan memindahkan folder runtime secara terpisah.

## Demo terisolasi (opsional)
Di PowerShell pada folder EXE:
```powershell
$env:TUTUP_BUKU_DATA_DIR = "$env:LOCALAPPDATA\TutupBuku-Demo"
& '.\TutupBuku.exe'
```
Ini memakai folder demo terpisah; bukan data normal. Pada PowerShell yang sama, `Remove-Item Env:TUTUP_BUKU_DATA_DIR` mengembalikan pilihan lokasi normal untuk peluncuran berikutnya, tanpa menghapus berkas.

Untuk uji jaringan terputus/komputer kedua/skala Windows125%, ikuti RELEASE_ACCEPTANCE.md. Pengembang tidak mengubah pengaturan global tersebut secara otomatis.

## Hasil pemeriksaan kandidat ini — 10 November 2026

ZIP0.5.1-rc1 bertimestamp20261109-224207 sudah diuji melalui EXE ekstraksi dengan data demo terisolasi. Nilai aktual8/10, course3SKS, semester aktif dan skenario tersimpan bertargetIP3 tetap terbaca setelah tutup/buka ulang; IP Simulasi tetap3,70. Seluruh227file ZIP cocok dengan ekstraksi yang diuji. Path/hash dan batas bukti ada di HARDENING_VERIFICATION.md pada repository. Dokumen panduan repository ini diperbarui sesudah paket dibuat; ZIP tidak dibangun ulang.

Log uji memuat exception internal JavaFX daftar yang belum dipastikan pemicunya, meskipun data tetap dan aplikasi ditutup normal. Jika daftar bermasalah, catat urutan klik dan pesan, tutup normal bila memungkinkan, cadangkan DB+assets ketika aplikasi tertutup, lalu laporkan. Jangan menghapus/reset DB. Kandidat belum dinyatakan selesai penerimaan lapangan: komputer kedua, internet fisik terputus, Windows125% nyata dan usability mahasiswa masih belum diuji.
