package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.service.*;

import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.Consumer;

final class LearningView extends BorderPane {
    private final LearningService learning;
    private final AcademicService academic;
    private final UiTasks tasks;
    private final ComboBox<String> path = new ComboBox<>();
    private final ComboBox<Semester> semesters = new ComboBox<>();
    private final ComboBox<Course> courses = new ComboBox<>();
    private final ListView<LearningFolder> folders = new ListView<>();
    private final ListView<Material> materials = new ListView<>();
    private final TextField folderName = text("learning-folder-name", "Nama bab / bidang");
    private final Label status = status("learning-status"), progress = status("learning-progress");
    private final MaterialEditor editor;
    private final Button seed = button("initialize-starter", "Salin 5 materi contoh (sekali)");
    private Map<Long, List<Course>> catalog = Map.of();
    private boolean filling;
    private Long preferredSemester;
    private final BorderPane workspace = new BorderPane();
    private final Button manage = button("manage-learning", "Kelola Daftar");
    private ScrollPane management;
    private boolean managing;
    private TitledPane contextBranch;
    private long hubCourse;

    void courseScope(boolean scoped) {
        editor.minHeightProperty().unbind();
        editor.setMinHeight(Region.USE_COMPUTED_SIZE);
        // Pada hub sempit, beri ruang reader di atas catatan; panel luar menggulir bila perlu.
        if (scoped)
            editor.minHeightProperty()
                    .bind(
                            javafx.beans.binding.Bindings.max(
                                    250, ((Region) editor.getBottom()).heightProperty().add(150)));
        hubCourse = scoped && courses.getValue() != null ? courses.getValue().id() : 0;
        contextBranch.setVisible(!scoped);
        contextBranch.setManaged(!scoped);
    }

    void openCourse(Semester semester, Course course, boolean resumeRequested) {
        hubCourse = course.id();
        if (!resumeRequested
                && courses.getValue() != null
                && courses.getValue().id() == course.id()
                && kind() == LearningLocation.Kind.CHAPTER
                && editor.current() != null) {
            showEditor();
            filling = true;
            semesters.getItems().setAll(semester);
            semesters.setValue(semester);
            courses.getItems().setAll(course);
            courses.setValue(course);
            catalog = Map.of(semester.id(), List.of(course));
            filling = false;
            editor.showReadMode();
            return;
        }
        editor.clear();
        filling = true;
        folders.getItems().clear();
        materials.getItems().clear();
        filling = false;
        tasks.run(
                status,
                () ->
                        new CourseReading(
                                learning.folders(LearningLocation.Kind.CHAPTER, course.id()),
                                learning.resumeCourse(course.id())),
                loaded -> {
                    filling = true;
                    path.setValue("Kuliah");
                    semesters.setDisable(false);
                    courses.setDisable(false);
                    semesters.getItems().setAll(semester);
                    semesters.setValue(semester);
                    courses.getItems().setAll(course);
                    courses.setValue(course);
                    catalog = Map.of(semester.id(), List.of(course));
                    folders.getItems().setAll(loaded.folders());
                    folders.getSelectionModel()
                            .select(
                                    loaded.folders().stream()
                                            .filter(
                                                    f ->
                                                            loaded.resume()
                                                                    .previousLocation()
                                                                    .filter(f.location()::equals)
                                                                    .isPresent())
                                            .findFirst()
                                            .orElse(
                                                    loaded.folders().isEmpty()
                                                            ? null
                                                            : loaded.folders().getFirst()));
                    filling = false;
                    editor.clear();
                    showEditor();
                    folderName.setText(selectedFolder() == null ? "" : selectedFolder().name());
                    long material =
                            loaded.resume().reading().map(r -> r.material().id()).orElse(0L);
                    loadMaterials(
                            material,
                            () -> {
                                Material selected = materials.getSelectionModel().getSelectedItem();
                                if (selected == null && !materials.getItems().isEmpty())
                                    selected = materials.getItems().getFirst();
                                if (selected == null) {
                                    status.setText(
                                            "Belum ada materi. Buka Kelola Daftar untuk menambah"
                                                    + " bab dan materi.");
                                    return;
                                }
                                Material target = selected;
                                filling = true;
                                materials.getSelectionModel().select(target);
                                filling = false;
                                tasks.run(
                                        status,
                                        () -> learning.open(target.id()),
                                        reading -> {
                                            boolean same =
                                                    loaded.resume()
                                                            .reading()
                                                            .filter(
                                                                    r ->
                                                                            r.material().id()
                                                                                            == reading.material()
                                                                                                    .id()
                                                                                    && r.material()
                                                                                                    .revision()
                                                                                            == reading.material()
                                                                                                    .revision())
                                                            .isPresent();
                                            editor.load(
                                                    reading,
                                                    same ? loaded.resume().scrollRatio() : 0,
                                                    true);
                                            status.setText(
                                                    same
                                                            ? loaded.resume().message()
                                                            : "Materi dibuka pada mode Baca.");
                                        });
                            });
                });
    }

