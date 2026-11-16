# Adopsi komponen Fase 5C — 15 November 2026

Inventaris berikut mencakup seluruh layar produk yang tersedia, bukan fitur baru. Catatan Fase4 di bawah tetap menjadi riwayat.

| Layar/komponen | Implementasi presentasi | Kontrak yang dipertahankan |
|---|---|---|
| Startup/gagal membuka data | Root bertema, heading/form/tombol Coba lagi | DB gagal tidak direset, retry asli |
| Masuk/Daftar, PasswordInput | Pilot5B dipertahankan; tema root diteruskan setelah login | LabelFor aktif, toggle, Enter, reset, ukuran R002 |
| Workspace/Dashboard | Header permukaan, tab aktif, shortcut utuh, header ringkas menurut tinggi | Instance view, tab, worker dan semua guard yang sama |
| Profil | Form card, aksi primary, dropdown bertema | NIM readonly, canonical/legacy prodi, save/retry |
| Semester | Form card, primary Simpan, danger Hapus | Transaksi simpan/aktivasi, dirty state dan ukuran lokal |
| Mata kuliah | Sidebar adaptif bergulir, card, FlowPane aksi | Edit metadata saja, kehadiran tidak berubah |
| Belajar kuliah/peminatan | Dua cabang TitledPane, list dan panel putih, scrollbar native | Seleksi tetap; collapse tidak mengubah data/guard |
| Materi tulis/pratinjau/catatan | Judul17, metadata, code editor, primary Save, panel bergulir | Autosave2detik, CAS, draft, completion dan note terpisah |
| MarkdownReader | reader.css UI, teks14, heading/code/table/link | Parser/CSP/script/assets/scroll ratio tetap |
| Nilai aktual | Panel putih, save primary, hapus danger, hasil menonjol | Rumus, total100%, aktual, kehadiran, footer Fase3 |
| Skenario/target/IP/perbandingan | Panel/list/table, aksi primary, hasil, formula collapsible | Snapshot, CAS, target, perbandingan historis, tooltip panjang |
| Konfirmasi hapus/guard/mode/review/seed | styleDialog bersama dan stylesheet Scene dialog | ButtonType, default Batal, owner, callback identik |
| File chooser/window chrome | Dikecualikan: native Windows | Tidak mengganti mekanisme pilih file |
| CalculationDemoView | Dikecualikan: fixture M1, bukan layar runtime produk | Smoke engine lama tetap diuji |

Gunakan factory FormSupport untuk label/input/button/form dan `limitWidth` untuk ukuran lokal. Tambahkan `tb-primary` hanya untuk aksi utama yang jelas, `tb-danger` untuk hapus, `tb-result` untuk hasil; jangan mengklasifikasikan error dengan parsing pesan service. Panggil `styleDialog` setelah ButtonType ditetapkan dan sebelum showAndWait. CSS tidak memasang event handler atau memvalidasi data.

Tidak ada custom Control/framework baru. Enam lokasi konfirmasi/guard memakai satu helper styling; duplikasi lebar yang dikurangi Fase4 tetap. Navigasi hierarkis memakai TitledPane dan daftar yang sudah ada; TreeView baru dikecualikan agar model seleksi/worker/guard tidak digandakan. H-02 ListView tidak dianggap selesai oleh perubahan style.

---

## Riwayat Fase 4

# Komponen UI - Fase 4

14 November 2026 (WIB), aplikasi 0.5.4-rc1, Java 21 / JavaFX 21.0.10.

## Lingkup dan pemetaan riwayat

Fase 4 pada instruksi pengguna terbaru berarti **Reusable UI Components & Component Standardization**. Bagian 8 `UI_AUDIT_AND_PLAN.md` lama memakai nama Fase 4 untuk panel belajar/reader; roadmap tersebut tetap disimpan sebagai riwayat. Pekerjaan panel belajar tidak otomatis ikut fase ini. Fase 3 adalah checkpoint layout nilai yang masih belum di-commit. Fase 4 tidak menyelesaikan Formal Design System Foundation dan tidak mengizinkan commit/push atau Fase 5.

Sumber audit: AGENTS, seluruh teks PRD 25 halaman melalui ekstraksi lokal yang tersedia, README, IMPLEMENTATION_PLAN (milestone produk, bukan Migration Plan UI), UI_AUDIT_AND_PLAN, DEVELOPMENT_STATUS, REQUIREMENTS, ARCHITECTURE, REVISION_002, KNOWN_ISSUES dan BACKLOG. Migration Plan UI formal dan DESIGN_SYSTEM.md belum tersedia. Instruksi pengguna terbaru menentukan lingkup; P1/P2 produk tetap backlog. Tidak menjalankan instruksi implementasi/commit lama yang tersimpan di dokumen historis.

