package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.sql.DriverManager;
import java.time.*;
import java.util.*;

class AgendaPersistenceTest {
    @TempDir Path directory;
    private SqliteDatabase db;
    private SqliteRepository academic;
    private UserSession session;
    private AgendaService agenda;
    private GradePlanningService grades;
    private LearningService learning;
    private long semester, archive, first, second, oldCourse, foreignSemester, foreignCourse;
    private MaterialReading material;
    private SemesterScenario scenario;
    private static final id.ac.uns.tutupbuku.security.PasswordHash HASH =
            new PasswordHasher().hash("fixture-agenda-password".toCharArray());

    @BeforeEach
    void setup() {
        db = new SqliteDatabase(directory.resolve("agenda.db"));
        academic = new SqliteRepository(db);
        academic.createAccount("001", "Pertama", HASH);
        academic.createAccount("002", "Kedua", HASH);
        session = new UserSession();
        session.login("001");
        var service = new AcademicService(academic, session);
        semester = service.saveSemesterForm(0, "3", "2026", true).id();
        archive = service.saveSemesterForm(0, "1", "2025", false).id();
        first = service.saveCourseMetadata(0, semester, "PBO", "", "3").id();
        second = service.saveCourseMetadata(0, semester, "Struktur Data", "", "2").id();
        oldCourse = service.saveCourseMetadata(0, archive, "Arsip", "", "2").id();
        session.login("002");
        foreignSemester = service.saveSemesterForm(0, "3", "2026", true).id();
        foreignCourse =
                service.saveCourseMetadata(0, foreignSemester, "Milik akun lain", "", "3").id();
        session.login("001");
        connect();
        grades.saveComponent(first, "nilai", "Nilai", "100", "84.5", "100", 0);
        scenario = grades.save(grades.create(semester, "Skenario lama", TestData.n("3")));
        var chapter = learning.saveFolder(0, LearningLocation.Kind.CHAPTER, first, "Bab");
        material =
                learning.save(
                        new Material(0, chapter.location(), "Materi", "# Isi", "Sumber", 0, 0, ""),
                        "Catatan",
                        0);
        learning.complete(material.material().id(), true);
        learning.bookmarkWriter(
                        new ReadingBookmark(
                                material.material().id(),
                                material.material().revision(),
                                0.6,
                                chapter.location()))
                .run();
    }

    private void connect() {
        agenda = new AgendaService(new SqliteAgendaRepository(db), session);
        grades = new GradePlanningService(new SqliteGradeRepository(db), session);
        learning =
                new LearningService(
                        new SqliteLearningRepository(db), session, new AssetStore(directory));
    }

    @AfterEach
    void close() {
        db.close();
    }

    private AgendaItem task(long course, String title, List<ChecklistStep> steps) {
        return new AgendaItem(
                0,
                course,
                AgendaType.TASK,
                title,
                LocalDate.of(2026, 11, 19),
                Optional.empty(),
                "Deskripsi\nbaris kedua",
                AgendaPriority.NORMAL,
                false,
                0,
                steps);
    }

    private AgendaItem changed(AgendaItem a, String title, List<ChecklistStep> steps) {
        return new AgendaItem(
                a.id(),
                a.courseId(),
                a.type(),
                title,
                a.date(),
                a.time(),
                a.description(),
                a.priority(),
                a.completed(),
                a.version(),
                steps);
    }

    private List<ChecklistStep> steps() {
        return List.of(
                new ChecklistStep("analisis", "Analisis", true),
                new ChecklistStep("implementasi", "Implementasi", false));
    }

    private void sql(String value) {
        db.read(
                c -> {
                    try (var s = c.createStatement()) {
                        s.execute(value);
                    }
                    return null;
                });
    }

    @Test
    void taskExamEditingChecklistOrderAndOptionalTimeSurviveReopening() {
        var a = agenda.save(task(first, " Tugas pertama ", steps()));
        assertEquals("Tugas pertama", a.title());
        assertEquals(1, a.version());
        var reordered =
                agenda.save(
                        changed(
                                a,
                                "Tugas diubah",
                                List.of(a.checklist().get(1), a.checklist().get(0))));
        var exam =
                agenda.save(
                        new AgendaItem(
                                0,
                                second,
                                AgendaType.EXAM,
                                "Ujian",
                                LocalDate.of(2026, 12, 1),
                                Optional.of(LocalTime.of(9, 30)),
                                "",
                                AgendaPriority.HIGH,
                                false,
                                0,
                                List.of()));
        db.read(
                c -> {
                    try (var s =
                            c.prepareStatement(
                                    "SELECT relevant_time FROM agenda_items WHERE id=?")) {
                        s.setLong(1, a.id());
                        try (var r = s.executeQuery()) {
                            r.next();
                            assertNull(r.getString(1));
                        }
                    }
                    return null;
                });
        db.close();
        db = new SqliteDatabase(directory.resolve("agenda.db"));
        connect();
        assertEquals(reordered, agenda.open(a.id()));
        assertEquals(
                List.of("implementasi", "analisis"),
                agenda.open(a.id()).checklist().stream().map(ChecklistStep::id).toList());
        assertEquals(exam, agenda.open(exam.id()));
        assertEquals(2, agenda.items(semester, 0).size());
        assertTrue(agenda.open(a.id()).time().isEmpty());
    }

