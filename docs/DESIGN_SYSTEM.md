# Tutup Buku — Modern Academic Workspace 1.1 (Fase 5C)

15 November 2026 (WIB). Instruksi pengguna kini mengizinkan perluasan UI seluruh layar dan Fase6 launcher setelah gerbang Fase5C lulus. Spesifikasi 1.0 di bawah dipertahankan sebagai riwayat; batas “hanya pilot” dan roadmap belum diizinkan pada bagian lama tidak lagi menggambarkan lingkup terbaru.

## Kontrak adopsi terbaru

- `tb-theme` dipasang pada root aplikasi: warna, font dan fokus berlaku pada startup, autentikasi dan workspace. Dua puluh satu token warna 1.0 tidak diubah.
- `tb-workspace` membatasi varian padat: tombol padding4x10, form gap12/padding16, label-input4, tab4x12, status4x8. Auth tetap memakai desain pilot dan seluruh ukuran R002 tetap.
- `tb-workspace-header`: permukaan putih, divider bawah. Pada tinggi workspace <600 logical px, pseudo-class `compact` memberi heading17 dan padding4, menyembunyikan slogan dekoratif. Identitas, semester aktif, Keluar dan kedua shortcut tetap tersedia. Heading normal22, auth36, judul materi17.
- `tb-form-card`, `tb-surface`, `tb-editor-header`, `tb-result`, `tb-code-editor`: permukaan form/panel, pemisah editor, penekanan hasil, font editor Consolas13. Tidak membuat statistik atau progres baru.
- Native TabPane/ListView/ComboBox/TextArea/CheckBox/TitledPane/TableView mendapat normal, selected, hover/focus dan warna yang konsisten. Tombol workspace membungkus teks; selected table/list memakai teks putih pada teal. Tabel tetap dapat digulir horizontal, tooltip kolom panjang Fase3 dipertahankan.
- `FormSupport.styleDialog(Dialog<?>)` memasang CSS pada Scene dialog sendiri; dialog resizable. ButtonType, handler, default Batal, owner dan modality tetap milik pemanggil.
- `reader.css` adalah stylesheet WebView milik lapisan UI, dihubungkan melalui `WebEngine.setUserStyleSheetLocation`. Isi14px/line-height1.7, heading22/19/17, link bergaris bawah, code/table/blockquote selaras. Tidak mengubah renderer service, CSP, raw HTML, script disabled, link callback, aset atau resume.

## Navigasi, responsivitas dan batas

Dashboard tetap wadah dua pilar yang sama, tanpa rute atau angka baru. Sidebar belajar memakai cabang native TitledPane “Lokasi belajar” dan “Bab / bidang” yang dapat dilipat. Ini **navigasi hierarkis berkelompok, bukan TreeView baru**. Penggantian seluruh pilihan menjadi TreeView dikecualikan: daftar bab/materi memiliki model seleksi, worker dan dirty guard terpisah; tidak menggandakannya dengan pohon kedua. Collapse hanya memengaruhi presentasi dan mempertahankan pilihan.

Sidebar dan editor belajar serta sidebar semester dapat digulir. Editor minimum360 logical px menjaga akses header, tab, reader dan catatan; pada layar kecil pengguna menggulir panel luar dan isi reader secara terpisah. Ini kompromi minimum760x480, bukan janji seluruh isi muat bersamaan. Native file chooser, window chrome dan menu OS tetap mengikuti Windows; CalculationDemoView adalah fixture M1 yang tidak menjadi layar produk, dikecualikan dari migrasi.

Ukuran1366x768/1024x768/760x480 adalah **Stage/window**. PNG konten native umumnya1350x729/1008x729/744x441. Recovery1024x760 adalah mode Scene khusus, tetap dipertahankan tanpa crop/resampling. Pengujian125% hanya properti proses, bukan DPI Windows nyata.

Hasil aktual final dan gerbang Fase6 dicatat pada DEVELOPMENT_STATUS; screenshot/log pada `tmp/phase5c-20261115/`. H-02 tetap OPEN. Screen reader, keyboard fisik dan Windows DPI nyata tidak boleh disimpulkan lulus dari KeyEvent atau snapshot.

---

## Riwayat spesifikasi Fase 5A–5B (dipertahankan)

# Tutup Buku Design System 1.0

14 November 2026 (WIB). Spesifikasi resmi Fase5A, diimplementasikan dalam JavaFX CSS21.0.10. Arah **Modern Academic Workspace**: permukaan terang yang tenang, struktur tugas jelas, hierarki tipografi kuat dan aksen teal. Kerapian Notion, struktur workspace VS Code dan orientasi belajar Khan Academy menjadi prinsip, bukan penyalinan merek/aset/layout produk tersebut.

## Batas dan status

