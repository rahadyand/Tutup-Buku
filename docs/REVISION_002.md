# Revision 002 — akun ringkas dan navigasi kanan

14 November 2026 · kandidat **0.5.3-rc1**, Java21/JavaFX21, **schema4 / RULE_VERSION1 tetap**. Permintaan pengguna membatasi perubahan pada presentasi akun/profil/akademik/header; tidak mendesain ulang dashboard atau menambah fitur.

## Baseline dan keputusan

Baseline main69c7b21d5c0e2829344ae6d8ec4b80f78615baed, origin https://github.com/rahadyand/Tutup-Buku.git. AGENTS, Revision001, status dan source terkait dibaca sebelum perubahan. Draf untracked HUMAN_ACCEPTANCE_KIT, MORNING_CHECKLIST, P0_AUDIT dan docs/academic dipertahankan di luar revisi. R002 mengatasi arahan R001 yang menempatkan shortcut di kiri.

| Permintaan | Implementasi / bukti terfokus |
|---|---|
| 1–2 kolom/header auth | NIM200, password/konfirmasi260, nama300 logical px dengan pref/max lokal; judul36px, deskripsi480px wrap. Minimum tinggi preferred mencegah elipsis125%. |
| 3 sapaan | NimGuidance.welcome murni dari nama/NIM input. L01→Informatika, L02→Sains Data,25→2025 hanya untuk pola lengkap. Input kosong/tidak valid tanpa tebakan; login tanpa nama tersimpan sebelum auth. Header memakai profil sesudah auth. |
| 4–5 semester | Label Semester; angka90/tahun110/aksi170px. Urutan Aktif/Save/Delete/petunjuk persis/Semester Baru. Guard dan save+activate tetap. |
| 6 course | Satu Mata Kuliah Baru di bawah status simpan; Save/Delete dekat form, area dapat digulir. |
| 7 profil | Terpusat; NIM200 readonly/nama300/ComboBox240. Tepat Informatika/Sains Data, trim+case-insensitive pada tampilan. Raw legacy kosong/tidak dikenal tetap sampai pilihan baru; save nama saja mempertahankan raw. Opening tidak dirty. Save gagal mempertahankan form; guard Simpan/Buang/Batal termasuk dropdown. |
| 8 workspace | Identitas kiri; Keluar kanan atas, Modul Belajar+Simulasi Nilai berdampingan di bawah, rata kanan. View/konteks/guard sama; urutan Dashboard/Akademik/Profil tetap. |
| 9 data | Tidak mengubah schema, hashing, SQL, engine, aktual/skenario/belajar/kehadiran. Regresi M1–M5 dan R001 dipakai kembali. |
| 10–11 verifikasi/distribusi | Ukur layout aktual, screenshot, regresi dan paket terpisah; hasil nyata di bawah. ZIP/runtime/data tidak masuk Git. |

## Perintah dan hasil

PowerShell (cache lokal, tidak mengunduh ulang):
```powershell
$env:JAVA_HOME = (Resolve-Path '.tools/jdk-21.0.13+9').Path
$env:MAVEN_USER_HOME = "$PWD\.tools\maven-user"
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp -Pwindows-package verify
```
**177tes default lulus,0failures/errors/skipped.** Termasuk seluruh regresi perhitungan, auth/isolasi, SQLite/migrasi1→4, attendance, belajar, skenario, serta RevisionTwoGuidanceTest baru. Tidak menurunkan parameter hash untuk tes.

```powershell
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck,MilestoneFourFxCheck,MilestoneFiveFxCheck,ReleaseHardeningFxCheck,RevisionOneFxCheck,RevisionTwoFxCheck' '-DreuseForks=false' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=RevisionTwoFxCheck' '-DargLine=-Dglass.win.uiScale=125%' verify
.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=RevisionTwoFxCheck' '-Drevision.width=1024' verify
```
**Suite GUI final10kasus lulus,0failures/errors/skipped. Pengulangan125%1kasus dan1024×7681kasus juga lulus** setelah perbaikan deskripsi dan tambahan assertion tombol terlihat sesudah scroll. GUI memakai @TempDir. Coverage baru: lebar getLayoutBounds sesudah CSS/layout, title/deskripsi, dua sapaan, pre-auth privacy, wrong-login retains input, password toggle dan Enter handler, profil unknown/empty/case-insensitive, trigger kegagalan save+retry, pilihan Simpan/Buang/Batal, DB readback, action order/new forms tidak menghapus record, dan posisi/destinasi shortcut.

Kondisi: jendela target fisik1366×768 native,1024×768 native, serta1366×768 dengan **glass.win.uiScale=125%** (konten sekitar1080×584 logical). Tidak mengubah skala/resolusi/jaringan global. Screenshot merupakan snapshot root JavaFX; bukan foto uji usability atau klaim DPI Windows nyata.

