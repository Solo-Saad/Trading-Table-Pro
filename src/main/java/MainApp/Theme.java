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

    // Warm, soft-card design language: cream backgrounds, white cards,
    // violet accent -- matching the reference look the person shared.
    private static final Palette LIGHT = new Palette(
            new Color(247, 245, 240),   // background -- warm cream
            Color.WHITE,                 // surface -- card white
            new Color(35, 32, 28),       // headerBg -- warm near-black
            Color.WHITE,                 // headerFg
            new Color(250, 248, 244),    // rowEven
            Color.WHITE,                 // rowOdd
            new Color(238, 234, 250),    // hover -- soft lavender tint
            new Color(230, 226, 218),    // border -- warm soft gray
            new Color(32, 29, 25),       // textPrimary -- warm near-black
            new Color(128, 122, 112),    // textSecondary -- warm gray
            new Color(109, 90, 226),     // accent -- violet
            new Color(34, 197, 94),      // success
            new Color(239, 68, 68),      // danger
            new Color(245, 158, 11),     // warning
            new Color(109, 90, 226)      // selection
    );

    private static final Palette DARK = new Palette(
            new Color(24, 22, 20),       // background -- warm near-black
            new Color(34, 32, 29),       // surface
            new Color(20, 18, 16),       // headerBg
            new Color(235, 232, 225),    // headerFg
            new Color(34, 32, 29),       // rowEven
            new Color(40, 37, 34),       // rowOdd
            new Color(52, 46, 68),       // hover -- soft violet tint
            new Color(55, 52, 47),       // border
            new Color(235, 232, 225),    // textPrimary -- warm off-white
            new Color(158, 152, 142),    // textSecondary
            new Color(148, 130, 245),    // accent -- lighter violet for dark contrast
            new Color(52, 211, 133),     // success
            new Color(248, 113, 113),    // danger
            new Color(251, 191, 36),     // warning
            new Color(148, 130, 245)     // selection
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

    /** Blends two colors -- ratio 1.0 = fully `a`, 0.0 = fully `b`.
     *  Used for tinted badges/highlights (e.g. a soft accent-tinted
     *  pill background) without needing a dedicated palette entry
     *  for every possible tint. */
    public static Color mix(Color a, Color b, float ratio) {
        int r = (int) (a.getRed() * ratio + b.getRed() * (1 - ratio));
        int g = (int) (a.getGreen() * ratio + b.getGreen() * (1 - ratio));
        int bl = (int) (a.getBlue() * ratio + b.getBlue() * (1 - ratio));
        return new Color(r, g, bl);
    }
}