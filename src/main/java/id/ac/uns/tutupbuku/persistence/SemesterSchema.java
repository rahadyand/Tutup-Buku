package id.ac.uns.tutupbuku.persistence;

import java.sql.*;

/** Rebuild satu tabel mengikuti urutan SQLite: FK off sebelum transaksi, cek sebelum commit. */
final class SemesterSchema {
    private SemesterSchema() {}

    static void migrate(Connection db) throws SQLException {
        try (Statement sql = db.createStatement()) {
            sql.execute("PRAGMA foreign_keys=OFF");
            db.setAutoCommit(false);
            try {
                long sequence = 0;
                try (ResultSet row =
                        sql.executeQuery(
                                "SELECT seq FROM sqlite_sequence WHERE name='semesters'")) {
                    if (row.next()) sequence = row.getLong(1);
                }
                sql.execute(
                        """
CREATE TABLE semesters_r001 (
    id INTEGER PRIMARY KEY AUTOINCREMENT, owner_nim TEXT NOT NULL,
    semester_number INTEGER NOT NULL CHECK(semester_number>0),
    academic_year TEXT NOT NULL, period TEXT NOT NULL CHECK(period IN ('ODD','EVEN')),
    legacy INTEGER NOT NULL DEFAULT 0 CHECK(legacy IN (0,1)),
    UNIQUE(owner_nim,semester_number,academic_year,period), UNIQUE(id,owner_nim),
    FOREIGN KEY(owner_nim) REFERENCES users(nim) ON DELETE CASCADE
)
""");
                sql.execute(
                        "INSERT INTO"
                            + " semesters_r001(id,owner_nim,semester_number,academic_year,period,legacy)"
                            + " SELECT id,owner_nim,semester_number,academic_year,period,1 FROM"
                            + " semesters");
                sql.execute("DROP TABLE semesters");
                sql.execute("ALTER TABLE semesters_r001 RENAME TO semesters");
                try (PreparedStatement update =
                        db.prepareStatement(
                                "UPDATE sqlite_sequence SET seq=MAX(seq,?) WHERE"
                                    + " name='semesters'")) {
                    update.setLong(1, sequence);
                    update.executeUpdate();
                }
                try (ResultSet row = sql.executeQuery("PRAGMA foreign_key_check")) {
                    if (row.next())
                        throw new SQLException(
                                "Relasi migrasi semester tidak konsisten; dibatalkan.");
                }
                sql.execute("PRAGMA user_version=4");
                db.commit();
            } catch (SQLException | RuntimeException error) {
                db.rollback();
                throw error;
            } finally {
                db.setAutoCommit(true);
                sql.execute("PRAGMA foreign_keys=ON");
            }
        }
    }
}
