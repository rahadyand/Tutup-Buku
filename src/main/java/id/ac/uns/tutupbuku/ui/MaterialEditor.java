package id.ac.uns.tutupbuku.ui;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.service.LearningService;
import javafx.animation.PauseTransition;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.util.function.Consumer;
import static id.ac.uns.tutupbuku.ui.FormSupport.*;

/** Buffer form, bukan penyimpanan. SQL dan parsing Markdown tetap di service/repository. */
final class MaterialEditor extends BorderPane {
    private final LearningService learning;
    private final UiTasks tasks;
    private final Consumer<Material> onSaved;
    private final Runnable onProgress;
    private final TextField title = text("material-title", "Judul wajib");
    private final TextField source = text("material-source", "Sumber/atribusi opsional");
    private final TextArea markdown = new TextArea(), note = new TextArea();
    private final Label saveStatus = status("material-save-status"), readStatus = status("reading-status"), metadata = new Label();
    private final CheckBox complete = new CheckBox("Selesai — tandai secara manual");
    private final MarkdownReader reader;
    private final TabPane tabs;
    private final Tab writeTab, previewTab;
    private final PauseTransition autosave = new PauseTransition(Duration.seconds(2));
    private final PauseTransition readingPause = new PauseTransition(Duration.millis(700));
    private EditorDraft draft;
    private boolean filling, suspended;
    private long renderedRevision;
    private String renderedMarkdown = "";

