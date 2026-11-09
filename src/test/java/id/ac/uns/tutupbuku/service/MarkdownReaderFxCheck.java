package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.persistence.AssetStore;
import id.ac.uns.tutupbuku.ui.MarkdownReader;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class MarkdownReaderFxCheck {
    @TempDir Path directory;
    @Test void rawScriptsStayDisabledAndTrustedScrollApiRestoresPosition() throws Exception {
        CompletableFuture<Void> started = new CompletableFuture<>(), completed = new CompletableFuture<>();
        Platform.startup(() -> { Platform.setImplicitExit(false); started.complete(null); }); started.get(10, TimeUnit.SECONDS);
        Platform.runLater(() -> {
            Stage stage = new Stage();
            var clicked = new java.util.concurrent.atomic.AtomicReference<String>();
            MarkdownReader reader = new MarkdownReader(clicked::set, () -> { });
            stage.setScene(new Scene(reader, 700, 400)); stage.show();
            String markdown = "<script>window.pwned=1</script>\n\n[Sumber](https://example.org)\n\n" + "Paragraf panjang.\n\n".repeat(200);
            reader.showHtml(new MarkdownRenderer(new AssetStore(directory)).render("001", markdown), 0.65, () -> {
                try {
                    WebView view = (WebView) reader.lookup("#material-reader");
                    assertFalse(view.getEngine().isJavaScriptEnabled());
                    assertEquals("undefined", view.getEngine().executeScript("typeof window.pwned"));
                    assertEquals(0.65, reader.scrollRatio(), 0.02);
                    assertNull(clicked.get());
                    view.getEngine().executeScript("document.querySelector('a').click()");
                    assertEquals("https://example.org", clicked.get());
                    assertFalse(view.getEngine().getLocation().startsWith("https://"));
                    completed.complete(null);
                } catch (Throwable error) { completed.completeExceptionally(error); }
                finally { stage.close(); }
            });
        });
        try { completed.get(20, TimeUnit.SECONDS); } finally { Platform.exit(); }
    }
}
