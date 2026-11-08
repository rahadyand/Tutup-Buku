package id.ac.uns.tutupbuku.domain;

import java.math.BigDecimal;
import java.util.Optional;

public record Course(long id, long semesterId, String name, String code, BigDecimal credits,
                     Attendance attendance, String attendanceReference, long revision) {
    public static final String DEFAULT_ATTENDANCE_REFERENCE = "75% atau 12 dari 16 pertemuan";
    public Course {
        name = name == null ? "" : name.trim();
        code = code == null ? "" : code.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Nama mata kuliah wajib diisi.");
        if (credits == null || credits.signum() <= 0) throw new IllegalArgumentException("SKS wajib berupa angka lebih dari 0.");
        if (attendance == null) throw new IllegalArgumentException("Asumsi kehadiran wajib tersedia.");
        attendanceReference = attendanceReference == null ? "" : attendanceReference.trim();
        if (attendanceReference.isEmpty()) throw new IllegalArgumentException("Acuan kehadiran wajib diisi.");
    }
    /** Adapter ke engine M1: belum ada nilai akademik sampai editor M4 tersedia. */
    public CourseResult withoutGrade() {
        return new CourseResult(Long.toString(id), name, Optional.of(credits), Optional.empty());
    }
    @Override public String toString() { return name + " · " + credits.stripTrailingZeros().toPlainString().replace('.', ',') + " SKS"; }
}
