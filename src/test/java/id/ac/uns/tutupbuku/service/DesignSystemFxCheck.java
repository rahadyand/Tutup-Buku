package id.ac.uns.tutupbuku.service;

import static org.junit.jupiter.api.Assertions.*;

import id.ac.uns.tutupbuku.ui.TutupBukuApplication;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;

/** Kontrak CSS opt-in; galeri hanya fixture tes, bukan halaman produk baru. */
class DesignSystemFxCheck extends FxTestSupport {
    private static <T extends Node> T styled(T node, String... classes) {
        node.getStyleClass().addAll(classes);
        return node;
    }

    private void layout() {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
    }

    private static Color background(Region region) {
        return (Color) region.getBackground().getFills().getFirst().getFill();
    }

    private static double luminance(Color color) {
        double[] c = {color.getRed(), color.getGreen(), color.getBlue()};
        for (int i = 0; i < c.length; i++)
            c[i] = c[i] <= 0.04045 ? c[i] / 12.92 : Math.pow((c[i] + 0.055) / 1.055, 2.4);
        return c[0] * 0.2126 + c[1] * 0.7152 + c[2] * 0.0722;
    }

    private static void contrast(Color a, Color b, double minimum, String role) {
        double x = luminance(a), y = luminance(b);
        double ratio = (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
        System.out.printf(Locale.ROOT, "DESIGN contrast %s=%.2f%n", role, ratio);
        assertTrue(ratio >= minimum, role + "=" + ratio);
    }

    @Test
    void semanticTokensControlStatesAndScopeAreRenderedByJavaFx() throws Exception {
        var started = new CompletableFuture<Void>();
        Platform.startup(
                () -> {
                    Platform.setImplicitExit(false);
                    started.complete(null);
                });
        started.get(10, TimeUnit.SECONDS);
        try {
            fx(
                    () -> {
                        Label brand = styled(new Label("Tutup Buku"), "tb-brand");
                        Label title =
                                styled(new Label("Ruang akademik yang tertata"), "tb-page-title");
                        Label section =
                                styled(new Label("Komponen dan keadaan"), "tb-section-title");
                        Label caption =
                                styled(
                                        new Label("Design System 1.0 / galeri pengujian"),
                                        "tb-caption");
                        Button primary = styled(new Button("Simpan"), "tb-button", "tb-primary");
                        Button secondary = styled(new Button("Batal"), "tb-button");
                        Button danger = styled(new Button("Hapus"), "tb-button", "tb-danger");
                        Button disabled =
                                styled(new Button("Belum tersedia"), "tb-button", "tb-primary");
                        disabled.setDisable(true);
                        TextField input = styled(new TextField("Nama mahasiswa"), "tb-input");
                        TextField invalid =
                                styled(new TextField("Isian belum sah"), "tb-input", "tb-invalid");
                        TextField readonly = styled(new TextField("NIM hanya baca"), "tb-input");
                        readonly.setEditable(false);
                        TextField disabledInput =
                                styled(new TextField("Sedang memproses"), "tb-input");
                        disabledInput.setDisable(true);
                        PasswordField password = styled(new PasswordField(), "tb-input");
                        password.setPromptText("Password tersembunyi");
                        ComboBox<String> combo = styled(new ComboBox<>(), "tb-input");
                        combo.getItems().setAll("Informatika", "Sains Data");
                        combo.setValue("Informatika");
                        CheckBox check = styled(new CheckBox("Pilihan terpilih"), "tb-check");
                        check.setSelected(true);
                        TextArea area = styled(new TextArea("Catatan di panel"), "tb-input");
                        area.setPrefRowCount(2);
                        Label fieldLabel = styled(new Label("Nama"), "tb-field-label");
                        fieldLabel.setLabelFor(input);
                        VBox field = styled(new VBox(4, fieldLabel, input), "tb-field");
                        VBox panel = styled(new VBox(8, area, combo, check), "tb-panel");
                        Label success =
                                styled(
                                        new Label("Tersimpan. Data tetap tersedia."),
                                        "tb-status",
                                        "tb-success");
                        Label warning =
                                styled(
                                        new Label("Periksa asumsi sebelum melanjutkan."),
                                        "tb-status",
                                        "tb-warning");
                        Label error =
                                styled(
                                        new Label("Isian belum sah. Periksa formulir."),
                                        "tb-status",
                                        "tb-error");
                        Label info =
                                styled(
                                        new Label(
                                                "Informasi ditulis dalam teks, bukan warna saja."),
                                        "tb-status");
                        TabPane tabs =
                                styled(
                                        new TabPane(
                                                new Tab("Belajar", new Label("Materi")),
                                                new Tab("Nilai", new Label("Simulasi"))),
                                        "tb-tabs");
                        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
                        tabs.setPrefHeight(90);
                        ListView<String> navigation = styled(new ListView<>(), "tb-nav-list");
                        navigation.getItems().setAll("Dashboard", "Semester", "Profil");
                        navigation.getSelectionModel().select(0);
                        navigation.setPrefHeight(120);
                        VBox card =
                                styled(
                                        new VBox(
                                                12,
                                                brand,
                                                title,
                                                caption,
                                                section,
                                                new HBox(8, primary, secondary, danger, disabled),
                                                new HBox(12, field, invalid, readonly),
                                                new HBox(12, password, disabledInput),
                                                panel,
                                                new HBox(8, success, warning, error),
                                                info,
                                                tabs,
                                                navigation),
                                        "tb-card",
                                        "tb-gap-3");
                        VBox theme = styled(new VBox(card), "tb-theme");
                        ScrollPane scroll = new ScrollPane(theme);
                        scroll.setFitToWidth(true);
                        TextField legacy =
                                styled(
                                        new TextField("Di luar tema: kontrol native tetap"),
                                        "tb-input",
                                        "tb-invalid");
                        VBox shell = styled(new VBox(8, legacy, scroll), "tutup-buku-root");
                        shell.setPadding(new Insets(16));
                        VBox.setVgrow(scroll, Priority.ALWAYS);
                        stage = new Stage();
                        Scene scene = new Scene(shell, 1200, 760);
                        scene.getStylesheets()
                                .add(
                                        TutupBukuApplication.class
                                                .getResource("tutup-buku.css")
                                                .toExternalForm());
                        stage.setScene(scene);
                        stage.show();
                        layout();

                        assertEquals(36, brand.getFont().getSize(), 0.01);
                        assertEquals(22, title.getFont().getSize(), 0.01);
                        assertEquals(17, section.getFont().getSize(), 0.01);
                        assertEquals(12, caption.getFont().getSize(), 0.01);
                        assertEquals("Segoe UI", input.getFont().getFamily());
                        assertEquals(13, input.getFont().getSize(), 0.01);
                        assertEquals(8, field.getSpacing(), 0.01);
                        assertEquals(12, card.getSpacing(), 0.01);
                        assertEquals(new Insets(24), card.getPadding());
                        assertEquals(
                                16,
                                card.getBackground()
                                        .getFills()
                                        .getFirst()
                                        .getRadii()
                                        .getTopLeftHorizontalRadius(),
                                0.01);
                        assertEquals(
                                12,
                                panel.getBackground()
                                        .getFills()
                                        .getFirst()
                                        .getRadii()
                                        .getTopLeftHorizontalRadius(),
                                0.01);
                        assertNull(
                                legacy.getBorder(),
                                "Kelas komponen tanpa tb-theme tidak memigrasikan halaman lama");

                        Map<String, String> tokens = new LinkedHashMap<>();
                        tokens.put("canvas", "#f5f7f6");
                        tokens.put("surface", "#ffffff");
                        tokens.put("surface-muted", "#eef3f2");
                        tokens.put("text", "#20383d");
                        tokens.put("text-muted", "#536a70");
                        tokens.put("border", "#738b90");
                        tokens.put("divider", "#d9e4e2");
                        tokens.put("primary", "#0f766e");
                        tokens.put("primary-hover", "#115e59");
                        tokens.put("primary-pressed", "#134e4a");
                        tokens.put("on-primary", "#ffffff");
                        tokens.put("focus", "#0f766e");
                        tokens.put("info-surface", "#eff8f6");
                        tokens.put("success", "#166534");
                        tokens.put("success-surface", "#edf8ee");
                        tokens.put("warning", "#854d0e");
                        tokens.put("warning-surface", "#fff8e6");
                        tokens.put("danger", "#b42318");
                        tokens.put("danger-hover", "#912018");
                        tokens.put("danger-pressed", "#781c16");
                        tokens.put("danger-surface", "#fff1ef");
                        Label probe = new Label();
                        probe.setManaged(false);
                        theme.getChildren().add(probe);
                        Map<String, Color> colors = new HashMap<>();
                        for (var token : tokens.entrySet()) {
                            probe.setStyle("-fx-text-fill: -tb-" + token.getKey() + ";");
                            layout();
                            assertEquals(
                                    Color.web(token.getValue()),
                                    probe.getTextFill(),
                                    token.getKey());
                            colors.put(token.getKey(), (Color) probe.getTextFill());
                        }
                        for (String role : List.of("text", "text-muted", "primary"))
                            contrast(colors.get(role), colors.get("surface"), 4.5, role);
                        for (String role : List.of("success", "warning", "danger"))
                            contrast(colors.get(role), colors.get(role + "-surface"), 4.5, role);
                        contrast(
                                colors.get("on-primary"),
                                colors.get("primary"),
                                4.5,
                                "primary text");
                        contrast(colors.get("focus"), colors.get("surface"), 3, "focus");
                        contrast(colors.get("border"), colors.get("surface"), 3, "input border");
                        contrast(colors.get("border"), colors.get("canvas"), 3, "border on canvas");
                        contrast(
                                colors.get("border"),
                                colors.get("surface-muted"),
                                3,
                                "border on panel");

                        assertEquals(colors.get("success"), success.getTextFill());
                        assertEquals(colors.get("warning"), warning.getTextFill());
                        assertEquals(colors.get("danger"), error.getTextFill());
                        VBox scale = styled(new VBox(), "tb-panel");
                        scale.setManaged(false);
                        scale.setVisible(false);
                        theme.getChildren().add(scale);
                        for (int unit : new int[] {1, 2, 3, 4, 6, 8}) {
                            scale.getStyleClass().setAll("tb-panel", "tb-gap-" + unit);
                            layout();
                            assertEquals(unit * 4, scale.getSpacing(), 0.01);
                            if (unit != 1) {
                                scale.getStyleClass().add("tb-pad-" + unit);
                                layout();
                                assertEquals(new Insets(unit * 4), scale.getPadding());
                            }
                        }
                        String[] radii = {"sm", "md", "lg"};
                        for (int index = 0; index < radii.length; index++) {
                            scale.getStyleClass().setAll("tb-panel", "tb-radius-" + radii[index]);
                            layout();
                            assertEquals(
                                    8 + 4 * index,
                                    scale.getBackground()
                                            .getFills()
                                            .getFirst()
                                            .getRadii()
                                            .getTopLeftHorizontalRadius(),
                                    0.01);
                        }

                        assertEquals(colors.get("primary"), background(primary));
                        primary.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
                        layout();
                        assertEquals(colors.get("primary-hover"), background(primary));
                        primary.pseudoClassStateChanged(
                                PseudoClass.getPseudoClass("pressed"), true);
                        layout();
                        assertEquals(colors.get("primary-pressed"), background(primary));
                        primary.setDisable(true);
                        layout();
                        assertEquals(colors.get("surface-muted"), background(primary));
                        primary.setDisable(false);
                        primary.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), false);
                        primary.pseudoClassStateChanged(
                                PseudoClass.getPseudoClass("pressed"), false);
                        assertEquals(colors.get("danger"), background(danger));
                        danger.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), true);
                        layout();
                        assertEquals(colors.get("danger-hover"), background(danger));
                        danger.pseudoClassStateChanged(PseudoClass.getPseudoClass("pressed"), true);
                        layout();
                        assertEquals(colors.get("danger-pressed"), background(danger));
                        danger.setDisable(true);
                        layout();
                        assertEquals(colors.get("surface-muted"), background(danger));
                        danger.setDisable(false);
                        danger.pseudoClassStateChanged(PseudoClass.getPseudoClass("hover"), false);
                        danger.pseudoClassStateChanged(
                                PseudoClass.getPseudoClass("pressed"), false);
                        assertEquals(
                                colors.get("danger"),
                                invalid.getBorder().getStrokes().getFirst().getTopStroke());
                        assertEquals(colors.get("surface-muted"), background(readonly));
                        assertEquals(colors.get("surface-muted"), background(disabledInput));
                        readonly.requestFocus();
                        layout();
                        assertEquals(colors.get("focus"), background(readonly));
                        assertEquals(
                                colors.get("surface-muted"),
                                readonly.getBackground().getFills().get(1).getFill());
                        invalid.requestFocus();
                        layout();
                        assertEquals(colors.get("danger"), background(invalid));
                        input.requestFocus();
                        layout();
                        assertTrue(input.isFocused());
                        assertEquals(colors.get("focus"), background(input));
                        assertSame(
                                fieldLabel,
                                input.queryAccessibleAttribute(
                                        javafx.scene.AccessibleAttribute.LABELED_BY));
                        Button apply = secondary;
                        apply.requestFocus();
                        layout();
                        assertEquals(
                                2,
                                apply.getBorder().getStrokes().getFirst().getWidths().getTop(),
                                0.01);
                        var selectedTabLabel =
                                tabs.lookupAll(".tab").stream()
                                        .filter(
                                                n ->
                                                        n.getPseudoClassStates()
                                                                .contains(
                                                                        PseudoClass.getPseudoClass(
                                                                                "selected")))
                                        .findFirst()
                                        .orElseThrow()
                                        .lookup(".tab-label");
                        assertEquals(
                                colors.get("primary"), ((Label) selectedTabLabel).getTextFill());
                        var selectedCell =
                                navigation.lookupAll(".list-cell").stream()
                                        .filter(
                                                n ->
                                                        n.getPseudoClassStates()
                                                                .contains(
                                                                        PseudoClass.getPseudoClass(
                                                                                "selected")))
                                        .findFirst()
                                        .orElseThrow();
                        assertEquals(colors.get("primary"), background((Region) selectedCell));
                        assertEquals(
                                colors.get("on-primary"),
                                ((ListCell<?>) selectedCell).getTextFill());
                        return null;
                    });
            screenshot(
                    "design-system-gallery-"
                            + System.getProperty("glass.win.uiScale", "native")
                            + ".png");
        } finally {
            fx(
                    () -> {
                        if (stage != null) stage.hide();
                        return null;
                    });
            Platform.exit();
        }
    }
}
