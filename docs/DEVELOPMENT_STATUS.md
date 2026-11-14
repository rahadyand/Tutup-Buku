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
