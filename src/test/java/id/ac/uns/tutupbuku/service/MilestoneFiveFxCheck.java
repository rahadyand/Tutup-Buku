package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.ui.*;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.stage.*;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;

import javax.imageio.ImageIO;

class MilestoneFiveFxCheck extends FxTestSupport {
    void chooseCourse(String name) throws Exception {
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

    void createSemester(String number, String year) throws Exception {
        click("new-semester");
        text("semester-number", number);
        text("semester-year", year.substring(0, 4));
        click("save-semester");
    }

    void createCourse(String name, String credits) throws Exception {
        click("new-course");
        text("course-name", name);
        text("course-credits", credits);
        click("save-course");
    }

    void readyReader() throws Exception {
        await(() -> ((MarkdownReader) node("material-reader").getParent()).ready());
    }

    @Test
    void connectedJourneysUnsavedGuardsActiveSemesterImagesAndLayouts() throws Exception {
        Path file = directory.resolve("connected.db");
        var started = new CompletableFuture<Void>();
        Platform.startup(
                () -> {
                    Platform.setImplicitExit(false);
                    started.complete(null);
                });
        started.get(10, TimeUnit.SECONDS);
        String suffix = System.getProperty("glass.win.uiScale", "native");
        try {
            fx(
                    () -> {
                        stage = new Stage();
                        application = new TutupBukuApplication(file);
                        application.start(stage);
                        stage.setWidth(1366 / stage.getOutputScaleX());
                        stage.setHeight(768 / stage.getOutputScaleY());
                        return null;
                    });
            idle();
            tab("auth-tabs", 1);
            text("register-nim", "001");
            text("register-name", "Demo Terhubung");
            text("register-password", "sandi-uji-lokal");
            text("register-confirmation", "sandi-uji-lokal");
            click("register");
            login();
            tab("workspace-tabs", 1);
            createSemester("3", "2026/2027");
            click("edit-semesters");
            click("set-active-semester");
            createCourse("PBO", "3");
            createCourse("Algoritma", "3");
            createCourse("Basis Data", "3");
            createCourse("Bahasa Inggris", "2");
            text("course-name", "Edit yang belum disimpan");
            dialog(
                    () -> ((TabPane) node("workspace-tabs")).getSelectionModel().select(2),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(
                    "Edit yang belum disimpan",
                    fx(() -> ((TextField) node("course-name")).getText()));
            dialog(
                    () -> ((TabPane) node("workspace-tabs")).getSelectionModel().select(2),
                    ButtonBar.ButtonData.NO);
            fx(
                    () -> {
                        ((ComboBox<String>) node("profile-program")).setValue("Informatika");
                        return null;
                    });
            dialog(() -> ((Button) node("logout")).fire(), ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(
                    "Informatika", fx(() -> ((ComboBox<?>) node("profile-program")).getValue()));
            click("save-profile");
            tab("workspace-tabs", 1);
            createSemester("4", "2026/2027");
            // Revision001: semester4/year2026 is distinct from semester3, without a period field.
            tab("workspace-tabs", 0);
            click("open-learning");
            assertEquals(
                    3,
                    fx(
                            () ->
                                    ((Semester)
                                                    ((ComboBox<?>) node("learning-semester"))
                                                            .getValue())
                                            .number()));
            fx(
                    () -> {
                        var box = (ComboBox<Course>) node("learning-course");
                        box.setValue(
                                box.getItems().stream()
                                        .filter(c -> c.name().equals("PBO"))
                                        .findFirst()
                                        .orElseThrow());
                        return null;
                    });
            idle();
            text("learning-folder-name", "Bab Integrasi");
            click("learning-add-folder");
            click("learning-new-material");
            text("material-title", "Materi terhubung");
            String reference;
            try (var db = new SqliteDatabase(file)) {
                var session = new UserSession();
                session.login("001");
                var learning =
                        new LearningService(
                                new SqliteLearningRepository(db),
                                session,
                                new AssetStore(directory));
                Path original = directory.resolve("gambar-asli.png");
                ImageIO.write(
                        new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB),
                        "png",
                        original.toFile());
                reference = learning.importImage(original);
                Files.move(original, directory.resolve("gambar-dipindah.png"));
                var grades = new GradePlanningService(new SqliteGradeRepository(db), session);
                long semester = new SqliteRepository(db).listSemesters("001").getFirst().id();
                for (ActualCourse c : grades.courses(semester))
                    if (!c.course().name().equals("PBO"))
                        grades.saveActual(
                                c.course().id(),
                                EvaluationMode.DIRECT,
                                java.util.Optional.of(
                                        c.course().name().equals("Bahasa Inggris")
                                                ? LetterGrade.B
                                                : LetterGrade.A),
                                Attendance.assumedMet());
            }
            text(
                    "material-markdown",
                    "# Belajar\n\n"
                            + "| A | B |\n"
                            + "| --- | --- |\n"
                            + "| 1 | 2 |\n\n"
                            + "```java\n"
                            + "class Course {}\n"
                            + "```\n\n"
                            + "![Gambar]("
                            + reference
                            + ")\n\n"
                            + "Paragraf bacaan.\n\n".repeat(70));
            text("material-note", "Catatan integrasi");
            click("save-material");
            click("material-completed");
            tab("material-tabs", 1);
            readyReader();
            assertTrue(
                    fx(
                            () ->
                                    (Boolean)
                                            ((WebView) node("material-reader"))
                                                    .getEngine()
                                                    .executeScript(
                                                            "document.querySelector('img').complete"
                                                                + " && document.querySelector('img').naturalWidth"
                                                                + " === 40")));
            screenshot("m5-learning-" + suffix + ".png");
            fx(
                    () -> {
                        ((WebView) node("material-reader"))
                                .getEngine()
                                .executeScript(
                                        "window.scrollTo(0,(document.documentElement.scrollHeight-window.innerHeight)*0.6)");
                        return null;
                    });
            click("open-simulation");
            assertEquals(
                    3,
                    fx(
                            () ->
                                    ((Semester) ((ComboBox<?>) node("grade-semester")).getValue())
                                            .number()));
            chooseCourse("PBO");
            for (String[] item : new String[][] {{"Tugas", "20"}, {"UTS", "30"}, {"UAS", "50"}}) {
                click("new-component");
                text("component-name", item[0]);
                text("component-weight", item[1]);
                text("component-maximum", "100");
                click("save-component");
            }
            tab("grade-tabs", 1);
            click("new-scenario");
            text("scenario-name", "Terhubung");
            fx(
                    () -> {
                        var box = (ComboBox<ScenarioCourse>) node("scenario-course");
                        box.setValue(
                                box.getItems().stream()
                                        .filter(c -> c.basis().course().name().equals("PBO"))
                                        .findFirst()
                                        .orElseThrow());
                        return null;
                    });
            idle();
            click("calculate-scenario");
            assertTrue(label("scenario-gpa-result").contains("3,27"));
            click("save-scenario");
            combo("scenario-mode", EvaluationMode.DIRECT);
            combo("scenario-estimate", LetterGrade.A);
            assertTrue(label("scenario-status").contains("Belum disimpan"));
            click("calculate-scenario");
            assertTrue(label("scenario-course-result").contains("Lulus"));
            click("save-scenario");
            screenshot("m5-grades-" + suffix + ".png");
            tab("workspace-tabs", 1);
            fx(
                    () -> {
                        var list = (ListView<Semester>) node("semester-list");
                        list.getSelectionModel().select(0);
                        return null;
                    });
            idle();
            fx(
                    () -> {
                        var list = (ListView<Course>) node("course-list");
                        list.getSelectionModel()
                                .select(
                                        list.getItems().stream()
                                                .filter(c -> c.name().equals("PBO"))
                                                .findFirst()
                                                .orElseThrow());
                        return null;
                    });
            idle();
            text("course-credits", "4");
            click("save-course");
            screenshot("m5-academic-" + suffix + ".png");
            dialog(
                    () -> ((Button) node("delete-course")).fire(),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            tab("workspace-tabs", 0);
            click("open-simulation");
            tab("grade-tabs", 1);
            select("saved-scenarios", 0);
            assertTrue(label("scenario-review").contains("SKS"));
            dialog(() -> ((Button) node("refresh-scenario")).fire(), ButtonBar.ButtonData.OK_DONE);
            click("save-scenario");
            click("logout");
            tab("auth-tabs", 1);
            text("register-nim", "002");
            text("register-name", "Akun Dua");
            text("register-password", "sandi-uji-lokal");
            text("register-confirmation", "sandi-uji-lokal");
            click("register");
            text("login-nim", "002");
            text("login-password", "sandi-uji-lokal");
            click("login");
            click("open-simulation");
            assertEquals(0, fx(() -> ((ComboBox<?>) node("grade-semester")).getItems().size()));
            click("logout");
            fx(
                    () -> {
                        stage.close();
                        application.stop();
                        stage = new Stage();
                        application = new TutupBukuApplication(file);
                        application.start(stage);
                        return null;
                    });
            idle();
            login();
            click("open-learning");
            click("resume-reading");
            readyReader();
            assertEquals(
                    "Catatan integrasi", fx(() -> ((TextArea) node("material-note")).getText()));
            assertTrue(fx(() -> ((CheckBox) node("material-completed")).isSelected()));
            assertEquals(
                    0.6,
                    fx(() -> ((MarkdownReader) node("material-reader").getParent()).scrollRatio()),
                    0.04);
            Path out = Path.of("target/verification");
            Files.createDirectories(out);
            Files.writeString(
                    out.resolve("layout-" + suffix + ".txt"),
                    fx(
                            () ->
                                    "outputScale="
                                            + stage.getOutputScaleX()
                                            + "; screen="
                                            + Screen.getPrimary().getBounds()
                                            + "; tested outer-window physical target1366x768;"
                                            + " global Windows settings unchanged."));
        } catch (Throwable error) {
            screenshot("m5-failure-" + suffix + ".png");
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
