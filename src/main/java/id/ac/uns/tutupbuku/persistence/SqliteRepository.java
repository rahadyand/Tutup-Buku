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
    private static final String PROFILE_QUERY =
            """
            SELECT u.*, a.semester_id AS active_semester_id
            FROM users u LEFT JOIN active_semesters a ON a.owner_nim=u.nim WHERE u.nim=?
            """;

    public SqliteRepository(SqliteDatabase database) {
        this.database = database;
    }

    @Override
    public Optional<StoredAccount> findAccount(String nim) {
        return database.read(
                connection -> {
                    try (PreparedStatement statement = connection.prepareStatement(PROFILE_QUERY)) {
                        statement.setString(1, nim);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) return Optional.empty();
                            return Optional.of(
                                    new StoredAccount(
                                            profile(result),
                                            new PasswordHash(
                                                    result.getString("password_algorithm"),
                                                    result.getInt("password_iterations"),
                                                    result.getString("password_salt"),
                                                    result.getString("password_hash"))));
                        }
                    }
                });
    }

    private UserProfile profile(ResultSet result) throws SQLException {
        long activeId = result.getLong("active_semester_id");
        Optional<Long> active = result.wasNull() ? Optional.empty() : Optional.of(activeId);
        return new UserProfile(
                result.getString("nim"),
                result.getString("name"),
                result.getString("study_program"),
                active);
    }

    @Override
    public UserProfile getProfile(String nim) {
        return findAccount(nim)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Akun tidak ditemukan. Silakan masuk kembali."))
                .profile();
    }

    @Override
    public void createAccount(String nim, String name, PasswordHash hash) {
        database.write(
                connection -> {
                    try (PreparedStatement statement =
                            connection.prepareStatement(
                                    "INSERT INTO"
                                        + " users(nim,name,password_algorithm,password_iterations,password_salt,password_hash)"
                                        + " VALUES(?,?,?,?,?,?)")) {
                        statement.setString(1, nim);
                        statement.setString(2, name);
                        statement.setString(3, hash.algorithm());
                        statement.setInt(4, hash.iterations());
                        statement.setString(5, hash.salt());
                        statement.setString(6, hash.hash());
                        statement.executeUpdate();
                    }
                    return null;
                });
    }

    @Override
    public void updateProfile(String nim, String name, String studyProgram) {
        database.write(
                connection -> {
                    try (PreparedStatement statement =
                            connection.prepareStatement(
                                    "UPDATE users SET name=?,study_program=? WHERE nim=?")) {
                        statement.setString(1, name);
                        statement.setString(2, studyProgram);
                        statement.setString(3, nim);
                        requireChanged(statement.executeUpdate());
                    }
                    return null;
                });
    }

    @Override
    public List<Semester> listSemesters(String ownerNim) {
        return database.read(
                connection -> {
                    try (PreparedStatement statement =
                            connection.prepareStatement(
                                    "SELECT * FROM semesters WHERE owner_nim=? ORDER BY"
                                        + " academic_year, semester_number, CASE period WHEN 'ODD'"
                                        + " THEN 0 ELSE 1 END, id")) {
                        statement.setString(1, ownerNim);
                        try (ResultSet result = statement.executeQuery()) {
                            List<Semester> semesters = new ArrayList<>();
                            while (result.next()) semesters.add(semester(result));
                            return List.copyOf(semesters);
                        }
                    }
                });
    }

    @Override
    public Semester saveSemester(String ownerNim, Semester semester) {
        return database.write(
                connection -> {
                    // API lama dipertahankan bagi fixture/kompatibilitas, bukan alur UI
                    // Revision001.
                    try (PreparedStatement check =
                            connection.prepareStatement(
                                    "SELECT 1 FROM semesters WHERE owner_nim=? AND academic_year=?"
                                        + " AND period=? AND id<>?")) {
                        check.setString(1, ownerNim);
                        check.setString(2, semester.academicYear());
                        check.setString(3, semester.period().name());
                        check.setLong(4, semester.id());
                        try (ResultSet row = check.executeQuery()) {
                            if (row.next())
                                throw new IllegalArgumentException(
                                        "Tahun akademik dan periode tersebut sudah ada pada akun"
                                            + " ini.");
                        }
                    }
                    boolean creating = semester.id() == 0;
                    String sql =
                            creating
                                    ? "INSERT INTO"
                                          + " semesters(semester_number,academic_year,period,owner_nim)"
                                          + " VALUES(?,?,?,?)"
                                    : "UPDATE semesters SET"
                                          + " semester_number=?,academic_year=?,period=? WHERE"
                                          + " owner_nim=? AND id=?";
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setInt(1, semester.number());
                        statement.setString(2, semester.academicYear());
                        statement.setString(3, semester.period().name());
                        statement.setString(4, ownerNim);
                        if (!creating) statement.setLong(5, semester.id());
                        requireChanged(statement.executeUpdate());
                        long id = creating ? insertedId(connection) : semester.id();
                        return new Semester(
                                id, semester.number(), semester.academicYear(), semester.period());
                    }
                });
    }

    private Semester semester(ResultSet row) throws SQLException {
        return new Semester(
                row.getLong("id"),
                row.getInt("semester_number"),
                row.getString("academic_year"),
                SemesterPeriod.valueOf(row.getString("period")),
                row.getBoolean("legacy"));
    }

    @Override
    public Semester saveSemesterAndActivate(String owner, Semester value, boolean activate) {
        return database.write(
                connection -> {
                    Semester previous = null;
                    if (value.id() != 0) {
                        try (var query =
                                connection.prepareStatement(
                                        "SELECT * FROM semesters WHERE owner_nim=? AND id=?")) {
                            query.setString(1, owner);
                            query.setLong(2, value.id());
                            try (var rows = query.executeQuery()) {
                                if (!rows.next())
                                    throw new IllegalArgumentException(
                                            "Semester tidak ditemukan pada akun ini.");
                                previous = semester(rows);
                            }
                        }
                    }
                    boolean unchangedIdentity =
                            previous != null
                                    && previous.number() == value.number()
                                    && previous.year().equals(value.year());
                    if (!unchangedIdentity) {
                        try (var query =
                                connection.prepareStatement(
                                        "SELECT 1 FROM semesters WHERE owner_nim=? AND"
                                            + " semester_number=? AND substr(academic_year,1,4)=?"
                                            + " AND id<>?")) {
                            query.setString(1, owner);
                            query.setInt(2, value.number());
                            query.setString(3, value.year());
                            query.setLong(4, value.id());
                            try (var rows = query.executeQuery()) {
                                if (rows.next())
                                    throw new IllegalArgumentException(
                                            "Nomor semester dan tahun tersebut sudah ada. Pilih"
                                                + " semester tersimpan untuk mengedit.");
                            }
                        }
                    }
                    String sql =
                            value.id() == 0
                                    ? "INSERT INTO"
                                          + " semesters(owner_nim,semester_number,academic_year,period,legacy)"
                                          + " VALUES(?,?,?,?,0)"
                                    : "UPDATE semesters SET"
                                          + " owner_nim=?,semester_number=?,academic_year=?,period=?,legacy=?"
                                          + " WHERE owner_nim=? AND id=?";
                    try (var save = connection.prepareStatement(sql)) {
                        save.setString(1, owner);
                        save.setInt(2, value.number());
                        save.setString(3, value.academicYear());
                        // Periode historis tidak diganti berdasarkan dugaan paritas.
                        save.setString(
                                4,
                                previous == null
                                        ? value.period().name()
                                        : previous.period().name());
                        if (previous != null) {
                            save.setBoolean(5, previous.legacy());
                            save.setString(6, owner);
                            save.setLong(7, value.id());
                        }
                        requireChanged(save.executeUpdate());
                    }
                    long id = value.id() == 0 ? insertedId(connection) : value.id();
                    if (activate) {
                        try (var save =
                                connection.prepareStatement(
                                        "INSERT INTO active_semesters(owner_nim,semester_id)"
                                            + " VALUES(?,?) ON CONFLICT(owner_nim) DO UPDATE SET"
                                            + " semester_id=excluded.semester_id")) {
                            save.setString(1, owner);
                            save.setLong(2, id);
                            save.executeUpdate();
                        }
                    }
                    try (var query =
                            connection.prepareStatement(
                                    "SELECT * FROM semesters WHERE owner_nim=? AND id=?")) {
                        query.setString(1, owner);
                        query.setLong(2, id);
                        try (var row = query.executeQuery()) {
                            row.next();
                            return semester(row);
                        }
                    }
                });
    }

    @Override
    public Course saveCourseMetadata(
            String owner, long id, long semesterId, String name, String code, BigDecimal credits) {
        return database.write(
                connection -> {
                    long courseId = id;
                    requireSemester(connection, owner, semesterId);
                    Course value =
                            new Course(
                                    courseId,
                                    semesterId,
                                    name,
                                    code,
                                    credits,
                                    Attendance.assumedMet(),
                                    Course.DEFAULT_ATTENDANCE_REFERENCE,
                                    0);
                    if (courseId == 0) {
                        try (var save =
                                connection.prepareStatement(
                                        "INSERT INTO"
                                            + " courses(owner_nim,semester_id,name,code,credits,attendance_reference)"
                                            + " VALUES(?,?,?,?,?,?)")) {
                            save.setString(1, owner);
                            save.setLong(2, semesterId);
                            save.setString(3, value.name());
                            save.setString(4, value.code());
                            save.setString(5, value.credits().toPlainString());
                            save.setString(6, value.attendanceReference());
                            save.executeUpdate();
                        }
                        courseId = insertedId(connection);
                    } else {
                        // UPDATE ini sengaja tidak menyebut kolom kehadiran, juga aman terhadap
                        // data lama UI.
                        try (var save =
                                connection.prepareStatement(
                                        "UPDATE courses SET"
                                            + " name=?,code=?,credits=?,revision=revision+1 WHERE"
                                            + " owner_nim=? AND semester_id=? AND id=?")) {
                            save.setString(1, value.name());
                            save.setString(2, value.code());
                            save.setString(3, credits.toPlainString());
                            save.setString(4, owner);
                            save.setLong(5, semesterId);
                            save.setLong(6, courseId);
                            requireChanged(save.executeUpdate());
                        }
                    }
                    try (var query =
                            connection.prepareStatement(
                                    "SELECT * FROM courses WHERE owner_nim=? AND semester_id=? AND"
                                        + " id=?")) {
                        query.setString(1, owner);
                        query.setLong(2, semesterId);
                        query.setLong(3, courseId);
                        try (var row = query.executeQuery()) {
                            row.next();
                            return course(row);
                        }
                    }
                });
    }

    @Override
    public void setActiveSemester(String ownerNim, long semesterId) {
        database.write(
                connection -> {
                    requireSemester(connection, ownerNim, semesterId);
                    try (PreparedStatement statement =
                            connection.prepareStatement(
                                    "INSERT INTO active_semesters(owner_nim,semester_id)"
                                        + " VALUES(?,?) ON CONFLICT(owner_nim) DO UPDATE SET"
                                        + " semester_id=excluded.semester_id")) {
                        statement.setString(1, ownerNim);
                        statement.setLong(2, semesterId);
                        statement.executeUpdate();
                    }
                    return null;
                });
    }

    @Override
    public void deleteSemester(String ownerNim, long semesterId) {
        database.write(
                connection -> {
                    // FK CASCADE menghapus matkul dan referensi aktif dalam transaksi yang sama.
                    try (PreparedStatement statement =
                            connection.prepareStatement(
                                    "DELETE FROM semesters WHERE owner_nim=? AND id=?")) {
                        statement.setString(1, ownerNim);
                        statement.setLong(2, semesterId);
                        requireChanged(statement.executeUpdate());
                    }
                    return null;
                });
    }

    @Override
    public List<Course> listCourses(String ownerNim, long semesterId) {
        return database.read(
                connection -> {
                    requireSemester(connection, ownerNim, semesterId);
                    try (PreparedStatement statement =
                            connection.prepareStatement(
                                    "SELECT * FROM courses WHERE owner_nim=? AND semester_id=?"
                                        + " ORDER BY name COLLATE NOCASE,id")) {
                        statement.setString(1, ownerNim);
                        statement.setLong(2, semesterId);
                        try (ResultSet result = statement.executeQuery()) {
                            List<Course> courses = new ArrayList<>();
                            while (result.next()) courses.add(course(result));
                            return List.copyOf(courses);
                        }
                    }
                });
    }

    private Course course(ResultSet result) throws SQLException {
        return new Course(
                result.getLong("id"),
                result.getLong("semester_id"),
                result.getString("name"),
                result.getString("code"),
                new BigDecimal(result.getString("credits")),
                new Attendance(
                        result.getBoolean("attendance_met"),
                        result.getBoolean("attendance_confirmed")),
                result.getString("attendance_reference"),
                result.getLong("revision"));
    }

    @Override
    public Course saveCourse(String ownerNim, Course course) {
        return database.write(
                connection -> {
                    requireSemester(connection, ownerNim, course.semesterId());
                    boolean creating = course.id() == 0;
                    String sql =
                            creating
                                    ? "INSERT INTO"
                                          + " courses(name,code,credits,attendance_met,attendance_confirmed,attendance_reference,owner_nim,semester_id)"
                                          + " VALUES(?,?,?,?,?,?,?,?)"
                                    : "UPDATE courses SET"
                                          + " name=?,code=?,credits=?,attendance_met=?,attendance_confirmed=?,attendance_reference=?,revision=revision+1"
                                          + " WHERE owner_nim=? AND semester_id=? AND id=?";
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, course.name());
                        statement.setString(2, course.code());
                        statement.setString(3, course.credits().toPlainString());
                        statement.setBoolean(4, course.attendance().met());
                        statement.setBoolean(5, course.attendance().confirmed());
                        statement.setString(6, course.attendanceReference());
                        statement.setString(7, ownerNim);
                        statement.setLong(8, course.semesterId());
                        if (!creating) statement.setLong(9, course.id());
                        requireChanged(statement.executeUpdate());
                        long id = creating ? insertedId(connection) : course.id();
                        try (PreparedStatement select =
                                connection.prepareStatement(
                                        "SELECT * FROM courses WHERE owner_nim=? AND semester_id=?"
                                            + " AND id=?")) {
                            select.setString(1, ownerNim);
                            select.setLong(2, course.semesterId());
                            select.setLong(3, id);
                            try (ResultSet result = select.executeQuery()) {
                                if (!result.next())
                                    throw new SQLException("Data simpan tidak ditemukan.");
                                return course(result);
                            }
                        }
                    }
                });
    }

    @Override
    public void deleteCourse(String ownerNim, long semesterId, long courseId) {
        database.write(
                connection -> {
                    try (PreparedStatement statement =
                            connection.prepareStatement(
                                    "DELETE FROM courses WHERE owner_nim=? AND semester_id=? AND"
                                        + " id=?")) {
                        statement.setString(1, ownerNim);
                        statement.setLong(2, semesterId);
                        statement.setLong(3, courseId);
                        requireChanged(statement.executeUpdate());
                    }
                    return null;
                });
    }

    private void requireSemester(Connection connection, String ownerNim, long id)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("SELECT 1 FROM semesters WHERE owner_nim=? AND id=?")) {
            statement.setString(1, ownerNim);
            statement.setLong(2, id);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next())
                    throw new IllegalArgumentException("Semester tidak ditemukan pada akun ini.");
            }
        }
    }

    private void requireChanged(int rows) {
        if (rows != 1)
            throw new IllegalArgumentException(
                    "Data tidak ditemukan pada akun ini. Muat ulang daftar.");
    }

    private long insertedId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
            if (!result.next()) throw new SQLException("ID data baru tidak tersedia.");
            return result.getLong(1);
        }
    }
}
