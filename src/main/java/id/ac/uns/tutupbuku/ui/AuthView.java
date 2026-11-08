package id.ac.uns.tutupbuku.ui;

import id.ac.uns.tutupbuku.domain.UserProfile;
import id.ac.uns.tutupbuku.service.AuthService;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;
import static id.ac.uns.tutupbuku.ui.FormSupport.*;

final class AuthView extends VBox {
    AuthView(AuthService auth, UiTasks tasks, Consumer<UserProfile> onLogin) {
        super(12);
        setSpacing(12);
        TextField loginNim = text("login-nim", "NIM, termasuk angka nol di depan");
        PasswordField loginPassword = password("login-password");
        Label loginStatus = status("login-status");
        Button login = button("login", "Masuk");
        TextField registerNim = text("register-nim", "NIM tanpa batas pola khusus");
        TextField registerName = text("register-name", "Nama Anda");
        PasswordField registerPassword = password("register-password");
        PasswordField confirmation = password("register-confirmation");
        Label registerStatus = status("register-status");
        Button register = button("register", "Daftar akun lokal");
        Tab loginTab = new Tab("Masuk", scroll(form(field("NIM", loginNim), field("Password", loginPassword), login, loginStatus)));
        Tab registerTab = new Tab("Daftar", scroll(form(field("NIM", registerNim), field("Nama", registerName),
                field("Password (minimal 8 karakter)", registerPassword), field("Konfirmasi password", confirmation),
                note("Akun hanya berlaku pada perangkat ini. NIM tidak diverifikasi ke kampus. Pemulihan password melalui email belum tersedia; tidak ada password universal."), register, registerStatus)));
        TabPane tabs = new TabPane(loginTab, registerTab); tabs.setId("auth-tabs"); tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        login.setOnAction(event -> {
            String nim = loginNim.getText(); char[] password = loginPassword.getText().toCharArray();
            tasks.run(loginStatus, () -> auth.login(nim, password), profile -> {
                loginPassword.clear(); registerPassword.clear(); confirmation.clear(); onLogin.accept(profile);
            });
        });
        loginPassword.setOnAction(event -> login.fire());
        register.setOnAction(event -> {
            String nim = registerNim.getText(), name = registerName.getText();
            char[] password = registerPassword.getText().toCharArray(), repeated = confirmation.getText().toCharArray();
            tasks.run(registerStatus, () -> { auth.register(nim, name, password, repeated); return nim.trim(); }, registered -> {
                registerPassword.clear(); confirmation.clear(); loginNim.setText(registered);
                loginStatus.setText("Akun berhasil dibuat. Silakan masuk, lalu lengkapi program studi di Profil.");
                tabs.getSelectionModel().select(loginTab);
            });
        });
        getChildren().addAll(heading("Tutup Buku"), note("Materi tertata, target terencana. Masuk untuk membuka data lokal milik Anda."), tabs);
        VBox.setVgrow(tabs, javafx.scene.layout.Priority.ALWAYS);
        setPadding(new javafx.geometry.Insets(28));
    }
}
