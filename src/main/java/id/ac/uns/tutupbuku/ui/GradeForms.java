package id.ac.uns.tutupbuku.ui;

import id.ac.uns.tutupbuku.domain.*;

import javafx.scene.Node;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Consumer;

/** Bantuan presentasi saja: tidak berisi rumus nilai. */
final class GradeForms {
    private GradeForms() {}

    static String number(BigDecimal n) {
        return n.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    static String approximation(BigDecimal n) {
        return n.stripTrailingZeros().scale() > 6
                ? "≈" + number(n.setScale(6, java.math.RoundingMode.HALF_UP))
                : number(n);
    }

    static String letter(Optional<LetterGrade> grade) {
        return grade.map(LetterGrade::label).orElse("Belum lengkap");
    }

    static String result(GradeResult grade) {
        return "Asal: "
                + grade.underlyingScore().map(GradeForms::number).orElse("—")
                + " / "
                + letter(grade.underlyingLetter())
                + " · Efektif: "
                + letter(grade.effectiveLetter())
                + " · "
                + (grade.estimated() ? "Simulasi/asumsi" : "Input aktual")
                + " · Status: "
                + grade.effectiveLetter()
                        .map(g -> g.isPassing() ? "Lulus" : "Tidak lulus")
                        .orElse("Belum lengkap")
                + (grade.issues().isEmpty() ? "" : "\n" + String.join("\n", grade.issues()));
    }

    static ComboBox<LetterGrade> letters(String id) {
        ComboBox<LetterGrade> box = new ComboBox<>();
        box.setId(id);
        box.getItems().addAll(LetterGrade.values());
        box.setPromptText("Belum ada huruf");
        box.setConverter(
                new javafx.util.StringConverter<>() {
                    @Override
                    public String toString(LetterGrade value) {
                        return value == null ? "" : value.label();
                    }

                    @Override
                    public LetterGrade fromString(String value) {
                        return java.util.Arrays.stream(LetterGrade.values())
                                .filter(g -> g.label().equals(value))
                                .findFirst()
                                .orElse(null);
                    }
                });
        return box;
    }

    static void guard(
            Node owner, boolean dirty, Runnable discard, Consumer<Runnable> save, Runnable action) {
        if (!dirty) {
            action.run();
            return;
        }
        ButtonType keep = new ButtonType("Simpan", ButtonBar.ButtonData.YES),
                drop = new ButtonType("Buang perubahan", ButtonBar.ButtonData.NO),
                cancel = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert dialog =
                new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Ada perubahan formulir yang belum disimpan.",
                        keep,
                        drop,
                        cancel);
        dialog.initOwner(owner.getScene().getWindow());
        dialog.setTitle("Perubahan belum disimpan");
        dialog.setHeaderText("Simpan sebelum berpindah?");
        ((Button) dialog.getDialogPane().lookupButton(keep)).setDefaultButton(false);
        ((Button) dialog.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        dialog.showAndWait()
                .ifPresent(
                        choice -> {
                            if (choice == keep) save.accept(action);
                            else if (choice == drop) {
                                discard.run();
                                action.run();
                            }
                        });
    }

    static boolean confirmMode(Node owner, EvaluationMode mode) {
        ButtonType apply = new ButtonType("Gunakan mode ini", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert dialog =
                new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Hasil yang dipakai untuk nilai/IP: "
                                + mode
                                + ". Data mode lainnya tetap disimpan tetapi tidak ikut dihitung.",
                        apply,
                        cancel);
        dialog.initOwner(owner.getScene().getWindow());
        dialog.setTitle("Ganti mode penilaian");
        dialog.setHeaderText("Gunakan " + mode + "?");
        return dialog.showAndWait().filter(apply::equals).isPresent();
    }

    static String comparisonGrade(GradeResult grade) {
        return letter(grade.effectiveLetter()) + " · " + (grade.estimated() ? "Asumsi" : "Aktual");
    }
}
