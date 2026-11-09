package id.ac.uns.tutupbuku.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Satu worker untuk hashing/JDBC; semua perubahan scene tetap pada JavaFX thread. */
final class UiTasks implements AutoCloseable {
    private final Node root;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "tutup-buku-storage"); thread.setDaemon(true); return thread;
    });
    private volatile boolean closed;
    private int blockingCount;
    UiTasks(Node root) { this.root = root; }
    <T> void run(Label status, Supplier<T> action, Consumer<T> success) {
        execute(status, "Memproses…", true, action, success, ignored -> { });
    }
    <T> void run(Label status, Supplier<T> action, Consumer<T> success, Consumer<Throwable> failure) {
        execute(status, "Memproses…", true, action, success, failure);
    }
    <T> void save(Label status, Supplier<T> action, Consumer<T> success, Consumer<Throwable> failure) {
        execute(status, "Menyimpan…", true, action, success, failure);
    }
    <T> void background(Label status, Supplier<T> action, Consumer<T> success) {
        execute(status, null, false, action, success, ignored -> { });
    }
    private <T> void execute(Label status, String progress, boolean blocking, Supplier<T> action, Consumer<T> success, Consumer<Throwable> failure) {
        if (blocking) { blockingCount++; root.setDisable(true); }
        if (progress != null) status.setText(progress);
        CompletableFuture.supplyAsync(action, worker).whenComplete((value, error) -> Platform.runLater(() -> {
            if (closed) return;
            if (blocking) { blockingCount--; root.setDisable(blockingCount > 0); }
            if (error != null) {
                Throwable cause = error instanceof CompletionException ? error.getCause() : error;
                status.setText(cause instanceof IllegalArgumentException || cause instanceof IllegalStateException
                        || cause instanceof id.ac.uns.tutupbuku.persistence.StorageException
                        ? cause.getMessage() : "Operasi gagal. Isian tetap dipertahankan; coba lagi.");
                failure.accept(cause);
            } else {
                if (blocking) status.setText("Berhasil.");
                success.accept(value);
            }
        }));
    }
    @Override public void close() { closed = true; worker.shutdown(); }
}
