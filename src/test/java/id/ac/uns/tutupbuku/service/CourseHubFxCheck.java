package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.ui.*;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/** Jalur produksi, satu reader/editor dipindahkan; tidak pernah memakai database pengguna. */
class CourseHubFxCheck extends FxTestSupport {
    private long first,
            second,
            empty,
            archived,
            currentSemester,
            oldSemester,
            materialA,
            materialB,
            interest;
    private final String suffix = System.getProperty("glass.win.uiScale", "native");

    private void press(String id) throws Exception {
        await(() -> stage.getScene().lookup("#" + id) != null);
        click(id);
    }

    private void hubCourse(long id) throws Exception {
        fx(
                () -> {
                    var box = (ComboBox<Course>) node("hub-course");
                    box.setValue(
                            box.getItems().stream()
                                    .filter(c -> c.id() == id)
                                    .findFirst()
                                    .orElseThrow());
                    return null;
                });
        idle();
    }

    private void hubSemester(long id) throws Exception {
        fx(
                () -> {
                    var box = (ComboBox<Semester>) node("hub-semester");
                    box.setValue(
                            box.getItems().stream()
                                    .filter(s -> s.id() == id)
                                    .findFirst()
                                    .orElseThrow());
                    return null;
                });
        idle();
    }

    private void ready(long material) throws Exception {
        await(
                () ->
                        stage.getScene().lookup("#material-reader") != null
                                && ((MarkdownReader) node("material-reader").getParent()).ready()
                                && ((ListView<Material>) node("learning-materials"))
                                                .getSelectionModel()
                                                .getSelectedItem()
                                        != null
                                && ((ListView<Material>) node("learning-materials"))
                                                .getSelectionModel()
                                                .getSelectedItem()
                                                .id()
                                        == material);
    }

    private void shot(String name) throws Exception {
        var painted = new CompletableFuture<Void>();
        fx(
                () -> {
                    new javafx.animation.AnimationTimer() {
                        int frames;

                        @Override
                        public void handle(long now) {
                            if (++frames == 3) {
                                stop();
                                painted.complete(null);
                            }
                        }
                    }.start();
                    return null;
                });
        painted.get(5, TimeUnit.SECONDS);
        screenshot("course-hub-" + name + "-" + suffix + ".png");
    }

    private void trigger(SqliteDatabase db, String sql) {
        db.read(
                c -> {
                    try (var s = c.createStatement()) {
                        s.execute(sql);
                    }
                    return null;
                });
    }

    private MaterialReading material(LearningService learning, long course, String name) {
        var folder = learning.saveFolder(0, LearningLocation.Kind.CHAPTER, course, "Bab " + name);
        return learning.save(
                new Material(
                        0,
                        folder.location(),
                        name,
                        "# "
                                + name
                                + "\n\n"
                                + ("Paragraf untuk menguji posisi baca.\n\n".repeat(100)),
                        "Sumber asli",
                        0,
                        0,
                        ""),
                "Catatan " + name,
                0);
    }

