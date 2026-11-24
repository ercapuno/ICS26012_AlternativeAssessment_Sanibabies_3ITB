package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import model.GanttBlock;

/**
 * Draws a Gantt chart with custom painting. Block widths are proportional to time,
 * each block shows its PID (or IDLE in gray), and time markers run along the bottom.
 * Put it in a JScrollPane: its preferred width grows with the schedule length.
 */
public class GanttChartPanel extends JPanel {

    private static final int MARGIN = 20;
    private static final int BAR_TOP = 18;
    private static final int BAR_HEIGHT = 46;
    private static final int TICK_LENGTH = 5;
    private static final int PANEL_HEIGHT = BAR_TOP + BAR_HEIGHT + TICK_LENGTH + 28;

    /** Pixels per time unit for normal schedules. */
    private static final double DEFAULT_UNIT_WIDTH = 40;
    /** Very long schedules are squeezed so the chart stays at most this wide. */
    private static final double MAX_CHART_WIDTH = 30000;

    private static final Color IDLE_COLOR = new Color(190, 190, 190);
    private static final Color BORDER_COLOR = new Color(60, 60, 60);
    private static final Color TEXT_COLOR = new Color(20, 20, 20);
    private static final Color[] PROCESS_COLORS = {
        new Color(129, 178, 236), new Color(247, 167, 112), new Color(140, 210, 140),
        new Color(232, 140, 160), new Color(190, 160, 230), new Color(240, 210, 110),
        new Color(110, 205, 200), new Color(220, 170, 140), new Color(170, 200, 110),
        new Color(160, 180, 220),
    };

    private List<GanttBlock> blocks = Collections.emptyList();
    private final Map<String, Color> colorByPid = new HashMap<String, Color>();
    private double unitWidth = DEFAULT_UNIT_WIDTH;

    public GanttChartPanel() {
        setBackground(Color.WHITE);
        setToolTipText(""); // registers with the tooltip manager; the text comes from getToolTipText(MouseEvent)
    }

    /** Shows a new schedule. Each PID keeps one color across the chart. */
    public void setBlocks(List<GanttBlock> newBlocks) {
        blocks = new ArrayList<GanttBlock>(newBlocks);
        colorByPid.clear();
        for (GanttBlock block : blocks) {
            if (!block.isIdle() && !colorByPid.containsKey(block.getLabel())) {
                colorByPid.put(block.getLabel(), PROCESS_COLORS[colorByPid.size() % PROCESS_COLORS.length]);
            }
        }
        int totalTime = blocks.isEmpty() ? 0 : blocks.get(blocks.size() - 1).getEnd();
        unitWidth = totalTime == 0 ? DEFAULT_UNIT_WIDTH : Math.min(DEFAULT_UNIT_WIDTH, MAX_CHART_WIDTH / totalTime);
        revalidate(); // the preferred width changed, so the scroll pane must update its scrollbar
        repaint();
    }

    public void clear() {
        setBlocks(Collections.<GanttBlock>emptyList());
    }

    @Override
    public Dimension getPreferredSize() {
        // When empty, reserve room for the hint text.
        int width = blocks.isEmpty() ? 300 : xOf(blocks.get(blocks.size() - 1).getEnd()) + MARGIN;
        return new Dimension(width, PANEL_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (blocks.isEmpty()) {
            g2.setColor(Color.GRAY);
            g2.drawString("Click Compute to draw the Gantt chart.", MARGIN, BAR_TOP + BAR_HEIGHT / 2);
            g2.dispose();
            return;
        }

        drawBlocks(g2);
        drawTimeMarkers(g2);
        g2.dispose();
    }

    private void drawBlocks(Graphics2D g2) {
        g2.setFont(getFont().deriveFont(Font.BOLD));
        FontMetrics fm = g2.getFontMetrics();
        for (GanttBlock block : blocks) {
            int x = xOf(block.getStart());
            int width = xOf(block.getEnd()) - x;

            g2.setColor(block.isIdle() ? IDLE_COLOR : colorByPid.get(block.getLabel()));
            g2.fillRect(x, BAR_TOP, width, BAR_HEIGHT);
            g2.setColor(BORDER_COLOR);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(x, BAR_TOP, width, BAR_HEIGHT);

            // Center the label; skip it when the block is too narrow (the tooltip still shows it).
            int textWidth = fm.stringWidth(block.getLabel());
            if (textWidth + 4 <= width) {
                g2.setColor(TEXT_COLOR);
                int textX = x + (width - textWidth) / 2;
                int textY = BAR_TOP + (BAR_HEIGHT + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(block.getLabel(), textX, textY);
            }
        }
    }

    /** Draws a tick and a time value at every block boundary, skipping values that would overlap. */
    private void drawTimeMarkers(Graphics2D g2) {
        g2.setFont(getFont().deriveFont(Font.PLAIN));
        FontMetrics fm = g2.getFontMetrics();
        int tickTop = BAR_TOP + BAR_HEIGHT;
        int textY = tickTop + TICK_LENGTH + fm.getAscent() + 2;

        List<Integer> times = new ArrayList<Integer>();
        times.add(blocks.get(0).getStart());
        for (GanttBlock block : blocks) {
            times.add(block.getEnd());
        }

        int lastTextRight = Integer.MIN_VALUE;
        g2.setColor(TEXT_COLOR);
        for (int time : times) {
            int x = xOf(time);
            g2.drawLine(x, tickTop, x, tickTop + TICK_LENGTH);
            String text = String.valueOf(time);
            int textX = x - fm.stringWidth(text) / 2;
            if (textX > lastTextRight + 3) {
                g2.drawString(text, textX, textY);
                lastTextRight = textX + fm.stringWidth(text);
            }
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (event.getY() < BAR_TOP || event.getY() > BAR_TOP + BAR_HEIGHT) {
            return null;
        }
        for (GanttBlock block : blocks) {
            if (event.getX() >= xOf(block.getStart()) && event.getX() < xOf(block.getEnd())) {
                return block.getLabel() + ": " + block.getStart() + " to " + block.getEnd()
                        + " (" + block.getDuration() + " unit" + (block.getDuration() == 1 ? "" : "s") + ")";
            }
        }
        return null;
    }

    /** Converts a time value to an x pixel position. */
    private int xOf(int time) {
        return MARGIN + (int) Math.round(time * unitWidth);
    }
}

