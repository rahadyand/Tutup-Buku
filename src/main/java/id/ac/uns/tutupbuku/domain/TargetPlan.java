package id.ac.uns.tutupbuku.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public record TargetPlan(Status status, BigDecimal fixedContribution, BigDecimal freeWeight,
                         Optional<BigDecimal> rawRequiredScore, Map<String, BigDecimal> suggestedScores,
                         BigDecimal minimumFinalScore, BigDecimal maximumFinalScore,
                         boolean attendanceMet) {
    public enum Status { ACHIEVABLE, ALREADY_REACHED, IMPOSSIBLE, NO_FREE_REACHED, NO_FREE_NOT_REACHED }

    public TargetPlan {
        suggestedScores = Map.copyOf(suggestedScores);
    }
}
