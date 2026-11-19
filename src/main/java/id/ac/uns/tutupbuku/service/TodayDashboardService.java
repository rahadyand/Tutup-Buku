package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Komposisi read-only; record di bawah hanya hasil satu pemuatan, tidak disimpan lagi. */
public final class TodayDashboardService {
    private final AuthService auth;
    private final AcademicService academic;
    private final LearningService learning;
    private final AgendaService agenda;
    private final GradePlanningService grades;

    public TodayDashboardService(
            AuthService auth,
            AcademicService academic,
            LearningService learning,
            AgendaService agenda,
            GradePlanningService grades) {
        this.auth = auth;
        this.academic = academic;
        this.learning = learning;
        this.agenda = agenda;
        this.grades = grades;
    }

    public record CourseSummary(
            ActualCourse actual,
            LearningProgress progress,
            GradeResult grade,
            ResumeResult reading,
            int openItems) {}

    public record ReadingLink(ResumeResult resume, Optional<Course> course, String locationLabel) {
        public ReadingBookmark bookmark() {
            Material material = resume.reading().orElseThrow().material();
            return new ReadingBookmark(
                    material.id(), material.revision(), resume.scrollRatio(), material.location());
        }
    }

    public record Snapshot(
            UserProfile profile,
            List<Semester> semesters,
            Optional<Semester> viewed,
            LocalDate date,
            Map<AgendaSection, List<AgendaItem>> agenda,
            List<CourseSummary> courses,
            List<ReadingLink> readings,
            List<String> readingWarnings,
            GpaResult gpa) {}

    public LocalDate today() {
        return agenda.today();
    }

    public Snapshot load(Long requestedSemester) {
        UserProfile profile = auth.profile();
        var asOf = agenda.now();
        List<Semester> semesters = academic.semesters();
        Long wanted =
                requestedSemester == null
                        ? profile.activeSemesterId().orElse(null)
                        : requestedSemester;
        Optional<Semester> viewed =
                semesters.stream().filter(s -> Objects.equals(wanted, s.id())).findFirst();
        if (requestedSemester != null && viewed.isEmpty())
            throw new IllegalArgumentException(
                    "Semester tidak tersedia pada akun ini. Muat ulang Hari Ini.");
        if (viewed.isEmpty() && !semesters.isEmpty()) viewed = Optional.of(semesters.getFirst());
        List<AgendaItem> items = viewed.map(s -> agenda.items(s.id(), 0)).orElse(List.of());
        List<ActualCourse> actual = viewed.map(s -> grades.courses(s.id())).orElse(List.of());
        List<CourseSummary> courses = new ArrayList<>();
        List<CourseResult> results = new ArrayList<>();
        for (ActualCourse course : actual) {
            GradeResult grade = grades.actualResult(course);
            courses.add(
                    new CourseSummary(
                            course,
                            learning.courseProgress(course.course().id()),
                            grade,
                            learning.resumeCourse(course.course().id()),
                            (int)
                                    items.stream()
                                            .filter(
                                                    i ->
                                                            i.courseId() == course.course().id()
                                                                    && !i.completed())
                                            .count()));
            results.add(
                    new CourseResult(
                            Long.toString(course.course().id()),
                            course.course().name(),
                            Optional.of(course.course().credits()),
                            Optional.of(grade)));
        }
        List<ReadingLink> readings = new ArrayList<>();
        Set<String> warnings = new LinkedHashSet<>();
        Set<Long> included = new HashSet<>();
        ResumeResult last = learning.resume();
        if (last.reading().isPresent()) {
            var material = last.reading().orElseThrow().material();
            var folder = learning.folder(material.location());
            if (folder.isPresent()
                    && material.location().kind() == LearningLocation.Kind.INTEREST) {
                readings.add(
                        new ReadingLink(
                                last,
                                Optional.empty(),
                                "Peminatan · "
                                        + folder.orElseThrow().name()
                                        + " (lintas semester)"));
                included.add(material.id());
            } else if (folder.isPresent()) {
                actual.stream()
                        .filter(c -> c.course().id() == folder.orElseThrow().courseId())
                        .findFirst()
                        .ifPresent(
                                c -> {
                                    readings.add(
                                            new ReadingLink(
                                                    last,
                                                    Optional.of(c.course()),
                                                    "Kuliah · " + c.course().name()));
                                    included.add(material.id());
                                });
            }
        } else if (last.previousLocation().isPresent())
            warnings.add("Referensi terakhir akun: " + last.message());
        for (CourseSummary course : courses) {
            ResumeResult history = course.reading();
            if (history.reading().isPresent()) {
                long id = history.reading().orElseThrow().material().id();
                if (included.add(id))
                    readings.add(
                            new ReadingLink(
                                    history,
                                    Optional.of(course.actual().course()),
                                    "Kuliah · " + course.actual().course().name()));
            } else if (history.previousLocation().isPresent())
                warnings.add(course.actual().course().name() + ": " + history.message());
        }
        // Target internal nol hanya memenuhi API engine; dashboard tidak menampilkan target rekaan.
        GpaResult gpa = new SemesterGpaService().calculate(results, BigDecimal.ZERO);
        if (!profile.nim().equals(auth.profile().nim()))
            throw new IllegalStateException("Akun berubah saat memuat data. Buka ulang Hari Ini.");
        return new Snapshot(
                profile,
                List.copyOf(semesters),
                viewed,
                asOf.toLocalDate(),
                agenda.group(items.stream().filter(i -> !i.completed()).toList(), asOf),
                List.copyOf(courses),
                List.copyOf(readings),
                List.copyOf(warnings),
                gpa);
    }

    public ResumeResult resolveReading(ReadingLink link) {
        ResumeResult result = learning.resumeReference(link.bookmark());
        if (result.reading().isEmpty())
            throw new IllegalArgumentException(
                    result.message()
                            + " Muat ulang Hari Ini; tidak ada bacaan pengganti otomatis.");
        var folder = learning.folder(result.reading().orElseThrow().material().location());
        if (folder.isEmpty()
                || link.course().filter(c -> c.id() != folder.orElseThrow().courseId()).isPresent())
            throw new IllegalArgumentException("Konteks bacaan berubah. Muat ulang Hari Ini.");
        return result;
    }
}
