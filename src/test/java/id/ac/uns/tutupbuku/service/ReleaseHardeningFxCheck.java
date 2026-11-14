package id.ac.uns.tutupbuku.service;

import static id.ac.uns.tutupbuku.service.TestData.n;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;
import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

/** Regresi lintas layar, bukan simulasi ulang method service. Database hanya @TempDir. */
class ReleaseHardeningFxCheck extends FxTestSupport {
    @Test
    void returningToAcademicReloadsAttendanceBeforeMetadataSave() throws Exception {
        var file = directory.resolve("hardening.db");
        long courseId;
        try (var db = new SqliteDatabase(file)) {
            var repo = new SqliteRepository(db);
            repo.createAccount(
                    "001",
                    "Uji lintas layar",
                    new PasswordHasher().hash("sandi-uji-lokal".toCharArray()));
            var semester =
                    repo.saveSemester("001", new Semester(0, 3, "2026/2027", SemesterPeriod.ODD));
            repo.setActiveSemester("001", semester.id());
            courseId =
                    repo.saveCourse(
                                    "001",
                                    new Course(
                                            0,
                                            semester.id(),
                                            "PBO",
                                            "",
                                            n("3"),
                                            Attendance.assumedMet(),
                                            "75%",
                                            0))
                            .id();
        }
        var started = new CompletableFuture<Void>();
        Platform.startup(
                () -> {
                    Platform.setImplicitExit(false);
                    started.complete(null);
                });
        started.get(10, TimeUnit.SECONDS);
        try {
            fx(
                    () -> {
                        stage = new Stage();
                        application = new TutupBukuApplication(file);
                        application.start(stage);
                        stage.setWidth(1366 / stage.getOutputScaleX());
                        stage.setHeight(768 / stage.getOutputScaleY());
                        return null;
                    });
            login();
            tab("workspace-tabs", 1);
            await(() -> stage.getScene().lookup("#view-courses")!=null || stage.getScene().lookup("#courses-page")!=null);
            if (fx(() -> stage.getScene().lookup("#view-courses") != null)) click("view-courses");
            await(() -> stage.getScene().lookup("#course-list")!=null);
            select("course-list", 0);
            assertNull(fx(() -> stage.getScene().lookup("#course-attendance")));
            tab("workspace-tabs", 0);
            click("open-simulation");
            select("grade-courses", 0);
            click("actual-attendance");
            click("actual-attendance-confirmed");
            click("save-actual-settings");
            tab("workspace-tabs", 1);
            await(() -> stage.getScene().lookup("#view-courses")!=null || stage.getScene().lookup("#courses-page")!=null);
            if (fx(() -> stage.getScene().lookup("#view-courses") != null)) click("view-courses");
            await(() -> stage.getScene().lookup("#course-list")!=null);
            select("course-list", 0);
            assertNull(fx(() -> stage.getScene().lookup("#course-attendance")));
            text("course-name", "PBO diperbarui");
            text("course-credits", "4");
            click("save-course");
            try (var db = new SqliteDatabase(file)) {
                var saved =
                        new SqliteRepository(db)
                                .listCourses(
                                        "001",
                                        new SqliteRepository(db)
                                                .listSemesters("001")
                                                .getFirst()
                                                .id())
                                .getFirst();
                assertEquals(courseId, saved.id());
                assertEquals("PBO diperbarui", saved.name());
                assertEquals(0, n("4").compareTo(saved.credits()));
                assertFalse(saved.attendance().met());
                assertTrue(saved.attendance().confirmed());
            }
            screenshot("hardening-academic.png");
        } finally {
            fx(
                    () -> {
                        stage.hide();
                        application.stop();
                        return null;
                    });
            Platform.exit();
        }
    }
}
