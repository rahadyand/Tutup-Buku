package id.ac.uns.tutupbuku.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/** Data utama. Asumsi dan saran tidak disimpan pada item aktual. */
public record AssessmentItem(String id, String name, BigDecimal weight, Optional<Score> actualScore) {
    public AssessmentItem {
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("ID dan nama komponen wajib diisi.");
        }
        Objects.requireNonNull(weight, "Bobot wajib diisi.");
        Objects.requireNonNull(actualScore, "Gunakan Optional.empty untuk nilai kosong.");
        if (weight.signum() < 0 || weight.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Bobot " + name + " harus antara 0 dan 100.");
        }
    }

    public static AssessmentItem free(String id, String name, BigDecimal weight) {
        return new AssessmentItem(id, name, weight, Optional.empty());
    }
}
