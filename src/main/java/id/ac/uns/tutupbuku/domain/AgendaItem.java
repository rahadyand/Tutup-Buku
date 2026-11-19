package id.ac.uns.tutupbuku.domain;

import java.time.*;
import java.util.*;

public record AgendaItem(
        long id,
        long courseId,
        AgendaType type,
        String title,
        LocalDate date,
        Optional<LocalTime> time,
        String description,
        AgendaPriority priority,
        boolean completed,
        long version,
        List<ChecklistStep> checklist) {
    public AgendaItem {
        if (id < 0 || version < 0 || courseId <= 0)
            throw new IllegalArgumentException("Pilih mata kuliah yang sah.");
        if (type == null || priority == null)
            throw new IllegalArgumentException("Pilih jenis dan prioritas.");
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Judul wajib diisi.");
        if (date == null) throw new IllegalArgumentException("Tanggal wajib diisi.");
        title = title.strip();
        description = description == null ? "" : description;
        time = Objects.requireNonNull(time);
        if (time.filter(t -> t.getSecond() != 0 || t.getNano() != 0).isPresent())
            throw new IllegalArgumentException("Jam memakai format HH:mm.");
        checklist = List.copyOf(checklist);
        if (type == AgendaType.EXAM && !checklist.isEmpty())
            throw new IllegalArgumentException(
                    "Hapus langkah tugas secara eksplisit sebelum mengubah jenis menjadi Ujian.");
        Set<String> ids = new HashSet<>();
        for (var step : checklist)
            if (!ids.add(step.id()))
                throw new IllegalArgumentException("Identitas langkah tidak boleh berulang.");
    }

    public AgendaItem withCompleted(boolean value) {
        return new AgendaItem(
                id,
                courseId,
                type,
                title,
                date,
                time,
                description,
                priority,
                value,
                version,
                checklist);
    }

    public int completedSteps() {
        return (int) checklist.stream().filter(ChecklistStep::completed).count();
    }
}
