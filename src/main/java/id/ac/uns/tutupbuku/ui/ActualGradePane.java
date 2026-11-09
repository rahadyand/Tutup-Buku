package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.service.GradePlanningService;

import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

/** Form aktual; asumsi solver tidak pernah diteruskan ke saveComponent. */
final class ActualGradePane extends BorderPane {
    private final GradePlanningService service;
    private final UiTasks tasks;
    private final ListView<ActualCourse> courses = new ListView<>();
    private final ListView<AssessmentComponent> components = new ListView<>();
    private final TextField name = text("component-name", "Nama komponen"),
            weight = text("component-weight", "Bobot %"),
            earned = text("component-earned", "Kosong jika belum diketahui"),
            maximum = text("component-maximum", "Misalnya 10 atau 40");
    private final ComboBox<EvaluationMode> mode = new ComboBox<>();
    private final ComboBox<LetterGrade> letter = GradeForms.letters("actual-letter");
    private final CheckBox attendance = new CheckBox("Syarat kehadiran terpenuhi (awal: asumsi)"),
            confirmed = new CheckBox("Kehadiran sudah saya verifikasi");
    private final Label status = status("actual-grade-status"),
            result = status("actual-result"),
            total = status("component-total");
    private ActualCourse current;
    private AssessmentComponent selected;
    private boolean filling, componentDirty, settingsDirty;
    private long semester;

