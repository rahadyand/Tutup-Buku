package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.sql.DriverManager;

class GradeMigrationTest {
    @TempDir Path directory;

    private void createM3(Path file) throws Exception {
        // Bangun fixture data M3, lalu lepas tabel aditif M4 sehingga file benar-benar versi 2.
        try (var db = new SqliteDatabase(file)) {
            var academic = new SqliteRepository(db);
            academic.createAccount(
                    "001", "Profil M3", new PasswordHasher().hash("sandi-migrasi".toCharArray()));
            var semester =
                    academic.saveSemester(
                            "001", new Semester(0, 3, "2026/2027", SemesterPeriod.ODD));
            academic.setActiveSemester("001", semester.id());
            var course =
                    academic.saveCourse(
                            "001",
                            new Course(
                                    0,
                                    semester.id(),
                                    "PBO",
                                    "IF",
                                    TestData.n("3"),
                                    Attendance.assumedMet(),
                                    "75%",
                                    0));
            var learning = new SqliteLearningRepository(db);
            learning.initializeStarter("001", course.id());
            var folder =
                    learning.folders("001", LearningLocation.Kind.CHAPTER, course.id()).getFirst();
            var original =
                    learning.find(
                                    "001",
                                    learning.materials("001", folder.location()).getFirst().id())
                            .orElseThrow();
            learning.save(
                    "001", original.material(), "Catatan harus bertahan", original.noteRevision());
            learning.setCompleted("001", original.material().id(), true);
            learning.recordReading(
                    "001",
                    new ReadingBookmark(
                            original.material().id(),
                            original.material().revision() + 1,
                            0.5,
                            folder.location()));
        }
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file);
                var s = c.createStatement()) {
            for (String table :
                    new String[] {
                        "scenario_items",
                        "scenario_courses",
                        "grade_scenarios",
                        "course_actual_grades",
                        "assessment_components"
                    }) s.execute("DROP TABLE " + table);
            s.execute("PRAGMA user_version=2");
        }
    }

    @Test
    void migrationPreservesAccountsLearningNotesProgressAndAssetBytes() throws Exception {
        Path file = directory.resolve("m3.db");
        createM3(file);
        Path asset = directory.resolve("assets/fixture.png");
        Files.createDirectories(asset.getParent());
        byte[] bytes = {1, 2, 3, 4};
        Files.write(asset, bytes);
        for (int reopen = 0; reopen < 2; reopen++)
            try (var db = new SqliteDatabase(file)) {
                var academic = new SqliteRepository(db);
                var session = new UserSession();
                var auth = new AuthService(academic, new PasswordHasher(), session);
                assertEquals("001", auth.login("001", "sandi-migrasi".toCharArray()).nim());
                long semester = academic.getProfile("001").activeSemesterId().orElseThrow();
                var course = academic.listCourses("001", semester).getFirst();
                var learning = new SqliteLearningRepository(db);
                var folder =
                        learning.folders("001", LearningLocation.Kind.CHAPTER, course.id())
                                .getFirst();
                var material =
                        learning.find(
                                        "001",
                                        learning.materials("001", folder.location())
                                                .getFirst()
                                                .id())
                                .orElseThrow();
                assertEquals("Catatan harus bertahan", material.note());
                assertTrue(material.completed());
                assertTrue(material.material().markdown().contains("#"));
                assertTrue(learning.lastReading("001").isPresent());
                assertTrue(learning.seedInitialized("001"));
                assertArrayEquals(bytes, Files.readAllBytes(asset));
                assertTrue(new SqliteGradeRepository(db).scenarios("001", semester).isEmpty());
                db.read(
                        c -> {
                            try (var s = c.createStatement();
                                    var r = s.executeQuery("PRAGMA user_version")) {
                                r.next();
                                assertEquals(3, r.getInt(1));
                            }
                            return null;
                        });
            }
    }

    @Test
    void failedMigrationRollsBackDdlAndVersionWithoutReset() throws Exception {
        Path file = directory.resolve("fail.db");
        createM3(file);
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file);
                var s = c.createStatement()) {
            s.execute("CREATE TABLE grade_scenarios(unknown TEXT)");
        }
        assertThrows(StorageException.class, () -> new SqliteDatabase(file));
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file);
                var s = c.createStatement()) {
            try (var r = s.executeQuery("PRAGMA user_version")) {
                r.next();
                assertEquals(2, r.getInt(1));
            }
            try (var r = s.executeQuery("SELECT name FROM users WHERE nim='001'")) {
                r.next();
                assertEquals("Profil M3", r.getString(1));
            }
            try (var r = s.executeQuery("SELECT count(*) FROM materials")) {
                r.next();
                assertEquals(5, r.getInt(1));
            }
            try (var r =
                    s.executeQuery(
                            "SELECT count(*) FROM sqlite_master WHERE"
                                + " name='assessment_components'")) {
                r.next();
                assertEquals(0, r.getInt(1));
            }
        }
    }
}
