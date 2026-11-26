package clook;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import clook.CLookSampleData.DiskSample;
import gui.SeekChartPanel;
import model.DiskResult;
import scheduler.DiskScheduler;

/**
 * Input/output screen for C-LOOK disk scheduling.
 * Reads and validates the disk parameters and request queue, runs the scheduler,
 * and shows the seek sequence chart, step-by-step table, and summary metrics.
 */
public class CLookPanel extends JPanel {

    private static final String WINDOW_TITLE = "C-LOOK Disk Scheduling";

    private static final int MIN_REQUESTS = 1;
    private static final int MAX_REQUESTS = 50;
    private static final int DEFAULT_REQUESTS = 8;
    private static final int DEFAULT_DISK_SIZE = 200;
    /** Largest allowed disk size, so cylinders run from 0 to 9999 (same limit as the other modules). */
    private static final int MAX_DISK_SIZE = 10000;
    /** Largest allowed seek rate (time per cylinder). */
    private static final double MAX_SEEK_RATE = 1000;

    private static final int COL_REQ_NUM = 0;
    private static final int COL_CYLINDER = 1;
    private static final String[] INPUT_COLUMNS = {"Request #", "Cylinder"};
    private static final String[] RESULT_COLUMNS = {"Step", "From Cylinder", "To Cylinder", "Head Movement", "Running Total"};

    private static final Pattern WHOLE_NUMBER = Pattern.compile("[+-]?\\d+");
    private static final Pattern DECIMAL_NUMBER = Pattern.compile("[+-]?(\\d+\\.\\d*|\\.\\d+)");
    /** A seek rate may be a whole number or a decimal, such as 4 or 0.5. */
    private static final Pattern WHOLE_OR_DECIMAL = Pattern.compile("\\+?(\\d+(\\.\\d*)?|\\.\\d+)");
    private static final Pattern RATE_NUMBER = Pattern.compile("\\d{1,4}(\\.\\d{0,3})?|\\.\\d{1,3}");
    private static final String NO_VALUE = "-";

    private final DiskScheduler scheduler = new CLook();
    private final boolean readOnly;

    private final JSpinner requestCountSpinner =
            new JSpinner(new SpinnerNumberModel(DEFAULT_REQUESTS, MIN_REQUESTS, MAX_REQUESTS, 1));
    private final JTextField headField = new JTextField(6);
    private final JTextField diskSizeField = new JTextField(String.valueOf(DEFAULT_DISK_SIZE), 6);
    private final JTextField seekRateField = new JTextField(5);
    private final JComboBox<String> directionCombo = new JComboBox<String>(new String[] {"Up", "Down"});