    private void fixture(Path file) {
        try (var db = new SqliteDatabase(file)) {
            var repo = new SqliteRepository(db);
            var hash = new PasswordHasher().hash("sandi-uji-lokal".toCharArray());
            repo.createAccount("001", "Ruang Kuliah", hash);
            repo.createAccount("002", "Akun Kosong", hash);
            var session = new UserSession();
            session.login("001");
            var academic = new AcademicService(repo, session);
            currentSemester = academic.saveSemesterForm(0, "3", "2026", true).id();
            oldSemester = academic.saveSemesterForm(0, "1", "2025", false).id();
            first = academic.saveCourseMetadata(0, currentSemester, "PBO", "PBO-21", "3").id();
            second = academic.saveCourseMetadata(0, currentSemester, "Struktur Data", "", "2").id();
            empty = academic.saveCourseMetadata(0, currentSemester, "Kosong", "", "3").id();
            archived = academic.saveCourseMetadata(0, oldSemester, "Dasar Komputasi", "", "2").id();
            var grades = new GradePlanningService(new SqliteGradeRepository(db), session);
            grades.saveComponent(first, "tugas", "Tugas", "20", "", "10", 0);
            grades.saveComponent(first, "proyek", "Proyek", "30", "30", "100", 1);
            grades.saveComponent(first, "akhir", "Penilaian akhir", "50", "", "100", 2);
            grades.saveActual(
                    second,
                    EvaluationMode.DIRECT,
                    Optional.of(LetterGrade.B),
                    new Attendance(true, true));
            var basis =
                    grades.courses(currentSemester).stream()
                            .filter(c -> c.course().id() == first)
                            .findFirst()
                            .orElseThrow();
            grades.save(
                    grades.fromWhatIf(
                            grades.whatIfInput(
                                    basis,
                                    TestData.n("60"),
                                    Map.of("tugas", TestData.n("80")),
                                    Set.of()),
                            "Skenario PBO"));
            grades.saveComponent(first, "proyek", "Proyek", "30", "30", "100", 1);
            var learning =
                    new LearningService(
                            new SqliteLearningRepository(db), session, new AssetStore(directory));
            var a = material(learning, first, "Materi A");
            materialA = a.material().id();
            var extra =
                    learning.save(
                            new Material(
                                    0,
                                    a.material().location(),
                                    "Materi kedua",
                                    "# Kedua",
                                    "",
                                    1,
                                    0,
                                    ""),
                            "",
                            0);
            learning.complete(extra.material().id(), true);
            var b = material(learning, second, "Materi B");
            materialB = b.material().id();
            var field = learning.saveFolder(0, LearningLocation.Kind.INTEREST, 0, "ML");
            var ml =
                    learning.save(
                            new Material(0, field.location(), "Peminatan ML", "# ML", "", 0, 0, ""),
                            "Catatan ML",
                            0);
            interest = ml.material().id();
            for (var r : List.of(a, b, ml))
                learning.bookmarkWriter(
                                new ReadingBookmark(
                                        r.material().id(),
                                        r.material().revision(),
                                        r == a ? 0.6 : r == b ? 0.4 : 0.0,
                                        r.material().location()))
                        .run();
        }
    }

