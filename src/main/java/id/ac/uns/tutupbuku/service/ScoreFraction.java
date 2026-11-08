package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.AssessmentItem;
import id.ac.uns.tutupbuku.domain.ScenarioAssumptions;
import id.ac.uns.tutupbuku.domain.LetterGrade;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Pecahan internal agar 2/3 + 2/3 + 2/3 tidak turun di bawah ambang akibat pembulatan.
 * Numerator dan denominator memakai operasi BigDecimal eksak; pembagian hanya saat mengeluarkan angka.
 * Class ini tidak perlu diketahui UI atau repository.
 */
record ScoreFraction(BigDecimal numerator, BigDecimal denominator) {
    static ScoreFraction zero() { return of(BigDecimal.ZERO); }
    static ScoreFraction of(BigDecimal value) { return new ScoreFraction(value, BigDecimal.ONE); }

    static ScoreFraction contribution(AssessmentItem item, ScenarioAssumptions assumptions) {
        if (item.actualScore().isPresent()) {
            var score = item.actualScore().orElseThrow();
            // (100 × earned / maximum) × weight / 100 = earned × weight / maximum.
            return new ScoreFraction(score.earned().multiply(item.weight()), score.maximum());
        }
        return of(assumptions.lockedScores().get(item.id()).multiply(item.weight()).movePointLeft(2));
    }

    ScoreFraction add(ScoreFraction other) {
        return new ScoreFraction(numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator));
    }

    ScoreFraction subtractFrom(BigDecimal value) {
        return new ScoreFraction(value.multiply(denominator).subtract(numerator), denominator);
    }

    ScoreFraction divideBy(BigDecimal positiveValue) {
        if (positiveValue.signum() <= 0) throw new IllegalArgumentException("Pembagi harus positif.");
        return new ScoreFraction(numerator, denominator.multiply(positiveValue));
    }

    int compareTo(BigDecimal value) { return numerator.compareTo(value.multiply(denominator)); }
    BigDecimal decimalValue() { return numerator.divide(denominator, MathContext.DECIMAL128); }
    BigDecimal safeTarget() { return numerator.max(BigDecimal.ZERO).divide(denominator, 2, RoundingMode.CEILING); }

    LetterGrade letterGrade() {
        for (var grade : LetterGrade.values()) {
            if (compareTo(grade.minimumScore()) >= 0) return grade;
        }
        throw new IllegalStateException("Kontribusi nilai negatif.");
    }
}
