package id.ac.uns.tutupbuku.ui;

/** Entry point non-Application untuk launcher jpackage dengan JavaFX pada classpath. */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        javafx.application.Application.launch(TutupBukuApplication.class, args);
    }
}
