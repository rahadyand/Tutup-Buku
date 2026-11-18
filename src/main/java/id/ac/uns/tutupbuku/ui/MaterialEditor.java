package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.service.LearningService;

import javafx.animation.PauseTransition;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.util.function.Consumer;

/** Buffer form, bukan penyimpanan. SQL dan parsing Markdown tetap di service/repository. */
final class MaterialEditor extends BorderPane {
    private final LearningService learning;
    private final UiTasks tasks;
    private final Consumer<Material> onSaved;
    private final Runnable onProgress;
    private final TextField title = text("material-title", "Judul wajib");
    private final TextField source = text("material-source", "Sumber/atribusi opsional");
    private final TextArea markdown = new TextArea(), note = new TextArea();
    private final Label saveStatus = status("material-save-status"),
            readStatus = status("reading-status"),
            metadata = new Label();
    private final CheckBox complete = new CheckBox("Selesai — tandai secara manual");
    private final MarkdownReader reader;
    private final TabPane tabs;
    private final Tab writeTab, previewTab;
    private final VBox readHeader, editHeader;
    private final PauseTransition autosave = new PauseTransition(Duration.seconds(2));
    private final PauseTransition readingPause = new PauseTransition(Duration.millis(700));
    private EditorDraft draft;
    private boolean filling, suspended;
    private long renderedRevision;
    private String renderedMarkdown = "";
    private boolean readMode = true;
    private double readingRatio;
    private long readingRevision;

