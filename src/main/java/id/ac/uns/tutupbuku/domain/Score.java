package id.ac.uns.tutupbuku.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

/** Skor asli disimpan agar skala perolehan/maksimum tidak hilang. */
public record Score(BigDecimal earned, BigDecimal maximum) {
    public Score {
        Objects.requireNonNull(earned, "Perolehan wajib diisi.");
        Objects.requireNonNull(maximum, "Maksimum wajib diisi.");
        if (maximum.signum() <= 0 || earned.signum() < 0 || earned.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("Maksimum harus positif; perolehan harus antara 0 dan maksimum.");
        }
    }

    public BigDecimal normalized() {
        return earned.multiply(new BigDecimal("100")).divide(maximum, MathContext.DECIMAL128);
    }
}
