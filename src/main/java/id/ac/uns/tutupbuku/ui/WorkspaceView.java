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
        academicService = academic;
        uiTasks = tasks;
        learningView = new LearningView(learning, academic, tasks, browser);
        gradeView = new GradeView(academic, planning, tasks);
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
        VBox titles =
                new VBox(
                        3,
                        heading("Tutup Buku"),
                        identity,
                        active,
                        note("Materi tertata, target terencana."));
        HBox.setHgrow(titles, Priority.ALWAYS);
        Button openLearning = button("open-learning", "Modul Belajar"),
                openGrades = button("open-simulation", "Simulasi Nilai");
        VBox navigation = new VBox(8, logout, new HBox(8, openLearning, openGrades));
        navigation.setId("workspace-shortcuts");
        HBox header = new HBox(18, navigation, titles);
        header.setPadding(new Insets(8));
        setTop(header);

        TextField nim = text("profile-nim", "");
        nim.setText(profile.nim());
        nim.setEditable(false);
        TextField name = text("profile-name", "Nama");
        name.setText(profile.name());
        TextField program = text("profile-program", "Program studi (teks bebas)");
        program.setText(profile.studyProgram());
        Label profileStatus = status("profile-status");
        Button save = button("save-profile", "Simpan profil");
        String[] savedProfile = {profile.name(), profile.studyProgram()};
        java.util.function.Consumer<Runnable> saveProfile =
                done -> {
                    String newName = name.getText(), newProgram = program.getText();
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
                                        || !program.getText().equals(savedProfile[1]),
                                () -> {
                                    name.setText(savedProfile[0]);
                                    program.setText(savedProfile[1]);
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
                        gradeView::refresh);
        Tab dashboard = new Tab("Dashboard", dashboardContent);
        Tab profileTab =
                new Tab(
                        "Profil",
                        scroll(
                                form(
                                        heading("Profil"),
                                        field("NIM (tidak dapat diubah)", nim),
                                        field("Nama", name),
                                        field("Program studi (Informatika / Sains Data)", program),
                                        save,
                                        profileStatus)));
        TabPane tabs =
                new TabPane(dashboard, new Tab("Semester & Mata Kuliah", academicView), profileTab);
        tabs.setId("workspace-tabs");
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
                                            learningView.refresh();
                                            gradeView.refresh();
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
        identity.setText(profile.name() + " · NIM " + profile.nim());
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

    void requestLeave(Runnable action) {
        learningView.guard(
                () -> gradeView.guard(() -> academicView.guard(() -> profileGuard.accept(action))));
    }
}
