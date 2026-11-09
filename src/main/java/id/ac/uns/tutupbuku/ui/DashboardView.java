package id.ac.uns.tutupbuku.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Node;
import java.util.function.Consumer;
import static id.ac.uns.tutupbuku.ui.FormSupport.*;

/** Shell dua pilar dari M1, dipertahankan setelah login M2. */
public final class DashboardView extends VBox {
    public DashboardView() {
        this(null, Runnable::run, () -> { });
    }
    DashboardView(Node learningContent, Consumer<Runnable> guard, Runnable refreshLearning) {
        super(8);
        setPadding(new Insets(6));
        Tab learningTab = new Tab("Modul belajar", learningContent == null ? form(heading("Ruang belajar"),
                note("Modul kuliah\nSemester → Mata kuliah → Bab → Materi\n\nPeminatan\nBidang → Materi"),
                note("Fixture shell M1. Masuk melalui aplikasi untuk membuka fitur belajar M3.")) : learningContent);
        Tab simulationTab = new Tab("Simulasi nilai", new CalculationDemoView());
        TabPane tabs = new TabPane(learningTab, simulationTab);
        tabs.setId("main-tabs"); tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        boolean[] selecting = {false};
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selecting[0]) return;
            selecting[0] = true; tabs.getSelectionModel().select(old); selecting[0] = false;
            guard.accept(() -> { selecting[0] = true; tabs.getSelectionModel().select(selected); selecting[0] = false; if (selected == learningTab) refreshLearning.run(); });
        });
        Button learning = button("open-learning", "Buka modul belajar");
        Button simulation = button("open-simulation", "Buka simulasi nilai");
        learning.setMaxWidth(Double.MAX_VALUE); simulation.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(learning, Priority.ALWAYS); HBox.setHgrow(simulation, Priority.ALWAYS);
        learning.setOnAction(event -> { if (tabs.getSelectionModel().getSelectedItem() == learningTab) guard.accept(refreshLearning); else tabs.getSelectionModel().select(learningTab); });
        simulation.setOnAction(event -> tabs.getSelectionModel().select(simulationTab));
        getChildren().addAll(note("Materi belajar tersimpan lokal. Simulasi nilai masih contoh M1; editor komponen dan skenario menyusul M4."), new HBox(12, learning, simulation), tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
    }
}
