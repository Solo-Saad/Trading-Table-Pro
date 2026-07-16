package MainApp;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.function.Consumer;

/**
 * A rounded search pill with a hand-drawn magnifying glass icon
 * embedded on the left -- built as a composite over RoundedPanel
 * instead of a bare JTextField, to actually match the app's card
 * language instead of looking like an unstyled form input.
 */
public class SearchField extends RoundedPanel {

    private final JTextField textField;

    public SearchField(String placeholder) {
        super(new BorderLayout(6, 0));
        cornerRadius(18).showBorder(true).showShadow(false);
        setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 14));
        setPreferredSize(new Dimension(220, 34));

        MagnifierIcon icon = new MagnifierIcon();
        icon.setPreferredSize(new Dimension(16, 16));
        add(icon, BorderLayout.WEST);

        textField = new JTextField();
        textField.setBorder(BorderFactory.createEmptyBorder());
        textField.setOpaque(false);
        textField.setFont(Theme.cellFont());
        textField.putClientProperty("JTextField.placeholderText", placeholder);
        add(textField, BorderLayout.CENTER);
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public JTextComponent getTextComponent() {
        return textField;
    }

    public void onChange(Consumer<String> listener) {
        textField.getDocument().addDocumentListener(new DocumentListener() {
            private void apply() { listener.accept(textField.getText()); }
            @Override public void insertUpdate(DocumentEvent e) { apply(); }
            @Override public void removeUpdate(DocumentEvent e) { apply(); }
            @Override public void changedUpdate(DocumentEvent e) { apply(); }
        });
    }

    /** Small hand-drawn magnifying glass, resolved from Theme at paint time. */
    private static class MagnifierIcon extends JComponent {
        MagnifierIcon() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.current().textSecondary);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(1, 1, 9, 9);
            g2.drawLine(9, 9, 14, 14);
            g2.dispose();
        }
    }
}