    ActualGradePane(GradePlanningService service, UiTasks tasks) {
        this.service = service;
        this.tasks = tasks;
        courses.setId("grade-courses");
        components.setId("assessment-components");
        mode.setId("actual-mode");
        mode.getItems().addAll(EvaluationMode.values());
        attendance.setId("actual-attendance");
        confirmed.setId("actual-attendance-confirmed");
        courses.setPrefWidth(220);
        courses.setPlaceholder(note("Buat mata kuliah pada tab Semester & mata kuliah."));
        components.setPrefHeight(180);
        courses.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (o, old, value) -> {
                            if (filling) return;
                            filling = true;
                            courses.getSelectionModel().select(old);
                            filling = false;
                            guard(
                                    () -> {
                                        filling = true;
                                        courses.getSelectionModel().select(value);
                                        filling = false;
                                        showCourse(value);
                                    });
                        });
        components
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (o, old, value) -> {
                            if (filling) return;
                            filling = true;
                            components.getSelectionModel().select(old);
                            filling = false;
                            guard(
                                    () -> {
                                        filling = true;
                                        components.getSelectionModel().select(value);
                                        filling = false;
                                        showComponent(value);
                                    });
                        });
        for (TextField field : List.of(name, weight, earned, maximum))
            field.textProperty()
                    .addListener(
                            (o, a, b) -> {
                                if (!filling) componentDirty = true;
                            });
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
                            markSettings();
                        });
        letter.valueProperty().addListener((o, a, b) -> markSettings());
        attendance.selectedProperty().addListener((o, a, b) -> markSettings());
        confirmed.selectedProperty().addListener((o, a, b) -> markSettings());
        Button newItem = button("new-component", "Komponen baru"),
                saveItem = button("save-component", "Simpan komponen / nilai aktual"),
                delete = button("delete-component", "Hapus komponen"),
                up = button("component-up", "Naik"),
                down = button("component-down", "Turun"),
                saveSettings = button("save-actual-settings", "Simpan mode, huruf & kehadiran"),
                clearLetter = button("clear-actual-letter", "Kosongkan huruf");
        newItem.setOnAction(
                e ->
                        guard(
                                () -> {
                                    filling = true;
                                    components.getSelectionModel().clearSelection();
                                    filling = false;
                                    showComponent(null);
                                }));
        saveItem.setOnAction(e -> saveComponent(() -> {}));
        saveSettings.setOnAction(e -> saveSettings(() -> {}));
        clearLetter.setOnAction(e -> letter.setValue(null));
        delete.setOnAction(
                e ->
                        guard(
                                () -> {
                                    if (current == null || selected == null) {
                                        status.setText("Pilih komponen tersimpan.");
                                        return;
                                    }
                                    if (!confirm(
                                            this,
                                            "Hapus komponen?",
                                            "Nilai aktual komponen ikut dihapus. Snapshot skenario"
                                                    + " lama dipertahankan dan perlu ditinjau."))
                                        return;
                                    long course = current.course().id();
                                    String id = selected.id();
                                    tasks.run(
                                            status,
                                            () -> {
                                                service.deleteComponent(course, id);
                                                return null;
                                            },
                                            v -> reload(course, () -> {}));
                                }));
        up.setOnAction(e -> move(-1));
        down.setOnAction(e -> move(1));
        VBox body =
                form(
                        heading("Nilai aktual"),
                        note(
                                "Masukkan hasil yang benar-benar diperoleh. Asumsi dan target"
                                        + " dikelola pada Skenario. Mode yang tidak aktif tetap"
                                        + " disimpan."),
                        field("Mode penilaian aktif", mode),
                        field("Huruf aktual (alternatif detail)", letter),
                        clearLetter,
                        attendance,
                        confirmed,
                        saveSettings,
                        result,
                        heading("Komponen bobot rata / flat"),
                        note(
                                "Nama bebas; UTS/UAS tidak wajib. Draf boleh disimpan, tetapi bobot"
                                        + " harus tepat 100% untuk hasil akhir. Bobot 0% bersifat"
                                        + " informasi."),
                        components,
                        new FlowPane(6, 6, newItem, delete, up, down),
                        field("Nama komponen", name),
                        field("Bobot terhadap nilai akhir (%)", weight),
                        field("Perolehan aktual (kosong ≠ 0)", earned),
                        field("Maksimum skor (>0)", maximum),
                        saveItem,
                        status);
        SplitPane split = new SplitPane(courses, scroll(body));
        split.setDividerPositions(0.25);
        setCenter(split);
        total.setPadding(new javafx.geometry.Insets(6));
        setTop(total);
    }

    void load(long semester, List<ActualCourse> values) {
        this.semester = semester;
        long id = current == null ? 0 : current.course().id();
        filling = true;
        courses.getItems().setAll(values);
        ActualCourse next =
                values.stream()
                        .filter(c -> c.course().id() == id)
                        .findFirst()
                        .orElse(values.isEmpty() ? null : values.getFirst());
        courses.getSelectionModel().select(next);
        filling = false;
        showCourse(next);
    }

    private void showCourse(ActualCourse value) {
        current = value;
        filling = true;
        mode.setValue(value == null ? EvaluationMode.DETAIL : value.mode());
        letter.setValue(value == null ? null : value.letter().orElse(null));
        attendance.setSelected(value == null || value.course().attendance().met());
        confirmed.setSelected(value != null && value.course().attendance().confirmed());
        components.getItems().setAll(value == null ? List.of() : value.components());
        components.getSelectionModel().clearSelection();
        filling = false;
        settingsDirty = false;
        showComponent(null);
        if (value == null) {
            result.setText("Pilih mata kuliah.");
            total.setText("");
            return;
        }
        java.math.BigDecimal sum =
                value.components().stream()
                        .map(AssessmentComponent::weight)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        total.setText(
                "Jumlah bobot: "
                        + GradeForms.number(sum)
                        + "%"
                        + (sum.compareTo(new java.math.BigDecimal("100")) == 0
                                ? " · Sah"
                                : " · Draf belum sah (wajib tepat 100%)"));
        result.setText(GradeForms.result(service.actualResult(value)));
    }

    private void showComponent(AssessmentComponent value) {
        selected = value;
        filling = true;
        name.setText(value == null ? "" : value.name());
        weight.setText(value == null ? "" : GradeForms.number(value.weight()));
        earned.setText(value == null ? "" : value.earned().map(GradeForms::number).orElse(""));
        maximum.setText(value == null ? "" : value.maximum().map(GradeForms::number).orElse(""));
        filling = false;
        componentDirty = false;
    }

    private void markSettings() {
        if (!filling) settingsDirty = true;
    }

    private void reload(long courseId, Runnable done) {
        tasks.run(
                status,
                () -> service.courses(semester),
                values -> {
                    load(semester, values);
                    filling = true;
                    ActualCourse match =
                            values.stream()
                                    .filter(c -> c.course().id() == courseId)
                                    .findFirst()
                                    .orElse(null);
                    courses.getSelectionModel().select(match);
                    filling = false;
                    showCourse(match);
                    status.setText(
                            "Data aktual tersimpan. Skenario dengan basis lama perlu ditinjau.");
                    done.run();
                });
    }

    private void saveComponent(Runnable done) {
        if (current == null) {
            status.setText("Pilih mata kuliah.");
            return;
        }
        long courseId = current.course().id();
        String id = selected == null ? null : selected.id(),
                n = name.getText(),
                w = weight.getText(),
                e = earned.getText(),
                m = maximum.getText();
        int position =
                selected == null
                        ? current.components().stream()
                                        .mapToInt(AssessmentComponent::position)
                                        .max()
                                        .orElse(-1)
                                + 1
                        : selected.position();
        // Menyimpan komponen tidak memuat ulang form mode yang mungkin masih belum disimpan.
        tasks.save(
                status,
                () -> {
                    service.saveComponent(courseId, id, n, w, e, m, position);
                    return service.courses(semester);
                },
                values -> {
                    ActualCourse saved =
                            values.stream()
                                    .filter(c -> c.course().id() == courseId)
                                    .findFirst()
                                    .orElseThrow();
                    boolean pending = settingsDirty;
                    EvaluationMode chosen = mode.getValue();
                    LetterGrade chosenLetter = letter.getValue();
                    boolean met = attendance.isSelected(), verified = confirmed.isSelected();
                    load(semester, values);
                    if (pending) {
                        filling = true;
                        mode.setValue(chosen);
                        letter.setValue(chosenLetter);
                        attendance.setSelected(met);
                        confirmed.setSelected(verified);
                        filling = false;
                        settingsDirty = true;
                    }
                    componentDirty = false;
                    status.setText("Komponen/nilai aktual tersimpan.");
                    done.run();
                },
                error -> {});
    }

    private void saveSettings(Runnable done) {
        if (current == null) {
            status.setText("Pilih mata kuliah.");
            return;
        }
        long id = current.course().id();
        EvaluationMode chosen = mode.getValue();
        Optional<LetterGrade> actual = Optional.ofNullable(letter.getValue());
        Attendance value = new Attendance(attendance.isSelected(), confirmed.isSelected());
        // Form komponen yang belum disimpan tidak boleh hilang saat mode disimpan.
        tasks.save(
                status,
                () -> {
                    service.saveActual(id, chosen, actual, value);
                    return service.courses(semester);
                },
                values -> {
                    boolean pending = componentDirty;
                    String n = name.getText(),
                            w = weight.getText(),
                            e = earned.getText(),
                            m = maximum.getText();
                    AssessmentComponent old = selected;
                    load(semester, values);
                    if (pending) {
                        filling = true;
                        selected = old;
                        name.setText(n);
                        weight.setText(w);
                        earned.setText(e);
                        maximum.setText(m);
                        filling = false;
                        componentDirty = true;
                    }
                    settingsDirty = false;
                    status.setText("Mode/huruf/kehadiran aktual tersimpan.");
                    done.run();
                },
                error -> {});
    }

    private void move(int direction) {
        guard(
                () -> {
                    if (current == null || selected == null) {
                        status.setText("Pilih komponen.");
                        return;
                    }
                    long id = current.course().id();
                    String item = selected.id();
                    tasks.run(
                            status,
                            () -> {
                                service.moveComponent(id, item, direction);
                                return null;
                            },
                            v -> reload(id, () -> {}));
                });
    }

    void guard(Runnable action) {
        GradeForms.guard(
                this,
                componentDirty || settingsDirty,
                () -> {
                    componentDirty = false;
                    settingsDirty = false;
                    showCourse(current);
                },
                done -> {
                    if (componentDirty)
                        saveComponent(
                                () -> {
                                    if (settingsDirty) saveSettings(done);
                                    else done.run();
                                });
                    else saveSettings(done);
                },
                action);
    }
}
