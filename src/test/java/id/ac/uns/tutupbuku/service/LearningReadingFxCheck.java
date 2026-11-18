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

/** Alur nyata memakai view produksi; seluruh data fiktif berada di @TempDir. */
class LearningReadingFxCheck extends FxTestSupport {
    private void ready() throws Exception {
        await(
                () ->
                        stage.getScene().lookup("#material-reader") != null
                                && ((MarkdownReader) node("material-reader").getParent()).ready());
    }

    private void readerLayout() throws Exception {
        fx(
                () -> {
                    stage.getScene().getRoot().applyCss();
                    stage.getScene().getRoot().layout();
                    return null;
                });
        // WebView memperbarui viewport pada pulse berikutnya, sesudah layout Scene.
        await(
                () -> {
                    var web = (WebView) node("material-reader");
                    return Math.abs(web.getWidth() - web.getParent().getLayoutBounds().getWidth())
                                    < 2
                            && Math.abs(
                                            ((Number)
                                                                    web.getEngine()
                                                                            .executeScript(
                                                                                    "window.innerWidth"))
                                                            .doubleValue()
                                                    - web.getWidth())
                                    < 20;
                });
    }

    private void mode(String action, String expected) throws Exception {
        click(action);
        await(() -> stage.getScene().lookup("#" + expected) != null);
    }

    private long selected() {
        return ((Material)
                        ((ListView<?>) node("learning-materials"))
                                .getSelectionModel()
                                .getSelectedItem())
                .id();
    }

    private double ratio() {
        return ((MarkdownReader) node("material-reader").getParent()).scrollRatio();
    }

    private void position(double value) throws Exception {
        fx(
                () -> {
                    ((WebView) node("material-reader"))
                            .getEngine()
                            .executeScript(
                                    "window.scrollTo(0,(document.documentElement.scrollHeight-window.innerHeight)*"
                                            + value
                                            + ")");
                    return null;
                });
    }

    private void trigger(SqliteDatabase database, String sql) {
        database.read(
                connection -> {
                    try (var statement = connection.createStatement()) {
                        statement.execute(sql);
                    }
                    return null;
                });
    }

