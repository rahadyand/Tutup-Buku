# Phase 3 — Ruang mata kuliah terhubung

19 November 2026 (WIB). Baseline nyata `main/7f9455702c1c57cc64154b5c00da97d21c2290b7` (Phase 2). Lingkup instruksi Phase 3: hub kontekstual **Ringkasan / Materi / Nilai**, memakai data dan view yang sudah ada. PRD v1.1 F02–F13 tetap menjadi acuan; P1/P2 dan fase berikutnya tidak dikerjakan.

## Alur mahasiswa

1. Masuk → Dashboard → **Mata Kuliah**. Dua pintu global Modul Belajar dan Simulasi Nilai tetap tersedia pada header dan tab lama. Pilih **Semester dilihat** dan mata kuliah. Header membedakan semester aktif akun dari semester yang sedang dilihat, termasuk label arsip. Membuka arsip tidak memanggil aktivasi semester.
2. **Ringkasan** memperlihatkan nama/kode opsional/SKS, progres dari completion manual, bacaan terakhir khusus course, hasil aktual dan daftar status skenario semester yang memuat course tersebut. Tidak ada materi, belum selesai, tidak ada riwayat baca, struktur kosong, skor belum lengkap, huruf langsung, asumsi kehadiran dan skenario usang dinyatakan eksplisit. Hasil nilai menggunakan `GradePlanningService.actualResult` dan formatter lama; proyeksi skenario tidak disamarkan sebagai aktual. Belum lengkap tidak disebut lulus.
3. **Buka materi / Lanjutkan membaca** menuju **Materi** milik course terpilih. Reader, katalog, editor, catatan, completion, simpan, autosave, preview, aset dan Kelola Daftar adalah instance lama yang sama. Mode Baca tetap default. Materi/catatan/completion yang dibuka dari hub dan pintu global adalah record yang sama. Jalur peminatan tetap terpisah di Modul Belajar.
4. **Nilai / Buka nilai aktual** memilih semester dan course yang dimaksud di view nilai lama. Pemilih lokasi/course internal disembunyikan selama berada dalam hub, sehingga tidak ada pemilih course kedua yang berlawanan. **Simulasi Target (What If)** membuka eksperimen untuk course tersebut. Target, override, save scenario, copy, CAS, stale review, perbandingan dan aturan IP Phase 1–2 tetap.
5. **Rencana semester & IP** menjelaskan cakupan seluruh semester dan membuka tab skenario lama, tanpa membuat atau memilih skenario otomatis. Pilih/buat skenario secara eksplisit. Bila membuka What If dari course lain dalam skenario semester, view yang sama dipindahkan ke pintu global Simulasi Nilai agar heading hub tidak mengklaim course yang salah. Tab Mata Kuliah tetap menjadi rute kembali ke konteks hub sebelumnya. IP lengkap/parsial dan snapshot historis mengikuti aturan Phase 2, bukan rumus baru di UI.
6. Kembali ke Ringkasan memuat status terkini. Kembali dari akademik ke hub memeriksa ulang course; course yang dihapus tidak meninggalkan header lama dengan isi milik course lain. Bila course lama hilang, pilih course tersedia pertama dan tampilkan konteksnya; bila tidak ada, tampilkan panduan membuat melalui Semester & Mata Kuliah.

## Konteks, draf dan transisi

`CourseHubView` menyimpan hanya semester/course dilihat dan section UI. Tidak ada model course/material/grade/scenario tandingan. `DashboardView` memindahkan satu `LearningView` dan satu `GradeView` antara tab global dan hub, dengan melepas parent sebelumnya. Dalam LearningView tetap ada satu MaterialEditor, MarkdownReader, EditorDraft dan timer autosave. Dalam GradeView tetap ada satu ActualGradePane, WhatIfPane dan ScenarioPane. Tidak ada listener/timer baru per kunjungan.

Perpindahan course, semester, section, pintu global, tab akademik/profil, logout dan close melewati `WorkspaceView.requestLeave` serta guard materi/catatan, nilai aktual, skenario, metadata akademik dan profil yang sudah ada. Transisi bersih tanpa dialog. Draf berubah menawarkan Simpan/Buang/Batal; gagal simpan menahan transisi dan input, Batal mengembalikan selector/mode lama. Eksperimen yang ditinggalkan diperlakukan sesuai guard Phase 2, bukan hilang tanpa keputusan. Menyimpan tetap eksplisit untuk nilai/skenario, sedangkan autosave materi dua detik tetap berjalan sesuai kontrak lama.

Posisi baca dicatat sebelum navigasi. Membuka ulang course memakai referensi course tersebut, bukan bacaan terakhir akun yang kebetulan berasal dari course/peminatan lain. Berpindah ke pintu global mempertahankan pilihan materi dan posisi, memuat ulang record tersimpan. Isi dengan revisi berbeda kembali ke rasio 0; catatan/completion tidak menjadi perubahan Markdown. Referensi materi terhapus memberi penjelasan dan tidak membuka course lain. Tampilan yang sedang memuat course baru membersihkan konten course lama lebih dahulu, setelah guard selesai; input gagal simpan tetap tertahan pada konteks asal.

