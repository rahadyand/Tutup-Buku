package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.service.*;

import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.*;

/** Satu draf course/semester aktif; save tetap melalui GradePlanningService dan CAS skenario. */
final class WhatIfPane extends BorderPane {
    private final GradePlanningService service;
    private final UiTasks tasks;
    private final Label context = note(""),
            state = status("what-if-state"),
            targetError = status("what-if-target-error"),
            actualResult = status("what-if-actual"),
            projection = status("what-if-projection"),
            explanation = note(""),
            status = status("what-if-status"),
            staleText = note("");
    private final TextField target = text("what-if-target", "0–100");
    private final ComboBox<LetterGrade> letter = GradeForms.letters("what-if-letter");
    private final VBox rows = new VBox(8);
    private final ScrollPane page;
    private final TitledPane stale;
    private final Button save = button("what-if-save", "Simpan sebagai skenario"),
            copy = button("what-if-copy", "Simpan salinan"),
            reset = button("what-if-reset", "Reset sementara");
    private final Map<String, AssumptionRow> inputs = new LinkedHashMap<>();
    private ActualCourse baseline;
    private ScenarioCourse initial;
    private SemesterScenario scenario;
    private boolean filling, dirty, saving, projectionEnabled, outdated;
    private Optional<LetterGrade> targetChoice = Optional.empty();
    private String pendingName;
    private long revision;

    private record AssumptionRow(
            AssessmentComponent item,
            CheckBox locked,
            TextField score,
            Label error,
            Label output) {}

    WhatIfPane(
            GradePlanningService service,
            UiTasks tasks,
            Runnable returnActual,
            Runnable openSaved,
            Runnable reviewSaved) {
        this.service = service;
        this.tasks = tasks;
        setId("what-if-pane");
        context.setId("what-if-context");
        explanation.setId("what-if-explanation");
        limitWidth(target, 100);
        limitWidth(letter, 100);
        Button back = button("what-if-back", "Kembali ke aktual"),
                saved = button("what-if-saved", "IP & perbandingan");
        back.setOnAction(e -> returnActual.run());
        saved.setOnAction(e -> openSaved.run());
        reset.setOnAction(e -> reset());
        save.setOnAction(e -> save(false, () -> {}));
        copy.setOnAction(e -> save(true, () -> {}));
        save.getStyleClass().add("tb-primary");
        Button review = button("what-if-review", "Tinjau / perbarui basis");
        review.setOnAction(e -> reviewSaved.run());
        stale = new TitledPane("Basis lama — tinjau perubahan", new VBox(6, staleText, review));
        stale.setId("what-if-stale");
        stale.setExpanded(false);
        stale.setAnimated(false);
        stale.setVisible(false);
        stale.setManaged(false);
        VBox header =
                new VBox(
                        4,
                        context,
                        new FlowPane(
                                8,
                                6,
                                field("Target angka (0–100)", target),
                                field("atau huruf minimum", letter),
                                back,
                                reset,
                                saved),
                        note(
                                "Eksperimen tidak mengubah aktual. Skenario menyimpan rencana"
                                        + " semester terpisah."),
                        targetError);
        header.setPadding(new javafx.geometry.Insets(6, 8, 6, 8));
        context.getStyleClass().add("tb-section-title");
        projection.getStyleClass().add("tb-result");
        hideEmpty(targetError);
        hideEmpty(status);
        VBox body =
                new VBox(
                        8,
                        new FlowPane(8, 6, save, copy, state),
                        status,
                        stale,
                        actualResult,
                        projection,
                        explanation,
                        rows);
        body.setPadding(new javafx.geometry.Insets(8));
        body.setMinHeight(Region.USE_PREF_SIZE);
        page = scroll(body);
        setTop(header);
        setCenter(page);
        target.textProperty()
                .addListener(
                        (o, a, b) -> {
                            if (!filling) targetChoice = Optional.empty();
                            changed();
                        });
        letter.valueProperty()
                .addListener(
                        (o, a, b) -> {
                            if (filling || b == null) return;
                            filling = true;
                            target.setText(GradeForms.number(b.minimumScore()));
                            targetChoice = Optional.of(b);
                            filling = false;
                            changed();
                        });
    }

