package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.ui.*;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.Path;
import java.util.concurrent.*;

import javax.imageio.ImageIO;

/** Kontrak layar modern; seluruh akun/aset/DB fiktif di @TempDir. */
class ModernWorkspaceFxCheck extends FxTestSupport {
    private String suffix;

    private void layout() {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
    }

    private void capture(String screen) throws Exception {
        String name = "modern-" + screen + "-" + suffix + ".png";
        screenshot(name);
        var png = ImageIO.read(Path.of("target/verification", name).toFile());
        assertNotNull(png);
        assertEquals(fx(() -> (int) Math.ceil(stage.getScene().getWidth())), png.getWidth());
        assertEquals(fx(() -> (int) Math.ceil(stage.getScene().getHeight())), png.getHeight());
        System.out.printf("MODERN %s PNG=%dx%d%n", name, png.getWidth(), png.getHeight());
    }

    private void reachable(String id) throws Exception {
        fx(
                () -> {
                    layout();
                    Node target = node(id);
                    assertTrue(target.isVisible(), id);
                    for (Node parent = target.getParent();
                            parent != null;
                            parent = parent.getParent()) {
                        if (!(parent instanceof ScrollPane scroll)) continue;
                        var bounds =
                                scroll.getContent()
                                        .sceneToLocal(
                                                target.localToScene(target.getLayoutBounds()));
                        double dy =
                                scroll.getContent().getLayoutBounds().getHeight()
                                        - scroll.getViewportBounds().getHeight();
                        double dx =
                                scroll.getContent().getLayoutBounds().getWidth()
                                        - scroll.getViewportBounds().getWidth();
                        if (dy > 0) scroll.setVvalue(Math.clamp((bounds.getMinY() - 8) / dy, 0, 1));
                        if (dx > 0) scroll.setHvalue(Math.clamp((bounds.getMinX() - 8) / dx, 0, 1));
                        layout();
                        var viewport = scroll.lookup(".viewport");
                        var visible = viewport.localToScene(viewport.getLayoutBounds());
                        var actual = target.localToScene(target.getLayoutBounds());
                        assertTrue(actual.getMinY() >= visible.getMinY() - 1, id + " top");
                        assertTrue(actual.getMaxY() <= visible.getMaxY() + 1, id + " bottom");
                        assertTrue(actual.getMinX() >= visible.getMinX() - 1, id + " left");
                        assertTrue(actual.getMaxX() <= visible.getMaxX() + 1, id + " right");
                    }
                    var actual = target.localToScene(target.getLayoutBounds());
                    assertTrue(
                            actual.getMinY() >= 0
                                    && actual.getMaxY() <= stage.getScene().getHeight() + 1,
                            id);
                    assertTrue(
                            actual.getMinX() >= 0
                                    && actual.getMaxX() <= stage.getScene().getWidth() + 1,
                            id);
                    return null;
                });
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void navigationBranchesReaderAndActionsRemainReachableAtSupportedSizes() throws Exception {
        Path file = directory.resolve("modern.db");
        try (var db = new SqliteDatabase(file)) {
            var repo = new SqliteRepository(db);
            repo.createAccount(
                    "001",
                    "Mahasiswa Uji",
                    new PasswordHasher().hash("sandi-uji-lokal".toCharArray()));
            var semester =
                    repo.saveSemester("001", new Semester(0, 3, "2026/2027", SemesterPeriod.ODD));
            repo.saveCourse(
                    "001",
                    new Course(
                            0,
                            semester.id(),
                            "Pemrograman Berorientasi Objek",
                            "PBO",
                            TestData.n("3"),
                            Attendance.assumedMet(),
                            "75%",
                            0));
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
            login();
            click("open-learning");
            dialog(
                    () -> ((Button) node("initialize-starter")).fire(),
                    ButtonBar.ButtonData.OK_DONE);
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
                            return null;
                        });
                await(
                        () ->
                                Math.abs(stage.getWidth() * stage.getOutputScaleX() - size[0]) <= 2
                                        && Math.abs(
                                                        stage.getHeight() * stage.getOutputScaleY()
                                                                - size[1])
                                                <= 2
                                        && stage.getScene().getWidth() * stage.getOutputScaleX()
                                                < size[0]
                                        && stage.getScene().getWidth() * stage.getOutputScaleX()
                                                >= size[0] - 64
                                        && stage.getScene().getHeight() * stage.getOutputScaleY()
                                                >= size[1] - 96
                                        && stage.getScene().getHeight() * stage.getOutputScaleY()
                                                < size[1]);
                click("open-learning");
                fx(
                        () -> {
                            layout();
                            for (String id : new String[] {"open-learning", "open-simulation"}) {
                                Button action = (Button) node(id);
                                assertTrue(
                                        action.getWidth() >= action.prefWidth(-1) - 1,
                                        id + " label utuh");
                            }
                            return null;
                        });
                select("learning-folders", 0);
                select("learning-materials", 0);
                await(() -> ((MarkdownReader) node("material-reader").getParent()).ready());
                fx(
                        () -> {
                            var web = ((WebView) node("material-reader")).getEngine();
                            assertFalse(web.isJavaScriptEnabled());
                            assertTrue(web.getUserStyleSheetLocation().endsWith("reader.css"));
                            assertEquals(
                                    "14px",
                                    web.executeScript("getComputedStyle(document.body).fontSize"));
                            var branch = (TitledPane) node("learning-folder-branch");
                            var selection =
                                    ((ListView<?>) node("learning-folders"))
                                            .getSelectionModel()
                                            .getSelectedItem();
                            branch.setExpanded(false);
                            layout();
                            assertFalse(branch.isExpanded());
                            branch.setExpanded(true);
                            layout();
                            assertSame(
                                    selection,
                                    ((ListView<?>) node("learning-folders"))
                                            .getSelectionModel()
                                            .getSelectedItem());
                            return null;
                        });
                reachable("learning-refresh");
                reachable("learning-new-material");
                reachable("save-material");
                capture("learning");
                tab("material-tabs", 0);
                reachable("material-title");
                capture("editor");
                click("open-simulation");
                tab("grade-tabs", 0);
                reachable("save-actual-settings");
                reachable("save-component");
                capture("actual");
                tab("grade-tabs", 1);
                reachable("new-scenario");
                reachable("compare-scenarios");
                capture("scenario");
                tab("workspace-tabs", 2);
                reachable("save-profile");
                capture("profile");
                tab("workspace-tabs", 1);
                fx(
                        () -> {
                            ((ComboBox<?>) node("semester-editor-selector"))
                                    .getSelectionModel()
                                    .selectFirst();
                            return null;
                        });
                idle();
                reachable("view-courses");
                capture("semester");
                click("view-courses");
                reachable("save-course");
                reachable("delete-course");
                capture("courses");
                click("edit-semesters");
            }
            tab("workspace-tabs", 2);
            text("profile-name", "Draf uji dialog");
            fx(
                    () -> {
                        Platform.runLater(() -> ((Button) node("open-learning")).fire());
                        return null;
                    });
            await(
                    () ->
                            javafx.stage.Window.getWindows().stream()
                                    .anyMatch(
                                            w ->
                                                    w.isShowing()
                                                            && w.getScene().getRoot()
                                                                    instanceof DialogPane));
            fx(
                    () -> {
                        var pane =
                                (DialogPane)
                                        javafx.stage.Window.getWindows().stream()
                                                .filter(
                                                        w ->
                                                                w.isShowing()
                                                                        && w.getScene().getRoot()
                                                                                instanceof
                                                                                DialogPane)
                                                .findFirst()
                                                .orElseThrow()
                                                .getScene()
                                                .getRoot();
                        pane.applyCss();
                        pane.layout();
                        assertTrue(pane.getStyleClass().contains("tb-theme"));
                        for (ButtonType type : pane.getButtonTypes()) {
                            Node button = pane.lookupButton(type);
                            var bounds = button.localToScene(button.getLayoutBounds());
                            assertTrue(
                                    bounds.getMinX() >= 0
                                            && bounds.getMaxX() <= pane.getScene().getWidth() + 1);
                            assertTrue(
                                    bounds.getMinY() >= 0
                                            && bounds.getMaxY() <= pane.getScene().getHeight() + 1);
                        }
                        var snapshot = pane.snapshot(null, null);
                        int w = (int) snapshot.getWidth(), h = (int) snapshot.getHeight();
                        int[] pixels = new int[w * h];
                        snapshot.getPixelReader()
                                .getPixels(
                                        0,
                                        0,
                                        w,
                                        h,
                                        javafx.scene.image.PixelFormat.getIntArgbInstance(),
                                        pixels,
                                        0,
                                        w);
                        var png =
                                new java.awt.image.BufferedImage(
                                        w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        png.setRGB(0, 0, w, h, pixels, 0, w);
                        ImageIO.write(
                                png,
                                "png",
                                Path.of("target/verification", "modern-dialog-" + suffix + ".png")
                                        .toFile());
                        var cancel =
                                pane.getButtonTypes().stream()
                                        .filter(
                                                t ->
                                                        t.getButtonData()
                                                                == ButtonBar.ButtonData
                                                                        .CANCEL_CLOSE)
                                        .findFirst()
                                        .orElseThrow();
                        assertTrue(((Button) pane.lookupButton(cancel)).isDefaultButton());
                        ((Button) pane.lookupButton(cancel)).fire();
                        return null;
                    });
            assertEquals("Draf uji dialog", fx(() -> ((TextField) node("profile-name")).getText()));
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
