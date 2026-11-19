package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.service.*;

import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.Consumer;

/** Konteks navigasi saja; materi, draf, penilaian dan rumus tetap milik view/service lama. */
final class CourseHubView extends BorderPane {
    private final AcademicService academic;
    private final LearningService learning;
    private final GradePlanningService planning;
    private final UiTasks tasks;
    private final Consumer<Runnable> guard;
    private final LearningView learningView;
    private final GradeView gradeView;
    private final AgendaView agendaView;
    private final ComboBox<Semester> semesters = new ComboBox<>();
    private final ComboBox<Course> courses = new ComboBox<>();
    private final Label context = note("Pilih semester dan mata kuliah."),
            active = note(""),
            learningStatus = note(""),
            readingStatus = note(""),
            gradeStatus = note(""),
            scenarios = note(""),
            status = status("hub-status");
    private final Tab overview = new Tab("Ringkasan"),
            materials = new Tab("Materi"),
            grades = new Tab("Nilai"),
            agenda = new Tab("Tugas & Ujian");
    private final TabPane sections = new TabPane(overview, materials, grades, agenda);
    private final Button resume = button("hub-resume", "Lanjutkan membaca");
    private final List<Button> courseActions = new ArrayList<>();
    private Optional<Long> activeSemester = Optional.empty();
    private boolean filling;
    private Semester viewed;
    private Course selected;
    private Runnable releaseGlobal = () -> {};
    private final ScrollPane materialPage = scroll(new Pane()), gradePage = scroll(new Pane());