Screenshot lokal: target/verification/r002-{login,register,profile,semester,courses,header}-{native-1366,native-1024,125%-1366}.png. Tambahan semester-actions menunjukkan tombol bawah setelah gulir. Enam layar normal dan skala sudah ditinjau; bukti jendela kecil juga ditinjau. Form panjang digulir, bukan mengecilkan editor/nilai global.

Temuan pengembangan: assertion awal posisi memakai batas bayangan tombol (selisih1,4px), diperbaiki memakai layout bounds; bukan bug navigasi. Screenshot125% mengungkap deskripsi terpotong; min-height diperbaiki, screenshot ulang dan assertion tinggi lulus. Warning JavaFX unnamed module/unchecked helper lama tetap nonfatal.

## Paket Windows final dan launcher hasil ekstraksi

```powershell
.\scripts\Build-Windows.ps1 -Offline -SkipBuild
.\scripts\Verify-WindowsPackage.ps1 -Zip 'releases/TutupBuku-0.5.3-rc1-20261114-120721/TutupBuku-0.5.3-rc1-windows-x64.zip'
python tmp/verify-r002-payload.py
```

- ZIP: `releases/TutupBuku-0.5.3-rc1-20261114-120721/TutupBuku-0.5.3-rc1-windows-x64.zip`, **95.937.590byte**.
- Launcher distribusi: `releases/TutupBuku-0.5.3-rc1-20261114-120721/TutupBuku/TutupBuku.exe`.
- Launcher yang benar-benar diuji: `tmp/Uji Paket Ekstraksi 20261114-120803/TutupBuku/TutupBuku.exe`.
- SHA256ZIP **88B856B2304BF9D198B40A0033C885311EDD226F9D949015ACEA5750010F17A5**.
- SHA256JAR **4AC5E64759ABB40C6650C69B041E02B936DE101B0FB61068AC45810B66B23723**, sama dengan target build terverifikasi.

Build memakai JAR/dependency dari verify final; tidak mengulang unduhan atau menimpa paket terdahulu. Audit226hash+manifest, forbidden0 dan runtime17modul lulus. Semua **227file ZIP cocok byte/hash dengan ekstraksi yang diuji**. BUILD-INFO mencatat baseline69c7b21+working-tree changes=True karena paket dibuat sebelum checkpoint source R002; hash JAR mengikat input terverifikasi ke payload. Panduan paket disalin saat build; catatan bukti repository ini diperbarui sesudah uji tanpa regenerasi paket.

EXE hasil ekstraksi dijalankan dua kali dengan JAVA_HOME/MAVEN_HOME kosong, PATH hanyaSystem32, runtime bawaan dan folder data terisolasi `tmp/r002-package-data-20261114-120816`. Akun/semester/course/8/10/snapshot fiktif dibuat lewat service sebelum launcher, bukan diklaim lewat GUI. Interaksi desktop pada EXE membuktikan Enterlogin, header terautentikasi, profil terpusat dengan tepat dua opsi, perubahan nama/prodi→Simpan→header berubah, halaman semester/course, edit metadata course→status tersimpan→tombol baru di bawahnya. Close normal **exit0**; buka ulang/login menunjukkan nama Demo Revision002, Sains Data dan course PBO PaketR002 tetap, lalu close **exit0**.

Read-only SQLite sesudah tutup membuktikan profil/course3SKS/aktif/aktual8/10/snapshot tetap, schema4 dan quick_check/FK sah. Nilai/skenario diperiksa lewat readback pada paket ini, bukan diklaim seluruh editor/skema uji GUI source diulang pada EXE. Kedua log EXE hanya warning JavaFX unnamed module, tanpa exception baru. Ada satu error alat desktop “coordinate input geometry is unavailable”; pulih dengan memilih ulang jendela dan refresh, tidak mengubah kode atau data.

Bukti lokal di `tmp/r002-{verify,gui,scale,small,package,package-audit}.log`, `tmp/r002-exe*-{stderr.log,result.txt}`, `tmp/r002-final-evidence.json`, serta target/surefire-reports dan target/verification. Folder tersebut diabaikan Git. Tidak memakai database AppData nyata atau mengubah jaringan/DPI global.

## Checkpoint dan berhenti

Diff dan .gitignore ditinjau, Wrapper tetap tracked; source/test/docs/config revisi saja disertakan. Fetch+ls-remote sebelum commit menunjukkan origin/main69c7b21 sama dengan baseline, tanpa outgoing commit lain. Commit yang diminta: `feat: refine account forms and academic layout for revision 002`; push normal main dan kesamaan hash remote dicatat dalam `tmp/r002-checkpoint.txt` dan jawaban akhir. Tidak force-push/rewrite/publikasi Release/upload binary. Draf lama tetap untracked. Setelah push terverifikasi, berhenti untuk review pengguna.

## Batas

Komputer Windows kedua, jaringan fisik terputus, skala Windows125% nyata dan usability3–5mahasiswa belum dilakukan. H-02 historis JavaFXListView belum diklaim selesai karena tidak muncul pada satu sesi. Tidak mengerjakan P1/P2, laporan/presentasi atau revisi berikutnya. Paket terdahulu/data pengguna dipertahankan.
