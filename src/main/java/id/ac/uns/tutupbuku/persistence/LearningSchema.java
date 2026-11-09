package id.ac.uns.tutupbuku.persistence;

import java.sql.*;

/** Migrasi penambahan tabel saja: tidak mengubah/menghapus tabel dan data akademik M2. */
final class LearningSchema {
    private LearningSchema() { }
    static void migrate(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE UNIQUE INDEX courses_id_owner ON courses(id,owner_nim)");
            statement.execute("""
                CREATE TABLE learning_chapters (
                  id INTEGER PRIMARY KEY AUTOINCREMENT, owner_nim TEXT NOT NULL, course_id INTEGER NOT NULL,
                  name TEXT NOT NULL CHECK(length(trim(name))>0), position INTEGER NOT NULL CHECK(position>=0), UNIQUE(id,owner_nim),
                  FOREIGN KEY(course_id,owner_nim) REFERENCES courses(id,owner_nim) ON DELETE CASCADE)
                """);
            statement.execute("""
                CREATE TABLE interest_fields (
                  id INTEGER PRIMARY KEY AUTOINCREMENT, owner_nim TEXT NOT NULL REFERENCES users(nim) ON DELETE CASCADE,
                  name TEXT NOT NULL CHECK(length(trim(name))>0), position INTEGER NOT NULL CHECK(position>=0), UNIQUE(id,owner_nim))
                """);
            statement.execute("""
                CREATE TABLE materials (
                  id INTEGER PRIMARY KEY AUTOINCREMENT, owner_nim TEXT NOT NULL,
                  chapter_id INTEGER, field_id INTEGER, title TEXT NOT NULL CHECK(length(trim(title))>0),
                  markdown TEXT NOT NULL DEFAULT '', source TEXT NOT NULL DEFAULT '', position INTEGER NOT NULL CHECK(position>=0),
                  revision INTEGER NOT NULL DEFAULT 1, updated_at TEXT NOT NULL, UNIQUE(id,owner_nim),
                  CHECK((chapter_id IS NOT NULL) != (field_id IS NOT NULL)),
                  FOREIGN KEY(chapter_id,owner_nim) REFERENCES learning_chapters(id,owner_nim) ON DELETE CASCADE,
                  FOREIGN KEY(field_id,owner_nim) REFERENCES interest_fields(id,owner_nim) ON DELETE CASCADE)
                """);
            statement.execute("""
                CREATE TABLE learning_state (
                  material_id INTEGER NOT NULL, owner_nim TEXT NOT NULL, completed INTEGER NOT NULL DEFAULT 0 CHECK(completed IN (0,1)),
                  note TEXT NOT NULL DEFAULT '', note_revision INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(material_id,owner_nim),
                  FOREIGN KEY(material_id,owner_nim) REFERENCES materials(id,owner_nim) ON DELETE CASCADE)
                """);
            // ID terakhir sengaja dipertahankan setelah materi dihapus, agar resume dapat menjelaskan kehilangan materi.
            statement.execute("""
                CREATE TABLE last_reading (
                  owner_nim TEXT PRIMARY KEY NOT NULL REFERENCES users(nim) ON DELETE CASCADE,
                  material_id INTEGER NOT NULL, material_revision INTEGER NOT NULL, scroll_ratio REAL NOT NULL CHECK(scroll_ratio>=0 AND scroll_ratio<=1),
                  location_kind TEXT NOT NULL CHECK(location_kind IN ('CHAPTER','INTEREST')), location_id INTEGER NOT NULL)
                """);
            statement.execute("CREATE TABLE learning_seed(owner_nim TEXT PRIMARY KEY NOT NULL REFERENCES users(nim) ON DELETE CASCADE, initialized_at TEXT NOT NULL)");
            statement.execute("CREATE INDEX materials_chapter_owner ON materials(chapter_id,owner_nim)");
            statement.execute("CREATE INDEX materials_field_owner ON materials(field_id,owner_nim)");
            statement.execute("PRAGMA user_version=2"); connection.commit();
        } catch (SQLException error) { connection.rollback(); throw error; }
        finally { connection.setAutoCommit(true); }
    }
    static void validate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String query : new String[]{"SELECT id,owner_nim,course_id,name,position FROM learning_chapters LIMIT 0",
                    "SELECT id,owner_nim,name,position FROM interest_fields LIMIT 0",
                    "SELECT id,owner_nim,chapter_id,field_id,title,markdown,source,position,revision,updated_at FROM materials LIMIT 0",
                    "SELECT material_id,owner_nim,completed,note,note_revision FROM learning_state LIMIT 0",
                    "SELECT owner_nim,material_id,material_revision,scroll_ratio,location_kind,location_id FROM last_reading LIMIT 0",
                    "SELECT owner_nim,initialized_at FROM learning_seed LIMIT 0"}) statement.executeQuery(query).close();
        }
    }
}
