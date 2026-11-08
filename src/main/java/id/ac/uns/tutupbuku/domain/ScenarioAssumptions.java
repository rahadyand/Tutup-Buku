package id.ac.uns.tutupbuku.domain;

import java.math.BigDecimal;
import java.util.Map;

/** Hanya asumsi yang sengaja dikunci pengguna. Hasil solver dikembalikan terpisah. */
public record ScenarioAssumptions(Map<String, BigDecimal> lockedScores) {
    public ScenarioAssumptions {
        lockedScores = Map.copyOf(lockedScores);
        lockedScores.forEach((id, score) -> {
            if (id.isBlank() || score.signum() < 0 || score.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Asumsi wajib memiliki ID dan nilai normalisasi 0–100.");
            }
        });
    }

    public static ScenarioAssumptions none() {
        return new ScenarioAssumptions(Map.of());
    }
}
