# Fase 7 — QA dan kesiapan rilis

16 November 2026 (WIB). **Keputusan: CONDITIONALLY READY (siap bersyarat), belum READY FOR RELEASE.** Regresi otomatis lulus pada working tree saat ini; interaksi langsung dengan EXE terkemas, shortcut Desktop nyata dan beberapa pemeriksaan lapangan masih menunggu. Tidak ada defek UI baru yang terbukti sehingga Fase 7 tidak mengubah kode aplikasi.

## Ruang lingkup dan bukti

Audit membaca AGENTS, DESIGN_SYSTEM, UI_COMPONENTS, DEVELOPMENT_STATUS, WINDOWS_DESKTOP_LAUNCHER, KNOWN_ISSUES, `tmp/phase5c-20261115/phase5c-verification.json`, `tmp/recovery-20261115/verification.json`, konfigurasi launcher, dan script shortcut. Inventaris layar produk berada di UI_COMPONENTS: startup/error, masuk/daftar, dashboard/shell, profil, semester/mata kuliah, belajar/editor/reader, nilai aktual, skenario/perbandingan, serta dialog. File chooser dan window chrome asli Windows dikecualikan; CalculationDemoView hanya fixture.

Sebelum tes, 147 file source/test/docs/scripts dicatat dalam `tmp/phase7-20261116/initial-hashes.json`. Pemeriksaan ulang sebelum penulisan laporan menemukan **0 perubahan** dari checkpoint tersebut. Hash source sejak gerbang Fase 5C juga tetap sama. Draf dan bukti historis tidak dihapus; Git staging tetap kosong. Log Fase 7 di `tmp/phase7-20261116/`.

## Tes yang dijalankan pada sesi ini

