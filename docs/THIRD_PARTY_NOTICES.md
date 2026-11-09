# Komponen distribusi Tutup Buku 0.5.0-rc1

Distribusi membawa salinan teks lisensi pada folder licenses dan runtime/legal. Pertahankan folder tersebut ketika menyalin paket.

| Komponen | Versi | Lisensi / sumber |
|---|---|---|
| Eclipse Temurin / OpenJDK |21.0.13+9 Windows x64|GPLv2 dengan Classpath Exception dan pemberitahuan pihak ketiga; runtime/legal, [sumber/biner resmi](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.13%2B9), [FAQ](https://adoptium.net/docs/faq) |
| OpenJFX base/graphics/controls/media/web |21.0.10 Windows x64|GPLv2+Classpath Exception dan legal pihak ketiga pada setiap module; licenses/javafx.*, [OpenJFX](https://openjfx.io/), [sumber](https://github.com/openjdk/jfx/tree/jfx21) |
| Xerial SQLite JDBC |3.51.0.0|Apache2.0 dan pemberitahuan Zentus/SQLite bawaan; licenses/sqlite-jdbc-*, [sumber](https://github.com/xerial/sqlite-jdbc/tree/3.51.0.0) |
| CommonMark Java + GFM tables |0.24.0|BSD2-Clause; licenses/commonmark-*, [sumber](https://github.com/commonmark/commonmark-java/tree/commonmark-parent-0.24.0) |

Maven/Wrapper dan JUnit hanya alat build/test, tidak dibawa ke paket. Materi contoh adalah tulisan asli singkat proyek; materi/gambar pribadi pengguna tidak dikemas. Gambar yang diimpor pengguna tetap membutuhkan hak penggunaan yang sesuai.

Script packaging memverifikasi SHA256 JDK dan SDK legal yang dikunci, menyimpan hash JAR pada BUILD-INFO dan hash file pada SHA256SUMS. Teks legal OpenJFX diambil dari SDK resmi versi yang sama dengan JAR Maven. Tidak ada perubahan terhadap kode biner library.
