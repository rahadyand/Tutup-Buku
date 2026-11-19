package id.ac.uns.tutupbuku.domain;

public enum AgendaSection {
    OVERDUE("Terlambat"),
    PAST_EXAMS("Sudah lewat — ujian"),
    TODAY("Hari Ini"),
    UPCOMING("Mendatang"),
    COMPLETED("Selesai");
    private final String label;

    AgendaSection(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
