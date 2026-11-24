package model;

/** One bar of a Gantt chart: a process (or the idle CPU) occupying [start, end). */
public class GanttBlock {

    public static final String IDLE_LABEL = "IDLE";

    private final String label;
    private final int start;
    private final int end;
    private final boolean idle;

    private GanttBlock(String label, int start, int end, boolean idle) {
        this.label = label;
        this.start = start;
        this.end = end;
        this.idle = idle;
    }

    /** A block where the given process runs on the CPU. */
    public static GanttBlock running(String pid, int start, int end) {
        return new GanttBlock(pid, start, end, false);
    }

    /** A block where the CPU has nothing to run. */
    public static GanttBlock idle(int start, int end) {
        return new GanttBlock(IDLE_LABEL, start, end, true);
    }

    public String getLabel() {
        return label;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getDuration() {
        return end - start;
    }

    public boolean isIdle() {
        return idle;
    }
}
