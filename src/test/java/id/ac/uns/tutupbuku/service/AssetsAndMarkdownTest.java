package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class AssetsAndMarkdownTest {
    @TempDir Path directory;
    private Path image() throws Exception {
        Path file = directory.resolve("source.png"); ImageIO.write(new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB), "png", file.toFile()); return file;
    }
    @ParameterizedTest @ValueSource(strings = {"png", "jpg"})
    void managedImageSurvivesOriginalRelocationAndIsScopedToOwner(String format) throws Exception {
        var assets = new AssetStore(directory.resolve("data")); Path source = directory.resolve("source." + format);
        ImageIO.write(new BufferedImage(12, 12, BufferedImage.TYPE_INT_RGB), format, source.toFile());
        String reference = assets.importImage("001", source); Files.move(source, directory.resolve("moved-original.png"));
        assertFalse(reference.contains(directory.toString())); assertTrue(reference.startsWith("assets/"));
        assertTrue(assets.resolve("001", reference).isPresent()); assertTrue(assets.resolve("002", reference).isEmpty());
        assertTrue(new MarkdownRenderer(assets).render("001", "![Rumus](" + reference + ")").contains("data:image/" + (format.equals("png") ? "png" : "jpeg") + ";base64,"));
    }
    @Test void invalidImagesAndReferencesAreRejectedAndMissingImagesBecomeText() throws Exception {
        var assets = new AssetStore(directory.resolve("data"));
        Path fake = directory.resolve("fake.png"); Files.writeString(fake, "<script>not an image</script>");
        assertThrows(StorageException.class, () -> assets.importImage("001", fake));
        for (String value : new String[]{"../secret.png", "file:///C:/secret.png", "https://example.org/image.png", "assets/../image.png", "data:image/svg+xml;base64,AA"}) {
            assertTrue(assets.resolve("001", value).isEmpty());
            assertTrue(new MarkdownRenderer(assets).render("001", "![Gambar](" + value + ")").contains("Gambar tidak tersedia"));
        }
        String reference = assets.importImage("001", image()); Files.delete(assets.resolve("001", reference).orElseThrow());
        assertTrue(new MarkdownRenderer(assets).render("001", "![Gambar](" + reference + ")").contains("Gambar tidak tersedia"));
    }
    @Test void commonmarkTablesCodeAndFormattingAreRenderedButRawHtmlScriptsAreText() {
        var renderer = new MarkdownRenderer(new AssetStore(directory));
        String html = renderer.render("001", "# Judul\n\n**Tebal** *Miring*\n\n- Satu\n- Dua\n\n1. Langkah\n\n| A | B |\n| --- | --- |\n| 1 | 2 |\n\n```java\nSystem.exit(0);\n```\n\n<script>window.pwned=1</script><iframe src='https://example.org'></iframe>\n\n[Web](https://example.org)");
        for (String element : new String[]{"<h1>", "<strong>", "<em>", "<ul>", "<ol>", "<table>", "language-java", "System.exit(0);", "https://example.org"}) assertTrue(html.contains(element), element);
        assertFalse(html.contains("<script>")); assertFalse(html.contains("<iframe")); assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("default-src 'none'"));
    }
    @Test void dangerousLinkSchemesArePlainText() {
        var renderer = new MarkdownRenderer(new AssetStore(directory));
        for (String uri : new String[]{"javascript:alert%281%29", "file:///C:/secret", "data:text/html,hello", "mailto:test@example.org", "https://user:pass@example.org", "relative/path"}) {
            assertFalse(MarkdownRenderer.permittedLink(uri));
            assertFalse(renderer.render("001", "[Uji](" + uri + ")").contains("href="));
        }
        assertTrue(MarkdownRenderer.permittedLink("https://dev.java/learn/")); assertTrue(MarkdownRenderer.permittedLink("http://example.org"));
    }
    @Test void failedSaveDoesNotClearDraftAndOldAcknowledgmentKeepsNewerEditsDirty() {
        var location = new LearningLocation(LearningLocation.Kind.INTEREST, 1);
        var original = new MaterialReading(new Material(1, location, "Judul", "Awal", "", 0, 1, "now"), false, "Catatan", 0);
        var draft = new EditorDraft(original); draft.edit("Judul", "Versi A", "", "Catatan A"); var first = draft.snapshot();
        draft.edit("Judul", "Versi B", "", "Catatan B");
        draft.acknowledge(first, new MaterialReading(new Material(1, location, "Judul", "Versi A", "", 0, 2, "later"), false, "Catatan A", 1));
        assertTrue(draft.dirty()); assertEquals("Versi B", draft.snapshot().material().markdown()); assertEquals("Catatan B", draft.snapshot().note());
        // Tanpa acknowledge saat error, input dan status dirty tetap ada.
        assertTrue(draft.dirty()); assertEquals(2, draft.snapshot().material().revision());
    }
    @Test void nimCannotEscapeManagedAssetDirectory() throws Exception {
        Path root = directory.resolve("data"); var assets = new AssetStore(root);
        String reference = assets.importImage("../../outside", image());
        assertTrue(assets.resolve("../../outside", reference).orElseThrow().startsWith(root));
        assertFalse(reference.contains("outside")); assertTrue(assets.resolve("other", reference).isEmpty());
    }
    @Test void failedMaterialSavePreservesOldAndNewManagedImages() throws Exception {
        Path root = directory.resolve("data"); var assets = new AssetStore(root);
        String oldReference = assets.importImage("001", image());
        String newReference = assets.importImage("001", image());
        try (var database = new SqliteDatabase(root.resolve("test.db"))) {
            var academic = new SqliteRepository(database);
            academic.createAccount("001", "Uji", new id.ac.uns.tutupbuku.security.PasswordHasher().hash("fixture-password".toCharArray()));
            var repository = new SqliteLearningRepository(database);
            var folder = repository.saveFolder("001", new LearningFolder(0, LearningLocation.Kind.INTEREST, 0, "Bidang", 0));
            var saved = repository.save("001", new Material(0, folder.location(), "Gambar", "![Lama](" + oldReference + ")", "", 0, 0, ""), "", 0);
            database.read(connection -> { try (var statement = connection.createStatement()) { statement.execute("CREATE TRIGGER fail_save BEFORE UPDATE ON materials BEGIN SELECT RAISE(ABORT,'failure'); END"); } return null; });
            assertThrows(StorageException.class, () -> repository.save("001", new Material(saved.material().id(), folder.location(), "Gambar", "![Baru](" + newReference + ")", "", 0, 1, ""), "", saved.noteRevision()));
            assertTrue(repository.find("001", saved.material().id()).orElseThrow().material().markdown().contains(oldReference));
            assertTrue(assets.resolve("001", oldReference).isPresent()); assertTrue(assets.resolve("001", newReference).isPresent());
        }
    }
}
