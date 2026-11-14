package id.ac.uns.tutupbuku.persistence;

import id.ac.uns.tutupbuku.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface GradeRepository {
    List<ActualCourse> actualCourses(String owner, long semesterId);

    void saveComponent(String owner, long courseId, AssessmentComponent component);

    void deleteComponent(String owner, long courseId, String componentId);

    void moveComponent(String owner, long courseId, String componentId, int direction);

    void saveActualSettings(
            String owner,
            long courseId,
            EvaluationMode mode,
            java.util.Optional<LetterGrade> letter,
            Attendance attendance);

    void saveActualSettings(
            String owner,
            long courseId,
            EvaluationMode mode,
            java.util.Optional<LetterGrade> letter,
            Attendance attendance,
            String reference);

    List<SemesterScenario> scenarios(String owner, long semesterId);

    SemesterScenario loadScenario(String owner, long id);

    SemesterScenario saveScenario(
            String owner,
            SemesterScenario scenario,
            Map<Long, Map<String, BigDecimal>> suggestions);

    void deleteScenario(String owner, long id);
}
