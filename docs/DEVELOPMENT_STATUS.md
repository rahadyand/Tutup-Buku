# Penajaman bukti screenshot — 19 November 2026

Pemeriksaan akhir setelah push menemukan satu PNG katalog tersembunyi1024 masih memakai texture WebView dari frame sebelum resize, walaupun ukuran Scene/DOM sudah benar. `LearningReadingFxCheck.readerLayout` kini menunggu tiga pulse render dan menegaskan reader memakai lebar Scene−maksimum24px saat katalog tersembunyi. Ini perubahan tes/capture saja; kode aplikasi, JAR dan ZIP tidak berubah. Tidak membangun ulang paket0.5.5 yang sama.

Kasus terarah diulang pada tiga ukuran native dan emulasi JavaFX125%: **masing-masing1kasus,0gagal/error/skip**, log `tmp/learning-screen-20261119/reading-final-frames{,-scale}.log`.16PNG akhir dan screenshots.json diperbarui; capture sebelumnya tetap di previous-capture/. Pemeriksaan gambar tersembunyi1366/1024/minimum/125% kini memperlihatkan WebView mengisi lebar yang tersedia. Gerbang179regresi,16GUI/benchmark dan dua alur paket di bawah tetap sah; helper tambahan hanya memperketat bukti gambar. Paket tetap berasal dari source aplikasi636a147, sedangkan commit setelahnya hanya dokumentasi/tes.

---

# Verifikasi penutup kandidat — 19 November 2026

**Layar belajar0.5.5-rc1 selesai, source dan paket terverifikasi.** Source aplikasi/paket: `636a147454159c33b93b8d2d245c677d4b3f1c0b` (`feat(ui): prioritize reading in learning workspace`), branch `main`, repository https://github.com/rahadyand/Tutup-Buku. BUILD-INFO mencatat tracked working-tree changes=False; draf untracked dikecualikan tanpa dihapus. Commit dokumentasi penutup sesudahnya tidak mengubah aplikasi. Status push/hash remote dicatat pada receipt lokal `tmp/learning-screen-20261119/git-checkpoint.txt` dan handoff chat setelah verifikasi remote.

- **ZIP:** `releases/TutupBuku-0.5.5-rc1-20261119-060834/TutupBuku-0.5.5-rc1-windows-x64.zip`.
- **Launcher distribusi:** `releases/TutupBuku-0.5.5-rc1-20261119-060834/TutupBuku/TutupBuku.exe`.
- **Payload ekstraksi yang diuji:** `tmp/Uji Paket Ekstraksi 20261119-060934/TutupBuku/`.
- SHA256 ZIP: `3F2703DA35ADB9BC7C73C9FB46DF7346A90EB6467D74A432F266FEC036C2B5AD`.
- SHA256 JAR build/extracted/distribusi: `491D70A403ACF3CD6108000AA94407D5AEFC4A1B27F97DF3D5892BBAF13C7AAF`.

Pengemasan `scripts/Build-Windows.ps1 -Offline -SkipBuild` memakai JAR hasil verify yang dicatat di bawah, dari commit source di atas. Verify-WindowsPackage memeriksa229entri manifest+manifest itu sendiri=230file, hash ZIP/runtime/legal dan nol berkas terlarang. Perbandingan langsung setiap file ZIP dengan ekstraksi sesudah pengujian mencocokkan230file. Main JAR dan seluruh13dependency sama dengan input build; rincian `package-integrity.json`. Tidak membawa DB/aset pengguna/cache/log/source/test/rahasia. Semua paket lama dipertahankan;0.5.4 tetap artefak historis.

