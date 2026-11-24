package sstf;

/**
 * Sample problems for the SSTF module, kept in one place so the Sample window and the
 * "Load Sample Data" button always use the same values.
 * C-LOOK (CLookSampleData) uses the same requests, head, disk size and seek rate,
 * so the two disk algorithms can be compared side by side.
 */
public final class SstfSampleData {

    private SstfSampleData() {
    }

    /** One disk scheduling problem: the request queue, the initial head position, the disk size and the seek rate. */
    public static final class DiskProblem {
        private final int[] requests;
        private final int head;
        private final int diskSize;
        private final double seekRate;

        public DiskProblem(int[] requests, int head, int diskSize, double seekRate) {
            this.requests = requests.clone();
            this.head = head;
            this.diskSize = diskSize;
            this.seekRate = seekRate;
        }

        /** Time per cylinder moved (ms), used for the seek time. */
        public double getSeekRate() {
            return seekRate;
        }

        public int[] getRequests() {
            return requests.clone();
        }

        public int getHead() {
            return head;
        }

        public int getDiskSize() {
            return diskSize;
        }
    }

    /**
     * SSTF sample with 20 requests on a 200-cylinder disk, head at 100, seek rate 4 ms. It shows:
     * - a request on the head's own cylinder: 100 is served first with zero movement;
     * - a tie broken toward the lower cylinder: from 100, both 90 and 110 are 10 away, so 90 goes first;
     * - a duplicate request: the two 45s are served one after the other (zero movement);
     * - both disk boundaries: cylinders 0 and 199 are requested;
     * - the long "turn back" SSTF makes once one side is used up (0 -> 110).
     *
     * Expected seek sequence:
     * 100, 100, 90, 75, 60, 45, 45, 35, 25, 12, 5, 0, 110, 118, 130, 140, 150, 160, 170, 182, 199
     * Total head movement = 299 cylinders; average seek distance = 299 / 20 = 14.95
     * Seek rate 4 ms per cylinder, so seek time = 299 x 4 = 1196 ms
     *
     * @return a new problem each call
     */
    public static DiskProblem tiesAndDuplicates() {
        int[] requests = {
            110, 90, 45, 160, 12, 199, 45, 130, 75, 0,
            182, 150, 60, 25, 118, 100, 140, 5, 170, 35
        };
        return new DiskProblem(requests, 100, 200, 4);
    }

    /**
     * The classic textbook SSTF problem: 8 requests, head at 53, disk 0-199.
     *
     * Expected seek sequence: 53, 65, 67, 37, 14, 98, 122, 124, 183
     * Total head movement = 236 cylinders; average seek distance = 236 / 8 = 29.50
     * Seek rate 4 ms per cylinder, so seek time = 236 x 4 = 944 ms
     *
     * @return a new problem each call
     */
    public static DiskProblem textbook() {
        int[] requests = {98, 183, 37, 122, 14, 124, 65, 67};
        return new DiskProblem(requests, 53, 200, 4);
    }
}
