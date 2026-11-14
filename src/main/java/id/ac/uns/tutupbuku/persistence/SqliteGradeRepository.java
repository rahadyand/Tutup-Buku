package id.ac.uns.tutupbuku.persistence;

import id.ac.uns.tutupbuku.domain.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Data aktual dan snapshot memakai tabel berbeda; semua pencarian dibatasi pemilik. */
public final class SqliteGradeRepository implements GradeRepository {
    private final SqliteDatabase database;

    public SqliteGradeRepository(SqliteDatabase database) {
        this.database = database;
    }

    @Override
    public List<ActualCourse> actualCourses(String owner, long semesterId) {
        return database.read(
                c -> {
                    requireSemester(c, owner, semesterId);
                    List<ActualCourse> courses = new ArrayList<>();
                    try (PreparedStatement s =
                                    prepare(
                                            c,
                                            "SELECT c.*,g.mode,g.letter FROM courses c LEFT JOIN"
                                                + " course_actual_grades g ON g.course_id=c.id AND"
                                                + " g.owner_nim=c.owner_nim WHERE c.owner_nim=? AND"
                                                + " c.semester_id=? ORDER BY c.name COLLATE"
                                                + " NOCASE,c.id",
                                            owner,
                                            semesterId);
                            ResultSet r = s.executeQuery()) {
                        while (r.next()) {
                            Course course =
                                    new Course(
                                            r.getLong("id"),
                                            semesterId,
                                            r.getString("name"),
                                            r.getString("code"),
                                            decimal(r, "credits"),
                                            new Attendance(
                                                    r.getBoolean("attendance_met"),
                                                    r.getBoolean("attendance_confirmed")),
                                            r.getString("attendance_reference"),
                                            r.getLong("revision"));
                            List<AssessmentComponent> items = new ArrayList<>();
                            try (PreparedStatement q =
                                            prepare(
                                                    c,
                                                    "SELECT * FROM assessment_components WHERE"
                                                        + " owner_nim=? AND course_id=? ORDER BY"
                                                        + " position,id",
                                                    owner,
                                                    course.id());
                                    ResultSet rows = q.executeQuery()) {
                                while (rows.next()) items.add(component(rows, "id"));
                            }
                            courses.add(
                                    new ActualCourse(
                                            course,
                                            r.getString("mode") == null
                                                    ? EvaluationMode.DETAIL
                                                    : EvaluationMode.valueOf(r.getString("mode")),
                                            letter(r, "letter"),
                                            items));
                        }
                    }
                    return List.copyOf(courses);
                });
    }

    @Override
    public void saveComponent(String owner, long courseId, AssessmentComponent item) {
        database.write(
                c -> {
                    requireCourse(c, owner, courseId);
                    // UUID baru disisipkan, UUID lama hanya dapat diedit pada pemilik/matkul yang
                    // sama.
                    try (PreparedStatement s =
                                    prepare(
                                            c,
                                            "SELECT owner_nim,course_id FROM assessment_components"
                                                    + " WHERE id=?",
                                            item.id());
                            ResultSet r = s.executeQuery()) {
                        if (r.next() && (!owner.equals(r.getString(1)) || courseId != r.getLong(2)))
                            throw unavailable();
                    }
                    try (PreparedStatement s =
                            prepare(
                                    c,
                                    "INSERT INTO"
                                        + " assessment_components(id,owner_nim,course_id,name,weight,earned,maximum,position)"
                                        + " VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET"
                                        + " name=excluded.name,weight=excluded.weight,earned=excluded.earned,maximum=excluded.maximum,position=excluded.position"
                                        + " WHERE"
                                        + " assessment_components.owner_nim=excluded.owner_nim AND"
                                        + " assessment_components.course_id=excluded.course_id",
                                    item.id(),
                                    owner,
                                    courseId,
                                    item.name(),
                                    item.weight(),
                                    item.earned().orElse(null),
                                    item.maximum().orElse(null),
                                    item.position())) {
                        changed(s.executeUpdate());
                    }
                    bump(c, owner, courseId);
                    return null;
                });
    }

