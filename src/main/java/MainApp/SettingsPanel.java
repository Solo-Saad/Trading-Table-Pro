package MainApp;

import javax.swing.*;
import java.awt.*;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;

/**
 * Redesigned as a list-style settings screen: grouped sections, each
 * holding rows of "label + description" on the left and a control on
 * the right, separated by thin dividers -- instead of the old boxy
 * bordered cards.
 */
public class SettingsPanel extends JPanel {

    private final Consumer<String> onProfileSaved;
    private JTextField nameField;

    public SettingsPanel(Consumer<String> onProfileSaved) {
        this.onProfileSaved = onProfileSaved;
        build();
    }

    private void build() {
        removeAll();
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.current().background);

        JLabel title = new JLabel("Settings");
        title.setFont(Theme.headerFont());
        title.setForeground(Theme.current().textPrimary);
        add(title, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Theme.current().background);

        body.add(sectionGroup("Profile", profileRows()));
        body.add(Box.createRigidArea(new Dimension(0, 20)));
        body.add(sectionGroup("Appearance", appearanceRows()));
        body.add(Box.createRigidArea(new Dimension(0, 20)));
        body.add(sectionGroup("Trading defaults", tradingDefaultRows()));
        body.add(Box.createRigidArea(new Dimension(0, 20)));
        body.add(sectionGroup("Data & export", dataRows()));

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    public void refreshTheme() {
        build();
    }

    // ─── Section / row builders ─────────────────────────────────────

