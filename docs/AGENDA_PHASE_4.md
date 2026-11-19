# Phase 4 — Agenda bersama: tugas dan ujian

19 November 2026 (WIB). Baseline nyata `main/11047086b237596a3d17725082a99ad3ad5f86d1`, schema 6. Agenda adalah perluasan atas instruksi Phase 4 pengguna; tidak mengarang ID fitur PRD baru. Aturan akun/akademik/belajar/nilai PRD v1.1 dan Phase 1–3 tetap. Tidak ada dashboard redesign, dependency atau paket rilis baru.

## Alur pengguna

- **Dashboard → Agenda** menyediakan daftar lintas course pada semester yang dilihat. Pilih semua/satu mata kuliah, Tugas/Ujian/keduanya, serta Belum selesai/Selesai/Semua status. Default awal Belum selesai. Semester aktif akun tetap terpisah; membuka arsip tidak mengaktifkannya.
- **Mata Kuliah → Tugas & Ujian** memakai AgendaView dan record yang sama. Course sudah dipilih dan terkunci pada formulir; pengguna tidak memilih lokasi yang sama lagi. Ke Agenda global mempertahankan semester/course hub. Kembali dari agenda dengan satu course membawa konteks tersebut ke hub; filter Semua course membawa semester dilihat dan hub memilih course tersedia pertama. Tidak ada record hasil penyalinan atau daftar persisten kedua.
- **Tambah tugas / ujian** membuka form: course wajib, judul wajib, jenis, tanggal wajib dd/MM/yyyy, jam WIB opsional HH:mm, prioritas (Normal awal), deskripsi teks opsional. Pada agenda global, pilih course yang valid di semester tersebut. Bila belum ada semester/course, gunakan Semester & Mata Kuliah dahulu; tombol Tambah memberi petunjuk dan tidak membuat item palsu.
- **Ubah → Simpan** menyimpan record ber-ID sama; prioritas, tanggal/jam, deskripsi dan langkah dapat diedit. Course dapat diganti ke course sendiri pada semester dilihat melalui form global. Form hub mempertahankan course. Fokus awal diarahkan ke course bila belum dipilih, atau judul; input pendek dibatasi lebar lokal. Save/Batal dan pesan status tetap di bawah, isi form menggunakan satu ScrollPane.
- Input invalid atau save gagal tidak dihapus. Kembali/Batal, pergantian filter lokasi, course/semester hub, section, tab, pintu global, logout dan close memakai Simpan/Buang/Batal lama. Batal menahan mode, selector dan draf; Save gagal menahan transisi. Buang hanya membuang draf, tanpa perubahan data tersimpan. Nilai/skenario tetap memakai simpan eksplisitnya dan materi tetap memakai autosave lama.
- Hapus item meminta konfirmasi; hanya item tersebut dan langkah-langkahnya yang dihapus. Status selesai dapat dibuka kembali. Filter Selesai tetap menyediakan record yang hilang dari filter Belum selesai setelah completion.

## Tanggal, waktu dan urutan

Tanggal tugas berarti tenggat; tanggal ujian berarti jadwal/tanggal mulai. Jam kosong disimpan SQL NULL dan tampil **Tanpa jam**, bukan diisi 00:00. Jam 00:00 hanya berlaku bila diisi eksplisit. Deskripsi adalah teks biasa, tanpa attachment/renderer Markdown.

`AgendaService` menggunakan Clock standar Java dengan zona Asia/Jakarta (WIB), dapat diganti Clock.fixed untuk tes:

- Tugas tanpa jam tetap Hari Ini sepanjang tanggal lokal tersebut dan baru Terlambat pada tanggal berikutnya.
- Tugas dengan jam menjadi Terlambat **sesudah** tenggat; tepat pada tenggat belum terlambat.
- Ujian lewat berlabel **Sudah lewat — ujian**. Ujian hari ini tanpa jam tetap Hari Ini sampai tanggal berikutnya. Ujian berjam masuk kelompok sudah lewat sesudah waktu mulai; tidak diasumsikan gagal atau belum mengumpulkan tugas.
- Item selesai masuk kelompok Selesai. Kelompok terbuka: Terlambat, Sudah lewat — ujian, Hari Ini, Mendatang. Di dalam kelompok: tanggal, tanpa jam lebih dahulu pada tanggal sama, kemudian jam, lalu ID stabil. Prioritas tetap berupa teks Tinggi/Normal/Rendah dan tidak menggantikan urutan kronologis. Mengurutkan tanpa jam lebih dahulu bukan mengubah tenggatnya.

Satu pembentukan daftar membaca Clock sekali agar item tidak hilang/berpindah ganda saat melintasi batas waktu. Klasifikasi dihitung saat daftar dibuka/dimuat ulang/filter berubah; gunakan Muat ulang bila aplikasi dibiarkan terbuka melewati tenggat. Tidak ada timer reminder, background service atau notifikasi.

## Checklist dan completion

