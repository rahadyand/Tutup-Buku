# Arsitektur Tutup Buku — M1–M4

## Aliran sederhana

UI menangkap input → service memvalidasi/mengoordinasikan → repository mengakses SQLite → hasil immutable ditampilkan. Service tidak mengenal JavaFX/SQL; engine perhitungan tetap murni. M2 menambahkan SQLite repository untuk akun/akademik; engine M1 tidak diubah.

| Class | Tanggung jawab dan alasan |
|---|---|
| Score | Menyimpan perolehan dan maksimum, validasi0≤earned≤maximum dan maximum>0; normalized() mengeluarkan skala100 dengan presisi34digit |
| AssessmentItem | ID, nama bebas, bobot0–100 dan aktual Optional; tidak menyimpan asumsi |
| ScenarioAssumptions | Map ID→normalisasi terkunci, disalin immutable; saran tidak ditulis otomatis ke sini |
| Attendance | met dan confirmed terpisah; awaltrue/false berarti memenuhi sebagai asumsi |
| LetterGrade | Tabel persis PRD, batas bawah, bobotIP, status lulus; konversi angka tanpa pembulatan tampilan |
| AssessmentRules | Helper internal bersama: total bobot tepat100, ID unik, asumsi dikenal, konflik asumsi/aktual ditolak agar skenario ditinjau |
| GradeCalculator | Hitung flat atau huruf langsung; hasil asal dan efektif terpisah; item bobot positif kosong membuat nilai akademik belum lengkap |
| ScoreFraction | Helper internal pecahan numerator/denominator BigDecimal; menjaga keputusan batas tetap eksak untuk skor berulang |
| TargetPlanner | K kontribusi tetap, W bobot bebas, x=(T−K)/W; satu saran normalisasi sama bagi semua bebas; tidak memutasi sumber |
| TargetPlan | Status kelayakan, raw x, saran aman, K/W, batas akhir minimum/maksimum, flag kehadiran |
| CourseResult / SemesterGpaService | Draf SKS/hasil boleh kosong, semua masalah dikumpulkan; IP ditahan jika ada masalah, bukan IP parsial |
| GpaResult | NilaiIP/delta/status target Optional, label aktual/simulasi, daftar masalah; dua desimal hanya format |
| DecimalInput | Koma/titik desimal diterima, tanpa pemisah ribuan atau notasi ilmiah agar parsing konsisten |

## Contoh yang bisa dijelaskan saat presentasi

Tugas20%, UTS30%, UAS50%, target60. Awalnya K=0, W=1, maka x=60 untuk semua. Kunci tugas80: K=16, W=0,8, x=55. Isi aktual UTS30: K=25, W=0,5, x=70. Hasil aktual80/30/70 menghasilkan60/C. `ScenarioAssumptions` tidak dapat menimpa aktual; konflik ditolak untuk ditinjau, bukan diam-diam memilih salah satu.

Huruf langsung hanya menyimpan huruf, tanpa angka fiktif. Kedua mode menghasilkan `GradeResult`. Tidak ada interface strategi tambahan karena dua method pada satu service sudah cukup untuk M1. `record`, enum, encapsulation melalui constructor, List/Map, composition, serta exception dipakai sesuai kebutuhan; tidak ada hierarki inheritance yang dipaksakan.

## Presisi dan validasi

Input adalah desimal eksak. Pembagian seperti2/3 berulang; jika setiap normalisasi dibulatkan lalu dijumlahkan, nilai yang tepat60 dapat salah menjadi sedikit di bawah60. `ScoreFraction` menambahkan pecahan tanpa pembagian, lalu membandingkan numerator dengan ambang×denominator. Grade weighted dan status solver memakai pembandingan eksak itu. Angka keluaran berulang menggunakan `MathContext.DECIMAL128` (34digit), bukan double. Saat UI lengkap dibuat, beri tanda≈/cukup digit untuk nilai dekat batas; jangan mengandalkan format2desimal untuk konversi ulang.

Saran aman dibulatkan **ke atas2desimal** pada skala100. Ini pilihan ketelitian input M1, contoh56,666… menjadi56,67; 56,66 tetap69,996/C+. Saran adalah nilai normalisasi, bukan angka perolehan pada skala asal. Konversi ke skala perolehan jika nanti ditampilkan perlu ketelitian input yang eksplisit.