Fondasi CSS menyediakan kontrak warna, tipografi, jarak/radius, tombol, input, card/panel, navigasi, feedback dan fokus. **Penerapan tema bersifat opt-in melalui `.tb-theme`.** Melengkapi sistem desain tidak berarti seluruh halaman sudah dimigrasikan. Fase5B memakai satu pilot Masuk/Daftar (AuthView); halaman lain tetap tampil sebagaimana Fase1-4 sampai Fase5C diizinkan. CSS HTML/WebView materi merupakan sistem render terpisah dan tidak berubah.

Sumber: AGENTS, PRD25halaman yang telah dibaca pada audit proyek, implementasi JavaFX aktual, FormSupport/PasswordInput, R001/R002 dan inventaris UI_COMPONENTS. Instruksi chat terbaru menetapkan5A=Design System,5B=pilot,5C=perluasan yang belum diizinkan. Roadmap historis UI_AUDIT_AND_PLAN tetap utuh; fase5 tree opsional di sana bukan definisi pekerjaan5A/5B sekarang. Milestone produk M5 dan audit launcher juga berbeda; launcher/installer tidak dikerjakan pada tugas ini.

## Token warna semantik

Token didefinisikan sebagai **looked-up colors JavaFX** di `.tb-theme`, bukan variabel CSS browser. Jangan memakai `var()`, custom properties `--nama`, atau angka buatan sebagai lookup jarak.

| Token (awalan `-tb-`) | Nilai | Peran |
|---|---|---|
| canvas | #f5f7f6 | Kanvas aplikasi |
| surface | #ffffff | Card dan input |
| surface-muted | #eef3f2 | Panel, input readonly/disabled, tombol nonaktif |
| text | #20383d | Teks utama |
| text-muted | #536a70 | Bantuan/caption |
| border | #738b90 | Batas kontrol interaktif |
| divider | #d9e4e2 | Pemisah/dekorasi card, bukan indikator kontrol tunggal |
| primary | #0f766e | Aksi utama dan pilihan navigasi |
| primary-hover | #115e59 | Hover aksi utama |
| primary-pressed | #134e4a | Aksi utama ditekan |
| on-primary | #ffffff | Teks pada aksi/seleksi kuat |
| focus | #0f766e | Indikator fokus |
| info-surface | #eff8f6 | Informasi dan tab terpilih |
| success / success-surface | #166534 / #edf8ee | Hasil berhasil |
| warning / warning-surface | #854d0e / #fff8e6 | Peringatan |
| danger / danger-surface | #b42318 / #fff1ef | Kesalahan/destruktif |
| danger-hover / danger-pressed | #912018 / #781c16 | State aksi destruktif |

Gunakan peran, bukan hex baru per view. Perubahan palet berikutnya harus menguji seluruh pasangan foreground/background dan state fokus/selected/disabled yang terpengaruh.

## Tipografi

Segoe UI, ukuran logical px; font native mengikuti outputScale JavaFX. Teks kode reader/editor tetap memakai aturan yang sudah ada, tidak dimigrasikan.

| Kelas | Ukuran/berat | Pemakaian |
|---|---|---|
| tb-caption / tb-help |12/normal|Bantuan, caption; jangan untuk informasi utama yang panjang |
| tb-body |13/normal|Isi utama, input dan feedback status |
| tb-field-label |13/bold|Label form, tetap labelFor ke input |
| tb-section-title |17/bold|Judul bagian; selaras peran judul materi lama17 |
| tb-page-title / section-heading |22/bold|Heading halaman/form |
| tb-brand |36/bold|Nama aplikasi pada pilot autentikasi |

Tidak mengubah konten penting menjadi placeholder saja. Helper text/status tetap berupa Label wrap. Jangan membatasi tinggi label multi-baris hingga teks terpotong; gunakan minimum preferred bila diperlukan dan scroll untuk form panjang.

## Jarak, radius dan permukaan

Skala jarak:4,8,12,16,24,32. Implementasi memakai kelas CSS dengan literal numerik karena JavaFX tidak menyediakan variabel numerik CSS browser. Satu unit nama kelas=4logical px.

| Kelas | Kontrak |
|---|---|
| tb-gap-1/2/3/4/6/8 |HBox/VBox spacing4/8/12/16/24/32 |
| tb-pad-2/3/4/6/8 |Padding8/12/16/24/32 |
| tb-field |Gap label-input8 pada subtree tema |
| tb-form |Gap16, padding16 pada subtree tema |
| tb-radius-sm/md/lg |Radius8/12/16 |
| tb-card |Surface putih, border divider1, radius16, padding24, shadow tipis |
| tb-panel |Surface muted, divider, radius12, padding16 |

