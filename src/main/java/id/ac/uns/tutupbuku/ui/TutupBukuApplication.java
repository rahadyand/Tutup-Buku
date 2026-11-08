package id.ac.uns.tutupbuku.ui;

import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.service.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.nio.file.Path;
import static id.ac.uns.tutupbuku.ui.FormSupport.*;

/** Entry point M2. Constructor Path memungkinkan tes menggunakan database sementara. */
public final class TutupBukuApplication extends Application {
    private final Path databaseFile;
    private SqliteDatabase database;
    private UiTasks tasks;
    private BorderPane root;
    private Label status;
    public TutupBukuApplication() { this(AppPaths.databaseFile()); }
    public TutupBukuApplication(Path databaseFile) { this.databaseFile = databaseFile; }

    @Override public void start(Stage stage) {
        root = new BorderPane(); root.setId("application-root");
        root.setStyle("-fx-background-color: #f5f7f6; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
        status = status("application-status");
        var footer = form(status);
        footer.visibleProperty().bind(status.textProperty().isNotEmpty());
        footer.managedProperty().bind(footer.visibleProperty());
        root.setBottom(footer); tasks = new UiTasks(root);
        Button retry = button("retry-storage", "Coba lagi membuka penyimpanan"); retry.setOnAction(event -> initializeStorage());
        root.setCenter(form(heading("Membuka Tutup Buku"), note("Jika penyimpanan gagal dibuka, data tidak direset.\nLokasi: " + databaseFile.toAbsolutePath()), retry));
        stage.setTitle("Tutup Buku — Milestone 2"); stage.setScene(new Scene(root, 1080, 720));
        stage.setMinWidth(900); stage.setMinHeight(620);
        stage.setOnCloseRequest(event -> {
            if (root.isDisable()) { event.consume(); status.setText("Tunggu proses selesai sebelum menutup aplikasi."); }
        });
        stage.show(); initializeStorage();
    }

    private void initializeStorage() {
        tasks.run(status, () -> new SqliteDatabase(databaseFile), opened -> {
            database = opened;
            var repository = new SqliteRepository(database); var session = new UserSession();
            var auth = new AuthService(repository, new PasswordHasher(), session);
            var academic = new AcademicService(repository, session);
            showLogin(auth, academic);
        });
    }
    private void showLogin(AuthService auth, AcademicService academic) {
        status.setText("");
        root.setCenter(new AuthView(auth, tasks, profile -> {
            status.setText("");
            root.setCenter(new WorkspaceView(auth, academic, tasks, profile, () -> showLogin(auth, academic)));
        }));
    }
    @Override public void stop() {
        if (tasks != null) tasks.close();
        if (database != null) database.close();
    }
    public static void main(String[] args) { launch(args); }
}
