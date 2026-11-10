# Batas dan masalah yang diketahui

Kandidat0.5.1-rc1. Tidak ditemukan defek kritis lain dalam cakupan audit/tes yang dijalankan; bukan jaminan bebas bug. H-01 stale attendance lintas layar telah diperbaiki dan diregresi. Rincian bukti pada HARDENING_VERIFICATION.

| Hal | Dampak dan langkah sementara | Status |
|---|---|---|
| Perangkat kedua/offline fisik/Windows125% nyata belum diuji | Jalankan kit, catat hasil dan reproduksi. Emulasi/proxy bukan pengganti | NOT PERFORMED |
| Usability3–5mahasiswa belum dilaksanakan | Keterpahaman label/alur belum dibuktikan; manfaat belajar tidak dapat diklaim | NOT PERFORMED |
| ZIP app-image belum ditandatangani/installer | Semua folder harus diekstrak; SmartScreen/AV/perizinan perangkat lain belum diketahui. Catat blokir, jangan menonaktifkan perlindungan | Batas distribusi |
| HanyaWindowsx64 teruji | OS/arsitektur lain belum didukung/dibuktikan | Batas platform |
| DB tidak terenkripsi, tanpa recovery password | Akses file akun Windows berada di luar isolasi aplikasi; gunakan sandi uji untuk demo dan jangan bagikanDB nyata | Batas keamanan lokal |
| Multi-instance tidak lengkap | CAS materi/skenario menolak save lama, tetapi metadata/aktual bukan sistem kolaborasi; gunakan satu instance | Batas lingkup |
| Orphan assets sengaja tidak dibersihkan | Ukuran folder dapat bertambah; backup DB+assets bersama saat app tertutup; jangan hapus manual tanpa memeriksa referensi | Deferred |
| Tidak ada autosave nilai/skenario | Simpan eksplisit; guard mencegah pindah diam-diam; autosave hanya materi/note | Keputusan lingkup |
| Tidak ada undo penghapusan/backup UI | Baca konfirmasi; cadangkan manual sebelum data penting/migrasi | Deferred |
| Form panjang perlu scroll pada1366×768/125% | Screenshot proses ditinjau dan kontrol dapat dicapai; penerimaan keyboard/DPI nyata masih perlu | Batas UX |
| Warning unnamed JavaFX module, unchecked helper, jlinkcompress=2 | Tidak menggagalkan build/tes; tidak berarti hasil lapangan telah lulus | Diketahui |

P1/P2 tetap pada BACKLOG. Jika ditemukan crash, kehilangan data, hasil rumus salah atau akses akun lain, hentikan pemakaian data nyata, simpan bukti/cadangan tanpa menghapus yang rusak, dan prioritaskan perbaikan/uji ulang sebelum pembekuan.

## H-02 — exception JavaFX daftar, belum direproduksi terfokus

Log run kedua paket memuat IndexOutOfBoundsException dari ReadOnlyUnbackedObservableList/ListViewBehavior. Pemicu dan waktu tepat belum dipastikan. Aktual8/10 dan skenario/IP3,70 tetap berhasil dibuka kembali; tutup normal exit0, quick_check/FK dan data fixture sah. Belum ada bukti kehilangan data, tetapi exception ini tetap belum selesai. Catat langkah klik/pilihan daftar jika muncul lagi; jangan mereset DB. Pemeriksaan visual saja tidak menggantikan log. Paket tidak diubah untuk masalah yang belum dipastikan ini.
