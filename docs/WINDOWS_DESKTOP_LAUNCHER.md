# Pembaruan verifikasi Fase 7 — 16 November 2026

App-image, ZIP dan JAR paket masih identik dengan bukti build Fase 6; setelah build regresi terbaru, JAR paket sama persis dengan `target` (`6F43155BA180DE5FB04519ACA5B0E91D294E20A3C24FD0ECD81A50E977F4C6AA`). Entri kelas utama dan kedua CSS tersedia. Uji ulang otomatis 179 regresi dan 15 GUI lulus. Shortcut uji sementara tetap mengarah ke paket ini; **shortcut Desktop asli belum ada dan interaksi EXE/double-click belum diverifikasi**. Status distribusi: CONDITIONALLY READY, belum READY FOR RELEASE. Rincian dan checklist manual ada di [PHASE_7_QA.md](PHASE_7_QA.md). Bagian Fase 6 di bawah tetap merupakan catatan sejarah.

---

# Windows Desktop Launcher - Fase 6 (15 November 2026)

Fase5C telah melewati gerbang otomatis pada `tmp/phase5c-20261115/phase5c-verification.json`. Fase6 memakai mekanisme jpackage app-image yang sudah ada, Java21/JavaFX21.0.10 dan entry point Launcher yang sama. Ini **bukan installer**. Catatan audit lama di bawah dipertahankan sebagai riwayat; status pending5A/5B di sana telah digantikan hasil fase terbaru.

## Membuka aplikasi

1. Ekstrak seluruh ZIP app-image ke folder yang tetap dapat diakses. Jangan mengambil EXE saja atau memindahkan app/runtime secara terpisah.
2. Buka `TutupBuku/TutupBuku.exe`. App-image menyertakan runtime; penerima tidak membutuhkan IDE, Maven atau JDK terpisah.
3. Untuk memasang shortcut, jalankan `Create-DesktopShortcut.ps1` yang berada di samping EXE melalui menu Windows **Run with PowerShell** jika diizinkan kebijakan perangkat. Setelah terpasang, buka **Tutup Buku** dari Desktop. Script tidak mengubah execution policy/security Windows.
4. Script versi repository memerlukan `-ApplicationDirectory` menunjuk folder app-image yang sudah diverifikasi. Default Desktop diambil dari Known Folder pengguna, termasuk lokasi redirect; tidak ada path komputer lain di dalam script.

Shortcut mengarah langsung ke GUI EXE, working directory folder app-image, icon dari EXE (ikon bawaan jpackage; belum ada .ico merek kustom). Script memeriksa paket/checksum EXE dan membaca ulang konfigurasi .lnk. Pemanggilan ulang dengan target yang sama tidak menulis ulang. Shortcut lama dengan target/working directory/argumen berbeda ditolak dan dipertahankan; pengguna dapat menyimpan/rename shortcut lama sebelum memasang versi baru.

`-DesktopDirectory` disediakan untuk pengujian pada folder sementara. .lnk di folder uji **bukan bukti shortcut Desktop pengguna telah dibuat**. Script selalu melaporkan DoubleClickVerified=False karena membaca konfigurasi tidak membuktikan interaksi Windows.

## Build dan verifikasi reproduktif

Dari repository Windows: `scripts/Build-Windows.ps1 -Offline` dengan JDK dan JavaFX SDK portable yang sudah disiapkan. Script menjalankan Maven verify/profile windows-package, jlink dan jpackage ke folder releases bertimestamp baru, tanpa menimpa paket lama. Output mencakup JAR aplikasi+13dependency, runtime, panduan, legal, BUILD-INFO, manifest SHA256 dan ZIP. Setup shortcut ikut app-image; perubahan script hanya menambah panduan/setup, bukan build system/dependency baru.

Jalankan `scripts/Verify-WindowsPackage.ps1 -Zip <ZIP>` untuk ekstraksi terpisah, checksum seluruh file dan uji runtime bawaan. Verifikasi EXE terpisah wajib dilakukan; Maven/manifest saja tidak membuktikan aplikasi terbuka. Gunakan TUTUP_BUKU_DATA_DIR absolut yang terisolasi untuk pengujian. Data default pengguna tetap %LOCALAPPDATA%/TutupBuku dan tidak boleh dipakai untuk fixture.

## Error dan batas

Paket tidak lengkap/checksum shortcut tidak cocok menghasilkan pesan Indonesia dan tidak mengganti shortcut lama. Aplikasi memiliki layar gagal membuka penyimpanan dan Coba lagi; data tidak direset. Bila JVM/EXE gagal sebelum JavaFX tampil, pesan native launcher dapat berbahasa sistem, bukan jaminan pesan aplikasi Indonesia. SmartScreen, kebijakan script, tanda tangan digital, perangkat kedua, offline fisik dan DPI Windows nyata tetap perlu pengujian lapangan; jangan menonaktifkan perlindungan untuk menjalankan paket.

Hasil build/run/shortcut aktual final dicatat pada bagian terbaru DEVELOPMENT_STATUS dan bukti `tmp/phase6-20261115/`. Dokumen dalam paket ini menjelaskan prosedur; bukan klaim bahwa shortcut Desktop atau double-click telah teruji pada setiap komputer.

