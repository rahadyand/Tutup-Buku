package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.GradeRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Koordinator M4: rumus tetap di engine M1; repository hanya menyimpan data/snapshot. */
public final class GradePlanningService {
    public static final int RULE_VERSION = 1;
    private final GradeRepository repository;
    private final UserSession session;
    private final GradeCalculator calculator = new GradeCalculator();
    private final TargetPlanner planner = new TargetPlanner();
    private final SemesterGpaService gpa = new SemesterGpaService();

    public GradePlanningService(GradeRepository repository, UserSession session) {
        this.repository = repository;
        this.session = session;
    }

    public List<ActualCourse> courses(long semester) {
        return repository.actualCourses(session.requireNim(), semester);
    }

    public void saveComponent(
            long course,
            String id,
            String name,
            String weight,
            String earned,
            String maximum,
            int position) {
        AssessmentComponent item =
                new AssessmentComponent(
                        id == null ? UUID.randomUUID().toString() : id,
                        name,
                        DecimalInput.parse(weight),
                        optionalNumber(earned),
                        optionalNumber(maximum),
                        position);
        repository.saveComponent(session.requireNim(), course, item);
    }

    public void deleteComponent(long course, String id) {
        repository.deleteComponent(session.requireNim(), course, id);
    }

    public void moveComponent(long course, String id, int direction) {
        repository.moveComponent(session.requireNim(), course, id, direction);
    }

    public void saveActual(
            long course, EvaluationMode mode, Optional<LetterGrade> letter, Attendance attendance) {
        if (mode == null) throw new IllegalArgumentException("Pilih mode penilaian.");
        repository.saveActualSettings(session.requireNim(), course, mode, letter, attendance);
    }

    public void saveActual(
            long course,
            EvaluationMode mode,
            Optional<LetterGrade> letter,
            Attendance attendance,
            String reference) {
        if (mode == null) throw new IllegalArgumentException("Pilih mode penilaian.");
        if (reference == null || reference.isBlank())
            throw new IllegalArgumentException("Acuan kehadiran wajib diisi.");
        repository.saveActualSettings(
                session.requireNim(), course, mode, letter, attendance, reference.trim());
    }

    public GradeResult actualResult(ActualCourse course) {
        if (course.mode() == EvaluationMode.DIRECT)
            return calculator.direct(course.letter(), true, course.course().attendance());
        try {
            return calculator.weighted(
                    course.items(), ScenarioAssumptions.none(), course.course().attendance());
        } catch (IllegalArgumentException error) {
            return invalid(course.course().attendance(), error.getMessage());
        }
    }

    public List<SemesterScenario> scenarios(long semester) {
        return repository.scenarios(session.requireNim(), semester);
    }

    public SemesterScenario open(long id) {
        return repository.loadScenario(session.requireNim(), id);
    }

    public SemesterScenario create(long semester, String name, BigDecimal targetGpa) {
        return new SemesterScenario(
                0,
                semester,
                name,
                targetGpa,
                0,
                RULE_VERSION,
                Instant.now(),
                courses(semester).stream().map(ScenarioCourse::fromActual).toList());
    }

    public SemesterScenario rename(
            SemesterScenario source, String name, BigDecimal target, List<ScenarioCourse> courses) {
        session.requireNim();
        return new SemesterScenario(
                source.id(),
                source.semesterId(),
                name,
                target,
                source.version(),
                source.ruleVersion(),
                source.updatedAt(),
                courses);
    }

    public SemesterScenario duplicate(long id, String name) {
        SemesterScenario source = open(id);
        // Simpan salinan snapshot apa adanya; basis historis tidak diam-diam diperbarui.
        return repository.saveScenario(
                session.requireNim(),
                new SemesterScenario(
                        0,
                        source.semesterId(),
                        name,
                        source.targetGpa(),
                        0,
                        source.ruleVersion(),
                        Instant.now(),
                        source.courses()),
                suggestions(source));
    }

    public SemesterScenario save(SemesterScenario scenario) {
        return repository.saveScenario(session.requireNim(), scenario, suggestions(scenario));
    }

    public void delete(long id) {
        repository.deleteScenario(session.requireNim(), id);
    }