    void open(ActualCourse course) {
        openScenario(null, ScenarioCourse.fromActual(course), false);
    }

    void openScenario(SemesterScenario source, ScenarioCourse input, boolean modified) {
        revision++;
        scenario = source;
        initial = input;
        baseline = input.basis();
        outdated = false;
        pendingName = null;
        filling = true;
        inputs.clear();
        rows.getChildren().clear();
        targetChoice = input.targetLetter();
        target.setText(GradeForms.number(input.targetScore()));
        letter.setValue(LetterGrade.fromScore(input.targetScore()));
        projectionEnabled = input.projectionEnabled();
        boolean direct = input.mode() == EvaluationMode.DIRECT;
        target.setDisable(direct);
        letter.setDisable(direct);
        context.setText(
                "Simulasi Target · "
                        + baseline.course().name()
                        + " · "
                        + GradeForms.number(baseline.course().credits())
                        + " SKS · Bobot "
                        + GradeForms.number(baseline.totalWeight())
                        + "%");
        actualResult.setText("Membaca hasil pada basis…");
        if (!direct) for (AssessmentComponent item : baseline.components()) addRow(item, input);
        filling = false;
        dirty = modified;
        updateState();
        ActualCourse basis = baseline;
        tasks.background(
                status,
                () -> service.actualResult(basis),
                value -> {
                    if (baseline == basis)
                        actualResult.setText(
                                "Aktual pada basis — " + GradeForms.compactResult(value));
                });
        if (source != null)
            tasks.background(
                    status,
                    () -> service.review(source),
                    review -> {
                        if (scenario != source) return;
                        outdated = review.outdated();
                        staleText.setText(review.message());
                        updateState();
                    });
        recalculate();
    }

    void clear() {
        revision++;
        baseline = null;
        scenario = null;
        dirty = false;
    }

    boolean dirty() {
        return dirty;
    }

    void error(String message) {
        status.setText(message);
    }

    long courseId() {
        return baseline.course().id();
    }

    boolean hasScenario() {
        return scenario != null;
    }

    SemesterScenario scenarioDraft() {
        return scenario == null ? null : service.replaceCourse(scenario, capture());
    }

    private void updateState() {
        boolean stored = scenario != null && scenario.id() != 0;
        state.setText(
                (stored
                                ? (dirty ? "Perubahan belum disimpan — " : "Tersimpan — ")
                                        + scenario.name()
                                : "Sementara — belum disimpan")
                        + (outdated ? " · BASIS USANG" : ""));
        save.setText(stored ? "Perbarui skenario" : "Simpan sebagai skenario");
        copy.setVisible(stored);
        copy.setManaged(stored);
        reset.setText(stored ? "Pulihkan tersimpan" : "Reset sementara");
        stale.setVisible(outdated);
        stale.setManaged(outdated);
    }

