package id.ac.uns.tutupbuku.domain;

public record Attendance(boolean met, boolean confirmed) {
    public static Attendance assumedMet() {
        return new Attendance(true, false);
    }
}