    private JPanel sectionGroup(String heading, java.util.List<JPanel> rows) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(Theme.current().background);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel headingLbl = new JLabel(heading.toUpperCase());
        headingLbl.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 11));
        headingLbl.setForeground(Theme.current().textSecondary);
        headingLbl.setBorder(BorderFactory.createEmptyBorder(0, 4, 8, 0));
        headingLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(headingLbl);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.current().surface);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createLineBorder(Theme.current().border));

        for (int i = 0; i < rows.size(); i++) {
            JPanel row = rows.get(i);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(row);
            if (i < rows.size() - 1) {
                JSeparator sep = new JSeparator();
                sep.setForeground(Theme.current().border);
                card.add(sep);
            }
        }

        wrapper.add(card);
        return wrapper;
    }

    private JPanel settingRow(String label, String description, JComponent control) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setBackground(Theme.current().surface);
        row.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setBackground(Theme.current().surface);

        JLabel labelLbl = new JLabel(label);
        labelLbl.setFont(Theme.cellFont());
        labelLbl.setForeground(Theme.current().textPrimary);
        labelLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        textCol.add(labelLbl);

        if (description != null && !description.isBlank()) {
            JLabel descLbl = new JLabel(description);
            descLbl.setFont(new Font(Theme.FONT_FAMILY, Font.PLAIN, 11));
            descLbl.setForeground(Theme.current().textSecondary);
            descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            textCol.add(descLbl);
        }

        row.add(textCol, BorderLayout.WEST);

        JPanel controlWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controlWrap.setBackground(Theme.current().surface);
        controlWrap.add(control);
        row.add(controlWrap, BorderLayout.EAST);

        return row;
    }

    // ─── Profile ─────────────────────────────────────────────────────

    private java.util.List<JPanel> profileRows() {
        String savedName = "";
        try (Connection conn = DatabaseManager.connect()) {
            savedName = DatabaseManager.getSetting(conn, "profile_name", "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        nameField = new JTextField(savedName, 16);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            try (Connection conn = DatabaseManager.connect()) {
                DatabaseManager.setSetting(conn, "profile_name", name);
                onProfileSaved.accept(name);
                JOptionPane.showMessageDialog(this, "Profile updated.",
                        "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to save profile.\n\n" + ex.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel nameControl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        nameControl.setBackground(Theme.current().surface);
        nameControl.add(nameField);
        nameControl.add(saveButton);

        java.util.List<JPanel> rows = new java.util.ArrayList<>();
        rows.add(settingRow("Display name", "Shown in the top bar avatar", nameControl));
        return rows;
    }

    // ─── Appearance ──────────────────────────────────────────────────

    private java.util.List<JPanel> appearanceRows() {
        JButton toggle = new JButton(Theme.getMode() == Theme.Mode.DARK ? "Switch to light" : "Switch to dark");
        toggle.addActionListener(e -> Theme.toggle());

        java.util.List<JPanel> rows = new java.util.ArrayList<>();
        rows.add(settingRow("Theme", "Currently " + (Theme.getMode() == Theme.Mode.DARK ? "dark" : "light") + " mode", toggle));
        return rows;
    }

    // ─── Trading defaults ────────────────────────────────────────────

    private java.util.List<JPanel> tradingDefaultRows() {
        String savedRisk = "1.0";
        String savedBalance = "1000";
        try (Connection conn = DatabaseManager.connect()) {
            savedRisk = DatabaseManager.getSetting(conn, "risk_percent", "1.0");
            savedBalance = DatabaseManager.getSetting(conn, "account_balance", "1000");
        } catch (Exception e) {
            e.printStackTrace();
        }

        JTextField balanceField = new JTextField(savedBalance, 8);
        JTextField riskField = new JTextField(savedRisk, 5);
        JLabel computedLabel = new JLabel();
        computedLabel.setFont(Theme.buttonFont());
        computedLabel.setForeground(Theme.current().accent);

        Runnable recompute = () -> {
            try {
                double balance = Double.parseDouble(balanceField.getText().trim());
                double risk = Double.parseDouble(riskField.getText().trim());
                double amount = balance * (risk / 100.0);
                computedLabel.setText(String.format("= $%.2f per trade", amount));
            } catch (NumberFormatException ex) {
                computedLabel.setText("Enter valid numbers");
            }
        };
        recompute.run();

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            recompute.run();
            try (Connection conn = DatabaseManager.connect()) {
                DatabaseManager.setSetting(conn, "risk_percent", riskField.getText().trim());
                DatabaseManager.setSetting(conn, "account_balance", balanceField.getText().trim());
                JOptionPane.showMessageDialog(this, "Trading defaults updated.",
                        "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to save trading defaults.\n\n" + ex.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel balanceControl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        balanceControl.setBackground(Theme.current().surface);
        balanceControl.add(new JLabel("$"));
        balanceControl.add(balanceField);

        JPanel riskControl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        riskControl.setBackground(Theme.current().surface);
        riskControl.add(riskField);
        riskControl.add(new JLabel("%"));
        riskControl.add(computedLabel);
        riskControl.add(saveButton);

        java.util.List<JPanel> rows = new java.util.ArrayList<>();
        rows.add(settingRow("Account balance", "Used to calculate dollar risk per trade", balanceControl));
        rows.add(settingRow("Default risk per trade", "Shown on the Dashboard", riskControl));
        return rows;
    }

    // ─── Data & export ───────────────────────────────────────────────

    private java.util.List<JPanel> dataRows() {
        JButton csvButton = new JButton("Export CSV");
        csvButton.addActionListener(e -> exportCsv());

        JButton wordButton = new JButton("Export Word report");
        wordButton.addActionListener(e -> exportWord());

        JButton excelButton = new JButton("Export Excel workbook");
        excelButton.addActionListener(e -> exportExcel());

        java.util.List<JPanel> rows = new java.util.ArrayList<>();
        rows.add(settingRow("Raw data", "All trades as a spreadsheet-ready CSV", csvButton));
        rows.add(settingRow("Summary report", "Formatted Word document with stats + trade log", wordButton));
        rows.add(settingRow("Full workbook", "Excel file with a data sheet and a summary sheet", excelButton));
        return rows;
    }

    // ─── Export implementations ──────────────────────────────────────

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("trades_export.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        String sql = "SELECT pair, pattern, wave, diversion, sr, direction, entrySignal, outcome, trade_date FROM trades ORDER BY id ASC";

        try (
                Connection conn = DatabaseManager.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                FileWriter writer = new FileWriter(file)
        ) {
            writer.append("Pair,Pattern,Wave,Diversion,S/R,Direction,Entry signal,Outcome,Date\n");
            int rowsWritten = 0;
            while (rs.next()) {
                String pair = nullSafe(rs.getString("pair"));
                if (pair.isEmpty() && nullSafe(rs.getString("outcome")).isEmpty()
                        && nullSafe(rs.getString("pattern")).isEmpty()) continue;

                writer.append(csvField(pair)).append(",");
                writer.append(csvField(nullSafe(rs.getString("pattern")))).append(",");
                writer.append(csvField(nullSafe(rs.getString("wave")))).append(",");
                writer.append(csvField(nullSafe(rs.getString("diversion")))).append(",");
                writer.append(csvField(nullSafe(rs.getString("sr")))).append(",");
                writer.append(csvField(nullSafe(rs.getString("direction")))).append(",");
                writer.append(csvField(nullSafe(rs.getString("entrySignal")))).append(",");
                writer.append(csvField(nullSafe(rs.getString("outcome")))).append(",");
                writer.append(csvField(nullSafe(rs.getString("trade_date")))).append("\n");
                rowsWritten++;
            }
            JOptionPane.showMessageDialog(this,
                    "Exported " + rowsWritten + " trades to:\n" + file.getAbsolutePath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Export failed.\n\n" + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportWord() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("trade_report.docx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        try (
                Connection conn = DatabaseManager.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT pair, pattern, wave, diversion, sr, direction, entrySignal, outcome, trade_date "
                                + "FROM trades ORDER BY id ASC");
                XWPFDocument doc = new XWPFDocument();
                FileOutputStream out = new FileOutputStream(file)
        ) {
            XWPFParagraph titlePara = doc.createParagraph();
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("Trading Analysis Report");
            titleRun.setBold(true);
            titleRun.setFontSize(20);

            XWPFParagraph datePara = doc.createParagraph();
            datePara.createRun().setText("Generated " + java.time.LocalDate.now());

            doc.createParagraph();

            java.util.List<String[]> allRows = new java.util.ArrayList<>();
            int wins = 0, losses = 0, breakEvens = 0;
            Map<String, int[]> patternStats = new LinkedHashMap<>();

            while (rs.next()) {
                String pair = nullSafe(rs.getString("pair"));
                String pattern = nullSafe(rs.getString("pattern"));
                String outcome = nullSafe(rs.getString("outcome"));
                if (pair.isEmpty() && pattern.isEmpty() && outcome.isEmpty()) continue;

                allRows.add(new String[]{
                        pair, pattern, nullSafe(rs.getString("wave")), nullSafe(rs.getString("diversion")),
                        nullSafe(rs.getString("sr")), nullSafe(rs.getString("direction")),
                        nullSafe(rs.getString("entrySignal")), outcome, nullSafe(rs.getString("trade_date"))
                });

                if ("Win".equals(outcome)) wins++;
                else if ("Loss".equals(outcome)) losses++;
                else if ("Break Even".equals(outcome)) breakEvens++;

                if (!pattern.isEmpty() && !outcome.isEmpty()) {
                    int[] stat = patternStats.computeIfAbsent(pattern, k -> new int[2]);
                    stat[1]++;
                    if ("Win".equals(outcome)) stat[0]++;
                }
            }

            XWPFParagraph summaryHeading = doc.createParagraph();
            XWPFRun summaryRun = summaryHeading.createRun();
            summaryRun.setText("Summary");
            summaryRun.setBold(true);
            summaryRun.setFontSize(14);

            String winRate = (wins + losses > 0) ? String.format("%.1f%%", (double) wins / (wins + losses) * 100) : "N/A";
            addBodyLine(doc, "Total trades: " + allRows.size());
            addBodyLine(doc, "Wins: " + wins + "   Losses: " + losses + "   Break evens: " + breakEvens);
            addBodyLine(doc, "Win rate: " + winRate);

            doc.createParagraph();
            XWPFParagraph patternHeading = doc.createParagraph();
            XWPFRun patternRun = patternHeading.createRun();
            patternRun.setText("Win rate by pattern");
            patternRun.setBold(true);
            patternRun.setFontSize(14);

            for (Map.Entry<String, int[]> entry : patternStats.entrySet()) {
                int w = entry.getValue()[0], t = entry.getValue()[1];
                addBodyLine(doc, entry.getKey() + ": " + w + "/" + t
                        + String.format(" (%.0f%%)", (double) w / t * 100));
            }

            doc.createParagraph();
            XWPFParagraph tableHeading = doc.createParagraph();
            XWPFRun tableHeadingRun = tableHeading.createRun();
            tableHeadingRun.setText("Trade log");
            tableHeadingRun.setBold(true);
            tableHeadingRun.setFontSize(14);

            String[] headers = {"Pair", "Pattern", "Wave", "Diversion", "S/R", "Direction", "Signal", "Outcome", "Date"};
            XWPFTable table = doc.createTable(allRows.size() + 1, headers.length);
            for (int c = 0; c < headers.length; c++) {
                table.getRow(0).getCell(c).setText(headers[c]);
            }
            for (int r = 0; r < allRows.size(); r++) {
                String[] row = allRows.get(r);
                for (int c = 0; c < row.length; c++) {
                    table.getRow(r + 1).getCell(c).setText(row[c]);
                }
            }

            doc.write(out);
            JOptionPane.showMessageDialog(this,
                    "Report saved to:\n" + file.getAbsolutePath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Word export failed.\n\n" + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addBodyLine(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.createRun().setText(text);
    }

    private void exportExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("trade_workbook.xlsx"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        try (
                Connection conn = DatabaseManager.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT pair, pattern, wave, diversion, sr, direction, entrySignal, outcome, trade_date "
                                + "FROM trades ORDER BY id ASC");
                XSSFWorkbook workbook = new XSSFWorkbook();
                FileOutputStream out = new FileOutputStream(file)
        ) {
            Sheet tradesSheet = workbook.createSheet("Trades");
            String[] headers = {"Pair", "Pattern", "Wave", "Diversion", "S/R", "Direction", "Entry signal", "Outcome", "Date"};
            Row headerRow = tradesSheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                headerRow.createCell(c).setCellValue(headers[c]);
            }

            int wins = 0, losses = 0, breakEvens = 0, rowIndex = 1;
            Map<String, int[]> patternStats = new LinkedHashMap<>();

            while (rs.next()) {
                String pair = nullSafe(rs.getString("pair"));
                String pattern = nullSafe(rs.getString("pattern"));
                String outcome = nullSafe(rs.getString("outcome"));
                if (pair.isEmpty() && pattern.isEmpty() && outcome.isEmpty()) continue;

                Row row = tradesSheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(pair);
                row.createCell(1).setCellValue(pattern);
                row.createCell(2).setCellValue(nullSafe(rs.getString("wave")));
                row.createCell(3).setCellValue(nullSafe(rs.getString("diversion")));
                row.createCell(4).setCellValue(nullSafe(rs.getString("sr")));
                row.createCell(5).setCellValue(nullSafe(rs.getString("direction")));
                row.createCell(6).setCellValue(nullSafe(rs.getString("entrySignal")));
                row.createCell(7).setCellValue(outcome);
                row.createCell(8).setCellValue(nullSafe(rs.getString("trade_date")));

                if ("Win".equals(outcome)) wins++;
                else if ("Loss".equals(outcome)) losses++;
                else if ("Break Even".equals(outcome)) breakEvens++;

                if (!pattern.isEmpty() && !outcome.isEmpty()) {
                    int[] stat = patternStats.computeIfAbsent(pattern, k -> new int[2]);
                    stat[1]++;
                    if ("Win".equals(outcome)) stat[0]++;
                }
            }
            for (int c = 0; c < headers.length; c++) tradesSheet.autoSizeColumn(c);

            Sheet summarySheet = workbook.createSheet("Summary");
            int sr = 0;
            sr = writeSummaryRow(summarySheet, sr, "Total trades", String.valueOf(rowIndex - 1));
            sr = writeSummaryRow(summarySheet, sr, "Wins", String.valueOf(wins));
            sr = writeSummaryRow(summarySheet, sr, "Losses", String.valueOf(losses));
            sr = writeSummaryRow(summarySheet, sr, "Break evens", String.valueOf(breakEvens));
            String winRate = (wins + losses > 0) ? String.format("%.1f%%", (double) wins / (wins + losses) * 100) : "N/A";
            sr = writeSummaryRow(summarySheet, sr, "Win rate", winRate);
            sr++;

            Row patternHeader = summarySheet.createRow(sr++);
            patternHeader.createCell(0).setCellValue("Pattern");
            patternHeader.createCell(1).setCellValue("Win rate");
            for (Map.Entry<String, int[]> entry : patternStats.entrySet()) {
                Row row = summarySheet.createRow(sr++);
                row.createCell(0).setCellValue(entry.getKey());
                int w = entry.getValue()[0], t = entry.getValue()[1];
                row.createCell(1).setCellValue(String.format("%.0f%% (%d/%d)", (double) w / t * 100, w, t));
            }
            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);

            workbook.write(out);
            JOptionPane.showMessageDialog(this,
                    "Workbook saved to:\n" + file.getAbsolutePath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Excel export failed.\n\n" + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int writeSummaryRow(Sheet sheet, int rowIndex, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        return rowIndex + 1;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String csvField(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}