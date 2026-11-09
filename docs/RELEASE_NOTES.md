# Catatan kandidat rilis 0.5.0-rc1

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
