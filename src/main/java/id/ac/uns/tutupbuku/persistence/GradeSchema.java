package id.ac.uns.tutupbuku.persistence;

import java.sql.*;

/** Skema 3 menambah data nilai/snapshot; data akademik dan belajar M2/M3 tidak dihapus. */
final class GradeSchema {
    private GradeSchema() {}

    static void migrate(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    """
CREATE TABLE assessment_components (
  id TEXT PRIMARY KEY NOT NULL, owner_nim TEXT NOT NULL, course_id INTEGER NOT NULL,
  name TEXT NOT NULL CHECK(length(trim(name))>0), weight TEXT NOT NULL CHECK(CAST(weight AS REAL)>=0 AND CAST(weight AS REAL)<=100),
  earned TEXT, maximum TEXT, position INTEGER NOT NULL CHECK(position>=0),
  CHECK(earned IS NULL OR (maximum IS NOT NULL AND CAST(maximum AS REAL)>0 AND CAST(earned AS REAL)>=0 AND CAST(earned AS REAL)<=CAST(maximum AS REAL))),
  FOREIGN KEY(course_id,owner_nim) REFERENCES courses(id,owner_nim) ON DELETE CASCADE)
""");
            statement.execute(
                    "CREATE INDEX assessment_course_owner ON"
                        + " assessment_components(course_id,owner_nim)");
            statement.execute(
                    """
CREATE TABLE course_actual_grades (
  owner_nim TEXT NOT NULL, course_id INTEGER NOT NULL, mode TEXT NOT NULL CHECK(mode IN ('DETAIL','DIRECT')),
  letter TEXT, PRIMARY KEY(course_id,owner_nim),
  FOREIGN KEY(course_id,owner_nim) REFERENCES courses(id,owner_nim) ON DELETE CASCADE)
""");
            statement.execute(
                    """
CREATE TABLE grade_scenarios (
  id INTEGER PRIMARY KEY AUTOINCREMENT, owner_nim TEXT NOT NULL, semester_id INTEGER NOT NULL,
  name TEXT NOT NULL CHECK(length(trim(name))>0), target_gpa TEXT NOT NULL, version INTEGER NOT NULL DEFAULT 1,
  rule_version INTEGER NOT NULL DEFAULT 1, updated_at TEXT NOT NULL, UNIQUE(id,owner_nim),
  FOREIGN KEY(semester_id,owner_nim) REFERENCES semesters(id,owner_nim) ON DELETE CASCADE)
""");
            // course_id snapshot sengaja tidak menunjuk live courses: snapshot tetap terbaca
            // setelah course dihapus.
            statement.execute(
                    """
CREATE TABLE scenario_courses (
  scenario_id INTEGER NOT NULL, owner_nim TEXT NOT NULL, course_id INTEGER NOT NULL,
  course_name TEXT NOT NULL, code TEXT NOT NULL, credits TEXT NOT NULL, course_revision INTEGER NOT NULL,
  base_attendance_met INTEGER NOT NULL, base_attendance_confirmed INTEGER NOT NULL, attendance_reference TEXT NOT NULL,
  actual_mode TEXT NOT NULL, actual_letter TEXT, active_mode TEXT NOT NULL, estimated_letter TEXT,
  target_score TEXT NOT NULL, target_letter TEXT, attendance_met INTEGER NOT NULL, attendance_confirmed INTEGER NOT NULL,
  PRIMARY KEY(scenario_id,course_id), UNIQUE(scenario_id,course_id,owner_nim),
  FOREIGN KEY(scenario_id,owner_nim) REFERENCES grade_scenarios(id,owner_nim) ON DELETE CASCADE)
""");
            statement.execute(
                    """
CREATE TABLE scenario_items (
  scenario_id INTEGER NOT NULL, owner_nim TEXT NOT NULL, course_id INTEGER NOT NULL, item_id TEXT NOT NULL,
  name TEXT NOT NULL, weight TEXT NOT NULL, earned TEXT, maximum TEXT, position INTEGER NOT NULL,
  locked_score TEXT, suggested_score TEXT, PRIMARY KEY(scenario_id,course_id,item_id),
  FOREIGN KEY(scenario_id,course_id,owner_nim) REFERENCES scenario_courses(scenario_id,course_id,owner_nim) ON DELETE CASCADE)
""");
            statement.execute("PRAGMA user_version=3");
            connection.commit();
        } catch (SQLException error) {
            connection.rollback();
            throw error;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    static void validate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String query :
                    new String[] {
                        "SELECT id,owner_nim,course_id,name,weight,earned,maximum,position FROM"
                            + " assessment_components LIMIT 0",
                        "SELECT owner_nim,course_id,mode,letter FROM course_actual_grades LIMIT 0",
                        "SELECT"
                            + " id,owner_nim,semester_id,name,target_gpa,version,rule_version,updated_at"
                            + " FROM grade_scenarios LIMIT 0",
                        "SELECT"
                            + " scenario_id,owner_nim,course_id,course_revision,active_mode,estimated_letter,target_score,target_letter,attendance_met,attendance_confirmed"
                            + " FROM scenario_courses LIMIT 0",
                        "SELECT"
                            + " scenario_id,owner_nim,course_id,item_id,weight,earned,maximum,locked_score,suggested_score"
                            + " FROM scenario_items LIMIT 0"
                    }) statement.executeQuery(query).close();
        }
    }
}
