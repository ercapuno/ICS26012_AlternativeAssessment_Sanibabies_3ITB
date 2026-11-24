
package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The output of any CPU scheduler: the Gantt chart, the processes with their
 * recorded times, and the summary metrics. Metrics are computed here once, so
 * every CPU scheduling module reports them the same way.
 */
public class ScheduleResult {

    private final List<GanttBlock> ganttBlocks;
    private final List<Process> processes;

    private final int totalTime;
    private final int busyTime;
    private final double averageTurnaround;
    private final double averageWaiting;
    private final double averageResponse;

    /**
     * @param ganttBlocks blocks in time order, starting at time 0
     * @param processes   completed processes (any order; the GUI shows them in this order)
     */
    public ScheduleResult(List<GanttBlock> ganttBlocks, List<Process> processes) {
        this.ganttBlocks = Collections.unmodifiableList(new ArrayList<GanttBlock>(ganttBlocks));
        this.processes = Collections.unmodifiableList(new ArrayList<Process>(processes));

        // Total time = last completion time = end of the last Gantt block.
        totalTime = ganttBlocks.isEmpty() ? 0 : ganttBlocks.get(ganttBlocks.size() - 1).getEnd();

        int busy = 0;
        for (GanttBlock block : ganttBlocks) {
            if (!block.isIdle()) {
                busy += block.getDuration();
            }
        }
        busyTime = busy;

        int sumTurnaround = 0;
        int sumWaiting = 0;
        int sumResponse = 0;
        for (Process p : processes) {
            sumTurnaround += p.getTurnaroundTime();
            sumWaiting += p.getWaitingTime();
            sumResponse += p.getResponseTime();
        }
        int count = processes.size();
        averageTurnaround = count == 0 ? 0 : (double) sumTurnaround / count;
        averageWaiting = count == 0 ? 0 : (double) sumWaiting / count;
        averageResponse = count == 0 ? 0 : (double) sumResponse / count;
    }

    public List<GanttBlock> getGanttBlocks() {
        return ganttBlocks;
    }

    public List<Process> getProcesses() {
        return processes;
    }

    /** Last completion time. */
    public int getTotalTime() {
        return totalTime;
    }

    /** Time units the CPU spent running processes (total time minus idle time). */
    public int getBusyTime() {
        return busyTime;
    }

    public double getAverageTurnaround() {
        return averageTurnaround;
    }

    public double getAverageWaiting() {
        return averageWaiting;
    }

    public double getAverageResponse() {
        return averageResponse;
    }

    /** CPU Utilization = (busy time / total time) x 100 */
    public double getCpuUtilization() {
        return totalTime == 0 ? 0 : 100.0 * busyTime / totalTime;
    }

    /** Throughput = number of processes / total time */
    public double getThroughput() {
        return totalTime == 0 ? 0 : (double) processes.size() / totalTime;
    }
}
