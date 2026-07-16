package MainApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Simple pill-shaped button: solid rounded fill, centered text. No
 * shadow, no glow, no gradient -- just a clean pill, theme-colored so
 * it follows light/dark mode like everything else.
 */
public class PillButton extends JButton {

    public enum Style { PRIMARY, SECONDARY }
    public enum ColorRole { ACCENT, SUCCESS, DANGER, WARNING }

    private final Style style;
    private final ColorRole colorRole;
    private boolean hovered = false;
    private boolean pressed = false;
    private boolean compact = false;

    public PillButton(String text, Style style) {
        this(text, style, ColorRole.ACCENT);
    }

    public PillButton(String text, Style style, ColorRole colorRole) {
        super(text);
        this.style = style;
        this.colorRole = colorRole;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setHorizontalAlignment(SwingConstants.CENTER);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 14));
        setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
        });
    }

    public PillButton compact() {
        this.compact = true;
        setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 12));
        setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        return this;
    }

    private Color resolveColor() {
        return switch (colorRole) {
            case SUCCESS -> Theme.current().success;
            case DANGER -> Theme.current().danger;
            case WARNING -> Theme.current().warning;
            default -> Theme.current().accent;
        };
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (compact) {
            return new Dimension(Math.max(d.width, 80), Math.max(d.height, 30));
        }
        return new Dimension(Math.max(d.width, 100), Math.max(d.height, 38));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int arc = h;

        Color base = resolveColor();

        if (style == Style.PRIMARY) {
            Color fill = pressed ? base.darker() : hovered ? Theme.mix(Color.WHITE, base, 0.15f) : base;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            setForeground(Color.WHITE);
        } else {
            if (hovered) {
                g2.setColor(Theme.mix(base, Theme.current().background, 0.15f));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
            }
            g2.setColor(base);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);
            setForeground(base);
        }

        g2.dispose();
        super.paintComponent(g); // draws the centered text only
    }
}