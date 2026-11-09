package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.ui.*;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.PixelFormat;
import javafx.scene.web.WebView;
import javafx.stage.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

class MilestoneThreeFxCheck {
    @TempDir Path directory;
    private Stage stage;
    private TutupBukuApplication application;
    private <T> T fx(Callable<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> { try { result.complete(action.call()); } catch (Throwable error) { result.completeExceptionally(error); } });
        return result.get(15, TimeUnit.SECONDS);
    }
    private NodeAccess nodes() { return new NodeAccess(); }
    private class NodeAccess {
        javafx.scene.Node get(String id) { var node = stage.getScene().lookup("#" + id); assertNotNull(node, id); return node; }
        TextInputControl text(String id) { return (TextInputControl) get(id); }
    }
    private void await(BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!fx(condition::getAsBoolean)) { if (System.nanoTime() > end) fail("Menunggu UI melebihi batas waktu"); Thread.sleep(40); }
    }
    private void idle() throws Exception { await(() -> !stage.getScene().getRoot().isDisable()); }
    private void click(String id) throws Exception {
        idle();
        await(() -> !nodes().get(id).isDisabled());
        fx(() -> { ((ButtonBase) nodes().get(id)).fire(); return null; }); idle();
    }
    private void text(String id, String text) throws Exception { fx(() -> { nodes().text(id).setText(text); return null; }); }
    private String text(String id) throws Exception { return fx(() -> nodes().text(id).getText()); }
    private String label(String id) throws Exception { return fx(() -> ((Label) nodes().get(id)).getText()); }
    private void select(String id, int index) throws Exception { fx(() -> { ((ListView<?>) nodes().get(id)).getSelectionModel().select(index); return null; }); idle(); }
    private void tab(String id, int index) throws Exception { fx(() -> { ((TabPane) nodes().get(id)).getSelectionModel().select(index); return null; }); idle(); }
    private void readReady() throws Exception {
        await(() -> ((MarkdownReader) nodes().get("material-reader").getParent()).ready());
    }
    private void dialog(Runnable action, ButtonBar.ButtonData response) throws Exception {
        fx(() -> { Platform.runLater(action); return null; });
        await(() -> Window.getWindows().stream().anyMatch(window -> window.isShowing() && window.getScene().getRoot() instanceof DialogPane));
        fx(() -> {
            var pane = (DialogPane) Window.getWindows().stream().filter(window -> window.isShowing() && window.getScene().getRoot() instanceof DialogPane).findFirst().orElseThrow().getScene().getRoot();
            var type = pane.getButtonTypes().stream().filter(button -> button.getButtonData() == response).findFirst().orElseThrow();
            ((Button) pane.lookupButton(type)).fire(); return null;
        }); idle();
    }
    private void screenshot(String name) throws Exception {
        fx(() -> {
            var root = stage.getScene().getRoot(); root.applyCss(); root.layout(); var snapshot = root.snapshot(null, null);
            int width = (int) snapshot.getWidth(), height = (int) snapshot.getHeight(); int[] pixels = new int[width * height];
            snapshot.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
            var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); image.setRGB(0, 0, width, height, pixels, 0, width);
            Path output = Path.of("target", "verification"); Files.createDirectories(output); ImageIO.write(image, "png", output.resolve(name).toFile()); return null;
        });
    }
    private void login() throws Exception {
        await(() -> stage.getScene().lookup("#login-nim") != null);
        text("login-nim", "001"); text("login-password", "sandi-uji-lokal"); click("login");
    }
    @Test void learningEditorAutosaveFailureGuardProgressResumeAndReopen() throws Exception {
        Path file = directory.resolve("ui.db");
        try (var database = new SqliteDatabase(file)) {
            var repository = new SqliteRepository(database);
            repository.createAccount("001", "Pengguna Belajar", new PasswordHasher().hash("sandi-uji-lokal".toCharArray()));
            var semester = repository.saveSemester("001", new Semester(0, 3, "2026/2027", SemesterPeriod.ODD));
            repository.saveCourse("001", new Course(0, semester.id(), "PBO", "", TestData.n("3"), Attendance.assumedMet(), "75%", 0));
        }
        CompletableFuture<Void> started = new CompletableFuture<>();
        Platform.startup(() -> { Platform.setImplicitExit(false); started.complete(null); }); started.get(10, TimeUnit.SECONDS);
        try {
            fx(() -> { stage = new Stage(); application = new TutupBukuApplication(file); application.start(stage); return null; }); idle(); login();
            dialog(() -> ((Button) nodes().get("initialize-starter")).fire(), ButtonBar.ButtonData.OK_DONE);
            select("learning-folders", 0); select("learning-materials", 0); readReady();
            assertFalse(fx(() -> ((CheckBox) nodes().get("material-completed")).isSelected()));
            click("material-completed"); assertTrue(label("learning-progress").contains("1/3"));
            long materialId = fx(() -> ((Material) ((ListView<?>) nodes().get("learning-materials")).getSelectionModel().getSelectedItem()).id());
            tab("material-tabs", 0); text("material-title", "Materi revisi");
            String content = "# Bacaan\n\n| A | B |\n| --- | --- |\n| 1 | 2 |\n\n```java\nSystem.exit(0);\n```\n\n<script>window.pwned=1</script>\n\n" + "Paragraf belajar.\n\n".repeat(100);
            text("material-markdown", content); text("material-note", "Catatan pribadi tersimpan"); click("save-material");
            assertEquals("Tersimpan", label("material-save-status"));
            text("material-markdown", content + "\nAutosave berhasil.");
            await(() -> ((Label) nodes().get("material-save-status")).getText().equals("Tersimpan")); idle();
            try (var inspection = new SqliteDatabase(file)) {
                var repository = new SqliteLearningRepository(inspection);
                assertTrue(repository.find("001", materialId).orElseThrow().material().markdown().contains("Autosave berhasil"));
                inspection.read(connection -> { try (var statement = connection.createStatement()) { statement.execute("CREATE TRIGGER fail_material BEFORE UPDATE OF markdown ON materials BEGIN SELECT RAISE(ABORT,'failure'); END"); } return null; });
                text("material-markdown", "Isian yang gagal disimpan"); click("save-material");
                assertTrue(label("material-save-status").contains("Gagal menyimpan")); assertEquals("Isian yang gagal disimpan", text("material-markdown"));
                dialog(() -> ((TabPane) nodes().get("workspace-tabs")).getSelectionModel().select(1), ButtonBar.ButtonData.CANCEL_CLOSE);
                assertEquals(0, fx(() -> ((TabPane) nodes().get("workspace-tabs")).getSelectionModel().getSelectedIndex()));
                dialog(() -> ((TabPane) nodes().get("workspace-tabs")).getSelectionModel().select(1), ButtonBar.ButtonData.YES);
                assertEquals(0, fx(() -> ((TabPane) nodes().get("workspace-tabs")).getSelectionModel().getSelectedIndex()));
                assertEquals("Isian yang gagal disimpan", text("material-markdown"));
                dialog(() -> ((TabPane) nodes().get("workspace-tabs")).getSelectionModel().select(1), ButtonBar.ButtonData.NO);
                assertTrue(repository.find("001", materialId).orElseThrow().material().markdown().contains("Autosave berhasil"));
                inspection.read(connection -> { try (var statement = connection.createStatement()) { statement.execute("DROP TRIGGER fail_material"); } return null; });
            }
            tab("workspace-tabs", 0); select("learning-folders", 0);
            // Urutan tidak berubah; materi pertama tetap ID yang sama walaupun judulnya diedit.
            select("learning-materials", 0); readReady();
            assertEquals("Catatan pribadi tersimpan", text("material-note"));
            var bad = fx(() -> ((WebView) nodes().get("material-reader")).getEngine().executeScript("typeof window.pwned")); assertEquals("undefined", bad);
            fx(() -> { ((WebView) nodes().get("material-reader")).getEngine().executeScript("window.scrollTo(0,(document.documentElement.scrollHeight-window.innerHeight)*0.65)"); return null; });
            click("open-simulation"); click("open-learning"); click("resume-reading"); readReady();
            assertEquals(0.65, fx(() -> ((MarkdownReader) nodes().get("material-reader").getParent()).scrollRatio()), 0.03);
            screenshot("m3-learning.png");
            dialog(() -> ((Button) nodes().get("delete-note")).fire(), ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals("Catatan pribadi tersimpan", text("material-note"));
            dialog(() -> ((Button) nodes().get("delete-note")).fire(), ButtonBar.ButtonData.OK_DONE);
            assertEquals("", text("material-note"));
            click("learning-material-down"); assertTrue(fx(() -> ((CheckBox) nodes().get("material-completed")).isSelected()));
            dialog(() -> ((Button) nodes().get("learning-delete-material")).fire(), ButtonBar.ButtonData.OK_DONE);
            click("resume-reading"); assertTrue(label("learning-status").contains("dihapus"));
            click("learning-new-material"); text("material-title", "Materi baru sendiri"); text("material-markdown", "# Milik sendiri\n\n## Ringkasan\nManual"); text("material-note", "Catatan setelah restart"); click("save-material"); click("material-completed");
            click("logout"); login(); select("learning-folders", 0);
            fx(() -> { var list = (ListView<Material>) nodes().get("learning-materials"); list.getSelectionModel().select(list.getItems().stream().filter(material -> material.title().equals("Materi baru sendiri")).findFirst().orElseThrow()); return null; }); idle(); readReady();
            assertEquals("Catatan setelah restart", text("material-note")); assertTrue(fx(() -> ((CheckBox) nodes().get("material-completed")).isSelected()));
            click("logout"); fx(() -> { stage.close(); application.stop(); return null; });
            fx(() -> { stage = new Stage(); application = new TutupBukuApplication(file); application.start(stage); return null; }); idle(); login();
            assertTrue(fx(() -> nodes().get("initialize-starter").isDisable()));
            fx(() -> { ((ComboBox<String>) nodes().get("learning-path")).setValue("Peminatan"); return null; }); idle();
            select("learning-folders", 0); assertEquals(2, fx(() -> ((ListView<?>) nodes().get("learning-materials")).getItems().size()));
            select("learning-materials", 0); readReady(); tab("material-tabs", 0);
            text("material-note", "Catatan tersimpan saat jendela ditutup");
            dialog(() -> javafx.event.Event.fireEvent(stage, new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)), ButtonBar.ButtonData.CANCEL_CLOSE);
            assertTrue(fx(stage::isShowing)); assertEquals("Catatan tersimpan saat jendela ditutup", text("material-note"));
            dialog(() -> javafx.event.Event.fireEvent(stage, new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)), ButtonBar.ButtonData.YES);
            await(() -> !stage.isShowing());
        } catch (Throwable error) {
            screenshot("m3-failure.png"); throw error;
        } finally {
            if (stage != null) fx(() -> { stage.close(); application.stop(); return null; });
            Platform.exit();
        }
    }
}
