package sstf;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import gui.SeekPathPanel;
import model.DiskResult;
import scheduler.DiskScheduler;

/**
 * Input/output screen for Shortest Seek Time First (SSTF) disk scheduling.
 * Reads and validates the request queue, head position and disk size, runs the scheduler,
 * and shows the seek sequence, the movement of each step, the totals, and the head's path.
 * All scheduling logic lives in {@link SSTF}; this class only handles the GUI.
 */
public class SstfPanel extends JPanel {

    private static final String WINDOW_TITLE = "Shortest Seek Time First (SSTF) Disk Scheduling";

    private static final int DEFAULT_DISK_SIZE = 200;
    /** Largest allowed disk size, so cylinders run from 0 to 9999 (same limit as the CPU modules' inputs). */
    private static final int MAX_DISK_SIZE = 10000;
    private static final int MAX_REQUESTS = 100;
    /** Largest allowed seek rate (time per cylinder). */
    private static final double MAX_SEEK_RATE = 1000;

    private static final String[] RESULT_COLUMNS = {"Step", "From", "To", "Movement", "Running Total"};

    /** One request in the queue text: anything between commas, semicolons or whitespace. */
    private static final Pattern REQUEST_TOKEN = Pattern.compile("[^,;\\s]+");
    private static final Pattern WHOLE_NUMBER = Pattern.compile("[+-]?\\d+");
    private static final Pattern DECIMAL_NUMBER = Pattern.compile("[+-]?(\\d+\\.\\d*|\\.\\d+)");
    /** A seek rate may be a whole number or a decimal, such as 4 or 0.5. */
    private static final Pattern WHOLE_OR_DECIMAL = Pattern.compile("\\+?(\\d+(\\.\\d*)?|\\.\\d+)");
    private static final Pattern RATE_NUMBER = Pattern.compile("\\d{1,4}(\\.\\d{0,3})?|\\.\\d{1,3}");
    private static final String NO_VALUE = "-";

    private final DiskScheduler scheduler = new SSTF();

    /** True for a fixed sample display: inputs cannot be edited and there are no action buttons. */
    private final boolean readOnly;

    private final JTextField requestField = new JTextField(46);
    private final JTextField headField = new JTextField(6);
    private final JTextField diskSizeField = new JTextField(String.valueOf(DEFAULT_DISK_SIZE), 6);
    private final JTextField seekRateField = new JTextField(6);
    private final JLabel requestCountLabel = new JLabel();