    @Override
    public void deleteComponent(String owner, long courseId, String id) {
        database.write(
                c -> {
                    try (PreparedStatement s =
                            prepare(
                                    c,
                                    "DELETE FROM assessment_components WHERE owner_nim=? AND"
                                            + " course_id=? AND id=?",
                                    owner,
                                    courseId,
                                    id)) {
                        changed(s.executeUpdate());
                    }
                    bump(c, owner, courseId);
                    return null;
                });
    }

    @Override
    public void moveComponent(String owner, long courseId, String id, int direction) {
        if (direction != -1 && direction != 1)
            throw new IllegalArgumentException("Arah urutan tidak sah.");
        database.write(
                c -> {
                    requireCourse(c, owner, courseId);
                    List<String> ids = new ArrayList<>();
                    try (PreparedStatement s =
                                    prepare(
                                            c,
                                            "SELECT id FROM assessment_components WHERE owner_nim=?"
                                                    + " AND course_id=? ORDER BY position,id",
                                            owner,
                                            courseId);
                            ResultSet r = s.executeQuery()) {
                        while (r.next()) ids.add(r.getString(1));
                    }
                    int index = ids.indexOf(id);
                    if (index < 0) throw unavailable();
                    int other = index + direction;
                    if (other < 0 || other >= ids.size()) return null;
                    Collections.swap(ids, index, other);
                    for (int i = 0; i < ids.size(); i++)
                        try (PreparedStatement s =
                                prepare(
                                        c,
                                        "UPDATE assessment_components SET position=? WHERE"
                                                + " owner_nim=? AND course_id=? AND id=?",
                                        i,
                                        owner,
                                        courseId,
                                        ids.get(i))) {
                            changed(s.executeUpdate());
                        }
                    bump(c, owner, courseId);
                    return null;
                });
    }

    @Override
    public void saveActualSettings(
            String owner,
            long courseId,
            EvaluationMode mode,
            Optional<LetterGrade> letter,
            Attendance attendance) {
        saveActualSettings(owner, courseId, mode, letter, attendance, null);
    }

    @Override
    public void saveActualSettings(
            String owner,
            long courseId,
            EvaluationMode mode,
            Optional<LetterGrade> letter,
            Attendance attendance,
            String reference) {
        database.write(
                c -> {
                    requireCourse(c, owner, courseId);
                    try (PreparedStatement s =
                            prepare(
                                    c,
                                    "INSERT INTO"
                                        + " course_actual_grades(owner_nim,course_id,mode,letter)"
                                        + " VALUES(?,?,?,?) ON CONFLICT(course_id,owner_nim) DO"
                                        + " UPDATE SET mode=excluded.mode,letter=excluded.letter",
                                    owner,
                                    courseId,
                                    mode,
                                    letter.orElse(null))) {
                        s.executeUpdate();
                    }
                    try (PreparedStatement s =
                            prepare(
                                    c,
                                    "UPDATE courses SET"
                                        + " attendance_met=?,attendance_confirmed=?,attendance_reference=COALESCE(?,attendance_reference),revision=revision+1"
                                        + " WHERE owner_nim=? AND id=?",
                                    attendance.met(),
                                    attendance.confirmed(),
                                    reference,
                                    owner,
                                    courseId)) {
                        changed(s.executeUpdate());
                    }
                    return null;
                });
    }

    @Override
    public List<SemesterScenario> scenarios(String owner, long semesterId) {
        return database.read(
                c -> {
                    requireSemester(c, owner, semesterId);
                    List<SemesterScenario> scenarios = new ArrayList<>();
                    try (PreparedStatement s =
                                    prepare(
                                            c,
                                            "SELECT id FROM grade_scenarios WHERE owner_nim=? AND"
                                                + " semester_id=? ORDER BY updated_at DESC,id DESC",
                                            owner,
                                            semesterId);
                            ResultSet r = s.executeQuery()) {
                        while (r.next()) scenarios.add(load(c, owner, r.getLong(1)));
                    }
                    return List.copyOf(scenarios);
                });
    }

