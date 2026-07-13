package MainApp;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Month-grid calendar colored by each day's dominant trade outcome.
 * Depends on the trade_date column added in the DatabaseManager
 * migration -- trades saved before that migration have no date and
 * are simply excluded here, which is expected, not a bug.
 */
public class CalendarPanel extends JPanel {

    private YearMonth currentMonth = YearMonth.now();
    private JLabel monthLabel;
    private JPanel gridPanel;

    public CalendarPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.current().background);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(Theme.current().background);

        JLabel title = new JLabel("Calendar");
        title.setFont(Theme.headerFont());
        title.setForeground(Theme.current().textPrimary);
        headerRow.add(title, BorderLayout.WEST);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        navPanel.setBackground(Theme.current().background);

        JButton prevButton = new JButton("< Prev");
        prevButton.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            loadMonth();
        });

        monthLabel = new JLabel();
        monthLabel.setFont(Theme.buttonFont());
        monthLabel.setForeground(Theme.current().textPrimary);

        JButton nextButton = new JButton("Next >");
        nextButton.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            loadMonth();
        });

        navPanel.add(prevButton);
        navPanel.add(monthLabel);
        navPanel.add(nextButton);
        headerRow.add(navPanel, BorderLayout.EAST);

        add(headerRow, BorderLayout.NORTH);

        gridPanel = new JPanel(new GridLayout(0, 7, 4, 4));
        gridPanel.setBackground(Theme.current().background);
        add(gridPanel, BorderLayout.CENTER);

        loadMonth();

        Timer refreshTimer = new Timer(5000, e -> loadMonth());
        refreshTimer.start();
    }

    public void refreshTheme() {
        setBackground(Theme.current().background);
        if (gridPanel != null) {
            gridPanel.setBackground(Theme.current().background);
        }
        loadMonth();
    }

    private void loadMonth() {
        gridPanel.removeAll();
        monthLabel.setText(currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                + " " + currentMonth.getYear());

        // day-of-week headers, Monday first
        for (int i = 0; i < 7; i++) {
            DayOfWeek dow = DayOfWeek.of(i + 1);
            JLabel dowLabel = new JLabel(dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()), SwingConstants.CENTER);
            dowLabel.setFont(Theme.cellFont());
            dowLabel.setForeground(Theme.current().textSecondary);
            gridPanel.add(dowLabel);
        }

        Map<Integer, int[]> dayStats = new HashMap<>(); // day -> [wins, losses, breakEvens]
        String monthPrefix = currentMonth.toString(); // e.g. "2026-07"

        String sql = "SELECT trade_date, outcome FROM trades WHERE trade_date LIKE ?";
        try (
                Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setString(1, monthPrefix + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String dateStr = rs.getString("trade_date");
                    String outcome = rs.getString("outcome");
                    if (dateStr == null || dateStr.isBlank() || outcome == null || outcome.isBlank()) continue;

                    int day;
                    try {
                        day = LocalDate.parse(dateStr).getDayOfMonth();
                    } catch (Exception parseEx) {
                        continue; // malformed date, e.g. hand-typed wrong -- skip rather than abort the whole load
                    }

                    int[] stat = dayStats.computeIfAbsent(day, k -> new int[3]);
                    switch (outcome) {
                        case "Win" -> stat[0]++;
                        case "Loss" -> stat[1]++;
                        case "Break Even" -> stat[2]++;
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load calendar data.\n\n" + e.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int leadingBlanks = firstOfMonth.getDayOfWeek().getValue() - 1; // Monday = 0 offset

        for (int i = 0; i < leadingBlanks; i++) {
            JPanel blank = new JPanel();
            blank.setBackground(Theme.current().background);
            gridPanel.add(blank);
        }

        int daysInMonth = currentMonth.lengthOfMonth();
        for (int day = 1; day <= daysInMonth; day++) {
            gridPanel.add(dayCell(day, dayStats.get(day)));
        }

        revalidate();
        repaint();
    }

    private JPanel dayCell(int day, int[] stats) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setPreferredSize(new Dimension(60, 60));

        Color baseBg = Theme.current().surface;
        String summary = "";

        if (stats != null) {
            int wins = stats[0], losses = stats[1], be = stats[2];
            int total = wins + losses + be;
            summary = total + (total == 1 ? " trade" : " trades");

            if (wins > losses) baseBg = mix(Theme.current().success, Theme.current().surface, 0.25f);
            else if (losses > wins) baseBg = mix(Theme.current().danger, Theme.current().surface, 0.25f);
            else if (total > 0) baseBg = mix(Theme.current().warning, Theme.current().surface, 0.2f);
        }

        cell.setBackground(baseBg);
        cell.setBorder(BorderFactory.createLineBorder(Theme.current().border));
        cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cell.setToolTipText("Click to see this day's trades and journal entries");

        JLabel dayLabel = new JLabel(String.valueOf(day));
        dayLabel.setFont(Theme.cellFont());
        dayLabel.setForeground(Theme.current().textPrimary);
        dayLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 0, 0));
        cell.add(dayLabel, BorderLayout.NORTH);

        if (!summary.isEmpty()) {
            JLabel summaryLabel = new JLabel(summary);
            summaryLabel.setFont(new Font(Theme.FONT_FAMILY, Font.PLAIN, 10));
            summaryLabel.setForeground(Theme.current().textSecondary);
            summaryLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 4, 0));
            cell.add(summaryLabel, BorderLayout.SOUTH);
        }

        LocalDate clickedDate = currentMonth.atDay(day);
        cell.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                cell.setBorder(BorderFactory.createLineBorder(Theme.current().accent, 2));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                cell.setBorder(BorderFactory.createLineBorder(Theme.current().border));
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showDayDetail(clickedDate);
            }
        });

        return cell;
    }

    /** Drill-down view: every trade and journal entry logged on a specific day. */
    private void showDayDetail(LocalDate date) {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.current().surface);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        String tradesSql = "SELECT pair, pattern, wave, diversion, sr, direction, entrySignal, outcome "
                + "FROM trades WHERE trade_date = ? ORDER BY id ASC";
        String journalSql = "SELECT note, trade_id FROM journal_entries WHERE entry_date = ? ORDER BY id ASC";

        int tradeCount = 0, journalCount = 0;

        JLabel tradesHeading = new JLabel("Trades");
        tradesHeading.setFont(Theme.headerFont());
        tradesHeading.setForeground(Theme.current().textPrimary);
        tradesHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(tradesHeading);
        content.add(Box.createRigidArea(new Dimension(0, 6)));

        try (
                Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(tradesSql)
        ) {
            pstmt.setString(1, date.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tradeCount++;
                    content.add(dayDetailTradeRow(
                            rs.getString("pair"), rs.getString("pattern"), rs.getString("direction"),
                            rs.getString("entrySignal"), rs.getString("outcome")));
                    content.add(Box.createRigidArea(new Dimension(0, 4)));
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load trades for this day.\n\n" + e.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }

        if (tradeCount == 0) {
            JLabel none = new JLabel("No trades logged this day.");
            none.setFont(Theme.cellFont());
            none.setForeground(Theme.current().textSecondary);
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(none);
        }

        content.add(Box.createRigidArea(new Dimension(0, 16)));

        JLabel journalHeading = new JLabel("Journal entries");
        journalHeading.setFont(Theme.headerFont());
        journalHeading.setForeground(Theme.current().textPrimary);
        journalHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(journalHeading);
        content.add(Box.createRigidArea(new Dimension(0, 6)));

        try (
                Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(journalSql)
        ) {
            pstmt.setString(1, date.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    journalCount++;
                    JTextArea noteArea = new JTextArea(rs.getString("note"));
                    noteArea.setEditable(false);
                    noteArea.setLineWrap(true);
                    noteArea.setWrapStyleWord(true);
                    noteArea.setFont(Theme.cellFont());
                    noteArea.setBackground(Theme.current().background);
                    noteArea.setForeground(Theme.current().textPrimary);
                    noteArea.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Theme.current().border),
                            BorderFactory.createEmptyBorder(8, 8, 8, 8)
                    ));
                    noteArea.setAlignmentX(Component.LEFT_ALIGNMENT);
                    noteArea.setMaximumSize(new Dimension(420, 100));
                    content.add(noteArea);
                    content.add(Box.createRigidArea(new Dimension(0, 6)));
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load journal entries for this day.\n\n" + e.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }

        if (journalCount == 0) {
            JLabel none = new JLabel("No journal entries this day.");
            none.setFont(Theme.cellFont());
            none.setForeground(Theme.current().textSecondary);
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(none);
        }

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setPreferredSize(new Dimension(440, 420));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JOptionPane.showMessageDialog(this, scrollPane,
                date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()) + ", " + date,
                JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel dayDetailTradeRow(String pair, String pattern, String direction, String entrySignal, String outcome) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Theme.current().background);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(420, 44));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.current().border),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        String left = nullSafe(pair) + "  \u00b7  " + nullSafe(pattern)
                + (direction != null && !direction.isBlank() ? "  \u00b7  " + direction : "")
                + (entrySignal != null && !entrySignal.isBlank() ? "  \u00b7  " + entrySignal : "");
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

    private String nullSafe(String s) {
        return (s == null || s.isBlank()) ? "--" : s;
    }

    private static Color mix(Color a, Color b, float ratio) {
        int r = (int) (a.getRed() * ratio + b.getRed() * (1 - ratio));
        int g = (int) (a.getGreen() * ratio + b.getGreen() * (1 - ratio));
        int bl = (int) (a.getBlue() * ratio + b.getBlue() * (1 - ratio));
        return new Color(r, g, bl);
    }
}