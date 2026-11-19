package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.service.*;

import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.*;

/** Draf per mata kuliah di memori; tidak mempunyai jalur simpan ke repository. */
final class WhatIfPane extends BorderPane {
    private final GradePlanningService service;
    private final UiTasks tasks;
    private final Label context = note(""),
            targetError = status("what-if-target-error"),
            actualResult = status("what-if-actual"),
            projection = status("what-if-projection"),
            explanation = note(""),
            status = status("what-if-status");
    private final TextField target = text("what-if-target", "0–100");
    private final ComboBox<LetterGrade> letter = GradeForms.letters("what-if-letter");
    private final VBox rows = new VBox(8);
    private final Map<String, AssumptionRow> inputs = new LinkedHashMap<>();
    private ActualCourse baseline;
    private boolean filling, dirty;
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
            Runnable openSaved) {
        this.service = service;
        this.tasks = tasks;
        setId("what-if-pane");
        context.setId("what-if-context");
        explanation.setId("what-if-explanation");
        limitWidth(target, 100);
        limitWidth(letter, 100);
        Button back = button("what-if-back", "Kembali ke aktual"),
                reset = button("what-if-reset", "Reset"),
                saved = button("what-if-saved", "Skenario tersimpan");
        back.setOnAction(event -> returnActual.run());
        saved.setOnAction(event -> openSaved.run());
        reset.setOnAction(event -> guard(() -> open(baseline)));
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
                        note("Simulasi sementara — perubahan di sini tidak mengubah nilai aktual."),
                        targetError);
        header.setPadding(new javafx.geometry.Insets(6, 8, 6, 8));
        context.getStyleClass().add("tb-section-title");
        projection.getStyleClass().add("tb-result");
        hideEmpty(targetError);
        hideEmpty(status);
        VBox body = new VBox(8, actualResult, projection, explanation, rows, status);
        body.setPadding(new javafx.geometry.Insets(8));
        body.setMinHeight(Region.USE_PREF_SIZE);
        setTop(header);
        setCenter(scroll(body));
        target.textProperty().addListener((o, a, b) -> changed());
        letter.valueProperty()
                .addListener(
                        (o, a, b) -> {
                            if (filling || b == null) return;
                            filling = true;
                            target.setText(GradeForms.number(b.minimumScore()));
                            filling = false;
                            changed();
                        });
    }

    void open(ActualCourse course) {
        baseline = course;
        filling = true;
        inputs.clear();
        rows.getChildren().clear();
        target.setText("60");
        letter.setValue(LetterGrade.C);
        boolean direct = course.mode() == EvaluationMode.DIRECT;
        target.setDisable(direct);
        letter.setDisable(direct);
        context.setText(
                "Simulasi Target · "
                        + course.course().name()
                        + " · "
                        + GradeForms.number(course.course().credits())
                        + " SKS · Bobot "
                        + GradeForms.number(course.totalWeight())
                        + "%");
        actualResult.setText("Membaca hasil aktual…");
        if (!direct) for (AssessmentComponent item : course.components()) addRow(item);
        dirty = false;
        filling = false;
        tasks.background(
                status,
                () -> service.actualResult(course),
                value -> {
                    if (baseline == course)
                        actualResult.setText("Aktual tersimpan — " + compactResult(value));
                });
        recalculate();
    }

    void clear() {
        revision++;
        baseline = null;
        dirty = false;
    }

    private void addRow(AssessmentComponent item) {
        boolean actual = item.earned().isPresent(), zero = item.weight().signum() == 0;
        CheckBox locked = new CheckBox(actual ? "Override sementara" : "Kunci asumsi");
        locked.setId("what-if-lock-" + item.id());
        locked.setDisable(zero);
        TextField score = text("what-if-score-" + item.id(), "0–100");
        limitWidth(score, 100);
        score.setDisable(true);
        if (actual)
            score.setText(GradeForms.number(item.item().actualScore().orElseThrow().normalized()));
        Label original =
                note(
                        actual
                                ? "Aktual asli: "
                                        + GradeForms.number(item.earned().orElseThrow())
                                        + "/"
                                        + GradeForms.number(item.maximum().orElseThrow())
                                        + " = "
                                        + score.getText()
                                        + "/100"
                                : "Aktual: belum ada nilai (kosong bukan nol)");
        original.setId("what-if-original-" + item.id());
        Label error = status("what-if-error-" + item.id()), output = note("");
        output.setId("what-if-required-" + item.id());
        hideEmpty(error);
        Button restore = button("what-if-restore-" + item.id(), "Pulihkan aktual");
        restore.setOnAction(event -> locked.setSelected(false));
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
                                original),
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
                                score.setText(
                                        GradeForms.number(
                                                item.item()
                                                        .actualScore()
                                                        .orElseThrow()
                                                        .normalized()));
                                filling = false;
                            }
                            changed();
                        });
        score.textProperty().addListener((o, a, b) -> changed());
    }

    private void changed() {
        if (filling || baseline == null) return;
        dirty = true;
        recalculate();
    }

    private static BigDecimal score(String text) {
        BigDecimal value = DecimalInput.parse(text);
        LetterGrade.fromScore(value); // Validasi rentang yang sama dengan domain, tanpa pembulatan.
        return value;
    }

    private void recalculate() {
        long request = ++revision;
        ActualCourse source = baseline;
        targetError.setText("");
        explanation.setText("");
        status.setText("");
        inputs.values()
                .forEach(
                        row -> {
                            row.error().setText("");
                            row.output().setText("—");
                        });
        projection.setText("Proyeksi belum dihitung.");
        if (source.mode() == EvaluationMode.DIRECT) {
            projection.setText("Mode huruf langsung — tidak membuat skor komponen rekaan.");
            explanation.setText(
                    "Gunakan estimasi huruf pada Skenario, target & IP, atau kembali ke editor"
                        + " aktual untuk mengubah mode penilaian.");
            return;
        }
        BigDecimal targetScore;
        try {
            targetScore = score(target.getText());
            filling = true;
            letter.setValue(LetterGrade.fromScore(targetScore));
            filling = false;
        } catch (IllegalArgumentException error) {
            filling = true;
            letter.setValue(null);
            filling = false;
            targetError.setText(error.getMessage());
            projection.setText("Proyeksi ditahan — perbaiki target.");
            return;
        }
        Map<String, BigDecimal> assumptions = new LinkedHashMap<>();
        Set<String> overrides = new HashSet<>();
        boolean valid = true;
        for (var entry : inputs.entrySet()) {
            AssumptionRow row = entry.getValue();
            if (!row.locked().isSelected() || row.item().weight().signum() == 0) continue;
            if (row.score().getText().isBlank() && row.item().earned().isEmpty()) continue;
            try {
                assumptions.put(entry.getKey(), score(row.score().getText()));
                if (row.item().earned().isPresent()) overrides.add(entry.getKey());
            } catch (IllegalArgumentException error) {
                row.error().setText("Asumsi: " + error.getMessage());
                valid = false;
            }
        }
        if (!valid) {
            projection.setText("Proyeksi ditahan — perbaiki asumsi yang ditandai.");
            return;
        }
        status.setText("Menghitung…");
        Map<String, BigDecimal> locks = Map.copyOf(assumptions);
        Set<String> replaced = Set.copyOf(overrides);
        tasks.background(
                status,
                () -> service.whatIf(source, targetScore, locks, replaced),
                evaluated -> {
                    if (revision != request || baseline != source) return;
                    // Seluruh hasil satu revisi diterapkan bersama; hasil antrean lama diabaikan.
                    projection.setText(
                            "Proyeksi jika tercapai — " + compactResult(evaluated.grade()));
                    explanation.setText(
                            "Pada simulasi ini: "
                                    + evaluated.explanation()
                                    + (evaluated.targetPlan().isEmpty()
                                            ? "\n"
                                                  + "Perbaiki struktur melalui Kembali ke aktual →"
                                                  + " Komponen & aktual."
                                            : ""));
                    inputs.forEach(
                            (id, row) -> {
                                String text;
                                if (row.item().weight().signum() == 0)
                                    text = "Bobot nol — tidak ikut perhitungan/solver.";
                                else if (row.item().earned().isPresent() && !replaced.contains(id))
                                    text = "Aktual tetap (tidak diubah).";
                                else if (locks.containsKey(id))
                                    text =
                                            (replaced.contains(id)
                                                            ? "Override terkunci: "
                                                            : "Asumsi terkunci: ")
                                                    + GradeForms.number(locks.get(id))
                                                    + "/100";
                                else
                                    text =
                                            evaluated
                                                    .targetPlan()
                                                    .map(
                                                            plan -> {
                                                                if (plan.status()
                                                                        == TargetPlan.Status
                                                                                .IMPOSSIBLE)
                                                                    return "Diperlukan: "
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
                                                                                    .orElse(">100")
                                                                            + "/100 — mustahil"
                                                                            + " (>100).";
                                                                return Optional.ofNullable(
                                                                                plan.suggestedScores()
                                                                                        .get(id))
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
                                                                                "Tidak ada komponen"
                                                                                    + " bebas.");
                                                            })
                                                    .orElse(
                                                            "Kebutuhan belum dapat dihitung;"
                                                                + " periksa struktur komponen di"
                                                                + " editor aktual.");
                                row.output().setText(text);
                            });
                    status.setText("");
                });
    }

    private static String compactResult(GradeResult grade) {
        return "Asal: "
                + grade.underlyingScore().map(GradeForms::approximation).orElse("—")
                + " / "
                + GradeForms.letter(grade.underlyingLetter())
                + " · Efektif: "
                + GradeForms.letter(grade.effectiveLetter())
                + " · "
                + grade.effectiveLetter()
                        .map(g -> g.isPassing() ? "Lulus" : "Tidak lulus")
                        .orElse("Belum lengkap");
    }

    void guard(Runnable action) {
        if (!dirty) {
            action.run();
            return;
        }
        ButtonType discard = new ButtonType("Buang simulasi sementara", ButtonBar.ButtonData.NO),
                cancel = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert dialog =
                new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Target dan asumsi sementara akan dibuang. Nilai aktual dan skenario"
                            + " tersimpan tetap ada.",
                        discard,
                        cancel);
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Simulasi sementara");
        dialog.setHeaderText("Tinggalkan atau reset simulasi ini?");
        ((Button) dialog.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        styleDialog(dialog);
        if (dialog.showAndWait().filter(discard::equals).isPresent()) action.run();
    }

    private static void hideEmpty(Label label) {
        label.visibleProperty().bind(label.textProperty().isNotEmpty());
        label.managedProperty().bind(label.visibleProperty());
    }
}
