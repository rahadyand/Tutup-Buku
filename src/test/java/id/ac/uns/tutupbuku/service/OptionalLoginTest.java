package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;

import javax.imageio.ImageIO;

class OptionalLoginTest {
    @TempDir Path directory;

    static class Context implements AutoCloseable {
        final SqliteDatabase db;
        final UserSession session = new UserSession();
        final AuthService auth;
        final AcademicService academic;
        final LearningService learning;
        final GradePlanningService grades;
        final AgendaService agenda;
        final AssetStore assets;

        Context(Path file) {
            db = new SqliteDatabase(file);
            var repo = new SqliteRepository(db);
            auth = new AuthService(repo, new PasswordHasher(), session);
            academic = new AcademicService(repo, session);
            assets = new AssetStore(file.getParent());
            learning = new LearningService(new SqliteLearningRepository(db), session, assets);
            grades = new GradePlanningService(new SqliteGradeRepository(db), session);
            agenda = new AgendaService(new SqliteAgendaRepository(db), session);
        }

        void sql(String sql) {
            db.write(
                    c -> {
                        try (var s = c.createStatement()) {
                            s.execute(sql);
                        }
                        return null;
                    });
        }

        int count(String table) {
            return db.read(
                    c -> {
                        try (var s = c.createStatement();
                                var r = s.executeQuery("SELECT count(*) FROM " + table)) {
                            r.next();
                            return r.getInt(1);
                        }
                    });
        }

        public void close() {
            db.close();
        }
    }

    private char[] secret() {
        return "sandi-uji-lokal".toCharArray();
    }

    static Material material(Context c, long course, String title) {
        var folder = c.learning.saveFolder(0, LearningLocation.Kind.CHAPTER, course, "Bab lokal");
        return c.learning
                .save(
                        new Material(0, folder.location(), title, "# Materi\n\nIsi", "", 0, 0, ""),
                        "Catatan tersimpan",
                        0)
                .material();
    }

    @Test
    void guestStateAndSingleIdentityPersistWithoutCredentialOrProfileRequirement() {
        Path file = directory.resolve("local.db");
        String owner = null;
        for (int i = 0; i < 3; i++)
            try (var c = new Context(file)) {
                assertEquals(UserSession.State.SIGNED_OUT, c.session.state());
                var guest = c.auth.openGuest();
                assertTrue(c.auth.isGuest());
                assertEquals(UserSession.State.GUEST, c.session.state());
                if (owner == null) owner = guest.nim();
                else assertEquals(owner, guest.nim());
                for (int j = 0; j < 4; j++) assertEquals(owner, c.auth.openGuest().nim());
                assertEquals(1, c.count("users"));
                assertTrue(new SqliteRepository(c.db).findAccount(owner).isEmpty());
                assertThrows(
                        IllegalStateException.class,
                        () -> c.auth.updateProfile("Nama", "Informatika"));
                assertTrue(guest.activeSemesterId().isEmpty());
            }
    }

