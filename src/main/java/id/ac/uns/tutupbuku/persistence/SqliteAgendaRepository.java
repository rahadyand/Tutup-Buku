package id.ac.uns.tutupbuku.persistence;

import id.ac.uns.tutupbuku.domain.*;

import java.sql.*;
import java.time.*;
import java.util.*;

public final class SqliteAgendaRepository implements AgendaRepository {
    private final SqliteDatabase database;

    public SqliteAgendaRepository(SqliteDatabase database) {
        this.database = database;
    }

    private static PreparedStatement prepare(Connection c, String sql, Object... values)
            throws SQLException {
        var s = c.prepareStatement(sql);
        try {
            for (int i = 0; i < values.length; i++) s.setObject(i + 1, values[i]);
            return s;
        } catch (SQLException error) {
            s.close();
            throw error;
        }
    }

    private static void requireCourse(Connection c, String owner, long course) throws SQLException {
        try (var s = prepare(c, "SELECT 1 FROM courses WHERE id=? AND owner_nim=?", course, owner);
                var r = s.executeQuery()) {
            if (!r.next()) throw new IllegalArgumentException("Pilih mata kuliah milik akun ini.");
        }
    }

    @Override
    public List<AgendaItem> list(String owner, long semester, long course) {
        return database.read(
                c -> {
                    try (var s =
                                    prepare(
                                            c,
                                            "SELECT 1 FROM semesters WHERE id=? AND owner_nim=?",
                                            semester,
                                            owner);
                            var r = s.executeQuery()) {
                        if (!r.next())
                            throw new IllegalArgumentException("Pilih semester milik akun ini.");
                    }
                    if (course != 0)
                        try (var s =
                                        prepare(
                                                c,
                                                "SELECT 1 FROM courses WHERE id=? AND owner_nim=?"
                                                    + " AND semester_id=?",
                                                course,
                                                owner,
                                                semester);
                                var r = s.executeQuery()) {
                            if (!r.next())
                                throw new IllegalArgumentException(
                                        "Mata kuliah tidak ada pada semester yang dilihat.");
                        }
                    List<AgendaItem> result = new ArrayList<>();
                    try (var s =
                                    prepare(
                                            c,
                                            "SELECT a.* FROM agenda_items a JOIN courses c ON"
                                                + " c.id=a.course_id AND c.owner_nim=a.owner_nim"
                                                + " WHERE a.owner_nim=? AND c.semester_id=? AND"
                                                + " (?=0 OR a.course_id=?) ORDER BY a.id",
                                            owner,
                                            semester,
                                            course,
                                            course);
                            var rows = s.executeQuery()) {
                        while (rows.next()) result.add(item(c, owner, rows));
                    }
                    return List.copyOf(result);
                });
    }

    @Override
    public AgendaItem find(String owner, long id) {
        return database.read(c -> find(c, owner, id));
    }

    private static AgendaItem find(Connection c, String owner, long id) throws SQLException {
        try (var s =
                        prepare(
                                c,
                                "SELECT * FROM agenda_items WHERE id=? AND owner_nim=?",
                                id,
                                owner);
                var rows = s.executeQuery()) {
            if (!rows.next())
                throw new IllegalArgumentException(
                        "Item tidak ditemukan pada akun ini atau sudah dihapus.");
            return item(c, owner, rows);
        }
    }

    private static AgendaItem item(Connection c, String owner, ResultSet row) throws SQLException {
        List<ChecklistStep> steps = new ArrayList<>();
        try (var s =
                        prepare(
                                c,
                                "SELECT id,text,completed FROM agenda_steps WHERE item_id=? AND"
                                    + " owner_nim=? ORDER BY position,id",
                                row.getLong("id"),
                                owner);
                var rows = s.executeQuery()) {
            while (rows.next())
                steps.add(
                        new ChecklistStep(
                                rows.getString("id"),
                                rows.getString("text"),
                                rows.getBoolean("completed")));
        }
        String time = row.getString("relevant_time");
        return new AgendaItem(
                row.getLong("id"),
                row.getLong("course_id"),
                AgendaType.valueOf(row.getString("type")),
                row.getString("title"),
                LocalDate.parse(row.getString("relevant_date")),
                time == null ? Optional.empty() : Optional.of(LocalTime.parse(time)),
                row.getString("description"),
                AgendaPriority.valueOf(row.getString("priority")),
                row.getBoolean("completed"),
                row.getLong("version"),
                steps);
    }