    CourseHubView(
            AcademicService academic,
            LearningService learning,
            GradePlanningService planning,
            UiTasks tasks,
            Consumer<Runnable> guard,
            LearningView learningView,
            GradeView gradeView,
            AgendaView agendaView) {
        this.academic = academic;
        this.learning = learning;
        this.planning = planning;
        this.tasks = tasks;
        this.guard = guard;
        this.learningView = learningView;
        this.gradeView = gradeView;
        this.agendaView = agendaView;
        materialPage.setId("hub-material-page");
        gradePage.setId("hub-grade-page");
        materialPage.setFitToHeight(true);
        gradePage.setFitToHeight(true);
        setId("course-hub");
        semesters.setId("hub-semester");
        courses.setId("hub-course");
        context.setId("hub-context");
        active.setId("hub-active-context");
        learningStatus.setId("hub-learning-status");
        readingStatus.setId("hub-reading-status");
        gradeStatus.setId("hub-grade-status");
        scenarios.setId("hub-scenario-status");
        sections.setId("hub-sections");
        sections.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        limitWidth(semesters, 200);
        limitWidth(courses, 260);
        Button refresh = button("hub-refresh", "Muat ulang ringkasan");
        refresh.setOnAction(e -> guard.accept(this::refresh));
        VBox header =
                new VBox(
                        4,
                        new FlowPane(
                                10,
                                6,
                                field("Semester dilihat", semesters),
                                field("Mata kuliah", courses),
                                refresh),
                        active,
                        context);
        header.setPadding(new javafx.geometry.Insets(6, 8, 6, 8));
        setTop(header);
        setCenter(sections);
        setBottom(status);
        Button openMaterials = button("hub-open-materials", "Buka materi");
        openMaterials.getStyleClass().add("tb-primary");
        Button actual = button("hub-open-actual", "Buka nilai aktual"),
                whatIf = button("hub-open-what-if", "Simulasi Target (What If)"),
                plan = button("hub-open-planning", "Rencana semester & IP");
        courseActions.addAll(List.of(openMaterials, actual, whatIf, plan));
        openMaterials.setOnAction(e -> navigate(materials, false, GradeView.Entry.KEEP));
        resume.setOnAction(e -> navigate(materials, true, GradeView.Entry.KEEP));
        actual.setOnAction(e -> navigate(grades, false, GradeView.Entry.ACTUAL));
        whatIf.setOnAction(e -> navigate(grades, false, GradeView.Entry.WHAT_IF));
        plan.setOnAction(e -> navigate(grades, false, GradeView.Entry.PLANNING));
        VBox summary =
                form(
                        heading("Ringkasan mata kuliah"),
                        field(
                                "Belajar",
                                new VBox(
                                        4,
                                        learningStatus,
                                        readingStatus,
                                        new FlowPane(8, 6, openMaterials, resume))),
                        field(
                                "Nilai aktual",
                                new VBox(4, gradeStatus, new FlowPane(8, 6, actual, whatIf))),
                        field("Rencana semester", new VBox(4, scenarios, plan)),
                        note(
                                "Rencana dan IP mencakup seluruh semester yang dilihat. Membuka"
                                    + " rencana tidak memilih skenario otomatis. Peminatan tetap"
                                    + " tersedia melalui Modul Belajar."));
        summary.setSpacing(10);
        overview.setContent(scroll(summary));
        sections.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (o, old, value) -> {
                            if (filling) return;
                            filling = true;
                            sections.getSelectionModel().select(old);
                            filling = false;
                            guard.accept(() -> showSection(value, false, GradeView.Entry.KEEP));
                        });
        semesters
                .valueProperty()
                .addListener(
                        (o, old, value) -> {
                            if (filling || value == null) return;
                            filling = true;
                            semesters.setValue(old);
                            filling = false;
                            guard.accept(() -> loadCourse(value, null));
                        });
        courses.valueProperty()
                .addListener(
                        (o, old, value) -> {
                            if (filling || value == null) return;
                            filling = true;
                            courses.setValue(old);
                            filling = false;
                            guard.accept(() -> loadCourse(viewed, value.id()));
                        });
        empty();
    }

    void activeSemester(Optional<Long> id) {
        activeSemester = id;
        updateContext();
    }

    Semester viewedSemester() {
        return viewed;
    }

    Course selectedCourse() {
        return selected;
    }

    void openOverview(Semester semester, Course course) {
        viewed = semester;
        selected = course;
        filling = true;
        sections.getSelectionModel().select(0);
        filling = false;
        refresh();
    }

    void openAgenda(Semester semester, Course course) {
        viewed = semester;
        selected = course;
        filling = true;
        sections.getSelectionModel().select(agenda);
        filling = false;
        refresh();
    }

    void releaseGlobal(Runnable action) {
        releaseGlobal = action;
    }

    void detach() {
        materialPage.setContent(null);
        gradePage.setContent(null);
        materials.setContent(null);
        grades.setContent(null);
        agenda.setContent(null);
    }

    private void navigate(Tab target, boolean continueReading, GradeView.Entry entry) {
        guard.accept(() -> showSection(target, continueReading, entry));
    }

    private void showSection(Tab target, boolean continueReading, GradeView.Entry entry) {
        if (selected == null && target != overview) return;
        releaseGlobal.run();
        detach();
        filling = true;
        sections.getSelectionModel().select(target);
        filling = false;
        if (target == materials) {
            learningView.courseScope(true);
            materialPage.setContent(learningView);
            materials.setContent(materialPage);
            learningView.openCourse(viewed, selected, continueReading);
        } else if (target == grades) {
            gradeView.courseScope(true);
            gradePage.setContent(gradeView);
            grades.setContent(gradePage);
            gradeView.openCourse(viewed, selected, entry);
        } else if (target == agenda) {
            agenda.setContent(agendaView);
            agendaView.openCourse(viewed, selected);
        } else if (selected != null) loadCourse(viewed, selected.id());
    }

    void refresh() {
        Long wanted = viewed == null ? activeSemester.orElse(null) : Long.valueOf(viewed.id());
        Long course = selected == null ? null : selected.id();
        tasks.run(
                status,
                academic::semesters,
                values -> {
                    Semester semester =
                            values.stream()
                                    .filter(s -> Objects.equals(wanted, s.id()))
                                    .findFirst()
                                    .orElse(values.isEmpty() ? null : values.getFirst());
                    filling = true;
                    semesters.getItems().setAll(values);
                    semesters.setValue(viewed);
                    filling = false;
                    if (semester == null) {
                        viewed = null;
                        selected = null;
                        empty();
                        return;
                    }
                    loadCourse(semester, course);
                });
    }

    private record Summary(
            List<Course> courses,
            ActualCourse actual,
            LearningProgress progress,
            ResumeResult resume,
            int scenarioCount,
            int staleCount,
            GradeResult grade) {}

    private void loadCourse(Semester semester, Long wantedCourse) {
        tasks.run(
                status,
                () -> {
                    List<ActualCourse> values = planning.courses(semester.id());
                    ActualCourse actual =
                            values.stream()
                                    .filter(c -> Objects.equals(wantedCourse, c.course().id()))
                                    .findFirst()
                                    .orElse(values.isEmpty() ? null : values.getFirst());
                    if (actual == null) return new Summary(List.of(), null, null, null, 0, 0, null);
                    long id = actual.course().id();
                    int count = 0, stale = 0;
                    for (var scenario : planning.scenarios(semester.id()))
                        if (scenario.courses().stream()
                                .anyMatch(c -> c.basis().course().id() == id)) {
                            count++;
                            if (planning.review(scenario).outdated()) stale++;
                        }
                    return new Summary(
                            values.stream().map(ActualCourse::course).toList(),
                            actual,
                            learning.courseProgress(id),
                            learning.resumeCourse(id),
                            count,
                            stale,
                            planning.actualResult(actual));
                },
                summary -> {
                    viewed = semester;
                    selected = summary.actual() == null ? null : summary.actual().course();
                    filling = true;
                    semesters.setValue(semester);
                    courses.getItems().setAll(summary.courses());
                    courses.setValue(selected);
                    filling = false;
                    if (selected == null) {
                        empty();
                        return;
                    }
                    courseActions.forEach(b -> b.setDisable(false));
                    materials.setDisable(false);
                    grades.setDisable(false);
                    agenda.setDisable(false);
                    updateContext();
                    learningStatus.setText(
                            summary.progress().display()
                                    + (summary.progress().total() > 0
                                                    && summary.progress().completed() == 0
                                            ? " — belum ada materi ditandai selesai."
                                            : ""));
                    resume.setDisable(summary.resume().reading().isEmpty());
                    readingStatus.setText(
                            summary.resume()
                                    .reading()
                                    .map(
                                            r ->
                                                    "Bacaan mata kuliah ini: "
                                                            + r.material().title()
                                                            + ". "
                                                            + summary.resume().message())
                                    .orElse(summary.resume().message()));
                    String kind =
                            summary.actual().mode() == EvaluationMode.DIRECT
                                    ? "Huruf langsung"
                                    : summary.actual().components().isEmpty()
                                            ? "Belum ada struktur penilaian"
                                            : "Detail komponen aktual";
                    gradeStatus.setText(kind + "\n" + GradeForms.result(summary.grade()));
                    scenarios.setText(
                            summary.scenarioCount() == 0
                                    ? "Belum ada skenario untuk mata kuliah ini. Buat dari What If"
                                            + " atau layar rencana semester."
                                    : summary.scenarioCount()
                                            + " skenario semester memuat mata kuliah ini; "
                                            + summary.staleCount()
                                            + " usang/perlu ditinjau. Proyeksi bukan nilai"
                                            + " aktual.");
                    status.setText("Ringkasan dimuat. Semester aktif tidak diubah.");
                    Tab section = sections.getSelectionModel().getSelectedItem();
                    if (section != overview) showSection(section, false, GradeView.Entry.KEEP);
                });
    }

    private void updateContext() {
        active.setText(
                "Semester aktif: "
                        + semesters.getItems().stream()
                                .filter(s -> activeSemester.filter(id -> id == s.id()).isPresent())
                                .map(Semester::toString)
                                .findFirst()
                                .orElse(
                                        activeSemester.isEmpty()
                                                ? "belum dipilih"
                                                : "lihat header akun")
                        + (viewed == null
                                ? ""
                                : " · Dilihat: "
                                        + viewed
                                        + (activeSemester
                                                        .filter(id -> id == viewed.id())
                                                        .isPresent()
                                                ? " (aktif)"
                                                : " (arsip; tidak diaktifkan)")));
        context.setText(
                selected == null
                        ? "Pilih mata kuliah. Buat semester/mata kuliah melalui Semester & Mata"
                                + " Kuliah bila daftar kosong."
                        : selected.name()
                                + (selected.code().isBlank() ? "" : " · " + selected.code())
                                + " · "
                                + GradeForms.number(selected.credits())
                                + " SKS");
    }

    private void empty() {
        filling = true;
        courses.getItems().clear();
        courses.setValue(null);
        semesters.setValue(viewed);
        filling = false;
        detach();
        filling = true;
        sections.getSelectionModel().select(overview);
        filling = false;
        materials.setDisable(true);
        grades.setDisable(true);
        agenda.setDisable(true);
        courseActions.forEach(b -> b.setDisable(true));
        resume.setDisable(true);
        learningStatus.setText("Belum ada mata kuliah. Tambahkan melalui Semester & Mata Kuliah.");
        readingStatus.setText("Belum ada bacaan mata kuliah.");
        gradeStatus.setText("Belum lengkap — pilih mata kuliah.");
        scenarios.setText("Belum ada rencana yang dibuka.");
        updateContext();
    }
}
