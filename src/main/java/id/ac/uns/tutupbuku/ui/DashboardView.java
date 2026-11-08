package id.ac.uns.tutupbuku.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import static id.ac.uns.tutupbuku.ui.FormSupport.*;

/** Shell dua pilar dari M1, dipertahankan setelah login M2. */
public final class DashboardView extends VBox {
    public DashboardView() {
        super(14);
        setPadding(new Insets(18));
        Tab learningTab = new Tab("Modul belajar", form(heading("Ruang belajar"),
                note("Modul kuliah\nSemester → Mata kuliah → Bab → Materi\n\nPeminatan\nBidang → Materi"),
                note("Pembaca/editor Markdown, gambar lokal, ringkasan manual, progres, lanjut baca, dan catatan belum tersedia. Fitur belajar dijadwalkan pada M3.")));
        Tab simulationTab = new Tab("Simulasi nilai", new CalculationDemoView());
        TabPane tabs = new TabPane(learningTab, simulationTab);
        tabs.setId("main-tabs"); tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Button learning = button("open-learning", "Buka modul belajar");
        Button simulation = button("open-simulation", "Buka simulasi nilai");
        learning.setMaxWidth(Double.MAX_VALUE); simulation.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(learning, Priority.ALWAYS); HBox.setHgrow(simulation, Priority.ALWAYS);
        learning.setOnAction(event -> tabs.getSelectionModel().select(learningTab));
        simulation.setOnAction(event -> tabs.getSelectionModel().select(simulationTab));
        getChildren().addAll(note("M2: akun dan data akademik tersimpan lokal. Simulasi di bawah adalah contoh tetap M1, belum memakai mata kuliah Anda. Editor komponen dan skenario menyusul M4."), new HBox(12, learning, simulation), tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
    }
}