## Penyimpanan: schema 5 → 6

Inspeksi schema 5 membuktikan `last_reading` hanya memiliki satu baris per owner. Query yang memfilter course dapat mencegah salah buka, tetapi tidak dapat memulihkan bacaan course A setelah course B/peminatan menimpa baris global. Karena riwayat per-course yang bertahan setelah restart diperlukan, dipilih perluasan SQLite aditif terkecil, bukan file/framework penyimpanan baru:

- `course_reading`: satu ReadingBookmark per `(owner_nim,course_id)`, berisi ID materi, revisi, rasio dan lokasi bab. Menggunakan domain `ReadingBookmark`/`ResumeResult` lama.
- Penulisan bookmark global dan course dilakukan pada transaksi yang sama. Bookmark peminatan hanya memperbarui global. Query selalu memvalidasi course milik akun dan memfilter owner+ID; referensi yang berpindah course tidak dibuka sebagai bacaan course asal.
- Migrasi menyalin referensi global lama bila bab masih dapat dihubungkan ke course. Riwayat course sebelum referensi global tersebut memang belum pernah disimpan; tidak direka ulang. Akun, semester aktif, materi/aset, catatan, completion, nilai dan skenario tidak di-reset.
- FK course menghapus riwayat ketika course dihapus. Tidak ada FK material pada bookmark, mengikuti kontrak lama agar penghapusan materi tetap dapat dijelaskan. Materi/data peminatan tidak dipindahkan.
- Migrasi gagal di-rollback dan dilaporkan. Versi tidak dikenal/kolom rusak tidak menyebabkan reset atau DB pengganti. `RULE_VERSION=1` dan seluruh engine/ambang tetap.

**Sebelum memakai data pribadi:** tutup aplikasi dan cadangkan folder data lengkap (DB dan aset). Coba dengan folder demo terpisah terlebih dahulu. Source lama schema 5 dan EXE/ZIP lama schema 4 tidak mendukung DB schema 6. Tidak ada downgrade otomatis atau paket baru pada fase ini. Nomor Maven masih 0.5.5-rc1 untuk build pengembangan; ZIP 0.5.5 historis tidak mewakili Phase 1–3.

## Berkas dan penggunaan ulang

| Bagian | Tanggung jawab |
|---|---|
| CourseHubView | Selector semester/course, ringkasan read-only melalui service, section dan aksi kontekstual |
| DashboardView / WorkspaceView | Tab hub tambahan tanpa mengubah dua pintu lama; memindahkan view tunggal, guard dan refresh konteks sesudah akademik |
| LearningView / MaterialEditor | Buka course pada reader lama, scope lokasi, posisi/seleksi global yang dipertahankan; akses rasio editor, bukan editor baru |
| GradeView / ActualGradePane / ScenarioPane | Pilih course yang dituju, scope lokal, preferensi course saat membuka snapshot, refresh daftar tanpa memilih skenario lain, rute global saat cakupan course melebar |
| LearningService / LearningRepository / SqliteLearningRepository | Resume per course, isolasi owner, penulisan bookmark atomik; ulang pakai objek/material/progress lama |
| LearningSchema / SqliteDatabase | Migrasi aditif 6 dan validasi; data/engine lain tetap |

Tidak ada dependency, CSS token baru, rumus nilai baru, autentikasi baru, Tasks/Events/Calendar/Schedule, dashboard redesign atau perubahan modul peminatan. Penambahan tab Mata Kuliah mempertahankan tab global lama beserta urutannya.

## Verifikasi aktual

Temurin 21.0.13+9, JavaFX 21.0.10, Maven Wrapper 3.9.11; seluruh tes memakai TempDir. Log lokal `tmp/course-hub-phase3-20261119/`, screenshot `target/verification/course-hub-*.png`.

| Pemeriksaan | Hasil |
|---|---|
| Maven verify (`verify-final.log`) | 200 kasus, 0 gagal/error/skip (194 lama + 6 baru) |
| GUI regresi: CourseHub, LearningReading, WhatIf, ScenarioJourney, M4, M5, ModernWorkspace; reuseForks=false (`gui-final.log`) | 7 kasus, 0 gagal/error/skip pada source final |
| Emulasi JavaFX 125% (`gui-scale.log`) | 4 kasus, 0 gagal/error/skip; emulasi proses, bukan DPI Windows nyata |

Enam kasus persistence baru memeriksa dua course/peminatan/restart, isolasi akun dan semester aktif, revisi/deletion/cascade, rollback penulisan bookmark global+course lalu retry, migrasi schema 5 dengan skenario/catatan/progres tetap, serta migrasi gagal tanpa reset/retry. Fixture migrasi lama diperbaiki agar benar-benar tidak mempunyai tabel schema 6; jumlah lima materi fixture lama tetap lima.

