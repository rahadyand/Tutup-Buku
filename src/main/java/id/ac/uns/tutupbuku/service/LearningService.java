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
    public List<LearningFolder> folders(LearningLocation.Kind kind, long courseId) { return repository.folders(session.requireOwner(), kind, courseId); }
    public Optional<LearningFolder> folder(LearningLocation location) { return repository.findFolder(session.requireOwner(), location); }
    public LearningFolder saveFolder(long id, LearningLocation.Kind kind, long courseId, String name) { return repository.saveFolder(session.requireOwner(), new LearningFolder(id, kind, kind == LearningLocation.Kind.INTEREST ? 0 : courseId, name, 0)); }
    public void deleteFolder(LearningLocation location) { repository.deleteFolder(session.requireOwner(), location); }
    public void moveFolder(LearningLocation location, int direction) { repository.moveFolder(session.requireOwner(), location, direction); }
    public List<Material> materials(LearningLocation location) { return repository.materials(session.requireOwner(), location); }
    public MaterialReading open(long id) { return repository.find(session.requireOwner(), id).orElseThrow(() -> new IllegalArgumentException("Materi tidak ditemukan pada akun ini atau telah dihapus.")); }
    public MaterialReading save(Material material, String note, long noteRevision) { return repository.save(session.requireOwner(), material, note == null ? "" : note, noteRevision); }
    public void deleteMaterial(long id) { repository.deleteMaterial(session.requireOwner(), id); }
    public void moveMaterial(long id, int direction) { repository.moveMaterial(session.requireOwner(), id, direction); }
    public void complete(long id, boolean completed) { repository.setCompleted(session.requireOwner(), id, completed); }
    public LearningProgress progress(LearningLocation location) { return repository.progress(session.requireOwner(), location); }
    public LearningProgress courseProgress(long id) { return repository.courseProgress(session.requireOwner(), id); }
    public String importImage(Path selected) { return assets.importImage(session.requireOwner(), selected); }
    public String render(String markdown) { return renderer.render(session.requireOwner(), markdown); }
    public boolean seedInitialized() { return repository.seedInitialized(session.requireOwner()); }
    public boolean initializeStarter(long courseId) { return repository.initializeStarter(session.requireOwner(), courseId); }
    /** Tangkap pemilik saat aksi dibentuk, sehingga antrean lama tidak menulis atas nama akun yang baru login. */
    public Runnable bookmarkWriter(ReadingBookmark bookmark) {
        String owner = session.requireOwner(); return () -> repository.recordReading(owner, bookmark);
    }
    public ResumeResult resume() {
        String owner = session.requireOwner(); Optional<ReadingBookmark> last = repository.lastReading(owner);
        return resume(owner, last);
    }
    public ResumeResult resumeCourse(long courseId) {
        String owner = session.requireOwner();
        ResumeResult result = resume(owner, repository.lastCourseReading(owner, courseId));
        if (result.reading().isPresent()) {
            Material material = result.reading().orElseThrow().material();
            Optional<LearningFolder> folder = repository.findFolder(owner, material.location());
            if (material.location().kind() != LearningLocation.Kind.CHAPTER || folder.isEmpty() || folder.orElseThrow().courseId() != courseId)
                return new ResumeResult(Optional.empty(), Optional.empty(), 0, "Bacaan tidak lagi tersedia pada mata kuliah ini. Pilih materi dari daftar.");
        }
        return result;
    }
    public ResumeResult resumeReference(ReadingBookmark reference) {
        ResumeResult result = resume(session.requireOwner(), Optional.of(reference));
        if (result.reading().filter(r -> !r.material().location().equals(reference.location())).isPresent())
            return new ResumeResult(Optional.empty(), Optional.of(reference.location()), 0, "Lokasi materi berubah. Muat ulang daftar bacaan.");
        return result;
    }
    private ResumeResult resume(String owner, Optional<ReadingBookmark> last) {
        if (last.isEmpty()) return new ResumeResult(Optional.empty(), Optional.empty(), 0, "Belum ada bacaan terakhir.");
        ReadingBookmark bookmark = last.orElseThrow(); Optional<MaterialReading> reading = repository.find(owner, bookmark.materialId());
        if (reading.isEmpty()) return new ResumeResult(Optional.empty(), Optional.of(bookmark.location()), 0, "Materi terakhir telah dihapus. Silakan pilih materi dari daftar.");
        boolean changed = reading.orElseThrow().material().revision() != bookmark.revision();
        return new ResumeResult(reading, Optional.of(bookmark.location()), changed ? 0 : bookmark.scrollRatio(),
                changed ? "Isi materi berubah; bacaan dikembalikan ke awal." : "Bacaan terakhir dilanjutkan.");
    }
}
