package id.ac.uns.tutupbuku.domain;

import java.math.BigDecimal;
import java.util.Optional;

/** Optional kosong mempertahankan draf yang belum lengkap agar IP dapat menjelaskan masalahnya. */
public record CourseResult(String id, String name, Optional<BigDecimal> credits, Optional<GradeResult> grade) {
    public CourseResult {
        if (id == null || id.isBlank() || name == null || name.isBlank() || credits == null || grade == null) {
            throw new IllegalArgumentException("Identitas mata kuliah dan wadah nilai/SKS wajib tersedia.");
        }
    }
}
