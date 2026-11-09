package id.ac.uns.tutupbuku.persistence;

import id.ac.uns.tutupbuku.domain.*;
import java.util.List;
import java.util.Optional;

public interface LearningRepository {
    List<LearningFolder> folders(String owner, LearningLocation.Kind kind, long courseId);
    Optional<LearningFolder> findFolder(String owner, LearningLocation location);
    LearningFolder saveFolder(String owner, LearningFolder folder);
    void deleteFolder(String owner, LearningLocation location);
    void moveFolder(String owner, LearningLocation location, int direction);
    List<Material> materials(String owner, LearningLocation location);
    Optional<MaterialReading> find(String owner, long materialId);
    MaterialReading save(String owner, Material material, String note, long noteRevision);
    void deleteMaterial(String owner, long materialId);
    void moveMaterial(String owner, long materialId, int direction);
    void setCompleted(String owner, long materialId, boolean completed);
    LearningProgress progress(String owner, LearningLocation location);
    LearningProgress courseProgress(String owner, long courseId);
    void recordReading(String owner, ReadingBookmark bookmark);
    Optional<ReadingBookmark> lastReading(String owner);
    boolean seedInitialized(String owner);
    boolean initializeStarter(String owner, long courseId);
}