Mustahil jika kebutuhan eksak>100: statusIMPOSSIBLE, raw x tetap tersedia dan saran kosong. Jika x≤0: saran0, target sudah aman dari kontribusi terkunci. Jika W=0: banding K dengan T tanpa pembagian. Bobot tidak sah dan input di luar rentang melempar IllegalArgumentException dengan pesan Indonesia. Draf boleh disimpan pada M4, tetapi tidak boleh dikalkulasi sampai struktur valid.

IP memakai seluruh mata kuliah, termasuk SKS D/E. Missing/invalid SKS atau hasil efektif kosong mengeluarkan `GpaResult` tanpa angka/delta/keputusan target serta daftar masalah. E karena kehadiran boleh menyelesaikan hasil efektif meski nilai akademik belum lengkap (PRD GPA-01). Target dibanding melalui `weightedPoints − target×totalCredits`, sebelum pembagian, sehingga tampilan2,73 dari30/11 tetap belum mencapai target2,73. Perubahan kehadiran menghitung objek hasil baru; nilai asal pada input tidak berubah.

## Batas implementasi

Secara historis, M1 belum mempunyai auth/persistence dan hanya menguji JDBC in-memory serta CommonMark sebagai smoke test. M2 sekarang mempunyai UserProfile, Semester, Course, AuthService/PBKDF2 dan SQLite dengan tes persistensi pada file sementara. M3 menambahkan Material/reader; M4 menambahkan aktual dan Scenario tersimpan. Reader M3 diuji terpisah dengan tes keamanan/JavaFX. JavaFX WebView21.0.10 ditambahkan pada M3. Tidak ada parser Markdown sendiri, FXML, ORM, server, koneksi cloud, AI atau paid service.

## Aliran M2
JavaFX AuthView/WorkspaceView/AcademicView → UiTasks worker → AuthService/AcademicService → interface repository → SqliteRepository → SqliteDatabase → SQLite lokal. Password hash berjalan pada worker yang sama; JavaFX thread hanya membaca input, menampilkan hasil dan dialog konfirmasi. Root form dinonaktifkan selama satu operasi untuk mencegah pergantian akun ketika request lama belum selesai.

| Class M2 | Tanggung jawab |
|---|---|
| UserProfile / Semester / Course | Data immutable; Semester memvalidasi nomor/tahun/periode, Course memvalidasi nama/SKS/acuan. Course menyimpan Attendance M1 dan revision |
| UserSession | NIM akun yang login di memori; requireNim menolak operasi setelah logout. Sesi tidak dipersistenkan |
| PasswordHasher / PasswordHash | PBKDF2WithHmacSHA256 bawaan Java, 600.000 iterasi, SecureRandom salt 16 byte, output 256 bit. Salt/hash Base64 beserta algoritme/iterasi disimpan. Verifikasi MessageDigest.isEqual; PBEKeySpec dibersihkan |
| AuthService | Validasi registrasi, trim NIM tanpa parse angka, konfirmasi password, login/logout, edit profil tanpa method mengubah NIM. Buffer char[] dibersihkan dalam finally |
| AcademicService | Mengambil pemilik dari sesi; memvalidasi masukan sebelum repository. Tidak memuat SQL/JavaFX |
| AccountRepository / AcademicRepository | Kontrak penyimpanan dua area; tidak ada generic repository/ORM tambahan |
| SqliteRepository | PreparedStatement untuk seluruh input pengguna; setiap query akademik menyertakan owner_nim dan ID. Mutasi diselesaikan sebelum status berhasil UI |
| SqliteDatabase | Satu connection diserialkan; FK aktif, busy_timeout 3 detik, transaksi commit/rollback, initialization versi 1 dan pemeriksaan integritas. Tidak menghapus/reset saat error |
| AppPaths | %LOCALAPPDATA%/TutupBuku/tutup-buku.db; fallback user.home/AppData/Local. TUTUP_BUKU_DATA_DIR untuk folder demo terisolasi |
| UiTasks / FormSupport | Worker tunggal, callback JavaFX thread, status simpan/gagal, form tetap saat error, konfirmasi hapus dengan Batal sebagai default |
| DashboardView | Shell dua pilar M1 dipindahkan ke dashboard setelah login. CalculationDemoView tetap fixture M1; runtime login memakai GradeView dengan course yang tersimpan |

