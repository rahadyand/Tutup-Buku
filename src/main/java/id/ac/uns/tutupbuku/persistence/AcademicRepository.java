package id.ac.uns.tutupbuku.persistence;

import id.ac.uns.tutupbuku.domain.*;
import java.util.List;

public interface AcademicRepository {
    List<Semester> listSemesters(String ownerNim);
    Semester saveSemester(String ownerNim, Semester semester);
    void deleteSemester(String ownerNim, long semesterId);
    void setActiveSemester(String ownerNim, long semesterId);
    List<Course> listCourses(String ownerNim, long semesterId);
    Course saveCourse(String ownerNim, Course course);
    void deleteCourse(String ownerNim, long semesterId, long courseId);
}
