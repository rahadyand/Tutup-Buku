package id.ac.uns.tutupbuku.ui;

import id.ac.uns.tutupbuku.service.MarkdownRenderer;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import org.w3c.dom.*;
import org.w3c.dom.events.EventTarget;
import java.util.function.Consumer;

/** HTML hanya berasal dari renderer aman; script halaman dimatikan. Tidak ada Java bridge. */
public final class MarkdownReader extends BorderPane {
    private final WebView browser = new WebView();
    private double restoreRatio;
    private boolean ready;
    private Runnable onReady = () -> { };
    public MarkdownReader(Consumer<String> openExternal, Runnable onScroll) {
        browser.setId("material-reader"); browser.setContextMenuEnabled(false);
        browser.getEngine().setJavaScriptEnabled(false);
        browser.getEngine().setCreatePopupHandler(configuration -> null);
        browser.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state != Worker.State.SUCCEEDED || browser.getEngine().getDocument() == null) return;
            Document document = browser.getEngine().getDocument();
            ((EventTarget) document).addEventListener("click", event -> {
                Node target = (Node) event.getTarget();
                while (target != null && !(target instanceof Element element && element.getTagName().equalsIgnoreCase("a"))) target = target.getParentNode();
                if (target instanceof Element anchor) {
                    event.preventDefault(); event.stopPropagation(); String href = anchor.getAttribute("href");
                    if (MarkdownRenderer.permittedLink(href)) openExternal.accept(href);
                }
            }, true);
            Platform.runLater(() -> {
                // Hanya ekspresi milik aplikasi, tidak pernah menyisipkan teks Markdown ke script.
                browser.getEngine().executeScript("window.scrollTo(0, Math.max(0,document.documentElement.scrollHeight-window.innerHeight)*" + restoreRatio + ")");
                ready = true; onReady.run();
            });
        });
        browser.addEventHandler(ScrollEvent.SCROLL, event -> Platform.runLater(onScroll));
        browser.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> Platform.runLater(onScroll));
        browser.addEventHandler(KeyEvent.KEY_RELEASED, event -> Platform.runLater(onScroll));
        setCenter(browser);
    }
    public void showHtml(String html, double ratio, Runnable afterRender) {
        ready = false; restoreRatio = Math.max(0, Math.min(1, ratio)); onReady = afterRender;
        browser.getEngine().loadContent(html, "text/html");
    }
    public double scrollRatio() {
        if (!ready) return restoreRatio;
        Object value = browser.getEngine().executeScript("(function(){var h=document.documentElement.scrollHeight-window.innerHeight;return h>0?Math.max(0,Math.min(1,window.pageYOffset/h)):0;})()");
        return value instanceof Number number ? number.doubleValue() : 0;
    }
    public boolean ready() { return ready; }
}
