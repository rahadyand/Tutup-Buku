package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.*;

import org.junit.jupiter.api.*;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

class OptionalLoginFxCheck extends FxTestSupport {
    private final String scale = System.getProperty("glass.win.uiScale", "native");

    private void present(String id) throws Exception {
        await(() -> stage.getScene().lookup("#" + id) != null);
        idle();
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
        screenshot("optional-login-" + name + "-" + scale + ".png");
    }

    private String allText() throws Exception {
        return fx(
                () ->
                        stage.getScene().getRoot().lookupAll(".label").stream()
                                .map(n -> ((Label) n).getText())
                                .reduce("", (a, b) -> a + "\n" + b));
    }

    private void localAccount() throws Exception {
        tab("workspace-tabs", 2);
        present("open-login");
        assertNull(fx(() -> stage.getScene().lookup("#profile-nim")));
    }

    private void home() throws Exception {
        tab("workspace-tabs", 0);
        tab("main-tabs", 4);
        present("today-refresh");
    }

    private void createCourse(String number, String title) throws Exception {
        tab("workspace-tabs", 1);
        present("semester-number");
        text("semester-number", number);
        text("semester-year", "2026");
        click("set-active-semester");
        if (fx(() -> stage.getScene().lookup("#view-courses") != null)) click("view-courses");
        present("course-name");
        text("course-name", title);
        text("course-credits", "3");
        click("save-course");
    }

    private void start(Path file) throws Exception {
        fx(
                () -> {
                    stage = new Stage();
                    application = new TutupBukuApplication(file);
                    application.start(stage);
                    stage.setWidth(1366 / stage.getOutputScaleX());
                    stage.setHeight(768 / stage.getOutputScaleY());
                    return null;
                });
        present("today-dashboard");
    }

