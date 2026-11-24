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
import java.util.List;
import java.util.TreeSet;

import javax.swing.JPanel;

/**
 * Draws a disk head movement chart with custom painting.
 * The horizontal axis represents cylinder positions (0 to diskSize - 1),
 * and each step downwards plots the next visited cylinder with step distance tooltips.
 * The C-LOOK return jump (the one step that goes against the initial direction) is drawn dashed.
 */
public class SeekChartPanel extends JPanel {

    private static final int MARGIN_X = 36;
    private static final int AXIS_TOP = 26;
    /** Gap between the axis and the first point, so the start point does not sit on the axis line. */
    private static final int FIRST_ROW_GAP = 18;
    private static final int STEP_HEIGHT = 24;
    private static final int BOTTOM_PADDING = 20;
    private static final int MIN_PANEL_HEIGHT = 180;
    private static final int POINT_RADIUS = 4;

    private static final Color AXIS_COLOR = new Color(60, 60, 60);
    private static final Color GRID_COLOR = new Color(230, 230, 230);
    private static final Color LINE_COLOR = new Color(70, 130, 210);
    private static final Color START_POINT_COLOR = new Color(60, 165, 95);
    private static final Color POINT_COLOR = new Color(230, 110, 60);
    private static final Color TEXT_COLOR = new Color(20, 20, 20);

    private int[] seekSequence = new int[0];
    private int diskSize = 200;
    /** +1 when the head starts moving up, -1 when down, 0 when unknown (then no jump is marked). */
    private int initialDirection = 0;

    public SeekChartPanel() {
        setBackground(Color.WHITE);
        setToolTipText("");
    }

    public void setResult(int[] sequence, int diskSize) {
        setResult(sequence, diskSize, 0);
    }

    /**
     * @param initialDirection +1 if the head starts moving up, -1 if down; the first step against
     *                         this direction is the return jump and is drawn dashed
     */
    public void setResult(int[] sequence, int diskSize, int initialDirection) {
        this.initialDirection = Integer.signum(initialDirection);
        this.seekSequence = sequence == null ? new int[0] : sequence.clone();
        this.diskSize = Math.max(2, diskSize);
        revalidate();
        repaint();
    }

