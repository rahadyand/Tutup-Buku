package id.ac.uns.tutupbuku.service;

/** Hanya NIM disimpan selama sesi proses; logout tidak menghapus data lokal. */
public final class UserSession {
    private String nim;
    public synchronized void login(String nim) { this.nim = nim; }
    public synchronized void logout() { nim = null; }
    public synchronized String requireNim() {
        if (nim == null) throw new IllegalStateException("Silakan masuk terlebih dahulu.");
        return nim;
    }
}
