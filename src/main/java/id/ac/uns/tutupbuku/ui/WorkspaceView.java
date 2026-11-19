package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import id.ac.uns.tutupbuku.domain.UserProfile;
import id.ac.uns.tutupbuku.service.*;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class WorkspaceView extends BorderPane {
    private final Label identity = new Label();
    private final Label active = status("active-semester-label");
    private final LearningView learningView;
    private final GradeView gradeView;
    private final AcademicView academicView;
    private final CourseHubView courseHub;
    private AcademicService academicService;
    private UiTasks uiTasks;
    private java.util.function.Consumer<Runnable> profileGuard = Runnable::run;

    WorkspaceView(
            AuthService auth,
            AcademicService academic,
            LearningService learning,
            GradePlanningService planning,
            UiTasks tasks,
            UserProfile profile,
            java.util.function.Consumer<String> browser,
            Runnable onLogout) {
        getStyleClass().add("tb-workspace");
        academicService = academic;
        uiTasks = tasks;
        learningView = new LearningView(learning, academic, tasks, browser);
        gradeView = new GradeView(academic, planning, tasks);
        courseHub =
                new CourseHubView(
                        academic,
                        learning,
                        planning,
                        tasks,
                        this::requestLeave,
                        learningView,
                        gradeView);
        Button logout = button("logout", "Keluar");
        logout.setOnAction(
                event ->
                        requestLeave(
                                () -> {
                                    learningView.suspend();
                                    auth.logout();
                                    onLogout.run();
                                }));
        identity.setWrapText(true);
        Label tagline = note("Materi tertata, target terencana.");
        tagline.visibleProperty().bind(heightProperty().greaterThanOrEqualTo(600));
        tagline.managedProperty().bind(tagline.visibleProperty());
        var compact = javafx.css.PseudoClass.getPseudoClass("compact");
        heightProperty()
                .addListener(
                        (o, old, height) ->
                                pseudoClassStateChanged(compact, height.doubleValue() < 600));
        VBox titles = new VBox(3, heading("Tutup Buku"), identity, active, tagline);
        HBox.setHgrow(titles, Priority.ALWAYS);
        Button openLearning = button("open-learning", "Modul Belajar"),
                openGrades = button("open-simulation", "Simulasi Nilai");
        HBox shortcuts = new HBox(8, openLearning, openGrades);
        shortcuts.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        VBox navigation = new VBox(8, logout, shortcuts);
        navigation.setMinWidth(Region.USE_PREF_SIZE);
        openLearning.setMinWidth(Region.USE_PREF_SIZE);
        openGrades.setMinWidth(Region.USE_PREF_SIZE);
        navigation.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
        navigation.setId("workspace-shortcuts");
        HBox header = new HBox(18, titles, navigation);
        header.getStyleClass().add("tb-workspace-header");
        titles.setMinWidth(0);
        identity.setMinHeight(Region.USE_PREF_SIZE);
        active.setMaxWidth(Double.MAX_VALUE);
        active.setMinHeight(Region.USE_PREF_SIZE);
        header.setPadding(new Insets(8));
        setTop(header);

        TextField nim = text("profile-nim", "");
        nim.setText(profile.nim());
        nim.setEditable(false);
        nim.setAccessibleHelp("NIM akun hanya dapat dibaca dan tidak dapat diubah.");
        TextField name = text("profile-name", "Nama");
        name.setText(profile.name());
        limitWidth(nim, 200);
        limitWidth(name, 300);
        ComboBox<String> program = new ComboBox<>();
        program.setId("profile-program");
        program.getItems().setAll("Informatika", "Sains Data");
        limitWidth(program, 240);
        program.setPromptText("Pilih program studi");
        program.setValue(canonicalProgram(profile.studyProgram()));
        Label programHelp =
                note(
                        "Pilih prodi lalu Simpan. Nilai lama yang kosong/tidak dikenali tetap"
                                + " disimpan sampai Anda memilih prodi.");
        programHelp.setMaxWidth(360);
        program.accessibleHelpProperty().bind(programHelp.textProperty());
        Label profileStatus = status("profile-status");
        Button save = button("save-profile", "Simpan profil");
        save.getStyleClass().add("tb-primary");
        program.getStyleClass().add("tb-input");
        String[] savedProfile = {profile.name(), profile.studyProgram()};
        java.util.function.Consumer<Runnable> saveProfile =
                done -> {
                    // Pilihan kosong mempertahankan prodi lama sampai pengguna memilih
                    // penggantinya.
                    String newName = name.getText(),
                            newProgram =
                                    program.getValue() == null
                                            ? savedProfile[1]
                                            : program.getValue();
                    tasks.run(
                            profileStatus,
                            () -> {
                                auth.updateProfile(newName, newProgram);
                                return auth.profile();
                            },
                            updated -> {
                                profileStatus.setText("Profil tersimpan.");
                                showProfile(updated);
                                savedProfile[0] = newName;
                                savedProfile[1] = newProgram;
                                done.run();
                            });
                };
        save.setOnAction(event -> saveProfile.accept(() -> {}));
        profileGuard =
                action ->
                        GradeForms.guard(
                                this,
                                !name.getText().equals(savedProfile[0])
                                        || !java.util.Objects.equals(
                                                program.getValue(),
                                                canonicalProgram(savedProfile[1])),
                                () -> {
                                    name.setText(savedProfile[0]);
                                    program.setValue(canonicalProgram(savedProfile[1]));
                                },
                                saveProfile,
                                action);
        academicView = new AcademicView(auth, academic, tasks, this::showProfile);
        DashboardView dashboardContent =
                new DashboardView(
                        learningView,
                        gradeView,
                        this::requestLeave,
                        learningView::refresh,
                        gradeView::refresh,
                        courseHub);
        Tab dashboard = new Tab("Dashboard", dashboardContent);
        Tab profileTab =
                new Tab(
                        "Profil",
                        centeredPage(
                                centeredForm(
                                        heading("Profil"),
                                        fieldCentered("NIM (tidak dapat diubah)", nim),
                                        fieldCentered("Nama", name),
                                        fieldCentered("Program studi", program),
                                        programHelp,
                                        save,
                                        profileStatus),
                                460));
        TabPane tabs =
                new TabPane(dashboard, new Tab("Semester & Mata Kuliah", academicView), profileTab);
        tabs.setId("workspace-tabs");
        tabs.setAccessibleText("Navigasi ruang kerja");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        setCenter(tabs);
        boolean[] selecting = {false};
        tabs.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, old, selected) -> {
                            if (selecting[0]) return;
                            selecting[0] = true;
                            tabs.getSelectionModel().select(old);
                            selecting[0] = false;
                            requestLeave(
                                    () -> {
                                        selecting[0] = true;
                                        tabs.getSelectionModel().select(selected);
                                        selecting[0] = false;
                                        if (selected == dashboard) {
                                            dashboardContent.refreshCurrentArea();
                                        } else if (selected.getContent() == academicView) {
                                            // Kehadiran juga bisa diubah di editor aktual. Muat
                                            // ulang
                                            // setelah guard agar form lama tidak menimpanya saat
                                            // edit SKS.
                                            academicView.refresh();
                                        }
                                    });
                        });
        java.util.function.IntConsumer shortcut =
                index ->
                        requestLeave(
                                () -> {
                                    selecting[0] = true;
                                    tabs.getSelectionModel().select(dashboard);
                                    selecting[0] = false;
                                    dashboardContent.openArea(index);
                                });
        openLearning.setOnAction(e -> shortcut.accept(0));
        openGrades.setOnAction(e -> shortcut.accept(1));
        showProfile(profile);
        academicView.refresh();
        learningView.refresh();
    }

    private void showProfile(UserProfile profile) {
        learningView.preferSemester(profile.activeSemesterId());
        gradeView.preferSemester(profile.activeSemesterId());
        courseHub.activeSemester(profile.activeSemesterId());
        identity.setText("Selamat datang, " + profile.name() + " · NIM " + profile.nim());
        String program =
                profile.studyProgram().isBlank()
                        ? "Program studi belum diisi"
                        : profile.studyProgram();
        if (profile.activeSemesterId().isEmpty()) {
            active.setText(
                    program + " · Belum ada semester aktif. Atur semester untuk mulai belajar.");
        } else {
            uiTasks.run(
                    active,
                    () ->
                            academicService.semesters().stream()
                                    .filter(
                                            s ->
                                                    profile.activeSemesterId()
                                                            .filter(id -> id == s.id())
                                                            .isPresent())
                                    .findFirst(),
                    semester ->
                            active.setText(
                                    program
                                            + " · "
                                            + semester.map(
                                                            s ->
                                                                    "Semester "
                                                                            + s.number()
                                                                            + " · Tahun "
                                                                            + s.year())
                                                    .orElse(
                                                            "Belum ada semester aktif. Atur"
                                                                    + " semester untuk mulai"
                                                                    + " belajar.")));
        }
    }

    private static String canonicalProgram(String value) {
        for (String program : java.util.List.of("Informatika", "Sains Data"))
            if (program.equalsIgnoreCase(value.trim())) return program;
        return null;
    }

    void requestLeave(Runnable action) {
        learningView.guard(
                () -> gradeView.guard(() -> academicView.guard(() -> profileGuard.accept(action))));
    }
}
