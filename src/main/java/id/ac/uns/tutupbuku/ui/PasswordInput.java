package id.ac.uns.tutupbuku.ui;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Dua representasi satu nilai; tidak menyimpan password di luar umur form. */
final class PasswordInput extends HBox {
    final PasswordField masked;
    final TextField visible;
    private final Button toggle;

    PasswordInput(String id) {
        super(8);
        setAlignment(Pos.CENTER);
        masked = FormSupport.password(id);
        visible = FormSupport.text(id + "-visible", "");
        visible.textProperty().bindBidirectional(masked.textProperty());
        visible.setVisible(false);
        visible.setManaged(false);
        toggle = FormSupport.button(id + "-toggle", "Tampilkan");
        toggle.setMinWidth(98);
        HBox.setHgrow(masked, Priority.ALWAYS);
        HBox.setHgrow(visible, Priority.ALWAYS);
        toggle.setOnAction(e -> setRevealed(!visible.isVisible()));
        getChildren().addAll(masked, visible, toggle);
    }

    private void setRevealed(boolean reveal) {
        visible.setVisible(reveal);
        visible.setManaged(reveal);
        masked.setVisible(!reveal);
        masked.setManaged(!reveal);
        toggle.setText(reveal ? "Sembunyikan" : "Tampilkan");
    }

    void hide() {
        setRevealed(false);
    }

    void clear() {
        masked.clear();
        hide();
    }

    String value() {
        return masked.getText();
    }

    void onSubmit(Runnable action) {
        masked.setOnAction(e -> action.run());
        visible.setOnAction(e -> action.run());
    }
}
