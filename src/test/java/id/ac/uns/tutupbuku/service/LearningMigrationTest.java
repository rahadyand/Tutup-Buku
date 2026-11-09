package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.*;

class LearningMigrationTest {
    @TempDir Path directory;
    private void createM2(Path file) throws Exception {
        // Fixture schema versi 1 yang sama dengan checkpoint M2; tidak membuat database pengguna.
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE users(nim TEXT PRIMARY KEY NOT NULL,name TEXT NOT NULL,study_program TEXT NOT NULL DEFAULT '',password_algorithm TEXT NOT NULL,password_iterations INTEGER NOT NULL,password_salt TEXT NOT NULL,password_hash TEXT NOT NULL)");
            statement.execute("CREATE TABLE semesters(id INTEGER PRIMARY KEY AUTOINCREMENT,owner_nim TEXT NOT NULL,semester_number INTEGER NOT NULL,academic_year TEXT NOT NULL,period TEXT NOT NULL,UNIQUE(owner_nim,academic_year,period),UNIQUE(id,owner_nim),FOREIGN KEY(owner_nim) REFERENCES users(nim) ON DELETE CASCADE)");
            statement.execute("CREATE TABLE active_semesters(owner_nim TEXT PRIMARY KEY NOT NULL REFERENCES users(nim) ON DELETE CASCADE,semester_id INTEGER NOT NULL,FOREIGN KEY(semester_id,owner_nim) REFERENCES semesters(id,owner_nim) ON DELETE CASCADE)");
            statement.execute("CREATE TABLE courses(id INTEGER PRIMARY KEY AUTOINCREMENT,owner_nim TEXT NOT NULL,semester_id INTEGER NOT NULL,name TEXT NOT NULL,code TEXT NOT NULL DEFAULT '',credits TEXT NOT NULL,attendance_met INTEGER NOT NULL DEFAULT 1,attendance_confirmed INTEGER NOT NULL DEFAULT 0,attendance_reference TEXT NOT NULL,revision INTEGER NOT NULL DEFAULT 0,FOREIGN KEY(semester_id,owner_nim) REFERENCES semesters(id,owner_nim) ON DELETE CASCADE)");
            var hash = new PasswordHasher().hash("sandi-migrasi".toCharArray());
            try (var insert = connection.prepareStatement("INSERT INTO users VALUES(?,?,?,?,?,?,?)")) {
                insert.setString(1, "001234"); insert.setString(2, "Profil M2"); insert.setString(3, "Informatika"); insert.setString(4, hash.algorithm());
                insert.setInt(5, hash.iterations()); insert.setString(6, hash.salt()); insert.setString(7, hash.hash()); insert.executeUpdate();
            }
            statement.execute("INSERT INTO semesters VALUES(12,'001234',3,'2026/2027','ODD')");
            statement.execute("INSERT INTO active_semesters VALUES('001234',12)");
            statement.execute("INSERT INTO courses VALUES(34,'001234',12,'PBO','IF01','2.5',1,0,'75%',7)");
            statement.execute("PRAGMA user_version=1");
        }
    }
    @Test void m2DataAndLoginArePreservedAndMigrationIsIdempotent() throws Exception {
        Path file = directory.resolve("m2.db"); createM2(file);
        try (var database = new SqliteDatabase(file)) {
            var repository = new SqliteRepository(database); var session = new UserSession();
            var auth = new AuthService(repository, new PasswordHasher(), session);
            assertEquals("001234", auth.login("001234", "sandi-migrasi".toCharArray()).nim());
            assertEquals("Informatika", auth.profile().studyProgram()); assertEquals(12, auth.profile().activeSemesterId().orElseThrow());
            var course = repository.listCourses("001234", 12).getFirst(); assertEquals(34, course.id()); assertEquals(7, course.revision());
            assertEquals(0, TestData.n("2.5").compareTo(course.credits()));
            assertFalse(new SqliteLearningRepository(database).seedInitialized("001234"));
        }
        try (var database = new SqliteDatabase(file)) { assertEquals("Profil M2", new SqliteRepository(database).getProfile("001234").name()); }
    }
    @Test void migrationFailureRollsBackWithoutChangingM2DataOrVersion() throws Exception {
        Path file = directory.resolve("failure.db"); createM2(file);
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE materials(dummy TEXT)");
        }
        assertThrows(StorageException.class, () -> new SqliteDatabase(file));
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file); var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("PRAGMA user_version")) { rows.next(); assertEquals(1, rows.getInt(1)); }
            try (var rows = statement.executeQuery("SELECT name FROM users WHERE nim='001234'")) { rows.next(); assertEquals("Profil M2", rows.getString(1)); }
            try (var rows = statement.executeQuery("SELECT count(*) FROM sqlite_master WHERE name='learning_chapters'")) { rows.next(); assertEquals(0, rows.getInt(1)); }
        }
    }
}
