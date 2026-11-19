package id.ac.uns.tutupbuku.domain;

public enum AgendaPriority {
    HIGH("Tinggi"),
    NORMAL("Normal"),
    LOW("Rendah");
    private final String label;

    AgendaPriority(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
