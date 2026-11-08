package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SemesterGpaService {
    public GpaResult calculate(List<CourseResult> courses, BigDecimal target) {
        if (target == null || target.signum() < 0 || target.compareTo(new BigDecimal("4")) > 0) {
            throw new IllegalArgumentException("Target IP harus antara 0 dan 4,00.");
        }
        List<String> issues = new ArrayList<>();
        if (courses.isEmpty()) issues.add("Belum ada mata kuliah.");
        Set<String> ids = new HashSet<>();
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal weightedPoints = BigDecimal.ZERO;
        boolean estimated = false;
        for (CourseResult course : courses) {
            if (!ids.add(course.id())) issues.add(course.name() + ": mata kuliah duplikat.");
            boolean validCredits = course.credits().filter(c -> c.signum() > 0).isPresent();
            if (!validCredits) issues.add(course.name() + ": SKS wajib diisi dan lebih dari 0.");
            Optional<LetterGrade> letter = course.grade().flatMap(GradeResult::effectiveLetter);
            if (letter.isEmpty()) issues.add(course.name() + ": hasil efektif belum lengkap.");
            if (validCredits && letter.isPresent()) {
                BigDecimal credits = course.credits().orElseThrow();
                totalCredits = totalCredits.add(credits);
                weightedPoints = weightedPoints.add(letter.orElseThrow().gradePoint().multiply(credits));
                estimated |= course.grade().orElseThrow().estimated();
            }
        }
        if (!issues.isEmpty()) {
            return new GpaResult(Optional.empty(), Optional.empty(), Optional.empty(), estimated, issues);
        }
        BigDecimal value = weightedPoints.divide(totalCredits, MathContext.DECIMAL128);
        // Bandingkan numerator, agar pembulatan pembagian/tampilan tidak menentukan ketercapaian.
        BigDecimal gapNumerator = weightedPoints.subtract(target.multiply(totalCredits));
        return new GpaResult(Optional.of(value), Optional.of(gapNumerator.divide(totalCredits, MathContext.DECIMAL128)),
                Optional.of(gapNumerator.signum() >= 0), estimated, List.of());
    }
}
