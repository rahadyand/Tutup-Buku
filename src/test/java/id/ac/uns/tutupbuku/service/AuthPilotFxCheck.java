package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.*;

/** Error/retry pilot asli dan adopsi tema workspace Fase5C, seluruh data di @TempDir. */
class AuthPilotFxCheck extends FxTestSupport {
    private final boolean exactCapture = Boolean.getBoolean("pilot.exactCapture");
    private final int captureWidth = Integer.getInteger("pilot.capture.width", 1024);
    private final int captureHeight = Integer.getInteger("pilot.capture.height", 760);

    private void resizeCapture(int width, int height) throws Exception {
        fx(
                () -> {
                    // Ukur dekorasi saat ini; targetnya konten snapshot, bukan bingkai Stage.
                    stage.setWidth(stage.getWidth() + width - stage.getScene().getWidth());
                    stage.setHeight(stage.getHeight() + height - stage.getScene().getHeight());
                    return null;
                });
        await(
                () ->
                        Math.abs(stage.getScene().getWidth() - width) < 0.01
                                && Math.abs(stage.getScene().getHeight() - height) < 0.01);
    }

    private void resize(int width, int height) throws Exception {
        fx(
                () -> {
                    stage.setWidth(width / stage.getOutputScaleX());
                    stage.setHeight(height / stage.getOutputScaleY());
                    return null;
                });
        // Native resize selesai setelah FX turn; nama screenshot bukan bukti ukuran.
        await(
                () ->
                        Math.abs(stage.getWidth() * stage.getOutputScaleX() - width) <= 2
                                && Math.abs(stage.getHeight() * stage.getOutputScaleY() - height)
                                        <= 2
                                && stage.getScene().getWidth() * stage.getOutputScaleX() <= width
                                && stage.getScene().getWidth() * stage.getOutputScaleX()
                                        >= width - 64
                                && stage.getScene().getHeight() * stage.getOutputScaleY() <= height
                                && stage.getScene().getHeight() * stage.getOutputScaleY()
                                        >= height - 96);
    }

