package MainApp;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

/**
 * Standalone journal -- freeform notes with a date, not linked to any
 * specific trade row (decision made in Phase 3 planning: simpler
 * schema, journal_entries table has no trade_id column).
 */
public class JournalPanel extends JPanel {

    private JTextArea noteInput;
    private JPanel entryList;
    private JPanel form;
    private JScrollPane inputScroll;

    public JournalPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.current().background);

        JLabel title = new JLabel("Journal");
        title.setFont(Theme.headerFont());
        title.setForeground(Theme.current().textPrimary);
        add(title, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(Theme.current().background);

        // ─── New entry form ───
        JPanel form = new JPanel(new BorderLayout(0, 8));
        this.form = form;
        form.setBackground(Theme.current().surface);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.current().border),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel formLabel = new JLabel("New entry — " + LocalDate.now());
        formLabel.setFont(Theme.cellFont());
        formLabel.setForeground(Theme.current().textSecondary);
        form.add(formLabel, BorderLayout.NORTH);

        noteInput = new JTextArea(4, 20);
        noteInput.setLineWrap(true);
        noteInput.setWrapStyleWord(true);
        noteInput.setFont(Theme.cellFont());
        JScrollPane inputScroll = new JScrollPane(noteInput);
        this.inputScroll = inputScroll;
        form.add(inputScroll, BorderLayout.CENTER);

        JButton saveButton = new JButton("Save entry");
        saveButton.addActionListener(e -> saveEntry());
        JPanel saveRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveRow.setBackground(Theme.current().surface);
        saveRow.add(saveButton);
        form.add(saveRow, BorderLayout.SOUTH);

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

        loadEntries();
    }

    public void refreshTheme() {
        setBackground(Theme.current().background);
        form.setBackground(Theme.current().surface);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.current().border),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        noteInput.setFont(Theme.cellFont());
        entryList.setBackground(Theme.current().background);
        loadEntries();
    }

    private void saveEntry() {
        String note = noteInput.getText().trim();
        if (note.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Write something before saving.",
                    "Empty entry", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseManager.connect()) {
            DatabaseManager.insertJournalEntry(conn, LocalDate.now().toString(), note);
            noteInput.setText("");
            loadEntries();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save entry.\n\n" + e.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadEntries() {
        entryList.removeAll();

        String sql = "SELECT id, entry_date, note FROM journal_entries ORDER BY id DESC";

        try (
                Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                entryList.add(entryCard(rs.getInt("id"), rs.getString("entry_date"), rs.getString("note")));
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

    private JPanel entryCard(int id, String date, String note) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Theme.current().surface);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.current().border),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(Theme.current().surface);

        JLabel dateLbl = new JLabel(date);
        dateLbl.setFont(Theme.buttonFont());
        dateLbl.setForeground(Theme.current().accent);
        headerRow.add(dateLbl, BorderLayout.WEST);

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