## Skema versi 1
- users: nim TEXT primary key; name/study_program; algorithm/iterations/salt/hash. NIM tidak menjadi angka dan tidak berisi semester.
- semesters: id, owner_nim FK, semester_number, academic_year, period. UNIQUE(owner_nim, academic_year, period) dan UNIQUE(id, owner_nim).
- active_semesters: owner_nim primary key dan FK user; composite FK(semester_id, owner_nim) memastikan referensi milik akun. Tabel terpisah menghindari FK melingkar.
- courses: id, owner_nim, semester_id, name, code, credits TEXT, attendance_met/confirmed/reference, revision. Composite FK ke semester; SKS disimpan dari BigDecimal.toPlainString agar desimal tidak berubah menjadi floating point.
- ON DELETE CASCADE menghapus course serta referensi aktif ketika semester dihapus dalam satu transaksi. Semester lain/akun lain tetap ada. Jika semester aktif dihapus, tidak memilih semester pengganti otomatis.
- PRAGMA user_version=1. Versi0 hanya diinisialisasi jika tidak berisi tabel pengguna. Versi lebih baru, schema tak dikenal, atau database rusak ditolak tanpa reset. Tidak ada migrasi destruktif.
- Tidak ada tabel assessment, grade atau scenario sebelum M4. Nilai kosong belum menjadi E; Course.withoutGrade menghasilkan input engine M1 yang belum lengkap. Attendance course dapat langsung dipakai GradeCalculator M1.

## Keputusan dan batas keamanan
PBKDF2 dipilih sesuai rekomendasi PRD dan tersedia di JDK tanpa library tambahan; bukan algoritme buatan sendiri. Parameter mengikuti [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html). JavaFX PasswordField menyediakan String di memori; field dibersihkan setelah berhasil/logout, buffer service dibersihkan, tetapi Java tidak menjamin semua salinan String terhapus seketika. Tidak ada password/plaintext di database atau log. Login gagal menyampaikan NIM atau password salah, bukan password yang dicoba.

Isolasi ini berlaku melalui aplikasi dan FK, bukan enkripsi database atau perlindungan terhadap orang yang dapat membaca/mengubah file melalui akun Windows yang sama. Akun tidak diverifikasi ke kampus, tidak ada email/cloud, recovery universal, atau penyimpanan sesi otomatis.

Form M2 memakai Simpan eksplisit. Jika save gagal, input tetap dan tombol Simpan dapat dicoba lagi. Refresh daftar adalah aksi terpisah; gagal refresh setelah commit tidak dilaporkan sebagai gagal menyimpan. Tahun akademik memakai YYYY/YYYY berurutan; prodi bebas dan boleh kosong hingga diisi. Layar semester terpilih untuk dilihat tidak otomatis menjadi semester aktif.

## Aliran M3 dan class penting
LearningView/MaterialEditor → LearningService → LearningRepository → SqliteLearningRepository/AssetStore. UiTasks tetap satu worker dan menghitung operasi blocking yang masih berjalan; pekerjaan bookmark ringan memakai antrean yang sama tanpa mengunci form. Pemilik bookmark ditangkap saat request dibuat agar antrean lama tidak berganti akun.