Checkbox memakai radius4 agar bentuk kontrol tetap dikenal. Jarak/radius bukan alasan menyeragamkan seluruh lebar: NIM200, password260, pembungkus password366/toggle minimum98, nama300, profil prodi240, semester90/tahun110/aksi170 tetap kontrak R002. Course/editor yang hanya memiliki max-width tidak diberi fixed width/minimum baru. Border fokus tombol2 mengurangi padding1 agar tidak mengubah ukuran saat fokus.

## Kontrol dan state

| Kelas/elemen | Normal dan interaksi | Disabled / validasi |
|---|---|---|
| tb-button |Tombol sekunder putih, border kontras, hover muted, pressed divider |Muted, teks tetap terbaca, aksi nonaktif native |
| tb-button + tb-primary |Teal, teks putih bold; hover/pressed memakai token khusus |Rule disabled mengalahkan hover/pressed |
| tb-button + tb-danger |Merah, teks putih; hover/pressed khusus |Konfirmasi/guard wajib tetap di view, bukan di CSS |
| tb-input pada TextField/PasswordField/TextArea |Surface, border, padding8x10; hover border lebih gelap; seleksi teks teal/putih |Disabled muted; readonly tetap dapat difokuskan/dibaca sesuai kontrol native |
| tb-input pada ComboBox |Padding4x6, cell teks utama, state native |Tidak mengganti pilihan, value atau popup behavior |
| tb-input + tb-invalid |Border merah; fokus merah |Harus disertai pesan bermakna. CSS tidak memvalidasi data |
| tb-check |Kotak border, selected teal/mark putih, fokus border2 |Opacity0,65 untuk disabled; perilaku native tetap |
| tb-status |Latar informasi, radius8, padding8x12, teks13 wrap |Tambahkan tb-success/tb-warning/tb-error untuk peran eksplisit |
| tb-scroll |Latar transparan; batas fokus bila ScrollPane fokus |Tetap scrollbar native; jangan menyembunyikan akses konten |

Kelas state feedback/invalid adalah API presentasi. Jangan menyimpulkan jenis error dengan parsing teks service atau mengubah rule bisnis. Pilot mempertahankan Label status dan pesan service yang sama; panel status netral memberi konteks tanpa menganggap semua operasi berhasil/gagal. State semantik lain tersedia dan diuji dalam galeri untuk pemakaian eksplisit berikutnya.

## Navigasi

- `tb-tabs` pada TabPane: header transparan, tab muted, tab terpilih info-surface dengan teks primary bold; fokus memakai border2. Tidak mengubah tab, selection model, event handler, ID atau tab closing policy.
- `tb-nav-list` pada ListView: baris surface, hover info-surface, selected primary/on-primary, border fokus2. Tidak mengubah selection/keyboard behavior.
- TableView, SplitPane dan dialog kompleks tetap kontrol native. Token focus/accent diwariskan hanya pada subtree tema; migrasi presentasi khusus tabel/dialog/reader memerlukan lingkup5C tersendiri. Jangan menambahkan tb-theme ke workspace/root aplikasi untuk menghindari modernisasi massal.

## Fokus dan aksesibilitas

Input fokus memiliki ring2px di luar batas dan border focus; tombol/tab/list/check memakai batas fokus2px. Tombol juga memiliki ring luar3px berwarna focus. Label, nama kontrol, role native, toggle password, Tab/Shift+Tab/Enter/Space/Escape tetap tanggung jawab struktur JavaFX yang sudah diuji. Label password harus menunjuk field masked atau visible yang aktif, bukan HBox pembungkus. Tidak mengubah focusTraversable untuk tujuan dekorasi.

Rasio hasil perhitungan warna galeri: text/surface12,39; muted/surface5,73; primary/surface dan on-primary/primary5,47; success/success-surface6,54; warning/warning-surface6,47; danger/danger-surface5,97; focus/surface5,47. Border input final: surface3,61, canvas3,35 dan surface-muted3,22. Pasangan teks yang diuji memenuhi ambang4,5:1, indikator fokus/batas3:1. Border divider dekoratif tidak dimaksudkan sebagai satu-satunya petunjuk kontrol. Ini bukan sertifikasi seluruh aplikasi, semua piksel/state maupun screen reader; disabled bukan petunjuk satu-satunya dan pesan tidak memakai warna saja.

**NOT VERIFIED:** keyboard OS/fisik, Narrator/NVDA, Windows UI Automation eksternal, DPI Windows nyata, usability pengguna/perangkat kedua. KeyEvent/properti/snapshot tidak menggantikan pengujian tersebut. H-02 ListView tetap OPEN.

## Integrasi dan contoh

Stylesheet tetap `src/main/resources/id/ac/uns/tutupbuku/ui/tutup-buku.css`, dimuat sekali oleh bootstrap yang sudah ada. Rule legacy root dan section-heading dipertahankan. FormSupport memberi kelas tb-input/button/field-label/field/form/help/status/scroll; di luar tb-theme kelas tersebut tidak mengubah tampilan. Factory dan event tidak diganti.

