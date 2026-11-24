package model;

/**
 * The output of any disk scheduler: the order the head visits cylinders and the total distance moved.
 * The per-step movement and the average seek distance are computed here once, so every disk
 * scheduling module (SSTF, C-LOOK, ...) reports them the same way.
 */
public class DiskResult {

    private final int[] seekSequence;
    private final int totalHeadMovement;

    /**
     * @param seekSequence      cylinders in visiting order, starting with the initial head position
     *                          and followed by one entry per serviced request
     * @param totalHeadMovement sum of |next - current| over the whole sequence
     */
    public DiskResult(int[] seekSequence, int totalHeadMovement) {
        this.seekSequence = seekSequence.clone();
        this.totalHeadMovement = totalHeadMovement;
    }

    public int[] getSeekSequence() {
        return seekSequence.clone();
    }

    public int getTotalHeadMovement() {
        return totalHeadMovement;
    }

    /** Number of requests serviced (the sequence minus the initial head position). */
    public int getRequestCount() {
        return Math.max(0, seekSequence.length - 1);
    }

    /** Head movement of each step: element i is |seekSequence[i + 1] - seekSequence[i]|. */
    public int[] getStepMovements() {
        int[] steps = new int[getRequestCount()];
        for (int i = 0; i < steps.length; i++) {
            steps[i] = Math.abs(seekSequence[i + 1] - seekSequence[i]);
        }
        return steps;
    }

    /**
     * Seek time = total head movement x seek rate.
     *
     * @param seekRate time the head needs to cross one cylinder (e.g. 4 ms per cylinder)
     */
    public double getSeekTime(double seekRate) {
        return totalHeadMovement * seekRate;
    }

    /** Average seek distance = total head movement / number of requests serviced. */
    public double getAverageSeekDistance() {
        int count = getRequestCount();
        return count == 0 ? 0 : (double) totalHeadMovement / count;
    }
}
