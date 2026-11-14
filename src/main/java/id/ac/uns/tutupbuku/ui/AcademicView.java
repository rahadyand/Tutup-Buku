package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.service.*;

import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;

final class AcademicView extends BorderPane {
    private final AuthService auth;
    private final AcademicService academic;
    private final UiTasks tasks;
    private final Consumer<UserProfile> onProfile;
    private final ListView<Semester> semesters = new ListView<>();
    private final ComboBox<Semester> editorSelection = new ComboBox<>();
    private final ListView<Course> courses = new ListView<>();
    private final TextField number = text("semester-number", "Misalnya 3"),
            year = text("semester-year", "2026");
    private final TextField courseName = text("course-name", "Nama mata kuliah"),
            code = text("course-code", "Opsional"),
            credits = text("course-credits", "3");
    private final Label active = status("academic-active-semester"),
            semesterStatus = status("semester-status"),
            courseStatus = status("course-status"),
            loadStatus = status("academic-status"),
            courseContext = status("course-semester-context");
    private final ScrollPane semesterPage;
    private final BorderPane coursePage = new BorderPane();
    private Semester selectedSemester;
    private Course selectedCourse;
    private UserProfile profile;
    private boolean filling, selecting, semesterDirty, courseDirty;

    AcademicView(
            AuthService auth,
            AcademicService academic,
            UiTasks tasks,
            Consumer<UserProfile> onProfile) {
        this.auth = auth;
        this.academic = academic;
        this.tasks = tasks;
        this.onProfile = onProfile;
        semesters.setId("semester-list");
        courses.setId("course-list");
        editorSelection.setId("semester-editor-selector");
        editorSelection.setPromptText("Pilih semester tersimpan untuk mengedit");
        editorSelection.setMaxWidth(360);
        semesters.setPlaceholder(note("Belum ada semester tersimpan."));
        courses.setPlaceholder(note("Belum ada mata kuliah pada semester pilihan."));
        semesters.setCellFactory(
                list ->
                        new ListCell<>() {
                            @Override
                            protected void updateItem(Semester value, boolean empty) {
                                super.updateItem(value, empty);
                                setText(
                                        empty || value == null
                                                ? null
                                                : value
                                                        + (profile != null
                                                                        && profile.activeSemesterId()
                                                                                .filter(
                                                                                        id ->
                                                                                                id
                                                                                                        == value
                                                                                                                .id())
                                                                                .isPresent()
                                                                ? " [aktif]"
                                                                : ""));
                            }
                        });
        // Tunda perubahan selection sampai event JavaFX selesai; jangan mutasi selection di tengah
        // Change.
        semesters
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (o, old, value) -> {
                            if (selecting || value == null) return;
                            Platform.runLater(
                                    () -> {
                                        if (selecting) return;
                                        selecting = true;
                                        semesters.getSelectionModel().select(selectedSemester);
                                        selecting = false;
                                        guard(() -> selectSemester(value));
                                    });
                        });
        editorSelection
                .valueProperty()
                .addListener(
                        (o, old, value) -> {
                            if (selecting || value == null) return;
                            selecting = true;
                            editorSelection.setValue(selectedSemester);
                            selecting = false;
                            guard(() -> selectSemester(value));
                        });
        courses.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (o, old, value) -> {
                            if (selecting) return;
                            Platform.runLater(
                                    () -> {
                                        if (selecting) return;
                                        selecting = true;
                                        courses.getSelectionModel().select(selectedCourse);
                                        selecting = false;
                                        guardCourse(
                                                () -> {
                                                    selectedCourse = value;
                                                    selecting = true;
                                                    courses.getSelectionModel().select(value);
                                                    selecting = false;
                                                    fillCourse(value);
                                                });
                                    });
                        });
        number.setMaxWidth(120);
        year.setMaxWidth(150);
        Button newForm = button("new-semester", "Semester baru");
        newForm.setOnAction(e -> newSemester());
        Button activate = button("set-active-semester", "Jadikan Aktif");
        activate.setOnAction(e -> guardCourse(() -> saveSemester(true, () -> showCourses())));
        Button save = button("save-semester", "Simpan Semester");
        save.setOnAction(e -> guardCourse(() -> saveSemester(false, () -> showCourses())));
        Button delete = button("delete-semester", "Hapus Semester");
        delete.setOnAction(e -> deleteSemester());
        Button view = button("view-courses", "Lihat mata kuliah semester ini");
        view.setOnAction(
                e ->
                        guard(
                                () -> {
                                    if (selectedSemester == null)
                                        semesterStatus.setText(
                                                "Pilih semester tersimpan terlebih dahulu.");
                                    else showCourses();
                                }));
        for (Button button : List.of(activate, save, delete)) button.setMaxWidth(260);
        semesterPage =
                centeredPage(
                        centeredForm(
                                heading("Semester"),
                                active,
                                fieldCentered("Semester tersimpan", editorSelection),
                                newForm,
                                fieldCentered("Nomor semester", number),
                                fieldCentered("Tahun", year),
                                activate,
                                save,
                                delete,
                                view,
                                semesterStatus),
                        500);
        semesterPage.setId("semester-page");
        coursePage.setId("courses-page");
        Button back = button("edit-semesters", "Kembali ke halaman Semester");
        back.setOnAction(e -> guard(() -> showSemester()));
        Button newFromCourses = button("new-semester", "Semester baru");
        newFromCourses.setOnAction(e -> newSemester());
        VBox sidebar =
                new VBox(
                        10,
                        heading("Semester tersimpan"),
                        note("Sorotan = dipilih · [aktif] = semester aktif"),
                        semesters,
                        newFromCourses,
                        back);
        sidebar.setPadding(new Insets(16));
        sidebar.setPrefWidth(300);
        VBox.setVgrow(semesters, Priority.ALWAYS);
        coursePage.setLeft(sidebar);
        courses.setPrefHeight(160);
        courses.setMinHeight(90);
        courseName.setMaxWidth(480);
        code.setMaxWidth(180);
        credits.setMaxWidth(100);
        Button newCourse = button("new-course", "Mata kuliah baru");
        newCourse.setOnAction(
                e ->
                        guardCourse(
                                () -> {
                                    selectedCourse = null;
                                    selecting = true;
                                    courses.getSelectionModel().clearSelection();
                                    selecting = false;
                                    fillCourse(null);
                                }));
        Button saveCourse = button("save-course", "Simpan mata kuliah");
        saveCourse.setOnAction(e -> saveCourse(() -> {}));
        Button deleteCourse = button("delete-course", "Hapus mata kuliah");
        deleteCourse.setOnAction(e -> deleteCourse());
        VBox courseForm =
                form(
                        heading("Mata kuliah"),
                        courseContext,
                        courses,
                        newCourse,
                        field("Nama mata kuliah", courseName),
                        field("Kode (opsional)", code),
                        field("SKS (lebih dari 0)", credits),
                        new HBox(10, saveCourse, deleteCourse),
                        courseStatus);
        courseForm.setMaxWidth(680);
        coursePage.setCenter(centeredPage(courseForm, 680));
        setBottom(loadStatus);
        setPadding(new Insets(6));
        showSemester();
        for (TextField field : List.of(number, year))
            field.textProperty()
                    .addListener(
                            (o, a, b) -> {
                                if (!filling) {
                                    semesterDirty = true;
                                    semesterStatus.setText("Belum disimpan.");
                                }
                            });
        for (TextField field : List.of(courseName, code, credits))
            field.textProperty()
                    .addListener(
                            (o, a, b) -> {
                                if (!filling) {
                                    courseDirty = true;
                                    courseStatus.setText("Belum disimpan.");
                                }
                            });
    }

    void refresh() {
        reload(selectedSemester == null ? 0 : selectedSemester.id(), () -> {});
    }

    private void reload(long preferred, Runnable done) {
        tasks.run(
                loadStatus,
                () -> new Snapshot(auth.profile(), academic.semesters()),
                snapshot -> {
                    profile = snapshot.profile();
                    onProfile.accept(profile);
                    selecting = true;
                    semesters.getItems().setAll(snapshot.semesters());
                    editorSelection.getItems().setAll(snapshot.semesters());
                    selecting = false;
                    active.setText(
                            snapshot.semesters().stream()
                                    .filter(
                                            s ->
                                                    profile.activeSemesterId()
                                                            .filter(id -> id == s.id())
                                                            .isPresent())
                                    .findFirst()
                                    .map(s -> "Aktif: " + s)
                                    .orElse("Belum ada semester aktif."));
                    Semester choice =
                            snapshot.semesters().stream()
                                    .filter(s -> s.id() == preferred)
                                    .findFirst()
                                    .orElseGet(
                                            () ->
                                                    snapshot.semesters().stream()
                                                            .filter(
                                                                    s ->
                                                                            profile.activeSemesterId()
                                                                                    .filter(
                                                                                            id ->
                                                                                                    id
                                                                                                            == s
                                                                                                                    .id())
                                                                                    .isPresent())
                                                            .findFirst()
                                                            .orElse(null));
                    selectSemester(choice);
                    loadStatus.setText("Daftar semester dimuat.");
                    done.run();
                });
    }

    private void selectSemester(Semester value) {
        selectedSemester = value;
        selecting = true;
        semesters.getSelectionModel().select(value);
        editorSelection.setValue(value);
        semesters.refresh();
        selecting = false;
        fillSemester(value);
        loadCourses();
    }

    private void showSemester() {
        setCenter(semesterPage);
    }

    private void showCourses() {
        setCenter(coursePage);
    }

    private void newSemester() {
        guard(
                () -> {
                    selectedSemester = null;
                    selecting = true;
                    semesters.getSelectionModel().clearSelection();
                    editorSelection.setValue(null);
                    selecting = false;
                    fillSemester(null);
                    showSemester();
                });
    }

    private void fillSemester(Semester value) {
        filling = true;
        number.setText(value == null ? "" : Integer.toString(value.number()));
        year.setText(value == null ? "" : value.year());
        filling = false;
        semesterDirty = false;
    }

    private void loadCourses() {
        selectedCourse = null;
        if (selectedSemester == null) {
            selecting = true;
            courses.getItems().clear();
            selecting = false;
            fillCourse(null);
            courseContext.setText("Pilih semester tersimpan.");
            return;
        }
        Semester context = selectedSemester;
        courseContext.setText("Dipilih: " + context);
        tasks.run(
                loadStatus,
                () -> academic.courses(context.id()),
                loaded -> {
                    if (selectedSemester == null || selectedSemester.id() != context.id()) return;
                    selecting = true;
                    courses.getItems().setAll(loaded);
                    courses.getSelectionModel().clearSelection();
                    selecting = false;
                    fillCourse(null);
                });
    }

    private void fillCourse(Course value) {
        filling = true;
        courseName.setText(value == null ? "" : value.name());
        code.setText(value == null ? "" : value.code());
        credits.setText(value == null ? "" : value.credits().toPlainString().replace('.', ','));
        filling = false;
        courseDirty = false;
    }

    private void saveSemester(boolean activate, Runnable done) {
        long id = selectedSemester == null ? 0 : selectedSemester.id();
        String inputNumber = number.getText(), inputYear = year.getText();
        tasks.run(
                semesterStatus,
                () -> academic.saveSemesterForm(id, inputNumber, inputYear, activate),
                saved -> {
                    selectedSemester = saved;
                    fillSemester(saved);
                    semesterStatus.setText(
                            activate ? "Semester tersimpan dan aktif." : "Semester tersimpan.");
                    // Tandai hasil commit sebelum refresh daftar; retry tidak membuat record baru.
                    reload(saved.id(), done);
                });
    }

    private void saveCourse(Runnable done) {
        if (selectedSemester == null) {
            courseStatus.setText("Pilih semester terlebih dahulu.");
            return;
        }
        long semester = selectedSemester.id(),
                id = selectedCourse == null ? 0 : selectedCourse.id();
        String name = courseName.getText(),
                courseCode = code.getText(),
                courseCredits = credits.getText();
        tasks.run(
                courseStatus,
                () -> academic.saveCourseMetadata(id, semester, name, courseCode, courseCredits),
                saved -> {
                    selectedCourse = saved;
                    fillCourse(saved);
                    courseStatus.setText("Mata kuliah tersimpan.");
                    tasks.run(
                            loadStatus,
                            () -> academic.courses(semester),
                            loaded -> {
                                selecting = true;
                                courses.getItems().setAll(loaded);
                                courses.getSelectionModel().select(saved);
                                selecting = false;
                                done.run();
                            });
                });
    }

    private void deleteSemester() {
        guardCourse(
                () -> {
                    if (selectedSemester == null) {
                        semesterStatus.setText("Pilih semester tersimpan.");
                        return;
                    }
                    if (!confirm(
                            this,
                            "Hapus semester?",
                            "Hapus semester beserta mata kuliah, nilai aktual, skenario, bab,"
                                + " materi kuliah, catatan dan progresnya? Semester lain dan"
                                + " peminatan tetap. Pilihan aktif dikosongkan bila semester ini"
                                + " aktif.")) return;
                    long id = selectedSemester.id();
                    tasks.run(
                            semesterStatus,
                            () -> {
                                academic.deleteSemester(id);
                                return null;
                            },
                            ignored -> {
                                selectedSemester = null;
                                fillSemester(null);
                                reload(0, () -> showSemester());
                            });
                });
    }

    private void deleteCourse() {
        if (selectedSemester == null || selectedCourse == null) {
            courseStatus.setText("Pilih mata kuliah tersimpan.");
            return;
        }
        if (!confirm(
                this,
                "Hapus mata kuliah?",
                "Hapus mata kuliah beserta komponen/nilai, bab, materi, catatan dan progres?"
                        + " Snapshot skenario lama tetap historis.")) return;
        long semester = selectedSemester.id(), id = selectedCourse.id();
        tasks.run(
                courseStatus,
                () -> {
                    academic.deleteCourse(semester, id);
                    return null;
                },
                ignored -> {
                    courseDirty = false;
                    loadCourses();
                    courseStatus.setText("Mata kuliah dihapus.");
                });
    }

    private void guardCourse(Runnable action) {
        GradeForms.guard(
                this, courseDirty, () -> fillCourse(selectedCourse), this::saveCourse, action);
    }

    void guard(Runnable action) {
        guardCourse(
                () ->
                        GradeForms.guard(
                                this,
                                semesterDirty,
                                () -> fillSemester(selectedSemester),
                                done -> saveSemester(false, done),
                                action));
    }

    private record Snapshot(UserProfile profile, List<Semester> semesters) {}
}
