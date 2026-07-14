package MainApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;

/**
 * Icon-only light/dark toggle -- shows the icon of the mode you'll
 * switch TO (sun while in dark mode, moon while in light mode), a
 * common convention. Replaces the old text button.
 */
public class ThemeToggleButton extends JComponent {

    private boolean hovered = false;

    public ThemeToggleButton() {
        setPreferredSize(new Dimension(34, 34));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setToolTipText("Toggle light / dark mode");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Theme.toggle();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int cx = w / 2, cy = h / 2;

        if (hovered) {
            g2.setColor(Theme.current().hover);
            g2.fillRoundRect(0, 0, w, h, 10, 10);
        }

        g2.setColor(Theme.current().textPrimary);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if (Theme.getMode() == Theme.Mode.DARK) {
            // currently dark -- show sun (click to go light)
            g2.drawOval(cx - 4, cy - 4, 8, 8);
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45);
                int x1 = (int) (cx + 7 * Math.cos(angle));
                int y1 = (int) (cy + 7 * Math.sin(angle));
                int x2 = (int) (cx + 10 * Math.cos(angle));
                int y2 = (int) (cy + 10 * Math.sin(angle));
                g2.drawLine(x1, y1, x2, y2);
            }
        } else {
            // currently light -- show moon (click to go dark)
            Area moon = new Area(new Ellipse2D.Double(cx - 7, cy - 7, 14, 14));
            moon.subtract(new Area(new Ellipse2D.Double(cx - 3, cy - 9, 14, 14)));
            g2.fill(moon);
        }

        g2.dispose();
    }
}
