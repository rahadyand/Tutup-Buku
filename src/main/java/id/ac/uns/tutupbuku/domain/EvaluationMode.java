package id.ac.uns.tutupbuku.domain;

public enum EvaluationMode {
    DETAIL("Detail komponen"),
    DIRECT("Huruf langsung");
    private final String label;

    EvaluationMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
