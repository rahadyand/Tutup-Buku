package id.ac.uns.tutupbuku.service;

import static id.ac.uns.tutupbuku.service.TestData.*;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.*;
import java.util.*;

/** Oracle integer dan pencarian maju, tidak menyalin algoritme pecahan/solver produksi. */
class IndependentCalculationTest {
    private final GradeCalculator grades = new GradeCalculator();
    private final TargetPlanner targets = new TargetPlanner();
    private final Attendance present = new Attendance(true, true);

    @ParameterizedTest
    @CsvSource({
        "0,E",
        "54.99999999999999999999,E",
        "55,D",
        "59.99999999999999999999,D",
        "60,C",
        "64.99999999999999999999,C",
        "65,C_PLUS",
        "69.99999999999999999999,C_PLUS",
        "70,B",
        "74.99999999999999999999,B",
        "75,B_PLUS",
        "79.99999999999999999999,B_PLUS",
        "80,A_MINUS",
        "84.5,A_MINUS",
        "84.99999999999999999999,A_MINUS",
        "85,A",
        "100,A"
    })
    void literalPrdBoundariesThroughWeightedCalculation(String score, LetterGrade expected) {
        var result =
                grades.weighted(
                        List.of(actual("x", "100", score)), ScenarioAssumptions.none(), present);
        assertEquals(expected, result.effectiveLetter().orElseThrow());
        equalNumber(score, result.underlyingScore().orElseThrow());
        assertEquals(
                !Set.of(LetterGrade.D, LetterGrade.E).contains(expected), expected.isPassing());
    }

