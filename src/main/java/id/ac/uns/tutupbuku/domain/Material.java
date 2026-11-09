package id.ac.uns.tutupbuku.domain;

public record Material(long id, LearningLocation location, String title, String markdown, String source,
                       int position, long revision, String updatedAt) {
    public Material {
        title = title == null ? "" : title.trim();
        if (title.isEmpty()) throw new IllegalArgumentException("Judul materi wajib diisi.");
        if (location == null) throw new IllegalArgumentException("Lokasi materi wajib dipilih.");
        markdown = markdown == null ? "" : markdown; source = source == null ? "" : source;
    }
    @Override public String toString() { return title + (markdown.isBlank() ? " [Draf]" : ""); }
}