    @Test
    void allCoreGuestRecordsSurviveReopenWithNoSeedDuplication() throws Exception {
        Path file = directory.resolve("core.db");
        String owner, asset;
        Material material;
        SemesterScenario scenario;
        AgendaItem item;
        long semester, course;
        try (var c = new Context(file)) {
            owner = c.auth.openGuest().nim();
            semester = c.academic.saveSemesterForm(0, "1", "2026", true).id();
            course = c.academic.saveCourseMetadata(0, semester, "Kuliah lokal", "", "3").id();
            material = material(c, course, "Materi lokal");
            c.learning.complete(material.id(), true);
            c.learning
                    .bookmarkWriter(
                            new ReadingBookmark(
                                    material.id(), material.revision(), 0.65, material.location()))
                    .run();
            var png = directory.resolve("uji.png");
            assertTrue(
                    ImageIO.write(
                            new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB),
                            "png",
                            png.toFile()));
            asset = c.learning.importImage(png);
            c.grades.saveComponent(course, "proyek", "Proyek", "100", "8", "10", 0);
            c.grades.saveActual(
                    course, EvaluationMode.DETAIL, Optional.empty(), new Attendance(true, true));
            var actual = c.grades.courses(semester).getFirst();
            scenario =
                    c.grades.save(
                            c.grades.fromWhatIf(
                                    new ScenarioCourse(
                                            actual,
                                            EvaluationMode.DIRECT,
                                            Optional.of(LetterGrade.A),
                                            new BigDecimal("85"),
                                            Optional.of(LetterGrade.A),
                                            new Attendance(true, true),
                                            Map.of()),
                                    "Eksperimen lokal"));
            item =
                    c.agenda.save(
                            new AgendaItem(
                                    0,
                                    course,
                                    AgendaType.TASK,
                                    "Tugas lokal",
                                    LocalDate.of(2026, 11, 20),
                                    Optional.empty(),
                                    "Deskripsi",
                                    AgendaPriority.HIGH,
                                    false,
                                    0,
                                    List.of(new ChecklistStep("langkah", "Analisis", true))));
            assertTrue(c.learning.initializeStarter(course));
            assertFalse(c.learning.initializeStarter(course));
        }
        for (int i = 0; i < 2; i++)
            try (var c = new Context(file)) {
                assertEquals(owner, c.auth.openGuest().nim());
                assertEquals(semester, c.auth.profile().activeSemesterId().orElseThrow());
                assertEquals(course, c.academic.courses(semester).getFirst().id());
                assertEquals("Catatan tersimpan", c.learning.open(material.id()).note());
                assertTrue(c.learning.open(material.id()).completed());
                assertEquals(0.65, c.learning.resumeCourse(course).scrollRatio());
                assertTrue(c.assets.resolve(owner, asset).isPresent());
                assertEquals(scenario, c.grades.open(scenario.id()));
                assertEquals("4,00", c.grades.evaluate(scenario).gpa().displayValue());
                assertEquals(
                        LetterGrade.A_MINUS,
                        c.grades
                                .actualResult(c.grades.courses(semester).getFirst())
                                .effectiveLetter()
                                .orElseThrow());
                assertEquals(item, c.agenda.open(item.id()));
                assertTrue(c.learning.seedInitialized());
                assertFalse(c.learning.initializeStarter(course));
                assertEquals(1, c.count("users"));
                assertEquals(1, c.grades.scenarios(semester).size());
            }
    }

    @Test
    void threeWorkspacesAndManagedAssetsStayIsolated() {
        try (var c = new Context(directory.resolve("isolation.db"))) {
            c.auth.register("001", "A", secret(), secret());
            c.auth.register("002", "B", secret(), secret());
            Map<String, Long> semesters = new LinkedHashMap<>();
            Map<String, Material> materials = new LinkedHashMap<>();
            Map<String, String> images = new LinkedHashMap<>();
            Path png = directory.resolve("image.png");
            try {
                ImageIO.write(
                        new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", png.toFile());
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            String guest = c.auth.openGuest().nim();
            for (String owner : List.of(guest, "001", "002")) {
                if (owner.equals(guest)) c.auth.openGuest();
                else c.auth.login(owner, secret());
                long sem = c.academic.saveSemesterForm(0, "1", "2026", true).id();
                semesters.put(owner, sem);
                long course = c.academic.saveCourseMetadata(0, sem, "Kuliah " + sem, "", "2").id();
                materials.put(owner, material(c, course, "Materi " + sem));
                images.put(owner, c.learning.importImage(png));
                c.grades.save(c.grades.create(sem, "Skenario " + sem, BigDecimal.ZERO));
                c.agenda.save(
                        new AgendaItem(
                                0,
                                course,
                                AgendaType.EXAM,
                                "Ujian " + sem,
                                LocalDate.of(2026, 11, 20),
                                Optional.empty(),
                                "",
                                AgendaPriority.NORMAL,
                                false,
                                0,
                                List.of()));
            }
            for (String owner : semesters.keySet()) {
                if (owner.equals(guest)) c.auth.openGuest();
                else c.auth.login(owner, secret());
                assertEquals(
                        List.of(semesters.get(owner)),
                        c.academic.semesters().stream().map(Semester::id).toList());
                for (String other : semesters.keySet())
                    if (!other.equals(owner)) {
                        assertThrows(
                                IllegalArgumentException.class,
                                () -> c.academic.courses(semesters.get(other)));
                        assertThrows(
                                IllegalArgumentException.class,
                                () -> c.learning.open(materials.get(other).id()));
                        assertThrows(
                                IllegalArgumentException.class,
                                () -> c.grades.scenarios(semesters.get(other)));
                        assertThrows(
                                IllegalArgumentException.class,
                                () -> c.agenda.items(semesters.get(other), 0));
                        assertTrue(c.assets.resolve(owner, images.get(other)).isEmpty());
                        assertThrows(
                                IllegalArgumentException.class,
                                () -> c.learning.save(materials.get(other), "Silang", 1));
                    }
            }
        }
    }

    @Test
    void guestCannotBeClaimedOrAuthenticatedWithAnyPublicPassword() {
        try (var c = new Context(directory.resolve("authentication.db"))) {
            String owner = c.auth.openGuest().nim();
            for (String value : List.of("", "NO_LOGIN", "password", "sandi-uji-lokal", owner)) {
                char[] password = value.toCharArray();
                assertThrows(IllegalArgumentException.class, () -> c.auth.login(owner, password));
                assertArrayEquals(new char[password.length], password);
            }
            assertThrows(
                    IllegalArgumentException.class,
                    () -> c.auth.register(owner, "Klaim", secret(), secret()));
            assertEquals(1, c.count("users"));
            assertEquals(owner, c.auth.openGuest().nim());
            // NIM terdaftar lama dengan tanda baca tetap sah; tidak menambah pola NIM wajib.
            String legacy = "local-workspace:akun-lama";
            c.auth.register(legacy, "Akun lama", secret(), secret());
            assertEquals(legacy, c.auth.login(legacy, secret()).nim());
            assertFalse(c.auth.isGuest());
        }
    }

    private Path legacySeven() throws Exception {
        Path file = directory.resolve("legacy.db");
        try (var f = new TodayDashboardServiceTest.Fixture(file)) {
            f.completeGrades();
            f.grades.save(f.grades.create(f.active.id(), "Skenario lama", new BigDecimal("3")));
            Path image = directory.resolve("legacy.png");
            ImageIO.write(
                    new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB), "png", image.toFile());
            String reference = f.learning.importImage(image);
            var r = f.learning.open(f.material.id());
            var m = r.material();
            f.learning.save(
                    new Material(
                            m.id(),
                            m.location(),
                            m.title(),
                            m.markdown() + "\n![Gambar](" + reference + ")",
                            m.source(),
                            m.position(),
                            m.revision(),
                            m.updatedAt()),
                    r.note(),
                    r.noteRevision());
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                var s = connection.createStatement()) {
            s.execute("DROP INDEX one_local_workspace");
            s.execute("ALTER TABLE users DROP COLUMN account_kind");
            s.execute("PRAGMA user_version=7");
        }
        return file;
    }

    @Test
    void versionSevenMigrationKeepsIdsHashesAndAllRelatedData() throws Exception {
        Path file = legacySeven();
        List<String> before;
        try (var conn = DriverManager.getConnection("jdbc:sqlite:" + file);
                var s = conn.createStatement();
                var r =
                        s.executeQuery(
                                "SELECT"
                                    + " password_algorithm,password_iterations,password_salt,password_hash"
                                    + " FROM users WHERE nim='001'")) {
            r.next();
            before = List.of(r.getString(1), r.getString(2), r.getString(3), r.getString(4));
        }
        try (var c = new Context(file)) {
            c.auth.login("001", secret());
            long sem = c.auth.profile().activeSemesterId().orElseThrow();
            assertEquals(2, c.academic.courses(sem).size());
            assertEquals(5, c.agenda.items(sem, 0).size());
            assertEquals(1, c.grades.scenarios(sem).size());
            assertEquals(
                    "Catatan asli",
                    c.learning
                            .resumeCourse(c.academic.courses(sem).getFirst().id())
                            .reading()
                            .orElseThrow()
                            .note());
            var reading =
                    c.learning
                            .resumeCourse(c.academic.courses(sem).getFirst().id())
                            .reading()
                            .orElseThrow();
            String markdown = reading.material().markdown();
            String reference =
                    markdown.substring(
                            markdown.lastIndexOf("![Gambar](") + 10, markdown.lastIndexOf(")"));
            assertArrayEquals(
                    Files.readAllBytes(directory.resolve("legacy.png")),
                    Files.readAllBytes(c.assets.resolve("001", reference).orElseThrow()));
            var hash = new SqliteRepository(c.db).findAccount("001").orElseThrow().passwordHash();
            assertEquals(
                    before,
                    List.of(
                            hash.algorithm(),
                            Integer.toString(hash.iterations()),
                            hash.salt(),
                            hash.hash()));
            c.auth.openGuest();
            assertTrue(c.academic.semesters().isEmpty());
            assertTrue(c.learning.resume().reading().isEmpty());
        }
    }

    @Test
    void failedMigrationRollsBackWithoutResettingLegacyData() throws Exception {
        Path file = legacySeven();
        try (var conn = DriverManager.getConnection("jdbc:sqlite:" + file);
                var s = conn.createStatement()) {
            s.execute("CREATE TABLE one_local_workspace(marker TEXT)");
        }
        assertThrows(StorageException.class, () -> new SqliteDatabase(file));
        try (var conn = DriverManager.getConnection("jdbc:sqlite:" + file);
                var s = conn.createStatement()) {
            try (var r = s.executeQuery("PRAGMA user_version")) {
                r.next();
                assertEquals(7, r.getInt(1));
            }
            try (var r = s.executeQuery("SELECT count(*) FROM users")) {
                r.next();
                assertEquals(2, r.getInt(1));
            }
            try (var r = s.executeQuery("PRAGMA table_info(users)")) {
                while (r.next()) assertNotEquals("account_kind", r.getString("name"));
            }
            s.execute("DROP TABLE one_local_workspace");
        }
        try (var c = new Context(file)) {
            assertEquals("001", c.auth.login("001", secret()).nim());
            assertTrue(c.auth.openGuest().activeSemesterId().isEmpty());
        }
    }

    @Test
    void guestInitializationFailureIsVisibleAndRetryDoesNotLoseAccounts() {
        try (var c = new Context(directory.resolve("retry.db"))) {
            c.auth.register("001", "A", secret(), secret());
            c.auth.login("001", secret());
            c.sql(
                    "CREATE TRIGGER guest_fail BEFORE INSERT ON users WHEN NEW.account_kind='LOCAL'"
                        + " BEGIN SELECT RAISE(ABORT,'uji gagal'); END");
            assertThrows(StorageException.class, c.auth::openGuest);
            assertEquals("001", c.session.requireOwner());
            assertEquals(1, c.count("users"));
            c.sql("DROP TRIGGER guest_fail");
            c.auth.openGuest();
            assertEquals(2, c.count("users"));
            c.auth.openGuest();
            assertEquals(2, c.count("users"));
            assertEquals("001", c.auth.login("001", secret()).nim());
        }
    }
}