**Uji otomatis payload distribusi:** harness sementara memanggil `LearningReadingFxCheck` dan `MilestoneThreeFxCheck` dalam dua proses Java terpisah, menggunakan runtime21.0.13 dari ekstraksi dan classpath app/*.jar + target/test-classes + dependensi assertion JUnit. Tidak memasukkan target/classes. Harness memeriksa lokasi kelas TutupBukuApplication benar-benar packaged JAR. Kedua alur LULUS: mode, catatan/completion, guard/failure/retry, resume/revisi, pengelolaan dan tutup/buka ulang. Log `packaged-check/reading.log` dan `restart.log`; ini tes otomatis, bukan usability manusia. Source tes/harness tetap di luar ZIP.

**Uji EXE melalui kontrol Windows:** EXE ekstraksi dibuka dengan JAVA_HOME/MAVEN_HOME kosong, PATH hanya System32, TUTUP_BUKU_DATA_DIR=`tmp/learning-screen-20261119/exe-data` (salinan DB fiktif tes). Login001, Lanjutkan Membaca, Mode Baca, catatan terpisah dan completion teramati. Catatan diubah menjadi “Catatan uji EXE 0.5.5: tetap setelah dibuka ulang.”, tombol Simpan catatan menghasilkan status Catatan tersimpan. Proses ditutup normal, exit0/PID9540, lalu EXE baru dibuka pada DB yang sama; login/resume/panel catatan memperlihatkan teks yang sama. Bukti AX/screenshot berada dalam hasil tool chat, PID/lingkungan/exit pertama di exe-launch.txt. Jendela kedua dibiarkan terbuka pada akun fiktif untuk review; exit kedua belum diklaim. Tidak menguji ulang grade/skenario secara interaktif dalam task layar belajar ini. Input awal pada pembukaan kedua belum berfokus di field; fokus diperbaiki lalu login berhasil, tanpa perubahan source.

Regresi source:179tes bisnis/persistensi,16kasus GUI/benchmark,1pengulangan capture terarah dan3kasus emulasi125%, seluruhnya0gagal/error/skip.16PNG final empat mode/ukuran tersimpan beserta dimensi/hash di folder bukti. Proses JavaFX125% bukan DPI Windows fisik. Warning unnamed-module JavaFX, unchecked test helper dan jlink compress deprecated tetap tercatat; tidak ada exception ListView/CSS baru pada log final. H-02 historis tetap OPEN. Pemeriksaan tanda kredensial pada15berkas staged, scope, .gitignore, Wrapper dan diff --check lulus; Git Credential Manager dipakai, tanpa meminta token. gh tidak tersedia di PATH; operasi remote dilakukan dengan Git.

**Batas penerimaan:** penilaian kenyamanan pengguna, keyboard fisik/screen reader, DPI Windows nyata, perangkat kedua, jaringan fisik terputus dan shortcut Desktop masih PENDING. Ukuran minimum760×480 memerlukan gulir katalog/editor dan catatan dilipat untuk meluaskan baca. Jangan menganggap gerbang otomatis sebagai penerimaan manusia. Review layar ini lebih dahulu; tidak melanjutkan dashboard/simulasi atau fitur lain.

Panduan interaksi di USER_GUIDE/UI_COMPONENTS diperbarui sebelum paket dibuat. Catatan hasil di repository ditambahkan sesudah paket; paket tidak dibuat ulang untuk perubahan dokumentasi bukti saja. Seluruh draf lokal yang tidak terkait tetap untracked.

---

# Layar belajar Baca/Ubah/Kelola — 19 November 2026 (WIB)

**Implementasi dan verifikasi source selesai; checkpoint/package0.5.5-rc1 belum dibuat pada catatan ini.** Baseline sebelum perubahan: `5d55027ab322cb0e7574f6770d2ba3f611b2abe6`, main/origin main sama. Handoff TUTUP_BUKU_PROJECT_REVIEW diperiksa terhadap Git, source dan bukti lokal; modernisasi lama dipertahankan. Paket0.5.4-rc1 lama tidak mewakili source layar baru.

## Perubahan terbatas yang disetujui

Baca default, satu katalog kiri, baca dominan kanan tanpa ScrollPane pembungkus WebView. Tutup/Buka katalog mempertahankan pilihan/rasio. Ubah Materi memakai editor, reader dan EditorDraft yang sama; Kelola Daftar memuat seluruh CRUD/urutan/seed/reload dengan konteks pilihan yang sama. Catatan dan completion tetap tersedia saat membaca; simpan catatan saja tidak mengubah revisi isi atau merender ulang bacaan. Info materi dapat dilipat. Posisi baca sama untuk revisi sama; isi direvisi kembali ke0.

Guard Simpan/Buang/Batal tetap untuk mode/lokasi/materi/navigasi/logout/close; gagal simpan menahan perpindahan dan mempertahankan isian. Gagal bookmark saat perpindahan bersih kini juga menahan navigasi. Autosave2detik/worker tunggal/CAS/kepemilikan/konfirmasi hapus tetap. Label materi terpilih memakai warna cell tema dan penanda draf tetap. Tidak mengubah dependency, schema4, domain/service/persistence/security/perhitungan, CSS tema/header global/dashboard/profil/akademik/simulasi. Versi aplikasi/pom/script menjadi0.5.5-rc1; BUILD-INFO membedakan perubahan tracked dari draf untracked agar provenance tidak rancu.

## Pemeriksaan yang benar-benar dijalankan

Temurin21.0.13+9 portable, Maven Wrapper3.9.11, JavaFX21.0.10, cache offline. Semua DB/aset tes terisolasi; database AppData pengguna tidak dipakai.

| Pemeriksaan | Hasil | Bukti lokal di tmp/learning-screen-20261119/ |
|---|---|---|
| Baseline sebelum edit |179tes lulus|reading-baseline.log|
| `mvnw.cmd -Dmaven.repo.local=.m2/repository -o -B -ntp -Pwindows-package verify` |179tes,0gagal/error/skip; JAR0.5.5 dan13runtime libs dibuat|reading-final-unit.log|
| Seluruh15class opt-in, `-DreuseForks=false test` |16kasus=15GUI+1benchmark,0gagal/error/skip|reading-final-gui.log|
| `LearningReadingFxCheck` setelah penantian capture diperketat |1kasus lulus,12PNG baru|reading-final-capture.log|
| LearningReading/ModernWorkspace/MilestoneThree dengan `-DargLine=-Dglass.win.uiScale=125%` |3kasus lulus; emulasi proses JavaFX, bukan DPI Windows nyata|reading-final-scale.log|
| PNG empat mode pada3ukuran native +125% |16PNG diperiksa; dimensi/hash di screenshots.json|learning-{read,edit,manage,hidden}-*.png|
| Git diff --check; sintaks script |diff bersih; parser PowerShell diperiksa sebelum commit|checkpoint berikut|

Tes baru memeriksa default Baca, absennya editor/management dari layout Baca, tidak ada ScrollPane luar reader, hide/show+seleksi+rasio, semua transisi mode, catatan/autosave/completion dari Baca, catatan gagal tersimpan/ulang berhasil, Batal pemilihan materi/lokasi, Simpan/Buang/Batal saat masuk Kelola, ketersediaan seluruh fungsi management, resume, perubahan revisi kembali0, judul trim tetap revisi/rasio, dan gagal bookmark menahan navigasi. Regresi M3 tetap mencakup gagal simpan isi/rollback, guard navigasi/close, urutan/hapus, restart dan catatan; M5 mencakup perjalanan belajar–akademik–nilai. Pengujian bukan klik/ketik manusia.

Tangkapan native menargetkan **jendela**1366×768,1024×768,minimum760×480; PNG Scene masing-masing1350×729,1008×729,744×441. Emulasi125% jendela fisik1366×768 menghasilkan Scene1080×584. Reader Baca minimum≥80logical px saat catatan dilipat; katalog dan editor tetap memerlukan gulir pada minimum. Pemeriksaan gambar memastikan kontrol pengelolaan tidak mengambil ruang Baca dan katalog tersembunyi meluaskan WebView. Tes capture awal menangkap Scene/WebView sebelum pulse resize selesai; penantian diperketat dan PNG dihasilkan ulang, tanpa mengubah skala gambar.

Kegagalan pengembangan disimpan pada tmp/reading-regression-first.log dan reading-focused-{2,3}.log: tes lama masih mengakses kontrol management yang kini terpisah; penantian skin dan langkah masuk mode diperbaiki. Footer status sukses berulang mengurangi viewport minimum; status bookmark sukses dikosongkan sehingga viewport Baca memenuhi80px. Gerbang akhir di atas lulus.

## Batas dan langkah berikut

Paket/ZIP/launcher/source commit/push akan dicatat sesudah checkpoint dan pengemasan, terpisah dari gerbang source ini. Tidak mengklaim paket lama sudah memuat perubahan. H-02 ListView historis tetap OPEN; suite akhir tidak mereproduksinya. Windows DPI nyata, keyboard fisik/screen reader, perangkat kedua/offline fisik dan penilaian kenyamanan pengguna masih PENDING. Pada minimum, lipat catatan dan gulir katalog/editor; bukan semua kontrol tampil sekaligus.

Perubahan dashboard/simulasi, laporan/presentasi dan fitur tambahan ditunda. Tunggu review pengguna atas layar ini. Draf .codex/, docs/academic/ dan empat draf audit/kit/review/morning tidak diubah atau disertakan.

---

# Pemeriksaan checkpoint Git - 16 November 2026 (WIB)

Atas permintaan pengguna, staging diperiksa: 28 berkas UI/design system/launcher/dokumentasi/tes sesuai daftar; satu commit lokal sebelumnya843aaa9 juga diperiksa karena ikut push. .codex/config.toml, docs/academic dan draf audit/kit/morning tetap di luar staging. Build/cache/DB/log/rahasia tidak disertakan; Wrapper tetap tracked.

Pemeriksaan ulang sebelum commit: `mvnw.cmd verify` **179tes lulus** dan suite14class opt-in **15kasus GUI/benchmark lulus**, seluruhnya0failures/errors/skipped. Log lokal `tmp/github-check-20261116-unit.log` dan `tmp/github-check-20261116-gui.log`. Sintaks dua script PowerShell valid; diff --cached --check lulus. Status penerimaan tetap **CONDITIONALLY READY**: tidak menjalankan uji interaktif EXE/shortcut, DPI nyata, komputer kedua atau offline fisik pada checkpoint ini; H-02 tetap OPEN. Tidak membuat paket baru atau mengubah implementasi.

Build verifikasi kini menghasilkan JAR SHA256 `E5FE50A971C46A98C1C1374F3CF1A9D5868605469FD3B152F07EFC3C0A1E6053`, berbeda byte dari JAR app-image15November `6F43155BA180DE5FB04519ACA5B0E91D294E20A3C24FD0ECD81A50E977F4C6AA`; keduanya memakai versi0.5.4-rc1. Riwayat kecocokan hash Fase7 di bawah berlaku saat audit tersebut, bukan klaim kedua artefak masih identik setelah rebuild ini. Tes terbaru memverifikasi build source, bukan EXE paket; paket terdahulu dipertahankan dan tidak diregenerasi. Hasil commit/push normal main serta hash remote dicatat pada receipt lokal `tmp/ui-github-checkpoint-20261116.txt` dan jawaban akhir.

---

# Status Fase 7 - 16 November 2026 (WIB)

**QA akhir: CONDITIONALLY READY, belum READY FOR RELEASE.** Working tree saat ini lulus `mvnw.cmd verify` (179 tes) dan suite JavaFX terpisah (15 kasus); masing-masing 0 gagal/error/skip. Source sejak Fase 5C tetap identik dan paket app-image tetap cocok dengan JAR hasil build terkini. Seluruh 30 PNG Fase 5C dan dua PNG recovery cocok ukuran/hash. Tidak ada defek UI baru yang terbukti, sehingga tidak mengubah kode aplikasi. H-02 ListView tetap OPEN; dua log baru tidak memuat exception tersebut.

EXE sebelumnya terlihat membuka jendela, tetapi **interaksi EXE, login/navigasi/simpan/tutup/buka ulang melalui paket, double-click Desktop, keyboard fisik, pembaca layar, Windows DPI nyata dan komputer kedua belum diverifikasi**. Shortcut uji sementara benar; shortcut Desktop pengguna tidak ada dan Desktop berada di luar cakupan tulis sesi ini. Tidak membuat installer, staging, commit atau push; seluruh pekerjaan lama tetap tersedia. Kesimpulan, perintah tepat, bukti, tingkat isu dan langkah uji manual berada di [PHASE_7_QA.md](PHASE_7_QA.md). Jangan distribusikan sebagai rilis final sebelum gerbang interaktif diselesaikan.

---

# Status Fase 6 - 15 November 2026 (WIB)

**App-image Windows terbaru berhasil dibangun dan diverifikasi paketnya; peluncuran EXE terlihat sebagai jendela Windows, tetapi interaksi di dalamnya serta double-click shortcut belum dapat diverifikasi. Status Fase6: sebagian terverifikasi.** Gerbang otomatis Fase5C di bawah telah lulus sebelum pekerjaan ini dimulai. Tidak ada installer, staging, commit atau push.

Lokasi rilis baru: `releases/TutupBuku-0.5.4-rc1-20261115-072902/TutupBuku/TutupBuku.exe`; ZIP di direktori rilis yang sama. Versi JAR aplikasi `6F43155BA180DE5FB04519ACA5B0E91D294E20A3C24FD0ECD81A50E977F4C6AA`, identik dengan JAR target terbaru dan berbeda dari paket lama (`62AB91...`). `Verify-WindowsPackage.ps1` mengekstrak ZIP ke `tmp/Uji Paket Ekstraksi 20261115-072935`: SHA256 ZIP `CD4A5566CFFBA3A8605CAA5955CDA1C3419C933CB5B31D20F112684473A670B5`, 229file manifest lolos,0file terlarang, runtime bawaan21.0.13 menampilkan17modul, 13JAR dependensi dan dua CSS ada di JAR. Ikon merek .ico belum tersedia; shortcut memakai ikon yang diekstrak dari EXE bawaan jpackage.

Build pertama `build.log` gagal pada pemeriksaan `Files.isWritable` jpackage terhadap direktori rilis sandbox, meskipun Java berhasil membuat file di sana. Probe menunjukkan temp proses writable=true. `Build-Windows.ps1` diperbaiki agar jpackage membuat image pada temp proses, lalu menyalin hasil selesai ke folder rilis baru. Build kedua `-Offline -SkipBuild` lulus pada source/JAR yang sudah diverifikasi; tidak mengulang179tes untuk perubahan packaging saja. Folder keluaran pertama `071956` ditandai `INCOMPLETE.txt` dan tidak dianggap paket. Paket lama tidak ditimpa atau dihapus.

Script `scripts/Create-DesktopShortcut.ps1` ikut di app-image. Uji pada direktori sementara membuktikan `.lnk` bernama Tutup Buku mengarah ke EXE rilis baru, working directory folder image dan ikon EXE. Pemanggilan ulang tidak mengubah hash .lnk; shortcut konflik ditolak tanpa perubahan byte; paket tidak lengkap menghasilkan pesan bahasa Indonesia. **Shortcut Desktop pengguna belum dibuat**, karena writable root lingkungan tidak mencakup Desktop. File uji berada di `tmp/phase6-20261115/shortcut-test/`, bukan Desktop. Status double-click dari Desktop tetap NOT VERIFIED.

EXE hasil ekstraksi dijalankan dengan `TUTUP_BUKU_DATA_DIR` absolut ke fixture `tmp/phase6-20261115/data`, JAVA_HOME/MAVEN_HOME kosong dan PATH hanya System32. Proses 12512 bertahan selama45detik dan JavaFX menulis warning lama unnamed module; inventaris Windows Computer Use melihat jendela `Tutup Buku — Kandidat Rilis 0.5.4-rc1`. Namun binding jendela gagal dengan `foreground window did not report a process id`, sehingga screenshot isi, login, navigasi, penutupan normal dan double-click nyata **tidak terbukti dari EXE**. Upaya awal tanpa sesi induk bertahan tidak menghasilkan jendela yang dapat diamati; tidak disimpulkan sebagai crash. Pengujian GUI JavaFX dari source pada Fase5C mencakup navigasi belajar/nilai, CSS, autosave/resume dan persistensi, tetapi bukan pengganti uji EXE tersebut.

Fixture terisolasi dibuat dari class paket, berisi satu akun fiktif, semester/mata kuliah, lima materi contoh, huruf aktual B+ dan satu skenario. Inspeksi setelah peluncuran menemukan nilai yang sama dan `PRAGMA quick_check=ok`; tidak membaca/menulis database default pengguna. Ini bukti data uji tidak rusak, bukan bukti penyimpanan melalui interaksi EXE. Error launcher sebelum JavaFX, interaksi layar EXE, shortcut Desktop, komputer kedua, Windows DPI nyata dan offline fisik belum teruji.

Dokumentasi prosedur berada pada `WINDOWS_DESKTOP_LAUNCHER.md`; bukti `tmp/phase6-20261115/` berisi log build, verifikasi ZIP, fixture, konfigurasi shortcut, proses dan diagnosa. Keluaran lama tetap tersedia. Setelah lingkungan GUI bisa mengikat proses dan Desktop dapat ditulis, uji login, navigasi, simpan/tutup/buka ulang, lalu pasang dan double-click shortcut tanpa mengganti data pengguna. Tidak memulai fase berikutnya.

---

# Status Fase 5C - 15 November 2026 (WIB)

**Gerbang otomatis Fase5C LULUS. Fase6 baru dimulai setelah verifikasi berikut selesai.** Seluruh layar produk diinventarisasi pada UI_COMPONENTS; tema Modern Academic Workspace diterapkan pada shell, profil, akademik, belajar/editor/reader, nilai aktual, skenario/perbandingan, dialog dan startup. Pilot autentikasi dipertahankan. Tidak ada perubahan domain/service/persistence/security/schema/dependency/pom. Hash69berkas terlindungi (termasuk draf/config) identik checkpoint awal.

Scope terbaru:5A=Design System,5B=pilot,5C=adopsi seluruh UI,6=launcher setelah gerbang5C, berbeda dari milestone produk/roadmap UI historis. Semua riwayat di bawah dipertahankan. Tidak ada staging/commit/push/reset, installer atau fase7.

## Implementasi dan batas eksplisit

Token21warna sama; native controls/table/tab/list/dialog mengikuti tema. Form card, panel putih, aksi primary/danger, penekanan hasil, code editor, reader.css dan header ringkas melengkapi fondasi. Semua guard/worker/service tetap. Footer Fase3 dan limitWidth/labelFor/password Fase2/4 tetap. Sidebar belajar memiliki cabang TitledPane collapsible dengan pilihan tetap; **TreeView pengganti dikecualikan** karena memerlukan perubahan model seleksi/worker/guard, bukan dibangun sebagai navigasi duplikat. File chooser/window chrome native dan fixture CalculationDemoView juga dikecualikan. Tidak ada layar produk lain yang sengaja tertinggal.

Header compact pada tinggi<600logical px mempertahankan identitas/semester/shortcut dan menghilangkan slogan dekoratif saja. Sidebar semester/reader/editor bergulir; teks tombol wrap. Halaman minimum760x480 tetap memerlukan scroll, bukan semua data terlihat bersamaan. MarkdownRenderer/CSP/security tidak disentuh; reader memakai stylesheet UI sendiri, script halaman tetap disabled.

## Bukti pengujian aktual

Folder bukti: `tmp/phase5c-20261115/`. Semua DB/aset fixture @TempDir. Maven Wrapper, Temurin21.0.13+9, JavaFX21.0.10, cache offline. NO perubahan DPI Windows global.

| Gerbang | Hasil | Log |
|---|---|---|
| Fondasi5A sebelum migrasi |1kasus lulus|design-system-gate.log|
| Shell/profil/auth/R002 |3kasus lulus|shell.log|
| Akademik/belajar/reader/autosave/resume |3kasus lulus|academic-learning.log|
| Nilai/failure/guard |2kasus lulus|grades.log|
| Regresi seluruh bisnis/persistensi |179tes,0gagal/error/skipped|final-regression.log|
| Seluruh14class opt-in |15kasus=14GUI+1benchmark,0gagal/error/skipped|final-gui-2.log|
| Emulasi125% |7kasus,0gagal/error/skipped|final-scale.log|
|1024x768 M4/R002 |2kasus lulus|final-small.log|
|760x480 M4 |1kasus lulus; footer dan viewport>=80 tetap|final-minimum.log|
| ModernWorkspaceFxCheck |7layar x3native +125%, collapse mempertahankan pilihan, computed reader14px/script disabled, tombol/scroll/dialog/PNG|dalam suite native/scale|
| CSS dan Git |Source/classes/JAR dua CSS identik; tanpa warning CSS/exception baru; diff --check lulus; index kosong|phase5c-verification.json|

Setup: `$env:JAVA_HOME=(Resolve-Path '.tools/jdk-21.0.13+9').Path`; `$env:MAVEN_USER_HOME=Join-Path $PWD '.tools/maven-user'`. Semua perintah memakai `.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp ... verify`, GUI dengan `-DreuseForks=false`. Class suite:JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ReleaseHardeningFxCheck,RevisionOneFxCheck,RevisionTwoFxCheck,ReusableComponentsFxCheck,DesignSystemFxCheck,AuthPilotFxCheck,ModernWorkspaceFxCheck,CalculationPerformanceCheck. Scale menambah `-DargLine=-Dglass.win.uiScale=125%`; ukuranM4 lewat phase3.width/height dan R002 lewat revision.width.

Kegagalan pengembangan tidak disembunyikan: fixture baru awalnya belum memilih semester (modern-layout.log); diperbaiki pada setup fixture. Minimum M4 gagal viewport55px (minimum.log), diperbaiki header compact sehingga112px tanpa melonggarkan assertion. Suite pertama gagal lookup semester-list sebelum skin ScrollPane terpasang (final-gui.log); helper count menunggu node berbatas15detik, assertion jumlah data tetap. Pengulangan terarah dan suite final di atas lulus. Tidak mengulang kegagalan tanpa diagnosis.

30PNG baru pada after-native/after-scale untuk tujuh layar dan dialog. Setiap PNG baru dibaca ulang dan ukuran dicocokkan dengan Scene:1350x729,1008x729,744x441 native;1080x584 pada125%. Target Stage1366x768/1024x768/760x480 berbeda dari PNG. Dua recovery PNG1024x760 tetap disimpan dan mode exactCapture tidak diubah; tidak menjalankan ulang mode khusus tersebut. Baseline target sebelum fase disalin ke before; arsip fase lama tidak dihapus. after-native/after-scale juga memuat salinan output lama yang tidak dihasilkan ulang; jangan menghitung semuanya sebagai bukti baru.

Perubahan AuthPilot hanya ekspektasi scope tema setelah login (kini workspace bertema); algoritma resize/capture/timeout recovery dipertahankan. ModernWorkspaceFxCheck baru menguji reachability, PNG aktual dan dialog; MilestoneTwoFxCheck hanya await skin. Bukti hash awal/source sebelum edit berada di initial-hashes.json/source-before. Seluruh perubahan sebelumnya tetap di workspace.

**Batas:** H-02 ListView OPEN; keyboard OS/fisik, screen reader, Windows DPI nyata, perangkat kedua/offline fisik/usability belum diverifikasi. Lulus di atas adalah gerbang otomatis/presentasi pada lingkungan ini, bukan sertifikasi aksesibilitas atau penerimaan rilis lapangan. Belum ada regresi baru yang diketahui tersisa dari Fase5C.

---

## Riwayat sebelum Fase 5C

# Recovery screenshot - 15 November 2026

**Selesai: dua PNG pesan error pilot kini tepat1024x760. Tidak ada perubahan kode aplikasi.** Inspeksi state aktual menemukan perbaikan wait resize dari sesi sebelumnya sudah tersimpan dan `final-pilot-size.log`/`final-pilot-scale.log` benar-benar BUILD SUCCESS. Tidak mengulang implementasi5A/5B atau regresi yang sudah berhasil.

Target sebelumnya adalah ukuran **jendela**1024x768;50arsip PNG1024 yang diperiksa pada target/verification dan after sebenarnya1008x729 karena dekorasi Windows. Itu bukan PNG1024x760, sehingga kebutuhan recovery terbaru diterapkan sebagai mode capture konten terpisah. Arsip dan penamaan lama tidak ditimpa/dihapus. Mode default/matrix historis tetap.

Hanya `src/test/java/id/ac/uns/tutupbuku/service/AuthPilotFxCheck.java` dan tambahan status ini diperluas. Flag `pilot.exactCapture=true` mengatur Scene dari selisih Stage/Scene yang diukur, menunggu ukuran aktual, lalu memeriksa ulang dimensi PNG yang dibaca dari disk. Tidak memakai crop, scaling atau interpolasi gambar. Timeout kasus90detik; penantian resize memakai await berbatas yang sudah ada. Dua output baru berawalan capture membedakan target piksel dari target jendela lama.

Perintah benar-benar dijalankan satu kali, exit0, BUILD SUCCESS,1kasus/0failure/error/skipped:

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME = Join-Path $PWD '.tools/maven-user'
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=AuthPilotFxCheck' '-DreuseForks=false' '-Dpilot.exactCapture=true' '-Dpilot.capture.width=1024' '-Dpilot.capture.height=760' verify
```

Output `pilot-login-status-capture-native-1024x760.png` dan `pilot-register-status-capture-native-1024x760.png` di target/verification, disalin identik ke `tmp/recovery-20261115/screenshots/`. Java ImageIO dan pemeriksaan independen Pillow sama-sama memastikan kedua file valid dan tepat1024x760. Feedback wrap/terlihat setelah scroll, input gagal tetap, retry login, scope tema dan navigasi belajar/nilai tetap lulus. Bukti: `tmp/recovery-20261115/exact-capture.log` dan `verification.json`.

SHA256 seluruh76file src/main dan50arsip gambar awal tetap identik; perubahan/draf awal lain dipertahankan. Command tes selesai exit0, tanpa command gagal yang dicoba berulang, staging/commit/push/reset/rollback atau fase baru. Pemeriksaan proses lewat CIM ditolak lingkungan dan hasil kosongnya tidak dianggap bukti; alternatif Get-Process benar-benar menemukan0proses java/javaw. Pengujian luas179tes/13GUI+benchmark di bawah tetap bukti sesi sebelumnya, bukan diklaim dijalankan ulang pada recovery yang hanya mengubah fixture ini.

## Riwayat sebelum recovery

# Status terbaru - Fase 5A dan 5B, 14 November 2026

**Fase5A Formal Design System1.0 dan Fase5B satu pilot Masuk/Daftar selesai diimplementasikan dan diverifikasi otomatis.** Fondasi bukan lagi sekadar CSS root/heading:21token warna semantik, hierarki12/13/17/22/36, skala jarak/radius, kontrol/state, card/panel, navigasi dan fokus tersedia sebagai CSS opt-in. Kontrak resmi, contoh dan usulan5C ada di [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md). Seluruh perubahan tetap uncommitted, tanpa staging/push. Tidak membuat launcher/installer atau memulai5C.

## Lingkup, pilot dan pemetaan riwayat

Instruksi terbaru mendefinisikan5A=Formal Design System,5B=satu Modern UI Pilot,5C=perluasan yang belum diizinkan. Roadmap historis UI_AUDIT_AND_PLAN dan catatan Fase1-4 tidak ditulis ulang. Bagian fase5 tree opsional di roadmap lama bukan pekerjaan sekarang. Catatan audit launcher di bawah yang menyebut5A/5B belum terdefinisi adalah riwayat sebelum otorisasi terbaru; kedua fase sekarang telah didefinisikan/diimplementasikan. Launcher tetap ditunda karena instruksi tugas ini secara eksplisit melarang pembuatannya.

Dashboard diprioritaskan dalam audit: DashboardView runtime langsung menampung LearningView dan GradeView beserta tab/guard/refresh, bukan landing page ringkasan. Mengubahnya menjadi kartu route baru memperluas migrasi ke dua modul dan mengubah struktur navigasi. Karena prioritas Dashboard bersyarat aman, dipilih **AuthView (Masuk/Daftar)** sebagai satu halaman autentikasi terisolasi. Tidak menambah route atau memodernisasi isi workspace. `.tb-theme` hanya diberikan ke AuthView; fixture memeriksa scope hilang setelah login.

Audit memakai AGENTS, PRD25halaman yang telah dibaca pada audit proyek, rencana/status/roadmap/inventaris aktual, entry point/CSS dan source UI/fixture yang relevan. FormSupport/PasswordInput dipakai kembali; helper lebar dan aksesibilitas Fase2/4 dipertahankan. Implementasi baru hanya UI Java/CSS, dokumentasi dan fixture GUI; domain/service produksi/persistence/security, rumus, schema4/RULE_VERSION1, pom/Wrapper, dependency dan scripts tidak berubah.

## Berkas yang ditambah/diperluas pada tugas ini

| Berkas | Implementasi |
|---|---|
| `src/main/resources/id/ac/uns/tutupbuku/ui/tutup-buku.css` | Pertahankan rule legacy root/heading; tambah Design System opt-in,21looked-up colors, skala jarak/radius, native button/input/check/tab/list/scroll, feedback, card/panel dan fokus. |
| `src/main/java/id/ac/uns/tutupbuku/ui/FormSupport.java` | Tambahkan kelas peran tb-input/button/field-label/field/form/help/status/scroll pada factory yang sama. Urutan node, labelFor/password association, method limitWidth Fase4 dan konfirmasi tetap. Di luar subtree tema kelas tersebut tidak mengubah tampilan. |
| `src/main/java/id/ac/uns/tutupbuku/ui/AuthView.java` | Aktifkan tema hanya halaman ini; card640px/padding halaman24, eyebrow akademik, primary action dan tab, brand36 dari CSS. Status netral wrap hanya visible/managed bila ada teks. Handler login/daftar, worker, callback, Enter, guidance dan reset password tetap. |
| `src/test/java/id/ac/uns/tutupbuku/service/DesignSystemFxCheck.java` (baru) | Galeri test-only:21warna benar-benar diselesaikan JavaFX, kontras, seluruh skala jarak/padding/radius, hierarki font, state primary/destruktif/disabled/readonly/error/focus, tab/list selected dan isolasi legacy. |
| `src/test/java/id/ac/uns/tutupbuku/service/AuthPilotFxCheck.java` (baru) | Aplikasi asli dengan @TempDir: feedback gagal login/daftar, input dipertahankan, retry login, keluar scope tema dan navigasi belajar/nilai; tiga ukuran native+125%. Menunggu resize native benar-benar selesai. |
| `docs/DESIGN_SYSTEM.md` (baru) | Kontrak resmi, kelas/pemakaian, intentional changes, aksesibilitas/batas dan usulan roadmap5C. |
| `docs/DEVELOPMENT_STATUS.md` | Tambahkan laporan ini; seluruh isi awal dokumen tetap sebagai suffix riwayat di bawah. |

Tidak mengedit DashboardView, WorkspaceView, AcademicView, PasswordInput, MaterialEditor, MarkdownReader atau tiga pane nilai pada tugas5A/5B. Perubahan Fase3/4 di file-file tersebut tetap ada tanpa ditimpa. Patch UI relatif awal sesi dicatat di `tmp/ui-phase5ab-20261114/phase5-ui.diff`, terpisah dari diff Git gabungan fase-fase yang belum di-commit.

## Perubahan visual yang disengaja dan aksesibilitas

Auth sebelumnya memakai panel580px tanpa permukaan card khusus, padding halaman12, form gap12/padding20 dan label gap4. Sekarang card putih640px/radius16/shadow tipis, padding halaman24, gap form16/padding16, label gap8, label bold13, bantuan12, deskripsi/status13, brand36 melalui kelas CSS, input radius8 dan aksi utama teal. Latar #f5f7f6/Segoe UI13 serta lebar NIM200/password260/pembungkus366/nama300 tetap. Tidak mengubah seluruh palette/layout workspace. Label status yang sama tetap menerima pesan UiTasks; panel netral tidak menebak kategori error dari teks service. State tb-error/success/warning tersedia/teruji untuk penggunaan eksplisit, bukan perubahan validasi bisnis.

Fokus input memakai ring2px; tombol border2 dan ring luar3px; tab/list/check tetap native dengan indikator fokus. Tombol/field/label role dan nama aksesibel tetap. Pengukuran warna akhir: teks utama12,39:1, muted5,73:1, primary/on-primary5,47:1; status semantik5,97-6,54:1 pada permukaannya. Border akhir #738b90 memberi3,61:1 pada putih,3,35:1 pada canvas dan3,22:1 pada muted. Divider dekoratif tidak dijadikan satu-satunya batas kontrol. Ini bukti pasangan warna yang diuji, bukan sertifikasi aksesibilitas seluruh aplikasi.

Pada fixture pilot, viewport native1366/1024 adalah451logical px, minimum760 adalah163px dan proses125%305,6px. Form panjang/error di bawah dicapai dengan scroll; tidak mengecilkan input atau mengklaim semua isi tampil bersamaan. Pesan gagal native memakai33px (125%33,6px), wrap/tidak terpotong dan berada dalam viewport setelah digulir.

## Verifikasi aktual akhir

Temurin21.0.13+9 portable, JavaFX21.0.10, Maven3.9.11 lewat Wrapper3.3.4 dan cache offline lokal. Semua DB/aset @TempDir/in-memory, bukan database default. Tidak mengubah DPI/resolusi/Java Windows global. Setup dan perintah:

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME = Join-Path $PWD '.tools/maven-user'
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ReleaseHardeningFxCheck,RevisionOneFxCheck,RevisionTwoFxCheck,ReusableComponentsFxCheck,DesignSystemFxCheck,AuthPilotFxCheck,CalculationPerformanceCheck' '-DreuseForks=false' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=DesignSystemFxCheck,AuthPilotFxCheck,ReusableComponentsFxCheck,RevisionTwoFxCheck,MilestoneFourFxCheck' '-DreuseForks=false' '-DargLine=-Dglass.win.uiScale=125%' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=MilestoneFourFxCheck,RevisionTwoFxCheck' '-DreuseForks=false' '-Dphase3.width=1024' '-Dphase3.height=768' '-Drevision.width=1024' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=MilestoneFourFxCheck' '-Dphase3.width=760' '-Dphase3.height=480' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=AuthPilotFxCheck' '-DreuseForks=false' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=AuthPilotFxCheck' '-DargLine=-Dglass.win.uiScale=125%' verify
```

| Pemeriksaan | Hasil aktual; bukti relatif `tmp/ui-phase5ab-20261114/` |
|---|---|
| Regresi + JAR pada CSS final | BUILD SUCCESS;179tes,0failure/error/skipped; `final-verify.log`. |
| Seluruh13class opt-in yang tersedia | BUILD SUCCESS;14kasus=13GUI+1benchmark,0failure/error/skipped; `final-gui.log`. |
| Emulasi125% | BUILD SUCCESS;5kasus,0failure/error/skipped; `final-scale.log`. Design System, pilot, form, R002 dan M4. |
|1024x768 | BUILD SUCCESS;2kasus M4+R002,0failure/error/skipped; `final-small.log`. |
|760x480 M4 | BUILD SUCCESS;1kasus,0failure/error/skipped; `final-minimum.log`. Empat variasi M4 termasuk native1366 di suite dan125% di atas. |
| Fixture pilot sesudah penguatan wait resize | Dua BUILD SUCCESS;masing-masing1kasus,0failure/error/skipped; `final-pilot-size.log` dan `final-pilot-scale.log`. Native satu kasus meliputi1366/1024/760; scale satu kasus1366/125%. Tidak mengubah kode aplikasi setelah suite luas. |
| Keyboard/accessibility otomatis | R002 Tab/Shift+Tab/Space/Enter/F4/Escape, labelFor/password aktif, nama/peran/help/fokus dan ukuran lokal tetap lulus native/1024/125%. Tidak menambah focus stop pada dekorasi. |
| Fungsional/regresi lintas modul | Kalkulasi batas/target/IP, current vs snapshot/attendance/review/CAS, transaksi/migrasi/isolasi, failure/input tetap/retry/guard, belajar/assets/catatan/completion/autosave/resume/restart tetap lulus pada fixture lama. Expected result bisnis tidak diganti. |
| Benchmark |30coursex20komponen; cold74,152ms,3warmup/20sampel, median12,248ms, p9518,343ms, max20,710ms. Bukan pengukuran render atau jaminan perangkat lain. |
| Resource/log/diff | CSS source/classes/entry JAR identik SHA256 `9b7e2ead1093e9e1489adb608c05a752fc38475eab12be360472e087f16543aa`; tidak ada ERROR/exception/warning CSS pada tujuh log akhir. Warning JavaFX unnamed module/unchecked helper lama tetap nonfatal. `git diff --check` lulus. |

Baseline sebelum pilot:11kasus GUI lulus (`before-gui.log`). Fondasi5A diuji sebelum penerapan5B (`phase5a-design-system.log`,1kasus lulus). Run pengembangan `pilot-first.log`, `pilot-feedback.log` dan verify/gui/scale/small/minimum tanpa awalan final juga lulus, tetapi bukan pengganti bukti final setelah border diperkuat. Audit tambahan menemukan border awal hanya memenuhi3:1 pada putih; token digelapkan dan seluruh regresi/suite/matrix diulang pada CSS final di atas.

QA screenshot menemukan dua gambar feedback bernama1024 masih1350px lebar akibat resize native belum selesai. Fixture AuthPilot kemudian menunggu ukuran Stage DAN Scene aktual, tanpa perubahan produksi. Pengulangan akhir menghasilkan native scene1350x729,1008x729,744x441 sesuai target window1366/1024/760; PNG proses125%1080x584. Gambar feedback yang salah ukuran tidak dipakai sebagai bukti1024. Fixture form dan R0021024 sudah benar ukurannya pada suite/matrix; log dan metadata PNG akhir diperiksa lagi. Tidak mengklaim hasil suite luas sebelum penguatan wait sebagai pengujian sizing baru tersebut.

## Perbandingan screenshot akhir

46gambar baseline native berpasangan dengan hasil akhir berukuran sama: **12gambar pilot berubah disengaja;34gambar nonpilot terdiri dari29identik byte/piksel dan5variasi.** Semua6gambar profil/semester dari fixture komponen pada1366/1024/760 identik. Direktori `before/` dan `after/` terpisah;105gambar after mencakup tambahan125%,1024,minimum, galeri serta feedback. Hanya46pasangan yang benar-benar sepadan dihitung; galeri/error baru bukan pasangan baseline. Screenshot M4 tanpa suffix disalin segera setelah suite native, sebelum run125%/minimum menimpa nama di target. Detail hash/bounding box: `comparison.json`.

| Variasi nonpilot | Pengamatan / batas |
|---|---|
| `m3-learning.png`, `m5-learning-native.png` | Selisih kecil pada waktu materi. Reader/container lainnya sama pada pasangan akhir ini. Variasi reader/scroll historis sebelumnya tetap belum dinyatakan selesai. |
| `m4-targets.png`, `phase3-long-comparison-native-1366x768.png` | Posisi scroll body dan state fokus Keluar berbeda; tabel hanya tampak penuh pada salah satu posisi snapshot. Header/footer/katalog dan hasil fixture tetap. Pasangan bukan bukti identitas piksel pada state scroll yang sama; variasi belum diselesaikan. |
| `r001-semester-native.png` | Indikator fokus Keluar; area lain identik. |

Tinjauan visual dilakukan pada login native1366/1024, register minimum/125%, feedback minimum/125%, galeri, dan sebelum/sesudah M4. Tidak ditemukan perubahan geometri pada halaman nonpilot yang diperiksa; tidak menyatakan seluruh screenshot identik atau semua keadaan UI/data panjang telah diverifikasi. Modernisasi halaman lain tidak diteruskan untuk memperbaiki variasi snapshot di luar lingkup.

## Preservasi, sisa dan berhenti

Git tetap main/HEAD `843aaa9a57266cee29b46bf5a4a703d14af02d52`, staging kosong. Awal22file berubah/untracked dicatat di `initial-hashes.json`/`initial.diff`;19file awal selain AuthView/FormSupport/status yang diperluas tetap identik SHA256. Ini mencakup empat kode/tes Fase3, PasswordInput/AcademicView/WorkspaceView dan fixture form Fase4, UI_COMPONENTS, audit launcher serta sembilan pengecualian. Isi awal DEVELOPMENT_STATUS dipertahankan byte sebagai suffix. Dashboard/engine/service produksi/persistence/security/pom/Wrapper/scripts tidak berubah. Akhir11tracked modified dan15untracked (termasuk tiga file baru5A/5B); belum stage/commit/push atau membuat branch/remote baru.

H-02 tetap **OPEN**. Keyboard OS/fisik, screen reader manual, Windows UI Automation eksternal, DPI Windows nyata, komputer kedua dan usability tetap **NOT VERIFIED**. Rasio warna/KeyEvent/property/snapshot bukan penerimaan manusia atau sertifikasi aksesibilitas keseluruhan. **Fondasi Design System1.0 selesai sesuai kontrak**, sedangkan migrasi halaman lain belum dilakukan. Usulan5C ada pada DESIGN_SYSTEM: review pilot/manual, profil-akademik, shell Dashboard, nilai, lalu belajar/reader/dialog bertahap. Semua hanya rekomendasi dan memerlukan instruksi baru. Tidak memulai5C, launcher, installer, staging, commit atau push.

## Riwayat sebelum Fase5A/5B

# Tambahan lingkup - Windows Desktop Launcher, 14 November 2026

Pengguna menambahkan launcher/shortcut Desktop **Tutup Buku** ke lingkup Fase5, dengan syarat implementasi hanya sesudah Fase5A dan Fase5B selesai serta terverifikasi. **Gerbang tersebut belum dapat diverifikasi:** dokumen lokal dan konteks yang tersedia belum memuat definisi/kriteria/bukti5A/5B. Milestone produk M5 historis tidak disamakan dengan keduanya. Tidak menebak lingkup atau memulai implementasi/Fase5C.

Audit hanya-baca menemukan Launcher main biasa, JavaFX Application, Java21/JavaFX21.0.10/Wrapper3.3.4/Maven3.9.11, script app-image jpackage dan EXE lama dengan runtime/dependensi. **JAR paket lama berbeda hash dari target Fase4 dan belum memiliki resource CSS Fase1**; nomor versi sama tidak berarti kode terbaru sudah didistribusikan. Tidak mengemas ulang atau mengubah paket lama. Tidak mengklaim launcher tambahan sudah standalone, desktop shortcut sudah ada, atau double-click sudah diuji.

Kebutuhan, inventaris, lokasi/hash, working directory, runtime, kontrak error dan rencana verifikasi setelah gerbang lulus dicatat di [WINDOWS_DESKTOP_LAUNCHER.md](WINDOWS_DESKTOP_LAUNCHER.md). Perubahan pada pekerjaan ini hanya dokumen tersebut dan tambahan status ini. Source/tes Fase3-4 serta draf pengguna dipertahankan; tidak menjalankan tes/launcher baru karena gerbang belum terpenuhi. Git tetap main/HEAD843aaa9, staging kosong; tidak commit/push/installer/shortcut Desktop. Hasil Fase4 di bawah tetap riwayat yang berlaku untuk kode tersebut.

# Status terbaru - Fase 4 komponen UI reusable, 14 November 2026

**Fase 4 selesai dalam lingkup standardisasi komponen yang disetujui; seluruh perubahan tetap di workspace tanpa staging/commit/push.** Tampilan dasar dan dimensi R002 dipertahankan. Formal Design System Foundation masih parsial; tidak dinyatakan selesai. H-02 tetap OPEN; keyboard OS, screen reader manual dan DPI Windows nyata tetap NOT VERIFIED.

## Keputusan lingkup terbaru dan preservasi

Instruksi pengguna terbaru secara eksplisit menetapkan Fase 4 sebagai **Reusable UI Components & Component Standardization**, menggantikan lingkup panel belajar/reader untuk pekerjaan sekarang. Roadmap historis `UI_AUDIT_AND_PLAN.md` bagian8 tetap utuh: Fase4 di sana berarti pengukuran panel belajar/reader, Fase5 adalah evaluasi tree opsional. Keduanya tidak dikerjakan otomatis. Catatan Fase3 di bawah tetap merupakan riwayat penutupan layout nilai, bukan instruksi aktif untuk commit/push atau penghambat otorisasi Fase4 terbaru.

Audit: AGENTS, teks seluruh PRD25halaman melalui ekstraksi lokal yang tersedia, README, rencana milestone produk, audit/roadmap, status, requirements, arsitektur, R001/R002, hardening/known issues/backlog dan source komponen terkait. Migration Plan UI formal dan DESIGN_SYSTEM.md tidak tersedia; IMPLEMENTATION_PLAN adalah rencana milestone produk. Inventaris, keputusan ekstraksi, lokasi penggunaan, kontrak helper dan sisa pekerjaan ada di [UI_COMPONENTS.md](UI_COMPONENTS.md).

Git awal: root proyek yang sama, branch main, HEAD `843aaa9a57266cee29b46bf5a4a703d14af02d52`, staging kosong. Lima perubahan tracked Fase3 dan sembilan pengecualian untracked sudah ada. Hash awal serta diff disimpan di `tmp/ui-phase4-20261114/initial-hashes.json` dan `initial.diff`. Fase4 tidak mengubah remote, branch, history, konfigurasi global atau `.codex/config.toml`.

## Perubahan dan pengurangan duplikasi

| Berkas Fase4 | Perubahan |
|---|---|
| `src/main/java/id/ac/uns/tutupbuku/ui/FormSupport.java` | Tambah limitWidth(Region,double) untuk pref+max lokal; status memakai factory note agar pembuatan Label wrap satu jalur. Factory label/input/button/layout dan konfirmasi tetap. |
| `src/main/java/id/ac/uns/tutupbuku/ui/PasswordInput.java` | Tiga pasangan setter lebar memakai helper. Binding dua arah, representasi aktif, toggle, labelFor/accessibility, Enter, hide/clear dan security tidak diubah. |
| `src/main/java/id/ac/uns/tutupbuku/ui/AuthView.java` | Tiga lokasi lebar NIM/nama memakai helper. |
| `src/main/java/id/ac/uns/tutupbuku/ui/WorkspaceView.java` | Tiga lokasi lebar profil memakai helper. |
| `src/main/java/id/ac/uns/tutupbuku/ui/AcademicView.java` | Nomor, tahun dan loop tiga tombol semester memakai helper. |
| `src/test/java/id/ac/uns/tutupbuku/service/ReusableComponentsFxCheck.java` (baru) | Kontrak ukuran aktual/pref/max, label/password visible, CSS/judul, setiap kontrol dapat digulir ke viewport, validasi semester0 menampilkan alasan tepat dan mempertahankan isian. |
| `docs/UI_COMPONENTS.md` (baru) | Inventaris, contoh pemakaian, analisis duplikasi, batas dan rekomendasi. |
| `docs/DEVELOPMENT_STATUS.md` | Tambahkan hasil Fase4; seluruh isi status Fase3 sebelumnya dipertahankan di bawah. |

**Duplikasi yang benar-benar dihapus:**12blok pasangan setPrefWidth/setMaxWidth dalam empat consumer menjadi12panggilan helper dengan satu implementasi pasangan setter. Baris pengaturan pada consumer24 menjadi12, berkurang12baris/50% khusus pola tersebut; bukan klaim pengurangan seluruh UI50%. Dua jalur Label wrap pada status/note menjadi satu. Tidak membuat class kontrol/framework produksi baru. Form labels, input, helper text, button dan layout yang sudah reusable dipertahankan alih-alih membuat abstraksi tandingan.

Lebar NIM200, nama300, password260/pembungkus366, prodi240, semester90/tahun110/aksi170 tetap. Minimum kontrol tidak disentuh; max-only pada course/editor tetap berbeda. Gap/padding form bersama dan kepadatan lokal nilai Fase3 tetap. CSS tidak diedit: latar #f5f7f6, Segoe UI13, heading22bold, auth36, materi17 tetap. Source CSS/classes/JAR cocok byte, SHA256 `923eab21a94fe3e4fcc0f802e343258402a2f5f077c915f76e014ac1b5f1b4eb`.

MaterialEditor dan MarkdownReader diaudit, tidak diedit. Tidak ada duplikasi editor/reader kedua yang membenarkan ekstraksi besar. Autosave/CAS/draft, script disabled/DOM link dan restore posisi memiliki kontrak khusus. Tidak mengubah bisnis, domain, service produksi, persistence, keamanan, schema4/RULE_VERSION1, dependency, pom/Wrapper atau scripts.

## Pengujian aktual pada kode Fase4

Temurin21.0.13+9 portable dan Maven Wrapper3.9.11/cache offline proyek, JavaFX21.0.10. Semua database/aset tes di @TempDir/in-memory. Tidak memakai database default pengguna atau mengubah DPI/resolusi Windows. Setup dan perintah:

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME = Join-Path $PWD '.tools/maven-user'
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ReleaseHardeningFxCheck,RevisionOneFxCheck,RevisionTwoFxCheck,ReusableComponentsFxCheck' '-DreuseForks=false' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=ReusableComponentsFxCheck,RevisionTwoFxCheck,MilestoneFourFxCheck' '-DreuseForks=false' '-DargLine=-Dglass.win.uiScale=125%' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=MilestoneFourFxCheck' '-Dphase3.width=1024' '-Dphase3.height=768' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=MilestoneFourFxCheck' '-Dphase3.width=760' '-Dphase3.height=480' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=MilestoneFourFxCheck,CalculationPerformanceCheck' '-DreuseForks=false' verify
```

| Pemeriksaan | Hasil aktual; bukti relatif `tmp/ui-phase4-20261114/` |
|---|---|
| Regresi default + JAR | BUILD SUCCESS;179tes,0failure/error/skipped; `verify.log`. |
| GUI lintas modul + fixture baru | BUILD SUCCESS;11kasus=10kasus lama+1baru,0failure/error/skipped; `gui.log`. |
| Emulasi proses125% | BUILD SUCCESS;3kasus,0failure/error/skipped; `scale.log`. M4, R002 dan fixture komponen. |
| M4 ukuran1024 dan760 | Dua BUILD SUCCESS;masing-masing1kasus,0failure/error/skipped; `m4-small.log`, `m4-minimum.log`. |
| M4 native1366 + benchmark | BUILD SUCCESS;2kasus,0failure/error/skipped; `m4-native-performance.log`. Native diulang agar tiga screenshot M4 tanpa suffix tidak tertukar dengan hasil minimum/125%. |
| Cakupan ukuran | Fixture komponen memeriksa1366x768,1024x768,760x480 native dalam satu kasus, serta1366x768 pada proses125%. M4 lulus empat variasi tersebut; native juga diuji dalam suite luas. |
| Benchmark tersedia |30coursex20komponen; cold22,818ms, median4,861ms, p957,140ms, max7,482ms;3warmup/20sampel. Bukan pengukuran render atau jaminan perangkat lain. |
| Aksesibilitas otomatis | R002 tetap lulus asosiasi label/peran/nama/help, Tab/Shift+Tab, Space, Enter, F4/Escape dan fokus. Password masked/visible, sinkronisasi/reset, submit, validasi, profil legacy dan guard Simpan/Buang/Batal tetap. |
| Perilaku lintas modul | Engine/target/IP/attendance, SQL/isolasi/migrasi, actual vs snapshot/usang/review, failure/input tetap/retry, autosave/catatan/completion/resume/restart tetap lulus pada fixture yang tersedia. Expected result bisnis tidak diubah. |
| Log dan resource | Tidak ada ERROR/exception/warning CSS baru dalam enam log akhir; warning JavaFX unnamed module dan unchecked helper lama tetap nonfatal. CSS classes/JAR identik dengan source. |

Baseline terpisah sebelum refactoring produksi: fixture komponen1kasus, suite lama10kasus dan emulasi komponen+R0022kasus lulus (`before-components.log`, `before-gui.log`, `before-scale.log`). Baseline125 memakai `surefire:test` atas class hasil kompilasi sebelum edit tanpa compile ulang, dengan hash class dicatat di `baseline-class.txt`; source produksi yang baru kemudian dikompilasi oleh verify. Penguatan assertion teks validasi dan formatting fixture baru tidak mengubah urutan snapshot. Baseline tidak ditambahkan ke hitungan pascaperubahan.

## Perbandingan visual dan keterbatasan

59gambar baseline sebelum edit dan86gambar hasil setelah edit disalin terpisah (`before/`, `after/`). **59pasangan berukuran sama;52identik byte/piksel dan7berbeda. Semua16pasangan khusus form komponen (login/daftar/profil/semester x4kondisi) identik byte.** Detail hash/bounding box pada `comparison.json`. Snapshot tanpa suffix M4 diambil ulang native sebelum dibandingkan; tidak membandingkan gambar native dengan hasil minimum yang menimpa nama output.

| Gambar berbeda | Pengamatan visual / batas kesimpulan |
|---|---|
| `m3-learning.png` | Timestamp dan posisi scroll WebView berbeda; baseline tampak di heading, hasil baru pada paragraf setelah resume. Container di luar area waktu/reader sama. Assertion resume lulus pada kedua run; pasangan ini tidak membuktikan identitas piksel konten reader. Variasi belum diselesaikan. |
| `m4-scenarios.png`, `phase3-long-comparison-native-1366x768.png` | Scroll body skenario berbeda (termasuk tabel yang baru terlihat penuh di salah satu snapshot) serta state fokus Keluar; header/footer/katalog dan hasil fixture tetap. Variasi sinkronisasi/state snapshot belum diselesaikan; bukan klaim semua gambar setara pada posisi scroll. |
| `m5-grades-native.png`, `r002-profile-125%-1366.png` | Perbedaan terbatas pada indikator fokus tombol Keluar. |
| `m5-learning-native.png` | Perbedaan kecil pada angka waktu materi. |
| `phase2-focus-password-125%-1366.png` | Perbedaan kecil indikator input/caret. |

Tinjauan visual sebelum/sesudah mencakup register760, profil125%, semester760, reader M3 dan dua skenario M4; ditambah16pasangan komponen identik. Tidak ditemukan perubahan geometri pada form yang distandardisasi. Kontrol bawah form minimum memang memerlukan scroll dan masing-masing diperiksa keterjangkauannya; tidak menjanjikan semua kontrol terlihat bersamaan atau semua data panjang sudah diuji. Variasi historis fase1-3 tetap belum dinyatakan resolved. Tidak mengubah fixture M3/M4 untuk menyembunyikan perbedaan screenshot.

## Status Git, sisa dan penghentian

Empat file kode/tes Fase3 (`ActualGradePane.java`, `GradeView.java`, `ScenarioPane.java`, `MilestoneFourFxCheck.java`) serta sembilan pengecualian tetap identik SHA256 dengan awal sesi. Isi awal DEVELOPMENT_STATUS dipertahankan utuh sebagai suffix setelah bagian Fase4 ini. Dokumen tersebut satu-satunya file bersama Fase3/Fase4.

Pengecualian yang tetap untracked dan tidak disentuh: `.codex/config.toml`; `docs/HUMAN_ACCEPTANCE_KIT.md`, `docs/MORNING_CHECKLIST.md`, `docs/P0_AUDIT.md`; `docs/academic/DEFENSE_QA.md`, `OOP_GUIDE.md`, `PRESENTATION.md`, `README.md`, `REPORT_DRAFT.md`. Hasil akhir10file tracked modified dan11untracked=9pengecualian+2file Fase4 baru. Staging kosong, branch main dan HEAD tetap; tidak ada staging/commit/push/branch switch/remote edit. `git diff --check` lulus.

**Sisa/batas:** H-02 OPEN; keyboard OS/fisik, screen reader manual dan DPI Windows nyata NOT VERIFIED. Formal Design System belum lengkap: token semantik, kontrak state/spacing lintas aplikasi, DESIGN_SYSTEM.md dan Migration Plan UI formal masih diperlukan. Rekomendasi untuk penentuan Fase5: review penerimaan manual, lengkapi kontrak formal, dan stabilkan pembanding snapshot; panel belajar/reader hanya bila kembali diizinkan. Tidak membuat installer atau memulai Fase5.

## Riwayat sebelum Fase4

# Status terbaru - modernisasi UI fase 3, 14 November 2026

**Pekerjaan layout modul nilai selesai; Formal Design System Foundation belum selesai.** Footer status aktual/skenario selalu terlihat pada fixture yang diuji, form nilai lebih rapat secara lokal, dan nama kolom perbandingan lengkap tersedia melalui tooltip native. Regresi179tes, suite GUI10kasus dan empat run M4 akhir lulus. Pemeriksaan keyboard OS/screen reader/perangkat nyata tetap **NOT VERIFIED**; bukan validasi seluruh keadaan aplikasi. Permintaan terbaru memisahkan checkpoint Fase3 ini dari Fase4 yang belum terdefinisi dan mengizinkan commit/push hanya lima file pekerjaan layout ke `rahadyand/Tutup-Buku` pada branch yang sudah ada. Pesan commit: `fix(javafx): improve grade pane layout and responsiveness`. Installer dan implementasi fase berikutnya tetap dilarang.

## Klarifikasi penutupan: layout nilai berbeda dari fondasi Design System

Pengguna menegaskan bahwa tujuan Fase3 yang dimaksud adalah **Formal Design System Foundation**. Implementasi sebelumnya memakai definisi fase3 pada `docs/UI_AUDIT_AND_PLAN.md` bagian8: "Status terlihat dan kepadatan nilai" dengan ActualGradePane/ScenarioPane/GradeView. Itulah alasan perubahan berfokus pada layout nilai. Penamaan "Fase3 selesai" pada laporan sebelumnya terlalu luas terhadap tujuan formal tersebut; yang terbukti selesai hanya pekerjaan layout dan pengujiannya. Koreksi status ini tidak membatalkan atau mengubah UI yang sudah diuji.

PRD hlm15–16/19 memuat arah visual dan keterbacaan, bukan katalog token atau pembagian fase modernisasi. AGENTS menegaskan preservasi perilaku/R002 dan batas UI. Migration Plan UI formal tetap tidak ditemukan; `docs/IMPLEMENTATION_PLAN.md` adalah milestone produk M1–M5, bukan penggantinya. `DESIGN_SYSTEM.md` juga tidak ditemukan pada workspace yang diperiksa. Tidak mengarang isi dokumen yang hilang atau menganggap rekomendasi audit sebagai spesifikasi formal lengkap.

| Unsur fondasi | Status aktual dan bukti |
|---|---|
| Warna / design tokens | Parsial: CSS root memiliki literal latar `#f5f7f6`, tetapi belum ada token bernama untuk permukaan, teks, border, fokus dan status semantik. |
| Tipografi | Parsial: Segoe UI13 dan heading22bold di CSS; auth36 di AuthView dan materi17 di MaterialEditor masih inline. Peran tersebut sudah ada, belum menjadi kontrak formal terdokumentasi. |
| Spacing / ukuran | Parsial: helper FormSupport memakai nilai Java lokal dan pane nilai memakai spacing/padding lokal. Belum ada skala/aturan spacing bersama terdokumentasi; ukuran R002 tetap harus dipertahankan. |
| Style reusable | Parsial: `.tutup-buku-root` dan `.section-heading` digunakan ulang; helper FormSupport/PasswordInput sudah ada. Belum ada katalog peran style kontrol/status beserta aturan pemakaiannya. |
| Dokumentasi formal / migrasi | Belum tersedia: `DESIGN_SYSTEM.md` dan Migration Plan UI formal. Audit dan status implementasi tidak dianggap pengganti keduanya. |

Sisa pekerjaan sebelum melanjutkan fase4, **belum diimplementasikan pada penutupan ini**:

1. Selaraskan tujuan/penomoran fase dan Migration Plan; gunakan referensi pengguna bila tersedia atau ajukan spesifikasi terbatas untuk disetujui. Jangan mengubah definisi secara diam-diam.
2. Dokumentasikan kontrak di `docs/DESIGN_SYSTEM.md`: token warna/semantik, peran tipografi13/17/22/36, spacing, radius/control sizing, state fokus/selected/disabled/error, dan batas lokal R002. Jangan menetapkan palet baru tanpa keputusan.
3. Pusatkan nilai/peran berulang yang disetujui ke JavaFX CSS dan style class bersama; gunakan helper/native controls yang sudah ada, tanpa framework/dependency atau abstraksi baru yang tidak dibutuhkan.
4. Petakan pemakaian dan pengecualian lokal, lalu migrasikan bertahap sesuai file yang disetujui. NIM200, nama course dan editor Markdown tidak boleh diseragamkan ke satu lebar tetap; navigasi/guard R002 tetap.
5. Verifikasi resource CSS/JAR, computed style, kontrol/state, screenshot lintas modul/ukuran dan regresi; catat pengujian manual yang belum dilakukan. Tutup fondasi formal berdasarkan bukti dan review pengguna sebelum fase4.

Tidak membuat DESIGN_SYSTEM.md/CSS baru atau mengubah empat file kode/tes pada langkah penutupan ini. Lima file commit tetap terdiri dari tiga pane/view UI, fixture M4, dan dokumen status ini. Sembilan file pengecualian tetap di luar staging; tidak menambah bisnis/dependency/schema. Hasil commit dilaporkan setelah Git benar-benar berhasil, bukan diasumsikan dari izin pengguna.

Verifikasi ulang penutupan benar-benar dijalankan pada source yang akan di-commit: `.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp verify` **lulus179tes** dan perintah yang sama dengan `'-Dtest=MilestoneFourFxCheck'` **lulus1kasus GUI**, keduanya BUILD SUCCESS,0failure/error/skipped. Lingkungan JDK/cache sama dengan bagian build di bawah; bukti `tmp/ui-phase3-20261114/closure-verify.log` dan `closure-m4.log`. Empat file kode/tes tidak diubah pada penutupan, sehingga laporan sebelumnya tentang suite GUI10kasus/empat ukuran tetap dibedakan dari dua run ulang ini. `git diff --check` juga lulus.

Percobaan commit yang diizinkan belum berhasil: `git add` lima file gagal membuat `.git/index.lock` dengan `Permission denied`; perintah commit tidak dijalankan. Metadata Git dilindungi lingkungan dan jalur approval eksekusi tambahan tidak tersedia. Sebelum percobaan, index kosong dan index.lock tidak ada; tidak menganggapnya stale lock atau menghapusnya. Tidak mengubah ACL, konfigurasi global, history atau cara penyimpanan Git untuk melewati batas. Commit belum memiliki hash baru; HEAD masih `843aaa9a57266cee29b46bf5a4a703d14af02d52`. Pengguna perlu menjalankan perintah commit terbatas dari PowerShell lokal yang memiliki izin tulis normal. Catatan ini merekam percobaan sesi, bukan hasil eksekusi manual di kemudian hari.

Pada pemulihan checkpoint Fase3, staging biasa kembali ditolak dengan error izin yang sama. Permintaan eksekusi khusus staging/commit lima file melalui mekanisme approval juga ditolak otomatis karena kebijakan sesi tidak mengizinkan peningkatan izin. Diagnosis hanya-baca `git ls-remote --heads origin refs/heads/main` gagal dengan `getaddrinfo() thread failed to start`; ini belum membuktikan kegagalan autentikasi. Remote fetch/push tetap `https://github.com/rahadyand/Tutup-Buku.git`, upstream `origin/main`. Commit baru belum dibuat dan push belum dicoba; seluruh kode/tes serta sembilan pengecualian dipertahankan, tanpa implementasi Fase4.

## Analisis awal dan lingkup

Fase3 diizinkan melalui permintaan pengguna "Lanjut fase 3" setelah commit fase1–2 `843aaa9a57266cee29b46bf5a4a703d14af02d52`. Git awal tidak memiliki diff tracked/staged; sembilan berkas untracked sebelumnya, termasuk `.codex/config.toml`, dipertahankan. AGENTS, PRD25halaman, roadmap UI dan hasil fase2 ditinjau kembali. ZIP refactoring, Design System formal dan Migration Plan UI tetap belum tersedia; acuan fase3 adalah bagian8 `UI_AUDIT_AND_PLAN.md`, bukan contoh struktur baru.

Baseline GUI M4 diperluas untuk mengukur layout sebelum perubahan produksi: empat run pada jendela target1366x768,1024x768, emulasi proses125%1366x768, serta minimum760x480 lulus. DB/trigger kegagalan hanya dalam @TempDir. Screenshot membuktikan status gagal aktual/skenario di luar viewport saat scroll0 dan baru terlihat saat scroll1. Pada1366native, header nilai64px, viewport408px, tinggi form aktual1081px/skenario1286px. Header nama skenario panjang dirender sebagai `Rencana semester de...`; nama penuh masih ada dalam data dan ringkasan bawah tabel.

Lingkup file yang ditentukan sebelum perubahan produksi:

| File relatif root | Koreksi minimum / batas |
|---|---|
| `src/main/java/id/ac/uns/tutupbuku/ui/ActualGradePane.java` | Pindahkan Label status yang sama dari form scroll ke footer; padding/gap lokal lebih rapat. Tanpa perubahan handler/guard/save/perhitungan. |
| `src/main/java/id/ac/uns/tutupbuku/ui/ScenarioPane.java` | Footer status yang sama, kepadatan form lokal, tooltip native berisi nama kolom lengkap untuk header yang terbukti terpotong. Saat verifikasi minimum menemukan footer terdorong oleh tinggi minimum katalog, katalog dibungkus ScrollPane native fitToHeight agar aksi tetap dicapai; wrapper tidak menambah perhentian Tab. Data, kolom, lebar, resize, sumber Aktual/Asumsi, selection/guard dan peringatan historis tetap. |
| `src/main/java/id/ac/uns/tutupbuku/ui/GradeView.java` | Kurangi gap dan padding vertikal header lokal; semua teks, selector, tab dan event tetap. Tidak menyentuh header R002/DashboardView. |
| `src/test/java/id/ac/uns/tutupbuku/service/MilestoneFourFxCheck.java` | Ukuran fixture dapat dipilih; screenshot/pengukuran dan assertion footer terlihat pada dua ujung scroll, pesan wrap, viewport minimum serta tooltip nama panjang; kegagalan/retry/guard/hasil lama tetap diuji. |
| `docs/DEVELOPMENT_STATUS.md` | Analisis, hasil aktual dan batas fase3 di bagian ini. |

Tidak menambah CSS/token/dependency/komponen produksi. Warna/font/heading/ukuran kontrol tetap fase1–2. H-02 historis, pembaca materi, tree navigation, perubahan perhitungan/domain/service/persistence/security/schema dan packaging di luar lingkup. Enam variasi snapshot fase2 dan tiga baseline berbeda fase1 tetap perlu review, bukan dinyatakan selesai oleh fase3. Keyboard OS, screen reader, DPI nyata/perangkat kedua tetap NOT VERIFIED.

## Perubahan dan kesesuaian visual

Tiga file produksi di tabel di atas hanya mengubah penyusunan node/presentasi. Label `actual-grade-status` dan `scenario-status` dipindahkan utuh ke bottom pane masing-masing, dengan padding6/8, lebar fleksibel dan minimum height sesuai preferred agar pesan wrap tidak ditekan. Footer kosong tidak mengambil ruang. Semua pemanggil UiTasks masih menerima instance Label yang sama, sehingga progress/disabled, hasil gagal/sukses, retry, dirty state, guard dan callback tidak berubah.

Gap form aktual/skenario12→8 dan padding20→12 hanya diterapkan pada dua body lokal. Header GradeView memakai gap6→4 dan padding vertikal8→4, padding horizontal8 tetap. Label/konteks, jumlah bobot, urutan kontrol, tab, tombol dan tinggi ListView komponen tetap. Katalog skenario memperoleh ScrollPane native fitToHeight; list/aksi yang sama tetap dapat digulir ketika viewport pendek. Wrapper tidak menjadi tambahan target Tab. Tidak mengganti daftar dengan tree.

Header tabel panjang tetap memakai ellipsis native, resize dan horizontal scroll; tooltip seluruh tabel memuat empat nama kolom lengkap. Nama penuh juga tetap tampil dalam ringkasan bawah. Tidak mengubah text/peran native TableColumn atau membuat cell factory/renderer baru. Tes membuktikan isi tooltip sama dengan keempat header, bukan bukti hover OS/screen reader manual.

CSS fase1–2 tidak berubah, termasuk `#f5f7f6`, Segoe UI13, heading22bold; override auth36/material17 dan dimensi R002 tetap. Source CSS identik dengan entry JAR Maven, SHA256 `923eab21a94fe3e4fcc0f802e343258402a2f5f077c915f76e014ac1b5f1b4eb`. Hash awal/akhir membatasi perubahan pada tepat lima file di atas; sembilan draf/config untracked tetap identik. Diff domain/service/persistence/security, pom/Wrapper dan scripts kosong; tidak ada rumus, SQL, format data, schema4, RULE_VERSION1 atau dependency berubah.

## Build dan pengujian aktual

Temurin21.0.13+9 dan Maven Wrapper3.9.11/cache offline lokal digunakan, tanpa mengubah Java/DPI Windows global. Semua database/aset memakai fixture @TempDir. Perintah yang dijalankan:

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME = Join-Path $PWD '.tools/maven-user'
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ReleaseHardeningFxCheck,RevisionOneFxCheck,RevisionTwoFxCheck' '-DreuseForks=false' verify
```

Empat run akhir memakai `.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=MilestoneFourFxCheck'` ditambah `-Dphase3.width=1366 -Dphase3.height=768`, `-Dphase3.width=1024 -Dphase3.height=768`, dan `-Dphase3.width=760 -Dphase3.height=480`, lalu `verify`. Run1366 tambahan menambahkan `'-DargLine=-Dglass.win.uiScale=125%'` untuk emulasi proses, bukan DPI Windows nyata.

| Verifikasi | Hasil aktual / bukti di `tmp/ui-phase3-20261114/` |
|---|---|
| Regresi default / build JAR | **BUILD SUCCESS,179tes,0failure/error/skipped**; `verify.log`. |
| Sembilan kelas GUI, fork terpisah | **BUILD SUCCESS,10kasus,0failure/error/skipped**; `gui.log`. Mencakup M2–M5, smoke/reader, hardening dan R001/R002. |
| M4 fixture akhir pada empat ukuran | **4×BUILD SUCCESS,masing-masing1kasus,0failure/error/skipped**; `after-native.log`, `after-scale.log`, `after-small.log`, `after-minimum.log`. Suite luas lulus sebelum penyesuaian assertion keterjangkauan aksi minimum; setelah itu hanya fixture M4 berubah dan diuji ulang pada empat ukuran ini. |
| Status / keterjangkauan aksi | Assertion footer tetap di luar scroll, seluruh pesan sesuai preferred height berada dalam pane/scene, viewport≥80logical px pada keadaan gagal fixture; scroll0 dan1. Simpan komponen serta Baru/Duplikasi/Hapus skenario masing-masing dapat digulir sampai seluruh tinggi tombol berada dalam viewport dan tombol tidak disabled. |
| Failure / retry / guard | Trigger gagal simpan aktual dan skenario tetap menghasilkan pesan gagal, mempertahankan isian30/nama draf, lalu retry berhasil sesudah trigger dilepas. Batal menahan navigasi. Tidak mengganti kegagalan dengan status sukses. |
| Hasil / persistensi / regresi lain | M4 tetap memeriksa saran60→55→70, IP4,00, kehadiran→E/IP0,00, basis usang/refresh, duplikasi/perbandingan dan restart. Suite lainnya mencakup kalkulasi batas/target/IP, penyimpanan/isolasi/migrasi, validasi/guard, autosave dan resume. Ini bukti otomatis pada fixture, bukan seluruh data/perangkat pengguna. |
| Keyboard / semantik fase2 | Assertion label/peran, fokus terlihat dan KeyEvent Tab/Shift+Tab/Space/Enter/F4/Escape dalam R002 tetap lulus dalam suite GUI. Keterjangkauan scroll baru M4 diperiksa secara programatik, bukan keyboard OS. |
| Log / diff | Tidak ditemukan `[ERROR]`, exception atau warning CSS pada enam log akhir di atas. Warning JavaFX unnamed module dan unchecked helper lama tetap. `git diff --check` lulus; warning konversi LF/CRLF bukan kegagalan. |

Run pengembangan yang gagal tetap dipisahkan dari hasil akhir:

- `development-compile.log`:1kesalahan kompilasi fixture karena indeks loop tertangkap lambda; diperbaiki dengan nilai final lokal, tanpa edit produksi.
- `footer-before.log`:1error assertion baru pada implementasi lama, karena parent status masih VBox dalam scroll; membuktikan tes mendeteksi masalah sebelum koreksi.
- `development-minimum-footer.log`/PNG:1error nyata di minimum760x480; minimum height katalog mendorong footer keluar scene. Diperbaiki dengan ScrollPane katalog dalam ScenarioPane, tidak mengubah guard/selection.
- `development-minimum-actions.log`/PNG:1error fixture yang mengharuskan tiga tombol sekaligus terlihat pada posisi scroll paling bawah, padahal tiga baris aksi melebihi viewport pendek. Fixture diperbaiki untuk menggulir ke setiap tombol dan tetap menuntut seluruh tinggi tombol terlihat; bukan melonggarkan batas footer atau menghapus assertion. Empat run akhir di atas lulus sesudah koreksi.

## Ukuran dan perbandingan screenshot

Nilai berikut adalah **logical px JavaFX**, pada keadaan gagal fixture yang sama, bukan semua isi form. Ukuran target jendela termasuk bingkai OS; scene native1366 adalah1350x729, emulasi125%1080x583. Header lebih kecil tetapi footer mengurangi tinggi viewport; keuntungan kepadatan berasal dari form yang lebih pendek, bukan klaim viewport membesar.

| Target / mode | Header sebelum→sesudah | Tinggi form aktual/skenario sebelum→sesudah | Viewport aktual/skenario sebelum→sesudah |
|---|---|---|---|
|1366x768 native|64→54|1081/1286→964/1149|408/408→389/389|
|1366x768 emulasi125%|65,6→55,2|1110,4/1308→992/1170,4|256/256→236/236|
|1024x768 native|64→54|1115/1320→998/1149|408/408→389/389|
|760x480 native|64→54|1149/1354→1015/1200|120/136→101/101|

Footer gagal fixture memakai29px native/30,4px emulasi125%; posisinya sama saat scroll0/1. Minimum760x480 tetap sempit: pengguna perlu menggulir body maupun katalog; bukan janji kenyamanan atau semua aksi tampil bersamaan.

Baseline `before/` berisi20gambar berpasangan pada empat ukuran; `final/` berisi36gambar khusus fase3, termasuk16gambar tambahan keterjangkauan aksi. Semua20pasangan berukuran sama dan berubah sesuai penataan area nilai. Pada18pasangan,150px bagian atas identik. Dua lainnya memiliki selisih tambahan di area Keluar: `phase3-long-comparison-native-1024x768.png` `(944,6,1002,35)` dan `phase3-scenario-error-0-native-1366x768.png` `(1286,6,1344,35)`; variasi state tetap perlu review, bukan diklaim resolved.

Terhadap salinan fase2, terdapat11gambar sepadan non-M4 yang benar-benar dihasilkan ulang suite GUI,7identik byte. Empat selisih:

| Gambar | Area selisih / penilaian |
|---|---|
|`m3-learning.png`|`(699,215,730,224)`, area waktu materi; tetap dicatat sebagai variasi, bukan perbaikan resume/scroll historis.|
|`phase2-focus-password-native-1366.png`|`(500,470,502,487)`, area indikator input; state snapshot perlu review.|
|`r002-header-native-1366.png`|`(6,176,1344,705)`, area nilai yang memang diubah fase3; header R002 bagian atas tetap.|
|`r002-profile-native-1366.png`|`(1286,6,1344,35)`, area Keluar; perlu review state.|

Tiga screenshot lama M4 tidak dihitung sepadan terhadap fase2 karena fixture M4 kini memakai ukuran jendela eksplisit. Bukti sebelum/sesudah fase3 menggunakan nama ukuran yang sama. Tidak menghitung gambar lama yang tidak dihasilkan ulang sebagai hasil run baru. Tinjauan visual dilakukan pada gagal aktual/skenario native1366/1024, aktual emulasi125%, katalog minimum dan tabel nama panjang; footer terlihat dan kontrol yang diperiksa tidak terpotong vertikal setelah digulir. Tooltip diuji melalui properti, belum melalui hover manual. Detail piksel: `comparison.json`; suite lintas modul disalin ke `after/`, baseline fase2 ke `phase2/`.

## Risiko tersisa dan penghentian

**NOT VERIFIED:** keyboard OS/fisik, screen reader/Narrator/NVDA, Windows UI Automation eksternal, DPI125% nyata/perangkat kedua, hover tooltip manual, serta seluruh kemungkinan data panjang/keadaan UI. Pemeriksaan KeyEvent/property/screenshot tidak menggantikan penerimaan manusia. Minimum760x480 hanya dibuktikan untuk fixture M4 ini. Nama kolom tetap dapat berellipsis; tooltip/resize/ringkasan mempertahankan akses teks lengkap, bukan redesign tabel responsif menyeluruh.

H-02 tetap **OPEN, OUT OF SCOPE**. Tidak mengubah ListViewBehavior/selection atau mengklaim akar masalah selesai; tidak ditemukannya exception pada log akhir bukan bukti perbaikan. Tiga baseline berbeda fase1 dan enam variasi fase2 tetap belum dinyatakan resolved; laporan lama dipertahankan.

Pada akhir implementasi layout sebelum permintaan penutupan ini, lima file tracked berubah dan sembilan draf/config untracked tetap; belum stage/commit/push atau membuat paket/installer. Berikutnya: review manusia status/scroll minimum, keyboard/screen reader dan variasi snapshot. Fase4 (pengukuran panel belajar/reader pada roadmap yang tersedia) ditahan sampai fondasi formal diverifikasi dan ada persetujuan baru. Rollback bila perlu hanya patch lima file pekerjaan layout, tanpa reset/clean atau menyentuh data/draf lain.

## Riwayat hasil fase 2

# Status terbaru - modernisasi UI fase 2, 14 November 2026

**Implementasi yang ditargetkan selesai; verifikasi otomatis JavaFX lulus.** Verifikasi keyboard OS, screen reader manual dan perangkat pengguna tetap **NOT VERIFIED**. Fase2 tidak dinyatakan sebagai validasi aksesibilitas menyeluruh. Aplikasi tetap0.5.4-rc1/Java21/JavaFX21.0.10/schema4/RULE_VERSION1. Tidak memulai fase3, commit, push atau paket Windows.

## Analisis sebelum implementasi dan batas yang disetujui

Analisis sebelum implementasi: AGENTS, PRD25halaman, hasil fase1, roadmap audit dan Ponytail FULL dibaca ulang. Migration Plan UI/Design System formal tetap tidak tersedia; roadmap dalam `UI_AUDIT_AND_PLAN.md` menjadi definisi fase yang tersedia. Persetujuan terbaru membatasi pekerjaan pada aksesibilitas dengan tampilan fase1 tetap, sehingga tidak mengubah palet/CSS atau membuat framework komponen.

Git awal memuat perubahan fase1 pada FormSupport/TutupBukuApplication/JavaFxSmokeCheck/DEVELOPMENT_STATUS dan resource CSS baru, serta draf untracked lama. Semuanya dipertahankan; hash source/dokumen awal dicatat di `tmp/ui-phase2-20261114/baseline-hashes.json`.

| Temuan sebelum edit | Bukti dan keputusan lingkup |
|---|---|
| Label form belum terkait input | `FormSupport.field` hanya membuat Label dan input. Target: `labelFor` langsung untuk kontrol native; factory centered memakai jalur yang sama. Ini juga menjangkau pemakai helper pada modul lain tanpa mengedit view tersebut. |
| Label password belum menunjuk representasi aktif | `PasswordInput` adalah HBox berisi masked/visible dan tombol. Target: label mengarah field aktif; nama toggle memasukkan konteks label; bantuan tidak mengambil isi password. Peran PASSWORD_FIELD/TEXT_FIELD/BUTTON tetap native. |
| Nama tombol | Tombol standar sudah memiliki teks bermakna. Kekurangan terkonfirmasi adalah toggle "Tampilkan"/"Sembunyikan" tanpa konteks tiga password. Tidak mengganti nama/peran semua tombol secara generik. |
| Bantuan dan nama navigasi | Target AuthView: panduan NIM dan bantuan submit password; WorkspaceView: profil readonly/legacy dan navigasi; AcademicView: nama dua daftar dan bantuan Simpan vs Jadikan Aktif. Tidak mengubah event atau validasi. |
| Keyboard/fokus/dialog | Tidak ada handler traversal khusus atau style yang menghapus fokus. Fokus native terlihat pada bukti fase1; keterjangkauan melalui tombol keyboard belum dibuktikan. Target verifikasi Tab/Shift+Tab, toggle, Enter/Space, ComboBox dan Escape/Batal dialog; CSS tetap kecuali ada bukti yang memerlukan persetujuan lain. |
| Kontrol di luar file fase2 | Folder-name belajar, TextArea Markdown/note, selector yang dibuat langsung dan baris kunci/asumsi memerlukan perubahan view tersendiri bila hendak diberi semantik khusus. Tidak diedit; perlu persetujuan tambahan untuk lingkup file tersebut. Guard GradeForms/MaterialEditor tetap tanpa perubahan. |

File yang direncanakan: `src/main/java/id/ac/uns/tutupbuku/ui/{FormSupport,PasswordInput,AuthView,WorkspaceView,AcademicView}.java`, fixture R002 yang sudah ada `src/test/java/id/ac/uns/tutupbuku/service/RevisionTwoFxCheck.java`, dan dokumen status ini. Ini lima file UI serta fixture terkait yang disebut fase2 pada roadmap, bukan izin mengedit layer bisnis atau file fase3/4. Tidak menambah class produksi, dependency, selector CSS, shortcut, atau urutan fokus buatan.

Tiga baseline fase1 dipertahankan untuk review: `m3-learning.png` (waktu/posisi isi WebView), `r002-courses-125%-1366.png` dan `r002-courses-native-1024.png` (fokus Keluar). Salinan26gambar dan catatan perbandingan berada di `tmp/ui-phase2-20261114/phase1/` dan `phase1-comparison.json`. Belum ada klaim perbedaan terselesaikan.

H-02 ListView tetap masalah historis terbuka, di luar fase2. Bagian di atas mencatat keputusan sebelum edit aplikasi; hasil pelaksanaan berikut dibedakan dari rencana dan hasil fase1.

## Perubahan fase 2

| File relatif root proyek | Implementasi dan alasan |
|---|---|
| `src/main/java/id/ac/uns/tutupbuku/ui/FormSupport.java` | `field` menghubungkan Label langsung ke input native. `fieldCentered` tetap memakai helper tersebut. PasswordInput ditangani sebagai kontrol gabungan, tanpa mengubah layout VBox4 atau factory lain. |
| `src/main/java/id/ac/uns/tutupbuku/ui/PasswordInput.java` | Binding `labelFor` mengikuti field masked/visible yang aktif; nama aksesibel toggle memuat label password/konfirmasi dan berubah bersama teks Tampilkan/Sembunyikan. Bantuan wrapper diteruskan ke kedua field. Tidak mengambil isi password sebagai nama/help, tidak mengganti peran native atau handler/timing/fokus. |
| `src/main/java/id/ac/uns/tutupbuku/ui/AuthView.java` | Panduan NIM menjadi accessible help yang mengikuti teks panduan yang sudah ada. Bantuan Enter/minimum password dan nama navigasi akun ditambahkan, tanpa mengubah validasi/login/registrasi/sapaan. |
| `src/main/java/id/ac/uns/tutupbuku/ui/WorkspaceView.java` | Bantuan NIM readonly, panduan prodi legacy dan nama navigasi workspace. Field tetap dapat difokuskan sesuai perilaku native; canonicalisasi, dirty guard, posisi header dan shortcut tetap. |
| `src/main/java/id/ac/uns/tutupbuku/ui/AcademicView.java` | Nama aksesibel dua daftar serta bantuan nomor/tahun, Simpan dan Jadikan Aktif. Semua handler, selection, save/activate, dimensi dan metadata-only attendance tetap. |
| `src/test/java/id/ac/uns/tutupbuku/service/RevisionTwoFxCheck.java` | Fixture R002 diperluas dengan pemeriksaan15kontrol logis berlabel, representasi password aktif, nama/help/peran native, lima jalur Tab/Shift+Tab, fokus terlihat, kontrol keyboard dan dialog. Ekspektasi lama tetap; kasus tambahan berada pada tes opt-in yang sama, bukan penambahan angka kasus JUnit. |
| `docs/DEVELOPMENT_STATUS.md` | Analisis awal, hasil aktual, kegagalan pengembangan, perbandingan screenshot, batas dan penghentian fase2. |

Pemeriksaan hash awal/akhir menunjukkan tepat tujuh file implementasi ini berubah sejak awal fase2. Bootstrap, CSS dan fixture smoke fase1 serta seluruh draf lain yang diinventarisasi tetap identik. `git diff` layer domain/service/persistence/security, pom, Wrapper dan script packaging kosong. Pada akhir sesi Git juga menampilkan `.codex/config.toml` baru yang untracked, dengan waktu pembuatan bertepatan percobaan alat Windows; berkas tersebut tidak diedit, dihapus atau di-stage sebagai perubahan aplikasi.

Tidak ada `setAccessibleRole`, shortcut, handler traversal atau aturan CSS baru dalam kode produksi. Tombol yang sudah bernama jelas tetap memakai nama/peran native; hanya toggle gabungan mendapat konteks tambahan. Latar `#f5f7f6`, Segoe UI13, heading22bold, auth36, judul materi17, ukuran R002 dan seluruh selector fase1 tetap. CSS dalam source/JAR masih identik, SHA256 `923eab21a94fe3e4fcc0f802e343258402a2f5f077c915f76e014ac1b5f1b4eb`.

## Build dan hasil tes akhir

Semua perintah berikut benar-benar dijalankan pada Temurin21.0.13+9, Maven3.9.11/Wrapper, cache offline lokal. Database/aset tes memakai @TempDir; probe style memakai folder tmp baru, tanpa database AppData pengguna. Tidak mengubah Windows/DPI/jaringan global.

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME = Join-Path $PWD '.tools/maven-user'
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ReleaseHardeningFxCheck,RevisionOneFxCheck,RevisionTwoFxCheck' '-DreuseForks=false' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=RevisionTwoFxCheck' '-DargLine=-Dglass.win.uiScale=125%' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=RevisionTwoFxCheck' '-Drevision.width=1024' verify
```

| Run akhir | Hasil |
|---|---|
| Regresi default | **BUILD SUCCESS;179tes,0failure/error/skipped**; kompilasi dan JAR Maven berhasil. |
| Sembilan kelas GUI, fork terpisah | **BUILD SUCCESS;10kasus,0failure/error/skipped**. Termasuk smoke, reader, M2-M5, hardening dan R001/R002. |
| R002 emulasi proses125% pada target1366x768 | **BUILD SUCCESS;1kasus,0failure/error/skipped**, termasuk pemeriksaan aksesibilitas/keyboard JavaFX baru. |
| R0021024x768 native | **BUILD SUCCESS;1kasus,0failure/error/skipped**, termasuk pemeriksaan baru yang sama. |
| Style/protected paths/diff | Probe menghasilkan background/font/ukuran yang identik dengan fase1; source CSS sama dengan entry JAR. `git diff --check` lulus. Tidak ada exception atau warning CSS baru pada empat log final. |

Warning JavaFX unnamed module dan catatan unchecked helper lama tetap ada. Build/test default tidak diubah untuk menyembunyikan kegagalan. Regresi mempertahankan perhitungan/target/IP, persistensi/isolasi/migrasi, validation/guard/failure/retry, autosave/resume, materi aman dan skenario; ini bukti fixture otomatis, bukan semua perangkat atau semua keadaan aplikasi.

## Bukti keyboard dan aksesibilitas

`RevisionTwoFxCheck` memeriksa `AccessibleAttribute.LABELED_BY` dan labelFor secara dua arah, termasuk perpindahan label ke field password visible dan kembali masked. Peran PASSWORD_FIELD/TEXT_FIELD/BUTTON/COMBO_BOX/LIST_VIEW/TAB_PANE tetap native; atribut TEXT password masked tetap null. Nama toggle tiga password bermakna, panduan NIM tidak mengambil nama akun tersimpan, dan help tidak mengambil isi password.

Lima jalur traversal maju/mundur mencakup form Daftar, Masuk, Profil, Semester dan Mata Kuliah (28posisi kontrol pada lima jalur). Tes mengirim **KeyEvent melalui event dispatch JavaFX**, bukan `.fire()` untuk langkah keyboard tersebut dan bukan bukti injeksi tombol tingkat OS. Diuji: hidden password dilewati, toggle lewat Space, Shift+Tab ke field aktif, Enter login gagal/benar dengan input tetap, F4/Down/Enter ComboBox, Space Simpan, tiga tombol guard melalui Tab, default Batal, serta Escape mempertahankan form dan menahan navigasi. Assertion `isFocusVisible` dan screenshot fokus password/Simpan mendukung indikator native yang terlihat; tidak menambah custom traversal.

Screenshot `phase2-focus-password-*` dan `phase2-focus-save-*` diambil pada native1366, native1024 dan emulasi125%; fokus biru pada field/tombol ditinjau visual, termasuk Simpan profil125%. Ini tinjauan gambar oleh pengembang, bukan sesi screen reader atau uji keyboard fisik peserta.

Kegagalan selama pengembangan dicatat terpisah:

- `label-before.log`: tes relasi baru sengaja dijalankan sebelum perbaikan;1error karena LABELED_BY NIM null. `label-after.log`:1kasus lulus sesudah perbaikan.
- `keyboard.log`:1error karena fixture membaca login sebelum callback logout selesai; ditambahkan await view siap pada fixture.
- `keyboard-ready.log`/`keyboard-focus.log`:masing-masing1failure; JavaFX Robot tidak memindahkan fokus dari register-nim pada lingkungan ini. Alat akses jendela Windows juga gagal dimulai (`failed to launch codex app-server: program not found`). Keyboard OS tetap **NOT VERIFIED**, tidak dilaporkan lulus.
- `keyboard-javafx.log`:1error karena pengiriman event sinkron masuk ke dialog modal dan memblok future tes. Event kemudian diantrekan dengan Platform.runLater pada fixture; tidak mengubah aplikasi. `keyboard-modal.log`:1kasus terfokus lulus sebelum regresi akhir di atas.

## Perbandingan screenshot terhadap fase 1

Baseline26gambar fase1 dipertahankan. Hasil fase2: **20identik byte;6berbeda**. Seluruh ukuran gambar sama; pemeriksaan piksel membuktikan enam gambar berbeda identik di luar area berikut. Tidak mengklaim seluruh screenshot sama atau bahwa variasi fokus sudah diperbaiki.

| Screenshot | Area perbedaan `(x1,y1,x2,y2)` | Penilaian / status |
|---|---|---|
| `m3-learning.png` | `(717,215,730,224)` | Angka waktu materi berbeda pada run ini; isi/scroll kali ini cocok. Variasi scroll historis fase1 tetap **PERLU REVIEW**, bukan dinyatakan selesai. |
| `r002-courses-125%-1366.png` | `(1016,6,1074,36)` | Area tombol Keluar; baseline berbeda fase1 tetap **PERLU REVIEW**. |
| `r002-courses-native-1024.png` | `(944,6,1002,35)` | Area tombol Keluar; baseline berbeda fase1 tetap **PERLU REVIEW**. |
| `m4-scenarios.png` | `(1200,6,1258,35)` | Variasi tambahan terbatas area tombol Keluar; state snapshot perlu review. |
| `r002-courses-native-1366.png` | `(1286,6,1344,35)` | Variasi tambahan terbatas area tombol Keluar; state snapshot perlu review. |
| `r002-register-native-1366.png` | `(760,501,858,526)` | Variasi tampilan tombol toggle konfirmasi; state fokus/hover belum dibuktikan penyebabnya. Tidak ditandai resolved. |

Tinjauan gambar tidak menemukan perpindahan layout pada layar yang dicakup. CSS dan computed style sama; perbedaan area tombol tetap didokumentasikan, bukan dihilangkan lewat CSS atau perubahan fokus untuk mengejar hash identik. Bukti: `tmp/ui-phase2-20261114/{phase1,after}/`, `phase1-comparison.json`, `comparison.json`, `{verify,gui,scale,small,style}.log`, log pengembangan di atas, dan target/surefire-reports. Probe style sama dengan fase1: NIM200/password260 pada Segoe UI13, brand36, heading22 dan backgroundf5f7f6.

## Risiko, H-02, dan penghentian

**NOT VERIFIED:** screen reader manual/Narrator/NVDA, integrasi Windows UI Automation eksternal, keyboard tingkat OS/fisik pada perangkat pengguna, DPI Windows125% nyata, perangkat kedua, seluruh kontrol di luar target dan seluruh keadaan minimum760x480. Tes KeyEvent/property JavaFX tidak menggantikan pemeriksaan tersebut. Penerimaan manusia diperlukan sebelum menyatakan aksesibilitas lengkap. ZIP/DS/Migration Plan formal masih tidak tersedia; tidak mengarang kepatuhan terhadapnya.

H-02 tetap **OPEN, OUT OF SCOPE**; fase2 tidak mengubah selection/ListViewBehavior atau mengklaim akar masalah selesai. Tidak ada exception H-02 pada log final yang diperiksa; ketiadaannya dalam run ini bukan bukti perbaikan. Kontrol langsung pada LearningView/MaterialEditor/GradeView/ScenarioPane di luar helper tetap memerlukan lingkup file tambahan bila ingin diperbaiki secara khusus.

Berhenti setelah fase2. Rekomendasi berikutnya: tinjauan screen reader/keyboard OS dan enam variasi snapshot, lalu keputusan lingkup fase3 (status nilai terlihat dan kepadatan lokal) sesuai audit. Tidak menjalankan fase3, mengedit file tambahan, membuat paket/installer, commit atau push.

## Riwayat hasil fase 1

# Status terbaru - modernisasi UI fase 1, 14 November 2026

**Fase 1 selesai dalam lingkup yang disetujui:** style root dan heading umum dipindahkan ke CSS, dengan nilai visual lama tetap. Baseline Git `220d1f0de270f11c1e5eb7b662bea789445021cd`, aplikasi tetap `0.5.4-rc1`, Java21/JavaFX21.0.10, schema4/RULE_VERSION1. Berhenti sebelum fase2; tidak membuat paket Windows, commit atau push.

## Lingkup dan berkas

Sebelum edit, Git tidak memiliki perubahan tracked/staged. Draf untracked `HUMAN_ACCEPTANCE_KIT.md`, `MORNING_CHECKLIST.md`, `P0_AUDIT.md`, `UI_AUDIT_AND_PLAN.md` dan `docs/academic/` dipertahankan tanpa perubahan. Definisi fase1 dibaca dari bagian9 `docs/UI_AUDIT_AND_PLAN.md`; PRD/AGENTS dan Ponytail FULL tetap berlaku. ZIP refactoring, Design System formal dan Migration Plan UI belum tersedia, sehingga tidak mengarang palet/token baru atau mengklaim kesesuaian dengan spesifikasi yang belum dibaca.

Hanya lima file berikut yang berubah pada fase ini:

| Berkas relatif root proyek | Perubahan dan alasan |
|---|---|
| `src/main/resources/id/ac/uns/tutupbuku/ui/tutup-buku.css` (baru) | `.tutup-buku-root`: latar `#f5f7f6`, Segoe UI13px; `.section-heading`:22px bold. Memusatkan nilai presentasi yang sebelumnya inline, tanpa aturan global untuk ukuran input/spacing/fokus. |
| `src/main/java/id/ac/uns/tutupbuku/ui/TutupBukuApplication.java` | Root memakai style class; Scene memuat CSS dari classpath. Lifecycle, handler, ID node dan ukuran jendela tetap. |
| `src/main/java/id/ac/uns/tutupbuku/ui/FormSupport.java` | Factory `heading` menambahkan kelas CSS, menggantikan inline22px bold. Factory lain tetap. |
| `src/test/java/id/ac/uns/tutupbuku/service/JavaFxSmokeCheck.java` | Scene fixture standalone memuat stylesheet yang sama; assertion baru memeriksa heading22px bold sesudah CSS/layout. Ekspektasi hitung dan alur lama tidak diubah. |
| `docs/DEVELOPMENT_STATUS.md` | Mencatat hasil implementasi, perintah, bukti dan batas aktual pada bagian ini. |

Override auth36px dan judul materi17px dipertahankan. Lebar lokal R002, struktur JavaFX programatik, header kanan, tab, dua halaman akademik, guard, CSS HTML reader dan seluruh dependency tetap. Diff domain/service/persistence/security/pom/Wrapper/scripts kosong; tidak ada migrasi atau perubahan format data.

## Build dan tes yang benar-benar dijalankan

Lingkungan: Temurin21.0.13+9 portable di `.tools/jdk-21.0.13+9`, JavaFX21.0.10, Maven3.9.11 melalui Wrapper. Semua database/aset tes memakai @TempDir; probe style memakai folder sementara baru di `tmp`, bukan AppData pengguna. Tidak mengubah pengaturan Windows global.

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME = Join-Path $PWD '.tools/maven-user'
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,RevisionTwoFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck' '-DreuseForks=false' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=RevisionTwoFxCheck' '-DargLine=-Dglass.win.uiScale=125%' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=RevisionTwoFxCheck' '-Drevision.width=1024' verify
```

| Pemeriksaan | Hasil aktual |
|---|---|
| Baseline sebelum edit: empat GUI di atas, lalu R002125% dan R0021024 |4+1+1kasus lulus;0failure/error/skipped.26snapshot disalin sebelum perubahan. Ini baseline terpisah, tidak ditambahkan ke hitungan pascaperubahan. |
| `verify` pascaperubahan | **BUILD SUCCESS;179tes,0failure/error/skipped**.75source dan27file tes dikompilasi;1resource disalin; JAR Maven berhasil dibuat. |
| Empat GUI pascaperubahan | **BUILD SUCCESS;4kasus,0failure/error/skipped**, fork terpisah. Termasuk assertion CSS baru pada fixture standalone. |
| R002 emulasi proses125% | **BUILD SUCCESS;1kasus,0failure/error/skipped**; target jendela fisik1366x768. |
| R002 lebar1024 | **BUILD SUCCESS;1kasus,0failure/error/skipped**; target1024x768 native. |
| Resource | CSS di `target/classes` dan entry `id/ac/uns/tutupbuku/ui/tutup-buku.css` dalam JAR identik byte dengan source; SHA256 `923eab21a94fe3e4fcc0f802e343258402a2f5f077c915f76e014ac1b5f1b4eb`. Bukan uji EXE distribusi. |

Probe Java sementara menjalankan aplikasi dengan database fiktif terisolasi, menunggu login siap, lalu membaca properti sesudah `applyCss/layout`. Hasil sebelum/sesudah identik: background `0xf5f7f6ff`; NIM Segoe UI13/lebar200; password Segoe UI13/lebar260; brand Segoe UI Bold36/lebar198; heading Segoe UI Bold22. Probe bukan fitur produksi atau pengujian keyboard.

## Bukti fungsi dan tampilan

Regresi default menguji kembali kalkulasi/target/IP, autentikasi/isolasi, transaksi/migrasi dan persistensi yang sudah ada. GUI memverifikasi60→55→70, override kehadiran, snapshot/usang/refresh/duplikasi/perbandingan, failure/retry/input tetap dan restart; M3 meliputi autosave, catatan/completion, script nonaktif, guard, urutan serta resume. R002 memeriksa ukuran lokal, sapaan/privasi, password/Enter handler, profil legacy/dropdown, save gagal/retry, guard Simpan/Buang/Batal, urutan aksi dan posisi shortcut kanan. Ini bukti otomatis pada fixture, bukan klaim seluruh perilaku atau perangkat telah diperiksa manual.

Perbandingan26snapshot sebelum/sesudah: **23identik byte**. Dua screenshot course125%/1024 hanya berbeda dalam area fokus tombol Keluar; screenshot M3 berbeda pada waktu materi dan isi/scroll WebView. Pemeriksaan piksel membuktikan bagian lain dari tiga gambar itu identik. Tidak mengklaim26gambar identik atau memperbaiki variasi state snapshot. Assertion resume M3 tetap lulus pada kedua run. Tinjauan visual dilakukan pada login native, course native/1024/125%, register/header125%, reader M3 dan tabel perbandingan M4; tidak ditemukan perubahan layout pada gambar yang diperiksa.

Bukti lokal: `tmp/ui-phase1-20261114/{baseline-gui,baseline-scale,baseline-small,verify,gui,scale,small}.log`, `baseline-style.log`, `style.log`, `comparison.json`, dan direktori `before/after` berisi screenshot. XML/TXT tes tetap di `target/surefire-reports`. Tidak ada exception atau warning CSS baru pada log build/GUI yang diperiksa. Warning JavaFX unnamed module dan catatan unchecked helper tes lama tetap muncul; tidak menyebabkan kegagalan. `git diff --check` lulus; peringatan konversi LF/CRLF Git bukan kegagalan tes.

## Risiko, batas, dan langkah berikutnya

Stylesheet menjadi resource wajib pada classpath; bootstrap dan isinya dalam JAR telah diperiksa. Semua constructor aplikasi dalam suite memakai jalur tersebut; fixture standalone disesuaikan secara eksplisit. API baru/pemuat tema/abstraksi tambahan tidak dibuat.

Belum dilakukan pada fase1: paket/EXE baru, perangkat kedua, offline fisik, DPI Windows125% nyata, perjalanan keyboard/screen reader manual, seluruh keadaan UI pada minimum760x480, benchmark atau usability mahasiswa. Emulasi125% dan pemanggilan event otomatis bukan pengganti pemeriksaan tersebut. H-02 historis ListView tetap terbuka, tidak dinyatakan selesai dari run ini. Variasi fokus/scroll pada snapshot membatasi perbandingan piksel menyeluruh; bukan alasan mengubah reader/guard di luar lingkup.

Berikutnya hanya setelah persetujuan: fase2 sesuai audit, yaitu relasi label dan peran tombol/fokus, dengan aturan warna yang disetujui terlebih dahulu. Tidak memulai fase2, tree navigation, perubahan spacing, reader HTML, refactoring lain atau packaging otomatis.

## Riwayat sebelum modernisasi UI fase 1

# Status terbaru - perawatan Ponytail FULL, 14 November2026

Baseline4dac60c/main diperiksa; dua penyederhanaan internal dipilih: hapus panduan NIM mati dan satukan validasi metadata Course. UI/engine/schema4 tetap. Baseline177/fokus16tes lulus; final179tes default+10kasus GUI+1skala proses125% lulus,0failures/errors/skipped. 14snapshot identik sebelum/sesudah. Paket0.5.4-rc1-20261114-124851: audit226hash+manifest/forbidden0/runtime17modul, seluruh227file cocok ekstraksi/JARtarget. EXE Enterlogin/save metadata/close/reopen duaexit0, SQLite4/integrity/FK dan data lama sah. SHA256ZIP445FC5DA871AD3679A73DC55FCC7B541B107713D02C77B5E2A0A0EDBCEB3EF95. Checkpoint normal main dengan receipt push aktual tmp/ponytail-checkpoint.txt/jawaban akhir; hasil terbaru di [PONYTAIL_FULL_REVIEW.md](PONYTAIL_FULL_REVIEW.md). Draf lama tetap di luar lingkup; tidak memulai dashboard.

## Riwayat sebelum perawatan

# Status terbaru — Revision 002, 14 November 2026

Kandidat0.5.3-rc1/schema4. Implementasi akun ringkas/sapaan, profil dropdown, urutan aksi akademik dan navigasi kanan selesai. **177tes default,10kasus GUI final,1pengulangan125% dan1jendela1024×768 lulus**,0failures/errors/skipped. Paket final20261114-120721: audit226hash+manifest/forbidden0/runtime17modul, seluruh227file ZIP cocok ekstraksi dan JAR target. EXE ekstraksi Enterlogin/saveprofil+course/close/reopen duaexit0; data/integrity/FK sah. HashZIP88B856B2304BF9D198B40A0033C885311EDD226F9D949015ACEA5750010F17A5. Source/test/docs checkpoint normal main; receipt push aktual di tmp/r002-checkpoint.txt dan jawaban akhir. Tidak regenerasi paket sesudah uji. Lihat [REVISION_002.md](REVISION_002.md) untuk hasil terbaru dan lingkup. Draf akademik lama tetap tidak disentuh. Gerbang lapangan/H-02 historis masih pending.

## Riwayat status sebelum Revision 002

# Status pengembangan — M5 kandidat Windows

Tanggal **9 November 2026, Asia/Jakarta**. Versi **0.5.0-rc1**, schema **3**. **Kandidat lokal M5 siap untuk penerimaan pengguna; gerbang lapangan tetap pending.** Tidak memulai fitur baru/laporan/PPT.

## Baseline dan lingkup

main/origin https://github.com/rahadyand/Tutup-Buku.git dan hash **cf37dbea4e0de35b729808ef130e21c065c9f04a** diperiksa sebelum perubahan. Working tree awal bersih. Baseline137tes diulang dan lulus. Seluruh PRD25halaman, AGENTS, checklist, plan, README, arsitektur/status dibaca kembali. Riwayat M4 disimpan di MILESTONE_4_STATUS.

Pemetaan perilaku/test/pendidikan lapangan ada di [INTEGRATION_REVIEW.md](INTEGRATION_REVIEW.md) dan REQUIREMENTS. F01–F13 tetap lingkup, F08G/F14/F15 bersyarat dan seluruh P2 tetap backlog. Jumlah tes bukan bukti tunggal; perjalanan multi-course dan paket nyata diperiksa.

## Defek/perbaikan

- Label akademik yang masih menyebut nilai belum tersedia diperbaiki.
- Profil dan dua form akademik mendapat Simpan/Buang/Batal; navigasi baru diteruskan setelah save berhasil.
- Edit metadata course mempertahankan attendance.confirmed bila met tidak berubah; perubahan met menjadi asumsi baru.
- Pilihan awal belajar/nilai mengikuti semester aktif jika belum ada pilihan tampilan.
- Save/refresh skenario mempertahankan course terpilih; ditemukan lewat alur multi-course.
- Status Belum disimpan, konfirmasi mode, Lulus/Tidak lulus, sumber Aktual/Asumsi di perbandingan, bantuan rumus dan konteks/bobot ditambahkan.
- Pada125% area reader hampir hilang karena note; note kini panel lipat, sidebar scroll, header ringkas dan window menyesuaikan visual bounds. Selisih IP panjang memakai≈6desimal, tanpa mengubah keputusan engine.
- Main Launcher non-Application, profil Maven runtime dan script Windows. Java21/JavaFX21 tetap, tanpa framework/dependency aplikasi baru; dependency-plugin3.8.1 hanya profil build distribusi.
- Tidak mengubah rumus M1 atau schema. Berkas pengguna/starter dipertahankan, tidak memakai DB nyata atau menghapus artefak rilis lama.

## Verifikasi otomatis akhir

PowerShell memakai:
```powershell
$env:JAVA_HOME = (Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME = "$PWD\.tools\maven-user"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp -Pwindows-package verify
```
Dijalankan melalui **scripts/Build-Windows.ps1 -Offline**. **BUILD SUCCESS;138tes,0failures/errors/skipped.** M1=70, M2=27 (AcademicPersistence14), M3=22, M4=19. Kompilasi72class source dan21file test (termasuk helper/opt-in checks); tidak memasukkan test-only ke aplikasi.

```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck' '-DreuseForks=false' verify
```
**BUILD SUCCESS;7tes,0failures/errors/skipped.** M1shell1, M2GUI2, readeraman1, M3belajar1, M4nilai1, M5terhubung1.

M5FxCheck: register akun baru, semester/course/aktif, guard profil/akademik+Batal/Buang, import/copy aset lewat LearningService yang sama dengan UI lalu pindah file asal, Markdown/tabel/kode/gambar/note/completion, empat course dan IP3,27, konfirmasi mode/estimasi/label lulus, save mempertahankan course, ubah SKS4 dan review/refresh, batal hapus, akun kedua kosong, restart/login/resume dengan note/completion/rasio≈0,6. File chooser native diuji terpisah pada paket, bukan diklaim dari service import dalam tes ini.

```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=MilestoneFiveFxCheck' '-DargLine=-Dglass.win.uiScale=125%' verify
```
**1tes lulus**, diulang setelah perbaikan reader. outputScale=1,25; jendela target fisik1366×768 (konten≈1080×584 logical pixels), desktop fisik1920×1080 dengan Screen logical1536×864 pada emulasi. Pengujian normal juga menargetkan jendela1366×768. **Resolusi/skala Windows global tidak diubah.** Screenshot native/125% di target/verification/m5-learning-*.png, m5-grades-*.png, m5-academic-*.png ditinjau; bacaan, status dan tombol dapat dicapai. Panel panjang digulir, note dilipat.

```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=MilestoneFiveFxCheck,CalculationPerformanceCheck' '-DreuseForks=false' verify
```
**2tes lulus.** Pengukuran terakhir diulang dengan parameter yang sama dan '-Dtest=CalculationPerformanceCheck' verify (**1tes lulus**); median memakai rata-rata dua sampel tengah. Benchmark: Windows11amd64, Temurin21.0.13+9-LTS,12CPUlogis.30course×20komponen,2aktual85/100 per course dan18bebas, bobot5%. Mengukur TargetPlanner+GradeCalculator+IP+review SQLite, bukan setup/hash/render.
- cold **21,738ms**
-3warmup,20sampel: min4,023ms; median **5,606ms**; p95 7,879ms; max **8,762ms**
- Target<1detik tercapai pada mesin ini, bukan klaim semua komputer.

Seluruh DB/aset tes di @TempDir. XML/TXT pada target/surefire-reports dan performance/layout.txt pada target/verification. Warning JavaFX unnamed module, unchecked cast helper tes, serta jlink compress=2 deprecated tercatat; tidak menggagalkan pemeriksaan. Error awal selama pengembangan (nullable preference, selection reset, sinkronisasi layout helper) diperbaiki sebelum hasil akhir.

## Paket Windows dan uji launcher nyata

Persiapan:
```powershell
.\scripts\Prepare-PackagingTools.ps1
.\scripts\Build-Windows.ps1 -Offline
.\scripts\Verify-WindowsPackage.ps1 -Zip 'releases\TutupBuku-0.5.0-rc1-20261109-145254\TutupBuku-0.5.0-rc1-windows-x64.zip'
```

Temurin portable21.0.13+9 resmi:
SHA256 a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9.
SDK OpenJFX21.0.10 untuk legal:
SHA256 8e87151957acf82205e85087071cc4393769a82e672bacebe63d945b718e7e55.
Tidak mengganti Java/PATH global. Runtime Temurin dipilih untuk distribusi beserta legal; semua regresi diulang pada vendor/patch ini.

Build memakai runtime scope13JAR yang dikenali, jlink17modul JDK (termasuk transitif) dan jpackage app-image. Legal JDK/OpenJFX/CommonMark/SQLite, panduan, release notes, checklist/demo dan hash disertakan. Penerima tidak membutuhkan Java/Maven/IDE. Tidak ada DB/aset pribadi, cache, Git, source atau JUnit/test-only dalam image.

Paket pertama **95.921.088byte**, SHA256 **7869b76414071af8bee5c6f071ee09469aca8a517765542058120846e8158812**. Audit Verify-WindowsPackage: **226file hash cocok** +manifest, forbidden0; runtime bawaan --list-modules berhasil dan JAR aplikasi tanpa source/test/data. Artefak pertama dipertahankan. Folder final bertimestamp terbaru dibuat setelah checkpoint untuk menyertakan dokumentasi/hash source terbaru; payload kode harus identik dengan JAR yang diuji.

JAR aplikasi yang dipakai/dibuktikan pada paket:
**2b954f23210ce7c2f63c1c8c5ed18c7866f5afaa544983427479f27d1c914508**.
Sama dengan target/tutup-buku-0.5.0-rc1.jar. BUILD-INFO dan SHA256SUMS mengikat input/payload; hash dan lokasi paket final dilaporkan pada jawaban akhir.

Pemeriksaan EXE hasil ZIP (bukan javafx:run):
- Ekstraksi ke **tmp/Uji Paket Folder Dengan Spasi/TutupBuku/TutupBuku.exe**.
- Child process: JAVA_HOME/MAVEN_HOME kosong, PATH hanya WindowsSystem32, properti proxy Java ke127.0.0.1:9. Tidak mengubah jaringan/firewall Windows, browser atau pengaturan global.
- Folder data **tmp/packaged-demo** di luar instalasi; tidak menyentuh AppData pengguna.
- Otomasi desktop dengan izin pengguna: daftar NIMfiktif0005/login, peminatan/bidang, materi Bacaan PaketM5, Markdown/tabel/kode, **file chooser PNG native**, note, selesai, pratinjau gambar biru.
- Tutup normal → **exit0**. Pindahkan sumber PNG buatan penguji tanpa menghapus aset terkelola. Buka EXE yang sama lagi, login, Lanjutkan Membaca: gambar/tabel/kode/note/completion1/1 dan akun tetap; gambar sumber sudah pindah. Tutup normal lagi → **exit0**.
- Ini uji paket otomatis/visual pada mesin pengembang, bukan uji usability mahasiswa. Nilai/skenario lengkap diverifikasi pada suite M4/M5 dengan SQLite sementara; tidak mengklaim seluruh perjalanan itu diulang manual pada paket.

Script menghasilkan folder timestamp baru dan menolak overwrite. ZIP/runtime tetap lokal dalam releases/ (ignored). Tidak ada GitHub Release otomatis.

## Dokumentasi dan checkpoint

README, requirements, arsitektur, plan, backlog, AGENTS, status historis M4, USER_GUIDE, RELEASE_NOTES, RELEASE_ACCEPTANCE, DEMO_SCRIPT dan THIRD_PARTY_NOTICES diperbarui. Demo9menit adalah langkah yang dapat diulang, bukan hasil pengukuran peserta.

Diff/ignore/daftar file/pola rahasia umum ditinjau sebelum staging; .mvn/Wrapper tetap dilacak. Commit diminta **feat: prepare milestone 5 Windows release candidate**, main pada repository yang sama. Hash/push diverifikasi dan dilaporkan di jawaban akhir; lihat git log -1/git ls-remote untuk hash commit yang memuat status ini.

## Pemeriksaan yang masih memerlukan pengguna

- Komputer Windows x64 kedua tanpa Java/IDE; termasuk MSVC/SmartScreen/AV/izin pada perangkat penerima.
- Jaringan **fisik** diputus dan link luar gagal wajar. Proxy proses bukan pengganti tes ini.
- Windows display1366×768/skala125% **nyata**; emulasi proses/window bukan perubahan layar global.
- Usability3–5mahasiswa: keberhasilan/waktu/kebingungan nyata tanpa bantuan langkah demi langkah.
- Verifikasi data normal AppData pada perangkat pengguna; pengujian pengembang sengaja memakai override demo agar tidak menyentuh database nyata.

Kandidat lokal siap dicoba, **penerimaan akhir pengumpulan belum dinyatakan selesai**. Ikuti RELEASE_ACCEPTANCE, laporkan masalah nyata sebelum pembekuan. Grouped/PDF/AI/kuis/backupUI/IPK/optimasi dan semua penundaan tetap BACKLOG. Tidak melanjutkan milestone/fitur/laporan/PPT otomatis.

## Penutupan verifikasi kandidat0.5.1-rc1 — 10 November 2026

Lingkup kelanjutan dipersempit pengguna: selesai verifikasi paket, bukan lanjut laporan/presentasi. Checkpoint executable30d257a649d6c653e0c3daa59735af1123f9654e sudah di-push sebelumnya. Tidak ada perubahan executable atau pengulangan regresi/build pada kelanjutan ini. Hasil163tes default,8GUI+benchmark1 dan skala proses1 dari9November tetap bukti historis revisi yang sama.

Run kedua EXE ekstraksi yang tertunda dilanjutkan: login00051, aktual Proyek paket100%/8/10 dan skenario Rencana Paket051/target3/IP Simulasi3,70 berhasil dibuka. Tutup normal exit0. Read-only DB membuktikan akun/aktif/course3SKS/note/completion dan snapshot tetap; integrity/FK sah. Seluruh227file ZIP sama hash dengan ekstraksi yang diuji, JAR sama target terverifikasi. ZIP95.921.473byte SHA256 BDA1BB4A92669ABA67680562638D355790E5D0EBE12447704F260A3DA36ED411, folder releases/TutupBuku-0.5.1-rc1-20261109-224207. Tidak regenerasi/unggah binary. Rincian di HARDENING_VERIFICATION.md.

H-02 belum selesai: log run kedua memuat IndexOutOfBoundsException JavaFXListView; pemicu belum pasti. Pembacaan data/skenario tetap berhasil dan exit0, tanpa kehilangan data teramati; bukan klaim bebas bug. Lihat KNOWN_ISSUES. Gerbang komputer kedua/offline fisik/Windows125% nyata/usability masih NOT PERFORMED. Draf audit/kit/academic yang sudah ada dipertahankan di working tree dan tidak dimasukkan checkpoint penutupan terbatas ini. Panduan/status/verifikasi/known issues saja diperbarui. Commit/push akhir diverifikasi pada Git; jika jaringan menolak, catat blokir dan pertahankan commit lokal.

## Revision001 — sedang diverifikasi, 14 November2026
PRD25halaman dan source aktual ditinjau. UI akun/navigasi/akademik/header telah direvisi; schema4 menjaga arsip dan transaksiaktivasi. Defaultbaseline163tes, RevisionOnePersistenceTest12tes awal, M2GUI2/M3GUI1/M4GUI1/M5GUI1/reader1/shell1/hardening1/revisi1 telah lulus pada run pengembangan. Ada koreksi header setelah saveprofil dan sinkronisasi layout tes sesudahlogout; belum mengklaim kandidat final. Tambahan kegagalan save semester/headerdelete/acuan kehadiran dan regresi akhir sedang dijalankan. Bukti/lanjutan di docs/REVISION_001.md. Draf akademik lama tetap tidak disentuh.

## Revision001 — penutupan verifikasi lokal
Kandidat final0.5.2-rc1 pada releases/TutupBuku-0.5.2-rc1-20261114-111743, SHA256ZIP F85224558D7899366E198FBCFD1931578217D4C1AE010554C143AAEAC04462EC. 176tes default lulus;9kasus GUI lulus melalui suite+pengulangan terfokus (satu error sinkronisasi layout tes diperbaiki). Sesudah perbaikan pemotongan label yang ditemukan pada screenshot125%, runM2/M5/Revision4tes skala proses lulus, assertion label1tes lulus, Revision normal1tes lulus. Ini bukan DPIWindows nyata. Screenshot finalnormal/125% di target/verification/r001-*.png ditinjau.

Audit paket226hash+manifest/forbidden0/runtime17modul lulus; seluruh227file ZIP cocok ekstraksi/JARtarget. EXE ekstraksi berspasi berjalan tanpa JAVA_HOME/MAVEN_HOME/PATHpengembang, data fiktif sendiri; Enterlogin, header/page1/page2/editmetadata/save dan close exit0. DBreadback/integrity/FK/schema4 sah, nilai/skenario tetap. Log hanya warningJavaFX. Tidak mengklaim seluruh tesGUI source diulang pada EXE. Artefak percobaan sebelumnya dan DB pengguna tidak dihapus/disentuh. H-02 historis dan gerbang lapangan tetap pending. Perintah/keputusan kompatibilitas lengkap di REVISION_001; hasil Git akhir di receipt tmp/r001-checkpoint.txt dan jawaban akhir. Berhenti sesudah Revision001 untuk review, draf akademik/P1/P2 tidak dilanjutkan.