    @Override
    public SemesterScenario loadScenario(String owner, long id) {
        return database.read(c -> load(c, owner, id));
    }

    private SemesterScenario load(Connection c, String owner, long id) throws SQLException {
        try (PreparedStatement s =
                        prepare(
                                c,
                                "SELECT * FROM grade_scenarios WHERE owner_nim=? AND id=?",
                                owner,
                                id);
                ResultSet r = s.executeQuery()) {
            if (!r.next()) throw unavailable();
            List<ScenarioCourse> courses = new ArrayList<>();
            try (PreparedStatement q =
                            prepare(
                                    c,
                                    "SELECT * FROM scenario_courses WHERE owner_nim=? AND"
                                            + " scenario_id=? ORDER BY course_name COLLATE"
                                            + " NOCASE,course_id",
                                    owner,
                                    id);
                    ResultSet row = q.executeQuery()) {
                while (row.next()) {
                    long courseId = row.getLong("course_id");
                    Course course =
                            new Course(
                                    courseId,
                                    r.getLong("semester_id"),
                                    row.getString("course_name"),
                                    row.getString("code"),
                                    decimal(row, "credits"),
                                    new Attendance(
                                            row.getBoolean("base_attendance_met"),
                                            row.getBoolean("base_attendance_confirmed")),
                                    row.getString("attendance_reference"),
                                    row.getLong("course_revision"));
                    List<AssessmentComponent> items = new ArrayList<>();
                    Map<String, BigDecimal> locks = new LinkedHashMap<>();
                    try (PreparedStatement a =
                                    prepare(
                                            c,
                                            "SELECT * FROM scenario_items WHERE owner_nim=? AND"
                                                    + " scenario_id=? AND course_id=? ORDER BY"
                                                    + " position,item_id",
                                            owner,
                                            id,
                                            courseId);
                            ResultSet item = a.executeQuery()) {
                        while (item.next()) {
                            items.add(component(item, "item_id"));
                            if (item.getString("locked_score") != null)
                                locks.put(item.getString("item_id"), decimal(item, "locked_score"));
                        }
                    }
                    ActualCourse basis =
                            new ActualCourse(
                                    course,
                                    EvaluationMode.valueOf(row.getString("actual_mode")),
                                    letter(row, "actual_letter"),
                                    items);
                    courses.add(
                            new ScenarioCourse(
                                    basis,
                                    EvaluationMode.valueOf(row.getString("active_mode")),
                                    letter(row, "estimated_letter"),
                                    decimal(row, "target_score"),
                                    letter(row, "target_letter"),
                                    new Attendance(
                                            row.getBoolean("attendance_met"),
                                            row.getBoolean("attendance_confirmed")),
                                    locks));
                }
            }
            return new SemesterScenario(
                    id,
                    r.getLong("semester_id"),
                    r.getString("name"),
                    decimal(r, "target_gpa"),
                    r.getLong("version"),
                    r.getInt("rule_version"),
                    Instant.parse(r.getString("updated_at")),
                    courses);
        }
    }

