package multilevelqueue;

import java.util.ArrayList;
import java.util.List;

import model.Process;

/**
 * Sample problems for the Multilevel Queue module. They use the same processes (PID, arrival, burst)
 * as the Priority (Non-Preemptive) samples, so the two modules can be compared side by side.
 *
 * The Priority field is the queue number (1 = Round Robin q=4, 2 = Round Robin q=8, 3 = FCFS).
 * The Priority samples use priorities 0 to 5, so they are mapped onto the three queues:
 * priority 0-1 -> Queue 1, priority 2-3 -> Queue 2, priority 4-5 -> Queue 3.
 */
public final class MLQSampleData {

    private MLQSampleData() {
    }

    /**
     * Same 10 processes as PrioritySampleData.withIdleGapAndTies(). It shows:
     * - preemption by a higher queue: P2 (Q1) interrupts P1 at 1, and P7 (Q1) interrupts P5 at 14;
     * - Round Robin in Queue 1 (quantum 4): P2 is followed by P3 at 5;
     * - a preempted process goes back to the front of its queue (P1 resumes at 7, P5 at 16);
     * - an idle gap: nothing is ready from 10 to 13;
     * - Queue 3 (FCFS) runs last: P8 only gets the CPU at 25.
     *
     * Expected Gantt chart:
     * P1 0-1 | P2 1-5 | P3 5-7 | P1 7-9 | P4 9-10 | IDLE 10-13 | P5 13-14 | P7 14-16 | P5 16-18 |
     * P6 18-20 | P10 20-23 | P9 23-25 | P8 25-26
     * Average turnaround = 62 / 10 = 6.20; average waiting = 39 / 10 = 3.90; average response = 31 / 10 = 3.10
     *
     * @return a new list each call, so callers can change it freely
     */
    public static List<Process> preemptionAndIdle() {
        List<Process> processes = new ArrayList<Process>();
        //                       PID  Arrival Burst Queue
        processes.add(new Process("P1", 0, 3, 2));
        processes.add(new Process("P2", 1, 4, 1));
        processes.add(new Process("P3", 2, 2, 1));
        processes.add(new Process("P4", 3, 1, 2));
        processes.add(new Process("P5", 13, 3, 2));
        processes.add(new Process("P6", 13, 2, 2));
        processes.add(new Process("P7", 14, 2, 1));
        processes.add(new Process("P8", 15, 1, 3));
        processes.add(new Process("P9", 20, 2, 2));
        processes.add(new Process("P10", 16, 3, 2));
        return processes;
    }

    /**
     * Same 5 processes as PrioritySampleData.allArriveAtZero() (all arrive at 0, burst in ms).
     *
     * Expected Gantt chart: P2 0-1 | P1 1-9 | P5 9-14 | P1 14-16 | P3 16-18 | P4 18-19
     * Average turnaround = 68 / 5 = 13.60; average waiting = 49 / 5 = 9.80; average response = 44 / 5 = 8.80
     *
     * @return a new list each call, so callers can change it freely
     */
    public static List<Process> allArriveAtZero() {
        List<Process> processes = new ArrayList<Process>();
        //                       PID  Arrival Burst Queue
        processes.add(new Process("P1", 0, 10, 2));
        processes.add(new Process("P2", 0, 1, 1));
        processes.add(new Process("P3", 0, 2, 3));
        processes.add(new Process("P4", 0, 1, 3));
        processes.add(new Process("P5", 0, 5, 2));
        return processes;
    }
}