Perintah dijalankan dari root repository Windows dengan JDK portable proyek dan repository Maven lokal offline:

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME=Join-Path $PWD '.tools/maven-user'
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ReleaseHardeningFxCheck,RevisionOneFxCheck,RevisionTwoFxCheck,ReusableComponentsFxCheck,DesignSystemFxCheck,AuthPilotFxCheck,ModernWorkspaceFxCheck,CalculationPerformanceCheck' '-DreuseForks=false' test
```

| Bukti | Hasil | Cakupan |
|---|---|---|
| `tmp/phase7-20261116/regression.log` | BUILD SUCCESS; 179 tes, 0 gagal/error/skip | Rumus, validasi, auth, repository, migrasi, kegagalan/retry dan service terkait. |
| `tmp/phase7-20261116/gui.log` | BUILD SUCCESS; 15 tes GUI/benchmark, 0 gagal/error/skip | Form, password toggle, labelFor, traversal fokus sintetis, navigasi, materi, reader, autosave/resume, nilai/skenario, guard, CSS dan startup. |
| Fase 5C `final-scale.log`, `final-small.log`, `final-minimum.log` | Bukti historis: 7 kasus 125%, 2 kasus 1024, 1 kasus 760 lulus | Source sejak pengujian sama; pengujian ini tidak diulang dalam Fase 7. |

Kedua log baru tidak berisi `IndexOutOfBoundsException` atau test failure. Warning JavaFX `unnamed module` masih muncul dan sudah dicatat historis; warning ini tidak membuktikan kegagalan aplikasi. Tes memakai fixture terisolasi; database default pengguna tidak digunakan.

## Audit tampilan dan aksesibilitas

`tutup-buku.css` masih mengandung aturan hover, fokus, pilihan, disabled, danger/primary, panel, daftar, tabel dan dialog; `reader.css` tersedia. Audit visual sampel layar belajar minimum 760×480 dan nilai pada emulasi 125% menunjukkan header dan tindakan utama terlihat, panel panjang menyediakan scroll; pemeriksaan reachability otomatis tertera pada tes GUI. Tidak ada perombakan tampilan atau perubahan CSS pada Fase 7.

Seluruh **30 PNG Fase 5C dan 2 PNG recovery** dibuka sebagai gambar, ukuran piksel serta SHA-256 dibandingkan dengan manifest: **32/32 cocok**. Distribusi 30 PNG: 7×1350×729, 7×1008×729, 7×744×441, 7×1080×584, dua dialog masing-masing 434×168 dan 439×170. Dua PNG recovery tepat 1024×760. Ukuran PNG adalah *Scene/content* atau dialog; target 1366×768, 1024×768 dan 760×480 adalah *Stage/window*. Tidak menganggap emulasi `-Dglass.win.uiScale=125%` sebagai uji DPI Windows nyata. Variasi screenshot lama tetap arsip, tidak ditimpa atau dinyatakan hilang.

Asosiasi label-input, nama aksesibel, password masked/visible, pengalihan `labelFor`, tombol toggle, state fokus dan traversalnya tercakup tes sintetis. Kontras token utama terhadap putih dihitung: teks utama 12,39:1, teks redup 5,73:1, teks putih pada primary 5,47:1, dan danger 6,57:1. Ini sampel pasangan token, bukan audit semua piksel atau sertifikasi pembaca layar. **Keyboard fisik/OS, dialog lewat perangkat input nyata, pembaca layar, dan DPI Windows nyata: NOT VERIFIED.**

## Audit paket Windows dan shortcut

App-image tetap di `releases/TutupBuku-0.5.4-rc1-20261115-072902/TutupBuku/`, ZIP di parent dengan SHA-256 `CD4A5566CFFBA3A8605CAA5955CDA1C3419C933CB5B31D20F112684473A670B5`. JAR app-image dan `target/tutup-buku-0.5.4-rc1.jar` sama-sama `6F43155BA180DE5FB04519ACA5B0E91D294E20A3C24FD0ECD81A50E977F4C6AA`; source sejak build tidak berubah. `TutupBuku.cfg` mengarah ke `id.ac.uns.tutupbuku.ui.Launcher` dengan 14 JAR. Kedua CSS dan kelas Launcher/Application ditemukan dalam JAR, byte kedua CSS cocok dengan source. Runtime `java.exe`/`javaw.exe`, launcher dan 229 entri manifest tersedia. Verifikasi seluruh manifest/ekstraksi sebelumnya berada di `tmp/phase6-20261115/`; ZIP dan JAR yang diperiksa kini identik dengan bukti tersebut. Paket lama tidak diganti.

Riwayat Fase 6 mencatat proses EXE/jendela Windows terlihat dengan data uji terisolasi, tetapi alat GUI gagal mengikat proses foreground. Tidak ada sarana interaksi GUI Windows yang berhasil tersedia pada Fase 7 ini. **Login, navigasi, simpan/tutup/buka ulang, double-click dan kegagalan startup dari EXE terkemas belum teruji secara interaktif**; tes GUI dari Maven bukan penggantinya. Ini batas pengujian, bukan bukti EXE gagal. Pesan saat JVM gagal sebelum JavaFX muncul belum dibuktikan mudah dipahami.

`scripts/Create-DesktopShortcut.ps1` memeriksa checksum EXE, paket lengkap, mempertahankan shortcut konflik, dan idempoten bila target sama. Shortcut uji lama `tmp/phase6-20261115/shortcut-test/Tutup Buku.lnk` dibaca ulang: target adalah EXE terbaru, working directory folder app-image, ikon `TutupBuku.exe,0`. **`C:\Users\Lenovo\Desktop\Tutup Buku.lnk` tidak ada** pada pemeriksaan ini. Desktop tersebut berada di luar area yang dapat ditulisi pada sesi ini, jadi tidak memasang shortcut nyata dan tidak menguji double-click. Folder app-image lengkap tetap ada. Ikon khusus merek `.ico` memang belum tersedia; ikon bawaan EXE digunakan.

## Isu dan tingkat dampak

| Isu | Status / dampak | Prioritas rilis |
|---|---|---|
| H-02 exception historis ListView | OPEN; pernah ada pada log paket lama; tidak terulang pada dua log otomatis baru, tetapi langkah reproduksi belum dipastikan. Data historis tidak dilaporkan rusak. | Sedang; eskalasi menjadi penghalang bila terulang atau menghambat daftar/data. |
| Interaksi EXE dan shortcut Windows nyata | NOT VERIFIED; syarat READY FOR RELEASE belum terpenuhi. | Tinggi sebagai gerbang penerimaan, bukan defek produk yang sudah terbukti. |
| Keyboard OS/fisik dan screen reader | NOT VERIFIED; tes sintetis tidak mewakili teknologi bantu nyata. | Sedang; uji sebelum rilis tanpa syarat. |
| DPI Windows nyata, perangkat kedua/offline fisik | NOT VERIFIED; responsivitas hanya diuji pada lingkungan/emulasi. | Sedang; uji lapangan sebelum rilis tanpa syarat. |
| Variasi screenshot historis | Arsip dipertahankan; 32 gambar terdaftar kini cocok. | Rendah; bukan kegagalan baru. |
| Pesan startup sebelum JavaFX / SmartScreen | Perilaku native EXE dan kebijakan mesin tujuan belum diuji. | Sedang untuk dukungan distribusi. |

## Prosedur akhir pada Desktop Windows yang dapat diakses

1. Ekstrak ZIP lengkap ke folder tetap; periksa `TutupBuku.exe`, `app/`, `runtime/`, dan `SHA256SUMS.txt`. Jalankan EXE dengan **fixture akun/data uji terisolasi**, bukan database asli; catat jendela dan pesan error startup bila ada.
2. Secara nyata lakukan masuk/daftar, toggle password, Tab/Shift+Tab dan Enter, buka Dashboard/Profil/Semester, buka modul materi dan simulasi nilai, simpan data uji, tutup normal lalu buka lagi dan pastikan data uji kembali. Periksa log bila H-02 terulang; jangan menghapus data bila terjadi kesalahan.
3. Uji pembaca layar, keyboard fisik dan Windows DPI 125% pada sesi Windows nyata. Bila mungkin ulangi di komputer kedua tanpa jaringan dan jalankan kit penerimaan manual historis.
4. Dari folder app-image lengkap jalankan `Create-DesktopShortcut.ps1` melalui **Run with PowerShell** jika kebijakan perangkat mengizinkan. Bila `Tutup Buku.lnk` sudah ada dengan target berbeda, script menolak perubahan; periksa/simpan konfigurasi lama sebelum tindakan berikutnya. Periksa target, working directory dan ikon shortcut hasil, lalu lakukan double-click serta alur buka/tutup ulang. Jangan mengubah execution policy global.
5. Catat hasil, OS/versi/DPI, langkah reproduksi dan bukti. Jika ada crash, nilai salah, kehilangan data atau akses akun keliru, tahan rilis dan investigasi sebelum distribusi.

**Kesimpulan:** gerbang otomatis lulus dan paket konsisten dengan source, sehingga CONDITIONAL READY. Status READY FOR RELEASE menunggu verifikasi interaktif EXE, shortcut sebenarnya dan pemeriksaan lapangan yang relevan; jangan publikasikan otomatis.

## Pemeriksaan sebelum checkpoint Git - 16 November2026

Atas permintaan pengguna terbaru, pemeriksaan179tes default dan15kasus GUI/benchmark diulang dan lulus (0gagal/error/skip), log `tmp/github-check-20261116-{unit,gui}.log`. Pemeriksaan staging/commit outgoing tidak menemukan path terlarang atau pola rahasia umum; config lokal dan draf tetap dikecualikan. Tidak melakukan pengujian EXE/shortcut baru, sehingga keputusan siap bersyarat dan semua gerbang interaktif di atas tetap.

Rebuild untuk verifikasi menghasilkan JAR `E5FE50A971C46A98C1C1374F3CF1A9D5868605469FD3B152F07EFC3C0A1E6053`, berbeda byte dari JAR paket `6F43155BA180DE5FB04519ACA5B0E91D294E20A3C24FD0ECD81A50E977F4C6AA`. Bagian audit paket di atas adalah hasil sebelum rebuild; tidak mengklaim kesamaan byte terbaru atau mengganti paket. Perbandingan JAR lokal dicatat pada `tmp/github-check-20261116-payload.json`. Commit/push kini diizinkan pengguna dan mengatasi larangan Git historis fase sebelumnya; tidak mengizinkan publikasi Release atau perluasan fitur.
