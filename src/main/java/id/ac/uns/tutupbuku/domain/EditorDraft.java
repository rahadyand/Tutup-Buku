package id.ac.uns.tutupbuku.domain;

/** Buffer editor terpisah dari data tersimpan. Ack save lama tidak menghilangkan edit yang lebih baru. */
public final class EditorDraft {
    private MaterialReading base;
    private String title, markdown, source, note;
    private long generation;
    private long savedGeneration;
    public EditorDraft(MaterialReading reading) { reset(reading); }
    public void reset(MaterialReading reading) {
        base = reading; title = reading.material().title(); markdown = reading.material().markdown(); source = reading.material().source(); note = reading.note();
        generation = 0; savedGeneration = 0;
    }
    public void edit(String title, String markdown, String source, String note) {
        if (!this.title.equals(title) || !this.markdown.equals(markdown) || !this.source.equals(source) || !this.note.equals(note)) generation++;
        this.title = title; this.markdown = markdown; this.source = source; this.note = note;
    }
    public SaveSnapshot snapshot() {
        Material previous = base.material();
        return new SaveSnapshot(new Material(previous.id(), previous.location(), title, markdown, source, previous.position(), previous.revision(), previous.updatedAt()), note, base.noteRevision(), generation);
    }
    public void acknowledge(SaveSnapshot snapshot, MaterialReading saved) {
        base = saved;
        if (generation == snapshot.generation()) savedGeneration = generation;
    }
    public boolean dirty() { return base.material().id() == 0 || generation != savedGeneration; }
    public void setCompleted(boolean completed) { base = new MaterialReading(base.material(), completed, base.note(), base.noteRevision()); }
    public MaterialReading base() { return base; }
    public record SaveSnapshot(Material material, String note, long noteRevision, long generation) { }
}
