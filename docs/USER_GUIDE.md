# Panduan Tutup Buku 0.5.5-rc1 — Windows x64

## Membuka paket
1. Ekstrak seluruh ZIP memakai **Extract All**, misalnya ke `D:\Aplikasi Saya\TutupBuku`. Jangan menjalankan EXE langsung dari tampilan ZIP.
2. Buka folder hasil ekstraksi **TutupBuku**, lalu klik **TutupBuku.exe**. Java, Maven dan IDE tidak perlu dipasang. Folder runtime dan app harus tetap bersama EXE.
3. Simpan folder paket pada lokasi yang boleh Anda baca. Data ditulis ke **%LOCALAPPDATA%\TutupBuku**, bukan folder paket.
4. Daftar akun lokal: NIM, nama, password minimal8karakter dan konfirmasi; lalu Masuk. NIM teks mempertahankan nol awal. Tidak ada recovery password universal/email.

Paket kandidat belum ditandatangani digital. Bila Windows memblokir, catat pesan dan minta bantuan pengembang; jangan mengubah pengaturan keamanan untuk pengujian ini.

## Akun, profil, semester dan mata kuliah — Revision 002

- Login/Daftar terpusat dan ringkas, dengan judul/deskripsi kegunaan aplikasi. Sapaan pendaftaran mengikuti nama dan NIM yang diketik; sebelum login, nama akun tersimpan tidak ditampilkan. Pola NIM L0125001 = L +01 Informatika +25 angkatan 2025 +001 identitas; L0225001 memakai02 Sains Data. Ini panduan, bukan verifikasi kampus atau larangan masuk bagi akun lama. Nol awal tetap dan NIM lama tidak diubah.
- Password tersembunyi awal; Tampilkan/Sembunyikan tersedia terpisah untuk login, password daftar dan konfirmasinya. Visibility kembali tersembunyi setelah sukses/berpindah tab. Enter pada field password tetap mengirim form. Gagal simpan/login tidak mengosongkan isian.
- Profil terpusat: ubah nama dan pilih Informatika atau Sains Data, lalu Simpan. Nilai lama yang dikenali ditampilkan dengan label baku. Nilai kosong/tidak dikenali tetap tersimpan dengan dropdown kosong hingga Anda memilih pengganti; membuka profil tidak mengubah database. Prodi tidak diisi otomatis dari NIM. NIM tidak dapat diubah.
- Shortcut Modul Belajar/Simulasi Nilai selalu di kanan atas, berdampingan di bawah Keluar yang rata kanan. Tab: Dashboard, Semester & Mata Kuliah, Profil; perubahan belum tersimpan tetap mendapat Simpan/Buang/Batal.
- Halaman Semester: pilih data tersimpan atau Semester Baru di bawah Hapus Semester dan teks petunjuk; isi nomor positif dan tahun tunggal4 digit (1000–9998), misalnya2026. Jadikan Aktif langsung menyimpan+aktivasi lalu ke halaman course; tidak perlu Simpan dahulu. Simpan Semester juga ke course tetapi tidak mengganti semester aktif lain.
- Halaman course: daftar semester di kiri dan mata kuliahnya di kanan. Sorotan berarti dipilih; [aktif] adalah referensi aktif akun. Isi nama, kode opsional, SKS positif; Simpan/Hapus dekat form; Mata Kuliah Baru berada tepat di bawah status simpan. Gulir bila bagian bawah belum terlihat. Kembali ke halaman Semester untuk mengedit/aktivasi/hapus semester.
- Tidak ada kehadiran pada form course. Nama/kode/SKS dapat diubah tanpa mengubah kehadiran, konfirmasi atau acuan. Acuan/checkbox tersedia pada Simulasi Nilai → Komponen & aktual. Course baru tetap berasumsi kehadiran terpenuhi, belum diverifikasi.
- Arsip dari format lama mempertahankan ID/periode/data. Label menampilkan tahun tunggal serta arsip Ganjil/Genap+#ID supaya dua arsip yang sama nomor/tahun tidak tertukar. Tidak ada penggabungan otomatis. Cadangkan DB+assets saat app tertutup sebelum memperbarui; versi lama tidak membuka schema 4.

## Modul Belajar — Baca, Ubah Materi, Kelola Daftar

