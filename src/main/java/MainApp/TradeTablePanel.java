package MainApp;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.*;

/**
 * The "Trades" screen: the editable trade table, its dropdown cell
 * editors, and the action buttons (add/remove/clear/save/analyse).
 * Extracted out of the old TradingTablePro god-class — behavior is
 * unchanged from before, just relocated.
 */
public class TradeTablePanel extends JPanel {

    private static final Map<String, String[]> columnOptions = new HashMap<>();

    static {
        columnOptions.put("Pair", new String[]{"", "AUDUSD", "USDJPY", "EURUSD", "GBPUSD", "EURJPY", "USDCADA", "USDCHF", "DXY", "EURGBP", "EURCHF", "CHFJP", "GBPJPY", "EURCAD", "CADJPY", "SGDJPY", "USDZAR", "CADCHF", "GBPCAD", "GBPCHF", "USDSGD", "GBPSGD", "EURSGD", "GBPZAR", "EURZAR", "AUDJPY", "GBPAUD", "EURAUD", "NZDUSD", "AUDNZD", "AUDCHF", "AUDCAD", "GBPNZD", "AUDSGD", "EURNZD", "NZDJPY", "NZDCAD", "NZDCHF", "USDNOK", "EURSEK", "USDSEK", "CADNOK", "CHFNOK", "EURDKK", "EURNOK", "GBPNOK", "GBPSEK", "NOKSEK", "NOKJPY", "SEKJPY", "USDDKK", "MXNJPY", "EURMXN", "GBPMXN", "USDMXN", "AUDCNH", "USDCNH", "CNHJPY", "CADCNH", "NZDCNH", "EURCNH", "GBPCNH", "NQ100", "AU200", "JPN225", "GER40", "US30", "US500", "UK100", "HK50", "FRA40", "TAIWAN", "US2000", "CHINA50", "EU50", "SPAIN35", "FANG", "NL25", "EMERGING MARKETS", "SWEDEN30", "SINGAPORE30", "SA40", "SWITZERLAND", "ETHER", "BITCOIN", "SOLANA", "USOIL", "UKOIL", "GOLD", "SILVER", "COOPER"});
        columnOptions.put("Pattern", new String[]{"", "BAT", "BUTTERFLY", "CRAB", "DEEPCRAB", "ABC", "SHARK", "ALT-BAT", "GARTLY"});
        columnOptions.put("Wave", new String[]{"", "5 Wave", "ABC"});
        columnOptions.put("Diversion", new String[]{"", "DL 1", "DL 2", "DL 3", "DL 4"});
        columnOptions.put("S/R", new String[]{"", "Structure", "FIB 0.382", "FIB 0.50", "FIB 0.618", "FIB 0.786", "FIB 0.886", "FIB 100", "FIB 1.13", "FIB 1.27", "FIB 1.44", "FIB 1.168", "FIB 2.0"});
        columnOptions.put("Direction", new String[]{"", "Bullish", "Bearish"});
        columnOptions.put("Entry signal", new String[]{"", "Double top", "Double-bottom", "Doji", "Wicks", "Wedge", "Tweezer", "Three drive", "V-shape", "Head & shoulders", "Spinning", "Triple top", "Tribottom btm", "Decending-wedge", "Ascending-wedge"});
        columnOptions.put("outcome", new String[]{"", "Win", "Loss", "Break Even"});
    }

    private JTable table;
    private JScrollPane scrollPane;
    private JPanel buttonPanel;
    private JButton addRowButton, removeRowButton, clearButton, saveButton;
    private JWindow currentDropdown = null;
    private AWTEventListener dropdownListener = null;
    private int hoveredRow = -1;
    private int hoveredCol = -1;
    private final List<Integer> deletedIds = new ArrayList<>();

    private boolean isLoading = false;
    private boolean dirty = false;
    private Timer autosaveTimer;

    private static final int AUTOSAVE_INTERVAL_MS = 10_000;

