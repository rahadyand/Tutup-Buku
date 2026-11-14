package id.ac.uns.tutupbuku.domain;

public record Semester(
        long id, int number, String academicYear, SemesterPeriod period, boolean legacy) {
    public Semester(long id, int number, String academicYear, SemesterPeriod period) {
        this(id, number, academicYear, period, false);
    }

    public Semester {
        if (number <= 0)
            throw new IllegalArgumentException("Nomor semester harus bilangan bulat lebih dari 0.");
        academicYear = academicYear == null ? "" : academicYear.trim();
        if (!academicYear.matches("[0-9]{4}/[0-9]{4}")) {
            throw new IllegalArgumentException(
                    "Tahun akademik memakai format YYYY/YYYY, misalnya 2026/2027.");
        }
        if (Integer.parseInt(academicYear.substring(5))
                != Integer.parseInt(academicYear.substring(0, 4)) + 1) {
            throw new IllegalArgumentException(
                    "Tahun akhir akademik harus satu tahun setelah tahun awal.");
        }
        if (period == null) throw new IllegalArgumentException("Pilih periode Ganjil atau Genap.");
    }

    public String year() {
        return academicYear.substring(0, 4);
    }

    @Override
    public String toString() {
        return "Semester "
                + number
                + " · "
                + year()
                + (legacy ? " · arsip " + period + " #" + id : "");
    }
}
