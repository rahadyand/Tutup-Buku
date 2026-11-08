package id.ac.uns.tutupbuku.service;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Smoke test dependensi saja; tidak mengklaim repository atau reader sudah dibuat. */
class StackCompatibilityTest {
    @Test void sqliteJdbcLoadsNativeDriverAndExecutesQuery() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             var statement = connection.prepareStatement("SELECT ? AS nim")) {
            statement.setString(1, "001234");
            try (var rows = statement.executeQuery()) {
                assertTrue(rows.next());
                assertEquals("001234", rows.getString("nim"));
            }
        }
    }

    @Test void commonmarkSupportsTablesCodeImagesLinksAndEscapedHtml() {
        var extensions = List.of(TablesExtension.create());
        var parser = Parser.builder().extensions(extensions).build();
        var renderer = HtmlRenderer.builder().extensions(extensions).escapeHtml(true).sanitizeUrls(true).build();
        var html = renderer.render(parser.parse("# Ringkasan\n\n| Skor | Nilai |\n| --- | --- |\n| 8/10 | 80 |\n\n```java\nSystem.out.println(1);\n```\n\n![Rumus](assets/rumus.png)\n\n[Sumber](https://example.org)\n\n<script>alert(1)</script>"));
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("<code class=\"language-java\">"));
        assertTrue(html.contains("src=\"assets/rumus.png\""));
        assertTrue(html.contains("href=\"https://example.org\""));
        assertFalse(html.contains("<script>"));
    }
}
