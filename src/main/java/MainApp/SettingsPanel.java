package MainApp;

import javax.swing.*;
import java.awt.*;

/** Placeholder — built out in Phase 3. */
public class SettingsPanel extends JPanel {
    public SettingsPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.current().background);

        JLabel label = new JLabel("Settings — coming soon", SwingConstants.CENTER);
        label.setFont(Theme.subtitleFont());
        label.setForeground(Theme.current().textSecondary);
        add(label, BorderLayout.CENTER);
    }
}
