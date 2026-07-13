package MainApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;

/**
 * Application shell. Every screen is built exactly once, here, at
 * startup -- never torn down and recreated. Theme toggling calls
 * refreshTheme() on each panel instead of rebuilding, so unsaved
 * edits (e.g. on the Trades screen) survive a theme change. Theme
 * mode itself is persisted to app_settings so it's remembered
 * across restarts.
 */
public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    private Sidebar sidebar;
    private TopBar topBar;
    private TradeTablePanel tradeTablePanel;
    private DashboardPanel dashboardPanel;
    private AnalyticsPanel analyticsPanel;
    private JournalPanel journalPanel;
    private CalendarPanel calendarPanel;
    private SettingsPanel settingsPanel;

    public MainFrame() {
        setTitle("Trading Analysis Dashboard");

        try {
            ImageIcon appIcon = new ImageIcon(getClass().getResource("/Trades.png"));
            setIconImage(appIcon.getImage());
        } catch (Exception e) {
            e.printStackTrace();
        }

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                tradeTablePanel.saveOnExit();
                dispose();
                System.exit(0);
            }
        });

        setSize(1400, 800);
        setMinimumSize(new Dimension(1000, 650));
        setResizable(true);
        setLocationRelativeTo(null);

        loadPersistedTheme();
        applyLookAndFeel();

        buildShellOnce();

        Theme.addListener(this::onThemeChanged);
    }

    private void loadPersistedTheme() {
        try (Connection conn = DatabaseManager.connect()) {
            String saved = DatabaseManager.getSetting(conn, "theme", "LIGHT");
            Theme.setMode(Theme.Mode.valueOf(saved));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyLookAndFeel() {
        try {
            if (Theme.getMode() == Theme.Mode.DARK) {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
            }
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Builds the whole window content ONCE. Never called again. */
    private void buildShellOnce() {
        setLayout(new BorderLayout());

        sidebar = new Sidebar(this::showCard);
        add(sidebar, BorderLayout.WEST);

        topBar = new TopBar();

        tradeTablePanel = new TradeTablePanel();
        dashboardPanel  = new DashboardPanel();
        analyticsPanel  = new AnalyticsPanel();
        journalPanel    = new JournalPanel();
        calendarPanel   = new CalendarPanel();
        settingsPanel   = new SettingsPanel(name -> topBar.setProfileName(name));

        cardPanel.add(dashboardPanel, "Dashboard");
        cardPanel.add(tradeTablePanel, "Trades");
        cardPanel.add(analyticsPanel, "Analytics");
        cardPanel.add(journalPanel, "Journal");
        cardPanel.add(calendarPanel, "Calendar");
        cardPanel.add(settingsPanel, "Settings");
        cardPanel.setBackground(Theme.current().background);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(topBar, BorderLayout.NORTH);
        centerPanel.add(cardPanel, BorderLayout.CENTER);
        centerPanel.setBackground(Theme.current().background);

        add(centerPanel, BorderLayout.CENTER);

        cardLayout.show(cardPanel, "Dashboard");
    }

    /** Called whenever Theme.toggle() fires. Recolors in place, persists
     *  the choice, and touches nothing that holds unsaved user data. */
    private void onThemeChanged() {
        try (Connection conn = DatabaseManager.connect()) {
            DatabaseManager.setSetting(conn, "theme", Theme.getMode().name());
        } catch (Exception e) {
            e.printStackTrace();
        }

        applyLookAndFeel();

        sidebar.refreshTheme();
        topBar.refreshTheme();
        tradeTablePanel.refreshTheme();
        dashboardPanel.refreshTheme();
        analyticsPanel.refreshTheme();
        journalPanel.refreshTheme();
        calendarPanel.refreshTheme();
        settingsPanel.refreshTheme();

        getContentPane().setBackground(Theme.current().background);
        cardPanel.setBackground(Theme.current().background);
        revalidate();
        repaint();
    }

    private void showCard(String name) {
        cardLayout.show(cardPanel, name);
    }

    public static void main(String[] args) {
        DatabaseManager.initializeDatabase();

        SwingUtilities.invokeLater(() -> {
            // Load the persisted theme and install FlatLaf BEFORE any
            // frame is created. setDefaultLookAndFeelDecorated must run
            // before the JFrame's own constructor -- otherwise Windows
            // draws the title bar natively, and it won't follow the
            // app's light/dark toggle (that's the "still black" bug).
            try (Connection conn = DatabaseManager.connect()) {
                String saved = DatabaseManager.getSetting(conn, "theme", "LIGHT");
                Theme.setMode(Theme.Mode.valueOf(saved));
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                if (Theme.getMode() == Theme.Mode.DARK) {
                    UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
                } else {
                    UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            JFrame.setDefaultLookAndFeelDecorated(true);

            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}