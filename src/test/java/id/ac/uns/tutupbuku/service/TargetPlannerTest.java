package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static id.ac.uns.tutupbuku.service.TestData.*;
import static org.junit.jupiter.api.Assertions.*;

class TargetPlannerTest {
    private final TargetPlanner planner = new TargetPlanner();
    private final Attendance attendance = Attendance.assumedMet();

    @Test void mt02_replansAllFreeThenLockedThenActualWithoutMutation() {
        var items = List.of(free("Tugas", "20"), free("UTS", "30"), free("UAS", "50"));
        var allFree = planner.plan(items, ScenarioAssumptions.none(), n("60"), attendance);
        assertEquals(3, allFree.suggestedScores().size());
        allFree.suggestedScores().values().forEach(v -> equalNumber("60", v));
        var locked = new ScenarioAssumptions(Map.of("Tugas", n("80")));
        var second = planner.plan(items, locked, n("60"), attendance);
        assertEquals(2, second.suggestedScores().size());
        second.suggestedScores().values().forEach(v -> equalNumber("55", v));
        var withActual = List.of(items.getFirst(), actual("UTS", "30", "30"), items.getLast());
        var third = planner.plan(withActual, locked, n("60"), attendance);
        assertEquals(1, third.suggestedScores().size());
        equalNumber("70", third.suggestedScores().get("UAS"));
        assertTrue(items.stream().allMatch(i -> i.actualScore().isEmpty()));
        equalNumber("30", withActual.get(1).actualScore().orElseThrow().earned());
        equalNumber("80", locked.lockedScores().get("Tugas"));
    }

    @Test void mt03_impossibleTargetIsNotCapped() {
        var result = planner.plan(List.of(actual("UTS", "70", "20"), free("UAS", "30")), ScenarioAssumptions.none(), n("60"), attendance);
        assertEquals(TargetPlan.Status.IMPOSSIBLE, result.status());
        assertTrue(result.rawRequiredScore().orElseThrow().compareTo(n("153.33")) > 0);
        assertTrue(result.suggestedScores().isEmpty());
        equalNumber("44", result.maximumFinalScore());
    }

    @Test void mt04_alreadyReachedSuggestsZeroAndKeepsNegativeRawRequirement() {
        var result = planner.plan(List.of(actual("Proyek", "80", "90"), free("Tugas", "20")), ScenarioAssumptions.none(), n("60"), attendance);
        assertEquals(TargetPlan.Status.ALREADY_REACHED, result.status());
        equalNumber("0", result.suggestedScores().get("Tugas"));
        equalNumber("72", result.minimumFinalScore());
        equalNumber("-60", result.rawRequiredScore().orElseThrow());
    }

    @Test void mt05_zeroRemainingWeightChecksBothOutcomes() {
        var items = List.of(free("Proyek", "100"), free("Info", "0"));
        var locked = new ScenarioAssumptions(Map.of("Proyek", n("60")));
        var reached = planner.plan(items, locked, n("60"), attendance);
        assertEquals(TargetPlan.Status.NO_FREE_REACHED, reached.status());
        assertTrue(reached.rawRequiredScore().isEmpty());
        assertTrue(reached.suggestedScores().isEmpty());
        assertEquals(TargetPlan.Status.NO_FREE_NOT_REACHED, planner.plan(items, locked, n("61"), attendance).status());
    }

    @ParameterizedTest @ValueSource(strings = {"99.99", "100.01"})
    void mt06_rejectsInexactWeightTotals(String total) {
        var items = List.of(free("1", "50"), free("2", n(total).subtract(n("50")).toPlainString()));
        var error = assertThrows(IllegalArgumentException.class, () -> planner.plan(items, ScenarioAssumptions.none(), n("60"), attendance));
        assertTrue(error.getMessage().contains(total));
    }