    MaterialEditor(
            LearningService learning,
            UiTasks tasks,
            Consumer<String> openExternal,
            Consumer<Material> onSaved,
            Runnable onProgress) {
        this.learning = learning;
        this.tasks = tasks;
        this.onSaved = onSaved;
        this.onProgress = onProgress;
        markdown.setId("material-markdown");
        markdown.setWrapText(true);
        markdown.getStyleClass().add("tb-code-editor");
        note.setId("material-note");
        note.setWrapText(true);
        note.setPrefRowCount(2);
        complete.setId("material-completed");
        reader = new MarkdownReader(openExternal, () -> readingPause.playFromStart());
        VBox writing =
                new VBox(
                        6,
                        field("Judul materi", title),
                        field("Sumber / atribusi", source),
                        markdown);
        writing.setPadding(new javafx.geometry.Insets(8));
        VBox.setVgrow(markdown, Priority.ALWAYS);
        writing.setMinHeight(Region.USE_PREF_SIZE);
        markdown.setPrefRowCount(8);
        writeTab = new Tab("Tulis", scroll(writing));
        previewTab = new Tab("Pratinjau / Baca", reader);
        tabs = new TabPane(writeTab, previewTab);
        tabs.setId("material-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, old, selected) -> {
                            if (!filling && selected == previewTab) preview(readingPosition());
                        });
        Button save = button("save-material", "Simpan");
        save.getStyleClass().add("tb-primary");
        save.setOnAction(event -> save(() -> {}));
        Button image = button("insert-image", "Sisipkan gambar lokal");
        image.setOnAction(
                event -> {
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Pilih PNG/JPEG untuk disalin");
                    chooser.getExtensionFilters()
                            .add(
                                    new FileChooser.ExtensionFilter(
                                            "Gambar PNG/JPEG",
                                            "*.png",
                                            "*.jpg",
                                            "*.jpeg",
                                            "*.PNG",
                                            "*.JPG",
                                            "*.JPEG"));
                    var selected = chooser.showOpenDialog(getScene().getWindow());
                    if (selected == null) return;
                    tasks.run(
                            readStatus,
                            () -> learning.importImage(selected.toPath()),
                            reference -> {
                                markdown.insertText(
                                        markdown.getCaretPosition(),
                                        "\n![Gambar](" + reference + ")\n");
                                tabs.getSelectionModel().select(writeTab);
                                readStatus.setText(
                                        "Gambar disalin. Simpan materi untuk menyimpan"
                                                + " referensinya.");
                            });
                });
        Button deleteNote = button("delete-note", "Hapus catatan");
        deleteNote.setOnAction(
                event ->
                        guard(
                                () -> {
                                    if (draft == null
                                            || draft.base().material().id() == 0
                                            || !confirm(
                                                    this,
                                                    "Hapus catatan?",
                                                    "Hapus catatan pribadi materi ini? Isi materi"
                                                            + " dan ringkasan tidak dihapus."))
                                        return;
                                    MaterialReading base = draft.base();
                                    tasks.save(
                                            saveStatus,
                                            () ->
                                                    learning.save(
                                                            base.material(),
                                                            "",
                                                            base.noteRevision()),
                                            saved -> {
                                                load(saved, readingPosition(), readMode);
                                                onSaved.accept(saved.material());
                                            },
                                            error ->
                                                    saveStatus.setText(
                                                            "Gagal menyimpan: "
                                                                    + error.getMessage()));
                                }));
        complete.setOnAction(
                event -> {
                    if (draft == null || draft.base().material().id() == 0) return;
                    boolean value = complete.isSelected();
                    long id = draft.base().material().id();
                    tasks.save(
                            readStatus,
                            () -> {
                                learning.complete(id, value);
                                return value;
                            },
                            saved -> {
                                draft.setCompleted(saved);
                                readStatus.setText("Status selesai tersimpan.");
                                onProgress.run();
                            },
                            error -> complete.setSelected(!value));
                });
        metadata.setWrapText(true);
        metadata.getStyleClass().add("tb-caption");
        Button edit = button("edit-material", "Ubah Materi");
        edit.setOnAction(event -> guard(this::showEditMode));
        Button read = button("read-material", "Kembali Membaca");
        read.setOnAction(event -> guard(this::showReadMode));
        Label infoText = note("");
        infoText.textProperty()
                .bind(
                        title.textProperty()
                                .concat("\nSumber: ")
                                .concat(source.textProperty())
                                .concat("\n")
                                .concat(metadata.textProperty()));
        TitledPane info = new TitledPane("Info materi", infoText);
        info.setId("material-info");
        info.setExpanded(false);
        info.setAnimated(false);
        info.setPrefWidth(150);
        readHeader = new VBox(4, new FlowPane(8, 6, note("Mode Baca"), edit, info));
        editHeader = new VBox(4, new FlowPane(8, 6, save, image, read));
        for (VBox header : new VBox[] {readHeader, editHeader}) {
            header.setPadding(new javafx.geometry.Insets(8));
            header.getStyleClass().add("tb-editor-header");
        }
        Button saveNote = button("save-note", "Simpan catatan");
        saveNote.setOnAction(event -> save(() -> {}));
        TitledPane notesPanel =
                new TitledPane(
                        "Catatan pribadi (terpisah dari isi dan Ringkasan)",
                        new VBox(6, note, new FlowPane(8, 6, saveNote, deleteNote)));
        notesPanel.setId("material-notes-pane");
        notesPanel.setExpanded(false);
        VBox notes = new VBox(4, complete, notesPanel, saveStatus, readStatus);
        saveStatus.visibleProperty().bind(saveStatus.textProperty().isNotEmpty());
        saveStatus.managedProperty().bind(saveStatus.visibleProperty());
        readStatus.visibleProperty().bind(readStatus.textProperty().isNotEmpty());
        readStatus.managedProperty().bind(readStatus.visibleProperty());
        notes.setPadding(new javafx.geometry.Insets(8));
        setBottom(notes);
        for (TextInputControl field : new TextInputControl[] {title, source, markdown, note})
            field.textProperty().addListener((obs, old, value) -> edited());
        autosave.setOnFinished(
                event -> {
                    if (draft != null && draft.dirty() && !title.getText().isBlank())
                        save(() -> {});
                });
        readingPause.setOnFinished(event -> recordReading(false, () -> {}));
        clear();
    }

    private void edited() {
        if (filling || draft == null || suspended) return;
        draft.edit(title.getText(), markdown.getText(), source.getText(), note.getText());
        saveStatus.setText(
                (contentChanged() ? "Materi/catatan" : "Catatan")
                        + " belum tersimpan — simpan otomatis setelah 2 detik.");
        autosave.playFromStart();
    }

    void load(MaterialReading reading, double ratio, boolean read) {
        autosave.stop();
        readingPause.stop();
        suspended = false;
        filling = true;
        draft = new EditorDraft(reading);
        setDisable(false);
        title.setText(reading.material().title());
        source.setText(reading.material().source());
        markdown.setText(reading.material().markdown());
        note.setText(reading.note());
        complete.setSelected(reading.completed());
        complete.setDisable(reading.material().id() == 0);
        metadata.setText(metadata(reading.material()));
        saveStatus.setText(reading.material().id() == 0 ? "Belum tersimpan" : "Tersimpan");
        readStatus.setText("");
        readingRatio = ratio;
        readingRevision = reading.material().revision();
        filling = false;
        showMode(read);
    }

    void clear() {
        autosave.stop();
        readingPause.stop();
        filling = true;
        draft = null;
        readingRatio = 0;
        title.clear();
        source.clear();
        markdown.clear();
        note.clear();
        complete.setSelected(false);
        metadata.setText("Pilih atau buat materi.");
        saveStatus.setText("");
        setDisable(true);
        filling = false;
        showMode(true);
        reader.showHtml(
                "<html><body>Pilih atau buat materi untuk mulai membaca.</body></html>",
                0,
                () -> {});
    }

    boolean dirty() {
        return draft != null && draft.dirty();
    }

    Material current() {
        return draft == null ? null : draft.base().material();
    }

    void suspend() {
        suspended = true;
        autosave.stop();
        readingPause.stop();
    }

    private boolean showingReader() {
        return readMode || tabs.getSelectionModel().getSelectedItem() == previewTab;
    }

    private boolean contentChanged() {
        if (draft == null) return false;
        Material base = draft.base().material();
        return !title.getText().trim().equals(base.title())
                || !source.getText().equals(base.source())
                || !markdown.getText().equals(base.markdown());
    }

    private double readingPosition() {
        return draft != null
                        && readingRevision == draft.base().material().revision()
                        && !contentChanged()
                ? readingRatio
                : 0;
    }

    private void rememberPosition() {
        if (getScene() != null
                && draft != null
                && showingReader()
                && reader.ready()
                && !contentChanged()
                && renderedRevision == draft.base().material().revision()) {
            readingRatio = reader.scrollRatio();
            readingRevision = renderedRevision;
        }
    }

    void preservePosition(Runnable layoutChange) {
        rememberPosition();
        double ratio = readingPosition();
        layoutChange.run();
        javafx.application.Platform.runLater(
                () -> {
                    if (showingReader() && reader.ready()) reader.restorePosition(ratio);
                });
    }

    void showReadMode() {
        showMode(true);
    }

    void showEditMode() {
        showMode(false);
    }

    private void showMode(boolean read) {
        readMode = read;
        // WebView dan draft yang sama dipindahkan, bukan dibuat ulang per mode.
        previewTab.setContent(null);
        setCenter(null);
        setTop(read ? readHeader : editHeader);
        if (read) {
            setCenter(reader);
            if (draft != null) preview(readingPosition());
        } else {
            previewTab.setContent(reader);
            setCenter(tabs);
            filling = true;
            tabs.getSelectionModel().select(writeTab);
            filling = false;
        }
    }

    private void preview(double ratio) {
        if (draft == null) return;
        EditorDraft current = draft;
        String content = markdown.getText();
        long revision = current.base().material().revision();
        tasks.run(
                readStatus,
                () -> learning.render(content),
                html -> {
                    if (draft != current) return;
                    renderedRevision = revision;
                    renderedMarkdown = content;
                    reader.showHtml(
                            html,
                            ratio,
                            () -> {
                                if (draft == current) recordReading(false, () -> {});
                            });
                });
    }

    private void save(Runnable afterSave) {
        autosave.stop();
        if (draft == null) {
            afterSave.run();
            return;
        }
        EditorDraft current = draft;
        rememberPosition();
        long previousRevision = current.base().material().revision();
        boolean materialChanged = contentChanged();
        EditorDraft.SaveSnapshot snapshot;
        try {
            snapshot = current.snapshot();
        } catch (IllegalArgumentException error) {
            saveStatus.setText("Gagal menyimpan: " + error.getMessage());
            return;
        }
        tasks.save(
                saveStatus,
                () -> learning.save(snapshot.material(), snapshot.note(), snapshot.noteRevision()),
                saved -> {
                    if (draft != current) return;
                    current.acknowledge(snapshot, saved);
                    if (saved.material().revision() != previousRevision) readingRatio = 0;
                    readingRevision = saved.material().revision();
                    complete.setDisable(false);
                    metadata.setText(metadata(saved.material()));
                    saveStatus.setText(
                            current.dirty()
                                    ? "Belum tersimpan"
                                    : materialChanged
                                            ? "Materi & catatan tersimpan."
                                            : "Catatan tersimpan.");
                    onSaved.accept(saved.material());
                    if (showingReader()
                            && (renderedRevision != saved.material().revision()
                                    || !renderedMarkdown.equals(markdown.getText())))
                        preview(readingPosition());
                    afterSave.run();
                },
                error -> saveStatus.setText("Gagal menyimpan: " + error.getMessage()));
    }

    void guard(Runnable action) {
        rememberPosition();
        autosave.stop();
        readingPause.stop();
        if (!dirty()) {
            recordReading(true, action);
            return;
        }
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Perubahan belum tersimpan");
        dialog.setHeaderText("Simpan perubahan materi dan catatan sebelum berpindah?");
        ButtonType save = new ButtonType("Simpan", ButtonBar.ButtonData.YES),
                discard = new ButtonType("Buang perubahan", ButtonBar.ButtonData.NO),
                cancel = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getButtonTypes().setAll(save, discard, cancel);
        ((Button) dialog.getDialogPane().lookupButton(save)).setDefaultButton(false);
        ((Button) dialog.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        styleDialog(dialog);
        var choice = dialog.showAndWait();
        if (choice.filter(save::equals).isPresent()) save(() -> recordReading(true, action));
        else if (choice.filter(discard::equals).isPresent()) {
            MaterialReading previous = draft.base();
            if (previous.material().id() == 0) clear();
            else load(previous, readingPosition(), readMode);
            action.run();
        } else if (dirty()) autosave.playFromStart();
    }

    private void recordReading(boolean blocking, Runnable after) {
        if (draft == null
                || getScene() == null
                || suspended
                || current().id() == 0
                || !reader.ready()
                || !showingReader()
                || current().revision() != renderedRevision
                || !current().markdown().equals(renderedMarkdown)) {
            after.run();
            return;
        }
        Runnable writer =
                learning.bookmarkWriter(
                        new ReadingBookmark(
                                current().id(),
                                renderedRevision,
                                reader.scrollRatio(),
                                current().location()));
        if (blocking)
            tasks.run(
                    readStatus,
                    () -> {
                        writer.run();
                        return null;
                    },
                    ignored -> {
                        readStatus.setText("");
                        after.run();
                    },
                    error ->
                            readStatus.setText(
                                    "Posisi baca gagal disimpan. Coba lagi sebelum berpindah."));
        else
            tasks.background(
                    readStatus,
                    () -> {
                        writer.run();
                        return null;
                    },
                    ignored -> {
                        readStatus.setText("");
                        after.run();
                    });
    }

    private String metadata(Material material) {
        if (material.id() == 0) return "Materi baru · isi kosong boleh disimpan sebagai draf";
        var format =
                java.time.format.DateTimeFormatter.ofPattern(
                                "dd MMM yyyy HH:mm", java.util.Locale.forLanguageTag("id-ID"))
                        .withZone(java.time.ZoneId.of("Asia/Jakarta"));
        return "Revisi "
                + material.revision()
                + " · "
                + format.format(java.time.Instant.parse(material.updatedAt()))
                + " WIB";
    }
}
