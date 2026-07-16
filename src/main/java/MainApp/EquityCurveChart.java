package MainApp;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Hand-drawn line chart of cumulative win/loss score (win = +1,
 * loss = -1, break even = 0) over trade order. Shared between
 * AnalyticsPanel (full size) and DashboardPanel (compact preview) --
 * extracted here instead of being duplicated in both.
 */
public class EquityCurveChart extends JPanel {
    private List<Integer> data = new ArrayList<>();

    public void setData(List<Integer> data) {
        this.data = data;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Theme.current().surface);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int padding = 20;

        if (data.size() < 2) {
            g2.setColor(Theme.current().textSecondary);
            g2.drawString("Not enough completed trades to chart yet.", padding, h / 2);
            return;
        }

        int min = data.stream().min(Integer::compareTo).orElse(0);
        int max = data.stream().max(Integer::compareTo).orElse(0);
        if (min == max) { min -= 1; max += 1; }

        g2.setColor(Theme.current().border);
        g2.drawLine(padding, h - padding, w - padding, h - padding);

        int zeroY = h - padding - (int) (((0.0 - min) / (max - min)) * (h - 2 * padding));
        g2.drawLine(padding, zeroY, w - padding, zeroY);

        g2.setColor(Theme.current().accent);
        g2.setStroke(new BasicStroke(2f));

        int stepX = (w - 2 * padding) / (data.size() - 1);
        int prevX = padding, prevY = 0;
        for (int i = 0; i < data.size(); i++) {
            int x = padding + i * stepX;
            int y = h - padding - (int) (((data.get(i) - min) / (double) (max - min)) * (h - 2 * padding));
            if (i > 0) g2.drawLine(prevX, prevY, x, y);
            prevX = x;
            prevY = y;
        }
    }
}
