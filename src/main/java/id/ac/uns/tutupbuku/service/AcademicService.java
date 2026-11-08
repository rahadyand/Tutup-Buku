package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.AcademicRepository;
import java.util.List;

/** Pemilik diambil dari sesi, bukan dari field NIM yang dapat diubah pada layar. */
public final class AcademicService {
    private final AcademicRepository repository;
    private final UserSession session;
    public AcademicService(AcademicRepository repository, UserSession session) { this.repository = repository; this.session = session; }
    public List<Semester> semesters() { return repository.listSemesters(session.requireNim()); }
    public Semester saveSemester(long id, int number, String year, SemesterPeriod period) {
        return repository.saveSemester(session.requireNim(), new Semester(id, number, year, period));
    }
    public void setActiveSemester(long id) { repository.setActiveSemester(session.requireNim(), id); }
    public void deleteSemester(long id) { repository.deleteSemester(session.requireNim(), id); }
    public List<Course> courses(long semesterId) { return repository.listCourses(session.requireNim(), semesterId); }
    public Course saveCourse(long id, long semesterId, String name, String code, String credits, boolean attendanceMet, String reference) {
        java.math.BigDecimal parsedCredits;
        try { parsedCredits = DecimalInput.parse(credits); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("SKS wajib berupa angka lebih dari 0, misalnya 3 atau 2,5."); }
        return repository.saveCourse(session.requireNim(), new Course(id, semesterId, name, code, parsedCredits,
                new Attendance(attendanceMet, false), reference, 0));
    }
    public void deleteCourse(long semesterId, long id) { repository.deleteCourse(session.requireNim(), semesterId, id); }
}