    @Test
    void completionRequiresApprovalAndReopeningPreservesStepsWithoutChangingGrades() {
        var before = grades.courses(semester);
        var a = agenda.save(task(first, "Tugas", steps()));
        assertThrows(IllegalArgumentException.class, () -> agenda.complete(a, true, false));
        assertEquals(a, agenda.open(a.id()));
        assertThrows(IllegalArgumentException.class, () -> agenda.save(a.withCompleted(true)));
        var completed = agenda.complete(a, true, true);
        assertTrue(completed.completed());
        assertEquals(a.checklist(), completed.checklist());
        var reopened = agenda.complete(completed, false, false);
        assertFalse(reopened.completed());
        assertEquals(a.checklist(), reopened.checklist());
        var allSteps =
                reopened.checklist().stream()
                        .map(s -> new ChecklistStep(s.id(), s.text(), true))
                        .toList();
        var checked = agenda.save(changed(reopened, reopened.title(), allSteps));
        assertFalse(checked.completed());
        assertTrue(agenda.complete(checked, true, false).completed());
        var exam =
                agenda.save(
                        new AgendaItem(
                                0,
                                second,
                                AgendaType.EXAM,
                                "Ujian lewat",
                                LocalDate.of(2020, 1, 1),
                                Optional.empty(),
                                "",
                                AgendaPriority.LOW,
                                false,
                                0,
                                List.of()));
        assertEquals(AgendaSection.PAST_EXAMS, agenda.section(exam));
        agenda.complete(exam, true, false);
        assertEquals(before, grades.courses(semester));
        assertEquals(scenario, grades.open(scenario.id()));
        assertEquals(material.note(), learning.open(material.material().id()).note());
        assertTrue(learning.open(material.material().id()).completed());
    }

