package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.service.*;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/** Satu snapshot read-only; semua aksi menuju instance workspace yang sudah ada. */
final class TodayDashboardView extends BorderPane {
    interface Navigation {
        void agenda(Semester semester, AgendaSection group, Long item);

        void reading(ResumeResult reading);

        void course(Semester semester, Course course);

        void grades(Semester semester, Course course, GradeView.Entry entry);

        void academic();

        void learning();
    }

    private final TodayDashboardService service;
    private final UiTasks tasks;
    private final Consumer<Runnable> guard;
    private final String owner;
    private final ComboBox<Semester> semesters = new ComboBox<>();
    private final Label status = status("today-status");
    private final VBox sections = new VBox(12);
    private final Label greeting = note("");
    private final Label context = note("");
    private Navigation navigation;
    private boolean filling;
    private long generation;
    private LocalDate loadedDate;

    TodayDashboardView(
            TodayDashboardService service, UiTasks tasks, Consumer<Runnable> guard, String owner) {
        this.service = service;
        this.tasks = tasks;
        this.guard = guard;
        this.owner = owner;
        setId("today-dashboard");
        semesters.setId("today-semester");
        semesters.setMaxWidth(240);
        Button refresh = button("today-refresh", "Muat ulang");
        refresh.setOnAction(e -> guard.accept(this::refresh));
        VBox header =
                new VBox(
                        4,
                        heading("Hari Ini"),
                        greeting,
                        new FlowPane(8, 4, new Label("Semester dilihat"), semesters, refresh),
                        context);
        header.setPadding(new javafx.geometry.Insets(8));
        setTop(header);
        sections.setPadding(new javafx.geometry.Insets(8));
        sections.setMinWidth(0);
        ScrollPane scroll = new ScrollPane(sections);
        scroll.setId("today-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setCenter(scroll);
        setBottom(status);
        semesters
                .valueProperty()
                .addListener(
                        (o, old, value) -> {
                            if (filling || value == null) return;
                            filling = true;
                            semesters.setValue(old);
                            filling = false;
                            guard.accept(() -> load(value.id()));
                        });
    }

    void navigation(Navigation navigation) {
        this.navigation = navigation;
    }

    void invalidate() {
        generation++;
    }

    void refresh() {
        load(semesters.getValue() == null ? null : semesters.getValue().id());
    }

    void refreshDateIfNeeded() {
        if (loadedDate != null && !loadedDate.equals(service.today())) refresh();
    }

    private void load(Long semester) {
        long request = ++generation;
        sections.getChildren().setAll(note("Memuat agenda, bacaan, mata kuliah dan nilai aktual…"));
        tasks.run(
                status,
                () -> service.load(semester),
                snapshot -> {
                    if (request != generation || !owner.equals(snapshot.profile().nim())) return;
                    loadedDate = snapshot.date();
                    greeting.setText(
                            "Halo, "
                                    + (snapshot.profile().name().isBlank()
                                            ? "teman belajar"
                                            : snapshot.profile().name())
                                    + " · "
                                    + snapshot.date()
                                            .format(
                                                    DateTimeFormatter.ofPattern(
                                                            "EEEE, d MMMM uuuu",
                                                            Locale.forLanguageTag("id-ID"))));
                    filling = true;
                    semesters.getItems().setAll(snapshot.semesters());
                    semesters.setValue(snapshot.viewed().orElse(null));
                    filling = false;
                    context.setText(
                            snapshot.viewed()
                                    .map(
                                            s ->
                                                    "Dilihat: "
                                                            + s
                                                            + (snapshot.profile()
                                                                            .activeSemesterId()
                                                                            .filter(
                                                                                    id ->
                                                                                            id
                                                                                                    == s
                                                                                                            .id())
                                                                            .isPresent()
                                                                    ? " (aktif)"
                                                                    : " (arsip; semester aktif"
                                                                            + " tidak diubah)"))
                                    .orElse("Belum ada semester. Mulai dengan membuat semester."));
                    render(snapshot);
                    status.setText(
                            "Data terbaru · nilai aktual dan asumsi kehadiran tersimpan; tanpa"
                                    + " proyeksi skenario.");
                },
                error -> {
                    if (request != generation) return;
                    loadedDate = null;
                    sections.getChildren()
                            .setAll(
                                    note(
                                            "Data Hari Ini belum berhasil dimuat. Jumlah agenda,"
                                                    + " progres, dan IP belum dapat dipastikan."),
                                    action("today-retry", "Coba lagi", () -> load(semester)));
                });
    }

    private Button action(String id, String text, Runnable action) {
        Button button = button(id, text);
        button.setWrapText(true);
        button.setMinWidth(0);
        button.setMaxWidth(Region.USE_PREF_SIZE);
        button.setOnAction(e -> guard.accept(action));
        return button;
    }

    private VBox section(String title, Node... children) {
        VBox box = new VBox(6, heading(title));
        box.getChildren().addAll(children);
        return box;
    }

    private void render(TodayDashboardService.Snapshot data) {
        sections.getChildren().clear();
        Semester semester = data.viewed().orElse(null);
        if (semester == null) {
            sections.getChildren()
                    .add(
                            section(
                                    "Mulai semester Anda",
                                    note(
                                            "Belum ada semester. Data belajar dan nilai Anda tetap"
                                                    + " tersimpan pada akun ini."),
                                    action(
                                            "today-create-semester",
                                            "Buat semester",
                                            navigation::academic)));
            addReadings(data);
            return;
        }
        VBox agenda = section("Perlu perhatian");
        for (AgendaSection group : AgendaSection.values()) {
            List<AgendaItem> items = data.agenda().getOrDefault(group, List.of());
            if (items.isEmpty()) continue;
            AgendaItem item = items.getFirst();
            String course =
                    data.courses().stream()
                            .filter(c -> c.actual().course().id() == item.courseId())
                            .map(c -> c.actual().course().name())
                            .findFirst()
                            .orElse("Mata kuliah tidak tersedia");
            Label groupTitle = note(group + " · " + items.size() + " item, 1 ditampilkan");
            groupTitle.getStyleClass().add("tb-section-title");
            Button openItem =
                    action(
                            "today-item-" + item.id(),
                            item.type() + " · " + item.title(),
                            () -> navigation.agenda(semester, group, item.id()));
            FlowPane actions =
                    new FlowPane(
                            8,
                            4,
                            openItem,
                            action(
                                    "today-all-" + group.name(),
                                    "Lihat semua di Agenda",
                                    () -> navigation.agenda(semester, group, null)));
            actions.setMinWidth(0);
            openItem.maxWidthProperty().bind(actions.widthProperty());
            agenda.getChildren()
                    .add(
                            new VBox(
                                    4,
                                    groupTitle,
                                    actions,
                                    note(
                                            course
                                                    + " · "
                                                    + item.date().format(AgendaService.DATE_FORMAT)
                                                    + " · "
                                                    + item.time()
                                                            .map(Object::toString)
                                                            .orElse("Tanpa jam")
                                                    + " · Prioritas "
                                                    + item.priority()
                                                    + (item.checklist().isEmpty()
                                                            ? ""
                                                            : " · Langkah "
                                                                    + item.completedSteps()
                                                                    + "/"
                                                                    + item.checklist().size()))));
        }
        if (agenda.getChildren().size() == 1)
            agenda.getChildren()
                    .addAll(
                            note(
                                    "Belum ada tugas/ujian terbuka yang tercatat. Ini tidak berarti"
                                            + " seluruh pekerjaan kuliah sudah selesai."),
                            action(
                                    "today-add-agenda",
                                    "Buka Agenda untuk menambah tugas / ujian",
                                    () -> navigation.agenda(semester, null, null)));
        sections.getChildren().add(agenda);
        addReadings(data);
        VBox courses = section("Mata kuliah · " + data.courses().size() + " total");
        for (var summary : data.courses().stream().limit(4).toList()) {
            Course course = summary.actual().course();
            String result =
                    summary.grade()
                            .effectiveLetter()
                            .map(LetterGrade::label)
                            .orElse("Belum lengkap");
            courses.getChildren()
                    .add(
                            section(
                                    course.name()
                                            + " · "
                                            + course.credits().stripTrailingZeros().toPlainString()
                                            + " SKS",
                                    note(
                                            summary.progress().display()
                                                    + " · "
                                                    + summary.openItems()
                                                    + " agenda terbuka · Nilai aktual efektif: "
                                                    + result
                                                    + (summary.grade().estimated()
                                                            ? " (kehadiran masih asumsi)"
                                                            : "")),
                                    new FlowPane(
                                            8,
                                            4,
                                            action(
                                                    "today-course-" + course.id(),
                                                    "Buka mata kuliah",
                                                    () -> navigation.course(semester, course)),
                                            action(
                                                    "today-actual-" + course.id(),
                                                    "Nilai aktual",
                                                    () ->
                                                            navigation.grades(
                                                                    semester,
                                                                    course,
                                                                    GradeView.Entry.ACTUAL)),
                                            action(
                                                    "today-whatif-" + course.id(),
                                                    "What If",
                                                    () ->
                                                            navigation.grades(
                                                                    semester,
                                                                    course,
                                                                    GradeView.Entry.WHAT_IF)))));
        }
        if (data.courses().isEmpty())
            courses.getChildren()
                    .addAll(
                            note("Belum ada mata kuliah dalam semester ini."),
                            action(
                                    "today-add-course",
                                    "Kelola semester & mata kuliah",
                                    navigation::academic));
        else if (data.courses().size() > 4)
            courses.getChildren()
                    .add(
                            action(
                                    "today-all-courses",
                                    "4 ditampilkan · Lihat semua mata kuliah",
                                    () ->
                                            navigation.course(
                                                    semester,
                                                    data.courses().getFirst().actual().course())));
        sections.getChildren().add(courses);
        Label value =
                heading(
                        data.gpa().value().isPresent()
                                ? data.gpa().label() + ": " + data.gpa().displayValue()
                                : "IP semester belum lengkap");
        value.setId("today-gpa");
        VBox grades =
                section(
                        "Kondisi semester",
                        note(
                                "Sumber: nilai aktual seluruh mata kuliah; kehadiran mengikuti"
                                        + " asumsi/konfirmasi tersimpan. Tidak mengambil nilai dari"
                                        + " skenario."),
                        value);
        data.gpa().issues().forEach(issue -> grades.getChildren().add(note(issue)));
        for (var summary : data.courses())
            if (summary.grade().effectiveLetter().isEmpty()) {
                Course course = summary.actual().course();
                grades.getChildren()
                        .add(
                                action(
                                        "today-unresolved-" + course.id(),
                                        "Lengkapi nilai · " + course.name(),
                                        () ->
                                                navigation.grades(
                                                        semester, course, GradeView.Entry.ACTUAL)));
            }
        grades.getChildren()
                .add(
                        action(
                                "today-open-grades",
                                "Buka perencanaan semester & IP",
                                () -> navigation.grades(semester, null, GradeView.Entry.PLANNING)));
        sections.getChildren().add(grades);
    }

    private void addReadings(TodayDashboardService.Snapshot data) {
        VBox reading = section("Lanjutkan membaca");
        if (!data.readings().isEmpty())
            reading.getChildren()
                    .add(
                            note(
                                    "Bacaan terakhir akun jika sesuai konteks, lalu riwayat per"
                                            + " mata kuliah; maksimal 3 bacaan."));
        for (var link : data.readings().stream().limit(3).toList()) {
            long id = link.bookmark().materialId();
            reading.getChildren()
                    .addAll(
                            action(
                                    "today-reading-" + id,
                                    link.resume().reading().orElseThrow().material().title(),
                                    () -> {
                                        long request = generation;
                                        tasks.run(
                                                status,
                                                () -> service.resolveReading(link),
                                                result -> {
                                                    if (request == generation)
                                                        navigation.reading(result);
                                                });
                                    }),
                            note(link.locationLabel()));
        }
        data.readingWarnings().forEach(w -> reading.getChildren().add(note(w)));
        if (data.readings().isEmpty())
            reading.getChildren()
                    .add(
                            note(
                                    "Belum ada referensi bacaan yang dapat dilanjutkan pada konteks"
                                            + " ini."));
        reading.getChildren()
                .add(action("today-open-learning", "Pilih materi belajar", navigation::learning));
        sections.getChildren().add(reading);
    }
}