---

## Riwayat audit sebelum implementasi Fase 5A-5B

# Windows Desktop Launcher - tambahan lingkup Fase 5

14 November 2026 (WIB). **Status: kebutuhan dicatat dan audit statis selesai; implementasi serta pengujian launcher/shortcut tertunda pada gerbang Fase 5A-5B.**

## Gerbang dan batas pekerjaan

Instruksi pengguna terbaru mensyaratkan launcher dikerjakan hanya setelah Fase5A dan Fase5B selesai diimplementasikan dan diverifikasi. Status terakhir yang tersedia adalah Fase4 komponen reusable selesai. Pencarian dokumen Markdown proyek tidak menemukan definisi, kriteria penerimaan atau bukti penyelesaian Fase5A/5B/5C. Konteks percakapan yang tersedia juga belum memuatnya; pencarian konteks tambahan tidak tersedia. Ini berarti **belum dapat diverifikasi**, bukan bukti bahwa kedua fase pernah diuji dan gagal.

Milestone produk M5 historis (kandidat Windows) berbeda dari Fase5A/5B modernisasi UI. Tidak menganggap paket EXE lama atau pengujian Fase4 sebagai kelulusan kedua gerbang tersebut. Lingkup/kriteria Fase5A dan Fase5B, atau catatan penyelesaiannya bila sudah dikerjakan, diperlukan untuk melanjutkan sesuai urutan yang diminta.

Tidak membuat launcher/script shortcut baru, tidak membuat shortcut Desktop, tidak menjalankan paket lama, tidak mengemas ulang, tidak membuat installer, tidak memulai Fase5C. NO STAGING/COMMIT/PUSH tetap berlaku. Perubahan kode/draf fase sebelumnya dipertahankan.

## Audit mekanisme yang tersedia

| Aspek | Temuan yang benar-benar diperiksa |
|---|---|
| Entry point GUI | `src/main/java/id/ac/uns/tutupbuku/ui/TutupBukuApplication.java` adalah JavaFX Application; `Launcher.java` menyediakan main biasa yang memanggil Application.launch. |
| Java/JavaFX | pom mengunci Java21 dan JavaFX21.0.10; tools packaging mengunci Temurin21.0.13+9 dengan checksum. Tidak mengganti Java global. |
| Maven Wrapper | Wrapper3.3.4 mengunci Maven3.9.11 dan SHA256 distribusi di `.mvn/wrapper/maven-wrapper.properties`. Ini alat build/development, bukan antarmuka yang harus dioperasikan penerima aplikasi. |
| Jalur development | Plugin javafx-maven memakai TutupBukuApplication. Manifest JAR memakai Launcher dan Class-Path dependensi; keberadaan Main-Class saja tidak membuat JAR standalone. Dependensi runtime masih terpisah. Tidak menjalankan java -jar tanpa dependensi. |
| Mekanisme distribusi | `scripts/Build-Windows.ps1` sudah membuat jpackage **app-image**, bukan installer; jlink runtime,13JAR dependensi, panduan/legal/hash. `Prepare-PackagingTools.ps1` dan `Verify-WindowsPackage.ps1` tersedia. Script tidak dijalankan pada audit ini. |
| EXE yang ditemukan | `releases/TutupBuku-0.5.4-rc1-20261114-124851/TutupBuku/TutupBuku.exe`,454144byte. Runtime java/javaw dan app/TutupBuku.cfg tersedia. |
| Konfigurasi EXE | `app/TutupBuku.cfg` menunjuk Launcher dan14JAR (aplikasi+13dependensi) memakai $APPDIR. Runtime/release menyatakan Java21.0.13 dan17modul. Ini pemeriksaan file, bukan pengujian runtime ulang. |
| Terminal | Script jpackage tidak memakai --win-console dan sudah menghasilkan EXE GUI. Penggunaan EXE ini diprioritaskan untuk implementasi nanti; perilaku double-click/no-terminal belum diuji pada pekerjaan ini. |
| CSS/resource | Source CSS berada di `src/main/resources/id/ac/uns/tutupbuku/ui/tutup-buku.css`, dimuat dari classpath oleh Application. CSS pada JAR target saat ini cocok byte dengan source. JAR paket lama tidak memiliki resource CSS tersebut karena dibuat sebelum Fase1 modernisasi; tidak menyimpulkannya sebagai kegagalan startup paket lama. |
| Data/config | `persistence/AppPaths.java` menggunakan %LOCALAPPDATA%/TutupBuku/tutup-buku.db atau fallback user.home/AppData/Local; override TUTUP_BUKU_DATA_DIR menjadi path absolut. Override relatif bergantung pada working directory. Tidak membaca/mengubah DB pengguna. |
| Working directory | Untuk shortcut nanti, gunakan folder app-image yang benar-benar dipilih/diverifikasi. Default data tidak menunjuk source root; resource classpath dan $APPDIR tidak mensyaratkan membuka folder proyek dahulu. Verifikasi nyata dari direktori berbeda masih tertunda. |
| Ikon | Tidak ditemukan ikon .ico kustom di source/resources/scripts. Jangan mengklaim ikon merek tersedia. Ikon EXE dapat menjadi fallback setelah diperiksa. |
| Shortcut | Tidak ada script pembuatan .lnk di scripts; tidak membuat atau memeriksa .lnk Desktop pada audit ini. Status shortcut yang diminta: belum dibuat oleh pekerjaan ini, belum diverifikasi. |
| Error startup | Application memiliki pesan kegagalan penyimpanan dan Coba lagi; kegagalan JVM/dependensi sebelum Application tampil belum diuji. Penanganan error launcher yang mudah dipahami tetap kriteria penerimaan baru, belum dinyatakan lengkap. |

