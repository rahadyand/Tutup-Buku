package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Satu perjalanan aplikasi produksi pada TempDir; Clock boundary diuji terpisah tanpa UI. */
class AgendaFxCheck extends FxTestSupport {
    private long semester, archive, first, second, oldCourse, foreignSemester, foreignCourse;
    private final String suffix = System.getProperty("glass.win.uiScale", "native");

    private void press(String id) throws Exception {
        await(() -> stage.getScene().lookup("#" + id) != null);
        click(id);
    }

    private void present(String id) throws Exception {
        await(() -> stage.getScene().lookup("#" + id) != null);
    }

    private void shot(String name) throws Exception {
        var painted = new CompletableFuture<Void>();
        fx(
                () -> {
                    new javafx.animation.AnimationTimer() {
                        int n;

                        public void handle(long now) {
                            if (++n == 3) {
                                stop();
                                painted.complete(null);
                            }
                        }
                    }.start();
                    return null;
                });
        painted.get(5, TimeUnit.SECONDS);
        screenshot("agenda-" + name + "-" + suffix + ".png");
    }

    private Object choice(String prefix) {
        return ((ComboBox<?>) node("agenda-course-filter"))
                .getItems().stream()
                        .filter(c -> c.toString().startsWith(prefix))
                        .findFirst()
                        .orElseThrow();
    }

    private void filterCourse(String name) throws Exception {
        fx(
                () -> {
                    ((ComboBox<Object>) node("agenda-course-filter")).setValue(choice(name));
                    return null;
                });
        idle();
    }

    private void editorCourse(long id) throws Exception {
        fx(
                () -> {
                    var box = (ComboBox<Course>) node("agenda-editor-course");
                    box.setValue(
                            box.getItems().stream()
                                    .filter(c -> c.id() == id)
                                    .findFirst()
                                    .orElseThrow());
                    return null;
                });
        idle();
    }

    private Set<String> stepIds() throws Exception {
        return fx(
                () -> {
                    var ids = new HashSet<String>();
                    for (Node n : stage.getScene().getRoot().lookupAll(".text-field"))
                        if (n.getId() != null && n.getId().startsWith("agenda-step-text-"))
                            ids.add(n.getId().substring("agenda-step-text-".length()));
                    return ids;
                });
    }

    private String addStep(String text) throws Exception {
        var before = stepIds();
        press("agenda-add-step");
        var after = stepIds();
        after.removeAll(before);
        String id = after.iterator().next();
        text("agenda-step-text-" + id, text);
        return id;
    }