    @Test
    @Timeout(value = 150, unit = TimeUnit.SECONDS)
    void readEditManageShareDraftGuardNotesAndResumeAtSupportedSizes() throws Exception {
        Path file = directory.resolve("reading.db");
        long first;
        try (var db = new SqliteDatabase(file)) {
            var repo = new SqliteRepository(db);
            repo.createAccount(
                    "001",
                    "Pembaca Uji",
                    new PasswordHasher().hash("sandi-uji-lokal".toCharArray()));
            var semester =
                    repo.saveSemester("001", new Semester(0, 3, "2026/2027", SemesterPeriod.ODD));
            var course =
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
            var session = new UserSession();
            session.login("001");
            var learning =
                    new LearningService(
                            new SqliteLearningRepository(db), session, new AssetStore(directory));
            learning.initializeStarter(course.id());
            var folder = learning.folders(LearningLocation.Kind.CHAPTER, course.id()).getFirst();
            var material = learning.materials(folder.location()).getFirst();
            first = material.id();
            learning.save(
                    new Material(
                            first,
                            material.location(),
                            "Konsep dasar OOP",
                            "# Konsep dasar OOP\n\n"
                                    + "Memahami objek dan tanggung jawabnya.\n\n"
                                    + "## Membaca materi\n\n"
                                    + "### Contoh\n\nObjek menyimpan keadaan dan perilaku.\n\n```java\nclass Student { String name; }\n```\n\n"
                                            .repeat(45),
                            "Catatan kuliah PBO",
                            material.position(),
                            material.revision(),
                            material.updatedAt()),
                    "",
                    learning.open(first).noteRevision());
        }
        var started = new CompletableFuture<Void>();
        Platform.startup(
                () -> {
                    Platform.setImplicitExit(false);
                    started.complete(null);
                });
        started.get(10, TimeUnit.SECONDS);
        try (var inspection = new SqliteDatabase(file)) {
            var repo = new SqliteLearningRepository(inspection);
            fx(
                    () -> {
                        stage = new Stage();
                        application = new TutupBukuApplication(file);
                        application.start(stage);
                        return null;
                    });
            login();
            click("open-learning");
            select("learning-folders", 0);
            select("learning-materials", 0);
            ready();
            assertEquals(first, fx(this::selected));
            assertNull(fx(() -> stage.getScene().lookup("#material-tabs")));
            assertNull(fx(() -> stage.getScene().lookup("#learning-new-material")));
            assertNotNull(fx(() -> node("edit-material")));
            // Reader tidak dibungkus ScrollPane lain: satu area scroll utama.
            fx(
                    () -> {
                        for (Node n = node("material-reader").getParent();
                                n != null;
                                n = n.getParent()) assertFalse(n instanceof ScrollPane);
                        return null;
                    });
            position(0.62);
            click("toggle-learning-catalog");
            await(
                    () ->
                            stage.getScene().lookup("#learning-catalog") == null
                                    && Math.abs(ratio() - 0.62) < 0.03);
            click("toggle-learning-catalog");
            await(
                    () ->
                            stage.getScene().lookup("#learning-catalog") != null
                                    && Math.abs(ratio() - 0.62) < 0.03);
            assertEquals(first, fx(this::selected));
            mode("edit-material", "material-tabs");
            assertEquals(
                    "Konsep dasar OOP", fx(() -> ((TextField) node("material-title")).getText()));
            mode("read-material", "material-reader");
            ready();
            assertEquals(0.62, fx(this::ratio), 0.03);

            long revision = repo.find("001", first).orElseThrow().material().revision();
            fx(
                    () -> {
                        ((TitledPane) node("material-notes-pane")).setExpanded(true);
                        return null;
                    });
            text("material-note", "Catatan dari mode Baca");
            await(
                    () ->
                            ((Label) node("material-save-status"))
                                    .getText()
                                    .equals("Catatan tersimpan."));
            assertEquals("Catatan dari mode Baca", repo.find("001", first).orElseThrow().note());
            assertEquals(revision, repo.find("001", first).orElseThrow().material().revision());
            assertEquals(0.62, fx(this::ratio), 0.03);
            click("material-completed");
            assertTrue(repo.find("001", first).orElseThrow().completed());

            // Batal mengembalikan mode/pilihan yang sama, tanpa loop dialog.
            text("material-note", "Catatan belum tersimpan");
            dialog(
                    () -> ((Button) node("manage-learning")).fire(),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertNotNull(fx(() -> node("edit-material")));
            assertEquals(first, fx(this::selected));
            assertEquals(
                    "Catatan belum tersimpan",
                    fx(() -> ((TextArea) node("material-note")).getText()));
            trigger(
                    inspection,
                    "CREATE TRIGGER fail_note BEFORE UPDATE OF note ON learning_state BEGIN SELECT"
                            + " RAISE(ABORT,'failure'); END");
            dialog(() -> ((Button) node("manage-learning")).fire(), ButtonBar.ButtonData.YES);
            assertTrue(label("material-save-status").contains("Gagal menyimpan"));
            assertNull(fx(() -> stage.getScene().lookup("#learning-management")));
            assertEquals(
                    "Catatan belum tersimpan",
                    fx(() -> ((TextArea) node("material-note")).getText()));
            dialog(
                    () -> ((ListView<?>) node("learning-materials")).getSelectionModel().select(1),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(first, fx(this::selected));
            dialog(
                    () -> ((ComboBox<String>) node("learning-path")).setValue("Peminatan"),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals("Kuliah", fx(() -> ((ComboBox<?>) node("learning-path")).getValue()));
            trigger(inspection, "DROP TRIGGER fail_note");
            fx(
                    () -> {
                        ((TitledPane) node("material-notes-pane")).setExpanded(true);
                        return null;
                    });
            click("save-note");
            assertEquals("Catatan belum tersimpan", repo.find("001", first).orElseThrow().note());
            text("material-note", "Draf yang akan dibuang");
            // Buang hanya catatan draf; isi dan catatan tersimpan tetap ada.
            dialog(() -> ((Button) node("manage-learning")).fire(), ButtonBar.ButtonData.NO);
            await(() -> stage.getScene().lookup("#learning-management") != null);
            assertEquals("Catatan belum tersimpan", repo.find("001", first).orElseThrow().note());
            for (String id :
                    new String[] {
                        "learning-add-folder",
                        "learning-rename-folder",
                        "learning-delete-folder",
                        "learning-folder-up",
                        "learning-folder-down",
                        "learning-new-material",
                        "manage-edit-material",
                        "learning-delete-material",
                        "learning-material-up",
                        "learning-material-down",
                        "initialize-starter",
                        "learning-refresh"
                    }) assertNotNull(fx(() -> node(id)));
            mode("manage-edit-material", "material-tabs");
            text(
                    "material-markdown",
                    "# Konsep dasar OOP\n\n"
                            + "## Memahami objek\n\n"
                            + "Objek menyimpan keadaan dan perilaku.\n\n"
                            + "| Konsep | Makna |\n"
                            + "| --- | --- |\n"
                            + "| Class | Rancangan objek |\n"
                            + "| Object | Instance dari class |\n\n"
                            + "```java\n"
                            + "class Student { String name; }\n"
                            + "```\n\n"
                            + "## Ringkasan\n\n"
                            + "Pisahkan tanggung jawab setiap objek.\n\n"
                            + "Bacaan revisi baru.\n\n".repeat(150));
            click("save-material");
            mode("read-material", "material-reader");
            ready();
            assertEquals(0, fx(this::ratio), 0.01);
            position(0.45);
            click("open-simulation");
            click("open-learning");
            click("resume-reading");
            ready();
            assertEquals(0.45, fx(this::ratio), 0.03);

            // Simpan catatan berhasil sebelum masuk Kelola, menggunakan draf yang sama.
            text("material-note", "Catatan disimpan sebelum kelola");
            dialog(() -> ((Button) node("manage-learning")).fire(), ButtonBar.ButtonData.YES);
            await(() -> stage.getScene().lookup("#learning-management") != null);
            assertEquals(
                    "Catatan disimpan sebelum kelola",
                    repo.find("001", first).orElseThrow().note());
            mode("manage-learning", "material-reader");
            ready();
            assertEquals(0.45, fx(this::ratio), 0.03);
            fx(
                    () -> {
                        ((TitledPane) node("material-notes-pane")).setExpanded(false);
                        return null;
                    });

            trigger(
                    inspection,
                    "CREATE TRIGGER fail_bookmark BEFORE INSERT ON last_reading BEGIN SELECT"
                            + " RAISE(ABORT,'failure'); END");
            click("manage-learning");
            assertNull(fx(() -> stage.getScene().lookup("#learning-management")));
            assertTrue(label("reading-status").contains("Posisi baca gagal"));
            trigger(inspection, "DROP TRIGGER fail_bookmark");
            mode("manage-learning", "learning-management");
            mode("manage-learning", "material-reader");
            ready();
            assertEquals(0.45, fx(this::ratio), 0.03);
            // Normalisasi judul yang sama tidak membuat posisi baca kehilangan revisi.
            mode("edit-material", "material-tabs");
            text("material-title", "  Konsep dasar OOP  ");
            click("save-material");
            mode("read-material", "material-reader");
            ready();
            assertEquals(0.45, fx(this::ratio), 0.03);
            position(0);
            int[][] sizes =
                    System.getProperty("glass.win.uiScale") == null
                            ? new int[][] {{1366, 768}, {1024, 768}, {760, 480}}
                            : new int[][] {{1366, 768}};
            for (int[] size : sizes) {
                fx(
                        () -> {
                            stage.setWidth(size[0] / stage.getOutputScaleX());
                            stage.setHeight(size[1] / stage.getOutputScaleY());
                            return null;
                        });
                await(
                        () ->
                                Math.abs(stage.getWidth() * stage.getOutputScaleX() - size[0]) < 2
                                        && stage.getScene().getWidth() * stage.getOutputScaleX()
                                                >= size[0] - 64
                                        && stage.getScene().getHeight() * stage.getOutputScaleY()
                                                >= size[1] - 96
                                        && stage.getScene().getWidth() * stage.getOutputScaleX()
                                                < size[0]
                                        && stage.getScene().getHeight() * stage.getOutputScaleY()
                                                < size[1]);
                String suffix =
                        System.getProperty("glass.win.uiScale", "native")
                                + "-"
                                + size[0]
                                + "x"
                                + size[1]
                                + ".png";
                ready();
                readerLayout();
                fx(
                        () -> {
                            stage.getScene().getRoot().applyCss();
                            stage.getScene().getRoot().layout();
                            assertTrue(
                                    node("material-reader").getBoundsInLocal().getHeight() >= 80);
                            return null;
                        });
                screenshot("learning-read-" + suffix);
                click("toggle-learning-catalog");
                await(() -> stage.getScene().lookup("#learning-catalog") == null);
                readerLayout();
                screenshot("learning-hidden-" + suffix);
                click("toggle-learning-catalog");
                await(() -> stage.getScene().lookup("#learning-catalog") != null);
                assertEquals(first, fx(this::selected));
                mode("edit-material", "material-tabs");
                screenshot("learning-edit-" + suffix);
                mode("read-material", "material-reader");
                ready();
                mode("manage-learning", "learning-management");
                screenshot("learning-manage-" + suffix);
                mode("manage-learning", "material-reader");
                ready();
            }
        } catch (Throwable error) {
            screenshot("learning-reading-failure.png");
            throw error;
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
