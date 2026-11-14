package id.ac.uns.tutupbuku.ui;

import static id.ac.uns.tutupbuku.ui.FormSupport.*;

import id.ac.uns.tutupbuku.domain.UserProfile;
import id.ac.uns.tutupbuku.service.*;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

final class AuthView extends StackPane {
    AuthView(AuthService auth, UiTasks tasks, Consumer<UserProfile> onLogin) {
        TextField loginNim = text("login-nim", "NIM Anda");
        TextField registerNim = text("register-nim", "NIM Anda");
        loginNim.setPrefWidth(200);
        loginNim.setMaxWidth(200);
        registerNim.setPrefWidth(200);
        registerNim.setMaxWidth(200);
        PasswordInput loginPassword = new PasswordInput("login-password");
        PasswordInput registerPassword = new PasswordInput("register-password");
        PasswordInput confirmation = new PasswordInput("register-confirmation");
        TextField name = text("register-name", "Nama lengkap");
        name.setPrefWidth(300);
        name.setMaxWidth(300);
        Label loginStatus = status("login-status"), registerStatus = status("register-status");
        Button login = button("login", "Masuk"), register = button("register", "Daftar akun lokal");
        Label loginHint = guidance(loginNim, null, "login-nim-guidance");
        Label registerHint = guidance(registerNim, name, "register-nim-guidance");
        VBox loginForm =
                centeredForm(
                        heading("Masuk"),
                        fieldCentered("NIM", loginNim),
                        loginHint,
                        fieldCentered("Password", loginPassword),
                        login,
                        loginStatus);
        VBox registerForm =
                centeredForm(
                        heading("Daftar akun lokal"),
                        fieldCentered("NIM", registerNim),
                        registerHint,
                        fieldCentered("Nama", name),
                        fieldCentered("Password (minimal 8 karakter)", registerPassword),
                        fieldCentered("Konfirmasi password", confirmation),
                        note(
                                "Akun lokal; NIM tidak diverifikasi ke kampus. Tidak ada pemulihan"
                                        + " email atau password universal."),
                        register,
                        registerStatus);
        Tab loginTab = new Tab("Masuk", centeredPage(loginForm, 520));
        Tab registerTab = new Tab("Daftar", centeredPage(registerForm, 520));
        TabPane tabs = new TabPane(loginTab, registerTab);
        tabs.setId("auth-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (o, a, b) -> {
                            loginPassword.hide();
                            registerPassword.hide();
                            confirmation.hide();
                        });
        login.setOnAction(
                e -> {
                    String nim = loginNim.getText();
                    char[] secret = loginPassword.value().toCharArray();
                    tasks.run(
                            loginStatus,
                            () -> auth.login(nim, secret),
                            profile -> {
                                loginPassword.clear();
                                registerPassword.clear();
                                confirmation.clear();
                                onLogin.accept(profile);
                            });
                });
        loginPassword.onSubmit(login::fire);
        loginNim.setOnAction(e -> login.fire());
        register.setOnAction(
                e -> {
                    String nim = registerNim.getText(), displayName = name.getText();
                    char[] secret = registerPassword.value().toCharArray(),
                            repeated = confirmation.value().toCharArray();
                    tasks.run(
                            registerStatus,
                            () -> {
                                auth.register(nim, displayName, secret, repeated);
                                return nim.trim();
                            },
                            saved -> {
                                loginPassword.clear();
                                registerPassword.clear();
                                confirmation.clear();
                                loginNim.setText(saved);
                                loginStatus.setText(
                                        "Akun berhasil dibuat. Silakan masuk, lalu simpan program"
                                                + " studi di Profil.");
                                tabs.getSelectionModel().select(loginTab);
                            });
                });
        registerPassword.onSubmit(register::fire);
        confirmation.onSubmit(register::fire);
        name.setOnAction(e -> register.fire());
        registerNim.setOnAction(e -> register.fire());
        Label title = heading("Tutup Buku");
        title.setId("auth-title");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold;");
        Label description =
                note(
                        "Kelola materi kuliah dan peminatan, catatan serta progres belajar."
                                + " Rencanakan target penilaian dan estimasi IP semester.");
        description.setId("auth-description");
        description.setMaxWidth(480);
        description.setMinHeight(Region.USE_PREF_SIZE);
        description.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        VBox panel = new VBox(10, title, description, tabs);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(580);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        getChildren().add(panel);
        setPadding(new Insets(12));
    }

    private Label guidance(TextField input, TextField name, String id) {
        Label label = status(id);
        Runnable update =
                () ->
                        label.setText(
                                NimGuidance.welcome(
                                        name == null ? "" : name.getText(), input.getText()));
        input.textProperty().addListener((o, a, b) -> update.run());
        if (name != null) name.textProperty().addListener((o, a, b) -> update.run());
        update.run();
        return label;
    }
}
