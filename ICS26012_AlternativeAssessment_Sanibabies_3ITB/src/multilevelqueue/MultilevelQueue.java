/* Algorithm: Multilevel Queue */
package multilevelqueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.GanttBlock;
import model.Process;
import model.ScheduleResult;
import scheduler.CpuScheduler;

/**
 * Multilevel Queue scheduling (preemptive between queues), with a configurable
 * scheduling algorithm for every queue.
 *
 * Rules:
 * - There are 1 to {@link #MAX_QUEUES} queues. A process's queue is the value in its Priority field
 *   (1 = highest queue).
 * - Every queue has its own algorithm, chosen with a {@link QueueConfig}:
 *     ROUND_ROBIN - needs a time quantum; a process whose quantum expires goes to the BACK of its queue.
 *     FCFS        - first come, first served; runs until it finishes.
 *     SJF         - shortest job first (non-preemptive): the shortest burst runs next and is never
 *                   interrupted by a process of the same queue.
 *     SRTF        - shortest remaining time first (preemptive): a process arriving in the same queue
 *                   with a strictly shorter remaining time takes the CPU at once.
 *   Ties in SJF and SRTF go to the earlier arrival, then to the earlier input row.
 * - Fixed priority between queues: a lower queue runs only when all higher queues are empty,
 *   and a process arriving in a higher queue preempts the running process immediately.
 * - A Round Robin / FCFS process preempted by a higher queue goes back to the FRONT of its own queue.
 *   An SJF process preempted by a higher queue resumes first when its queue gets the CPU again
 *   (so it is not interrupted by a shorter job of its own queue). An SRTF process is simply
 *   compared again by remaining time.
 * - Processes enter their queue in arrival-time order; equal arrival times keep input order.
 * - Processes arriving at the same moment a slice ends enter the queue before the
 *   process that just ran is put back.
 * - Time starts at 0 in whole units; context switch takes 0 time; idle gaps appear as IDLE blocks.
 *
 * The no-argument constructor uses the default set-up: Queue 1 = RR q=4, Queue 2 = RR q=8, Queue 3 = FCFS.
 */
public class MultilevelQueue implements CpuScheduler {

    /** Largest number of queues. */
    public static final int MAX_QUEUES = 5;

    /** Scheduling algorithms a queue can use. */
    public enum Algorithm {
        ROUND_ROBIN("Round Robin", "RR", true),
        FCFS("FCFS", "FCFS", false),
        SJF("SJF", "SJF", false),
        SRTF("SRTF", "SRTF", false);

        private final String displayName;
        private final String shortName;
        private final boolean needsQuantum;

        Algorithm(String displayName, String shortName, boolean needsQuantum) {
            this.displayName = displayName;
            this.shortName = shortName;
            this.needsQuantum = needsQuantum;
        }

        public String getShortName() {
            return shortName;
        }

