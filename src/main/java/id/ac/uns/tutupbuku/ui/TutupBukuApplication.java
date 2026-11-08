package id.ac.uns.tutupbuku.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/** Shell M1: navigasi minimal; belum mengakses akun atau penyimpanan. */
public final class TutupBukuApplication extends Application {
    @Override
    public void start(Stage stage) {
        Label title = new Label("Tutup Buku");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #173b40;");
        Label subtitle = new Label("Materi tertata, target terencana.");
        Label status = new Label("Milestone 1 · Fondasi aplikasi · Data contoh tidak disimpan");
        status.setWrapText(true);
        VBox heading = new VBox(6, title, subtitle, status);

        Tab learningTab = new Tab("Modul belajar", learningPlaceholder());
        Tab simulationTab = new Tab("Simulasi nilai", new CalculationDemoView());
        TabPane tabs = new TabPane(learningTab, simulationTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Button learning = new Button("Buka modul belajar");
        Button simulation = new Button("Buka simulasi nilai");
        learning.setId("open-learning");
        simulation.setId("open-simulation");
        tabs.setId("main-tabs");
        learning.setMaxWidth(Double.MAX_VALUE);
        simulation.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(learning, Priority.ALWAYS);
        HBox.setHgrow(simulation, Priority.ALWAYS);
        learning.setOnAction(event -> tabs.getSelectionModel().select(learningTab));
        simulation.setOnAction(event -> tabs.getSelectionModel().select(simulationTab));

        VBox root = new VBox(18, heading, new HBox(12, learning, simulation), tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #f5f7f6; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");
        stage.setTitle("Tutup Buku — Milestone 1");
        stage.setScene(new Scene(root, 1000, 690));
        stage.setMinWidth(780);
        stage.setMinHeight(600);
        stage.show();
    }

    private VBox learningPlaceholder() {
        Label title = new Label("Ruang belajar");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label paths = new Label("Modul kuliah\nSemester → Mata kuliah → Bab → Materi\n\nPeminatan\nBidang → Materi");
        Label next = new Label("Pembaca/editor Markdown, gambar lokal, ringkasan manual, progres, lanjut baca, dan catatan akan dibuat pada milestone belajar.");
        next.setWrapText(true);
        Label note = new Label("Belum ada materi atau akun pada milestone ini.");
        VBox pane = new VBox(20, title, paths, next, note);
        pane.setPadding(new Insets(24));
        return pane;
    }

    public static void main(String[] args) { launch(args); }
}
