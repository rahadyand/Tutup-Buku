package id.ac.uns.tutupbuku.service;

/** Panduan dari konvensi pengguna, bukan validasi akun atau verifikasi kampus. */
public final class NimGuidance {
    private NimGuidance() {}

    public static final String EXAMPLE =
            "Contoh L0125001: L fakultas · 01 Informatika · 25 angkatan 2025 · 001 identitas"
                    + " mahasiswa. Sains Data: L0225001 (kode 02).";

    public static String welcome(String name, String input) {
        String displayName = name == null ? "" : name.trim();
        String nim = input == null ? "" : input.trim();
        String greeting =
                displayName.isEmpty() ? "Selamat datang" : "Selamat datang, " + displayName;
        if (!nim.matches("L(01|02)[0-9]{5}"))
            return greeting
                    + "! Gunakan NIM akun lokal Anda. Lengkapi pola NIM untuk melihat prodi dan"
                    + " angkatan; bukan verifikasi kampus.";
        String program = nim.startsWith("L01") ? "Informatika" : "Sains Data";
        return greeting
                + (displayName.isEmpty() ? " di " : " dari ")
                + program
                + " angkatan 20"
                + nim.substring(3, 5)
                + "! Berdasarkan input, bukan verifikasi kampus.";
    }

    public static String describe(String input) {
        String nim = input == null ? "" : input.trim();
        if (nim.isEmpty())
            return "Pola panduan: L + kode prodi 2 digit + tahun masuk 2 digit + identitas 3"
                    + " digit.";
        if (!nim.matches("L[0-9]{7}"))
            return "Panduan: L diikuti 7 digit. NIM akun lokal lama tetap dapat dipakai; teks tidak"
                    + " diubah.";
        String program =
                switch (nim.substring(1, 3)) {
                    case "01" -> "Informatika";
                    case "02" -> "Sains Data";
                    default -> "kode prodi belum dikenali (panduan hanya 01/02)";
                };
        return "Berdasarkan pola: "
                + program
                + " · angkatan 20"
                + nim.substring(3, 5)
                + " · identitas "
                + nim.substring(5)
                + ". Bukan verifikasi kampus; profil tidak diubah otomatis.";
    }
}
