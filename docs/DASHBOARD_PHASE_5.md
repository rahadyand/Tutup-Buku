# Hari Ini — Phase 5

Baseline: `main/6b0f6603cb67bda9e254c375663cf60c08bded0d` (Phase 4). Implementasi 19 November 2026 WIB. Schema tetap **7**, aturan kalkulasi tetap RULE_VERSION 1. Tidak ada dependency, tabel, installer atau ZIP baru.

## Pengalaman pengguna dan sumber data

**Hari Ini** menjadi halaman awal setelah login. Tab lama tetap pada urutan/indeks semula; tab Hari Ini ditambahkan setelah Agenda. Header global dan dua shortcut tetap. Sapaan memakai profil tersimpan; tanggal WIB berasal dari Clock AgendaService.

Semester aktif menjadi konteks pertama. Pemilih **Semester dilihat** memungkinkan arsip; label menjelaskan apakah aktif atau arsip, tanpa mengaktifkannya. Jika belum ada semester aktif tetapi ada arsip, arsip pertama ditampilkan dengan label arsip. Akun tanpa semester mendapat tombol Buat semester. Pergantian akun membuat workspace baru.

| Bagian | Sumber dan tindakan |
|---|---|
| Perlu perhatian | AgendaService.items/group: Terlambat, Sudah lewat—ujian, Hari Ini, Mendatang. Maksimal satu item per kelompok; jumlah mencakup semua item terbuka dalam kelompok, bukan hanya baris terlihat. Judul membuka editor AgendaView lama; Lihat semua membuka daftar dengan filter jenis/status lama serta menggulir ke kelompok terkait. |
| Lanjutkan membaca | ReadingBookmark global jika sesuai semester, lalu bookmark per course dalam urutan daftar course; maksimal tiga, tanpa mengklaim urutan recency penuh. Peminatan terakhir diberi label lintas semester. LearningService memeriksa kembali ID/lokasi/revisi sebelum membuka reader lama. Tidak mengganti referensi hilang dengan materi lain. |
| Mata kuliah | Maksimal empat ringkasan course: SKS, courseProgress, jumlah agenda terbuka dan nilai aktual efektif. Penyebut progres sama dengan hub; tanpa materi tidak 100%. Tombol menuju hub, aktual atau What If course dan semester yang sama. Lihat semua menuju selector hub lama. |
| Kondisi semester | GradePlanningService.actualResult + SemesterGpaService untuk **seluruh** course semester. Jika lengkap, format IP lama; jika tidak, daftar kendala/tombol course yang harus dilengkapi, tanpa IP parsial/nilai kosong sebagai E. Tombol menuju perencanaan/IP semester tanpa memilih skenario tersimpan otomatis. |

Nilai aktual dan skenario tetap terpisah. Dashboard tidak membaca nilai dari skenario. Bila kehadiran belum dikonfirmasi, hasil mengikuti asumsi tersimpan dan engine tetap memberi label **IP Simulasi**; sumber dijelaskan sebagai nilai aktual dengan asumsi kehadiran, bukan proyeksi skenario. Override kehadiran dan semua rumus lama tidak berubah.

## Struktur dan kontrak interaksi

- TodayDashboardService hanya mengomposisi hasil service lama; record Snapshot/CourseSummary/ReadingLink bersifat sementara, tidak disimpan sebagai salinan data.
- TodayDashboardView memiliki satu ScrollPane utama; bagian vertikal, tombol pendek sesuai isi, judul/deskripsi wrap. Memakai FormSupport dan CSS yang ada.
- DashboardView memasang kembali instance LearningView, GradeView, AgendaView dan CourseHubView yang sama. Tidak ada reader/editor/draf/autosave kedua. Jalur bacaan spesifik memakai callback resume lama, termasuk notes/completion dan fallback rasio 0 ketika revisi konten berubah.
- Semua tindakan navigasi/pemilih semester melewati WorkspaceView.requestLeave. Save/Buang/Batal dan kegagalan save tetap menahan input/konteks; transisi bersih tidak meminta konfirmasi. Tombol Hari Ini sudah melewati guard sekali sebelum pemasangan view tujuan.
- Pintasan nilai memeriksa kembali keberadaan course sebelum mengisi editor. Course yang hilang dilaporkan; tidak membuka course pengganti.

