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
    private boolean filling;
    private Long preferredSemester;

    void preferSemester(java.util.Optional<Long> id) {
        preferredSemester = id.orElse(null);
    }

    GradeView(AcademicService academic, GradePlanningService planning, UiTasks tasks) {
        this.academic = academic;
        this.planning = planning;
        this.tasks = tasks;
        actual = new ActualGradePane(planning, tasks);
        scenarios = new ScenarioPane(planning, tasks);
        semesters.setId("grade-semester");
        Button refresh = button("grade-refresh", "Muat ulang data");
        refresh.setOnAction(e -> guard(this::refresh));
        VBox header =
                new VBox(
                        6,
                        new FlowPane(10, 6, new Label("Semester perencanaan"), semesters, refresh),
                        note(
                                "Nilai aktual dan asumsi skenario tersimpan terpisah. IP hanya"
                                        + " dihitung setelah semua mata kuliah memiliki hasil"
                                        + " efektif."));
        header.setPadding(new javafx.geometry.Insets(8));
        setTop(header);
        Tab actualTab = new Tab("Komponen & aktual", actual),
                scenarioTab = new Tab("Skenario, target & IP", scenarios);
        TabPane tabs = new TabPane(actualTab, scenarioTab);
        tabs.setId("grade-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        setCenter(tabs);
        setBottom(status);
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
                    actual.load(id, value.courses());
                    scenarios.load(id, value.scenarios());
                    status.setText("Data semester dimuat.");
                });
    }

    void guard(Runnable action) {
        actual.guard(() -> scenarios.guard(action));
    }
}
