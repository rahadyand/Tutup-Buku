package id.ac.uns.tutupbuku.service;

import static id.ac.uns.tutupbuku.service.TestData.n;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;

class GradePlanningPersistenceTest {
    @TempDir Path directory;
    SqliteDatabase database;
    SqliteRepository academic;
    SqliteGradeRepository repository;
    GradePlanningService service;
    UserSession session;
    long semester;
    long course;
    private static final id.ac.uns.tutupbuku.security.PasswordHash HASH =
            new PasswordHasher().hash("fixture-password".toCharArray());

    @BeforeEach
    void setup() {
        database = new SqliteDatabase(directory.resolve("grades.db"));
        academic = new SqliteRepository(database);
        academic.createAccount("001", "Satu", HASH);
        academic.createAccount("002", "Dua", HASH);
        semester =
                academic.saveSemester("001", new Semester(0, 3, "2026/2027", SemesterPeriod.ODD))
                        .id();
        course = addCourse("PBO", "3");
        connect();
    }

    void connect() {
        repository = new SqliteGradeRepository(database);
        session = new UserSession();
        session.login("001");
        service = new GradePlanningService(repository, session);
    }

    @AfterEach
    void close() {
        database.close();
    }

    long addCourse(String name, String credits) {
        return academic.saveCourse(
                        "001",
                        new Course(
                                0,
                                semester,
                                name,
                                "",
                                n(credits),
                                Attendance.assumedMet(),
                                "75%",
                                0))
                .id();
    }

    void item(String id, String name, String weight, String earned, String maximum, int position) {
        service.saveComponent(course, id, name, weight, earned, maximum, position);
    }

    void three() {
        item("tasks", "Tasks", "20", "", "10", 0);
        item("uts", "UTS", "30", "", "100", 1);
        item("uas", "UAS", "50", "", "100", 2);
    }

    SemesterScenario draft() {
        return service.create(semester, "Rencana", n("3"));
    }

    ScenarioCourse input(SemesterScenario scenario) {
        return scenario.courses().getFirst();
    }

    ScenarioCourse with(
            ScenarioCourse c,
            EvaluationMode mode,
            Optional<LetterGrade> letter,
            BigDecimal target,
            Attendance attendance,
            Map<String, BigDecimal> locks) {
        return new ScenarioCourse(
                c.basis(), mode, letter, target, Optional.empty(), attendance, locks);
    }

    SemesterScenario replace(SemesterScenario s, ScenarioCourse input) {
        return service.rename(s, s.name(), s.targetGpa(), List.of(input));
    }

    @Test
    void componentCrudOrderingAndDraftWeights() {
        item("a", " Tugas bebas ", "20", "8", "10", 0);
        item("b", "Praktik", "30", "35", "40", 1);
        assertEquals(
                "Tugas bebas", service.courses(semester).getFirst().components().getFirst().name());
        assertTrue(
                service.actualResult(service.courses(semester).getFirst())
                        .issues()
                        .getFirst()
                        .contains("50"));
        service.moveComponent(course, "b", -1);
        assertEquals("b", service.courses(semester).getFirst().components().getFirst().id());
        item("a", "Revisi", "70", "8", "10", 1);
        assertEquals(
                "82.25",
                service.actualResult(service.courses(semester).getFirst())
                        .underlyingScore()
                        .orElseThrow()
                        .stripTrailingZeros()
                        .toPlainString());
        service.deleteComponent(course, "a");
        assertEquals(1, service.courses(semester).getFirst().components().size());
    }

    @Test
    void exactWeightsNeverNormalizedAndMissingIsNotZero() {
        item("a", "A", "99.99", "", "10", 0);
        assertTrue(
                service.actualResult(service.courses(semester).getFirst())
                        .underlyingScore()
                        .isEmpty());
        assertThrows(IllegalArgumentException.class, () -> item("a", "A", "100.01", "0", "10", 0));
        item("a", "A", "99.99", "0", "10", 0);
        item("b", "B", "0.02", "0", "10", 1);
        assertTrue(
                service.actualResult(service.courses(semester).getFirst())
                        .issues()
                        .getFirst()
                        .contains("100.01"));
    }

