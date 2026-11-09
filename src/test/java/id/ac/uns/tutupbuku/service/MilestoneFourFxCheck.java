package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.PixelFormat;
import javafx.stage.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

import javax.imageio.ImageIO;

/** Peluncuran aplikasi asli dan interaksi kontrol JavaFX; seluruh data hanya di @TempDir. */
class MilestoneFourFxCheck {
    @TempDir Path directory;
    Stage stage;
    TutupBukuApplication application;

    <T> T fx(Callable<T> action) throws Exception {
        var f = new CompletableFuture<T>();
        Platform.runLater(
                () -> {
                    try {
                        f.complete(action.call());
                    } catch (Throwable error) {
                        f.completeExceptionally(error);
                    }
                });
        return f.get(15, TimeUnit.SECONDS);
    }

    javafx.scene.Node node(String id) {
        var node = stage.getScene().lookup("#" + id);
        assertNotNull(node, id);
        return node;
    }

    void await(BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!fx(condition::getAsBoolean)) {
            if (System.nanoTime() > end) fail("UI timeout");
            Thread.sleep(40);
        }
    }

    void idle() throws Exception {
        await(() -> !stage.getScene().getRoot().isDisable());
    }

    void click(String id) throws Exception {
        idle();
        fx(
                () -> {
                    assertFalse(node(id).isDisabled(), id);
                    ((ButtonBase) node(id)).fire();
                    return null;
                });
        idle();
    }

    void text(String id, String text) throws Exception {
        fx(
                () -> {
                    ((TextInputControl) node(id)).setText(text);
                    return null;
                });
    }

    String label(String id) throws Exception {
        return fx(() -> ((Label) node(id)).getText());
    }

    void tab(String id, int index) throws Exception {
        fx(
                () -> {
                    ((TabPane) node(id)).getSelectionModel().select(index);
                    return null;
                });
        idle();
    }

    void combo(String id, Object value) throws Exception {
        fx(
                () -> {
                    ((ComboBox<Object>) node(id)).setValue(value);
                    return null;
                });
        idle();
    }

    void select(String id, int index) throws Exception {
        fx(
                () -> {
                    ((ListView<?>) node(id)).getSelectionModel().select(index);
                    return null;
                });
        idle();
    }

    void scroll(String id, double position) throws Exception {
        fx(
                () -> {
                    javafx.scene.Node parent = node(id);
                    while (parent != null && !(parent instanceof ScrollPane))
                        parent = parent.getParent();
                    assertNotNull(parent);
                    ((ScrollPane) parent).setVvalue(position);
                    return null;
                });
    }

    void dialog(Runnable action, ButtonBar.ButtonData response) throws Exception {
        fx(
                () -> {
                    Platform.runLater(action);
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
                    var pane =
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
                    var type =
                            pane.getButtonTypes().stream()
                                    .filter(b -> b.getButtonData() == response)
                                    .findFirst()
                                    .orElseThrow();
                    ((Button) pane.lookupButton(type)).fire();
                    return null;
                });
        idle();
    }

    void screenshot(String name) throws Exception {
        fx(
                () -> {
                    var root = stage.getScene().getRoot();
                    root.applyCss();
                    root.layout();
                    var snapshot = root.snapshot(null, null);
                    int w = (int) snapshot.getWidth(), h = (int) snapshot.getHeight();
                    int[] pixels = new int[w * h];
                    snapshot.getPixelReader()
                            .getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
                    var image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    image.setRGB(0, 0, w, h, pixels, 0, w);
                    Files.createDirectories(Path.of("target/verification"));
                    ImageIO.write(image, "png", Path.of("target/verification", name).toFile());
                    return null;
                });
    }

    void login() throws Exception {
        await(() -> stage.getScene().lookup("#login-nim") != null);
        text("login-nim", "001");
        text("login-password", "sandi-uji-lokal");
        click("login");
        click("open-simulation");
    }

    @Test
    void actualTargetsSavedScenariosReviewComparisonAndFailureRecovery() throws Exception {
        Path file = directory.resolve("m4-ui.db");
        try (var db = new SqliteDatabase(file)) {
            var repo = new SqliteRepository(db);
            repo.createAccount(
                    "001",
                    "Pengguna Nilai",
                    new PasswordHasher().hash("sandi-uji-lokal".toCharArray()));
            var semester =
                    repo.saveSemester("001", new Semester(0, 3, "2026/2027", SemesterPeriod.ODD));
            long course =
                    repo.saveCourse(
                                    "001",
                                    new Course(
                                            0,
                                            semester.id(),
                                            "PBO",
                                            "IF",
                                            TestData.n("3"),
                                            Attendance.assumedMet(),
                                            "75%",
                                            0))
                            .id();
            var session = new UserSession();
            session.login("001");
            var grades = new GradePlanningService(new SqliteGradeRepository(db), session);
            grades.saveComponent(course, "tasks", "Tugas", "20", "", "10", 0);
            grades.saveComponent(course, "uts", "UTS", "30", "", "100", 1);
            grades.saveComponent(course, "uas", "UAS", "50", "", "100", 2);
        }
        var started = new CompletableFuture<Void>();
        Platform.startup(
                () -> {
                    Platform.setImplicitExit(false);
                    started.complete(null);
                });
        started.get(10, TimeUnit.SECONDS);
        try {
            fx(
                    () -> {
                        stage = new Stage();
                        application = new TutupBukuApplication(file);
                        application.start(stage);
                        return null;
                    });
            idle();
            login();
            assertTrue(label("component-total").contains("100%"));
            assertTrue(label("actual-result").contains("Belum lengkap"));
            click("new-component");
            text("component-name", "Tambahan informasi");
            text("component-weight", "0");
            text("component-maximum", "10");
            click("save-component");
            select("assessment-components", 3);
            click("component-up");
            select("assessment-components", 2);
            dialog(() -> ((Button) node("delete-component")).fire(), ButtonBar.ButtonData.OK_DONE);
            tab("grade-tabs", 1);
            click("new-scenario");
            text("scenario-name", "Target lulus");
            click("calculate-scenario");
            assertTrue(label("suggestion-tasks").contains("60"));
            assertTrue(label("suggestion-uas").contains("60"));
            click("lock-tasks");
            text("assumption-tasks", "80");
            click("calculate-scenario");
            assertTrue(label("suggestion-uts").contains("55"));
            click("save-scenario");
            tab("grade-tabs", 0);
            select("assessment-components", 1);
            text("component-earned", "30");
            try (var inspection = new SqliteDatabase(file)) {
                inspection.write(
                        c -> {
                            try (var s = c.createStatement()) {
                                s.execute(
                                        "CREATE TRIGGER fail_actual BEFORE UPDATE ON"
                                            + " assessment_components BEGIN SELECT"
                                            + " RAISE(ABORT,'fixture failure'); END");
                            }
                            return null;
                        });
                click("save-component");
                assertTrue(label("actual-grade-status").contains("gagal disimpan"));
                assertEquals("30", fx(() -> ((TextField) node("component-earned")).getText()));
                inspection.write(
                        c -> {
                            try (var s = c.createStatement()) {
                                s.execute("DROP TRIGGER fail_actual");
                            }
                            return null;
                        });
            }
            click("save-component");
            tab("grade-tabs", 1);
            click("calculate-scenario");
            assertTrue(label("scenario-review").contains("USANG"));
            dialog(() -> ((Button) node("refresh-scenario")).fire(), ButtonBar.ButtonData.OK_DONE);
            assertTrue(label("suggestion-uas").contains("70"));
            click("save-scenario");
            scroll("suggestion-uas", 0.65);
            screenshot("m4-targets.png");
            click("duplicate-scenario");
            text("scenario-name", "Optimis");
            combo("scenario-mode", EvaluationMode.DIRECT);
            combo("scenario-estimate", LetterGrade.A);
            click("calculate-scenario");
            assertTrue(label("scenario-gpa-result").contains("4,00"));
            click("save-scenario");
            fx(
                    () -> {
                        var box = (ComboBox<SemesterScenario>) node("compare-scenario");
                        box.setValue(
                                box.getItems().stream()
                                        .filter(s -> s.name().equals("Target lulus"))
                                        .findFirst()
                                        .orElseThrow());
                        return null;
                    });
            click("compare-scenarios");
            assertEquals(
                    1,
                    fx(() -> ((TableView<?>) node("scenario-comparison-table")).getItems().size()));
            scroll("scenario-comparison-table", 1);
            screenshot("m4-comparison.png");
            click("scenario-attendance");
            click("calculate-scenario");
            assertTrue(label("scenario-gpa-result").contains("0,00"));
            assertTrue(label("scenario-course-result").contains("Efektif: E"));
            click("scenario-attendance");
            click("save-scenario");
            try (var inspection = new SqliteDatabase(file)) {
                inspection.write(
                        c -> {
                            try (var s = c.createStatement()) {
                                s.execute(
                                        "CREATE TRIGGER fail_scenario BEFORE UPDATE ON"
                                            + " grade_scenarios BEGIN SELECT RAISE(ABORT,'fixture"
                                            + " failure'); END");
                            }
                            return null;
                        });
                text("scenario-name", "Isian tidak hilang");
                click("save-scenario");
                assertTrue(label("scenario-status").contains("gagal disimpan"));
                assertEquals(
                        "Isian tidak hilang",
                        fx(() -> ((TextField) node("scenario-name")).getText()));
                dialog(
                        () -> ((TabPane) node("workspace-tabs")).getSelectionModel().select(1),
                        ButtonBar.ButtonData.CANCEL_CLOSE);
                assertEquals(
                        0,
                        fx(
                                () ->
                                        ((TabPane) node("workspace-tabs"))
                                                .getSelectionModel()
                                                .getSelectedIndex()));
                inspection.write(
                        c -> {
                            try (var s = c.createStatement()) {
                                s.execute("DROP TRIGGER fail_scenario");
                            }
                            return null;
                        });
                click("save-scenario");
            }
            screenshot("m4-scenarios.png");
            click("logout");
            fx(
                    () -> {
                        stage.close();
                        application.stop();
                        return null;
                    });
            fx(
                    () -> {
                        stage = new Stage();
                        application = new TutupBukuApplication(file);
                        application.start(stage);
                        return null;
                    });
            idle();
            login();
            tab("grade-tabs", 1);
            assertEquals(2, fx(() -> ((ListView<?>) node("saved-scenarios")).getItems().size()));
            select("saved-scenarios", 0);
            assertEquals(
                    "Isian tidak hilang", fx(() -> ((TextField) node("scenario-name")).getText()));
            assertTrue(label("scenario-gpa-result").contains("4,00"));
        } catch (Throwable error) {
            screenshot("m4-failure.png");
            throw error;
        } finally {
            fx(
                    () -> {
                        stage.close();
                        application.stop();
                        return null;
                    });
            Platform.exit();
        }
    }
}