    private final DefaultTableModel inputModel = new DefaultTableModel(INPUT_COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return !CLookPanel.this.readOnly && column == COL_CYLINDER;
        }
    };
    private final JTable inputTable = new JTable(inputModel);

    private final DefaultTableModel resultModel = new DefaultTableModel(RESULT_COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable resultTable = new JTable(resultModel);
    private final SeekChartPanel seekChart = new SeekChartPanel();

    private final JLabel totalHeadMovementValue = new JLabel(NO_VALUE);
    private final JLabel averageSeekLengthValue = new JLabel(NO_VALUE);
    private final JLabel seekTimeValue = new JLabel(NO_VALUE);
    private final JLabel seekSequenceValue = new JLabel(NO_VALUE);

    /**
     * Opens this module in its own window, hiding the menu until the window closes.
     * Matches {@link gui.ModuleLauncher}, so the menu can use CLookPanel::openWindow.
     */
    public static void openWindow(final JFrame menu, boolean sampleView) {
        JFrame frame = new JFrame(sampleView ? WINDOW_TITLE + " - Sample" : WINDOW_TITLE);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(sampleView ? buildSampleWindowContent() : new CLookPanel());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                menu.setVisible(true);
            }
        });

        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setSize(Math.min(1000, screen.width), Math.min(820, screen.height));
        frame.setMinimumSize(new Dimension(Math.min(700, screen.width), Math.min(520, screen.height)));
        frame.setLocationRelativeTo(null);

        menu.setVisible(false);
        frame.setVisible(true);
    }

    private static JPanel buildSampleWindowContent() {
        JPanel samples = new JPanel();
        samples.setLayout(new BoxLayout(samples, BoxLayout.Y_AXIS));
        samples.add(new CLookPanel("Sample 1: 20 requests, head at 100, moving Down (same data as the SSTF Sample 1)",
                CLookSampleData.tiesAndDuplicates()));
        samples.add(new CLookPanel("Sample 2: textbook problem, 8 requests, head at 53, moving Up (same data as the SSTF Sample 2)",
                CLookSampleData.textbook()));

        JScrollPane scroll = new JScrollPane(samples,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setBorder(null);

        final JButton backButton = new JButton("Back to Menu");
        backButton.addActionListener(e -> SwingUtilities.getWindowAncestor(backButton).dispose());
        JLabel hint = new JLabel("Sample problems (read-only). To enter your own data, open C-LOOK from the menu.");
        hint.setForeground(Color.DARK_GRAY);
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topBar.add(backButton);
        topBar.add(hint);

        JPanel content = new JPanel(new BorderLayout());
        content.add(topBar, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        return content;
    }

    /** The normal, editable module, starting with an empty request table. */
    public CLookPanel() {
        super(new BorderLayout());
        this.readOnly = false;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        configureTables();

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildInputSection(), buildOutputSection());
        split.setResizeWeight(0);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        inputModel.addTableModelListener(e -> clearOutputs());
        requestCountSpinner.addChangeListener(e -> resizeInputRows((Integer) requestCountSpinner.getValue()));
        directionCombo.addActionListener(e -> clearOutputs());

        DocumentListener clearOnEdit = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                clearOutputs();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                clearOutputs();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                clearOutputs();
            }
        };
        headField.getDocument().addDocumentListener(clearOnEdit);
        diskSizeField.getDocument().addDocumentListener(clearOnEdit);
        seekRateField.getDocument().addDocumentListener(clearOnEdit);

        resizeInputRows(DEFAULT_REQUESTS);
    }

    /** A fixed, read-only display of one sample problem, pre-computed. */
    public CLookPanel(String heading, DiskSample sample) {
        super(new BorderLayout(0, 4));
        this.readOnly = true;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 8, 8, 8),
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), heading)));

        configureTables();
        applySample(sample);
        compute();

        fitHeightToRows(inputTable);
        fitHeightToRows(resultTable);

        add(buildInputSection(), BorderLayout.NORTH);
        add(buildOutputSection(), BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------- layout

    private void configureTables() {
        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(SwingConstants.CENTER);

        inputTable.getTableHeader().setReorderingAllowed(false);
        inputTable.setCellSelectionEnabled(true);
        inputTable.setDefaultRenderer(Object.class, centered);
        inputTable.setRowHeight(22);
        inputTable.setPreferredScrollableViewportSize(new Dimension(520, 22 * 6));
        inputTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        resultTable.getTableHeader().setReorderingAllowed(false);
        resultTable.setDefaultRenderer(Object.class, centered);
        resultTable.setRowHeight(22);
        resultTable.setPreferredScrollableViewportSize(new Dimension(700, 22 * 6));
    }

    private static void fitHeightToRows(JTable table) {
        Dimension size = table.getPreferredScrollableViewportSize();
        table.setPreferredScrollableViewportSize(
                new Dimension(size.width, table.getRowCount() * table.getRowHeight()));
    }

    private JPanel buildInputSection() {
        JPanel section = new JPanel(new BorderLayout(0, 4));
        JPanel configRow = new JPanel(new GridLayout(0, 1, 0, 2));
        JPanel firstLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        JPanel secondLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));

        if (readOnly) {
            firstLine.add(new JLabel("Requests: " + inputModel.getRowCount()));
            firstLine.add(new JLabel("  |  Initial Head (current position): " + headField.getText()));
            firstLine.add(new JLabel("  |  Disk Size (track size): " + diskSizeField.getText()));
            secondLine.add(new JLabel("Direction: " + directionCombo.getSelectedItem()));
            secondLine.add(new JLabel("  |  Seek Rate: " + (seekRateField.getText().isEmpty()
                    ? "(none)" : seekRateField.getText() + " ms per cylinder")));
            configRow.add(firstLine);
            configRow.add(secondLine);
            section.setBorder(BorderFactory.createTitledBorder("Input (read-only)"));
            section.add(configRow, BorderLayout.NORTH);
            section.add(new JScrollPane(inputTable), BorderLayout.CENTER);
            return section;
        }

        firstLine.add(new JLabel("Requests (" + MIN_REQUESTS + "-" + MAX_REQUESTS + "):"));
        firstLine.add(requestCountSpinner);
        firstLine.add(new JLabel("Initial Head (current position):"));
        firstLine.add(headField);
        firstLine.add(new JLabel("Disk Size (track size):"));
        firstLine.add(diskSizeField);
        secondLine.add(new JLabel("Direction:"));
        secondLine.add(directionCombo);
        secondLine.add(new JLabel("Seek Rate (ms per cylinder):"));
        secondLine.add(seekRateField);
        configRow.add(firstLine);
        configRow.add(secondLine);

        JButton computeButton = new JButton("Compute");
        JButton sampleButton = new JButton("Load Sample Data");
        JButton resetButton = new JButton("Reset/Clear");
        JButton backButton = new JButton("Back to Menu");
        computeButton.addActionListener(e -> compute());
        sampleButton.addActionListener(e -> loadSampleData());
        resetButton.addActionListener(e -> reset());
        backButton.addActionListener(e -> closeWindow());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonRow.add(computeButton);
        buttonRow.add(sampleButton);
        buttonRow.add(resetButton);
        buttonRow.add(backButton);

        JLabel hint = new JLabel("Cylinders are whole numbers from 0 to Disk Size - 1. Direction: Up (increasing) or Down (decreasing)."
                + " Seek rate is optional.");
        hint.setForeground(Color.DARK_GRAY);

        JPanel south = new JPanel(new BorderLayout());
        south.add(hint, BorderLayout.NORTH);
        south.add(buttonRow, BorderLayout.SOUTH);

        // Always keep at least 5 rows of the input table visible, so it can never collapse to just
        // its header on short (laptop) screens; the output section shrinks instead.
        JScrollPane inputScroll = new JScrollPane(inputTable);
        inputScroll.setMinimumSize(new Dimension(200, inputTable.getRowHeight() * 5 + 26));

        section.setBorder(BorderFactory.createTitledBorder("Input"));
        section.add(configRow, BorderLayout.NORTH);
        section.add(inputScroll, BorderLayout.CENTER);
        section.add(south, BorderLayout.SOUTH);
        section.setMinimumSize(section.getPreferredSize());
        return section;
    }

    private JPanel buildOutputSection() {
        JScrollPane chartScroll = new JScrollPane(seekChart,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        chartScroll.getViewport().setBackground(Color.WHITE);
        chartScroll.getHorizontalScrollBar().setUnitIncrement(20);
        chartScroll.getVerticalScrollBar().setUnitIncrement(20);

        JPanel chartSection = new JPanel(new BorderLayout());
        chartSection.setBorder(BorderFactory.createTitledBorder("Seek Sequence Chart"));
        chartSection.add(chartScroll, BorderLayout.CENTER);
        JPanel resultsSection = new JPanel(new BorderLayout());
        resultsSection.setBorder(BorderFactory.createTitledBorder("Step-by-Step Seek Operations"));
        resultsSection.add(new JScrollPane(resultTable), BorderLayout.CENTER);

        // Sample window: it scrolls as a whole, so the chart is shown at full height (jump included),
        // with the step table below it.
        // Editable window: chart and step table sit side by side (like SSTF) and each scrolls in its
        // own box, so the window fits short laptop screens without squeezing the input table.
        JPanel chartAndSteps;
        if (readOnly) {
            chartSection.setPreferredSize(new Dimension(700, seekChart.getPreferredSize().height + 30));
            chartAndSteps = new JPanel(new BorderLayout(0, 4));
            chartAndSteps.add(chartSection, BorderLayout.NORTH);
            chartAndSteps.add(resultsSection, BorderLayout.CENTER);
        } else {
            resultTable.setPreferredScrollableViewportSize(new Dimension(360, 22 * 6));
            chartAndSteps = new JPanel(new GridLayout(1, 2, 6, 0));
            chartAndSteps.add(chartSection);
            chartAndSteps.add(resultsSection);
            chartAndSteps.setMinimumSize(new Dimension(200, 120));
        }

        JPanel metricsGrid = new JPanel(new GridLayout(0, 4, 12, 4));
        addSummaryItem(metricsGrid, "Total Head Movement:", totalHeadMovementValue);
        addSummaryItem(metricsGrid, "Average Seek Distance:", averageSeekLengthValue);
        addSummaryItem(metricsGrid, "Seek Time:", seekTimeValue);

        JPanel seqRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JLabel seqLabel = new JLabel("Seek Sequence:");
        seekSequenceValue.setFont(seekSequenceValue.getFont().deriveFont(Font.BOLD));
        seqRow.add(seqLabel);
        seqRow.add(seekSequenceValue);

        JPanel summary = new JPanel(new BorderLayout(0, 4));
        summary.setBorder(BorderFactory.createTitledBorder("Summary"));
        summary.add(metricsGrid, BorderLayout.NORTH);
        summary.add(seqRow, BorderLayout.SOUTH);

        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.add(chartAndSteps, BorderLayout.CENTER);
        section.add(summary, BorderLayout.SOUTH);
        return section;
    }

    private static void addSummaryItem(JPanel summary, String name, JLabel value) {
        summary.add(new JLabel(name, SwingConstants.RIGHT));
        value.setFont(value.getFont().deriveFont(Font.BOLD));
        summary.add(value);
    }

    // ---------------------------------------------------------------- actions

    private void compute() {
        stopEditing();
        int diskSize;
        int head;
        int[] requests;
        Double seekRate;
        try {
            diskSize = parseWholeNumber(diskSizeField.getText(), "Disk Size", 1, MAX_DISK_SIZE);
            head = parseWholeNumber(headField.getText(), "Initial Head", 0, MAX_DISK_SIZE);
            if (head > diskSize - 1) {
                throw new InvalidFieldException(headField, "Error in \"Initial Head\":\n\"" + head
                        + "\" is outside the disk. It must be from 0 to " + (diskSize - 1) + ".");
            }
            requests = readRequests(diskSize);
            seekRate = readSeekRate();
        } catch (InvalidFieldException ex) {
            ex.field.requestFocusInWindow();
            ex.field.selectAll();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (InvalidCellException ex) {
            inputTable.changeSelection(ex.row, ex.column, false, false);
            inputTable.requestFocusInWindow();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String direction = (String) directionCombo.getSelectedItem();
        DiskResult result = scheduler.schedule(requests, head, diskSize, direction);
        showResult(result, diskSize, seekRate);
    }

    /** Fills the inputs with Sample 1 (the same data SSTF loads). Results still have to be computed. */
    private void loadSampleData() {
        stopEditing();
        applySample(CLookSampleData.tiesAndDuplicates());
    }

    private void applySample(DiskSample sample) {
        int[] reqs = sample.getRequests();
        requestCountSpinner.setValue(reqs.length);
        resizeInputRows(reqs.length);
        diskSizeField.setText(String.valueOf(sample.getDiskSize()));
        headField.setText(String.valueOf(sample.getHead()));
        directionCombo.setSelectedItem(sample.getDirection());
        seekRateField.setText(formatNumber(sample.getSeekRate()));

        for (int row = 0; row < reqs.length; row++) {
            inputModel.setValueAt("R" + (row + 1), row, COL_REQ_NUM);
            inputModel.setValueAt(String.valueOf(reqs[row]), row, COL_CYLINDER);
        }
    }

    private void reset() {
        stopEditing();
        inputModel.setRowCount(0);
        requestCountSpinner.setValue(DEFAULT_REQUESTS);
        resizeInputRows(DEFAULT_REQUESTS);
        headField.setText("");
        diskSizeField.setText(String.valueOf(DEFAULT_DISK_SIZE));
        directionCombo.setSelectedIndex(0);
        seekRateField.setText("");
        clearOutputs();
    }

    private void closeWindow() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }

    private void resizeInputRows(int count) {
        stopEditing();
        while (inputModel.getRowCount() < count) {
            int reqNumber = inputModel.getRowCount() + 1;
            inputModel.addRow(new Object[] {"R" + reqNumber, ""});
        }
        while (inputModel.getRowCount() > count) {
            inputModel.removeRow(inputModel.getRowCount() - 1);
        }
    }

    private void stopEditing() {
        if (inputTable.isEditing()) {
            inputTable.getCellEditor().stopCellEditing();
        }
    }

    // ---------------------------------------------------------------- output

    /** @param seekRate time per cylinder, or null when no seek rate was entered */
    private void showResult(DiskResult result, int diskSize, Double seekRate) {
        int[] seq = result.getSeekSequence();
        int[] steps = result.getStepMovements();
        boolean up = !"Down".equalsIgnoreCase((String) directionCombo.getSelectedItem());
        seekChart.setResult(seq, diskSize, up ? 1 : -1);

        resultModel.setRowCount(0);
        int runningTotal = 0;
        for (int i = 0; i < steps.length; i++) {
            runningTotal += steps[i];
            resultModel.addRow(new Object[] {i + 1, seq[i], seq[i + 1], steps[i], runningTotal});
        }

        // The averages and seek time come from DiskResult, so SSTF and C-LOOK compute them the same way.
        int total = result.getTotalHeadMovement();
        totalHeadMovementValue.setText(total + " cylinders");
        averageSeekLengthValue.setText(twoDecimals(result.getAverageSeekDistance()) + " cylinders  ("
                + total + " / " + result.getRequestCount() + ")");
        seekTimeValue.setText(seekRate == null ? "- (no seek rate entered)"
                : formatNumber(result.getSeekTime(seekRate)) + " ms  (" + total + " \u00d7 "
                        + formatNumber(seekRate) + ")");

        StringBuilder seqText = new StringBuilder();
        for (int i = 0; i < seq.length; i++) {
            if (i > 0) {
                seqText.append(" -> ");
            }
            seqText.append(seq[i]);
        }
        seekSequenceValue.setText(seqText.toString());
    }

    private void clearOutputs() {
        seekChart.clear();
        resultModel.setRowCount(0);
        totalHeadMovementValue.setText(NO_VALUE);
        averageSeekLengthValue.setText(NO_VALUE);
        seekTimeValue.setText(NO_VALUE);
        seekSequenceValue.setText(NO_VALUE);
    }

    private static String twoDecimals(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    /** Shows whole values without decimals (4, 1288) and others with up to three (0.5, 161.5). */
    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.3f", value).replaceFirst("0+$", "");
    }

    // ---------------------------------------------------------------- input validation

    private int[] readRequests(int diskSize) throws InvalidCellException {
        int count = inputModel.getRowCount();
        int[] requests = new int[count];
        for (int row = 0; row < count; row++) {
            String text = cellText(row, COL_CYLINDER);
            try {
                requests[row] = validateWholeNumber(text, 0, MAX_DISK_SIZE);
            } catch (IllegalArgumentException ex) {
                throw new InvalidCellException(row, COL_CYLINDER, ex.getMessage());
            }
            if (requests[row] > diskSize - 1) {
                throw new InvalidCellException(row, COL_CYLINDER, "\"" + text + "\" is outside the disk."
                        + " Cylinders must be from 0 to " + (diskSize - 1) + ".");
            }
        }
        return requests;
    }

    /** Reads the optional seek rate: null when blank, otherwise a number from 0 to MAX_SEEK_RATE. */
    private Double readSeekRate() throws InvalidFieldException {
        String text = seekRateField.getText().trim();
        if (text.isEmpty()) {
            return null;
        }
        String prefix = "Error in \"Seek Rate\":\n";
        if (text.startsWith("-") && WHOLE_OR_DECIMAL.matcher(text.substring(1)).matches()) {
            throw new InvalidFieldException(seekRateField,
                    prefix + "\"" + text + "\" is negative. Negative numbers are not allowed.");
        }
        if (!WHOLE_OR_DECIMAL.matcher(text).matches()) {
            throw new InvalidFieldException(seekRateField,
                    prefix + "\"" + text + "\" is not a number. Please enter a number such as 4 or 0.5.");
        }
        String unsigned = text.replaceFirst("^\\+", "").replaceFirst("^0+(?=\\d)", "");
        if (!RATE_NUMBER.matcher(unsigned).matches() || Double.parseDouble(unsigned) > MAX_SEEK_RATE) {
            throw new InvalidFieldException(seekRateField, prefix + "\"" + text + "\" must be at most "
                    + formatNumber(MAX_SEEK_RATE) + " with up to 3 decimal places.");
        }
        return Double.parseDouble(unsigned);
    }

    private int parseWholeNumber(String rawText, String fieldName, int min, int max) throws InvalidFieldException {
        JTextField targetField = fieldName.equals("Disk Size") ? diskSizeField : headField;
        try {
            return validateWholeNumber(rawText == null ? "" : rawText.trim(), min, max);
        } catch (IllegalArgumentException ex) {
            throw new InvalidFieldException(targetField, "Error in \"" + fieldName + "\":\n" + ex.getMessage());
        }
    }

    private static int validateWholeNumber(String text, int min, int max) {
        if (text.isEmpty()) {
            throw new IllegalArgumentException("This field is empty. Please enter a whole number.");
        }
        if (DECIMAL_NUMBER.matcher(text).matches()) {
            throw new IllegalArgumentException("\"" + text + "\" is a decimal. Only whole numbers are allowed.");
        }
        if (!WHOLE_NUMBER.matcher(text).matches()) {
            throw new IllegalArgumentException("\"" + text + "\" is not a number. Please enter a whole number.");
        }

        String digits = text.replaceFirst("^[+-]", "").replaceFirst("^0+(?=\\d)", "");
        boolean negative = text.startsWith("-") && !digits.equals("0");
        if (negative) {
            throw new IllegalArgumentException("\"" + text + "\" is negative. Negative numbers are not allowed.");
        }
        if (digits.length() > String.valueOf(max).length() || Integer.parseInt(digits) > max) {
            throw new IllegalArgumentException("\"" + text + "\" exceeds the maximum allowed value of " + max + ".");
        }
        int value = Integer.parseInt(digits);
        if (value < min) {
            throw new IllegalArgumentException("The value must be at least " + min + " (you entered " + text + ").");
        }
        return value;
    }

    private String cellText(int row, int column) {
        Object value = inputModel.getValueAt(row, column);
        return value == null ? "" : value.toString().trim();
    }

    private static final class InvalidCellException extends Exception {
        final int row;
        final int column;

        InvalidCellException(int row, int column, String problem) {
            super("Error in row " + (row + 1) + ", column \"" + INPUT_COLUMNS[column] + "\":\n" + problem);
            this.row = row;
            this.column = column;
        }
    }

    private static final class InvalidFieldException extends Exception {
        final JTextField field;

        InvalidFieldException(JTextField field, String message) {
            super(message);
            this.field = field;
        }
    }
}
