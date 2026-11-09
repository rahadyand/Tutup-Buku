package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class LearningPersistenceTest {
    @TempDir Path directory;
    private SqliteDatabase database;
    private SqliteRepository academic;
    private SqliteLearningRepository repository;
    private LearningService learning;
    private UserSession session;
    private long courseId;
    private static final id.ac.uns.tutupbuku.security.PasswordHash HASH = new PasswordHasher().hash("fixture-password".toCharArray());
    @BeforeEach void open() {
        database = new SqliteDatabase(directory.resolve("learning.db")); academic = new SqliteRepository(database);
        academic.createAccount("001", "Satu", HASH); academic.createAccount("002", "Dua", HASH);
        var semester = academic.saveSemester("001", new Semester(0, 3, "2026/2027", SemesterPeriod.ODD));
        courseId = academic.saveCourse("001", new Course(0, semester.id(), "PBO", "", TestData.n("3"), Attendance.assumedMet(), "75%", 0)).id();
        repository = new SqliteLearningRepository(database); session = new UserSession(); session.login("001");
        learning = new LearningService(repository, session, new AssetStore(directory));
    }
    @AfterEach void close() { database.close(); }
    private LearningFolder chapter(String name) { return learning.saveFolder(0, LearningLocation.Kind.CHAPTER, courseId, name); }
    private MaterialReading create(LearningLocation location, String title) { return learning.save(new Material(0, location, title, "# Isi\n\n## Ringkasan\nDitulis manual.", "Asli", 0, 0, ""), "Catatan terpisah", 0); }

    @Test void chaptersFieldsMaterialsCrudAndOrderingSurviveReopen() {
        var first = chapter("Bab 1"); var second = chapter("Bab 2");
        learning.moveFolder(second.location(), -1); assertEquals(second.id(), learning.folders(LearningLocation.Kind.CHAPTER, courseId).getFirst().id());
        assertEquals("Bab Satu", learning.saveFolder(first.id(), first.kind(), courseId, "Bab Satu").name());
        var one = create(first.location(), "Materi 1"); var two = create(first.location(), "Materi 2");
        learning.moveMaterial(two.material().id(), -1); assertEquals(two.material().id(), learning.materials(first.location()).getFirst().id());
        var saved = learning.save(new Material(one.material().id(), first.location(), "Judul baru", "Isi baru", "Sumber baru", 0, one.material().revision(), ""), "Catatan baru", one.noteRevision());
        assertEquals(2, saved.material().revision()); assertFalse(saved.material().updatedAt().isBlank());
        database.close(); database = new SqliteDatabase(directory.resolve("learning.db")); repository = new SqliteLearningRepository(database);
        assertEquals("Judul baru", repository.find("001", one.material().id()).orElseThrow().material().title());
        assertEquals("Catatan baru", repository.find("001", one.material().id()).orElseThrow().note());
        repository.deleteMaterial("001", two.material().id()); assertEquals(1, repository.materials("001", first.location()).size());
        repository.deleteFolder("001", first.location()); assertTrue(repository.find("001", one.material().id()).isEmpty());
        assertEquals(1, repository.folders("001", LearningLocation.Kind.CHAPTER, courseId).size());
    }
    @Test void materialHasOneLocationAndEqualTitlesRemainIndependent() {
        var chapter = chapter("PBO"); var interest = learning.saveFolder(0, LearningLocation.Kind.INTEREST, 0, "Peminatan");
        var one = create(chapter.location(), "Sama"); var two = create(interest.location(), "Sama");
        learning.save(new Material(one.material().id(), chapter.location(), "Sama", "Hanya kuliah berubah", "", 0, one.material().revision(), ""), "", one.noteRevision());
        assertTrue(learning.open(two.material().id()).material().markdown().contains("Ringkasan"));
        assertThrows(IllegalArgumentException.class, () -> learning.save(new Material(one.material().id(), interest.location(), "Pindah", "x", "", 0, 2, ""), "", 2));
        assertThrows(StorageException.class, () -> database.write(connection -> {
            try (var statement = connection.prepareStatement("INSERT INTO materials(owner_nim,chapter_id,field_id,title,position,updated_at) VALUES(?,?,?,?,?,?)")) {
                statement.setString(1, "001"); statement.setLong(2, chapter.id()); statement.setLong(3, interest.id()); statement.setString(4, "Dua lokasi"); statement.setInt(5, 0); statement.setString(6, "now"); statement.executeUpdate();
            } return null;
        }));
    }
    @Test void allLearningOperationsAreScopedToAccount() {
        var folder = chapter("Privat"); var material = create(folder.location(), "Privat");
        session.login("002");
        assertThrows(IllegalArgumentException.class, () -> learning.folders(LearningLocation.Kind.CHAPTER, courseId));
        assertThrows(IllegalArgumentException.class, () -> learning.materials(folder.location()));
        assertThrows(IllegalArgumentException.class, () -> learning.open(material.material().id()));
        assertThrows(IllegalArgumentException.class, () -> learning.deleteFolder(folder.location()));
        assertThrows(IllegalArgumentException.class, () -> learning.moveMaterial(material.material().id(), 1));
        assertThrows(IllegalArgumentException.class, () -> learning.deleteMaterial(material.material().id()));
        assertThrows(IllegalArgumentException.class, () -> learning.complete(material.material().id(), true));
        assertThrows(IllegalArgumentException.class, () -> learning.save(material.material(), "Dibajak", material.noteRevision()));
        assertTrue(learning.folders(LearningLocation.Kind.INTEREST, 0).isEmpty());
        session.login("001"); assertEquals("Catatan terpisah", learning.open(material.material().id()).note());
    }
    @Test void courseProgressCountsMaterialsAcrossUnequalChapterSizes() {
        var small = chapter("Kecil"); var big = chapter("Besar");
        for (int i = 0; i < 2; i++) learning.complete(create(small.location(), "Kecil" + i).material().id(), true);
        for (int i = 0; i < 8; i++) create(big.location(), "Besar" + i);
        assertEquals(new LearningProgress(2, 10), learning.courseProgress(courseId));
        assertEquals("2/10 selesai (20%)", learning.courseProgress(courseId).display());
        assertEquals(new LearningProgress(2, 2), learning.progress(small.location()));
        create(big.location(), "Tambah"); assertEquals(11, learning.courseProgress(courseId).total());
    }
    @Test void emptyProgressDoesNotDivideByZeroAndOpeningDoesNotComplete() {
        assertEquals("Belum ada materi", learning.courseProgress(courseId).display());
        var folder = chapter("Kosong"); assertEquals("Belum ada materi", learning.progress(folder.location()).display());
        var material = create(folder.location(), "Baru"); assertFalse(learning.open(material.material().id()).completed());
        learning.complete(material.material().id(), true); learning.complete(material.material().id(), false);
        assertFalse(learning.open(material.material().id()).completed());
    }
    @Test void noteCompletionAndReadingPositionPersistWithoutChangingContentRevision() {
        var folder = chapter("Bab"); var material = create(folder.location(), "Baca"); long id = material.material().id();
        var noted = learning.save(material.material(), "Catatan saya", material.noteRevision()); learning.complete(id, true);
        assertEquals(material.material().revision(), noted.material().revision());
        learning.bookmarkWriter(new ReadingBookmark(id, noted.material().revision(), 0.65, folder.location())).run();
        database.close(); database = new SqliteDatabase(directory.resolve("learning.db")); repository = new SqliteLearningRepository(database);
        learning = new LearningService(repository, session, new AssetStore(directory));
        assertTrue(learning.open(id).completed()); assertEquals("Catatan saya", learning.open(id).note());
        assertEquals(0.65, learning.resume().scrollRatio(), 0.0001);
    }
    @Test void resumeFallsBackOnChangedRevisionOrDeletedMaterial() {
        assertTrue(learning.resume().reading().isEmpty());
        var folder = chapter("Bab"); var material = create(folder.location(), "Baca"); long id = material.material().id();
        learning.bookmarkWriter(new ReadingBookmark(id, 1, 0.8, folder.location())).run();
        learning.save(new Material(id, folder.location(), "Baca", "Isi berubah", "Asli", 0, 1, ""), material.note(), material.noteRevision());
        assertEquals(0, learning.resume().scrollRatio()); assertTrue(learning.resume().message().contains("berubah"));
        learning.deleteMaterial(id); assertTrue(learning.resume().reading().isEmpty()); assertTrue(learning.resume().message().contains("dihapus"));
        assertEquals(folder.location(), learning.resume().previousLocation().orElseThrow());
        session.login("002"); assertTrue(learning.resume().previousLocation().isEmpty());
    }
    @Test void staleSavesCannotOverwriteNewerContentAndFailingBundleRollsBack() {
        var folder = chapter("Bab"); var old = create(folder.location(), "Baca"); long id = old.material().id();
        var newer = learning.save(new Material(id, folder.location(), "Baru", "Versi baru", "", 0, 1, ""), "Baru", old.noteRevision());
        assertThrows(IllegalArgumentException.class, () -> learning.save(old.material(), "Lama", old.noteRevision()));
        assertEquals("Baru", learning.open(id).material().title());
        database.read(connection -> { try (var statement = connection.createStatement()) { statement.execute("CREATE TRIGGER fail_note BEFORE UPDATE OF note ON learning_state BEGIN SELECT RAISE(ABORT,'failure'); END"); } return null; });
        assertThrows(StorageException.class, () -> learning.save(new Material(id, folder.location(), "Gagal", "Gagal", "", 0, newer.material().revision(), ""), "Gagal", newer.noteRevision()));
        assertEquals("Baru", learning.open(id).material().title()); assertEquals("Baru", learning.open(id).note());
    }
    @Test void newerEditorInputSurvivesEarlierSaveAcknowledgmentAndPersistsOnNextSave() {
        var folder = chapter("Antrean"); var original = create(folder.location(), "Awal");
        var draft = new EditorDraft(original);
        draft.edit("Judul", "Isi A", "", "Catatan A"); var first = draft.snapshot();
        draft.edit("Judul", "Isi B", "", "Catatan B");
        var savedA = learning.save(first.material(), first.note(), first.noteRevision());
        draft.acknowledge(first, savedA);
        assertTrue(draft.dirty()); assertEquals("Isi B", draft.snapshot().material().markdown());
        var next = draft.snapshot();
        var savedB = learning.save(next.material(), next.note(), next.noteRevision());
        draft.acknowledge(next, savedB);
        assertFalse(draft.dirty());
        assertEquals("Isi B", learning.open(original.material().id()).material().markdown());
        assertEquals("Catatan B", learning.open(original.material().id()).note());
        assertThrows(IllegalArgumentException.class, () -> learning.save(first.material(), first.note(), first.noteRevision()));
        assertEquals("Isi B", learning.open(original.material().id()).material().markdown());
    }
    @Test void seedIsIdempotentIndependentPerAccountAndDoesNotOverwriteEdits() {
        assertTrue(learning.initializeStarter(courseId)); assertFalse(learning.initializeStarter(courseId));
        var chapter = learning.folders(LearningLocation.Kind.CHAPTER, courseId).getFirst();
        assertEquals(3, learning.materials(chapter.location()).size());
        assertEquals(2, learning.materials(learning.folders(LearningLocation.Kind.INTEREST, 0).getFirst().location()).size());
        var first = learning.open(learning.materials(chapter.location()).getFirst().id());
        learning.save(new Material(first.material().id(), chapter.location(), "Edit sendiri", "Konten sendiri", "", 0, 1, ""), "", first.noteRevision());
        assertFalse(learning.initializeStarter(courseId)); assertEquals("Edit sendiri", learning.open(first.material().id()).material().title());
        session.login("002");
        var semester = academic.saveSemester("002", new Semester(0, 3, "2026/2027", SemesterPeriod.ODD));
        long otherCourse = academic.saveCourse("002", new Course(0, semester.id(), "PBO", "", TestData.n("3"), Attendance.assumedMet(), "75%", 0)).id();
        assertTrue(learning.initializeStarter(otherCourse));
        var otherChapter = learning.folders(LearningLocation.Kind.CHAPTER, otherCourse).getFirst();
        assertEquals("Class dan Object", learning.materials(otherChapter.location()).getFirst().title());
        assertNotEquals(first.material().id(), learning.materials(otherChapter.location()).getFirst().id());
    }
    @Test void deletingCourseCascadesContentNotesCompletionButOtherContextsRemain() {
        var folder = chapter("Bab"); var owned = create(folder.location(), "Kuliah");
        var field = learning.saveFolder(0, LearningLocation.Kind.INTEREST, 0, "Bidang"); var independent = create(field.location(), "Peminatan");
        var semesterId = academic.listSemesters("001").getFirst().id(); academic.deleteCourse("001", semesterId, courseId);
        assertTrue(repository.find("001", owned.material().id()).isEmpty()); assertTrue(repository.find("001", independent.material().id()).isPresent());
    }
    @Test void orderingDoesNotInvalidateReadingOrDeleteNotes() {
        var folder = chapter("Bab"); var one = create(folder.location(), "Satu"); var two = create(folder.location(), "Dua");
        learning.bookmarkWriter(new ReadingBookmark(one.material().id(), 1, 0.5, folder.location())).run();
        learning.moveMaterial(two.material().id(), -1);
        assertEquals(0.5, learning.resume().scrollRatio()); assertEquals(one.note(), learning.open(one.material().id()).note());
    }
    @Test void interestFieldRenameAndOrderingDoNotDependOnSelectedCourse() {
        var one = learning.saveFolder(0, LearningLocation.Kind.INTEREST, courseId, "Bidang satu");
        var two = learning.saveFolder(0, LearningLocation.Kind.INTEREST, courseId, "Bidang dua");
        assertEquals(0, one.courseId());
        learning.saveFolder(one.id(), one.kind(), courseId, "Nama baru"); learning.moveFolder(two.location(), -1);
        assertEquals(two.id(), learning.folders(LearningLocation.Kind.INTEREST, 0).getFirst().id());
        assertEquals("Nama baru", learning.folder(one.location()).orElseThrow().name());
    }
}
