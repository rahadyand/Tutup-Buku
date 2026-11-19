package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/** Perjalanan UI produksi; akun dan database selalu sementara. */
class ScenarioJourneyFxCheck extends FxTestSupport {
    private String suffix = System.getProperty("glass.win.uiScale", "native");

    private void press(String id) throws Exception {
        await(() -> stage.getScene().lookup("#" + id) != null);
        click(id);
    }

    private void required(String id, String number) throws Exception {
        await(
                () ->
                        ((Label) node("what-if-required-" + id))
                                .getText()
                                .contains("Diperlukan: " + number + "/100"));
    }

    private void nameDialog(Runnable action, String name) throws Exception {
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
                                                                instanceof DialogPane p
                                                        && p.lookup("#what-if-name") != null));
        fx(
                () -> {
                    DialogPane pane =
                            (DialogPane)
                                    Window.getWindows().stream()
                                            .filter(
                                                    w ->
                                                            w.isShowing()
                                                                    && w.getScene().getRoot()
                                                                            instanceof DialogPane p
                                                                    && p.lookup("#what-if-name")
                                                                            != null)
                                            .findFirst()
                                            .orElseThrow()
                                            .getScene()
                                            .getRoot();
                    ((TextField) pane.lookup("#what-if-name")).setText(name);
                    ((Button) pane.lookupButton(ButtonType.OK)).fire();
                    return null;
                });
        idle();
    }

    private void course(String name) throws Exception {
        fx(
                () -> {
                    var list = (ListView<ActualCourse>) node("grade-courses");
                    list.getSelectionModel()
                            .select(
                                    list.getItems().stream()
                                            .filter(c -> c.course().name().equals(name))
                                            .findFirst()
                                            .orElseThrow());
                    return null;
                });
        idle();
    }

    private void scenarioCourse(String name) throws Exception {
        fx(
                () -> {
                    var box = (ComboBox<ScenarioCourse>) node("scenario-course");
                    box.setValue(
                            box.getItems().stream()
                                    .filter(c -> c.basis().course().name().equals(name))
                                    .findFirst()
                                    .orElseThrow());
                    return null;
                });
        idle();
    }

    private void selectScenario(String name) throws Exception {
        fx(
                () -> {
                    var list = (ListView<SemesterScenario>) node("saved-scenarios");
                    list.getSelectionModel()
                            .select(
                                    list.getItems().stream()
                                            .filter(c -> c.name().equals(name))
                                            .findFirst()
                                            .orElseThrow());
                    return null;
                });
        idle();
    }

    private void reveal(String id) throws Exception {
        fx(
                () -> {
                    var root = stage.getScene().getRoot();
                    root.applyCss();
                    root.layout();
                    javafx.scene.Node n = node(id), parent = n.getParent();
                    while (parent != null && !(parent instanceof ScrollPane))
                        parent = parent.getParent();
                    if (parent instanceof ScrollPane p) {
                        var bounds =
                                p.getContent().sceneToLocal(n.localToScene(n.getLayoutBounds()));
                        double range =
                                p.getContent().getLayoutBounds().getHeight()
                                        - p.getViewportBounds().getHeight();
                        p.setVvalue(
                                range <= 0 ? 0 : Math.clamp((bounds.getMinY() - 8) / range, 0, 1));
                        root.layout();
                    }
                    return null;
                });
    }

    private void capture(String name) throws Exception {
        screenshot("phase2-" + name + "-" + suffix + ".png");
    }

    private void readableComparison() throws Exception {
        fx(
                () -> {
                    var table = (TableView<?>) node("scenario-comparison-table");
                    table.applyCss();
                    table.layout();
                    int checked = 0;
                    for (var node : table.lookupAll(".table-cell")) {
                        if (node instanceof TableCell<?, ?> cell
                                && !cell.isEmpty()
                                && cell.getGraphic() instanceof Label text) {
                            assertTrue(text.isWrapText());
                            assertTrue(
                                    text.getHeight() + 1 >= text.prefHeight(text.getWidth()),
                                    "Hasil perbandingan terpotong: " + text.getText());
                            checked++;
                        }
                    }
                    assertTrue(checked > 0);
                    return null;
                });
    }

    private void trigger(SqliteDatabase database, String sql) {
        database.read(
                c -> {
                    try (var s = c.createStatement()) {
                        s.execute(sql);
                    }
                    return null;
                });
    }

    @Test
    @Timeout(value = 240, unit = TimeUnit.SECONDS)
    void saveReopenModifyCopyRefreshCompareAndInterpretWholeSemester() throws Exception {
        Path file = directory.resolve("journey.db");
        long semester, course;
        try (var db = new SqliteDatabase(file)) {
            var repo = new SqliteRepository(db);
            repo.createAccount(
                    "001",
                    "Rencana Semester",
                    new PasswordHasher().hash("sandi-uji-lokal".toCharArray()));
            var session = new UserSession();
            session.login("001");
            var academic = new AcademicService(repo, session);
            semester = academic.saveSemesterForm(0, "3", "2026", true).id();
            course = academic.saveCourseMetadata(0, semester, "PBO", "", "3").id();
            long other = academic.saveCourseMetadata(0, semester, "Proyek lain", "", "3").id(),
                    direct = academic.saveCourseMetadata(0, semester, "Huruf", "", "2").id();
            var service = new GradePlanningService(new SqliteGradeRepository(db), session);
            service.saveComponent(course, "tugas", "Tugas", "20", "", "10", 0);
            service.saveComponent(course, "proyek", "Proyek", "30", "30", "100", 1);
            service.saveComponent(course, "akhir", "Penilaian akhir", "50", "", "100", 2);
            service.saveComponent(other, "other", "Proyek akhir", "100", "", "100", 0);
            service.saveActual(
                    direct,
                    EvaluationMode.DIRECT,
                    Optional.of(LetterGrade.B),
                    new Attendance(true, true));
        }
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
            var service = new GradePlanningService(new SqliteGradeRepository(inspection), session);
            var before = service.courses(semester);
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
            tab("grade-tabs", 1);
            capture("empty");
            tab("grade-tabs", 0);
            course("PBO");
            press("open-what-if");
            await(() -> stage.getScene().lookup("#what-if-pane") != null);
            press("what-if-lock-tugas");
            text("what-if-score-tugas", "80");
            required("akhir", "70");
            press("what-if-lock-proyek");
            text("what-if-score-proyek", "60");
            required("akhir", "52");
            assertTrue(label("what-if-state").contains("Sementara"));
            capture("temporary");
            if (System.getProperty("glass.win.uiScale") == null) {
                fx(
                        () -> {
                            stage.setWidth(1024);
                            return null;
                        });
                await(
                        () ->
                                stage.getScene().getWidth() < 1024
                                        && stage.getScene().getWidth() > 960);
                capture("temporary-1024");
                fx(
                        () -> {
                            stage.setWidth(1366);
                            return null;
                        });
                await(() -> stage.getScene().getWidth() > 1300);
            }
            nameDialog(
                    () -> {
                        ((Button) node("what-if-save")).fire();
                        ((Button) node("what-if-save")).fire();
                    },
                    "Rencana A");
            await(
                    () ->
                            ((Label) node("what-if-state"))
                                    .getText()
                                    .contains("Tersimpan — Rencana A"));
            required("akhir", "52");
            assertEquals(1, service.scenarios(semester).size());
            var a = service.scenarios(semester).getFirst();
            assertEquals(
                    Map.of("proyek", TestData.n("60")),
                    a.courses().stream()
                            .filter(c -> c.basis().course().id() == course)
                            .findFirst()
                            .orElseThrow()
                            .overriddenScores());
            assertEquals(before, service.courses(semester));
            capture("saved");
            text("what-if-score-proyek", "75");
            required("akhir", "43");
            assertTrue(label("what-if-state").contains("Perubahan belum disimpan"));
            capture("modified");
            dialog(() -> ((Button) node("what-if-back")).fire(), ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals("75", fx(() -> ((TextField) node("what-if-score-proyek")).getText()));
            trigger(
                    inspection,
                    "CREATE TRIGGER fail_journey BEFORE INSERT ON scenario_items BEGIN SELECT"
                            + " RAISE(ABORT,'failure'); END");
            dialog(() -> ((Button) node("what-if-back")).fire(), ButtonBar.ButtonData.YES);
            assertTrue(label("what-if-status").contains("Gagal menyimpan"));
            assertNotNull(fx(() -> node("what-if-pane")));
            assertEquals(a, service.open(a.id()));
            assertEquals("75", fx(() -> ((TextField) node("what-if-score-proyek")).getText()));
            capture("save-error");
            trigger(inspection, "DROP TRIGGER fail_journey");
            press("what-if-save");
            await(
                    () ->
                            ((Label) node("what-if-state"))
                                    .getText()
                                    .contains("Tersimpan — Rencana A"));
            a = service.open(a.id());
            text("what-if-target", "84.5");
            required("akhir", "92");
            nameDialog(() -> ((Button) node("what-if-copy")).fire(), "Rencana B");
            await(
                    () ->
                            ((Label) node("what-if-state"))
                                    .getText()
                                    .contains("Tersimpan — Rencana B"));
            required("akhir", "92");
            var b =
                    service.scenarios(semester).stream()
                            .filter(c -> c.name().equals("Rencana B"))
                            .findFirst()
                            .orElseThrow();
            assertEquals(a, service.open(a.id()));
            assertEquals(before, service.courses(semester));
            text("what-if-target", "80");
            dialog(() -> ((Button) node("what-if-reset")).fire(), ButtonBar.ButtonData.NO);
            required("akhir", "92");
            assertEquals("84,5", fx(() -> ((TextField) node("what-if-target")).getText()));
            press("what-if-saved");
            await(() -> stage.getScene().lookup("#scenario-gpa-result") != null);
            await(
                    () ->
                            ((Label) node("scenario-gpa-result"))
                                    .getText()
                                    .contains("IP belum dapat dihitung lengkap"));
            assertTrue(label("scenario-gpa-result").contains("Proyek lain"));
            reveal("scenario-gpa-result");
            capture("ip-incomplete");
            press("edit-scenario-what-if");
            await(() -> stage.getScene().lookup("#what-if-pane") != null);
            required("akhir", "92");
            service.saveComponent(course, "proyek", "Proyek", "30", "40", "100", 1);
            press("what-if-reset");
            await(() -> ((Label) node("what-if-state")).getText().contains("BASIS USANG"));
            assertTrue(label("what-if-original-proyek").contains("30/100"));
            capture("stale");
            dialog(() -> ((Button) node("what-if-review")).fire(), ButtonBar.ButtonData.OK_DONE);
            await(() -> stage.getScene().lookup("#scenario-context") != null);
            assertTrue(label("scenario-context").contains("Perubahan belum disimpan"));
            assertEquals(b, service.open(b.id()));
            press("edit-scenario-what-if");
            await(() -> stage.getScene().lookup("#what-if-pane") != null);
            assertTrue(label("what-if-original-proyek").contains("40/100"));
            dialog(() -> ((Button) node("what-if-reset")).fire(), ButtonBar.ButtonData.NO);
            await(() -> ((Label) node("what-if-state")).getText().contains("BASIS USANG"));
            assertTrue(label("what-if-original-proyek").contains("30/100"));
            dialog(() -> ((Button) node("what-if-review")).fire(), ButtonBar.ButtonData.OK_DONE);
            press("save-scenario");
            assertFalse(service.review(service.open(b.id())).outdated());
            scenarioCourse("Proyek lain");
            press("scenario-projection");
            combo("scenario-target-kind", "Angka");
            text("scenario-score-target", "85");
            press("calculate-scenario");
            await(() -> ((Label) node("scenario-gpa-result")).getText().contains("3,64"));
            press("save-scenario");
            b = service.open(b.id());
            fx(
                    () -> {
                        ((TitledPane) node("scenario-ip-sources")).setExpanded(true);
                        return null;
                    });
            await(() -> stage.getScene().lookup("#ip-source-" + course) != null);
            assertTrue(label("ip-source-" + course).contains("Proyeksi skenario"));
            reveal("scenario-gpa-result");
            capture("ip-complete");
            combo("compare-scenario", service.open(a.id()));
            press("compare-scenarios");
            await(() -> stage.getScene().lookup("#scenario-comparison-table") != null);
            assertTrue(label("comparison-context").contains("Rencana A"));
            assertTrue(label("comparison-context").contains("Rencana B"));
            reveal("comparison-context");
            capture("comparison");
            readableComparison();
            if (System.getProperty("glass.win.uiScale") == null) {
                fx(
                        () -> {
                            stage.setWidth(1024);
                            return null;
                        });
                await(
                        () ->
                                stage.getScene().getWidth() < 1024
                                        && stage.getScene().getWidth() > 960);
                reveal("comparison-context");
                capture("comparison-1024");
                readableComparison();
                fx(
                        () -> {
                            stage.setWidth(1366);
                            return null;
                        });
                await(() -> stage.getScene().getWidth() > 1300);
            }
            fx(
                    () -> {
                        for (var n : stage.getScene().getRoot().lookupAll(".titled-pane"))
                            if (n instanceof TitledPane p && p.getText().startsWith("PBO —"))
                                p.setExpanded(true);
                        return null;
                    });
            await(() -> stage.getScene().lookup("#comparison-item-proyek") != null);
            String detail = label("comparison-item-proyek");
            assertTrue(detail.contains("30/100"));
            assertTrue(detail.contains("40/100"));
            assertTrue(detail.contains("Override 75"));
            reveal("comparison-item-proyek");
            capture("comparison-components");
            var actualAfterEdit = service.courses(semester);
            var originalB = b;
            press("duplicate-scenario");
            assertEquals(originalB, service.open(originalB.id()));
            assertTrue(label("scenario-context").contains("salinan"));
            var duplicate =
                    service.scenarios(semester).stream()
                            .filter(c -> c.name().contains("salinan"))
                            .findFirst()
                            .orElseThrow();
            assertNotEquals(originalB.id(), duplicate.id());
            press("logout");
            login();
            tab("grade-tabs", 1);
            selectScenario("Rencana B");
            assertEquals(originalB, service.open(originalB.id()));
            assertEquals(actualAfterEdit, service.courses(semester));
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
            tab("grade-tabs", 1);
            selectScenario("Rencana B");
            assertEquals(originalB, service.open(originalB.id()));
            assertEquals(actualAfterEdit, service.courses(semester));
        } catch (Throwable error) {
            screenshot("phase2-failure.png");
            throw error;
        } finally {
            fx(
                    () -> {
                        if (stage != null) stage.hide();
                        if (application != null) application.stop();
                        return null;
                    });
            Platform.exit();
        }
    }
}
