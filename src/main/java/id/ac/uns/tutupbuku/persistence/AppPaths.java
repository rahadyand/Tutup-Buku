package id.ac.uns.tutupbuku.persistence;

import java.nio.file.Path;

public final class AppPaths {
    private AppPaths() { }
    public static Path databaseFile() {
        String override = System.getenv("TUTUP_BUKU_DATA_DIR");
        if (override != null && !override.isBlank()) return Path.of(override).toAbsolutePath().resolve("tutup-buku.db");
        String local = System.getenv("LOCALAPPDATA");
        Path base = local == null || local.isBlank()
                ? Path.of(System.getProperty("user.home"), "AppData", "Local") : Path.of(local);
        return base.resolve("TutupBuku").resolve("tutup-buku.db");
    }
}
