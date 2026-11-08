package id.ac.uns.tutupbuku.ui;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.service.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.function.Consumer;
import static id.ac.uns.tutupbuku.ui.FormSupport.*;

final class AcademicView extends BorderPane {
    private final AuthService auth;
    private final AcademicService academic;
    private final UiTasks tasks;
    private final Consumer<UserProfile> onProfile;
    private final ListView<Semester> semesters = new ListView<>();
    private final ListView<Course> courses = new ListView<>();
    private final TextField number = text("semester-number", "Misalnya 3");
    private final TextField year = text("semester-year", "2026/2027");
    private final ComboBox<SemesterPeriod> period = new ComboBox<>();
    private final TextField courseName = text("course-name", "Nama mata kuliah");
    private final TextField code = text("course-code", "Opsional");
    private final TextField credits = text("course-credits", "Misalnya 3 atau 2,5");
    private final TextField reference = text("course-attendance-reference", Course.DEFAULT_ATTENDANCE_REFERENCE);
    private final CheckBox attendance = new CheckBox("Syarat kehadiran terpenuhi — asumsi, belum diverifikasi");
    private final Label semesterStatus = status("semester-status");
    private final Label courseStatus = status("course-status");
    private final Label loadStatus = status("academic-status");
    private boolean refreshing;
    private UserProfile profile;

