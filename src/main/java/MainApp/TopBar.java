package MainApp;

import javax.swing.*;
import java.awt.*;

/**
 * Sits above the card container: title on the left, dark mode toggle
 * and profile avatar on the right. The dark mode button flips
 * Theme's mode; MainFrame listens for that change and rebuilds the
 * shell so colors actually update.
 */
public class TopBar extends JPanel {

    private JButton themeButton;

    public TopBar() {
        setLayout(new BorderLayout());
        setBackground(Theme.current().background);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("Trading Analysis Dashboard");
        title.setFont(Theme.titleFont());
        title.setForeground(Theme.current().textPrimary);
        add(title, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setBackground(Theme.current().background);

        themeButton = new JButton(Theme.getMode() == Theme.Mode.DARK ? "Light mode" : "Dark mode");
        themeButton.setFont(Theme.cellFont());
        themeButton.setFocusPainted(false);
        themeButton.addActionListener(e -> Theme.toggle());
        rightPanel.add(themeButton);

        JLabel profile = new JLabel("IK", SwingConstants.CENTER);
        profile.setOpaque(true);
        profile.setBackground(Theme.current().accent);
        profile.setForeground(Color.WHITE);
        profile.setFont(Theme.buttonFont());
        profile.setPreferredSize(new Dimension(34, 34));
        rightPanel.add(profile);

        add(rightPanel, BorderLayout.EAST);
    }
}
