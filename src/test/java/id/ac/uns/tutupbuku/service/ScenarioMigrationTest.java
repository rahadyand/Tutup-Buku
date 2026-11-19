package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.*;

class ScenarioMigrationTest {
    @TempDir Path directory;
    private static final id.ac.uns.tutupbuku.security.PasswordHash HASH =
            new PasswordHasher().hash("migration-fixture".toCharArray());

    private SemesterScenario legacy(Path file) throws Exception {
        SemesterScenario saved;
        try (var db = new SqliteDatabase(file)) {
            var repo = new SqliteRepository(db);
            repo.createAccount("001", "Akun lama", HASH);
            var semester =
                    repo.saveSemester("001", new Semester(0, 3, "2026/2027", SemesterPeriod.ODD));
            var course =
                    repo.saveCourse(
                            "001",
                            new Course(
                                    0,
                                    semester.id(),
                                    "PBO",
                                    "",
                                    TestData.n("3"),
                                    Attendance.assumedMet(),
                                    "75%",
                                    0));
            var session = new UserSession();
            session.login("001");
            var service = new GradePlanningService(new SqliteGradeRepository(db), session);
            service.saveComponent(course.id(), "nilai", "Proyek", "100", "8", "10", 0);
            saved = service.save(service.create(semester.id(), "Skenario v4", TestData.n("3")));
        }
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file);
                var s = c.createStatement()) {
            s.execute("ALTER TABLE scenario_items DROP COLUMN override_score");
            s.execute("ALTER TABLE scenario_courses DROP COLUMN projection_enabled");
            s.execute("PRAGMA user_version=4");
        }
        return saved;
    }

    @Test
    void versionFourScenariosRetainIdsValuesAndLegacyProjectionAfterMigrationAndReopen()
            throws Exception {
        Path file = directory.resolve("legacy.db");
        var before = legacy(file);
        for (int i = 0; i < 2; i++)
            try (var db = new SqliteDatabase(file)) {
                var repo = new SqliteRepository(db);
                assertEquals("Akun lama", repo.getProfile("001").name());
                var after = new SqliteGradeRepository(db).loadScenario("001", before.id());
                assertEquals(before, after);
                assertTrue(after.courses().getFirst().projectionEnabled());
                assertTrue(after.courses().getFirst().overriddenScores().isEmpty());
                db.read(
                        c -> {
                            try (var q = c.createStatement();
                                    var r = q.executeQuery("PRAGMA user_version")) {
                                r.next();
                                assertEquals(5, r.getInt(1));
                            }
                            return null;
                        });
            }
    }

    @Test
    void failedExtensionRollsBackBothDdlAndVersionWithoutResetThenCanRetry() throws Exception {
        Path file = directory.resolve("fail.db");
        var before = legacy(file);
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file);
                var s = c.createStatement()) {
            s.execute(
                    "ALTER TABLE scenario_courses ADD COLUMN projection_enabled INTEGER NOT NULL"
                        + " DEFAULT 1");
        }
        assertThrows(StorageException.class, () -> new SqliteDatabase(file));
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file);
                var s = c.createStatement()) {
            try (var r = s.executeQuery("PRAGMA user_version")) {
                r.next();
                assertEquals(4, r.getInt(1));
            }
            try (var r = s.executeQuery("SELECT COUNT(*) FROM grade_scenarios")) {
                r.next();
                assertEquals(1, r.getInt(1));
            }
            try (var r = s.executeQuery("PRAGMA table_info(scenario_items)")) {
                while (r.next()) assertNotEquals("override_score", r.getString("name"));
            }
            s.execute("ALTER TABLE scenario_courses DROP COLUMN projection_enabled");
        }
        try (var db = new SqliteDatabase(file)) {
            assertEquals(before, new SqliteGradeRepository(db).loadScenario("001", before.id()));
        }
    }

    @Test
    void brokenVersionFiveIsReportedAndNotSilentlyRebuilt() throws Exception {
        Path file = directory.resolve("broken.db");
        var before = legacy(file);
        try (var ignored = new SqliteDatabase(file)) {}
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file);
                var s = c.createStatement()) {
            s.execute("ALTER TABLE scenario_items DROP COLUMN override_score");
        }
        assertThrows(StorageException.class, () -> new SqliteDatabase(file));
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file);
                var s = c.prepareStatement("SELECT name FROM grade_scenarios WHERE id=?")) {
            s.setLong(1, before.id());
            try (var r = s.executeQuery()) {
                assertTrue(r.next());
                assertEquals(before.name(), r.getString(1));
            }
        }
    }
}