| Class | Peran |
|---|---|
| LearningLocation / LearningFolder | Kind CHAPTER atau INTEREST, parent ID, nama/urutan; bab mempunyai course, bidang independen dari semester/course |
| Material / MaterialReading | Materi immutable berisi markdown/source/urutan/revisi/timestamp; reading membungkus completion/note/noteRevision terpisah |
| LearningProgress | Jumlah selesai/total, empty state tanpa pembagian; course menghitung semua materi dalam semua bab |
| ReadingBookmark / ResumeResult | Material ID, content revision, scroll ratio dan lokasi terakhir; hasil resume membawa pesan/fallback |
| EditorDraft | Buffer mutable yang memisahkan data tersimpan dari isian; generation dan ack mencegah hasil lama menghilangkan edit baru |
| LearningSchema | Migrasi aditif dan atomik1→2, validasi tabel/kolom baru |
| SqliteLearningRepository | Query owner+ID, CRUD/order, CAS save bundle materi+note, progress, bookmark, starter initialization dalam transaksi |
| AssetStore | Decode PNG/JPEG, batas10MB/16MP, UUID, direktori SHA256(NIM), resolve/realpath guard, copy atomik dan data URI untuk renderer |
| MarkdownRenderer | Parser CommonMark/GFM table yang sudah ada; visitor membatasi link/gambar, escape rawHTML, template CSS/CSP lokal |
| MarkdownReader | WebView script halaman disabled, tanpa bridgeJava. DOM click membuka link http/https via HostServices setelah klik; tidak memuat halaman luar |
| MaterialEditor | Tulis/Preview, buffer, status simpan, autosave2detik, Save/Discard/Cancel, note/completion, restore/capture scroll |
| LearningView | Sidebar semester/course atau peminatan, folder/material lists, CRUD/ordering, progress, starter button dan resume |

## Skema versi2 dan migrasi
Tabel M2 tidak dihapus/diganti. Migrasi menambah unique index courses(id,owner_nim), learning_chapters, interest_fields, materials, learning_state, last_reading dan learning_seed lalu menetapkan user_version2 dalam satu transaksi. Jika gagal, rollback; data akun/password/profil/semester/course/active semester tetap. Versi lebih baru/tidak dikenal tetap ditolak, tanpa reset.

materials memiliki chapter_id ATAU field_id dengan CHECK XOR dan composite FK parent+owner. learning_state memiliki unique material+owner untuk note/completed/note_revision, cascade saat materi dihapus. courses→chapters→materials→state memakai FK cascade; interest terpisah. Konfirmasi hapus course/semester diperluas untuk menjelaskan materi/catatan/progres yang ikut terhapus.

last_reading memiliki FK pemilik, tetapi material_id sengaja tidak di-FK agar ID terakhir menjadi tombstone setelah delete. Resume mencari material dengan owner yang sama; jika hilang menampilkan pesan lalu daftar yang masih tersedia. learning_seed adalah marker unik per akun, dibuat bersama5salinan contoh dalam satu transaksi setelah tombol salin dikonfirmasi.

## Save, revisi dan navigasi
Simpan/auto-save mengirim snapshot EditorDraft ke worker tunggal. UPDATE memakai expected material revision dan note revision. Judul/markdown/source yang berubah menaikkan material revision/timestamp; note-only update hanya menaikkan note revision. Material dan note disimpan dalam transaksi yang sama, completion terpisah. Reorder mengubah position, bukan content revision, sehingga note dan resume tidak hilang.

Ack sukses memperbarui base dan menandai generasi yang tersimpan. Error tidak memanggil reset/ack, sehingga input tetap. Autosave2detik berhenti setelah error sampai pengguna mencoba lagi atau mengedit. Navigasi di learning, tab utama, tab simulasi, logout dan close memakai dialog Simpan/Buang/Batal. Save gagal menahan navigasi. Batal mempertahankan editor; Buang kembali ke snapshot tersimpan.

Bookmark didebounce700ms pada scroll/mouse/key, serta di-flush sebelum meninggalkan bacaan. Restorasi dilakukan setelah WebEngine SUCCEEDED. Rasio0–1 menyesuaikan panjang viewport; content revision berubah mengembalikan ke0. Waktu disimpan UTC (Instant) dan ditampilkan WIB.

## Rendering dan aset
HTML raw di-escape oleh CommonMark, bukan dibersihkan dengan regex buatan sendiri. Tidak ada kode materi dieksekusi. Script halaman WebView dimatikan. executeScript hanya digunakan untuk ekspresi tetap milik aplikasi yang membaca/mengatur posisi gulir dengan angka rasio; tidak pernah memasukkan markdown atau teks pengguna. Tidak ada Java object bridge ke halaman.

Image nodes diubah menjadi dataURI dari PNG/JPEG pemilik yang telah lolos validasi, atau teks placeholder berisi referensi yang hilang/ditolak. Renderer menolak remote images, file://, traversal, dataURI buatan pengguna dan aset akun lain. CSP default-src none, img-src data, style-src inline milik aplikasi membatasi pemuatan resource. Link hanya http/https absolut tanpa credentials; DOM listener preventDefault lalu membuka browser melalui callback setelah klik. Formula memakai gambar, tanpa LaTeX/video/PDF/AI/kuis.