    private void feedback(String id, String suffix) throws Exception {
        fx(
                () -> {
                    var root = stage.getScene().getRoot();
                    root.applyCss();
                    root.layout();
                    Label message = (Label) node(id);
                    assertTrue(message.isVisible() && message.isManaged());
                    assertTrue(message.isWrapText());
                    Node ancestor = message.getParent();
                    while (!(ancestor instanceof ScrollPane)) ancestor = ancestor.getParent();
                    ScrollPane scroll = (ScrollPane) ancestor;
                    scroll.setVvalue(1);
                    root.layout();
                    Node viewport = scroll.lookup(".viewport");
                    var visible = viewport.localToScene(viewport.getLayoutBounds());
                    var bounds = message.localToScene(message.getLayoutBounds());
                    assertTrue(bounds.getMinY() >= visible.getMinY() - 1, id);
                    assertTrue(bounds.getMaxY() <= visible.getMaxY() + 1, id);
                    assertTrue(
                            message.getHeight() >= message.prefHeight(message.getWidth()) - 1, id);
                    assertTrue(bounds.getMaxY() <= stage.getScene().getHeight(), id);
                    assertEquals(13, message.getFont().getSize(), 0.01);
                    System.out.printf(
                            java.util.Locale.ROOT,
                            "PILOT %s %s scene=%.0fx%.0f feedback=%.1fx%.1f viewport=%.1f%n",
                            id,
                            suffix,
                            stage.getScene().getWidth(),
                            stage.getScene().getHeight(),
                            message.getWidth(),
                            message.getHeight(),
                            scroll.getViewportBounds().getHeight());
                    return null;
                });
        String filename = "pilot-" + id + "-" + suffix + ".png";
        if (exactCapture) {
            fx(
                    () -> {
                        assertEquals(captureWidth, stage.getScene().getWidth(), 0.01);
                        assertEquals(captureHeight, stage.getScene().getHeight(), 0.01);
                        return null;
                    });
        }
        screenshot(filename);
        if (exactCapture) {
            var saved =
                    javax.imageio.ImageIO.read(
                            java.nio.file.Path.of("target/verification", filename).toFile());
            assertNotNull(saved, filename);
            assertEquals(captureWidth, saved.getWidth(), filename);
            assertEquals(captureHeight, saved.getHeight(), filename);
            System.out.printf(
                    "CAPTURE %s PNG=%dx%d%n", filename, saved.getWidth(), saved.getHeight());
        }
    }

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void feedbackIsReachableAndThemeContinuesIntoWorkspace() throws Exception {
        assertTrue(captureWidth > 0 && captureHeight > 0);
        var file = directory.resolve("auth-pilot.db");
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
            openLogin();
            int[][] sizes =
                    exactCapture
                            ? new int[][] {{captureWidth, captureHeight}}
                            : System.getProperty("glass.win.uiScale") == null
                                    ? new int[][] {{1366, 768}, {1024, 768}, {760, 480}}
                                    : new int[][] {{1366, 768}};
            for (int[] size : sizes) {
                String suffix =
                        (exactCapture ? "capture-" : "")
                                + System.getProperty("glass.win.uiScale", "native")
                                + "-"
                                + size[0]
                                + "x"
                                + size[1];
                await(() -> stage.getScene().lookup("#auth-card") != null);
                idle();
                if (exactCapture) resizeCapture(size[0], size[1]);
                else resize(size[0], size[1]);
                fx(
                        () -> {
                            stage.getScene().getRoot().applyCss();
                            stage.getScene().getRoot().layout();
                            assertNotNull(stage.getScene().lookup(".tb-theme"));
                            Region card = (Region) node("auth-card");
                            assertEquals(
                                    Color.WHITE,
                                    card.getBackground().getFills().getFirst().getFill());
                            assertTrue(card.getWidth() <= 640.1);
                            assertFalse(node("login-status").isManaged());
                            assertEquals(
                                    Color.web("#0f766e"),
                                    ((Button) node("login"))
                                            .getBackground()
                                            .getFills()
                                            .getFirst()
                                            .getFill());
                            return null;
                        });
                text("login-nim", "001");
                text("login-password", "salah");
                click("login");
                assertTrue(label("login-status").contains("NIM atau password salah"));
                assertEquals("001", fx(() -> ((TextField) node("login-nim")).getText()));
                assertEquals("salah", fx(() -> ((PasswordField) node("login-password")).getText()));
                feedback("login-status", suffix);
                tab("auth-tabs", 1);
                text("register-nim", "002");
                text("register-name", "Pengguna Uji");
                text("register-password", "singkat");
                text("register-confirmation", "singkat");
                click("register");
                assertTrue(label("register-status").contains("8 karakter"));
                assertEquals(
                        "Pengguna Uji", fx(() -> ((TextField) node("register-name")).getText()));
                feedback("register-status", suffix);
                tab("auth-tabs", 0);
                text("login-password", "sandi-uji-lokal");
                click("login");
                await(() -> stage.getScene().lookup("#workspace-tabs") != null);
                idle();
                assertNotNull(fx(() -> stage.getScene().lookup(".tb-workspace")));
                assertTrue(
                        fx(() -> stage.getScene().getRoot().getStyleClass().contains("tb-theme")));
                click("open-learning");
                assertEquals(
                        0,
                        fx(
                                () ->
                                        ((TabPane) node("main-tabs"))
                                                .getSelectionModel()
                                                .getSelectedIndex()));
                click("open-simulation");
                assertEquals(
                        1,
                        fx(
                                () ->
                                        ((TabPane) node("main-tabs"))
                                                .getSelectionModel()
                                                .getSelectedIndex()));
                click("logout"); openLogin();
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