    private void addRow(AssessmentComponent item, ScenarioCourse input) {
        boolean actual = item.earned().isPresent(), zero = item.weight().signum() == 0;
        CheckBox locked = new CheckBox(actual ? "Override sementara" : "Kunci asumsi");
        locked.setId("what-if-lock-" + item.id());
        Map<String, BigDecimal> scores = actual ? input.overriddenScores() : input.lockedScores();
        locked.setSelected(scores.containsKey(item.id()));
        locked.setDisable(zero);
        TextField score = text("what-if-score-" + item.id(), "0–100");
        limitWidth(score, 100);
        score.setAccessibleText((actual ? "Override " : "Asumsi ") + item.name() + " skala0–100");
        String original =
                actual
                        ? GradeForms.number(item.item().actualScore().orElseThrow().normalized())
                        : "";
        score.setText(
                scores.containsKey(item.id())
                        ? GradeForms.number(scores.get(item.id()))
                        : original);
        score.setDisable(zero || !locked.isSelected());
        Label base =
                note(
                        actual
                                ? "Aktual asli: "
                                        + GradeForms.number(item.earned().orElseThrow())
                                        + "/"
                                        + GradeForms.number(item.maximum().orElseThrow())
                                        + " = "
                                        + original
                                        + "/100"
                                : "Aktual: belum ada nilai (kosong bukan nol)");
        base.setId("what-if-original-" + item.id());
        Label error = status("what-if-error-" + item.id()), output = note("");
        output.setId("what-if-required-" + item.id());
        hideEmpty(error);
        Button restore = button("what-if-restore-" + item.id(), "Pulihkan aktual");
        restore.setOnAction(e -> locked.setSelected(false));
        FlowPane controls = new FlowPane(8, 6, locked, score);
        if (actual) controls.getChildren().add(restore);
        controls.getChildren().add(output);
        VBox row =
                new VBox(
                        4,
                        new FlowPane(
                                8,
                                4,
                                note(item.name() + " · " + GradeForms.number(item.weight()) + "%"),
                                base),
                        controls,
                        error);
        row.setPadding(new javafx.geometry.Insets(6));
        row.getStyleClass().add("tb-surface");
        rows.getChildren().add(row);
        inputs.put(item.id(), new AssumptionRow(item, locked, score, error, output));
        locked.selectedProperty()
                .addListener(
                        (o, a, b) -> {
                            score.setDisable(!b || zero);
                            if (actual && !b) {
                                filling = true;
                                score.setText(original);
                                filling = false;
                            }
                            changed();
                        });
        score.textProperty().addListener((o, a, b) -> changed());
    }

    private void changed() {
        if (filling || baseline == null) return;
        dirty = true;
        projectionEnabled = true;
        updateState();
        recalculate();
    }

    private static BigDecimal score(String text) {
        BigDecimal value = DecimalInput.parse(text);
        LetterGrade.fromScore(value);
        return value;
    }

    private ScenarioCourse capture() {
        targetError.setText("");
        inputs.values().forEach(row -> row.error().setText(""));
        BigDecimal value;
        try {
            value = score(target.getText());
        } catch (IllegalArgumentException error) {
            targetError.setText(error.getMessage());
            throw error;
        }
        Map<String, BigDecimal> locks = new LinkedHashMap<>(), overrides = new LinkedHashMap<>();
        boolean valid = true;
        for (var entry : inputs.entrySet()) {
            AssumptionRow row = entry.getValue();
            if (!row.locked().isSelected() || row.item().weight().signum() == 0) continue;
            if (row.score().getText().isBlank() && row.item().earned().isEmpty()) continue;
            try {
                (row.item().earned().isPresent() ? overrides : locks)
                        .put(entry.getKey(), score(row.score().getText()));
            } catch (IllegalArgumentException error) {
                row.error().setText("Asumsi: " + error.getMessage());
                valid = false;
            }
        }
        if (!valid) throw new IllegalArgumentException("Perbaiki asumsi yang ditandai.");
        boolean direct = initial.mode() == EvaluationMode.DIRECT;
        return new ScenarioCourse(
                baseline,
                initial.mode(),
                initial.estimatedLetter(),
                value,
                targetChoice,
                initial.attendance(),
                direct ? initial.lockedScores() : locks,
                direct ? initial.overriddenScores() : overrides,
                projectionEnabled);
    }

