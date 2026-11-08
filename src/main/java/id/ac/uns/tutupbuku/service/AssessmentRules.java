package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.AssessmentItem;
import id.ac.uns.tutupbuku.domain.ScenarioAssumptions;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validasi yang sama dipakai kalkulator dan solver, tanpa menormalkan bobot diam-diam. */
final class AssessmentRules {
    private AssessmentRules() { }

    static void validate(List<AssessmentItem> items, ScenarioAssumptions assumptions) {
        if (items.isEmpty()) throw new IllegalArgumentException("Belum ada komponen; jumlah bobot 0%, wajib 100%.");
        Set<String> ids = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (AssessmentItem item : items) {
            if (!ids.add(item.id())) throw new IllegalArgumentException("ID komponen duplikat: " + item.id());
            total = total.add(item.weight());
            if (item.actualScore().isPresent() && assumptions.lockedScores().containsKey(item.id())) {
                throw new IllegalArgumentException("Asumsi " + item.name() + " bertabrakan dengan aktual; tinjau skenario.");
            }
        }
        if (total.compareTo(new BigDecimal("100")) != 0) {
            throw new IllegalArgumentException("Jumlah bobot " + total.toPlainString() + "%, wajib tepat 100% (struktur komponen).");
        }
        for (String id : assumptions.lockedScores().keySet()) {
            if (!ids.contains(id)) throw new IllegalArgumentException("Asumsi mengacu komponen yang tidak tersedia: " + id);
        }
    }

    static boolean hasKnownScore(AssessmentItem item, ScenarioAssumptions assumptions) {
        return item.actualScore().isPresent() || assumptions.lockedScores().containsKey(item.id());
    }
}
