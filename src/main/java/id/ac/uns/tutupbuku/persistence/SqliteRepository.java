package id.ac.uns.tutupbuku.persistence;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.security.PasswordHash;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL hanya di lapisan persistence. Semua ID akademik selalu dipasangkan dengan pemilik. */
public final class SqliteRepository implements AccountRepository, AcademicRepository {
    private final SqliteDatabase database;
    private static final String PROFILE_QUERY = """
        SELECT u.*, a.semester_id AS active_semester_id
        FROM users u LEFT JOIN active_semesters a ON a.owner_nim=u.nim WHERE u.nim=?
        """;
    public SqliteRepository(SqliteDatabase database) { this.database = database; }

    @Override public Optional<StoredAccount> findAccount(String nim) {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(PROFILE_QUERY)) {
                statement.setString(1, nim);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return Optional.empty();
                    return Optional.of(new StoredAccount(profile(result), new PasswordHash(result.getString("password_algorithm"),
                            result.getInt("password_iterations"), result.getString("password_salt"), result.getString("password_hash"))));
                }
            }
        });
    }

    private UserProfile profile(ResultSet result) throws SQLException {
        long activeId = result.getLong("active_semester_id");
        Optional<Long> active = result.wasNull() ? Optional.empty() : Optional.of(activeId);
        return new UserProfile(result.getString("nim"), result.getString("name"), result.getString("study_program"), active);
    }

    @Override public UserProfile getProfile(String nim) {
        return findAccount(nim).orElseThrow(() -> new IllegalArgumentException("Akun tidak ditemukan. Silakan masuk kembali.")).profile();
    }

    @Override public void createAccount(String nim, String name, PasswordHash hash) {
        database.write(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO users(nim,name,password_algorithm,password_iterations,password_salt,password_hash) VALUES(?,?,?,?,?,?)")) {
                statement.setString(1, nim); statement.setString(2, name); statement.setString(3, hash.algorithm());
                statement.setInt(4, hash.iterations()); statement.setString(5, hash.salt()); statement.setString(6, hash.hash());
                statement.executeUpdate();
            }
            return null;
        });
    }

    @Override public void updateProfile(String nim, String name, String studyProgram) {
        database.write(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE users SET name=?,study_program=? WHERE nim=?")) {
                statement.setString(1, name); statement.setString(2, studyProgram); statement.setString(3, nim);
                requireChanged(statement.executeUpdate());
            }
            return null;
        });
    }

    @Override public List<Semester> listSemesters(String ownerNim) {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM semesters WHERE owner_nim=? ORDER BY academic_year, CASE period WHEN 'ODD' THEN 0 ELSE 1 END, id")) {
                statement.setString(1, ownerNim);
                try (ResultSet result = statement.executeQuery()) {
                    List<Semester> semesters = new ArrayList<>();
                    while (result.next()) semesters.add(new Semester(result.getLong("id"), result.getInt("semester_number"), result.getString("academic_year"), SemesterPeriod.valueOf(result.getString("period"))));
                    return List.copyOf(semesters);
                }
            }
        });
    }

    @Override public Semester saveSemester(String ownerNim, Semester semester) {
        return database.write(connection -> {
            boolean creating = semester.id() == 0;
            String sql = creating ? "INSERT INTO semesters(semester_number,academic_year,period,owner_nim) VALUES(?,?,?,?)"
                    : "UPDATE semesters SET semester_number=?,academic_year=?,period=? WHERE owner_nim=? AND id=?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, semester.number()); statement.setString(2, semester.academicYear()); statement.setString(3, semester.period().name());
                statement.setString(4, ownerNim);
                if (!creating) statement.setLong(5, semester.id());
                requireChanged(statement.executeUpdate());
                long id = creating ? insertedId(connection) : semester.id();
                return new Semester(id, semester.number(), semester.academicYear(), semester.period());
            }
        });
    }

    @Override public void setActiveSemester(String ownerNim, long semesterId) {
        database.write(connection -> {
            requireSemester(connection, ownerNim, semesterId);
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO active_semesters(owner_nim,semester_id) VALUES(?,?) ON CONFLICT(owner_nim) DO UPDATE SET semester_id=excluded.semester_id")) {
                statement.setString(1, ownerNim); statement.setLong(2, semesterId); statement.executeUpdate();
            }
            return null;
        });
    }

    @Override public void deleteSemester(String ownerNim, long semesterId) {
        database.write(connection -> {
            // FK CASCADE menghapus matkul dan referensi aktif dalam transaksi yang sama.
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM semesters WHERE owner_nim=? AND id=?")) {
                statement.setString(1, ownerNim); statement.setLong(2, semesterId); requireChanged(statement.executeUpdate());
            }
            return null;
        });
    }

    @Override public List<Course> listCourses(String ownerNim, long semesterId) {
        return database.read(connection -> {
            requireSemester(connection, ownerNim, semesterId);
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM courses WHERE owner_nim=? AND semester_id=? ORDER BY name COLLATE NOCASE,id")) {
                statement.setString(1, ownerNim); statement.setLong(2, semesterId);
                try (ResultSet result = statement.executeQuery()) {
                    List<Course> courses = new ArrayList<>();
                    while (result.next()) courses.add(course(result));
                    return List.copyOf(courses);
                }
            }
        });
    }

    private Course course(ResultSet result) throws SQLException {
        return new Course(result.getLong("id"), result.getLong("semester_id"), result.getString("name"), result.getString("code"),
                new BigDecimal(result.getString("credits")), new Attendance(result.getBoolean("attendance_met"), result.getBoolean("attendance_confirmed")),
                result.getString("attendance_reference"), result.getLong("revision"));
    }

    @Override public Course saveCourse(String ownerNim, Course course) {
        return database.write(connection -> {
            requireSemester(connection, ownerNim, course.semesterId());
            boolean creating = course.id() == 0;
            String sql = creating ? "INSERT INTO courses(name,code,credits,attendance_met,attendance_confirmed,attendance_reference,owner_nim,semester_id) VALUES(?,?,?,?,?,?,?,?)"
                    : "UPDATE courses SET name=?,code=?,credits=?,attendance_met=?,attendance_confirmed=?,attendance_reference=?,revision=revision+1 WHERE owner_nim=? AND semester_id=? AND id=?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, course.name()); statement.setString(2, course.code()); statement.setString(3, course.credits().toPlainString());
                statement.setBoolean(4, course.attendance().met()); statement.setBoolean(5, course.attendance().confirmed());
                statement.setString(6, course.attendanceReference()); statement.setString(7, ownerNim); statement.setLong(8, course.semesterId());
                if (!creating) statement.setLong(9, course.id());
                requireChanged(statement.executeUpdate());
                long id = creating ? insertedId(connection) : course.id();
                try (PreparedStatement select = connection.prepareStatement("SELECT * FROM courses WHERE owner_nim=? AND semester_id=? AND id=?")) {
                    select.setString(1, ownerNim); select.setLong(2, course.semesterId()); select.setLong(3, id);
                    try (ResultSet result = select.executeQuery()) { if (!result.next()) throw new SQLException("Data simpan tidak ditemukan."); return course(result); }
                }
            }
        });
    }

    @Override public void deleteCourse(String ownerNim, long semesterId, long courseId) {
        database.write(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM courses WHERE owner_nim=? AND semester_id=? AND id=?")) {
                statement.setString(1, ownerNim); statement.setLong(2, semesterId); statement.setLong(3, courseId);
                requireChanged(statement.executeUpdate());
            }
            return null;
        });
    }

    private void requireSemester(Connection connection, String ownerNim, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM semesters WHERE owner_nim=? AND id=?")) {
            statement.setString(1, ownerNim); statement.setLong(2, id);
            try (ResultSet result = statement.executeQuery()) { if (!result.next()) throw new IllegalArgumentException("Semester tidak ditemukan pada akun ini."); }
        }
    }
    private void requireChanged(int rows) {
        if (rows != 1) throw new IllegalArgumentException("Data tidak ditemukan pada akun ini. Muat ulang daftar.");
    }
    private long insertedId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
            if (!result.next()) throw new SQLException("ID data baru tidak tersedia.");
            return result.getLong(1);
        }
    }
}