    public TradeTablePanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.current().background);

        table = createProfessionalTable();
        loadTrades();

        table.getModel().addTableModelListener(e -> {
            if (!isLoading) dirty = true;
        });

        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.current().border, 1));
        scrollPane.getViewport().setBackground(Theme.current().surface);
        add(scrollPane, BorderLayout.CENTER);

        buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.NORTH);

        autosaveTimer = new Timer(AUTOSAVE_INTERVAL_MS, e -> {
            if (dirty) saveTrades();
        });
        autosaveTimer.start();
    }

    /** Re-applies current Theme colors to existing components without
     *  rebuilding the table model -- unsaved edits are preserved. */
    public void refreshTheme() {
        setBackground(Theme.current().background);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.current().border, 1));
        scrollPane.getViewport().setBackground(Theme.current().surface);

        addRowButton.setBackground(Theme.current().success);
        removeRowButton.setBackground(Theme.current().danger);
        clearButton.setBackground(Theme.current().warning);
        saveButton.setBackground(Theme.current().accent);
        buttonPanel.setBackground(Theme.current().background);

        table.getTableHeader().repaint();
        table.repaint();
        revalidate();
        repaint();
    }

    /** Called by MainFrame when the window is closing. */
    public void saveOnExit() {
        saveTrades();
    }

    private void loadTrades() {
        isLoading = true;
        deletedIds.clear();

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        String sql = "SELECT * FROM trades";

        try (
                Connection conn = DatabaseManager.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)
        ) {
            while (rs.next()) {
                String storedDate = rs.getString("trade_date");
                model.addRow(new Object[]{
                        rs.getString("pair"),
                        rs.getString("pattern"),
                        rs.getString("wave"),
                        rs.getString("diversion"),
                        rs.getString("sr"),
                        rs.getString("direction"),
                        rs.getString("entrySignal"),
                        rs.getString("outcome"),
                        (storedDate == null || storedDate.isBlank()) ? "" : storedDate,
                        rs.getInt("id")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to load saved trades.\n\n" + e.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE
            );
        } finally {
            isLoading = false;
            dirty = false;
        }
    }

    private void saveTrades() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        try (Connection conn = DatabaseManager.connect()) {
            conn.setAutoCommit(false);

            try {
                for (int row = 0; row < model.getRowCount(); row++) {

                    boolean hasData = false;
                    for (int col = 0; col < 8; col++) {
                        Object value = model.getValueAt(row, col);
                        if (value != null && !value.toString().trim().isEmpty()) {
                            hasData = true;
                            break;
                        }
                    }

                    Object idVal = model.getValueAt(row, 9);

                    if (!hasData) {
                        if (idVal != null) deletedIds.add((Integer) idVal);
                        continue;
                    }

                    String pair        = getCellValue(model, row, 0);
                    String pattern     = getCellValue(model, row, 1);
                    String wave        = getCellValue(model, row, 2);
                    String diversion   = getCellValue(model, row, 3);
                    String sr          = getCellValue(model, row, 4);
                    String direction   = getCellValue(model, row, 5);
                    String entrySignal = getCellValue(model, row, 6);
                    String outcome     = getCellValue(model, row, 7);
                    String tradeDate   = getCellValue(model, row, 8);

                    if (idVal == null) {
                        int newId = DatabaseManager.insertTrade(conn, pair, pattern, wave,
                                diversion, sr, direction, entrySignal, outcome, tradeDate);
                        model.setValueAt(newId, row, 9);
                        if (tradeDate.isBlank()) {
                            model.setValueAt(java.time.LocalDate.now().toString(), row, 8);
                        }
                    } else {
                        DatabaseManager.updateTrade(conn, (Integer) idVal, pair, pattern, wave,
                                diversion, sr, direction, entrySignal, outcome, tradeDate);
                    }
                }

                DatabaseManager.deleteTrades(conn, deletedIds);
                deletedIds.clear();

                conn.commit();
                dirty = false;

            } catch (Exception innerEx) {
                conn.rollback();
                JOptionPane.showMessageDialog(this,
                        "Save failed — your data has NOT been changed.\n" + innerEx.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getCellValue(DefaultTableModel model, int row, int col) {
        Object value = model.getValueAt(row, col);
        return value != null ? value.toString() : "";
    }

    private JTable createProfessionalTable() {
        String[] columnNames = {"Pair", "Pattern", "Wave", "Diversion", "S/R", "Direction", "Entry signal", "outcome", "Date", "id"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        JTable table = new JTable(model);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(40);
        table.setFont(Theme.cellFont());
        table.setGridColor(Theme.current().border);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));

        // id column stays in the model (needed for update/delete) but hidden from view
        table.getColumnModel().removeColumn(table.getColumnModel().getColumn(9));

        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.headerFont());
        header.setPreferredSize(new Dimension(header.getWidth(), 45));
        header.setBorder(BorderFactory.createEmptyBorder());

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(Theme.current().headerBg);
                setForeground(Theme.current().headerFg);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 1, Color.WHITE),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
                setFont(Theme.headerFont());
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
                    c.setBackground(Theme.current().selection);
                    c.setForeground(Color.WHITE);
                } else if (isHovered) {
                    c.setBackground(Theme.current().hover);
                    c.setForeground(Theme.current().textPrimary);
                } else {
                    c.setBackground(row % 2 == 0 ? Theme.current().rowEven : Theme.current().rowOdd);
                    c.setForeground(Theme.current().textPrimary);
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
        table.getColumnModel().getColumn(8).setPreferredWidth(100);

        return table;
    }

    private void showDropdownMenu(int row, int col, int x, int y) {
        closeDropdown();

        String columnName = table.getColumnName(col);
        String[] options = columnOptions.get(columnName);
        if (options == null) return;

        JWindow dropdown = new JWindow(SwingUtilities.getWindowAncestor(this));
        currentDropdown = dropdown;

        JPanel itemPanel = new JPanel();
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));
        itemPanel.setBackground(Theme.current().surface);
        itemPanel.setBorder(BorderFactory.createLineBorder(Theme.current().border, 1));

        for (String option : options) {
            String label = option.isEmpty() ? "-- Clear --" : option;
            JPanel itemRow = new JPanel(new BorderLayout());
            itemRow.setBackground(Theme.current().surface);
            itemRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            itemRow.setPreferredSize(new Dimension(200, 32));
            itemRow.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

            JLabel itemLabel = new JLabel(label);
            itemLabel.setFont(Theme.cellFont());
            itemRow.add(itemLabel, BorderLayout.CENTER);

            itemRow.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    itemRow.setBackground(Theme.current().hover);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    itemRow.setBackground(Theme.current().surface);
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
        int panelWidth = 210;

        JScrollPane scrollPane = new JScrollPane(itemPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        int visibleHeight = Math.min(options.length, MAX_VISIBLE) * ITEM_HEIGHT;
        scrollPane.setPreferredSize(new Dimension(panelWidth, visibleHeight));

        dropdown.add(scrollPane);
        dropdown.pack();

        Rectangle cellRect = table.getCellRect(row, col, false);
        Point cellOnScreen = table.getLocationOnScreen();
        dropdown.setLocation(
                cellOnScreen.x + cellRect.x,
                cellOnScreen.y + cellRect.y + cellRect.height
        );

        dropdown.setVisible(true);

        dropdownListener = event -> {
            if (event instanceof MouseEvent me && me.getID() == MouseEvent.MOUSE_PRESSED) {
                if (!dropdown.getBounds().contains(me.getLocationOnScreen())) {
                    closeDropdown();
                }
            }
        };

        Toolkit.getDefaultToolkit().addAWTEventListener(dropdownListener, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void closeDropdown() {
        if (currentDropdown != null) {
            currentDropdown.dispose();
            currentDropdown = null;
        }
        if (dropdownListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(dropdownListener);
            dropdownListener = null;
        }
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        buttonPanel.setBackground(Theme.current().background);

        addRowButton    = createStyledButton("Add Row", Theme.current().success);
        removeRowButton = createStyledButton("Remove Last Row", Theme.current().danger);
        clearButton     = createStyledButton("Clear All", Theme.current().warning);
        saveButton      = createStyledButton("Save Data", Theme.current().accent);

        addRowButton.addActionListener(e -> {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.addRow(new Object[model.getColumnCount()]);
        });

        removeRowButton.addActionListener(e -> {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            if (model.getRowCount() > 0) {
                int lastRow = model.getRowCount() - 1;
                Object idVal = model.getValueAt(lastRow, 9);
                if (idVal != null) deletedIds.add((Integer) idVal);
                model.removeRow(lastRow);
            }
        });

        clearButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to clear all data?",
                    "Confirm Clear",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                for (int r = 0; r < model.getRowCount(); r++) {
                    Object idVal = model.getValueAt(r, 9);
                    if (idVal != null) deletedIds.add((Integer) idVal);
                }
                model.setRowCount(0);
            }
        });

        saveButton.addActionListener(e -> saveTrades());

        buttonPanel.add(addRowButton);
        buttonPanel.add(removeRowButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(saveButton);

        return buttonPanel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(Theme.buttonFont());
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
}