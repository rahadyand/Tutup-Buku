# Phase 1 — Simulasi Target / What If

Selesai pada19November2026 dari baseline source `d77c8a7fb1a97fab2c7fda9b85213766b8158976`, branch main. Acuan produk PRD v1.1 (F08–F13/PLAN-01/SCN-01/02); instruksi Phase1 pengguna membatasi perubahan ke eksperimen satu mata kuliah. Power Planner hanya referensi perilaku yang disediakan pengguna. P1 grouped/search/backup dan P2 tetap ditunda; tidak ada pekerjaan dashboard, kalender, tugas, modul belajar atau Phase2.

## Alur mahasiswa

1. Simulasi Nilai → Komponen & aktual → pilih semester/mata kuliah → **Simulasi Target (What If)**. Form aktual yang berubah tetap menawarkan Simpan/Buang/Batal sebelum masuk. Masuk membaca ulang data milik akun melalui worker dan membuat basis immutable di memori; tidak membuat skenario tersimpan.
2. Target angka dan huruf adalah satu target. Pilih C→60/A→85; angka84,5 atau84.5 tetap84,5 secara matematis dan menampilkan A-. Koma/titik mengikuti DecimalInput; angka di luar0–100/teks invalid dipertahankan dengan pesan dekat field.
3. Aktual earned/max selalu terlihat dan tetap secara default. Centang **Kunci asumsi** pada komponen unknown, isi normalisasi0–100. Kosong tetap bebas, bukan nol, meskipun checkbox belum dilepas; isian yang tidak dikunci tidak dipakai. Kosongkan nilai asumsi atau lepas kuncinya untuk kembali bebas. Bobot0 tidak memerlukan nilai/kunci dan tidak ikut solver.
4. Untuk nilai yang sudah diketahui, **Override sementara** membuka asumsi normalisasi; **Pulihkan aktual** mengembalikan nilai asli dan menonaktifkan override. Nilai asli tetap terlihat. Override kosong/invalid menahan proyeksi agar dikoreksi atau dipulihkan.
5. Target/asumsi valid memicu kalkulasi otomatis. Kebutuhan output terpisah dari input, tidak otomatis menjadi kunci/aktual. Semua hasil satu revisi diterapkan bersama; callback antrean lama diabaikan. Tampilkan aktual, proyeksi jika kebutuhan/asumsi dipenuhi, dan efektif sesudah kehadiran secara terpisah. Angka mustahil tidak dipotong ke100; status tepat dari engine tetap dipakai bahkan di batas yang lebih presisi daripada tampilan.
6. **Reset** mengembalikan target60/C dan membersihkan seluruh asumsi/override terhadap basis saat masuk. **Kembali ke aktual** membuka view/seleksi lama. Draft berubah meminta Buang simulasi sementara/Batal; Batal menahan mode, pilihan dan isian. Ini juga berlaku untuk tab, semester, refresh, dashboard/logout/close. Transisi bersih tanpa dialog.
7. **Skenario tersimpan** menuju layar yang sama seperti sebelumnya. Phase1 sengaja tidak menawarkan penyimpanan draft What If: snapshot override sementara tidak disamarkan sebagai data aktual dalam skenario lama. Simpan/duplikasi/banding/review/CAS semester tetap memakai alur lama. Form/skenario lama yang belum disimpan tetap dijaga guard-nya.
8. Mode huruf langsung tidak membentuk skor komponen; gunakan estimasi huruf di workflow skenario yang ada atau kembali ke editor aktual. Struktur kosong/total bobot tidak tepat100 dijelaskan beserta rute ke editor. Tidak ada nilai100/GPA4 fiktif. IP semester tidak dihitung pada layar per-course ini; layanan IP lengkap lama tetap.

## Berkas dan tanggung jawab

| Berkas | Perubahan |
|---|---|
| ActualGradePane.java | Tombol masuk yang jelas; guard aktual dijalankan sebelum callback |
| GradeView.java | Ambil basis terbaru per pemilik/semester/course, pertahankan view/seleksi, integrasi guard dan kembali/tab skenario; footer status lama dilepas hanya selama What If |
| WhatIfPane.java | Kontrol target, baris asumsi/override, validasi inline, draft sementara dan generation guard; satu ScrollPane untuk hasil+baris, field100px |
| GradePlanningService.java | `whatIf` membentuk salinan ScenarioCourse untuk `evaluateCourse`; hanya aktual yang dioverride ditutupi pada salinan. Pesan mustahil menyatakan >100 berdasarkan status eksak, tanpa mengandalkan raw display yang mungkin mendekati100 |
| GradePlanningPersistenceTest.java | Enam kasus baru, termasuk pembandingan SQLite aktual dan snapshot sebelum/sesudah eksperimen |
| WhatIfFxCheck.java | UI produksi, angka/huruf, rapid target, lock/override/restore, inline error, reset/guard, gagal simpan saat masuk, saved/stale, kosong/direct/kehadiran dan ukuran layar |
| UI_COMPONENTS.md / DEVELOPMENT_STATUS.md | Kontrak dan hasil aktual; catatan Graphify lama dipertahankan di working tree |

