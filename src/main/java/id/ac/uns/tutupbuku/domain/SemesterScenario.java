package id.ac.uns.tutupbuku.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SemesterScenario(
        long id,
        long semesterId,
        String name,
        BigDecimal targetGpa,
        long version,
        int ruleVersion,
        Instant updatedAt,
        List<ScenarioCourse> courses) {
    public SemesterScenario {
        name = name == null ? "" : name.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Nama skenario wajib diisi.");
        if (targetGpa == null
                || targetGpa.signum() < 0
                || targetGpa.compareTo(new BigDecimal("4")) > 0)
            throw new IllegalArgumentException("Target IP harus antara 0 dan 4,00.");
        courses = List.copyOf(courses);
    }

    @Override
    public String toString() {
        return name;
    }
}
