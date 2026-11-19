package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.persistence.AgendaRepository;

import java.time.*;
import java.time.format.*;
import java.util.*;

public final class AgendaService {
    private final AgendaRepository repository;
    private final UserSession session;
    private final Clock clock;
    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    public AgendaService(AgendaRepository repository, UserSession session) {
        this(repository, session, Clock.system(ZoneId.of("Asia/Jakarta")));
    }

    public AgendaService(AgendaRepository repository, UserSession session, Clock clock) {
        this.repository = repository;
        this.session = session;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }
    public LocalDateTime now() { return LocalDateTime.now(clock); }

    public static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value == null ? "" : value.trim(), DATE_FORMAT);
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Isi tanggal yang sah dengan format dd/MM/yyyy.");
        }
    }

    public static Optional<LocalTime> parseTime(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            if (!value.trim().matches("\\d{2}:\\d{2}"))
                throw new DateTimeParseException("HH:mm", value, 0);
            return Optional.of(LocalTime.parse(value.trim()));
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException(
                    "Jam opsional memakai HH:mm (00:00–23:59), misalnya 09:30.");
        }
    }

    public List<AgendaItem> items(long semester, long course) {
        return repository.list(session.requireNim(), semester, course).stream()
                .sorted(chronological())
                .toList();
    }

    public AgendaItem open(long id) {
        return repository.find(session.requireNim(), id);
    }

    public AgendaItem save(AgendaItem item) {
        if (item.completed()
                && item.completedSteps() != item.checklist().size()
                && (item.id() == 0 || !open(item.id()).completed()))
            throw new IllegalArgumentException(
                    "Konfirmasikan penyelesaian tugas yang masih memiliki langkah belum selesai.");
        return repository.save(session.requireNim(), item);
    }

    public AgendaItem complete(AgendaItem item, boolean completed, boolean confirmUnfinished) {
        if (completed && item.completedSteps() != item.checklist().size() && !confirmUnfinished)
            throw new IllegalArgumentException(
                    "Masih ada langkah belum selesai. Konfirmasikan sebelum menyelesaikan tugas.");
        return repository.save(session.requireNim(), item.withCompleted(completed));
    }

    public void delete(AgendaItem item) {
        repository.delete(session.requireNim(), item.id(), item.version());
    }

    public AgendaSection section(AgendaItem item) {
        return section(item, LocalDateTime.now(clock));
    }

    public Map<AgendaSection, List<AgendaItem>> group(List<AgendaItem> items) {
        return group(items, now());
    }

    public Map<AgendaSection, List<AgendaItem>> group(List<AgendaItem> items, LocalDateTime now) {
        var result = new EnumMap<AgendaSection, List<AgendaItem>>(AgendaSection.class);
        items.stream()
                .sorted(chronological())
                .forEach(
                        i ->
                                result.computeIfAbsent(section(i, now), key -> new ArrayList<>())
                                        .add(i));
        result.replaceAll((key, list) -> List.copyOf(list));
        return Collections.unmodifiableMap(result);
    }

    private AgendaSection section(AgendaItem item, LocalDateTime now) {
        if (item.completed()) return AgendaSection.COMPLETED;
        boolean past =
                item.time()
                        .map(t -> now.isAfter(item.date().atTime(t)))
                        .orElseGet(() -> item.date().isBefore(now.toLocalDate()));
        if (past)
            return item.type() == AgendaType.TASK
                    ? AgendaSection.OVERDUE
                    : AgendaSection.PAST_EXAMS;
        return item.date().equals(now.toLocalDate()) ? AgendaSection.TODAY : AgendaSection.UPCOMING;
    }

    /** Tanpa jam diurutkan lebih dahulu pada tanggalnya; ini bukan deadline tengah malam. */
    public static Comparator<AgendaItem> chronological() {
        return Comparator.comparing(AgendaItem::date)
                .thenComparing(i -> i.time().isPresent() ? 1 : 0)
                .thenComparing(i -> i.time().orElse(LocalTime.MIN))
                .thenComparingLong(AgendaItem::id);
    }
}