    private final JTextArea sequenceText = new JTextArea(2, 40);
    private final DefaultTableModel resultModel = new DefaultTableModel(RESULT_COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable resultTable = new JTable(resultModel);
    private final SeekPathPanel seekPath = new SeekPathPanel();

    private final JLabel totalMovementValue = new JLabel(NO_VALUE);
    private final JLabel averageSeekValue = new JLabel(NO_VALUE);
    private final JLabel requestsServedValue = new JLabel(NO_VALUE);
    private final JLabel seekTimeValue = new JLabel(NO_VALUE);

    /**
     * Opens this module in its own window, hiding the menu until the window closes.
     * Matches {@link gui.ModuleLauncher}, so the menu can use SstfPanel::openWindow.
     *
     * @param sampleView true for the read-only Sample window (both sample problems, computed);
     *                   false for the normal editable module
     */
    public static void openWindow(final JFrame menu, boolean sampleView) {
        JFrame frame = new JFrame(sampleView ? WINDOW_TITLE + " - Sample" : WINDOW_TITLE);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(sampleView ? buildSampleWindowContent() : new SstfPanel());
        // Both "Back to Menu" and the window's X button dispose the frame, which brings the menu back.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                menu.setVisible(true);
            }
        });

        // Fit small laptop screens: never larger than the usable screen area.
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setSize(Math.min(1000, screen.width), Math.min(820, screen.height));
        frame.setMinimumSize(new Dimension(Math.min(700, screen.width), Math.min(520, screen.height)));
        frame.setLocationRelativeTo(null);

        menu.setVisible(false);
        frame.setVisible(true);
    }

    /**
     * The Sample window: a Back to Menu button above the sample problems, stacked
     * vertically in one scrollable area.
     */
    private static JPanel buildSampleWindowContent() {
        JPanel samples = new JPanel();
        samples.setLayout(new BoxLayout(samples, BoxLayout.Y_AXIS));
        samples.add(new SstfPanel("Sample 1: 20 requests, head at 100 (same data as the C-LOOK Sample 1)",
                SstfSampleData.tiesAndDuplicates()));
        samples.add(new SstfPanel("Sample 2: textbook problem, 8 requests, head at 53 (same data as the C-LOOK Sample 2)",
                SstfSampleData.textbook()));

        JScrollPane scroll = new JScrollPane(samples,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setBorder(null);

        final JButton backButton = new JButton("Back to Menu");
        backButton.addActionListener(e -> SwingUtilities.getWindowAncestor(backButton).dispose());
        JLabel hint = new JLabel("Sample problems (read-only). To enter your own data, open SSTF from the menu.");
        hint.setForeground(Color.DARK_GRAY);
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topBar.add(backButton);
        topBar.add(hint);

        JPanel content = new JPanel(new BorderLayout());
        content.add(topBar, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        return content;
    }

    /** The normal, editable module, starting with an empty request queue and the default disk size. */
    public SstfPanel() {
        super(new BorderLayout());
        this.readOnly = false;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        configureOutputs();

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildInputSection(), buildOutputSection());
        split.setResizeWeight(0); // extra height goes to the output section
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        // Any change to the inputs makes the shown results stale, so clear them.
        DocumentListener inputChanged = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                inputsChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                inputsChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                inputsChanged();
            }
        };
        requestField.getDocument().addDocumentListener(inputChanged);
        headField.getDocument().addDocumentListener(inputChanged);
        diskSizeField.getDocument().addDocumentListener(inputChanged);
        seekRateField.getDocument().addDocumentListener(inputChanged);
        updateRequestCount();
    }

    /**
     * A fixed, read-only display of one sample problem. The inputs are filled in and the
     * results are computed by the algorithm, exactly as if Compute had been clicked.
     */
    public SstfPanel(String heading, SstfSampleData.DiskProblem sample) {
        super(new BorderLayout(0, 4));
        this.readOnly = true;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 8, 8, 8),
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), heading)));

        configureOutputs();
        fillInputs(sample);
        requestField.setEditable(false);
        headField.setEditable(false);
        diskSizeField.setEditable(false);
        seekRateField.setEditable(false);
        updateRequestCount();
        compute();

        // Show every row: the Sample window scrolls as a whole, so the table needs no scrollbar.
        resultTable.setPreferredScrollableViewportSize(
                new Dimension(360, resultTable.getRowCount() * resultTable.getRowHeight()));

        add(buildInputSection(), BorderLayout.NORTH);
        add(buildOutputSection(), BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------- layout

    private void configureOutputs() {
        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(SwingConstants.CENTER);
        resultTable.getTableHeader().setReorderingAllowed(false);
        resultTable.setDefaultRenderer(Object.class, centered);
        resultTable.setRowHeight(22);
        resultTable.setPreferredScrollableViewportSize(new Dimension(360, 22 * 8));

        sequenceText.setEditable(false);
        sequenceText.setLineWrap(true);
        sequenceText.setWrapStyleWord(true);
        sequenceText.setOpaque(false);
        sequenceText.setFont(sequenceText.getFont().deriveFont(Font.BOLD, 13f));
        sequenceText.setText(NO_VALUE);
    }

    private JPanel buildInputSection() {
        JPanel fields = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        fields.add(new JLabel("Request queue (cylinders):"), c);
        c.gridx = 1;
        c.gridwidth = 3;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        fields.add(requestField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        fields.add(new JLabel("Initial head (current position):"), c);
        c.gridx = 1;
        fields.add(headField, c);
        c.gridx = 2;
        fields.add(new JLabel("Disk size (track size, cylinders):"), c);
        c.gridx = 3;
        fields.add(diskSizeField, c);

        c.gridx = 0;
        c.gridy = 2;
        fields.add(new JLabel("Seek rate (ms per cylinder):"), c);
        c.gridx = 1;
        fields.add(seekRateField, c);
        c.gridx = 2;
        c.gridwidth = 2;
        JLabel rateHint = new JLabel("optional; used for the seek time");
        rateHint.setForeground(Color.DARK_GRAY);
        fields.add(rateHint, c);

        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 3;
        requestCountLabel.setForeground(Color.DARK_GRAY);
        fields.add(requestCountLabel, c);

        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.add(fields, BorderLayout.NORTH);

        if (readOnly) {
            section.setBorder(BorderFactory.createTitledBorder("Input (read-only)"));
            return section;
        }

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

        JLabel hint = new JLabel("Separate requests with commas or spaces. Cylinders run from 0 to disk size - 1."
                + " Ties go to the lower cylinder.");
        hint.setForeground(Color.DARK_GRAY);

        JPanel south = new JPanel(new BorderLayout());
        south.add(hint, BorderLayout.NORTH);
        south.add(buttonRow, BorderLayout.SOUTH);

        section.setBorder(BorderFactory.createTitledBorder("Input"));
        section.add(south, BorderLayout.SOUTH);
        return section;
    }

    private JPanel buildOutputSection() {
        JPanel sequenceSection = new JPanel(new BorderLayout());
        sequenceSection.setBorder(BorderFactory.createTitledBorder("Seek Sequence (order of service)"));
        sequenceSection.add(sequenceText, BorderLayout.CENTER);

        JPanel graphSection = new JPanel(new BorderLayout());
        graphSection.setBorder(BorderFactory.createTitledBorder("Head Path"));
        JPanel tableSection = new JPanel(new BorderLayout());
        tableSection.setBorder(BorderFactory.createTitledBorder("Head Movement per Step"));

        if (readOnly) {
            // The Sample window scrolls as a whole, so show the graph and table at full height.
            graphSection.add(seekPath, BorderLayout.CENTER);
            JPanel table = new JPanel(new BorderLayout());
            table.add(resultTable.getTableHeader(), BorderLayout.NORTH);
            table.add(resultTable, BorderLayout.CENTER);
            tableSection.add(table, BorderLayout.NORTH);
        } else {
            JScrollPane graphScroll = new JScrollPane(seekPath,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            graphScroll.getViewport().setBackground(Color.WHITE);
            graphScroll.getVerticalScrollBar().setUnitIncrement(20);
            graphSection.add(graphScroll, BorderLayout.CENTER);
            tableSection.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        }

        JPanel middle = new JPanel(new GridLayout(1, 2, 6, 0));
        middle.add(graphSection);
        middle.add(tableSection);

        JPanel summary = new JPanel(new GridLayout(0, 4, 12, 4));
        summary.setBorder(BorderFactory.createTitledBorder("Summary"));
        addSummaryItem(summary, "Total Head Movement:", totalMovementValue);
        addSummaryItem(summary, "Average Seek Distance:", averageSeekValue);
        addSummaryItem(summary, "Requests Serviced:", requestsServedValue);
        addSummaryItem(summary, "Seek Time:", seekTimeValue);

        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.add(sequenceSection, BorderLayout.NORTH);
        section.add(middle, BorderLayout.CENTER);
        section.add(summary, BorderLayout.SOUTH);
        return section;
    }

    private static void addSummaryItem(JPanel summary, String name, JLabel value) {
        summary.add(new JLabel(name, SwingConstants.RIGHT));
        value.setFont(value.getFont().deriveFont(Font.BOLD));
        summary.add(value);
    }

    // ---------------------------------------------------------------- actions

    /** Validates the inputs, runs the scheduler, and shows the results. */
    private void compute() {
        int diskSize;
        int head;
        int[] requests;
        Double seekRate;
        try {
            // Disk size first: the head and the requests are checked against it.
            diskSize = readDiskSize();
            head = readCylinder(headField, "Initial head position", diskSize);
            requests = readRequests(diskSize);
            seekRate = readSeekRate();
        } catch (InvalidInputException ex) {
            ex.field.requestFocusInWindow();
            if (ex.selectionEnd > ex.selectionStart) {
                ex.field.select(ex.selectionStart, ex.selectionEnd);
            } else {
                ex.field.selectAll();
            }
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // SSTF does not use a direction, so none is passed.
        showResult(scheduler.schedule(requests, head, diskSize, null), diskSize, seekRate);
    }

    /** Fills the inputs with Sample 1. Results still have to be computed. */
    private void loadSampleData() {
        fillInputs(SstfSampleData.tiesAndDuplicates());
    }

    private void fillInputs(SstfSampleData.DiskProblem sample) {
        StringBuilder text = new StringBuilder();
        for (int request : sample.getRequests()) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(request);
        }
        requestField.setText(text.toString());
        headField.setText(String.valueOf(sample.getHead()));
        diskSizeField.setText(String.valueOf(sample.getDiskSize()));
        seekRateField.setText(formatNumber(sample.getSeekRate()));
    }

    /** Empties every input and output; the disk size goes back to the default. */
    private void reset() {
        requestField.setText("");
        headField.setText("");
        diskSizeField.setText(String.valueOf(DEFAULT_DISK_SIZE));
        seekRateField.setText("");
        clearOutputs();
    }

    private void closeWindow() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }

    private void inputsChanged() {
        updateRequestCount();
        clearOutputs();
    }

    /** Shows how many requests have been typed so far, so the user can check the count before computing. */
    private void updateRequestCount() {
        int count = 0;
        Matcher m = REQUEST_TOKEN.matcher(requestField.getText());
        while (m.find()) {
            count++;
        }
        requestCountLabel.setText("Requests entered: " + count + " (max " + MAX_REQUESTS + ")");
    }

    // ---------------------------------------------------------------- output

    /** @param seekRate time per cylinder, or null when no seek rate was entered */
    private void showResult(DiskResult result, int diskSize, Double seekRate) {
        int[] sequence = result.getSeekSequence();
        int[] steps = result.getStepMovements();

        StringBuilder text = new StringBuilder();
        for (int i = 0; i < sequence.length; i++) {
            text.append(i == 0 ? "" : " → ").append(sequence[i]);
        }
        sequenceText.setText(text.toString());

        resultModel.setRowCount(0);
        int runningTotal = 0;
        for (int i = 0; i < steps.length; i++) {
            runningTotal += steps[i];
            resultModel.addRow(new Object[] {i + 1, sequence[i], sequence[i + 1], steps[i], runningTotal});
        }

        seekPath.setPath(sequence, diskSize);

        totalMovementValue.setText(result.getTotalHeadMovement() + " cylinders");
        averageSeekValue.setText(twoDecimals(result.getAverageSeekDistance()) + " cylinders  ("
                + result.getTotalHeadMovement() + " / " + result.getRequestCount() + ")");
        requestsServedValue.setText(String.valueOf(result.getRequestCount()));
        seekTimeValue.setText(seekRate == null ? "- (no seek rate entered)"
                : formatNumber(result.getSeekTime(seekRate)) + " ms  ("
                        + result.getTotalHeadMovement() + " \u00d7 " + formatNumber(seekRate) + ")");
    }

    private void clearOutputs() {
        sequenceText.setText(NO_VALUE);
        resultModel.setRowCount(0);
        seekPath.clear();
        totalMovementValue.setText(NO_VALUE);
        averageSeekValue.setText(NO_VALUE);
        requestsServedValue.setText(NO_VALUE);
        seekTimeValue.setText(NO_VALUE);
    }

    private static String twoDecimals(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    /** Shows whole values without decimals (4, 1196) and others with up to three (0.5, 149.5). */
    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.3f", value).replaceFirst("0+$", "");
    }

    // ---------------------------------------------------------------- input validation

    private int readDiskSize() throws InvalidInputException {
        String name = "Disk size";
        String text = diskSizeField.getText().trim();
        int value = parseWholeNumber(text, name, diskSizeField, 0, 0);
        if (value < 1 || value > MAX_DISK_SIZE) {
            throw new InvalidInputException(diskSizeField, name + " must be from 1 to " + MAX_DISK_SIZE
                    + " (you entered " + text + ").");
        }
        return value;
    }

    /** Reads one cylinder number from a field and checks that it lies on the disk. */
    private int readCylinder(JTextField field, String name, int diskSize) throws InvalidInputException {
        String text = field.getText().trim();
        int value = parseWholeNumber(text, name, field, 0, 0);
        if (value >= diskSize) {
            throw new InvalidInputException(field, name + " must be from 0 to " + (diskSize - 1)
                    + " for a disk of " + diskSize + " cylinders (you entered " + text + ").");
        }
        return value;
    }

    /** Reads the optional seek rate: null when blank, otherwise a number from 0 to MAX_SEEK_RATE. */
    private Double readSeekRate() throws InvalidInputException {
        String text = seekRateField.getText().trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.startsWith("-") && WHOLE_OR_DECIMAL.matcher(text.substring(1)).matches()) {
            throw new InvalidInputException(seekRateField,
                    "Seek rate (\"" + text + "\") is negative. Negative numbers are not allowed.");
        }
        if (!WHOLE_OR_DECIMAL.matcher(text).matches()) {
            throw new InvalidInputException(seekRateField,
                    "Seek rate (\"" + text + "\") is not a number. Please enter a number such as 4 or 0.5.");
        }
        String unsigned = text.replaceFirst("^\\+", "").replaceFirst("^0+(?=\\d)", "");
        if (!RATE_NUMBER.matcher(unsigned).matches() || Double.parseDouble(unsigned) > MAX_SEEK_RATE) {
            throw new InvalidInputException(seekRateField, "Seek rate (\"" + text + "\") must be at most "
                    + formatNumber(MAX_SEEK_RATE) + " with up to 3 decimal places.");
        }
        return Double.parseDouble(unsigned);
    }

    /** Reads the request queue, stopping at the first invalid request and selecting it in the field. */
    private int[] readRequests(int diskSize) throws InvalidInputException {
        List<Integer> requests = new ArrayList<Integer>();
        Matcher m = REQUEST_TOKEN.matcher(requestField.getText());
        while (m.find()) {
            int number = requests.size() + 1;
            String name = "Request #" + number;
            int value = parseWholeNumber(m.group(), name, requestField, m.start(), m.end());
            if (value >= diskSize) {
                throw new InvalidInputException(requestField, m.start(), m.end(), name + " (" + m.group()
                        + ") is outside the disk. Cylinders must be from 0 to " + (diskSize - 1) + ".");
            }
            requests.add(value);
            if (requests.size() > MAX_REQUESTS) {
                throw new InvalidInputException(requestField, m.start(), m.end(),
                        "Too many requests. The maximum is " + MAX_REQUESTS + ".");
            }
        }
        if (requests.isEmpty()) {
            throw new InvalidInputException(requestField,
                    "The request queue is empty. Please enter at least one cylinder number, e.g. 98, 183, 37.");
        }

        int[] result = new int[requests.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = requests.get(i);
        }
        return result;
    }

    /**
     * Parses a whole number that is 0 or more, with a specific message for each mistake.
     * 'start' and 'end' mark the text to highlight in the field (0 and 0 = the whole field).
     */
    private static int parseWholeNumber(String text, String name, JTextField field, int start, int end)
            throws InvalidInputException {
        if (text.isEmpty()) {
            throw new InvalidInputException(field, name + " is empty. Please enter a whole number.");
        }
        if (DECIMAL_NUMBER.matcher(text).matches()) {
            throw new InvalidInputException(field, start, end,
                    name + " (\"" + text + "\") is a decimal. Only whole numbers are allowed.");
        }
        if (!WHOLE_NUMBER.matcher(text).matches()) {
            throw new InvalidInputException(field, start, end,
                    name + " (\"" + text + "\") is not a number. Please enter a whole number.");
        }

        // Drop the sign and leading zeros, then check size before parsing so huge inputs cannot overflow.
        String digits = text.replaceFirst("^[+-]", "").replaceFirst("^0+(?=\\d)", "");
        if (text.startsWith("-") && !digits.equals("0")) {
            throw new InvalidInputException(field, start, end,
                    name + " (\"" + text + "\") is negative. Negative numbers are not allowed.");
        }
        if (digits.length() > String.valueOf(MAX_DISK_SIZE).length() || Integer.parseInt(digits) > MAX_DISK_SIZE) {
            throw new InvalidInputException(field, start, end,
                    name + " (\"" + text + "\") is too large. The maximum is " + MAX_DISK_SIZE + ".");
        }
        return Integer.parseInt(digits);
    }

    /** An invalid input; remembers which field (and which part of its text) to highlight. */
    private static final class InvalidInputException extends Exception {
        final JTextField field;
        final int selectionStart;
        final int selectionEnd;

        InvalidInputException(JTextField field, String problem) {
            this(field, 0, 0, problem);
        }

        InvalidInputException(JTextField field, int selectionStart, int selectionEnd, String problem) {
            super(problem);
            this.field = field;
            this.selectionStart = selectionStart;
            this.selectionEnd = selectionEnd;
        }
    }
}
/**DONE */