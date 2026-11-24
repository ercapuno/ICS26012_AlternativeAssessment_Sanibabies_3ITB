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
import java.util.TreeSet;

import javax.swing.JPanel;

/**
 * Draws the disk head's path with custom painting, in the usual textbook style:
 * the cylinder scale runs across the top (0 to diskSize - 1), and each step of the
 * seek sequence is one row lower, joined by lines. Any disk scheduler can use it.
 * Put it in a JScrollPane: its preferred height grows with the number of steps.
 */
public class SeekPathPanel extends JPanel {

    private static final int SIDE_MARGIN = 34;
    private static final int AXIS_Y = 30;
    private static final int FIRST_ROW_Y = AXIS_Y + 22;
    /** Vertical distance between steps; matches the results table's row height. */
    private static final int ROW_HEIGHT = 22;
    private static final int BOTTOM_MARGIN = 16;
    private static final int POINT_SIZE = 7;
    private static final int TICK_LENGTH = 5;

    private static final Color AXIS_COLOR = new Color(60, 60, 60);
    private static final Color GUIDE_COLOR = new Color(225, 225, 225);
    private static final Color PATH_COLOR = new Color(47, 95, 208);
    private static final Color START_COLOR = new Color(220, 110, 40);
    private static final Color TEXT_COLOR = new Color(20, 20, 20);

    private int[] sequence = new int[0];
    private int diskSize = 200;

    public SeekPathPanel() {
        setBackground(Color.WHITE);
        setToolTipText(""); // registers with the tooltip manager; the text comes from getToolTipText(MouseEvent)
    }

    /**
     * Shows a new path.
     *
     * @param seekSequence cylinders in visiting order, starting with the initial head position
     * @param diskSize     number of cylinders; the scale runs from 0 to diskSize - 1
     */
    public void setPath(int[] seekSequence, int diskSize) {
        this.sequence = seekSequence.clone();
        this.diskSize = Math.max(1, diskSize);
        revalidate(); // the preferred height changed, so the scroll pane must update its scrollbar
        repaint();
    }

    public void clear() {
        setPath(new int[0], diskSize);
    }

    @Override
    public Dimension getPreferredSize() {
        int rows = Math.max(1, sequence.length);
        return new Dimension(420, FIRST_ROW_Y + (rows - 1) * ROW_HEIGHT + BOTTOM_MARGIN);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (sequence.length == 0) {
            g2.setColor(Color.GRAY);
            g2.drawString("Click Compute to draw the head's path.", SIDE_MARGIN, AXIS_Y);
            g2.dispose();
            return;
        }

        drawAxis(g2);
        drawPath(g2);
        g2.dispose();
    }

    /** The cylinder scale: a line with ticks at 0, the last cylinder, and every cylinder visited. */
    private void drawAxis(Graphics2D g2) {
        int left = xOf(0);
        int right = xOf(diskSize - 1);
        int bottom = yOf(sequence.length - 1);

        TreeSet<Integer> marks = new TreeSet<Integer>();
        marks.add(0);
        marks.add(diskSize - 1);
        for (int cylinder : sequence) {
            marks.add(cylinder);
        }

        // Light vertical guides first, so the path is drawn on top of them.
        g2.setColor(GUIDE_COLOR);
        for (int cylinder : marks) {
            g2.drawLine(xOf(cylinder), AXIS_Y, xOf(cylinder), bottom);
        }

        g2.setColor(AXIS_COLOR);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(left, AXIS_Y, right, AXIS_Y);

        g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
        FontMetrics fm = g2.getFontMetrics();
        int lastTextRight = Integer.MIN_VALUE;
        for (int cylinder : marks) {
            int x = xOf(cylinder);
            g2.drawLine(x, AXIS_Y - TICK_LENGTH, x, AXIS_Y);
            // Skip a label that would overlap the previous one (the point labels still show it).
            String text = String.valueOf(cylinder);
            int textX = x - fm.stringWidth(text) / 2;
            boolean isEnd = cylinder == diskSize - 1;
            if (textX > lastTextRight + 3 || isEnd) {
                g2.drawString(text, textX, AXIS_Y - TICK_LENGTH - 3);
                lastTextRight = textX + fm.stringWidth(text);
            }
        }
    }

    /** The path itself: a line between successive positions, a dot at each one, and its cylinder number. */
    private void drawPath(Graphics2D g2) {
        g2.setColor(PATH_COLOR);
        g2.setStroke(new BasicStroke(2f));
        for (int i = 1; i < sequence.length; i++) {
            g2.drawLine(xOf(sequence[i - 1]), yOf(i - 1), xOf(sequence[i]), yOf(i));
        }

        g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < sequence.length; i++) {
            int x = xOf(sequence[i]);
            int y = yOf(i);
            g2.setColor(i == 0 ? START_COLOR : PATH_COLOR);
            g2.fillOval(x - POINT_SIZE / 2, y - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE);

            // Label to the right of the dot, or to the left when it would run past the edge.
            String text = i == 0 ? "head " + sequence[i] : String.valueOf(sequence[i]);
            int textWidth = fm.stringWidth(text);
            int textX = x + POINT_SIZE;
            if (textX + textWidth > getWidth() - 2) {
                textX = x - POINT_SIZE - textWidth;
            }
            g2.setColor(i == 0 ? START_COLOR : TEXT_COLOR);
            g2.drawString(text, textX, y + fm.getAscent() / 2 - 1);
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        for (int i = 0; i < sequence.length; i++) {
            if (Math.abs(event.getX() - xOf(sequence[i])) <= POINT_SIZE
                    && Math.abs(event.getY() - yOf(i)) <= POINT_SIZE) {
                if (i == 0) {
                    return "Start: head at cylinder " + sequence[0];
                }
                int moved = Math.abs(sequence[i] - sequence[i - 1]);
                return "Step " + i + ": " + sequence[i - 1] + " to " + sequence[i]
                        + " (" + moved + " cylinder" + (moved == 1 ? "" : "s") + ")";
            }
        }
        return null;
    }

    /** Converts a cylinder number to an x pixel position; the scale stretches to the panel's width. */
    private int xOf(int cylinder) {
        int usable = Math.max(1, getWidth() - 2 * SIDE_MARGIN);
        double fraction = diskSize <= 1 ? 0 : (double) cylinder / (diskSize - 1);
        return SIDE_MARGIN + (int) Math.round(fraction * usable);
    }

    /** Converts a step number (0 = initial head position) to a y pixel position. */
    private static int yOf(int step) {
        return FIRST_ROW_Y + step * ROW_HEIGHT;
    }
}