        /** True when the algorithm uses a time quantum (only Round Robin). */
        public boolean needsQuantum() {
            return needsQuantum;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** The algorithm (and quantum, for Round Robin) of one queue. */
    public static final class QueueConfig {
        private final Algorithm algorithm;
        private final int quantum;

        /**
         * @param algorithm the queue's algorithm
         * @param quantum   time quantum, at least 1 for Round Robin; ignored (stored as 0) for the others
         */
        public QueueConfig(Algorithm algorithm, int quantum) {
            if (algorithm == null) {
                throw new IllegalArgumentException("An algorithm is required for every queue.");
            }
            if (algorithm.needsQuantum() && quantum < 1) {
                throw new IllegalArgumentException("Round Robin needs a time quantum of at least 1.");
            }
            this.algorithm = algorithm;
            this.quantum = algorithm.needsQuantum() ? quantum : 0;
        }

        public Algorithm getAlgorithm() {
            return algorithm;
        }

        /** Time quantum; 0 when the algorithm does not use one. */
        public int getQuantum() {
            return quantum;
        }

        /** Short text such as "RR q=4" or "SRTF". */
        public String describe() {
            return algorithm.needsQuantum() ? algorithm.getShortName() + " q=" + quantum : algorithm.getShortName();
        }
    }

    /** The default set-up: Queue 1 = RR q=4, Queue 2 = RR q=8, Queue 3 = FCFS. */
    public static List<QueueConfig> defaultQueues() {
        List<QueueConfig> queues = new ArrayList<QueueConfig>();
        queues.add(new QueueConfig(Algorithm.ROUND_ROBIN, 4));
        queues.add(new QueueConfig(Algorithm.ROUND_ROBIN, 8));
        queues.add(new QueueConfig(Algorithm.FCFS, 0));
        return queues;
    }

    private final List<QueueConfig> queueConfigs;

    /** Uses the default set-up (see {@link #defaultQueues()}). */
    public MultilevelQueue() {
        this(defaultQueues());
    }

    /** @param queueConfigs one entry per queue, highest queue first (1 to {@link #MAX_QUEUES} entries) */
    public MultilevelQueue(List<QueueConfig> queueConfigs) {
        if (queueConfigs == null || queueConfigs.isEmpty() || queueConfigs.size() > MAX_QUEUES) {
            throw new IllegalArgumentException("The number of queues must be 1 to " + MAX_QUEUES + ".");
        }
        for (QueueConfig config : queueConfigs) {
            if (config == null) {
                throw new IllegalArgumentException("Every queue needs an algorithm.");
            }
        }
        this.queueConfigs = Collections.unmodifiableList(new ArrayList<QueueConfig>(queueConfigs));
    }

    public int getQueueCount() {
        return queueConfigs.size();
    }

    public List<QueueConfig> getQueueConfigs() {
        return queueConfigs;
    }

    @Override
    public ScheduleResult schedule(List<Process> processes) {
        if (processes == null || processes.isEmpty()) {
            throw new IllegalArgumentException("At least one process is required.");
        }
        final int queueCount = queueConfigs.size();

        // Work on copies so the caller's objects are never changed.
        List<Process> results = new ArrayList<Process>();
        for (Process p : processes) {
            results.add(p.copyInput());
            int q = p.getPriority();
            if (q < 1 || q > queueCount) {
                throw new IllegalArgumentException(
                        "Queue number for " + p.getPid() + " must be 1 to " + queueCount + ".");
            }
        }

        final Map<Process, Integer> remaining = new HashMap<Process, Integer>();
        final Map<Process, Integer> inputRow = new HashMap<Process, Integer>();
        for (int i = 0; i < results.size(); i++) {
            Process p = results.get(i);
            remaining.put(p, p.getBurstTime());
            inputRow.put(p, i);
        }

        // Not yet arrived, sorted by arrival time (stable sort, so arrival ties keep input order).
        // Several processes can arrive while one is running; sorting makes them enter their
        // queue in the order they actually arrived, not in table-row order.
        List<Process> notArrived = new ArrayList<Process>(results);
        Collections.sort(notArrived, new Comparator<Process>() {
            @Override
            public int compare(Process a, Process b) {
                return Integer.compare(a.getArrivalTime(), b.getArrivalTime());
            }
        });
        List<List<Process>> queues = new ArrayList<List<Process>>();
        for (int i = 0; i < queueCount; i++) {
            queues.add(new ArrayList<Process>());
        }
        List<GanttBlock> gantt = new ArrayList<GanttBlock>();

        int time = 0;
        int finished = 0;
        while (finished < results.size()) {
            admitArrivals(notArrived, queues, time);

            int level = firstNonEmpty(queues);
            if (level < 0) {
                // Nothing ready: idle until the earliest arrival.
                int nextArrival = Integer.MAX_VALUE;
                for (Process p : notArrived) {
                    nextArrival = Math.min(nextArrival, p.getArrivalTime());
                }
                addBlock(gantt, GanttBlock.idle(time, nextArrival));
                time = nextArrival;
                continue;
            }

            QueueConfig config = queueConfigs.get(level);
            Algorithm algorithm = config.getAlgorithm();
            List<Process> queue = queues.get(level);

            Process current = pickNext(queue, algorithm, remaining, inputRow);
            queue.remove(current);
            int left = remaining.get(current);
            int slice = algorithm == Algorithm.ROUND_ROBIN ? Math.min(config.getQuantum(), left) : left;
            int end = time + slice;
            int full = end;

            // The earliest arrival that interrupts this slice, if any.
            for (Process p : notArrived) {
                int arrival = p.getArrivalTime();
                if (arrival >= end) {
                    continue;
                }
                int pLevel = p.getPriority() - 1;
                boolean interrupts;
                if (pLevel < level) {
                    interrupts = true;              // a higher queue always preempts
                } else if (pLevel == level && algorithm == Algorithm.SRTF) {
                    // Same queue, SRTF: preempt only when the newcomer is strictly shorter than
                    // what the running process still has left at that moment.
                    interrupts = remaining.get(p) < left - (arrival - time);
                } else {
                    interrupts = false;
                }
                if (interrupts) {
                    end = arrival;
                }
            }
            boolean preempted = end < full;

            current.markStarted(time);
            addBlock(gantt, GanttBlock.running(current.getPid(), time, end));
            left -= end - time;
            remaining.put(current, left);
            time = end;

            admitArrivals(notArrived, queues, time);

            if (left == 0) {
                current.markCompleted(time);
                finished++;
            } else if (preempted && algorithm != Algorithm.SJF && algorithm != Algorithm.SRTF) {
                queue.add(0, current);                 // RR / FCFS: keeps its place in line
            } else {
                // Quantum used up (RR: back of the queue). SJF / SRTF choose by length, not by position.
                queue.add(current);
            }
        }

        return new ScheduleResult(gantt, results);
    }

    /** Chooses which process of this queue runs next, according to the queue's algorithm. */
    private static Process pickNext(List<Process> queue, Algorithm algorithm,
            Map<Process, Integer> remaining, Map<Process, Integer> inputRow) {
        if (algorithm == Algorithm.ROUND_ROBIN || algorithm == Algorithm.FCFS) {
            return queue.get(0);
        }
        Process best = null;
        for (Process candidate : queue) {
            if (best == null || isBetter(candidate, best, algorithm, remaining, inputRow)) {
                best = candidate;
            }
        }
        return best;
    }

    /** True when 'a' should run before 'b' under SJF or SRTF. */
    private static boolean isBetter(Process a, Process b, Algorithm algorithm,
            Map<Process, Integer> remaining, Map<Process, Integer> inputRow) {
        if (algorithm == Algorithm.SJF) {
            // A job that was cut off by a higher queue has already started: it resumes first.
            boolean aStarted = remaining.get(a) < a.getBurstTime();
            boolean bStarted = remaining.get(b) < b.getBurstTime();
            if (aStarted != bStarted) {
                return aStarted;
            }
        }
        int byLength = Integer.compare(
                algorithm == Algorithm.SJF ? a.getBurstTime() : remaining.get(a),
                algorithm == Algorithm.SJF ? b.getBurstTime() : remaining.get(b));
        if (byLength != 0) {
            return byLength < 0;
        }
        int byArrival = Integer.compare(a.getArrivalTime(), b.getArrivalTime());
        if (byArrival != 0) {
            return byArrival < 0;
        }
        return inputRow.get(a) < inputRow.get(b);
    }

    /** Moves every process with arrival <= time into its queue. */
    private static void admitArrivals(List<Process> notArrived, List<List<Process>> queues, int time) {
        List<Process> arrived = new ArrayList<Process>();
        for (Process p : notArrived) {
            if (p.getArrivalTime() <= time) {
                arrived.add(p);
            }
        }
        for (Process p : arrived) {
            queues.get(p.getPriority() - 1).add(p);
            notArrived.remove(p);
        }
    }

    private static int firstNonEmpty(List<List<Process>> queues) {
        for (int i = 0; i < queues.size(); i++) {
            if (!queues.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /** Adds a block, merging it into the previous one when the same process simply continues. */
    private static void addBlock(List<GanttBlock> gantt, GanttBlock block) {
        if (block.getDuration() <= 0) {
            return;
        }
        if (!gantt.isEmpty()) {
            GanttBlock last = gantt.get(gantt.size() - 1);
            if (last.getEnd() == block.getStart() && last.isIdle() == block.isIdle()
                    && last.getLabel().equals(block.getLabel())) {
                gantt.set(gantt.size() - 1, last.isIdle()
                        ? GanttBlock.idle(last.getStart(), block.getEnd())
                        : GanttBlock.running(last.getLabel(), last.getStart(), block.getEnd()));
                return;
            }
        }
        gantt.add(block);
    }
}