CourseHubFxCheck menggunakan aplikasi produksi: ringkasan parsial dan skenario usang, resume course A/B (termasuk sesudah peminatan), identitas reader yang sama, catatan/completion dari Baca dan akses global, edit materi/CAS/save gagal/retry, Batal pada section/course/semester/pintu global dan close, gagal simpan aktual/skenario menahan context, What If target 60+kunci80 dengan aktual30→kebutuhan70, rencana semester tanpa auto-select, snapshot usang, arsip tanpa aktivasi, empty/direct, penghapusan course lewat konteks akademik, dua akun, logout/login dan restart. Semua perubahan nilai aktual dibandingkan tetap sama; satu course kosong sengaja dihapus pada fixture untuk menguji refresh konteks. Error JavaFX asinkron ditangkap sebagai kegagalan tes.

Regresi WhatIf/ScenarioJourney dan suite murni mempertahankan 60→55→70, 84,5/A-, earned/max, bobot tepat100, impossible/W=0, kehadiran E sambil mempertahankan asal, CAS, ownership dan IP lengkap. Tidak ada perubahan engine.

Visual diperiksa pada Stage 1366×768, 1024×768 dan minimum 760×480. PNG native masing-masing 1350×729, 1008×729 dan 744×441. Minimum membutuhkan gulir isi hub: selector/section tetap terlihat, sedangkan materi/nilai diberi ruang minimum dan panel luar menggulir bila perlu. Dengan catatan terbuka, ruang baca tetap dialokasikan; satu scrollbar reader utama tetap mengatur isi Markdown. Ini kompromi ukuran minimum, bukan janji semua isi muat. Emulasi 125% hanya properti proses; resolusi/skala Windows tidak diubah.

Inspeksi awal menemukan exception pada akun tanpa semester aktif (ternary unboxing null) dan reader terdesak ketika catatan terbuka. Keduanya diperbaiki; tes kini menangkap error JavaFX asinkron dan mengukur ruang reader pada ukuran kecil. Gerbang akhir di atas dijalankan setelah perbaikan konteks/label. Peringatan harness JavaFX unnamed module tetap muncul tanpa error tes. Graphify AST lokal: 1361 node, 5103 relasi, 0 endpoint yatim, 1 self-loop statis lama; bukan audit runtime atau gerbang penerimaan produk.

## Menjalankan dan review manual

Dari root proyek, PowerShell; cache lokal sesi ini memakai:

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME="$PWD\.tools\maven-user"
$env:TUTUP_BUKU_DATA_DIR="$PWD\tmp\course-hub-review-data"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o javafx:run
```

Untuk setup baru gunakan JDK 21 sendiri dan hilangkan `-o` pada unduhan pertama. Folder demo di atas terpisah dari AppData dan bertahan setelah tutup. Uji ulang:

```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o '-Dtest=CourseHubFxCheck,LearningReadingFxCheck,WhatIfFxCheck,ScenarioJourneyFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ModernWorkspaceFxCheck' '-DreuseForks=false' test
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o '-Dtest=CourseHubFxCheck,LearningReadingFxCheck,WhatIfFxCheck,ScenarioJourneyFxCheck' '-DreuseForks=false' '-DargLine=-Dglass.win.uiScale=125%' test
```

Review manusia berikutnya:

1. Buat dua course dan satu arsip; pilih arsip di hub, pastikan label aktif tidak berubah. Periksa course kosong, skor parsial, huruf langsung dan skenario usang.
2. Baca materi di A dan B, tulis catatan, tandai selesai secara manual, buka peminatan lalu lanjutkan A/B. Bandingkan record dari pintu global, tutup/buka aplikasi dan periksa posisi/revisi.
3. Ubah materi/nilai/skenario, coba Batal saat berpindah course/semester/section/keluar. Periksa tidak ada pilihan bergerak atau input menghilang. Simpan/duplikasi/banding dan IP tetap di workflow lama.
4. Coba keyboard dan kenyamanan baca pada layar kecil serta DPI Windows nyata; periksa fokus sesudah dialog dan gulir minimum. Pengujian otomatis tidak menggantikan usability mahasiswa, keyboard fisik atau screen reader.

Belum dilakukan: usability manusia, DPI Windows nyata, keyboard fisik/screen reader dan komputer kedua. H-02 ListView historis tidak diaudit ulang. Tidak membangun/menjalankan EXE lama terhadap data migrasi atau membuat installer/ZIP. Graphify diperbarui AST lokal tanpa backend AI; artefak/draf awal tetap di luar commit Phase 3. Commit/push normal hanya perubahan fase; hash remote diverifikasi dan dicatat di receipt lokal/handoff akhir. Berhenti setelah Phase 3.
