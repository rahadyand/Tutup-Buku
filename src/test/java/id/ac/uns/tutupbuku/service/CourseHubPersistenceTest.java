package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.*;

class CourseHubPersistenceTest {
    @TempDir Path directory;
    private SqliteDatabase db;
    private SqliteRepository academic;
    private LearningService learning;
    private UserSession session;
    private Course first, second;
    private MaterialReading a, b, interest;
    private static final id.ac.uns.tutupbuku.security.PasswordHash HASH =
            new PasswordHasher().hash("fixture-course-hub".toCharArray());

    @BeforeEach
    void setup() {
        db = new SqliteDatabase(directory.resolve("hub.db"));
        academic = new SqliteRepository(db);
        academic.createAccount("001", "Akun pertama", HASH);
        academic.createAccount("002", "Akun kedua", HASH);
        var semester =
                academic.saveSemester("001", new Semester(0, 3, "2026/2027", SemesterPeriod.ODD));
        academic.setActiveSemester("001", semester.id());
        first =
                academic.saveCourse(
                        "001",
                        new Course(
                                0,
                                semester.id(),
                                "PBO",
                                "PBO",
                                TestData.n("3"),
                                Attendance.assumedMet(),
                                "75%",
                                0));
        second =
                academic.saveCourse(
                        "001",
                        new Course(
                                0,
                                semester.id(),
                                "Struktur Data",
                                "",
                                TestData.n("2"),
                                Attendance.assumedMet(),
                                "75%",
                                0));
        session = new UserSession();
        session.login("001");
        connect();
        a =
                material(
                        learning.saveFolder(0, LearningLocation.Kind.CHAPTER, first.id(), "Bab A")
                                .location(),
                        "Materi A");
        b =
                material(
                        learning.saveFolder(0, LearningLocation.Kind.CHAPTER, second.id(), "Bab B")
                                .location(),
                        "Materi B");
        interest =
                material(
                        learning.saveFolder(0, LearningLocation.Kind.INTEREST, 0, "ML").location(),
                        "Peminatan");
    }

    private void connect() {
        learning =
                new LearningService(
                        new SqliteLearningRepository(db), session, new AssetStore(directory));
    }

    private MaterialReading material(LearningLocation location, String name) {
        return learning.save(
                new Material(0, location, name, "# Isi\n\nBacaan", "Sumber", 0, 0, ""),
                "Catatan",
                0);
    }

    private void read(MaterialReading reading, double ratio) {
        learning.bookmarkWriter(
                        new ReadingBookmark(
                                reading.material().id(),
                                reading.material().revision(),
                                ratio,
                                reading.material().location()))
                .run();
    }

    @AfterEach
    void close() {
        db.close();
    }

    @Test
    void courseHistorySurvivesOtherCourseInterestAndRestartWithoutActivatingSemester() {
        assertTrue(learning.resumeCourse(first.id()).reading().isEmpty());
        read(a, 0.65);
        read(b, 0.4);
        read(interest, 0.2);
        var profile = academic.getProfile("001");
        var archive =
                academic.saveSemester("001", new Semester(0, 1, "2025/2026", SemesterPeriod.ODD));
        assertTrue(academic.listCourses("001", archive.id()).isEmpty());
        assertEquals(profile.activeSemesterId(), academic.getProfile("001").activeSemesterId());
        db.close();
        db = new SqliteDatabase(directory.resolve("hub.db"));
        connect();
        assertEquals(a, learning.resumeCourse(first.id()).reading().orElseThrow());
        assertEquals(0.65, learning.resumeCourse(first.id()).scrollRatio());
        assertEquals(b, learning.resumeCourse(second.id()).reading().orElseThrow());
        assertEquals(0.4, learning.resumeCourse(second.id()).scrollRatio());
        assertEquals(interest, learning.resume().reading().orElseThrow());
    }

    @Test
    void ownershipCoversCourseHistoryReadingAndProgress() {
        read(a, 0.5);
        session.login("002");
        assertThrows(IllegalArgumentException.class, () -> learning.resumeCourse(first.id()));
        assertThrows(IllegalArgumentException.class, () -> learning.open(a.material().id()));
        assertThrows(IllegalArgumentException.class, () -> learning.courseProgress(first.id()));
        assertThrows(IllegalArgumentException.class, () -> read(a, 0.8));
        session.login("001");
        assertEquals(0.5, learning.resumeCourse(first.id()).scrollRatio());
    }

