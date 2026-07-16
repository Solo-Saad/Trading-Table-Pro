package MainApp;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

/**
 * Title on the left, dark mode toggle + profile avatar on the right.
 * Profile initials are loaded from the saved profile_name setting and
 * can be updated live via setProfileName() when Settings saves a change.
 */
public class TopBar extends JPanel {

    private JLabel title;
    private JPanel rightPanel;
    private ThemeToggleButton themeToggle;
    private JLabel profile;

    public TopBar() {
        setLayout(new BorderLayout());
        setBackground(Theme.current().background);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        title = new JLabel("Trading Analysis Dashboard");
        title.setFont(Theme.titleFont());
        title.setForeground(Theme.current().textPrimary);
        add(title, BorderLayout.WEST);

        rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setBackground(Theme.current().background);

        themeToggle = new ThemeToggleButton();
        rightPanel.add(themeToggle);

        String savedName = "";
        try (Connection conn = DatabaseManager.connect()) {
            savedName = DatabaseManager.getSetting(conn, "profile_name", "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        profile = new JLabel(initialsFor(savedName), SwingConstants.CENTER);
        profile.setOpaque(true);
        profile.setBackground(Theme.current().accent);
        profile.setForeground(Color.WHITE);
        profile.setFont(Theme.buttonFont());
        profile.setPreferredSize(new Dimension(34, 34));
        profile.setToolTipText(savedName.isBlank() ? "Set your name in Settings" : savedName);
        rightPanel.add(profile);

        add(rightPanel, BorderLayout.EAST);
    }

    public void setProfileName(String name) {
        profile.setText(initialsFor(name));
        profile.setToolTipText(name.isBlank() ? "Set your name in Settings" : name);
    }

    private String initialsFor(String name) {
        if (name == null || name.isBlank()) return "IK";
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty()) initials.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return initials.length() > 0 ? initials.toString() : "IK";
    }

    public void refreshTheme() {
        setBackground(Theme.current().background);
        title.setForeground(Theme.current().textPrimary);
        rightPanel.setBackground(Theme.current().background);
        themeToggle.repaint();
        profile.setBackground(Theme.current().accent);
        revalidate();
        repaint();
    }
}