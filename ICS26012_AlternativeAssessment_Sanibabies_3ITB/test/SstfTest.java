import java.util.Arrays;
import java.util.Locale;

import model.DiskResult;
import sstf.SSTF;
import sstf.SstfSampleData;

/**
 * Checks the SSTF algorithm without the GUI.
 * Plain Java (no JUnit): prints PASS/FAIL per check and exits with code 1 if any check fails.
 */
public class SstfTest {

    private static int failures = 0;

    public static void main(String[] args) {
        textbookSample();
        tiesAndDuplicatesSample();
        tieGoesToLowerCylinder();
        headAtBoundaries();
        singleRequest();
        inputArrayIsNotModified();
        invalidInputIsRejected();

        System.out.println(failures == 0 ? "\nAll checks passed." : "\n" + failures + " check(s) FAILED.");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void textbookSample() {
        DiskResult r = run(SstfSampleData.textbook());
        check("textbook sequence", seq(r), "53 65 67 37 14 98 122 124 183");
        check("textbook total", String.valueOf(r.getTotalHeadMovement()), "236");
        check("textbook average", fmt(r.getAverageSeekDistance()), "29.50");
        check("textbook seek time", fmt(r.getSeekTime(4)), "944.00");
    }

    private static void tiesAndDuplicatesSample() {
        DiskResult r = run(SstfSampleData.tiesAndDuplicates());
        check("sample 1 sequence", seq(r),
                "100 100 90 75 60 45 45 35 25 12 5 0 110 118 130 140 150 160 170 182 199");
        check("sample 1 total", String.valueOf(r.getTotalHeadMovement()), "299");
        check("sample 1 average", fmt(r.getAverageSeekDistance()), "14.95");
        check("sample 1 seek time", fmt(r.getSeekTime(4)), "1196.00");
        check("sample 1 step movements", Arrays.toString(r.getStepMovements()),
                "[0, 10, 15, 15, 15, 0, 10, 10, 13, 7, 5, 110, 8, 12, 10, 10, 10, 10, 12, 17]");
    }

    private static void tieGoesToLowerCylinder() {
        DiskResult r = new SSTF().schedule(new int[] {60, 40}, 50, 200, null);
        check("tie: 40 and 60 both 10 away, lower first", seq(r), "50 40 60");
    }

    private static void headAtBoundaries() {
        check("head at 0", seq(new SSTF().schedule(new int[] {199, 5, 5}, 0, 200, null)), "0 5 5 199");
        check("head at 199", seq(new SSTF().schedule(new int[] {0, 198}, 199, 200, null)), "199 198 0");
    }

    private static void singleRequest() {
        DiskResult r = new SSTF().schedule(new int[] {7}, 7, 200, null);
        check("single request on the head", seq(r) + " / " + r.getTotalHeadMovement(), "7 7 / 0");
    }

    private static void inputArrayIsNotModified() {
        int[] requests = {98, 183, 37};
        new SSTF().schedule(requests, 53, 200, null);
        check("input array unchanged", Arrays.toString(requests), "[98, 183, 37]");
    }

    private static void invalidInputIsRejected() {
        check("empty queue rejected", rejects(new int[0], 0, 200), "rejected");
        check("request outside disk rejected", rejects(new int[] {200}, 0, 200), "rejected");
        check("negative head rejected", rejects(new int[] {1}, -1, 200), "rejected");
    }

    // ---- helpers

    private static DiskResult run(SstfSampleData.DiskProblem p) {
        return new SSTF().schedule(p.getRequests(), p.getHead(), p.getDiskSize(), null);
    }

    private static String rejects(int[] requests, int head, int diskSize) {
        try {
            new SSTF().schedule(requests, head, diskSize, null);
            return "accepted";
        } catch (IllegalArgumentException ex) {
            return "rejected";
        }
    }

    private static String seq(DiskResult r) {
        StringBuilder sb = new StringBuilder();
        for (int c : r.getSeekSequence()) {
            sb.append(sb.length() == 0 ? "" : " ").append(c);
        }
        return sb.toString();
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
