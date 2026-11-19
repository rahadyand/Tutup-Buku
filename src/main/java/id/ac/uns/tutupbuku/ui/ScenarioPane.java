package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.service.*;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.*;

/** Form skenario memegang draft immutable; kegagalan simpan tidak mengganti isian. */
final class ScenarioPane extends BorderPane {
    private final GradePlanningService service;
    private final UiTasks tasks;

    record CourseDraft(SemesterScenario scenario, long courseId, boolean modified) {}

    private final java.util.function.Consumer<CourseDraft> editCourse;
    private final CheckBox projectionEnabled =
            new CheckBox("Gunakan target/asumsi sebagai proyeksi mata kuliah ini");
    private final VBox ipSources = new VBox(6);
    private final Button saveScenario = button("save-scenario", "Simpan skenario");
    private boolean outdated;
    private final ListView<SemesterScenario> saved = new ListView<>();
    private final ComboBox<ScenarioCourse> courses = new ComboBox<>();
    private final TextField name = text("scenario-name", "Nama skenario"),
            gpaTarget = text("scenario-gpa-target", "0–4"),
            numericTarget = text("scenario-score-target", "0–100");
    private final ComboBox<EvaluationMode> mode = new ComboBox<>();
    private final ComboBox<LetterGrade> estimate = GradeForms.letters("scenario-estimate"),
            targetLetter = GradeForms.letters("scenario-target-letter");
    private final ComboBox<String> targetKind = new ComboBox<>();
    private final CheckBox attendance = new CheckBox("Asumsi syarat kehadiran terpenuhi");
    private final VBox rows = new VBox(8);
    private final Map<String, LockRow> locks = new LinkedHashMap<>();
    private final Label status = status("scenario-status"),
            courseResult = status("scenario-course-result"),
            gpaResult = status("scenario-gpa-result"),
            review = status("scenario-review");
    private final ComboBox<SemesterScenario> comparison = new ComboBox<>();
    private final VBox comparisonContent = new VBox(8);
    private SemesterScenario draft;
    private ScenarioCourse current;
    private long semester;
    private boolean filling, dirty;
    private final Label context = status("scenario-context");

    private record LockRow(CheckBox locked, TextField score, Label suggestion, boolean actual) {}