## Refresh, tanggal dan kegagalan

Pemuatan berjalan di worker UiTasks yang ada. Dashboard dimuat ketika masuk/kembali, berpindah semester, dan Muat ulang. Setelah menyimpan/menyelesaikan item di layar lama, kembali ke Hari Ini mengambil data terbaru. Header tanggal dan kelompok agenda memakai satu waktu acuan WIB per pemuatan; date-only tetap Hari Ini sepanjang tanggalnya, ujian lewat tetap Sudah lewat.

Satu listener fokus jendela meminta pemeriksaan tanggal hanya bila Hari Ini terlihat. Tanggal berubah memicu refresh; tidak ada polling, pengingat atau timer latar baru. Clock dapat disuntikkan pada aplikasi untuk pengujian pergantian tanggal tanpa mengubah jam Windows.

Setiap permintaan membawa generation; respons yang sudah dibatalkan/digantikan serta pemilik berbeda tidak diterapkan. Workspace lama diinvalidasi saat keluar. UiTasks menonaktifkan input selama pemuatan dan memakai worker tunggal.

Jika salah satu sumber gagal, **seluruh ringkasan halaman ditahan** dengan pesan data belum dapat dipastikan dan Coba lagi. Ini pilihan sederhana untuk menjaga konsistensi; bagian lain tidak diberi angka nol atau dianggap lengkap. Tidak ada reset database atau substitusi penyimpanan. Referensi bacaan yang terhapus dilaporkan terpisah; tombol usang tidak menavigasi ke materi lain.

## Verifikasi

Hasil final: **222 non-GUI**, **11 GUI pada 10 kelas**, regresi akhir terfokus **2 native + 2 emulasi 125%**, dan pengambilan ulang screenshot **1 native + 1 emulasi 125%** lulus, semua tanpa gagal/error/skip. Pengulangan terakhir hanya mengenai file yang berubah dan sinkronisasi bukti visual.

| Perintah Maven (tambahkan opsi repo/cache lokal bila mengikuti setup project) | Hasil |
|---|---|
| `verify` | 222 tes, BUILD SUCCESS; `verify-final.log` |
| `-Dtest=DashboardFxCheck,AgendaFxCheck,CourseHubFxCheck,LearningReadingFxCheck,WhatIfFxCheck,ScenarioJourneyFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ModernWorkspaceFxCheck,MilestoneTwoFxCheck -DreuseForks=false test` | 11 kasus/10 kelas; `gui-final.log` |
| `-Dtest=DashboardFxCheck,AgendaFxCheck -DreuseForks=false test` | 2; `focused-final-native.log` |
| Perintah terfokus di atas + `-DargLine=-Dglass.win.uiScale=125%` | 2; `focused-final-125.log` |
| `-Dtest=DashboardFxCheck -DreuseForks=false test`, ulang dengan argLine125% | masing-masing1; `screenshots-final-native.log` / `screenshots-final-125.log` |

26 PNG dashboard final (13 native +13 emulasi) memiliki manifest dimensi/SHA256 di `tmp/dashboard-phase5-20261119/screenshots.json`. Jendela native1366×768 menghasilkan client1350×729; 1024×768 →1008×729; minimum760×480 →744×441. Emulasi125% memakai ukuran logical berdasarkan outputScale, tetap mematuhi batas minimum aplikasi; bukan pengukuran DPI monitor.

Hasil final dan log lokal dicatat di DEVELOPMENT_STATUS. Semua data uji memakai @TempDir, bukan database AppData pengguna. Artefak lokal: `tmp/dashboard-phase5-20261119/` dan `target/verification/dashboard-*.png`.