    @Test
    void revisionFallbackDeletionAndCourseCascadeAreHonest() {
        read(a, 0.75);
        learning.complete(a.material().id(), true);
        var changed =
                new Material(
                        a.material().id(),
                        a.material().location(),
                        "Baru",
                        "# Revisi",
                        "Sumber",
                        0,
                        a.material().revision(),
                        a.material().updatedAt());
        learning.save(changed, a.note(), a.noteRevision());
        assertEquals(0, learning.resumeCourse(first.id()).scrollRatio());
        assertTrue(learning.resumeCourse(first.id()).message().contains("berubah"));
        assertEquals(new LearningProgress(1, 1), learning.courseProgress(first.id()));
        learning.deleteMaterial(a.material().id());
        assertTrue(learning.resumeCourse(first.id()).reading().isEmpty());
        assertTrue(learning.resumeCourse(first.id()).message().contains("dihapus"));
        academic.deleteCourse("001", first.semesterId(), first.id());
        db.read(
                c -> {
                    try (var s = c.createStatement();
                            var r = s.executeQuery("SELECT count(*) FROM course_reading")) {
                        r.next();
                        assertEquals(0, r.getInt(1));
                    }
                    return null;
                });
        assertEquals(b, learning.open(b.material().id()));
    }

    @Test
    void bookmarkFailureRollsBackGlobalAndCourseHistoryThenRetryWorks() {
        read(a, 0.3);
        db.read(
                c -> {
                    try (var s = c.createStatement()) {
                        s.execute(
                                "CREATE TRIGGER reject_course_reading BEFORE INSERT ON"
                                    + " course_reading BEGIN SELECT RAISE(ABORT,'fixture'); END");
                    }
                    return null;
                });
        assertThrows(StorageException.class, () -> read(b, 0.8));
        assertEquals(a, learning.resume().reading().orElseThrow());
        assertEquals(0.3, learning.resumeCourse(first.id()).scrollRatio());
        assertTrue(learning.resumeCourse(second.id()).reading().isEmpty());
        db.read(
                c -> {
                    try (var s = c.createStatement()) {
                        s.execute("DROP TRIGGER reject_course_reading");
                    }
                    return null;
                });
        read(b, 0.8);
        assertEquals(b, learning.resume().reading().orElseThrow());
        assertEquals(0.8, learning.resumeCourse(second.id()).scrollRatio());
    }

    private void versionFive() throws Exception {
        read(a, 0.6);
        db.close();
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("hub.db"));
                var s = c.createStatement()) {
            s.execute("DROP TABLE course_reading");
            s.execute("DROP TABLE agenda_steps");
            s.execute("DROP TABLE agenda_items");
            s.execute("PRAGMA user_version=5");
        }
    }

    @Test
    void versionFiveMigrationSeedsOnlyKnownCourseHistoryAndPreservesAllRecords() throws Exception {
        var grades = new GradePlanningService(new SqliteGradeRepository(db), session);
        var scenario =
                grades.save(grades.create(first.semesterId(), "Rencana lama", TestData.n("3")));
        learning.complete(a.material().id(), true);
        versionFive();
        for (int i = 0; i < 2; i++) {
            db = new SqliteDatabase(directory.resolve("hub.db"));
            connect();
            assertEquals(a.material(), learning.open(a.material().id()).material());
            assertEquals("Catatan", learning.open(a.material().id()).note());
            assertTrue(learning.open(a.material().id()).completed());
            assertEquals(0.6, learning.resumeCourse(first.id()).scrollRatio());
            assertTrue(learning.resumeCourse(second.id()).reading().isEmpty());
            assertEquals(
                    scenario,
                    new GradePlanningService(new SqliteGradeRepository(db), session)
                            .open(scenario.id()));
            db.close();
        }
    }

    @Test
    void failedMigrationDoesNotResetVersionOrAccountsAndCanRetry() throws Exception {
        versionFive();
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("hub.db"));
                var s = c.createStatement()) {
            s.execute("CREATE TABLE course_reading(collision TEXT)");
        }
        assertThrows(StorageException.class, () -> new SqliteDatabase(directory.resolve("hub.db")));
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("hub.db"));
                var s = c.createStatement()) {
            try (var r = s.executeQuery("PRAGMA user_version")) {
                r.next();
                assertEquals(5, r.getInt(1));
            }
            try (var r = s.executeQuery("SELECT count(*) FROM materials")) {
                r.next();
                assertEquals(3, r.getInt(1));
            }
            s.execute("DROP TABLE course_reading");
        }
        db = new SqliteDatabase(directory.resolve("hub.db"));
        connect();
        assertEquals(0.6, learning.resumeCourse(first.id()).scrollRatio());
    }
}
