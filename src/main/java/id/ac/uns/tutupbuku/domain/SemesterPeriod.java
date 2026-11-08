package id.ac.uns.tutupbuku.domain;

public enum SemesterPeriod {
    ODD("Ganjil"), EVEN("Genap");
    private final String label;
    SemesterPeriod(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
