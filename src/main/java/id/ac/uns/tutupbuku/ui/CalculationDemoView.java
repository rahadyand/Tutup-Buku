package id.ac.uns.tutupbuku.ui;

import id.ac.uns.tutupbuku.domain.*;
import id.ac.uns.tutupbuku.service.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Contoh tetap untuk mencoba engine M1, bukan editor nilai lengkap. */
final class CalculationDemoView extends VBox {
    private final TargetPlanner planner = new TargetPlanner();
    private final GradeCalculator calculator = new GradeCalculator();
    private final TextArea output = new TextArea();
    private final CheckBox attendance = new CheckBox("Syarat kehadiran terpenuhi — asumsi, belum diverifikasi");

    CalculationDemoView() {
        super(12);
        setPadding(new Insets(24));
        Label title = new Label("Contoh perencanaan bertahap");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label explanation = new Label("Tugas 20%, UTS 30%, UAS 50%. Minimal mencapai target C / 60.\nIni satu saran merata, bukan satu-satunya kombinasi. Nama komponen dapat bebas pada versi lengkap.");
        explanation.setWrapText(true);
        attendance.setSelected(true);
        attendance.setId("attendance");
        output.setId("calculation-output");
        Button recalculate = new Button("Jalankan contoh PRD");
        recalculate.setId("run-example");
        recalculate.setOnAction(event -> showExamples());
        attendance.setOnAction(event -> showExamples());
        output.setEditable(false);
        output.setWrapText(true);
        output.setStyle("-fx-font-family: Consolas; -fx-font-size: 14px;");
        VBox.setVgrow(output, Priority.ALWAYS);
        getChildren().addAll(title, explanation, attendance, recalculate, output);
        showExamples();
    }

    private void showExamples() {
        Attendance attendanceState = new Attendance(attendance.isSelected(), false);
        List<AssessmentItem> free = List.of(item("task", "Tugas", "20"), item("midterm", "UTS", "30"), item("final", "UAS", "50"));
        ScenarioAssumptions locked = new ScenarioAssumptions(Map.of("task", number("80")));
        TargetPlan first = planner.plan(free, ScenarioAssumptions.none(), number("60"), attendanceState);
        TargetPlan second = planner.plan(free, locked, number("60"), attendanceState);
        List<AssessmentItem> withActual = List.of(free.get(0), actual("midterm", "UTS", "30", "30"), free.get(2));
        TargetPlan third = planner.plan(withActual, locked, number("60"), attendanceState);
        GradeResult complete = calculator.weighted(List.of(actual("task", "Tugas", "20", "80"),
                actual("midterm", "UTS", "30", "30"), actual("final", "UAS", "50", "70")), ScenarioAssumptions.none(), attendanceState);
        String text = "1. Semua bebas → " + suggestions(free, first)
                + "\n2. Tugas dikunci 80 → " + suggestions(free, second)
                + "\n3. Tugas tetap 80; UTS aktual 30 → " + suggestions(withActual, third)
                + "\n\nContoh hasil lengkap: aktual 80, 30, 70\nNilai asal: " + display(complete.underlyingScore().orElseThrow())
                + " / " + complete.underlyingLetter().orElseThrow().label()
                + "\nHuruf efektif: " + complete.effectiveLetter().orElseThrow().label()
                + "\nStatus nilai: " + (complete.effectiveLetter().orElseThrow().isPassing() ? "Lulus" : "Tidak lulus")
                + "\nKehadiran masih asumsi; hasil bukan transkrip resmi.";
        if (!attendanceState.met()) text += "\n\nKehadiran tidak terpenuhi: hasil efektif E. Target akademik saja tidak memenuhi kelulusan. Nilai asal tetap tersimpan dalam hasil perhitungan.";
        output.setText(text);
    }

    private String suggestions(List<AssessmentItem> items, TargetPlan plan) {
        return items.stream().filter(item -> plan.suggestedScores().containsKey(item.id()))
                .map(item -> item.name() + " " + display(plan.suggestedScores().get(item.id())))
                .collect(Collectors.joining(", "));
    }

    private static AssessmentItem item(String id, String name, String weight) {
        return AssessmentItem.free(id, name, number(weight));
    }

    private static AssessmentItem actual(String id, String name, String weight, String score) {
        return new AssessmentItem(id, name, number(weight), Optional.of(new Score(number(score), number("100"))));
    }

    private static BigDecimal number(String value) { return new BigDecimal(value); }
    private static String display(BigDecimal value) { return value.stripTrailingZeros().toPlainString().replace('.', ','); }
}
