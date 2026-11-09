# Status pengembangan — Milestone 3

Tanggal: **8 November 2026, Asia/Jakarta**. Aplikasi **0.3.0-SNAPSHOT**, schema **2**. **M3 selesai; berhenti sebelum M4.** Perubahan M3 belum di-stage, di-commit atau di-push.

## Checkpoint M2 sebelum M3
Atas permintaan pengguna, instruksi/status/diff/remote/outgoing diperiksa.37berkas M2 relevan di-stage,96tes diulang dan lulus. Ignore rule menahan build/cache/tool/database/log/secrets; Maven Wrapper/.mvn tetap dilacak. Pemindaian pola rahasia umum tidak menemukan kecocokan. Tidak ada database/cache/build di commit.

Commit **0ca1b2899bbfff9c057f20f0ba984a807e9a7e2a** — feat: complete milestone 2 accounts and academic persistence — berhasil di-push ke **main**, [rahadyand/Tutup-Buku](https://github.com/rahadyand/Tutup-Buku). git ls-remote mengembalikan hash yang sama sebelum M3 dimulai. Tidak ada force-push, rewrite history atau perubahan visibility. Status historis M2 di [MILESTONE_2_STATUS.md](MILESTONE_2_STATUS.md).

## Implementasi M3
- Dua jalur terpisah: semester/course/bab/materi dan peminatan/bidang/materi. Materi tepat satu lokasi, owner+parent FK; CRUD dan urutan Naik/Turun. Edit satu materi/salinan tidak memperbarui materi lain.
- Editor Tulis dan Pratinjau/Baca memakai CommonMark0.24.0+GFM tables. Mendukung heading/paragraf/bold/italic/list/tabel/fenced code/gambar/link. Isi kosong boleh Draf, judul wajib, sumber opsional, revisi/urutan/timestamp tersimpan.
- Ringkasan hanya heading manual. Kode sebagai teks. Raw HTML di-escape, script halaman disabled, tanpa Java bridge; link hanya http/https absolut tanpa credentials setelah klik, popup/navigasi halaman luar tidak dipakai.
- PNG/JPEG lokal didekode/divalidasi, batas10MB/16MP, disalin atomik ke assets/{SHA256-NIM}/{UUID}.png/jpg. Referensi relatif; normalisasi/realpath/owner guard; missing/invalid images menjadi placeholder bernama. Tidak ada cleanup aset agresif.
- Save eksplisit dan autosave2detik pada satu worker, status Belum tersimpan/Menyimpan/Tersimpan/Gagal menyimpan. Materi+note save atomik, CAS material/note revision, generation ack EditorDraft. Gagal save mempertahankan isian/aset dan menahan navigasi.
- Simpan/Buang perubahan/Batal pada pemilihan lokasi/materi, tab utama/simulasi, logout dan close. Save yang gagal tidak membawa pengguna keluar editor.
- Completion manual; membuka bacaan tidak menandainya selesai. Chapter/field menghitung materi dalam lokasinya; course menghitung seluruh materi semua bab (2/10=20%, bukan50%). Empty state tanpa pembagian nol.
- Note teks pribadi terpisah dari isi/ringkasan, satu per material+owner, tersimpan dan konfirmasi hapus. Reorder tidak menghapus note/completion atau menaikkan content revision.
- Resume per akun menyimpan ID/revisi/rasio0–1/lokasi, debounce700ms dan flush sebelum meninggalkan bacaan. Posisi dipulihkan setelah render. Revisi berubah→awal, deleted material→pesan dan daftar yang tersedia. Tombstone ID sengaja bertahan setelah delete.
-5contoh singkat asli:3PBO pada course pilihan,2peminatan pada bidang contoh. Disalin eksplisit sekali per akun dalam transaksi+marker, bukan pada setiap login/migrasi; tidak menimpa edit atau mengklaim course lengkap.
- Migrasi aditif1→2 menambah index/tabel belajar dalam transaksi. Akun/profil/hash/semester/course/active reference M2 dipertahankan. Error migrasi rollback, tanpa reset.
- Module javafx-web21.0.10 ditambahkan sesuai versi JavaFX proyek; tanpa framework/layanan baru. Tampilan waktu materi WIB, timestamp DB UTC.
- README/arsitektur/checklist/plan/backlog/AGENTS diperbarui. Academic deletion confirmations mencakup materi/catatan/progres yang ikut cascade. Runtime root assets/ ditambahkan ke ignore jika data dir diarahkan ke root proyek.

## Verifikasi akhir
Windows x64, JDK21.0.12, Maven3.9.11 melalui Wrapper3.3.4. Semua tes persistence/asset/GUI memakai @TempDir atau in-memory, tidak memakai database AppData pengguna.

Setelan:
```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12'
$env:MAVEN_USER_HOME = "$PWD\.tools\maven-user"
```

| Perintah | Hasil aktual |
|---|---|
| `.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp verify` | BUILD SUCCESS;118tes,0failures,0errors,0skipped; JAR0.3.0-SNAPSHOT dibuat |
| `.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp '-Dtest=JavaFxSmokeCheck,MilestoneTwoFxCheck,MarkdownReaderFxCheck,MilestoneThreeFxCheck' '-DreuseForks=false' verify` | BUILD SUCCESS;5tes GUI,0failures,0errors,0skipped |
| `.\mvnw.cmd '-Dmaven.repo.local=.m2/repository' -o -B -ntp javafx:run`, TUTUP_BUKU_DATA_DIR=$PWD/tmp/m3-launch-check | Launcher berjalan, database demo dibuat; jendela berjudul Tutup Buku — Milestone3 ditemukan |
| `git diff --check` | Tidak ada whitespace error; warning LF/CRLF Windows bukan failure |
| M1 engine/test comparison dan Git index | Mesin hitung/domain/tes numerik tetap; index M3 kosong, HEAD tetap checkpoint M2 |

Rincian118tes default:70M1,26M2,22M3. M3: LearningMigrationTest2, LearningPersistenceTest12, AssetsAndMarkdownTest8. Regresi schema test diperbarui untuk mengharapkan versi2. UI: JavaFxSmokeCheck1, MilestoneTwoFxCheck2, MarkdownReaderFxCheck1, MilestoneThreeFxCheck1.

Cakupan otomatis M3:
- Migrasi database v1 berisi akun/hash/profil/semester/course/active semester, login setelah migrasi, reopen idempotent; konflik tabel saat migrasi membuktikan rollback versi dan data.
- CRUD/order/persistensi, pemilik silang ditolak, satu lokasi XOR, judul sama pada dua jalur tetap independen, starter seed idempotent dan salinan akun lain independen.
- Progres bab2materi dan8materi, kosong, tambahan materi mengubah penyebut, open tidak complete; note/completion/reopen/reorder.
- Resume normal, revisi berubah dan materi deleted; pointer akun lain tidak bocor.
- CAS menolak save lama; trigger kegagalan note rollback seluruh bundle materi+note.
- PNG dan JPEG tetap ada setelah asal direlokasi, akun lain ditolak, ekstensi palsu/traversal/file/remote/data image ditolak, missing image placeholder, NIM berisi path tidak keluar root; gagal save mempertahankan aset lama dan baru.
- CommonMark headings/format/list/table/code; raw script/iframe menjadi teks, scheme link berbahaya tidak menjadi anchor.
- WebView script halaman false, script raw tidak berjalan, perintah internal restore scroll bekerja; link tidak terbuka otomatis dan callback browser hanya setelah click, tanpa navigasi WebView ke website luar.
- GUI penuh: seed, pilih/baca/edit, save/auto-save, gagal simpan mempertahankan text, Save/Discard/Cancel pada navigasi dan close, manual progress, note delete confirmation, reorder, resume/scroll, deleted fallback, logout/restart, interest path dan marker seed bertahan.

Laporan XML/TXT: target/surefire-reports. Snapshot scene akhir: target/verification/m3-learning.png. Snapshot1280×820 ditinjau; ruang baca, tombol, catatan dan progres terlihat. Snapshot berasal dari scene test, bukan capture aplikasi pengguna lain.

Catatan eksekusi: dua tes aset mula-mula ditolak sandbox Windows saat rename file sementara; tes yang sama lulus dengan akses normal di luar sandbox, tanpa melonggarkan validasi. Fixture GUI awal mencoba tombol ketika belum aktif; penantian tombol diperbaiki lalu seluruh5tes GUI diulang lulus. Warning unnamed-module pada JavaFX test classpath dan unchecked cast selector pada fixture GUI tercatat; tidak ada exception toolkit pada run akhir.

## Batas verifikasi dan fitur tertunda
Pemeriksaan visual dilakukan melalui snapshot; alur interaksi adalah tes GUI otomatis. Dialog file chooser pemilihan gambar belum dioperasikan manual; decode/copy/persistence/render gambar diuji otomatis. Belum ada uji usability mahasiswa, skala125%/1366×768, jaringan fisik diputus, komputer kedua, paket jpackage/runtime, benchmark30×20 atau audit keamanan formal.

Aset orphan sengaja dipertahankan; tidak ada cleanup otomatis. Cadangan manual perlu DB+assets. Database tidak dienkripsi seluruhnya, recovery/ganti password belum tersedia. Materi contoh singkat, bukan modul lengkap. Tidak ada LaTeX/PDF/video tertanam/AI/kuis atau skenario nilai tersimpan.

Milestone berikutnya **M4 editor nilai aktual/asumsi dan skenario semester tersimpan**. Belum dimulai. F08G/P1/P2 tetap backlog. Tidak ada commit/push perubahan M3.