File dipilih lewat chooser, didekode ImageIO, disalin ke assets/{SHA256-NIM}/{UUID}.png/jpg, lalu referensi relatif dimasukkan ke markdown. NIM tidak dipakai langsung sebagai path. Resolve memeriksa pola, pemilik, normalized path dan realpath. Tidak ada penghapusan aset setelah edit/delete materi; orphan files mungkin tersisa dan cleanup ditunda. Salin folder database dan assets bersama jika membuat cadangan manual; referensi tidak bergantung pada lokasi file gambar asli.

## Aliran M4

GradeView → ActualGradePane/ScenarioPane → UiTasks → GradePlanningService → GradeRepository/SqliteGradeRepository → SqliteDatabase. GradePlanningService menggabungkan data dengan **GradeCalculator, TargetPlanner, SemesterGpaService M1 yang tidak diubah**. UI hanya membaca/memformat input, mengatur form dan dialog. Tidak ada SQL atau rumus solver/IP di handler.

| Class M4 | Peran |
|---|---|
| EvaluationMode | DETAIL atau DIRECT; tepat satu mode aktif |
| AssessmentComponent | UUID, nama, bobot, perolehan Optional, maksimum Optional, urutan. Maksimum boleh disiapkan sebelum aktual; item() mengadaptasi ke AssessmentItem M1 |
| ActualCourse | Course terkini, mode/huruf aktual, daftar komponen immutable; tidak mempunyai asumsi solver |
| ScenarioCourse | Salinan ActualCourse sebagai basis + mode aktif skenario, estimasi huruf, target angka/huruf, kehadiran dan map kunci normalisasi |
| SemesterScenario | Nama, semester, target IP, version untuk CAS, ruleVersion, waktu UTC dan daftar snapshot course |
| ScenarioReview | Perubahan struktur/revisi dibanding data live; dipakai penanda usang dan dialog review |
| ScenarioEvaluation | Hasil per-course dengan TargetPlan/GradeResult serta satu GpaResult lengkap dan review |
| GradePlanningService | CRUD melalui repository, evaluasi aktual/skenario, perbandingan basis, duplikasi, deteksi perubahan, refresh draft |
| SqliteGradeRepository | SQL berparameter dibatasi owner+ID; mutasi aktual + revision atomik, save header/children snapshot atomik dan CAS |
| GradeSchema | Migrasi aditif 2→3 dalam transaksi dan validasi kolom |
| GradeView / ActualGradePane | Pemilihan semester, editor komponen/urutan/skor earned-max, mode/huruf aktual dan konfirmasi kehadiran |
| ScenarioPane / GradeForms | Form snapshot/asumsi, kunci, target, hasil IP, review/refresh, CRUD/duplikasi/perbandingan, Save/Discard/Cancel |

### Aktual, asumsi dan hasil

ActualCourse dibaca dari courses, assessment_components, course_actual_grades. Nilai kosong menghasilkan Optional.empty, bukan0. Input earned/max dipertahankan, normalisasi memakai Score M1. Total bobot salah boleh disimpan; hasil detail menjadi draf belum sah. Mode DIRECT tidak memerlukan struktur detail100%; mode DETAIL tidak menghitung huruf aktual yang tersimpan sebagai data mode tidak aktif.

Evaluasi skenario detail menjalankan TargetPlanner pada snapshot actual + lockedScores, lalu menggabungkan **map sementara** kunci dan saran untuk GradeCalculator. Tidak ada perintah save aktual dalam jalur ini. Saran >100 tidak dibuat: status mustahil/raw kebutuhan tetap terlihat; hasil akademik tidak lengkap kecuali ada effective E karena kehadiran. W=0 memakai status engine, tanpa pembagian nol.

Mode huruf skenario memakai huruf aktual pada snapshot bila tersedia; jika tidak, memakai estimatedLetter milik skenario. Tidak pernah mengubah huruf menjadi skor angka rekaan. Mengganti mode tidak menghapus data mode lain. Saran/kunci detail tidak dihitung dalam mode huruf.