    @Override
    public SemesterScenario saveScenario(
            String owner,
            SemesterScenario scenario,
            Map<Long, Map<String, BigDecimal>> suggestions) {
        return database.write(
                c -> {
                    requireSemester(c, owner, scenario.semesterId());
                    SemesterScenario previous =
                            scenario.id() == 0 ? null : load(c, owner, scenario.id());
                    if (previous != null && previous.semesterId() != scenario.semesterId())
                        throw unavailable();
                    // Snapshot course yang sudah dihapus hanya boleh berasal dari versi tersimpan
                    // akun ini.
                    for (ScenarioCourse input : scenario.courses()) {
                        Course course = input.basis().course();
                        if (course.semesterId() != scenario.semesterId()) throw unavailable();
                        boolean historical =
                                previous != null
                                        && previous.courses().stream()
                                                .anyMatch(
                                                        old ->
                                                                old.basis().course().id()
                                                                        == course.id());
                        if (!historical)
                            try (PreparedStatement s =
                                            prepare(
                                                    c,
                                                    "SELECT 1 FROM courses WHERE owner_nim=? AND"
                                                        + " semester_id=? AND id=? UNION ALL SELECT"
                                                        + " 1 FROM scenario_courses c JOIN"
                                                        + " grade_scenarios s ON s.id=c.scenario_id"
                                                        + " AND s.owner_nim=c.owner_nim WHERE"
                                                        + " c.owner_nim=? AND s.semester_id=? AND"
                                                        + " c.course_id=?",
                                                    owner,
                                                    scenario.semesterId(),
                                                    course.id(),
                                                    owner,
                                                    scenario.semesterId(),
                                                    course.id());
                                    ResultSet r = s.executeQuery()) {
                                if (!r.next()) throw unavailable();
                            }
                    }
                    Instant now = Instant.now();
                    long id = scenario.id();
                    if (id == 0) {
                        try (PreparedStatement s =
                                prepare(
                                        c,
                                        "INSERT INTO"
                                            + " grade_scenarios(owner_nim,semester_id,name,target_gpa,rule_version,updated_at)"
                                            + " VALUES(?,?,?,?,?,?)",
                                        owner,
                                        scenario.semesterId(),
                                        scenario.name(),
                                        scenario.targetGpa(),
                                        scenario.ruleVersion(),
                                        now)) {
                            s.executeUpdate();
                        }
                        try (Statement s = c.createStatement();
                                ResultSet r = s.executeQuery("SELECT last_insert_rowid()")) {
                            r.next();
                            id = r.getLong(1);
                        }
                    } else {
                        try (PreparedStatement s =
                                prepare(
                                        c,
                                        "UPDATE grade_scenarios SET"
                                            + " name=?,target_gpa=?,rule_version=?,updated_at=?,version=version+1"
                                            + " WHERE owner_nim=? AND id=? AND version=?",
                                        scenario.name(),
                                        scenario.targetGpa(),
                                        scenario.ruleVersion(),
                                        now,
                                        owner,
                                        id,
                                        scenario.version())) {
                            if (s.executeUpdate() != 1)
                                throw new IllegalArgumentException(
                                        "Skenario telah berubah. Buka ulang sebelum menyimpan;"
                                                + " isian Anda belum dihapus.");
                        }
                        try (PreparedStatement s =
                                prepare(
                                        c,
                                        "DELETE FROM scenario_courses WHERE owner_nim=? AND"
                                                + " scenario_id=?",
                                        owner,
                                        id)) {
                            s.executeUpdate();
                        }
                    }
                    Set<Long> seen = new HashSet<>();
                    for (ScenarioCourse input : scenario.courses()) {
                        ActualCourse basis = input.basis();
                        Course course = basis.course();
                        if (!seen.add(course.id()))
                            throw new IllegalArgumentException("Mata kuliah ganda dalam skenario.");
                        try (PreparedStatement s =
                                prepare(
                                        c,
                                        "INSERT INTO"
                                            + " scenario_courses(scenario_id,owner_nim,course_id,course_name,code,credits,course_revision,base_attendance_met,base_attendance_confirmed,attendance_reference,actual_mode,actual_letter,active_mode,estimated_letter,target_score,target_letter,attendance_met,attendance_confirmed)"
                                            + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                                        id,
                                        owner,
                                        course.id(),
                                        course.name(),
                                        course.code(),
                                        course.credits(),
                                        course.revision(),
                                        course.attendance().met(),
                                        course.attendance().confirmed(),
                                        course.attendanceReference(),
                                        basis.mode(),
                                        basis.letter().orElse(null),
                                        input.mode(),
                                        input.estimatedLetter().orElse(null),
                                        input.targetScore(),
                                        input.targetLetter().orElse(null),
                                        input.attendance().met(),
                                        input.attendance().confirmed())) {
                            s.executeUpdate();
                        }
                        for (AssessmentComponent item : basis.components())
                            try (PreparedStatement s =
                                    prepare(
                                            c,
                                            "INSERT INTO"
                                                + " scenario_items(scenario_id,owner_nim,course_id,item_id,name,weight,earned,maximum,position,locked_score,suggested_score)"
                                                + " VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                                            id,
                                            owner,
                                            course.id(),
                                            item.id(),
                                            item.name(),
                                            item.weight(),
                                            item.earned().orElse(null),
                                            item.maximum().orElse(null),
                                            item.position(),
                                            input.lockedScores().get(item.id()),
                                            suggestions
                                                    .getOrDefault(course.id(), Map.of())
                                                    .get(item.id()))) {
                                s.executeUpdate();
                            }
                    }
                    return load(c, owner, id);
                });
    }

