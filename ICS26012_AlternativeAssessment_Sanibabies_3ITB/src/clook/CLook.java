/* Algorithm: C-LOOK  Owner: Jayvi Al Andres */
package clook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.DiskResult;
import scheduler.DiskScheduler;

/**
 * C-LOOK (Circular LOOK) disk scheduling algorithm.
 * Moves in the initial direction servicing all requests up to the last request in that
 * direction, jumps directly to the furthest request on the opposite side, and continues
 * servicing in the same direction without going to the physical disk boundaries unless requested.
 *
 * Conventions:
 * - Cylinders run from 0 to diskSize - 1. Head movement is the sum of |next - current|,
 *   starting from the initial head position.
 * - The return jump is counted in the total head movement and appears in the seek sequence.
 * - Requests on the head's own cylinder are served first (zero movement).
 * - Duplicate requests are served one after the other (zero movement), because the lists are sorted.
 * - Direction "Up" (toward higher cylinders) or "Down" (toward lower cylinders); null means Up.
 */
public class CLook implements DiskScheduler {

    @Override
    public DiskResult schedule(int[] requests, int head, int diskSize, String direction) {
        if (requests == null || requests.length == 0) {
            throw new IllegalArgumentException("At least one request is required.");
        }
        if (diskSize < 1) {
            throw new IllegalArgumentException("Disk size must be at least 1.");
        }
        checkCylinder("Initial head position", head, diskSize);
        for (int req : requests) {
            checkCylinder("Request " + req, req, diskSize);
        }
        if (direction != null && !direction.equalsIgnoreCase("up") && !direction.equalsIgnoreCase("down")) {
            throw new IllegalArgumentException("Direction must be Up or Down.");
        }

        List<Integer> lower = new ArrayList<Integer>();
        List<Integer> higher = new ArrayList<Integer>();
        // Requests on the head's own cylinder are served first (zero movement) in either direction.
        List<Integer> atHead = new ArrayList<Integer>();

        for (int req : requests) {
            if (req < head) {
                lower.add(req);
            } else if (req > head) {
                higher.add(req);
            } else {
                atHead.add(req);
            }
        }

        Collections.sort(lower);
        Collections.sort(higher);

        boolean moveUp = direction == null || !direction.equalsIgnoreCase("down");

        List<Integer> order = new ArrayList<Integer>(requests.length + 1);
        order.add(head);
        order.addAll(atHead);

        if (moveUp) {
            // Service requests > head in ascending order, then jump to lowest request and go up
            order.addAll(higher);
            order.addAll(lower);
        } else {
            // Service requests < head in descending order, then jump to highest request and go down
            Collections.reverse(lower);
            Collections.reverse(higher);
            order.addAll(lower);
            order.addAll(higher);
        }

        int[] seekSequence = new int[order.size()];
        int totalMovement = 0;
        seekSequence[0] = order.get(0);

        for (int i = 1; i < order.size(); i++) {
            seekSequence[i] = order.get(i);
            totalMovement += Math.abs(seekSequence[i] - seekSequence[i - 1]);
        }

        return new DiskResult(seekSequence, totalMovement);
    }

    private static void checkCylinder(String name, int cylinder, int diskSize) {
        if (cylinder < 0 || cylinder >= diskSize) {
            throw new IllegalArgumentException(
                    name + " is outside the disk (valid cylinders are 0 to " + (diskSize - 1) + ").");
        }
    }
}
