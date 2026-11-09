package id.ac.uns.tutupbuku.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record LearningProgress(int completed, int total) {
    public LearningProgress {
        if (completed < 0 || total < completed) throw new IllegalArgumentException("Jumlah progres tidak valid.");
    }
    public String display() {
        if (total == 0) return "Belum ada materi";
        String percent = BigDecimal.valueOf(completed).multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',');
        return completed + "/" + total + " selesai (" + percent + "%)";
    }
}
