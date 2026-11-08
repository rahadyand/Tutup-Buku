package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.UserProfile;
import id.ac.uns.tutupbuku.persistence.AccountRepository;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import java.util.Arrays;

public final class AuthService {
    private final AccountRepository accounts;
    private final PasswordHasher hasher;
    private final UserSession session;
    public AuthService(AccountRepository accounts, PasswordHasher hasher, UserSession session) {
        this.accounts = accounts; this.hasher = hasher; this.session = session;
    }

    /** Buffer milik pemanggil dibersihkan setelah proses, juga saat validasi/simpan gagal. */
    public void register(String nim, String name, char[] password, char[] confirmation) {
        try {
            nim = required(nim, "NIM"); name = required(name, "Nama");
            if (password.length < 8) throw new IllegalArgumentException("Password minimal 8 karakter.");
            if (!Arrays.equals(password, confirmation)) throw new IllegalArgumentException("Konfirmasi password tidak sama.");
            if (accounts.findAccount(nim).isPresent()) throw new IllegalArgumentException("NIM sudah terdaftar pada perangkat ini.");
            accounts.createAccount(nim, name, hasher.hash(password));
        } finally { Arrays.fill(password, '\0'); Arrays.fill(confirmation, '\0'); }
    }

    public UserProfile login(String nim, char[] password) {
        // Login gagal tidak boleh meninggalkan sesi akun sebelumnya.
        session.logout();
        try {
            if (nim == null || nim.trim().isEmpty() || password.length == 0) throw invalidLogin();
            var account = accounts.findAccount(nim.trim()).orElseThrow(AuthService::invalidLogin);
            if (!hasher.verify(password, account.passwordHash())) throw invalidLogin();
            session.login(account.profile().nim());
            return account.profile();
        } finally { Arrays.fill(password, '\0'); }
    }

    public void logout() { session.logout(); }
    public UserProfile profile() { return accounts.getProfile(session.requireNim()); }
    public void updateProfile(String name, String studyProgram) {
        accounts.updateProfile(session.requireNim(), required(name, "Nama"), studyProgram == null ? "" : studyProgram.trim());
    }
    private static IllegalArgumentException invalidLogin() { return new IllegalArgumentException("NIM atau password salah."); }
    private static String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " wajib diisi.");
        return value.trim();
    }
}
