package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.service.*;

import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.Consumer;

/** Satu daftar/editor dipakai bergantian oleh agenda global dan hub, bukan dua draf. */
final class AgendaView extends BorderPane {
    private final AcademicService academic;
    private final AgendaService service;
    private final UiTasks tasks;
    private final Consumer<Runnable> navigationGuard;
    private final ComboBox<Semester> semesters = new ComboBox<>();
    private final ComboBox<CourseChoice> courses = new ComboBox<>();
    private final ComboBox<String> typeFilter = new ComboBox<>(), stateFilter = new ComboBox<>();
    private final Label context = note(""),
            status = status("agenda-status"),
            editorState = note("");
    private final FlowPane locationControls, listControls;
    private final VBox items = new VBox(10), steps = new VBox(6);
    private final Label checklistHelp =
            note(
                    "Checklist tugas — centang langkah tidak otomatis menyelesaikan tugas. Status"
                            + " tugas diubah dari daftar.");
    private final ScrollPane listPage = scroll(items), editPage;
    private final TextField title = text("agenda-title", "Judul wajib"),
            time = text("agenda-time", "HH:mm");
    private final TextField date = text("agenda-date", "dd/MM/yyyy");
    private final ComboBox<AgendaType> type = new ComboBox<>();
    private final ComboBox<AgendaPriority> priority = new ComboBox<>();
    private final ComboBox<Course> editorCourse = new ComboBox<>();
    private final TextArea description = new TextArea();
    private final Button save = button("agenda-save", "Simpan"),
            back = button("agenda-cancel", "Kembali / Batal"),
            addStep = button("agenda-add-step", "Tambah langkah");
    private final FlowPane editorActions = new FlowPane(8, 6, save, back);
    private final List<StepRow> stepRows = new ArrayList<>();
    private List<Course> courseList = List.of();
    private AgendaItem base;
    private boolean filling, dirty, editing, scoped, saving;
    private Semester viewed;
    private Optional<Long> activeSemester = Optional.empty();

    private record CourseChoice(Course course) {
        long id() {
            return course == null ? 0 : course.id();
        }

        @Override
        public String toString() {
            return course == null ? "Semua mata kuliah" : course.toString();
        }
    }

    private record StepRow(String id, TextField text, CheckBox completed, HBox node) {}

