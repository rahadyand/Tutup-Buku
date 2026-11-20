# Login opsional dan ruang kerja lokal

Implementasi 20 November 2026 WIB, baseline `main/c8e21e614e3336200af98d7f9db26599fe111634`. Permintaan pengguna mengganti kewajiban login pada PRD F01 sebagai pintu masuk aplikasi; keamanan akun dan kebutuhan inti F01–F13 tetap. Ini perubahan source **schema 8**, bukan paket rilis baru.

## Memulai dan berpindah ruang

Aplikasi selalu membuka **Hari Ini → Ruang kerja lokal / Mode tamu**, termasuk sesudah terakhir dipakai melalui akun. Tidak wajib NIM, password, nama atau prodi. Tidak ada remember-me/auto-login akun terakhir.

Ruang lokal adalah penyimpanan sungguhan: semester/course dan semester aktif, materi/gambar, catatan/completion/progres/resume, nilai aktual, What If, skenario/perbandingan/IP, tugas/ujian/checklist. Perhitungan dan penyimpanan skenario tidak dibatasi untuk tamu. Materi contoh hanya disalin melalui tindakan eksplisit lama, sekali per ruang; tidak disalin setiap startup/login/migrasi.

Buka tab **Akun** untuk Masuk/Daftar. **Masuk** membuka data akun yang NIM/password-nya sesuai. **Daftar** tetap mengikuti alur lama: NIM teks unik setelah trim, nama, password minimal 8 dan konfirmasi; berhasil mendaftar mengarahkan ke form login, bukan langsung masuk. **Kembali ke ruang kerja lokal** membatalkan form; isian password dibersihkan. Data lokal tidak dibawa ke akun baru atau akun kosong.

Setelah masuk, **Profil** dan penyuntingan nama/prodi/NIM read-only tetap seperti sebelumnya. **Keluar** kembali ke ruang lokal, bukan menghapus data. Ruang tamu menampilkan informasi workspace dan Masuk/Daftar, tanpa profil mahasiswa rekaan atau aksi Keluar yang terlihat. Tidak ada editor profil tamu.

**Ruang lokal, akun A, dan akun B sepenuhnya terpisah.** Semester aktif/arsip, course, filter, draf, skenario, aset dan bookmark tidak berpindah pemilik. Semua data tetap di perangkat; tidak ada sinkronisasi, backup cloud atau transfer/merge otomatis.

## Penyimpanan, identitas dan autentikasi

Folder default tetap `%LOCALAPPDATA%/TutupBuku`; `TUTUP_BUKU_DATA_DIR` dapat menunjuk folder demo terisolasi. Database dan folder assets tetap menggunakan pola lama.

Model FK historis mengaitkan seluruh data ke `users.nim`. Agar tidak membuat repository/tabel bisnis paralel, satu baris **pemilik lokal** memakai UUID berprefiks internal sebagai owner key pada kolom tersebut. Key ini bukan NIM mahasiswa, tidak menjadi input pengguna dan tidak ditampilkan. Kolom `account_kind` membedakan `LOCAL` dan `REGISTERED`; unique partial index memastikan satu LOCAL per database. `localWorkspace()` mencari/membuat baris itu dalam transaksi, sehingga startup ulang memakai identitas yang sama.

LOCAL **tidak memiliki password untuk login**. Kolom kredensial wajib historis berisi penanda `NO_LOGIN`, salt/hash kosong dan nilai iterasi yang memenuhi constraint lama; nilai ini tidak digunakan sebagai hash/sandi. Query `findAccount` hanya menerima REGISTERED sebelum membentuk PasswordHash/verifikasi. Registrasi adalah INSERT dan tidak dapat mengambil owner key yang sudah ada. NIM lama dengan tanda baca/prefiks tetap dapat login bila merupakan REGISTERED; tidak ada aturan pola NIM baru. Tidak ada NIM khusus/sandi kosong/default/shortcut yang membuka LOCAL lewat autentikasi. Edit profil juga ditolak untuk LOCAL pada service dan SQL.

Akun terdaftar tetap memakai PBKDF2WithHmacSHA256600.000, salt acak16byte dan hash256bit beserta parameter, serta pembersihan buffer password. Query tetap berparameter, setiap ID dipasangkan owner dari sesi. AssetStore memakai hash owner dan validasi gambar/path lama; aset akun tidak menjadi terbaca oleh guest.

## Sesi, draf dan pekerjaan latar

