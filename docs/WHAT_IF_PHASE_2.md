# Phase 2 — What If, skenario tersimpan, dan IP semester

Selesai pada 19 November 2026 (WIB), dibangun dari `main/eda5d315b72fb26cc1896ad034e2324f152b3b45` (Phase 1). Lingkup mengikuti instruksi Phase 2 pengguna dan PRD v1.1 F08–F13. Dashboard, navigasi global, modul belajar, model bobot, dan engine perhitungan tidak diubah. Tidak ada dependensi atau paket rilis baru.

## Alur pengguna

1. Buka **Simulasi Nilai → Komponen & aktual → Simulasi Target (What If)**. Pilih satu target angka/huruf, kunci asumsi, atau override nilai aktual sementara. Perhitungan tetap otomatis; aktual asli selalu ditampilkan.
2. Tekan **Simpan sebagai skenario** dan beri nama. Ini menyimpan **satu rencana semester**, dengan mata kuliah terpilih memakai eksperimen tersebut. Mata kuliah lain memakai hasil aktualnya saja; target C tidak diterapkan diam-diam untuk mengisi hasil yang kosong. Nama sama diperbolehkan sesuai aturan lama, tetapi ID selalu membedakan record.
3. Sesudah simpan, konteks mata kuliah tetap. Status berubah dari **Sementara** menjadi **Tersimpan — nama**. Perubahan berikutnya berlabel **Perubahan belum disimpan**. **Perbarui skenario** memperbarui ID yang sama melalui CAS; **Simpan salinan** menyimpan draf saat ini dengan ID baru tanpa mengubah skenario asal.
4. **IP & perbandingan** menyerahkan draf semester yang sama ke layar skenario lama. Pilih mata kuliah lalu **Buka What If mata kuliah** untuk kembali, termasuk perubahan yang belum disimpan. Tidak ada salinan draf paralel. Untuk mata kuliah yang masih hanya aktual, ubah target/asumsi di What If atau centang **Gunakan target/asumsi sebagai proyeksi mata kuliah ini** di layar semester.
5. Layar semester tetap menyediakan mode detail/huruf langsung, estimasi huruf, kehadiran, target IP, hitung, simpan, salin, hapus, tinjau basis, dan perbandingan. Target IP awal rencana yang dibuat dari What If adalah 3, dapat diedit. Estimasi huruf dan kehadiran skenario diedit di layar ini; What If mempertahankannya saat bolak-balik.
6. **Reset sementara** membersihkan eksperimen terhadap basis saat masuk. **Pulihkan tersimpan** membaca ulang versi tersimpan, termasuk target dan basis lama; ini bukan mengambil aktual terkini. Reset draf berubah meminta Buang/Batal. Keluar dari draf berubah menawarkan **Simpan/Buang/Batal**, berlaku pula pada perpindahan semester/workspace, logout, dan tutup aplikasi. Handoff What If–IP mempertahankan draf tanpa dialog buang yang berulang. Batal mempertahankan mode, pilihan, dan isian.
7. Save gagal mempertahankan seluruh isian/konteks dan menahan transisi; pesan kegagalan ditampilkan dekat tombol simpan dan area digulir ke pesan. Nama percobaan pertama disimpan untuk retry. Tombol simpan berulang tidak membuat dua record selama proses berlangsung. Tidak ada autosave skenario.

## Aktual, basis, dan skenario usang

`ScenarioCourse` menyimpan basis immutable earned/max, revision, target, kunci unknown, override aktual terpisah, kehadiran, mode/estimasi, serta flag proyeksi. `GradePlanningService` mengevaluasi override pada salinan sementara; tidak memanggil penulisan aktual. Kebutuhan solver tetap keluaran turunan. Kolom saran lama tetap merupakan cache hasil, bukan kunci atau nilai aktual saat membuka ulang.

Membuka ulang menghasilkan target/asumsi/hasil pada basis tersimpan. Perubahan aktual, komponen, urutan, SKS atau metadata yang menaikkan revisi memberi label **BASIS USANG / historis**. **Tinjau / perbarui basis** memakai review/refresh lama, menjelaskan kunci/override yang tidak kompatibel. Refresh hanya mengubah draf sampai Simpan. Kunci dipertahankan hanya pada ID yang masih unknown dan berbobot positif; override hanya pada ID yang masih aktual dan berbobot positif. Urutan baris tidak menentukan identitas. Komponen terhapus tidak dipindahkan ke komponen lain; tidak ada skor pengganti rekaan. Aturan pergantian mode tetap mengikuti workflow M4.

## IP dan perbandingan