    @Test
    void literalNormalizationAndMissingVersusZero() {
        equalNumber("80", new Score(n("8"), n("10")).normalized());
        equalNumber("87.5", new Score(n("35"), n("40")).normalized());
        var items =
                List.of(
                        new AssessmentItem(
                                "a", "Skala 10", n("40"), Optional.of(new Score(n("8"), n("10")))),
                        new AssessmentItem(
                                "b",
                                "Skala 40",
                                n("60"),
                                Optional.of(new Score(n("35"), n("40")))));
        var result = grades.weighted(items, ScenarioAssumptions.none(), present);
        equalNumber("84.5", result.underlyingScore().orElseThrow());
        assertEquals(LetterGrade.A_MINUS, result.effectiveLetter().orElseThrow());
        assertTrue(
                grades.weighted(List.of(free("x", "100")), ScenarioAssumptions.none(), present)
                        .effectiveLetter()
                        .isEmpty());
        assertEquals(
                LetterGrade.E,
                grades.weighted(
                                List.of(actual("x", "100", "0"), free("info", "0")),
                                ScenarioAssumptions.none(),
                                present)
                        .effectiveLetter()
                        .orElseThrow());
        for (String weight : List.of("99.99", "100.01"))
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            grades.weighted(
                                    List.of(
                                            free("x", "50"),
                                            free("y", n(weight).subtract(n("50")).toPlainString())),
                                    ScenarioAssumptions.none(),
                                    present));
    }

    @Test
    void deterministicWeightedCasesUseIndependentIntegerLedger() {
        var random = new Random(20261109);
        int[] maxima = {10, 20, 40, 60};
        for (int trial = 0; trial < 40; trial++) {
            int a = 1 + random.nextInt(35), b = 1 + random.nextInt(25), c = 1 + random.nextInt(25);
            int[] weights = {a, b, c, 100 - a - b - c};
            long numerator = 0;
            var items = new ArrayList<AssessmentItem>();
            for (int i = 0; i < 4; i++) {
                int earned = random.nextInt(maxima[i] + 1);
                // 1200 habis dibagi semua maksimum: buku besar integer, tanpa normalisasi desimal.
                numerator += (long) earned * weights[i] * (1200 / maxima[i]);
                items.add(
                        new AssessmentItem(
                                "i" + i,
                                "Item " + i,
                                n("" + weights[i]),
                                Optional.of(new Score(n("" + earned), n("" + maxima[i])))));
            }
            String expected =
                    numerator >= 85 * 1200
                            ? "A"
                            : numerator >= 80 * 1200
                                    ? "A-"
                                    : numerator >= 75 * 1200
                                            ? "B+"
                                            : numerator >= 70 * 1200
                                                    ? "B"
                                                    : numerator >= 65 * 1200
                                                            ? "C+"
                                                            : numerator >= 60 * 1200
                                                                    ? "C"
                                                                    : numerator >= 55 * 1200
                                                                            ? "D"
                                                                            : "E";
            var result = grades.weighted(items, ScenarioAssumptions.none(), present);
            assertEquals(
                    expected, result.effectiveLetter().orElseThrow().label(), "kasus " + trial);
            assertEquals(
                    0,
                    n("" + numerator)
                            .divide(n("1200"), MathContext.DECIMAL128)
                            .compareTo(result.underlyingScore().orElseThrow()));
        }
    }

    @Test
    void prdSequenceDoesNotMutateActualOrLocks() {
        var items = List.of(free("a", "20"), free("b", "30"), free("c", "50"));
        var first = targets.plan(items, ScenarioAssumptions.none(), n("60"), present);
        assertEquals(3, first.suggestedScores().size());
        first.suggestedScores().values().forEach(v -> equalNumber("60", v));
        var lock = new ScenarioAssumptions(Map.of("a", n("80")));
        var second = targets.plan(items, lock, n("60"), present);
        assertEquals(2, second.suggestedScores().size());
        second.suggestedScores().values().forEach(v -> equalNumber("55", v));
        var changed = List.of(items.getFirst(), actual("b", "30", "30"), items.getLast());
        var third = targets.plan(changed, lock, n("60"), present);
        equalNumber("70", third.suggestedScores().get("c"));
        equalNumber("30", changed.get(1).actualScore().orElseThrow().earned());
        equalNumber("80", lock.lockedScores().get("a"));
        assertTrue(items.stream().allMatch(i -> i.actualScore().isEmpty()));
    }

    @Test
    void generatedTargetsMatchFirstFeasibleHundredthByForwardSearch() {
        var random = new Random(6172645);
        for (int trial = 0; trial < 32; trial++) {
            int actual = random.nextInt(101),
                    lock = random.nextInt(101),
                    target = random.nextInt(101);
            int actualWeight = 5 + random.nextInt(40), lockedWeight = 5 + random.nextInt(25);
            int freeWeight = 100 - actualWeight - lockedWeight;
            var items =
                    List.of(
                            actual("actual", "" + actualWeight, "" + actual),
                            free("locked", "" + lockedWeight),
                            free("free1", "1"),
                            free("free2", "" + (freeWeight - 1)));
            int firstFeasible = -1;
            // Uji calon 0,00..100,00 maju; oracle tidak membagi (T-K)/W.
            for (int candidate = 0; candidate <= 10000; candidate++) {
                long total =
                        (long) actual * 100 * actualWeight
                                + (long) lock * 100 * lockedWeight
                                + (long) candidate * freeWeight;
                if (total >= (long) target * 10000) {
                    firstFeasible = candidate;
                    break;
                }
            }
            var plan =
                    targets.plan(
                            items,
                            new ScenarioAssumptions(Map.of("locked", n("" + lock))),
                            n("" + target),
                            present);
            if (firstFeasible < 0) {
                assertEquals(TargetPlan.Status.IMPOSSIBLE, plan.status());
                assertTrue(plan.suggestedScores().isEmpty());
            } else {
                assertEquals(2, plan.suggestedScores().size());
                var expected = n("" + firstFeasible).movePointLeft(2);
                plan.suggestedScores()
                        .values()
                        .forEach(v -> assertEquals(0, expected.compareTo(v)));
            }
        }
    }

    @Test
    void safeMinimumImpossibleAndZeroFreeWeightHaveLiteralOutcomes() {
        var items = List.of(actual("a", "40", "90"), free("b", "60"));
        equalNumber(
                "56.67",
                targets.plan(items, ScenarioAssumptions.none(), n("70"), present)
                        .suggestedScores()
                        .get("b"));
        assertEquals(
                LetterGrade.C_PLUS,
                grades.weighted(items, new ScenarioAssumptions(Map.of("b", n("56.66"))), present)
                        .effectiveLetter()
                        .orElseThrow());
        assertEquals(
                LetterGrade.B,
                grades.weighted(items, new ScenarioAssumptions(Map.of("b", n("56.67"))), present)
                        .effectiveLetter()
                        .orElseThrow());
        var impossible =
                targets.plan(
                        List.of(actual("a", "70", "20"), free("b", "30")),
                        ScenarioAssumptions.none(),
                        n("60"),
                        present);
        assertEquals(TargetPlan.Status.IMPOSSIBLE, impossible.status());
        equalNumber("44", impossible.maximumFinalScore());
        assertTrue(impossible.rawRequiredScore().orElseThrow().compareTo(n("100")) > 0);
        assertTrue(impossible.suggestedScores().isEmpty());
        for (String target : List.of("60", "61")) {
            var plan =
                    targets.plan(
                            List.of(actual("a", "100", "60"), free("info", "0")),
                            ScenarioAssumptions.none(),
                            n(target),
                            present);
            assertEquals(
                    target.equals("60")
                            ? TargetPlan.Status.NO_FREE_REACHED
                            : TargetPlan.Status.NO_FREE_NOT_REACHED,
                    plan.status());
            assertTrue(plan.rawRequiredScore().isEmpty());
        }
    }

    @Test
    void literalGpaNumeratorsAttendanceAndBeforeRoundingComparison() {
        var courses = new ArrayList<CourseResult>();
        var letters = List.of(LetterGrade.A, LetterGrade.A, LetterGrade.C, LetterGrade.B);
        for (int i = 0; i < 4; i++)
            courses.add(
                    new CourseResult(
                            "" + i,
                            "MK" + i,
                            Optional.of(n(i == 3 ? "2" : "3")),
                            Optional.of(
                                    grades.direct(Optional.of(letters.get(i)), true, present))));
        var gpa = new SemesterGpaService();
        assertEquals(
                n("36").divide(n("11"), MathContext.DECIMAL128),
                gpa.calculate(courses, n("3.27")).value().orElseThrow());
        assertTrue(gpa.calculate(courses, n("3.2727")).targetReached().orElseThrow());
        assertFalse(gpa.calculate(courses, n("3.2728")).targetReached().orElseThrow());
        var failed = grades.direct(Optional.of(LetterGrade.C), true, new Attendance(false, true));
        assertEquals(LetterGrade.C, failed.underlyingLetter().orElseThrow());
        assertEquals(LetterGrade.E, failed.effectiveLetter().orElseThrow());
        courses.set(2, new CourseResult("2", "MK2", Optional.of(n("3")), Optional.of(failed)));
        var result = gpa.calculate(courses, n("2.73"));
        assertEquals(n("30").divide(n("11"), MathContext.DECIMAL128), result.value().orElseThrow());
        assertEquals("2,73", result.displayValue());
        assertFalse(result.targetReached().orElseThrow());
        courses.add(
                new CourseResult(
                        "missing", "Belum lengkap", Optional.of(n("3")), Optional.empty()));
        var blocked = gpa.calculate(courses, n("2"));
        assertTrue(blocked.value().isEmpty());
        assertTrue(blocked.targetReached().isEmpty());
    }
}
