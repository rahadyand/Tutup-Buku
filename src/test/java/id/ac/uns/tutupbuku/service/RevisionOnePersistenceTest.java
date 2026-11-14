package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

class RevisionOnePersistenceTest {
    @TempDir Path directory;
    SqliteDatabase db;
    SqliteRepository repo;
    AcademicService academic;
    UserSession session;

    @BeforeEach
    void open() {
        db = new SqliteDatabase(directory.resolve("revision.db"));
        repo = new SqliteRepository(db);
        session = new UserSession();
        session.login("0001");
        repo.createAccount(
                "0001", "Akun lama", new PasswordHasher().hash("sandi-uji-lokal".toCharArray()));
        academic = new AcademicService(repo, session);
    }

    @AfterEach
    void close() {
        db.close();
    }

    @Test
    void guidanceIsInformativeAndOldAccountsStillLogin() {
        assertTrue(NimGuidance.describe("L0125001").contains("Informatika"));
        assertTrue(NimGuidance.describe("L0225001").contains("Sains Data"));
        assertTrue(NimGuidance.describe("L0125001").contains("2025"));
        assertTrue(NimGuidance.describe("L0125001").contains("001"));
        assertTrue(NimGuidance.describe("L9925001").contains("belum dikenali"));
        assertTrue(NimGuidance.describe("0001").contains("lama"));
        var auth = new AuthService(repo, new PasswordHasher(), session);
        assertEquals("0001", auth.login(" 0001 ", "sandi-uji-lokal".toCharArray()).nim());
        assertEquals("", auth.profile().studyProgram());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026/2027", "26", "abcd", "9999", "0000", "20260"})
    void singleYearValidationPreservesExisting(String year) {
        var first = academic.saveSemesterForm(0, "3", "2026", false);
        assertThrows(
                IllegalArgumentException.class,
                () -> academic.saveSemesterForm(first.id(), "4", year, true));
        assertEquals(first, academic.semesters().getFirst());
        assertTrue(repo.getProfile("0001").activeSemesterId().isEmpty());
    }

    @Test
    void unsavedCreateActivateSaveProceedAndUniqueness() {
        var first = academic.saveSemesterForm(0, "3", "2026", true);
        assertEquals(first.id(), repo.getProfile("0001").activeSemesterId().orElseThrow());
        var second = academic.saveSemesterForm(0, "5", "2026", false);
        assertNotEquals(first.id(), second.id());
        assertEquals(first.id(), repo.getProfile("0001").activeSemesterId().orElseThrow());
        academic.saveSemesterForm(second.id(), "5", "2026", false);
        assertEquals(2, academic.semesters().size());
        assertThrows(
                IllegalArgumentException.class,
                () -> academic.saveSemesterForm(0, "5", "2026", true));
        assertThrows(
                IllegalArgumentException.class,
                () -> academic.saveSemesterForm(0, "0", "2026", true));
        assertFalse(first.toString().contains("2027"));
    }

    @Test
    void activationFailureRollsBackNewSemesterAndEdits() {
        var first = academic.saveSemesterForm(0, "1", "2026", true);
        db.read(
                c -> {
                    try (var s = c.createStatement()) {
                        s.execute(
                                "CREATE TRIGGER reject_activation BEFORE INSERT ON active_semesters"
                                        + " BEGIN SELECT RAISE(ABORT,'fixture'); END");
                    }
                    return null;
                });
        assertThrows(StorageException.class, () -> academic.saveSemesterForm(0, "2", "2026", true));
        assertEquals(1, academic.semesters().size());
        assertThrows(
                StorageException.class,
                () -> academic.saveSemesterForm(first.id(), "3", "2027", true));
        assertEquals(first, academic.semesters().getFirst());
        assertEquals(first.id(), repo.getProfile("0001").activeSemesterId().orElseThrow());
    }

    @Test
    void metadataNeverWritesAttendanceAndOtherSemesterCoursesStaySeparate() {
        var one = academic.saveSemesterForm(0, "1", "2026", true);
        var two = academic.saveSemesterForm(0, "2", "2026", false);
        var old =
                repo.saveCourse(
                        "0001",
                        new Course(
                                0,
                                one.id(),
                                "PBO",
                                "",
                                new BigDecimal("3"),
                                new Attendance(false, true),
                                "Acuan lama 80%",
                                0));
        var edited = academic.saveCourseMetadata(old.id(), one.id(), "PBO Baru", "IF", "4");
        assertEquals(old.attendance(), edited.attendance());
        assertEquals(old.attendanceReference(), edited.attendanceReference());
        assertTrue(academic.courses(two.id()).isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                () -> academic.saveCourseMetadata(old.id(), two.id(), "Asing", "", "3"));
        session.login("002");
        assertThrows(
                IllegalArgumentException.class,
                () -> academic.saveCourseMetadata(old.id(), one.id(), "Asing", "", "3"));
        session.login("0001");
        var fresh = academic.saveCourseMetadata(0, two.id(), "Baru", "", "3");
        assertEquals(Attendance.assumedMet(), fresh.attendance());
    }

    @Test
    void attendanceReferenceLivesInActualWorkspaceAndMetadataPreservesIt() {
        var semester = academic.saveSemesterForm(0, "3", "2026", true);
        var course = academic.saveCourseMetadata(0, semester.id(), "PBO", "", "3");
        var planning = new GradePlanningService(new SqliteGradeRepository(db), session);
        planning.saveActual(
                course.id(),
                EvaluationMode.DIRECT,
                Optional.of(LetterGrade.A),
                new Attendance(false, true),
                "90% perkuliahan");
        var edited = academic.saveCourseMetadata(course.id(), semester.id(), "PBO Baru", "IF", "4");
        assertEquals("90% perkuliahan", edited.attendanceReference());
        assertEquals(new Attendance(false, true), edited.attendance());
        var grade = planning.actualResult(planning.courses(semester.id()).getFirst());
        assertEquals(LetterGrade.A, grade.underlyingLetter().orElseThrow());
        assertEquals(LetterGrade.E, grade.effectiveLetter().orElseThrow());
    }

    private void downgradeSemesterTableToV3(boolean collision) throws Exception {
        db.close();
        try (var c =
                        DriverManager.getConnection(
                                "jdbc:sqlite:" + directory.resolve("revision.db"));
                var s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=OFF");
            c.setAutoCommit(false);
            s.execute(
                    "CREATE TABLE semesters_v3(id INTEGER PRIMARY KEY AUTOINCREMENT,owner_nim TEXT"
                        + " NOT NULL,semester_number INTEGER NOT NULL,academic_year TEXT NOT"
                        + " NULL,period TEXT NOT"
                        + " NULL,UNIQUE(owner_nim,academic_year,period),UNIQUE(id,owner_nim),FOREIGN"
                        + " KEY(owner_nim) REFERENCES users(nim) ON DELETE CASCADE)");
            s.execute(
                    "INSERT INTO semesters_v3 SELECT"
                            + " id,owner_nim,semester_number,academic_year,period FROM semesters");
            s.execute("DROP TABLE semesters");
            s.execute("ALTER TABLE semesters_v3 RENAME TO semesters");
            s.execute("PRAGMA user_version=3");
            if (collision) s.execute("CREATE TABLE semesters_r001(marker TEXT)");
            c.commit();
        }
    }

    @Test
    void migrationPreservesLegacyIdsContextsAndBothPeriodsWithoutMerging() throws Exception {
        var odd = repo.saveSemester("0001", new Semester(0, 3, "2026/2027", SemesterPeriod.ODD));
        var even = repo.saveSemester("0001", new Semester(0, 3, "2026/2027", SemesterPeriod.EVEN));
        repo.setActiveSemester("0001", even.id());
        var course = academic.saveCourseMetadata(0, odd.id(), "PBO", "", "3");
        var learning =
                new LearningService(
                        new SqliteLearningRepository(db), session, new AssetStore(directory));
        var folder = learning.saveFolder(0, LearningLocation.Kind.CHAPTER, course.id(), "Bab");
        var material =
                learning.save(
                        new Material(0, folder.location(), "Materi", "Teks", "", 0, 0, ""),
                        "Catatan",
                        0);
        var grades = new GradePlanningService(new SqliteGradeRepository(db), session);
        grades.saveComponent(course.id(), "proyek", "Proyek", "100", "8", "10", 0);
        var scenario = grades.save(grades.create(odd.id(), "Lama", new BigDecimal("3")));
        downgradeSemesterTableToV3(false);
        db = new SqliteDatabase(directory.resolve("revision.db"));
        repo = new SqliteRepository(db);
        academic = new AcademicService(repo, session);
        assertEquals(
                List.of(odd.id(), even.id()),
                academic.semesters().stream().map(Semester::id).toList());
        assertTrue(academic.semesters().stream().allMatch(Semester::legacy));
        assertNotEquals(
                academic.semesters().getFirst().toString(),
                academic.semesters().getLast().toString());
        assertEquals(even.id(), repo.getProfile("0001").activeSemesterId().orElseThrow());
        assertEquals(course.id(), academic.courses(odd.id()).getFirst().id());
        assertEquals(
                "Catatan",
                new SqliteLearningRepository(db)
                        .find("0001", material.material().id())
                        .orElseThrow()
                        .note());
        var reopened =
                new GradePlanningService(new SqliteGradeRepository(db), session)
                        .open(scenario.id());
        assertEquals(course.id(), reopened.courses().getFirst().basis().course().id());
        academic.saveSemesterForm(even.id(), "3", "2026", true);
        assertEquals(SemesterPeriod.EVEN, academic.semesters().getLast().period());
        assertThrows(
                IllegalArgumentException.class,
                () -> academic.saveSemesterForm(0, "3", "2026", false));
    }

    @Test
    void failedMigrationLeavesVersionAndLegacyDataIntact() throws Exception {
        var saved = academic.saveSemesterForm(0, "3", "2026", true);
        downgradeSemesterTableToV3(true);
        assertThrows(
                StorageException.class, () -> new SqliteDatabase(directory.resolve("revision.db")));
        try (var c =
                        DriverManager.getConnection(
                                "jdbc:sqlite:" + directory.resolve("revision.db"));
                var s = c.createStatement()) {
            try (var r = s.executeQuery("PRAGMA user_version")) {
                r.next();
                assertEquals(3, r.getInt(1));
            }
            try (var r = s.executeQuery("SELECT id FROM semesters")) {
                r.next();
                assertEquals(saved.id(), r.getLong(1));
            }
            s.execute("DROP TABLE semesters_r001");
        }
        db = new SqliteDatabase(directory.resolve("revision.db"));
    }
}
