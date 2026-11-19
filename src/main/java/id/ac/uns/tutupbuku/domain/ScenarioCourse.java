package id.ac.uns.tutupbuku.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public record ScenarioCourse(
        ActualCourse basis,
        EvaluationMode mode,
        Optional<LetterGrade> estimatedLetter,
        BigDecimal targetScore,
        Optional<LetterGrade> targetLetter,
        Attendance attendance,
        Map<String, BigDecimal> lockedScores,
        Map<String, BigDecimal> overriddenScores,
        boolean projectionEnabled) {
    public ScenarioCourse {
        if (mode == null || attendance == null)
            throw new IllegalArgumentException("Pilih mode nilai dan asumsi kehadiran.");
        if (targetScore == null
                || targetScore.signum() < 0
                || targetScore.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("Target nilai harus antara 0 dan 100.");
        if (targetLetter.isPresent()
                && targetScore.compareTo(targetLetter.orElseThrow().minimumScore()) != 0)
            throw new IllegalArgumentException(
                    "Target huruf harus memakai ambang minimum yang sesuai.");
        lockedScores = new ScenarioAssumptions(lockedScores).lockedScores();
        overriddenScores = new ScenarioAssumptions(overriddenScores).lockedScores();
        for (String id : overriddenScores.keySet()) {
            if (lockedScores.containsKey(id)
                    || basis.components().stream()
                            .noneMatch(
                                    item ->
                                            item.id().equals(id)
                                                    && item.earned().isPresent()
                                                    && item.weight().signum() > 0))
                throw new IllegalArgumentException(
                        "Override harus merujuk komponen aktual berbobot positif pada basis yang"
                            + " sama.");
        }
    }

    /** Skenario lama tetap memakai perilaku proyeksi yang sama sebelum schema 5. */
    public ScenarioCourse(
            ActualCourse basis,
            EvaluationMode mode,
            Optional<LetterGrade> estimatedLetter,
            BigDecimal targetScore,
            Optional<LetterGrade> targetLetter,
            Attendance attendance,
            Map<String, BigDecimal> lockedScores) {
        this(
                basis,
                mode,
                estimatedLetter,
                targetScore,
                targetLetter,
                attendance,
                lockedScores,
                Map.of(),
                true);
    }

    public static ScenarioCourse actualOnly(ActualCourse course) {
        return new ScenarioCourse(
                course,
                course.mode(),
                Optional.empty(),
                new BigDecimal("60"),
                Optional.of(LetterGrade.C),
                course.course().attendance(),
                Map.of(),
                Map.of(),
                false);
    }

    public static ScenarioCourse fromActual(ActualCourse course) {
        return new ScenarioCourse(
                course,
                course.mode(),
                Optional.empty(),
                new BigDecimal("60"),
                Optional.of(LetterGrade.C),
                course.course().attendance(),
                Map.of());
    }

    @Override
    public String toString() {
        return basis.course().toString();
    }
}