    @Test
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void connectedCourseJourneyPreservesContextAndUsesOneReaderAndGradeWorkspace()
            throws Exception {
        Path file = directory.resolve("hub-ui.db");
        fixture(file);
        var asynchronousErrors = new CopyOnWriteArrayList<Throwable>();
        var previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, error) -> {
                    asynchronousErrors.add(error);
                    error.printStackTrace();
                });
        var started = new CompletableFuture<Void>();
        Platform.startup(
                () -> {
                    Platform.setImplicitExit(false);
                    started.complete(null);
                });
        started.get(10, TimeUnit.SECONDS);
        try (var inspection = new SqliteDatabase(file)) {
            var session = new UserSession();
            session.login("001");
            var grades = new GradePlanningService(new SqliteGradeRepository(inspection), session);
            var learning =
                    new LearningService(
                            new SqliteLearningRepository(inspection),
                            session,
                            new AssetStore(directory));
            var repo = new SqliteRepository(inspection);
            var actualBefore = grades.courses(currentSemester);
            fx(
                    () -> {
                        stage = new Stage();
                        application = new TutupBukuApplication(file);
                        application.start(stage);
                        stage.setWidth(1366 / stage.getOutputScaleX());
                        stage.setHeight(768 / stage.getOutputScaleY());
                        return null;
                    });
            login();
            tab("main-tabs", 2);
            hubCourse(first);
            assertTrue(label("hub-context").contains("PBO-21"));
            assertTrue(label("hub-learning-status").contains("1/2"));
            assertTrue(label("hub-grade-status").contains("Belum lengkap"));
            assertFalse(label("hub-grade-status").contains("Lulus"));
            assertTrue(label("hub-scenario-status").contains("1 usang"));
            shot("overview");
            press("hub-resume");
            ready(materialA);
            var reader = fx(() -> node("material-reader"));
            assertTrue(fx(() -> ((MarkdownReader) reader.getParent()).scrollRatio()) > 0.5);
            assertNotNull(fx(() -> node("edit-material")));
            assertNull(fx(() -> stage.getScene().lookup("#save-material")));
            shot("materials");
            fx(
                    () -> {
                        ((TitledPane) node("material-notes-pane")).setExpanded(true);
                        return null;
                    });
            text("material-note", "Catatan A dari hub");
            dialog(
                    () -> ((TabPane) node("hub-sections")).getSelectionModel().select(0),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(
                    1,
                    fx(
                            () ->
                                    ((TabPane) node("hub-sections"))
                                            .getSelectionModel()
                                            .getSelectedIndex()));
            assertEquals(
                    "Catatan A dari hub", fx(() -> ((TextArea) node("material-note")).getText()));
            press("save-note");
            press("material-completed");
            assertTrue(learning.open(materialA).completed());
            assertEquals("Catatan A dari hub", learning.open(materialA).note());
            press("edit-material");
            trigger(
                    inspection,
                    "CREATE TRIGGER fail_hub BEFORE UPDATE ON materials BEGIN SELECT"
                            + " RAISE(ABORT,'fixture'); END");
            text("material-title", "Materi A berubah");
            dialog(
                    () -> {
                        var box = (ComboBox<Course>) node("hub-course");
                        box.setValue(
                                box.getItems().stream()
                                        .filter(c -> c.id() == second)
                                        .findFirst()
                                        .orElseThrow());
                    },
                    ButtonBar.ButtonData.YES);
            assertEquals(first, fx(() -> ((ComboBox<Course>) node("hub-course")).getValue().id()));
            assertEquals(
                    "Materi A berubah", fx(() -> ((TextField) node("material-title")).getText()));
            assertEquals("Materi A", learning.open(materialA).material().title());
            shot("save-error");
            trigger(inspection, "DROP TRIGGER fail_hub");
            press("save-material");
            press("read-material");
            ready(materialA);
            assertEquals("Materi A berubah", learning.open(materialA).material().title());
            assertTrue(
                    fx(() -> ((MarkdownReader) node("material-reader").getParent()).scrollRatio())
                            < 0.05);
            hubCourse(second);
            ready(materialB);
            assertSame(reader, fx(() -> node("material-reader")));
            assertEquals(
                    "Catatan Materi B", fx(() -> ((TextArea) node("material-note")).getText()));
            tab("hub-sections", 0);
            assertTrue(label("hub-grade-status").contains("Huruf langsung"));
            assertTrue(label("hub-grade-status").contains("Efektif: B"));
            shot("direct");
            press("open-learning");
            ready(materialB);
            assertSame(reader, fx(() -> node("material-reader")));
            assertEquals(
                    "Catatan Materi B", fx(() -> ((TextArea) node("material-note")).getText()));
            combo("learning-path", "Peminatan");
            select("learning-folders", 0);
            select("learning-materials", 0);
            ready(interest);
            tab("main-tabs", 2);
            press("hub-resume");
            ready(materialB);
            assertSame(reader, fx(() -> node("material-reader")));
            tab("hub-sections", 0);
            hubCourse(first);
            press("hub-open-actual");
            await(() -> stage.getScene().lookup("#actual-mode") != null);
            assertEquals(
                    3, fx(() -> ((ListView<?>) node("assessment-components")).getItems().size()));
            assertNull(fx(() -> stage.getScene().lookup("#grade-courses")));
            shot("grades");
            select("assessment-components", 0);
            text("component-earned", "8");
            dialog(
                    () -> {
                        var box = (ComboBox<Semester>) node("hub-semester");
                        box.setValue(
                                box.getItems().stream()
                                        .filter(s -> s.id() == oldSemester)
                                        .findFirst()
                                        .orElseThrow());
                    },
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(
                    currentSemester,
                    fx(() -> ((ComboBox<Semester>) node("hub-semester")).getValue().id()));
            assertEquals("8", fx(() -> ((TextField) node("component-earned")).getText()));
            dialog(
                    () -> ((Button) node("open-learning")).fire(),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(
                    2,
                    fx(() -> ((TabPane) node("main-tabs")).getSelectionModel().getSelectedIndex()));
            trigger(
                    inspection,
                    "CREATE TRIGGER fail_hub_actual BEFORE UPDATE ON assessment_components BEGIN"
                            + " SELECT RAISE(ABORT,'fixture'); END");
            dialog(
                    () -> ((TabPane) node("hub-sections")).getSelectionModel().select(0),
                    ButtonBar.ButtonData.YES);
            assertEquals(
                    2,
                    fx(
                            () ->
                                    ((TabPane) node("hub-sections"))
                                            .getSelectionModel()
                                            .getSelectedIndex()));
            assertEquals("8", fx(() -> ((TextField) node("component-earned")).getText()));
            trigger(inspection, "DROP TRIGGER fail_hub_actual");
            dialog(
                    () -> ((TabPane) node("hub-sections")).getSelectionModel().select(0),
                    ButtonBar.ButtonData.NO);
            press("hub-open-actual");
            press("open-what-if");
            await(() -> stage.getScene().lookup("#what-if-pane") != null);
            assertTrue(label("what-if-context").contains("PBO"));
            press("what-if-lock-tugas");
            text("what-if-score-tugas", "80");
            await(() -> ((Label) node("what-if-required-akhir")).getText().contains("70/100"));
            dialog(
                    () -> ((TabPane) node("hub-sections")).getSelectionModel().select(0),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals("80", fx(() -> ((TextField) node("what-if-score-tugas")).getText()));
            dialog(
                    () ->
                            stage.fireEvent(
                                    new javafx.stage.WindowEvent(
                                            stage, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST)),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertTrue(fx(() -> stage.isShowing()));
            dialog(
                    () -> ((TabPane) node("hub-sections")).getSelectionModel().select(0),
                    ButtonBar.ButtonData.NO);
            press("hub-open-planning");
            await(() -> stage.getScene().lookup("#saved-scenarios") != null);
            assertNull(
                    fx(
                            () ->
                                    ((ListView<?>) node("saved-scenarios"))
                                            .getSelectionModel()
                                            .getSelectedItem()));
            select("saved-scenarios", 0);
            assertTrue(label("scenario-context").contains("USANG"));
            shot("scenario");
            text("scenario-name", "Draf skenario dari hub");
            dialog(
                    () -> {
                        var box = (ComboBox<Course>) node("hub-course");
                        box.setValue(
                                box.getItems().stream()
                                        .filter(c -> c.id() == second)
                                        .findFirst()
                                        .orElseThrow());
                    },
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(first, fx(() -> ((ComboBox<Course>) node("hub-course")).getValue().id()));
            assertEquals(
                    "Draf skenario dari hub",
                    fx(() -> ((TextField) node("scenario-name")).getText()));
            trigger(
                    inspection,
                    "CREATE TRIGGER fail_hub_scenario BEFORE INSERT ON scenario_items BEGIN SELECT"
                            + " RAISE(ABORT,'fixture'); END");
            dialog(() -> ((Button) node("open-learning")).fire(), ButtonBar.ButtonData.YES);
            assertEquals(
                    2,
                    fx(() -> ((TabPane) node("main-tabs")).getSelectionModel().getSelectedIndex()));
            assertEquals(
                    "Draf skenario dari hub",
                    fx(() -> ((TextField) node("scenario-name")).getText()));
            trigger(inspection, "DROP TRIGGER fail_hub_scenario");
            press("save-scenario");
            press("edit-scenario-what-if");
            await(() -> stage.getScene().lookup("#what-if-pane") != null);
            assertTrue(label("what-if-state").contains("BASIS USANG"));
            tab("hub-sections", 0);
            press("open-simulation");
            tab("grade-tabs", 1);
            select("saved-scenarios", 0);
            tab("main-tabs", 2);
            press("hub-open-planning");
            assertNull(
                    fx(
                            () ->
                                    ((ListView<?>) node("saved-scenarios"))
                                            .getSelectionModel()
                                            .getSelectedItem()));
            tab("hub-sections", 0);
            hubSemester(oldSemester);
            assertEquals(currentSemester, repo.getProfile("001").activeSemesterId().orElseThrow());
            assertTrue(label("hub-active-context").contains("arsip"));
            shot("archived");
            hubSemester(currentSemester);
            hubCourse(empty);
            assertTrue(label("hub-learning-status").contains("Belum ada materi"));
            assertTrue(label("hub-grade-status").contains("Belum ada struktur"));
            shot("empty");
            press("hub-open-materials");
            await(() -> stage.getScene().lookup("#manage-learning") != null);
            assertTrue(((ListView<?>) fx(() -> node("learning-materials"))).getItems().isEmpty());
            assertNotNull(fx(() -> node("manage-learning")));
            tab("workspace-tabs", 1);
            new AcademicService(repo, session).deleteCourse(currentSemester, empty);
            tab("workspace-tabs", 0);
            assertNotEquals(
                    empty, fx(() -> ((ComboBox<Course>) node("hub-course")).getValue().id()));
            tab("hub-sections", 0);
            hubCourse(first);
            if (System.getProperty("glass.win.uiScale") == null)
                for (int width : new int[] {1024, 760}) {
                    fx(
                            () -> {
                                stage.setWidth(width);
                                stage.setHeight(width == 760 ? 480 : 768);
                                return null;
                            });
                    shot("overview-" + width);
                    fx(
                            () -> {
                                for (String id :
                                        List.of("hub-course", "hub-semester", "hub-refresh")) {
                                    var bounds = node(id).localToScene(node(id).getBoundsInLocal());
                                    assertTrue(
                                            bounds.getMinY() >= 0
                                                    && bounds.getMaxY()
                                                            <= stage.getScene().getHeight(),
                                            id);
                                }
                                return null;
                            });
                    press("hub-open-materials");
                    ready(materialA);
                    shot("materials-" + width);
                    assertTrue(fx(() -> ((WebView) node("material-reader")).getHeight()) >= 70);
                    tab("hub-sections", 0);
                    press("hub-open-actual");
                    await(() -> stage.getScene().lookup("#actual-mode") != null);
                    shot("grades-" + width);
                    assertTrue(
                            fx(
                                            () ->
                                                    ((ScrollPane) node("hub-grade-page"))
                                                            .getViewportBounds()
                                                            .getHeight())
                                    > 60);
                    tab("hub-sections", 0);
                }
            assertEquals(
                    actualBefore.stream().filter(c -> c.course().id() != empty).toList(),
                    grades.courses(currentSemester));
            assertEquals(currentSemester, repo.getProfile("001").activeSemesterId().orElseThrow());
            press("logout");
            text("login-nim", "002");
            text("login-password", "sandi-uji-lokal");
            press("login");
            tab("main-tabs", 2);
            assertTrue(label("hub-context").contains("Pilih mata kuliah"));
            assertTrue(fx(() -> ((ComboBox<?>) node("hub-course")).getItems().isEmpty()));
            press("logout");
            fx(
                    () -> {
                        stage.hide();
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
            login();
            tab("main-tabs", 2);
            hubCourse(first);
            assertTrue(learning.open(materialA).completed());
            assertEquals("Catatan A dari hub", learning.open(materialA).note());
            press("hub-resume");
            ready(materialA);
            assertTrue(asynchronousErrors.isEmpty(), asynchronousErrors.toString());
        } catch (Throwable error) {
            screenshot("course-hub-failure.png");
            throw error;
        } finally {
            fx(
                    () -> {
                        if (stage != null) stage.hide();
                        if (application != null) application.stop();
                        return null;
                    });
            Platform.exit();
            Thread.setDefaultUncaughtExceptionHandler(previousHandler);
        }
    }
}