    @Override
    public AgendaItem save(String owner, AgendaItem input) {
        return database.write(
                c -> {
                    requireCourse(c, owner, input.courseId());
                    if (input.id() != 0) find(c, owner, input.id());
                    for (var step : input.checklist())
                        try (var s =
                                        prepare(
                                                c,
                                                "SELECT owner_nim,item_id FROM agenda_steps WHERE"
                                                    + " id=?",
                                                step.id());
                                var rows = s.executeQuery()) {
                            if (rows.next()
                                    && (!owner.equals(rows.getString("owner_nim"))
                                            || input.id() != rows.getLong("item_id")))
                                throw new IllegalArgumentException(
                                        "Langkah checklist bukan milik item ini.");
                        }
                    long id = input.id();
                    if (id == 0) {
                        try (var s =
                                prepare(
                                        c,
                                        "INSERT INTO"
                                            + " agenda_items(owner_nim,course_id,type,title,relevant_date,relevant_time,description,priority,completed,version)"
                                            + " VALUES(?,?,?,?,?,?,?,?,?,1)",
                                        owner,
                                        input.courseId(),
                                        input.type().name(),
                                        input.title(),
                                        input.date().toString(),
                                        input.time().map(LocalTime::toString).orElse(null),
                                        input.description(),
                                        input.priority().name(),
                                        input.completed())) {
                            s.executeUpdate();
                        }
                        try (var s = c.createStatement();
                                var rows = s.executeQuery("SELECT last_insert_rowid()")) {
                            rows.next();
                            id = rows.getLong(1);
                        }
                    } else {
                        try (var s =
                                prepare(
                                        c,
                                        "UPDATE agenda_items SET"
                                            + " course_id=?,type=?,title=?,relevant_date=?,relevant_time=?,description=?,priority=?,completed=?,version=version+1"
                                            + " WHERE id=? AND owner_nim=? AND version=?",
                                        input.courseId(),
                                        input.type().name(),
                                        input.title(),
                                        input.date().toString(),
                                        input.time().map(LocalTime::toString).orElse(null),
                                        input.description(),
                                        input.priority().name(),
                                        input.completed(),
                                        id,
                                        owner,
                                        input.version())) {
                            if (s.executeUpdate() != 1)
                                throw new IllegalArgumentException(
                                        "Item sudah berubah. Isian tetap ada; buka ulang versi"
                                            + " terbaru sebelum menyimpan.");
                        }
                        try (var s =
                                prepare(
                                        c,
                                        "DELETE FROM agenda_steps WHERE item_id=? AND owner_nim=?",
                                        id,
                                        owner)) {
                            s.executeUpdate();
                        }
                    }
                    for (int position = 0; position < input.checklist().size(); position++) {
                        var step = input.checklist().get(position);
                        try (var s =
                                prepare(
                                        c,
                                        "INSERT INTO"
                                            + " agenda_steps(id,owner_nim,item_id,text,position,completed)"
                                            + " VALUES(?,?,?,?,?,?)",
                                        step.id(),
                                        owner,
                                        id,
                                        step.text(),
                                        position,
                                        step.completed())) {
                            s.executeUpdate();
                        }
                    }
                    return find(c, owner, id);
                });
    }

    @Override
    public void delete(String owner, long id, long version) {
        database.write(
                c -> {
                    try (var s =
                            prepare(
                                    c,
                                    "DELETE FROM agenda_items WHERE id=? AND owner_nim=? AND"
                                        + " version=?",
                                    id,
                                    owner,
                                    version)) {
                        if (s.executeUpdate() != 1)
                            throw new IllegalArgumentException(
                                    "Item tidak ditemukan atau sudah berubah; muat ulang sebelum"
                                        + " menghapus.");
                    }
                    return null;
                });
    }
}