    @Test
    void zeroActualIsResolvedWhileMissingIsNot() {
        item("a", "A", "100", "", "10", 0);
        assertTrue(
                service.actualResult(service.courses(semester).getFirst())
                        .effectiveLetter()
                        .isEmpty());
        item("a", "A", "100", "0", "10", 0);
        assertEquals(
                LetterGrade.E,
                service.actualResult(service.courses(semester).getFirst())
                        .effectiveLetter()
                        .orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> item("a", "A", "100", "11", "10", 0));
        assertThrows(IllegalArgumentException.class, () -> item("a", "A", "100", "1", "0", 0));
        assertEquals(
                0,
                service.courses(semester)
                        .getFirst()
                        .components()
                        .getFirst()
                        .earned()
                        .orElseThrow()
                        .signum());
    }

    @Test
    void targetSequence60Then55Then70WithoutChangingActual() {
        three();
        SemesterScenario s = draft();
        var c = input(s);
        var all = service.evaluateCourse(c).targetPlan().orElseThrow();
        all.suggestedScores().values().forEach(v -> assertEquals(0, v.compareTo(n("60"))));
        c =
                with(
                        c,
                        EvaluationMode.DETAIL,
                        Optional.empty(),
                        n("60"),
                        c.attendance(),
                        Map.of("tasks", n("80")));
        s = replace(s, c);
        service.evaluateCourse(c)
                .targetPlan()
                .orElseThrow()
                .suggestedScores()
                .values()
                .forEach(v -> assertEquals(0, v.compareTo(n("55"))));
        item("uts", "UTS", "30", "30", "100", 1);
        s = service.refresh(s);
        assertEquals(
                0,
                service.evaluateCourse(input(s))
                        .targetPlan()
                        .orElseThrow()
                        .suggestedScores()
                        .get("uas")
                        .compareTo(n("70")));
        assertTrue(service.courses(semester).getFirst().components().getFirst().earned().isEmpty());
        assertEquals(
                0,
                service.courses(semester)
                        .getFirst()
                        .components()
                        .get(1)
                        .earned()
                        .orElseThrow()
                        .compareTo(n("30")));
    }

    @Test
    void impossibleNoFreeAndAlreadyReachedAreExplicit() {
        three();
        var c = input(draft());
        c =
                with(
                        c,
                        EvaluationMode.DETAIL,
                        Optional.empty(),
                        n("100"),
                        c.attendance(),
                        Map.of("tasks", n("0")));
        var impossible = service.evaluateCourse(c);
        assertEquals(TargetPlan.Status.IMPOSSIBLE, impossible.targetPlan().orElseThrow().status());
        assertTrue(impossible.grade().effectiveLetter().isEmpty());
        assertTrue(impossible.explanation().contains("> 100"));
        c =
                with(
                        c,
                        EvaluationMode.DETAIL,
                        Optional.empty(),
                        n("10"),
                        c.attendance(),
                        Map.of("tasks", n("100")));
        assertEquals(
                TargetPlan.Status.ALREADY_REACHED,
                service.evaluateCourse(c).targetPlan().orElseThrow().status());
        c =
                with(
                        c,
                        EvaluationMode.DETAIL,
                        Optional.empty(),
                        n("60"),
                        c.attendance(),
                        Map.of("tasks", n("80"), "uts", n("80"), "uas", n("80")));
        assertEquals(
                TargetPlan.Status.NO_FREE_REACHED,
                service.evaluateCourse(c).targetPlan().orElseThrow().status());
        c =
                with(
                        c,
                        EvaluationMode.DETAIL,
                        Optional.empty(),
                        n("90"),
                        c.attendance(),
                        c.lockedScores());
        assertEquals(
                TargetPlan.Status.NO_FREE_NOT_REACHED,
                service.evaluateCourse(c).targetPlan().orElseThrow().status());
    }

