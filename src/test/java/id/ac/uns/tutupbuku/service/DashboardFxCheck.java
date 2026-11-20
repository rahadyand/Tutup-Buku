package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.*;

/** Parcours aplikasi produksi dengan database sementara dan Clock WIB yang dapat digeser. */
class DashboardFxCheck extends FxTestSupport {
    private final String scale = System.getProperty("glass.win.uiScale", "native");

    private void present(String id) throws Exception {
        await(() -> stage.getScene().lookup("#" + id) != null);
        idle();
    }

    private void home() throws Exception {
        tab("main-tabs", 4);
        present("today-refresh");
        idle();
    }

    private void signIn(String nim) throws Exception {
        openLogin();
        present("login-nim");
        text("login-nim", nim);
        text("login-password", "sandi-uji-lokal");
        click("login");
        present("today-dashboard");
        idle();
        assertEquals(
                4, fx(() -> ((TabPane) node("main-tabs")).getSelectionModel().getSelectedIndex()));
    }

    private void shot(String name) throws Exception {
        shot(name, null);
    }

    private void shot(String name, Double position) throws Exception {
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
        if (position != null)
            fx(
                    () -> {
                        var scroll = (ScrollPane) node("today-scroll");
                        scroll.setVvalue(position);
                        assertEquals(position, scroll.getVvalue(), 0.001);
                        return null;
                    });
        screenshot("dashboard-" + name + "-" + scale + ".png");
    }

    private String allText() throws Exception {
        return fx(
                () ->
                        stage.getScene().getRoot().lookupAll(".label").stream()
                                .map(n -> ((Label) n).getText())
                                .reduce("", (a, b) -> a + "\n" + b));
    }

