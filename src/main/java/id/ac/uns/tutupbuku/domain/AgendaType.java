package id.ac.uns.tutupbuku.domain;

public enum AgendaType {
    TASK("Tugas"),
    EXAM("Ujian");
    private final String label;

    AgendaType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
