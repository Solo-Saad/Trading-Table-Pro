package MainApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Left-side navigation. Each entry corresponds to a card name in
 * MainFrame's CardLayout. Clicking an item calls back to MainFrame
 * to switch screens and highlights the active item.
 */
public class Sidebar extends JPanel {

    private static final String[] NAV_ITEMS = {
            "Dashboard", "Trades", "Analytics", "Journal", "Calendar"
    };

    private final Map<String, JPanel> navRows = new LinkedHashMap<>();
    private String activeItem = "Dashboard";

    public Sidebar(Consumer<String> onNavigate) {
        setLayout(new BorderLayout());
        setBackground(Theme.current().surface);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.current().border));
        setPreferredSize(new Dimension(190, 0));

        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(Theme.current().surface);
        navPanel.setBorder(BorderFactory.createEmptyBorder(16, 10, 16, 10));

        JLabel brand = new JLabel("TradeLog");
        brand.setFont(Theme.headerFont());
        brand.setForeground(Theme.current().textPrimary);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 6, 20, 0));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        navPanel.add(brand);

        for (String item : NAV_ITEMS) {
            JPanel row = createNavRow(item, onNavigate);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            navRows.put(item, row);
            navPanel.add(row);
            navPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        }

        add(navPanel, BorderLayout.NORTH);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBackground(Theme.current().surface);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 16, 10));

        JPanel settingsRow = createNavRow("Settings", onNavigate);
        settingsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        navRows.put("Settings", settingsRow);
        bottomPanel.add(settingsRow);

        add(bottomPanel, BorderLayout.SOUTH);

        updateHighlight();
    }

    private JPanel createNavRow(String label, Consumer<String> onNavigate) {
        JPanel row = new JPanel(new BorderLayout());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        row.setOpaque(true);
        row.setBackground(Theme.current().surface);
        row.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel text = new JLabel(label);
        text.setFont(Theme.cellFont());
        text.setForeground(Theme.current().textSecondary);
        row.add(text, BorderLayout.WEST);

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!label.equals(activeItem)) {
                    row.setBackground(Theme.current().hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!label.equals(activeItem)) {
                    row.setBackground(Theme.current().surface);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                activeItem = label;
                updateHighlight();
                onNavigate.accept(label);
            }
        });

        return row;
    }

    private void updateHighlight() {
        for (Map.Entry<String, JPanel> entry : navRows.entrySet()) {
            JPanel row = entry.getValue();
            JLabel text = (JLabel) row.getComponent(0);
            boolean isActive = entry.getKey().equals(activeItem);
            row.setBackground(isActive ? Theme.current().hover : Theme.current().surface);
            text.setForeground(isActive ? Theme.current().accent : Theme.current().textSecondary);
        }
    }
}
