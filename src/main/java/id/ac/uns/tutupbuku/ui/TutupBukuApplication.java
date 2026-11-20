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
    private final java.time.Clock clock;
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
        this(databaseFile, java.time.Clock.system(java.time.ZoneId.of("Asia/Jakarta")));
    }

    public TutupBukuApplication(Path databaseFile, java.time.Clock clock) {
        this.databaseFile = databaseFile;
        this.clock = clock;
    }

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        root.setId("application-root");
        root.getStyleClass().addAll("tutup-buku-root", "tb-theme");
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
        stage.setTitle("Tutup Buku — Kandidat Rilis 0.5.5-rc1");
        Scene scene = new Scene(root, 1280, 820);
        scene.getStylesheets()
                .add(TutupBukuApplication.class.getResource("tutup-buku.css").toExternalForm());
        stage.setScene(scene);
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
                                    workspace.suspend();
                                    tasks.finish(
                                            () -> {
                                                closing = true;
                                                stage.close();
                                            });
                                });
                    }
                });
        stage.focusedProperty()
                .addListener(
                        (o, old, focused) -> {
                            if (focused && workspace != null && !root.isDisable())
                                workspace.refreshDateIfNeeded();
                        });
        stage.show();
        initializeStorage();
    }

    private void initializeStorage() {
        if (database != null) {
            openGuest();
            return;
        }
        tasks.run(
                status,
                () -> new SqliteDatabase(databaseFile),
                opened -> {
                    database = opened;
                    openGuest();
                });
    }

    private AuthService newAuth() {
        session = new UserSession();
        return new AuthService(new SqliteRepository(database), new PasswordHasher(), session);
    }

    private void openGuest() {
        var auth = newAuth();
        tasks.run(
                status,
                auth::openGuest,
                profile -> showWorkspace(auth, profile),
                error -> {
                    Button retry =
                            button("retry-local-workspace", "Coba lagi membuka ruang kerja lokal");
                    retry.setOnAction(e -> openGuest());
                    root.setCenter(
                            form(
                                    heading("Ruang kerja lokal belum dapat dibuka"),
                                    note("Data tidak direset. Periksa penyimpanan lalu coba lagi."),
                                    retry));
                });
    }

    private void switchTasks(Runnable next) {
        if (workspace != null) workspace.suspend();
        workspace = null;
        root.setCenter(
                form(
                        heading("Membuka ruang kerja"),
                        note("Menyelesaikan penyimpanan sebelum membuka ruang berikutnya…")));
        tasks.finish(
                () -> {
                    tasks = new UiTasks(root);
                    status.setText("");
                    next.run();
                });
    }

    private void showLogin(boolean registration) {
        var auth = newAuth();
        root.setCenter(
                new AuthView(
                        auth,
                        tasks,
                        profile -> switchTasks(() -> showWorkspace(auth, profile)),
                        () ->
                                switchTasks(
                                        () -> {
                                            auth.logout();
                                            openGuest();
                                        }),
                        registration));
    }

    private void showWorkspace(AuthService auth, id.ac.uns.tutupbuku.domain.UserProfile profile) {
        status.setText("");
        var academic = new AcademicService(new SqliteRepository(database), session);
        var learning =
                new LearningService(
                        new SqliteLearningRepository(database),
                        session,
                        new AssetStore(databaseFile.toAbsolutePath().getParent()));
        var planning = new GradePlanningService(new SqliteGradeRepository(database), session);
        workspace =
                new WorkspaceView(
                        auth,
                        academic,
                        learning,
                        planning,
                        new AgendaService(new SqliteAgendaRepository(database), session, clock),
                        tasks,
                        profile,
                        link -> getHostServices().showDocument(link),
                        () ->
                                switchTasks(
                                        () -> {
                                            auth.logout();
                                            openGuest();
                                        }),
                        registration ->
                                switchTasks(
                                        () -> {
                                            auth.logout();
                                            showLogin(registration);
                                        }));
        root.setCenter(workspace);
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
