package multilevelqueue;

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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import gui.GanttChartPanel;
import model.Process;
import model.ScheduleResult;
import scheduler.CpuScheduler;

/**
 * Input/output screen for Multilevel Queue scheduling.
 * Reads and validates the process table, runs the scheduler, and shows the
 * Gantt chart, the per-process results, and the summary metrics.
 * All scheduling logic lives in {@link MultilevelQueue}; this class only handles the GUI.
 */
public class MLQPanel extends JPanel {

    private static final String WINDOW_TITLE = "Multilevel Queue Scheduling";

    private static final int MIN_QUEUES = 2;
    private static final int MIN_PROCESSES = 10;
    private static final int MAX_PROCESSES = 20;
    private static final int DEFAULT_PROCESSES = MIN_PROCESSES;
    /** Largest allowed arrival, burst or priority value. Keeps sums far from int overflow. */
    private static final int MAX_INPUT_VALUE = 9999;

    private static final int COL_PID = 0;
    private static final int COL_ARRIVAL = 1;
    private static final int COL_BURST = 2;
    private static final int COL_PRIORITY = 3;
    private static final String[] INPUT_COLUMNS = {"PID", "Arrival Time", "Burst Time", "Queue"};
    private static final String[] RESULT_COLUMNS = {
        "PID", "Arrival", "Burst", "Queue", "Completion", "Turnaround", "Waiting", "Response"
    };

    private static final Pattern WHOLE_NUMBER = Pattern.compile("[+-]?\\d+");
    private static final Pattern DECIMAL_NUMBER = Pattern.compile("[+-]?(\\d+\\.\\d*|\\.\\d+)");
    private static final String NO_VALUE = "-";
    /** Largest allowed time quantum (same limit as the other numeric inputs). */
    private static final int MAX_QUANTUM = MAX_INPUT_VALUE;
    private static final int DEFAULT_QUANTUM = 4;

    /** Explains the queues; shown in both the editable window and the Sample window. */
    private final JLabel queueHint = new JLabel();

    /** True for a fixed sample display: inputs cannot be edited and there are no action buttons. */
    private final boolean readOnly;

