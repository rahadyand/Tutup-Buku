# Konvensi Tutup Buku

- Sumber produk: baca seluruh `PRD_Tutup_Buku_v1.1.pdf`; aturan pengguna dalam chat mengatur lingkup pekerjaan. Bedakan P0, P1 bersyarat, P2, rekomendasi, dan asumsi.
- UI, dokumentasi dan penjelasan berbahasa Indonesia. Nama class/method/variable memakai bahasa Inggris yang jelas.
- Java 21, JavaFX 21, Maven Wrapper. Versi persis di `pom.xml` dan `.mvn/wrapper/maven-wrapper.properties`. Jangan tambah framework tanpa kebutuhan nyata.
- `domain`: model immutable dan validasi; `service`: rumus/auth/akademik tanpa JavaFX atau SQL; `ui`: tampilan; `persistence`: interface repository dan implementasi SQLite. Jangan letakkan rumus atau SQL di event handler.
- Gunakan BigDecimal dari String, jangan double untuk nilai/bobot/SKS. Jangan membulatkan tampilan sebelum konversi huruf atau pembandingan target. Bobot total harus tepat 100, termasuk 99,99/100,01 tidak sah.
- Nilai kosong bukan nol. Aktual tidak pernah diubah oleh solver/asumsi. Bobot nol tidak membutuhkan nilai dan tidak ikut solver. Target >100 harus dilaporkan tidak mungkin.
- Kehadiran awal terpenuhi sebagai asumsi belum terverifikasi; override E harus mempertahankan hasil asal. IP hanya untuk seluruh mata kuliah dengan SKS/hasil efektif sah.
- Simpan password hanya sebagai hash bersalt beserta parameter; gunakan query berparameter dan isolasi pemilik pada M2. NIM teks trim, nol awal tetap. Jangan log password.
- Materi kuliah dan peminatan terpisah; gunakan library Markdown, gambar lokal; raw HTML/script dinonaktifkan saat reader dibuat.
- Perbarui `docs/DEVELOPMENT_STATUS.md` setelah perubahan bermakna. Uji dengan `./mvnw.cmd verify` (PowerShell: `.\mvnw.cmd verify`). Jangan mengklaim fitur/tes manual yang belum dijalankan.
- Pertahankan berkas pengguna. `src/App.java` adalah starter lama, di luar source root Maven. Salinan konfigurasi awal ada di docs.
- Jangan mengerjakan milestone berikutnya tanpa permintaan pengguna. P1 hanya setelah gerbang P0; semua penundaan tetap di backlog.

- PBKDF2WithHmacSHA256 bawaan Java: 600.000 iterasi, salt acak 16 byte, hash 256 bit. Jangan menurunkan parameter untuk mempercepat tes. AuthService membersihkan buffer char[] setelah operasi; jangan mencetak password/hash.
- UI meneruskan data ke service melalui worker UiTasks; pemilik query akademik berasal dari UserSession. Query matkul/semester wajib difilter pemilik bersama ID. Logout membuang sesi, bukan data.
- Database default %LOCALAPPDATA%/TutupBuku/tutup-buku.db; TUTUP_BUKU_DATA_DIR untuk folder demo terisolasi. Semua tes memakai @TempDir atau in-memory, tidak boleh database default pengguna.
- Skema memakai PRAGMA user_version (saat ini 4), FK aktif dan transaksi. Versi tak dikenal/korup/gagal simpan harus dilaporkan, tanpa reset atau database pengganti diam-diam. Migrasi berikutnya harus mempertahankan data.
- Konfirmasi sebelum hapus; simpan gagal mempertahankan form. Revision 001: UI memakai tahun tunggal4 digit (1000–9998), nomor semester positif; identitas baru nomor+tahun per akun. academic_year rentang/periode tetap internal untuk kompatibilitas, bukan input UI. Prodi Informatika/Sains Data sebagai panduan; profil lama tidak ditimpa otomatis.

