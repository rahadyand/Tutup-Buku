package id.ac.uns.tutupbuku.domain;

/** Urutan langkah adalah urutan List pada AgendaItem, bukan identitas langkah. */
public record ChecklistStep(String id, String text, boolean completed) {
    public ChecklistStep {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("Identitas langkah wajib diisi.");
        if (text == null || text.isBlank())
            throw new IllegalArgumentException("Teks langkah wajib diisi.");
        text = text.strip();
    }
}
