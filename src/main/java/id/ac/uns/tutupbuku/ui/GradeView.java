package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.service.*;

import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

final class GradeView extends BorderPane {
    private final AcademicService academic;
    private final GradePlanningService planning;
    private final UiTasks tasks;
    private final ComboBox<Semester> semesters = new ComboBox<>();
    private final Label status = status("grade-status");
    private final ActualGradePane actual;
    private final ScenarioPane scenarios;
    private final WhatIfPane whatIf;
    private final TabPane tabs;
    private final Tab actualTab, scenarioTab;
    private boolean simulating;
    private boolean filling;
    private Long preferredSemester;
    private long scopedCourse;
    private long loadedSemester;
    private Runnable widerCourse = () -> {};

    enum Entry {
        KEEP,
        ACTUAL,
        WHAT_IF,
        PLANNING
    }

    void onWiderCourse(Runnable action) {
        widerCourse = action;
    }

    void courseScope(boolean scoped) {
        setMinHeight(scoped ? 300 : Region.USE_COMPUTED_SIZE);
        if (!scoped) scopedCourse = 0;
        getTop().setVisible(!scoped);
        getTop().setManaged(!scoped);
        actual.courseScope(scoped);
    }

    void openCourse(Semester semester, Course course, Entry entry) {
        boolean newContext = scopedCourse != course.id() || loadedSemester != semester.id();
        scopedCourse = course.id();
        scenarios.preferCourse(course.id());
        Runnable select =
                () -> {
                    if (entry == Entry.PLANNING) tabs.getSelectionModel().select(scenarioTab);
                    else if (entry != Entry.KEEP) {
                        tabs.getSelectionModel().select(actualTab);
                        if (entry == Entry.WHAT_IF) beginWhatIf(actual.selectedCourse());
                    }
                };
        if (loadedSemester == semester.id()
                && actual.courseId() == course.id()
                && actual.selectedCourse().course().revision() == course.revision()) {
            if (newContext) {
                scenarios.load(semester.id(), List.of());
                scenarios.reloadList();
                tabs.getSelectionModel().select(actualTab);
            }
            select.run();
            return;
        }
        actual.load(0, List.of());
        scenarios.load(semester.id(), List.of());
        tasks.run(
                status,
                () -> {
                    var values = planning.courses(semester.id());
                    if (values.stream().noneMatch(c -> c.course().id() == course.id()))
                        throw new IllegalArgumentException(
                                "Mata kuliah sudah dihapus. Muat ulang ringkasan.");
                    return new Catalog(values, planning.scenarios(semester.id()));
                },
                value -> {
                    filling = true;
                    semesters.getItems().setAll(semester);
                    semesters.setValue(semester);
                    filling = false;
                    actual.load(semester.id(), value.courses());
                    actual.selectCourse(course.id());
                    scenarios.preferCourse(course.id());
                    scenarios.load(semester.id(), value.scenarios());
                    loadedSemester = semester.id();
                    tabs.getSelectionModel().select(actualTab);
                    select.run();
                });
    }

    void openGlobal() {
        courseScope(false);
        Long viewed = semesters.getValue() == null ? null : semesters.getValue().id();
        if (viewed == null) {
            refresh();
            return;
        }
        tasks.run(
                status,
                academic::semesters,
                values -> {
                    filling = true;
                    semesters.getItems().setAll(values);
                    semesters.setValue(
                            values.stream().filter(s -> s.id() == viewed).findFirst().orElse(null));
                    filling = false;
                    if (semesters.getValue() == null) load();
                    else scenarios.reloadList();
                });
    }

    void preferSemester(java.util.Optional<Long> id) {
        preferredSemester = id.orElse(null);
    }

    GradeView(AcademicService academic, GradePlanningService planning, UiTasks tasks) {
        this.academic = academic;
        this.planning = planning;
        this.tasks = tasks;
        actual = new ActualGradePane(planning, tasks, this::beginWhatIf);
        scenarios = new ScenarioPane(planning, tasks, this::beginSavedWhatIf);
        whatIf =
                new WhatIfPane(
                        planning, tasks, this::returnActual, this::openSaved, this::reviewSaved);
        semesters.setId("grade-semester");
        Button refresh = button("grade-refresh", "Muat ulang data");
        refresh.setOnAction(e -> guard(this::refresh));
        VBox header =
                new VBox(
                        4,
                        new FlowPane(10, 6, new Label("Semester perencanaan"), semesters, refresh),
                        note(
                                "Nilai aktual dan asumsi skenario tersimpan terpisah. IP hanya"
                                        + " dihitung setelah semua mata kuliah memiliki hasil"
                                        + " efektif."));
        header.setPadding(new javafx.geometry.Insets(4, 8, 4, 8));
        setTop(header);
        actualTab = new Tab("Komponen & aktual", actual);
        scenarioTab = new Tab("Skenario, target & IP", scenarios);
        tabs = new TabPane(actualTab, scenarioTab);
        tabs.setId("grade-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        setCenter(tabs);
        setBottom(status);
        tabs.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (o, old, selected) -> {
                            if (filling || !simulating) return;
                            filling = true;
                            tabs.getSelectionModel().select(old);
                            filling = false;
                            if (whatIf.hasScenario() && selected == scenarioTab) {
                                openSaved();
                                return;
                            }
                            whatIf.guard(
                                    () -> {
                                        endWhatIf();
                                        filling = true;
                                        tabs.getSelectionModel().select(selected);
                                        filling = false;
                                    });
                        });
        // Berpindah aktual/skenario mempertahankan form masing-masing; keluar semester/dashboard
        // memakai guard.
        semesters
                .valueProperty()
                .addListener(
                        (o, old, value) -> {
                            if (filling) return;
                            filling = true;
                            semesters.setValue(old);
                            filling = false;
                            guard(
                                    () -> {
                                        filling = true;
                                        semesters.setValue(value);
                                        filling = false;
                                        load();
                                    });
                        });
    }

