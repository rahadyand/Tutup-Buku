package id.ac.uns.tutupbuku.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

public record GpaResult(Optional<BigDecimal> value, Optional<BigDecimal> difference,
                        Optional<Boolean> targetReached, boolean estimated, List<String> issues) {
    public GpaResult { issues = List.copyOf(issues); }

    public String displayValue() {
        return value.map(v -> v.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ','))
                .orElse("Belum lengkap");
    }

    public String label() {
        if (value.isEmpty()) return "Belum lengkap";
        return estimated ? "IP Simulasi" : "IP Berdasarkan Input Aktual";
    }
}
