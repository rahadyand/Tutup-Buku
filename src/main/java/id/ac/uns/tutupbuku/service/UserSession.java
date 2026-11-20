package id.ac.uns.tutupbuku.service;

/** Sesi proses eksplisit; owner lokal adalah kunci internal, bukan NIM mahasiswa. */
public final class UserSession {
    public enum State {
        SIGNED_OUT,
        GUEST,
        ACCOUNT
    }

    private String owner;
    private State state = State.SIGNED_OUT;

    public synchronized void login(String nim) {
        owner = nim;
        state = State.ACCOUNT;
    }

    synchronized void enterGuest(String key) {
        owner = key;
        state = State.GUEST;
    }

    public synchronized State state() {
        return state;
    }

    public synchronized void logout() {
        owner = null;
        state = State.SIGNED_OUT;
    }

    public synchronized String requireOwner() {
        if (owner == null)
            throw new IllegalStateException("Buka ruang kerja lokal atau masuk terlebih dahulu.");
        return owner;
    }

    /** Adapter bagi pemanggil M2; query tetap memakai owner yang sama. */
    public synchronized String requireNim() {
        return requireOwner();
    }
}
