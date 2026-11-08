# Konvensi Tutup Buku

- Sumber produk: baca seluruh `PRD_Tutup_Buku_v1.1.pdf`; aturan pengguna dalam chat mengatur lingkup pekerjaan. Bedakan P0, P1 bersyarat, P2, rekomendasi, dan asumsi.
- UI, dokumentasi dan penjelasan berbahasa Indonesia. Nama class/method/variable memakai bahasa Inggris yang jelas.
- Java 21, JavaFX 21, Maven Wrapper. Versi persis di `pom.xml` dan `.mvn/wrapper/maven-wrapper.properties`. Jangan tambah framework tanpa kebutuhan nyata.
- `domain`: model immutable dan validasi; `service`: kalkulasi murni tanpa JavaFX/JDBC; `ui`: tampilan; `persistence` baru dibuat saat M2. Jangan letakkan rumus di event handler.
- Gunakan BigDecimal dari String, jangan double untuk nilai/bobot/SKS. Jangan membulatkan tampilan sebelum konversi huruf atau pembandingan target. Bobot total harus tepat 100, termasuk 99,99/100,01 tidak sah.
- Nilai kosong bukan nol. Aktual tidak pernah diubah oleh solver/asumsi. Bobot nol tidak membutuhkan nilai dan tidak ikut solver. Target >100 harus dilaporkan tidak mungkin.
- Kehadiran awal terpenuhi sebagai asumsi belum terverifikasi; override E harus mempertahankan hasil asal. IP hanya untuk seluruh mata kuliah dengan SKS/hasil efektif sah.
- Simpan password hanya sebagai hash bersalt beserta parameter; gunakan query berparameter dan isolasi pemilik pada M2. NIM teks trim, nol awal tetap. Jangan log password.
- Materi kuliah dan peminatan terpisah; gunakan library Markdown, gambar lokal; raw HTML/script dinonaktifkan saat reader dibuat.
- Perbarui `docs/DEVELOPMENT_STATUS.md` setelah perubahan bermakna. Uji dengan `./mvnw.cmd verify` (PowerShell: `.\mvnw.cmd verify`). Jangan mengklaim fitur/tes manual yang belum dijalankan.
- Pertahankan berkas pengguna. `src/App.java` adalah starter lama, di luar source root Maven. Salinan konfigurasi awal ada di docs.
- Jangan mengerjakan milestone berikutnya tanpa permintaan pengguna. P1 hanya setelah gerbang P0; semua penundaan tetap di backlog.