## Inventaris dan keputusan

Semua path Java berikut relatif terhadap `src/main/java/id/ac/uns/tutupbuku/ui/`.

| Komponen/pola | Pengguna dan tanggung jawab | Keputusan Fase 4 |
|---|---|---|
| `FormSupport.text/password/button` | Factory native dengan ID stabil, digunakan form akun/akademik, editor, nilai dan fixture demo | Dipakai kembali; tidak membuat subclass kontrol atau framework tombol. Event tetap ditetapkan pemanggil. |
| `FormSupport.field/fieldCentered` | AuthView, profil WorkspaceView, AcademicView, MaterialEditor dan pane nilai | Sudah menyatukan label, gap 4 dan asosiasi input. Dipertahankan, termasuk pengalihan label password ke input aktif. |
| `FormSupport.form/centeredForm/centeredPage/scroll` | Form akun/profil/semester/course dan berbagai pane | Sudah reusable: gap 12/padding 20, centeredPage padding 12 serta minimum tinggi preferred. Tidak diseragamkan ke pane nilai yang sengaja lebih padat pada Fase 3. |
| `FormSupport.note/status` | Bantuan dan status pada seluruh modul melalui helper yang sama | Status kini mendelegasikan pembuatan Label wrap ke note. ID status dan teks runtime tetap. Tidak membuat sistem validasi baru. |
| Pasangan prefWidth/maxWidth | AuthView, WorkspaceView, AcademicView, PasswordInput | Disatukan sebagai `FormSupport.limitWidth`, dengan lebar tetap ditentukan pemanggil. |
| `PasswordInput` | Tiga instance pada AuthView: login, daftar dan konfirmasi | Hanya memakai helper lebar; dua representasi, binding, label aktif, bantuan, toggle, Enter, reset/clear tetap. Tidak mengubah keamanan. |
| Bantuan input | Panduan NIM, password, profil legacy dan aturan semester | Konten bersifat kontekstual; binding bantuan yang sudah ada dipertahankan. Tidak memaksa semua bantuan ke komponen baru. |
| Validasi/guard | Status Label dan UiTasks, GradeForms.guard, guard materi | Validasi tetap pada service; presentasi status memakai Label wrap. Dialog/guard tidak digabung karena kontrak save materi berbeda. |
| `MaterialEditor` | Satu komponen editor/preview/catatan/completion, memakai field/status/button bersama | Diaudit, tidak diedit. Tidak ada duplikasi editor kedua. Autosave, CAS dan buffer draft membuat ekstraksi besar berisiko. |
| `MarkdownReader` | Satu wrapper WebView yang dipakai MaterialEditor | Diaudit, tidak diedit. Render, DOM link, script disabled dan pemulihan posisi khusus reader tidak dipindah ke helper form. |
| CSS aplikasi | `.tutup-buku-root`, `.section-heading` | Tetap sama; refactoring ini tidak memerlukan selector/warna/theme baru. |

## Kontrak pemakaian

Helper tetap package-private di lapisan UI, dipanggil pada JavaFX thread ketika menyusun view. Setiap pemanggilan factory menghasilkan node baru; jangan memakai satu Label atau input untuk dua parent.

```java
TextField name = text("profile-name", "Nama");
limitWidth(name, 300);
VBox row = fieldCentered("Nama", name);
Label feedback = status("profile-status");
Label explanation = note("Lengkapi profil lalu Simpan.");
Button save = button("save-profile", "Simpan profil");
```

- `limitWidth(Region, double)` hanya mengatur preferred dan maximum width. Tidak mengatur minimum, tinggi, managed/visible, fokus, handler atau binding. Nama method menyatakan batas lokal, bukan lebar wajib untuk semua kontrol. Jangan terapkan pada kontrol dengan width property yang sudah bound.
- Gunakan `field`/`fieldCentered` untuk asosiasi label. Untuk PasswordInput, helper memasang binding labelFor ke field masked/visible yang sedang aktif; jangan menggantinya dengan labelFor ke HBox pembungkus.
- `note` untuk teks bantuan wrap; `status` untuk Label wrap dengan ID stabil yang dapat diteruskan ke UiTasks. Pemanggil tetap memiliki lifecycle/pesan/binding visibility. Tidak ada warna sukses/gagal atau pengumuman screen reader baru.
- Gunakan `button` untuk kontrol native berteks dan ID. Tetapkan aksi di view; guard, konfirmasi hapus dan tombol Batal default tidak menjadi tanggung jawab factory tombol.
- Tetap gunakan layout form bersama sebelum membuat factory layout baru. Gunakan CSS untuk style yang memang berulang; jangan menambah CSS hanya untuk memindahkan ukuran lokal tanpa kebutuhan.

