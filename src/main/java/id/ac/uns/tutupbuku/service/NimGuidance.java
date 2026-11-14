package id.ac.uns.tutupbuku.service;

/** Panduan dari konvensi pengguna, bukan validasi akun atau verifikasi kampus. */
public final class NimGuidance {
    private NimGuidance() {}

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
}