    AcademicView(AuthService auth, AcademicService academic, UiTasks tasks, Consumer<UserProfile> onProfile) {
        this.auth = auth; this.academic = academic; this.tasks = tasks; this.onProfile = onProfile;
        semesters.setId("semester-list"); courses.setId("course-list"); period.setId("semester-period"); attendance.setId("course-attendance");
        period.getItems().setAll(SemesterPeriod.values()); period.setValue(SemesterPeriod.ODD);
        semesters.setPrefHeight(145); courses.setPrefHeight(145);
        semesters.setPlaceholder(new Label("Belum ada semester. Klik Semester baru."));
        courses.setPlaceholder(new Label("Pilih semester; belum ada mata kuliah."));
        semesters.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Semester item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item + (profile != null && profile.activeSemesterId().filter(id -> id == item.id()).isPresent() ? " [aktif]" : ""));
            }
        });
        semesters.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (refreshing) return;
            fillSemester(selected); loadCourses();
        });
        courses.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> fillCourse(selected));
        Button newSemester = button("new-semester", "Semester baru");
        newSemester.setOnAction(event -> { refreshing = true; semesters.getSelectionModel().clearSelection(); refreshing = false; fillSemester(null); courses.getItems().clear(); });
        Button saveSemester = button("save-semester", "Simpan semester");
        saveSemester.setOnAction(event -> saveSemester());
        Button active = button("set-active-semester", "Jadikan aktif");
        active.setOnAction(event -> {
            Semester selected = semesters.getSelectionModel().getSelectedItem();
            if (selected == null) { semesterStatus.setText("Pilih semester terlebih dahulu."); return; }
            tasks.run(semesterStatus, () -> { academic.setActiveSemester(selected.id()); return null; }, ignored -> { semesterStatus.setText("Semester aktif tersimpan."); refresh(); });
        });
        Button deleteSemester = button("delete-semester", "Hapus semester");
        deleteSemester.setOnAction(event -> {
            Semester selected = semesters.getSelectionModel().getSelectedItem();
            if (selected == null) { semesterStatus.setText("Pilih semester yang akan dihapus."); return; }
            if (!confirm(this, "Hapus semester?", "Hapus " + selected + " beserta seluruh mata kuliahnya? Jika aktif, pilihan semester aktif dikosongkan. Semester lain tetap tersimpan.")) return;
            tasks.run(semesterStatus, () -> { academic.deleteSemester(selected.id()); return null; }, ignored -> { semesterStatus.setText("Semester dan mata kuliah terkait dihapus."); refresh(); });
        });
        Button newCourse = button("new-course", "Mata kuliah baru");
        newCourse.setOnAction(event -> { courses.getSelectionModel().clearSelection(); fillCourse(null); });
        Button saveCourse = button("save-course", "Simpan mata kuliah"); saveCourse.setOnAction(event -> saveCourse());
        Button deleteCourse = button("delete-course", "Hapus mata kuliah");
        deleteCourse.setOnAction(event -> {
            Semester selectedSemester = semesters.getSelectionModel().getSelectedItem(); Course selected = courses.getSelectionModel().getSelectedItem();
            if (selected == null || selectedSemester == null) { courseStatus.setText("Pilih mata kuliah yang akan dihapus."); return; }
            if (!confirm(this, "Hapus mata kuliah?", "Hapus mata kuliah " + selected.name() + " dari semester ini? Mata kuliah lain tetap tersimpan.")) return;
            tasks.run(courseStatus, () -> { academic.deleteCourse(selectedSemester.id(), selected.id()); return null; }, ignored -> { courseStatus.setText("Mata kuliah dihapus."); loadCourses(); });
        });
        Button reload = button("reload-academic", "Muat ulang daftar"); reload.setOnAction(event -> refresh());
        VBox left = form(heading("Semester"), semesters, newSemester, field("Nomor semester", number), field("Tahun akademik", year), field("Periode", period));
        VBox right = form(heading("Mata kuliah"), courses, newCourse, field("Nama mata kuliah", courseName), field("Kode", code), field("SKS (lebih dari 0)", credits),
                attendance, field("Acuan kehadiran (tidak dihitung otomatis)", reference),
                note("Nilai dan komponen belum tersedia. Checkbox ini menyimpan asumsi kehadiran mata kuliah, bukan absensi per tanggal."));
        BorderPane semesterPanel = new BorderPane(scroll(left));
        semesterPanel.setBottom(form(new FlowPane(8, 8, saveSemester, active, deleteSemester), semesterStatus));
        BorderPane coursePanel = new BorderPane(scroll(right));
        coursePanel.setBottom(form(new FlowPane(8, 8, saveCourse, deleteCourse), courseStatus));
        SplitPane split = new SplitPane(semesterPanel, coursePanel); split.setDividerPositions(0.44);
        HBox loadBar = new HBox(12, reload, loadStatus); loadBar.setPadding(new javafx.geometry.Insets(12));
        setCenter(split); setBottom(loadBar); fillCourse(null);
    }

    void refresh() {
        Semester previous = semesters.getSelectionModel().getSelectedItem();
        long previousId = previous == null ? 0 : previous.id();
        tasks.run(loadStatus, () -> new Snapshot(auth.profile(), academic.semesters()), snapshot -> {
            profile = snapshot.profile(); onProfile.accept(profile);
            refreshing = true; semesters.getItems().setAll(snapshot.semesters());
            Semester selection = snapshot.semesters().stream().filter(s -> s.id() == previousId).findFirst()
                    .orElseGet(() -> snapshot.semesters().stream().filter(s -> profile.activeSemesterId().filter(id -> id == s.id()).isPresent()).findFirst().orElse(null));
            semesters.getSelectionModel().select(selection); semesters.refresh(); refreshing = false;
            fillSemester(selection); loadStatus.setText(snapshot.semesters().isEmpty() ? "Belum ada semester. Buat semester pertama Anda." : "Daftar semester dimuat.");
            loadCourses();
        });
    }
    private void loadCourses() {
        Semester selected = semesters.getSelectionModel().getSelectedItem();
        if (selected == null) { courses.getItems().clear(); fillCourse(null); return; }
        tasks.run(loadStatus, () -> academic.courses(selected.id()), loaded -> {
            courses.getItems().setAll(loaded); fillCourse(null);
            loadStatus.setText("Daftar dimuat. " + selected);
        });
    }
    private void fillSemester(Semester semester) {
        number.setText(semester == null ? "" : Integer.toString(semester.number())); year.setText(semester == null ? "" : semester.academicYear());
        period.setValue(semester == null ? SemesterPeriod.ODD : semester.period());
    }
    private void fillCourse(Course course) {
        courseName.setText(course == null ? "" : course.name()); code.setText(course == null ? "" : course.code());
        credits.setText(course == null ? "" : course.credits().toPlainString().replace('.', ',')); attendance.setSelected(course == null || course.attendance().met());
        reference.setText(course == null ? Course.DEFAULT_ATTENDANCE_REFERENCE : course.attendanceReference());
    }
    private void saveSemester() {
        Semester selected = semesters.getSelectionModel().getSelectedItem(); long id = selected == null ? 0 : selected.id();
        String value = number.getText(), academicYear = year.getText(); SemesterPeriod selectedPeriod = period.getValue();
        tasks.run(semesterStatus, () -> {
            int parsed;
            try { parsed = Integer.parseInt(value.trim()); }
            catch (NumberFormatException error) { throw new IllegalArgumentException("Nomor semester harus bilangan bulat lebih dari 0."); }
            return academic.saveSemester(id, parsed, academicYear, selectedPeriod);
        }, saved -> {
            semesterStatus.setText("Semester tersimpan.");
            // Pilih hasil baru setelah muat ulang, tanpa mengubah pilihan semester aktif secara otomatis.
            tasks.run(loadStatus, () -> new Snapshot(auth.profile(), academic.semesters()), snapshot -> {
                profile = snapshot.profile(); onProfile.accept(profile); refreshing = true;
                semesters.getItems().setAll(snapshot.semesters()); semesters.getSelectionModel().select(saved); semesters.refresh(); refreshing = false;
                fillSemester(saved); loadCourses();
            });
        });
    }
    private void saveCourse() {
        Semester selectedSemester = semesters.getSelectionModel().getSelectedItem();
        if (selectedSemester == null) { courseStatus.setText("Pilih atau buat semester terlebih dahulu."); return; }
        Course selected = courses.getSelectionModel().getSelectedItem(); long id = selected == null ? 0 : selected.id();
        String name = courseName.getText(), courseCode = code.getText(), courseCredits = credits.getText(), attendanceReference = reference.getText();
        boolean met = attendance.isSelected();
        tasks.run(courseStatus, () -> academic.saveCourse(id, selectedSemester.id(), name, courseCode, courseCredits, met, attendanceReference), saved -> {
            courseStatus.setText("Mata kuliah tersimpan.");
            tasks.run(loadStatus, () -> academic.courses(selectedSemester.id()), loaded -> {
                courses.getItems().setAll(loaded); courses.getSelectionModel().select(saved); fillCourse(saved); loadStatus.setText("Daftar mata kuliah dimuat.");
            });
        });
    }
    private record Snapshot(UserProfile profile, List<Semester> semesters) { }
}