Langkah hanya untuk Tugas. Setiap langkah memiliki UUID stabil, teks wajib, urutan List tersimpan sebagai position, serta completion sendiri. Tambah, ubah teks, centang, naik/turun dan hapus langkah adalah perubahan draf sampai Simpan. Hapus langkah meminta konfirmasi. Mengganti Tugas menjadi Ujian ditolak selama checklist masih ada; hapus langkah secara eksplisit bila memang ingin mengganti jenis. Batal/gagal simpan mempertahankan data sebelumnya.

Centang semua langkah **tidak otomatis menyelesaikan tugas**. Penyelesaian parent dilakukan dari daftar. Bila masih ada langkah terbuka, konfirmasi “Tetap selesaikan / Batal” muncul dengan default Batal. Membatalkan tidak menulis data. Konfirmasi menyelesaikan parent tanpa mencentang langkah lain. Reopen mempertahankan checklist. Parent sudah selesai boleh tetap mempunyai checklist parsial; status parent dan progress langkah terpisah dan selalu terlihat.

Agenda tidak membuat/menyalin assessment, mengisi skor, mengubah Course.revision, memaksakan E/0, atau mengklaim kelulusan. Hasil aktual, proyeksi What If, saved scenarios dan IP tetap ditentukan layanan akademik lama. Ujian selesai berarti penanda pribadi selesai saja.

## Penyimpanan dan kepemilikan

Schema **6 → 7** menambah `agenda_items`, `agenda_steps`, dan indeks owner/course/date dalam satu transaksi. Item memakai ID SQLite AUTOINCREMENT, course+owner, tipe, tanggal ISO, waktu nullable, prioritas, completion dan version. Langkah memakai UUID global, owner, FK parent, position unik per parent dan completion. `AgendaItem`/`ChecklistStep` immutable; urutan tidak menjadi identitas.

Seluruh list/load/save/complete/delete memvalidasi owner dari UserSession. Filter semester/course diperiksa terhadap akun; course asing ditolak. ID langkah yang telah dimiliki item atau akun lain ditolak sebelum header/children diubah. FK owner+parent mencegah relasi silang. Query berparameter, transaksi header+checklist dan CAS version menolak perubahan lama; error mempertahankan draf untuk koreksi/retry atau buka versi terbaru. Tidak ada overwrite paksa.

Hapus course mengikuti kebijakan live-data lama: agenda dan checklist ikut cascade seperti materi/nilai aktual course tersebut. Hapus semester ikut menghapus agenda course di dalamnya. Dialog akademik menyebut tugas/ujian dan checklist secara eksplisit. Snapshot skenario historis tetap mengikuti kebijakan M4; penghapusan satu item agenda tidak menghapus course, materi atau nilai.

Akun, semester aktif, learning content/aset/catatan/completion, nilai, skenario, serta riwayat baca global/per course tidak di-reset oleh migrasi. Migrasi gagal di-rollback; versi tak dikenal/struktur rusak dilaporkan tanpa DB pengganti.

**Sebelum menggunakan data pribadi:** tutup aplikasi dan cadangkan seluruh folder data (DB + aset). Coba dahulu pada folder demo terpisah. Source schema 6 dan EXE lama tidak mendukung DB schema 7; tidak ada downgrade otomatis. ZIP 0.5.5 historis tidak mewakili Phase 1–4. Versi Maven lama masih untuk build pengembangan, bukan identitas paket baru.

## Bagian yang dipakai ulang

| Bagian | Peran |
|---|---|
| AgendaItem, ChecklistStep dan enum | Data immutable/validasi sederhana, tanpa hierarki scheduling |
| AgendaService | Parser tanggal/jam, Clock, klasifikasi/urutan, aturan konfirmasi completion; tidak mengenal JavaFX/SQL |
| AgendaRepository / SqliteAgendaRepository | Owner, relasi, CAS dan transaksi SQLite memakai SqliteDatabase lama |
| AgendaSchema | Migrasi/validasi versi melalui PRAGMA user_version lama |
| AgendaView | Satu editor/daftar, form dan filter; FormSupport, GradeForms.guard, UiTasks, native controls dan token tema lama |
| DashboardView / CourseHubView / WorkspaceView | Tab Agenda dan section Tugas & Ujian; melepas parent sebelumnya sebelum memindahkan instance yang sama; guard seluruh workspace tetap |
| AcademicView | Pesan konfirmasi cascade diperjelas, operasi akademik tidak diubah |

## Verifikasi aktual

Temurin 21.0.13+9, JavaFX 21.0.10, Maven Wrapper 3.9.11. Semua DB memakai TempDir; tidak mengakses AppData pengguna. Bukti lokal: `tmp/agenda-phase4-20261119/` dan `target/verification/agenda-*.png`.

| Pemeriksaan | Hasil |
|---|---|
| Maven verify | 216 kasus, 0 gagal/error/skip pada source final (`verify-final.log`) |
| GUI produksi | 10 kasus pada 9 kelas, 0 gagal/error/skip (`gui-final.log`) |
| Emulasi 125% | 2 kasus AgendaFxCheck + CourseHubFxCheck lulus, 0 gagal/error/skip (`gui-scale.log`); emulasi proses, bukan DPI Windows nyata |

