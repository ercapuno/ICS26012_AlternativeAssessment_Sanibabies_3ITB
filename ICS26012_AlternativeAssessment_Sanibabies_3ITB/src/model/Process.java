package model;

/**
 * One process for CPU scheduling: its input values (PID, arrival, burst, priority)
 * and the times a scheduler records for it (first CPU time, completion).
 *
 * Note: this class shares its simple name with java.lang.Process, so other packages
 * must import it explicitly ("import model.Process;"), not through "import model.*;".
 */
public class Process {

    /** Marker for "not scheduled yet". */
    private static final int NOT_SET = -1;

    private final String pid;
    private final int arrivalTime;
    private final int burstTime;
    private final int priority;

    private int firstStartTime = NOT_SET;
    private int completionTime = NOT_SET;

    public Process(String pid, int arrivalTime, int burstTime, int priority) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.priority = priority;
    }

    /** Returns a fresh copy with the same input values and no scheduling results. */
    public Process copyInput() {
        return new Process(pid, arrivalTime, burstTime, priority);
    }

    public String getPid() {
        return pid;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public int getPriority() {
        return priority;
    }

    /** Records when the process first gets the CPU. Later calls are ignored, so it stays the FIRST time. */
    public void markStarted(int time) {
        if (firstStartTime == NOT_SET) {
            firstStartTime = time;
        }
    }

    public void markCompleted(int time) {
        completionTime = time;
    }

    public int getFirstStartTime() {
        return firstStartTime;
    }

    public int getCompletionTime() {
        return completionTime;
    }

    /** Turnaround = Completion - Arrival */
    public int getTurnaroundTime() {
        return completionTime - arrivalTime;
    }

    /** Waiting = Turnaround - Burst */
    public int getWaitingTime() {
        return getTurnaroundTime() - burstTime;
    }

    /** Response = First CPU time - Arrival */
    public int getResponseTime() {
        return firstStartTime - arrivalTime;
    }
}
