package MainApp;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Second Dashboard pass, built with the person's own priority order:
 * 1. loss-streak warning  2. mini equity curve  3. best/worst pattern
 * 4. "haven't journaled today" nudge. The old full-size hero card is
 * shrunk to a thin welcome strip -- it was redundant with the header
 * right above it and taking up space better spent on the above.
 */
public class DashboardPanel extends JPanel {

    private final Consumer<String> onNavigate;

    private JLabel welcomeLabel;
    private JPanel alertsContainer;
    private RoundedPanel statCard;
    private JPanel statCardInner;
    private RoundedPanel equityCard;
    private EquityCurveChart equityCurve;
    private JPanel feedContainer;
    private RoundedPanel welcomeStrip;
    private JLabel welcomeMsgLabel;

    public DashboardPanel(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;

        setLayout(new BorderLayout(0, 20));
        setBackground(Theme.current().background);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        String name = "";
        try (Connection conn = DatabaseManager.connect()) {
            name = DatabaseManager.getSetting(conn, "profile_name", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        welcomeLabel = new JLabel("Welcome back" + (name.isBlank() ? "" : ", " + name));
        welcomeLabel.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 24));
        welcomeLabel.setForeground(Theme.current().textPrimary);
        headerRow.add(welcomeLabel, BorderLayout.WEST);

        add(headerRow, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        // ─── Alerts: loss streak + journal nudge, shown only when relevant ───
        alertsContainer = new JPanel();
        alertsContainer.setLayout(new BoxLayout(alertsContainer, BoxLayout.Y_AXIS));
        alertsContainer.setOpaque(false);
        alertsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(alertsContainer);

        // ─── Thin welcome strip (shrunk from the old full hero card) ───
        welcomeStrip = buildWelcomeStrip();
        welcomeStrip.setAlignmentX(Component.LEFT_ALIGNMENT);
        welcomeStrip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        body.add(welcomeStrip);
        body.add(Box.createRigidArea(new Dimension(0, 16)));

        // ─── Stat card + mini equity curve, side by side ───
        JPanel topRow = new JPanel(new BorderLayout(16, 0));
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        statCard = new RoundedPanel(new BorderLayout());
        statCard.cornerRadius(18).showBorder(true).showShadow(true);
        statCard.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        statCardInner = new JPanel();
        statCardInner.setLayout(new BoxLayout(statCardInner, BoxLayout.Y_AXIS));
        statCardInner.setOpaque(false);
        statCard.add(statCardInner, BorderLayout.CENTER);

        JPanel statCardWrap = new JPanel(new BorderLayout());
        statCardWrap.setOpaque(false);
        statCardWrap.setPreferredSize(new Dimension(230, 220));
        statCardWrap.add(statCard, BorderLayout.CENTER);
        topRow.add(statCardWrap, BorderLayout.WEST);

        equityCard = new RoundedPanel(new BorderLayout(0, 8));
        equityCard.cornerRadius(18).showBorder(true).showShadow(true);
        equityCard.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel equityHeading = new JLabel("Equity curve");
        equityHeading.setFont(Theme.headerFont());
        equityHeading.setForeground(Theme.current().textPrimary);
        equityCard.add(equityHeading, BorderLayout.NORTH);

        equityCurve = new EquityCurveChart();
        equityCard.add(equityCurve, BorderLayout.CENTER);

        topRow.add(equityCard, BorderLayout.CENTER);
        body.add(topRow);
        body.add(Box.createRigidArea(new Dimension(0, 24)));

        JLabel feedHeading = new JLabel("Recent activity");
        feedHeading.setFont(Theme.headerFont());
        feedHeading.setForeground(Theme.current().textPrimary);
        feedHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(feedHeading);
        body.add(Box.createRigidArea(new Dimension(0, 10)));

        feedContainer = new JPanel();
        feedContainer.setLayout(new BoxLayout(feedContainer, BoxLayout.Y_AXIS));
        feedContainer.setOpaque(false);
        feedContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(feedContainer);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        loadAll();

        javax.swing.Timer refreshTimer = new javax.swing.Timer(5000, e -> loadAll());
        refreshTimer.start();
    }

    private RoundedPanel buildWelcomeStrip() {
        RoundedPanel strip = new RoundedPanel(new BorderLayout());
        strip.cornerRadius(14).showBorder(false).showShadow(false);
        strip.fixedBackground(Theme.mix(Theme.current().accent, Theme.current().background, 0.12f));
        strip.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 12));

        JLabel msg = new JLabel("Log trades, review patterns, and build your edge over time.");
        this.welcomeMsgLabel = msg;
        msg.setFont(Theme.cellFont());
        msg.setForeground(Theme.current().textPrimary);
        strip.add(msg, BorderLayout.WEST);

        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        linkRow.setOpaque(false);

        PillButton logTradeBtn = new PillButton("Log a trade", PillButton.Style.PRIMARY);
        logTradeBtn.addActionListener(e -> onNavigate.accept("Trades"));
        linkRow.add(logTradeBtn);

