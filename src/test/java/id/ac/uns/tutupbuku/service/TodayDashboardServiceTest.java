package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.*;
import java.util.*;

class TodayDashboardServiceTest {
    @TempDir Path directory;
    static final ZoneId WIB = ZoneId.of("Asia/Jakarta");

    static class MutableClock extends Clock {
        Instant instant = LocalDateTime.parse("2026-11-19T23:59:00").atZone(WIB).toInstant();

        public ZoneId getZone() {
            return WIB;
        }

        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        public Instant instant() {
            return instant;
        }
    }

    static class Fixture implements AutoCloseable {
        final SqliteDatabase db;
        final UserSession session = new UserSession();
        final AuthService auth;
        final AcademicService academic;
        final LearningService learning;
        final GradePlanningService grades;
        final AgendaService agenda;
        final TodayDashboardService today;
        final MutableClock clock = new MutableClock();
        final Semester active, archive;
        final Course first, second, old;
        final Material material, interest;
        final AgendaItem task, exam;

        Fixture(Path path) {
            db = new SqliteDatabase(path);
            var repo = new SqliteRepository(db);
            var hash = new PasswordHasher().hash("sandi-uji-lokal".toCharArray());
            repo.createAccount("001", "Mahasiswa Hari Ini", hash);
            repo.createAccount("002", "Akun Baru", hash);
            session.login("001");
            auth = new AuthService(repo, new PasswordHasher(), session);
            academic = new AcademicService(repo, session);
            learning =
                    new LearningService(
                            new SqliteLearningRepository(db),
                            session,
                            new AssetStore(path.getParent()));
            grades = new GradePlanningService(new SqliteGradeRepository(db), session);
            agenda = new AgendaService(new SqliteAgendaRepository(db), session, clock);
            today = new TodayDashboardService(auth, academic, learning, agenda, grades);
            active = academic.saveSemesterForm(0, "3", "2026", true);
            archive = academic.saveSemesterForm(0, "1", "2025", false);
            first =
                    academic.saveCourseMetadata(
                            0,
                            active.id(),
                            "Pemrograman Berorientasi Objek dan Praktikum Perancangan Aplikasi"
                                    + " Desktop",
                            "PBO",
                            "3");
            second = academic.saveCourseMetadata(0, active.id(), "Struktur Data", "", "2");
            old = academic.saveCourseMetadata(0, archive.id(), "Mata Kuliah Arsip", "", "2");
            var folder =
                    learning.saveFolder(0, LearningLocation.Kind.CHAPTER, first.id(), "Bab Awal");
            material =
                    learning.save(
                                    new Material(
                                            0,
                                            folder.location(),
                                            "Mengenal OOP melalui materi panjang dengan contoh,"
                                                    + " gambar, dan latihan mandiri",
                                            ("## Bagian bacaan\n\n"
                                                 + "Isi materi yang dapat dilanjutkan.\n\n")
                                                    .repeat(70),
                                            "",
                                            0,
                                            0,
                                            ""),
                                    "Catatan asli",
                                    0)
                            .material();
            learning.save(
                    new Material(0, folder.location(), "Materi kedua", "Isi", "", 1, 0, ""), "", 0);
            var field = learning.saveFolder(0, LearningLocation.Kind.INTEREST, 0, "Desain");
            interest =
                    learning.save(
                                    new Material(
                                            0,
                                            field.location(),
                                            "Desain visual peminatan",
                                            "# Desain\n\nBacaan peminatan.",
                                            "",
                                            0,
                                            0,
                                            ""),
                                    "",
                                    0)
                            .material();
            learning.complete(material.id(), true);
            learning.bookmarkWriter(
                            new ReadingBookmark(
                                    material.id(), material.revision(), 0.6, material.location()))
                    .run();
            learning.bookmarkWriter(
                            new ReadingBookmark(
                                    interest.id(), interest.revision(), 0.2, interest.location()))
                    .run();
            task =
                    agenda.save(
                            item(
                                    first.id(),
                                    AgendaType.TASK,
                                    "Tugas hari ini tanpa jam",
                                    LocalDate.of(2026, 11, 19),
                                    Optional.empty()));
            exam =
                    agenda.save(
                            item(
                                    second.id(),
                                    AgendaType.EXAM,
                                    "Ujian yang sudah lewat",
                                    LocalDate.of(2026, 11, 18),
                                    Optional.empty()));
            agenda.save(
                    item(
                            first.id(),
                            AgendaType.TASK,
                            "Tugas terlambat dengan judul panjang untuk memeriksa pembungkusan"
                                + " teks, konteks mata kuliah dan aksi pada jendela minimum",
                            LocalDate.of(2026, 11, 18),
                            Optional.empty()));
            agenda.save(
                    item(
                            first.id(),
                            AgendaType.TASK,
                            "Tugas mendatang",
                            LocalDate.of(2026, 11, 21),
                            Optional.of(LocalTime.of(9, 0))));
            agenda.save(
                    item(
                            first.id(),
                            AgendaType.TASK,
                            "Tugas hari ini kedua",
                            LocalDate.of(2026, 11, 19),
                            Optional.empty()));
            agenda.save(
                    item(
                            old.id(),
                            AgendaType.TASK,
                            "Tugas arsip",
                            LocalDate.of(2026, 11, 19),
                            Optional.empty()));
        }

