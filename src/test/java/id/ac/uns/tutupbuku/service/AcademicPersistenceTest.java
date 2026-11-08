package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Path;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class AcademicPersistenceTest {
    @TempDir Path directory;
    private SqliteDatabase database;
    private SqliteRepository repository;
    private AuthService auth;
    private AcademicService academic;
    @BeforeEach void open() {
        database = new SqliteDatabase(directory.resolve("academic.db"));
        repository = new SqliteRepository(database);
        var session = new UserSession();
        auth = new AuthService(repository, new PasswordHasher(), session);
        auth.register("001", "Satu", "sandi-uji-lokal".toCharArray(), "sandi-uji-lokal".toCharArray());
        auth.register("002", "Dua", "sandi-uji-lokal".toCharArray(), "sandi-uji-lokal".toCharArray());
        login("001"); academic = new AcademicService(repository, session);
    }
    @AfterEach void close() { database.close(); }
    private void login(String nim) { auth.login(nim, "sandi-uji-lokal".toCharArray()); }
    private Semester semester() { return academic.saveSemester(0, 3, "2026/2027", SemesterPeriod.ODD); }
    private Course course(long semester) { return academic.saveCourse(0, semester, "PBO", "", "3", true, Course.DEFAULT_ATTENDANCE_REFERENCE); }

    @Test void semesterUniquenessIsPerAccountAndEditFailureKeepsOriginal() {
        var first = semester();
        var second = academic.saveSemester(0, 4, "2026/2027", SemesterPeriod.EVEN);
        assertThrows(IllegalArgumentException.class, this::semester);
        assertThrows(IllegalArgumentException.class, () -> academic.saveSemester(second.id(), 4, "2026/2027", SemesterPeriod.ODD));
        assertEquals(SemesterPeriod.EVEN, academic.semesters().get(1).period());
        var edited = academic.saveSemester(first.id(), 5, "2027/2028", SemesterPeriod.ODD);
        assertEquals(first.id(), edited.id());
        login("002"); assertDoesNotThrow(this::semester);
    }
    @Test void changingActiveSemesterRetainsPreviousCoursesAndPersistsAcrossReopen() {
        var first = semester(); var course = course(first.id());
        var second = academic.saveSemester(0, 4, "2026/2027", SemesterPeriod.EVEN);
        academic.setActiveSemester(first.id()); academic.setActiveSemester(second.id());
        assertEquals(second.id(), auth.profile().activeSemesterId().orElseThrow());
        assertEquals(course.id(), academic.courses(first.id()).getFirst().id());
        database.close(); database = new SqliteDatabase(directory.resolve("academic.db"));
        repository = new SqliteRepository(database);
        assertEquals(second.id(), repository.getProfile("001").activeSemesterId().orElseThrow());
        assertEquals(2, repository.listSemesters("001").size());
        assertEquals("PBO", repository.listCourses("001", first.id()).getFirst().name());
    }
    @Test void foreignAccountCannotReadUpdateDeleteOrSelectOwnedRecords() {
        var first = semester(); var course = course(first.id());
        login("002"); var other = semester();
        assertEquals(1, academic.semesters().size());
        assertThrows(IllegalArgumentException.class, () -> academic.courses(first.id()));
        assertThrows(IllegalArgumentException.class, () -> academic.saveSemester(first.id(), 9, "2028/2029", SemesterPeriod.EVEN));
        assertThrows(IllegalArgumentException.class, () -> academic.deleteSemester(first.id()));
        assertThrows(IllegalArgumentException.class, () -> academic.setActiveSemester(first.id()));
        assertThrows(IllegalArgumentException.class, () -> academic.saveCourse(course.id(), other.id(), "Dibajak", "", "3", true, "75%"));
        assertThrows(IllegalArgumentException.class, () -> academic.saveCourse(0, first.id(), "Dibajak", "", "3", true, "75%"));
        assertThrows(IllegalArgumentException.class, () -> academic.deleteCourse(first.id(), course.id()));
        assertTrue(auth.profile().activeSemesterId().isEmpty());
        login("001"); assertEquals("PBO", academic.courses(first.id()).getFirst().name());
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "", "abc"})
    void positiveCreditsRequired(String credits) {
        var semester = semester();
        assertThrows(IllegalArgumentException.class, () -> academic.saveCourse(0, semester.id(), "PBO", "", credits, true, "75%"));
        assertTrue(academic.courses(semester.id()).isEmpty());
    }
    @Test void courseEditPreservesDecimalCreditsAndAttendanceAndAdaptsToM1() {
        var semester = semester(); var created = course(semester.id());
        assertEquals(Attendance.assumedMet(), created.attendance());
        var edited = academic.saveCourse(created.id(), semester.id(), " PBO Lanjut ", " IF123 ", "2,5", false, "80%");
        assertEquals("PBO Lanjut", edited.name()); assertEquals("IF123", edited.code());
        assertEquals(0, TestData.n("2.5").compareTo(edited.credits())); assertFalse(edited.attendance().met());
        assertFalse(edited.attendance().confirmed()); assertEquals(1, edited.revision());
        assertEquals("80%", academic.courses(semester.id()).getFirst().attendanceReference());
        var evaluated = new GradeCalculator().direct(Optional.of(LetterGrade.A), true, edited.attendance());
        assertEquals(LetterGrade.E, evaluated.effectiveLetter().orElseThrow());
        assertTrue(new SemesterGpaService().calculate(java.util.List.of(edited.withoutGrade()), TestData.n("3")).value().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> academic.saveCourse(created.id(), semester.id(), " ", "", "3", true, "75%"));
        assertEquals("PBO Lanjut", academic.courses(semester.id()).getFirst().name());
    }
    @Test void safeDeletionRemovesOnlyRelatedCoursesAndActiveReference() {
        var first = semester(); var firstCourse = course(first.id());
        var second = academic.saveSemester(0, 4, "2026/2027", SemesterPeriod.EVEN); course(second.id());
        academic.setActiveSemester(first.id());
        academic.deleteCourse(first.id(), firstCourse.id()); assertTrue(academic.courses(first.id()).isEmpty());
        course(first.id()); academic.deleteSemester(first.id());
        assertTrue(auth.profile().activeSemesterId().isEmpty());
        assertEquals(1, academic.semesters().size()); assertEquals(1, academic.courses(second.id()).size());
        login("002"); assertEquals("Dua", auth.profile().name());
    }
    @Test void foreignKeysEnforceActiveOwnershipEvenForRawSql() {
        var first = semester();
        assertThrows(StorageException.class, () -> database.write(connection -> {
            try (var statement = connection.prepareStatement("INSERT INTO active_semesters(owner_nim,semester_id) VALUES(?,?)")) {
                statement.setString(1, "002"); statement.setLong(2, first.id()); statement.executeUpdate();
            }
            return null;
        }));
        assertTrue(repository.getProfile("002").activeSemesterId().isEmpty());
    }
    @Test void failureDuringCascadeRollsBackCoursesSemesterAndActiveSelection() {
        var semester = semester(); course(semester.id()); academic.setActiveSemester(semester.id());
        database.read(connection -> { try (var statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER fail_course_delete BEFORE DELETE ON courses BEGIN SELECT RAISE(ABORT, 'simulated failure'); END");
        } return null; });
        assertThrows(StorageException.class, () -> academic.deleteSemester(semester.id()));
        assertEquals(semester.id(), auth.profile().activeSemesterId().orElseThrow());
        assertEquals(1, academic.semesters().size()); assertEquals(1, academic.courses(semester.id()).size());
    }
    @Test void failedWriteKeepsOldProfileAndRetryWorks() {
        database.read(connection -> { try (var statement = connection.createStatement()) { statement.execute("PRAGMA query_only=ON"); } return null; });
        assertThrows(StorageException.class, () -> auth.updateProfile("Tidak tersimpan", "X"));
        assertEquals("Satu", auth.profile().name());
        database.read(connection -> { try (var statement = connection.createStatement()) { statement.execute("PRAGMA query_only=OFF"); } return null; });
        auth.updateProfile("Coba lagi", "Informatika"); assertEquals("Coba lagi", auth.profile().name());
    }
    @Test void academicActionsRequireLoginAndValidSemesterFields() {
        assertThrows(IllegalArgumentException.class, () -> academic.saveSemester(0, 0, "2026/2027", SemesterPeriod.ODD));
        assertThrows(IllegalArgumentException.class, () -> academic.saveSemester(0, 1, "2026/2028", SemesterPeriod.ODD));
        assertThrows(IllegalArgumentException.class, () -> academic.saveSemester(0, 1, "2026", SemesterPeriod.ODD));
        auth.logout(); assertThrows(IllegalStateException.class, academic::semesters);
    }
}
