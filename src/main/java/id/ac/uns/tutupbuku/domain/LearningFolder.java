package id.ac.uns.tutupbuku.domain;

public record LearningFolder(long id, LearningLocation.Kind kind, long courseId, String name, int position) {
    public LearningFolder {
        name = name == null ? "" : name.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Nama bab/bidang wajib diisi.");
    }
    public LearningLocation location() { return new LearningLocation(kind, id); }
    @Override public String toString() { return name; }
}
