package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.domain.*;

import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class AgendaServiceTest {
    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");

    private AgendaItem item(long id, AgendaType type, String date, String time) {
        return new AgendaItem(
                id,
                1,
                type,
                "Judul",
                LocalDate.parse(date),
                AgendaService.parseTime(time),
                "",
                AgendaPriority.NORMAL,
                false,
                1,
                List.of());
    }

    private AgendaService at(String local) {
        return new AgendaService(
                null, null, Clock.fixed(LocalDateTime.parse(local).atZone(WIB).toInstant(), WIB));
    }

    @Test
    void requiredFieldsAndChecklistIdentityAreValidated() {
        var a = item(1, AgendaType.TASK, "2026-11-19", "");
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new AgendaItem(
                                0,
                                0,
                                a.type(),
                                "Judul",
                                a.date(),
                                a.time(),
                                "",
                                a.priority(),
                                false,
                                0,
                                List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new AgendaItem(
                                0,
                                1,
                                a.type(),
                                "  ",
                                a.date(),
                                a.time(),
                                "",
                                a.priority(),
                                false,
                                0,
                                List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new AgendaItem(
                                0,
                                1,
                                a.type(),
                                "Judul",
                                null,
                                a.time(),
                                "",
                                a.priority(),
                                false,
                                0,
                                List.of()));
        var step = new ChecklistStep("stable", " Analisis ", true);
        assertEquals("Analisis", step.text());
        assertThrows(IllegalArgumentException.class, () -> new ChecklistStep("stable", " ", false));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new AgendaItem(
                                0,
                                1,
                                a.type(),
                                "Judul",
                                a.date(),
                                a.time(),
                                "",
                                a.priority(),
                                false,
                                0,
                                List.of(step, step)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new AgendaItem(
                                0,
                                1,
                                AgendaType.EXAM,
                                "Judul",
                                a.date(),
                                a.time(),
                                "",
                                a.priority(),
                                false,
                                0,
                                List.of(step)));
    }

    @Test
    void dateAndTimeParsingIsStrictAndAbsenceIsNotMidnight() {
        assertEquals(LocalDate.of(2024, 2, 29), AgendaService.parseDate("29/02/2024"));
        for (String date : List.of("", "31/02/2026", "29/02/2026", "19-11-2026", "2026-11-19"))
            assertThrows(IllegalArgumentException.class, () -> AgendaService.parseDate(date));
        assertTrue(AgendaService.parseTime("  ").isEmpty());
        assertEquals(Optional.of(LocalTime.MIDNIGHT), AgendaService.parseTime("00:00"));
        assertEquals(Optional.of(LocalTime.of(23, 59)), AgendaService.parseTime(" 23:59 "));
        for (String time : List.of("9:30", "24:00", "12:60", "12:30:00", "abc"))
            assertThrows(IllegalArgumentException.class, () -> AgendaService.parseTime(time));
    }

    @Test
    void dateOnlyTaskStaysTodayUntilNextLocalDate() {
        var a = item(1, AgendaType.TASK, "2026-11-19", "");
        assertEquals(AgendaSection.TODAY, at("2026-11-19T00:00:00").section(a));
        assertEquals(AgendaSection.TODAY, at("2026-11-19T23:59:59.999999999").section(a));
        assertEquals(AgendaSection.OVERDUE, at("2026-11-20T00:00:00").section(a));
    }

    @Test
    void timedTaskBecomesOverdueStrictlyAfterDeadline() {
        var a = item(1, AgendaType.TASK, "2026-11-19", "09:30");
        assertEquals(AgendaSection.TODAY, at("2026-11-19T09:30:00").section(a));
        assertEquals(AgendaSection.OVERDUE, at("2026-11-19T09:30:00.000000001").section(a));
        assertEquals(AgendaSection.UPCOMING, at("2026-11-18T23:59:00").section(a));
    }

    @Test
    void pastExamsAreNotUnsubmittedTasksAndCompletionIsSeparate() {
        var untimed = item(1, AgendaType.EXAM, "2026-11-19", "");
        assertEquals(AgendaSection.TODAY, at("2026-11-19T23:59:00").section(untimed));
        assertEquals(AgendaSection.PAST_EXAMS, at("2026-11-20T00:00:00").section(untimed));
        var timed = item(2, AgendaType.EXAM, "2026-11-19", "09:30");
        assertEquals(AgendaSection.TODAY, at("2026-11-19T09:30:00").section(timed));
        assertEquals(AgendaSection.PAST_EXAMS, at("2026-11-19T09:31:00").section(timed));
        assertEquals(
                AgendaSection.COMPLETED,
                at("2026-11-20T00:00:00").section(timed.withCompleted(true)));
    }

    @Test
    void chronologicalOrderUsesDateThenMissingTimeThenClockTimeThenStableId() {
        var later = item(1, AgendaType.TASK, "2026-11-20", "");
        AgendaItem b = item(20, AgendaType.EXAM, "2026-11-19", "09:30"),
                a = item(10, AgendaType.TASK, "2026-11-19", "09:30");
        AgendaItem midnight = item(3, AgendaType.TASK, "2026-11-19", "00:00"),
                untimed = item(4, AgendaType.TASK, "2026-11-19", "");
        assertEquals(
                List.of(untimed, midnight, a, b, later),
                List.of(later, b, midnight, a, untimed).stream()
                        .sorted(AgendaService.chronological())
                        .toList());
    }

    @Test
    void oneGroupingUsesOneClockReadingAcrossBoundary() {
        var reads = new AtomicInteger();
        Clock clock =
                new Clock() {
                    public ZoneId getZone() {
                        return WIB;
                    }

                    public Clock withZone(ZoneId zone) {
                        return this;
                    }

                    public Instant instant() {
                        return LocalDateTime.parse("2026-11-19T09:30:00")
                                .atZone(WIB)
                                .toInstant()
                                .plusSeconds(reads.getAndIncrement());
                    }
                };
        var service = new AgendaService(null, null, clock);
        var items =
                List.of(
                        item(1, AgendaType.TASK, "2026-11-19", "09:30"),
                        item(2, AgendaType.TASK, "2026-11-19", "09:30"));
        assertEquals(items, service.group(items).get(AgendaSection.TODAY));
        assertEquals(1, reads.get());
    }

    @Test
    void clockZoneDefinesLocalToday() {
        var service =
                new AgendaService(
                        null, null, Clock.fixed(Instant.parse("2026-11-18T18:00:00Z"), WIB));
        assertEquals(LocalDate.of(2026, 11, 19), service.today());
    }
}
