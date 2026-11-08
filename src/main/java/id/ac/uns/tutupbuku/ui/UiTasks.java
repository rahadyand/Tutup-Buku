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
    UiTasks(Node root) { this.root = root; }
    <T> void run(Label status, Supplier<T> action, Consumer<T> success) {
        root.setDisable(true);
        status.setText("Memproses…");
        CompletableFuture.supplyAsync(action, worker).whenComplete((value, error) -> Platform.runLater(() -> {
            if (closed) return;
            root.setDisable(false);
            if (error != null) {
                Throwable cause = error instanceof CompletionException ? error.getCause() : error;
                status.setText(cause instanceof IllegalArgumentException || cause instanceof IllegalStateException
                        || cause instanceof id.ac.uns.tutupbuku.persistence.StorageException
                        ? cause.getMessage() : "Operasi gagal. Isian tetap dipertahankan; coba lagi.");
            } else {
                status.setText("Berhasil.");
                success.accept(value);
            }
        }));
    }
    @Override public void close() { closed = true; worker.shutdown(); }
}
