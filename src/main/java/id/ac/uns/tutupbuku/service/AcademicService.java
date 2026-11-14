package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.AcademicRepository;

import java.util.List;

/** Pemilik diambil dari sesi, bukan dari field NIM yang dapat diubah pada layar. */
public final class AcademicService {
    private final AcademicRepository repository;
    private final UserSession session;

    public AcademicService(AcademicRepository repository, UserSession session) {
        this.repository = repository;
        this.session = session;
    }

    public List<Semester> semesters() {
        return repository.listSemesters(session.requireNim());
    }

    public Semester saveSemester(long id, int number, String year, SemesterPeriod period) {
        return repository.saveSemester(
                session.requireNim(), new Semester(id, number, year, period));
    }

    public void setActiveSemester(long id) {
        repository.setActiveSemester(session.requireNim(), id);
    }

    public Semester saveSemesterForm(long id, String number, String year, boolean activate) {
        int parsed;
        try {
            parsed = Integer.parseInt(number.trim());
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Nomor semester harus bilangan bulat lebih dari 0.");
        }
        String single = year == null ? "" : year.trim();
        if (!single.matches("[0-9]{4}")
                || Integer.parseInt(single) < 1000
                || Integer.parseInt(single) > 9998)
            throw new IllegalArgumentException(
                    "Tahun harus 4 digit antara 1000–9998, misalnya 2026.");
        String internal = single + "/" + (Integer.parseInt(single) + 1);
        return repository.saveSemesterAndActivate(
                session.requireNim(),
                new Semester(
                        id,
                        parsed,
                        internal,
                        parsed % 2 == 0 ? SemesterPeriod.EVEN : SemesterPeriod.ODD),
                activate);
    }

    public Course saveCourseMetadata(
            long id, long semester, String name, String code, String credits) {
        java.math.BigDecimal parsed;
        try {
            parsed = DecimalInput.parse(credits);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("SKS wajib berupa angka lebih dari 0.");
        }
        return repository.saveCourseMetadata(
                session.requireNim(), id, semester, name, code, parsed);
    }

    public void deleteSemester(long id) {
        repository.deleteSemester(session.requireNim(), id);
    }

    public List<Course> courses(long semesterId) {
        return repository.listCourses(session.requireNim(), semesterId);
    }

    public Course saveCourse(
            long id,
            long semesterId,
            String name,
            String code,
            String credits,
            boolean attendanceMet,
            String reference) {
        java.math.BigDecimal parsedCredits;
        try {
            parsedCredits = DecimalInput.parse(credits);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(
                    "SKS wajib berupa angka lebih dari 0, misalnya 3 atau 2,5.");
        }
        String owner = session.requireNim();
        // Edit nama/SKS tidak membatalkan verifikasi kehadiran yang sudah dilakukan.
        // Mengubah centangnya tetap merupakan asumsi baru sampai diverifikasi di layar nilai.
        boolean confirmed =
                id != 0
                        && repository.listCourses(owner, semesterId).stream()
                                .filter(course -> course.id() == id)
                                .findFirst()
                                .map(
                                        course ->
                                                course.attendance().met() == attendanceMet
                                                        && course.attendance().confirmed())
                                .orElse(false);
        return repository.saveCourse(
                owner,
                new Course(
                        id,
                        semesterId,
                        name,
                        code,
                        parsedCredits,
                        new Attendance(attendanceMet, confirmed),
                        reference,
                        0));
    }

    public void deleteCourse(long semesterId, long id) {
        repository.deleteCourse(session.requireNim(), semesterId, id);
    }
}