    @Override
    public void deleteScenario(String owner, long id) {
        database.write(
                c -> {
                    try (PreparedStatement s =
                            prepare(
                                    c,
                                    "DELETE FROM grade_scenarios WHERE owner_nim=? AND id=?",
                                    owner,
                                    id)) {
                        changed(s.executeUpdate());
                    }
                    return null;
                });
    }

    private static AssessmentComponent component(ResultSet r, String id) throws SQLException {
        return new AssessmentComponent(
                r.getString(id),
                r.getString("name"),
                decimal(r, "weight"),
                optionalDecimal(r, "earned"),
                optionalDecimal(r, "maximum"),
                r.getInt("position"));
    }

    private static BigDecimal decimal(ResultSet r, String field) throws SQLException {
        return new BigDecimal(r.getString(field));
    }

    private static Optional<BigDecimal> optionalDecimal(ResultSet r, String field)
            throws SQLException {
        return Optional.ofNullable(r.getString(field)).map(BigDecimal::new);
    }

    private static Optional<LetterGrade> letter(ResultSet r, String field) throws SQLException {
        return Optional.ofNullable(r.getString(field)).map(LetterGrade::valueOf);
    }

    private static void changed(int rows) {
        if (rows != 1) throw unavailable();
    }

    private static IllegalArgumentException unavailable() {
        return new IllegalArgumentException(
                "Data nilai tidak ditemukan pada akun ini. Muat ulang daftar.");
    }

    private static void requireCourse(Connection c, String owner, long id) throws SQLException {
        try (PreparedStatement s =
                        prepare(c, "SELECT 1 FROM courses WHERE owner_nim=? AND id=?", owner, id);
                ResultSet r = s.executeQuery()) {
            if (!r.next()) throw unavailable();
        }
    }

    private static void requireSemester(Connection c, String owner, long id) throws SQLException {
        try (PreparedStatement s =
                        prepare(
                                c,
                                "SELECT 1 FROM semesters WHERE owner_nim=? AND id=?",
                                owner,
                                id);
                ResultSet r = s.executeQuery()) {
            if (!r.next()) throw unavailable();
        }
    }

    private static void bump(Connection c, String owner, long id) throws SQLException {
        try (PreparedStatement s =
                prepare(
                        c,
                        "UPDATE courses SET revision=revision+1 WHERE owner_nim=? AND id=?",
                        owner,
                        id)) {
            changed(s.executeUpdate());
        }
    }

    /** Binder sederhana untuk SQL berparameter; angka desimal tetap teks presisi penuh. */
    private static PreparedStatement prepare(Connection c, String sql, Object... values)
            throws SQLException {
        PreparedStatement s = c.prepareStatement(sql);
        for (int i = 0; i < values.length; i++) {
            Object v = values[i];
            if (v instanceof BigDecimal d) s.setString(i + 1, d.toPlainString());
            else if (v instanceof Enum<?> e) s.setString(i + 1, e.name());
            else if (v instanceof Instant t) s.setString(i + 1, t.toString());
            else s.setObject(i + 1, v);
        }
        return s;
    }
}
