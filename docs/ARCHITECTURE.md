# Penjelasan fondasi M1

## Aliran sederhana

UI membuat contoh data → memanggil service → menerima objek hasil → menampilkan teks. Service tidak mengenal JavaFX/database. Pada M2 repository akan memuat model yang sama dari SQLite; tidak ada repository dummy atau framework tambahan sekarang.

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

Mustahil jika kebutuhan eksak>100: statusIMPOSSIBLE, raw x tetap tersedia dan saran kosong. Jika x≤0: saran0, target sudah aman dari kontribusi terkunci. Jika W=0: banding K dengan T tanpa pembagian. Bobot tidak sah dan input di luar rentang melempar IllegalArgumentException dengan pesan Indonesia. Draf boleh disimpan nantinya, tetapi tidak boleh dikalkulasi sampai struktur valid.

IP memakai seluruh mata kuliah, termasuk SKS D/E. Missing/invalid SKS atau hasil efektif kosong mengeluarkan `GpaResult` tanpa angka/delta/keputusan target serta daftar masalah. E karena kehadiran boleh menyelesaikan hasil efektif meski nilai akademik belum lengkap (PRD GPA-01). Target dibanding melalui `weightedPoints − target×totalCredits`, sebelum pembagian, sehingga tampilan2,73 dari30/11 tetap belum mencapai target2,73. Perubahan kehadiran menghitung objek hasil baru; nilai asal pada input tidak berubah.

## Batas implementasi

Model User/Semester/Material/Scenario tersimpan, AuthService/PBKDF2, persistence dan reader tidak dibuat pada M1. Smoke test hanya memuat driver JDBC in-memory dan CommonMark+table/code/image/link/escaped HTML; ini bukan bukti penyimpanan akun atau keamanan reader end-to-end. JavaFX WebView baru ditambahkan saat reader M3 dibangun. Tidak ada parser Markdown sendiri, FXML, ORM, server, koneksi cloud, AI atau paid service.