GradeCalculator, TargetPlanner, Score/ScoreFraction, LetterGrade, ScenarioAssumptions/ScenarioCourse dan SemesterGpaService dipakai ulang. Engine/domain, repository/SQL, schema4, dependencies, theme dan layar di luar nilai tidak diubah. `whatIf` tidak mempunyai operasi save; map overrides/locks hanya di memori, bukan tabel/kolom baru. Basis aktual terjaga immutable; evaluasi yang diperkirakan tidak menjadi hasil aktual. Masuk ulang mengambil basis terbaru; reset memakai basis saat masuk, tanpa rebase diam-diam.

## Verifikasi aktual

Temurin21.0.13+9, JavaFX21.0.10, Maven Wrapper3.9.11. Seluruh DB/aset tes memakai TempDir; demo tambahan di tmp. Log lokal `tmp/what-if-phase1-20261119/`.

| Pemeriksaan | Hasil | Log |
|---|---|---|
| Maven verify akhir |185kasus,0gagal/error/skip (179lama+6baru)|verify-final.log|
| WhatIfFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ModernWorkspaceFxCheck; reuseForks=false |4kasus lulus|gui-final.log|
| WhatIfFxCheck+M4; properti proses glass.win.uiScale=125% |2kasus lulus; bukan DPI Windows nyata|gui-scale.log|
| M4 jendela760×480 |1kasus lulus; viewport aktual110/skenario112logical px|gui-minimum.log|
| Visual |6PNG plan/empty: native1366/1024/760 dan emulasi125%; Scene1350×729/1008×729/744×441/1080×584, hash di screenshots.json|what-if-*.png|
| Native Windows tambahan |Source JavaFX dibuka pada DB fiktif, tetapi getApp(window1640688) gagal: window is minimized. Login/klik Windows tambahan tidak diklaim lulus|native-launch.txt; hasil tool chat|

Cakupan numerik:60semua bebas→55setelah tugas80→70setelah aktual kedua30/100; earned8/10; override kedua60→kebutuhan terakhir52 dan pulih70;84,5/A-; kebutuhan125mustahil; target sangat presisi tetap impossible; NO_FREE tercapai/tidak; bobot0;99,99/100,01invalid; kosong≠0; kehadiran E/0 dengan asalA- tetap. GUI memeriksa pembaruan cepat60/85/55/84,5/60 tanpa hasil campuran, pemulihan error, pengembalian seleksi saat Batal, dan snapshot tersimpan tidak ditimpa/di-rebase. Regresi CAS/stale/pemilik/IP tetap di suite lama.

Percobaan pertama sudah lulus; inspeksi gambar mendorong pemadatan ringkasan/baris supaya tiga komponen terlihat pada1366/1024. Tes akhir di atas diulang setelah perubahan tersebut. Minimum760×480 membutuhkan gulir, tetapi target dan aksi utama tetap terlihat. Target dan baris memakai native controls serta factory/token tema yang sama, tanpa parser/framework baru.

## Menjalankan dan review

Dari root project pada PowerShell (cache lokal sesi ini):

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME="$PWD\.tools\maven-user"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o javafx:run
```

Hilangkan -o untuk unduhan pertama. TUTUP_BUKU_DATA_DIR dapat diarahkan ke folder demo terpisah sebelum run; jangan menunjuk DB produksi untuk tes. Uji otomatis relevan:

```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o '-Dtest=WhatIfFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ModernWorkspaceFxCheck' '-DreuseForks=false' test
```

Review manual: pilih targetC/60, kunci komponen80, coba override dan Pulihkan aktual, Batal saat keluar, lalu kembali ke aktual dan periksa nilainya. Periksa empty state dan skenario tersimpan. Uji manusia/keyboard fisik/screen reader/Windows DPI nyata belum dilakukan. H-02 ListView historis tetap OPEN; log final tidak menunjukkan regresi itu.

Tidak membuat installer/ZIP atau mengulang verifikasi paket. Paket0.5.5 lama tetap historis dan tidak mewakili perubahan What If ini; jalankan source untuk Phase1. Graphify diperbarui AST lokal tanpa backend AI/biaya API; hasil untracked awal dipertahankan dan tidak dimasukkan ke commit fase. Commit/push normal hanya perubahan Phase1; hash terverifikasi dicatat pada receipt lokal git-checkpoint.txt dan handoff. Berhenti sesudah Phase1.

Diagnostik Graphify akhir:1208node/4358relasi,0endpoint yatim dan1self-loop. Ini peringatan pada pemetaan statis, bukan kegagalan perhitungan/runtime; hasil graph tidak dianggap bukti produk. Penamaan komunitas fallback mengikuti hub, tanpa relabel backend AI. Rincian lokal graph-health.json.