    private void recalculate() {
        long request = ++revision;
        ActualCourse source = baseline;
        explanation.setText("");
        status.setText("");
        inputs.values().forEach(row -> row.output().setText("—"));
        projection.setText("Proyeksi belum dihitung.");
        ScenarioCourse input;
        try {
            input = capture();
            filling = true;
            letter.setValue(LetterGrade.fromScore(input.targetScore()));
            filling = false;
        } catch (IllegalArgumentException error) {
            filling = true;
            letter.setValue(null);
            filling = false;
            projection.setText("Proyeksi ditahan — perbaiki isian.");
            return;
        }
        status.setText("Menghitung…");
        tasks.background(
                status,
                () -> service.evaluateCourse(input),
                evaluated -> {
                    if (revision != request || baseline != source) return;
                    projection.setText(
                            (input.mode() == EvaluationMode.DIRECT
                                            ? "Mode huruf langsung — "
                                            : "Proyeksi jika tercapai — ")
                                    + GradeForms.compactResult(evaluated.grade()));
                    explanation.setText(
                            "Pada rencana ini: "
                                    + evaluated.explanation()
                                    + (input.mode() == EvaluationMode.DIRECT
                                            ? " Gunakan estimasi huruf pada layar IP &"
                                                    + " perbandingan; tidak membentuk skor rekaan."
                                            : evaluated.targetPlan().isEmpty()
                                                            && input.projectionEnabled()
                                                    ? " Perbaiki struktur melalui Kembali ke aktual"
                                                            + " → Komponen & aktual."
                                                    : ""));
                    inputs.forEach(
                            (id, row) -> {
                                String text;
                                if (row.item().weight().signum() == 0)
                                    text = "Bobot nol — tidak ikut perhitungan/solver.";
                                else if (!input.projectionEnabled())
                                    text =
                                            "Hanya aktual; ubah target/asumsi untuk mengaktifkan"
                                                    + " proyeksi.";
                                else if (input.overriddenScores().containsKey(id))
                                    text =
                                            "Override terkunci: "
                                                    + GradeForms.number(
                                                            input.overriddenScores().get(id))
                                                    + "/100";
                                else if (row.item().earned().isPresent())
                                    text = "Aktual tetap (tidak diubah).";
                                else if (input.lockedScores().containsKey(id))
                                    text =
                                            "Asumsi terkunci: "
                                                    + GradeForms.number(
                                                            input.lockedScores().get(id))
                                                    + "/100";
                                else
                                    text =
                                            evaluated
                                                    .targetPlan()
                                                    .map(
                                                            plan ->
                                                                    plan.status()
                                                                                    == TargetPlan
                                                                                            .Status
                                                                                            .IMPOSSIBLE
                                                                            ? "Diperlukan: "
                                                                                    + plan.rawRequiredScore()
                                                                                            .filter(
                                                                                                    n ->
                                                                                                            n
                                                                                                                            .compareTo(
                                                                                                                                    new BigDecimal(
                                                                                                                                            "100"))
                                                                                                                    > 0)
                                                                                            .map(
                                                                                                    GradeForms
                                                                                                            ::number)
                                                                                            .orElse(
                                                                                                    ">100")
                                                                                    + "/100 —"
                                                                                    + " mustahil"
                                                                                    + " (>100)."
                                                                            : Optional.ofNullable(
                                                                                            plan.suggestedScores()
                                                                                                    .get(
                                                                                                            id))
                                                                                    .map(
                                                                                            n ->
                                                                                                    "Diperlukan:"
                                                                                                        + " "
                                                                                                            + GradeForms
                                                                                                                    .number(
                                                                                                                            n)
                                                                                                            + "/100 (bebas,"
                                                                                                            + " bukan"
                                                                                                            + " aktual)")
                                                                                    .orElse(
                                                                                            "Tidak"
                                                                                                + " ada komponen"
                                                                                                + " bebas."))
                                                    .orElse(
                                                            "Kebutuhan belum dapat dihitung;"
                                                                + " periksa struktur komponen di"
                                                                + " editor aktual.");
                                row.output().setText(text);
                            });
                    status.setText("");
                });
    }