    ScenarioPane(
            GradePlanningService service,
            UiTasks tasks,
            java.util.function.Consumer<CourseDraft> editCourse) {
        this.service = service;
        this.tasks = tasks;
        this.editCourse = editCourse;
        projectionEnabled.setId("scenario-projection");
        projectionEnabled.selectedProperty().addListener((o, a, b) -> changed());
        saved.setId("saved-scenarios");
        courses.setId("scenario-course");
        mode.setId("scenario-mode");
        mode.getItems().addAll(EvaluationMode.values());
        targetKind.setId("scenario-target-kind");
        targetKind.getItems().addAll("Angka", "Huruf minimum");
        attendance.setId("scenario-attendance");
        comparison.setId("compare-scenario");
        courses.setMaxWidth(Double.MAX_VALUE);
        saved.setPrefWidth(200);
        saved.setPlaceholder(note("Belum ada skenario tersimpan."));
        for (TextField field : List.of(name, gpaTarget, numericTarget))
            field.textProperty().addListener((o, a, b) -> changed());
        mode.valueProperty()
                .addListener(
                        (o, a, b) -> {
                            if (filling || a == null || b == null) return;
                            if (!GradeForms.confirmMode(this, b)) {
                                filling = true;
                                mode.setValue(a);
                                filling = false;
                                return;
                            }
                            changed();
                        });
        estimate.valueProperty().addListener((o, a, b) -> changed());
        attendance.selectedProperty().addListener((o, a, b) -> changed());
        targetLetter.valueProperty().addListener((o, a, b) -> changed());
        targetKind
                .valueProperty()
                .addListener(
                        (o, a, b) -> {
                            changed();
                            numericTarget.setDisable(!"Angka".equals(b));
                            targetLetter.setDisable(!"Huruf minimum".equals(b));
                        });
        saved.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (o, old, value) -> {
                            if (filling || value == null) return;
                            filling = true;
                            saved.getSelectionModel().select(old);
                            filling = false;
                            guard(
                                    () ->
                                            tasks.run(
                                                    status,
                                                    () -> service.open(value.id()),
                                                    this::show));
                        });
        courses.valueProperty()
                .addListener(
                        (o, old, value) -> {
                            if (filling) return;
                            filling = true;
                            courses.setValue(old);
                            filling = false;
                            try {
                                capture();
                                ScenarioCourse next =
                                        value == null
                                                ? null
                                                : draft.courses().stream()
                                                        .filter(
                                                                c ->
                                                                        c.basis().course().id()
                                                                                == value.basis()
                                                                                        .course()
                                                                                        .id())
                                                        .findFirst()
                                                        .orElseThrow();
                                filling = true;
                                courses.setValue(value);
                                filling = false;
                                showCourse(next);
                            } catch (IllegalArgumentException error) {
                                status.setText(error.getMessage());
                            }
                        });
        Button create = button("new-scenario", "Skenario baru"),
                duplicate = button("duplicate-scenario", "Simpan salinan"),
                delete = button("delete-scenario", "Hapus skenario"),
                save = saveScenario,
                calculate = button("calculate-scenario", "Hitung target & IP"),
                refresh = button("refresh-scenario", "Tinjau / perbarui basis"),
                clearEstimate = button("clear-scenario-estimate", "Kosongkan estimasi"),
                compare = button("compare-scenarios", "Bandingkan tersimpan");
        create.setOnAction(
                e ->
                        guard(
                                () -> {
                                    if (semester == 0) {
                                        status.setText("Pilih semester.");
                                        return;
                                    }
                                    tasks.run(
                                            status,
                                            () ->
                                                    service.create(
                                                            semester,
                                                            "Skenario baru",
                                                            new BigDecimal("3")),
                                            value -> {
                                                show(value);
                                                dirty = true;
                                            });
                                }));
        duplicate.setOnAction(
                e -> {
                    try {
                        capture();
                        SemesterScenario input = draft;
                        tasks.save(
                                status,
                                () -> service.saveCopy(input, input.name() + " (salinan)"),
                                value -> {
                                    show(value);
                                    reloadList();
                                    status.setText(
                                            "Salinan independen tersimpan; skenario asal tidak"
                                                    + " diubah.");
                                },
                                error -> {});
                    } catch (IllegalArgumentException error) {
                        status.setText(error.getMessage());
                    }
                });
        delete.setOnAction(
                e ->
                        guard(
                                () -> {
                                    if (draft == null || draft.id() == 0) {
                                        status.setText("Pilih skenario tersimpan.");
                                        return;
                                    }
                                    if (!confirm(
                                            this,
                                            "Hapus skenario?",
                                            "Hapus snapshot dan asumsi "
                                                    + draft.name()
                                                    + "? Nilai aktual tetap tersimpan.")) return;
                                    long id = draft.id();
                                    tasks.run(
                                            status,
                                            () -> {
                                                service.delete(id);
                                                return null;
                                            },
                                            v -> {
                                                show(null);
                                                reloadList();
                                            });
                                }));
        save.setOnAction(e -> save(() -> {}));
        calculate.setOnAction(e -> calculate());
        refresh.setOnAction(e -> refreshBasis());
        clearEstimate.setOnAction(e -> estimate.setValue(null));
        compare.setOnAction(e -> compare());
        save.getStyleClass().add("tb-primary");
        calculate.getStyleClass().add("tb-primary");
        delete.getStyleClass().add("tb-danger");
        gpaResult.getStyleClass().add("tb-result");
        courseResult.getStyleClass().add("tb-result");
        VBox left =
                form(
                        heading("Skenario tersimpan"),
                        saved,
                        new FlowPane(6, 6, create, duplicate, delete));
        VBox.setVgrow(saved, Priority.ALWAYS);
        left.setMinWidth(210);
        TitledPane formula =
                new TitledPane(
                        "Bagaimana saran target dihitung?",
                        note(
                                "Kontribusi tetap K berasal dari aktual dan asumsi terkunci. W"
                                    + " adalah bobot komponen bebas. Saran x = (target − K) / W,"
                                    + " sama untuk semua yang bebas. Ini satu pilihan merata, bukan"
                                    + " satu-satunya kombinasi. Saran minimum aman dibulatkan ke"
                                    + " atas 2 desimal; nilai aktual tidak dibulatkan untuk"
                                    + " konversi huruf."));
        formula.setExpanded(false);
        Button edit = button("edit-scenario-what-if", "Buka What If mata kuliah");
        edit.setOnAction(
                e -> {
                    try {
                        capture();
                        if (current == null)
                            throw new IllegalArgumentException("Pilih mata kuliah.");
                        CourseDraft input =
                                new CourseDraft(draft, current.basis().course().id(), dirty);
                        show(null); // Kepemilikan draf dipindahkan, bukan dua editor aktif untuk
                        // record yang sama.
                        editCourse.accept(input);
                    } catch (IllegalArgumentException error) {
                        status.setText(error.getMessage());
                    }
                });
        TitledPane ipDetails = new TitledPane("Sumber hasil tiap mata kuliah untuk IP", ipSources);
        ipDetails.setId("scenario-ip-sources");
        ipDetails.setExpanded(false);
        ipDetails.setAnimated(false);
        VBox body =
                form(
                        heading("Rencana semester"),
                        note(
                                "Skenario menyimpan snapshot dan asumsi terpisah dari aktual."
                                    + " Perhitungan hanya memakai mode aktif. Saran bebas adalah"
                                    + " skor normalisasi 0–100, bukan perolehan mentah."),
                        field("Nama skenario", name),
                        field("Target IP semester (0–4)", gpaTarget),
                        new FlowPane(6, 6, save, calculate, refresh),
                        review,
                        gpaResult,
                        note(
                                "IP di sini memakai satu skenario semester lengkap, bukan gabungan"
                                    + " otomatis skenario per mata kuliah. Hasil yang kosong tidak"
                                    + " diganti E/0."),
                        ipDetails,
                        field("Mata kuliah dalam snapshot", courses),
                        edit,
                        projectionEnabled,
                        field("Mode aktif skenario", mode),
                        field(
                                "Estimasi huruf (dipakai hanya jika belum ada huruf aktual)",
                                estimate),
                        clearEstimate,
                        field("Jenis target detail", targetKind),
                        field("Minimal mencapai nilai angka", numericTarget),
                        field("Minimal mencapai huruf (ambang bawah PRD)", targetLetter),
                        formula,
                        attendance,
                        note(
                                "Centang awal adalah asumsi. Perubahan kehadiran skenario belum"
                                        + " terverifikasi. Bobot 0% tidak memerlukan nilai/kunci."),
                        rows,
                        courseResult,
                        heading("Bandingkan dua skenario"),
                        note(
                                "Simpan perubahan terlebih dahulu. Perbandingan mengambil dua"
                                        + " snapshot tersimpan; basis usang diberi peringatan."),
                        field("Skenario pembanding", comparison),
                        compare,
                        comparisonContent);
        body.setSpacing(8);
        body.getStyleClass().add("tb-surface");
        left.getStyleClass().add("tb-surface");
        body.setPadding(new javafx.geometry.Insets(12));
        ScrollPane catalog = scroll(left);
        catalog.setFitToHeight(true);
        catalog.setMinWidth(210);
        catalog.setFocusTraversable(false);
        SplitPane split = new SplitPane(catalog, scroll(body));
        split.setDividerPositions(0.27);
        setCenter(split);
        context.setPadding(new javafx.geometry.Insets(6));
        setTop(context);
        status.setPadding(new javafx.geometry.Insets(6, 8, 6, 8));
        status.setMinHeight(Region.USE_PREF_SIZE);
        status.setMaxWidth(Double.MAX_VALUE);
        status.visibleProperty().bind(status.textProperty().isNotEmpty());
        status.managedProperty().bind(status.visibleProperty());
        setBottom(status);
        show(null);
    }

    private long preferredCourse;
    void preferCourse(long id) { preferredCourse = id; }

    void load(long semester, List<SemesterScenario> list) {
        this.semester = semester;
        setList(list);
        show(null);
    }

    private void setList(List<SemesterScenario> list) {
        filling = true;
        saved.getItems().setAll(list);
        comparison.getItems().setAll(list);
        if (draft != null)
            saved.getSelectionModel()
                    .select(
                            list.stream()
                                    .filter(s -> s.id() == draft.id())
                                    .findFirst()
                                    .orElse(null));
        filling = false;
    }

    void reloadList() {
        tasks.run(status, () -> service.scenarios(semester), this::setList);
    }

    private void show(SemesterScenario value) {
        long courseId = current == null ? preferredCourse : current.basis().course().id();
        draft = value;
        dirty = false;
        outdated = false;
        ipSources.getChildren().clear();
        current = null;
        filling = true;
        if (value == null) saved.getSelectionModel().clearSelection();
        name.setText(value == null ? "" : value.name());
        gpaTarget.setText(value == null ? "3" : GradeForms.number(value.targetGpa()));
        courses.getItems().setAll(value == null ? List.of() : value.courses());
        courses.getSelectionModel()
                .select(
                        value == null
                                ? null
                                : value.courses().stream()
                                        .filter(c -> c.basis().course().id() == courseId)
                                        .findFirst()
                                        .orElse(
                                                value.courses().isEmpty()
                                                        ? null
                                                        : value.courses().getFirst()));
        filling = false;
        showCourse(courses.getValue());
        gpaResult.setText("");
        review.setText(
                value == null
                        ? "Buat atau buka skenario."
                        : value.id() == 0
                                ? "Draf baru belum disimpan."
                                : "Snapshot tersimpan. Tekan Hitung untuk memeriksa basis dan"
                                        + " hasil.");
        comparisonContent.getChildren().clear();
        if (value != null) calculate();
    }

    void showScenario(SemesterScenario value, long courseId, boolean modified) {
        current =
                value.courses().stream()
                        .filter(c -> c.basis().course().id() == courseId)
                        .findFirst()
                        .orElse(null);
        show(value);
        dirty = modified;
        updateContext();
        reloadList();
    }

    void requestReview() {
        refreshBasis();
    }

    private void updateContext() {
        context.setText(
                draft == null
                        ? "Buat atau buka skenario."
                        : (draft.id() == 0
                                        ? "Sementara"
                                        : dirty ? "Perubahan belum disimpan" : "Tersimpan")
                                + " · "
                                + name.getText()
                                + (current == null ? "" : " · " + current.basis().course().name())
                                + (outdated ? " · BASIS USANG" : ""));
        saveScenario.setText(
                draft != null && draft.id() != 0 ? "Perbarui skenario" : "Simpan skenario");
    }

    private void showCourse(ScenarioCourse value) {
        current = value;
        filling = true;
        locks.clear();
        rows.getChildren().clear();
        projectionEnabled.setSelected(value == null || value.projectionEnabled());
        mode.setValue(value == null ? EvaluationMode.DETAIL : value.mode());
        estimate.setValue(value == null ? null : value.estimatedLetter().orElse(null));
        numericTarget.setText(value == null ? "60" : GradeForms.number(value.targetScore()));
        targetLetter.setValue(
                value == null ? LetterGrade.C : value.targetLetter().orElse(LetterGrade.C));
        targetKind.setValue(
                value != null && value.targetLetter().isEmpty() ? "Angka" : "Huruf minimum");
        attendance.setSelected(value == null || value.attendance().met());
        if (value != null)
            for (AssessmentComponent item : value.basis().components()) {
                boolean actual = item.earned().isPresent(), zero = item.weight().signum() == 0;
                CheckBox locked = new CheckBox(actual ? "Override sementara" : "Kunci asumsi");
                locked.setId("lock-" + item.id());
                Map<String, BigDecimal> activeScores =
                        actual ? value.overriddenScores() : value.lockedScores();
                locked.setSelected(activeScores.containsKey(item.id()));
                locked.setDisable(zero);
                TextField score = text("assumption-" + item.id(), "Skor normalisasi 0–100");
                score.setText(
                        Optional.ofNullable(activeScores.get(item.id()))
                                .map(GradeForms::number)
                                .orElse(""));
                score.setDisable(zero || !locked.isSelected());
                Label suggestion = note("");
                suggestion.setId("suggestion-" + item.id());
                locks.put(item.id(), new LockRow(locked, score, suggestion, actual));
                String state =
                        actual
                                ? "Aktual "
                                        + GradeForms.number(item.earned().orElseThrow())
                                        + "/"
                                        + GradeForms.number(item.maximum().orElseThrow())
                                        + " = "
                                        + GradeForms.number(
                                                item.item()
                                                        .actualScore()
                                                        .orElseThrow()
                                                        .normalized())
                                : zero ? "Bobot nol — tidak ikut solver" : "Belum ada nilai aktual";
                rows.getChildren()
                        .add(
                                new VBox(
                                        4,
                                        note(
                                                item.name()
                                                        + " · "
                                                        + GradeForms.number(item.weight())
                                                        + "% · "
                                                        + state),
                                        new FlowPane(6, 6, locked, score),
                                        suggestion));
                locked.selectedProperty()
                        .addListener(
                                (o, a, b) -> {
                                    score.setDisable(!b || zero);
                                    changed();
                                });
                score.textProperty().addListener((o, a, b) -> changed());
            }
        filling = false;
        updateContext();
        courseResult.setText(
                value == null
                        ? "Pilih mata kuliah."
                        : "Tekan Hitung target & IP setelah mengubah asumsi.");
    }

    private void changed() {
        if (!filling && draft != null) {
            dirty = true;
            updateContext();
            gpaResult.setText("IP belum diperbarui — tekan Hitung target & IP.");
            courseResult.setText("Perubahan belum dihitung.");
            locks.values().forEach(row -> row.suggestion().setText("Belum dihitung."));
            status.setText(
                    "Belum disimpan — hitung untuk melihat hasil terbaru, lalu Simpan skenario.");
        }
    }

    private void capture() {
        if (draft == null)
            throw new IllegalArgumentException("Buat atau buka skenario terlebih dahulu.");
        List<ScenarioCourse> values = new ArrayList<>(draft.courses());
        if (current != null) {
            Map<String, BigDecimal> scores = new LinkedHashMap<>(),
                    overrides = new LinkedHashMap<>();
            for (var row : locks.entrySet())
                if (row.getValue().locked().isSelected() && !row.getValue().locked().isDisable())
                    (row.getValue().actual() ? overrides : scores)
                            .put(
                                    row.getKey(),
                                    DecimalInput.parse(row.getValue().score().getText()));
            Optional<LetterGrade> target =
                    "Huruf minimum".equals(targetKind.getValue())
                            ? Optional.ofNullable(targetLetter.getValue())
                            : Optional.empty();
            if ("Huruf minimum".equals(targetKind.getValue()) && target.isEmpty())
                throw new IllegalArgumentException("Pilih huruf target minimum.");
            Attendance basis = current.basis().course().attendance();
            Attendance chosen =
                    new Attendance(
                            attendance.isSelected(),
                            attendance.isSelected() == basis.met() && basis.confirmed());
            ScenarioCourse input =
                    new ScenarioCourse(
                            current.basis(),
                            mode.getValue(),
                            Optional.ofNullable(estimate.getValue()),
                            target.map(LetterGrade::minimumScore)
                                    .orElseGet(() -> DecimalInput.parse(numericTarget.getText())),
                            target,
                            chosen,
                            scores,
                            overrides,
                            projectionEnabled.isSelected());
            for (int i = 0; i < values.size(); i++)
                if (values.get(i).basis().course().id() == current.basis().course().id()) {
                    values.set(i, input);
                    break;
                }
            current = input;
        }
        draft =
                service.rename(
                        draft, name.getText(), DecimalInput.parse(gpaTarget.getText()), values);
    }

    private void calculate() {
        try {
            capture();
            SemesterScenario input = draft;
            tasks.run(
                    status,
                    () -> service.evaluate(input),
                    value -> {
                        showEvaluation(value);
                        status.setText(
                                "Perhitungan selesai. Tekan Simpan untuk menyimpan perubahan.");
                    });
        } catch (IllegalArgumentException error) {
            status.setText(error.getMessage());
        }
    }

    private void showEvaluation(ScenarioEvaluation value) {
        outdated = value.review().outdated();
        updateContext();
        ipSources.getChildren().clear();
        for (var course : value.courses()) {
            Label source =
                    note(
                            course.input().basis().course().name()
                                    + " · "
                                    + GradeForms.number(course.input().basis().course().credits())
                                    + " SKS · "
                                    + service.resultSource(course)
                                    + "\n"
                                    + GradeForms.compactResult(course.grade()));
            source.setId("ip-source-" + course.input().basis().course().id());
            ipSources.getChildren().add(source);
        }
        review.setText(
                value.review().outdated()
                        ? "USANG / PERLU DITINJAU — hasil berikut adalah snapshot historis.\n"
                                + value.review().message()
                        : "Basis terkini. Hasil memakai snapshot yang sedang dibuka.");
        GpaResult gpa = value.gpa();
        gpaResult.setText(
                (gpa.value().isEmpty() ? "IP belum dapat dihitung lengkap.\n" : "")
                        + (value.review().outdated()
                                ? "Historis (bukan IP semester terkini) · "
                                : "")
                        + gpa.label()
                        + ": "
                        + gpa.displayValue()
                        + " · Target "
                        + GradeForms.number(value.scenario().targetGpa())
                        + gpa.difference()
                                .map(d -> " · Selisih " + GradeForms.approximation(d))
                                .orElse("")
                        + gpa.targetReached()
                                .map(
                                        reached ->
                                                reached
                                                        ? " · Target tercapai"
                                                        : " · Target belum tercapai")
                                .orElse("")
                        + (gpa.issues().isEmpty() ? "" : "\n" + String.join("\n", gpa.issues())));
        if (current == null) return;
        value.courses().stream()
                .filter(c -> c.input().basis().course().id() == current.basis().course().id())
                .findFirst()
                .ifPresent(
                        c -> {
                            courseResult.setText(
                                    c.explanation() + "\n" + GradeForms.result(c.grade()));
                            locks.forEach(
                                    (id, row) ->
                                            row.suggestion()
                                                    .setText(
                                                            row.locked().isSelected()
                                                                    ? (row.actual()
                                                                            ? "Override terkunci"
                                                                                  + " (aktual asli"
                                                                                  + " tetap)"
                                                                            : "Asumsi terkunci"
                                                                                    + " (bukan"
                                                                                    + " aktual)")
                                                                    : c.targetPlan()
                                                                            .map(
                                                                                    p ->
                                                                                            p.suggestedScores()
                                                                                                    .get(
                                                                                                            id))
                                                                            .map(
                                                                                    s ->
                                                                                            "Bebas"
                                                                                                + " → saran"
                                                                                                + " minimum"
                                                                                                + " "
                                                                                                    + GradeForms
                                                                                                            .number(
                                                                                                                    s))
                                                                            .orElse(
                                                                                    "Tidak ada"
                                                                                        + " saran"
                                                                                        + " bebas")));
                        });
    }

    private void save(Runnable done) {
        try {
            capture();
            SemesterScenario input = draft;
            tasks.save(
                    status,
                    () -> service.save(input),
                    value -> {
                        show(value);
                        dirty = false;
                        reloadList();
                        status.setText("Skenario tersimpan.");
                        done.run();
                    },
                    error -> {});
        } catch (IllegalArgumentException error) {
            status.setText(error.getMessage());
        }
    }

    private void refreshBasis() {
        try {
            capture();
            SemesterScenario input = draft;
            tasks.run(
                    status,
                    () -> service.review(input),
                    value -> {
                        if (!value.outdated()) {
                            review.setText("Basis sudah terkini.");
                            return;
                        }
                        ButtonType
                                apply =
                                        new ButtonType(
                                                "Perbarui draf", ButtonBar.ButtonData.OK_DONE),
                                cancel = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
                        Alert dialog =
                                new Alert(
                                        Alert.AlertType.CONFIRMATION,
                                        value.message(),
                                        apply,
                                        cancel);
                        dialog.initOwner(getScene().getWindow());
                        dialog.setHeaderText("Tinjau perubahan basis skenario");
                        dialog.setTitle("Perbarui basis");
                        styleDialog(dialog);
                        if (dialog.showAndWait().filter(apply::equals).isPresent())
                            tasks.run(
                                    status,
                                    () -> service.refresh(input),
                                    updated -> {
                                        show(updated);
                                        dirty = true;
                                        status.setText(
                                                "Basis diperbarui pada draf. Tinjau hasil lalu"
                                                        + " Simpan skenario.");
                                    });
                    });
        } catch (IllegalArgumentException error) {
            status.setText(error.getMessage());
        }
    }

    private void compare() {
        if (dirty) {
            status.setText("Simpan perubahan sebelum membandingkan snapshot tersimpan.");
            return;
        }
        if (draft == null || draft.id() == 0 || comparison.getValue() == null) {
            status.setText("Buka skenario tersimpan dan pilih pembanding.");
            return;
        }
        long first = draft.id(), second = comparison.getValue().id();
        tasks.run(
                status,
                () ->
                        List.of(
                                service.evaluate(service.open(first)),
                                service.evaluate(service.open(second))),
                values -> {
                    var a = values.get(0);
                    var b = values.get(1);
                    Label header =
                            note(
                                    "A: "
                                            + a.scenario().name()
                                            + " · Target IP "
                                            + GradeForms.number(a.scenario().targetGpa())
                                            + "\nB: "
                                            + b.scenario().name()
                                            + " · Target IP "
                                            + GradeForms.number(b.scenario().targetGpa()));
                    header.setId("comparison-context");
                    comparisonContent
                            .getChildren()
                            .setAll(header, note(service.comparisonWarning(a, b)));
                    if (a.scenario().semesterId() != b.scenario().semesterId()) return;
                    TableView<List<String>> table = new TableView<>();
                    table.setId("scenario-comparison-table");
                    String[] headings = {
                        "Mata kuliah",
                        "SKS A / B",
                        a.scenario().name() + " · target/hasil",
                        b.scenario().name() + " · target/hasil"
                    };
                    table.setTooltip(new Tooltip(String.join("\n", headings)));
                    for (int i = 0; i < headings.length; i++) {
                        int column = i;
                        TableColumn<List<String>, String> col = new TableColumn<>(headings[i]);
                        col.setCellValueFactory(
                                cell -> new ReadOnlyStringWrapper(cell.getValue().get(column)));
                        col.setPrefWidth(i == 0 ? 140 : i == 1 ? 80 : 260);
                        col.setMinWidth(i == 0 ? 100 : i == 1 ? 75 : 180);
                        col.setCellFactory(
                                c ->
                                        new TableCell<>() {
                                            private final Label content = note("");

                                            {
                                                content.prefWidthProperty()
                                                        .bind(c.widthProperty().subtract(14));
                                                content.textFillProperty().bind(textFillProperty());
                                                prefHeightProperty()
                                                        .bind(
                                                                javafx.beans.binding.Bindings
                                                                        .createDoubleBinding(
                                                                                () ->
                                                                                        content
                                                                                                        .prefHeight(
                                                                                                                Math
                                                                                                                        .max(
                                                                                                                                1,
                                                                                                                                c
                                                                                                                                                .getWidth()
                                                                                                                                        - 14))
                                                                                                + 12,
                                                                                c.widthProperty(),
                                                                                content
                                                                                        .textProperty(),
                                                                                content
                                                                                        .fontProperty()));
                                            }

                                            @Override
                                            protected void updateItem(String text, boolean empty) {
                                                super.updateItem(text, empty);
                                                setText(null);
                                                content.setText(empty ? "" : text);
                                                setGraphic(empty ? null : content);
                                                setTooltip(empty ? null : new Tooltip(text));
                                            }
                                        });
                        table.getColumns().add(col);
                    }
                    table.setColumnResizePolicy(
                            TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
                    Map<Long, ScenarioEvaluation.CourseEvaluation> left = new LinkedHashMap<>(),
                            right = new LinkedHashMap<>();
                    a.courses().forEach(c -> left.put(c.input().basis().course().id(), c));
                    b.courses().forEach(c -> right.put(c.input().basis().course().id(), c));
                    Set<Long> ids = new LinkedHashSet<>(left.keySet());
                    ids.addAll(right.keySet());
                    VBox details = new VBox(8);
                    details.setId("comparison-details");
                    for (long id : ids) {
                        var x = left.get(id);
                        var y = right.get(id);
                        var available = x == null ? y : x;
                        table.getItems()
                                .add(
                                        List.of(
                                                available.input().basis().course().name(),
                                                (x == null
                                                                ? "Tidak ada"
                                                                : GradeForms.number(
                                                                        x.input()
                                                                                .basis()
                                                                                .course()
                                                                                .credits()))
                                                        + " / "
                                                        + (y == null
                                                                ? "Tidak ada"
                                                                : GradeForms.number(
                                                                        y.input()
                                                                                .basis()
                                                                                .course()
                                                                                .credits())),
                                                comparisonCourse(x),
                                                comparisonCourse(y)));
                        details.getChildren().add(componentComparison(x, y));
                    }
                    table.setPrefHeight(Math.min(360, 70 + ids.size() * 100));
                    comparisonContent
                            .getChildren()
                            .addAll(
                                    table,
                                    note(
                                            (a.review().outdated() ? "Historis · " : "")
                                                    + a.scenario().name()
                                                    + ": "
                                                    + a.gpa().label()
                                                    + " "
                                                    + a.gpa().displayValue()
                                                    + "\n"
                                                    + (b.review().outdated() ? "Historis · " : "")
                                                    + b.scenario().name()
                                                    + ": "
                                                    + b.gpa().label()
                                                    + " "
                                                    + b.gpa().displayValue()),
                                    details);
                });
    }

    private String comparisonCourse(ScenarioEvaluation.CourseEvaluation value) {
        if (value == null) return "Tidak ada dalam snapshot";
        ScenarioCourse input = value.input();
        String target =
                !input.projectionEnabled()
                        ? "Hanya aktual (target tidak dipakai)"
                        : input.mode() == EvaluationMode.DIRECT
                                ? "Huruf langsung / estimasi"
                                : "Target "
                                        + GradeForms.number(input.targetScore())
                                        + " / "
                                        + LetterGrade.fromScore(input.targetScore()).label();
        return target
                + "\n"
                + GradeForms.compactResult(value.grade())
                + "\n"
                + service.resultSource(value);
    }

    private TitledPane componentComparison(
            ScenarioEvaluation.CourseEvaluation a, ScenarioEvaluation.CourseEvaluation b) {
        ScenarioCourse x = a == null ? null : a.input(), y = b == null ? null : b.input();
        var available = x == null ? y : x;
        VBox body = new VBox(8);
        body.getChildren()
                .add(
                        note(
                                x == null || y == null
                                        ? "Mata kuliah hanya ada pada satu snapshot; tidak"
                                                + " disejajarkan berdasarkan urutan."
                                        : x.basis().equals(y.basis())
                                                ? "Basis aktual sama. Perbedaan di bawah berasal"
                                                        + " dari rencana."
                                                : "BASIS BERBEDA — hasil bukan perbandingan asumsi"
                                                        + " saja. Revisi A/B: "
                                                        + x.basis().course().revision()
                                                        + " / "
                                                        + y.basis().course().revision()
                                                        + "; SKS: "
                                                        + GradeForms.number(
                                                                x.basis().course().credits())
                                                        + " / "
                                                        + GradeForms.number(
                                                                y.basis().course().credits())));
        body.getChildren()
                .add(note("Kehadiran A: " + attendanceText(x) + " · B: " + attendanceText(y)));
        Map<String, AssessmentComponent> left = new LinkedHashMap<>(),
                right = new LinkedHashMap<>();
        if (x != null) x.basis().components().forEach(c -> left.put(c.id(), c));
        if (y != null) y.basis().components().forEach(c -> right.put(c.id(), c));
        Set<String> ids = new LinkedHashSet<>(left.keySet());
        ids.addAll(right.keySet());
        for (String id : ids) {
            var ca = left.get(id);
            var cb = right.get(id);
            String name = (ca == null ? cb : ca).name();
            Label label =
                    note(
                            name
                                    + " · Bobot A/B: "
                                    + (ca == null ? "Tidak ada" : GradeForms.number(ca.weight()))
                                    + " / "
                                    + (cb == null ? "Tidak ada" : GradeForms.number(cb.weight()))
                                    + "%\nAktual A: "
                                    + actualText(ca)
                                    + " · B: "
                                    + actualText(cb)
                                    + "\nA: "
                                    + assumptionText(x, id)
                                    + " · Kebutuhan: "
                                    + requirement(a, id)
                                    + "\nB: "
                                    + assumptionText(y, id)
                                    + " · Kebutuhan: "
                                    + requirement(b, id));
            label.setId("comparison-item-" + id);
            label.setTooltip(new Tooltip("Identitas komponen: " + id));
            body.getChildren().add(label);
        }
        TitledPane pane =
                new TitledPane(
                        available.basis().course().name() + " — rincian komponen/basis", body);
        pane.setExpanded(false);
        pane.setAnimated(false);
        return pane;
    }

    private static String actualText(AssessmentComponent c) {
        return c == null
                ? "Tidak ada di snapshot"
                : c.earned()
                        .map(
                                n ->
                                        GradeForms.number(n)
                                                + "/"
                                                + GradeForms.number(c.maximum().orElseThrow()))
                        .orElse("Kosong");
    }

    private static String assumptionText(ScenarioCourse c, String id) {
        if (c == null || c.basis().components().stream().noneMatch(item -> item.id().equals(id)))
            return "Tidak ada di snapshot";
        if (!c.projectionEnabled()) return "Hanya aktual (asumsi tidak dipakai)";
        if (c.overriddenScores().containsKey(id))
            return "Override " + GradeForms.number(c.overriddenScores().get(id));
        return c.lockedScores().containsKey(id)
                ? "Kunci " + GradeForms.number(c.lockedScores().get(id))
                : "Tanpa kunci/override";
    }

    private static String requirement(ScenarioEvaluation.CourseEvaluation c, String id) {
        return c == null
                ? "Tidak ada"
                : c.targetPlan()
                        .map(
                                p ->
                                        Optional.ofNullable(p.suggestedScores().get(id))
                                                .map(GradeForms::number)
                                                .orElse(
                                                        p.status() == TargetPlan.Status.IMPOSSIBLE
                                                                ? ">100 (mustahil)"
                                                                : "—"))
                        .orElse("Belum lengkap / tidak memakai target");
    }

    private static String attendanceText(ScenarioCourse c) {
        if (c == null) return "Tidak ada";
        Attendance a = c.attendance();
        return (a.met() ? "Terpenuhi" : "Tidak terpenuhi → E/0")
                + (a.confirmed() ? " (terverifikasi)" : " (asumsi)");
    }

    void guard(Runnable action) {
        GradeForms.guard(this, dirty, () -> show(null), this::save, action);
    }
}