IP memakai satu snapshot semester, bukan gabungan otomatis skenario per mata kuliah. Panel **Sumber hasil tiap mata kuliah untuk IP** menunjukkan SKS, detail aktual/huruf langsung/proyeksi/estimasi/belum lengkap, hasil asal dan efektif, serta status asumsi kehadiran. Mata kuliah wajib yang belum masuk snapshot juga menahan IP dan meminta tinjau basis. Nilai kosong tidak menjadi E/0 dan IP parsial tidak ditampilkan. IP lengkap berlabel **IP Simulasi** bila ada proyeksi/asumsi; hasil historis ditandai bukan IP semester terkini.

Perbandingan memakai layar lama, membuka dua snapshot tersimpan (simpan perubahan dahulu). Nama, target IP, target per mata kuliah, angka/huruf asal dan efektif, sumber serta IP lengkap/belum lengkap ditampilkan. Tabel membungkus teks dan tinggi baris mengikuti konten; jendela sempit dapat memerlukan gulir tabel. Rincian per mata kuliah menunjukkan perbedaan basis/SKS/revisi/kehadiran, aktual earned/max A/B, kunci, override dan kebutuhan. Mata kuliah dan komponen dibandingkan lewat ID stabil. Yang hilang ditulis tidak ada di snapshot. Basis berbeda/usang dilabeli eksplisit sebagai perbandingan historis, bukan hanya perbedaan asumsi. Semester berbeda ditolak; tidak ada kombinasi skenario baru.

## Perubahan kecil pada penyimpanan

Model lama belum dapat menyimpan override Phase 1 tanpa menimpa earned/max basis, dan belum membedakan course hanya aktual dari course yang sengaja diberi target. Karena itu schema **4 → 5** menambahkan dua kolom dalam transaksi:

- `scenario_items.override_score TEXT` nullable, terpisah dari `earned`, `maximum`, `locked_score`, dan `suggested_score`.
- `scenario_courses.projection_enabled INTEGER NOT NULL DEFAULT 1`, untuk mempertahankan perilaku skenario lama. Skenario lama mendapat override kosong.

ID, akun, data akademik/belajar, basis, dan skenario lama tidak di-reset. Migrasi gagal di-rollback dan dilaporkan; database rusak/versi tak dikenal tidak diganti diam-diam. Query pemilik, FK, transaksi header+children dan CAS `Scenario.version` yang sudah ada dipakai ulang. Semua tes memakai TempDir.

**Sebelum mencoba source ini pada data sendiri:** tutup aplikasi dan cadangkan seluruh folder data (database beserta aset). Migrasi ke schema 5 berlangsung saat pembukaan. EXE/ZIP schema 4 lama tidak dapat membuka database schema 5; jangan gunakan paket lama untuk database yang sudah dimigrasikan. Tidak ada downgrade otomatis. Uji dahulu dengan `TUTUP_BUKU_DATA_DIR` terpisah. Paket 0.5.5 lama tidak mewakili source Phase 1/2; versi Maven lama masih dipakai untuk build pengembangan, bukan identitas paket baru.

## Layanan dan berkas

- `WhatIfPane`, `ScenarioPane`, `GradeView`, `GradeForms`: satu handoff draf, state/simpan/salin/reset/guard, sumber IP dan perbandingan kontekstual; tetap native JavaFX dengan tema/factory lama.
- `GradePlanningService`: reuse save/open/review/refresh/evaluate, CAS di repository, evaluator/solver/GPA lama; tambahan pembentukan rencana dari What If dan salinan draf.
- `ScenarioCourse`, `GradeSchema`, `SqliteDatabase`, `SqliteGradeRepository`: dua informasi yang sebelumnya belum dapat direpresentasikan dan migrasi aditifnya. Constructor lama dipertahankan.
- `ScenarioJourneyFxCheck`, `ScenarioMigrationTest`, perluasan `GradePlanningPersistenceTest`, serta fixture versi lama: perjalanan pengguna dan regresi penyimpanan.

## Verifikasi aktual

Temurin 21.0.13+9, JavaFX 21.0.10, Maven Wrapper 3.9.11. Log lokal: `tmp/what-if-phase2-20261119/`; gambar: `target/verification/phase2-*.png`. Ini pengujian aplikasi produksi dalam JavaFX, bukan usability manusia.

| Pemeriksaan | Hasil |
|---|---|
| `verify` akhir (`verify-final.log`) | 194 kasus, 0 gagal/error/skip |
| GUI akhir: ScenarioJourneyFxCheck, WhatIfFxCheck, M4, M5, ModernWorkspace; reuseForks=false (`gui-final.log`) | 5 kasus, 0 gagal/error/skip |
| Journey + What If, glass.win.uiScale=125% (`gui-scale.log`) | 2 kasus, 0 gagal/error/skip; emulasi proses, bukan DPI Windows nyata |
| Inspeksi visual JavaFX | Sementara, tersimpan, berubah, gagal simpan, usang, perbandingan dan komponennya, IP lengkap/belum lengkap, kosong, 1366×768 dan 1024×768; emulasi 125% juga diperiksa |

