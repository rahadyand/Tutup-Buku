package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.*;

import org.junit.jupiter.api.Test;

import java.nio.file.*;
import java.util.concurrent.*;

/** Peluncuran aplikasi asli dan interaksi kontrol JavaFX; seluruh data hanya di @TempDir. */
class MilestoneFourFxCheck extends FxTestSupport {
    private final String layoutSuffix =
            System.getProperty("glass.win.uiScale", "native")
                    + "-"
                    + System.getProperty("phase3.width", "1366")
                    + "x"
                    + System.getProperty("phase3.height", "768");

    private void sizeWindow() {
        stage.setWidth(Integer.getInteger("phase3.width", 1366) / stage.getOutputScaleX());
        stage.setHeight(Integer.getInteger("phase3.height", 768) / stage.getOutputScaleY());
    }

    private void statusLayout(String statusId, String inputId, String state) throws Exception {
        for (int position = 0; position <= 1; position++) {
            int scrollPosition = position;
            scroll(inputId, position);
            screenshot("phase3-" + state + "-" + position + "-" + layoutSuffix + ".png");
            fx(
                    () -> {
                        Label message = (Label) node(statusId);
                        Node parent = node(inputId);
                        while (!(parent instanceof ScrollPane)) parent = parent.getParent();
                        ScrollPane scroll = (ScrollPane) parent;
                        var bounds = message.localToScene(message.getLayoutBounds());
                        Region header = (Region) node("grade-semester").getParent().getParent();
                        System.out.printf(
                                java.util.Locale.ROOT,
                                "PHASE3 %s scroll=%d scene=%.0fx%.0f header=%.1f viewport=%.1f"
                                        + " body=%.1f statusY=%.1f statusHeight=%.1f parent=%s%n",
                                state,
                                scrollPosition,
                                stage.getScene().getWidth(),
                                stage.getScene().getHeight(),
                                header.getHeight(),
                                scroll.getViewportBounds().getHeight(),
                                scroll.getContent().getLayoutBounds().getHeight(),
                                bounds.getMinY(),
                                message.getHeight(),
                                message.getParent().getClass().getSimpleName());
                        var pane = assertInstanceOf(BorderPane.class, message.getParent());
                        assertSame(message, pane.getBottom());
                        assertTrue(message.isWrapText());
                        assertTrue(
                                message.getHeight() + 0.1
                                        >= message.prefHeight(message.getWidth()));
                        var paneBounds = pane.localToScene(pane.getLayoutBounds());
                        assertTrue(bounds.getMinY() >= paneBounds.getMinY());
                        assertTrue(bounds.getMaxY() <= paneBounds.getMaxY() + 0.1);
                        assertTrue(bounds.getMaxY() <= stage.getScene().getHeight());
                        assertTrue(scroll.getViewportBounds().getHeight() >= 80);
                        return null;
                    });
        }
        String[] actions =
                statusId.equals("actual-grade-status")
                        ? new String[] {"save-component"}
                        : new String[] {"new-scenario", "duplicate-scenario", "delete-scenario"};
        for (String id : actions) {
            fx(
                    () -> {
                        Node button = node(id), parent = button.getParent();
                        while (!(parent instanceof ScrollPane)) parent = parent.getParent();
                        ScrollPane page = (ScrollPane) parent;
                        var target =
                                page.getContent()
                                        .sceneToLocal(
                                                button.localToScene(button.getLayoutBounds()));
                        double viewport = page.getViewportBounds().getHeight();
                        double range = page.getContent().getLayoutBounds().getHeight() - viewport;
                        double offset = target.getMinY() - (viewport - target.getHeight()) / 2;
                        page.setVvalue(range <= 0 ? 0 : Math.max(0, Math.min(1, offset / range)));
                        return null;
                    });
            screenshot("phase3-" + state + "-" + id + "-" + layoutSuffix + ".png");
            fx(
                    () -> {
                        Node button = node(id), parent = button.getParent();
                        while (!(parent instanceof ScrollPane)) parent = parent.getParent();
                        Node viewport = parent.lookup(".viewport");
                        var bounds = button.localToScene(button.getLayoutBounds());
                        var visible = viewport.localToScene(viewport.getLayoutBounds());
                        assertTrue(bounds.getMinY() >= visible.getMinY(), id);
                        assertTrue(bounds.getMaxY() <= visible.getMaxY() + 0.1, id);
                        assertFalse(button.isDisabled(), id);
                        return null;
                    });
        }
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
                        sizeWindow();
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
                statusLayout("actual-grade-status", "component-earned", "actual-error");
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
                statusLayout("scenario-status", "scenario-name", "scenario-error");
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
                        sizeWindow();
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
            String longName =
                    "Rencana semester dengan target akademik dan asumsi yang perlu ditinjau"
                            + " bersama";
            text("scenario-name", longName);
            click("save-scenario");
            fx(
                    () -> {
                        var box = (ComboBox<SemesterScenario>) node("compare-scenario");
                        box.setValue(
                                box.getItems().stream()
                                        .filter(s -> !s.name().equals(longName))
                                        .findFirst()
                                        .orElseThrow());
                        return null;
                    });
            click("compare-scenarios");
            scroll("scenario-comparison-table", 1);
            screenshot("phase3-long-comparison-" + layoutSuffix + ".png");
            fx(
                    () -> {
                        var table = (TableView<?>) node("scenario-comparison-table");
                        assertNotNull(table.getTooltip());
                        assertTrue(table.getTooltip().getText().contains(longName));
                        assertEquals(
                                String.join(
                                        "\n",
                                        table.getColumns().stream()
                                                .map(TableColumn::getText)
                                                .toList()),
                                table.getTooltip().getText());
                        for (Node item : table.lookupAll(".column-header .label")) {
                            if (item instanceof Label label
                                    && label.getText().startsWith(longName)) {
                                var rendered = (javafx.scene.text.Text) label.lookup(".text");
                                System.out.println(
                                        "PHASE3 long header rendered=" + rendered.getText());
                            }
                        }
                        return null;
                    });
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
