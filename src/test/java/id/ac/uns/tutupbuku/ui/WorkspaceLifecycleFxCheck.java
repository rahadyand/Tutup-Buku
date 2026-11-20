package id.ac.uns.tutupbuku.ui;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.service.*;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Satu worker lama benar-benar selesai sebelum owner berubah; callback/submit usang ditolak. */
class WorkspaceLifecycleFxCheck {
    @TempDir Path directory;

    private <T> T fx(Callable<T> action) throws Exception {
        var future = new CompletableFuture<T>();
        Platform.runLater(
                () -> {
                    try {
                        future.complete(action.call());
                    } catch (Throwable e) {
                        future.completeExceptionally(e);
                    }
                });
        return future.get(10, TimeUnit.SECONDS);
    }

    @Test
    void drainWritesUnderOriginalOwnerAndIgnoreOldCallbacks() throws Exception {
        var started = new CompletableFuture<Void>();
        Platform.startup(() -> started.complete(null));
        started.get(10, TimeUnit.SECONDS);
        var release = new CountDownLatch(1);
        var entered = new CountDownLatch(1);
        var finished = new CompletableFuture<Void>();
        var root = fx(BorderPane::new);
        var label = fx(Label::new);
        var tasks = fx(() -> new UiTasks(root));
        var oldCallback = new AtomicInteger();
        var lateWrite = new AtomicBoolean();
        try (var db = new SqliteDatabase(directory.resolve("lifecycle.db"))) {
            var session = new UserSession();
            var repo = new SqliteRepository(db);
            var auth =
                    new AuthService(
                            repo, new id.ac.uns.tutupbuku.security.PasswordHasher(), session);
            String owner = auth.openGuest().nim();
            var academic = new AcademicService(repo, session);
            fx(
                    () -> {
                        tasks.background(
                                label,
                                () -> {
                                    entered.countDown();
                                    try {
                                        if (!release.await(5, TimeUnit.SECONDS))
                                            throw new AssertionError("worker timeout");
                                    } catch (InterruptedException e) {
                                        throw new AssertionError(e);
                                    }
                                    return academic.saveSemesterForm(0, "1", "2026", true);
                                },
                                ignored -> oldCallback.incrementAndGet());
                        tasks.background(
                                label,
                                () ->
                                        academic.saveCourseMetadata(
                                                0,
                                                academic.semesters().getFirst().id(),
                                                "Tersimpan sebelum pindah",
                                                "",
                                                "3"),
                                ignored -> oldCallback.incrementAndGet());
                        tasks.background(
                                label,
                                () -> {
                                    throw new IllegalStateException("Pesan ruang lama");
                                },
                                ignored -> oldCallback.incrementAndGet());
                        tasks.finish(
                                () -> {
                                    try {
                                        assertEquals(1, repo.listSemesters(owner).size());
                                        assertEquals(
                                                1,
                                                repo.listCourses(
                                                                owner,
                                                                repo.listSemesters(owner)
                                                                        .getFirst()
                                                                        .id())
                                                        .size());
                                        session.login("pemilik-baru");
                                        label.setText("Ruang baru");
                                        finished.complete(null);
                                    } catch (Throwable e) {
                                        finished.completeExceptionally(e);
                                    }
                                });
                        tasks.background(
                                label,
                                () -> {
                                    lateWrite.set(true);
                                    return "terlambat";
                                },
                                ignored -> oldCallback.incrementAndGet());
                        assertTrue(root.isDisable());
                        return null;
                    });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertFalse(finished.isDone());
            release.countDown();
            finished.get(10, TimeUnit.SECONDS);
            fx(() -> null);
            assertEquals(0, oldCallback.get());
            assertFalse(lateWrite.get());
            assertEquals("Ruang baru", fx(label::getText));
            assertFalse(fx(root::isDisable));
        } finally {
            release.countDown();
            fx(
                    () -> {
                        tasks.close();
                        return null;
                    });
            Platform.exit();
        }
    }
}
