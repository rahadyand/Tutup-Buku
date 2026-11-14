# Audit UI dan Rencana Modernisasi Tutup Buku

14 November 2026 (WIB) · baseline Git `220d1f0de270f11c1e5eb7b662bea789445021cd` · aplikasi `0.5.4-rc1`.

**Keputusan:** pertahankan JavaFX programatik, helper yang ada, navigasi Revision 002, dan alur akademik dua halaman. Mulai dari pemusatan CSS yang mempertahankan tampilan saat ini. Tree navigation belum direkomendasikan untuk implementasi. Dokumen ini adalah audit dan rencana, bukan izin refactoring.

## 1. Sumber, batas audit, dan konflik persyaratan

- Dibaca: seluruh 25 halaman `PRD_Tutup_Buku_v1.1.pdf`, `AGENTS.md`, `README.md`, seluruh Markdown proyek dalam `docs/` termasuk subdirektori `academic/`, konfigurasi Maven/Wrapper, seluruh 17 class UI, dan jalur service/model serta fixture tes yang relevan. Dokumen historis/draf dipakai sebagai konteks, bukan hasil pengujian baru atau perintah untuk melanjutkan pekerjaan lama.
- Inventaris arsip workspace: **13 ZIP**, terdiri dari enam arsip alat/sumber alat dan tujuh paket distribusi. Daftar entry diperiksa tanpa menjalankan script: tidak ditemukan `SKILL.md`, `AGENTS.md`, PRD, Design System, atau Migration Plan UI di dalamnya. **ZIP refactoring yang dimaksud belum tersedia.** `docs/IMPLEMENTATION_PLAN.md` adalah rencana milestone produk lama, bukan Migration Plan Design System.
- **Design System formal beserta token, spesifikasi komponen, dan Migration Plan UI belum tersedia.** Tidak ada klaim persentase kesesuaian terhadap spesifikasi tersebut. Nilai visual yang dicatat di bawah berasal dari kode; aturan berikutnya adalah usulan yang memerlukan persetujuan.
- Ponytail **FULL** dibaca dari `C:/Users/Lenovo/.agents/skills/ponytail/SKILL.md`; instruksi audit dari `ponytail-audit/SKILL.md` dan review dari `ponytail-review/SKILL.md` juga dibaca. Tujuh skill lokal teridentifikasi di direktori tersebut: `find-skills`, `ponytail`, `ponytail-audit`, `ponytail-debt`, `ponytail-gain`, `ponytail-help`, `ponytail-review`. Empat lainnya tidak diperlukan. Tidak ada skill audit UI khusus dari ZIP yang dapat diperiksa. Tidak memasang dependency atau menjalankan script referensi.
- Source Maven tersedia lengkap untuk inventaris UI. `src/App.java`, yang disebut konvensi sebagai starter lama, **tidak ditemukan**; `docs/README-starter.md` dan salinan konfigurasi awal tersedia. Audit tidak menghapus atau membuat ulang starter.
- Prioritas: klarifikasi chat terbaru → perilaku Revision 002/001 yang disetujui → PRD. Bahasa Indonesia mengikuti klarifikasi terakhir, mengatasi permintaan laporan Inggris sebelumnya. PRD hlm. 6 tentang tahun/periode dan prodi bebas telah diganti tahun tunggal/dropdown R001/R002. Shortcut kiri R001 diganti kanan R002. Wireframe sidebar PRD hlm. 15 adalah rekomendasi, bukan izin membatalkan navigasi terbaru.
- P0 tetap harus dipertahankan; F08G/F14/F15 adalah P1 bersyarat, P2 tetap backlog. Audit ini tidak membuka fitur baru atau mengubah penerimaan lapangan yang masih pending. Aturan "perbarui DEVELOPMENT_STATUS" akan diterapkan saat implementasi; tahap ini hanya menambahkan satu dokumen sesuai batas terbaru pengguna.

**Bukti visual:** sembilan screenshot yang sudah ada dibuka: enam di `target/verification/ponytail-full/` (`r002-login-native-1366.png`, `r002-register-125%-1366.png`, `r002-profile-native-1366.png`, `r002-semester-125%-1366.png`, `r002-courses-native-1366.png`, `r002-header-125%-1366.png`), serta `target/verification/m5-learning-125%.png`, `m5-grades-125%.png`, `m4-comparison.png`. Ini bukti historis yang ditinjau sekarang, **bukan screenshot dari run baru**. Screenshot M4/M5 tidak membuktikan posisi header terbaru. Nama 125% merujuk emulasi proses sebagaimana dicatat dokumen asal, bukan DPI Windows nyata.

