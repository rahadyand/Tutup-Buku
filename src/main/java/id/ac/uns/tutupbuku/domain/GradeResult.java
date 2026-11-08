package id.ac.uns.tutupbuku.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public record GradeResult(Optional<BigDecimal> underlyingScore,
                          Optional<LetterGrade> underlyingLetter,
                          Optional<LetterGrade> effectiveLetter,
                          boolean estimated, Attendance attendance, List<String> issues) {
    public GradeResult {
        issues = List.copyOf(issues);
    }
}