```java
page.getStyleClass().add("tb-theme"); // hanya root halaman yang dimigrasikan
VBox card = new VBox();
card.getStyleClass().addAll("tb-card", "tb-gap-4");
TextField name = FormSupport.text("name", "Nama");
FormSupport.limitWidth(name, 300);
Button save = FormSupport.button("save", "Simpan");
save.getStyleClass().add("tb-primary");
card.getChildren().addAll(FormSupport.field("Nama", name), save);
```

Kelas tidak memindah rumus/SQL/security ke UI. Jangan mengganti ID node atau membuat ulang view untuk menerapkan style. Tetap gunakan worker/guard/persistensi yang sama. Dialog punya Scene sendiri dan tidak otomatis mengambil theme dari halaman; jangan mengklaim sudah dimigrasikan.

## Pilot5B dan gerbang verifikasi

Dashboard runtime merupakan wadah langsung LearningView dan GradeView, bukan landing page ringkasan. Menggantinya dengan kartu navigasi baru akan mengubah struktur dua area/guard dan memperluas pekerjaan ke modul lain. Karena prioritas Dashboard bersyarat aman, satu pilot yang dipilih adalah **AuthView (Masuk/Daftar)**. Kedua tab merupakan satu halaman autentikasi, bukan migrasi dua halaman workspace.

Intentional: card putih dengan border/shadow tipis, eyebrow akademik, warna primary/muted, heading/input label lebih jelas, form spacing16/field8, input radius8, tombol aksi utama teal, tab terpilih jelas, feedback panel dan fokus kontras. Teks fitur, login/daftar, NIM guidance, password/Enter/reset dan dimensi R002 tetap. Form panjang tetap digulir pada760x480/125%.

| Unsur pilot | Sebelum5B | Sesudah5B |
|---|---|---|
| Permukaan |Panel tanpa card khusus |Card putih, radius16, border/shadow tipis di kanvas yang sama |
| Lebar maksimum panel / padding halaman |580 /12 |640 /24; lebar input lokal tetap |
| Gap form / label-input / padding form |12 /4 /20 |16 /8 /16 |
| Tipografi |Brand36 inline; label normal13 |Brand36 lewat CSS; label bold13; bantuan12; deskripsi/status13 |
| Aksi dan tab |Skin native yang setara |Aksi utama teal, selected tab jelas, focus ring; handler/urutan tetap |
| Feedback |Label wrap selalu mengambil ruang |Panel netral wrap, hanya managed/visible ketika ada teks; input tetap saat error |

Pada fixture minimum760x480, viewport form pilot163logical px; pada1366x768 native451px dan emulasi125%305,6px. Header/card sengaja memberi hierarki dengan konsekuensi form panjang perlu scroll. Semua input/aksi dan pesan gagal yang diuji dapat dicapai; bukan janji semua isi terlihat bersamaan atau seluruh data panjang sudah diuji.

`DesignSystemFxCheck` memeriksa21looked-up colors, tipografi, spacing/radius, kontrol/state, kontras dan isolasi stylesheet; galeri hanya fixture tes. Sebelum pilot, fondasi lulus pengujian khusus (`phase5a-design-system.log`). Regresi179, seluruh GUI yang tersedia, benchmark, empat ukuran form/M4, emulasi125%, screenshot dan resource JAR diperiksa setelah pilot. Hasil aktual akhir dan keterbatasan dicatat di DEVELOPMENT_STATUS; jangan menganggap daftar rencana ini sebagai hasil sebelum run selesai.

## Usulan roadmap5C - belum diimplementasikan

| Urutan usulan | Lingkup | Gerbang sebelum melanjutkan |
|---|---|---|
|1|Review pilot dan keyboard/screen reader/perangkat nyata; stabilkan state screenshot|Temuan nyata dicatat, hindari klaim aksesibilitas menyeluruh dari galeri |
|2|Profil lalu semester/course, satu halaman per checkpoint review|R002/legacy profile/save-activate/dirty guard/retry dan ukuran lokal tetap |
|3|Shell Dashboard/header/navigation yang sama|Jangan membuat route baru atau menggabungkan guard; ukur tinggi minimum sebelum menambah header/card |
|4|Aktual dan skenario secara terpisah|Footer Fase3 tetap terlihat; nilai/solver/attendance/CAS/historis tidak berubah |
|5|Belajar/editor/reader dan dialog|Ukur viewport/note/resume; CSS WebView/security tetap terpisah; H-02 tidak dianggap selesai |

Setiap langkah memerlukan instruksi baru. Launcher/shortcut adalah jalur kerja terpisah dan tidak dibuat pada tugas5A/5B ini. Tidak ada installer/staging/commit/push atau Fase5C otomatis.