        static AgendaItem item(
                long course,
                AgendaType type,
                String title,
                LocalDate date,
                Optional<LocalTime> time) {
            return new AgendaItem(
                    0,
                    course,
                    type,
                    title,
                    date,
                    time,
                    "Deskripsi asli",
                    AgendaPriority.NORMAL,
                    false,
                    0,
                    List.of());
        }

        void completeGrades() {
            grades.saveComponent(first.id(), "all", "Proyek", "100", "84.5", "100", 0);
            grades.saveActual(
                    first.id(),
                    EvaluationMode.DETAIL,
                    Optional.empty(),
                    new Attendance(true, true));
            grades.saveActual(
                    second.id(),
                    EvaluationMode.DIRECT,
                    Optional.of(LetterGrade.B),
                    new Attendance(true, true));
        }

        void sql(String sql) {
            db.read(
                    c -> {
                        try (var s = c.createStatement()) {
                            s.execute(sql);
                        }
                        return null;
                    });
        }

        public void close() {
            db.close();
        }
    }

    @Test
    void scopesSemesterAccountAndArchiveWithoutActivation() {
        try (var f = new Fixture(directory.resolve("scope.db"))) {
            var active = f.today.load(null);
            assertEquals(f.active.id(), active.viewed().orElseThrow().id());
            assertEquals(2, active.courses().size());
            assertEquals(2, active.agenda().get(AgendaSection.TODAY).size());
            assertEquals(f.agenda.group(f.agenda.items(f.active.id(), 0)), active.agenda());
            var archive = f.today.load(f.archive.id());
            assertEquals(1, archive.courses().size());
            assertEquals(f.old.id(), archive.courses().getFirst().actual().course().id());
            assertEquals(f.active.id(), f.auth.profile().activeSemesterId().orElseThrow());
            assertTrue(archive.readings().stream().allMatch(r -> r.course().isEmpty()));
            f.session.login("002");
            assertTrue(f.today.load(null).courses().isEmpty());
            assertThrows(IllegalArgumentException.class, () -> f.today.load(f.active.id()));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> f.today.resolveReading(active.readings().getFirst()));
        }
    }

    @Test
    void oneLocalDateSnapshotAndExistingAgendaSemantics() {
        try (var f = new Fixture(directory.resolve("date.db"))) {
            var before = f.today.load(null);
            assertEquals(LocalDate.of(2026, 11, 19), before.date());
            assertEquals(2, before.agenda().get(AgendaSection.TODAY).size());
            assertEquals(1, before.agenda().get(AgendaSection.PAST_EXAMS).size());
            f.clock.instant = f.clock.instant.plusSeconds(61);
            var after = f.today.load(null);
            assertEquals(LocalDate.of(2026, 11, 20), after.date());
            assertFalse(after.agenda().containsKey(AgendaSection.TODAY));
            assertEquals(3, after.agenda().get(AgendaSection.OVERDUE).size());
            f.agenda.complete(f.task, true, true);
            assertEquals(2, f.today.load(null).agenda().get(AgendaSection.OVERDUE).size());
        }
    }

    @Test
    void resumeUsesExactHistoryAndRevisionFallbackWithoutSubstitution() {
        try (var f = new Fixture(directory.resolve("reading.db"))) {
            var snapshot = f.today.load(null);
            assertEquals(2, snapshot.readings().size());
            var course =
                    snapshot.readings().stream()
                            .filter(r -> r.course().isPresent())
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    f.material.id(),
                    f.today.resolveReading(course).reading().orElseThrow().material().id());
            assertEquals(0.6, f.today.resolveReading(course).scrollRatio());
            var m = f.material;
            f.learning.save(
                    new Material(
                            m.id(),
                            m.location(),
                            m.title(),
                            m.markdown() + "Modifikasi",
                            m.source(),
                            m.position(),
                            m.revision(),
                            m.updatedAt()),
                    "Catatan asli",
                    f.learning.open(m.id()).noteRevision());
            assertEquals(0, f.today.resolveReading(course).scrollRatio());
            f.learning.deleteMaterial(m.id());
            assertThrows(IllegalArgumentException.class, () -> f.today.resolveReading(course));
            var deleted = f.today.load(null);
            assertEquals(1, deleted.readings().size());
            assertFalse(deleted.readingWarnings().isEmpty());
        }
    }

    @Test
    void progressAndActualGpaRemainHonestAndReadOnly() {
        try (var f = new Fixture(directory.resolve("grade.db"))) {
            var snapshot = f.today.load(null);
            assertEquals(new LearningProgress(1, 2), snapshot.courses().getFirst().progress());
            assertEquals("Belum ada materi", snapshot.courses().get(1).progress().display());
            var draft = f.grades.create(f.active.id(), "Proyeksi tersimpan", new BigDecimal("3"));
            var projections =
                    draft.courses().stream()
                            .map(
                                    c ->
                                            new ScenarioCourse(
                                                    c.basis(),
                                                    EvaluationMode.DIRECT,
                                                    Optional.of(LetterGrade.A),
                                                    new BigDecimal("85"),
                                                    Optional.of(LetterGrade.A),
                                                    new Attendance(true, true),
                                                    Map.of()))
                            .toList();
            f.grades.save(f.grades.rename(draft, draft.name(), draft.targetGpa(), projections));
            assertTrue(f.today.load(null).gpa().value().isEmpty());
            f.completeGrades();
            var full = f.today.load(null);
            assertEquals("3,42", full.gpa().displayValue());
            assertFalse(full.gpa().estimated());
            assertEquals(1, f.grades.scenarios(f.active.id()).size());
            assertEquals("Catatan asli", f.learning.open(f.material.id()).note());
            assertEquals(0.2, f.learning.resume().scrollRatio());
            f.grades.saveActual(
                    f.second.id(),
                    EvaluationMode.DIRECT,
                    Optional.of(LetterGrade.B),
                    Attendance.assumedMet());
            assertTrue(f.today.load(null).gpa().estimated());
        }
    }

    @Test
    void failureIsReportedAndRetryDoesNotResetData() {
        try (var f = new Fixture(directory.resolve("failure.db"))) {
            f.sql("ALTER TABLE agenda_items RENAME TO unavailable_agenda");
            assertThrows(StorageException.class, () -> f.today.load(null));
            f.sql("ALTER TABLE unavailable_agenda RENAME TO agenda_items");
            assertEquals(2, f.today.load(null).courses().size());
            assertEquals(5, f.agenda.items(f.active.id(), 0).size());
            assertEquals("Catatan asli", f.learning.open(f.material.id()).note());
        }
    }

    @Test
    void emptyAccountAndSemesterDoNotInventResults() {
        try (var f = new Fixture(directory.resolve("empty.db"))) {
            f.session.login("002");
            var empty = f.today.load(null);
            assertTrue(empty.viewed().isEmpty());
            assertTrue(empty.gpa().value().isEmpty());
            var sem = f.academic.saveSemesterForm(0, "1", "2026", true);
            var noCourse = f.today.load(null);
            assertEquals(sem.id(), noCourse.viewed().orElseThrow().id());
            assertTrue(noCourse.courses().isEmpty());
            assertTrue(noCourse.gpa().value().isEmpty());
            f.auth.logout();
            assertThrows(IllegalStateException.class, () -> f.today.load(null));
        }
    }
}