    AgendaView(
            AcademicService academic,
            AgendaService service,
            UiTasks tasks,
            Consumer<Runnable> navigationGuard) {
        this.academic = academic;
        this.service = service;
        this.tasks = tasks;
        this.navigationGuard = navigationGuard;
        setId("agenda-view");
        semesters.setId("agenda-semester");
        courses.setId("agenda-course-filter");
        typeFilter.setId("agenda-type-filter");
        stateFilter.setId("agenda-state-filter");
        context.setId("agenda-context");
        items.setId("agenda-items");
        listPage.setId("agenda-list-page");
        typeFilter.getItems().setAll("Tugas & Ujian", "Tugas", "Ujian");
        typeFilter.getSelectionModel().selectFirst();
        stateFilter.getItems().setAll("Belum selesai", "Selesai", "Semua status");
        stateFilter.getSelectionModel().selectFirst();
        limitWidth(semesters, 190);
        limitWidth(courses, 240);
        limitWidth(typeFilter, 150);
        limitWidth(stateFilter, 150);
        Button add = button("agenda-add", "Tambah tugas / ujian"),
                refresh = button("agenda-refresh", "Muat ulang");
        add.getStyleClass().add("tb-primary");
        add.setOnAction(e -> navigationGuard.accept(() -> edit(null)));
        refresh.setOnAction(e -> navigationGuard.accept(this::refresh));
        locationControls =
                new FlowPane(
                        10, 6, field("Semester dilihat", semesters), field("Mata kuliah", courses));
        listControls = new FlowPane(8, 6, typeFilter, stateFilter, add, refresh);
        VBox header = new VBox(6, locationControls, context, listControls);
        header.setPadding(new javafx.geometry.Insets(6, 8, 6, 8));
        setTop(header);
        save.getStyleClass().add("tb-primary");
        save.setOnAction(e -> save(() -> reload(base == null ? 0 : base.id())));
        back.setOnAction(e -> navigationGuard.accept(() -> reload(0)));
        editorActions.setVisible(false);
        editorActions.setManaged(false);
        VBox footer = new VBox(4, editorActions, status);
        footer.setPadding(new javafx.geometry.Insets(6, 8, 6, 8));
        setBottom(footer);
        type.setId("agenda-type");
        type.getItems().setAll(AgendaType.values());
        priority.setId("agenda-priority");
        priority.getItems().setAll(AgendaPriority.values());
        editorCourse.setId("agenda-editor-course");
        editorCourse.setPromptText("Pilih mata kuliah");
        date.setId("agenda-date");
        description.setId("agenda-description");
        description.setWrapText(true);
        description.setPrefRowCount(3);
        limitWidth(title, 400);
        limitWidth(time, 110);
        limitWidth(date, 150);
        limitWidth(type, 110);
        limitWidth(priority, 110);
        limitWidth(editorCourse, 280);
        addStep.setOnAction(e -> addStep(UUID.randomUUID().toString(), "", false));
        VBox form =
                form(
                        heading("Tugas / Ujian"),
                        editorState,
                        field("Mata kuliah", editorCourse),
                        field("Judul", title),
                        new FlowPane(
                                10,
                                6,
                                field("Jenis", type),
                                field("Tanggal dd/MM/yyyy", date),
                                field("Jam WIB (opsional)", time),
                                field("Prioritas", priority)),
                        note(
                                "Tugas: tenggat. Ujian: tanggal/jam mulai. Tanpa jam tetap tanpa"
                                        + " jam; selesai bukan nilai atau kelulusan."),
                        field("Deskripsi teks (opsional)", description),
                        checklistHelp,
                        steps,
                        addStep);
        editPage = scroll(form);
        editPage.setId("agenda-edit-page");
        type.valueProperty()
                .addListener(
                        (o, old, value) -> {
                            if (filling) return;
                            if (value == AgendaType.EXAM && !stepRows.isEmpty()) {
                                filling = true;
                                type.setValue(old);
                                filling = false;
                                status.setText(
                                        "Checklist masih ada. Hapus langkah secara eksplisit"
                                                + " sebelum memilih Ujian.");
                                return;
                            }
                            stepControls();
                            changed();
                        });
        title.textProperty().addListener((o, a, b) -> changed());
        time.textProperty().addListener((o, a, b) -> changed());
        description.textProperty().addListener((o, a, b) -> changed());
        date.textProperty().addListener((o, a, b) -> changed());
        priority.valueProperty().addListener((o, a, b) -> changed());
        editorCourse.valueProperty().addListener((o, a, b) -> changed());
        semesters
                .valueProperty()
                .addListener(
                        (o, old, value) -> {
                            if (filling || value == null) return;
                            filling = true;
                            semesters.setValue(old);
                            filling = false;
                            navigationGuard.accept(() -> loadCourses(value, 0));
                        });
        courses.valueProperty()
                .addListener(
                        (o, old, value) -> {
                            if (filling || value == null) return;
                            filling = true;
                            courses.setValue(old);
                            filling = false;
                            navigationGuard.accept(
                                    () -> {
                                        filling = true;
                                        courses.setValue(value);
                                        filling = false;
                                        reload(0);
                                    });
                        });
        typeFilter
                .valueProperty()
                .addListener(
                        (o, a, b) -> {
                            if (!filling) reload(0);
                        });
        stateFilter
                .valueProperty()
                .addListener(
                        (o, a, b) -> {
                            if (!filling) reload(0);
                        });
        setCenter(listPage);
    }

    void activeSemester(Optional<Long> id) {
        activeSemester = id;
    }

    Semester viewedSemester() {
        return viewed;
    }

