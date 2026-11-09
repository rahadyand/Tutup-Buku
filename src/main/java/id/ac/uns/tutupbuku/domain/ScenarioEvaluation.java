package id.ac.uns.tutupbuku.domain;

import java.util.List;
import java.util.Optional;

public record ScenarioEvaluation(
        SemesterScenario scenario,
        List<CourseEvaluation> courses,
        GpaResult gpa,
        ScenarioReview review) {
    public ScenarioEvaluation {
        courses = List.copyOf(courses);
    }

    public record CourseEvaluation(
            ScenarioCourse input,
            GradeResult grade,
            Optional<TargetPlan> targetPlan,
            String explanation) {}
}
