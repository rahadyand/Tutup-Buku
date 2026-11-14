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

import java.util.concurrent.*;

class RevisionOneFxCheck extends FxTestSupport {
    @Test
    void revisedAccountNavigationAndAcademicJourney() throws Exception {
        var file = directory.resolve("revision-ui.db");
        try (var db = new SqliteDatabase(file)) {
            new SqliteRepository(db)
                    .createAccount(
                            "0001",
                            "Akun historis",
                            new PasswordHasher().hash("sandi-uji-lokal".toCharArray()));
        }
        var ready = new CompletableFuture<Void>();
        Platform.startup(
                () -> {
                    Platform.setImplicitExit(false);
                    ready.complete(null);
                });
        ready.get(10, TimeUnit.SECONDS);
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
            screenshot("r001-login-" + System.getProperty("glass.win.uiScale", "native") + ".png");
            text("login-nim", "L0125001");
            assertTrue(label("login-nim-guidance").contains("Informatika"));
            text("login-password", "sandi-uji-lokal");
            click("login-password-toggle");
            assertTrue(fx(() -> node("login-password-visible").isVisible()));
            text("login-password-visible", "sandi-uji-baru");
            assertEquals(
                    "sandi-uji-baru", fx(() -> ((PasswordField) node("login-password")).getText()));
            click("login-password-toggle");
            assertFalse(fx(() -> node("login-password-visible").isManaged()));
            tab("auth-tabs", 1);
            text("register-nim", "L0225001");
            text("register-name", "Pengguna Demo Revision 001");
            assertTrue(label("register-nim-guidance").contains("Sains Data"));
            text("register-password", "sandi-uji-lokal");
            text("register-confirmation", "sandi-uji-lokal");
            for (String id : new String[] {"register-password", "register-confirmation"}) {
                click(id + "-toggle");
                assertTrue(fx(() -> node(id + "-visible").isManaged()));
                assertEquals(
                        "sandi-uji-lokal", fx(() -> ((TextField) node(id + "-visible")).getText()));
                click(id + "-toggle");
            }
            screenshot(
                    "r001-register-" + System.getProperty("glass.win.uiScale", "native") + ".png");
            fx(() -> {
                var hint=(Label)node("register-nim-guidance");
                stage.getScene().getRoot().applyCss();stage.getScene().getRoot().layout();
                assertTrue(hint.getHeight()>=hint.prefHeight(hint.getWidth())-1,"Panduan NIM tidak boleh terpotong menjadi elipsis.");
                return null;
            });
            click("register");
            assertTrue(label("login-status").contains("berhasil dibuat"));
            tab("auth-tabs", 1);
            assertFalse(fx(() -> node("register-password-visible").isManaged()));
            assertEquals("", fx(() -> ((PasswordField) node("register-confirmation")).getText()));
            tab("auth-tabs", 0);
            text("login-password", "sandi-uji-lokal");
            click("login-password-toggle");
            click("login");
            var labels =
                    fx(
                            () ->
                                    ((TabPane) node("workspace-tabs"))
                                            .getTabs().stream().map(Tab::getText).toList());
            assertEquals(
                    java.util.List.of("Dashboard", "Semester & Mata Kuliah", "Profil"), labels);
            assertTrue(label("active-semester-label").contains("Belum ada semester aktif"));
            tab("workspace-tabs", 2);
            text("profile-name", "Demo Revisi");
            text("profile-program", "Sains Data");
            click("save-profile");
            assertTrue(label("active-semester-label").contains("Sains Data"));
            click("open-simulation");
            assertEquals(
                    0,
                    fx(
                            () ->
                                    ((TabPane) node("workspace-tabs"))
                                            .getSelectionModel()
                                            .getSelectedIndex()));
            assertEquals(
                    1,
                    fx(() -> ((TabPane) node("main-tabs")).getSelectionModel().getSelectedIndex()));
            tab("workspace-tabs", 1);
            screenshot(
                    "r001-semester-" + System.getProperty("glass.win.uiScale", "native") + ".png");
            text("semester-number", "3");
            text("semester-year", "2026/2027");
            click("set-active-semester");
            assertTrue(label("semester-status").contains("4 digit"));
            assertNotNull(fx(() -> node("semester-page")));
            text("semester-year", "2026");
            click("set-active-semester");
            assertNotNull(fx(() -> node("courses-page")));
            assertTrue(label("active-semester-label").contains("Semester 3 · Tahun 2026"));
            assertNull(fx(() -> stage.getScene().lookup("#course-attendance")));
            text("course-name", "Pemrograman Berorientasi Objek");
            text("course-code", "IF203");
            text("course-credits", "3");
            click("save-course");
            screenshot(
                    "r001-courses-" + System.getProperty("glass.win.uiScale", "native") + ".png");
            screenshot(
                    "r001-navigation-"
                            + System.getProperty("glass.win.uiScale", "native")
                            + ".png");
            text("course-name", "Edit belum disimpan");
            dialog(
                    () -> ((Button) node("edit-semesters")).fire(),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(
                    "Edit belum disimpan", fx(() -> ((TextField) node("course-name")).getText()));
            dialog(
                    () -> ((Button) node("open-learning")).fire(),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(
                    1,
                    fx(
                            () ->
                                    ((TabPane) node("workspace-tabs"))
                                            .getSelectionModel()
                                            .getSelectedIndex()));
            click("save-course");
            click("edit-semesters");
            text("semester-number", "4");
            text("semester-year", "2027");
            click("save-semester");
            assertTrue(label("active-semester-label").contains("Semester 4 · Tahun 2027"));
            click("new-semester");
            text("semester-number", "5");
            text("semester-year", "2026");
            try (var inspection = new SqliteDatabase(file)) {
                inspection.read(
                        c -> {
                            try (var q = c.createStatement()) {
                                q.execute(
                                        "CREATE TRIGGER reject_semester BEFORE INSERT ON semesters"
                                            + " BEGIN SELECT RAISE(ABORT,'fixture'); END");
                            }
                            return null;
                        });
                click("save-semester");
                assertTrue(label("semester-status").contains("gagal disimpan"));
                assertNotNull(fx(() -> node("semester-page")));
                assertEquals("2026", fx(() -> ((TextField) node("semester-year")).getText()));
                inspection.read(
                        c -> {
                            try (var q = c.createStatement()) {
                                q.execute("DROP TRIGGER reject_semester");
                            }
                            return null;
                        });
            }
            click("save-semester");
            assertTrue(label("active-semester-label").contains("Semester 4 · Tahun 2027"));
            assertEquals(0, fx(() -> ((ListView<?>) node("course-list")).getItems().size()));
            select("semester-list", 1); // 2027 semester4 owns the original course
            await(() -> ((ListView<?>) node("course-list")).getItems().size() == 1);
            click("edit-semesters");
            dialog(() -> ((Button) node("delete-semester")).fire(), ButtonBar.ButtonData.OK_DONE);
            assertTrue(label("active-semester-label").contains("Belum ada semester aktif"));
            click("open-simulation");
            assertEquals(
                    0,
                    fx(
                            () ->
                                    ((TabPane) node("workspace-tabs"))
                                            .getSelectionModel()
                                            .getSelectedIndex()));
            click("open-learning");
            assertEquals(
                    0,
                    fx(() -> ((TabPane) node("main-tabs")).getSelectionModel().getSelectedIndex()));
            click("logout");
            await(() -> stage.getScene().lookup("#login-password-visible") != null);
            assertFalse(fx(() -> node("login-password-visible").isManaged()));
            text("login-nim", "0001");
            text("login-password", "sandi-uji-lokal");
            click("login");
            assertTrue(label("active-semester-label").contains("Belum ada semester aktif"));
        } finally {
            fx(
                    () -> {
                        stage.hide();
                        application.stop();
                        return null;
                    });
            Platform.exit();
        }
    }
}