## Mengapa paket lama belum menjadi launcher hasil terbaru

Nomor versi sama tidak membuktikan payload sama. Hash yang diperiksa:

- JAR paket lama: `62AB91A579ABE6BFA0FBABC6CCCA304B9DE2C0808D036DA11B887A12EA01639F`.
- JAR hasil Fase4 saat audit: `EAB15204B594BB91250CC413BFDD89DAEE9BE50A9A618696F2F6922F830B9EFC`.
- EXE lama: `EB1943556FB2659245569EB9929E524E6A97AFF467CC41E87952B0502045E881`.

BUILD-INFO paket lama mencatat baseline4dac60c dan working-tree changes=True, sebelum modernisasi UI. Paket itu tidak diperbarui/ditimpa. Keberadaan runtime dan dependensinya merupakan dasar yang bisa dipakai kembali; tidak mengklaim paket tersebut sudah membawa kode Fase3/4 atau telah lulus pengujian launcher tambahan ini.

Lokasi absolut launcher lama yang diaudit pada komputer ini:

`D:\Kuliah\SEMESTER 3\Pemrograman-Berorientasi-Objek\Final-Project\releases\TutupBuku-0.5.4-rc1-20261114-124851\TutupBuku\TutupBuku.exe`

Untuk paket lama, mekanisme pengguna yang sudah tersedia adalah membuka EXE tersebut dari folder paket lengkap yang telah diekstrak. Ini penjelasan mekanisme yang ditemukan, bukan instruksi untuk menganggap paket lama sebagai hasil Fase5 atau klaim double-click baru sudah diuji. JAR target saat ini tidak ditawarkan sebagai launcher mandiri.

## Lingkup yang harus dilaksanakan setelah gerbang lulus

1. Pertahankan Launcher/Application dan prioritaskan app-image jpackage yang sudah ada; buat keluaran baru dari source terverifikasi tanpa menimpa rilis lama. Jangan membuat installer atau mengubah bisnis/dependency/schema.
2. Pastikan jalur peluncuran GUI, working directory dan semua dependensi/resource benar. Uji kegagalan startup terkontrol dengan pesan Indonesia yang mudah dipahami, tanpa menyentuh database pengguna.
3. Buat script pembuatan shortcut dalam proyek. Script menerima/menentukan paket terverifikasi, mengambil Desktop pengguna saat dijalankan, membuat `Tutup Buku.lnk`, mengatur target/working directory dan ikon yang benar, serta tidak menimpa shortcut pengguna lain secara diam-diam. Jangan menanam path komputer pengembang.
4. Jika Desktop tidak dapat diakses, sediakan script saja dan catat .lnk masih pending. Jangan melewati pembatasan akses atau mengubah ACL/global execution policy.
5. Uji peluncuran double-click nyata serta shortcut melalui desktop Windows, bukan menyamakan Start-Process/terminal dengan double-click. Gunakan data fiktif terisolasi: login, CSS/resource, navigasi belajar/nilai, save/tutup/buka ulang dan persistensi.
6. Verifikasi properti target/working directory/icon .lnk, error path, dan ketergantungan runtime. Bila hanya launcher development yang dapat dibuat, nyatakan JDK/JavaFX/Maven yang masih diperlukan dan jangan menyebutnya standalone.
7. Laporkan hasil aktual dan berhenti; tidak melakukan Git staging/commit/push, installer, atau Fase5C.

## Bukti dan status pengujian saat audit

Yang dilakukan baru pemeriksaan source, manifest/config, isi JAR, keberadaan dependensi/runtime dan hash file. Tidak ada peluncuran aplikasi/shortcut, double-click, navigasi, uji persistensi atau simulasi kegagalan startup baru pada pekerjaan ini. Hasil Fase4 tetap riwayat pengujian kode yang sama, bukan bukti pengujian launcher. Tidak menjalankan ulang Maven karena perubahan pekerjaan ini hanya dokumentasi.

| Gerbang | Status |
|---|---|
| Fase5A | Belum dapat diverifikasi; lingkup/kriteria/bukti belum tersedia |
| Fase5B | Belum dapat diverifikasi; lingkup/kriteria/bukti belum tersedia |
| Launcher tambahan | Pending gerbang5A/5B |
| Script shortcut / .lnk Desktop | Belum dibuat / belum diverifikasi |
| Windows double-click / startup error / persistensi launcher | NOT VERIFIED pada pekerjaan ini |
