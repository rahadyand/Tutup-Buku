package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

/** Shell dua pilar dari M1, dipertahankan setelah login M2. */
public final class DashboardView extends VBox {
    private java.util.function.IntConsumer openArea;
    private Runnable refreshCurrent = () -> {};

    void refreshCurrentArea() {
        refreshCurrent.run();
    }

    void openArea(int index) {
        openArea.accept(index);
    }

    public DashboardView() {
        this(null, Runnable::run, () -> {});
    }

    DashboardView(Node learningContent, Consumer<Runnable> guard, Runnable refreshLearning) {
        this(learningContent, new CalculationDemoView(), guard, refreshLearning, () -> {});
    }

    DashboardView(
            Node learningContent,
            Node gradeContent,
            Consumer<Runnable> guard,
            Runnable refreshLearning,
            Runnable refreshGrades) {
        this(learningContent, gradeContent, guard, refreshLearning, refreshGrades, null);
    }

    DashboardView(
            Node learningContent,
            Node gradeContent,
            Consumer<Runnable> guard,
            Runnable refreshLearning,
            Runnable refreshGrades,
            CourseHubView hub) {
        this(learningContent, gradeContent, guard, refreshLearning, refreshGrades, hub, null);
    }

    DashboardView(
            Node learningContent,
            Node gradeContent,
            Consumer<Runnable> guard,
            Runnable refreshLearning,
            Runnable refreshGrades,
            CourseHubView hub,
            AgendaView agenda) {
        super(8);
        setPadding(new Insets(6));
        Tab learningTab =
                new Tab(
                        "Modul belajar",
                        learningContent == null
                                ? form(
                                        heading("Ruang belajar"),
                                        note(
                                                "Modul kuliah\n"
                                                    + "Semester → Mata kuliah → Bab → Materi\n\n"
                                                    + "Peminatan\n"
                                                    + "Bidang → Materi"),
                                        note(
                                                "Fixture shell M1. Masuk melalui aplikasi untuk"
                                                        + " membuka fitur belajar M3."))
                                : learningContent);
        Tab simulationTab = new Tab("Simulasi nilai", gradeContent);
        Node actualLearningContent = learningTab.getContent();
        TabPane tabs = new TabPane(learningTab, simulationTab);
        Tab agendaTab = new Tab("Agenda");
        if (hub != null) {
            tabs.getTabs().add(new Tab("Mata Kuliah", hub));
            hub.releaseGlobal(
                    () -> {
                        learningTab.setContent(null);
                        simulationTab.setContent(null);
                        agendaTab.setContent(null);
                    });
        }
        if (agenda != null) tabs.getTabs().add(agendaTab);
        tabs.setId("main-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        boolean[] selecting = {false};
        int[] mounted = {0};
        refreshCurrent =
                () -> {
                    if (hub != null && tabs.getSelectionModel().getSelectedIndex() == 2)
                        hub.refresh();
                    else if (agenda != null && tabs.getSelectionModel().getSelectedIndex() == 3)
                        agenda.openGlobal();
                    else {
                        refreshLearning.run();
                        refreshGrades.run();
                    }
                };
        java.util.function.IntConsumer mount =
                index -> {
                    int previous = mounted[0];
                    mounted[0] = index;
                    boolean fromHub =
                            learningTab.getContent() == null || simulationTab.getContent() == null;
                    if (hub != null) hub.detach();
                    learningTab.setContent(null);
                    simulationTab.setContent(null);
                    agendaTab.setContent(null);
                    if (index == 0) {
                        if (learningContent instanceof LearningView view) view.courseScope(false);
                        learningTab.setContent(actualLearningContent);
                        refreshLearning.run();
                    } else if (index == 1) {
                        simulationTab.setContent(gradeContent);
                        if (gradeContent instanceof GradeView view) {
                            view.courseScope(false);
                            if (fromHub) view.openGlobal();
                            else refreshGrades.run();
                        } else refreshGrades.run();
                    } else if (index == 2 && hub != null) {
                        if (previous == 3 && agenda != null && agenda.viewedSemester() != null)
                            hub.openAgenda(agenda.viewedSemester(), agenda.filteredCourse());
                        else hub.refresh();
                    } else if (index == 3 && agenda != null) {
                        agendaTab.setContent(agenda);
                        if (previous == 2 && hub != null && hub.selectedCourse() != null)
                            agenda.openGlobal(hub.viewedSemester(), hub.selectedCourse());
                        else agenda.openGlobal();
                    }
                };
        tabs.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, old, selected) -> {
                            if (selecting[0]) return;
                            selecting[0] = true;
                            tabs.getSelectionModel().select(old);
                            selecting[0] = false;
                            guard.accept(
                                    () -> {
                                        selecting[0] = true;
                                        tabs.getSelectionModel().select(selected);
                                        selecting[0] = false;
                                        mount.accept(tabs.getTabs().indexOf(selected));
                                    });
                        });
        openArea =
                index -> {
                    selecting[0] = true;
                    tabs.getSelectionModel().select(index);
                    selecting[0] = false;
                    mount.accept(index);
                };
        if (gradeContent instanceof GradeView view)
            view.onWiderCourse(
                    () -> {
                        if (hub != null) hub.detach();
                        simulationTab.setContent(gradeContent);
                        mounted[0] = 1;
                        view.openGlobal();
                        selecting[0] = true;
                        tabs.getSelectionModel().select(simulationTab);
                        selecting[0] = false;
                    });
        getChildren().add(tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
    }
}
