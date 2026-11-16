package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

/** Kontrak presentasi form asli; database selalu di @TempDir, tanpa input OS global. */
class ReusableComponentsFxCheck extends FxTestSupport {
    private String suffix;

    private void layout() {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
    }

    private void control(String id, double width) throws Exception {
        fx(
                () -> {
                    layout();
                    Region input = (Region) node(id);
                    assertEquals(width, input.getPrefWidth(), id);
                    assertEquals(width, input.getMaxWidth(), id);
                    assertEquals(width, input.getWidth(), 1.2, id);
                    var label = input.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY);
                    assertInstanceOf(Label.class, label, id);
                    assertSame(input, ((Label) label).getLabelFor(), id);
                    return null;
                });
    }

    private void page(String name, String... reachable) throws Exception {
        // Buktikan setiap kontrol dapat dicapai dalam viewport tanpa mengubah urutan fokus.
        for (String id : reachable) {
            fx(
                    () -> {
                        layout();
                        Node input = node(id);
                        Node ancestor = input.getParent();
                        while (ancestor != null && !(ancestor instanceof ScrollPane))
                            ancestor = ancestor.getParent();
                        assertInstanceOf(ScrollPane.class, ancestor, id);
                        ScrollPane scroll = (ScrollPane) ancestor;
                        scroll.setVvalue(0);
                        layout();
                        Node viewport = scroll.lookup(".viewport");
                        var visible = viewport.localToScene(viewport.getLayoutBounds());
                        var bounds = input.localToScene(input.getLayoutBounds());
                        double overflow =
                                scroll.getContent().getLayoutBounds().getHeight()
                                        - scroll.getViewportBounds().getHeight();
                        if (overflow > 0)
                            scroll.setVvalue(
                                    Math.max(
                                            0,
                                            Math.min(
                                                    1,
                                                    (bounds.getMinY() - visible.getMinY())
                                                            / overflow)));
                        layout();
                        bounds = input.localToScene(input.getLayoutBounds());
                        assertTrue(bounds.getMinY() >= visible.getMinY() - 1, id);
                        assertTrue(bounds.getMaxY() <= visible.getMaxY() + 1, id);
                        assertTrue(bounds.getMinX() >= visible.getMinX() - 1, id);
                        assertTrue(bounds.getMaxX() <= visible.getMaxX() + 1, id);
                        assertTrue(input.isFocusTraversable(), id);
                        return null;
                    });
        }
        screenshot("components-" + name + "-" + suffix + ".png");
    }

    @Test
    void accountProfileAndSemesterPresentationAtSupportedSizes() throws Exception {
        var file = directory.resolve("components.db");
        try (var db = new SqliteDatabase(file)) {
            new SqliteRepository(db)
                    .createAccount(
                            "001",
                            "Mahasiswa Uji",
                            new PasswordHasher().hash("sandi-uji-lokal".toCharArray()));
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
                        return null;
                    });
            int[][] sizes =
                    System.getProperty("glass.win.uiScale") == null
                            ? new int[][] {{1366, 768}, {1024, 768}, {760, 480}}
                            : new int[][] {{1366, 768}};
            for (int[] size : sizes) {
                suffix =
                        System.getProperty("glass.win.uiScale", "native")
                                + "-"
                                + size[0]
                                + "x"
                                + size[1];
                fx(
                        () -> {
                            stage.setWidth(size[0] / stage.getOutputScaleX());
                            stage.setHeight(size[1] / stage.getOutputScaleY());
                            layout();
                            return null;
                        });
                await(() -> stage.getScene().lookup("#login-nim") != null);
                idle();
                control("login-nim", 200);
                control("login-password", 260);
                assertEquals(36, fx(() -> ((Label) node("auth-title")).getFont().getSize()), 0.1);
                assertTrue(
                        fx(
                                () ->
                                        stage.getScene()
                                                .getStylesheets()
                                                .getFirst()
                                                .endsWith("tutup-buku.css")));
                page("login", "login-nim", "login-password", "login-password-toggle", "login");
                click("login-password-toggle");
                control("login-password-visible", 260);
                click("login-password-toggle");
                tab("auth-tabs", 1);
                control("register-nim", 200);
                control("register-name", 300);
                control("register-password", 260);
                control("register-confirmation", 260);
                page(
                        "register",
                        "register-nim",
                        "register-name",
                        "register-password",
                        "register-confirmation",
                        "register");
                tab("auth-tabs", 0);
                text("login-nim", "001");
                text("login-password", "sandi-uji-lokal");
                click("login");
                await(() -> stage.getScene().lookup("#workspace-tabs") != null);
                tab("workspace-tabs", 2);
                control("profile-nim", 200);
                control("profile-name", 300);
                control("profile-program", 240);
                page("profile", "profile-nim", "profile-name", "profile-program", "save-profile");
                tab("workspace-tabs", 1);
                control("semester-number", 90);
                control("semester-year", 110);
                page(
                        "semester",
                        "semester-number",
                        "semester-year",
                        "set-active-semester",
                        "save-semester",
                        "delete-semester",
                        "new-semester",
                        "view-courses");
                text("semester-number", "0");
                text("semester-year", "2026");
                click("save-semester");
                assertEquals(
                        "Nomor semester harus bilangan bulat lebih dari 0.",
                        label("semester-status"));
                assertEquals("0", fx(() -> ((TextField) node("semester-number")).getText()));
                fx(
                        () -> {
                            Label status = (Label) node("semester-status");
                            assertTrue(status.isWrapText());
                            assertEquals(170, ((Region) node("save-semester")).getWidth(), 1.2);
                            return null;
                        });
                dialog(() -> ((Button) node("logout")).fire(), ButtonBar.ButtonData.NO);
            }
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
