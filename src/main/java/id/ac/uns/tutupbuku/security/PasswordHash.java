package id.ac.uns.tutupbuku.security;

/** Salt/hash Base64 dan parameter algoritme; tidak pernah memuat password asli. */
public record PasswordHash(String algorithm, int iterations, String salt, String hash) {
    @Override public String toString() { return "PasswordHash[redacted]"; }
}
