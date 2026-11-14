package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

class RevisionTwoFxCheck extends FxTestSupport {
    double top(String id) {
        Node n = node(id);
        return n.localToScene(n.getLayoutBounds()).getMinY();
    }

    double bottom(String id) {
        Node n = node(id);
        return n.localToScene(n.getLayoutBounds()).getMaxY();
    }

    void width(String id, double min, double max) throws Exception {
        fx(
                () -> {
                    stage.getScene().getRoot().applyCss();
                    stage.getScene().getRoot().layout();
                    double value = node(id).getLayoutBounds().getWidth();
                    assertTrue(value >= min && value <= max, id + " width=" + value);
                    return null;
                });
    }

    void reveal(String id) throws Exception {
        fx(
                () -> {
                    javafx.scene.Parent parent = node(id).getParent();
                    while (!(parent instanceof ScrollPane)) parent = parent.getParent();
                    ((ScrollPane) parent).setVvalue(1);
                    return null;
                });
        idle();
        fx(
                () -> {
                    Node button = node(id);
                    javafx.scene.Parent parent = button.getParent();
                    while (!(parent instanceof ScrollPane)) parent = parent.getParent();
                    Node viewport = parent.lookup(".viewport");
                    var visible = viewport.localToScene(viewport.getLayoutBounds());
                    var bounds = button.localToScene(button.getLayoutBounds());
                    assertTrue(bounds.getMinY() >= visible.getMinY());
                    assertTrue(bounds.getMaxY() <= visible.getMaxY());
                    return null;
                });
    }

