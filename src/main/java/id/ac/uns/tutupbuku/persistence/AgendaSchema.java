package id.ac.uns.tutupbuku.persistence;

import java.sql.*;

final class AgendaSchema {
    private AgendaSchema() {}

    static void migrate(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        try (var s = connection.createStatement()) {
            s.execute(
                    """
CREATE TABLE agenda_items (
  id INTEGER PRIMARY KEY AUTOINCREMENT, owner_nim TEXT NOT NULL, course_id INTEGER NOT NULL,
  type TEXT NOT NULL CHECK(type IN ('TASK','EXAM')), title TEXT NOT NULL CHECK(length(trim(title))>0),
  relevant_date TEXT NOT NULL, relevant_time TEXT, description TEXT NOT NULL DEFAULT '',
  priority TEXT NOT NULL DEFAULT 'NORMAL' CHECK(priority IN ('HIGH','NORMAL','LOW')),
  completed INTEGER NOT NULL DEFAULT 0 CHECK(completed IN (0,1)), version INTEGER NOT NULL CHECK(version>0),
  UNIQUE(id,owner_nim),
  FOREIGN KEY(course_id,owner_nim) REFERENCES courses(id,owner_nim) ON DELETE CASCADE)
""");
            s.execute(
                    """
CREATE TABLE agenda_steps (
  id TEXT PRIMARY KEY NOT NULL, owner_nim TEXT NOT NULL, item_id INTEGER NOT NULL,
  text TEXT NOT NULL CHECK(length(trim(text))>0), position INTEGER NOT NULL CHECK(position>=0),
  completed INTEGER NOT NULL CHECK(completed IN (0,1)), UNIQUE(item_id,position),
  FOREIGN KEY(item_id,owner_nim) REFERENCES agenda_items(id,owner_nim) ON DELETE CASCADE)
""");
            s.execute(
                    "CREATE INDEX agenda_course_owner_date ON"
                        + " agenda_items(owner_nim,course_id,relevant_date)");
            validate(connection);
            s.execute("PRAGMA user_version=7");
            connection.commit();
        } catch (SQLException error) {
            connection.rollback();
            throw error;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    static void validate(Connection connection) throws SQLException {
        try (var s = connection.createStatement()) {
            s.executeQuery(
                            "SELECT"
                                + " id,owner_nim,course_id,type,title,relevant_date,relevant_time,description,priority,completed,version"
                                + " FROM agenda_items LIMIT 0")
                    .close();
            s.executeQuery(
                            "SELECT id,owner_nim,item_id,text,position,completed FROM agenda_steps"
                                + " LIMIT 0")
                    .close();
        }
    }
}
