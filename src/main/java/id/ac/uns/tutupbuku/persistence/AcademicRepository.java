package id.ac.uns.tutupbuku.persistence;

import id.ac.uns.tutupbuku.domain.*;

import java.util.List;

public interface AcademicRepository {
    List<Semester> listSemesters(String ownerNim);

    Semester saveSemester(String ownerNim, Semester semester);

    Semester saveSemesterAndActivate(String ownerNim, Semester semester, boolean activate);

    void deleteSemester(String ownerNim, long semesterId);

    void setActiveSemester(String ownerNim, long semesterId);

    List<Course> listCourses(String ownerNim, long semesterId);

    Course saveCourse(String ownerNim, Course course);

    Course saveCourseMetadata(
            String ownerNim,
            long id,
            long semesterId,
            String name,
            String code,
            java.math.BigDecimal credits);

    void deleteCourse(String ownerNim, long semesterId, long courseId);
}
