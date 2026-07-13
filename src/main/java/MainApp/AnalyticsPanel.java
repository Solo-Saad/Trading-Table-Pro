package MainApp;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Real analytics screen, replacing the old JOptionPane dialog version
 * of analyseTradeOutcomes() that still lives in TradeTablePanel.
 * Reads directly from the trades table -- reflects saved data only,
 * same tradeoff as DashboardPanel (manual Refresh button).
 */
public class AnalyticsPanel extends JPanel {

    private JPanel statsRow;
    private JPanel patternList;
    private EquityCurveChart equityCurve;

    public AnalyticsPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.current().background);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(Theme.current().background);

        JLabel title = new JLabel("Analytics");
        title.setFont(Theme.headerFont());
        title.setForeground(Theme.current().textPrimary);
        headerRow.add(title, BorderLayout.WEST);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadData());
        headerRow.add(refreshButton, BorderLayout.EAST);
        add(headerRow, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Theme.current().background);

        statsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        statsRow.setBackground(Theme.current().background);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        body.add(statsRow);

        body.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel equityLabel = new JLabel("Equity curve (win = +1, loss = -1, break even = 0)");
        equityLabel.setFont(Theme.cellFont());
        equityLabel.setForeground(Theme.current().textSecondary);
        equityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(equityLabel);
        body.add(Box.createRigidArea(new Dimension(0, 6)));

        equityCurve = new EquityCurveChart();
        equityCurve.setAlignmentX(Component.LEFT_ALIGNMENT);
        equityCurve.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        equityCurve.setPreferredSize(new Dimension(600, 160));
        body.add(equityCurve);

        body.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel patternLabel = new JLabel("Win rate by pattern");
        patternLabel.setFont(Theme.headerFont());
        patternLabel.setForeground(Theme.current().textPrimary);
        patternLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(patternLabel);
        body.add(Box.createRigidArea(new Dimension(0, 8)));

        patternList = new JPanel();
        patternList.setLayout(new BoxLayout(patternList, BoxLayout.Y_AXIS));
        patternList.setBackground(Theme.current().background);
        patternList.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(patternList);

        add(body, BorderLayout.CENTER);

        loadData();

        Timer refreshTimer = new Timer(5000, e -> loadData());
        refreshTimer.start();
    }

    public void refreshTheme() {
        setBackground(Theme.current().background);
        loadData();
    }

    private void loadData() {
        statsRow.removeAll();
        patternList.removeAll();

        int wins = 0, losses = 0, breakEvens = 0, bullish = 0, bearish = 0;
        Map<String, int[]> patternStats = new LinkedHashMap<>(); // pattern -> [wins, total]
        List<Integer> equitySeries = new ArrayList<>();

        String sql = "SELECT pattern, direction, outcome FROM trades ORDER BY id ASC";

        try (
                Connection conn = DatabaseManager.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ) {
            int running = 0;
            while (rs.next()) {
                String pattern = rs.getString("pattern");
                String direction = rs.getString("direction");
                String outcome = rs.getString("outcome");

                boolean hasData = (pattern != null && !pattern.isBlank())
                        || (outcome != null && !outcome.isBlank());
                if (!hasData) continue;

                if ("Win".equals(outcome)) { wins++; running += 1; }
                else if ("Loss".equals(outcome)) { losses++; running -= 1; }
                else if ("Break Even".equals(outcome)) { breakEvens++; }

                if ("Bullish".equals(direction)) bullish++;
                else if ("Bearish".equals(direction)) bearish++;

                if (outcome != null && !outcome.isBlank()) {
                    equitySeries.add(running);
                }

                if (pattern != null && !pattern.isBlank() && outcome != null && !outcome.isBlank()) {
                    int[] stat = patternStats.computeIfAbsent(pattern, k -> new int[2]);
                    stat[1]++;
                    if ("Win".equals(outcome)) stat[0]++;
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load analytics data.\n\n" + e.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }

        String winRateStr = (wins + losses > 0)
                ? String.format("%.0f%%", (double) wins / (wins + losses) * 100.0)
                : "--";
        String wlRatioStr = (losses == 0 && wins == 0) ? "--"
                : (losses == 0) ? wins + " : 0"
                  : (wins == 0) ? "0 : " + losses
                    : String.format("%d : %d", wins, losses);
        String biasStr = (bullish + bearish > 0)
                ? String.format("%.0f%% bearish", (double) bearish / (bullish + bearish) * 100.0)
                : "--";

        statsRow.add(statCard("Win rate", winRateStr, Theme.current().success));
        statsRow.add(statCard("Win : loss", wlRatioStr, Theme.current().accent));
        statsRow.add(statCard("Direction bias", biasStr, Theme.current().warning));

        if (patternStats.isEmpty()) {
            JLabel empty = new JLabel("No completed trades to analyse yet.");
            empty.setFont(Theme.cellFont());
            empty.setForeground(Theme.current().textSecondary);
            patternList.add(empty);
        } else {
            patternStats.entrySet().stream()
                    .sorted((a, b) -> Double.compare(
                            (double) b.getValue()[0] / b.getValue()[1],
                            (double) a.getValue()[0] / a.getValue()[1]))
                    .forEach(entry -> patternList.add(
                            patternBar(entry.getKey(), entry.getValue()[0], entry.getValue()[1])));
        }

        equityCurve.setData(equitySeries);

        revalidate();
        repaint();
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

        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 20));
        valueLbl.setForeground(accentColor);
        valueLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(labelLbl);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(valueLbl);
        return card;
    }

    private JPanel patternBar(String pattern, int wins, int total) {
        double rate = (double) wins / total;

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Theme.current().background);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JLabel nameLbl = new JLabel(pattern);
        nameLbl.setFont(Theme.cellFont());
        nameLbl.setForeground(Theme.current().textPrimary);
        nameLbl.setPreferredSize(new Dimension(110, 20));
        row.add(nameLbl, BorderLayout.WEST);

        JPanel barTrack = new JPanel(null);
        barTrack.setBackground(Theme.current().surface);
        barTrack.setBorder(BorderFactory.createLineBorder(Theme.current().border));
        barTrack.setPreferredSize(new Dimension(200, 18));

        JPanel barFill = new JPanel();
        barFill.setBackground(rate >= 0.5 ? Theme.current().success : Theme.current().danger);
        barTrack.add(barFill);
        barTrack.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                barFill.setBounds(0, 0, (int) (barTrack.getWidth() * rate), barTrack.getHeight());
            }
        });
        barFill.setBounds(0, 0, (int) (200 * rate), 18);
        row.add(barTrack, BorderLayout.CENTER);

        JLabel pctLbl = new JLabel(String.format("%.0f%% (%d/%d)", rate * 100, wins, total));
        pctLbl.setFont(Theme.cellFont());
        pctLbl.setForeground(Theme.current().textSecondary);
        row.add(pctLbl, BorderLayout.EAST);

        return row;
    }

    /** Hand-drawn line chart -- no charting library dependency needed for something this simple. */
    private static class EquityCurveChart extends JPanel {
        private List<Integer> data = new ArrayList<>();

        void setData(List<Integer> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBackground(Theme.current().surface);

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int padding = 20;

            if (data.size() < 2) {
                g2.setColor(Theme.current().textSecondary);
                g2.drawString("Not enough completed trades to chart yet.", padding, h / 2);
                return;
            }

            int min = data.stream().min(Integer::compareTo).orElse(0);
            int max = data.stream().max(Integer::compareTo).orElse(0);
            if (min == max) { min -= 1; max += 1; }

            g2.setColor(Theme.current().border);
            g2.drawLine(padding, h - padding, w - padding, h - padding);

            int zeroY = h - padding - (int) (((0.0 - min) / (max - min)) * (h - 2 * padding));
            g2.drawLine(padding, zeroY, w - padding, zeroY);

            g2.setColor(Theme.current().accent);
            g2.setStroke(new BasicStroke(2f));

            int stepX = (w - 2 * padding) / (data.size() - 1);
            int prevX = padding, prevY = 0;
            for (int i = 0; i < data.size(); i++) {
                int x = padding + i * stepX;
                int y = h - padding - (int) (((data.get(i) - min) / (double) (max - min)) * (h - 2 * padding));
                if (i > 0) g2.drawLine(prevX, prevY, x, y);
                prevX = x;
                prevY = y;
            }
        }
    }
}