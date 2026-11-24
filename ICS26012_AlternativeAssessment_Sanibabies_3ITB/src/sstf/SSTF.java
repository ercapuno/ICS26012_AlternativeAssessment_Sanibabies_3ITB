/* Algorithm: Shortest Seek Time First (SSTF)  Owner: Wynn Benedict De Villa */
package sstf;

import java.util.ArrayList;
import java.util.List;

import model.DiskResult;
import scheduler.DiskScheduler;

/**
 * Shortest Seek Time First disk scheduling.
 *
 * Rules:
 * - Cylinders run from 0 to diskSize - 1. Head movement is the sum of |next - current|,
 *   starting from the initial head position.
 * - At each step, service the pending request closest to the current head position.
 * - Ties (two requests equally far, one on each side) go to the LOWER cylinder number.
 * - Duplicate requests are served one after the other with zero movement
 *   (once the head is on a cylinder, the copy left in the queue is at distance 0).
 * - The initial direction is not used: SSTF only looks at distance.
 */
public class SSTF implements DiskScheduler {

    @Override
    public DiskResult schedule(int[] requests, int head, int diskSize, String direction) {
        if (requests == null || requests.length == 0) {
            throw new IllegalArgumentException("At least one request is required.");
        }
        if (diskSize < 1) {
            throw new IllegalArgumentException("Disk size must be at least 1.");
        }
        checkCylinder("Initial head position", head, diskSize);
        for (int request : requests) {
            checkCylinder("Request " + request, request, diskSize);
        }

        // Work on a copy so the caller's array is never changed; 'pending' shrinks as requests are served.
        List<Integer> pending = new ArrayList<Integer>();
        for (int request : requests) {
            pending.add(request);
        }

        int[] sequence = new int[requests.length + 1];
        sequence[0] = head;
        int current = head;
        int total = 0;

        for (int step = 1; step <= requests.length; step++) {
            int index = closestIndex(pending, current);
            int next = pending.remove(index);
            total += Math.abs(next - current);
            current = next;
            sequence[step] = next;
        }

        return new DiskResult(sequence, total);
    }

    /**
     * Index of the pending request with the shortest seek from 'head'.
     * On equal distance the lower cylinder wins (only possible when one is below and one is above the head).
     */
    private static int closestIndex(List<Integer> pending, int head) {
        int bestIndex = 0;
        for (int i = 1; i < pending.size(); i++) {
            int candidate = pending.get(i);
            int best = pending.get(bestIndex);
            int candidateDistance = Math.abs(candidate - head);
            int bestDistance = Math.abs(best - head);
            if (candidateDistance < bestDistance
                    || (candidateDistance == bestDistance && candidate < best)) {
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static void checkCylinder(String name, int cylinder, int diskSize) {
        if (cylinder < 0 || cylinder >= diskSize) {
            throw new IllegalArgumentException(
                    name + " is outside the disk (valid cylinders are 0 to " + (diskSize - 1) + ").");
        }
    }
}
/**DONE */