    Course filteredCourse() {
        return courses.getValue() == null ? null : courses.getValue().course();
    }

    void openGlobal() {
        dashboardItem = null;
        dashboardGroup = null;
        showGlobal();
    }

    private void showGlobal() {
        scoped = false;
        locationControls.setVisible(true);
        locationControls.setManaged(true);
        refresh();
    }

    void openGlobal(Semester semester, Course course) {
        viewed = semester;
        filling = true;
        courses.setValue(new CourseChoice(course));
        filling = false;
        openGlobal();
    }

    private Long dashboardItem;
    private AgendaSection dashboardGroup;

    void openDashboard(Semester semester, AgendaSection group, Long item) {
        viewed = semester;
        dashboardItem = item;
        dashboardGroup = group;
        filling = true;
        courses.setValue(new CourseChoice(null));
        typeFilter.setValue(
                group == AgendaSection.OVERDUE
                        ? "Tugas"
                        : group == AgendaSection.PAST_EXAMS ? "Ujian" : "Tugas & Ujian");
        stateFilter.setValue("Belum selesai");
        filling = false;
        showGlobal();
    }

    void openCourse(Semester semester, Course course) {
        dashboardItem = null;
        dashboardGroup = null;
        scoped = true;
        locationControls.setVisible(false);
        locationControls.setManaged(false);
        filling = true;
        semesters.getItems().setAll(semester);
        filling = false;
        loadCourses(semester, course.id());
    }

    private void refresh() {
        Long semester = viewed == null ? activeSemester.orElse(null) : Long.valueOf(viewed.id());
        long course = filteredCourse() == null ? 0 : filteredCourse().id();
        tasks.run(
                status,
                academic::semesters,
                values -> {
                    filling = true;
                    semesters.getItems().setAll(values);
                    filling = false;
                    Semester selected =
                            values.stream()
                                    .filter(s -> Objects.equals(semester, s.id()))
                                    .findFirst()
                                    .orElse(values.isEmpty() ? null : values.getFirst());
                    if (selected == null) {
                        viewed = null;
                        courseList = List.of();
                        filling = true;
                        semesters.setValue(null);
                        courses.getItems().clear();
                        filling = false;
                        showList();
                        items.getChildren()
                                .setAll(
                                        note(
                                                "Belum ada semester/mata kuliah. Tambahkan melalui"
                                                    + " Semester & Mata Kuliah, lalu Tambah tugas /"
                                                    + " ujian."));
                        context.setText("Belum ada semester dilihat.");
                        return;
                    }
                    loadCourses(selected, course);
                });
    }

    private void loadCourses(Semester semester, long course) {
        tasks.run(
                status,
                () -> academic.courses(semester.id()),
                values -> {
                    viewed = semester;
                    courseList = values;
                    filling = true;
                    semesters.setValue(semester);
                    courses.getItems().clear();
                    if (!scoped) courses.getItems().add(new CourseChoice(null));
                    values.forEach(c -> courses.getItems().add(new CourseChoice(c)));
                    courses.setValue(
                            courses.getItems().stream()
                                    .filter(c -> c.id() == course)
                                    .findFirst()
                                    .orElse(scoped ? null : new CourseChoice(null)));
                    filling = false;
                    reload(0);
                });
    }

