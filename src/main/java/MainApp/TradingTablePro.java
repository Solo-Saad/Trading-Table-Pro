package MainApp;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.awt.event.AWTEventListener;
import java.util.Map;
import java.sql.*;

public class TradingTablePro extends JFrame {

    // Professional color scheme
    private static final Color HEADER_BG = new Color(41, 128, 185);
    private static final Color HEADER_FG = Color.WHITE;
    private static final Color ROW_EVEN = new Color(236, 240, 241);
    private static final Color ROW_ODD = Color.WHITE;
    private static final Color HOVER_COLOR = new Color(174, 214, 241);
    private static final Color BORDER_COLOR = new Color(189, 195, 199);
    private static final Color BUTTON_BG = new Color(52, 152, 219);
    private static final Color BUTTON_HOVER = new Color(41, 128, 185);

    // Define options for each column
    private static final Map<String, String[]> columnOptions = new HashMap<>();

    static {
        columnOptions.put("Pair", new String[]{"", "AUDUSD", "USDJPY", "EURUSD", "GBPUSD", "EURJPY", "USDCADA", "USDCHF", "DXY", "EURGBP", "EURCHF", "CHFJP", "GBPJPY",  "EURCAD",  "CADJPY", "SGDJPY", "USDZAR", "CADCHF",  "GBPCAD", "GBPCHF",  "USDSGD", "GBPSGD",  "EURSGD", "GBPZAR", "EURZAR", "AUDJPY","GBPAUD", "EURAUD", "NZDUSD", "AUDNZD", "AUDCHF", "AUDCAD", "GBPNZD", "AUDSGD", "EURNZD", "NZDJPY", "NZDCAD", "NZDCHF", "USDNOK", "EURSEK", "USDSEK", "CADNOK", "CHFNOK", "EURDKK", "EURNOK", "GBPNOK", "GBPSEK", "NOKSEK", "NOKJPY", "SEKJPY", "USDDKK", "MXNJPY", "EURMXN", "GBPMXN", "USDMXN", "AUDCNH", "USDCNH", "CNHJPY", "CADCNH", "NZDCNH", "EURCNH", "GBPCNH", "NQ100", "AU200", "JPN225", "GER40", "US30", "US500", "UK100", "HK50", "FRA40", "TAIWAN", "US2000", "CHINA50", "EU50", "SPAIN35", "FANG", "NL25", "EMERGING MARKETS", "SWEDEN30", "SINGAPORE30", "SA40", "SWITZERLAND", "ETHER", "BITCOIN", "SOLANA", "USOIL", "UKOIL", "GOLD", "SILVER", "COOPER"});
        columnOptions.put("Pattern", new String[]{"", "BAT", "BUTTERFLY", "CRAB", "DEEPCRAB", "ABC", "SHARK", "ALT-BAT", "GARTLY"});
        columnOptions.put("Wave", new String[]{"", "5 Wave", "ABC"});
        columnOptions.put("Diversion", new String[]{"", "DL 1", "DL 2", "DL 3", "DL 4"});
        columnOptions.put("S/R", new String[]{"", "Structure", "FIB 0.382", "FIB 0.50", "FIB 0.618", "FIB 0.786", "FIB 0.886", "FIB 100", "FIB 1.13", "FIB 1.27", "FIB 1.44", "FIB 1.168", "FIB 2.0"});
        columnOptions.put("Direction", new String[]{"", "Bullish", "Bearish"});
        columnOptions.put("Entry signal", new String[]{"", "Double top", "Double-bottom", "Doji", "Wicks", "Wedge", "Tweezer", "Three drive", "V-shape", "Head & shoulders", "Spinning", "Triple top", "Tribottom btm", "Decending-wedge", "Ascending-wedge"});
        columnOptions.put("Outcome", new String[]{"", "Win", "Loss", "Break Even"});
    }

    private JTable table;
    private JWindow currentDropdown = null;
    private int hoveredRow = -1;
    private int hoveredCol = -1;

