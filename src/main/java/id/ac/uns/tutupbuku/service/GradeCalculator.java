package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GradeCalculator {
    public GradeResult weighted(List<AssessmentItem> items, ScenarioAssumptions assumptions, Attendance attendance) {
        AssessmentRules.validate(items, assumptions);
        ScoreFraction total = ScoreFraction.zero();
        boolean estimated = !attendance.confirmed();
        List<String> issues = new ArrayList<>();
        for (AssessmentItem item : items) {
            if (item.weight().signum() == 0) continue;
            if (!AssessmentRules.hasKnownScore(item, assumptions)) {
                issues.add(item.name() + ": Belum ada nilai.");
            } else {
                total = total.add(ScoreFraction.contribution(item, assumptions));
                estimated |= item.actualScore().isEmpty();
            }
        }
        // Hasil parsial sengaja tidak diekspos sebagai nilai akhir.
        Optional<BigDecimal> numeric = issues.isEmpty() ? Optional.of(total.decimalValue()) : Optional.empty();
        Optional<LetterGrade> letter = issues.isEmpty() ? Optional.of(total.letterGrade()) : Optional.empty();
        return result(numeric, letter, estimated, attendance, issues);
    }

    public GradeResult direct(Optional<LetterGrade> letter, boolean actual, Attendance attendance) {
        return result(Optional.empty(), letter, !actual || !attendance.confirmed(), attendance,
                letter.isPresent() ? List.of() : List.of("Belum ada nilai huruf."));
    }

    private GradeResult result(Optional<BigDecimal> numeric, Optional<LetterGrade> letter,
                               boolean estimated, Attendance attendance, List<String> issues) {
        // E karena kehadiran dapat menjadi hasil efektif meski skor akademik belum lengkap (GPA-01).
        Optional<LetterGrade> effective = attendance.met() ? letter : Optional.of(LetterGrade.E);
        return new GradeResult(numeric, letter, effective, estimated, attendance, issues);
    }
}
