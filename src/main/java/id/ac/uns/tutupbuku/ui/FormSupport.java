package id.ac.uns.tutupbuku.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class FormSupport {
    private FormSupport() { }
    static Label status(String id) { Label label = new Label(); label.setId(id); label.setWrapText(true); return label; }
    static TextField text(String id, String prompt) { TextField field = new TextField(); field.setId(id); field.setPromptText(prompt); return field; }
    static PasswordField password(String id) { PasswordField field = new PasswordField(); field.setId(id); return field; }
    static Button button(String id, String text) { Button button = new Button(text); button.setId(id); return button; }
    static VBox field(String label, Node input) { return new VBox(4, new Label(label), input); }
    static VBox form(Node... children) { VBox form = new VBox(12, children); form.setPadding(new Insets(20)); return form; }
    static ScrollPane scroll(Node content) { ScrollPane pane = new ScrollPane(content); pane.setFitToWidth(true); return pane; }
    static Label heading(String text) { Label label = new Label(text); label.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;"); return label; }
    static Label note(String text) { Label label = new Label(text); label.setWrapText(true); return label; }
    static boolean confirm(Node owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner.getScene().getWindow()); alert.setTitle(title); alert.setHeaderText(title); alert.setContentText(message);
        ButtonType delete = new ButtonType("Hapus", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(delete, cancel);
        Button cancelButton = (Button) alert.getDialogPane().lookupButton(cancel);
        ((Button) alert.getDialogPane().lookupButton(delete)).setDefaultButton(false); cancelButton.setDefaultButton(true);
        return alert.showAndWait().filter(delete::equals).isPresent();
    }
}
