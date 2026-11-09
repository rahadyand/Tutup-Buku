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

    private record LockRow(CheckBox locked, TextField score, Label suggestion) {}

    ScenarioPane(GradePlanningService service, UiTasks tasks) {
        this.service = service;
        this.tasks = tasks;
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
                duplicate = button("duplicate-scenario", "Duplikasi"),
                delete = button("delete-scenario", "Hapus skenario"),
                save = button("save-scenario", "Simpan skenario"),
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
                e ->
                        guard(
                                () -> {
                                    if (draft == null || draft.id() == 0) {
                                        status.setText("Simpan skenario sebelum menggandakan.");
                                        return;
                                    }
                                    long id = draft.id();
                                    String newName = draft.name() + " (salinan)";
                                    tasks.run(
                                            status,
                                            () -> service.duplicate(id, newName),
                                            value -> {
                                                show(value);
                                                reloadList();
                                            });
                                }));
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
                        field("Mata kuliah dalam snapshot", courses),
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
                        comparisonContent,
                        status);
        SplitPane split = new SplitPane(left, scroll(body));
        split.setDividerPositions(0.27);
        setCenter(split);
        context.setPadding(new javafx.geometry.Insets(6));
        setTop(context);
        show(null);
    }

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

    private void reloadList() {
        tasks.run(status, () -> service.scenarios(semester), this::setList);
    }

    private void show(SemesterScenario value) {
        long courseId = current == null ? 0 : current.basis().course().id();
        draft = value;
        dirty = false;
        current = null;
        filling = true;
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

    private void showCourse(ScenarioCourse value) {
        context.setText(
                value == null
                        ? "Buat atau buka skenario."
                        : "Skenario: "
                                + name.getText()
                                + " · "
                                + value.basis().course().name()
                                + " · Jumlah bobot "
                                + GradeForms.number(value.basis().totalWeight())
                                + "%");
        current = value;
        filling = true;
        locks.clear();
        rows.getChildren().clear();
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
                CheckBox locked = new CheckBox("Kunci asumsi");
                locked.setId("lock-" + item.id());
                locked.setSelected(value.lockedScores().containsKey(item.id()));
                locked.setDisable(actual || zero);
                TextField score = text("assumption-" + item.id(), "Skor normalisasi 0–100");
                score.setText(
                        Optional.ofNullable(value.lockedScores().get(item.id()))
                                .map(GradeForms::number)
                                .orElse(""));
                score.setDisable(actual || zero || !locked.isSelected());
                Label suggestion = note("");
                suggestion.setId("suggestion-" + item.id());
                locks.put(item.id(), new LockRow(locked, score, suggestion));
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
                                    score.setDisable(!b || actual || zero);
                                    changed();
                                });
                score.textProperty().addListener((o, a, b) -> changed());
            }
        filling = false;
        courseResult.setText(
                value == null
                        ? "Pilih mata kuliah."
                        : "Tekan Hitung target & IP setelah mengubah asumsi.");
    }

    private void changed() {
        if (!filling && draft != null) {
            dirty = true;
            status.setText(
                    "Belum disimpan — hitung untuk melihat hasil terbaru, lalu Simpan skenario.");
        }
    }

    private void capture() {
        if (draft == null)
            throw new IllegalArgumentException("Buat atau buka skenario terlebih dahulu.");
        List<ScenarioCourse> values = new ArrayList<>(draft.courses());
        if (current != null) {
            Map<String, BigDecimal> scores = new LinkedHashMap<>();
            for (var row : locks.entrySet())
                if (row.getValue().locked().isSelected() && !row.getValue().locked().isDisable())
                    scores.put(row.getKey(), DecimalInput.parse(row.getValue().score().getText()));
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
                            scores);
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
        review.setText(
                value.review().outdated()
                        ? "USANG / PERLU DITINJAU — hasil berikut adalah snapshot historis.\n"
                                + value.review().message()
                        : "Basis terkini. Hasil memakai snapshot yang sedang dibuka.");
        GpaResult gpa = value.gpa();
        gpaResult.setText(
                (value.review().outdated() ? "Historis · " : "")
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
                                                                    ? "Asumsi terkunci (bukan"
                                                                            + " aktual)"
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
                    TableView<List<String>> table = new TableView<>();
                    table.setId("scenario-comparison-table");
                    String[] headings = {
                        "Mata kuliah",
                        "SKS A / B",
                        a.scenario().name() + " · efektif",
                        b.scenario().name() + " · efektif"
                    };
                    for (int i = 0; i < headings.length; i++) {
                        final int column = i;
                        TableColumn<List<String>, String> col = new TableColumn<>(headings[i]);
                        col.setCellValueFactory(
                                cell -> new ReadOnlyStringWrapper(cell.getValue().get(column)));
                        col.setPrefWidth(i == 0 ? 180 : 150);
                        table.getColumns().add(col);
                    }
                    Map<Long, ScenarioEvaluation.CourseEvaluation> right = new LinkedHashMap<>();
                    b.courses().forEach(c -> right.put(c.input().basis().course().id(), c));
                    for (var left : a.courses()) {
                        var other = right.remove(left.input().basis().course().id());
                        table.getItems()
                                .add(
                                        List.of(
                                                left.input().basis().course().name(),
                                                GradeForms.number(
                                                                left.input()
                                                                        .basis()
                                                                        .course()
                                                                        .credits())
                                                        + " / "
                                                        + (other == null
                                                                ? "—"
                                                                : GradeForms.number(
                                                                        other.input()
                                                                                .basis()
                                                                                .course()
                                                                                .credits())),
                                                GradeForms.comparisonGrade(left.grade()),
                                                other == null
                                                        ? "Tidak ada"
                                                        : GradeForms.comparisonGrade(
                                                                other.grade())));
                    }
                    right.values()
                            .forEach(
                                    c ->
                                            table.getItems()
                                                    .add(
                                                            List.of(
                                                                    c.input()
                                                                            .basis()
                                                                            .course()
                                                                            .name(),
                                                                    "— / "
                                                                            + GradeForms.number(
                                                                                    c.input()
                                                                                            .basis()
                                                                                            .course()
                                                                                            .credits()),
                                                                    "Tidak ada",
                                                                    GradeForms.comparisonGrade(
                                                                            c.grade()))));
                    table.setPrefHeight(240);
                    comparisonContent
                            .getChildren()
                            .setAll(
                                    note(service.comparisonWarning(a, b)),
                                    table,
                                    note(
                                            a.scenario().name()
                                                    + ": "
                                                    + a.gpa().label()
                                                    + " "
                                                    + a.gpa().displayValue()
                                                    + "\n"
                                                    + b.scenario().name()
                                                    + ": "
                                                    + b.gpa().label()
                                                    + " "
                                                    + b.gpa().displayValue()));
                });
    }

    void guard(Runnable action) {
        GradeForms.guard(this, dirty, () -> show(null), this::save, action);
    }
}
