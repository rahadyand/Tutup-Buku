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
        VBox titles = new VBox(5, heading("Tutup Buku"), identity, active);
        HBox.setHgrow(titles, Priority.ALWAYS);
        HBox header = new HBox(16, titles, logout);
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
                                return null;
                            },
                            ignored -> {
                                profileStatus.setText("Profil tersimpan.");
                                identity.setText(newName.trim() + " · NIM " + profile.nim());
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
        Tab dashboard =
                new Tab(
                        "Dashboard",
                        new DashboardView(
                                learningView,
                                gradeView,
                                this::requestLeave,
                                learningView::refresh,
                                gradeView::refresh));
        TabPane tabs =
                new TabPane(
                        dashboard,
                        new Tab(
                                "Profil",
                                scroll(
                                        form(
                                                heading("Profil"),
                                                field("NIM (tidak dapat diubah)", nim),
                                                field("Nama", name),
                                                field("Program studi", program),
                                                save,
                                                profileStatus))),
                        new Tab("Semester & mata kuliah", academicView));
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
        showProfile(profile);
        academicView.refresh();
        learningView.refresh();
    }

    private void showProfile(UserProfile profile) {
        learningView.preferSemester(profile.activeSemesterId());
        gradeView.preferSemester(profile.activeSemesterId());
        identity.setText(profile.name() + " · NIM " + profile.nim());
        active.setText(
                profile.activeSemesterId().isPresent()
                        ? "Semester aktif dipilih. Lihat penandanya di Semester & mata kuliah."
                        : "Belum ada semester aktif. Buat atau pilih semester di Semester & mata"
                                + " kuliah.");
    }

    void requestLeave(Runnable action) {
        learningView.guard(
                () -> gradeView.guard(() -> academicView.guard(() -> profileGuard.accept(action))));
    }
}