    private final JSpinner processCountSpinner =
            new JSpinner(new SpinnerNumberModel(DEFAULT_PROCESSES, MIN_PROCESSES, MAX_PROCESSES, 1));
    private final JSpinner queueCountSpinner = new JSpinner(
            new SpinnerNumberModel(MultilevelQueue.defaultQueues().size(), MIN_QUEUES, MultilevelQueue.MAX_QUEUES, 1));
    /** One algorithm chooser and one quantum field per possible queue; only the first N are shown. */
    private final List<JComboBox<MultilevelQueue.Algorithm>> algorithmBoxes =
            new ArrayList<JComboBox<MultilevelQueue.Algorithm>>();
    private final List<JSpinner> quantumSpinners = new ArrayList<JSpinner>();
    private final List<JPanel> queueRows = new ArrayList<JPanel>();
    private final JPanel queueGrid = new JPanel(new GridLayout(0, 2, 12, 2));
    private JSplitPane splitPane;
    private final DefaultTableModel inputModel = new DefaultTableModel(INPUT_COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return !MLQPanel.this.readOnly;
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
    private final GanttChartPanel ganttChart = new GanttChartPanel();

    private final JLabel avgTurnaroundValue = new JLabel(NO_VALUE);
    private final JLabel avgWaitingValue = new JLabel(NO_VALUE);
    private final JLabel avgResponseValue = new JLabel(NO_VALUE);
    private final JLabel cpuUtilizationValue = new JLabel(NO_VALUE);
    private final JLabel throughputValue = new JLabel(NO_VALUE);

    /**
     * Opens this module in its own window, hiding the menu until the window closes.
     * Matches {@link gui.ModuleLauncher}, so the menu can use MLQPanel::openWindow.
     *
     * @param sampleView true for the read-only Sample window (both sample problems, computed);
     *                   false for the normal editable module
     */
    public static void openWindow(final JFrame menu, boolean sampleView) {
        JFrame frame = new JFrame(sampleView ? WINDOW_TITLE + " - Sample" : WINDOW_TITLE);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(sampleView ? buildSampleWindowContent() : new MLQPanel());
        // Both "Back to Menu" and the window's X button dispose the frame, which brings the menu back.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                menu.setVisible(true);
            }
        });

        // Fit small laptop screens: never larger than the usable screen area.
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        frame.setSize(Math.min(1000, screen.width), Math.min(900, screen.height));
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
        samples.add(new MLQPanel("Sample 1: 10 processes (preemption between queues and an idle gap)",
                MLQSampleData.preemptionAndIdle()));
        samples.add(new MLQPanel("Sample 2: 5 processes, all arrive at time 0 (burst in ms)",
                MLQSampleData.allArriveAtZero()));

        JScrollPane scroll = new JScrollPane(samples,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setBorder(null);

        final JButton backButton = new JButton("Back to Menu");
        backButton.addActionListener(e -> SwingUtilities.getWindowAncestor(backButton).dispose());
        JLabel hint = new JLabel("Sample problems (read-only). To enter your own data, open Multilevel Queue from the menu.");
        hint.setForeground(Color.DARK_GRAY);
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        topBar.add(backButton);
        topBar.add(hint);

        JPanel content = new JPanel(new BorderLayout());
        content.add(topBar, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        return content;
    }

    /** The normal, editable module, starting with an empty input table. */
    public MLQPanel() {
        super(new BorderLayout());
        this.readOnly = false;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        configureTables();
        buildQueueControls();
        applyQueueConfigs(MultilevelQueue.defaultQueues());

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildInputSection(), buildOutputSection());
        splitPane.setResizeWeight(0); // extra height goes to the output section
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);

        // Any change to the inputs makes the shown results stale, so clear them.
        inputModel.addTableModelListener(e -> clearOutputs());
        processCountSpinner.addChangeListener(e -> resizeInputRows((Integer) processCountSpinner.getValue()));

        resizeInputRows(DEFAULT_PROCESSES);
    }

    /**
     * A fixed, read-only display of one sample problem. The inputs are filled in and the
     * results are computed by the algorithm, exactly as if Compute had been clicked.
     */
    public MLQPanel(String heading, List<Process> sample) {
        super(new BorderLayout(0, 4));
        this.readOnly = true;
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 8, 8, 8),
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), heading)));

        configureTables();
        buildQueueControls();   // not shown in the Sample window, but compute() reads the queue setup from them
        applyQueueConfigs(MultilevelQueue.defaultQueues());
        resizeInputRows(sample.size());
        fillInputRows(sample);
        compute();

        // Show every row: the Sample window scrolls as a whole, so the tables need no scrollbars.
        fitHeightToRows(inputTable);
        fitHeightToRows(resultTable);

        add(buildInputSection(), BorderLayout.NORTH);
        add(buildOutputSection(), BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------- layout

    private void configureTables() {
        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(SwingConstants.CENTER);

        // Column indices are used in error messages, so columns must not be reordered.
        inputTable.getTableHeader().setReorderingAllowed(false);
        inputTable.setCellSelectionEnabled(true); // lets an invalid cell be highlighted on its own
        inputTable.setDefaultRenderer(Object.class, centered);
        inputTable.setRowHeight(22);
        inputTable.setPreferredScrollableViewportSize(new Dimension(520, 22 * 6));
        // Commit a cell being edited when the user clicks a button instead of pressing Enter.
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
        JPanel countRow = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel hint = queueHint;
        hint.setForeground(Color.DARK_GRAY);

        if (readOnly) {
            countRow.add(new JLabel("Number of processes: " + inputModel.getRowCount()));
            section.setBorder(BorderFactory.createTitledBorder("Input (read-only)"));
            section.add(countRow, BorderLayout.NORTH);
            section.add(new JScrollPane(inputTable), BorderLayout.CENTER);
            section.add(hint, BorderLayout.SOUTH);
            return section;
        }

        countRow.add(new JLabel("Number of processes (" + MIN_PROCESSES + "-" + MAX_PROCESSES + "):"));
        countRow.add(processCountSpinner);

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

        JPanel south = new JPanel(new BorderLayout());
        south.add(hint, BorderLayout.NORTH);
        south.add(buttonRow, BorderLayout.SOUTH);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(countRow);
        north.add(buildQueueSetupPanel());

        section.setBorder(BorderFactory.createTitledBorder("Input"));
        section.add(north, BorderLayout.NORTH);
        section.add(new JScrollPane(inputTable), BorderLayout.CENTER);
        section.add(south, BorderLayout.SOUTH);
        return section;
    }

    /** The "Queue setup" box: number of queues, then an algorithm (and quantum) for each queue. */
    private JPanel buildQueueSetupPanel() {
        JPanel countRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        countRow.add(new JLabel("Number of queues (" + MIN_QUEUES + "-" + MultilevelQueue.MAX_QUEUES + "):"));
        countRow.add(queueCountSpinner);
        JLabel note = new JLabel("Queue 1 has the highest priority. Quantum is used by Round Robin only.");
        note.setForeground(Color.DARK_GRAY);
        countRow.add(note);

        JPanel setup = new JPanel(new BorderLayout());
        setup.setBorder(BorderFactory.createTitledBorder("Queue setup"));
        setup.add(countRow, BorderLayout.NORTH);
        setup.add(queueGrid, BorderLayout.CENTER);
        return setup;
    }

    /** Creates the algorithm chooser and quantum field for every possible queue (done once). */
    private void buildQueueControls() {
        for (int i = 0; i < MultilevelQueue.MAX_QUEUES; i++) {
            final JComboBox<MultilevelQueue.Algorithm> box =
                    new JComboBox<MultilevelQueue.Algorithm>(MultilevelQueue.Algorithm.values());
            final JSpinner quantum = new JSpinner(new SpinnerNumberModel(DEFAULT_QUANTUM, 1, MAX_QUANTUM, 1));
            quantum.setPreferredSize(new Dimension(70, quantum.getPreferredSize().height));

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 1));
            row.add(new JLabel("Queue " + (i + 1) + ":"));
            row.add(box);
            row.add(new JLabel("Quantum:"));
            row.add(quantum);

            // The quantum only matters for Round Robin, so the field is switched off otherwise.
            box.addActionListener(e -> {
                quantum.setEnabled(selectedAlgorithm(box).needsQuantum());
                onQueueSetupChanged();
            });
            quantum.addChangeListener(e -> onQueueSetupChanged());

            algorithmBoxes.add(box);
            quantumSpinners.add(quantum);
            queueRows.add(row);
        }
        queueCountSpinner.addChangeListener(e -> {
            showQueueRows((Integer) queueCountSpinner.getValue());
            onQueueSetupChanged();
        });
    }

    private static MultilevelQueue.Algorithm selectedAlgorithm(JComboBox<MultilevelQueue.Algorithm> box) {
        return (MultilevelQueue.Algorithm) box.getSelectedItem();
    }

    /** Shows the rows of the first 'count' queues and hides the rest. */
    private void showQueueRows(int count) {
        queueGrid.removeAll();
        for (int i = 0; i < count; i++) {
            queueGrid.add(queueRows.get(i));
        }
        queueGrid.revalidate();
        queueGrid.repaint();
        if (splitPane != null) {
            // The input part grows or shrinks with the queue rows. Wait until Swing has re-laid-out
            // the new rows, so the divider moves to the new preferred height.
            SwingUtilities.invokeLater(() -> splitPane.resetToPreferredSizes());
        }
    }

    /** Sets the number of queues and each queue's algorithm and quantum from a configuration. */
    private void applyQueueConfigs(List<MultilevelQueue.QueueConfig> configs) {
        queueCountSpinner.setValue(configs.size());
        for (int i = 0; i < configs.size(); i++) {
            MultilevelQueue.QueueConfig config = configs.get(i);
            algorithmBoxes.get(i).setSelectedItem(config.getAlgorithm());
            quantumSpinners.get(i).setValue(config.getAlgorithm().needsQuantum() ? config.getQuantum() : DEFAULT_QUANTUM);
            quantumSpinners.get(i).setEnabled(config.getAlgorithm().needsQuantum());
        }
        showQueueRows(configs.size());
        applyQueueConfigsToHint(configs);
    }

    /** A change to the queue setup makes the shown results stale, and the hint text out of date. */
    private void onQueueSetupChanged() {
        clearOutputs();
        try {
            applyQueueConfigsToHint(readQueueConfigs());
        } catch (InvalidQueueSetupException ex) {
            // The quantum box holds text that is not a number yet; the hint is refreshed on the next valid change.
        }
    }

    private void applyQueueConfigsToHint(List<MultilevelQueue.QueueConfig> configs) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < configs.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append("Queue ").append(i + 1);
            if (i == 0) {
                text.append(" = highest");
            }
            text.append(" (").append(configs.get(i).describe()).append(")");
        }
        text.append(". Times are whole units starting at 0.");
        queueHint.setText(text.toString());
    }

    /** Reads the queue setup from the controls; the quantum fields are checked here. */
    private List<MultilevelQueue.QueueConfig> readQueueConfigs() throws InvalidQueueSetupException {
        int count = (Integer) queueCountSpinner.getValue();
        List<MultilevelQueue.QueueConfig> configs = new ArrayList<MultilevelQueue.QueueConfig>();
        for (int i = 0; i < count; i++) {
            MultilevelQueue.Algorithm algorithm = selectedAlgorithm(algorithmBoxes.get(i));
            int quantum = 0;
            if (algorithm.needsQuantum()) {
                JSpinner spinner = quantumSpinners.get(i);
                try {
                    spinner.commitEdit(); // accept text that was typed but not confirmed yet
                } catch (ParseException ex) {
                    throw new InvalidQueueSetupException(i,
                            "The quantum must be a whole number from 1 to " + MAX_QUANTUM + ".");
                }
                quantum = (Integer) spinner.getValue();
            }
            configs.add(new MultilevelQueue.QueueConfig(algorithm, quantum));
        }
        return configs;
    }

    private JPanel buildOutputSection() {
        JScrollPane ganttScroll = new JScrollPane(ganttChart,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ganttScroll.getViewport().setBackground(Color.WHITE);
        ganttScroll.getHorizontalScrollBar().setUnitIncrement(20);
        JPanel ganttSection = new JPanel(new BorderLayout());
        ganttSection.setBorder(BorderFactory.createTitledBorder("Gantt Chart"));
        ganttSection.add(ganttScroll, BorderLayout.CENTER);
        // Room for the chart plus a horizontal scrollbar.
        ganttSection.setPreferredSize(new Dimension(700, ganttChart.getPreferredSize().height + 45));

        JPanel resultsSection = new JPanel(new BorderLayout());
        resultsSection.setBorder(BorderFactory.createTitledBorder("Results"));
        resultsSection.add(new JScrollPane(resultTable), BorderLayout.CENTER);

        JPanel summary = new JPanel(new GridLayout(0, 4, 12, 4));
        summary.setBorder(BorderFactory.createTitledBorder("Summary"));
        addSummaryItem(summary, "Average Turnaround Time:", avgTurnaroundValue);
        addSummaryItem(summary, "Average Waiting Time:", avgWaitingValue);
        addSummaryItem(summary, "Average Response Time:", avgResponseValue);
        addSummaryItem(summary, "CPU Utilization:", cpuUtilizationValue);
        addSummaryItem(summary, "Throughput:", throughputValue);

        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.add(ganttSection, BorderLayout.NORTH);
        section.add(resultsSection, BorderLayout.CENTER);
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
        stopEditing();
        CpuScheduler scheduler;
        try {
            scheduler = new MultilevelQueue(readQueueConfigs());
        } catch (InvalidQueueSetupException ex) {
            quantumSpinners.get(ex.queueIndex).requestFocusInWindow();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<Process> processes;
        try {
            processes = readProcesses();
        } catch (InvalidCellException ex) {
            inputTable.changeSelection(ex.row, ex.column, false, false);
            inputTable.requestFocusInWindow();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        showResult(scheduler.schedule(processes));
    }

    /** Fills the input table with Sample 1. Results still have to be computed. */
    private void loadSampleData() {
        stopEditing();
        List<Process> sample = MLQSampleData.preemptionAndIdle();
        applyQueueConfigs(MultilevelQueue.defaultQueues()); // the samples are written for the default 3 queues
        processCountSpinner.setValue(sample.size()); // the spinner listener resizes the table
        fillInputRows(sample);
    }

    /** Writes the given processes into the input table, one per row. The table must already have enough rows. */
    private void fillInputRows(List<Process> sample) {
        for (int row = 0; row < sample.size(); row++) {
            Process p = sample.get(row);
            inputModel.setValueAt(p.getPid(), row, COL_PID);
            inputModel.setValueAt(String.valueOf(p.getArrivalTime()), row, COL_ARRIVAL);
            inputModel.setValueAt(String.valueOf(p.getBurstTime()), row, COL_BURST);
            inputModel.setValueAt(String.valueOf(p.getPriority()), row, COL_PRIORITY);
        }
    }

    /** Empties every input and output and goes back to the default number of processes. */
    private void reset() {
        stopEditing();
        inputModel.setRowCount(0);
        applyQueueConfigs(MultilevelQueue.defaultQueues());
        processCountSpinner.setValue(DEFAULT_PROCESSES);
        // If the spinner already showed the default, it fires no change event, so rebuild the rows here.
        resizeInputRows(DEFAULT_PROCESSES);
        clearOutputs();
    }

    private void closeWindow() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }

    /** Adds or removes rows at the bottom so the table has 'count' rows. Existing values are kept. */
    private void resizeInputRows(int count) {
        stopEditing();
        while (inputModel.getRowCount() < count) {
            int processNumber = inputModel.getRowCount() + 1;
            inputModel.addRow(new Object[] {"P" + processNumber, "", "", ""});
        }
        while (inputModel.getRowCount() > count) {
            inputModel.removeRow(inputModel.getRowCount() - 1);
        }
    }

    /** Saves a cell that is still being edited, so its value is included. */
    private void stopEditing() {
        if (inputTable.isEditing()) {
            inputTable.getCellEditor().stopCellEditing();
        }
    }

    // ---------------------------------------------------------------- output

    private void showResult(ScheduleResult result) {
        ganttChart.setBlocks(result.getGanttBlocks());

        resultModel.setRowCount(0);
        for (Process p : result.getProcesses()) {
            resultModel.addRow(new Object[] {
                p.getPid(), p.getArrivalTime(), p.getBurstTime(), p.getPriority(),
                p.getCompletionTime(), p.getTurnaroundTime(), p.getWaitingTime(), p.getResponseTime()
            });
        }

        avgTurnaroundValue.setText(twoDecimals(result.getAverageTurnaround()) + " ms");
        avgWaitingValue.setText(twoDecimals(result.getAverageWaiting()) + " ms");
        avgResponseValue.setText(twoDecimals(result.getAverageResponse()) + " ms");
        cpuUtilizationValue.setText(twoDecimals(result.getCpuUtilization()) + "%  ("
                + result.getBusyTime() + " / " + result.getTotalTime() + ")");
        throughputValue.setText(twoDecimals(result.getThroughput()) + " processes/ms  ("
                + result.getProcesses().size() + " / " + result.getTotalTime() + ")");
    }

    private void clearOutputs() {
        ganttChart.clear();
        resultModel.setRowCount(0);
        avgTurnaroundValue.setText(NO_VALUE);
        avgWaitingValue.setText(NO_VALUE);
        avgResponseValue.setText(NO_VALUE);
        cpuUtilizationValue.setText(NO_VALUE);
        throughputValue.setText(NO_VALUE);
    }

    private static String twoDecimals(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    // ---------------------------------------------------------------- input validation

    /** Reads every row into a Process, stopping at the first invalid cell. */
    private List<Process> readProcesses() throws InvalidCellException {
        List<Process> processes = new ArrayList<Process>();
        Map<String, Integer> rowByPid = new HashMap<String, Integer>(); // upper-cased PID -> row index

        for (int row = 0; row < inputModel.getRowCount(); row++) {
            String pid = cellText(row, COL_PID);
            if (pid.isEmpty()) {
                throw new InvalidCellException(row, COL_PID, "This cell is empty. Please enter a process ID such as P" + (row + 1) + ".");
            }
            Integer firstRow = rowByPid.get(pid.toUpperCase(Locale.ROOT));
            if (firstRow != null) {
                throw new InvalidCellException(row, COL_PID, "\"" + pid + "\" is already used in row "
                        + (firstRow + 1) + ". Each PID must be unique.");
            }
            rowByPid.put(pid.toUpperCase(Locale.ROOT), row);

            int arrival = readWholeNumber(row, COL_ARRIVAL, 0);
            int burst = readWholeNumber(row, COL_BURST, 1);
            int priority = readQueue(row);
            processes.add(new Process(pid, arrival, burst, priority));
        }
        return processes;
    }

    /** Parses a cell as a whole number between 'min' and MAX_INPUT_VALUE, with a specific message for each mistake. */
    private int readWholeNumber(int row, int column, int min) throws InvalidCellException {
        String text = cellText(row, column);
        if (text.isEmpty()) {
            throw new InvalidCellException(row, column, "This cell is empty. Please enter a whole number.");
        }
        if (DECIMAL_NUMBER.matcher(text).matches()) {
            throw new InvalidCellException(row, column, "\"" + text + "\" is a decimal. Only whole numbers are allowed.");
        }
        if (!WHOLE_NUMBER.matcher(text).matches()) {
            throw new InvalidCellException(row, column, "\"" + text + "\" is not a number. Please enter a whole number.");
        }

        // Drop the sign and leading zeros, then check size before parsing so huge inputs cannot overflow.
        String digits = text.replaceFirst("^[+-]", "").replaceFirst("^0+(?=\\d)", "");
        boolean negative = text.startsWith("-") && !digits.equals("0");
        if (negative) {
            throw new InvalidCellException(row, column, "\"" + text + "\" is negative. Negative numbers are not allowed.");
        }
        if (digits.length() > String.valueOf(MAX_INPUT_VALUE).length() || Integer.parseInt(digits) > MAX_INPUT_VALUE) {
            throw new InvalidCellException(row, column, "\"" + text + "\" is too large. The maximum is " + MAX_INPUT_VALUE + ".");
        }
        int value = Integer.parseInt(digits);
        if (value < min) {
            throw new InvalidCellException(row, column, "The value must be at least " + min + " (you entered " + text + ").");
        }
        return value;
    }

    /** Queue number: a whole number from 1 to the number of queues chosen in the Queue setup. */
    private int readQueue(int row) throws InvalidCellException {
        int queue = readWholeNumber(row, COL_PRIORITY, 1);
        int queueCount = (Integer) queueCountSpinner.getValue();
        if (queue > queueCount) {
            throw new InvalidCellException(row, COL_PRIORITY,
                    "Queue must be 1 to " + queueCount + " (you entered " + queue + ").");
        }
        return queue;
    }

    private String cellText(int row, int column) {
        Object value = inputModel.getValueAt(row, column);
        return value == null ? "" : value.toString().trim();
    }

    /** An invalid value in the queue setup (a quantum that is not a number). */
    private static final class InvalidQueueSetupException extends Exception {
        final int queueIndex;

        InvalidQueueSetupException(int queueIndex, String problem) {
            super("Error in Queue setup, Queue " + (queueIndex + 1) + ":\n" + problem);
            this.queueIndex = queueIndex;
        }
    }

    /** An invalid input cell; the message names its row and column. */
    private static final class InvalidCellException extends Exception {
        final int row;
        final int column;

        InvalidCellException(int row, int column, String problem) {
            super("Error in row " + (row + 1) + ", column \"" + INPUT_COLUMNS[column] + "\":\n" + problem);
            this.row = row;
            this.column = column;
        }
    }
}