**Pemeriksaan aktual sesi ini:** pembacaan source/dokumen, metadata PDF, daftar entry ZIP, pencarian style/layout/aksesibilitas, dan tinjauan gambar. Tidak menjalankan aplikasi, Maven, tes GUI, benchmark, atau suite regresi. Hasil tes lama di dokumentasi tidak dilabeli sebagai hasil audit ini. Tidak mengakses database pengguna.

## 2. Ringkasan arsitektur proyek

| Bagian | Temuan dan bukti aktual |
|---|---|
| Versi | `pom.xml`: Java release **21**, JavaFX **21.0.10**, aplikasi **0.5.4-rc1**; `javafx-controls` dan `javafx-web`. Versi build bukan hasil pemeriksaan runtime baru. |
| Build | Maven **3.9.11**, Wrapper **3.3.4**, checksum terkunci di `.mvn/wrapper/maven-wrapper.properties`. `mvnw.cmd` tersedia. |
| Dependency | SQLite JDBC **3.51.0.0**, CommonMark/GFM tables **0.24.0**, JUnit **5.12.2** untuk tes. Tidak ada framework UI tambahan. |
| Distribusi | `scripts/Prepare-PackagingTools.ps1` mengunci Temurin **21.0.13+9** + checksum. `Build-Windows.ps1` memakai `Launcher`, jlink/jpackage dan direktori rilis baru. Script hanya dibaca. |
| Source | **75** file Java produksi, **17** di package `ui`. `src/main` hanya berisi direktori `java`; tidak ada `resources`, FXML, stylesheet aplikasi, font, atau ikon UI di source Maven. |
| Tampilan/controller | Class view sekaligus menyusun node dan menangani event. Tidak ada controller FXML terpisah; pendekatan programatik sah dan dipertahankan. |
| Batas lapisan | `domain` model/invariant; `service` autentikasi/akademik/belajar/nilai; `persistence` repository SQLite/aset; `ui` presentasi dan interaksi. Tidak perlu reposisi package. |

