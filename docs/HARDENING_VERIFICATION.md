# Hasil hardening 0.5.1-rc1

Tanggal9November2026, Windows11amd64, Temurin21.0.13+9-LTS portable,12CPU logis. Baseline M5 `6172645b874575efc113678938f376fcaa82d1ce` diperiksa sama dengan origin/main sebelum edit. Bukti di bawah adalah run baru, bukan menyalin hasil M5.

## Hasil otomatis aktual

PowerShell pada root proyek; cache lokal tersedia:

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME="$PWD\.tools\maven-user"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp -Pwindows-package verify
```

**BUILD SUCCESS,163tes,0failures/errors/skipped**, selesai22:33:08WIB. Inventaris: AcademicPersistence14; AssetsAndMarkdown8; DatabaseSafety5; GradeCalculator43; GradeMigration2; GradePlanningPersistence18; IndependentCalculation23; LearningMigration2; LearningPersistence13; LocalAccount8; SemesterGpa10; StackCompatibility2; TargetPlanner15.

```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ReleaseHardeningFxCheck,CalculationPerformanceCheck' '-DreuseForks=false' verify
```

**BUILD SUCCESS,9tes=8GUI+1benchmark,0failures/errors/skipped**, selesai22:34:10WIB. Fork terpisah karena toolkit ditutup per class. M1shell, M2register/academic/error, reader aman, M3editor/autosave/failure/guard/close/resume, M4target/snapshot/comparison/failure, M5perjalanan terhubungmulti-course dan H-01 lintaslayar diperiksa melalui kontrol aplikasi dengan SQLite sementara.

```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=MilestoneFiveFxCheck' '-DargLine=-Dglass.win.uiScale=125%' verify
```

**BUILD SUCCESS,1tes,0failures/errors/skipped**, selesai22:36:41WIB. Ukuran jendela sasaran fisik1366×768, outputScale1,25, konten screenshot1080×584 logical. Desktop fisik1920×1080; pengaturan Windows tidak diubah. Gambar m5-learning-125%.png, m5-grades-native.png dan hardening-academic.png benar-benar dibuka/ditinjau. Reader/daftar/form panjang dapat digulir, note dilipat; tombol konteks/simpan/status terlihat. Tidak mengklaim aksesibilitas/usability penuh dari screenshot.

Benchmark30course×20komponen: cold21,610ms;3warmup;20sampel min2,862ms, median3,486ms (rata-rata dua tengah),p955,198ms,max5,418ms. Target<1detik pada mesin ini tercapai. Setup akun/data/password, render dan respons manusia tidak diukur; tidak menjamin perangkat lain.

Semua tes DB/aset menggunakan @TempDir, tidak pernah AppData pengguna. XML/TXT di target/surefire-reports; screenshot/performance di target/verification. Default test baru+lama dihitung163; generated40/32kasus adalah loop internal, tidak ditambahkan sebagai72tes JUnit. Warning JavaFX unnamed module/unchecked cast helper tetap ada tanpa kegagalan.

## Defek dan uji yang membuktikannya

H-01: ReleaseHardeningFxCheck awal **1failure yang disengaja untuk reproduksi**, expectedfalse actualtrue pada checkbox akademik setelah save aktualfalse. Setelah WorkspaceView.refresh akademik ditambahkan sesudah guard, run terfokus24tes (23independen+1GUI) lulus. Regresi final di atas mengulang pada versi executable yang stabil. Nama/SKS berubah sementara attendancefalse/confirmedtrue tetap di DB. Engine M1, schema3 dan dependency tidak berubah.

Tambahan integrasi: bobot/order menandai snapshot usang tanpa mengubah basis tersimpan; acksaveA mempertahankaninputB dan saveB benar-benar tersimpan, kirimanA lama ditolak. Pemetaan kebutuhan baseline tersedia pada [REQUIREMENTS](REQUIREMENTS.md).

## Kandidat Windows

Paket dibuat pada 9 November 2026; pemeriksaan restart diselesaikan pada 10 November 2026. Paket **tidak dibangun ulang** pada kelanjutan ini. Source executable/pom/script tidak berubah dari checkpoint `30d257a649d6c653e0c3daa59735af1123f9654e`; draf dokumentasi yang ada membuat BUILD-INFO mencatat working-tree changes=True. Draf tersebut bukan perubahan executable.

- ZIP: `releases/TutupBuku-0.5.1-rc1-20261109-224207/TutupBuku-0.5.1-rc1-windows-x64.zip`, **95.921.473 byte**.
- SHA256 ZIP: `BDA1BB4A92669ABA67680562638D355790E5D0EBE12447704F260A3DA36ED411`.
- Launcher distribusi: `releases/TutupBuku-0.5.1-rc1-20261109-224207/TutupBuku/TutupBuku.exe`.
- Launcher yang benar-benar diuji: `tmp/Uji Paket Ekstraksi 20261109-224245/TutupBuku/TutupBuku.exe`.
- SHA256 JAR aplikasi: `2399BE700629CA6D35F3FDB9262F14C920B98C182231B03F49AA1906F22D875D`.

Pemeriksaan awal Verify-WindowsPackage:226 file hash cocok ditambah manifest, forbidden0,17 modul runtime berjalan. Peluncuran memakai JAVA_HOME/MAVEN_HOME kosong, PATH hanya System32, proxy Java proses ke127.0.0.1:9, folder data writable terisolasi `tmp/package-data-20261109-224245`. Tidak mengubah jaringan global atau memakai DB AppData nyata. Ini bukti runtime mandiri di mesin pengembang, bukan komputer kedua/offline fisik.

Sebelum interupsi: registrasi/login fiktif00051, semester3/2026–2027/Ganjil aktif, PBO Paket051/3SKS; materi Markdown/tabel/kode, PNG120×60 melalui chooser native, note dan selesai. Nilai aktual Proyek paket berbobot100%,8/10; skenario Rencana Paket051 bertargetIP3 menampilkan IP Simulasi3,70. Tutup pertama exit0, sumber PNG dipindah, EXE yang sama dibuka kembali.

Kelanjutan: jendela run kedua masih hidup dan diminimalkan di layar login; diaktifkan kembali tanpa peluncuran/pembuatan data ulang. Login berhasil. Tab aktual memuat kembali Proyek paket100%/8/10; skenario tersimpan dibuka melalui daftar dan menampilkan nama, course3SKS, target3 serta IP Simulasi3,70/target tercapai. Tutup normal kedua menghasilkan **Run2 exit=0**.

Pemeriksaan tambahan read-only setelah aplikasi tertutup (`python tmp/verify-existing-candidate.py`): seluruh **227 file ZIP cocok byte-hash dengan ekstraksi yang diuji**, termasuk launcher/runtime/dependensi; JAR ZIP sama dengan target build terverifikasi. SQLite quick_check=ok dan foreign_key_check kosong. Akun/aktif/course/aktual/snapshot8/10/target3/note/completion sesuai fixture. Bookmark material1,rasio0,130622009569378 tetap tersimpan. Satu PNG terkelola identik dengan sumber yang telah dipindah. Hasil lokal `tmp/hardening-final-evidence.json`; fixture/hasil tidak masuk Git. Resume visual/gambar sesudah restart tidak diulang pada kelanjutan terbatas ini; yang baru dibuktikan lewat GUI adalah nilai dan skenario.

**Masalah terbuka H-02:** output run kedua memuat `java.lang.IndexOutOfBoundsException: [ fromIndex: 0, toIndex: 1, size: 0 ]` dari JavaFX ReadOnlyUnbackedObservableList/ListViewBehavior. Waktu/pemicu tepat belum dipastikan; jangan menyimpulkan terjadi karena tombol tutup. GUI tetap memuat aktual/skenario dan exit0; pemeriksaan DB tidak menemukan kehilangan data. Tidak dianggap crash kritis dari bukti ini, tetapi bukan dinyatakan bebas masalah. Reproduksi klik/pemilihan daftar perlu tindak lanjut terfokus; tidak ada perubahan executable pada kelanjutan ini. Batas pada [KNOWN_ISSUES](KNOWN_ISSUES.md).

## Penerimaan yang belum dilakukan

Komputer kedua tanpa alat, internet fisik terputus, Windows125% nyata dan usability3–5mahasiswa: **NOT PERFORMED**. Proxy/lingkungan child process dan emulasi JavaFX tidak menggantikan gerbang ini. Langkah penerimaan pada [RELEASE_ACCEPTANCE](RELEASE_ACCEPTANCE.md). Manfaat belajar belum diukur; penerimaan akhir belum selesai.