    void refresh() {
        Long id =
                semesters.getValue() == null
                        ? preferredSemester
                        : Long.valueOf(semesters.getValue().id());
        tasks.run(
                status,
                academic::semesters,
                values -> {
                    filling = true;
                    semesters.getItems().setAll(values);
                    semesters.setValue(
                            values.stream()
                                    .filter(s -> id != null && s.id() == id)
                                    .findFirst()
                                    .orElse(values.isEmpty() ? null : values.getLast()));
                    filling = false;
                    load();
                });
    }

    private record Catalog(List<ActualCourse> courses, List<SemesterScenario> scenarios) {}

    private void beginWhatIf(ActualCourse selected) {
        if (selected == null || semesters.getValue() == null) return;
        long semester = semesters.getValue().id(), course = selected.course().id();
        tasks.run(
                status,
                () -> planning.courses(semester),
                values -> {
                    ActualCourse basis =
                            values.stream()
                                    .filter(c -> c.course().id() == course)
                                    .findFirst()
                                    .orElse(null);
                    if (basis == null) {
                        status.setText("Mata kuliah telah dihapus. Muat ulang data.");
                        return;
                    }
                    actual.load(semester, values);
                    whatIf.open(basis);
                    simulating = true;
                    actualTab.setContent(whatIf);
                    setBottom(
                            null); // Status kalkulasi berada di WhatIfPane; beri ruang untuk baris
                    // nilai.
                });
    }

    private void returnActual() {
        whatIf.guard(this::endWhatIf);
    }

    private void beginSavedWhatIf(ScenarioPane.CourseDraft input) {
        var course =
                input.scenario().courses().stream()
                        .filter(c -> c.basis().course().id() == input.courseId())
                        .findFirst()
                        .orElseThrow();
        whatIf.openScenario(input.scenario(), course, input.modified());
        simulating = true;
        actualTab.setContent(whatIf);
        setBottom(null);
        filling = true;
        tabs.getSelectionModel().select(actualTab);
        filling = false;
        if (scopedCourse != 0 && scopedCourse != input.courseId()) widerCourse.run();
    }

    private void openSaved() {
        openSaved(false);
    }

    private void reviewSaved() {
        openSaved(true);
    }

    private void openSaved(boolean review) {
        Runnable transfer =
                () -> {
                    try {
                        SemesterScenario draft = whatIf.scenarioDraft();
                        long course = whatIf.courseId();
                        boolean modified = whatIf.dirty();
                        scenarios.guard(
                                () -> {
                                    if (draft != null)
                                        scenarios.showScenario(draft, course, modified);
                                    endWhatIf();
                                    tabs.getSelectionModel().select(scenarioTab);
                                    if (review && draft != null) scenarios.requestReview();
                                });
                    } catch (IllegalArgumentException error) {
                        whatIf.error(error.getMessage());
                    }
                };
        if (whatIf.hasScenario())
            transfer.run(); // Hand off the same draft, without discarding edits.
        else whatIf.guard(transfer);
    }

    private void endWhatIf() {
        if (!simulating) return;
        simulating = false;
        whatIf.clear();
        actualTab.setContent(actual);
        setBottom(status);
    }

    private void load() {
        Semester semester = semesters.getValue();
        if (semester == null) {
            actual.load(0, List.of());
            scenarios.load(0, List.of());
            status.setText("Buat semester di Semester & mata kuliah.");
            return;
        }
        long id = semester.id();
        tasks.run(
                status,
                () -> new Catalog(planning.courses(id), planning.scenarios(id)),
                value -> {
                    loadedSemester = id;
                    actual.load(id, value.courses());
                    scenarios.load(id, value.scenarios());
                    status.setText("Data semester dimuat.");
                });
    }

    void guard(Runnable action) {
        whatIf.guard(
                () ->
                        actual.guard(
                                () ->
                                        scenarios.guard(
                                                () -> {
                                                    endWhatIf();
                                                    action.run();
                                                })));
    }
}