Attendance berisi met/confirmed. Perubahan skenario dari data aktual adalah asumsi belum terverifikasi. Override E mempertahankan underlyingLetter/underlyingScore. Jika semua grade dan kehadiran telah aktual/terverifikasi, IP berlabel Berdasarkan Input Aktual; selain itu IP Simulasi. IP historis selalu diberi penanda snapshot usang.

### Skema versi3

- assessment_components: id UUID, owner_nim/course_id composite FK→courses, nama/bobot/earned/maximum/position; nilai desimal TEXT.
- course_actual_grades: satu mode aktif dan Optional letter per owner/course; kedua mode tidak saling menghapus.
- grade_scenarios: header owner/semester/name/target_gpa/version/rule_version/updated_at; FK owner+semester.
- scenario_courses: snapshot nama/kode/SKS/revision/attendance, mode/huruf aktual, mode/estimasi/target/attendance skenario.
- scenario_items: salinan komponen dan earned/max, locked_score terpisah serta suggested_score hasil engine saat save.

Snapshot course **tidak mempunyai FK ke course live**: course yang dihapus tetap terbaca historis. FK snapshot→scenario dan scenario→semester tetap CASCADE. Hapus course menghapus aktual+materi turunan dan menjadikan skenario usang; hapus semester menghapus seluruh skenario semester setelah konfirmasi. Tidak ada cleanup assets.

Snapshot disimpan sebagai tabel relasional agar mudah diinspeksi dan tidak memerlukan parser/serializer JSON baru. GradeRepository memeriksa course baru berasal dari semester/pemilik yang sesuai; snapshot historis yang diduplikasi harus berasal dari data tersimpan pemilik yang sama.

### Revisi dan review eksplisit

Setiap edit aktual, komponen/bobot/urutan atau course/SKS menaikkan courses.revision. Skenario menyimpan revisi basis dan set ID course. review membandingkan set/revisi: course ditambah/dihapus, SKS, identitas, komponen/nilai atau kehadiran berubah. Menyimpan ulang data yang sama boleh tetap menandai revisi baru (pilihan konservatif).

Open/Hitung tidak mengganti basis. Dialog Tinjau/perbarui menjelaskan perubahan. refresh membuat **draft baru di memori** dengan basis terkini, mempertahankan mode/target dan kunci pada komponen yang masih unknown dengan bobot positif, membuang kunci yang kini aktual/terhapus. Estimasi huruf dibuang ketika huruf aktual baru tersedia. Course baru ditambah, course dihapus dikeluarkan. Pengguna meninjau lalu Simpan; refresh sendiri tidak menulis database.

Duplikasi membuat header dan baris snapshot baru dengan ID independen. Save existing memakai WHERE id+owner+expected version, lalu mengganti children dalam satu transaksi. Versi berubah/gagal insert menyebabkan rollback header dan children. Draft/form tetap agar retry tidak kehilangan input; konflik versi meminta buka ulang.

Target IP0–4 hanya dibandingkan manual. Perbandingan dua snapshot tersimpan menunjukkan union course, SKS kedua sisi, huruf efektif dan IP. Semester/set/revisi/aturan berbeda atau usang ditandai, tanpa mengganti salah satu basis. Tidak ada optimasi antar-course atau IPK.

### Batas dan pemulihan

Data milik akun berasal dari UserSession, tidak dari field UI. Semua mutasi memakai owner dan ID; FK menguatkan isolasi. UiTasks menjaga satu worker dan memblok pergantian sesi saat operasi. Simpan gagal mempertahankan form; navigasi dengan pilihan Simpan yang gagal ditahan. Form aktual dan skenario tetap terpisah ketika berpindah subtab.

Migrasi2→3 hanya membuat tabel/index baru, menetapkan user_version3 di transaksi yang sama. Collision/gagal DDL rollback ke2; tidak mereset akun, belajar, catatan/progres atau mengganti database. RULE_VERSION=1 memisahkan versi rumus dari versi skema agar perubahan kebijakan berikutnya dapat ditandai.

Grouped weighting, backup/restore UI, IPK, optimasi kombinasi, PDF, AI, kuis, packaging/benchmark/usability tetap di backlog. M4 tidak menambah dependency aplikasi; formatter lokal hanya alat pengembangan pada .tools yang diabaikan.