    public TradingTablePro() {
        setTitle("Trading Analysis Dashboard");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {

                saveTrades();

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
        mainPanel.setBackground(new Color(236, 240, 241));

        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        table = createProfessionalTable();
        loadTrades();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void loadTrades() {

        DefaultTableModel model =
                (DefaultTableModel) table.getModel();

        model.setRowCount(0);

        String sql = "SELECT * FROM trades";

        try (
                Connection conn =
                        DatabaseManager.connect();

                Statement stmt =
                        conn.createStatement();

                ResultSet rs =
                        stmt.executeQuery(sql)
        ) {

            while (rs.next()) {

                model.addRow(new Object[]{

                        rs.getString("pair"),
                        rs.getString("pattern"),
                        rs.getString("wave"),
                        rs.getString("diversion"),
                        rs.getString("sr"),
                        rs.getString("direction"),
                        rs.getString("entrySignal"),
                        rs.getString("outcome")
                });
            }

        } catch (Exception e) {

        JOptionPane.showMessageDialog(
                this,
                "Failed to load saved trades.\n\n" + e.getMessage(),
                "Load Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
    }

    private void saveTrades() {

        DefaultTableModel model = (DefaultTableModel) table.getModel();

        String deleteSQL = "DELETE FROM trades";

        String insertSQL = """
        INSERT INTO trades (
            pair, pattern, wave, diversion, sr,
            direction, entrySignal, outcome
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (
                Connection conn = DatabaseManager.connect();
                Statement deleteStmt = conn.createStatement();
                PreparedStatement pstmt = conn.prepareStatement(insertSQL)
        ) {

            conn.setAutoCommit(false); // ← 1. START: turn off auto-commit before touching anything

            try {

                deleteStmt.executeUpdate(deleteSQL); // ← 2. Delete old data

                for (int row = 0; row < model.getRowCount(); row++) {

                    boolean hasData = false;
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        Object value = model.getValueAt(row, col);
                        if (value != null && !value.toString().trim().isEmpty()) {
                            hasData = true;
                            break;
                        }
                    }

                    if (!hasData) continue;

                    pstmt.setString(1, getCellValue(model, row, 0));
                    pstmt.setString(2, getCellValue(model, row, 1));
                    pstmt.setString(3, getCellValue(model, row, 2));
                    pstmt.setString(4, getCellValue(model, row, 3));
                    pstmt.setString(5, getCellValue(model, row, 4));
                    pstmt.setString(6, getCellValue(model, row, 5));
                    pstmt.setString(7, getCellValue(model, row, 6));
                    pstmt.setString(8, getCellValue(model, row, 7));

                    pstmt.executeUpdate();
                }

                conn.commit(); // ← 3. SUCCESS: everything worked, write it for real

                JOptionPane.showMessageDialog(this,
                        "Trades saved successfully!",
                        "Save Complete",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception innerEx) {

                conn.rollback(); // ← 4. FAILURE: something broke mid-way, undo everything

                JOptionPane.showMessageDialog(this,
                        "Save failed — your data has NOT been changed.\n" + innerEx.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getCellValue(DefaultTableModel model,
                                int row,
                                int col) {

        Object value = model.getValueAt(row, col);

        return value != null
                ? value.toString()
                : "";
    }

    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(236, 240, 241));

        JLabel titleLabel = new JLabel("Trading Analysis Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(44, 62, 80));

        JLabel subtitleLabel = new JLabel("Click on cells to select values");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(127, 140, 141));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setBackground(new Color(236, 240, 241));
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        titlePanel.add(textPanel, BorderLayout.WEST);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        return titlePanel;
    }

    private JTable createProfessionalTable() {
        String[] columnNames = {"Pair", "Pattern", "Wave", "Diversion", "S/R", "Direction", "Entry signal", "outcome"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 20) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        JTable table = new JTable(model);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setGridColor(BORDER_COLOR);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(header.getWidth(), 45));
        header.setBorder(BorderFactory.createEmptyBorder());

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            private final Color[] headerColors = {
                    new Color(231, 76, 60),
                    new Color(52, 152, 219),
                    new Color(46, 204, 113),
                    new Color(155, 89, 182),
                    new Color(230, 126, 34),
                    new Color(100, 100, 20),
                    new Color(26, 188, 156),
                    new Color(241, 196, 15)
            };

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(headerColors[column]);
                setForeground(Color.WHITE);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 1, Color.WHITE),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
                setFont(new Font("Segoe UI", Font.BOLD, 14));
                return this;
            }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                boolean isHovered = (row == hoveredRow && column == hoveredCol);

                if (isSelected) {
                    c.setBackground(new Color(52, 152, 219));
                    c.setForeground(Color.WHITE);
                } else if (isHovered) {
                    c.setBackground(HOVER_COLOR);
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                    c.setForeground(new Color(44, 62, 80));
                }

                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                setHorizontalAlignment(SwingConstants.LEFT);
                return c;
            }
        });

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row != hoveredRow || col != hoveredCol) {
                    hoveredRow = row;
                    hoveredCol = col;
                    table.repaint();
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                hoveredCol = -1;
                table.repaint();
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col >= 0) {
                    showDropdownMenu(row, col, e.getX(), e.getY());
                }
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(130);
        table.getColumnModel().getColumn(6).setPreferredWidth(120);

        return table;
    }

//    private JWindow currentDropdown = null;

    private void showDropdownMenu(int row, int col, int x, int y) {
        closeDropdown(); // close any existing dropdown

        String columnName = table.getColumnName(col);
        String[] options = columnOptions.get(columnName);
        if (options == null) return;

        JWindow dropdown = new JWindow(this);
        currentDropdown = dropdown;

        JPanel itemPanel = new JPanel();
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));
        itemPanel.setBackground(Color.WHITE);
        itemPanel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        for (String option : options) {
            String label = option.isEmpty() ? "-- Clear --" : option;
            JPanel itemRow = new JPanel(new BorderLayout());
            itemRow.setBackground(Color.WHITE);
            itemRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            itemRow.setPreferredSize(new Dimension(200, 32));
            itemRow.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

            JLabel itemLabel = new JLabel(label);
            itemLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            itemRow.add(itemLabel, BorderLayout.CENTER);

            // Hover effect
            itemRow.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    itemRow.setBackground(HOVER_COLOR);
                    itemLabel.setBackground(HOVER_COLOR);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    itemRow.setBackground(Color.WHITE);
                    itemLabel.setBackground(Color.WHITE);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    table.setValueAt(option, row, col);
                    closeDropdown();
                }
            });

            itemPanel.add(itemRow);
        }

        int MAX_VISIBLE = 8;
        int ITEM_HEIGHT = 32;
        int panelWidth  = 210;

        JScrollPane scrollPane = new JScrollPane(itemPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        int visibleHeight = Math.min(options.length, MAX_VISIBLE) * ITEM_HEIGHT;
        scrollPane.setPreferredSize(new Dimension(panelWidth, visibleHeight));

        dropdown.add(scrollPane);
        dropdown.pack();

        // Position it below the clicked cell
        Rectangle cellRect = table.getCellRect(row, col, false);
        Point cellOnScreen = table.getLocationOnScreen();
        dropdown.setLocation(
                cellOnScreen.x + cellRect.x,
                cellOnScreen.y + cellRect.y + cellRect.height
        );

        dropdown.setVisible(true);

        AWTEventListener dropdownListener = null;

        // Close if user clicks anywhere outside
        dropdownListener = new AWTEventListener() {

            @Override
            public void eventDispatched(AWTEvent event) {

                if (event instanceof MouseEvent me
                        && me.getID() == MouseEvent.MOUSE_PRESSED) {

                    if (!dropdown.getBounds().contains(me.getLocationOnScreen())) {
                        closeDropdown();
                    }
                }
            }
        };

        Toolkit.getDefaultToolkit().addAWTEventListener(
                dropdownListener,
                AWTEvent.MOUSE_EVENT_MASK
        );
    }

    private AWTEventListener dropdownListener = null;

    private void closeDropdown() {

        if (currentDropdown != null) {
            currentDropdown.dispose();
            currentDropdown = null;
        }

        if (dropdownListener != null) {

            Toolkit.getDefaultToolkit()
                    .removeAWTEventListener(dropdownListener);

            dropdownListener = null;
        }
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(new Color(236, 240, 241));

        JButton addRowButton    = createStyledButton("➕ Add Row",          new Color(46, 204, 113));
        JButton removeRowButton = createStyledButton("➖ Remove Last Row",   new Color(231, 76, 60));
        JButton clearButton     = createStyledButton("🗑 Clear All",         new Color(230, 126, 34));
        JButton saveButton    = createStyledButton("💾 Save Data",       BUTTON_BG);
        JButton analyseButton   = createStyledButton("📊 Analyse Trades",    new Color(142, 68, 173));

        addRowButton.addActionListener(e -> {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.addRow(new Object[model.getColumnCount()]);
        });

        removeRowButton.addActionListener(e -> {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            if (model.getRowCount() > 0) {
                model.removeRow(model.getRowCount() - 1);
            }
        });

        clearButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to clear all data?",
                    "Confirm Clear",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0);
                model.setRowCount(20);
            }
        });

        saveButton.addActionListener(e -> saveTrades());
        analyseButton.addActionListener(e -> analyseTradeOutcomes());

        buttonPanel.add(addRowButton);
        buttonPanel.add(removeRowButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(analyseButton);

        return buttonPanel;
    }

    // ─── TRADE ANALYSIS ──────────────────────────────────────────────────────────

    /**
     * Scans the table and computes:
     *  - Win / Loss / Break-Even counts and the W:L ratio
     *  - Bullish / Bearish counts and the Bull:Bear ratio
     * Then displays a formatted summary dialog.
     */
    private void analyseTradeOutcomes() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        // Locate the columns we need by name (order-independent)
        int outcomeCol   = -1;
        int directionCol = -1;
        for (int c = 0; c < model.getColumnCount(); c++) {
            String name = model.getColumnName(c);
            if (name.equalsIgnoreCase("outcome"))   outcomeCol   = c;
            if (name.equalsIgnoreCase("Direction")) directionCol = c;
        }

        if (outcomeCol == -1 || directionCol == -1) {
            JOptionPane.showMessageDialog(this,
                    "Required columns (outcome / Direction) not found.",
                    "Analysis Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Tally counters
        int wins       = 0;
        int losses     = 0;
        int breakEvens = 0;
        int bullish    = 0;
        int bearish    = 0;
        int totalRows  = 0; // rows that have at least one value filled

        for (int r = 0; r < model.getRowCount(); r++) {
            Object outcomeVal   = model.getValueAt(r, outcomeCol);
            Object directionVal = model.getValueAt(r, directionCol);

            String outcome   = (outcomeVal   != null) ? outcomeVal.toString().trim()   : "";
            String direction = (directionVal != null) ? directionVal.toString().trim() : "";

            // Skip completely empty rows
            boolean rowHasData = false;
            for (int c = 0; c < model.getColumnCount(); c++) {
                Object v = model.getValueAt(r, c);
                if (v != null && !v.toString().trim().isEmpty()) {
                    rowHasData = true;
                    break;
                }
            }
            if (!rowHasData) continue;
            totalRows++;

            switch (outcome) {
                case "Win":        wins++;       break;
                case "Loss":       losses++;     break;
                case "Break Even": breakEvens++; break;
            }

            switch (direction) {
                case "Bullish": bullish++; break;
                case "Bearish": bearish++; break;
            }
        }

        if (totalRows == 0) {
            JOptionPane.showMessageDialog(this,
                    "No trade data found. Fill in some rows first.",
                    "Nothing to Analyse", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ── Ratio calculations ────────────────────────────────────────────────
        // Win : Loss ratio  (expressed as X : 1)
        String wlRatio;
        if (losses == 0 && wins == 0) {
            wlRatio = "N/A (no wins or losses recorded)";
        } else if (losses == 0) {
            wlRatio = wins + " : 0  (no losses — perfect run! 🎯)";
        } else if (wins == 0) {
            wlRatio = "0 : " + losses + "  (no wins recorded)";
        } else {
            double ratio = (double) wins / losses;
            wlRatio = String.format("%d : %d  (%.2f : 1)", wins, losses, ratio);
        }

        // Bullish : Bearish ratio
        String bbRatio;
        if (bullish == 0 && bearish == 0) {
            bbRatio = "N/A (no direction data recorded)";
        } else if (bearish == 0) {
            bbRatio = bullish + " : 0  (all bullish)";
        } else if (bullish == 0) {
            bbRatio = "0 : " + bearish + "  (all bearish)";
        } else {
            double ratio = (double) bullish / bearish;
            bbRatio = String.format("%d : %d  (%.2f : 1)", bullish, bearish, ratio);
        }

        // Win-rate percentage (excludes break-evens from denominator — common trader convention)
        String winRatePct = "N/A";
        if (wins + losses > 0) {
            double pct = (double) wins / (wins + losses) * 100.0;
            winRatePct = String.format("%.1f%%", pct);
        }

        // ── Build the display panel ───────────────────────────────────────────
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 6, 6, 6);

        Font labelFont  = new Font("Segoe UI", Font.BOLD,  13);
        Font valueFont  = new Font("Segoe UI", Font.PLAIN, 13);
        Font sectionFont = new Font("Segoe UI", Font.BOLD, 14);

        int row = 0;

        // Section: Summary
        addSectionHeader(panel, gbc, row++, "📋  Summary", sectionFont);
        addRow(panel, gbc, row++, "Total trade rows analysed:", String.valueOf(totalRows),           labelFont, valueFont);

        // Section: Outcomes
        addSectionHeader(panel, gbc, row++, "🏆  Outcomes", sectionFont);
        addRow(panel, gbc, row++, "Wins:",        String.valueOf(wins),       labelFont, valueFont);
        addRow(panel, gbc, row++, "Losses:",      String.valueOf(losses),     labelFont, valueFont);
        addRow(panel, gbc, row++, "Break Evens:", String.valueOf(breakEvens), labelFont, valueFont);
        addRow(panel, gbc, row++, "Win Rate (excl. B/E):", winRatePct,        labelFont, new Font("Segoe UI", Font.BOLD, 13));
        addRow(panel, gbc, row++, "Win : Loss Ratio:", wlRatio,               labelFont, valueFont);

        // Section: Direction
        addSectionHeader(panel, gbc, row++, "📈  Trade Direction", sectionFont);
        addRow(panel, gbc, row++, "Bullish trades:", String.valueOf(bullish), labelFont, valueFont);
        addRow(panel, gbc, row++, "Bearish trades:", String.valueOf(bearish), labelFont, valueFont);
        addRow(panel, gbc, row++, "Bull : Bear Ratio:", bbRatio,              labelFont, valueFont);

        JOptionPane.showMessageDialog(this, panel,
                "Trade Outcome Analysis", JOptionPane.PLAIN_MESSAGE);
    }

    /** Adds a bold, coloured section header spanning both columns. */
    private void addSectionHeader(JPanel panel, GridBagConstraints gbc, int row, String text, Font font) {
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(14, 6, 2, 6);

        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(new Color(41, 128, 185));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(189, 195, 199));

        JPanel sectionPanel = new JPanel(new BorderLayout(0, 2));
        sectionPanel.setBackground(Color.WHITE);
        sectionPanel.add(lbl, BorderLayout.NORTH);
        sectionPanel.add(sep, BorderLayout.SOUTH);

        panel.add(sectionPanel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(6, 6, 6, 6);
    }

    /** Adds a label + value pair on one row. */
    private void addRow(JPanel panel, GridBagConstraints gbc, int row,
                        String label, String value, Font labelFont, Font valueFont) {
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lbl = new JLabel(label);
        lbl.setFont(labelFont);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        JLabel val = new JLabel(value);
        val.setFont(valueFont);
        panel.add(val, gbc);
    }

    // ─── EXISTING HELPERS ─────────────────────────────────────────────────────

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(180, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { button.setBackground(bgColor.darker()); }
            @Override
            public void mouseExited(MouseEvent e)  { button.setBackground(bgColor); }
        });

        return button;
    }

    public static void main(String[] args) {

        DatabaseManager.initializeDatabase();

        SwingUtilities.invokeLater(() -> {
            TradingTablePro frame = new TradingTablePro();
            frame.setVisible(true);
        });
    }
}