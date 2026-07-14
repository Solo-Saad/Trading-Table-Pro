package MainApp;

import javax.swing.*;
import java.awt.*;

/**
 * Small hand-drawn monochrome icons for the sidebar -- no icon font
 * dependency, just simple Graphics2D shapes. Color resolves from
 * Theme at paint time (via the color passed to paintIcon each call),
 * so it works whether the row is active or not without extra wiring.
 */
public class NavIcon extends JComponent {

    public enum Type { DASHBOARD, TRADES, ANALYTICS, JOURNAL, CALENDAR, SETTINGS }

    private final Type type;
    private Color color;

    public NavIcon(Type type) {
        this.type = type;
        setPreferredSize(new Dimension(18, 18));
        setOpaque(false);
    }

    public void setIconColor(Color color) {
        this.color = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color != null ? color : Theme.current().textSecondary);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int s = 18;
        switch (type) {
            case DASHBOARD -> {
                int gap = 2, cell = (s - gap * 3) / 2;
                g2.fillRoundRect(0, 0, cell, cell, 3, 3);
                g2.fillRoundRect(cell + gap, 0, cell, cell, 3, 3);
                g2.fillRoundRect(0, cell + gap, cell, cell, 3, 3);
                g2.fillRoundRect(cell + gap, cell + gap, cell, cell, 3, 3);
            }
            case TRADES -> {
                g2.fillRoundRect(0, 10, 3, 8, 1, 1);
                g2.fillRoundRect(5, 6, 3, 12, 1, 1);
                g2.fillRoundRect(10, 2, 3, 16, 1, 1);
                g2.fillRoundRect(15, 8, 3, 10, 1, 1);
            }
            case ANALYTICS -> {
                g2.fillRoundRect(0, 11, 3, 7, 1, 1);
                g2.fillRoundRect(5, 6, 3, 12, 1, 1);
                g2.fillRoundRect(10, 9, 3, 9, 1, 1);
                g2.fillRoundRect(15, 2, 3, 16, 1, 1);
                g2.setColor(new Color(color != null ? color.getRGB() : Theme.current().accent.getRGB(), true));
                g2.drawLine(1, 9, 6, 5);
                g2.drawLine(6, 5, 11, 8);
                g2.drawLine(11, 8, 16, 1);
            }
            case JOURNAL -> {
                g2.drawRoundRect(1, 1, s - 3, s - 3, 3, 3);
                g2.drawLine(4, 6, 14, 6);
                g2.drawLine(4, 10, 14, 10);
                g2.drawLine(4, 14, 10, 14);
            }
            case CALENDAR -> {
                g2.drawRoundRect(1, 3, s - 3, s - 5, 3, 3);
                g2.drawLine(1, 7, s - 2, 7);
                g2.drawLine(5, 1, 5, 5);
                g2.drawLine(13, 1, 13, 5);
                g2.fillOval(4, 10, 3, 3);
                g2.fillOval(9, 10, 3, 3);
            }
            case SETTINGS -> {
                g2.drawLine(2, 4, 16, 4);
                g2.fillOval(9, 2, 4, 4);
                g2.drawLine(2, 9, 16, 9);
                g2.fillOval(4, 7, 4, 4);
                g2.drawLine(2, 14, 16, 14);
                g2.fillOval(11, 12, 4, 4);
            }
        }

        g2.dispose();
    }
}