## Ukuran dan tampilan yang dipertahankan

| Lokasi | Lebar preferred/maximum (logical px) |
|---|---|
| Auth NIM / nama | 200 / 300 |
| Password masked dan visible / pembungkus | 260 / 366; toggle minimum 98 tetap |
| Profil NIM / nama / prodi | 200 / 300 / 240 |
| Semester nomor / tahun / tiga aksi | 90 / 110 / 170 |

Latar #f5f7f6, Segoe UI 13px, heading 22px bold, auth 36px dan judul materi 17px tetap. Lebar course/editor yang hanya memiliki maximum width tidak diubah menjadi pref+max. Minimum kontrol dan kemampuan native menyusut tetap; tidak memakai fixed min=pref=max. Urutan node, navigasi dan shortcut R002 tetap.

## Analisis duplikasi sebelum/sesudah

| Ukuran spesifik yang dihitung | Sebelum | Sesudah |
|---|---|---|
| Blok pasangan setPrefWidth/setMaxWidth bernilai sama dalam empat consumer | 12 blok / 24 pemanggilan setter | 12 panggilan limitWidth; implementasi pasangan setter hanya sekali di FormSupport |
| Baris pengaturan lebar di consumer | 24 | 12 (berkurang 12 baris atau 50% khusus pola ini) |
| Implementasi pembuatan Label wrap pada note/status | 2 jalur | 1 jalur di note, status mendelegasikan |
| Class kontrol/abstraksi produksi baru | 0 | 0 |

Masing-masing AuthView, WorkspaceView, AcademicView dan PasswordInput mempunyai 3 lokasi pemanggilan. Loop tiga tombol semester dan tiga instance PasswordInput tetap, sehingga jumlah kontrol runtime lebih besar dari jumlah lokasi kode. Angka 50% bukan pengurangan seluruh kode UI. Tidak mengukur total LOC bersih termasuk dokumentasi/tes sebagai pengurangan duplikasi.

## Verifikasi dan bukti

Hasil akhir serta perintah yang benar-benar dijalankan dicatat pada bagian Fase 4 di DEVELOPMENT_STATUS. Fixture baru `ReusableComponentsFxCheck` memeriksa ukuran aktual/pref/max, relasi label termasuk password visible, CSS/judul auth, seluruh kontrol form yang dapat digulir, serta pesan validasi dan input yang dipertahankan. Native mencakup 1366x768, 1024x768 dan 760x480 dalam satu kasus; proses 125% memakai 1366x768. Suite R002 tetap memeriksa keyboard KeyEvent dan guard/retry, tidak diganti oleh fixture baru.

Baseline sebelum edit produksi disimpan di `tmp/ui-phase4-20261114/before/`. Baseline emulasi memakai `surefire:test` pada class hasil kompilasi sebelum refactoring tanpa kompilasi ulang; hash class baseline direkam. Hasil setelah refactoring memakai `verify` yang mengompilasi source terbaru. Setiap perbandingan hanya menghitung gambar yang benar-benar dihasilkan pada run tersebut.

## Risiko dan pekerjaan terpisah

H-02 ListView tetap **OPEN**. Keyboard OS/fisik, screen reader manual dan DPI Windows nyata tetap **NOT VERIFIED**. KeyEvent, properti dan snapshot JavaFX tidak menggantikan pengujian tersebut. Form minimum tetap perlu scroll; tes bukan bukti semua data panjang atau seluruh dialog sudah nyaman dipakai.

Formal Design System masih parsial: belum ada DESIGN_SYSTEM.md, token warna semantik, spesifikasi state kontrol dan Migration Plan UI formal. Standardisasi helper tidak menyatakan kesenjangan itu selesai. Rekomendasi fase berikutnya, memerlukan instruksi baru: lengkapi kontrak formal tersebut dan review penerimaan manual/variasi snapshot; ukur panel belajar jika kembali diizinkan. CSS HTML reader perlu direncanakan terpisah dari CSS scene dan tidak boleh mengubah kebijakan keamanan atau service pada fase komponen ini.
