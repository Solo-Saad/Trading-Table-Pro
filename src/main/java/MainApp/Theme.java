package MainApp;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Single source of truth for colors and fonts across the app.
 * Every screen/panel should read from Theme.current() instead of
 * hardcoding Color/Font values, so light/dark toggling and any
 * future palette change happens in one place.
 */
public class Theme {

    public enum Mode { LIGHT, DARK }

    private static Mode currentMode = Mode.LIGHT;
    private static final List<Runnable> listeners = new ArrayList<>();

    public static final String FONT_FAMILY = "Segoe UI";

    // ─── Palette definition ─────────────────────────────────────────────

    public static class Palette {
        public final Color background;      // window/page background
        public final Color surface;         // cards, table body
        public final Color headerBg;        // table header / nav bg
        public final Color headerFg;
        public final Color rowEven;
        public final Color rowOdd;
        public final Color hover;
        public final Color border;
        public final Color textPrimary;
        public final Color textSecondary;
        public final Color accent;          // primary buttons, links
        public final Color success;
        public final Color danger;
        public final Color warning;
        public final Color selection;

        public Palette(Color background, Color surface, Color headerBg, Color headerFg,
                        Color rowEven, Color rowOdd, Color hover, Color border,
                        Color textPrimary, Color textSecondary, Color accent,
                        Color success, Color danger, Color warning, Color selection) {
            this.background = background;
            this.surface = surface;
            this.headerBg = headerBg;
            this.headerFg = headerFg;
            this.rowEven = rowEven;
            this.rowOdd = rowOdd;
            this.hover = hover;
            this.border = border;
            this.textPrimary = textPrimary;
            this.textSecondary = textSecondary;
            this.accent = accent;
            this.success = success;
            this.danger = danger;
            this.warning = warning;
            this.selection = selection;
        }
    }

    private static final Palette LIGHT = new Palette(
            new Color(236, 240, 241),   // background
            Color.WHITE,                // surface
            new Color(44, 62, 80),      // headerBg (was a 7-color rainbow array — now one color)
            Color.WHITE,                // headerFg
            new Color(236, 240, 241),   // rowEven
            Color.WHITE,                // rowOdd
            new Color(174, 214, 241),   // hover
            new Color(189, 195, 199),   // border
            new Color(44, 62, 80),      // textPrimary
            new Color(127, 140, 141),   // textSecondary
            new Color(52, 152, 219),    // accent
            new Color(46, 204, 113),    // success
            new Color(231, 76, 60),     // danger
            new Color(230, 126, 34),    // warning
            new Color(52, 152, 219)     // selection
    );

    private static final Palette DARK = new Palette(
            new Color(30, 33, 36),      // background
            new Color(40, 44, 48),      // surface
            new Color(52, 58, 64),      // headerBg
            new Color(236, 240, 241),   // headerFg
            new Color(40, 44, 48),      // rowEven
            new Color(48, 52, 56),      // rowOdd
            new Color(60, 75, 90),      // hover
            new Color(70, 74, 78),      // border
            new Color(230, 230, 230),   // textPrimary
            new Color(160, 165, 170),   // textSecondary
            new Color(66, 165, 245),    // accent
            new Color(46, 204, 113),    // success
            new Color(231, 76, 60),     // danger
            new Color(230, 126, 34),    // warning
            new Color(41, 98, 145)      // selection
    );

    public static Palette current() {
        return currentMode == Mode.DARK ? DARK : LIGHT;
    }

    public static Mode getMode() {
        return currentMode;
    }

    public static void setMode(Mode mode) {
        if (currentMode != mode) {
            currentMode = mode;
            notifyListeners();
        }
    }

    public static void toggle() {
        setMode(currentMode == Mode.LIGHT ? Mode.DARK : Mode.LIGHT);
    }

    /**
     * Screens/panels register here so they know to repaint when the
     * theme changes. Call Theme.addListener(this::refreshColors) once
     * per panel during setup.
     */
    public static void addListener(Runnable onChange) {
        listeners.add(onChange);
    }

    private static void notifyListeners() {
        for (Runnable r : listeners) {
            r.run();
        }
    }

    // ─── Fonts (also centralized — was hardcoded per-component before) ──

    public static Font titleFont()    { return new Font(FONT_FAMILY, Font.BOLD, 24); }
    public static Font subtitleFont() { return new Font(FONT_FAMILY, Font.PLAIN, 14); }
    public static Font headerFont()   { return new Font(FONT_FAMILY, Font.BOLD, 14); }
    public static Font cellFont()     { return new Font(FONT_FAMILY, Font.PLAIN, 14); }
    public static Font buttonFont()   { return new Font(FONT_FAMILY, Font.BOLD, 13); }
    public static Font sectionFont()  { return new Font(FONT_FAMILY, Font.BOLD, 14); }
}
