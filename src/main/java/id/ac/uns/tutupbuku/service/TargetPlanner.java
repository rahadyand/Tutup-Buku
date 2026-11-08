package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TargetPlanner {
    /** Target adalah minimal mencapai T; huruf E=0 bukan janji mendapat tepat E. */
    public TargetPlan plan(List<AssessmentItem> items, ScenarioAssumptions assumptions,
                           BigDecimal target, Attendance attendance) {
        AssessmentRules.validate(items, assumptions);
        if (target == null || target.signum() < 0 || target.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Target nilai harus antara 0 dan 100.");
        }
        ScoreFraction fixed = ScoreFraction.zero();
        BigDecimal freePercent = BigDecimal.ZERO;
        for (AssessmentItem item : items) {
            if (item.weight().signum() == 0) continue;
            if (!AssessmentRules.hasKnownScore(item, assumptions)) freePercent = freePercent.add(item.weight());
            else fixed = fixed.add(ScoreFraction.contribution(item, assumptions));
        }
        BigDecimal freeWeight = freePercent.movePointLeft(2);
        BigDecimal maximum = fixed.add(ScoreFraction.of(freePercent)).decimalValue();
        if (freeWeight.signum() == 0) {
            TargetPlan.Status status = fixed.compareTo(target) >= 0
                    ? TargetPlan.Status.NO_FREE_REACHED : TargetPlan.Status.NO_FREE_NOT_REACHED;
            return new TargetPlan(status, fixed.decimalValue(), freeWeight, Optional.empty(), Map.of(), fixed.decimalValue(), maximum, attendance.met());
        }
        ScoreFraction requirement = fixed.subtractFrom(target).divideBy(freeWeight);
        BigDecimal required = requirement.decimalValue();
        TargetPlan.Status status;
        Map<String, BigDecimal> suggestions = new LinkedHashMap<>();
        if (requirement.compareTo(new BigDecimal("100")) > 0) {
            status = TargetPlan.Status.IMPOSSIBLE;
            // Nilai mustahil tetap tersedia sebagai rawRequiredScore, tidak dipotong menjadi 100.
        } else {
            status = requirement.compareTo(BigDecimal.ZERO) <= 0 ? TargetPlan.Status.ALREADY_REACHED : TargetPlan.Status.ACHIEVABLE;
            // Ketelitian input awal 2 desimal; CEILING menjaga target pecahan berulang tetap aman.
            BigDecimal safeTarget = requirement.safeTarget();
            for (AssessmentItem item : items) {
                if (item.weight().signum() > 0 && !AssessmentRules.hasKnownScore(item, assumptions)) {
                    suggestions.put(item.id(), safeTarget);
                }
            }
        }
        return new TargetPlan(status, fixed.decimalValue(), freeWeight, Optional.of(required), suggestions,
                fixed.decimalValue(), maximum, attendance.met());
    }
}
