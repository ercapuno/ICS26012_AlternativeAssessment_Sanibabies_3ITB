package scheduler;

import java.util.List;

import model.Process;
import model.ScheduleResult;

/** Common contract for every CPU scheduling algorithm in the simulator. */
public interface CpuScheduler {

    /**
     * Builds a schedule for the given processes. Implementations must not modify
     * the input list or its Process objects; they work on copies.
     */
    ScheduleResult schedule(List<Process> processes);
}
