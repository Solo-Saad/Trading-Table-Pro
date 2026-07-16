package MainApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Icon + label nav rows with a rounded pill for the active item,
 * matching the warm reference design. The bottom "promo card" slot
 * from the reference is repurposed here to show a real logging
 * streak instead of an upsell.
 */
public class Sidebar extends JPanel {

    private static final NavIcon.Type[] NAV_TYPES = {
            NavIcon.Type.DASHBOARD, NavIcon.Type.TRADES, NavIcon.Type.ANALYTICS,
            NavIcon.Type.JOURNAL, NavIcon.Type.CALENDAR
    };
    private static final String[] NAV_LABELS = {
            "Dashboard", "Trades", "Analytics", "Journal", "Calendar"
    };

    private final Map<String, RoundedPanel> navRows = new LinkedHashMap<>();
    private final Map<String, NavIcon> navIcons = new LinkedHashMap<>();
    private final Map<String, JLabel> navLabels = new LinkedHashMap<>();
    private String activeItem = "Dashboard";

    private JPanel navPanel;
    private JPanel bottomPanel;
    private JLabel brandLabel;
    private RoundedPanel logoMark;
    private JLabel streakLabel;
    private RoundedPanel streakCard;

    public Sidebar(Consumer<String> onNavigate) {
        setLayout(new BorderLayout());
        setBackground(Theme.current().background);
        setBorder(BorderFactory.createEmptyBorder(20, 14, 20, 14));
        setPreferredSize(new Dimension(210, 0));

        navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);

        JPanel brandRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandRow.setOpaque(false);
        brandRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandRow.setBorder(BorderFactory.createEmptyBorder(0, 2, 24, 0));

        RoundedPanel logoMark = new RoundedPanel();
        this.logoMark = logoMark;
        logoMark.fixedBackground(Theme.current().accent).cornerRadius(8).showBorder(false).showShadow(false);
        logoMark.setPreferredSize(new Dimension(28, 28));
        brandRow.add(logoMark);

        brandLabel = new JLabel("TradeLog");
        brandLabel.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 17));
        brandLabel.setForeground(Theme.current().textPrimary);
        brandRow.add(brandLabel);

        navPanel.add(brandRow);

        for (int i = 0; i < NAV_LABELS.length; i++) {
            RoundedPanel row = createNavRow(NAV_TYPES[i], NAV_LABELS[i], onNavigate);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            navRows.put(NAV_LABELS[i], row);
            navPanel.add(row);
            navPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        }

        add(navPanel, BorderLayout.NORTH);

        bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        streakCard = buildStreakCard();
        streakCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        streakCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        bottomPanel.add(streakCard);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        RoundedPanel settingsRow = createNavRow(NavIcon.Type.SETTINGS, "Settings", onNavigate);
        settingsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        navRows.put("Settings", settingsRow);
        bottomPanel.add(settingsRow);

        add(bottomPanel, BorderLayout.SOUTH);

        updateHighlight();
    }

    private RoundedPanel buildStreakCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.cornerRadius(14).showBorder(true).showShadow(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        int streak = 0;
        try (Connection conn = DatabaseManager.connect()) {
            streak = DatabaseManager.getLoggingStreak(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }

        streakLabel = new JLabel("\uD83D\uDD25 " + streak + " day streak");
        streakLabel.setFont(Theme.buttonFont());
        streakLabel.setForeground(Theme.current().textPrimary);
        card.add(streakLabel, BorderLayout.NORTH);

        JLabel sub = new JLabel("Keep logging trades or journal entries");
        sub.setFont(new Font(Theme.FONT_FAMILY, Font.PLAIN, 10));
        sub.setForeground(Theme.current().textSecondary);
        card.add(sub, BorderLayout.SOUTH);

        return card;
    }

    private RoundedPanel createNavRow(NavIcon.Type iconType, String label, Consumer<String> onNavigate) {
        RoundedPanel row = new RoundedPanel(new BorderLayout(10, 0));
        row.cornerRadius(10).showBorder(false).showShadow(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        NavIcon icon = new NavIcon(iconType);
        navIcons.put(label, icon);
        row.add(icon, BorderLayout.WEST);

        JLabel text = new JLabel(label);
        text.setFont(Theme.cellFont());
        navLabels.put(label, text);
        row.add(text, BorderLayout.CENTER);

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                activeItem = label;
                updateHighlight();
                onNavigate.accept(label);
            }
        });

        return row;
    }

    /** Lets navigation triggered from elsewhere (e.g. a button on the
     *  Dashboard itself) keep the sidebar's highlight in sync, not just
     *  clicks on the sidebar rows themselves. */
    public void setActive(String label) {
        if (navRows.containsKey(label)) {
            activeItem = label;
            updateHighlight();
        }
    }

    private void updateHighlight() {
        for (Map.Entry<String, RoundedPanel> entry : navRows.entrySet()) {
            boolean isActive = entry.getKey().equals(activeItem);
            RoundedPanel row = entry.getValue();
            row.fixedBackground(isActive ? Theme.mix(Theme.current().accent, Theme.current().background, 0.14f) : null);
            navLabels.get(entry.getKey()).setForeground(isActive ? Theme.current().accent : Theme.current().textSecondary);
            navIcons.get(entry.getKey()).setIconColor(isActive ? Theme.current().accent : Theme.current().textSecondary);
            row.repaint();
        }
    }

    /** Recolors everything in place and recomputes the streak. */
    public void refreshTheme() {
        setBackground(Theme.current().background);
        brandLabel.setForeground(Theme.current().textPrimary);
        logoMark.fixedBackground(Theme.current().accent);
        logoMark.repaint();
        updateHighlight();

        int streak = 0;
        try (Connection conn = DatabaseManager.connect()) {
            streak = DatabaseManager.getLoggingStreak(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        streakLabel.setText("\uD83D\uDD25 " + streak + " day streak");

        revalidate();
        repaint();
    }
}