    private String card(long id) throws Exception {
        return fx(
                () ->
                        ((VBox) node("agenda-item-" + id))
                                .getChildren().stream()
                                        .filter(n -> n instanceof Label)
                                        .map(n -> ((Label) n).getText())
                                        .reduce("", (a, b) -> a + "\n" + b));
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

    private AgendaItem item(
            long course,
            AgendaType type,
            String title,
            LocalDate date,
            String time,
            List<ChecklistStep> steps) {
        return new AgendaItem(
                0,
                course,
                type,
                title,
                date,
                AgendaService.parseTime(time),
                "Deskripsi asli",
                AgendaPriority.NORMAL,
                false,
                0,
                steps);
    }

    private void fixture(Path file) {
        try (var db = new SqliteDatabase(file)) {
            var repo = new SqliteRepository(db);
            var hash = new PasswordHasher().hash("sandi-uji-lokal".toCharArray());
            repo.createAccount("001", "Agenda Mahasiswa", hash);
            repo.createAccount("002", "Akun Lain", hash);
            repo.createAccount("003", "Akun Baru", hash);
            var session = new UserSession();
            session.login("001");
            var academic = new AcademicService(repo, session);
            semester = academic.saveSemesterForm(0, "3", "2026", true).id();
            archive = academic.saveSemesterForm(0, "1", "2025", false).id();
            first = academic.saveCourseMetadata(0, semester, "PBO", "PBO-21", "3").id();
            second = academic.saveCourseMetadata(0, semester, "Struktur Data", "", "2").id();
            oldCourse = academic.saveCourseMetadata(0, archive, "Arsip", "", "2").id();
            var grades = new GradePlanningService(new SqliteGradeRepository(db), session);
            grades.saveComponent(first, "nilai", "Proyek", "100", "84.5", "100", 0);
            grades.save(grades.create(semester, "Skenario awal", TestData.n("3")));
            var learning =
                    new LearningService(
                            new SqliteLearningRepository(db), session, new AssetStore(directory));
            var chapter = learning.saveFolder(0, LearningLocation.Kind.CHAPTER, first, "Bab");
            learning.save(
                    new Material(0, chapter.location(), "Materi tetap", "# Materi", "", 0, 0, ""),
                    "Catatan tetap",
                    0);
            session.login("002");
            foreignSemester = academic.saveSemesterForm(0, "3", "2026", true).id();
            foreignCourse =
                    academic.saveCourseMetadata(0, foreignSemester, "Course privat", "", "3").id();
        }
    }

    @Test
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void sharedAgendaCrudChecklistGuardsAndRestartPreserveAcademicData() throws Exception {
        Path file = directory.resolve("agenda-ui.db");
        fixture(file);
        var errors = new CopyOnWriteArrayList<Throwable>();
        var handler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, error) -> {
                    errors.add(error);
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
            var service = new AgendaService(new SqliteAgendaRepository(inspection), session);
            var grades = new GradePlanningService(new SqliteGradeRepository(inspection), session);
            var repo = new SqliteRepository(inspection);
            var before = grades.courses(semester);
            var scenariosBefore = grades.scenarios(semester);
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
            tab("main-tabs", 3);
            present("agenda-items");
            assertEquals(
                    "Belum selesai",
                    fx(() -> ((ComboBox<?>) node("agenda-state-filter")).getValue()));
            shot("empty");
            var today = service.today();
            var partial =
                    service.save(
                            item(
                                    first,
                                    AgendaType.TASK,
                                    "Tugas hari ini",
                                    today,
                                    "",
                                    List.of(
                                            new ChecklistStep("analisis", "Analisis", true),
                                            new ChecklistStep(
                                                    "implementasi", "Implementasi", false))));
            long partialId = partial.id();
            var overdue =
                    service.save(
                            item(
                                    first,
                                    AgendaType.TASK,
                                    "Tugas terlambat",
                                    today.minusDays(1),
                                    "15:00",
                                    List.of()));
            var exam =
                    service.save(
                            item(
                                    second,
                                    AgendaType.EXAM,
                                    "Ujian sudah lewat",
                                    today.minusDays(1),
                                    "09:30",
                                    List.of()));
            var untimed =
                    service.save(
                            item(second, AgendaType.EXAM, "Ujian tanpa jam", today, "", List.of()));
            var future =
                    service.save(
                            item(
                                    second,
                                    AgendaType.EXAM,
                                    "Ujian mendatang",
                                    today.plusDays(3),
                                    "09:30",
                                    List.of()));
            var archived =
                    service.save(
                            item(
                                    oldCourse,
                                    AgendaType.TASK,
                                    "Tugas arsip",
                                    today.plusDays(2),
                                    "",
                                    List.of()));
            var done =
                    service.save(
                            item(first, AgendaType.TASK, "Selesai awal", today, "", List.of()));
            service.complete(done, true, false);
            session.login("002");
            service.save(
                    item(
                            foreignCourse,
                            AgendaType.TASK,
                            "Privat akun kedua",
                            today,
                            "",
                            List.of()));
            session.login("001");
            press("agenda-refresh");
            for (var group : List.of("OVERDUE", "PAST_EXAMS", "TODAY", "UPCOMING"))
                present("agenda-section-" + group);
            assertTrue(card(partial.id()).contains("Tanpa jam"));
            assertTrue(card(partial.id()).contains("Checklist 1/2"));
            assertTrue(card(exam.id()).contains("09:30 WIB"));
            assertTrue(card(untimed.id()).contains("Tanpa jam"));
            assertNull(fx(() -> stage.getScene().lookup("#agenda-item-" + archived.id())));
            shot("groups");
            combo("agenda-type-filter", "Ujian");
            present("agenda-item-" + exam.id());
            assertNull(fx(() -> stage.getScene().lookup("#agenda-item-" + partialId)));
            shot("exams");
            scroll("agenda-list-page", 1);
            shot("upcoming");
            combo("agenda-type-filter", "Tugas");
            present("agenda-item-" + partial.id());
            assertNull(fx(() -> stage.getScene().lookup("#agenda-item-" + exam.id())));
            combo("agenda-type-filter", "Tugas & Ujian");
            press("agenda-add");
            present("agenda-priority");
            assertEquals(
                    AgendaPriority.NORMAL,
                    fx(() -> ((ComboBox<?>) node("agenda-priority")).getValue()));
            assertNull(fx(() -> ((ComboBox<?>) node("agenda-editor-course")).getValue()));
            press("agenda-save");
            assertTrue(label("agenda-status").contains("Pilih mata kuliah"));
            editorCourse(first);
            text("agenda-title", "Tugas baru");
            text("agenda-date", "31/02/2026");
            press("agenda-save");
            assertEquals("31/02/2026", fx(() -> ((TextField) node("agenda-date")).getText()));
            shot("validation");
            text("agenda-date", AgendaService.DATE_FORMAT.format(today.plusDays(1)));
            text("agenda-time", "9:30");
            press("agenda-save");
            assertEquals("9:30", fx(() -> ((TextField) node("agenda-time")).getText()));
            text("agenda-time", "09:30");
            String stepA = addStep("Rancang"), stepB = addStep("Uji");
            press("agenda-step-done-" + stepA);
            combo("agenda-type", AgendaType.EXAM);
            assertEquals(AgendaType.TASK, fx(() -> ((ComboBox<?>) node("agenda-type")).getValue()));
            assertTrue(label("agenda-status").contains("Checklist masih ada"));
            combo("agenda-priority", AgendaPriority.HIGH);
            scroll("agenda-step-text-" + stepB, 1);
            shot("checklist-editor");
            fx(
                    () -> {
                        ((Button) node("agenda-save")).fire();
                        ((Button) node("agenda-save")).fire();
                        return null;
                    });
            idle();
            var created =
                    service.items(semester, first).stream()
                            .filter(i -> i.title().equals("Tugas baru"))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    1,
                    service.items(semester, first).stream()
                            .filter(i -> i.title().equals("Tugas baru"))
                            .count());
            assertEquals(AgendaPriority.HIGH, created.priority());
            press("agenda-edit-" + created.id());
            press("agenda-step-up-" + stepB);
            text("agenda-step-text-" + stepB, "Uji lengkap");
            combo("agenda-priority", AgendaPriority.LOW);
            press("agenda-save");
            var reordered = service.open(created.id());
            assertEquals(AgendaPriority.LOW, reordered.priority());
            assertEquals(
                    List.of(stepB, stepA),
                    reordered.checklist().stream().map(ChecklistStep::id).toList());
            press("agenda-edit-" + partial.id());
            text("agenda-title", "Draf tugas tetap");
            dialog(
                    () ->
                            ((ComboBox<Object>) node("agenda-course-filter"))
                                    .setValue(choice("Struktur Data")),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertTrue(
                    fx(() -> ((ComboBox<?>) node("agenda-course-filter")).getValue().toString())
                            .startsWith("Semua"));
            assertEquals(
                    "Draf tugas tetap", fx(() -> ((TextField) node("agenda-title")).getText()));
            dialog(
                    () -> {
                        var box = (ComboBox<Semester>) node("agenda-semester");
                        box.setValue(
                                box.getItems().stream()
                                        .filter(s -> s.id() == archive)
                                        .findFirst()
                                        .orElseThrow());
                    },
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(
                    semester,
                    fx(() -> ((ComboBox<Semester>) node("agenda-semester")).getValue().id()));
            dialog(() -> ((Button) node("logout")).fire(), ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog(
                    () -> stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertTrue(fx(() -> stage.isShowing()));
            trigger(
                    inspection,
                    "CREATE TRIGGER fail_agenda_ui BEFORE INSERT ON agenda_steps BEGIN SELECT"
                        + " RAISE(ABORT,'fixture'); END");
            dialog(
                    () -> ((TabPane) node("main-tabs")).getSelectionModel().select(2),
                    ButtonBar.ButtonData.YES);
            assertEquals(
                    3,
                    fx(() -> ((TabPane) node("main-tabs")).getSelectionModel().getSelectedIndex()));
            assertEquals(
                    "Draf tugas tetap", fx(() -> ((TextField) node("agenda-title")).getText()));
            assertEquals(partial, service.open(partial.id()));
            assertTrue(label("agenda-status").contains("Gagal menyimpan"));
            shot("save-error");
            trigger(inspection, "DROP TRIGGER fail_agenda_ui");
            press("agenda-save");
            partial = service.open(partial.id());
            assertEquals("Draf tugas tetap", partial.title());
            press("agenda-edit-" + partial.id());
            text("agenda-title", "Jangan disimpan");
            dialog(() -> ((Button) node("agenda-cancel")).fire(), ButtonBar.ButtonData.NO);
            assertEquals("Draf tugas tetap", service.open(partial.id()).title());
            dialog(
                    () -> ((Button) node("agenda-complete-" + partialId)).fire(),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertFalse(service.open(partialId).completed());
            dialog(
                    () -> ((Button) node("agenda-complete-" + partialId)).fire(),
                    ButtonBar.ButtonData.YES);
            assertTrue(service.open(partialId).completed());
            assertEquals(partial.checklist(), service.open(partialId).checklist());
            combo("agenda-state-filter", "Selesai");
            present("agenda-item-" + partialId);
            shot("completed");
            press("agenda-complete-" + partialId);
            assertFalse(service.open(partialId).completed());
            assertEquals(partial.checklist(), service.open(partialId).checklist());
            combo("agenda-state-filter", "Belum selesai");
            dialog(
                    () -> ((Button) node("agenda-delete-" + overdue.id())).fire(),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(overdue, service.open(overdue.id()));
            dialog(
                    () -> ((Button) node("agenda-delete-" + overdue.id())).fire(),
                    ButtonBar.ButtonData.OK_DONE);
            assertThrows(IllegalArgumentException.class, () -> service.open(overdue.id()));
            filterCourse("PBO");
            shot("course-filter");
            Node shared = fx(() -> node("agenda-view"));
            tab("main-tabs", 2);
            assertEquals(
                    3,
                    fx(
                            () ->
                                    ((TabPane) node("hub-sections"))
                                            .getSelectionModel()
                                            .getSelectedIndex()));
            assertEquals(first, fx(() -> ((ComboBox<Course>) node("hub-course")).getValue().id()));
            assertSame(shared, fx(() -> node("agenda-view")));
            press("agenda-add");
            assertEquals(
                    first,
                    fx(() -> ((ComboBox<Course>) node("agenda-editor-course")).getValue().id()));
            assertTrue(fx(() -> node("agenda-editor-course").isDisabled()));
            combo("agenda-type", AgendaType.EXAM);
            text("agenda-title", "Ujian dari hub");
            text("agenda-date", AgendaService.DATE_FORMAT.format(today.plusDays(5)));
            text("agenda-time", "");
            press("agenda-save");
            var fromHub =
                    service.items(semester, first).stream()
                            .filter(i -> i.title().equals("Ujian dari hub"))
                            .findFirst()
                            .orElseThrow();
            assertTrue(fromHub.time().isEmpty());
            shot("hub");
            tab("main-tabs", 3);
            assertSame(shared, fx(() -> node("agenda-view")));
            present("agenda-item-" + fromHub.id());
            press("agenda-edit-" + fromHub.id());
            text("agenda-time", "10:15");
            press("agenda-save");
            tab("main-tabs", 2);
            assertTrue(card(fromHub.id()).contains("10:15 WIB"));
            press("agenda-edit-" + fromHub.id());
            text("agenda-title", "Ujian hub berubah");
            trigger(
                    inspection,
                    "CREATE TRIGGER fail_hub_agenda BEFORE UPDATE ON agenda_items BEGIN SELECT"
                        + " RAISE(ABORT,'fixture'); END");
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
                    "Ujian hub berubah", fx(() -> ((TextField) node("agenda-title")).getText()));
            trigger(inspection, "DROP TRIGGER fail_hub_agenda");
            press("agenda-save");
            fx(
                    () -> {
                        var box = (ComboBox<Course>) node("hub-course");
                        box.setValue(
                                box.getItems().stream()
                                        .filter(c -> c.id() == second)
                                        .findFirst()
                                        .orElseThrow());
                        return null;
                    });
            idle();
            assertNull(fx(() -> stage.getScene().lookup("#agenda-item-" + fromHub.id())));
            present("agenda-item-" + future.id());
            tab("main-tabs", 3);
            fx(
                    () -> {
                        var box = (ComboBox<Semester>) node("agenda-semester");
                        box.setValue(
                                box.getItems().stream()
                                        .filter(s -> s.id() == archive)
                                        .findFirst()
                                        .orElseThrow());
                        return null;
                    });
            idle();
            present("agenda-item-" + archived.id());
            assertEquals(semester, repo.getProfile("001").activeSemesterId().orElseThrow());
            tab("main-tabs", 2);
            assertEquals(
                    archive, fx(() -> ((ComboBox<Semester>) node("hub-semester")).getValue().id()));
            shot("archived");
            tab("main-tabs", 3);
            fx(
                    () -> {
                        var box = (ComboBox<Semester>) node("agenda-semester");
                        box.setValue(
                                box.getItems().stream()
                                        .filter(s -> s.id() == semester)
                                        .findFirst()
                                        .orElseThrow());
                        return null;
                    });
            idle();
            if (System.getProperty("glass.win.uiScale") == null)
                for (int width : new int[] {1024, 760}) {
                    fx(
                            () -> {
                                stage.setWidth(width);
                                stage.setHeight(width == 760 ? 480 : 768);
                                return null;
                            });
                    shot("list-" + width);
                    press("agenda-edit-" + created.id());
                    shot("editor-" + width);
                    fx(
                            () -> {
                                for (String id : List.of("agenda-save", "agenda-cancel")) {
                                    var b = node(id).localToScene(node(id).getBoundsInLocal());
                                    assertTrue(
                                            b.getMinY() >= 0
                                                    && b.getMaxY() <= stage.getScene().getHeight(),
                                            id);
                                }
                                return null;
                            });
                    press("agenda-cancel");
                }
            assertEquals(before, grades.courses(semester));
            assertEquals(scenariosBefore, grades.scenarios(semester));
            assertEquals(semester, repo.getProfile("001").activeSemesterId().orElseThrow());
            press("logout"); openLogin();
            text("login-nim", "002");
            text("login-password", "sandi-uji-lokal");
            press("login");
            tab("main-tabs", 3);
            assertNull(fx(() -> stage.getScene().lookup("#agenda-item-" + created.id())));
            var privateItems = serviceItemsForSecond(inspection);
            assertEquals(1, privateItems.size());
            present("agenda-item-" + privateItems.getFirst().id());
            assertTrue(card(privateItems.getFirst().id()).contains("Privat akun kedua"));
            press("logout"); openLogin();
            text("login-nim", "003");
            text("login-password", "sandi-uji-lokal");
            press("login");
            tab("main-tabs", 3);
            press("agenda-add");
            assertTrue(label("agenda-status").contains("mata kuliah"));
            assertNull(fx(() -> stage.getScene().lookup("#agenda-title")));
            shot("no-courses");
            press("logout"); openLogin();
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
            tab("main-tabs", 3);
            present("agenda-item-" + fromHub.id());
            assertEquals("Ujian hub berubah", service.open(fromHub.id()).title());
            assertTrue(errors.isEmpty(), errors.toString());
        } catch (Throwable error) {
            screenshot("agenda-failure.png");
            throw error;
        } finally {
            fx(
                    () -> {
                        if (stage != null) stage.hide();
                        if (application != null) application.stop();
                        return null;
                    });
            Platform.exit();
            Thread.setDefaultUncaughtExceptionHandler(handler);
        }
    }

    private List<AgendaItem> serviceItemsForSecond(SqliteDatabase db) {
        var session = new UserSession();
        session.login("002");
        return new AgendaService(new SqliteAgendaRepository(db), session).items(foreignSemester, 0);
    }
}
