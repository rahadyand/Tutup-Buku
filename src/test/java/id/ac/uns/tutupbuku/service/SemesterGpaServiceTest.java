package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import java.util.Optional;
import static id.ac.uns.tutupbuku.service.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class SemesterGpaServiceTest {
    private final SemesterGpaService service = new SemesterGpaService();
    private final GradeCalculator calculator = new GradeCalculator();

    private CourseResult course(String id, String credits, LetterGrade letter, boolean actual, Attendance attendance) {
        return new CourseResult(id, id, Optional.of(n(credits)), Optional.of(calculator.direct(Optional.of(letter), actual, attendance)));
    }

    @Test void mt10_reproducesGpaAndAttendanceExample() {
        var attendance = Attendance.assumedMet();
        var one = course("1", "3", LetterGrade.A, false, attendance);
        var two = course("2", "3", LetterGrade.A, false, attendance);
        var four = course("4", "2", LetterGrade.B, false, attendance);
        var pboItems = List.of(actual("Tugas", "20", "80"), actual("UTS", "30", "30"), actual("UAS", "50", "70"));
        var pbo = new CourseResult("PBO", "PBO", Optional.of(n("3")), Optional.of(calculator.weighted(pboItems, ScenarioAssumptions.none(), attendance)));
        var result = service.calculate(List.of(one, two, pbo, four), n("3.27"));
        assertEquals("3,27", result.displayValue());
        assertTrue(result.value().orElseThrow().subtract(n("3.2727272727272727")).abs().compareTo(n("0.000000000000001")) < 0);
        assertEquals("IP Simulasi", result.label());
        assertTrue(result.targetReached().orElseThrow());
        var absent = new CourseResult("PBO", "PBO", Optional.of(n("3")), Optional.of(calculator.weighted(pboItems, ScenarioAssumptions.none(), new Attendance(false, false))));
        var lower = service.calculate(List.of(one, two, absent, four), n("2.73"));
        assertEquals("2,73", lower.displayValue());
        assertFalse(lower.targetReached().orElseThrow(), "Tampilan 2,73 tidak berarti target 2,73 tercapai.");
        assertTrue(lower.difference().orElseThrow().signum() < 0);
    }

    @Test void mt13_emptySemesterAndMissingGradeHaveNoPartialGpa() {
        var empty = service.calculate(List.of(), n("3"));
        assertTrue(empty.value().isEmpty());
        var complete = course("1", "3", LetterGrade.A, true, new Attendance(true, true));
        var missing = new CourseResult("PBO", "PBO", Optional.of(n("3")), Optional.empty());
        var result = service.calculate(List.of(complete, missing), n("3"));
        assertTrue(result.value().isEmpty());
        assertTrue(result.targetReached().isEmpty());
        assertTrue(result.difference().isEmpty());
        assertEquals("Belum lengkap", result.displayValue());
        assertTrue(result.issues().getFirst().contains("PBO"));
    }

    @ParameterizedTest @ValueSource(strings = {"0", "-1"})
    void mt13_invalidCreditsBlockGpa(String credits) {
        assertTrue(service.calculate(List.of(course("x", credits, LetterGrade.A, true, new Attendance(true, true))), n("3")).value().isEmpty());
    }

    @Test void missingCreditsAndUnresolvedEffectiveGradeAreReported() {
        var missing = new CourseResult("x", "PBO", Optional.empty(), Optional.of(calculator.direct(Optional.empty(), true, new Attendance(true, true))));
        var result = service.calculate(List.of(missing), n("3"));
        assertEquals(2, result.issues().size());
        assertTrue(result.value().isEmpty());
    }

    @Test void dAndEStayInCreditDenominatorAndConfirmedDataIsActual() {
        var attendance = new Attendance(true, true);
        var result = service.calculate(List.of(course("1", "3", LetterGrade.A, true, attendance), course("2", "3", LetterGrade.D, true, attendance), course("3", "3", LetterGrade.E, true, attendance)), n("2"));
        equalNumber("1.666666666666666666666666666666667", result.value().orElseThrow());
        assertEquals("IP Berdasarkan Input Aktual", result.label());
        assertFalse(result.estimated());
    }

    @Test void attendanceECanResolveOtherwiseMissingAcademicGrade() {
        var grade = calculator.direct(Optional.empty(), true, new Attendance(false, true));
        var result = service.calculate(List.of(new CourseResult("x", "PBO", Optional.of(n("3")), Optional.of(grade))), n("0"));
        equalNumber("0", result.value().orElseThrow());
        assertTrue(grade.underlyingLetter().isEmpty());
        assertTrue(result.targetReached().orElseThrow());
    }

    @Test void duplicateCourseCannotBeCountedTwice() {
        var course = course("x", "3", LetterGrade.A, true, new Attendance(true, true));
        assertTrue(service.calculate(List.of(course, course), n("3")).value().isEmpty());
    }

    @ParameterizedTest @ValueSource(strings = {"-0.01", "4.01"})
    void rejectsOutOfRangeGpaTarget(String target) {
        assertThrows(IllegalArgumentException.class, () -> service.calculate(List.of(), n(target)));
    }
}
