package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class LocalAccountTest {
    @TempDir Path directory;
    private SqliteDatabase database;
    private SqliteRepository repository;
    private UserSession session;
    private AuthService auth;
    @BeforeEach void open() {
        database = new SqliteDatabase(directory.resolve("accounts.db"));
        repository = new SqliteRepository(database);
        session = new UserSession();
        auth = new AuthService(repository, new PasswordHasher(), session);
    }
    @AfterEach void close() { database.close(); }
    private char[] password() { return "sandi-uji-lokal".toCharArray(); }
    private void register(String nim) { auth.register(nim, "Mahasiswa Uji", password(), password()); }

    @Test void nimIsTextTrimmedUniqueAndPreservesLeadingZeros() {
        register("  001234  ");
        assertEquals("001234", repository.getProfile("001234").nim());
        assertThrows(IllegalArgumentException.class, () -> register("001234 "));
        assertTrue(repository.findAccount("1234").isEmpty());
        assertEquals("001234", auth.login(" 001234 ", password()).nim());
    }
    @Test void correctLoginIncorrectLoginAndLogout() {
        register("001");
        assertThrows(IllegalArgumentException.class, () -> auth.login("001", "salah".toCharArray()));
        String knownError = assertThrows(IllegalArgumentException.class, () -> auth.login("001", "salah".toCharArray())).getMessage();
        String unknownError = assertThrows(IllegalArgumentException.class, () -> auth.login("missing", password())).getMessage();
        assertEquals(knownError, unknownError);
        auth.login("001", password());
        assertEquals("001", session.requireNim());
        auth.logout();
        assertThrows(IllegalStateException.class, auth::profile);
        auth.login("001", password());
        assertThrows(IllegalArgumentException.class, () -> auth.login("001", "wrong".toCharArray()));
        assertThrows(IllegalStateException.class, session::requireNim);
    }
    @Test void passwordConfirmationAndMinimumLengthAreValidatedWithoutCreatingAccount() {
        assertThrows(IllegalArgumentException.class, () -> auth.register("001", "Nama", password(), "berbeda".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> auth.register("001", "Nama", "short".toCharArray(), "short".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> auth.register(" ", "Nama", password(), password()));
        assertThrows(IllegalArgumentException.class, () -> auth.register("001", " ", password(), password()));
        assertTrue(repository.findAccount("001").isEmpty());
    }
    @Test void storedHashesHaveUniqueSaltsAndNoPlaintext() {
        register("001"); register("002");
        var one = repository.findAccount("001").orElseThrow().passwordHash();
        var two = repository.findAccount("002").orElseThrow().passwordHash();
        assertEquals(PasswordHasher.ALGORITHM, one.algorithm());
        assertEquals(600_000, one.iterations());
        assertNotEquals(one.salt(), two.salt()); assertNotEquals(one.hash(), two.hash());
        assertNotEquals(new String(password()), one.hash());
        assertFalse(one.toString().contains(one.hash()));
        database.read(connection -> {
            try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT * FROM users")) {
                while (result.next()) for (int i = 1; i <= result.getMetaData().getColumnCount(); i++) {
                    assertNotEquals(new String(password()), result.getString(i));
                }
            }
            return null;
        });
    }
    @Test void passwordBuffersAreClearedEvenOnFailure() {
        char[] original = password(); char[] confirmation = password();
        auth.register("001", "Nama", original, confirmation);
        assertArrayEquals(new char[original.length], original);
        assertArrayEquals(new char[confirmation.length], confirmation);
        char[] wrong = "wrong".toCharArray();
        assertThrows(IllegalArgumentException.class, () -> auth.login("001", wrong));
        assertTrue(Arrays.equals(new char[wrong.length], wrong));
    }
    @Test void profileEditKeepsNimAndIsIsolated() {
        register("001"); register("002"); auth.login("001", password());
        auth.updateProfile(" Nama Baru ", " Informatika ");
        assertEquals("001", auth.profile().nim()); assertEquals("Nama Baru", auth.profile().name());
        assertEquals("Informatika", auth.profile().studyProgram());
        assertEquals("Mahasiswa Uji", repository.getProfile("002").name());
        assertThrows(IllegalArgumentException.class, () -> auth.updateProfile(" ", "X"));
        assertEquals("Nama Baru", auth.profile().name());
    }
    @Test void loginAndProfileSurviveReopeningDatabase() {
        register("001"); auth.login("001", password()); auth.updateProfile("Nama Setelah Edit", "Sains Data");
        database.close();
        database = new SqliteDatabase(directory.resolve("accounts.db"));
        var reopened = new AuthService(new SqliteRepository(database), new PasswordHasher(), new UserSession());
        assertEquals("Sains Data", reopened.login("001", password()).studyProgram());
        assertEquals("Nama Setelah Edit", reopened.profile().name());
    }
    @Test void quotedNimIsNotExecutedAsSql() {
        String nim = "00' OR '1'='1";
        register(nim);
        assertEquals(nim, auth.login(nim, password()).nim());
        assertThrows(IllegalArgumentException.class, () -> auth.login("' OR 1=1 --", password()));
    }
}