        PillButton viewAnalyticsBtn = new PillButton("View analytics", PillButton.Style.SECONDARY);
        viewAnalyticsBtn.addActionListener(e -> onNavigate.accept("Analytics"));
        linkRow.add(viewAnalyticsBtn);

        strip.add(linkRow, BorderLayout.EAST);
        return strip;
    }

    public void refreshTheme() {
        setBackground(Theme.current().background);
        welcomeLabel.setForeground(Theme.current().textPrimary);
        welcomeStrip.fixedBackground(Theme.mix(Theme.current().accent, Theme.current().background, 0.12f));
        welcomeMsgLabel.setForeground(Theme.current().textPrimary);
        welcomeStrip.repaint();
        loadAll();
    }

    private void loadAll() {
        loadAlerts();
        loadStatCard();
        loadEquityCurve();
        loadFeed();
    }

    // ─── Alerts: loss streak (priority 1) + journal nudge (priority 4) ───

    private void loadAlerts() {
        alertsContainer.removeAll();

        int lossStreak = 0;
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT outcome FROM trades ORDER BY id DESC")) {
            while (rs.next()) {
                String outcome = rs.getString("outcome");
                if (outcome == null || outcome.isBlank()) continue;
                if ("Loss".equals(outcome)) lossStreak++;
                else break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (lossStreak >= 3) {
            RoundedPanel banner = new RoundedPanel(new BorderLayout());
            banner.cornerRadius(12).showBorder(false).showShadow(false);
            banner.fixedBackground(Theme.mix(Theme.current().danger, Theme.current().background, 0.16f));
            banner.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            banner.setAlignmentX(Component.LEFT_ALIGNMENT);
            banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

            JLabel label = new JLabel("\u26A0 " + lossStreak + " losses in a row \u2014 consider taking a break before the next trade.");
            label.setFont(Theme.buttonFont());
            label.setForeground(Theme.current().danger);
            banner.add(label, BorderLayout.WEST);

            alertsContainer.add(banner);
            alertsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        boolean journaledToday = false;
        String today = LocalDate.now().toString();
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) AS c FROM journal_entries WHERE entry_date = '" + today + "'")) {
            if (rs.next()) journaledToday = rs.getInt("c") > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!journaledToday) {
            JLabel nudge = new JLabel("You haven't journaled today yet.");
            nudge.setFont(new Font(Theme.FONT_FAMILY, Font.ITALIC, 12));
            nudge.setForeground(Theme.current().textSecondary);
            nudge.setAlignmentX(Component.LEFT_ALIGNMENT);
            nudge.setBorder(BorderFactory.createEmptyBorder(0, 2, 10, 0));
            alertsContainer.add(nudge);
        }

        alertsContainer.revalidate();
        alertsContainer.repaint();
    }

    // ─── Stat card, now including best/worst pattern (priority 3) ───

    private void loadStatCard() {
        statCardInner.removeAll();

        int wins = 0, losses = 0, total = 0;
        Map<String, int[]> patternStats = new LinkedHashMap<>(); // pattern -> [wins, total]

        String sql = "SELECT outcome, pair, pattern FROM trades";
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String outcome = rs.getString("outcome");
                String pair = rs.getString("pair");
                String pattern = rs.getString("pattern");
                boolean hasData = (pair != null && !pair.isBlank()) || (outcome != null && !outcome.isBlank());
                if (!hasData) continue;
                total++;
                if ("Win".equals(outcome)) wins++;
                else if ("Loss".equals(outcome)) losses++;

                if (pattern != null && !pattern.isBlank() && outcome != null && !outcome.isBlank()) {
                    int[] stat = patternStats.computeIfAbsent(pattern, k -> new int[2]);
                    stat[1]++;
                    if ("Win".equals(outcome)) stat[0]++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String winRate = (wins + losses > 0) ? String.format("%.0f%%", (double) wins / (wins + losses) * 100) : "--";

        statCardInner.add(statRow(winRate, "win rate"));
        statCardInner.add(Box.createRigidArea(new Dimension(0, 14)));
        statCardInner.add(statRow(String.valueOf(total), "trades logged"));
        statCardInner.add(Box.createRigidArea(new Dimension(0, 14)));
        statCardInner.add(statRow(riskPerTradeStr(), "risk per trade"));

        String bestPattern = null, worstPattern = null;
        double bestRate = -1, worstRate = 2;
        for (Map.Entry<String, int[]> entry : patternStats.entrySet()) {
            int w = entry.getValue()[0], t = entry.getValue()[1];
            double rate = (double) w / t;
            if (rate > bestRate) { bestRate = rate; bestPattern = entry.getKey(); }
            if (rate < worstRate) { worstRate = rate; worstPattern = entry.getKey(); }
        }

        if (bestPattern != null) {
            statCardInner.add(Box.createRigidArea(new Dimension(0, 14)));
            statCardInner.add(statRow(bestPattern, String.format("best pattern (%.0f%%)", bestRate * 100)));
        }
        if (worstPattern != null && !worstPattern.equals(bestPattern)) {
            statCardInner.add(Box.createRigidArea(new Dimension(0, 14)));
            statCardInner.add(statRow(worstPattern, String.format("worst pattern (%.0f%%)", worstRate * 100)));
        }

        statCardInner.revalidate();
        statCardInner.repaint();
    }

    private JPanel statRow(String bigValue, String label) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel bigLbl = new JLabel(bigValue);
        bigLbl.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 18));
        bigLbl.setForeground(Theme.current().textPrimary);
        bigLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(bigLbl);

        JLabel smallLbl = new JLabel(label);
        smallLbl.setFont(new Font(Theme.FONT_FAMILY, Font.PLAIN, 11));
        smallLbl.setForeground(Theme.current().textSecondary);
        smallLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(smallLbl);

        return row;
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

    // ─── Mini equity curve (priority 2) ───

    private void loadEquityCurve() {
        List<Integer> series = new ArrayList<>();
        String sql = "SELECT outcome FROM trades ORDER BY id ASC";
        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int running = 0;
            while (rs.next()) {
                String outcome = rs.getString("outcome");
                if (outcome == null || outcome.isBlank()) continue;
                if ("Win".equals(outcome)) running += 1;
                else if ("Loss".equals(outcome)) running -= 1;
                series.add(running);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        equityCurve.setData(series);
    }

    // ─── Activity feed (unchanged from before) ───

    private record ActivityItem(String date, String description, Color tagColor, String tag) {}

    private void loadFeed() {
        feedContainer.removeAll();

        List<ActivityItem> items = new ArrayList<>();

        String tradeSql = "SELECT trade_date, pair, pattern, outcome FROM trades "
                + "WHERE trade_date IS NOT NULL AND pair IS NOT NULL AND pair != '' "
                + "ORDER BY id DESC LIMIT 30";
        String journalSql = "SELECT entry_date, note FROM journal_entries ORDER BY id DESC LIMIT 30";

        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(tradeSql)) {
            while (rs.next()) {
                String date = rs.getString("trade_date");
                String outcome = rs.getString("outcome");
                String desc = "Logged a " + rs.getString("pair")
                        + (rs.getString("pattern") != null && !rs.getString("pattern").isBlank() ? " " + rs.getString("pattern") : "")
                        + " trade" + (outcome != null && !outcome.isBlank() ? " \u2014 " + outcome : "");
                Color tagColor = switch (outcome == null ? "" : outcome) {
                    case "Win" -> Theme.current().success;
                    case "Loss" -> Theme.current().danger;
                    case "Break Even" -> Theme.current().warning;
                    default -> Theme.current().textSecondary;
                };
                items.add(new ActivityItem(date, desc, tagColor, "Trade"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (Connection conn = DatabaseManager.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(journalSql)) {
            while (rs.next()) {
                String note = rs.getString("note");
                String preview = note.length() > 90 ? note.substring(0, 90) + "\u2026" : note;
                items.add(new ActivityItem(rs.getString("entry_date"), preview, Theme.current().accent, "Journal"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        items.sort((a, b) -> b.date().compareTo(a.date()));

        if (items.isEmpty()) {
            JLabel empty = new JLabel("Nothing logged yet. Add a trade or journal entry to see it here.");
            empty.setFont(Theme.cellFont());
            empty.setForeground(Theme.current().textSecondary);
            feedContainer.add(empty);
            feedContainer.revalidate();
            feedContainer.repaint();
            return;
        }

        String today = LocalDate.now().toString();
        String yesterday = LocalDate.now().minusDays(1).toString();

        String currentBucket = null;
        int shown = 0;
        for (ActivityItem item : items) {
            if (shown >= 20) break;
            String bucket = item.date().equals(today) ? "Today"
                    : item.date().equals(yesterday) ? "Yesterday"
                      : item.date();
            if (!bucket.equals(currentBucket)) {
                feedContainer.add(bucketHeader(bucket));
                currentBucket = bucket;
            }
            feedContainer.add(feedRow(item));
            shown++;
        }

        feedContainer.revalidate();
        feedContainer.repaint();
    }

    private JLabel bucketHeader(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 11));
        label.setForeground(Theme.current().textSecondary);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(14, 2, 6, 0));
        return label;
    }

    private JPanel feedRow(ActivityItem item) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.current().border),
                BorderFactory.createEmptyBorder(7, 4, 7, 4)
        ));

        JLabel tagLbl = new JLabel(item.tag());
        tagLbl.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 10));
        tagLbl.setForeground(item.tagColor());
        tagLbl.setPreferredSize(new Dimension(56, 18));
        row.add(tagLbl, BorderLayout.WEST);

        JLabel descLbl = new JLabel(item.description());
        descLbl.setFont(Theme.cellFont());
        descLbl.setForeground(Theme.current().textPrimary);
        row.add(descLbl, BorderLayout.CENTER);

        return row;
    }
}