UserSession mempunyai state `SIGNED_OUT`, `GUEST`, `ACCOUNT`. Service menggunakan `requireOwner()`; `requireNim()` hanya adapter API/test lama. Tidak ada cabang kalkulasi/repository khusus tamu.

Urutan perpindahan:

1. Jalankan guard agenda, materi/catatan, nilai/What If/skenario, akademik dan profil yang ada.
2. Batal tetap di ruang/draf lama; Save gagal mempertahankan input dan mencegah transisi. Save yang berhasil selesai dengan pemilik asal.
3. Suspend timer autosave/posisi reader dan invalidasi dashboard lama.
4. `UiTasks.finish` menolak submission/callback lama lalu menguras pekerjaan yang sudah diterima di worker tunggal, sebelum sesi lama dibersihkan.
5. Buat worker/sesi/view baru dengan konteks pemilik tujuan. Login berhasil, pembatalan auth dan logout memakai pola yang sama; close normal juga menguras worker sesudah guard.

Antrean lama tidak dapat menyimpan sebagai pemilik baru, memunculkan pesan/header lama, atau menjalankan autosave sesudah boundary. Draf dan selection tidak dibagi lintas workspace; editor lama dihentikan, bukan dibiarkan aktif di belakang editor baru. Ketika kembali lokal, dashboard/semester aktif/bookmark milik lokal dibaca kembali; posisi reader tetap mengikuti revisi dan fallback lama. Tab/filter sementara tidak diingat lintas pergantian sesi.

Saat perpindahan, tampilan lama diganti halaman memuat. Jika ruang lokal gagal dibaca/diinisialisasi, muncul **Ruang kerja lokal belum dapat dibuka** dan tombol retry; data akun lama tidak tetap tampil seolah guest berhasil dibuka. Error tidak membuat DB pengganti atau ruang kosong rekaan. Retry menggunakan database yang sama.

## Migrasi dan kompatibilitas

Migrasi **7→8** hanya menambah `users.account_kind DEFAULT REGISTERED` beserta constraint jenis dan unique partial index LOCAL, melalui transaksi/versioning lama. Seluruh akun sebelumnya tetap REGISTERED, ID/owner/hash/SKS/kehadiran/komponen/grade/scenario/revisi/materi/catatan/progres/resume/agenda dan referensi aset tidak diubah. Inisialisasi LOCAL terpisah dan idempoten. Versi lebih baru, korup, migrasi gagal atau guest init gagal dilaporkan tanpa reset.

**Cadangkan seluruh folder data (DB+assets) saat aplikasi tertutup sebelum mencoba pada data sendiri. EXE/source schema7 dan paket0.5.5 lama tidak boleh membuka database schema8.** Pengembangan/pengujian fitur ini hanya menggunakan database sementara, tanpa membuka database pengguna. Tidak membuat ZIP/installer atau GitHub Release baru.

## Verifikasi dan bukti

Hasil final **229 non-GUI**, **18 kasus GUI/17kelas**, **3 regresi terfokus** dan **2 emulasi125%** lulus; semua0gagal/error/skip. Pengulangan terfokus terakhir memeriksa label ruang kerja pada source final. Log lokal `tmp/optional-login-20261120/`:

| Perintah Maven (opsi cache/offline mengikuti setup lokal) | Hasil / log |
|---|---|
| `verify` | 229; verify-final.log |
| `-Dtest=OptionalLoginFxCheck,WorkspaceLifecycleFxCheck,DashboardFxCheck,AgendaFxCheck,CourseHubFxCheck,LearningReadingFxCheck,WhatIfFxCheck,ScenarioJourneyFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ModernWorkspaceFxCheck,MilestoneTwoFxCheck,AuthPilotFxCheck,ReusableComponentsFxCheck,RevisionOneFxCheck,RevisionTwoFxCheck,MilestoneThreeFxCheck -DreuseForks=false test` | 18; gui-final.log |
| `-Dtest=OptionalLoginFxCheck,DashboardFxCheck,MilestoneThreeFxCheck -DreuseForks=false test` | 3; gui-focused-final.log |
| `-Dtest=OptionalLoginFxCheck,AuthPilotFxCheck -DreuseForks=false -DargLine=-Dglass.win.uiScale=125% test` | 2; gui-125-final.log |

