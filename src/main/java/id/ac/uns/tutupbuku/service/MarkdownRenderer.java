package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.persistence.AssetStore;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import java.net.URI;
import java.util.List;

/** CommonMark melakukan parsing; visitor hanya membatasi tujuan link/gambar. */
public final class MarkdownRenderer {
    private final AssetStore assets;
    public MarkdownRenderer(AssetStore assets) { this.assets = assets; }
    public static boolean permittedLink(String value) {
        try {
            URI uri = URI.create(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException error) { return false; }
    }
    public String render(String owner, String markdown) {
        var extensions = List.of(TablesExtension.create());
        Node document = Parser.builder().extensions(extensions).build().parse(markdown == null ? "" : markdown);
        document.accept(new AbstractVisitor() {
            @Override public void visit(Image image) {
                var data = assets.dataUri(owner, image.getDestination());
                if (data.isPresent()) image.setDestination(data.orElseThrow());
                else { image.insertBefore(new Text("[Gambar tidak tersedia atau tidak diizinkan: " + image.getDestination() + "]")); image.unlink(); }
            }
            @Override public void visit(Link link) {
                visitChildren(link);
                if (!permittedLink(link.getDestination())) {
                    // Jadikan teks biasa; tidak ada navigasi file/javascript/data/URL relatif.
                    while (link.getFirstChild() != null) { Node child = link.getFirstChild(); link.insertBefore(child); }
                    link.insertBefore(new Text(" [tautan tidak diizinkan]")); link.unlink();
                }
            }
        });
        String body = HtmlRenderer.builder().extensions(extensions).escapeHtml(true).build().render(document);
        return """
            <!doctype html><html lang="id"><head><meta charset="UTF-8">
            <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data:; style-src 'unsafe-inline'">
            <style>body{font-family:'Segoe UI',sans-serif;margin:20px;line-height:1.6;color:#20383d;overflow-wrap:anywhere}
            img{max-width:100%;height:auto}table{border-collapse:collapse;width:100%}td,th{border:1px solid #bbc9c8;padding:8px}
            pre{background:#edf1f0;padding:12px;overflow:auto}code{font-family:Consolas,monospace}a{color:#086e67}</style>
            </head><body>
            """ + body + "</body></html>";
    }
}