    private void reload(long selected) {
        showList();
        context.setText(
                viewed == null
                        ? "Belum ada semester."
                        : "Dilihat: "
                                + viewed
                                + (activeSemester.filter(id -> id == viewed.id()).isPresent()
                                        ? " (aktif)"
                                        : " (arsip; tidak diaktifkan)")
                                + (filteredCourse() == null
                                        ? " · Semua mata kuliah"
                                        : " · " + filteredCourse().name()));
        items.getChildren().setAll(note("Memuat agenda…"));
        if (viewed == null || scoped && filteredCourse() == null) {
            items.getChildren()
                    .setAll(
                            note(
                                    "Belum ada mata kuliah. Tambahkan melalui Semester & Mata"
                                            + " Kuliah; kemudian gunakan Tambah tugas / ujian."));
            return;
        }
        long course = filteredCourse() == null ? 0 : filteredCourse().id();
        tasks.run(
                status,
                () -> service.items(viewed.id(), course),
                values -> {
                    items.getChildren().clear();
                    var groups = service.group(values.stream().filter(this::matches).toList());
                    for (AgendaSection section : AgendaSection.values()) {
                        var group = groups.getOrDefault(section, List.of());
                        if (group.isEmpty()) continue;
                        Label heading = heading(section.toString());
                        heading.setId("agenda-section-" + section.name());
                        items.getChildren().add(heading);
                        group.forEach(i -> items.getChildren().add(card(i)));
                    }
                    if (items.getChildren().isEmpty())
                        items.getChildren()
                                .add(
                                        note(
                                                "Tidak ada item untuk filter ini. Gunakan Tambah"
                                                        + " tugas / ujian atau ubah filter"
                                                        + " status/jenis."));
                    if (dashboardItem != null) {
                        long id = dashboardItem;
                        dashboardItem = null;
                        dashboardGroup = null;
                        tasks.run(
                                status,
                                () -> service.open(id),
                                item -> {
                                    if (courseList.stream()
                                            .noneMatch(c -> c.id() == item.courseId())) {
                                        status.setText("Item tidak tersedia pada semester ini.");
                                        return;
                                    }
                                    edit(item);
                                });
                    } else if (dashboardGroup != null) {
                        var target = items.lookup("#agenda-section-" + dashboardGroup.name());
                        dashboardGroup = null;
                        if (target != null)
                            javafx.application.Platform.runLater(
                                    () -> {
                                        items.applyCss();
                                        items.layout();
                                        double range =
                                                items.getHeight()
                                                        - listPage.getViewportBounds().getHeight();
                                        if (range > 0)
                                            listPage.setVvalue(
                                                    Math.min(
                                                            1,
                                                            target.getBoundsInParent().getMinY()
                                                                    / range));
                                    });
                    }
                    status.setText(
                            selected == 0
                                    ? "Agenda dimuat. Penyelesaian tidak mengubah nilai akademik."
                                    : "Item tersimpan. Jika tidak terlihat, periksa filter"
                                            + " status/jenis/mata kuliah.");
                });
    }

    private boolean matches(AgendaItem item) {
        return ("Tugas & Ujian".equals(typeFilter.getValue())
                        || item.type().toString().equals(typeFilter.getValue()))
                && ("Semua status".equals(stateFilter.getValue())
                        || item.completed() == "Selesai".equals(stateFilter.getValue()));
    }

