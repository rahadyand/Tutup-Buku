package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/** Alur UI produksi, tanpa database AppData atau input pengguna sungguhan. */
class WhatIfFxCheck extends FxTestSupport {
    private void required(String id, String value) throws Exception {
        await(
                () ->
                        ((Label) node("what-if-required-" + id))
                                .getText()
                                .contains("Diperlukan: " + value + "/100"));
    }

    private void open() throws Exception {
        click("open-what-if");
        await(() -> stage.getScene().lookup("#what-if-pane") != null);
    }

    private void back() throws Exception {
        dialog(() -> ((Button) node("what-if-back")).fire(), ButtonBar.ButtonData.NO);
        await(() -> stage.getScene().lookup("#open-what-if") != null);
    }

    private void choose(String name) throws Exception {
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

    private void trigger(SqliteDatabase database, String sql) {
        database.read(
                connection -> {
                    try (var statement = connection.createStatement()) {
                        statement.execute(sql);
                    }
                    return null;
                });
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    void reactiveTargetsOverridesGuardsEmptyAndSavedScenarios() throws Exception {
        Path file = directory.resolve("what-if.db");
        long semester, course, savedId;
        try (var db = new SqliteDatabase(file)) {
            var repo = new SqliteRepository(db);
            repo.createAccount(
                    "001",
                    "Mahasiswa Uji",
                    new PasswordHasher().hash("sandi-uji-lokal".toCharArray()));
            var session = new UserSession();
            session.login("001");
            var academic = new AcademicService(repo, session);
            semester = academic.saveSemesterForm(0, "3", "2026", true).id();
            academic.saveSemesterForm(0, "4", "2027", false);
            course = academic.saveCourseMetadata(0, semester, "PBO", "PBO", "3").id();
            academic.saveCourseMetadata(0, semester, "Kosong", "", "3");
            long direct = academic.saveCourseMetadata(0, semester, "Huruf langsung", "", "2").id();
            long absent = academic.saveCourseMetadata(0, semester, "Kehadiran gagal", "", "3").id();
            var service = new GradePlanningService(new SqliteGradeRepository(db), session);
            for (long id : new long[] {course, absent}) {
                service.saveComponent(id, "tugas-" + id, "Tugas", "20", "", "10", 0);
                service.saveComponent(id, "uts-" + id, "UTS", "30", "", "100", 1);
                service.saveComponent(id, "uas-" + id, "UAS", "50", "", "100", 2);
            }
            service.saveActual(
                    direct,
                    EvaluationMode.DIRECT,
                    Optional.of(LetterGrade.B),
                    Attendance.assumedMet());
            service.saveActual(
                    absent, EvaluationMode.DETAIL, Optional.empty(), new Attendance(false, true));
            savedId =
                    service.save(service.create(semester, "Rencana terlindungi", TestData.n("3")))
                            .id();
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
            var stored = service.open(savedId);
            fx(
                    () -> {
                        stage = new Stage();
                        application = new TutupBukuApplication(file);
                        application.start(stage);
                        return null;
                    });
            login();
            choose("PBO");
            open();
            String tasks = "tugas-" + course, uts = "uts-" + course, uas = "uas-" + course;
            required(tasks, "60");
            required(uts, "60");
            required(uas, "60");
            assertTrue(label("what-if-context").contains("PBO · 3 SKS"));
            assertTrue(label("what-if-actual").contains("Belum lengkap"));
            assertEquals(1, service.scenarios(semester).size());
            combo("what-if-letter", LetterGrade.A);
            required(tasks, "85");
            assertEquals("85", fx(() -> ((TextField) node("what-if-target")).getText()));
            combo("what-if-letter", LetterGrade.C);
            required(tasks, "60");
            assertEquals("60", fx(() -> ((TextField) node("what-if-target")).getText()));
            text("what-if-target", "84.5");
            required(tasks, "84,5");
            await(() -> ((Label) node("what-if-projection")).getText().contains("84,5 / A-"));
            assertEquals("84.5", fx(() -> ((TextField) node("what-if-target")).getText()));
            assertEquals(
                    LetterGrade.A_MINUS,
                    fx(() -> ((ComboBox<?>) node("what-if-letter")).getValue()));
            fx(
                    () -> {
                        for (String value : List.of("70", "85", "55", "84.5", "60"))
                            ((TextField) node("what-if-target")).setText(value);
                        return null;
                    });
            required(tasks, "60");
            required(uts, "60");
            required(uas, "60");
            await(() -> ((Label) node("what-if-projection")).getText().contains("60 / C"));
            click("what-if-lock-" + tasks);
            text("what-if-score-" + tasks, "80");
            required(uts, "55");
            required(uas, "55");
            int[][] sizes =
                    System.getProperty("glass.win.uiScale") == null
                            ? new int[][] {{1366, 768}, {1024, 768}, {760, 480}}
                            : new int[][] {{1366, 768}};
            for (int[] size : sizes) {
                fx(
                        () -> {
                            stage.setWidth(size[0] / stage.getOutputScaleX());
                            stage.setHeight(size[1] / stage.getOutputScaleY());
                            return null;
                        });
                await(
                        () ->
                                stage.getScene().getWidth() * stage.getOutputScaleX() < size[0]
                                        && stage.getScene().getWidth() * stage.getOutputScaleX()
                                                >= size[0] - 64
                                        && stage.getScene().getHeight() * stage.getOutputScaleY()
                                                < size[1]
                                        && stage.getScene().getHeight() * stage.getOutputScaleY()
                                                >= size[1] - 96);
                screenshot(
                        "what-if-plan-"
                                + System.getProperty("glass.win.uiScale", "native")
                                + "-"
                                + size[0]
                                + "x"
                                + size[1]
                                + ".png");
                fx(
                        () -> {
                            for (String id :
                                    List.of(
                                            "what-if-back",
                                            "what-if-reset",
                                            "what-if-target",
                                            "what-if-saved")) {
                                var bounds = node(id).localToScene(node(id).getLayoutBounds());
                                assertTrue(
                                        bounds.getMinY() >= 0
                                                && bounds.getMaxY()
                                                        <= stage.getScene().getHeight() + 1,
                                        id);
                                assertTrue(
                                        bounds.getMinX() >= 0
                                                && bounds.getMaxX()
                                                        <= stage.getScene().getWidth() + 1,
                                        id);
                            }
                            assertTrue(((TextField) node("what-if-target")).getWidth() <= 110);
                            return null;
                        });
            }
            fx(
                    () -> {
                        stage.setWidth(1366 / stage.getOutputScaleX());
                        stage.setHeight(768 / stage.getOutputScaleY());
                        return null;
                    });
            dialog(() -> ((Button) node("what-if-back")).fire(), ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals("80", fx(() -> ((TextField) node("what-if-score-" + tasks)).getText()));
            dialog(() -> ((Button) node("logout")).fire(), ButtonBar.ButtonData.CANCEL_CLOSE);
            assertNotNull(fx(() -> node("what-if-pane")));
            dialog(
                    () ->
                            javafx.event.Event.fireEvent(
                                    stage,
                                    new javafx.stage.WindowEvent(
                                            stage, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST)),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertTrue(fx(stage::isShowing));
            assertEquals("80", fx(() -> ((TextField) node("what-if-score-" + tasks)).getText()));
            dialog(
                    () -> ((TabPane) node("grade-tabs")).getSelectionModel().select(1),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(
                    0,
                    fx(
                            () ->
                                    ((TabPane) node("grade-tabs"))
                                            .getSelectionModel()
                                            .getSelectedIndex()));
            dialog(
                    () -> ((ComboBox<?>) node("grade-semester")).getSelectionModel().selectLast(),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(
                    semester,
                    fx(() -> ((Semester) ((ComboBox<?>) node("grade-semester")).getValue()).id()));
            back();
            assertEquals(stored, service.open(savedId));
            assertTrue(
                    service.courses(semester).stream()
                            .filter(c -> c.course().id() == course)
                            .findFirst()
                            .orElseThrow()
                            .components()
                            .stream()
                            .allMatch(c -> c.earned().isEmpty()));
            select("assessment-components", 1);
            text("component-earned", "30");
            text("component-maximum", "100");
            click("save-component");
            ActualCourse baseline =
                    service.courses(semester).stream()
                            .filter(c -> c.course().id() == course)
                            .findFirst()
                            .orElseThrow();
            open();
            click("what-if-lock-" + tasks);
            text("what-if-score-" + tasks, "80");
            required(uas, "70");
            click("what-if-lock-" + uts);
            text("what-if-score-" + uts, "60");
            required(uas, "52");
            assertTrue(label("what-if-original-" + uts).contains("30/100"));
            assertEquals(
                    baseline,
                    service.courses(semester).stream()
                            .filter(c -> c.course().id() == course)
                            .findFirst()
                            .orElseThrow());
            click("what-if-restore-" + uts);
            required(uas, "70");
            assertEquals("30", fx(() -> ((TextField) node("what-if-score-" + uts)).getText()));
            text("what-if-score-" + tasks, "");
            required(tasks, "72,86");
            required(uas, "72,86");
            text("what-if-target", "101");
            assertFalse(label("what-if-target-error").isBlank());
            assertEquals("101", fx(() -> ((TextField) node("what-if-target")).getText()));
            assertEquals("—", label("what-if-required-" + uas));
            text("what-if-target", "84.5");
            await(
                    () ->
                            ((Label) node("what-if-required-" + uas))
                                    .getText()
                                    .contains("mustahil (>100)"));
            text("what-if-target", "60");
            required(uas, "72,86");
            text("what-if-score-" + tasks, "abc");
            assertFalse(label("what-if-error-" + tasks).isBlank());
            assertEquals("—", label("what-if-required-" + uas));
            text("what-if-score-" + tasks, "80");
            required(uas, "70");
            dialog(
                    () -> ((Button) node("what-if-reset")).fire(),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals("80", fx(() -> ((TextField) node("what-if-score-" + tasks)).getText()));
            dialog(() -> ((Button) node("what-if-reset")).fire(), ButtonBar.ButtonData.NO);
            required(uas, "72,86");
            assertFalse(fx(() -> ((CheckBox) node("what-if-lock-" + tasks)).isSelected()));
            click("what-if-back");
            select("assessment-components", 1);
            text("component-earned", "40");
            trigger(
                    inspection,
                    "CREATE TRIGGER fail_what_if_entry BEFORE UPDATE ON assessment_components BEGIN"
                        + " SELECT RAISE(ABORT,'failure'); END");
            dialog(() -> ((Button) node("open-what-if")).fire(), ButtonBar.ButtonData.YES);
            assertNull(fx(() -> stage.getScene().lookup("#what-if-pane")));
            assertEquals("40", fx(() -> ((TextField) node("component-earned")).getText()));
            assertEquals(
                    baseline,
                    service.courses(semester).stream()
                            .filter(c -> c.course().id() == course)
                            .findFirst()
                            .orElseThrow());
            trigger(inspection, "DROP TRIGGER fail_what_if_entry");
            dialog(() -> ((Button) node("open-what-if")).fire(), ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals("40", fx(() -> ((TextField) node("component-earned")).getText()));
            dialog(() -> ((Button) node("open-what-if")).fire(), ButtonBar.ButtonData.NO);
            await(() -> stage.getScene().lookup("#what-if-pane") != null);
            required(uas, "72,86");
            click("what-if-saved");
            await(() -> stage.getScene().lookup("#saved-scenarios") != null);
            select("saved-scenarios", 0);
            await(() -> ((Label) node("scenario-review")).getText().contains("USANG"));
            assertEquals(stored, service.open(savedId));
            tab("grade-tabs", 0);
            choose("Kosong");
            open();
            await(() -> ((Label) node("what-if-explanation")).getText().contains("Draf belum sah"));
            assertFalse(label("what-if-projection").contains("100 / A"));
            assertTrue(label("what-if-actual").contains("Belum lengkap"));
            screenshot(
                    "what-if-empty-" + System.getProperty("glass.win.uiScale", "native") + ".png");
            click("what-if-back");
            choose("Huruf langsung");
            open();
            assertTrue(fx(() -> node("what-if-target").isDisabled()));
            assertTrue(label("what-if-projection").contains("Mode huruf langsung"));
            click("what-if-back");
            choose("Kehadiran gagal");
            open();
            await(
                    () ->
                            ((Label) node("what-if-projection"))
                                    .getText()
                                    .contains("60 / C · Efektif: E"));
            assertTrue(label("what-if-explanation").contains("E/0"));
            click("what-if-back");
            assertEquals(stored, service.open(savedId));
        } catch (Throwable error) {
            screenshot("what-if-failure.png");
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