    @Test
    void actualLetterAndDetailedDataSurviveModeSwitch() {
        item("a", "Nilai", "100", "84.5", "100", 0);
        service.saveActual(
                course, EvaluationMode.DIRECT, Optional.of(LetterGrade.B), Attendance.assumedMet());
        assertEquals(
                LetterGrade.B,
                service.actualResult(service.courses(semester).getFirst())
                        .effectiveLetter()
                        .orElseThrow());
        service.saveActual(
                course, EvaluationMode.DETAIL, Optional.of(LetterGrade.B), Attendance.assumedMet());
        assertEquals(
                LetterGrade.A_MINUS,
                service.actualResult(service.courses(semester).getFirst())
                        .effectiveLetter()
                        .orElseThrow());
        assertEquals(LetterGrade.B, service.courses(semester).getFirst().letter().orElseThrow());
    }

    @Test
    void scenarioModeAndEstimatesNeverChangeActualRecords() {
        three();
        var c = input(draft());
        var direct =
                with(
                        c,
                        EvaluationMode.DIRECT,
                        Optional.of(LetterGrade.A),
                        n("60"),
                        c.attendance(),
                        Map.of("tasks", n("80")));
        SemesterScenario saved = service.save(replace(draft(), direct));
        assertEquals(
                LetterGrade.A,
                service.evaluate(saved)
                        .courses()
                        .getFirst()
                        .grade()
                        .effectiveLetter()
                        .orElseThrow());
        var detail =
                with(
                        input(saved),
                        EvaluationMode.DETAIL,
                        input(saved).estimatedLetter(),
                        n("60"),
                        c.attendance(),
                        input(saved).lockedScores());
        assertEquals(
                LetterGrade.C,
                service.evaluateCourse(detail).grade().effectiveLetter().orElseThrow());
        assertTrue(service.courses(semester).getFirst().letter().isEmpty());
        assertTrue(
                service.courses(semester).getFirst().components().stream()
                        .allMatch(i -> i.earned().isEmpty()));
        assertEquals(
                LetterGrade.A, input(service.open(saved.id())).estimatedLetter().orElseThrow());
    }

    @Test
    void attendanceForcesEAndRestoresUnderlyingAndFlagsSimulation() {
        item("a", "Nilai", "100", "85", "100", 0);
        var c = input(draft());
        var failed =
                with(
                        c,
                        c.mode(),
                        c.estimatedLetter(),
                        c.targetScore(),
                        new Attendance(false, false),
                        Map.of());
        GradeResult grade = service.evaluateCourse(failed).grade();
        assertEquals(LetterGrade.A, grade.underlyingLetter().orElseThrow());
        assertEquals(LetterGrade.E, grade.effectiveLetter().orElseThrow());
        assertTrue(grade.estimated());
        assertEquals(
                LetterGrade.A, service.evaluateCourse(c).grade().effectiveLetter().orElseThrow());
        service.saveActual(
                course, EvaluationMode.DETAIL, Optional.empty(), new Attendance(true, true));
        assertFalse(service.actualResult(service.courses(semester).getFirst()).estimated());
    }

    @Test
    void persistenceDuplicationAndEditingAreIndependent() {
        three();
        var original = service.save(draft());
        var duplicate = service.duplicate(original.id(), "Salinan");
        assertNotEquals(original.id(), duplicate.id());
        var changed =
                with(
                        input(duplicate),
                        EvaluationMode.DIRECT,
                        Optional.of(LetterGrade.A),
                        n("85"),
                        new Attendance(false, false),
                        Map.of("tasks", n("80")));
        service.save(replace(duplicate, changed));
        database.close();
        database = new SqliteDatabase(directory.resolve("grades.db"));
        connect();
        assertEquals(EvaluationMode.DETAIL, input(service.open(original.id())).mode());
        assertTrue(input(service.open(original.id())).lockedScores().isEmpty());
        assertEquals(EvaluationMode.DIRECT, input(service.open(duplicate.id())).mode());
        assertFalse(input(service.open(duplicate.id())).attendance().met());
        service.delete(duplicate.id());
        assertEquals(1, service.scenarios(semester).size());
    }