    public ScenarioReview review(SemesterScenario scenario) {
        List<String> changes = new ArrayList<>();
        if (scenario.ruleVersion() != RULE_VERSION)
            changes.add("Versi aturan perhitungan berbeda; perbarui basis.");
        Map<Long, ActualCourse> current = index(courses(scenario.semesterId()));
        for (ScenarioCourse old : scenario.courses()) {
            ActualCourse now = current.remove(old.basis().course().id());
            String name = old.basis().course().name();
            if (now == null) {
                changes.add(name + ": mata kuliah telah dihapus.");
                continue;
            }
            if (now.course().revision() != old.basis().course().revision()) {
                List<String> details = new ArrayList<>();
                Course before = old.basis().course(), after = now.course();
                if (before.credits().compareTo(after.credits()) != 0)
                    details.add("SKS " + before.credits() + " → " + after.credits());
                if (!before.name().equals(after.name()) || !before.code().equals(after.code()))
                    details.add("identitas mata kuliah");
                if (!before.attendance().equals(after.attendance())
                        || !before.attendanceReference().equals(after.attendanceReference()))
                    details.add("data kehadiran");
                if (old.basis().mode() != now.mode() || !old.basis().letter().equals(now.letter()))
                    details.add("mode/huruf aktual");
                if (!old.basis().components().equals(now.components()))
                    details.add("komponen, bobot, urutan, atau nilai aktual");
                changes.add(
                        name
                                + ": revisi "
                                + before.revision()
                                + " → "
                                + after.revision()
                                + " ("
                                + (details.isEmpty()
                                        ? "data akademik disimpan ulang"
                                        : String.join("; ", details))
                                + ").");
            }
        }
        current.values().forEach(c -> changes.add(c.course().name() + ": mata kuliah baru."));
        if (!changes.isEmpty())
            changes.add(
                    "Perbarui basis: pertahankan target/mode; buang kunci yang sudah"
                            + " aktual/terhapus. Mata kuliah baru ditambahkan, yang dihapus"
                            + " dikeluarkan. Tinjau lalu Simpan skenario.");
        return new ScenarioReview(changes);
    }

    /** Refresh menghasilkan draft; tidak menulis skenario lama sampai pengguna menekan Simpan. */
    public SemesterScenario refresh(SemesterScenario source) {
        Map<Long, ScenarioCourse> old = new HashMap<>();
        source.courses().forEach(c -> old.put(c.basis().course().id(), c));
        List<ScenarioCourse> updated = new ArrayList<>();
        for (ActualCourse basis : courses(source.semesterId())) {
            ScenarioCourse before = old.get(basis.course().id());
            if (before == null) {
                updated.add(ScenarioCourse.fromActual(basis));
                continue;
            }
            Map<String, BigDecimal> locks = new LinkedHashMap<>();
            for (AssessmentComponent item : basis.components())
                if (item.earned().isEmpty()
                        && item.weight().signum() > 0
                        && before.lockedScores().containsKey(item.id()))
                    locks.put(item.id(), before.lockedScores().get(item.id()));
            Attendance attendance =
                    before.attendance().equals(before.basis().course().attendance())
                            ? basis.course().attendance()
                            : new Attendance(before.attendance().met(), false);
            updated.add(
                    new ScenarioCourse(
                            basis,
                            before.mode(),
                            basis.letter().isPresent()
                                    ? Optional.empty()
                                    : before.estimatedLetter(),
                            before.targetScore(),
                            before.targetLetter(),
                            attendance,
                            locks));
        }
        return new SemesterScenario(
                source.id(),
                source.semesterId(),
                source.name(),
                source.targetGpa(),
                source.version(),
                RULE_VERSION,
                source.updatedAt(),
                updated);
    }

    public ScenarioEvaluation evaluate(SemesterScenario scenario) {
        session.requireNim();
        List<ScenarioEvaluation.CourseEvaluation> evaluated = new ArrayList<>();
        List<CourseResult> results = new ArrayList<>();
        for (ScenarioCourse input : scenario.courses()) {
            var result = evaluateCourse(input);
            evaluated.add(result);
            Course course = input.basis().course();
            results.add(
                    new CourseResult(
                            Long.toString(course.id()),
                            course.name(),
                            Optional.of(course.credits()),
                            Optional.of(result.grade())));
        }
        return new ScenarioEvaluation(
                scenario,
                evaluated,
                gpa.calculate(results, scenario.targetGpa()),
                review(scenario));
    }