    public void clear() {
        this.seekSequence = new int[0];
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int height = seekSequence.length <= 1
                ? MIN_PANEL_HEIGHT
                : Math.max(MIN_PANEL_HEIGHT, yOf(seekSequence.length - 1) + BOTTOM_PADDING);
        return new Dimension(360, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (seekSequence.length == 0) {
            g2.setColor(Color.GRAY);
            g2.drawString("Click Compute to draw the seek sequence chart.", 20, 40);
            g2.dispose();
            return;
        }

        int plotWidth = Math.max(200, getWidth() - 2 * MARGIN_X);
        int plotBottom = yOf(Math.max(1, seekSequence.length - 1));

        drawCylinderAxis(g2, plotWidth, plotBottom);
        drawSeekPath(g2, plotWidth);

        g2.dispose();
    }

    private void drawCylinderAxis(Graphics2D g2, int plotWidth, int plotBottom) {
        g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
        FontMetrics fm = g2.getFontMetrics();

        // Top horizontal cylinder track bar
        g2.setColor(AXIS_COLOR);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(MARGIN_X, AXIS_TOP, MARGIN_X + plotWidth, AXIS_TOP);

        // Collect key cylinder markers: 0, diskSize - 1, and all visited cylinders
        TreeSet<Integer> markers = new TreeSet<Integer>();
        markers.add(0);
        markers.add(diskSize - 1);
        for (int cyl : seekSequence) {
            markers.add(cyl);
        }

        List<Integer> sortedMarkers = new ArrayList<Integer>(markers);
        Collections.sort(sortedMarkers);

        int lastLabelRight = Integer.MIN_VALUE;
        for (int cyl : sortedMarkers) {
            int x = xOf(cyl, plotWidth);

            // Light vertical guide line
            g2.setColor(GRID_COLOR);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(x, AXIS_TOP + 1, x, plotBottom);

            // Axis tick
            g2.setColor(AXIS_COLOR);
            g2.drawLine(x, AXIS_TOP - 4, x, AXIS_TOP + 4);

            // Cylinder label above axis (skip if overlapping)
            String label = String.valueOf(cyl);
            int labelWidth = fm.stringWidth(label);
            int labelX = x - labelWidth / 2;
            if (labelX > lastLabelRight + 4) {
                g2.setColor(TEXT_COLOR);
                g2.drawString(label, labelX, AXIS_TOP - 7);
                lastLabelRight = labelX + labelWidth;
            }
        }
    }

    private void drawSeekPath(Graphics2D g2, int plotWidth) {
        g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
        FontMetrics fm = g2.getFontMetrics();

        // Draw connecting lines; the return jump is dashed and labelled.
        int jumpStep = findJumpStep();
        BasicStroke solid = new BasicStroke(2f);
        BasicStroke dashed = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                new float[] {7f, 5f}, 0f);
        g2.setColor(LINE_COLOR);
        for (int i = 1; i < seekSequence.length; i++) {
            int x1 = xOf(seekSequence[i - 1], plotWidth);
            int y1 = yOf(i - 1);
            int x2 = xOf(seekSequence[i], plotWidth);
            int y2 = yOf(i);
            g2.setStroke(i == jumpStep ? dashed : solid);
            g2.drawLine(x1, y1, x2, y2);
            if (i == jumpStep) {
                String jump = "jump (" + Math.abs(seekSequence[i] - seekSequence[i - 1]) + ")";
                g2.drawString(jump, (x1 + x2 - fm.stringWidth(jump)) / 2, (y1 + y2) / 2 - 4);
            }
        }

        // Draw nodes and cylinder numbers
        for (int i = 0; i < seekSequence.length; i++) {
            int x = xOf(seekSequence[i], plotWidth);
            int y = yOf(i);

            g2.setColor(i == 0 ? START_POINT_COLOR : POINT_COLOR);
            g2.fillOval(x - POINT_RADIUS, y - POINT_RADIUS, POINT_RADIUS * 2, POINT_RADIUS * 2);
            g2.setColor(AXIS_COLOR);
            g2.setStroke(new BasicStroke(1f));
            g2.drawOval(x - POINT_RADIUS, y - POINT_RADIUS, POINT_RADIUS * 2, POINT_RADIUS * 2);

            String label = String.valueOf(seekSequence[i]);
            int labelX = (x + 8 + fm.stringWidth(label) > getWidth() - 6)
                    ? x - 8 - fm.stringWidth(label)
                    : x + 8;
            g2.setColor(TEXT_COLOR);
            g2.drawString(label, labelX, y + fm.getAscent() / 2 - 1);
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (seekSequence.length == 0) {
            return null;
        }
        int plotWidth = Math.max(200, getWidth() - 2 * MARGIN_X);
        for (int i = 0; i < seekSequence.length; i++) {
            int x = xOf(seekSequence[i], plotWidth);
            int y = yOf(i);
            if (Math.hypot(event.getX() - x, event.getY() - y) <= 8) {
                if (i == 0) {
                    return "Initial Head Position: Cylinder " + seekSequence[i];
                }
                int dist = Math.abs(seekSequence[i] - seekSequence[i - 1]);
                return "Step " + i + ": " + seekSequence[i - 1] + " -> " + seekSequence[i]
                        + " (Seek Distance: " + dist + " cylinder" + (dist == 1 ? "" : "s") + ")";
            }
        }
        return null;
    }

    /**
     * The first step that moves against the initial direction (the C-LOOK return jump),
     * or -1 if there is none or the direction is unknown.
     */
    private int findJumpStep() {
        if (initialDirection == 0) {
            return -1;
        }
        for (int i = 1; i < seekSequence.length; i++) {
            if (Integer.signum(seekSequence[i] - seekSequence[i - 1]) == -initialDirection) {
                return i;
            }
        }
        return -1;
    }

    private int xOf(int cylinder, int plotWidth) {
        double ratio = (double) cylinder / (diskSize - 1);
        return MARGIN_X + (int) Math.round(ratio * plotWidth);
    }

    private int yOf(int stepIndex) {
        return AXIS_TOP + FIRST_ROW_GAP + stepIndex * STEP_HEIGHT;
    }
}