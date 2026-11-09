package id.ac.uns.tutupbuku.domain;

public record ReadingBookmark(long materialId, long revision, double scrollRatio, LearningLocation location) {
    public ReadingBookmark {
        if (!Double.isFinite(scrollRatio) || scrollRatio < 0 || scrollRatio > 1) throw new IllegalArgumentException("Posisi bacaan harus antara 0 dan 1.");
    }
}
