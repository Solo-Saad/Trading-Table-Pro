package MainApp;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.LinkedHashMap;

/**
 * Journal entries can optionally link to a specific trade -- picked
 * from a dropdown of recent trades when writing the entry. Unlinked
 * entries are still fully supported (trade_id is nullable).
 */
public class JournalPanel extends JPanel {

    private JTextArea noteInput;
    private JPanel entryList;
    private JPanel form;
    private JComboBox<String> tradeSelector;
    private java.util.List<Integer> tradeSelectorIds; // parallel to combo box items; null at index 0

    private JLabel titleLabel;
    private JPanel body;
    private JLabel formLabel;
    private JPanel linkRow;
    private JLabel linkLabel;
    private JPanel saveRow;

    public JournalPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.current().background);

        titleLabel = new JLabel("Journal");
        titleLabel.setFont(Theme.headerFont());
        titleLabel.setForeground(Theme.current().textPrimary);
        add(titleLabel, BorderLayout.NORTH);

        body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(Theme.current().background);

        // ─── New entry form ───
        form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Theme.current().surface);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.current().border),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        formLabel = new JLabel("New entry \u2014 " + LocalDate.now());
        formLabel.setFont(Theme.cellFont());
        formLabel.setForeground(Theme.current().textSecondary);
        formLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(formLabel);
        form.add(Box.createRigidArea(new Dimension(0, 8)));

        linkRow = new JPanel(new BorderLayout(8, 0));
        linkRow.setBackground(Theme.current().surface);
        linkRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        linkRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        linkLabel = new JLabel("Link to trade:");
        linkLabel.setFont(Theme.cellFont());
        linkLabel.setForeground(Theme.current().textSecondary);
        linkRow.add(linkLabel, BorderLayout.WEST);

        tradeSelector = new JComboBox<>();
        linkRow.add(tradeSelector, BorderLayout.CENTER);
        form.add(linkRow);
        form.add(Box.createRigidArea(new Dimension(0, 8)));

        noteInput = new JTextArea(4, 20);
        noteInput.setLineWrap(true);
        noteInput.setWrapStyleWord(true);
        noteInput.setFont(Theme.cellFont());
        JScrollPane inputScroll = new JScrollPane(noteInput);
        inputScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(inputScroll);
        form.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton saveButton = new JButton("Save entry");
        saveButton.addActionListener(e -> saveEntry());
        saveRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveRow.setBackground(Theme.current().surface);
        saveRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveRow.add(saveButton);
        form.add(saveRow);

        body.add(form, BorderLayout.NORTH);

        // ─── Past entries ───
        entryList = new JPanel();
        entryList.setLayout(new BoxLayout(entryList, BoxLayout.Y_AXIS));
        entryList.setBackground(Theme.current().background);

        JScrollPane listScroll = new JScrollPane(entryList);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.getVerticalScrollBar().setUnitIncrement(16);
        body.add(listScroll, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);

        refreshTradeOptions();
        loadEntries();

        Timer refreshTimer = new Timer(5000, e -> {
            refreshTradeOptions();
            loadEntries();
        });
        refreshTimer.start();
    }

    public void refreshTheme() {
        setBackground(Theme.current().background);
        titleLabel.setForeground(Theme.current().textPrimary);
        body.setBackground(Theme.current().background);

        form.setBackground(Theme.current().surface);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.current().border),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        formLabel.setForeground(Theme.current().textSecondary);

        linkRow.setBackground(Theme.current().surface);
        linkLabel.setForeground(Theme.current().textSecondary);

        saveRow.setBackground(Theme.current().surface);

        noteInput.setFont(Theme.cellFont()); // font only -- text itself untouched
        entryList.setBackground(Theme.current().background);

        loadEntries();
        revalidate();
        repaint();
    }

    /** Repopulates the "link to trade" dropdown, preserving the current selection where possible. */
    private void refreshTradeOptions() {
        String previouslySelected = (String) tradeSelector.getSelectedItem();

        LinkedHashMap<Integer, String> options = new LinkedHashMap<>();
        try (Connection conn = DatabaseManager.connect()) {
            options = DatabaseManager.getTradeOptions(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }

        tradeSelector.removeAllItems();
        tradeSelectorIds = new java.util.ArrayList<>();

        tradeSelector.addItem("None");
        tradeSelectorIds.add(null);

        for (var entry : options.entrySet()) {
            tradeSelector.addItem(entry.getValue());
            tradeSelectorIds.add(entry.getKey());
        }

        if (previouslySelected != null) {
            tradeSelector.setSelectedItem(previouslySelected);
        }
    }

    private void saveEntry() {
        String note = noteInput.getText().trim();
        if (note.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Write something before saving.",
                    "Empty entry", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int selectedIndex = tradeSelector.getSelectedIndex();
        Integer tradeId = (selectedIndex > 0 && tradeSelectorIds != null && selectedIndex < tradeSelectorIds.size())
                ? tradeSelectorIds.get(selectedIndex) : null;

        try (Connection conn = DatabaseManager.connect()) {
            DatabaseManager.insertJournalEntry(conn, LocalDate.now().toString(), note, tradeId);
            noteInput.setText("");
            tradeSelector.setSelectedIndex(0);
            loadEntries();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save entry.\n\n" + e.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadEntries() {
        entryList.removeAll();

        String sql = "SELECT j.id, j.entry_date, j.note, j.trade_id, t.pair, t.pattern, t.trade_date "
                + "FROM journal_entries j LEFT JOIN trades t ON j.trade_id = t.id "
                + "ORDER BY j.id DESC";

        try (
                Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                String linkedLabel = null;
                if (rs.getObject("trade_id") != null && rs.getString("pair") != null) {
                    String pattern = rs.getString("pattern");
                    String date = rs.getString("trade_date");
                    linkedLabel = "Linked: " + rs.getString("pair")
                            + (pattern != null && !pattern.isBlank() ? " \u00b7 " + pattern : "")
                            + (date != null && !date.isBlank() ? " \u00b7 " + date : "");
                }
                entryList.add(entryCard(rs.getInt("id"), rs.getString("entry_date"), rs.getString("note"), linkedLabel));
                entryList.add(Box.createRigidArea(new Dimension(0, 8)));
            }
            if (!any) {
                JLabel empty = new JLabel("No journal entries yet.");
                empty.setFont(Theme.cellFont());
                empty.setForeground(Theme.current().textSecondary);
                entryList.add(empty);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load journal entries.\n\n" + e.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
        }

        revalidate();
        repaint();
    }

    private JPanel entryCard(int id, String date, String note, String linkedLabel) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Theme.current().surface);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.current().border),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(Theme.current().surface);

        JPanel headerLeft = new JPanel();
        headerLeft.setLayout(new BoxLayout(headerLeft, BoxLayout.Y_AXIS));
        headerLeft.setBackground(Theme.current().surface);

        JLabel dateLbl = new JLabel(date);
        dateLbl.setFont(Theme.buttonFont());
        dateLbl.setForeground(Theme.current().accent);
        dateLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerLeft.add(dateLbl);

        if (linkedLabel != null) {
            JLabel linkLbl = new JLabel(linkedLabel);
            linkLbl.setFont(new Font(Theme.FONT_FAMILY, Font.ITALIC, 11));
            linkLbl.setForeground(Theme.current().textSecondary);
            linkLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            headerLeft.add(linkLbl);
        }

        headerRow.add(headerLeft, BorderLayout.WEST);

        JButton deleteButton = new JButton("Delete");
        deleteButton.setFont(Theme.cellFont());
        deleteButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(this, "Delete this entry?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                try (Connection conn = DatabaseManager.connect()) {
                    DatabaseManager.deleteJournalEntry(conn, id);
                    loadEntries();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Failed to delete entry.\n\n" + ex.getMessage(),
                            "Delete Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        headerRow.add(deleteButton, BorderLayout.EAST);

        card.add(headerRow, BorderLayout.NORTH);

        JTextArea noteArea = new JTextArea(note);
        noteArea.setEditable(false);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setFont(Theme.cellFont());
        noteArea.setBackground(Theme.current().surface);
        noteArea.setForeground(Theme.current().textPrimary);
        noteArea.setBorder(BorderFactory.createEmptyBorder());
        card.add(noteArea, BorderLayout.CENTER);

        return card;
    }
}