package id.ac.uns.tutupbuku.service;

import id.ac.uns.tutupbuku.ui.DashboardView;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.PixelFormat;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Opt-in karena memerlukan desktop Windows: -Dtest=JavaFxSmokeCheck test.
 * Nama *Check sengaja tidak masuk pola tes default Surefire untuk build domain tanpa desktop.
 */
class JavaFxSmokeCheck {
    @Test void launchesNavigatesAndRestoresAttendanceGrade() throws Exception {
        CompletableFuture<Void> completed = new CompletableFuture<>();
        CompletableFuture<Void> started = new CompletableFuture<>();
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            started.complete(null);
        });
        started.get(30, TimeUnit.SECONDS);
        Platform.runLater(() -> {
            Stage stage = new Stage();
            try {
                // Fixture shell M1 sekarang berada di dashboard setelah login M2.
                stage.setScene(new javafx.scene.Scene(new DashboardView(), 1000, 690));
                stage.show();
                assertTrue(stage.isShowing());
                var root = stage.getScene().getRoot();
                ((Button) root.lookup("#open-learning")).fire();
                assertEquals("Modul belajar", ((TabPane) root.lookup("#main-tabs")).getSelectionModel().getSelectedItem().getText());
                ((Button) root.lookup("#open-simulation")).fire();
                assertEquals("Simulasi nilai", ((TabPane) root.lookup("#main-tabs")).getSelectionModel().getSelectedItem().getText());
                ((Button) root.lookup("#run-example")).fire();
                var output = (TextArea) root.lookup("#calculation-output");
                assertTrue(output.getText().contains("Tugas 60, UTS 60, UAS 60"));
                assertTrue(output.getText().contains("UTS 55, UAS 55"));
                assertTrue(output.getText().contains("UAS 70"));
                var attendance = (CheckBox) root.lookup("#attendance");
                assertTrue(attendance.isSelected());
                attendance.fire();
                assertTrue(output.getText().contains("Huruf efektif: E"));
                assertTrue(output.getText().contains("Nilai asal: 60 / C"));
                attendance.fire();
                assertTrue(output.getText().contains("Huruf efektif: C"));
                assertTrue(output.getText().contains("Status nilai: Lulus"));
                // Snapshot scene milik test sendiri, tidak menangkap jendela pengguna lain.
                root.applyCss();
                root.layout();
                var snapshot = root.snapshot(null, null);
                int width = (int) snapshot.getWidth();
                int height = (int) snapshot.getHeight();
                int[] pixels = new int[width * height];
                snapshot.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
                var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                image.setRGB(0, 0, width, height, pixels, 0, width);
                Path directory = Path.of("target", "verification");
                Files.createDirectories(directory);
                ImageIO.write(image, "png", directory.resolve("javafx-shell.png").toFile());
                completed.complete(null);
            } catch (Throwable error) {
                completed.completeExceptionally(error);
            } finally {
                stage.close();
            }
        });
        try {
            completed.get(30, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
    }
}
