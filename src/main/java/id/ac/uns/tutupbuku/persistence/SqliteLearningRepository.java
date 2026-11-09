package id.ac.uns.tutupbuku.persistence;

import id.ac.uns.tutupbuku.domain.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public final class SqliteLearningRepository implements LearningRepository {
    private final SqliteDatabase database;
    public SqliteLearningRepository(SqliteDatabase database) { this.database = database; }
    private PreparedStatement statement(Connection connection, String sql, Object... values) throws SQLException {
        PreparedStatement result = connection.prepareStatement(sql);
        try { for (int i = 0; i < values.length; i++) result.setObject(i + 1, values[i]); return result; }
        catch (SQLException error) { result.close(); throw error; }
    }
    private String folderTable(LearningLocation.Kind kind) { return kind == LearningLocation.Kind.CHAPTER ? "learning_chapters" : "interest_fields"; }
    private String locationColumn(LearningLocation location) { return location.kind() == LearningLocation.Kind.CHAPTER ? "chapter_id" : "field_id"; }
    private void changed(int count) { if (count != 1) throw new IllegalArgumentException("Data tidak ditemukan pada akun ini atau sudah berubah. Muat ulang terlebih dahulu."); }
    private long id(Connection connection) throws SQLException {
        try (var query = statement(connection, "SELECT last_insert_rowid()"); var rows = query.executeQuery()) { rows.next(); return rows.getLong(1); }
    }
    private void requireCourse(Connection connection, String owner, long courseId) throws SQLException {
        try (var query = statement(connection, "SELECT 1 FROM courses WHERE owner_nim=? AND id=?", owner, courseId); var rows = query.executeQuery()) {
            if (!rows.next()) throw new IllegalArgumentException("Pilih mata kuliah milik akun ini terlebih dahulu.");
        }
    }
    private LearningFolder folder(Connection connection, String owner, LearningLocation location) throws SQLException {
        try (var query = statement(connection, "SELECT * FROM " + folderTable(location.kind()) + " WHERE owner_nim=? AND id=?", owner, location.id()); var rows = query.executeQuery()) {
            if (!rows.next()) throw new IllegalArgumentException("Bab/bidang tidak ditemukan pada akun ini.");
            return new LearningFolder(rows.getLong("id"), location.kind(), location.kind() == LearningLocation.Kind.CHAPTER ? rows.getLong("course_id") : 0, rows.getString("name"), rows.getInt("position"));
        }
    }
    @Override public List<LearningFolder> folders(String owner, LearningLocation.Kind kind, long courseId) {
        return database.read(connection -> folders(connection, owner, kind, courseId));
    }
    @Override public Optional<LearningFolder> findFolder(String owner, LearningLocation location) {
        return database.read(connection -> {
            try { return Optional.of(folder(connection, owner, location)); }
            catch (IllegalArgumentException missing) { return Optional.empty(); }
        });
    }
    private List<LearningFolder> folders(Connection connection, String owner, LearningLocation.Kind kind, long courseId) throws SQLException {
        if (kind == LearningLocation.Kind.CHAPTER) requireCourse(connection, owner, courseId);
        String filter = kind == LearningLocation.Kind.CHAPTER ? " AND course_id=?" : "";
        Object[] arguments = kind == LearningLocation.Kind.CHAPTER ? new Object[]{owner, courseId} : new Object[]{owner};
        try (var query = statement(connection, "SELECT * FROM " + folderTable(kind) + " WHERE owner_nim=?" + filter + " ORDER BY position,id", arguments); var rows = query.executeQuery()) {
            List<LearningFolder> result = new ArrayList<>();
            while (rows.next()) result.add(new LearningFolder(rows.getLong("id"), kind, kind == LearningLocation.Kind.CHAPTER ? courseId : 0, rows.getString("name"), rows.getInt("position")));
            return List.copyOf(result);
        }
    }
    @Override public LearningFolder saveFolder(String owner, LearningFolder folder) { return database.write(connection -> saveFolder(connection, owner, folder)); }
    private LearningFolder saveFolder(Connection connection, String owner, LearningFolder value) throws SQLException {
        if (value.kind() == LearningLocation.Kind.CHAPTER) requireCourse(connection, owner, value.courseId());
        if (value.id() == 0) {
            List<LearningFolder> siblings = folders(connection, owner, value.kind(), value.courseId());
            int position = siblings.stream().mapToInt(LearningFolder::position).max().orElse(-1) + 1;
            String table = folderTable(value.kind());
            String columns = value.kind() == LearningLocation.Kind.CHAPTER ? "owner_nim,name,position,course_id" : "owner_nim,name,position";
            Object[] args = value.kind() == LearningLocation.Kind.CHAPTER ? new Object[]{owner, value.name(), position, value.courseId()} : new Object[]{owner, value.name(), position};
            try (var insert = statement(connection, "INSERT INTO " + table + "(" + columns + ") VALUES(" + (args.length == 4 ? "?,?,?,?" : "?,?,?") + ")", args)) { insert.executeUpdate(); }
            return new LearningFolder(id(connection), value.kind(), value.courseId(), value.name(), position);
        }
        LearningFolder previous = folder(connection, owner, value.location());
        if (previous.courseId() != value.courseId()) throw new IllegalArgumentException("Bab tidak dapat dipindah ke mata kuliah lain melalui edit nama.");
        try (var update = statement(connection, "UPDATE " + folderTable(value.kind()) + " SET name=? WHERE owner_nim=? AND id=?", value.name(), owner, value.id())) { changed(update.executeUpdate()); }
        return new LearningFolder(value.id(), value.kind(), value.courseId(), value.name(), previous.position());
    }
    @Override public void deleteFolder(String owner, LearningLocation location) {
        database.write(connection -> { try (var delete = statement(connection, "DELETE FROM " + folderTable(location.kind()) + " WHERE owner_nim=? AND id=?", owner, location.id())) { changed(delete.executeUpdate()); } return null; });
    }
    @Override public void moveFolder(String owner, LearningLocation location, int direction) {
        database.write(connection -> {
            LearningFolder selected = folder(connection, owner, location);
            List<Long> ids = folders(connection, owner, location.kind(), selected.courseId()).stream().map(LearningFolder::id).toList();
            reorder(connection, owner, folderTable(location.kind()), ids, location.id(), direction); return null;
        });
    }
    private void reorder(Connection connection, String owner, String table, List<Long> original, long selected, int direction) throws SQLException {
        if (direction != -1 && direction != 1) throw new IllegalArgumentException("Arah urutan tidak valid.");
        List<Long> ids = new ArrayList<>(original); int index = ids.indexOf(selected), destination = index + direction;
        if (index < 0) throw new IllegalArgumentException("Item tidak ditemukan.");
        if (destination < 0 || destination >= ids.size()) return;
        Collections.swap(ids, index, destination);
        try (var update = connection.prepareStatement("UPDATE " + table + " SET position=? WHERE owner_nim=? AND id=?")) {
            for (int i = 0; i < ids.size(); i++) { update.setInt(1, i); update.setString(2, owner); update.setLong(3, ids.get(i)); changed(update.executeUpdate()); }
        }
    }
    @Override public List<Material> materials(String owner, LearningLocation location) { return database.read(connection -> materials(connection, owner, location)); }
    private List<Material> materials(Connection connection, String owner, LearningLocation location) throws SQLException {
        folder(connection, owner, location);
        try (var query = statement(connection, "SELECT * FROM materials WHERE owner_nim=? AND " + locationColumn(location) + "=? ORDER BY position,id", owner, location.id()); var rows = query.executeQuery()) {
            List<Material> result = new ArrayList<>(); while (rows.next()) result.add(material(rows)); return List.copyOf(result);
        }
    }
    private Material material(ResultSet rows) throws SQLException {
        long chapter = rows.getLong("chapter_id"); boolean interest = rows.wasNull();
        LearningLocation location = new LearningLocation(interest ? LearningLocation.Kind.INTEREST : LearningLocation.Kind.CHAPTER, interest ? rows.getLong("field_id") : chapter);
        return new Material(rows.getLong("id"), location, rows.getString("title"), rows.getString("markdown"), rows.getString("source"), rows.getInt("position"), rows.getLong("revision"), rows.getString("updated_at"));
    }
    @Override public Optional<MaterialReading> find(String owner, long materialId) { return database.read(connection -> find(connection, owner, materialId)); }
    private Optional<MaterialReading> find(Connection connection, String owner, long materialId) throws SQLException {
        try (var query = statement(connection, "SELECT m.*,s.completed,s.note,s.note_revision FROM materials m JOIN learning_state s ON s.material_id=m.id AND s.owner_nim=m.owner_nim WHERE m.owner_nim=? AND m.id=?", owner, materialId); var rows = query.executeQuery()) {
            return rows.next() ? Optional.of(new MaterialReading(material(rows), rows.getBoolean("completed"), rows.getString("note"), rows.getLong("note_revision"))) : Optional.empty();
        }
    }
    @Override public MaterialReading save(String owner, Material value, String note, long noteRevision) {
        return database.write(connection -> save(connection, owner, value, note, noteRevision));
    }
    private MaterialReading save(Connection connection, String owner, Material value, String note, long noteRevision) throws SQLException {
        folder(connection, owner, value.location());
        long materialId = value.id();
        if (materialId == 0) {
            int position = materials(connection, owner, value.location()).stream().mapToInt(Material::position).max().orElse(-1) + 1;
            String column = locationColumn(value.location());
            try (var insert = statement(connection, "INSERT INTO materials(owner_nim," + column + ",title,markdown,source,position,revision,updated_at) VALUES(?,?,?,?,?,?,1,?)", owner, value.location().id(), value.title(), value.markdown(), value.source(), position, Instant.now().toString())) { insert.executeUpdate(); }
            materialId = id(connection);
            try (var insert = statement(connection, "INSERT INTO learning_state(material_id,owner_nim,note,note_revision) VALUES(?,?,?,1)", materialId, owner, note)) { insert.executeUpdate(); }
        } else {
            MaterialReading previous = find(connection, owner, materialId).orElseThrow(() -> new IllegalArgumentException("Materi tidak ditemukan pada akun ini."));
            if (!previous.material().location().equals(value.location())) throw new IllegalArgumentException("Materi memiliki tepat satu lokasi. Buat salinan terpisah untuk lokasi lain.");
            if (previous.material().revision() != value.revision() || previous.noteRevision() != noteRevision) throw new IllegalArgumentException("Materi/catatan sudah berubah. Salin isian Anda lalu muat ulang; perubahan lama tidak ditimpa.");
            if (!previous.material().title().equals(value.title()) || !previous.material().markdown().equals(value.markdown()) || !previous.material().source().equals(value.source())) {
                try (var update = statement(connection, "UPDATE materials SET title=?,markdown=?,source=?,revision=revision+1,updated_at=? WHERE owner_nim=? AND id=? AND revision=?", value.title(), value.markdown(), value.source(), Instant.now().toString(), owner, materialId, value.revision())) { changed(update.executeUpdate()); }
            }
            if (!previous.note().equals(note)) {
                try (var update = statement(connection, "UPDATE learning_state SET note=?,note_revision=note_revision+1 WHERE owner_nim=? AND material_id=? AND note_revision=?", note, owner, materialId, noteRevision)) { changed(update.executeUpdate()); }
            }
        }
        return find(connection, owner, materialId).orElseThrow();
    }
    @Override public void deleteMaterial(String owner, long materialId) {
        database.write(connection -> { try (var delete = statement(connection, "DELETE FROM materials WHERE owner_nim=? AND id=?", owner, materialId)) { changed(delete.executeUpdate()); } return null; });
    }
    @Override public void moveMaterial(String owner, long materialId, int direction) {
        database.write(connection -> {
            Material selected = find(connection, owner, materialId).orElseThrow(() -> new IllegalArgumentException("Materi tidak ditemukan pada akun ini.")).material();
            reorder(connection, owner, "materials", materials(connection, owner, selected.location()).stream().map(Material::id).toList(), materialId, direction); return null;
        });
    }
    @Override public void setCompleted(String owner, long materialId, boolean completed) {
        database.write(connection -> { try (var update = statement(connection, "UPDATE learning_state SET completed=? WHERE owner_nim=? AND material_id=?", completed, owner, materialId)) { changed(update.executeUpdate()); } return null; });
    }
    @Override public LearningProgress progress(String owner, LearningLocation location) {
        return database.read(connection -> { folder(connection, owner, location); return progress(connection, "SELECT s.completed FROM materials m JOIN learning_state s ON s.material_id=m.id AND s.owner_nim=m.owner_nim WHERE m.owner_nim=? AND m." + locationColumn(location) + "=?", owner, location.id()); });
    }
    @Override public LearningProgress courseProgress(String owner, long courseId) {
        return database.read(connection -> { requireCourse(connection, owner, courseId); return progress(connection, "SELECT s.completed FROM materials m JOIN learning_chapters c ON c.id=m.chapter_id AND c.owner_nim=m.owner_nim JOIN learning_state s ON s.material_id=m.id AND s.owner_nim=m.owner_nim WHERE m.owner_nim=? AND c.course_id=?", owner, courseId); });
    }
    private LearningProgress progress(Connection connection, String sql, String owner, long id) throws SQLException {
        int total = 0, complete = 0;
        try (var query = statement(connection, sql, owner, id); var rows = query.executeQuery()) { while (rows.next()) { total++; if (rows.getBoolean(1)) complete++; } }
        return new LearningProgress(complete, total);
    }
    @Override public void recordReading(String owner, ReadingBookmark bookmark) {
        database.write(connection -> {
            Material current = find(connection, owner, bookmark.materialId()).orElseThrow(() -> new IllegalArgumentException("Materi terakhir telah dihapus.")).material();
            if (!current.location().equals(bookmark.location())) throw new IllegalArgumentException("Lokasi bacaan tidak sesuai.");
            try (var update = statement(connection, "INSERT INTO last_reading(owner_nim,material_id,material_revision,scroll_ratio,location_kind,location_id) VALUES(?,?,?,?,?,?) ON CONFLICT(owner_nim) DO UPDATE SET material_id=excluded.material_id,material_revision=excluded.material_revision,scroll_ratio=excluded.scroll_ratio,location_kind=excluded.location_kind,location_id=excluded.location_id", owner, bookmark.materialId(), bookmark.revision(), bookmark.scrollRatio(), bookmark.location().kind().name(), bookmark.location().id())) { update.executeUpdate(); }
            return null;
        });
    }
    @Override public Optional<ReadingBookmark> lastReading(String owner) {
        return database.read(connection -> {
            try (var query = statement(connection, "SELECT * FROM last_reading WHERE owner_nim=?", owner); var rows = query.executeQuery()) {
                return rows.next() ? Optional.of(new ReadingBookmark(rows.getLong("material_id"), rows.getLong("material_revision"), rows.getDouble("scroll_ratio"), new LearningLocation(LearningLocation.Kind.valueOf(rows.getString("location_kind")), rows.getLong("location_id")))) : Optional.empty();
            }
        });
    }
    @Override public boolean seedInitialized(String owner) { return database.read(connection -> seeded(connection, owner)); }
    private boolean seeded(Connection connection, String owner) throws SQLException {
        try (var query = statement(connection, "SELECT 1 FROM learning_seed WHERE owner_nim=?", owner); var rows = query.executeQuery()) { return rows.next(); }
    }
    @Override public boolean initializeStarter(String owner, long courseId) {
        return database.write(connection -> {
            if (seeded(connection, owner)) return false;
            requireCourse(connection, owner, courseId);
            var chapter = saveFolder(connection, owner, new LearningFolder(0, LearningLocation.Kind.CHAPTER, courseId, "Pengantar PBO — contoh", 0));
            var field = saveFolder(connection, owner, new LearningFolder(0, LearningLocation.Kind.INTEREST, 0, "Pengantar peminatan — contoh", 0));
            String javaSource = "Bacaan lanjutan: https://dev.java/learn/classes-objects/ (contoh dan uraian ditulis asli oleh tim Tutup Buku).";
            String[][] samples = {
                {"Class dan Object", "# Class dan Object\n\n**Koleksi contoh**, bukan mata kuliah lengkap. Class mendefinisikan data dan perilaku; object adalah instansinya.\n\n```java\nclass Student {\n    String name;\n    void greet() { System.out.println(name); }\n}\nStudent student = new Student();\n```\n\nKode ditampilkan untuk dibaca, tidak dijalankan.\n\n## Ringkasan\nClass adalah rancangan; object menyimpan keadaan tiap instansi."},
                {"Encapsulation", "# Encapsulation\n\nData internal dapat diberi akses `private`. Method membantu menjaga aturan saat data berubah.\n\n```java\nclass Course {\n    private int credits;\n    void setCredits(int value) {\n        if (value <= 0) throw new IllegalArgumentException();\n        credits = value;\n    }\n}\n```\n\n## Ringkasan\nAkses terkontrol menjaga keadaan object tetap sah."},
                {"Inheritance dan Polymorphism", "# Inheritance dan Polymorphism\n\nTurunan dapat menyediakan implementasi perilaku yang sesuai melalui override.\n\n```java\nclass Material { String label() { return \"Materi\"; } }\nclass CourseMaterial extends Material {\n    @Override String label() { return \"Materi kuliah\"; }\n}\n```\n\n## Ringkasan\nGunakan hubungan pewarisan jika hubungan *is-a* masuk akal; tidak semua class perlu turunan."},
                {"Mengenal Machine Learning", "# Mengenal Machine Learning\n\nKoleksi pengantar singkat, bukan pelatihan lengkap. Model belajar pola dari data untuk membantu tugas tertentu. Hasil bergantung pada kualitas data dan evaluasi.\n\n| Tahap | Pertanyaan |\n| --- | --- |\n| Data | Apa yang mewakili masalah? |\n| Latihan | Pola apa yang dipelajari? |\n| Evaluasi | Apakah hasil berguna pada data lain? |\n\n## Ringkasan\nPisahkan data latihan dan evaluasi; periksa batas penggunaan hasil."},
                {"Mengenal Rekayasa Perangkat Lunak", "# Mengenal Rekayasa Perangkat Lunak\n\nMembangun perangkat lunak mencakup kebutuhan, desain, implementasi, pengujian dan pemeliharaan.\n\n1. Tulis perilaku yang diharapkan.\n2. Bangun bagian kecil yang dapat dijalankan.\n3. Catat hasil pengujian yang nyata.\n\n## Ringkasan\nKode, dokumentasi dan tes perlu menjelaskan perilaku yang sama."}
            };
            for (int i = 0; i < samples.length; i++) save(connection, owner,
                    new Material(0, i < 3 ? chapter.location() : field.location(), samples[i][0], samples[i][1], i < 3 ? javaSource : "Uraian pengantar asli tim Tutup Buku; tidak menggunakan gambar pihak ketiga.", 0, 0, ""), "", 0);
            try (var insert = statement(connection, "INSERT INTO learning_seed(owner_nim,initialized_at) VALUES(?,?)", owner, Instant.now().toString())) { insert.executeUpdate(); }
            return true;
        });
    }
}
