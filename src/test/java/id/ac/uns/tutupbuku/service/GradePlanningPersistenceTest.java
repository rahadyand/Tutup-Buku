package id.ac.uns.tutupbuku.service;

import static id.ac.uns.tutupbuku.service.TestData.equalNumber;
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
    void weightAndOrderChangesInvalidateSnapshotWithoutChangingSavedBasis() {
        three();
        var saved = service.save(draft());
        item("uts", "UTS", "25", "", "100", 1);
        assertTrue(service.review(service.open(saved.id())).outdated());
        equalNumber("30", input(service.open(saved.id())).basis().components().get(1).weight());
        var refreshed = service.refresh(saved);
        assertFalse(service.review(refreshed).outdated());
        assertTrue(service.review(service.open(saved.id())).outdated());
        var accepted = service.save(refreshed);
        service.moveComponent(course, "uts", -1);
        assertTrue(service.review(service.open(accepted.id())).outdated());
        assertEquals(
                "tasks", input(service.open(accepted.id())).basis().components().getFirst().id());
        assertEquals("uts", service.courses(semester).getFirst().components().getFirst().id());
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

    @Test
    void whatIfReusesTargetsAndNeverWritesActualOrSavedScenarios() {
        three();
        ActualCourse first = service.courses(semester).getFirst();
        var free = service.whatIf(first, n("60"), Map.of(), Set.of());
        assertEquals(3, free.targetPlan().orElseThrow().suggestedScores().size());
        free.targetPlan()
                .orElseThrow()
                .suggestedScores()
                .values()
                .forEach(v -> equalNumber("60", v));
        var locked = service.whatIf(first, n("60"), Map.of("tasks", n("80")), Set.of());
        locked.targetPlan()
                .orElseThrow()
                .suggestedScores()
                .values()
                .forEach(v -> equalNumber("55", v));
        item("uts", "UTS", "30", "30", "100", 1);
        ActualCourse basis = service.courses(semester).getFirst();
        SemesterScenario saved = service.save(draft());
        var third = service.whatIf(basis, n("60"), Map.of("tasks", n("80")), Set.of());
        equalNumber("70", third.targetPlan().orElseThrow().suggestedScores().get("uas"));
        var override =
                service.whatIf(
                        basis, n("60"), Map.of("tasks", n("80"), "uts", n("60")), Set.of("uts"));
        equalNumber("52", override.targetPlan().orElseThrow().suggestedScores().get("uas"));
        assertTrue(override.grade().estimated());
        assertEquals(basis, service.courses(semester).getFirst());
        assertEquals(saved, service.open(saved.id()));
        equalNumber(
                "70",
                service.whatIf(basis, n("60"), Map.of("tasks", n("80")), Set.of())
                        .targetPlan()
                        .orElseThrow()
                        .suggestedScores()
                        .get("uas"));
        equalNumber("30", basis.components().get(1).earned().orElseThrow());
        assertEquals(1, service.scenarios(semester).size());
    }

    @Test
    void whatIfEarnedMaxAndMissingAreDifferentFromZero() {
        three();
        item("tasks", "Tugas", "20", "8", "10", 0);
        item("uts", "UTS", "30", "30", "100", 1);
        ActualCourse basis = service.courses(semester).getFirst();
        equalNumber(
                "70",
                service.whatIf(basis, n("60"), Map.of(), Set.of())
                        .targetPlan()
                        .orElseThrow()
                        .suggestedScores()
                        .get("uas"));
        assertTrue(service.actualResult(basis).underlyingScore().isEmpty());
        item("uas", "UAS", "50", "0", "100", 2);
        var zero =
                service.whatIf(service.courses(semester).getFirst(), n("60"), Map.of(), Set.of());
        assertEquals(
                TargetPlan.Status.NO_FREE_NOT_REACHED, zero.targetPlan().orElseThrow().status());
        equalNumber("25", zero.grade().underlyingScore().orElseThrow());
    }

    @Test
    void whatIfExactLetterAndAttendancePreserveUnderlyingProjection() {
        three();
        var input = service.courses(semester).getFirst();
        var precise = service.whatIf(input, n("84.5"), Map.of(), Set.of());
        equalNumber("84.5", precise.grade().underlyingScore().orElseThrow());
        assertEquals(LetterGrade.A_MINUS, precise.grade().underlyingLetter().orElseThrow());
        service.saveActual(
                course, EvaluationMode.DETAIL, Optional.empty(), new Attendance(false, true));
        var failedAttendance =
                service.whatIf(service.courses(semester).getFirst(), n("84.5"), Map.of(), Set.of());
        equalNumber("84.5", failedAttendance.grade().underlyingScore().orElseThrow());
        assertEquals(
                LetterGrade.A_MINUS, failedAttendance.grade().underlyingLetter().orElseThrow());
        assertEquals(LetterGrade.E, failedAttendance.grade().effectiveLetter().orElseThrow());
        equalNumber("0", failedAttendance.grade().effectiveLetter().orElseThrow().gradePoint());
    }

    @Test
    void whatIfImpossibleNoFreeAndZeroWeightUseExistingStatuses() {
        three();
        var input = service.courses(semester).getFirst();
        var impossible = service.whatIf(input, n("100"), Map.of("tasks", n("0")), Set.of());
        assertEquals(TargetPlan.Status.IMPOSSIBLE, impossible.targetPlan().orElseThrow().status());
        equalNumber("125", impossible.targetPlan().orElseThrow().rawRequiredScore().orElseThrow());
        assertTrue(impossible.targetPlan().orElseThrow().suggestedScores().isEmpty());
        assertTrue(impossible.grade().underlyingScore().isEmpty());
        var preciseImpossible =
                service.whatIf(
                        input,
                        n("80.000000000000000000000000000000000001"),
                        Map.of("tasks", n("0")),
                        Set.of());
        assertEquals(
                TargetPlan.Status.IMPOSSIBLE,
                preciseImpossible.targetPlan().orElseThrow().status());
        assertTrue(preciseImpossible.explanation().contains("komponen bebas > 100"));
        Map<String, java.math.BigDecimal> all =
                Map.of("tasks", n("60"), "uts", n("60"), "uas", n("60"));
        assertEquals(
                TargetPlan.Status.NO_FREE_REACHED,
                service.whatIf(input, n("60"), all, Set.of()).targetPlan().orElseThrow().status());
        assertEquals(
                TargetPlan.Status.NO_FREE_NOT_REACHED,
                service.whatIf(input, n("61"), all, Set.of()).targetPlan().orElseThrow().status());
        item("info", "Informasi", "0", "", "", 3);
        var withZero =
                service.whatIf(service.courses(semester).getFirst(), n("60"), Map.of(), Set.of());
        assertFalse(withZero.targetPlan().orElseThrow().suggestedScores().containsKey("info"));
        assertEquals(3, withZero.targetPlan().orElseThrow().suggestedScores().size());
        var secured = service.whatIf(input, n("10"), Map.of("tasks", n("80")), Set.of());
        assertEquals(
                TargetPlan.Status.ALREADY_REACHED, secured.targetPlan().orElseThrow().status());
        secured.targetPlan()
                .orElseThrow()
                .suggestedScores()
                .values()
                .forEach(v -> equalNumber("0", v));
    }

    @Test
    void whatIfEmptyAndInexactWeightsCannotLookLikeACompleteGrade() {
        ActualCourse empty = service.courses(semester).getFirst();
        var result = service.whatIf(empty, n("60"), Map.of(), Set.of());
        assertTrue(result.targetPlan().isEmpty());
        assertTrue(result.grade().underlyingScore().isEmpty());
        assertTrue(result.grade().effectiveLetter().isEmpty());
        assertFalse(service.evaluate(draft()).gpa().issues().isEmpty());
        three();
        for (String last : List.of("49.99", "50.01")) {
            item("uas", "UAS", last, "", "100", 2);
            var invalid =
                    service.whatIf(
                            service.courses(semester).getFirst(), n("60"), Map.of(), Set.of());
            assertTrue(invalid.targetPlan().isEmpty());
            assertFalse(invalid.grade().issues().isEmpty());
            assertTrue(invalid.grade().underlyingScore().isEmpty());
        }
    }

    @Test
    void whatIfRejectsUnknownOverridesAndKeepsDirectLettersWithoutFictionalScores() {
        three();
        ActualCourse input = service.courses(semester).getFirst();
        assertThrows(
                IllegalArgumentException.class,
                () -> service.whatIf(input, n("60"), Map.of(), Set.of("deleted")));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.whatIf(input, n("60"), Map.of("tasks", n("80")), Set.of("tasks")));
        service.saveActual(
                course, EvaluationMode.DIRECT, Optional.of(LetterGrade.B), Attendance.assumedMet());
        ActualCourse direct = service.courses(semester).getFirst();
        var result = service.whatIf(direct, n("84.5"), Map.of(), Set.of());
        assertTrue(result.targetPlan().isEmpty());
        assertTrue(result.grade().underlyingScore().isEmpty());
        assertEquals(LetterGrade.B, result.grade().underlyingLetter().orElseThrow());
        assertEquals(direct, service.courses(semester).getFirst());
    }

    @Test
    void phaseTwoRoundTripPreservesOverridesBasisAndDerivedInputsAfterRestart() {
        three();
        item("uts", "UTS", "30", "30", "100", 1);
        ActualCourse baseline = service.courses(semester).getFirst();
        ScenarioCourse experiment =
                service.whatIfInput(
                        baseline,
                        n("60"),
                        Map.of("tasks", n("80"), "uts", n("60")),
                        java.util.Set.of("uts"));
        SemesterScenario saved = service.save(service.fromWhatIf(experiment, "Nama sama"));
        ScenarioCourse reopened = service.open(saved.id()).courses().getFirst();
        assertEquals(experiment, reopened);
        equalNumber(
                "52",
                service.evaluateCourse(reopened)
                        .targetPlan()
                        .orElseThrow()
                        .suggestedScores()
                        .get("uas"));
        assertEquals(Map.of("tasks", n("80")), reopened.lockedScores());
        assertEquals(Map.of("uts", n("60")), reopened.overriddenScores());
        assertEquals(baseline, service.courses(semester).getFirst());
        database.read(
                c -> {
                    try (var q =
                            c.prepareStatement(
                                    "SELECT earned,locked_score,suggested_score,override_score FROM"
                                            + " scenario_items WHERE scenario_id=? AND"
                                            + " item_id='uas'")) {
                        q.setLong(1, saved.id());
                        try (var rows = q.executeQuery()) {
                            assertTrue(rows.next());
                            assertNull(rows.getString(1));
                            assertNull(rows.getString(2));
                            equalNumber("52", new java.math.BigDecimal(rows.getString(3)));
                            assertNull(rows.getString(4));
                        }
                    }
                    return null;
                });
        database.close();
        database = new SqliteDatabase(directory.resolve("grades.db"));
        academic = new SqliteRepository(database);
        connect();
        assertEquals(saved, service.open(saved.id()));
        SemesterScenario another = service.save(service.fromWhatIf(experiment, "Nama sama"));
        assertNotEquals(saved.id(), another.id());
        assertEquals(saved, service.open(saved.id()));
        session.login("002");
        assertThrows(IllegalArgumentException.class, () -> service.open(saved.id()));
        session.login("001");
        assertEquals(saved, service.open(saved.id()));
    }

    @Test
    void phaseTwoUpdateAndSaveCopyAreIndependentAndDoNotWriteActual() {
        three();
        ActualCourse basis = service.courses(semester).getFirst();
        var a =
                service.save(
                        service.fromWhatIf(
                                service.whatIfInput(
                                        basis,
                                        n("60"),
                                        Map.of("tasks", n("80")),
                                        java.util.Set.of()),
                                "A"));
        var changed =
                service.replaceCourse(
                        a,
                        service.whatIfInput(
                                basis, n("84.5"), Map.of("tasks", n("80")), java.util.Set.of()));
        var b = service.saveCopy(changed, "B");
        assertNotEquals(a.id(), b.id());
        assertEquals(a, service.open(a.id()));
        assertEquals(
                LetterGrade.A_MINUS,
                service.evaluate(b).courses().getFirst().grade().underlyingLetter().orElseThrow());
        var updated =
                service.save(
                        service.replaceCourse(
                                b,
                                service.whatIfInput(basis, n("85"), Map.of(), java.util.Set.of())));
        assertEquals(b.id(), updated.id());
        assertEquals(b.version() + 1, updated.version());
        assertEquals(a, service.open(a.id()));
        assertEquals(basis, service.courses(semester).getFirst());
        assertTrue(
                service.comparisonWarning(service.evaluate(a), service.evaluate(updated))
                        .contains("kompatibel"));
    }

    @Test
    void phaseTwoOtherCoursesUseActualUntilExplicitlyPlannedAndMissingCoursesBlockIp() {
        three();
        long unknown = addCourse("Belum direncanakan", "3"),
                direct = addCourse("Huruf", "2"),
                detail = addCourse("Detail", "3");
        service.saveComponent(unknown, "unknown", "Proyek", "100", "", "100", 0);
        service.saveComponent(detail, "detail", "Proyek", "100", "80", "100", 0);
        service.saveActual(
                direct,
                EvaluationMode.DIRECT,
                Optional.of(LetterGrade.B),
                new Attendance(true, true));
        ActualCourse selected =
                service.courses(semester).stream()
                        .filter(c -> c.course().id() == course)
                        .findFirst()
                        .orElseThrow();
        var plan =
                service.fromWhatIf(
                        service.whatIfInput(selected, n("60"), Map.of(), java.util.Set.of()),
                        "Semester");
        assertTrue(
                plan.courses().stream()
                        .filter(c -> c.basis().course().id() != course)
                        .noneMatch(ScenarioCourse::projectionEnabled));
        var incomplete = service.evaluate(plan);
        assertTrue(incomplete.gpa().value().isEmpty());
        assertTrue(
                incomplete.gpa().issues().stream().anyMatch(v -> v.contains("Belum direncanakan")));
        var other =
                plan.courses().stream()
                        .filter(c -> c.basis().course().id() == unknown)
                        .findFirst()
                        .orElseThrow();
        plan =
                service.replaceCourse(
                        plan,
                        new ScenarioCourse(
                                other.basis(),
                                other.mode(),
                                other.estimatedLetter(),
                                n("85"),
                                Optional.empty(),
                                other.attendance(),
                                Map.of(),
                                Map.of(),
                                true));
        var complete = service.evaluate(plan);
        assertEquals("3,19", complete.gpa().displayValue());
        // Selected C=2, unknown A=4, detail A-=3.7, direct B=3: (6+12+11.1+6)/11.
        assertTrue(
                complete.courses().stream()
                        .anyMatch(v -> service.resultSource(v).contains("Huruf aktual langsung")));
        assertTrue(
                complete.courses().stream()
                        .anyMatch(v -> service.resultSource(v).contains("Detail aktual")));
        assertTrue(
                complete.courses().stream()
                        .anyMatch(v -> service.resultSource(v).contains("Proyeksi skenario")));
        var missing =
                service.rename(
                        plan,
                        plan.name(),
                        plan.targetGpa(),
                        plan.courses().stream()
                                .filter(c -> c.basis().course().id() != direct)
                                .toList());
        assertTrue(service.evaluate(missing).gpa().value().isEmpty());
    }

    @Test
    void phaseTwoRefreshUsesStableIdsAndChangesOnlyDraftUntilSaved() {
        three();
        item("uts", "UTS", "30", "30", "100", 1);
        var basis = service.courses(semester).getFirst();
        var saved =
                service.save(
                        service.fromWhatIf(
                                service.whatIfInput(
                                        basis,
                                        n("60"),
                                        Map.of("tasks", n("80"), "uts", n("60")),
                                        java.util.Set.of("uts")),
                                "Lama"));
        service.moveComponent(course, "uts", -1);
        item("uts", "UTS", "30", "40", "100", 0);
        assertTrue(service.review(saved).outdated());
        var refreshed = service.refresh(saved);
        var input = refreshed.courses().getFirst();
        assertEquals(saved, service.open(saved.id()));
        assertEquals(Map.of("tasks", n("80")), input.lockedScores());
        assertEquals(Map.of("uts", n("60")), input.overriddenScores());
        equalNumber(
                "40",
                input.basis().components().stream()
                        .filter(c -> c.id().equals("uts"))
                        .findFirst()
                        .orElseThrow()
                        .earned()
                        .orElseThrow());
        var persisted = service.save(refreshed);
        assertEquals(saved.version() + 1, persisted.version());
        service.deleteComponent(course, "uts");
        assertTrue(service.review(persisted).message().contains("override"));
        var removed = service.refresh(persisted);
        assertTrue(removed.courses().getFirst().overriddenScores().isEmpty());
        assertEquals(persisted, service.open(persisted.id()));
        assertTrue(
                service.comparisonWarning(service.evaluate(saved), service.evaluate(persisted))
                        .contains("usang"));
    }

    @Test
    void phaseTwoSaveFailureRollsBackHeaderAndOverridesThenRetryWorks() {
        three();
        item("uts", "UTS", "30", "30", "100", 1);
        var basis = service.courses(semester).getFirst();
        var saved =
                service.save(
                        service.fromWhatIf(
                                service.whatIfInput(
                                        basis,
                                        n("60"),
                                        Map.of("uts", n("60")),
                                        java.util.Set.of("uts")),
                                "Draf"));
        var changed =
                service.replaceCourse(
                        saved,
                        service.whatIfInput(
                                basis, n("84.5"), Map.of("uts", n("75")), java.util.Set.of("uts")));
        database.read(
                c -> {
                    try (var q = c.createStatement()) {
                        q.execute(
                                "CREATE TRIGGER fail_p2 BEFORE INSERT ON scenario_items BEGIN"
                                        + " SELECT RAISE(ABORT,'failure'); END");
                    }
                    return null;
                });
        assertThrows(StorageException.class, () -> service.save(changed));
        assertEquals(saved, service.open(saved.id()));
        assertEquals(Map.of("uts", n("75")), changed.courses().getFirst().overriddenScores());
        database.read(
                c -> {
                    try (var q = c.createStatement()) {
                        q.execute("DROP TRIGGER fail_p2");
                    }
                    return null;
                });
        var retry = service.save(changed);
        assertEquals(saved.version() + 1, retry.version());
        assertEquals(basis, service.courses(semester).getFirst());
    }

    @Test
    void phaseTwoAttendanceAffectsSavedIpWithoutErasingUnderlyingGrade() {
        three();
        var basis = service.courses(semester).getFirst();
        var input =
                new ScenarioCourse(
                        basis,
                        EvaluationMode.DETAIL,
                        Optional.empty(),
                        n("84.5"),
                        Optional.empty(),
                        new Attendance(false, false),
                        Map.of(),
                        Map.of(),
                        true);
        var saved = service.save(service.fromWhatIf(input, "Kehadiran"));
        var result = service.evaluate(service.open(saved.id()));
        equalNumber("84.5", result.courses().getFirst().grade().underlyingScore().orElseThrow());
        assertEquals(
                LetterGrade.E, result.courses().getFirst().grade().effectiveLetter().orElseThrow());
        equalNumber("0", result.gpa().value().orElseThrow());
        assertEquals(basis, service.courses(semester).getFirst());
        item("tasks", "Tasks", "20", "84.5", "100", 0);
        item("uts", "UTS", "30", "84.5", "100", 1);
        item("uas", "UAS", "50", "84.5", "100", 2);
        for (EvaluationMode mode : EvaluationMode.values()) {
            service.saveActual(
                    course, mode, Optional.of(LetterGrade.B), new Attendance(true, true));
            var actual = service.courses(semester).getFirst();
            var onlyActual = ScenarioCourse.actualOnly(actual);
            var absent =
                    new ScenarioCourse(
                            actual,
                            onlyActual.mode(),
                            onlyActual.estimatedLetter(),
                            onlyActual.targetScore(),
                            onlyActual.targetLetter(),
                            new Attendance(false, false),
                            Map.of(),
                            Map.of(),
                            false);
            var attendanceOnly = service.save(service.fromWhatIf(absent, "Tanpa target " + mode));
            var evaluated = service.evaluate(attendanceOnly);
            assertEquals(
                    mode == EvaluationMode.DETAIL ? LetterGrade.A_MINUS : LetterGrade.B,
                    evaluated.courses().getFirst().grade().underlyingLetter().orElseThrow());
            assertEquals(
                    LetterGrade.E,
                    evaluated.courses().getFirst().grade().effectiveLetter().orElseThrow());
            equalNumber("0", evaluated.gpa().value().orElseThrow());
            assertEquals(actual, service.courses(semester).getFirst());
        }
    }
}