    private void save(boolean asCopy, Runnable done) {
        if (saving) return;
        ScenarioCourse input;
        try {
            input = capture();
        } catch (IllegalArgumentException error) {
            status.setText(error.getMessage());
            return;
        }
        SemesterScenario source = scenario;
        String name = source == null ? "Rencana " + baseline.course().name() : source.name();
        if (source == null || source.id() == 0 || asCopy) {
            TextInputDialog dialog =
                    new TextInputDialog(
                            pendingName != null
                                    ? pendingName
                                    : name + (asCopy ? " (salinan)" : ""));
            dialog.initOwner(getScene().getWindow());
            dialog.setTitle(asCopy ? "Simpan salinan" : "Simpan sebagai skenario");
            dialog.setHeaderText("Nama rencana semester");
            dialog.setContentText("Nama skenario");
            dialog.getEditor().setId("what-if-name");
            styleDialog(dialog);
            var chosen = dialog.showAndWait();
            if (chosen.isEmpty()) return;
            name = chosen.get();
            pendingName = name;
        }
        String chosenName = name;
        long course = input.basis().course().id();
        double position = page.getVvalue();
        saving = true;
        tasks.save(
                status,
                () -> {
                    SemesterScenario draft =
                            source == null
                                    ? service.fromWhatIf(input, chosenName)
                                    : service.replaceCourse(source, input);
                    if (source == null || source.id() == 0)
                        draft =
                                service.rename(
                                        draft, chosenName, draft.targetGpa(), draft.courses());
                    return asCopy ? service.saveCopy(draft, chosenName) : service.save(draft);
                },
                saved -> {
                    saving = false;
                    openScenario(
                            saved,
                            saved.courses().stream()
                                    .filter(c -> c.basis().course().id() == course)
                                    .findFirst()
                                    .orElseThrow(),
                            false);
                    javafx.application.Platform.runLater(() -> page.setVvalue(position));
                    done.run();
                },
                error -> {
                    saving = false;
                    status.setText("Gagal menyimpan: " + error.getMessage());
                    page.setVvalue(0);
                });
    }

    private void reset() {
        discardGuard(
                () -> {
                    if (scenario == null || scenario.id() == 0) {
                        openScenario(
                                scenario, ScenarioCourse.fromActual(baseline), scenario != null);
                        return;
                    }
                    long course = courseId(), id = scenario.id();
                    tasks.run(
                            status,
                            () -> service.open(id),
                            saved -> {
                                var input =
                                        saved.courses().stream()
                                                .filter(c -> c.basis().course().id() == course)
                                                .findFirst();
                                if (input.isEmpty()) {
                                    status.setText(
                                            "Mata kuliah tidak ada pada versi tersimpan; draf tetap"
                                                    + " dipertahankan.");
                                    return;
                                }
                                openScenario(saved, input.orElseThrow(), false);
                            });
                });
    }

    void guard(Runnable action) {
        GradeForms.guard(this, dirty, () -> {}, done -> save(false, done), action);
    }

    private void discardGuard(Runnable action) {
        if (!dirty) {
            action.run();
            return;
        }
        ButtonType discard = new ButtonType("Buang perubahan draf", ButtonBar.ButtonData.NO),
                cancel = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert dialog =
                new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Reset hanya memulihkan basis draf/versi skenario tersimpan; tidak"
                                + " mengambil nilai aktual terbaru.",
                        discard,
                        cancel);
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Pulihkan draf");
        dialog.setHeaderText("Pulihkan tanpa menyimpan perubahan ini?");
        ((Button) dialog.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        styleDialog(dialog);
        if (dialog.showAndWait().filter(discard::equals).isPresent()) action.run();
    }

    private static void hideEmpty(Label label) {
        label.visibleProperty().bind(label.textProperty().isNotEmpty());
        label.managedProperty().bind(label.visibleProperty());
    }
}