    @Test
    void ownershipRejectsEveryMutationAndForeignSnapshot() {
        three();
        var saved = service.save(draft());
        var item = service.courses(semester).getFirst().components().getFirst();
        session.login("002");
        assertThrows(IllegalArgumentException.class, () -> service.courses(semester));
        assertThrows(IllegalArgumentException.class, () -> service.open(saved.id()));
        assertThrows(IllegalArgumentException.class, () -> service.scenarios(semester));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.saveComponent(course, item.id(), "X", "20", "", "10", 0));
        assertThrows(
                IllegalArgumentException.class, () -> service.moveComponent(course, item.id(), 1));
        assertThrows(
                IllegalArgumentException.class, () -> service.deleteComponent(course, item.id()));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.saveActual(
                                course,
                                EvaluationMode.DIRECT,
                                Optional.of(LetterGrade.A),
                                Attendance.assumedMet()));
        assertThrows(IllegalArgumentException.class, () -> service.delete(saved.id()));
        assertThrows(IllegalArgumentException.class, () -> service.duplicate(saved.id(), "Bajak"));
        assertThrows(IllegalArgumentException.class, () -> service.save(saved));
        long otherSemester =
                academic.saveSemester("002", new Semester(0, 1, "2026/2027", SemesterPeriod.ODD))
                        .id();
        var foreign =
                new SemesterScenario(
                        0,
                        otherSemester,
                        "Bajak",
                        n("3"),
                        0,
                        1,
                        java.time.Instant.now(),
                        saved.courses());
        assertThrows(IllegalArgumentException.class, () -> service.save(foreign));
        session.login("001");
        assertEquals(1, service.scenarios(semester).size());
    }

    @Test
    void revisionsRequireExplicitReviewAndRefreshRemovesActualLocks() {
        three();
        var c = input(draft());
        var saved =
                service.save(
                        replace(
                                draft(),
                                with(
                                        c,
                                        c.mode(),
                                        c.estimatedLetter(),
                                        c.targetScore(),
                                        c.attendance(),
                                        Map.of("uts", n("70")))));
        item("uts", "UTS", "30", "30", "100", 1);
        assertTrue(service.review(service.open(saved.id())).message().contains("nilai aktual"));
        assertTrue(input(service.open(saved.id())).basis().components().get(1).earned().isEmpty());
        var refreshed = service.refresh(saved);
        assertFalse(service.review(refreshed).outdated());
        assertFalse(input(refreshed).lockedScores().containsKey("uts"));
        assertTrue(
                service.review(service.open(saved.id())).outdated()); // refresh alone never saves
        service.save(refreshed);
        assertFalse(service.review(service.open(saved.id())).outdated());
        Course old = service.courses(semester).getFirst().course();
        academic.saveCourse(
                "001",
                new Course(
                        old.id(),
                        semester,
                        old.name(),
                        old.code(),
                        n("4"),
                        old.attendance(),
                        old.attendanceReference(),
                        old.revision()));
        assertTrue(service.review(service.open(saved.id())).message().contains("SKS"));
    }

    @Test
    void deletedAndAddedCoursesLeaveHistoricalSnapshotsUntilRefresh() {
        three();
        var saved = service.save(draft());
        academic.deleteCourse("001", semester, course);
        addCourse("Baru", "2");
        var historical = service.open(saved.id());
        assertTrue(service.review(historical).message().contains("dihapus"));
        assertTrue(service.review(historical).message().contains("baru"));
        assertEquals(course, input(historical).basis().course().id());
        assertEquals(
                course, input(service.duplicate(saved.id(), "Historis")).basis().course().id());
        assertEquals("Baru", input(service.refresh(historical)).basis().course().name());
    }

    @Test
    void completeGpaPrdExampleAttendanceAndUnroundedTarget() {
        long second = addCourse("Dua", "3"),
                third = addCourse("Tiga", "3"),
                fourth = addCourse("Empat", "2");
        for (long id : new long[] {course, second})
            service.saveActual(
                    id,
                    EvaluationMode.DIRECT,
                    Optional.of(LetterGrade.A),
                    new Attendance(true, true));
        service.saveActual(
                third,
                EvaluationMode.DIRECT,
                Optional.of(LetterGrade.C),
                new Attendance(true, true));
        service.saveActual(
                fourth,
                EvaluationMode.DIRECT,
                Optional.of(LetterGrade.B),
                new Attendance(true, true));
        var s = draft();
        s = service.rename(s, s.name(), n("3.2727"), s.courses());
        var result = service.evaluate(s);
        assertEquals("3,27", result.gpa().displayValue());
        assertTrue(result.gpa().targetReached().orElseThrow());
        assertFalse(result.gpa().estimated());
        s = service.rename(s, s.name(), n("3.2728"), s.courses());
        assertFalse(service.evaluate(s).gpa().targetReached().orElseThrow());
        service.saveActual(
                third,
                EvaluationMode.DIRECT,
                Optional.of(LetterGrade.C),
                new Attendance(false, true));
        assertEquals("2,73", service.evaluate(service.refresh(s)).gpa().displayValue());
    }

    @Test
    void unresolvedCourseBlocksWholeGpaAndDCountsCredits() {
        service.saveActual(
                course, EvaluationMode.DIRECT, Optional.of(LetterGrade.A), Attendance.assumedMet());
        long second = addCourse("Belum", "3");
        assertTrue(service.evaluate(draft()).gpa().value().isEmpty());
        service.saveActual(
                second, EvaluationMode.DIRECT, Optional.of(LetterGrade.D), Attendance.assumedMet());
        assertEquals("2,50", service.evaluate(draft()).gpa().displayValue());
        for (String target : List.of("-0.01", "4.01"))
            assertThrows(
                    IllegalArgumentException.class, () -> service.create(semester, "X", n(target)));
    }

    @Test
    void staleComparisonAndScenarioSaveConflictAreExplicit() {
        three();
        var original = service.save(draft());
        var copy = service.duplicate(original.id(), "Salinan");
        assertTrue(
                service.comparisonWarning(service.evaluate(original), service.evaluate(copy))
                        .contains("kompatibel"));
        service.save(service.rename(original, "Baru", n("3"), original.courses()));
        assertThrows(IllegalArgumentException.class, () -> service.save(original));
        assertEquals("Baru", service.open(original.id()).name());
        item("tasks", "Tasks", "20", "8", "10", 0);
        assertTrue(
                service.comparisonWarning(
                                service.evaluate(copy), service.evaluate(service.refresh(copy)))
                        .contains("usang"));
    }

    @Test
    void failedSnapshotSaveRollsBackHeaderAndChildrenAndCanRetry() {
        three();
        var original = service.save(draft());
        database.write(
                c -> {
                    try (var s = c.createStatement()) {
                        s.execute(
                                "CREATE TRIGGER reject_snapshot BEFORE INSERT ON scenario_items"
                                    + " BEGIN SELECT RAISE(ABORT,'fixture failure'); END");
                    }
                    return null;
                });
        var edited = service.rename(original, "Jangan hilang", n("3.5"), original.courses());
        assertThrows(StorageException.class, () -> service.save(edited));
        assertEquals(original, service.open(original.id()));
        assertEquals("Jangan hilang", edited.name());
        database.write(
                c -> {
                    try (var s = c.createStatement()) {
                        s.execute("DROP TRIGGER reject_snapshot");
                    }
                    return null;
                });
        assertEquals("Jangan hilang", service.save(edited).name());
    }

    @Test
    void semesterDeletionCascadesAllGradeRecordsSafely() {
        three();
        service.save(draft());
        academic.deleteSemester("001", semester);
        database.read(
                c -> {
                    for (String table :
                            List.of(
                                    "assessment_components",
                                    "course_actual_grades",
                                    "grade_scenarios",
                                    "scenario_courses",
                                    "scenario_items"))
                        try (var s = c.createStatement();
                                var r = s.executeQuery("SELECT count(*) FROM " + table)) {
                            assertEquals(0, r.getInt(1));
                        }
                    return null;
                });
        assertEquals("Dua", academic.getProfile("002").name());
    }
}
