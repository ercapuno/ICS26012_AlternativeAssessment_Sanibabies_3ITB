import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import multilevelqueue.MLQSampleData;
import multilevelqueue.MultilevelQueue;
import multilevelqueue.MultilevelQueue.Algorithm;
import multilevelqueue.MultilevelQueue.QueueConfig;
import model.GanttBlock;
import model.ScheduleResult;

/** Checks the Multilevel Queue algorithm against hand-worked answers (no GUI, no JUnit). */
public class MultilevelQueueTest {

    private static int failures = 0;

    public static void main(String[] args) {
        ScheduleResult r = new MultilevelQueue().schedule(MLQSampleData.preemptionAndIdle());
        check("sample 1 Gantt", gantt(r),
                "P1[0-1] P2[1-5] P3[5-7] P1[7-9] P4[9-10] IDLE[10-13] P5[13-14] P7[14-16] P5[16-18] "
                + "P6[18-20] P10[20-23] P9[23-25] P8[25-26]");
        check("sample 1 avg TAT", f(r.getAverageTurnaround()), "6.20");
        check("sample 1 avg WT", f(r.getAverageWaiting()), "3.90");
        check("sample 1 avg RT", f(r.getAverageResponse()), "3.10");
        check("sample 1 CPU util", f(r.getCpuUtilization()), "88.46");

        r = new MultilevelQueue().schedule(MLQSampleData.allArriveAtZero());
        check("sample 2 Gantt", gantt(r),
                "P2[0-1] P1[1-9] P5[9-14] P1[14-16] P3[16-18] P4[18-19]");
        check("sample 2 avg TAT", f(r.getAverageTurnaround()), "13.60");
        check("sample 2 avg WT", f(r.getAverageWaiting()), "9.80");
        check("sample 2 avg RT", f(r.getAverageResponse()), "8.80");

        // Both Q2 processes arrive while P1 runs; P3 arrived first (at 1) even though its row is
        // after P2 (arrives at 3), so P3 must be first in Queue 2.
        List<model.Process> order = new java.util.ArrayList<model.Process>();
        order.add(new model.Process("P1", 0, 5, 1));
        order.add(new model.Process("P2", 3, 1, 2));
        order.add(new model.Process("P3", 1, 1, 2));
        r = new MultilevelQueue().schedule(order);
        check("arrival order within a queue", gantt(r), "P1[0-5] P3[5-6] P2[6-7]");

        List<model.Process> in = MLQSampleData.allArriveAtZero();
        new MultilevelQueue().schedule(in);
        check("input untouched", String.valueOf(in.get(0).getCompletionTime()), "-1");

        // ---- Configurable queues: every queue can use RR, FCFS, SJF or SRTF ----

        // SJF (non-preemptive): the running job is never interrupted by a shorter job of its own queue.
        r = run(queues(cfg(Algorithm.SJF, 0), cfg(Algorithm.FCFS, 0)),
                proc("P1", 0, 7, 1), proc("P2", 1, 3, 1), proc("P3", 2, 1, 1), proc("P4", 3, 2, 1));
        check("SJF queue", gantt(r), "P1[0-7] P3[7-8] P4[8-10] P2[10-13]");

        // SRTF (preemptive): a newcomer with a strictly shorter remaining time takes the CPU.
        r = run(queues(cfg(Algorithm.SRTF, 0), cfg(Algorithm.FCFS, 0)),
                proc("P1", 0, 7, 1), proc("P2", 1, 3, 1), proc("P3", 2, 1, 1), proc("P4", 3, 2, 1));
        check("SRTF queue", gantt(r), "P1[0-1] P2[1-2] P3[2-3] P2[3-5] P4[5-7] P1[7-13]");
        check("SRTF avg WT", f(r.getAverageWaiting()), "2.25");

        // SRTF: an equal remaining time does not preempt.
        r = run(queues(cfg(Algorithm.SRTF, 0), cfg(Algorithm.FCFS, 0)),
                proc("P1", 0, 4, 1), proc("P2", 2, 2, 1));
        check("SRTF tie does not preempt", gantt(r), "P1[0-4] P2[4-6]");

        // Custom Round Robin quantum.
        r = run(queues(cfg(Algorithm.ROUND_ROBIN, 2), cfg(Algorithm.FCFS, 0)),
                proc("P1", 0, 5, 1), proc("P2", 0, 3, 1));
        check("RR quantum 2", gantt(r), "P1[0-2] P2[2-4] P1[4-6] P2[6-7] P1[7-8]");

        // A different algorithm in each queue, with preemption between queues:
        // Q1 = SRTF, Q2 = SJF, Q3 = RR q=3. P4 (Q1) cuts P2 (Q2) off at 3; P2 then resumes before P1 (Q3).
        r = run(queues(cfg(Algorithm.SRTF, 0), cfg(Algorithm.SJF, 0), cfg(Algorithm.ROUND_ROBIN, 3)),
                proc("P1", 0, 6, 3), proc("P2", 0, 4, 2), proc("P3", 0, 2, 2), proc("P4", 3, 2, 1));
        check("mixed queues", gantt(r), "P3[0-2] P2[2-3] P4[3-5] P2[5-8] P1[8-14]");
        check("mixed queues P1 response", String.valueOf(r.getProcesses().get(0).getResponseTime()), "8");

        // SJF: a job already cut off by a higher queue resumes before a shorter job of its own queue.
        r = run(queues(cfg(Algorithm.FCFS, 0), cfg(Algorithm.SJF, 0)),
                proc("P1", 0, 5, 2), proc("P2", 1, 1, 2), proc("P3", 2, 1, 1));
        check("SJF resumes interrupted job", gantt(r), "P1[0-2] P3[2-3] P1[3-6] P2[6-7]");

        // Five queues, one process in the last one.
        r = run(queues(cfg(Algorithm.ROUND_ROBIN, 1), cfg(Algorithm.FCFS, 0), cfg(Algorithm.SJF, 0),
                cfg(Algorithm.SRTF, 0), cfg(Algorithm.ROUND_ROBIN, 5)),
                proc("P1", 0, 2, 5), proc("P2", 0, 1, 1));
        check("five queues", gantt(r), "P2[0-1] P1[1-3]");

        // Invalid set-ups are rejected.
        expectError("6 queues", () -> new MultilevelQueue(queues(
                cfg(Algorithm.FCFS, 0), cfg(Algorithm.FCFS, 0), cfg(Algorithm.FCFS, 0),
                cfg(Algorithm.FCFS, 0), cfg(Algorithm.FCFS, 0), cfg(Algorithm.FCFS, 0))));
        expectError("RR quantum 0", () -> new QueueConfig(Algorithm.ROUND_ROBIN, 0));
        expectError("queue number beyond the chosen count", () ->
                run(queues(cfg(Algorithm.FCFS, 0), cfg(Algorithm.FCFS, 0)), proc("P1", 0, 1, 3)));

        System.out.println(failures == 0 ? "\nAll checks passed." : "\n" + failures + " check(s) FAILED.");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static QueueConfig cfg(Algorithm algorithm, int quantum) {
        return new QueueConfig(algorithm, quantum);
    }

    private static List<QueueConfig> queues(QueueConfig... configs) {
        List<QueueConfig> list = new ArrayList<QueueConfig>();
        for (QueueConfig c : configs) {
            list.add(c);
        }
        return list;
    }

    private static model.Process proc(String pid, int arrival, int burst, int queue) {
        return new model.Process(pid, arrival, burst, queue);
    }

    private static ScheduleResult run(List<QueueConfig> queues, model.Process... processes) {
        List<model.Process> list = new ArrayList<model.Process>();
        for (model.Process p : processes) {
            list.add(p);
        }
        return new MultilevelQueue(queues).schedule(list);
    }

    private static void expectError(String name, Runnable action) {
        boolean thrown = false;
        try {
            action.run();
        } catch (IllegalArgumentException ex) {
            thrown = true;
        }
        check(name + " rejected", String.valueOf(thrown), "true");
    }

    private static String gantt(ScheduleResult r) {
        StringBuilder sb = new StringBuilder();
        for (GanttBlock b : r.getGanttBlocks()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(b.getLabel()).append('[').append(b.getStart()).append('-').append(b.getEnd()).append(']');
        }
        return sb.toString();
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static void check(String name, String actual, String expected) {
        boolean ok = expected.equals(actual);
        if (!ok) failures++;
        System.out.println((ok ? "PASS  " : "FAIL  ") + name + (ok ? "" : "\n   expected: " + expected + "\n   actual:   " + actual));
    }
}
