package MainApp;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Landing screen. Reads directly from the trades table (same data
 * TradeTablePanel saves to) and shows summary stats + recent trades.
 * Only reflects SAVED data -- unsaved edits on the Trades screen
 * won't appear here until the user saves, then hits Refresh.
 */
public class DashboardPanel extends JPanel {

    private JPanel statsGrid;
    private JPanel recentList;
    private JLabel emptyLabel;

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.current().background);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(Theme.current().background);

        JLabel welcome = new JLabel("Welcome back, Ismail");
        welcome.setFont(Theme.subtitleFont());
        welcome.setForeground(Theme.current().textSecondary);
        headerRow.add(welcome, BorderLayout.WEST);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(Theme.cellFont());
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> loadStats());
        headerRow.add(refreshButton, BorderLayout.EAST);

        add(headerRow, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Theme.current().background);

        statsGrid = new JPanel(new GridLayout(1, 5, 12, 0));
        statsGrid.setBackground(Theme.current().background);
        statsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        body.add(statsGrid);

        body.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel recentTitle = new JLabel("Recent trades");
        recentTitle.setFont(Theme.headerFont());
        recentTitle.setForeground(Theme.current().textPrimary);
        recentTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(recentTitle);

        body.add(Box.createRigidArea(new Dimension(0, 8)));

        recentList = new JPanel();
        recentList.setLayout(new BoxLayout(recentList, BoxLayout.Y_AXIS));
        recentList.setBackground(Theme.current().surface);
        recentList.setAlignmentX(Component.LEFT_ALIGNMENT);
        recentList.setBorder(BorderFactory.createLineBorder(Theme.current().border));
        body.add(recentList);

        emptyLabel = new JLabel("No trades logged yet. Add some on the Trades screen.");
        emptyLabel.setFont(Theme.cellFont());
        emptyLabel.setForeground(Theme.current().textSecondary);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 4, 20, 4));

        add(body, BorderLayout.CENTER);

        loadStats();

        Timer refreshTimer = new Timer(5000, e -> loadStats());
        refreshTimer.start();
    }

    public void refreshTheme() {
        setBackground(Theme.current().background);
        recentList.setBackground(Theme.current().surface);
        recentList.setBorder(BorderFactory.createLineBorder(Theme.current().border));
        loadStats();
    }

    private void loadStats() {
        statsGrid.removeAll();
        recentList.removeAll();

        int wins = 0, losses = 0, breakEvens = 0, bearish = 0, total = 0;
        Map<String, Integer> patternWins = new HashMap<>();
        Map<String, Integer> patternTotals = new HashMap<>();
        java.util.List<String[]> recentRows = new java.util.ArrayList<>();

        String sql = "SELECT pair, pattern, direction, outcome FROM trades ORDER BY id DESC";

        try (
                Connection conn = DatabaseManager.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ) {
            while (rs.next()) {
                String pair = rs.getString("pair");
                String pattern = rs.getString("pattern");
                String direction = rs.getString("direction");
                String outcome = rs.getString("outcome");

                boolean hasData = (pair != null && !pair.isBlank())
                        || (pattern != null && !pattern.isBlank())
                        || (outcome != null && !outcome.isBlank());
                if (!hasData) continue;

                total++;
                if ("Win".equals(outcome)) wins++;
                else if ("Loss".equals(outcome)) losses++;
                else if ("Break Even".equals(outcome)) breakEvens++;
                if ("Bearish".equals(direction)) bearish++;

                if (pattern != null && !pattern.isBlank() && outcome != null && !outcome.isBlank()) {
                    patternTotals.merge(pattern, 1, Integer::sum);
                    if ("Win".equals(outcome)) patternWins.merge(pattern, 1, Integer::sum);
                }

                if (recentRows.size() < 5) {
                    recentRows.add(new String[]{pair, pattern, outcome});
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load dashboard data.\n\n" + e.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }

        String winRateStr = (wins + losses > 0)
                ? String.format("%.0f%%", (double) wins / (wins + losses) * 100.0)
                : "--";

        String bestPattern = "--";
        double bestRate = -1;
        for (String pattern : patternTotals.keySet()) {
            int t = patternTotals.get(pattern);
            int w = patternWins.getOrDefault(pattern, 0);
            double rate = (double) w / t;
            if (rate > bestRate) {
                bestRate = rate;
                bestPattern = pattern;
            }
        }

        String bearishBiasStr = (total > 0)
                ? String.format("%.0f%%", (double) bearish / total * 100.0)
                : "--";

        statsGrid.add(statCard("Win rate", winRateStr, Theme.current().success));
        statsGrid.add(statCard("Total trades", String.valueOf(total), Theme.current().accent));
        statsGrid.add(statCard("Best pattern", bestPattern, Theme.current().textPrimary));
        statsGrid.add(statCard("Bearish bias", bearishBiasStr, Theme.current().warning));
        statsGrid.add(statCard("Risk per trade", riskPerTradeStr(), Theme.current().danger));

        if (recentRows.isEmpty()) {
            recentList.add(emptyLabel);
        } else {
            for (String[] row : recentRows) {
                recentList.add(recentRow(row[0], row[1], row[2]));
            }
        }

        revalidate();
        repaint();
    }

    private String riskPerTradeStr() {
        try (Connection conn = DatabaseManager.connect()) {
            double balance = Double.parseDouble(DatabaseManager.getSetting(conn, "account_balance", "1000"));
            double risk = Double.parseDouble(DatabaseManager.getSetting(conn, "risk_percent", "1.0"));
            return String.format("$%.2f", balance * (risk / 100.0));
        } catch (Exception e) {
            return "--";
        }
    }

    private JPanel statCard(String label, String value, Color accentColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.current().surface);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.current().border),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JLabel labelLbl = new JLabel(label);
        labelLbl.setFont(Theme.cellFont());
        labelLbl.setForeground(Theme.current().textSecondary);
        labelLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueLbl = new JLabel(value == null || value.isBlank() ? "--" : value);
        valueLbl.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 22));
        valueLbl.setForeground(accentColor);
        valueLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(labelLbl);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(valueLbl);

        return card;
    }

    private JPanel recentRow(String pair, String pattern, String outcome) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Theme.current().surface);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.current().border),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        String left = (pair == null || pair.isBlank() ? "--" : pair)
                + "   ·   " + (pattern == null || pattern.isBlank() ? "--" : pattern);
        JLabel leftLbl = new JLabel(left);
        leftLbl.setFont(Theme.cellFont());
        leftLbl.setForeground(Theme.current().textPrimary);
        row.add(leftLbl, BorderLayout.WEST);

        Color outcomeColor = switch (outcome == null ? "" : outcome) {
            case "Win" -> Theme.current().success;
            case "Loss" -> Theme.current().danger;
            case "Break Even" -> Theme.current().warning;
            default -> Theme.current().textSecondary;
        };
        JLabel outcomeLbl = new JLabel(outcome == null || outcome.isBlank() ? "--" : outcome);
        outcomeLbl.setFont(Theme.buttonFont());
        outcomeLbl.setForeground(outcomeColor);
        row.add(outcomeLbl, BorderLayout.EAST);

        return row;
    }
}