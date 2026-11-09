package id.ac.uns.tutupbuku.domain;

import java.math.BigDecimal;
import java.util.Optional;

/** Maksimum boleh disiapkan sebelum perolehan diketahui; nilai kosong tetap bukan nol. */
public record AssessmentComponent(
        String id,
        String name,
        BigDecimal weight,
        Optional<BigDecimal> earned,
        Optional<BigDecimal> maximum,
        int position) {
    public AssessmentComponent {
        if (position < 0)
            throw new IllegalArgumentException("Urutan komponen tidak boleh negatif.");
        name = name == null ? "" : name.trim();
        if (maximum.isPresent() && maximum.orElseThrow().signum() <= 0)
            throw new IllegalArgumentException("Maksimum skor harus lebih dari 0.");
        if (earned.isPresent() && maximum.isEmpty())
            throw new IllegalArgumentException("Isi maksimum untuk nilai aktual yang dimasukkan.");
        new AssessmentItem(
                id, name, weight, earned.map(value -> new Score(value, maximum.orElseThrow())));
    }

    public AssessmentItem item() {
        return new AssessmentItem(
                id, name, weight, earned.map(value -> new Score(value, maximum.orElseThrow())));
    }

    @Override
    public String toString() {
        return name
                + " · "
                + weight.stripTrailingZeros().toPlainString().replace('.', ',')
                + "% · "
                + earned.map(
                                value ->
                                        "Aktual "
                                                + value.toPlainString()
                                                + "/"
                                                + maximum.orElseThrow().toPlainString())
                        .orElse("Belum ada nilai");
    }
}
