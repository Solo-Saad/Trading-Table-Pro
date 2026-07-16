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
 * Real analytics screen. Every section now lives inside a RoundedPanel
 * card, matching Dashboard/Trades. No manual Refresh button -- the
 * screen already auto-refreshes every 5 seconds. New: win rate by
 * S/R (Fibonacci) level, since which level a pattern completes at is
 * as core to the trade thesis as the pattern itself for harmonic
 * trading -- and Dashboard already covers win rate / best pattern, so
 * this adds something genuinely new rather than repeating it.
 */
public class AnalyticsPanel extends JPanel {

    private JPanel statsRow;
    private JPanel patternList;
    private JPanel srList;
    private EquityCurveChart equityCurve;

    public AnalyticsPanel() {
        build();

        Timer refreshTimer = new Timer(5000, e -> loadData());
        refreshTimer.start();
    }

    private void build() {
        removeAll();
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.current().background);

        JLabel title = new JLabel("Analytics");
        title.setFont(Theme.headerFont());
        title.setForeground(Theme.current().textPrimary);
        add(title, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        statsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        body.add(statsRow);

        body.add(Box.createRigidArea(new Dimension(0, 20)));

        RoundedPanel equityCard = new RoundedPanel(new BorderLayout(0, 8));
        equityCard.cornerRadius(16).showBorder(true).showShadow(true);
        equityCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        equityCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        equityCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        JLabel equityLabel = new JLabel("Equity curve (win = +1, loss = -1, break even = 0)");
        equityLabel.setFont(Theme.cellFont());
        equityLabel.setForeground(Theme.current().textSecondary);
        equityCard.add(equityLabel, BorderLayout.NORTH);

        equityCurve = new EquityCurveChart();
        equityCurve.setPreferredSize(new Dimension(600, 160));
        equityCard.add(equityCurve, BorderLayout.CENTER);

        body.add(equityCard);
        body.add(Box.createRigidArea(new Dimension(0, 20)));

        RoundedPanel patternCard = new RoundedPanel(new BorderLayout(0, 8));
        patternCard.cornerRadius(16).showBorder(true).showShadow(true);
        patternCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        patternCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel patternLabel = new JLabel("Win rate by pattern");
        patternLabel.setFont(Theme.headerFont());
        patternLabel.setForeground(Theme.current().textPrimary);
        patternCard.add(patternLabel, BorderLayout.NORTH);

        patternList = new JPanel();
        patternList.setLayout(new BoxLayout(patternList, BoxLayout.Y_AXIS));
        patternList.setOpaque(false);
        patternCard.add(patternList, BorderLayout.CENTER);

        body.add(patternCard);
        body.add(Box.createRigidArea(new Dimension(0, 20)));

        RoundedPanel srCard = new RoundedPanel(new BorderLayout(0, 8));
        srCard.cornerRadius(16).showBorder(true).showShadow(true);
        srCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        srCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel srLabel = new JLabel("Win rate by S/R level");
        srLabel.setFont(Theme.headerFont());
        srLabel.setForeground(Theme.current().textPrimary);
        srCard.add(srLabel, BorderLayout.NORTH);

        srList = new JPanel();
        srList.setLayout(new BoxLayout(srList, BoxLayout.Y_AXIS));
        srList.setOpaque(false);
        srCard.add(srList, BorderLayout.CENTER);

        body.add(srCard);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        loadData();
    }

    public void refreshTheme() {
        build();
        revalidate();
        repaint();
    }

    private void loadData() {
        statsRow.removeAll();
        patternList.removeAll();
        srList.removeAll();

        int wins = 0, losses = 0, breakEvens = 0, bullish = 0, bearish = 0;
        Map<String, int[]> patternStats = new LinkedHashMap<>(); // pattern -> [wins, total]
        Map<String, int[]> srStats = new LinkedHashMap<>();      // S/R level -> [wins, total]
        List<Integer> equitySeries = new ArrayList<>();

        String sql = "SELECT pattern, direction, outcome, sr FROM trades ORDER BY id ASC";

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
                String sr = rs.getString("sr");

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

                if (sr != null && !sr.isBlank() && outcome != null && !outcome.isBlank()) {
                    int[] stat = srStats.computeIfAbsent(sr, k -> new int[2]);
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
                            winRateBar(entry.getKey(), entry.getValue()[0], entry.getValue()[1])));
        }

        if (srStats.isEmpty()) {
            JLabel empty = new JLabel("No completed trades with an S/R level logged yet.");
            empty.setFont(Theme.cellFont());
            empty.setForeground(Theme.current().textSecondary);
            srList.add(empty);
        } else {
            srStats.entrySet().stream()
                    .sorted((a, b) -> Double.compare(
                            (double) b.getValue()[0] / b.getValue()[1],
                            (double) a.getValue()[0] / a.getValue()[1]))
                    .forEach(entry -> srList.add(
                            winRateBar(entry.getKey(), entry.getValue()[0], entry.getValue()[1])));
        }

        equityCurve.setData(equitySeries);

        revalidate();
        repaint();
    }

    private JPanel statCard(String label, String value, Color accentColor) {
        RoundedPanel card = new RoundedPanel();
        card.cornerRadius(14).showBorder(true).showShadow(true);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

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

    /** Shared row style for both pattern and S/R breakdowns -- a label,
     *  a rounded progress bar, and the win rate / count. */
    private JPanel winRateBar(String label, int wins, int total) {
        double rate = (double) wins / total;

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JLabel nameLbl = new JLabel(label);
        nameLbl.setFont(Theme.cellFont());
        nameLbl.setForeground(Theme.current().textPrimary);
        nameLbl.setPreferredSize(new Dimension(110, 20));
        row.add(nameLbl, BorderLayout.WEST);

        RoundedPanel barTrack = new RoundedPanel(null);
        barTrack.cornerRadius(9).showBorder(true).showShadow(false);
        barTrack.setPreferredSize(new Dimension(200, 18));

        RoundedPanel barFill = new RoundedPanel();
        barFill.cornerRadius(9).showBorder(false).showShadow(false);
        barFill.fixedBackground(rate >= 0.5 ? Theme.current().success : Theme.current().danger);
        barTrack.add(barFill);
        barTrack.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = Math.max((int) (barTrack.getWidth() * rate), rate > 0 ? 18 : 0);
                barFill.setBounds(0, 0, w, barTrack.getHeight());
            }
        });
        barFill.setBounds(0, 0, Math.max((int) (200 * rate), rate > 0 ? 18 : 0), 18);
        row.add(barTrack, BorderLayout.CENTER);

        JLabel pctLbl = new JLabel(String.format("%.0f%% (%d/%d)", rate * 100, wins, total));
        pctLbl.setFont(Theme.cellFont());
        pctLbl.setForeground(Theme.current().textSecondary);
        row.add(pctLbl, BorderLayout.EAST);

        return row;
    }
}