package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.*;
import id.ac.uns.tutupbuku.security.PasswordHasher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.*;

/** Pengukuran opt-in, bukan klaim benchmark pada komputer lain. */
class CalculationPerformanceCheck {
    @TempDir Path directory;

    @Test
    void calculateThirtyCoursesWithTwentyComponents() throws Exception {
        try (var db = new SqliteDatabase(directory.resolve("performance.db"))) {
            var repository = new SqliteRepository(db);
            repository.createAccount(
                    "benchmark",
                    "Data uji fiktif",
                    new PasswordHasher().hash("fixture-password".toCharArray()));
            var semester =
                    repository.saveSemester(
                            "benchmark", new Semester(0, 3, "2026/2027", SemesterPeriod.ODD));
            var session = new UserSession();
            session.login("benchmark");
            var service = new GradePlanningService(new SqliteGradeRepository(db), session);
            for (int course = 0; course < 30; course++) {
                var saved =
                        repository.saveCourse(
                                "benchmark",
                                new Course(
                                        0,
                                        semester.id(),
                                        "Course " + course,
                                        "",
                                        TestData.n("3"),
                                        Attendance.assumedMet(),
                                        "75%",
                                        0));
                for (int item = 0; item < 20; item++)
                    service.saveComponent(
                            saved.id(),
                            null,
                            "Komponen " + item,
                            "5",
                            item < 2 ? "85" : "",
                            "100",
                            item);
            }
            var scenario = service.create(semester.id(), "Benchmark", TestData.n("3"));
            long coldStart = System.nanoTime();
            var first = service.evaluate(scenario);
            long cold = System.nanoTime() - coldStart;
            assertEquals(30, first.courses().size());
            assertTrue(first.gpa().value().isPresent());
            for (int i = 0; i < 3; i++) service.evaluate(scenario);
            long[] times = new long[20];
            for (int i = 0; i < times.length; i++) {
                long start = System.nanoTime();
                service.evaluate(scenario);
                times[i] = System.nanoTime() - start;
            }
            Arrays.sort(times);
            String result =
                    "30 courses × 20 komponen; TargetPlanner + GradeCalculator + IP + review"
                        + " SQLite; setup/render/hash tidak dihitung.\n"
                        + "Java="
                            + System.getProperty("java.runtime.version")
                            + "; OS="
                            + System.getProperty("os.name")
                            + " "
                            + System.getProperty("os.arch")
                            + "; CPU logis="
                            + Runtime.getRuntime().availableProcessors()
                            + "\n"
                            + String.format(
                                    Locale.ROOT,
                                    "Cold=%.3f ms; warmup=3; samples=20; min=%.3f ms; median=%.3f"
                                        + " ms; p95=%.3f ms; max=%.3f ms\n",
                                    cold / 1e6,
                                    times[0] / 1e6,
                                    (times[9] + times[10]) / 2e6,
                                    times[18] / 1e6,
                                    times[19] / 1e6);
            Path output = Path.of("target/verification");
            Files.createDirectories(output);
            Files.writeString(output.resolve("performance.txt"), result);
            System.out.println(result);
            assertTrue(
                    times[19] < 1_000_000_000L,
                    "Target PRD <1detik pada mesin ini; lihat hasil terukur.");
        }
    }
}