    private VBox card(AgendaItem item) {
        String course =
                courseList.stream()
                        .filter(c -> c.id() == item.courseId())
                        .map(Course::name)
                        .findFirst()
                        .orElse("Mata kuliah tidak tersedia");
        Label name = note(item.type() + " · " + item.title());
        name.getStyleClass().add("tb-section-title");
        Label details =
                note(
                        course
                                + " · "
                                + AgendaService.DATE_FORMAT.format(item.date())
                                + item.time().map(t -> " · " + t + " WIB").orElse(" · Tanpa jam")
                                + " · Prioritas: "
                                + item.priority()
                                + " · "
                                + (item.completed() ? "Selesai" : "Belum selesai")
                                + (item.type() == AgendaType.TASK
                                        ? (item.checklist().isEmpty()
                                                ? " · Tanpa checklist"
                                                : " · Checklist "
                                                        + item.completedSteps()
                                                        + "/"
                                                        + item.checklist().size())
                                        : " · Selesai bukan kelulusan"));
        Button edit = button("agenda-edit-" + item.id(), "Ubah"),
                complete =
                        button(
                                "agenda-complete-" + item.id(),
                                item.completed() ? "Buka kembali" : "Tandai selesai"),
                delete = button("agenda-delete-" + item.id(), "Hapus");
        delete.getStyleClass().add("tb-danger");
        edit.setOnAction(
                e ->
                        navigationGuard.accept(
                                () ->
                                        tasks.run(
                                                status,
                                                () -> service.open(item.id()),
                                                this::edit)));
        complete.setOnAction(
                e ->
                        navigationGuard.accept(
                                () -> {
                                    if (!item.completed()
                                            && item.completedSteps() != item.checklist().size()
                                            && !confirmCompletion()) return;
                                    tasks.save(
                                            status,
                                            () -> service.complete(item, !item.completed(), true),
                                            saved -> reload(saved.id()),
                                            error -> {});
                                }));
        delete.setOnAction(
                e ->
                        navigationGuard.accept(
                                () -> {
                                    if (!confirm(
                                            this,
                                            "Hapus item agenda?",
                                            "Hapus “"
                                                    + item.title()
                                                    + "” beserta checklist-nya? Materi dan nilai"
                                                    + " tidak diubah.")) return;
                                    tasks.run(
                                            status,
                                            () -> {
                                                service.delete(item);
                                                return null;
                                            },
                                            unused -> reload(0));
                                }));
        VBox card = new VBox(6, name, details, new FlowPane(8, 6, edit, complete, delete));
        card.setId("agenda-item-" + item.id());
        card.getStyleClass().add("tb-form-card");
        card.setPadding(new javafx.geometry.Insets(10));
        return card;
    }

    private boolean confirmCompletion() {
        ButtonType yes = new ButtonType("Tetap selesaikan", ButtonBar.ButtonData.YES),
                cancel = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert =
                new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Masih ada langkah belum selesai. Tandai tugas selesai tanpa mengubah"
                                + " langkah-langkah tersebut?",
                        yes,
                        cancel);
        alert.initOwner(getScene().getWindow());
        alert.setTitle("Selesaikan tugas?");
        alert.setHeaderText("Checklist belum lengkap");
        styleDialog(alert);
        ((Button) alert.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        ((Button) alert.getDialogPane().lookupButton(yes)).setDefaultButton(false);
        return alert.showAndWait().filter(yes::equals).isPresent();
    }

    private void edit(AgendaItem value) {
        if (courseList.isEmpty()) {
            status.setText(
                    "Tambahkan mata kuliah melalui Semester & Mata Kuliah, lalu Muat ulang"
                            + " agenda.");
            return;
        }
        filling = true;
        base = value;
        stepRows.clear();
        steps.getChildren().clear();
        editorCourse.getItems().setAll(courseList);
        editorCourse.setValue(
                value == null
                        ? filteredCourse()
                        : courseList.stream()
                                .filter(c -> c.id() == value.courseId())
                                .findFirst()
                                .orElse(null));
        editorCourse.setDisable(scoped);
        title.setText(value == null ? "" : value.title());
        type.setValue(value == null ? AgendaType.TASK : value.type());
        priority.setValue(value == null ? AgendaPriority.NORMAL : value.priority());
        date.setText(
                AgendaService.DATE_FORMAT.format(value == null ? service.today() : value.date()));
        time.setText(value == null ? "" : value.time().map(Object::toString).orElse(""));
        description.setText(value == null ? "" : value.description());
        if (value != null) value.checklist().forEach(s -> addStep(s.id(), s.text(), s.completed()));
        filling = false;
        dirty = false;
        editing = true;
        editorState.setText(
                value == null
                        ? "Item baru — belum disimpan"
                        : (value.completed() ? "Selesai" : "Belum selesai")
                                + " · ID "
                                + value.id()
                                + ". Ubah status dari daftar; checklist tetap terpisah.");
        editorActions.setVisible(true);
        editorActions.setManaged(true);
        listControls.setVisible(false);
        listControls.setManaged(false);
        stepControls();
        setCenter(editPage);
        editPage.setVvalue(0);
        status.setText("Simpan eksplisit. Kembali / Batal tidak mengubah data tersimpan.");
        editPage.applyCss();
        editPage.layout();
        if (editorCourse.getValue() == null) editorCourse.requestFocus();
        else title.requestFocus();
    }