1. Pada katalog kiri, buka **Lokasi belajar** untuk memilih Kuliah → semester/mata kuliah, atau Peminatan. Pilih bab/bidang lalu materi. Materi langsung terbuka dalam **Mode Baca**; isi bergulir dalam area baca kanan.
2. **Tutup daftar materi** meluaskan bacaan; **Buka daftar materi** mengembalikan katalog beserta pilihan dan posisi baca. Ini tidak memunculkan konfirmasi atau mengubah isi.
3. **Ubah Materi** membuka editor materi terpilih. Isi judul wajib, sumber opsional, Markdown, lalu **Simpan** atau tunggu autosave dua detik. Tab **Pratinjau / Baca** untuk memeriksa hasil; **Kembali Membaca** menutup editor. Ringkasan adalah bagian manual `## Ringkasan`, bukan ringkasan AI.
4. **Sisipkan gambar lokal** memakai PNG/JPEG yang disalin aplikasi, maksimum10MB/16juta piksel. Rumus awal memakai gambar. Tabel dan fenced code didukung. HTML/script tidak dijalankan. Link http/https hanya membuka browser setelah diklik dan mungkin memerlukan internet.
5. Buka panel **Catatan pribadi** sambil membaca, tanpa perlu Ubah Materi. **Simpan catatan** atau autosave menyimpan catatan terpisah dari konten; status menyebut catatan saat hanya catatan berubah. Checkbox **Selesai** tetap manual, bukan bukti penguasaan materi. Lipat catatan untuk meluaskan bacaan.
6. **Kelola Daftar** menampilkan nama bab/bidang terpilih, tambah/ubah nama/hapus, urutan naik/turun, tambah/ubah/hapus/urutkan materi, salin lima materi contoh sekali per akun, serta muat ulang. Pilih objek melalui katalog yang sama; **Kembali Membaca** menutup pengelolaan. Penghapusan tetap meminta konfirmasi.
7. Perubahan belum tersimpan saat berpindah materi/lokasi/mode, logout atau menutup jendela menawarkan **Simpan / Buang perubahan / Batal**. Gagal simpan mempertahankan isian dan menghentikan perpindahan; Batal mempertahankan pilihan/mode. Perpindahan bersih tidak meminta konfirmasi. Jangan menutup paksa saat menyimpan.
8. Kembali dari editor/pengelolaan mempertahankan rasio baca bila revisi isi sama. Jika isi berubah, kembali ke awal sesuai aturan resume. **Lanjutkan Membaca** memulihkan bacaan terakhir; materi dihapus menampilkan penjelasan. Pada jendela minimum760×480 katalog dan editor dapat digulir; menutup katalog memberi lebar lebih luas.

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

## Bukti historis kandidat0.5.1 — 10 November 2026

ZIP0.5.1-rc1 bertimestamp20261109-224207 sudah diuji melalui EXE ekstraksi dengan data demo terisolasi. Nilai aktual8/10, course3SKS, semester aktif dan skenario tersimpan bertargetIP3 tetap terbaca setelah tutup/buka ulang; IP Simulasi tetap3,70. Seluruh227file ZIP cocok dengan ekstraksi yang diuji. Path/hash dan batas bukti ada di HARDENING_VERIFICATION.md pada repository. Dokumen panduan repository ini diperbarui sesudah paket dibuat; ZIP tidak dibangun ulang.

Log uji memuat exception internal JavaFX daftar yang belum dipastikan pemicunya, meskipun data tetap dan aplikasi ditutup normal. Jika daftar bermasalah, catat urutan klik dan pesan, tutup normal bila memungkinkan, cadangkan DB+assets ketika aplikasi tertutup, lalu laporkan. Jangan menghapus/reset DB. Kandidat belum dinyatakan selesai penerimaan lapangan: komputer kedua, internet fisik terputus, Windows125% nyata dan usability mahasiswa masih belum diuji.

## Bukti kandidat0.5.5 — 19 November 2026

Paket `TutupBuku-0.5.5-rc1-20261119-060834` berasal dari commit source `636a147454159c33b93b8d2d245c677d4b3f1c0b`. JAR paket identik dengan input build; semua230file ZIP identik dengan payload ekstraksi yang diuji. Dua alur otomatis memakai runtime dan JAR distribusi lulus, termasuk catatan, kegagalan/guard, resume dan tutup/buka ulang. EXE juga diuji masuk → baca → simpan catatan → tutup(exit0) → buka ulang; catatan tetap terbaca pada DB fiktif terisolasi. Ini bukan uji usability manusia atau komputer kedua. Detail hash, lingkungan, batas dan bukti ada di DEVELOPMENT_STATUS repository. Catatan hasil ini ditambahkan setelah paket dibuat; ZIP tidak diregenerasi untuk perubahan bukti dokumentasi.

Review singkat Anda: pilih bacaan dan gulir; tutup/buka katalog; tulis catatan tanpa editor; Ubah Materi lalu Kembali Membaca; buka Kelola Daftar; coba Batal saat ada perubahan. Pada minimum760×480, gulir form/katalog dan lipat catatan. Paket lama tetap tersedia; gunakan0.5.5 untuk menilai layar baru ini.
