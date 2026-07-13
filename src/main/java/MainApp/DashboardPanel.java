package MainApp;

import javax.swing.*;
import java.awt.*;

/** Placeholder — built out in Phase 3, step 8. */
public class DashboardPanel extends JPanel {
    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.current().background);

        JLabel label = new JLabel("Dashboard — coming soon", SwingConstants.CENTER);
        label.setFont(Theme.subtitleFont());
        label.setForeground(Theme.current().textSecondary);
        add(label, BorderLayout.CENTER);
    }
}
