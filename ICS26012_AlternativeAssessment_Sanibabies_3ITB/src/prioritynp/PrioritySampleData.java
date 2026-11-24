package prioritynp;

import java.util.ArrayList;
import java.util.List;

import model.Process;

/**
 * Sample problems for the Priority (Non-Preemptive) module, kept in one place so the
 * Sample window and the "Load Sample Data" button always use the same values.
 */
public final class PrioritySampleData {

    private PrioritySampleData() {
    }

    /**
     * Priority (Non-Preemptive) sample with 10 processes. It shows:
     * - non-preemption: P2 (priority 1) arrives at 1 but P1 keeps the CPU until 3,
     *   and P7 (priority 0) arrives at 14 but P5 keeps the CPU until 16;
     * - priority ties broken by arrival: P2 before P3 (both priority 1), P6 before P10 (both priority 2);
     * - an idle gap: nothing is ready from 10 to 13;
     * - a priority + arrival tie broken by PID: P5 and P6 both arrive at 13 with priority 2.
     *
     * Expected Gantt chart:
     * P1 0-3 | P2 3-7 | P3 7-9 | P4 9-10 | IDLE 10-13 | P5 13-16 | P7 16-18 | P6 18-20 | P10 20-23 | P9 23-25 | P8 25-26
     *
     * @return a new list each call, so callers can change it freely
     */
    public static List<Process> withIdleGapAndTies() {
        List<Process> processes = new ArrayList<Process>();
        //                       PID  Arrival Burst Priority
        processes.add(new Process("P1", 0, 3, 3));
        processes.add(new Process("P2", 1, 4, 1));
        processes.add(new Process("P3", 2, 2, 1));
        processes.add(new Process("P4", 3, 1, 2));
        processes.add(new Process("P5", 13, 3, 2));
        processes.add(new Process("P6", 13, 2, 2));
        processes.add(new Process("P7", 14, 2, 0));
        processes.add(new Process("P8", 15, 1, 4));
        processes.add(new Process("P9", 20, 2, 3));
        processes.add(new Process("P10", 16, 3, 2));
        return processes;
    }

    /**
     * Second Priority (Non-Preemptive) sample: the textbook problem where all five processes
     * arrive at time 0 (burst times in milliseconds). Shown only in the menu's Sample window.
     *
     * Expected Gantt chart: P2 0-1 | P5 1-6 | P1 6-16 | P3 16-18 | P4 18-19
     * Average waiting time = 41 / 5 = 8.20; average turnaround time = 60 / 5 = 12.00
     *
     * @return a new list each call, so callers can change it freely
     */
    public static List<Process> allArriveAtZero() {
        List<Process> processes = new ArrayList<Process>();
        //                       PID  Arrival Burst Priority
        processes.add(new Process("P1", 0, 10, 3));
        processes.add(new Process("P2", 0, 1, 1));
        processes.add(new Process("P3", 0, 2, 4));
        processes.add(new Process("P4", 0, 1, 5));
        processes.add(new Process("P5", 0, 5, 2));
        return processes;
    }
}