Enam kasus persistence baru meliputi roundtrip/restart basis+target+lock+override, derived bukan input, nama sama/ID berbeda, pemilik, update/salin independen, IP aktual/detail/direct/proyeksi/belum lengkap, course wajib hilang, refresh ID stabil/reorder/removal, transaksi gagal/retry, dan kehadiran E/0 dengan hasil asal dipertahankan, termasuk saat proyeksi target dimatikan pada mode detail/huruf langsung. Tiga kasus migrasi memeriksa schema 4/dua kali reopen, rollback saat ALTER gagal lalu retry, serta schema 5 tidak lengkap tanpa reset. Fixture migrasi 3 lama diperbaiki agar benar-benar tidak mempunyai kolom schema 5.

Perjalanan GUI mencakup sementara → simpan (termasuk aktivasi ganda) → ubah/Batal → gagal simpan saat keluar/retry → simpan salinan → pulihkan basis tersimpan → IP belum lengkap → aktual berubah/usang → refresh draf/explicit save → IP lengkap → perbandingan basis/override → duplikasi → logout/login → tutup/buka ulang. Aktual dibandingkan sebelum/sesudah operasi, kecuali perubahan aktual eksplisit dalam fixture. Regresi Phase 1 tetap memeriksa 60 → 55 → 70, 84,5/A-, impossible, bobot/kehadiran/guard dan input invalid; engine murni tidak diubah.

Inspeksi awal menemukan teks tabel terpotong dan pesan gagal simpan di bawah lipatan. Tinggi sel kini mengikuti label pembungkus; tes geometri mengukur tinggi teks yang benar-benar dialokasikan, dan pesan gagal simpan terlihat dekat aksi. Regresi akhir di atas dijalankan sesudah perbaikan. Peringatan classpath JavaFX unnamed module tetap muncul pada harness lama, tanpa kegagalan tes. Graphify AST akhir: 1280 node, 4717 relasi, 0 endpoint yatim dan 1 self-loop statis yang sudah ada; tidak ditafsirkan sebagai bug runtime atau diaudit di luar lingkup.

## Menjalankan dan meninjau

Dari root proyek, PowerShell dengan JDK 21 (cache lingkungan sesi ini):

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME="$PWD\.tools\maven-user"
$env:TUTUP_BUKU_DATA_DIR="$PWD\tmp\phase2-review-data"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o javafx:run
```

Untuk instalasi biasa, gunakan JDK 21 sendiri dan `JAVA_HOME` yang sesuai; hilangkan `-o` pada unduhan pertama. Folder demo di atas terpisah dari data pribadi dan tetap bertahan setelah aplikasi ditutup. Gunakan akun/semester/mata kuliah demo milik sendiri, bukan data nyata untuk tes.

```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o '-Dtest=ScenarioJourneyFxCheck,WhatIfFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ModernWorkspaceFxCheck' '-DreuseForks=false' test
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o '-Dtest=ScenarioJourneyFxCheck,WhatIfFxCheck' '-DreuseForks=false' '-DargLine=-Dglass.win.uiScale=125%' test
```

Review manual yang masih diperlukan:

1. Simpan eksperimen bernama, ubah target, coba Batal saat keluar; pastikan isian tetap. Bandingkan Perbarui dengan Simpan salinan, lalu buka ulang aplikasi.
2. Ubah aktual secara eksplisit; buka skenario lama, periksa label usang dan review sebelum refresh. Pastikan refresh belum mengubah versi tersimpan hingga Simpan.
3. Lihat course lain yang belum lengkap menahan IP, aktifkan proyeksinya secara sadar, lalu inspeksi sumber IP dan perbandingan. Pastikan nilai aktual tidak berubah.
4. Coba keyboard, jendela kecil, DPI Windows nyata dan kenyamanan membaca. Emulasi JavaFX 125% tidak menggantikan uji DPI/perangkat fisik.

Belum diverifikasi: usability mahasiswa, screen reader/keyboard fisik, DPI Windows nyata dan komputer kedua. H-02 ListView historis tetap open; tidak ada audit/perubahan di luar fase. Tidak membangun installer/ZIP, tidak memvalidasi ulang paket lama atau mengerjakan fase berikutnya. Graphify diperbarui lokal AST tanpa API; artefak dan draf awal tetap di luar commit Phase 2. Hash commit/push diverifikasi di receipt lokal dan handoff akhir.