- M3: materi hanya memiliki chapter_id ATAU field_id; query dan FK tetap dibatasi owner_nim. Catatan/completion terpisah dari markdown; course progress berdasarkan jumlah semua materi, bukan rata-rata bab.
- Migrasi 1→2 menambah tabel belajar dalam transaksi; jangan mereset akun/data M2 atau menyalin contoh saat migrasi/login. Starter disalin eksplisit sekali per akun melalui marker learning_seed.
- Gunakan CommonMark+GFM tables; escape raw HTML, script halaman WebView disabled, tanpa Java bridge. Gambar hanya dari AssetStore pemilik; external image/file/javascript URL ditolak. Link http/https hanya dibuka browser setelah klik DOM.
- Referensi gambar relatif assets/{SHA256-NIM}/{UUID}.png/jpg; validasi decode PNG/JPEG, ukuran≤10MB,≤16juta piksel, path dan realpath. Jangan hapus aset saat save/delete materi gagal/berhasil; cleanup aset belum dibuat.
- Save materi+catatan atomik, CAS material revision/note revision; urutan tidak mengubah content revision. EditorDraft mempertahankan input/dirty state saat error; autosave2detik di antrean worker tunggal. Simpan/Buang/Batal berlaku saat berpindah/logout/menutup jendela.
- Resume menyimpan rasio0–1 dan revisi; setelah render selesai, pulihkan rasio. Jika revisi berubah kembali ke0; ID terakhir dipertahankan setelah delete agar ada penjelasan. Waktu database UTC; tampilan materi WIB/Asia-Jakarta.

- M4: aktual assessment_components/course_actual_grades terpisah dari snapshot scenario_courses/scenario_items. Solver tidak menulis nilai aktual. Mode DETAIL/DIRECT menyimpan inactive data.
- Course.revision naik atomik pada edit komponen/aktual/order/SKS. Snapshot tidak otomatis direbase: review menjelaskan perubahan; refresh hanya draft sampai Simpan.
- Scenario.version adalah CAS saat save header+children dalam transaksi. Duplikasi ID baru independen; historical course tanpa live FK sengaja bertahan setelah course delete, tetapi semester delete menghapus skenario.
- RULE_VERSION=1 menyimpan versi aturan, berbeda dari user_version3. Skenario usang selalu berlabel historis; perbandingan memeriksa semester/set/revisi, bukan IP parsial.
- Simpan nilai eksplisit, input gagal dipertahankan. GradeView guard digabung learning guard untuk tab/dashboard/logout/close. Uji GUI opsional -Dtest=MilestoneFourFxCheck; tiap kelas GUI perlu -DreuseForks=false.

- M5 kandidat0.5.0-rc1: Launcher main biasa untuk jpackage/classpath; Java21 tetap. Runtime distribusi Temurin21.0.13+9 pinned+SHA256 di scripts/Prepare-PackagingTools.ps1, bukan Java global.
- scripts/Build-Windows.ps1 membuat folder releases bertimestamp baru dan tidak menghapus artefak lama. Jangan commit ZIP/runtime, jangan publikasi GitHub Release otomatis. Sertakan guide/legal/hash.
- AcademicService menjaga konfirmasi kehadiran saat metadata saja berubah. Guard profil/akademik digabung guard M3/M4; kegagalan save tidak meneruskan navigasi.
- Uji skala lewat -Dglass.win.uiScale=125% hanya proses tes; jangan ubah skala/resolusi/jaringan global. Komputer kedua/offline fisik/usability harus dilabeli pending sampai hasil nyata ada.

- Revision 001/0.5.2-rc1: urutan workspace Dashboard, Semester & Mata Kuliah, Profil. Shortcut tunggal di header kiri tepat di bawah Keluar; pakai view yang sama, lewati guard sebelum navigasi/refresh.
- Auth memakai panduan NimGuidance (L+01/02+tahun2digit+identitas3digit), bukan aturan login wajib baru. PasswordInput sinkron masked/visible; tersembunyi awal, reset saat sukses/keluar tab; jangan log isi.
- Akademik dua halaman: form semester terpusat, lalu daftar semester/course. Jadikan Aktif menyimpan+aktivasi dalam transaksi; Simpan tidak mengubah aktif; gagal tetap di form. Identitas tersimpan ditangkap sebelum refresh supaya retry tidak membuat duplikat.
- SemesterSchema3→4 rebuild satu tabel dengan FK off sebelum transaksi, cek FK sebelum commit, restore FK; salinID dan periode, jaga sqlite_sequence. Data arsip ditandai legacy dan label periode+#ID membedakan benturan nomor/tahun. Jangan merge arsip. Tahun yang terlihat selalu tunggal.
- UI akademik memanggil saveCourseMetadata; SQL hanya mengubah nama/kode/SKS/revisi, tidak menyebut kehadiran. Course baru memakai default asumsi. Acuan dan checkbox kehadiran hanya di nilai aktual/simulasi; saveActual overload menyimpan acuan atomik.
