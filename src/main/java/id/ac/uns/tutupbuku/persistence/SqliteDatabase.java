package id.ac.uns.tutupbuku.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

/** Satu koneksi per aplikasi, diserialkan; tidak dibagikan ke UI atau business service. */
public final class SqliteDatabase implements AutoCloseable {
    private final Connection connection;
    @FunctionalInterface public interface SqlWork<T> { T execute(Connection connection) throws SQLException; }

    public SqliteDatabase(Path file) {
        Connection opened = null;
        try {
            Path absolute = file.toAbsolutePath().normalize();
            Files.createDirectories(absolute.getParent());
            opened = DriverManager.getConnection("jdbc:sqlite:" + absolute);
            try (Statement statement = opened.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 3000");
            }
            initialize(opened);
            connection = opened;
        } catch (Exception error) {
            if (opened != null) try { opened.close(); } catch (SQLException closeError) { error.addSuppressed(closeError); }
            throw new StorageException("Penyimpanan lokal tidak dapat dibuka. Database tidak direset. Periksa lokasi/izin lalu coba lagi.", error);
        }
    }

    private void initialize(Connection database) throws SQLException {
        try (Statement statement = database.createStatement()) {
            try (ResultSet result = statement.executeQuery("PRAGMA quick_check")) {
                if (!result.next() || !"ok".equals(result.getString(1))) throw new SQLException("Database rusak; tidak direset.");
            }
            int version;
            try (ResultSet result = statement.executeQuery("PRAGMA user_version")) { result.next(); version = result.getInt(1); }
            if (version > 1) throw new SQLException("Versi database lebih baru dari aplikasi.");
            if (version == 0) {
                try (ResultSet result = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
                    if (result.next()) throw new SQLException("Database versi 0 berisi tabel yang tidak dikenal; tidak ditimpa.");
                }
                database.setAutoCommit(false);
                try {
                    statement.execute("""
                        CREATE TABLE users (
                            nim TEXT PRIMARY KEY NOT NULL CHECK(length(trim(nim)) > 0 AND nim = trim(nim)),
                            name TEXT NOT NULL CHECK(length(trim(name)) > 0),
                            study_program TEXT NOT NULL DEFAULT '',
                            password_algorithm TEXT NOT NULL,
                            password_iterations INTEGER NOT NULL CHECK(password_iterations >= 600000),
                            password_salt TEXT NOT NULL, password_hash TEXT NOT NULL
                        )
                        """);
                    statement.execute("""
                        CREATE TABLE semesters (
                            id INTEGER PRIMARY KEY AUTOINCREMENT, owner_nim TEXT NOT NULL,
                            semester_number INTEGER NOT NULL CHECK(semester_number > 0),
                            academic_year TEXT NOT NULL, period TEXT NOT NULL CHECK(period IN ('ODD','EVEN')),
                            UNIQUE(owner_nim, academic_year, period), UNIQUE(id, owner_nim),
                            FOREIGN KEY(owner_nim) REFERENCES users(nim) ON DELETE CASCADE
                        )
                        """);
                    // Referensi aktif terpisah menghindari FK melingkar dan memastikan semester milik akun.
                    statement.execute("""
                        CREATE TABLE active_semesters (
                            owner_nim TEXT PRIMARY KEY NOT NULL REFERENCES users(nim) ON DELETE CASCADE,
                            semester_id INTEGER NOT NULL,
                            FOREIGN KEY(semester_id, owner_nim) REFERENCES semesters(id, owner_nim) ON DELETE CASCADE
                        )
                        """);
                    statement.execute("""
                        CREATE TABLE courses (
                            id INTEGER PRIMARY KEY AUTOINCREMENT, owner_nim TEXT NOT NULL, semester_id INTEGER NOT NULL,
                            name TEXT NOT NULL CHECK(length(trim(name)) > 0), code TEXT NOT NULL DEFAULT '',
                            credits TEXT NOT NULL CHECK(CAST(credits AS REAL) > 0),
                            attendance_met INTEGER NOT NULL DEFAULT 1 CHECK(attendance_met IN (0,1)),
                            attendance_confirmed INTEGER NOT NULL DEFAULT 0 CHECK(attendance_confirmed IN (0,1)),
                            attendance_reference TEXT NOT NULL, revision INTEGER NOT NULL DEFAULT 0,
                            FOREIGN KEY(semester_id, owner_nim) REFERENCES semesters(id, owner_nim) ON DELETE CASCADE
                        )
                        """);
                    statement.execute("CREATE INDEX courses_semester_owner ON courses(semester_id, owner_nim)");
                    statement.execute("PRAGMA user_version = 1");
                    database.commit();
                } catch (SQLException error) { database.rollback(); throw error; }
                finally { database.setAutoCommit(true); }
            }
            // Jangan memperbaiki skema yang tidak dikenal dengan menghapus data.
            statement.executeQuery("SELECT nim, name, study_program, password_algorithm, password_iterations, password_salt, password_hash FROM users LIMIT 0").close();
            statement.executeQuery("SELECT id, owner_nim, semester_number, academic_year, period FROM semesters LIMIT 0").close();
            statement.executeQuery("SELECT owner_nim, semester_id FROM active_semesters LIMIT 0").close();
            statement.executeQuery("SELECT id, owner_nim, semester_id, name, code, credits, attendance_met, attendance_confirmed, attendance_reference, revision FROM courses LIMIT 0").close();
            try (ResultSet result = statement.executeQuery("PRAGMA foreign_key_check")) {
                if (result.next()) throw new SQLException("Relasi database tidak konsisten.");
            }
        }
    }

    public synchronized <T> T read(SqlWork<T> work) {
        try { return work.execute(connection); }
        catch (SQLException error) { throw new StorageException("Data tidak dapat dibaca. Periksa penyimpanan lalu coba lagi.", error); }
    }

    public synchronized <T> T write(SqlWork<T> work) {
        try {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException error) {
                try { connection.rollback(); } catch (SQLException rollbackError) { error.addSuppressed(rollbackError); }
                throw error;
            } finally { connection.setAutoCommit(true); }
        } catch (SQLException error) {
            String message = error.getMessage();
            if (message != null && message.contains("UNIQUE constraint failed: users.nim")) {
                throw new IllegalArgumentException("NIM sudah terdaftar pada perangkat ini.");
            }
            if (message != null && message.contains("UNIQUE constraint failed: semesters.owner_nim")) {
                throw new IllegalArgumentException("Tahun akademik dan periode tersebut sudah ada pada akun ini.");
            }
            throw new StorageException("Perubahan gagal disimpan. Isian tetap dipertahankan; periksa penyimpanan lalu coba lagi.", error);
        }
    }

    @Override public synchronized void close() {
        try { connection.close(); }
        catch (SQLException error) { throw new StorageException("Penyimpanan tidak dapat ditutup dengan benar.", error); }
    }
}
