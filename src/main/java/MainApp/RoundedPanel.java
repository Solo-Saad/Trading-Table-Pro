package MainApp;

import javax.swing.*;
import java.awt.*;

/**
 * A JPanel with rounded corners and a cheap, soft drop shadow --
 * the "card" building block for the warm design language every
 * screen will be rebuilt on top of.
 *
 * Colors resolve from Theme.current() at PAINT time by default, not
 * cached at construction, so cards automatically follow light/dark
 * toggles with no refreshTheme() call needed -- same trick already
 * used by the table's cell renderers. Pass a fixed background via
 * fixedBackground(...) only when a card needs a color that isn't
 * theme-dependent (rare).
 *
 * Usage:
 *   RoundedPanel card = new RoundedPanel(new BorderLayout());
 *   card.cornerRadius(20).showShadow(true);
 *   card.add(someLabel);
 */
public class RoundedPanel extends JPanel {

    private int cornerRadius = 16;
    private Color fixedBackground = null;
    private boolean showBorder = true;
    private boolean showShadow = true;

    public RoundedPanel() {
        setOpaque(false);
    }

    public RoundedPanel(LayoutManager layout) {
        this();
        setLayout(layout);
    }

    public RoundedPanel cornerRadius(int radius) {
        this.cornerRadius = radius;
        return this;
    }

    /** Overrides the theme-driven surface color with a fixed one. Use sparingly. */
    public RoundedPanel fixedBackground(Color color) {
        this.fixedBackground = color;
        return this;
    }

    public RoundedPanel showBorder(boolean show) {
        this.showBorder = show;
        return this;
    }

    public RoundedPanel showShadow(boolean show) {
        this.showShadow = show;
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int edgePad = showShadow ? 3 : 0;

        if (showShadow) {
            g2.setColor(new Color(0, 0, 0, 18));
            g2.fillRoundRect(1, 3, w - 3, h - 4, cornerRadius, cornerRadius);
        }

        Color bg = fixedBackground != null ? fixedBackground : Theme.current().surface;
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w - edgePad, h - edgePad - 1, cornerRadius, cornerRadius);

        if (showBorder) {
            g2.setColor(Theme.current().border);
            g2.drawRoundRect(0, 0, w - edgePad - 1, h - edgePad - 2, cornerRadius, cornerRadius);
        }

        g2.dispose();
    }
}
