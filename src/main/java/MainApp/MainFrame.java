package MainApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Application shell. Owns the window itself (size, icon, close
 * behavior) and the title panel. Screen content used to live directly
 * in here — it's been moved out to TradeTablePanel so this class can
 * stay small as more screens (Dashboard, Analytics, Journal, Calendar,
 * Settings) get added in Phase 2/3.
 */
public class MainFrame extends JFrame {

    private TradeTablePanel tradeTablePanel;

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
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Theme.current().background);

        mainPanel.add(createTitlePanel(), BorderLayout.NORTH);

        tradeTablePanel = new TradeTablePanel();
        mainPanel.add(tradeTablePanel, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Theme.current().background);

        JLabel titleLabel = new JLabel("Trading Analysis Dashboard");
        titleLabel.setFont(Theme.titleFont());
        titleLabel.setForeground(Theme.current().textPrimary);

        JLabel subtitleLabel = new JLabel("Click on cells to select values");
        subtitleLabel.setFont(Theme.subtitleFont());
        subtitleLabel.setForeground(Theme.current().textSecondary);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setBackground(Theme.current().background);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        titlePanel.add(textPanel, BorderLayout.WEST);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        return titlePanel;
    }

    public static void main(String[] args) {
        DatabaseManager.initializeDatabase();

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
