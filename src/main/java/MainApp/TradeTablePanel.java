package MainApp;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.sql.*;

/**
 * The "Trades" screen: the editable trade table, its dropdown cell
 * editors, per-row delete, live search, and the action buttons.
 */
public class TradeTablePanel extends JPanel {

    private static final Map<String, String[]> columnOptions = new HashMap<>();
    private static final int DELETE_COL = 10; // last visible column, model index

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
    private TableRowSorter<DefaultTableModel> sorter;
    private RoundedPanel tableCard;
    private RoundedPanel toolbarCard;
    private JScrollPane scrollPane;
    private JPanel buttonPanel;
    private SearchField searchField;
    private JLabel saveStatusLabel;
    private PillButton addRowButton, saveButton;
    private PillButton clearButton;
    private JWindow currentDropdown = null;
    private AWTEventListener dropdownListener = null;
    private int hoveredRow = -1;
    private int hoveredCol = -1;
    private final List<Integer> deletedIds = new ArrayList<>();

    private boolean isLoading = false;
    private boolean dirty = false;
    private Timer autosaveTimer;

    private static final int AUTOSAVE_INTERVAL_MS = 10_000;
    private static final int STARTER_ROW_COUNT = 5;

    public TradeTablePanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Theme.current().background);

        table = createProfessionalTable();
        loadTrades();

        table.getModel().addTableModelListener(e -> {
            if (!isLoading) {
                dirty = true;
                updateSaveStatus();
            }
        });

        tableCard = new RoundedPanel(new BorderLayout());
        tableCard.cornerRadius(16).showBorder(true).showShadow(true);
        tableCard.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Theme.current().surface);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        RoundedPanel toolbarCard = new RoundedPanel(new BorderLayout(12, 0));
        this.toolbarCard = toolbarCard;
        toolbarCard.cornerRadius(14).showBorder(true).showShadow(false);
        toolbarCard.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        buttonPanel = createButtonPanel();
        toolbarCard.add(buttonPanel, BorderLayout.WEST);
        toolbarCard.add(createControlsRow(), BorderLayout.EAST);

        add(toolbarCard, BorderLayout.NORTH);

        autosaveTimer = new Timer(AUTOSAVE_INTERVAL_MS, e -> {
            if (dirty) saveTrades();
        });
        autosaveTimer.start();

        updateSaveStatus();
    }

    /** Re-applies current Theme colors to existing components without
     *  rebuilding the table model -- unsaved edits are preserved. */
    public void refreshTheme() {
        setBackground(Theme.current().background);
        scrollPane.getViewport().setBackground(Theme.current().surface);
        toolbarCard.repaint();

        addRowButton.repaint();
        clearButton.repaint();
        saveButton.repaint();
        searchField.repaint();

        table.getTableHeader().repaint();
        table.repaint();
        tableCard.repaint();
        updateSaveStatus();
        revalidate();
        repaint();
    }

    /** Called by MainFrame when the window is closing. */
    public void saveOnExit() {
        saveTrades();
    }

    /** Called after a CSV import (or any external change to the trades
     *  table) so the currently-open Trades screen reflects new data
     *  without needing an app restart. */
    public void reloadFromDatabase() {
        loadTrades();
    }

    private void updateSaveStatus() {
        if (saveStatusLabel == null) return;
        if (dirty) {
            saveStatusLabel.setText("Unsaved changes");
            saveStatusLabel.setIcon(new DotIcon(Theme.current().warning));
            saveStatusLabel.setForeground(Theme.current().warning);
        } else {
            saveStatusLabel.setText("All changes saved");
            saveStatusLabel.setIcon(new CheckIcon(Theme.current().success));
            saveStatusLabel.setForeground(Theme.current().success);
        }
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
                        rs.getInt("id"),
                        "" // delete column -- no real data, just a click target
                });
            }

            // No saved trades at all -- show a handful of blank rows ready
            // to fill in, instead of a completely empty table on first open.
            if (model.getRowCount() == 0) {
                for (int i = 0; i < STARTER_ROW_COUNT; i++) {
                    model.addRow(new Object[]{"", "", "", "", "", "", "", "", "", null, ""});
                }
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
            updateSaveStatus();
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
                updateSaveStatus();

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
        String[] columnNames = {"Pair", "Pattern", "Wave", "Diversion", "S/R", "Direction", "Entry signal", "outcome", "Date", "id", ""};
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

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

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
                int modelCol = table.convertColumnIndexToModel(column);
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
                setIcon(null);

                if (modelCol == 7) { // outcome -- colored badge-style text
                    String outcome = value == null ? "" : value.toString();
                    setText(outcome);
                    setFont(Theme.buttonFont());
                    if (!isSelected) {
                        Color badgeColor = switch (outcome) {
                            case "Win" -> Theme.current().success;
                            case "Loss" -> Theme.current().danger;
                            case "Break Even" -> Theme.current().warning;
                            default -> Theme.current().textPrimary;
                        };
                        c.setForeground(badgeColor);
                    }
                } else if (modelCol == DELETE_COL) {
                    setText(null);
                    setIcon(new TrashIcon(isSelected ? Color.WHITE : Theme.current().danger));
                    setHorizontalAlignment(SwingConstants.CENTER);
                }

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
                boolean overDelete = col >= 0 && table.convertColumnIndexToModel(col) == DELETE_COL;
                table.setCursor(overDelete ? new Cursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
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
                int viewRow = table.rowAtPoint(e.getPoint());
                int viewCol = table.columnAtPoint(e.getPoint());
                if (viewRow < 0 || viewCol < 0) return;

                int modelCol = table.convertColumnIndexToModel(viewCol);

                if (modelCol == DELETE_COL) {
                    int modelRow = table.convertRowIndexToModel(viewRow);
                    DefaultTableModel m = (DefaultTableModel) table.getModel();
                    Object idVal = m.getValueAt(modelRow, 9);
                    if (idVal != null) deletedIds.add((Integer) idVal);
                    m.removeRow(modelRow);
                    dirty = true;
                    updateSaveStatus();
                    return;
                }

                showDropdownMenu(viewRow, viewCol, e.getX(), e.getY());
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
        table.getColumnModel().getColumn(table.getColumnModel().getColumnCount() - 1).setPreferredWidth(40);
        table.getColumnModel().getColumn(table.getColumnModel().getColumnCount() - 1).setMaxWidth(44);

        return table;
    }

    /** Sets the value and auto-advances to the next dropdown column --
     *  shared by both a mouse click on an option and pressing Enter
     *  in the filter box. */
    private void selectOption(int row, int col, String option) {
        table.setValueAt(option, row, col);
        closeDropdown();

        int nextCol = col + 1;
        if (nextCol <= 7) {
            SwingUtilities.invokeLater(() -> showDropdownMenu(row, nextCol, 0, 0));
        }
    }

    private void showDropdownMenu(int row, int col, int x, int y) {
        closeDropdown();

        String columnName = table.getColumnName(col);
        String[] options = columnOptions.get(columnName);
        if (options == null) return;

        JWindow dropdown = new JWindow(SwingUtilities.getWindowAncestor(this));
        try {
            dropdown.setBackground(new Color(0, 0, 0, 0)); // enables real rounded corners, not a square box
        } catch (Exception ignored) {
            // per-pixel translucency unsupported on this system -- falls back to a square window, still functional
        }
        currentDropdown = dropdown;

        JPanel itemPanel = new JPanel();
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));
        itemPanel.setOpaque(false);
        itemPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // Tracks each row alongside the option text it represents, so
        // the filter box can show/hide rows and Enter can jump to the
        // first currently-visible match.
        List<Object[]> rows = new ArrayList<>(); // {RoundedPanel itemRow, String option, String label}

        for (String option : options) {
            String label = option.isEmpty() ? "-- Clear --" : option;

            RoundedPanel itemRow = new RoundedPanel(new BorderLayout());
            itemRow.cornerRadius(8).showBorder(false).showShadow(false);
            itemRow.fixedBackground(new Color(0, 0, 0, 0));
            itemRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            itemRow.setPreferredSize(new Dimension(220, 32));
            itemRow.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

            JLabel itemLabel = new JLabel(label);
            itemLabel.setFont(Theme.cellFont());
            itemLabel.setForeground(Theme.current().textPrimary);
            itemRow.add(itemLabel, BorderLayout.CENTER);

            itemRow.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    itemRow.fixedBackground(Theme.current().hover);
                    itemRow.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    itemRow.fixedBackground(new Color(0, 0, 0, 0));
                    itemRow.repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    selectOption(row, col, option);
                }
            });

            itemPanel.add(itemRow);
            rows.add(new Object[]{itemRow, option, label});
        }

        int MAX_VISIBLE = 8;
        int ITEM_HEIGHT = 32;
        int panelWidth = 228;

        JScrollPane scrollPane = new JScrollPane(itemPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        int visibleHeight = Math.min(options.length, MAX_VISIBLE) * ITEM_HEIGHT + 8;
        scrollPane.setPreferredSize(new Dimension(panelWidth, visibleHeight));

        RoundedPanel content = new RoundedPanel(new BorderLayout());
        content.cornerRadius(14).showBorder(true).showShadow(true);

        // Only worth showing the filter box when there's enough options
        // to actually make scrolling annoying -- a 2-3 item list (like
        // Direction) doesn't need one.
        if (options.length > 6) {
            JTextField filterField = new JTextField();
            filterField.setOpaque(false);
            filterField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.current().border),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            filterField.setFont(Theme.cellFont());
            filterField.putClientProperty("JTextField.placeholderText", "Type to filter...");

            filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                private void apply() {
                    String query = filterField.getText().trim().toLowerCase();
                    for (Object[] r : rows) {
                        JPanel itemRow = (JPanel) r[0];
                        String label = ((String) r[2]).toLowerCase();
                        itemRow.setVisible(query.isEmpty() || label.contains(query));
                    }
                    itemPanel.revalidate();
                    itemPanel.repaint();
                }
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { apply(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { apply(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { apply(); }
            });

            filterField.addActionListener(e -> {
                // Enter -- jump straight to the first still-visible match
                for (Object[] r : rows) {
                    JPanel itemRow = (JPanel) r[0];
                    if (itemRow.isVisible()) {
                        selectOption(row, col, (String) r[1]);
                        return;
                    }
                }
            });

            content.add(filterField, BorderLayout.NORTH);
        }

        content.add(scrollPane, BorderLayout.CENTER);
        dropdown.add(content);
        dropdown.pack();

        Rectangle cellRect = table.getCellRect(row, col, false);
        Point cellOnScreen = table.getLocationOnScreen();
        dropdown.setLocation(
                cellOnScreen.x + cellRect.x,
                cellOnScreen.y + cellRect.y + cellRect.height + 4
        );

        dropdown.setVisible(true);

        if (options.length > 6) {
            SwingUtilities.invokeLater(() -> {
                Component first = ((BorderLayout) content.getLayout()).getLayoutComponent(BorderLayout.NORTH);
                if (first != null) first.requestFocusInWindow();
            });
        }

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
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttonPanel.setOpaque(false);

        // One dominant filled color (Save, since that's the action that
        // actually matters) instead of three separate colored pills
        // fighting for attention. Add is outlined/secondary. Clear is
        // downgraded to plain text -- it's rare and destructive, it
        // shouldn't visually compete with the buttons you use constantly.
        addRowButton = (PillButton) new PillButton("Add Trade", PillButton.Style.SECONDARY, PillButton.ColorRole.ACCENT).compact();
        saveButton   = (PillButton) new PillButton("Save Trades", PillButton.Style.PRIMARY, PillButton.ColorRole.ACCENT).compact();

        clearButton = (PillButton) new PillButton("Clear All", PillButton.Style.SECONDARY).compact();

        addRowButton.addActionListener(e -> {
            if (searchField != null && !searchField.getText().isBlank()) {
                searchField.setText(""); // clear filter so the new row is guaranteed visible
            }
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.addRow(new Object[]{"", "", "", "", "", "", "", "", "", null, ""});
            int newRow = model.getRowCount() - 1;
            int viewRow = table.convertRowIndexToView(newRow);
            table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
            SwingUtilities.invokeLater(() -> showDropdownMenu(viewRow, 0, 0, 0));
        });

        clearButton.addActionListener(e -> {
            JPanel confirmPanel = new JPanel();
            confirmPanel.setLayout(new BoxLayout(confirmPanel, BoxLayout.Y_AXIS));
            confirmPanel.setBackground(Theme.current().surface);
            confirmPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            JLabel warning = new JLabel("<html>This permanently deletes <b>every trade</b> once saved.<br>"
                    + "Type <b>CLEAR</b> below to confirm.</html>");
            warning.setFont(Theme.cellFont());
            warning.setAlignmentX(Component.LEFT_ALIGNMENT);
            confirmPanel.add(warning);
            confirmPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            JTextField confirmField = new JTextField();
            confirmField.setAlignmentX(Component.LEFT_ALIGNMENT);
            confirmPanel.add(confirmField);

            int result = JOptionPane.showConfirmDialog(this, confirmPanel,
                    "Confirm Clear All Trades", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            if (result != JOptionPane.OK_OPTION) return;

            if (!"CLEAR".equals(confirmField.getText().trim())) {
                JOptionPane.showMessageDialog(this,
                        "Text didn't match exactly — nothing was cleared.",
                        "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            for (int r = 0; r < model.getRowCount(); r++) {
                Object idVal = model.getValueAt(r, 9);
                if (idVal != null) deletedIds.add((Integer) idVal);
            }
            model.setRowCount(0);
        });

        saveButton.addActionListener(e -> saveTrades());

        buttonPanel.add(addRowButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(24, 0)));
        buttonPanel.add(clearButton);

        return buttonPanel;
    }

    private JPanel createControlsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        row.setOpaque(false);

        saveStatusLabel = new JLabel();
        saveStatusLabel.setIconTextGap(6);
        saveStatusLabel.setFont(new Font(Theme.FONT_FAMILY, Font.PLAIN, 12));
        row.add(saveStatusLabel);

        searchField = new SearchField("Search pair, pattern, outcome...");
        searchField.onChange(text -> applyFilter());
        row.add(searchField);

        return row;
    }

    private void applyFilter() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }
        try {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0, 1, 7));
        } catch (Exception ignored) {
            // malformed search text -- just don't filter rather than crash
        }
    }

    /** Hand-drawn trash can icon -- replaces a Unicode "X" glyph that
     *  wasn't rendering reliably (showed as a blank box on some fonts). */
    private static class TrashIcon implements Icon {
        private final Color color;
        TrashIcon(Color color) { this.color = color; }

        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            g2.drawLine(x + 2, y + 4, x + 12, y + 4);
            g2.drawLine(x + 5, y + 4, x + 5, y + 2);
            g2.drawLine(x + 9, y + 4, x + 9, y + 2);
            g2.drawLine(x + 5, y + 2, x + 9, y + 2);
            g2.drawRoundRect(x + 3, y + 4, 8, 9, 2, 2);
            g2.drawLine(x + 6, y + 6, x + 6, y + 11);
            g2.drawLine(x + 8, y + 6, x + 8, y + 11);

            g2.dispose();
        }
    }

    /** Small checkmark for "saved" status. */
    private static class CheckIcon implements Icon {
        private final Color color;
        CheckIcon(Color color) { this.color = color; }

        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 12; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 1, y + 6, x + 5, y + 10);
            g2.drawLine(x + 5, y + 10, x + 11, y + 2);
            g2.dispose();
        }
    }

    /** Small filled dot for "unsaved changes" status. */
    private static class DotIcon implements Icon {
        private final Color color;
        DotIcon(Color color) { this.color = color; }

        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 12; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x + 3, y + 3, 6, 6);
            g2.dispose();
        }
    }
}