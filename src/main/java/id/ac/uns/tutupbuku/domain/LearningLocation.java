package id.ac.uns.tutupbuku.domain;

/** Materi hanya berada pada satu bab kuliah ATAU satu bidang peminatan. */
public record LearningLocation(Kind kind, long id) {
    public enum Kind { CHAPTER, INTEREST }
    public LearningLocation { if (kind == null || id <= 0) throw new IllegalArgumentException("Pilih bab atau bidang peminatan."); }
}