TodayDashboardServiceTest mencakup isolasi akun/semester/arsip tanpa aktivasi, kesamaan grouping dengan AgendaService, date-only dan ujian lewat pada batas hari, completion setelah reload, riwayat tepat dan fallback revisi/hapus, progres 1/2/tanpa materi, IP aktual tidak lengkap walaupun proyeksi huruf A tersimpan, IP lengkap 3,42, asumsi kehadiran, gagal SQL/retry tanpa reset, akun/semester kosong dan logout.

DashboardFxCheck menjalankan aplikasi produksi dengan Clock WIB terkontrol: default Hari Ini; akun tanpa semester/course dan course kosong; kelompok/jumlah agenda; editor item; Save/Buang/Batal/gagal save/retry; arsip/hub/course; actual/What If/IP tanpa auto-pilih skenario; reader materi tepat + posisi 0,6; referensi terhapus; course dihapus setelah snapshot; gagal muat/retry; hari berganti saat kembali; dan isolasi akun setelah logout. Screenshot mencakup 1366×768, 1024×768 dan minimum 760×480 serta judul panjang, bacaan/progres, IP lengkap/tidak lengkap dan kegagalan. Ukuran PNG adalah area client, bukan bingkai luar Windows.

Tes reader lama menunggu isi skin tab editor tersedia sebelum membaca field, tanpa penundaan tetap. Percobaan awal menemukan perlombaan layout pada test tersebut; assertion tetap dipertahankan.

## Menjalankan dan review manual

Ikuti setup Java 21 pada README. Untuk demo aman di PowerShell dari root project:

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME="$PWD\.tools\maven-user"
$env:TUTUP_BUKU_DATA_DIR="$PWD\tmp\demo-dashboard-phase5"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' javafx:run
```

Folder demo dibuat terpisah dan tidak diisi data contoh diam-diam. Daftar akun lalu buat semester/course, materi dan agenda dari layar yang sudah ada. Jangan arahkan EXE/schema lama ke database ini. Cadangkan database+aset saat aplikasi tertutup sebelum mencoba data pribadi.

1. Masuk: Hari Ini terbuka; buat semester/course bila kosong. Bandingkan label aktif/dilihat saat memilih arsip; aktif tidak boleh berubah.
2. Buat tugas tanpa jam hari ini, tugas lewat, ujian lewat dan item mendatang. Cocokkan kelompok/jumlah dengan Agenda, buka item, ubah/selesaikan lalu kembali.
3. Baca materi kuliah, gulir, kembali ke Hari Ini dan lanjutkan bacaan. Periksa judul, posisi, catatan dan completion. Peminatan harus jelas terpisah; materi dihapus tidak membuka pengganti.
4. Lengkapi nilai sebagian: IP harus belum lengkap. Lengkapi semuanya: cocokkan IP aktual dengan layar nilai; simpan skenario berbeda dan pastikan dashboard tidak menggunakannya.
5. Ubah draf agenda/materi/nilai lalu kembali; coba Batal, Buang dan Simpan. Periksa keyboard/fokus serta jendela minimum di perangkat sendiri.

## Batas dan pekerjaan yang tidak dilakukan

Emulasi JavaFX 125% hanya skala proses tes, **bukan DPI Windows nyata**. Usability mahasiswa, keyboard fisik, pembaca layar perangkat kedua, serta perpindahan fokus melintasi tengah malam nyata masih memerlukan pemeriksaan manusia. Daftar bacaan memakai kemampuan bookmark lama, tanpa timestamp/riwayat baru; tidak menampilkan semua course sekaligus. Error satu sumber menahan seluruh ringkasan hingga retry.

Tidak membuat kalender, jadwal, recurrence, notifikasi, AI, cloud, salinan akademik atau release baru. Paket **0.5.5-rc1 lama tidak mewakili source Phase 1–5**. Berhenti setelah Phase 5 untuk review pengguna.
