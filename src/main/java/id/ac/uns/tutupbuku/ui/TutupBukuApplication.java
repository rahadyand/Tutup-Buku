package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.service.*;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.nio.file.Path;

/** Entry point M2. Constructor Path memungkinkan tes menggunakan database sementara. */
public final class TutupBukuApplication extends Application {
    private final Path databaseFile;
    private SqliteDatabase database;
    private UiTasks tasks;
    private BorderPane root;
    private Label status;
    private WorkspaceView workspace;
    private UserSession session;
    private boolean closing;

    public TutupBukuApplication() {
        this(AppPaths.databaseFile());
    }

    public TutupBukuApplication(Path databaseFile) {
        this.databaseFile = databaseFile;
    }

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        root.setId("application-root");
        root.setStyle(
                "-fx-background-color: #f5f7f6; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
        status = status("application-status");
        var footer = form(status);
        footer.visibleProperty().bind(status.textProperty().isNotEmpty());
        footer.managedProperty().bind(footer.visibleProperty());
        root.setBottom(footer);
        tasks = new UiTasks(root);
        Button retry = button("retry-storage", "Coba lagi membuka penyimpanan");
        retry.setOnAction(event -> initializeStorage());
        root.setCenter(
                form(
                        heading("Membuka Tutup Buku"),
                        note(
                                "Jika penyimpanan gagal dibuka, data tidak direset.\nLokasi: "
                                        + databaseFile.toAbsolutePath()),
                        retry));
        stage.setTitle("Tutup Buku — Kandidat Rilis 0.5.1-rc1");
        stage.setScene(new Scene(root, 1280, 820));
        stage.setMinWidth(760);
        stage.setMinHeight(480);
        // Visual bounds sudah dalam logical pixels; aman pada DPI tinggi tanpa mengubah skala
        // Windows.
        var bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setWidth(Math.min(1280, bounds.getWidth()));
        stage.setHeight(Math.min(820, bounds.getHeight()));
        stage.setOnCloseRequest(
                event -> {
                    if (root.isDisable()) {
                        event.consume();
                        status.setText("Tunggu proses selesai sebelum menutup aplikasi.");
                    } else if (!closing && workspace != null) {
                        event.consume();
                        workspace.requestLeave(
                                () -> {
                                    closing = true;
                                    stage.close();
                                });
                    }
                });
        stage.show();
        initializeStorage();
    }

    private void initializeStorage() {
        tasks.run(
                status,
                () -> new SqliteDatabase(databaseFile),
                opened -> {
                    database = opened;
                    var repository = new SqliteRepository(database);
                    session = new UserSession();
                    var auth = new AuthService(repository, new PasswordHasher(), session);
                    var academic = new AcademicService(repository, session);
                    showLogin(auth, academic);
                });
    }

    private void showLogin(AuthService auth, AcademicService academic) {
        workspace = null;
        status.setText("");
        root.setCenter(
                new AuthView(
                        auth,
                        tasks,
                        profile -> {
                            status.setText("");
                            var learning =
                                    new LearningService(
                                            new SqliteLearningRepository(database),
                                            session,
                                            new AssetStore(
                                                    databaseFile.toAbsolutePath().getParent()));
                            var planning =
                                    new GradePlanningService(
                                            new SqliteGradeRepository(database), session);
                            workspace =
                                    new WorkspaceView(
                                            auth,
                                            academic,
                                            learning,
                                            planning,
                                            tasks,
                                            profile,
                                            link -> getHostServices().showDocument(link),
                                            () -> showLogin(auth, academic));
                            root.setCenter(workspace);
                        }));
    }

    @Override
    public void stop() {
        if (tasks != null) tasks.close();
        if (database != null) database.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
