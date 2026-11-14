package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.PixelFormat;
import javafx.stage.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

import javax.imageio.ImageIO;

/** Opt-in GUI integration test. Seluruh data fiktif berada di @TempDir. */
class MilestoneTwoFxCheck {
    @TempDir Path directory;
    private Stage stage;
    private TutupBukuApplication application;

    @BeforeAll
    static void toolkit() throws Exception {
        CompletableFuture<Void> started = new CompletableFuture<>();
        Platform.startup(
                () -> {
                    Platform.setImplicitExit(false);
                    started.complete(null);
                });
        started.get(10, TimeUnit.SECONDS);
    }

    @AfterAll
    static void shutdown() {
        Platform.exit();
    }

    @AfterEach
    void close() throws Exception {
        if (stage != null)
            fx(
                    () -> {
                        stage.close();
                        application.stop();
                        return null;
                    });
    }

    private <T> T fx(Callable<T> action) throws Exception {
        CompletableFuture<T> completed = new CompletableFuture<>();
        Platform.runLater(
                () -> {
                    try {
                        completed.complete(action.call());
                    } catch (Throwable error) {
                        completed.completeExceptionally(error);
                    }
                });
        return completed.get(15, TimeUnit.SECONDS);
    }

    private Node node(String id) {
        Node result = stage.getScene().lookup("#" + id);
        assertNotNull(result, "Node " + id + " harus tersedia");
        return result;
    }

    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!fx(condition::getAsBoolean)) {
            if (System.nanoTime() > deadline) fail("Batas waktu menunggu UI");
            Thread.sleep(40);
        }
    }

    private void idle() throws Exception {
        await(() -> !stage.getScene().getRoot().isDisable());
    }

    private void open(Path file) throws Exception {
        fx(
                () -> {
                    stage = new Stage();
                    application = new TutupBukuApplication(file);
                    application.start(stage);
                    return null;
                });
        idle();
    }

    private void text(String id, String value) throws Exception {
        fx(
                () -> {
                    ((TextInputControl) node(id)).setText(value);
                    return null;
                });
    }

    private String label(String id) throws Exception {
        return fx(() -> ((Label) node(id)).getText());
    }

    private void click(String id) throws Exception {
        fx(
                () -> {
                    ((ButtonBase) node(id)).fire();
                    return null;
                });
        idle();
    }

    private void tab(String id, int index) throws Exception {
        fx(
                () -> {
                    ((TabPane) node(id)).getSelectionModel().select(index);
                    stage.getScene().getRoot().applyCss();
                    stage.getScene().getRoot().layout();
                    return null;
                });
        idle();
        if (id.equals("workspace-tabs")
                && index == 1
                && fx(
                        () ->
                                stage.getScene().lookup("#view-courses") != null
                                        && !((TextField) node("semester-number"))
                                                .getText()
                                                .isBlank())) click("view-courses");
    }

    private void register(String nim, String name) throws Exception {
        tab("auth-tabs", 1);
        text("register-nim", nim);
        text("register-name", name);
        text("register-password", "sandi-uji-lokal");
        text("register-confirmation", "sandi-uji-lokal");
        click("register");
    }

    private void login(String nim) throws Exception {
        tab("auth-tabs", 0);
        text("login-nim", nim);
        text("login-password", "sandi-uji-lokal");
        click("login");
    }

    private void createSemester(String number, String year) throws Exception {
        click("new-semester");
        text("semester-number", number);
        text("semester-year", year.substring(0, 4));
    }

    private int count(String id) throws Exception {
        return fx(() -> ((ListView<?>) node(id)).getItems().size());
    }

    private void selectSemester(int index) throws Exception {
        fx(
                () -> {
                    ((ListView<?>) node("semester-list")).getSelectionModel().select(index);
                    return null;
                });
        idle();
    }

    private void deleteAndRespond(String id, ButtonBar.ButtonData answer) throws Exception {
        fx(
                () -> {
                    Platform.runLater(() -> ((Button) node(id)).fire());
                    return null;
                });
        await(
                () ->
                        Window.getWindows().stream()
                                .anyMatch(
                                        w ->
                                                w.isShowing()
                                                        && w.getScene().getRoot()
                                                                instanceof DialogPane));
        fx(
                () -> {
                    DialogPane dialog =
                            (DialogPane)
                                    Window.getWindows().stream()
                                            .filter(
                                                    w ->
                                                            w.isShowing()
                                                                    && w.getScene().getRoot()
                                                                            instanceof DialogPane)
                                            .findFirst()
                                            .orElseThrow()
                                            .getScene()
                                            .getRoot();
                    ButtonType type =
                            dialog.getButtonTypes().stream()
                                    .filter(b -> b.getButtonData() == answer)
                                    .findFirst()
                                    .orElseThrow();
                    if (answer == ButtonBar.ButtonData.CANCEL_CLOSE)
                        assertTrue(((Button) dialog.lookupButton(type)).isDefaultButton());
                    ((Button) dialog.lookupButton(type)).fire();
                    return null;
                });
        idle();
    }

    private void snapshot(String name) throws Exception {
        fx(
                () -> {
                    var root = stage.getScene().getRoot();
                    root.applyCss();
                    root.layout();
                    var image = root.snapshot(null, null);
                    int width = (int) image.getWidth(), height = (int) image.getHeight();
                    int[] pixels = new int[width * height];
                    image.getPixelReader()
                            .getPixels(
                                    0,
                                    0,
                                    width,
                                    height,
                                    PixelFormat.getIntArgbInstance(),
                                    pixels,
                                    0,
                                    width);
                    var output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    output.setRGB(0, 0, width, height, pixels, 0, width);
                    Path folder = Path.of("target", "verification");
                    Files.createDirectories(folder);
                    ImageIO.write(output, "png", folder.resolve(name).toFile());
                    return null;
                });
    }

    @Test
    void registrationProfileAcademicPersistenceIsolationAndSafeDeleteFlow() throws Exception {
        Path file = directory.resolve("ui.db");
        open(file);
        snapshot("m2-login.png");
        register(" 001234 ", "Pengguna Demo");
        assertTrue(label("login-status").contains("Akun berhasil"));
        register("001234", "Duplikat");
        assertTrue(label("register-status").contains("sudah terdaftar"));
        tab("auth-tabs", 0);
        text("login-nim", "001234");
        text("login-password", "salah");
        click("login");
        assertTrue(label("login-status").contains("NIM atau password salah"));
        login("001234");
        assertNotNull(fx(() -> node("open-learning")));
        tab("workspace-tabs", 2);
        text("profile-name", "Nama Edit");
        fx(
                () -> {
                    ((ComboBox<String>) node("profile-program")).setValue("Informatika");
                    return null;
                });
        click("save-profile");
        assertEquals("Profil tersimpan.", label("profile-status"));
        tab("workspace-tabs", 1);
        createSemester("3", "2026/2027");
        click("set-active-semester");
        long firstId =
                fx(
                        () ->
                                ((Semester)
                                                ((ListView<?>) node("semester-list"))
                                                        .getSelectionModel()
                                                        .getSelectedItem())
                                        .id());
        text("course-name", "Pemrograman Berorientasi Objek");
        text("course-credits", "0");
        click("save-course");
        assertTrue(label("course-status").contains("SKS"));
        assertEquals(
                "Pemrograman Berorientasi Objek",
                fx(() -> ((TextField) node("course-name")).getText()));
        assertNull(fx(() -> stage.getScene().lookup("#course-attendance")));
        text("course-credits", "3");
        click("save-course");
        assertEquals(1, count("course-list"));
        click("save-course");
        snapshot("m2-academic.png");
        createSemester("5", "2027/2028");
        click("set-active-semester");
        assertEquals(2, count("semester-list"));
        selectSemester(0);
        assertEquals(1, count("course-list"));
        fx(
                () -> {
                    ((ListView<?>) node("course-list")).getSelectionModel().select(0);
                    return null;
                });
        deleteAndRespond("delete-course", ButtonBar.ButtonData.CANCEL_CLOSE);
        assertEquals(1, count("course-list"));
        deleteAndRespond("delete-course", ButtonBar.ButtonData.OK_DONE);
        assertEquals(0, count("course-list"));
        click("new-course");
        text("course-name", "Mata kuliah pengganti");
        text("course-credits", "3");
        click("save-course");
        assertEquals(1, count("course-list"));

        // Kegagalan SQLite nyata pada UPDATE: UI tidak mengosongkan input atau mengaku tersimpan.
        try (var inspection = new SqliteDatabase(file)) {
            inspection.read(
                    connection -> {
                        try (var statement = connection.createStatement()) {
                            statement.execute(
                                    "CREATE TRIGGER block_profile BEFORE UPDATE OF name ON users"
                                        + " BEGIN SELECT RAISE(ABORT, 'simulated save failure');"
                                        + " END");
                        }
                        return null;
                    });
            tab("workspace-tabs", 2);
            text("profile-name", "Isian belum tersimpan");
            click("save-profile");
            assertTrue(label("profile-status").contains("gagal disimpan"));
            assertEquals(
                    "Isian belum tersimpan",
                    fx(() -> ((TextField) node("profile-name")).getText()));
            assertEquals("Nama Edit", new SqliteRepository(inspection).getProfile("001234").name());
            inspection.read(
                    connection -> {
                        try (var statement = connection.createStatement()) {
                            statement.execute("DROP TRIGGER block_profile");
                        }
                        return null;
                    });
            click("save-profile");
            assertEquals("Profil tersimpan.", label("profile-status"));
        }
        click("logout");
        register("002", "Akun Kedua");
        login("002");
        tab("workspace-tabs", 1);
        assertEquals(
                0, fx(() -> ((ComboBox<?>) node("semester-editor-selector")).getItems().size()));
        click("logout");
        login("001234");
        tab("workspace-tabs", 1);
        assertEquals(2, count("semester-list"));
        selectSemester(0);
        click("edit-semesters");
        deleteAndRespond("delete-semester", ButtonBar.ButtonData.CANCEL_CLOSE);
        click("view-courses");
        assertEquals(2, count("semester-list"));
        click("edit-semesters");
        deleteAndRespond("delete-semester", ButtonBar.ButtonData.OK_DONE);
        click("view-courses");
        assertEquals(1, count("semester-list"));
        try (var inspection = new SqliteDatabase(file)) {
            var repository = new SqliteRepository(inspection);
            assertThrows(
                    IllegalArgumentException.class,
                    () -> repository.listCourses("001234", firstId));
            assertTrue(
                    repository.getProfile("001234").activeSemesterId().isPresent(),
                    "Semester aktif yang lain tetap ada");
        }
        fx(
                () -> {
                    stage.close();
                    application.stop();
                    stage = null;
                    return null;
                });
        open(file);
        login("001234");
        tab("workspace-tabs", 2);
        assertEquals("Informatika", fx(() -> ((ComboBox<?>) node("profile-program")).getValue()));
        assertEquals(
                "Isian belum tersimpan", fx(() -> ((TextField) node("profile-name")).getText()));
        tab("workspace-tabs", 1);
        assertEquals(1, count("semester-list"));
    }

    @Test
    void startupErrorOffersRetryWithoutReplacingCorruptedDatabase() throws Exception {
        Path file = directory.resolve("corrupted.db");
        byte[] bytes =
                "Data rusak, jangan direset".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, bytes);
        open(file);
        assertTrue(label("application-status").contains("tidak direset"));
        assertNotNull(fx(() -> node("retry-storage")));
        click("retry-storage");
        assertArrayEquals(bytes, Files.readAllBytes(file));
    }
}