    private record CourseReading(List<LearningFolder> folders, ResumeResult resume) {}

    void preferSemester(Optional<Long> id) {
        preferredSemester = id.orElse(null);
    }

    LearningView(
            LearningService learning,
            AcademicService academic,
            UiTasks tasks,
            Consumer<String> browser) {
        this.learning = learning;
        this.academic = academic;
        this.tasks = tasks;
        editor =
                new MaterialEditor(
                        learning,
                        tasks,
                        browser,
                        saved -> loadMaterials(saved.id(), null),
                        this::refreshProgress);
        path.setId("learning-path");
        path.getItems().setAll("Kuliah", "Peminatan");
        path.getSelectionModel().select(0);
        semesters.setId("learning-semester");
        courses.setId("learning-course");
        folders.setId("learning-folders");
        materials.setId("learning-materials");
        semesters.setMaxWidth(Double.MAX_VALUE);
        courses.setMaxWidth(Double.MAX_VALUE);
        path.setMaxWidth(Double.MAX_VALUE);
        folders.setPlaceholder(note("Belum ada bab/bidang. Buka Kelola Daftar untuk menambah."));
        materials.setPlaceholder(note("Pilih bab/bidang; buat materi baru atau salin contoh."));
        path.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, old, selected) -> {
                            if (filling) return;
                            filling = true;
                            path.setValue(old);
                            filling = false;
                            guard(
                                    () -> {
                                        filling = true;
                                        path.setValue(selected);
                                        filling = false;
                                        courses.setDisable(
                                                kind() == LearningLocation.Kind.INTEREST);
                                        semesters.setDisable(
                                                kind() == LearningLocation.Kind.INTEREST);
                                        editor.clear();
                                        loadFolders(0, null);
                                    });
                        });
        semesters
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, old, selected) -> {
                            if (filling) return;
                            filling = true;
                            semesters.setValue(old);
                            filling = false;
                            guard(
                                    () -> {
                                        filling = true;
                                        semesters.setValue(selected);
                                        courses.getItems()
                                                .setAll(
                                                        selected == null
                                                                ? List.of()
                                                                : catalog.getOrDefault(
                                                                        selected.id(), List.of()));
                                        courses.getSelectionModel().selectFirst();
                                        filling = false;
                                        editor.clear();
                                        loadFolders(0, null);
                                    });
                        });
        courses.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, old, selected) -> {
                            if (filling) return;
                            filling = true;
                            courses.setValue(old);
                            filling = false;
                            guard(
                                    () -> {
                                        filling = true;
                                        courses.setValue(selected);
                                        filling = false;
                                        editor.clear();
                                        loadFolders(0, null);
                                    });
                        });
        folders.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, old, selected) -> {
                            if (filling) return;
                            filling = true;
                            folders.getSelectionModel().select(old);
                            filling = false;
                            guard(
                                    () -> {
                                        filling = true;
                                        folders.getSelectionModel().select(selected);
                                        filling = false;
                                        folderName.setText(selected == null ? "" : selected.name());
                                        editor.clear();
                                        loadMaterials(0, null);
                                    });
                        });
        materials
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, old, selected) -> {
                            if (filling) return;
                            filling = true;
                            materials.getSelectionModel().select(old);
                            filling = false;
                            guard(
                                    () -> {
                                        filling = true;
                                        materials.getSelectionModel().select(selected);
                                        filling = false;
                                        if (selected == null) {
                                            editor.clear();
                                            return;
                                        }
                                        tasks.run(
                                                status,
                                                () -> learning.open(selected.id()),
                                                reading -> editor.load(reading, 0, true));
                                    });
                        });
        Button addFolder = button("learning-add-folder", "Tambah");
        addFolder.setOnAction(event -> saveFolder(false));
        Button rename = button("learning-rename-folder", "Ubah nama");
        rename.setOnAction(event -> saveFolder(true));
        Button deleteFolder = button("learning-delete-folder", "Hapus");
        deleteFolder.setOnAction(
                event ->
                        guard(
                                () -> {
                                    LearningFolder selected = selectedFolder();
                                    if (selected == null) {
                                        status.setText("Pilih bab/bidang terlebih dahulu.");
                                        return;
                                    }
                                    if (!confirm(
                                            this,
                                            "Hapus bab/bidang?",
                                            "Hapus "
                                                    + selected.name()
                                                    + " beserta seluruh materi, catatan pribadi dan"
                                                    + " status selesainya? Gambar tersimpan tetap"
                                                    + " dipertahankan. Lokasi lain tidak dihapus."))
                                        return;
                                    tasks.run(
                                            status,
                                            () -> {
                                                learning.deleteFolder(selected.location());
                                                return null;
                                            },
                                            ignored -> {
                                                editor.clear();
                                                loadFolders(0, null);
                                            });
                                }));
        Button folderUp = button("learning-folder-up", "Naik"),
                folderDown = button("learning-folder-down", "Turun");
        folderUp.setOnAction(event -> moveFolder(-1));
        folderDown.setOnAction(event -> moveFolder(1));
        Button addMaterial = button("learning-new-material", "Materi baru");
        addMaterial.setOnAction(
                event ->
                        guard(
                                () -> {
                                    LearningFolder selected = selectedFolder();
                                    if (selected == null) {
                                        status.setText("Pilih bab/bidang terlebih dahulu.");
                                        return;
                                    }
                                    filling = true;
                                    materials.getSelectionModel().clearSelection();
                                    filling = false;
                                    editor.load(
                                            new MaterialReading(
                                                    new Material(
                                                            0,
                                                            selected.location(),
                                                            "Materi baru",
                                                            "",
                                                            "",
                                                            0,
                                                            0,
                                                            ""),
                                                    false,
                                                    "",
                                                    0),
                                            0,
                                            false);
                                    showEditor();
                                }));
        Button deleteMaterial = button("learning-delete-material", "Hapus materi");
        deleteMaterial.setOnAction(
                event ->
                        guard(
                                () -> {
                                    Material selected = editor.current();
                                    if (selected == null || selected.id() == 0) {
                                        status.setText(
                                                "Pilih materi yang tersimpan terlebih dahulu.");
                                        return;
                                    }
                                    if (!confirm(
                                            this,
                                            "Hapus materi?",
                                            "Hapus "
                                                    + selected.title()
                                                    + " beserta catatan dan status selesainya? Aset"
                                                    + " gambar tetap tersedia; materi lain tidak"
                                                    + " dihapus.")) return;
                                    tasks.run(
                                            status,
                                            () -> {
                                                learning.deleteMaterial(selected.id());
                                                return null;
                                            },
                                            ignored -> {
                                                editor.clear();
                                                loadMaterials(0, null);
                                            });
                                }));
        Button materialUp = button("learning-material-up", "Naik"),
                materialDown = button("learning-material-down", "Turun");
        materialUp.setOnAction(event -> moveMaterial(-1));
        materialDown.setOnAction(event -> moveMaterial(1));
        Button resume = button("resume-reading", "Lanjutkan Membaca");
        resume.setOnAction(event -> guard(this::resume));
        seed.setOnAction(
                event ->
                        guard(
                                () -> {
                                    Course course = courses.getValue();
                                    if (course == null) {
                                        status.setText(
                                                "Buat/pilih mata kuliah di jalur Kuliah terlebih"
                                                        + " dahulu.");
                                        return;
                                    }
                                    ButtonType copy =
                                            new ButtonType(
                                                    "Salin contoh", ButtonBar.ButtonData.OK_DONE);
                                    ButtonType cancel =
                                            new ButtonType(
                                                    "Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
                                    Alert confirm =
                                            new Alert(
                                                    Alert.AlertType.CONFIRMATION,
                                                    "Salin 3 materi pengantar PBO ke "
                                                            + course.name()
                                                            + " dan 2 materi peminatan? Koleksi"
                                                            + " contoh tidak mewakili mata kuliah"
                                                            + " lengkap; disalin sekali per akun.",
                                                    copy,
                                                    cancel);
                                    confirm.initOwner(getScene().getWindow());
                                    confirm.setHeaderText("Salin koleksi contoh asli Tutup Buku");
                                    styleDialog(confirm);
                                    if (confirm.showAndWait().filter(copy::equals).isEmpty())
                                        return;
                                    tasks.run(
                                            status,
                                            () -> learning.initializeStarter(course.id()),
                                            created -> {
                                                seed.setDisable(true);
                                                editor.clear();
                                                loadFolders(
                                                        0,
                                                        () ->
                                                                status.setText(
                                                                        created
                                                                                ? "Koleksi contoh"
                                                                                      + " disalin."
                                                                                      + " Pilih"
                                                                                      + " bab/bidang"
                                                                                      + " untuk"
                                                                                      + " membaca."
                                                                                : "Koleksi contoh"
                                                                                      + " akun ini"
                                                                                      + " sudah"
                                                                                      + " pernah"
                                                                                      + " disalin;"
                                                                                      + " tidak"
                                                                                      + " digandakan."));
                                            });
                                }));
        Button refresh = button("learning-refresh", "Muat ulang daftar");
        refresh.setOnAction(event -> guard(this::refresh));
        folders.setPrefHeight(95);
        folders.setMinHeight(75);
        materials.setPrefHeight(160);
        materials.setMinHeight(100);
        for (ListView<?> list : List.of(folders, materials)) {
            list.setMinWidth(0);
        }
        materials.setCellFactory(
                list ->
                        new ListCell<>() {
                            private final Label title = note("");

                            {
                                title.maxWidthProperty().bind(widthProperty().subtract(24));
                                title.textFillProperty().bind(textFillProperty());
                            }

                            @Override
                            protected void updateItem(Material value, boolean empty) {
                                super.updateItem(value, empty);
                                title.setText(empty || value == null ? "" : value.toString());
                                setGraphic(empty ? null : title);
                                setText(null);
                            }
                        });
        contextBranch =
                new TitledPane(
                        "Lokasi belajar",
                        new VBox(
                                6,
                                field("Jalur", path),
                                field("Semester", semesters),
                                field("Mata kuliah", courses)));
        contextBranch.setId("learning-context-branch");
        contextBranch.setAnimated(false);
        contextBranch.setExpanded(false);
        TitledPane folderBranch = new TitledPane("Bab / bidang", folders);
        folderBranch.setId("learning-folder-branch");
        folderBranch.setAnimated(false);
        Label context = note("");
        context.textProperty()
                .bind(
                        javafx.beans.binding.Bindings.createStringBinding(
                                () ->
                                        kind() == LearningLocation.Kind.INTEREST
                                                ? "Peminatan"
                                                : courses.getValue() == null
                                                        ? "Pilih lokasi belajar"
                                                        : courses.getValue().name(),
                                path.valueProperty(),
                                courses.valueProperty()));
        VBox sidebar =
                new VBox(
                        8,
                        heading("Pilih materi"),
                        context,
                        contextBranch,
                        folderBranch,
                        heading("Materi"),
                        materials,
                        progress,
                        resume);
        sidebar.setPadding(new javafx.geometry.Insets(10));
        sidebar.setMinHeight(Region.USE_PREF_SIZE);
        sidebar.getStyleClass().add("tb-surface");
        VBox.setVgrow(materials, Priority.ALWAYS);
        ScrollPane catalogPane = scroll(sidebar);
        catalogPane.setId("learning-catalog");
        catalogPane.setFitToHeight(true);
        catalogPane.prefWidthProperty().bind(widthProperty().multiply(0.27));
        catalogPane.setMinWidth(220);
        catalogPane.setMaxWidth(300);
        workspace.setLeft(catalogPane);
        workspace.setCenter(editor);
        Button hide = button("toggle-learning-catalog", "Tutup daftar materi");
        hide.setOnAction(
                event ->
                        editor.preservePosition(
                                () -> {
                                    boolean visible = workspace.getLeft() != null;
                                    workspace.setLeft(visible ? null : catalogPane);
                                    hide.setText(
                                            visible ? "Buka daftar materi" : "Tutup daftar materi");
                                }));
        Label selection = note("");
        selection
                .textProperty()
                .bind(
                        javafx.beans.binding.Bindings.createStringBinding(
                                () ->
                                        "Bab/bidang: "
                                                + (selectedFolder() == null
                                                        ? "belum dipilih"
                                                        : selectedFolder().name())
                                                + "\nMateri: "
                                                + (materials.getSelectionModel().getSelectedItem()
                                                                == null
                                                        ? "belum dipilih"
                                                        : materials
                                                                .getSelectionModel()
                                                                .getSelectedItem()
                                                                .title()),
                                folders.getSelectionModel().selectedItemProperty(),
                                materials.getSelectionModel().selectedItemProperty()));
        Button editSelected = button("manage-edit-material", "Ubah Materi terpilih");
        editSelected.setOnAction(
                event ->
                        guard(
                                () -> {
                                    if (editor.current() == null) {
                                        status.setText("Pilih materi terlebih dahulu.");
                                        return;
                                    }
                                    showEditor();
                                    editor.showEditMode();
                                }));
        addMaterial.getStyleClass().add("tb-primary");
        deleteMaterial.getStyleClass().add("tb-danger");
        deleteFolder.getStyleClass().add("tb-danger");
        VBox manager =
                form(
                        heading("Kelola Daftar"),
                        selection,
                        field("Nama bab / bidang", folderName),
                        new FlowPane(8, 6, addFolder, rename, deleteFolder),
                        new FlowPane(8, 6, folderUp, folderDown),
                        heading("Materi terpilih"),
                        new FlowPane(8, 6, addMaterial, editSelected, deleteMaterial),
                        new FlowPane(8, 6, materialUp, materialDown),
                        seed,
                        refresh);
        manager.getStyleClass().add("tb-surface");
        manager.setMinHeight(Region.USE_PREF_SIZE);
        management = scroll(manager);
        management.setId("learning-management");
        manage.setOnAction(
                event ->
                        guard(
                                () -> {
                                    if (managing) {
                                        showEditor();
                                        editor.showReadMode();
                                    } else {
                                        managing = true;
                                        workspace.setCenter(management);
                                        manage.setText("Kembali Membaca");
                                        if (workspace.getLeft() == null) {
                                            workspace.setLeft(catalogPane);
                                            hide.setText("Tutup daftar materi");
                                        }
                                    }
                                }));
        setTop(new FlowPane(10, 6, hide, manage));
        setCenter(workspace);
        status.setPadding(new javafx.geometry.Insets(6));
        setBottom(status);
    }

    private LearningLocation.Kind kind() {
        return "Peminatan".equals(path.getValue())
                ? LearningLocation.Kind.INTEREST
                : LearningLocation.Kind.CHAPTER;
    }

    private LearningFolder selectedFolder() {
        return folders.getSelectionModel().getSelectedItem();
    }

    private void showEditor() {
        managing = false;
        manage.setText("Kelola Daftar");
        workspace.setCenter(editor);
    }

    void guard(Runnable action) {
        editor.guard(action);
    }

    void suspend() {
        editor.suspend();
    }

    boolean dirty() {
        return editor.dirty();
    }

    void refresh() {
        Material previousMaterial = editor.current();
        double ratio = editor.readingPosition();
        long previousFolder = selectedFolder() == null ? 0 : selectedFolder().id();
        Long previousSemester =
                semesters.getValue() == null
                        ? preferredSemester
                        : Long.valueOf(semesters.getValue().id());
        Long previousCourse = courses.getValue() == null ? null : courses.getValue().id();
        tasks.run(
                status,
                () -> {
                    List<Semester> all = academic.semesters();
                    Map<Long, List<Course>> allCourses = new LinkedHashMap<>();
                    for (Semester semester : all)
                        allCourses.put(semester.id(), academic.courses(semester.id()));
                    return new Catalog(all, allCourses, learning.seedInitialized());
                },
                loaded -> {
                    catalog = loaded.courses();
                    filling = true;
                    semesters.getItems().setAll(loaded.semesters());
                    semesters
                            .getSelectionModel()
                            .select(
                                    loaded.semesters().stream()
                                            .filter(s -> Objects.equals(previousSemester, s.id()))
                                            .findFirst()
                                            .orElse(
                                                    loaded.semesters().isEmpty()
                                                            ? null
                                                            : loaded.semesters().getFirst()));
                    Semester semester = semesters.getValue();
                    courses.getItems()
                            .setAll(semester == null ? List.of() : catalog.get(semester.id()));
                    courses.getSelectionModel()
                            .select(
                                    courses.getItems().stream()
                                            .filter(c -> Objects.equals(previousCourse, c.id()))
                                            .findFirst()
                                            .orElse(
                                                    courses.getItems().isEmpty()
                                                            ? null
                                                            : courses.getItems().getFirst()));
                    filling = false;
                    seed.setDisable(loaded.seeded());
                    editor.clear();
                    loadFolders(
                            previousFolder,
                            () -> {
                                if (previousMaterial == null
                                        || previousMaterial.id() == 0
                                        || selectedFolder() == null
                                        || !previousMaterial
                                                .location()
                                                .equals(selectedFolder().location())) return;
                                loadMaterials(
                                        previousMaterial.id(),
                                        () -> {
                                            Material found =
                                                    materials.getSelectionModel().getSelectedItem();
                                            if (found == null) return;
                                            tasks.run(
                                                    status,
                                                    () -> learning.open(found.id()),
                                                    reading ->
                                                            editor.load(
                                                                    reading,
                                                                    reading.material().revision()
                                                                                    == previousMaterial
                                                                                            .revision()
                                                                            ? ratio
                                                                            : 0,
                                                                    true));
                                        });
                            });
                });
    }

    private void loadFolders(long selectedId, Runnable after) {
        LearningLocation.Kind selectedKind = kind();
        Course course = courses.getValue();
        if (selectedKind == LearningLocation.Kind.CHAPTER && course == null) {
            filling = true;
            folders.getItems().clear();
            materials.getItems().clear();
            filling = false;
            status.setText(
                    "Belum ada mata kuliah. Buat semester/mata kuliah di tab akademik, lalu Muat"
                            + " ulang daftar. Peminatan dapat dipakai tanpa mata kuliah.");
            progress.setText("Belum ada materi");
            if (after != null) after.run();
            return;
        }
        tasks.run(
                status,
                () -> learning.folders(selectedKind, course == null ? 0 : course.id()),
                loaded -> {
                    filling = true;
                    folders.getItems().setAll(loaded);
                    folders.getSelectionModel()
                            .select(
                                    loaded.stream()
                                            .filter(folder -> folder.id() == selectedId)
                                            .findFirst()
                                            .orElse(null));
                    materials.getItems().clear();
                    filling = false;
                    folderName.setText(selectedFolder() == null ? "" : selectedFolder().name());
                    status.setText(
                            loaded.isEmpty()
                                    ? "Belum ada bab/bidang. Buka Kelola Daftar untuk tambah atau"
                                            + " salin koleksi contoh."
                                    : "Pilih bab/bidang dan materi.");
                    if (selectedFolder() != null) loadMaterials(0, after);
                    else {
                        progress.setText("Belum ada materi");
                        if (after != null) after.run();
                    }
                });
    }

    private void loadMaterials(long selectionId, Runnable after) {
        LearningFolder selected = selectedFolder();
        if (selected == null) {
            filling = true;
            materials.getItems().clear();
            filling = false;
            progress.setText("Belum ada materi");
            if (after != null) after.run();
            return;
        }
        tasks.run(
                status,
                () ->
                        new MaterialList(
                                learning.materials(selected.location()),
                                learning.progress(selected.location()),
                                selected.kind() == LearningLocation.Kind.CHAPTER
                                        ? learning.courseProgress(selected.courseId())
                                        : null),
                loaded -> {
                    filling = true;
                    materials.getItems().setAll(loaded.materials());
                    materials
                            .getSelectionModel()
                            .select(
                                    loaded.materials().stream()
                                            .filter(material -> material.id() == selectionId)
                                            .findFirst()
                                            .orElse(null));
                    filling = false;
                    progress.setText(
                            "Bab/bidang: "
                                    + loaded.progress().display()
                                    + (loaded.courseProgress() == null
                                            ? ""
                                            : "\nMata kuliah: "
                                                    + loaded.courseProgress().display()));
                    status.setText("Daftar materi dimuat.");
                    if (after != null) after.run();
                });
    }

    private void refreshProgress() {
        Material current = editor.current();
        loadMaterials(current == null ? 0 : current.id(), null);
    }

    private void saveFolder(boolean rename) {
        String name = folderName.getText();
        LearningFolder selected = selectedFolder();
        Course course = courses.getValue();
        LearningLocation.Kind selectedKind = kind();
        if (rename && selected == null) {
            status.setText("Pilih bab/bidang yang akan diubah namanya.");
            return;
        }
        if (selectedKind == LearningLocation.Kind.CHAPTER && course == null) {
            status.setText("Pilih mata kuliah terlebih dahulu.");
            return;
        }
        guard(
                () ->
                        tasks.run(
                                status,
                                () ->
                                        learning.saveFolder(
                                                rename ? selected.id() : 0,
                                                selectedKind,
                                                course == null ? 0 : course.id(),
                                                name),
                                saved -> {
                                    editor.clear();
                                    loadFolders(saved.id(), null);
                                }));
    }

    private void moveFolder(int direction) {
        guard(
                () -> {
                    LearningFolder selected = selectedFolder();
                    if (selected != null)
                        tasks.run(
                                status,
                                () -> {
                                    learning.moveFolder(selected.location(), direction);
                                    return null;
                                },
                                ignored -> loadFolders(selected.id(), null));
                });
    }

    private void moveMaterial(int direction) {
        guard(
                () -> {
                    Material selected = editor.current();
                    if (selected != null && selected.id() != 0)
                        tasks.run(
                                status,
                                () -> {
                                    learning.moveMaterial(selected.id(), direction);
                                    return null;
                                },
                                ignored -> loadMaterials(selected.id(), null));
                });
    }

    private void resume() {
        showEditor();
        tasks.run(
                status,
                () -> hubCourse == 0 ? learning.resume() : learning.resumeCourse(hubCourse),
                result -> {
                    if (result.previousLocation().isEmpty()) {
                        status.setText(result.message());
                        return;
                    }
                    tasks.run(
                            status,
                            () -> learning.folder(result.previousLocation().orElseThrow()),
                            found -> {
                                filling = true;
                                path.setValue(
                                        result.previousLocation().orElseThrow().kind()
                                                        == LearningLocation.Kind.CHAPTER
                                                ? "Kuliah"
                                                : "Peminatan");
                                semesters.setDisable(kind() == LearningLocation.Kind.INTEREST);
                                courses.setDisable(kind() == LearningLocation.Kind.INTEREST);
                                if (found.isPresent()
                                        && found.orElseThrow().kind()
                                                == LearningLocation.Kind.CHAPTER) {
                                    long courseId = found.orElseThrow().courseId();
                                    for (Semester semester : semesters.getItems())
                                        if (catalog.getOrDefault(semester.id(), List.of()).stream()
                                                .anyMatch(course -> course.id() == courseId)) {
                                            semesters.setValue(semester);
                                            courses.getItems().setAll(catalog.get(semester.id()));
                                            courses.getSelectionModel()
                                                    .select(
                                                            courses.getItems().stream()
                                                                    .filter(
                                                                            course ->
                                                                                    course.id()
                                                                                            == courseId)
                                                                    .findFirst()
                                                                    .orElseThrow());
                                            break;
                                        }
                                }
                                filling = false;
                                editor.clear();
                                loadFolders(
                                        found.map(LearningFolder::id).orElse(0L),
                                        () -> {
                                            result.reading()
                                                    .ifPresent(
                                                            reading -> {
                                                                filling = true;
                                                                materials
                                                                        .getSelectionModel()
                                                                        .select(reading.material());
                                                                filling = false;
                                                                editor.load(
                                                                        reading,
                                                                        result.scrollRatio(),
                                                                        true);
                                                            });
                                            status.setText(result.message());
                                        });
                            });
                });
    }

    private record Catalog(
            List<Semester> semesters, Map<Long, List<Course>> courses, boolean seeded) {}

    private record MaterialList(
            List<Material> materials, LearningProgress progress, LearningProgress courseProgress) {}
}
