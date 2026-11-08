package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static id.ac.uns.tutupbuku.service.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class GradeCalculatorTest {
    private final GradeCalculator calculator = new GradeCalculator();
    private final Attendance confirmed = new Attendance(true, true);

    @Test void mt01_normalizesDifferentScalesAndDecimals() {
        equalNumber("80", new Score(n("8"), n("10")).normalized());
        equalNumber("87.5", new Score(n("35"), n("40")).normalized());
        equalNumber("87.5", new Score(n("3.5"), n("4")).normalized());
    }

    @ParameterizedTest
    @CsvSource({"1,0", "1,-1", "-0.01,10", "10.01,10"})
    void mt01_rejectsInvalidScores(String earned, String maximum) {
        assertThrows(IllegalArgumentException.class, () -> new Score(n(earned), n(maximum)));
    }

    @ParameterizedTest
    @CsvSource({"0,E", "54.99,E", "55,D", "59.99,D", "60,C", "64.99,C", "65,C_PLUS", "69.99,C_PLUS",
            "70,B", "74.99,B", "75,B_PLUS", "79.99,B_PLUS", "80,A_MINUS", "84.5,A_MINUS", "84.999999999999999999,A_MINUS", "85,A", "100,A"})
    void mt07_to_mt09_convertsWithoutDisplayRounding(String score, LetterGrade expected) {
        assertEquals(expected, LetterGrade.fromScore(n(score)));
        assertEquals(expected != LetterGrade.D && expected != LetterGrade.E, expected.isPassing());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "100.01"})
    void rejectsOutOfRangeFinalScores(String score) {
        assertThrows(IllegalArgumentException.class, () -> LetterGrade.fromScore(n(score)));
    }

    @Test void at08_acceptsProjectOnlyAndPreservesActualZero() {
        GradeResult result = calculator.weighted(List.of(actual("Proyek", "100", "0")), ScenarioAssumptions.none(), confirmed);
        equalNumber("0", result.underlyingScore().orElseThrow());
        assertEquals(LetterGrade.E, result.effectiveLetter().orElseThrow());
        assertFalse(result.estimated());
    }

    @Test void missingScoreIsNotZeroAndZeroWeightNeedsNoScore() {
        GradeResult missing = calculator.weighted(List.of(free("Proyek", "100")), ScenarioAssumptions.none(), confirmed);
        assertTrue(missing.underlyingScore().isEmpty());
        assertTrue(missing.effectiveLetter().isEmpty());
        assertTrue(missing.issues().getFirst().contains("Proyek"));
        GradeResult complete = calculator.weighted(List.of(actual("Proyek", "100", "80"), free("Info", "0")), ScenarioAssumptions.none(), confirmed);
        equalNumber("80", complete.underlyingScore().orElseThrow());
    }

    @Test void prdCaseA_completeFlatScoresReachC() {
        GradeResult result = calculator.weighted(List.of(actual("Tugas", "20", "80"), actual("UTS", "30", "30"), actual("UAS", "50", "70")), ScenarioAssumptions.none(), confirmed);
        equalNumber("60", result.underlyingScore().orElseThrow());
        assertEquals(LetterGrade.C, result.effectiveLetter().orElseThrow());
    }

    @Test void repeatingNormalizedScoresDoNotFallBelowExactPassingBoundary() {
        var items = List.of(new AssessmentItem("1", "Bagian 1", n("20"), Optional.of(new Score(n("2"), n("3")))),
                new AssessmentItem("2", "Bagian 2", n("20"), Optional.of(new Score(n("2"), n("3")))),
                new AssessmentItem("3", "Bagian 3", n("20"), Optional.of(new Score(n("2"), n("3")))), actual("Sisa", "40", "50"));
        var result = calculator.weighted(items, ScenarioAssumptions.none(), confirmed);
        equalNumber("60", result.underlyingScore().orElseThrow());
        assertEquals(LetterGrade.C, result.effectiveLetter().orElseThrow());
    }

    @Test void prdCaseC_verifiesFlatEquivalentWithoutEnablingGroups() {
        AssessmentItem one = new AssessmentItem("1", "Tugas 1", n("40"), Optional.of(new Score(n("8"), n("10"))));
        AssessmentItem two = new AssessmentItem("2", "Tugas 2", n("60"), Optional.of(new Score(n("35"), n("40"))));
        equalNumber("84.5", calculator.weighted(List.of(one, two), ScenarioAssumptions.none(), confirmed).underlyingScore().orElseThrow());
        GradeResult flat = calculator.weighted(List.of(new AssessmentItem("1", "Tugas 1", n("8"), one.actualScore()),
                new AssessmentItem("2", "Tugas 2", n("12"), two.actualScore()), actual("Sisa", "80", "0")), ScenarioAssumptions.none(), confirmed);
        equalNumber("16.9", flat.underlyingScore().orElseThrow());
    }

    @ParameterizedTest @ValueSource(strings = {"99.99", "100.01"})
    void mt06_rejectsInvalidTotals(String total) {
        var items = List.of(free("1", "50"), free("2", n(total).subtract(n("50")).toPlainString()));
        assertThrows(IllegalArgumentException.class, () -> calculator.weighted(items, ScenarioAssumptions.none(), confirmed));
    }

    @Test void at11_attendanceOverrideIsReversibleAndKeepsUnderlyingScore() {
        var items = List.of(actual("Proyek", "100", "90"));
        GradeResult absent = calculator.weighted(items, ScenarioAssumptions.none(), new Attendance(false, true));
        equalNumber("90", absent.underlyingScore().orElseThrow());
        assertEquals(LetterGrade.A, absent.underlyingLetter().orElseThrow());
        assertEquals(LetterGrade.E, absent.effectiveLetter().orElseThrow());
        assertEquals(LetterGrade.A, calculator.weighted(items, ScenarioAssumptions.none(), confirmed).effectiveLetter().orElseThrow());
        assertEquals(n("90"), items.getFirst().actualScore().orElseThrow().earned());
    }

    @Test void mt11_directLetterDoesNotInventNumericScore() {
        GradeResult result = calculator.direct(Optional.of(LetterGrade.B_PLUS), true, new Attendance(false, true));
        assertTrue(result.underlyingScore().isEmpty());
        assertEquals(LetterGrade.B_PLUS, result.underlyingLetter().orElseThrow());
        equalNumber("3.30", result.underlyingLetter().orElseThrow().gradePoint());
        equalNumber("0", result.effectiveLetter().orElseThrow().gradePoint());
    }

    @Test void attendanceDefaultsToUnconfirmedAndAssumptionsAreEstimated() {
        assertTrue(Attendance.assumedMet().met());
        assertFalse(Attendance.assumedMet().confirmed());
        assertTrue(calculator.direct(Optional.of(LetterGrade.A), true, Attendance.assumedMet()).estimated());
        assertTrue(calculator.weighted(List.of(free("x", "100")), new ScenarioAssumptions(Map.of("x", n("60"))), confirmed).estimated());
    }

    @ParameterizedTest @CsvSource({"'87,5',87.5", "84.5,84.5", "' 0,00 ',0"})
    void parsesIndonesianDecimals(String input, String expected) { equalNumber(expected, DecimalInput.parse(input)); }

    @ParameterizedTest @ValueSource(strings = {"", "1,2.3", "NaN", "Infinity", "1 000", "1e2"})
    void rejectsAmbiguousDecimalInput(String input) { assertThrows(IllegalArgumentException.class, () -> DecimalInput.parse(input)); }
}
