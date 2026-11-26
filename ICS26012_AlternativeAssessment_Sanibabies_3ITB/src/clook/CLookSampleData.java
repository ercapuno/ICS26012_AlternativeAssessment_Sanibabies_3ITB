package clook;

/**
 * Sample problems for the C-LOOK disk scheduling module, kept in one place so the
 * Sample window and the "Load Sample Data" button always use the same values.
 * They use the same requests, head, disk size and seek rate as the SSTF samples
 * (SstfSampleData), so the two disk algorithms can be compared side by side.
 */
public final class CLookSampleData {

    private CLookSampleData() {
    }

    public static final class DiskSample {
        private final int[] requests;
        private final int head;
        private final int diskSize;
        private final String direction;
        private final double seekRate;

        public DiskSample(int[] requests, int head, int diskSize, String direction, double seekRate) {
            this.requests = requests.clone();
            this.head = head;
            this.diskSize = diskSize;
            this.direction = direction;
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

        public String getDirection() {
            return direction;
        }
    }

    /**
     * Sample 1: the same 20 requests as SstfSampleData.tiesAndDuplicates() (head 100, disk 200,
     * seek rate 4), so SSTF and C-LOOK can be compared on the same data. Moving Down. It shows:
     * - a request on the head's own cylinder: 100 is served first with zero movement;
     * - a duplicate request: the two 45s are served one after the other (zero movement);
     * - the circular jump from the lowest request (0) to the highest (199), counted in the total.
     *
     * Seek sequence:
     * 100, 100, 90, 75, 60, 45, 45, 35, 25, 12, 5, 0, 199, 182, 170, 160, 150, 140, 130, 118, 110
     * Total head movement: 388 cylinders (100 down to 0, jump 199, then 89).
     * Average seek distance: 388 / 20 = 19.40. Seek time = 388 x 4 = 1552 ms.
     *
     * @return a new sample each call
     */
    public static DiskSample tiesAndDuplicates() {
        int[] requests = {
            110, 90, 45, 160, 12, 199, 45, 130, 75, 0,
            182, 150, 60, 25, 118, 100, 140, 5, 170, 35
        };
        return new DiskSample(requests, 100, 200, "Down", 4);
    }

    /**
     * Sample 2: the same textbook problem as SstfSampleData.textbook() (8 requests, head 53,
     * disk 200, seek rate 4). Moving Up.
     *
     * Seek sequence: 53, 65, 67, 98, 122, 124, 183, 14, 37
     * Total head movement: 322 cylinders (130 up to 183, jump 169, then 23).
     * Average seek distance: 322 / 8 = 40.25. Seek time = 322 x 4 = 1288 ms.
     *
     * @return a new sample each call
     */
    public static DiskSample textbook() {
        int[] requests = {98, 183, 37, 122, 14, 124, 65, 67};
        return new DiskSample(requests, 53, 200, "Up", 4);
    }
}

