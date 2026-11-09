package id.ac.uns.tutupbuku.domain;

import java.util.Optional;

public record ResumeResult(Optional<MaterialReading> reading, Optional<LearningLocation> previousLocation,
                           double scrollRatio, String message) { }