Delapan tes service/domain mencakup tanggal invalid/leap date, jam opsional versus 00:00, batas tepat/sesudah tenggat, midnight hari lokal, past exam/completed, urutan tie ID, Clock zona WIB dan satu pembacaan Clock per grouping. Delapan tes persistence mencakup CRUD/urutan/restart/SQL NULL, konfirmasi completion dan checklist tetap, semua langkah selesai tanpa auto-parent, isolasi owner/course/semester/step-parent, perpindahan course tanpa duplikasi/aktivasi semester, delete/cascade, rollback/retry/CAS, serta migrasi Phase 1–3 + payload aset dan schema rusak tanpa reset.

AgendaFxCheck menjalankan aplikasi produksi: agenda kosong, seluruh kelompok waktu, filter jenis/status/course, ujian dengan/tanpa jam, form wajib/default priority, raw invalid input dipertahankan, UUID/order/edit langkah, cegah Task→Exam yang ber-checklist, aktivasi Save ganda tanpa duplikasi, Save/Buang/Batal dan gagal simpan/retry melalui tab maupun selector hub, pembatalan logout/close, unfinished completion Batal/setuju, completed/reopen, konfirmasi delete, instance/ID global-hub sama, preselect course hub, arsip tidak aktif, tiga akun termasuk tanpa course, dan tutup/buka ulang. Input nilai/snapshot dibandingkan sebelum/sesudah operasi agenda. Error JavaFX asinkron ditangkap sebagai kegagalan tes.

Ukuran Stage 1366×768, 1024×768 dan minimum760×480 diperiksa. Save/Batal tetap terjangkau; pada minimum gunakan gulir daftar/form. Kartu memuat teks status/prioritas, course, tanggal/jam dan checklist tanpa bergantung warna. GUI lama mempertahankan materi/read/edit/notes, What If 60→55→70 dan 84,5/A-, saved scenario/stale/compare, IP serta guard metadata.

Manifest 28 PNG final beserta ukuran dan SHA-256 ada di screenshots.json lokal. Native normal1350×729, jendela1024 menghasilkan1008×729, minimum744×441; emulasi125% menghasilkan1080×584. Tampilan no-courses native juga diperiksa pada minimum. Tes awal diperketat agar menunggu isi ScrollPane terpasang; fokus form kini diarahkan setelah CSS/layout. Peringatan harness JavaFX unnamed module tetap muncul tanpa kegagalan tes. Graphify AST akhir:1527node/5701relasi,0endpoint yatim,1self-loop statis lama; bukan audit runtime/fitur baru.

## Menjalankan dan review manual

Dari root proyek, PowerShell dengan cache lokal sesi ini:

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME="$PWD\.tools\maven-user"
$env:TUTUP_BUKU_DATA_DIR="$PWD\tmp\agenda-review-data"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o javafx:run
```

Gunakan JDK 21 sendiri untuk setup baru dan hilangkan `-o` pada unduhan pertama. Folder demo tersebut terpisah dari data pribadi dan tetap bertahan setelah aplikasi ditutup. Uji ulang:

```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o '-Dtest=AgendaFxCheck,MilestoneTwoFxCheck,CourseHubFxCheck,LearningReadingFxCheck,WhatIfFxCheck,ScenarioJourneyFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ModernWorkspaceFxCheck' '-DreuseForks=false' test
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o '-Dtest=AgendaFxCheck,CourseHubFxCheck' '-DreuseForks=false' '-DargLine=-Dglass.win.uiScale=125%' test
```

Review manusia:

1. Tambah tugas hari ini tanpa jam dan tugas berjam yang sudah lewat; periksa kelompok dan prioritas. Tambah ujian tanpa/dengan jam; periksa label “Sudah lewat”, bukan nilai gagal.
2. Tambah dua langkah, centang satu, batalkan lalu konfirmasikan parent complete, lihat filter Selesai dan reopen. Pastikan langkah yang belum selesai tidak ikut dicentang. Coba mengganti jenis sebelum menghapus checklist.
3. Buka item dari hub dan global, edit lalu pindah course/semester/tab dengan Batal. Periksa ID/data sama dan arsip tidak diaktifkan. Tutup/buka ulang dan bandingkan nilai/materi yang tetap.
4. Coba keyboard, fokus dan keterbacaan pada ukuran kecil serta DPI Windows nyata. Pengujian otomatis bukan usability mahasiswa, keyboard fisik atau screen reader.

Ditunda: calendar grid, jadwal mingguan, recurrence/alternating weeks, notifikasi/background reminders, AI, sync/share/cloud, attachments dan dashboard redesign. Batas/penundaan lama tetap di BACKLOG. Tidak membuat installer/ZIP atau menjalankan EXE lama terhadap data migrasi. Human DPI/usability/keyboard fisik/screen reader/komputer kedua masih pending; H-02 historis tidak diaudit ulang. Graphify diperbarui AST lokal tanpa API; artefak/draf awal dipertahankan di luar commit. Commit/push normal hanya perubahan fase, hash remote diverifikasi pada receipt lokal/handoff. Berhenti di Phase4.