    MaterialEditor(LearningService learning, UiTasks tasks, Consumer<String> openExternal, Consumer<Material> onSaved, Runnable onProgress) {
        this.learning = learning; this.tasks = tasks; this.onSaved = onSaved; this.onProgress = onProgress;
        markdown.setId("material-markdown"); markdown.setWrapText(true); note.setId("material-note"); note.setWrapText(true); note.setPrefRowCount(2);
        complete.setId("material-completed"); reader = new MarkdownReader(openExternal, () -> readingPause.playFromStart());
        VBox writing = new VBox(6, field("Judul materi", title), field("Sumber / atribusi", source), markdown);
        writing.setPadding(new javafx.geometry.Insets(8)); VBox.setVgrow(markdown, Priority.ALWAYS);
        writeTab = new Tab("Tulis", writing); previewTab = new Tab("Pratinjau / Baca", reader);
        tabs = new TabPane(writeTab, previewTab); tabs.setId("material-tabs"); tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> { if (!filling && selected == previewTab) preview(0); });
        Button save = button("save-material", "Simpan materi & catatan"); save.setOnAction(event -> save(() -> { }));
        Button image = button("insert-image", "Sisipkan gambar lokal"); image.setOnAction(event -> {
            FileChooser chooser = new FileChooser(); chooser.setTitle("Pilih PNG/JPEG untuk disalin");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Gambar PNG/JPEG", "*.png", "*.jpg", "*.jpeg", "*.PNG", "*.JPG", "*.JPEG"));
            var selected = chooser.showOpenDialog(getScene().getWindow()); if (selected == null) return;
            tasks.run(readStatus, () -> learning.importImage(selected.toPath()), reference -> {
                markdown.insertText(markdown.getCaretPosition(), "\n![Gambar](" + reference + ")\n");
                tabs.getSelectionModel().select(writeTab); readStatus.setText("Gambar disalin. Simpan materi untuk menyimpan referensinya.");
            });
        });
        Button deleteNote = button("delete-note", "Hapus catatan"); deleteNote.setOnAction(event -> guard(() -> {
            if (draft == null || draft.base().material().id() == 0 || !confirm(this, "Hapus catatan?", "Hapus catatan pribadi materi ini? Isi materi dan ringkasan tidak dihapus.")) return;
            MaterialReading base = draft.base();
            tasks.save(saveStatus, () -> learning.save(base.material(), "", base.noteRevision()), saved -> { load(saved, reader.scrollRatio(), false); onSaved.accept(saved.material()); }, error -> saveStatus.setText("Gagal menyimpan: " + error.getMessage()));
        }));
        complete.setOnAction(event -> {
            if (draft == null || draft.base().material().id() == 0) return;
            boolean value = complete.isSelected(); long id = draft.base().material().id();
            tasks.save(readStatus, () -> { learning.complete(id, value); return value; }, saved -> { draft.setCompleted(saved); readStatus.setText("Status selesai tersimpan."); onProgress.run(); }, error -> complete.setSelected(!value));
        });
        Label materialTitle = new Label(); materialTitle.textProperty().bind(title.textProperty()); materialTitle.setWrapText(true);
        materialTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold;");
        VBox heading = new VBox(6, materialTitle, metadata, new FlowPane(8, 6, save, image), saveStatus);
        heading.setPadding(new javafx.geometry.Insets(10)); setTop(heading); setCenter(tabs);
        VBox notes = new VBox(6, complete, new Label("Catatan pribadi (terpisah dari isi dan Ringkasan)"), note, deleteNote, readStatus);
        notes.setPadding(new javafx.geometry.Insets(10)); setBottom(notes);
        for (TextInputControl field : new TextInputControl[]{title, source, markdown, note}) field.textProperty().addListener((obs, old, value) -> edited());
        autosave.setOnFinished(event -> { if (draft != null && draft.dirty() && !title.getText().isBlank()) save(() -> { }); });
        readingPause.setOnFinished(event -> recordReading(false, () -> { }));
        clear();
    }
    private void edited() {
        if (filling || draft == null || suspended) return;
        draft.edit(title.getText(), markdown.getText(), source.getText(), note.getText());
        saveStatus.setText("Belum tersimpan — simpan otomatis setelah 2 detik."); autosave.playFromStart();
    }
    void load(MaterialReading reading, double ratio, boolean read) {
        autosave.stop(); readingPause.stop(); suspended = false; filling = true; draft = new EditorDraft(reading); setDisable(false);
        title.setText(reading.material().title()); source.setText(reading.material().source()); markdown.setText(reading.material().markdown()); note.setText(reading.note());
        complete.setSelected(reading.completed()); complete.setDisable(reading.material().id() == 0);
        metadata.setText(metadata(reading.material()));
        saveStatus.setText(reading.material().id() == 0 ? "Belum tersimpan" : "Tersimpan"); readStatus.setText("");
        tabs.getSelectionModel().select(read ? previewTab : writeTab); filling = false;
        if (read) preview(ratio);
    }
    void clear() {
        autosave.stop(); readingPause.stop(); filling = true; draft = null;
        title.clear(); source.clear(); markdown.clear(); note.clear(); complete.setSelected(false);
        metadata.setText("Pilih atau buat materi."); saveStatus.setText(""); setDisable(true); filling = false;
        reader.showHtml("<html><body>Pilih atau buat materi untuk mulai membaca.</body></html>", 0, () -> { });
    }
    boolean dirty() { return draft != null && draft.dirty(); }
    Material current() { return draft == null ? null : draft.base().material(); }
    void suspend() { suspended = true; autosave.stop(); readingPause.stop(); }
    private void preview(double ratio) {
        if (draft == null) return;
        EditorDraft current = draft; String content = markdown.getText(); long revision = current.base().material().revision();
        tasks.run(readStatus, () -> learning.render(content), html -> {
            if (draft != current) return;
            renderedRevision = revision; renderedMarkdown = content;
            reader.showHtml(html, ratio, () -> { if (draft == current) recordReading(false, () -> { }); });
        });
    }
    private void save(Runnable afterSave) {
        autosave.stop();
        if (draft == null) { afterSave.run(); return; }
        EditorDraft current = draft; EditorDraft.SaveSnapshot snapshot;
        try { snapshot = current.snapshot(); }
        catch (IllegalArgumentException error) { saveStatus.setText("Gagal menyimpan: " + error.getMessage()); return; }
        tasks.save(saveStatus, () -> learning.save(snapshot.material(), snapshot.note(), snapshot.noteRevision()), saved -> {
            if (draft != current) return;
            current.acknowledge(snapshot, saved); complete.setDisable(false);
            metadata.setText(metadata(saved.material()));
            saveStatus.setText(current.dirty() ? "Belum tersimpan" : "Tersimpan"); onSaved.accept(saved.material());
            if (tabs.getSelectionModel().getSelectedItem() == previewTab) preview(0);
            afterSave.run();
        }, error -> saveStatus.setText("Gagal menyimpan: " + error.getMessage()));
    }
    void guard(Runnable action) {
        autosave.stop(); readingPause.stop();
        if (!dirty()) { recordReading(true, action); return; }
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION); dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Perubahan belum tersimpan"); dialog.setHeaderText("Simpan perubahan materi dan catatan sebelum berpindah?");
        ButtonType save = new ButtonType("Simpan", ButtonBar.ButtonData.YES), discard = new ButtonType("Buang perubahan", ButtonBar.ButtonData.NO), cancel = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getButtonTypes().setAll(save, discard, cancel);
        ((Button) dialog.getDialogPane().lookupButton(save)).setDefaultButton(false); ((Button) dialog.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        var choice = dialog.showAndWait();
        if (choice.filter(save::equals).isPresent()) save(() -> recordReading(true, action));
        else if (choice.filter(discard::equals).isPresent()) {
            MaterialReading previous = draft.base();
            if (previous.material().id() == 0) clear(); else load(previous, 0, false);
            action.run();
        } else if (dirty()) autosave.playFromStart();
    }
    private void recordReading(boolean blocking, Runnable after) {
        if (draft == null || suspended || current().id() == 0 || !reader.ready() || tabs.getSelectionModel().getSelectedItem() != previewTab
                || current().revision() != renderedRevision || !current().markdown().equals(renderedMarkdown)) { after.run(); return; }
        Runnable writer = learning.bookmarkWriter(new ReadingBookmark(current().id(), renderedRevision, reader.scrollRatio(), current().location()));
        if (blocking) tasks.run(readStatus, () -> { writer.run(); return null; }, ignored -> after.run(), error -> after.run());
        else tasks.background(readStatus, () -> { writer.run(); return null; }, ignored -> after.run());
    }
    private String metadata(Material material) {
        if (material.id() == 0) return "Materi baru · isi kosong boleh disimpan sebagai draf";
        var format = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", java.util.Locale.forLanguageTag("id-ID"))
                .withZone(java.time.ZoneId.of("Asia/Jakarta"));
        return "Revisi " + material.revision() + " · " + format.format(java.time.Instant.parse(material.updatedAt())) + " WIB";
    }
}
