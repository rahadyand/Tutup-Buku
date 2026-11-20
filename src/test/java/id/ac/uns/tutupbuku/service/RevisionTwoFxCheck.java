package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

class RevisionTwoFxCheck extends FxTestSupport {
    void associatedLabel(String id, String expected) throws Exception {
        fx(
                () -> {
                    var input = node(id);
                    var label = input.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY);
                    assertInstanceOf(Label.class, label, id);
                    assertEquals(expected, ((Label) label).getText(), id);
                    assertSame(input, ((Label) label).getLabelFor(), id);
                    return null;
                });
    }

    void focus(String id) throws Exception {
        fx(
                () -> {
                    stage.toFront();
                    stage.requestFocus();
                    node(id).requestFocus();
                    return null;
                });
        await(() -> stage.isFocused() && node(id).isFocused());
    }

    void key(KeyCode code, boolean shift) throws Exception {
        fx(
                () -> {
                    var window =
                            Window.getWindows().stream()
                                    .filter(Window::isFocused)
                                    .findFirst()
                                    .orElseThrow();
                    assertTrue(
                            window == stage || window.getScene().getRoot() instanceof DialogPane);
                    Node target = window.getScene().getFocusOwner();
                    assertNotNull(target);
                    // Antrekan event agar dialog modal tidak memblok pemanggil tes.
                    Platform.runLater(
                            () -> {
                                Event.fireEvent(
                                        target,
                                        new KeyEvent(
                                                KeyEvent.KEY_PRESSED,
                                                "",
                                                "",
                                                code,
                                                shift,
                                                false,
                                                false,
                                                false));
                                Event.fireEvent(
                                        target,
                                        new KeyEvent(
                                                KeyEvent.KEY_RELEASED,
                                                "",
                                                "",
                                                code,
                                                shift,
                                                false,
                                                false,
                                                false));
                            });
                    return null;
                });
    }

    void key(KeyCode code) throws Exception {
        key(code, false);
    }

    void traversal(String... ids) throws Exception {
        focus(ids[0]);
        for (int i = 1; i < ids.length; i++) {
            String id = ids[i];
            key(KeyCode.TAB);
            try {
                await(() -> node(id).isFocused());
            } catch (AssertionError error) {
                System.out.println(
                        "PHASE2 expected focus="
                                + id
                                + "; actual="
                                + fx(() -> String.valueOf(stage.getScene().getFocusOwner())));
                screenshot("phase2-keyboard-failure.png");
                throw error;
            }
            assertTrue(fx(() -> node(id).isFocusVisible()), id);
        }
        for (int i = ids.length - 2; i >= 0; i--) {
            String id = ids[i];
            key(KeyCode.TAB, true);
            await(() -> node(id).isFocused());
        }
        System.out.println("PHASE2 traversal forward/reverse: " + String.join(" -> ", ids));
    }

    void accessibilityAndKeyboard(String suffix) throws Exception {
        click("logout"); openLogin();
        await(() -> stage.getScene().lookup("#login-nim") != null);
        associatedLabel("login-nim", "NIM");
        associatedLabel("login-password", "Password");
        assertEquals(
                AccessibleRole.PASSWORD_FIELD,
                fx(() -> node("login-password").getAccessibleRole()));
        assertNull(
                fx(
                        () ->
                                node("login-password")
                                        .queryAccessibleAttribute(AccessibleAttribute.TEXT)));
        assertEquals(
                "Tampilkan Password", fx(() -> node("login-password-toggle").getAccessibleText()));
        assertEquals(
                AccessibleRole.BUTTON, fx(() -> node("login-password-toggle").getAccessibleRole()));
        assertEquals(AccessibleRole.TAB_PANE, fx(() -> node("auth-tabs").getAccessibleRole()));
        text("login-nim", "L0125006");
        assertEquals(label("login-nim-guidance"), fx(() -> node("login-nim").getAccessibleHelp()));
        assertEquals(
                "Tekan Enter untuk masuk.", fx(() -> node("login-password").getAccessibleHelp()));
        tab("auth-tabs", 1);
        associatedLabel("register-nim", "NIM");
        associatedLabel("register-name", "Nama");
        associatedLabel("register-password", "Password (minimal 8 karakter)");
        associatedLabel("register-confirmation", "Konfirmasi password");
        assertEquals(
                "Tampilkan Password (minimal 8 karakter)",
                fx(() -> node("register-password-toggle").getAccessibleText()));
        assertEquals(
                "Tampilkan Konfirmasi password",
                fx(() -> node("register-confirmation-toggle").getAccessibleText()));
        traversal(
                "register-nim",
                "register-name",
                "register-password",
                "register-password-toggle",
                "register-confirmation",
                "register-confirmation-toggle",
                "register");
        tab("auth-tabs", 0);
        text("login-nim", "0003");
        traversal("login-nim", "login-password", "login-password-toggle", "login");
        focus("login-nim");
        key(KeyCode.TAB);
        await(() -> node("login-password").isFocused());
        screenshot("phase2-focus-password-" + suffix + ".png");
        key(KeyCode.TAB);
        await(() -> node("login-password-toggle").isFocused());
        key(KeyCode.SPACE);
        await(() -> node("login-password-visible").isVisible());
        associatedLabel("login-password-visible", "Password");
        assertNull(
                fx(
                        () ->
                                node("login-password")
                                        .queryAccessibleAttribute(AccessibleAttribute.LABELED_BY)));
        assertEquals(
                AccessibleRole.TEXT_FIELD,
                fx(() -> node("login-password-visible").getAccessibleRole()));
        assertEquals(
                "Sembunyikan Password",
                fx(() -> node("login-password-toggle").getAccessibleText()));
        key(KeyCode.TAB, true);
        await(() -> node("login-password-visible").isFocused());
        key(KeyCode.TAB);
        await(() -> node("login-password-toggle").isFocused());
        key(KeyCode.SPACE);
        await(() -> node("login-password").isVisible());
        associatedLabel("login-password", "Password");
        key(KeyCode.TAB, true);
        await(() -> node("login-password").isFocused());
        key(KeyCode.ENTER);
        await(() -> ((Label) node("login-status")).getText().contains("NIM atau password salah"));
        assertEquals("0003", fx(() -> ((TextField) node("login-nim")).getText()));
        text("login-password", "sandi-uji-lokal");
        focus("login-password");
        key(KeyCode.ENTER);
        await(() -> stage.getScene().lookup("#workspace-tabs") != null);
        idle();
        tab("workspace-tabs", 2);
        await(() -> stage.getScene().lookup("#profile-nim") != null);
        associatedLabel("profile-nim", "NIM (tidak dapat diubah)");
        associatedLabel("profile-name", "Nama");
        associatedLabel("profile-program", "Program studi");
        assertFalse(fx(() -> ((TextField) node("profile-nim")).isEditable()));
        assertTrue(fx(() -> node("profile-nim").isFocusTraversable()));
        assertEquals(
                AccessibleRole.COMBO_BOX, fx(() -> node("profile-program").getAccessibleRole()));
        traversal("profile-nim", "profile-name", "profile-program", "save-profile");
        focus("profile-program");
        key(KeyCode.F4);
        await(() -> ((ComboBox<?>) node("profile-program")).isShowing());
        key(KeyCode.DOWN);
        key(KeyCode.ENTER);
        await(
                () ->
                        !((ComboBox<?>) node("profile-program")).isShowing()
                                && ((ComboBox<?>) node("profile-program")).getValue() != null);
        key(KeyCode.TAB);
        await(() -> node("save-profile").isFocused());
        screenshot("phase2-focus-save-" + suffix + ".png");
        key(KeyCode.SPACE);
        await(() -> ((Label) node("profile-status")).getText().equals("Profil tersimpan."));
        String savedName = fx(() -> ((TextField) node("profile-name")).getText());
        text("profile-name", savedName + " aksesibilitas");
        focus("open-learning");
        key(KeyCode.SPACE);
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
                    var cancel =
                            pane.getButtonTypes().stream()
                                    .filter(
                                            t ->
                                                    t.getButtonData()
                                                            == ButtonBar.ButtonData.CANCEL_CLOSE)
                                    .findFirst()
                                    .orElseThrow();
                    assertTrue(((Button) pane.lookupButton(cancel)).isDefaultButton());
                    for (var type : pane.getButtonTypes()) {
                        var button = (Button) pane.lookupButton(type);
                        assertEquals(AccessibleRole.BUTTON, button.getAccessibleRole());
                        assertFalse(
                                ((String) button.queryAccessibleAttribute(AccessibleAttribute.TEXT))
                                        .isBlank());
                    }
                    return null;
                });
        var dialogFocus = new java.util.HashSet<String>();
        for (int i = 0; i < 3; i++) {
            key(KeyCode.TAB);
            dialogFocus.add(
                    fx(
                            () -> {
                                var window =
                                        Window.getWindows().stream()
                                                .filter(
                                                        w ->
                                                                w.isShowing()
                                                                        && w.getScene().getRoot()
                                                                                instanceof
                                                                                DialogPane)
                                                .findFirst()
                                                .orElseThrow();
                                var button =
                                        assertInstanceOf(
                                                Button.class, window.getScene().getFocusOwner());
                                assertTrue(button.isFocusVisible());
                                return button.getText();
                            }));
        }
        assertEquals(java.util.Set.of("Simpan", "Buang perubahan", "Batal"), dialogFocus);
        key(KeyCode.ESCAPE);
        await(
                () ->
                        Window.getWindows().stream()
                                .noneMatch(
                                        w ->
                                                w.isShowing()
                                                        && w.getScene().getRoot()
                                                                instanceof DialogPane));
        assertEquals(
                2,
                fx(
                        () ->
                                ((TabPane) node("workspace-tabs"))
                                        .getSelectionModel()
                                        .getSelectedIndex()));
        assertTrue(
                fx(() -> ((TextField) node("profile-name")).getText().endsWith(" aksesibilitas")));
        text("profile-name", savedName);
        tab("workspace-tabs", 1);
        await(() -> stage.getScene().lookup("#semester-number") != null);
        associatedLabel("semester-editor-selector", "Semester tersimpan");
        associatedLabel("semester-number", "Semester");
        associatedLabel("semester-year", "Tahun");
        traversal(
                "semester-number",
                "semester-year",
                "set-active-semester",
                "save-semester",
                "delete-semester",
                "new-semester",
                "view-courses");
        assertTrue(fx(() -> node("save-semester").getAccessibleHelp().contains("tanpa mengganti")));
        text("semester-number", "12");
        text("semester-year", "2028");
        focus("save-semester");
        key(KeyCode.SPACE);
        await(() -> stage.getScene().lookup("#courses-page") != null);
        idle();
        associatedLabel("course-name", "Nama mata kuliah");
        associatedLabel("course-code", "Kode (opsional)");
        associatedLabel("course-credits", "SKS (lebih dari 0)");
        assertEquals(
                "Daftar semester tersimpan", fx(() -> node("semester-list").getAccessibleText()));
        assertEquals(
                "Daftar mata kuliah semester pilihan",
                fx(() -> node("course-list").getAccessibleText()));
        assertEquals(AccessibleRole.LIST_VIEW, fx(() -> node("semester-list").getAccessibleRole()));
        traversal(
                "course-name",
                "course-code",
                "course-credits",
                "save-course",
                "delete-course",
                "new-course");
    }

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
            openLogin();
            idle();
            associatedLabel("login-nim", "NIM");
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
            click("logout"); openLogin();
            text("login-nim", "0002");
            text("login-password", "sandi-uji-lokal");
            click("login");
            tab("workspace-tabs", 2);
            await(() -> stage.getScene().lookup("#profile-program") != null);
            assertEquals(
                    "Sains Data", fx(() -> ((ComboBox<?>) node("profile-program")).getValue()));
            click("logout"); openLogin();
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
            accessibilityAndKeyboard(suffix);
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