30PNG fitur ini (15native+15emulasi) direkam dengan manifest dimensi/SHA256 `screenshots.json`. Screenshot representatif semua keadaan/ukuran di atas diperiksa; tidak mengklaim uji manusia. Percobaan awal menemukan tiga asumsi entry auth lama pada test M3/Reusable/Revision1; pintu eksplisit diperbaiki dan suite diulang. Pemotongan form panjang pada ukuran minimum ditangani scroll lama; tombol kembali tetap terlihat. Hasil juga dicatat di DEVELOPMENT_STATUS.

- OptionalLoginTest: satu guest lintas startup, semua data inti bertahan setelah reopen, starter tidak berulang, isolasi tiga owner dan aset, internal owner tidak dapat login/diambil registrasi, akun lama tetap bisa diverifikasi, migrasi mempertahankan ID/hash/konten/gambar, rollback migrasi dan retry init tanpa kehilangan akun.
- OptionalLoginFxCheck: startup guest kosong, Masuk/Daftar/batal dan login salah, akademik lokal, What If/simpan skenario tanpa mengubah aktual, agenda, autosave catatan, draf Batal/Save gagal/retry, register→login eksplisit, profil akun, logout kembali lokal, gagal muat lokal tanpa menampilkan akun lama dan retry, akun B terisolasi serta restart tetap guest. Semua memakai aplikasi produksi dan @TempDir.
- WorkspaceLifecycleFxCheck: dua penulisan SQL antrean lama selesai dengan owner asal sebelum transisi; callback sukses/error serta submission terlambat tidak mengubah ruang baru.
- Tes migrasi historis disesuaikan agar fixture benar-benar menggambarkan schema sebelum kolom baru; assertion preservation tetap. Tes GUI akun lama masuk melalui Akun→Masuk secara eksplisit, termasuk sesudah logout, sesuai UX baru; pemeriksaan fitur/guard lama tetap.

Screenshot native mencakup1366×768,1024×768 dan minimum760×480, guest kosong/berisi, area akun, auth/batal, profil akun, draf/gagal simpan/gagal muat dan kembali guest. Area client berbeda dari ukuran bingkai jendela. Emulasi125% bila tercatat memakai `glass.win.uiScale` hanya pada proses tes, bukan DPI Windows nyata. Screenshot dan manifest hash berada di `target/verification/optional-login-*.png` dan folder log lokal.

## Menjalankan dan mencoba

Dari root project pada PowerShell, gunakan setup Java21 pada README. Demo terisolasi dengan tool lokal yang sudah tersedia:

```powershell
$env:JAVA_HOME=(Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME="$PWD\.tools\maven-user"
$env:TUTUP_BUKU_DATA_DIR="$PWD\tmp\demo-optional-login"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' javafx:run
```

Folder ini memakai guest sendiri, terpisah dari database pengguna. Menghapus variabel/folder demo bukan bagian fitur reset dan tidak dilakukan otomatis. Untuk tes unit gunakan `verify`; GUI opsional memakai `-Dtest=OptionalLoginFxCheck,WorkspaceLifecycleFxCheck -DreuseForks=false test`.

1. Buka: langsung Hari Ini/Mode tamu, tanpa form login. Buat semester aktif/course, materi/catatan, agenda dan skenario; tutup dan buka ulang untuk memeriksa persistensi.
2. Akun→Masuk/Daftar→Kembali ke ruang lokal: data masih ada. Daftar akun baru, lalu login eksplisit; akun baru harus kosong, bukan salinan guest.
3. Di akun buat data berbeda dan ubah profil. Keluar: data lokal kembali, tanpa course/selection/draf akun. Coba akun kedua lalu buka ulang aplikasi; startup tetap lokal.
4. Ubah materi/catatan/agenda/nilai/What If/skenario/akademik lalu berpindah. Coba Batal, Buang dan Simpan; input dan pemilik asal harus tetap benar. Jangan sengaja merusak DB pribadi untuk meniru tes kegagalan; tes otomatis memakai trigger/DB sementara.
5. Periksa scroll/fokus keyboard serta tombol batal pada jendela kecil dan DPI perangkat Anda. Form auth panjang memakai scroll lama, tombol kembali tetap terlihat.

## Batas yang dipertahankan

Tidak ada transfer/merge tamu→akun, cloud/email/recovery/remember-me, penghapusan/reset guest, kustomisasi profil guest, fitur akademik baru atau kalkulasi baru. Transfer tetap di BACKLOG. Usability mahasiswa, keyboard fisik/pembaca layar, Windows DPI nyata, perangkat kedua dan offline fisik tetap pemeriksaan manusia yang belum diklaim. Berhenti setelah login opsional untuk review pengguna.