    public ScenarioEvaluation.CourseEvaluation evaluateCourse(ScenarioCourse input) {
        session.requireNim();
        GradeResult grade;
        Optional<TargetPlan> plan = Optional.empty();
        String explanation;
        if (input.mode() == EvaluationMode.DIRECT) {
            boolean actual = input.basis().letter().isPresent();
            grade =
                    calculator.direct(
                            actual ? input.basis().letter() : input.estimatedLetter(),
                            actual,
                            input.attendance());
            explanation =
                    actual
                            ? "Memakai huruf aktual; estimasi tersimpan tidak ikut dihitung."
                            : "Estimasi huruf skenario; tanpa skor angka rekaan.";
        } else
            try {
                TargetPlan target =
                        planner.plan(
                                input.basis().items(),
                                new ScenarioAssumptions(input.lockedScores()),
                                input.targetScore(),
                                input.attendance());
                plan = Optional.of(target);
                Map<String, BigDecimal> assumptions = new LinkedHashMap<>(input.lockedScores());
                assumptions.putAll(target.suggestedScores());
                grade =
                        calculator.weighted(
                                input.basis().items(),
                                new ScenarioAssumptions(assumptions),
                                input.attendance());
                explanation =
                        switch (target.status()) {
                            case ACHIEVABLE ->
                                    "Target dapat dicapai. Saran minimum sama untuk semua komponen"
                                            + " bebas (0–100).";
                            case ALREADY_REACHED ->
                                    "Target sudah tercapai; saran minimum komponen bebas 0.";
                            case IMPOSSIBLE ->
                                    "Target tidak mungkin: kebutuhan "
                                            + target.rawRequiredScore()
                                                    .orElseThrow()
                                                    .toPlainString()
                                            + " > 100. Nilai maksimum akhir "
                                            + target.maximumFinalScore().toPlainString()
                                            + ".";
                            case NO_FREE_REACHED -> "Tidak ada bobot bebas (W=0); target tercapai.";
                            case NO_FREE_NOT_REACHED ->
                                    "Tidak ada bobot bebas (W=0); target tidak tercapai.";
                        };
            } catch (IllegalArgumentException error) {
                grade = invalid(input.attendance(), error.getMessage());
                explanation = "Draf belum sah: " + error.getMessage();
            }
        if (!input.attendance().met())
            explanation +=
                    " Kehadiran tidak terpenuhi: hasil efektif E/0; hasil asal tetap tersimpan.";
        return new ScenarioEvaluation.CourseEvaluation(input, grade, plan, explanation);
    }

    public String comparisonWarning(ScenarioEvaluation first, ScenarioEvaluation second) {
        if (first.scenario().semesterId() != second.scenario().semesterId())
            return "Tidak kompatibel: semester berbeda.";
        if (first.review().outdated() || second.review().outdated())
            return "Basis usang/perlu ditinjau. Angka di bawah adalah snapshot historis, bukan"
                    + " perbandingan basis terkini.";
        Map<Long, Long> a = new HashMap<>(), b = new HashMap<>();
        first.scenario()
                .courses()
                .forEach(c -> a.put(c.basis().course().id(), c.basis().course().revision()));
        second.scenario()
                .courses()
                .forEach(c -> b.put(c.basis().course().id(), c.basis().course().revision()));
        return a.equals(b)
                ? "Basis kompatibel dan terkini."
                : "Tidak kompatibel: struktur/revisi berbeda.";
    }

    private Map<Long, Map<String, BigDecimal>> suggestions(SemesterScenario scenario) {
        Map<Long, Map<String, BigDecimal>> result = new HashMap<>();
        for (ScenarioCourse course : scenario.courses())
            evaluateCourse(course)
                    .targetPlan()
                    .ifPresent(
                            plan ->
                                    result.put(
                                            course.basis().course().id(), plan.suggestedScores()));
        return result;
    }

    private static GradeResult invalid(Attendance attendance, String message) {
        return new GradeResult(
                Optional.empty(),
                Optional.empty(),
                attendance.met() ? Optional.empty() : Optional.of(LetterGrade.E),
                !attendance.confirmed(),
                attendance,
                List.of(message));
    }

    private static Map<Long, ActualCourse> index(List<ActualCourse> courses) {
        Map<Long, ActualCourse> map = new LinkedHashMap<>();
        courses.forEach(c -> map.put(c.course().id(), c));
        return map;
    }

    private static Optional<BigDecimal> optionalNumber(String input) {
        return input == null || input.isBlank()
                ? Optional.empty()
                : Optional.of(DecimalInput.parse(input));
    }
}
