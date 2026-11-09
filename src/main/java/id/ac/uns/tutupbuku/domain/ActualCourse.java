package id.ac.uns.tutupbuku.domain;

import java.util.List;
import java.util.Optional;

/**
 * Juga dipakai sebagai snapshot immutable pada skenario; tidak menunjuk data hidup yang berubah.
 */
public record ActualCourse(
        Course course,
        EvaluationMode mode,
        Optional<LetterGrade> letter,
        List<AssessmentComponent> components) {
    public ActualCourse {
        components = List.copyOf(components);
    }

    public List<AssessmentItem> items() {
        return components.stream().map(AssessmentComponent::item).toList();
    }

    @Override
    public String toString() {
        return course.toString();
    }
}
