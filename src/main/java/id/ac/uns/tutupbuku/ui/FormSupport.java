package id.ac.uns.tutupbuku.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class FormSupport {
    private FormSupport() {}

    static Label status(String id) {
        Label label = note("");
        label.setId(id);
        label.getStyleClass().add("tb-status");
        return label;
    }

    /** Lebar lokal pilihan view; minimum dan kemampuan menyusut tetap milik kontrol native. */
    static void limitWidth(Region control, double width) {
        control.setPrefWidth(width);
        control.setMaxWidth(width);
    }

    static TextField text(String id, String prompt) {
        TextField field = new TextField();
        field.setId(id);
        field.setPromptText(prompt);
        field.getStyleClass().add("tb-input");
        return field;
    }

    static PasswordField password(String id) {
        PasswordField field = new PasswordField();
        field.setId(id);
        field.getStyleClass().add("tb-input");
        return field;
    }

    static Button button(String id, String text) {
        Button button = new Button(text);
        button.setId(id);
        button.getStyleClass().add("tb-button");
        button.setMinHeight(Region.USE_PREF_SIZE);
        return button;
    }

    static VBox field(String text, Node input) {
        Label label = new Label(text);
        label.getStyleClass().add("tb-field-label");
        if (input instanceof PasswordInput password) password.associateLabel(label);
        else label.setLabelFor(input);
        VBox field = new VBox(4, label, input);
        field.getStyleClass().add("tb-field");
        return field;
    }

    static VBox form(Node... children) {
        VBox form = new VBox(12, children);
        form.setPadding(new Insets(20));
        form.getStyleClass().add("tb-form");
        return form;
    }

    static ScrollPane scroll(Node content) {
        ScrollPane pane = new ScrollPane(content);
        pane.setFitToWidth(true);
        pane.getStyleClass().add("tb-scroll");
        return pane;
    }

    static Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-heading");
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    static Label note(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("tb-help");
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    static VBox fieldCentered(String label, Node input) {
        VBox box = field(label, input);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        return box;
    }

    static VBox centeredForm(Node... children) {
        VBox box = form(children);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        return box;
    }

    static ScrollPane centeredPage(Node content, double width) {
        if (content instanceof Region region) {
            region.getStyleClass().add("tb-form-card");
            region.setMaxWidth(width);
            // Saat tinggi viewport kecil, gulir form utuh alih-alih memotong label panduan.
            region.setMinHeight(Region.USE_PREF_SIZE);
        }
        StackPane container = new StackPane(content);
        container.setPadding(new Insets(12));
        ScrollPane page = scroll(container);
        page.setFitToHeight(true);
        return page;
    }

    /** Dialog memiliki Scene sendiri; gunakan stylesheet yang sama tanpa mengganti aksinya. */
    static void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(FormSupport.class.getResource("tutup-buku.css").toExternalForm());
        pane.getStyleClass().addAll("tb-theme", "tb-workspace", "tb-dialog");
        for (ButtonType type : pane.getButtonTypes()) {
            pane.lookupButton(type).getStyleClass().add("tb-button");
        }
        dialog.setResizable(true);
    }

    static boolean confirm(Node owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner.getScene().getWindow());
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        ButtonType delete = new ButtonType("Hapus", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(delete, cancel);
        Button cancelButton = (Button) alert.getDialogPane().lookupButton(cancel);
        ((Button) alert.getDialogPane().lookupButton(delete)).setDefaultButton(false);
        cancelButton.setDefaultButton(true);
        styleDialog(alert);
        return alert.showAndWait().filter(delete::equals).isPresent();
    }
}
