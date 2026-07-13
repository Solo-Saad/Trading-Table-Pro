package MainApp;

import javax.swing.*;
import java.awt.*;

/** Placeholder — built out in Phase 3. */
public class CalendarPanel extends JPanel {
    public CalendarPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.current().background);

        JLabel label = new JLabel("Calendar — coming soon", SwingConstants.CENTER);
        label.setFont(Theme.subtitleFont());
        label.setForeground(Theme.current().textSecondary);
        add(label, BorderLayout.CENTER);
    }
}