    private void stepControls() {
        boolean task = type.getValue() == AgendaType.TASK;
        steps.setVisible(task);
        steps.setManaged(task);
        checklistHelp.setVisible(task);
        checklistHelp.setManaged(task);
        addStep.setVisible(task);
        addStep.setManaged(task);
    }

    private void addStep(String id, String value, boolean done) {
        TextField text = text("agenda-step-text-" + id, "Teks langkah wajib");
        text.setText(value);
        text.setMaxWidth(400);
        HBox.setHgrow(text, Priority.ALWAYS);
        CheckBox completed = new CheckBox("Selesai");
        completed.setId("agenda-step-done-" + id);
        completed.setSelected(done);
        Button up = button("agenda-step-up-" + id, "↑"),
                down = button("agenda-step-down-" + id, "↓"),
                remove = button("agenda-step-remove-" + id, "Hapus");
        up.setAccessibleText("Naikkan langkah");
        down.setAccessibleText("Turunkan langkah");
        HBox node = new HBox(6, completed, text, up, down, remove);
        StepRow row = new StepRow(id, text, completed, node);
        stepRows.add(row);
        steps.getChildren().add(node);
        text.textProperty().addListener((o, a, b) -> changed());
        completed.selectedProperty().addListener((o, a, b) -> changed());
        up.setOnAction(e -> move(row, -1));
        down.setOnAction(e -> move(row, 1));
        remove.setOnAction(
                e -> {
                    if (confirm(
                            this,
                            "Hapus langkah?",
                            "Hapus langkah ini dari draf? Perubahan berlaku setelah Simpan.")) {
                        stepRows.remove(row);
                        steps.getChildren().remove(node);
                        changed();
                    }
                });
        changed();
    }

    private void move(StepRow row, int direction) {
        int from = stepRows.indexOf(row), to = from + direction;
        if (to < 0 || to >= stepRows.size()) return;
        Collections.swap(stepRows, from, to);
        steps.getChildren().setAll(stepRows.stream().map(StepRow::node).toList());
        changed();
    }

    private void changed() {
        if (!filling && editing) {
            dirty = true;
            status.setText("Perubahan belum disimpan.");
        }
    }

    private AgendaItem capture() {
        if (editorCourse.getValue() == null)
            throw new IllegalArgumentException("Pilih mata kuliah terlebih dahulu.");
        return new AgendaItem(
                base == null ? 0 : base.id(),
                editorCourse.getValue().id(),
                type.getValue(),
                title.getText(),
                AgendaService.parseDate(date.getText()),
                AgendaService.parseTime(time.getText()),
                description.getText(),
                priority.getValue(),
                base != null && base.completed(),
                base == null ? 0 : base.version(),
                stepRows.stream()
                        .map(
                                r ->
                                        new ChecklistStep(
                                                r.id(),
                                                r.text().getText(),
                                                r.completed().isSelected()))
                        .toList());
    }

    private void save(Runnable done) {
        if (saving) return;
        AgendaItem input;
        try {
            input = capture();
        } catch (IllegalArgumentException error) {
            status.setText(error.getMessage());
            return;
        }
        saving = true;
        tasks.save(
                status,
                () -> service.save(input),
                saved -> {
                    saving = false;
                    base = saved;
                    dirty = false;
                    showList();
                    done.run();
                },
                error -> {
                    saving = false;
                    status.setText("Gagal menyimpan. " + error.getMessage());
                });
    }

    private void showList() {
        editing = false;
        listControls.setVisible(true);
        listControls.setManaged(true);
        editorActions.setVisible(false);
        editorActions.setManaged(false);
        setCenter(listPage);
    }

    void guard(Runnable action) {
        GradeForms.guard(
                this,
                dirty,
                () -> {
                    dirty = false;
                    showList();
                },
                this::save,
                () -> {
                    showList();
                    action.run();
                });
    }
}
