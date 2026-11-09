package id.ac.uns.tutupbuku.ui;

import id.ac.uns.tutupbuku.domain.UserProfile;
import id.ac.uns.tutupbuku.service.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import static id.ac.uns.tutupbuku.ui.FormSupport.*;

final class WorkspaceView extends BorderPane {
    private final Label identity = new Label();
    private final Label active = status("active-semester-label");
    private final LearningView learningView;
    WorkspaceView(AuthService auth, AcademicService academic, LearningService learning, UiTasks tasks, UserProfile profile, java.util.function.Consumer<String> browser, Runnable onLogout) {
        learningView = new LearningView(learning, academic, tasks, browser);
        Button logout = button("logout", "Keluar"); logout.setOnAction(event -> learningView.guard(() -> { learningView.suspend(); auth.logout(); onLogout.run(); }));
        VBox titles = new VBox(5, heading("Tutup Buku"), identity, active); HBox.setHgrow(titles, Priority.ALWAYS);
        HBox header = new HBox(20, titles, logout); header.setPadding(new Insets(16)); setTop(header);

        TextField nim = text("profile-nim", ""); nim.setText(profile.nim()); nim.setEditable(false);
        TextField name = text("profile-name", "Nama"); name.setText(profile.name());
        TextField program = text("profile-program", "Program studi (teks bebas)"); program.setText(profile.studyProgram());
        Label profileStatus = status("profile-status"); Button save = button("save-profile", "Simpan profil");
        save.setOnAction(event -> {
            String newName = name.getText(), newProgram = program.getText();
            tasks.run(profileStatus, () -> { auth.updateProfile(newName, newProgram); return null; }, ignored -> {
                profileStatus.setText("Profil tersimpan."); identity.setText(newName.trim() + " · NIM " + profile.nim());
            });
        });
        AcademicView academicView = new AcademicView(auth, academic, tasks, this::showProfile);
        Tab dashboard = new Tab("Dashboard", new DashboardView(learningView, learningView::guard, learningView::refresh));
        TabPane tabs = new TabPane(dashboard,
                new Tab("Profil", scroll(form(heading("Profil"), field("NIM (tidak dapat diubah)", nim), field("Nama", name), field("Program studi", program), save, profileStatus))),
                new Tab("Semester & mata kuliah", academicView));
        tabs.setId("workspace-tabs"); tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE); setCenter(tabs);
        boolean[] selecting = {false};
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selecting[0]) return;
            selecting[0] = true; tabs.getSelectionModel().select(old); selecting[0] = false;
            learningView.guard(() -> { selecting[0] = true; tabs.getSelectionModel().select(selected); selecting[0] = false; if (selected == dashboard) learningView.refresh(); });
        });
        showProfile(profile); academicView.refresh(); learningView.refresh();
    }
    private void showProfile(UserProfile profile) {
        identity.setText(profile.name() + " · NIM " + profile.nim());
        active.setText(profile.activeSemesterId().isPresent() ? "Semester aktif dipilih. Lihat penandanya di Semester & mata kuliah."
                : "Belum ada semester aktif. Buat atau pilih semester di Semester & mata kuliah.");
    }
    void requestLeave(Runnable action) { learningView.guard(action); }
}
