package id.ac.uns.tutupbuku.domain;

import java.math.BigDecimal;

/** Urutan menurun penting untuk pemeriksaan ambang tanpa pembulatan tampilan. */
public enum LetterGrade {
    A("A", "85", "4.00", true),
    A_MINUS("A-", "80", "3.70", true),
    B_PLUS("B+", "75", "3.30", true),
    B("B", "70", "3.00", true),
    C_PLUS("C+", "65", "2.70", true),
    C("C", "60", "2.00", true),
    D("D", "55", "1.00", false),
    E("E", "0", "0.00", false);

    private final String label;
    private final BigDecimal minimumScore;
    private final BigDecimal gradePoint;
    private final boolean passing;

    LetterGrade(String label, String minimumScore, String gradePoint, boolean passing) {
        this.label = label;
        this.minimumScore = new BigDecimal(minimumScore);
        this.gradePoint = new BigDecimal(gradePoint);
        this.passing = passing;
    }

    public String label() { return label; }
    public BigDecimal minimumScore() { return minimumScore; }
    public BigDecimal gradePoint() { return gradePoint; }
    public boolean isPassing() { return passing; }

    public static LetterGrade fromScore(BigDecimal score) {
        if (score == null || score.signum() < 0 || score.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Nilai akhir harus antara 0 dan 100.");
        }
        for (LetterGrade grade : values()) {
            if (score.compareTo(grade.minimumScore) >= 0) return grade;
        }
        throw new IllegalStateException("Rentang nilai tidak terjangkau.");
    }
}
