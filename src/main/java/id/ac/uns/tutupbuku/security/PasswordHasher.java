package id.ac.uns.tutupbuku.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class PasswordHasher {
    public static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int ITERATIONS = 600_000;
    private final SecureRandom random = new SecureRandom();

    public PasswordHash hash(char[] password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        byte[] result = derive(password, salt, ITERATIONS);
        try {
            return new PasswordHash(ALGORITHM, ITERATIONS, Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(result));
        } finally { Arrays.fill(result, (byte) 0); }
    }

    public boolean verify(char[] password, PasswordHash stored) {
        try {
            if (!ALGORITHM.equals(stored.algorithm()) || stored.iterations() < ITERATIONS || stored.iterations() > 10_000_000) {
                throw new IllegalArgumentException("Parameter hash akun tidak didukung. Data tidak diubah.");
            }
            byte[] salt = Base64.getDecoder().decode(stored.salt());
            byte[] expected = Base64.getDecoder().decode(stored.hash());
            if (salt.length != 16 || expected.length != 32) throw new IllegalArgumentException("Hash akun tidak valid. Data tidak diubah.");
            byte[] actual = derive(password, salt, stored.iterations());
            try { return MessageDigest.isEqual(expected, actual); }
            finally { Arrays.fill(actual, (byte) 0); Arrays.fill(expected, (byte) 0); }
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Hash akun tidak dapat dibaca. Data tidak diubah.", error);
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec specification = new PBEKeySpec(password, salt, iterations, 256);
        try { return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(specification).getEncoded(); }
        catch (GeneralSecurityException error) { throw new IllegalStateException("Algoritme password tidak tersedia pada Java ini.", error); }
        finally { specification.clearPassword(); }
    }
}
