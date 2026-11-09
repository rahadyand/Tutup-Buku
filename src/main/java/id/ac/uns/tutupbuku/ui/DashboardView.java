package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

/** Shell dua pilar dari M1, dipertahankan setelah login M2. */
public final class DashboardView extends VBox {
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
        TabPane tabs = new TabPane(learningTab, simulationTab);
        tabs.setId("main-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        boolean[] selecting = {false};
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
                                        if (selected == learningTab) refreshLearning.run();
                                        else refreshGrades.run();
                                    });
                        });
        Button learning = button("open-learning", "Buka modul belajar");
        Button simulation = button("open-simulation", "Buka simulasi nilai");
        learning.setMaxWidth(Double.MAX_VALUE);
        simulation.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(learning, Priority.ALWAYS);
        HBox.setHgrow(simulation, Priority.ALWAYS);
        learning.setOnAction(
                event -> {
                    if (tabs.getSelectionModel().getSelectedItem() == learningTab)
                        guard.accept(refreshLearning);
                    else tabs.getSelectionModel().select(learningTab);
                });
        simulation.setOnAction(
                event -> {
                    if (tabs.getSelectionModel().getSelectedItem() == simulationTab)
                        guard.accept(refreshGrades);
                    else tabs.getSelectionModel().select(simulationTab);
                });
        getChildren().addAll(new HBox(12, learning, simulation), tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
    }
}