    @Test void mt12_roundsRepeatingMinimumUpAndConversionStaysUnrounded() {
        var items = List.of(actual("Praktikum", "40", "90"), free("Proyek", "60"));
        var plan = planner.plan(items, ScenarioAssumptions.none(), LetterGrade.B.minimumScore(), attendance);
        equalNumber("56.67", plan.suggestedScores().get("Proyek"));
        assertTrue(plan.rawRequiredScore().orElseThrow().compareTo(n("56.6666")) > 0);
        GradeCalculator calculator = new GradeCalculator();
        var tooLow = calculator.weighted(items, new ScenarioAssumptions(Map.of("Proyek", n("56.66"))), attendance);
        equalNumber("69.996", tooLow.underlyingScore().orElseThrow());
        assertEquals(LetterGrade.C_PLUS, tooLow.effectiveLetter().orElseThrow());
        assertEquals(LetterGrade.B, calculator.weighted(items, new ScenarioAssumptions(plan.suggestedScores()), attendance).effectiveLetter().orElseThrow());
    }

    @Test void zeroWeightDoesNotEnterSolverAndExactHundredIsPossible() {
        var result = planner.plan(List.of(free("Proyek", "100"), free("Info", "0")), ScenarioAssumptions.none(), n("100"), attendance);
        assertEquals(TargetPlan.Status.ACHIEVABLE, result.status());
        assertEquals(1, result.suggestedScores().size());
        equalNumber("100", result.suggestedScores().get("Proyek"));
    }

    @Test void zeroTargetIsOnlyAMinimumAndAttendanceRemainsSeparate() {
        var result = planner.plan(List.of(free("x", "100")), ScenarioAssumptions.none(), LetterGrade.E.minimumScore(), new Attendance(false, false));
        assertEquals(TargetPlan.Status.ALREADY_REACHED, result.status());
        equalNumber("0", result.suggestedScores().get("x"));
        assertFalse(result.attendanceMet());
    }

    @Test void refusesStaleOrUnknownAssumptionsAndDuplicateIds() {
        assertThrows(IllegalArgumentException.class, () -> planner.plan(List.of(actual("x", "100", "20")), new ScenarioAssumptions(Map.of("x", n("80"))), n("60"), attendance));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(List.of(free("x", "100")), new ScenarioAssumptions(Map.of("deleted", n("80"))), n("60"), attendance));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(List.of(free("x", "50"), free("x", "50")), ScenarioAssumptions.none(), n("60"), attendance));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(List.of(), ScenarioAssumptions.none(), n("60"), attendance));
    }

    @ParameterizedTest @ValueSource(strings = {"-0.01", "100.01"})
    void rejectsOutOfRangeTargetsAndAssumptions(String value) {
        assertThrows(IllegalArgumentException.class, () -> planner.plan(List.of(free("x", "100")), ScenarioAssumptions.none(), n(value), attendance));
        assertThrows(IllegalArgumentException.class, () -> new ScenarioAssumptions(Map.of("x", n(value))));
        assertThrows(IllegalArgumentException.class, () -> free("x", value));
    }

    @Test void assumptionsTakeDefensiveCopy() {
        var source = new HashMap<String, java.math.BigDecimal>();
        source.put("x", n("80"));
        var assumptions = new ScenarioAssumptions(source);
        source.put("x", n("0"));
        equalNumber("80", assumptions.lockedScores().get("x"));
        assertThrows(UnsupportedOperationException.class, () -> assumptions.lockedScores().put("y", n("1")));
    }

    @Test void precisionBeyondDisplayDoesNotHideImpossibleTarget() {
        var items = List.of(actual("Known", "50", "0"), free("Free", "50"));
        var result = planner.plan(items, ScenarioAssumptions.none(), n("50.000000000000000000000000000000000001"), attendance);
        assertEquals(TargetPlan.Status.IMPOSSIBLE, result.status());
        assertTrue(result.suggestedScores().isEmpty());
    }

    @Test void repeatingActualContributionsKeepSafeTargetsExact() {
        var items = List.of(new AssessmentItem("1", "Bagian 1", n("20"), java.util.Optional.of(new Score(n("2"), n("3")))),
                new AssessmentItem("2", "Bagian 2", n("20"), java.util.Optional.of(new Score(n("2"), n("3")))),
                new AssessmentItem("3", "Bagian 3", n("20"), java.util.Optional.of(new Score(n("2"), n("3")))), free("Sisa", "40"));
        equalNumber("50", planner.plan(items, ScenarioAssumptions.none(), n("60"), attendance).suggestedScores().get("Sisa"));
    }
}