Dalam tabel berikut, **U/** berarti `src/main/java/id/ac/uns/tutupbuku/ui/`, **S/** berarti `src/main/java/id/ac/uns/tutupbuku/service/`, dan **T/** berarti `src/test/java/id/ac/uns/tutupbuku/service/`. Nomor baris merujuk baseline audit.

| Alur UI → koordinasi → model/penyimpanan | Batas yang harus dijaga |
|---|---|
| `U/TutupBukuApplication.java` → `UiTasks` → pembuatan service/repository | Root, startup/error/retry, close guard dan lifecycle. `Launcher.java` adalah entry point distribusi. |
| `AuthView` / profil `WorkspaceView` → `AuthService` → `UserProfile`, `UserSession`, `AccountRepository`/`SqliteRepository`, `PasswordHasher` | NIM teks, privasi sebelum login, toggle password, profil legacy, logout sesi. |
| `AcademicView` → `AcademicService` → `Semester`, `Course`, `AcademicRepository`/`SqliteRepository` | `saveSemesterForm` menyimpan/aktivasi atomik; `saveCourseMetadata` menjaga kehadiran. Pilihan tampilan berbeda dari semester aktif. |
| `LearningView` / `MaterialEditor` → `LearningService` → `MaterialReading`, `EditorDraft`, `LearningLocation`, `SqliteLearningRepository`, `AssetStore` | Dua lokasi konten, autosave, save materi+note, completion, CAS dan resume tidak boleh berubah akibat layout. |
| `MaterialEditor` → `LearningService.render` → `MarkdownRenderer` → `MarkdownReader` | HTML/CSS pembaca terpisah dari JavaFX CSS. Escape HTML, script disabled, aset pemilik dan link atas klik tetap. |
| `GradeView` / `ActualGradePane` / `ScenarioPane` → `GradePlanningService` → `ActualCourse`, `ScenarioCourse`, `SemesterScenario`, `SqliteGradeRepository` | Aktual dan snapshot terpisah; review/refresh draf/CAS tetap. `GradeCalculator`, `TargetPlanner`, `SemesterGpaService` tidak disentuh. |

`UiTasks` menggunakan satu worker dan menonaktifkan root selama operasi blocking. `WorkspaceView.requestLeave` menggabungkan guard belajar → nilai → akademik → profil. `DashboardView` dan selector lokal memanggil guard sebelum refresh/perpindahan; jangan mengganti node/view dengan instance baru saat mempercantik navigasi.

## 3. Inventaris layar dan komponen

| Layar / tugas | Implementasi dan komponen |
|---|---|
| Startup / penyimpanan gagal | `TutupBukuApplication.start/initializeStorage`: pesan lokasi, status dan tombol coba lagi; tanpa reset otomatis. |
| Masuk / Daftar | `AuthView`: `TabPane`, form terpusat/scroll, panduan NIM, status, tiga `PasswordInput`, Enter submit. |
| Workspace / Dashboard | `WorkspaceView`: identitas kiri, Keluar dan dua shortcut kanan; tab Dashboard → Semester & Mata Kuliah → Profil. `DashboardView` berisi tab Modul belajar / Simulasi nilai; runtime langsung memuat view kedua pilar, bukan landing page kartu terpisah. |
| Profil | Form di `WorkspaceView`: NIM readonly, nama, ComboBox dua prodi, pesan nilai legacy, simpan/dirty guard. |
| Semester | `AcademicView`: selector data tersimpan, nomor/tahun tunggal, Jadikan Aktif/Simpan/Hapus, petunjuk/Semester Baru, lihat course; form terpusat bergulir. |
| Daftar semester / mata kuliah | Halaman kedua `AcademicView`: ListView semester kiri, penanda [aktif], daftar/course form kanan, kembali dan tombol data baru. |
| Katalog belajar | `LearningView`: pilihan Kuliah/Peminatan, semester/course, folder bab/bidang, daftar materi, CRUD/urutan, salin contoh sekali, resume/progres. Tiga panel `SplitPane`, dua panel daftar dibungkus scroll. |
| Tulis / Pratinjau / Baca | `MaterialEditor`, `MarkdownReader`: judul/sumber, TextArea Markdown, WebView, impor gambar via FileChooser, simpan/autosave/status/revisi. Catatan memakai TitledPane; selesai manual terpisah. |
| Komponen & aktual | `GradeView`, `ActualGradePane`: semester, ListView course/komponen, mode, huruf aktual, kehadiran/acuan, bobot, earned/max, CRUD/urutan, total bobot tetap di atas. |
| Skenario, target & IP | `ScenarioPane`: daftar tersimpan, create/duplikasi/hapus, nama/target IP, course snapshot, mode/estimasi, target angka/huruf, kunci/saran, hasil/penanda historis, review/refresh dan bantuan rumus lipat. |
| Perbandingan | `ScenarioPane.compare`: TableView empat kolom, SKS kedua sisi, sumber Aktual/Asumsi, peringatan basis dan kedua IP; bagian dari halaman skenario. |
| Dialog / keadaan gagal | `FormSupport.confirm`, `GradeForms.guard/confirmMode`, dialog `MaterialEditor.guard`, salin contoh dan review basis. Batal default pada guard/hapus; kontrak ini wajib dijaga. |
| Fixture lama | Constructor default `DashboardView` + `CalculationDemoView` dipakai `JavaFxSmokeCheck`; bukan editor runtime dan bukan kode mati yang boleh dihapus. |

## 4. Delapan temuan yang dapat ditindaklanjuti

Tidak ada crash atau kehilangan data baru yang dikonfirmasi dalam audit statis ini. **K** = kekurangan terkonfirmasi dari kode; **U** = observasi usability; **A** = pilihan estetika. Risiko di bawah adalah risiko perubahan, bukan kepastian kegagalan aplikasi.

| ID / jenis | Layar, tugas, bukti | Dampak dan koreksi terkecil | Risiko / kriteria penerimaan |
|---|---|---|---|
| **01 · K: label belum diasosiasikan** | Semua form yang memakai `U/FormSupport.java:37` (`field`) hanya membuat `new Label(label)` dan input; tidak ada `setLabelFor`. `fieldCentered` memakai helper sama. **Belum diverifikasi dengan screen reader/keyboard.** | Relasi nama kontrol belum dinyatakan eksplisit untuk teknologi bantu. Tambahkan asosiasi pada helper untuk kontrol langsung; tangani label `PasswordInput` ke representasi aktif secara lokal, jangan mengubah sinkronisasi/password. | Rendah–sedang; setiap input langsung memiliki label yang benar; masked/visible tetap bernama dan dapat dicapai. Urutan Tab dan Enter tetap; verifikasi reader nyata sebelum mengklaim aksesibilitas penuh. |
| **02 · U: status nilai dapat jauh dari aksi** | Saat menyimpan komponen/asumsi gagal, `U/ActualGradePane.java:149` menempatkan `status` terakhir di form scroll; `U/ScenarioPane.java:223` juga demikian. `U/UiTasks.java:31` menulis pesan ke Label yang diteruskan. **Keadaan error ini belum diverifikasi visual pada run sekarang.** | Pengguna di bagian atas dapat perlu menggulir untuk mengetahui alasan gagal. Pindahkan Label status yang sama ke footer tetap pada dua pane nilai; tetap wrap dan berteks. Jangan mengubah hasil operasi, retry, disabled state, atau membuat sukses sebelum callback berhasil. | Sedang; pesan gagal terlihat tanpa scroll dari posisi atas/bawah, input tetap, retry/guard tetap, footer tidak menghabiskan viewport. Gunakan failure fixture M4. |
| **03 · U: aksi utama/hapus tampak setara** | `U/FormSupport.java:31` membuat semua Button tanpa kelas peran; `U/AcademicView.java:133`, `:203`. Screenshot `ponytail-full/r002-semester-125%-1366.png` dan `r002-courses-native-1366.png` memperlihatkan gaya tombol sama. | Saat menyimpan pengguna harus membaca semua label untuk membedakan aksi. Tambahkan kelas CSS lokal untuk utama/sekunder/destruktif; teks, posisi, urutan R002 dan konfirmasi tetap. Jangan menjadikan Hapus tombol default. | Rendah untuk CSS, perlu review warna; fokus/hover/pressed/disabled tetap terbaca, peran dapat dikenali lewat teks, Batal tetap aman, lebar aksi semester tetap170. |
| **04 · K/A: sumber style tersebar** | Enam pemanggilan `setStyle` di lima class: `TutupBukuApplication:40`, `FormSupport:55`, `AuthView:110`, `MaterialEditor:158`, `CalculationDemoView:26/:38`. Tidak ada stylesheet aplikasi. Nilai yang ada: Segoe UI13, heading22, brand36, judul materi17; spacing4/5/6/8/10/12/18/20/24. | Perubahan visual berulang membutuhkan edit Java; belum ada kontrak peran visual bersama. Mulai memindahkan root/heading umum ke satu CSS dengan **nilai sama**; override khusus layar dimigrasikan kemudian. Angka spacing berbeda tidak otomatis cacat. | Rendah bila bertahap; class/ID event tidak berubah, computed font/background sama, fixture standalone tetap mendapat heading22. Palet baru dan konsistensi radius adalah usulan estetika, belum aturan DS resmi. |
| **05 · U: ruang vertikal nilai terbatas** | `U/WorkspaceView.java:45`, `U/DashboardView.java:50`, `U/GradeView.java:36/:48` menumpuk header dan tiga tingkat tab. Screenshot `ponytail-full/r002-header-125%-1366.png` memperlihatkan form aktual hanya sebagian di viewport584logical px. | Penyelesaian form panjang membutuhkan banyak gulir. Ukur tinggi tiap area lalu kurangi padding/gap berlebih secara lokal; jangan menghapus tab, memindah shortcut, menyembunyikan konteks bobot atau membatalkan header R002. | Sedang; ukuran awal/minimum, judul/konteks dan semua kontrol tetap dicapai pada1366×768,1024×768 dan emulasi125%; hasil ukur menunjukkan area kerja bertambah tanpa clipping. Ini observasi, bukan bukti tombol tidak dapat dicapai. |
| **06 · U: katalog belajar bersaing dengan reader** | `U/LearningView.java:331–370`: sidebar min210/pref240, list min170/pref220, sidebar min-height540, SplitPane0,23/0,44. Screenshot historis `m5-learning-125%.png` menunjukkan dua panel scroll dan area baca pendek. **Layout current belum dijalankan.** | Pada laptop kecil pengguna menyeimbangkan daftar dan bacaan dengan divider/scroll. Uji divider dan teks panjang dahulu; sesuaikan batas minimum/pref lokal jika terbukti perlu. Gunakan SplitPane/ScrollPane yang sudah ada; tree/collapse baru belum perlu. | Sedang; Kuliah/Peminatan, CRUD/urutan, materi terpilih, progress, note dan resume tetap; reader berguna saat catatan terbuka/tertutup. Jangan mengecilkan Markdown editor mengikuti NIM. |
| **07 · U: kolom perbandingan belum adaptif eksplisit** | `U/ScenarioPane.java:615–629/:677`: prefWidth180+150+150+150 dan tinggi240; tanpa resize policy/cell wrapping khusus. Screenshot historis `m4-comparison.png` hanya membuktikan nama pendek pada ukuran besar terbaca. **Nama panjang/viewport kecil belum diverifikasi.** | Nama course/skenario panjang berpotensi elipsis atau membutuhkan scroll horizontal. Uji dahulu; bila terpotong, beri wrap/tooltip pada header/cell relevan dan pembagian lebar lokal, tetap izinkan horizontal scroll bila perlu. Tidak mengganti tabel dengan kartu/grafik. | Rendah–sedang; semua empat kolom dan nilai lengkap dapat dibaca dengan keyboard/scroll, sumber Aktual/Asumsi serta peringatan historis tidak hilang; data/perbandingan tidak berubah. |
| **08 · A: pembaca dan aplikasi memiliki aturan visual terpisah** | `S/MarkdownRenderer.java:45–47` menyimpan HTML CSS (Segoe UI, line-height1,6, margin20, warna `#20383d`, border `#bbc9c8`, kode `#edf1f0`, link `#086e67`). `U/MaterialEditor.java:226` empty reader memakai HTML minimal. Bukti historis reader: `m5-learning-125%.png`. | CSS scene tidak otomatis menyamakan teks/tabel/kode/empty state WebView. Dokumentasikan aturan reader terpisah dahulu; harmonisasi nanti hanya melalui presentasi `MarkdownReader`/resource HTML CSS dengan persetujuan, tanpa edit parsing/service. | Sedang; security/CSP, gambar lokal, scroll restoration dan link atas klik tetap. Jika solusi ternyata perlu menyentuh `MarkdownRenderer` di package service, hentikan subtask dan minta keputusan lingkup; tidak masuk fase pertama. |

## 5. Analisis kesenjangan Design System

Perbandingan ini terhadap **tujuan desain dalam chat**, bukan spesifikasi DS yang belum tersedia.

| Aspek | Kondisi aktual → aturan yang diusulkan |
|---|---|
| Warna / semantik | Root `#f5f7f6`, kontrol mengikuti skin JavaFX, reader memiliki warna sendiri. Awali inventaris token berbasis nilai ini; pilih aksen/teks/border/fokus/sukses/error setelah referensi atau proposal disetujui. Tidak menyimpulkan rasio kontras lulus dari screenshot. |
| Tipografi | Hierarki13/17/22/36 sudah ada; pertahankan perannya dahulu. Segoe UI untuk antarmuka, monospace untuk kode. Ukuran judul auth36 tidak diseragamkan dengan heading22. |
| Spacing / radius | Nilai lokal beragam; radius/border kontrol belum diatur sebagai kontrak proyek. Bagikan gap label/field, padding section dan radius kontrol; pertahankan kebutuhan kepadatan layar dan hindari override semua VBox/HBox. |
| Ukuran kontrol | Pertahankan R002: NIM200/password260/nama300, profil prodi240, semester90/tahun110/aksi170. Course-name max480 dan editor fleksibel tetap berbeda. Tinggi/padding kontrol bisa dibagikan setelah diuji125%; tidak memberi semua input satu fixed width. |
| Tombol / input | Gunakan Button/TextField/ComboBox/CheckBox asli, kelas peran CSS dan label yang terasosiasi. Tidak perlu button framework, validation framework atau icon pack. |
| Kartu / tabel | Belum ada komponen card produksi; form/panel sudah memadai. Perbandingan tetap TableView; gunakan section heading/permukaan sederhana bila membantu, tanpa membuat kartu untuk setiap kontrol. |
| Ikon | Kontrol aksi memakai teks, tanpa aset ikon khusus. Ini bukan cacat. Ikon dekoratif opsional setelah ada kebutuhan; jangan mengganti teks bermakna dengan simbol atau emoji ambigu. |
| Sidebar / tree / tab | ListView/ComboBox/SplitPane dan TabPane native tersedia. Bedakan gaya tab global/lokal lewat selector terbatas; perbedaan warna terpilih/fokus tetap jelas. Tidak mengubah tujuan navigasi. |
| Dialog / notifikasi | Alert dan inline Label sudah digunakan. Stylesheet Scene tidak diasumsikan otomatis berlaku pada semua DialogPane; pasang style dialog secara eksplisit bila fase terkait disetujui. Pertahankan modalitas, owner, tombol/data dan Batal default. Tidak perlu toast manager. |
| Empty / loading / error | Placeholder daftar, reader kosong, pesan IP belum lengkap, status Memproses/Menyimpan/Gagal sudah ada. Samakan presentasi, bukan mengubah validasi atau mengganti unknown menjadi0. Status harus terbaca walau warna tidak terlihat. |
| Disabled / focus / selected | Saat ini terutama skin JavaFX; root disabled selama operasi, target/lock tertentu disabled, daftar punya selection. Pertahankan visual native dahulu. Verifikasi :focused/:disabled/:selected serta hover/pressed sebelum CSS baru diterima; screenshot Keluar berbingkai fokus bukan uji seluruh keyboard. |
| Responsif / aksesibilitas | Sudah memakai wrap, grow, scroll, SplitPane dan visual bounds; bukan layout absolut seluruhnya. Kekurangan utama adalah bukti keyboard/screen reader dan keadaan viewport kecil. Fixture umumnya memanggil `.fire()`/set selection, bukan perjalanan Tab pengguna nyata. |

## 6. Risiko dan evaluasi tree navigation

| Area sensitif | Pengendalian pada refactoring UI |
|---|---|
| Navigasi/dirty guard | Pertahankan instance view, urutan callback dan `requestLeave`; Batal/gagal save tidak boleh refresh atau berpindah. ID node dipakai fixture tes, jangan diganti demi penamaan CSS. |
| Akademik R001/R002 | Simpan ≠ aktivasi; ID hasil commit ditangkap sebelum refresh. Metadata course tidak mengubah attendance. Tahun tunggal dan arsip tetap. CSS tidak mengubah handler atau binding. |
| Aktual/skenario | Jangan memindah rumus, membulatkan sebelum keputusan, menulis solver ke aktual, merebase otomatis atau membuang inactive data. Hasil dan format sensitif di GradeForms dipertahankan. |
| Materi/aset | Jangan mengganti EditorDraft, timing autosave/antrean, CAS, completion, bookmark/revision/ratio, atau kebijakan aset. WebView tetap script disabled/tanpa bridge. |
| CSS bersama | Font/padding/radius memengaruhi minimum size, wrap dan tinggi form. Selector global berisiko mengubah reader/editor/nilai; migrasi kecil dengan screenshot/ukur sesudah CSS+layout. |
| Batas historis | `docs/KNOWN_ISSUES.md` H-02 mencatat exception internal ListView yang belum dipastikan pemicunya. Audit tidak mereproduksi atau menandainya selesai; perubahan selection/navigation berisiko memperumit diagnosis. |
| Data / cakupan | Schema4 dan RULE_VERSION1, SQL, hashing, dependency, packaging tetap di luar perubahan. Tidak ada audit keamanan formal atau seluruh kemungkinan kegagalan disk/multi-instance dalam laporan ini. |

| Opsi navigasi | Manfaat | Biaya / keputusan |
|---|---|---|
| Pertahankan tab + selector + daftar | Sesuai R002, guard dan fixture yang ada; semester aktif/dipilih jelas, dua jalur belajar tetap terpisah. | Ada kepadatan/tab bertingkat; optimalkan presentasi lokal terlebih dahulu. **Direkomendasikan sekarang.** |
| TreeView hanya dalam katalog belajar | Hierarki bab/materi terlihat bersamaan; cabang native dapat dilipat dan berpotensi mengurangi satu panel. | Harus memetakan owner/jenis/ID, load data, selected node, operasi folder/materi, reorder, guard, resume dan refresh. Tidak perlu schema baru, tetapi berisiko mengubah perilaku selection. Hanya prototipe setelah ada bukti usability. |
| TreeView mengganti workspace/akademik | Tujuan lintas area dapat terlihat dalam satu panel. | Bertentangan dengan navigasi dan alur semester/course yang disetujui bila diterapkan sekarang; memperluas guard/konteks dan tes. **Tidak direkomendasikan.** |

## 7. Komponen yang dipakai kembali

| Komponen yang sudah ada | Pengembangan minimum yang dibenarkan |
|---|---|
| `FormSupport` | Tetap helper form/heading/note/scroll/confirm; tambahkan style class pada factory yang berulang dan asosiasi label saat fase aksesibilitas. Tidak membuat BaseView, FormBuilder atau registry token Java. |
| `PasswordInput` | Pertahankan komponen, lebar lokal, submit dan reset; tambahkan dukungan label/fokus representasi aktif hanya pada fase terkait. |
| `GradeForms` | Tetap formatting dan guard nilai/profil/akademik. Jangan menjadikannya renderer generik atau menggabungkan guard materi yang memiliki autosave/bookmark khusus. |
| `MaterialEditor` / `MarkdownReader` | Tetap satu pasangan editor/reader, catatan TitledPane dan kontrol native; presentasi konten punya CSS HTML sendiri. |
| ListView / TableView / SplitPane / TabPane / Alert | Pakai kelas style/selector lokal. Status tetap Label yang sudah ada. Tidak membuat custom card/table/sidebar/notification framework; belum ada kebutuhan yang membenarkannya. |

Aturan bersama: peran tipografi, permukaan/teks/border/fokus, tombol, field-label, gap dasar dan status. Aturan lokal: lebar identitas, nama course, target numerik, editor, divider, urutan aksi, tinggi area baca dan kepadatan tabel.

## 8. Roadmap bertahap, verifikasi, dan rollback

Setiap fase berhenti untuk review; tidak otomatis memberi izin fase berikutnya. Seluruh fase di bawah adalah presentasi. Perubahan event/guard/service bukan bagian dari modernisasi visual.

| Urutan / prioritas | Tugas dan file yang direncanakan | Gerbang penerimaan / rollback |
|---|---|---|
| **1 · risiko rendah** | CSS dasar root + heading umum dengan nilai lama; rincian batas di bagian9. | Computed style/ukuran/alur tidak berubah, resource terbawa Maven, fixture standalone benar. Batalkan hanya diff fase ini bila gagal. |
| **2 · dampak tinggi, risiko rendah–sedang** | Relasi label serta peran tombol/fokus; `U/FormSupport.java`, `PasswordInput.java`, `AuthView.java`, `WorkspaceView.java`, `AcademicView.java`, CSS dan fixture terkait. Putuskan aturan warna terlebih dahulu. | Ukuran/posisi R002 tetap, keyboard masuk/toggle/save/Batal benar, fokus/disabled/selected terbaca. Rollback patch label atau CSS per subtask, jangan reset working tree. |
| **3 · dampak tinggi, risiko sedang** | Status terlihat dan kepadatan nilai; `U/ActualGradePane.java`, `ScenarioPane.java`, `GradeView.java`, selector CSS lokal. Koreksi tabel hanya jika kasus nama panjang membuktikan masalah. | Error/retry/guard dan hasil angka sama, footer terlihat, seluruh kontrol dicapai saat resize/125%. Rollback per pane; tidak ada migrasi data. |
| **4 · dampak sedang, risiko sedang** | Batas panel belajar dan reader setelah pengukuran; `U/LearningView.java`, `MaterialEditor.java`, `MarkdownReader.java`, resource CSS terkait. Tidak termasuk mengganti navigasi. | Catatan terbuka/tertutup, edit/preview, gambar, resume dan scroll tetap. HTML CSS/security dipisahkan dari Scene CSS. Rollback presentasi tanpa menyentuh DB/aset. |
| **5 · opsional, perlu keputusan baru** | Evaluasi prototipe tree terbatas `U/LearningView.java` hanya bila tugas peserta menunjukkan masalah hierarki yang tidak teratasi fase4. | Bandingkan penyelesaian tugas dengan versi daftar; seluruh selection/guard/resume terjaga. Jika manfaat tidak jelas, pertahankan daftar tanpa menambah abstraksi. |

Sesudah setiap perubahan bermakna, perbarui `docs/DEVELOPMENT_STATUS.md` dengan hasil aktual, bukan rencana tes. Verifikasi implementasi memakai `.\mvnw.cmd verify`; GUI opt-in dijalankan sesuai area dengan `-DreuseForks=false`. CSS bersama memerlukan tinjauan lintas layar, bukan hanya login. Gunakan JDK pinned/cache lokal bila tersedia; jangan mengubah instalasi atau DPI Windows global. Seluruh DB/aset tes memakai @TempDir atau folder demo terisolasi.

Simpan baseline screenshot per fase sebelum diubah, termasuk nama panjang, kosong, error, fokus, disabled dan selected. Target1366×768,1024×768 serta emulasi proses125%; minimum760×480 diperiksa sebagai batas operasional, bukan janji kenyamanan penuh. Bandingkan computed font/ukuran sesudah CSS+layout dan uji keyboard nyata Tab/Shift+Tab/Space/Enter/Escape. Jangan mengubah expected result kalkulasi atau mengendurkan validasi untuk meluluskan UI. Jika gagal, perbaiki/rollback patch fase; pertahankan semua draf pengguna dan artefak lama. Tidak perlu reset DB, repackage, commit atau push untuk audit ini.

## 9. Fase implementasi pertama yang diminta untuk disetujui

**Lingkup tunggal: pindahkan style root dan heading umum ke stylesheet JavaFX, dengan tampilan dan perilaku tetap.** Ini fondasi terkecil sebelum mengganti palet atau komponen. Tidak termasuk tombol/perubahan spacing, tree, reorganisasi view, reader HTML atau logika.

File yang boleh disentuh pada fase1:

1. Baru: `src/main/resources/id/ac/uns/tutupbuku/ui/tutup-buku.css` — kelas root aplikasi dengan background `#f5f7f6`, Segoe UI13, serta kelas heading22 bold; nilai berasal dari source, bukan DS yang belum ada.
2. `src/main/java/id/ac/uns/tutupbuku/ui/TutupBukuApplication.java` — muat CSS dari classpath dan ganti style root inline dengan kelas; lifecycle/event/ukuran window tetap.
3. `src/main/java/id/ac/uns/tutupbuku/ui/FormSupport.java` — `heading` memakai kelas CSS; factory lain dan lebar lokal tetap. Override auth36/judul materi17 tetap berjalan.
4. `src/test/java/id/ac/uns/tutupbuku/service/JavaFxSmokeCheck.java` — Scene fixture standalone juga memuat stylesheet, agar heading fixture tidak kehilangan ukuran22. Tidak mengubah ekspektasi hitung atau alur.
5. `docs/DEVELOPMENT_STATUS.md` — catat diff, hasil verifikasi dan batas aktual fase1.

Prosedur setelah persetujuan: ambil baseline/ukur style pada data fiktif; buat patch kecil; jalankan `.\mvnw.cmd verify`; jalankan GUI opt-in `JavaFxSmokeCheck,RevisionTwoFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck` dengan fork terpisah; ulangi `RevisionTwoFxCheck` pada `-DargLine=-Dglass.win.uiScale=125%` dan `-Drevision.width=1024`. Bila memakai lingkungan proyek, tambahkan `-Dmaven.repo.local=.m2/repository -o` dengan JAVA_HOME ke `.tools/jdk-21.0.13+9` dan MAVEN_USER_HOME lokal. Tinjau screenshot dan log CSS, pastikan resource tersedia di output/JAR Maven serta root background/font dan heading identik. **Semua langkah ini masih rencana, belum dijalankan pada audit.**

Terima fase1 hanya bila tidak ada warning CSS baru, R002/guard/persistensi dan hasil fixture tetap, lebar lokal tidak berubah, heading fixture benar, serta tidak ada file di domain/service/persistence/pom/schema/script packaging berubah. Jika gagal, kembalikan hanya patch fase1 dan file CSS baru yang dibuat fase itu; jangan menggunakan reset/clean atau menghapus file pengguna.

Informasi terbuka: ZIP/Design System/Migration Plan UI perlu dilampirkan bila hendak menjadi acuan fase visual berikutnya. Palet/radius/ikon belum diputuskan. Bukti keyboard/screen reader, DPI Windows nyata, perangkat kedua dan usability mahasiswa tetap belum tersedia dari audit ini; bukan alasan menebak hasil atau mengubah navigasi.

**Persetujuan yang diminta: apakah fase1 dengan lima file dan batas di atas boleh dimulai? Berhenti di sini sampai persetujuan diberikan.**
