/* Algorithm: Priority (Non-Preemptive)  Owner: Erwin G. Capuno */
package prioritynp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.GanttBlock;
import model.Process;
import model.ScheduleResult;
import scheduler.CpuScheduler;

/**
 * Priority scheduling, non-preemptive.
 *
 * Rules:
 * - Time starts at 0 in whole units; context switch takes 0 time.
 * - Once a process starts, it runs until it finishes.
 * - Lower priority number = higher priority. No aging.
 * - At each decision point, choose among the processes that have arrived the one with the
 *   lowest priority number. Ties: earlier arrival first, then lower PID.
 * - If nothing has arrived, the CPU is IDLE until the next arrival (recorded as an IDLE block).
 */
public class PriorityNonPreemptive implements CpuScheduler {

    /** Splits a PID such as "P12" into a prefix ("P") and a number (12) for natural ordering. */
    private static final Pattern PID_WITH_NUMBER = Pattern.compile("^(.*?)(\\d+)$");

    /** Order used to pick the next process: priority, then arrival time, then PID. */
    private static final Comparator<Process> SELECTION_ORDER = new Comparator<Process>() {
        @Override
        public int compare(Process a, Process b) {
            if (a.getPriority() != b.getPriority()) {
                return Integer.compare(a.getPriority(), b.getPriority());
            }
            if (a.getArrivalTime() != b.getArrivalTime()) {
                return Integer.compare(a.getArrivalTime(), b.getArrivalTime());
            }
            return comparePids(a.getPid(), b.getPid());
        }
    };

    @Override
    public ScheduleResult schedule(List<Process> processes) {
        if (processes == null || processes.isEmpty()) {
            throw new IllegalArgumentException("At least one process is required.");
        }

        // Work on copies so the caller's objects are never changed.
        // 'results' keeps the input order for display; 'pending' shrinks as processes finish.
        List<Process> results = new ArrayList<Process>();
        for (Process p : processes) {
            results.add(p.copyInput());
        }
        List<Process> pending = new ArrayList<Process>(results);
        List<GanttBlock> gantt = new ArrayList<GanttBlock>();

        int time = 0;
        while (!pending.isEmpty()) {
            Process next = pickHighestPriorityArrived(pending, time);

            if (next == null) {
                // Nothing has arrived yet: the CPU idles until the earliest pending arrival.
                int nextArrival = earliestArrival(pending);
                gantt.add(GanttBlock.idle(time, nextArrival));
                time = nextArrival;
                continue;
            }

            // Non-preemptive: the chosen process runs its whole burst without interruption.
            int start = time;
            time += next.getBurstTime();
            next.markStarted(start);
            next.markCompleted(time);
            gantt.add(GanttBlock.running(next.getPid(), start, time));
            pending.remove(next);
        }

        return new ScheduleResult(gantt, results);
    }

    /** Returns the best process that has arrived by 'time', or null if none has arrived. */
    private static Process pickHighestPriorityArrived(List<Process> pending, int time) {
        Process best = null;
        for (Process p : pending) {
            if (p.getArrivalTime() <= time && (best == null || SELECTION_ORDER.compare(p, best) < 0)) {
                best = p;
            }
        }
        return best;
    }

    private static int earliestArrival(List<Process> pending) {
        int earliest = Integer.MAX_VALUE;
        for (Process p : pending) {
            earliest = Math.min(earliest, p.getArrivalTime());
        }
        return earliest;
    }

    /**
     * "Lower PID" in natural order, so P2 comes before P10 (plain string order would put P10 first).
     * PIDs that do not share a prefix ending in a number fall back to case-insensitive text order.
     */
    private static int comparePids(String a, String b) {
        Matcher ma = PID_WITH_NUMBER.matcher(a);
        Matcher mb = PID_WITH_NUMBER.matcher(b);
        if (ma.matches() && mb.matches() && ma.group(1).equalsIgnoreCase(mb.group(1))) {
            // Compare the numbers as digit strings (works for any length): drop leading zeros,
            // then the shorter number is smaller; equal lengths compare digit by digit.
            String na = ma.group(2).replaceFirst("^0+(?=\\d)", "");
            String nb = mb.group(2).replaceFirst("^0+(?=\\d)", "");
            if (na.length() != nb.length()) {
                return Integer.compare(na.length(), nb.length());
            }
            int byDigits = na.compareTo(nb);
            if (byDigits != 0) {
                return byDigits;
            }
        }
        return a.compareToIgnoreCase(b);
    }
}
