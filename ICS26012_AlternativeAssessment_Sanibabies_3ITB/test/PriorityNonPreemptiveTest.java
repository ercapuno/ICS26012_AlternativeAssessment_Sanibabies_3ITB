import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import prioritynp.PriorityNonPreemptive;
import prioritynp.PrioritySampleData;
import model.GanttBlock;
import model.Process;
import model.ScheduleResult;

/**
 * Checks the Priority (Non-Preemptive) algorithm without the GUI.
 * Plain Java (no JUnit): prints PASS/FAIL per check and exits with code 1 if any check fails.
 */
public class PriorityNonPreemptiveTest {

    private static int failures = 0;

    public static void main(String[] args) {
        sampleDataMatchesHandComputedAnswer();
        allArriveAtZeroSampleMatchesTextbook();
        idleAtTimeZeroIsShown();
        pidTieUsesNaturalOrder();
        inputListIsNotModified();

        System.out.println(failures == 0 ? "\nAll checks passed." : "\n" + failures + " check(s) FAILED.");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void sampleDataMatchesHandComputedAnswer() {
        ScheduleResult r = new PriorityNonPreemptive().schedule(PrioritySampleData.withIdleGapAndTies());

        check("sample Gantt order", ganttText(r),
                "P1[0-3] P2[3-7] P3[7-9] P4[9-10] IDLE[10-13] P5[13-16] P7[16-18] P6[18-20] P10[20-23] P9[23-25] P8[25-26]");
        //                                 PID CT TAT WT RT
        check("sample P1", row(r, 0), "P1 3 3 0 0");
        check("sample P2", row(r, 1), "P2 7 6 2 2");
        check("sample P3", row(r, 2), "P3 9 7 5 5");
        check("sample P4", row(r, 3), "P4 10 7 6 6");
        check("sample P5", row(r, 4), "P5 16 3 0 0");
        check("sample P6", row(r, 5), "P6 20 7 5 5");
        check("sample P7", row(r, 6), "P7 18 4 2 2");
        check("sample P8", row(r, 7), "P8 26 11 10 10");
        check("sample P9", row(r, 8), "P9 25 5 3 3");
        check("sample P10", row(r, 9), "P10 23 7 4 4");
        check("sample avg turnaround", fmt(r.getAverageTurnaround()), "6.00");
        check("sample avg waiting", fmt(r.getAverageWaiting()), "3.70");
        check("sample avg response", fmt(r.getAverageResponse()), "3.70");
        check("sample busy/total", r.getBusyTime() + "/" + r.getTotalTime(), "23/26");
        check("sample CPU utilization", fmt(r.getCpuUtilization()), "88.46");
        check("sample throughput", fmt(r.getThroughput()), "0.38");
    }

    private static void allArriveAtZeroSampleMatchesTextbook() {
        ScheduleResult r = new PriorityNonPreemptive().schedule(PrioritySampleData.allArriveAtZero());

        check("sample 2 Gantt order", ganttText(r), "P2[0-1] P5[1-6] P1[6-16] P3[16-18] P4[18-19]");
        //                                   PID CT TAT WT RT
        check("sample 2 P1", row(r, 0), "P1 16 16 6 6");
        check("sample 2 P2", row(r, 1), "P2 1 1 0 0");
        check("sample 2 P3", row(r, 2), "P3 18 18 16 16");
        check("sample 2 P4", row(r, 3), "P4 19 19 18 18");
        check("sample 2 P5", row(r, 4), "P5 6 6 1 1");
        check("sample 2 avg turnaround", fmt(r.getAverageTurnaround()), "12.00");
        check("sample 2 avg waiting", fmt(r.getAverageWaiting()), "8.20");
        check("sample 2 avg response", fmt(r.getAverageResponse()), "8.20");
        check("sample 2 CPU utilization", fmt(r.getCpuUtilization()), "100.00");
        check("sample 2 throughput", fmt(r.getThroughput()), "0.26");
    }

    private static void idleAtTimeZeroIsShown() {
        List<Process> in = new ArrayList<Process>();
        in.add(new Process("P1", 4, 2, 0));
        ScheduleResult r = new PriorityNonPreemptive().schedule(in);
        check("leading idle", ganttText(r), "IDLE[0-4] P1[4-6]");
        check("leading idle utilization", fmt(r.getCpuUtilization()), "33.33");
    }

    private static void pidTieUsesNaturalOrder() {
        List<Process> in = new ArrayList<Process>();
        in.add(new Process("P10", 0, 1, 1));
        in.add(new Process("P2", 0, 1, 1));
        check("P2 before P10", ganttText(new PriorityNonPreemptive().schedule(in)), "P2[0-1] P10[1-2]");
    }

    private static void inputListIsNotModified() {
        List<Process> in = PrioritySampleData.withIdleGapAndTies();
        new PriorityNonPreemptive().schedule(in);
        check("input untouched", String.valueOf(in.get(0).getCompletionTime()), "-1");
    }

    // ---- helpers

    private static String ganttText(ScheduleResult r) {
        StringBuilder sb = new StringBuilder();
        for (GanttBlock b : r.getGanttBlocks()) {
            sb.append(sb.length() == 0 ? "" : " ").append(b.getLabel())
              .append('[').append(b.getStart()).append('-').append(b.getEnd()).append(']');
        }
        return sb.toString();
    }

    private static String row(ScheduleResult r, int index) {
        Process p = r.getProcesses().get(index);
        return p.getPid() + " " + p.getCompletionTime() + " " + p.getTurnaroundTime() + " "
                + p.getWaitingTime() + " " + p.getResponseTime();
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static void check(String name, String actual, String expected) {
        boolean ok = actual.equals(expected);
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "PASS  " : "FAIL  ") + name + (ok ? "" : "\n      expected: " + expected + "\n      actual:   " + actual));
    }
}