    @Test
    @Timeout(240)
    void connectedDashboardAndHonestStates() throws Exception {
        var errors = new CopyOnWriteArrayList<Throwable>();
        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> {
                    errors.add(e);
                    e.printStackTrace();
                });
        var started = new CompletableFuture<Void>();
        Platform.startup(
                () -> {
                    Platform.setImplicitExit(false);
                    started.complete(null);
                });
        started.get(10, TimeUnit.SECONDS);
        try (var f = new TodayDashboardServiceTest.Fixture(directory.resolve("dashboard.db"))) {
            fx(
                    () -> {
                        stage = new Stage();
                        application =
                                new TutupBukuApplication(
                                        directory.resolve("dashboard.db"), f.clock);
                        application.start(stage);
                        stage.setWidth(1366 / stage.getOutputScaleX());
                        stage.setHeight(768 / stage.getOutputScaleY());
                        return null;
                    });
            signIn("002");
            assertTrue(allText().contains("Belum ada semester"));
            shot("no-semester");
            f.session.login("002");
            var empty = f.academic.saveSemesterForm(0, "1", "2026", true);
            click("logout");
            signIn("002");
            assertTrue(allText().contains("Belum ada mata kuliah"));
            assertTrue(label("today-gpa").contains("belum lengkap"));
            shot("no-courses");
            f.academic.saveCourseMetadata(0, empty.id(), "Mata kuliah tanpa data", "", "2");
            click("today-refresh");
            assertTrue(allText().contains("Belum ada materi"));
            assertTrue(allText().contains("Belum ada tugas/ujian terbuka"));
            shot("empty-course");
            click("logout");
            signIn("001");
            f.session.login("001");
            assertEquals(
                    f.active.id(),
                    fx(() -> ((ComboBox<Semester>) node("today-semester")).getValue().id()));
            assertTrue(allText().contains("Hari Ini · 2 item, 1 ditampilkan"));
            assertTrue(allText().contains("Sudah lewat"));
            assertTrue(allText().contains("1/2 selesai"));
            assertTrue(label("today-gpa").contains("belum lengkap"));
            shot("mixed-1366", 0.0);
            fx(
                    () -> {
                        stage.setWidth(1024 / stage.getOutputScaleX());
                        stage.setHeight(768 / stage.getOutputScaleY());
                        return null;
                    });
            shot("mixed-1024", 0.0);
            fx(
                    () -> {
                        stage.setWidth(760);
                        stage.setHeight(480);
                        return null;
                    });
            shot("mixed-minimum", 0.0);
            var overflowing =
                    fx(
                            () ->
                                    node("today-dashboard").lookupAll(".button").stream()
                                            .filter(
                                                    n ->
                                                            n.getBoundsInParent().getWidth()
                                                                    > ((ScrollPane)
                                                                                            node(
                                                                                                    "today-scroll"))
                                                                                    .getViewportBounds()
                                                                                    .getWidth()
                                                                            + 1)
                                            .map(
                                                    n ->
                                                            n.getId()
                                                                    + ":"
                                                                    + n.getBoundsInParent()
                                                                            .getWidth())
                                            .toList());
            assertTrue(overflowing.isEmpty(), overflowing.toString());
            fx(
                    () -> {
                        ((ScrollPane) node("today-scroll")).setVvalue(0.55);
                        return null;
                    });
            shot("reading-course-minimum", 0.55);
            fx(
                    () -> {
                        ((ScrollPane) node("today-scroll")).setVvalue(1);
                        return null;
                    });
            shot("grades-minimum", 1.0);
            fx(
                    () -> {
                        stage.setWidth(1366 / stage.getOutputScaleX());
                        stage.setHeight(768 / stage.getOutputScaleY());
                        ((ScrollPane) node("today-scroll")).setVvalue(1);
                        return null;
                    });
            shot("incomplete-grades", 1.0);
            assertEquals(1, fx(() -> node("today-dashboard").lookupAll(".scroll-pane").size()));
            // Item yang sama, editor yang sama; batal dan gagal simpan tidak menavigasi.
            click("today-item-" + f.task.id());
            present("agenda-title");
            assertEquals(f.task.title(), fx(() -> ((TextField) node("agenda-title")).getText()));
            text("agenda-title", "Draf agenda dari Hari Ini");
            dialog(
                    () -> ((TabPane) node("main-tabs")).getSelectionModel().select(4),
                    ButtonBar.ButtonData.CANCEL_CLOSE);
            assertEquals(
                    3,
                    fx(() -> ((TabPane) node("main-tabs")).getSelectionModel().getSelectedIndex()));
            f.sql(
                    "CREATE TRIGGER dashboard_fail BEFORE UPDATE ON agenda_items BEGIN SELECT"
                            + " RAISE(ABORT,'uji simpan gagal'); END");
            dialog(
                    () -> ((TabPane) node("main-tabs")).getSelectionModel().select(4),
                    ButtonBar.ButtonData.YES);
            assertEquals(
                    3,
                    fx(() -> ((TabPane) node("main-tabs")).getSelectionModel().getSelectedIndex()));
            assertEquals(
                    "Draf agenda dari Hari Ini",
                    fx(() -> ((TextField) node("agenda-title")).getText()));
            f.sql("DROP TRIGGER dashboard_fail");
            dialog(
                    () -> ((TabPane) node("main-tabs")).getSelectionModel().select(4),
                    ButtonBar.ButtonData.YES);
            assertEquals("Draf agenda dari Hari Ini", f.agenda.open(f.task.id()).title());
            click("today-item-" + f.task.id());
            present("agenda-title");
            text("agenda-title", "Perubahan dibuang");
            dialog(
                    () -> ((TabPane) node("main-tabs")).getSelectionModel().select(4),
                    ButtonBar.ButtonData.NO);
            assertEquals("Draf agenda dari Hari Ini", f.agenda.open(f.task.id()).title());
            // Completion lama terlihat saat kembali, tanpa mengubah nilai.
            click("today-all-TODAY");
            present("agenda-items");
            assertEquals(
                    "Belum selesai",
                    fx(() -> ((ComboBox<?>) node("agenda-state-filter")).getValue()));
            f.agenda.complete(f.agenda.open(f.task.id()), true, true);
            home();
            assertTrue(allText().contains("Hari Ini · 1 item, 1 ditampilkan"));
            // Arsip konsisten, tidak mengaktifkan semester.
            combo("today-semester", f.archive);
            assertTrue(allText().contains("arsip; semester aktif tidak diubah"));
            assertNull(fx(() -> stage.getScene().lookup("#today-course-" + f.first.id())));
            click("today-course-" + f.old.id());
            present("hub-course");
            assertEquals(
                    f.old.id(), fx(() -> ((ComboBox<Course>) node("hub-course")).getValue().id()));
            home();
            assertEquals(f.active.id(), f.auth.profile().activeSemesterId().orElseThrow());
            combo("today-semester", f.active);
            // Riwayat spesifik, rasio 0,6 dan reader tunggal. Tidak mengambil bacaan global
            // peminatan.
            click("today-reading-" + f.material.id());
            present("material-note");
            assertEquals("Catatan asli", fx(() -> ((TextArea) node("material-note")).getText()));
            await(
                    () ->
                            stage.getScene().getRoot().lookupAll(".web-view").stream()
                                    .anyMatch(
                                            n ->
                                                    ((WebView) n)
                                                                    .getEngine()
                                                                    .getLoadWorker()
                                                                    .getState()
                                                            == javafx.concurrent.Worker.State
                                                                    .SUCCEEDED));
            await(
                    () ->
                            Math.abs(
                                            ((id.ac.uns.tutupbuku.ui.MarkdownReader)
                                                                    node("material-reader")
                                                                            .getParent())
                                                            .scrollRatio()
                                                    - 0.6)
                                    < 0.03);
            assertTrue(allText().contains(f.material.title()));
            shot("resumed-reader");
            home();
            click("today-actual-" + f.second.id());
            present("actual-mode");
            assertEquals(
                    f.second.id(),
                    fx(
                            () ->
                                    ((ActualCourse)
                                                    ((ListView<?>) node("grade-courses"))
                                                            .getSelectionModel()
                                                            .getSelectedItem())
                                            .course()
                                            .id()));
            assertEquals(
                    f.active.id(),
                    fx(() -> ((ComboBox<Semester>) node("grade-semester")).getValue().id()));
            home();
            click("today-whatif-" + f.first.id());
            present("what-if-pane");
            home();
            // Bacaan hilang setelah snapshot: tidak berpindah ke materi lain.
            f.learning
                    .bookmarkWriter(
                            new ReadingBookmark(
                                    f.interest.id(),
                                    f.interest.revision(),
                                    0.2,
                                    f.interest.location()))
                    .run();
            click("today-refresh");
            f.learning.deleteMaterial(f.interest.id());
            click("today-reading-" + f.interest.id());
            assertEquals(
                    4,
                    fx(() -> ((TabPane) node("main-tabs")).getSelectionModel().getSelectedIndex()));
            assertTrue(label("today-status").contains("tidak ada bacaan pengganti"));
            click("today-refresh");
            // IP lengkap aktual, tidak auto-pilih skenario.
            f.completeGrades();
            f.grades.save(
                    f.grades.create(f.active.id(), "Proyeksi lain", new java.math.BigDecimal("3")));
            click("today-refresh");
            assertTrue(allText().contains("Nilai aktual efektif: A-"));
            assertFalse(allText().contains("A_MINUS"));
            assertTrue(label("today-gpa").contains("3,42"));
            assertTrue(label("today-gpa").contains("Aktual"));
            fx(
                    () -> {
                        ((ScrollPane) node("today-scroll")).setVvalue(1);
                        return null;
                    });
            shot("complete-grades", 1.0);
            click("today-open-grades");
            present("saved-scenarios");
            assertNull(
                    fx(
                            () ->
                                    ((ListView<?>) node("saved-scenarios"))
                                            .getSelectionModel()
                                            .getSelectedItem()));
            home();
            var removed =
                    f.academic.saveCourseMetadata(
                            0, f.active.id(), "Mata kuliah dihapus setelah ringkasan", "", "1");
            click("today-refresh");
            f.academic.deleteCourse(f.active.id(), removed.id());
            click("today-actual-" + removed.id());
            present("grade-status");
            assertTrue(label("grade-status").contains("sudah dihapus"));
            home();
            // Gagal muat bukan nol item; retry memulihkan data yang sama.
            f.sql("ALTER TABLE agenda_items RENAME TO unavailable_agenda");
            click("today-item-" + f.exam.id());
            present("agenda-items");
            assertNull(fx(() -> stage.getScene().lookup("#agenda-title")));
            home();
            present("today-retry");
            assertTrue(allText().contains("belum dapat dipastikan"));
            assertNull(fx(() -> stage.getScene().lookup("#today-gpa")));
            shot("load-failure");
            f.sql("ALTER TABLE unavailable_agenda RENAME TO agenda_items");
            click("today-retry");
            assertTrue(label("today-gpa").contains("3,42"));
            tab("main-tabs", 3);
            present("agenda-items");
            assertNull(
                    fx(() -> stage.getScene().lookup("#agenda-title")),
                    "Target editor yang gagal tidak boleh bocor ke navigasi Agenda berikutnya");
            home();
            // Pergantian tanggal ketika kembali; Clock suntikan, bukan ubah jam Windows.
            tab("main-tabs", 3);
            f.clock.instant = f.clock.instant.plusSeconds(61);
            home();
            assertTrue(allText().contains("20 November 2026"));
            assertFalse(allText().contains("Hari Ini · 1 item"));
            shot("next-local-date");
            click("logout");
            signIn("002");
            assertNull(fx(() -> stage.getScene().lookup("#today-course-" + f.first.id())));
            assertFalse(allText().contains("Proyeksi lain"));
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