    @Test
    void accountsCoursesAndChecklistParentsAreIsolated() {
        var victim = agenda.save(task(first, "Milik pertama", steps()));
        assertThrows(
                IllegalArgumentException.class,
                () -> agenda.save(task(second, "Salin ID langkah", victim.checklist())));
        session.login("002");
        assertThrows(IllegalArgumentException.class, () -> agenda.open(victim.id()));
        assertThrows(IllegalArgumentException.class, () -> agenda.items(semester, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> agenda.save(task(first, "Course asing", List.of())));
        assertThrows(IllegalArgumentException.class, () -> agenda.complete(victim, true, true));
        assertThrows(IllegalArgumentException.class, () -> agenda.delete(victim));
        assertThrows(
                IllegalArgumentException.class,
                () -> agenda.save(task(foreignCourse, "Langkah asing", victim.checklist())));
        var own = agenda.save(task(foreignCourse, "Milik kedua", List.of()));
        var forged =
                new AgendaItem(
                        victim.id(),
                        foreignCourse,
                        own.type(),
                        "Bajak",
                        own.date(),
                        own.time(),
                        "",
                        own.priority(),
                        false,
                        victim.version(),
                        List.of());
        assertThrows(IllegalArgumentException.class, () -> agenda.save(forged));
        assertThrows(IllegalArgumentException.class, () -> agenda.items(foreignSemester, first));
        session.login("001");
        assertEquals(victim, agenda.open(victim.id()));
    }

    @Test
    void semesterAndCourseFiltersDoNotActivateArchiveOrDuplicateItems() {
        var a = agenda.save(task(first, "Satu", List.of()));
        agenda.save(task(second, "Dua", List.of()));
        var old = agenda.save(task(oldCourse, "Arsip", List.of()));
        assertEquals(2, agenda.items(semester, 0).size());
        assertEquals(List.of(a), agenda.items(semester, first));
        assertEquals(List.of(old), agenda.items(archive, 0));
        assertThrows(IllegalArgumentException.class, () -> agenda.items(semester, oldCourse));
        assertEquals(semester, academic.getProfile("001").activeSemesterId().orElseThrow());
        var moved =
                agenda.save(
                        new AgendaItem(
                                a.id(),
                                second,
                                a.type(),
                                a.title(),
                                a.date(),
                                a.time(),
                                a.description(),
                                a.priority(),
                                false,
                                a.version(),
                                a.checklist()));
        assertTrue(agenda.items(semester, first).isEmpty());
        assertEquals(a.id(), moved.id());
        assertEquals(2, agenda.items(semester, 0).size());
    }

    @Test
    void deletionTargetsOneItemAndCourseCascadeMatchesAcademicPolicy() {
        var before = grades.courses(semester);
        var a = agenda.save(task(first, "Hapus item", steps()));
        var b = agenda.save(task(second, "Tetap", List.of()));
        agenda.delete(a);
        assertEquals(before, grades.courses(semester));
        assertEquals(material.material(), learning.open(material.material().id()).material());
        assertEquals(b, agenda.open(b.id()));
        var c = agenda.save(task(first, "Hapus course", steps()));
        academic.deleteCourse("001", semester, first);
        assertThrows(IllegalArgumentException.class, () -> agenda.open(c.id()));
        assertEquals(List.of(b), agenda.items(semester, 0));
        db.read(
                connection -> {
                    try (var s = connection.createStatement();
                            var r = s.executeQuery("SELECT count(*) FROM agenda_steps")) {
                        r.next();
                        assertEquals(0, r.getInt(1));
                    }
                    return null;
                });
        assertEquals(scenario, grades.open(scenario.id()));
        assertTrue(grades.review(scenario).outdated());
    }

    @Test
    void failedSaveRollsBackHeaderChildrenAndVersionAndRetryIsSafe() {
        var a = agenda.save(task(first, "Asli", steps()));
        var draft = changed(a, "Isian tetap", List.of(a.checklist().get(1), a.checklist().get(0)));
        sql(
                "CREATE TRIGGER fail_agenda BEFORE INSERT ON agenda_steps BEGIN SELECT"
                        + " RAISE(ABORT,'fixture'); END");
        assertThrows(StorageException.class, () -> agenda.save(draft));
        assertEquals(a, agenda.open(a.id()));
        assertEquals("Isian tetap", draft.title());
        sql("DROP TRIGGER fail_agenda");
        var retry = agenda.save(draft);
        assertEquals(a.version() + 1, retry.version());
        assertEquals(draft.checklist(), retry.checklist());
        assertThrows(IllegalArgumentException.class, () -> agenda.save(a));
        assertThrows(IllegalArgumentException.class, () -> agenda.delete(a));
        assertEquals(retry, agenda.open(a.id()));
    }

    private void versionSix() throws Exception {
        db.close();
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("agenda.db"));
                var s = c.createStatement()) {
            s.execute("DROP TABLE agenda_steps");
            s.execute("DROP TABLE agenda_items");
            s.execute("DROP INDEX one_local_workspace");
            s.execute("ALTER TABLE users DROP COLUMN account_kind");
            s.execute("PRAGMA user_version=6");
        }
    }

    @Test
    void schemaSixMigrationPreservesPhaseOneToThreeDataAndAssets() throws Exception {
        var before = grades.courses(semester);
        Path asset = directory.resolve("assets/proof.png");
        Files.createDirectories(asset.getParent());
        byte[] bytes = {1, 2, 3};
        Files.write(asset, bytes);
        versionSix();
        for (int i = 0; i < 2; i++) {
            db = new SqliteDatabase(directory.resolve("agenda.db"));
            academic = new SqliteRepository(db);
            connect();
            assertEquals("Pertama", academic.getProfile("001").name());
            assertEquals(semester, academic.getProfile("001").activeSemesterId().orElseThrow());
            assertEquals(before, grades.courses(semester));
            assertEquals(scenario, grades.open(scenario.id()));
            assertEquals("Catatan", learning.open(material.material().id()).note());
            assertTrue(learning.open(material.material().id()).completed());
            assertEquals(0.6, learning.resumeCourse(first).scrollRatio());
            assertArrayEquals(bytes, Files.readAllBytes(asset));
            assertTrue(agenda.items(semester, 0).isEmpty());
            db.close();
        }
    }

    @Test
    void migrationFailureAndBrokenCurrentSchemaNeverResetExistingData() throws Exception {
        versionSix();
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("agenda.db"));
                var s = c.createStatement()) {
            s.execute("CREATE TABLE agenda_steps(collision TEXT)");
        }
        assertThrows(
                StorageException.class, () -> new SqliteDatabase(directory.resolve("agenda.db")));
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("agenda.db"));
                var s = c.createStatement()) {
            try (var r = s.executeQuery("PRAGMA user_version")) {
                r.next();
                assertEquals(6, r.getInt(1));
            }
            try (var r =
                    s.executeQuery(
                            "SELECT count(*) FROM sqlite_master WHERE name='agenda_items'")) {
                r.next();
                assertEquals(0, r.getInt(1));
            }
            try (var r = s.executeQuery("SELECT name FROM users WHERE nim='001'")) {
                r.next();
                assertEquals("Pertama", r.getString(1));
            }
            s.execute("DROP TABLE agenda_steps");
        }
        db = new SqliteDatabase(directory.resolve("agenda.db"));
        connect();
        assertEquals(scenario, grades.open(scenario.id()));
        db.close();
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("agenda.db"));
                var s = c.createStatement()) {
            s.execute("DROP TABLE agenda_steps");
        }
        assertThrows(
                StorageException.class, () -> new SqliteDatabase(directory.resolve("agenda.db")));
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("agenda.db"));
                var s = c.createStatement();
                var r = s.executeQuery("SELECT count(*) FROM users")) {
            r.next();
            assertEquals(2, r.getInt(1));
        }
    }
}
