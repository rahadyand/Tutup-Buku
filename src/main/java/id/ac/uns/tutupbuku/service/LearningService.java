package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class LearningService {
    private final LearningRepository repository;
    private final UserSession session;
    private final AssetStore assets;
    private final MarkdownRenderer renderer;
    public LearningService(LearningRepository repository, UserSession session, AssetStore assets) {
        this.repository = repository; this.session = session; this.assets = assets; renderer = new MarkdownRenderer(assets);
    }
    public List<LearningFolder> folders(LearningLocation.Kind kind, long courseId) { return repository.folders(session.requireNim(), kind, courseId); }
    public Optional<LearningFolder> folder(LearningLocation location) { return repository.findFolder(session.requireNim(), location); }
    public LearningFolder saveFolder(long id, LearningLocation.Kind kind, long courseId, String name) { return repository.saveFolder(session.requireNim(), new LearningFolder(id, kind, kind == LearningLocation.Kind.INTEREST ? 0 : courseId, name, 0)); }
    public void deleteFolder(LearningLocation location) { repository.deleteFolder(session.requireNim(), location); }
    public void moveFolder(LearningLocation location, int direction) { repository.moveFolder(session.requireNim(), location, direction); }
    public List<Material> materials(LearningLocation location) { return repository.materials(session.requireNim(), location); }
    public MaterialReading open(long id) { return repository.find(session.requireNim(), id).orElseThrow(() -> new IllegalArgumentException("Materi tidak ditemukan pada akun ini atau telah dihapus.")); }
    public MaterialReading save(Material material, String note, long noteRevision) { return repository.save(session.requireNim(), material, note == null ? "" : note, noteRevision); }
    public void deleteMaterial(long id) { repository.deleteMaterial(session.requireNim(), id); }
    public void moveMaterial(long id, int direction) { repository.moveMaterial(session.requireNim(), id, direction); }
    public void complete(long id, boolean completed) { repository.setCompleted(session.requireNim(), id, completed); }
    public LearningProgress progress(LearningLocation location) { return repository.progress(session.requireNim(), location); }
    public LearningProgress courseProgress(long id) { return repository.courseProgress(session.requireNim(), id); }
    public String importImage(Path selected) { return assets.importImage(session.requireNim(), selected); }
    public String render(String markdown) { return renderer.render(session.requireNim(), markdown); }
    public boolean seedInitialized() { return repository.seedInitialized(session.requireNim()); }
    public boolean initializeStarter(long courseId) { return repository.initializeStarter(session.requireNim(), courseId); }
    /** Tangkap pemilik saat aksi dibentuk, sehingga antrean lama tidak menulis atas nama akun yang baru login. */
    public Runnable bookmarkWriter(ReadingBookmark bookmark) {
        String owner = session.requireNim(); return () -> repository.recordReading(owner, bookmark);
    }
    public ResumeResult resume() {
        String owner = session.requireNim(); Optional<ReadingBookmark> last = repository.lastReading(owner);
        if (last.isEmpty()) return new ResumeResult(Optional.empty(), Optional.empty(), 0, "Belum ada bacaan terakhir.");
        ReadingBookmark bookmark = last.orElseThrow(); Optional<MaterialReading> reading = repository.find(owner, bookmark.materialId());
        if (reading.isEmpty()) return new ResumeResult(Optional.empty(), Optional.of(bookmark.location()), 0, "Materi terakhir telah dihapus. Silakan pilih materi dari daftar.");
        boolean changed = reading.orElseThrow().material().revision() != bookmark.revision();
        return new ResumeResult(reading, Optional.of(bookmark.location()), changed ? 0 : bookmark.scrollRatio(),
                changed ? "Isi materi berubah; bacaan dikembalikan ke awal." : "Bacaan terakhir dilanjutkan.");
    }
}
