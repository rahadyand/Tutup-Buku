package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.PixelFormat;
import javafx.stage.*;

import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

import javax.imageio.ImageIO;

/** Helper pengujian JavaFX M4/M5; tidak masuk distribusi aplikasi. */
abstract class FxTestSupport {
    @TempDir Path directory;
    Stage stage;
    TutupBukuApplication application;

    <T> T fx(Callable<T> action) throws Exception {
        var f = new CompletableFuture<T>();
        Platform.runLater(
                () -> {
                    try {
                        f.complete(action.call());
                    } catch (Throwable error) {
                        f.completeExceptionally(error);
                    }
                });
        return f.get(15, TimeUnit.SECONDS);
    }

    javafx.scene.Node node(String id) {
        var node = stage.getScene().lookup("#" + id);
        assertNotNull(node, id);
        return node;
    }

    void await(BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!fx(condition::getAsBoolean)) {
            if (System.nanoTime() > end) fail("UI timeout");
            Thread.sleep(40);
        }
    }

    void idle() throws Exception {
        await(() -> !stage.getScene().getRoot().isDisable());
    }

    void click(String id) throws Exception {
        idle();
        fx(
                () -> {
                    assertFalse(node(id).isDisabled(), id);
                    ((ButtonBase) node(id)).fire();
                    return null;
                });
        idle();
    }

    void text(String id, String text) throws Exception {
        await(() -> stage.getScene().lookup("#" + id) != null);
        fx(
                () -> {
                    ((TextInputControl) node(id)).setText(text);
                    return null;
                });
    }

    String label(String id) throws Exception {
        return fx(() -> ((Label) node(id)).getText());
    }

    void tab(String id, int index) throws Exception {
        fx(
                () -> {
                    ((TabPane) node(id)).getSelectionModel().select(index);
                    return null;
                });
        idle();
    }

    void combo(String id, Object value) throws Exception {
        if (id.equals("scenario-mode") || id.equals("actual-mode")) {
            if (fx(() -> java.util.Objects.equals(((ComboBox<?>) node(id)).getValue(), value)))
                return;
            dialog(
                    () -> ((ComboBox<Object>) node(id)).setValue(value),
                    ButtonBar.ButtonData.OK_DONE);
            return;
        }
        fx(
                () -> {
                    ((ComboBox<Object>) node(id)).setValue(value);
                    return null;
                });
        idle();
    }

    void select(String id, int index) throws Exception {
        fx(
                () -> {
                    ((ListView<?>) node(id)).getSelectionModel().select(index);
                    return null;
                });
        idle();
    }

    void scroll(String id, double position) throws Exception {
        fx(
                () -> {
                    javafx.scene.Node parent = node(id);
                    while (parent != null && !(parent instanceof ScrollPane))
                        parent = parent.getParent();
                    assertNotNull(parent);
                    ((ScrollPane) parent).setVvalue(position);
                    return null;
                });
    }

    void dialog(Runnable action, ButtonBar.ButtonData response) throws Exception {
        fx(
                () -> {
                    Platform.runLater(action);
                    return null;
                });
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
                    var type =
                            pane.getButtonTypes().stream()
                                    .filter(b -> b.getButtonData() == response)
                                    .findFirst()
                                    .orElseThrow();
                    ((Button) pane.lookupButton(type)).fire();
                    return null;
                });
        idle();
    }

    void screenshot(String name) throws Exception {
        fx(
                () -> {
                    var root = stage.getScene().getRoot();
                    root.applyCss();
                    root.layout();
                    var snapshot = root.snapshot(null, null);
                    int w = (int) snapshot.getWidth(), h = (int) snapshot.getHeight();
                    int[] pixels = new int[w * h];
                    snapshot.getPixelReader()
                            .getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
                    var image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    image.setRGB(0, 0, w, h, pixels, 0, w);
                    Files.createDirectories(Path.of("target/verification"));
                    ImageIO.write(image, "png", Path.of("target/verification", name).toFile());
                    return null;
                });
    }

    void login() throws Exception {
        await(() -> stage.getScene().lookup("#login-nim") != null);
        text("login-nim", "001");
        text("login-password", "sandi-uji-lokal");
        click("login");
        click("open-simulation");
    }
}