    @Test
    void compactFormsGreetingsProfileGuardsAndRightNavigation() throws Exception {
        var file = directory.resolve("revision-two.db");
        try (var db = new SqliteDatabase(file)) {
            var repo = new SqliteRepository(db);
            var hash = new PasswordHasher().hash("sandi-uji-lokal".toCharArray());
            repo.createAccount("L0125006", "Nama Privat", hash);
            repo.updateProfile("L0125006", "Nama Privat", "Prodi Arsip");
            repo.createAccount("0003", "Tanpa Prodi", hash);
            repo.createAccount("0002", "Lama", hash);
            repo.updateProfile("0002", "Lama", " sAiNs DaTa ");
        }
        var started = new CompletableFuture<Void>();
        Platform.startup(
                () -> {
                    Platform.setImplicitExit(false);
                    started.complete(null);
                });
        started.get(10, TimeUnit.SECONDS);
        String suffix =
                System.getProperty("glass.win.uiScale", "native")
                        + "-"
                        + System.getProperty("revision.width", "1366");
        try {
            fx(
                    () -> {
                        stage = new Stage();
                        application = new TutupBukuApplication(file);
                        application.start(stage);
                        stage.setWidth(
                                Integer.parseInt(System.getProperty("revision.width", "1366"))
                                        / stage.getOutputScaleX());
                        stage.setHeight(768 / stage.getOutputScaleY());
                        return null;
                    });
            idle();
            text("login-nim", "L0125006");
            assertFalse(label("login-nim-guidance").contains("Nama Privat"));
            width("login-nim", 180, 220);
            width("login-password", 240, 280);
            assertTrue(fx(() -> ((Label) node("auth-title")).getFont().getSize() >= 34));
            assertTrue(fx(() -> node("auth-description").getLayoutBounds().getHeight() >= 28));
            screenshot("r002-login-" + suffix + ".png");
            text("login-password", "salah");
            click("login");
            assertEquals("salah", fx(() -> ((PasswordField) node("login-password")).getText()));
            click("login-password-toggle");
            width("login-password-visible", 240, 280);
            text("login-password-visible", "sandi-uji-lokal");
            assertEquals(
                    "sandi-uji-lokal",
                    fx(() -> ((PasswordField) node("login-password")).getText()));
            tab("auth-tabs", 1);
            text("register-name", "Anto");
            text("register-nim", "L0125001");
            assertTrue(
                    label("register-nim-guidance").contains("Anto dari Informatika angkatan 2025"));
            text("register-nim", "L0225001");
            assertTrue(
                    label("register-nim-guidance").contains("Anto dari Sains Data angkatan 2025"));
            width("register-nim", 180, 220);
            width("register-name", 260, 320);
            width("register-password", 240, 280);
            width("register-confirmation", 240, 280);
            screenshot("r002-register-" + suffix + ".png");
            text("register-nim", "L02");
            assertFalse(label("register-nim-guidance").contains("2025"));
            tab("auth-tabs", 0);
            text("login-password", "sandi-uji-lokal");
            fx(
                    () -> {
                        ((PasswordField) node("login-password"))
                                .getOnAction()
                                .handle(new javafx.event.ActionEvent());
                        return null;
                    });
            idle();
            await(() -> stage.getScene().lookup("#workspace-tabs") != null);
            tab("workspace-tabs", 2);
            await(() -> stage.getScene().lookup("#profile-program") != null);
            assertNull(fx(() -> ((ComboBox<?>) node("profile-program")).getValue()));
            assertEquals(
                    java.util.List.of("Informatika", "Sains Data"),
                    fx(() -> ((ComboBox<?>) node("profile-program")).getItems().stream().toList()));
            width("profile-nim", 180, 220);
            width("profile-name", 260, 320);
            try (var db = new SqliteDatabase(file)) {
                assertEquals(
                        "Prodi Arsip",
                        new SqliteRepository(db).getProfile("L0125006").studyProgram());
            }
            combo("profile-program", "Informatika");
            text("profile-name", "Anto");
            dialog(
                    () -> ((Button) node("open-learning")).fire(),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(
                    "Informatika", fx(() -> ((ComboBox<?>) node("profile-program")).getValue()));
            try (var db = new SqliteDatabase(file)) {
                db.read(
                        c -> {
                            try (var s = c.createStatement()) {
                                s.execute(
                                        "CREATE TRIGGER reject_profile BEFORE UPDATE ON users BEGIN"
                                                + " SELECT RAISE(ABORT,'fixture'); END");
                            }
                            return null;
                        });
                click("save-profile");
                assertTrue(label("profile-status").contains("gagal disimpan"));
                assertEquals("Anto", fx(() -> ((TextField) node("profile-name")).getText()));
                assertEquals(
                        "Informatika",
                        fx(() -> ((ComboBox<?>) node("profile-program")).getValue()));
                db.read(
                        c -> {
                            try (var s = c.createStatement()) {
                                s.execute("DROP TRIGGER reject_profile");
                            }
                            return null;
                        });
            }
            click("save-profile");
            screenshot("r002-profile-" + suffix + ".png");
            combo("profile-program", "Sains Data");
            dialog(() -> ((Button) node("open-learning")).fire(), ButtonBar.ButtonData.NO);
            tab("workspace-tabs", 2);
            assertEquals(
                    "Informatika", fx(() -> ((ComboBox<?>) node("profile-program")).getValue()));
            combo("profile-program", "Sains Data");
            dialog(() -> ((Button) node("open-simulation")).fire(), ButtonBar.ButtonData.YES);
            try (var db = new SqliteDatabase(file)) {
                assertEquals(
                        "Sains Data",
                        new SqliteRepository(db).getProfile("L0125006").studyProgram());
            }
            tab("workspace-tabs", 1);
            text("semester-number", "3");
            text("semester-year", "2026");
            width("semester-number", 80, 100);
            width("semester-year", 100, 120);
            width("set-active-semester", 150, 180);
            width("save-semester", 150, 180);
            width("delete-semester", 150, 180);
            assertTrue(fx(() -> top("new-semester") > bottom("delete-semester")));
            screenshot("r002-semester-" + suffix + ".png");
            reveal("new-semester");
            screenshot("r002-semester-actions-" + suffix + ".png");
            click("set-active-semester");
            text("course-name", "PBO");
            text("course-credits", "3");
            click("save-course");
            assertTrue(label("course-status").contains("tersimpan"));
            assertTrue(fx(() -> top("new-course") > bottom("course-status")));
            reveal("new-course");
            screenshot("r002-courses-" + suffix + ".png");
            text("course-name", "Belum disimpan");
            dialog(() -> ((Button) node("new-course")).fire(), ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals("Belum disimpan", fx(() -> ((TextField) node("course-name")).getText()));
            click("save-course");
            click("new-course");
            assertEquals("", fx(() -> ((TextField) node("course-name")).getText()));
            click("edit-semesters");
            text("semester-year", "2027");
            dialog(() -> ((Button) node("new-semester")).fire(), ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals("2027", fx(() -> ((TextField) node("semester-year")).getText()));
            dialog(() -> ((Button) node("new-semester")).fire(), ButtonBar.ButtonData.NO);
            assertEquals("", fx(() -> ((TextField) node("semester-number")).getText()));
            try (var db = new SqliteDatabase(file)) {
                assertEquals(1, new SqliteRepository(db).listSemesters("L0125006").size());
            }
            click("open-simulation");
            assertEquals(
                    1,
                    fx(() -> ((TabPane) node("main-tabs")).getSelectionModel().getSelectedIndex()));
            fx(
                    () -> {
                        var root = stage.getScene().getRoot();
                        root.applyCss();
                        root.layout();
                        var logout = node("logout").localToScene(node("logout").getLayoutBounds());
                        var learning =
                                node("open-learning")
                                        .localToScene(node("open-learning").getLayoutBounds());
                        var grades =
                                node("open-simulation")
                                        .localToScene(node("open-simulation").getLayoutBounds());
                        assertTrue(logout.getMinX() > stage.getScene().getWidth() * 0.7);
                        assertEquals(logout.getMaxX(), grades.getMaxX(), 1);
                        assertTrue(learning.getMinY() > logout.getMaxY());
                        assertTrue(learning.getMaxX() < grades.getMinX());
                        return null;
                    });
            screenshot("r002-header-" + suffix + ".png");
            click("logout");
            text("login-nim", "0002");
            text("login-password", "sandi-uji-lokal");
            click("login");
            tab("workspace-tabs", 2);
            await(() -> stage.getScene().lookup("#profile-program") != null);
            assertEquals(
                    "Sains Data", fx(() -> ((ComboBox<?>) node("profile-program")).getValue()));
            click("logout");
            text("login-nim", "0003");
            text("login-password", "sandi-uji-lokal");
            click("login");
            tab("workspace-tabs", 2);
            await(() -> stage.getScene().lookup("#profile-program") != null);
            assertNull(fx(() -> ((ComboBox<?>) node("profile-program")).getValue()));
            click("open-learning");
            try (var db = new SqliteDatabase(file)) {
                assertEquals("", new SqliteRepository(db).getProfile("0003").studyProgram());
            }
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