    @Test
    @Timeout(240)
    void guestStartupAccountSwitchingGuardsAndRestart() throws Exception {
        var errors = new CopyOnWriteArrayList<Throwable>();
        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> {
                    errors.add(e);
                    e.printStackTrace();
                });
        var ready = new CompletableFuture<Void>();
        Platform.startup(
                () -> {
                    Platform.setImplicitExit(false);
                    ready.complete(null);
                });
        ready.get(10, TimeUnit.SECONDS);
        Path file = directory.resolve("optional.db");
        try (var c = new OptionalLoginTest.Context(file)) {
            start(file);
            assertTrue(allText().contains("Mode tamu"));
            assertNull(fx(() -> stage.getScene().lookup("#login-nim")));
            assertFalse(fx(() -> node("logout").isVisible()));
            assertFalse(allText().contains("NIM local-workspace"));
            shot("fresh-1366");
            localAccount();
            shot("local-account");
            click("open-register");
            present("auth-tabs");
            assertEquals(
                    1,
                    fx(() -> ((TabPane) node("auth-tabs")).getSelectionModel().getSelectedIndex()));
            shot("registration-entry");
            click("cancel-auth");
            present("today-dashboard");
            localAccount();
            click("open-login");
            present("login-nim");
            text("login-nim", "Tidak dikenal");
            text("login-password", "sandi-salah");
            click("login");
            assertTrue(label("login-status").contains("NIM atau password salah"));
            shot("login-entry");
            click("cancel-auth");
            present("today-dashboard");
            c.auth.openGuest();
            String guest = c.session.requireOwner();
            assertEquals(1, c.count("users"));
            assertFalse(allText().contains(guest));
            createCourse("1", "Kuliah lokal");
            long sem = c.auth.profile().activeSemesterId().orElseThrow();
            long course = c.academic.courses(sem).getFirst().id();
            Material material = OptionalLoginTest.material(c, course, "Materi ruang lokal");
            c.learning
                    .bookmarkWriter(
                            new ReadingBookmark(
                                    material.id(), material.revision(), 0, material.location()))
                    .run();
            c.grades.saveComponent(course, "a", "Proyek", "100", "", "100", 0);
            var task =
                    c.agenda.save(
                            new AgendaItem(
                                    0,
                                    course,
                                    AgendaType.TASK,
                                    "Agenda lokal",
                                    c.agenda.today(),
                                    Optional.empty(),
                                    "",
                                    AgendaPriority.NORMAL,
                                    false,
                                    0,
                                    List.of(new ChecklistStep("step", "Langkah pertama", false))));
            home();
            assertNotNull(fx(() -> node("today-course-" + course)));
            shot("saved-guest");
            // Semua UI perencanaan tetap tersedia sebagai tamu, termasuk simpan dan buka skenario.
            click("today-whatif-" + course);
            present("what-if-pane");
            text("what-if-target", "84.5");
            await(() -> ((Label) node("what-if-projection")).getText().contains("A-"));
            dialog(() -> ((Button) node("what-if-save")).fire(), ButtonBar.ButtonData.OK_DONE);
            assertEquals(1, c.grades.scenarios(sem).size());
            assertTrue(c.grades.courses(sem).getFirst().components().getFirst().earned().isEmpty());
            home();
            click("today-item-" + task.id());
            present("agenda-title");
            assertEquals("Agenda lokal", fx(() -> ((TextField) node("agenda-title")).getText()));
            home();
            click("today-reading-" + material.id());
            present("material-note");
            text("material-note", "Catatan tamu otomatis");
            await(() -> c.learning.open(material.id()).note().equals("Catatan tamu otomatis"));
            click("edit-material");
            present("material-title");
            text("material-title", "Draf sebelum masuk akun");
            dialog(
                    () -> ((TabPane) node("workspace-tabs")).getSelectionModel().select(2),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(
                    0,
                    fx(
                            () ->
                                    ((TabPane) node("workspace-tabs"))
                                            .getSelectionModel()
                                            .getSelectedIndex()));
            shot("cancelled-draft");
            c.sql(
                    "CREATE TRIGGER optional_fail BEFORE UPDATE ON materials BEGIN SELECT"
                        + " RAISE(ABORT,'uji gagal simpan'); END");
            dialog(
                    () -> ((TabPane) node("workspace-tabs")).getSelectionModel().select(2),
                    ButtonBar.ButtonData.YES);
            assertEquals(
                    0,
                    fx(
                            () ->
                                    ((TabPane) node("workspace-tabs"))
                                            .getSelectionModel()
                                            .getSelectedIndex()));
            assertEquals(
                    "Draf sebelum masuk akun",
                    fx(() -> ((TextField) node("material-title")).getText()));
            shot("failed-save");
            c.sql("DROP TRIGGER optional_fail");
            dialog(
                    () -> ((TabPane) node("workspace-tabs")).getSelectionModel().select(2),
                    ButtonBar.ButtonData.YES);
            present("open-register");
            assertEquals(
                    "Draf sebelum masuk akun", c.learning.open(material.id()).material().title());
            click("open-register");
            present("register-nim");
            text("register-nim", "001");
            text("register-name", "Akun A");
            text("register-password", "sandi-uji-lokal");
            text("register-confirmation", "sandi-uji-lokal");
            click("register");
            assertTrue(label("login-status").contains("Akun berhasil"));
            assertNotNull(fx(() -> node("cancel-auth")));
            assertNull(fx(() -> stage.getScene().lookup("#workspace-tabs")));
            text("login-password", "sandi-uji-lokal");
            click("login");
            present("today-dashboard");
            assertTrue(allText().contains("NIM 001"));
            assertNull(fx(() -> stage.getScene().lookup("#today-course-" + course)));
            assertTrue(allText().contains("Belum ada semester"));
            tab("workspace-tabs", 2);
            present("profile-nim");
            assertEquals("001", fx(() -> ((TextField) node("profile-nim")).getText()));
            text("profile-name", "Akun A diperbarui");
            combo("profile-program", "Informatika");
            click("save-profile");
            shot("authenticated-profile");
            createCourse("3", "Kuliah akun A");
            home();
            shot("account-workspace");
            c.sql("ALTER TABLE users RENAME TO unavailable_users");
            click("logout");
            present("retry-local-workspace");
            assertNull(fx(() -> stage.getScene().lookup("#workspace-tabs")));
            assertFalse(allText().contains("Kuliah akun A"));
            shot("local-load-failure");
            c.sql("ALTER TABLE unavailable_users RENAME TO users");
            click("retry-local-workspace");
            present("today-dashboard");
            assertTrue(allText().contains("Mode tamu"));
            assertNotNull(fx(() -> node("today-course-" + course)));
            assertFalse(allText().contains("Kuliah akun A"));
            shot("returned-guest");
            assertEquals(guest, c.auth.openGuest().nim());
            assertEquals(
                    "Draf sebelum masuk akun", c.learning.open(material.id()).material().title());
            c.auth.register(
                    "002",
                    "Akun B",
                    "sandi-uji-lokal".toCharArray(),
                    "sandi-uji-lokal".toCharArray());
            localAccount();
            click("open-login");
            present("login-nim");
            text("login-nim", "002");
            text("login-password", "sandi-uji-lokal");
            click("login");
            present("today-dashboard");
            assertNull(fx(() -> stage.getScene().lookup("#today-course-" + course)));
            assertTrue(allText().contains("Belum ada semester"));
            // Tutup saat akun terdaftar sedang terbuka: startup berikutnya tetap lokal, bukan akun
            // terakhir.
            fx(
                    () -> {
                        application.stop();
                        stage.hide();
                        return null;
                    });
            start(file);
            assertTrue(allText().contains("Mode tamu"));
            assertNotNull(fx(() -> node("today-course-" + course)));
            assertEquals(3, c.count("users"));
            fx(
                    () -> {
                        stage.setWidth(1024 / stage.getOutputScaleX());
                        stage.setHeight(768 / stage.getOutputScaleY());
                        return null;
                    });
            shot("guest-1024");
            fx(
                    () -> {
                        stage.setWidth(760);
                        stage.setHeight(480);
                        return null;
                    });
            shot("guest-minimum");
            localAccount();
            shot("account-minimum");
            click("open-login");
            present("login-nim");
            shot("login-minimum");
            click("cancel-auth");
            present("today-dashboard");
            assertTrue(errors.isEmpty(), errors.toString());
        } finally {
            fx(
                    () -> {
                        if (application != null) application.stop();
                        if (stage != null) stage.hide();
                        return null;
                    });
            Platform.exit();
        }
    }
}
