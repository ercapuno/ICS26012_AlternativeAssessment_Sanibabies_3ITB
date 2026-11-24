package scheduler;

import model.DiskResult;

/** Common contract for every disk scheduling algorithm in the simulator (SSTF, C-LOOK, ...). */
public interface DiskScheduler {

    /**
     * @param requests  pending cylinder requests
     * @param head      starting head position
     * @param diskSize  number of cylinders (valid cylinders are 0 to diskSize - 1)
     * @param direction initial head direction, e.g. "up" or "down" (algorithms that ignore it may do so)
     */
    DiskResult schedule(int[] requests, int head, int diskSize, String direction);
}
