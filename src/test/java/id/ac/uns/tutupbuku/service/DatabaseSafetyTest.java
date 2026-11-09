package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseSafetyTest {
    @TempDir Path directory;
    @Test void corruptedFileIsNotResetOrReplaced() throws Exception {
        Path file = directory.resolve("corrupted.db");
        byte[] original = "Berkas bukan database SQLite. Jangan dihapus.".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, original);
        assertThrows(StorageException.class, () -> new SqliteDatabase(file));
        assertArrayEquals(original, Files.readAllBytes(file));
    }
    @Test void unknownVersionIsRefusedWithoutDataLoss() throws Exception {
        Path file = directory.resolve("newer.db");
        try (var database = new SqliteDatabase(file)) {
            database.read(connection -> { try (var statement = connection.createStatement()) { statement.execute("PRAGMA user_version=99"); } return null; });
        }
        byte[] original = Files.readAllBytes(file);
        assertThrows(StorageException.class, () -> new SqliteDatabase(file));
        assertArrayEquals(original, Files.readAllBytes(file));
    }
    @Test void unversionedExistingTablesAreNotOverwritten() throws Exception {
        Path file = directory.resolve("legacy.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE important_data(value TEXT)");
            statement.execute("INSERT INTO important_data VALUES('tetap ada')");
        }
        byte[] original = Files.readAllBytes(file);
        assertThrows(StorageException.class, () -> new SqliteDatabase(file));
        assertArrayEquals(original, Files.readAllBytes(file));
    }
    @Test void closedDatabaseDoesNotReturnEmptyDataAsSuccess() {
        var database = new SqliteDatabase(directory.resolve("closed.db")); database.close();
        assertThrows(StorageException.class, () -> new SqliteRepository(database).listSemesters("001"));
    }
    @Test void schemaIsVersionedAndForeignKeysEnabled() {
        try (var database = new SqliteDatabase(directory.resolve("schema.db"))) {
            database.read(connection -> {
                try (var statement = connection.createStatement()) {
                    try (var result = statement.executeQuery("PRAGMA user_version")) { result.next(); assertEquals(3, result.getInt(1)); }
                    try (var result = statement.executeQuery("PRAGMA foreign_keys")) { result.next(); assertEquals(1, result.getInt(1)); }
                }
                return null;
            });
        }
    }
}
