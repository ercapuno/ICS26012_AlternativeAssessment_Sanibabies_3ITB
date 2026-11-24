import java.util.Arrays;
import java.util.Locale;

import clook.CLook;
import clook.CLookSampleData;
import model.DiskResult;

/**
 * Checks the C-LOOK algorithm without the GUI.
 * Plain Java (no JUnit): prints PASS/FAIL per check and exits with code 1 if any check fails.
 */
public class CLookTest {

    private static int failures = 0;

    public static void main(String[] args) {
        textbookUpSample();
        twentyRequestDownSample();
        requestOnTheHead();
        duplicatesAreConsecutive();
        allRequestsOnTheOtherSide();
        headAtBoundaries();
        singleRequest();
        inputArrayIsNotModified();
        invalidInputIsRejected();

        System.out.println(failures == 0 ? "\nAll checks passed." : "\n" + failures + " check(s) FAILED.");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void textbookUpSample() {
        DiskResult r = run(CLookSampleData.textbook());
        check("textbook up sequence", seq(r), "53 65 67 98 122 124 183 14 37");
        check("textbook up total (jump 183->14 counted)", String.valueOf(r.getTotalHeadMovement()), "322");
        check("textbook up average", fmt(r.getAverageSeekDistance()), "40.25");
        check("textbook up seek time", fmt(r.getSeekTime(4)), "1288.00");
        check("textbook up steps", Arrays.toString(r.getStepMovements()), "[12, 2, 31, 24, 2, 59, 169, 23]");
    }

    private static void twentyRequestDownSample() {
        DiskResult r = run(CLookSampleData.tiesAndDuplicates());
        check("sample 1 sequence", seq(r),
                "100 100 90 75 60 45 45 35 25 12 5 0 199 182 170 160 150 140 130 118 110");
        check("sample 1 total (jump 0->199 counted)", String.valueOf(r.getTotalHeadMovement()), "388");
        check("sample 1 average", fmt(r.getAverageSeekDistance()), "19.40");
        check("sample 1 seek time", fmt(r.getSeekTime(4)), "1552.00");
        check("sample 1 steps", Arrays.toString(r.getStepMovements()),
                "[0, 10, 15, 15, 15, 0, 10, 10, 13, 7, 5, 199, 17, 12, 10, 10, 10, 10, 12, 8]");
    }

    private static void requestOnTheHead() {
        check("request on head, moving down: served first",
                seq(new CLook().schedule(new int[] {100, 50, 150}, 100, 200, "Down")), "100 100 50 150");
        check("request on head, moving up: served first",
                seq(new CLook().schedule(new int[] {100, 50, 150}, 100, 200, "Up")), "100 100 150 50");
    }

    private static void duplicatesAreConsecutive() {
        DiskResult r = new CLook().schedule(new int[] {70, 30, 70, 30}, 50, 200, "Up");
        check("duplicates served back-to-back", seq(r), "50 70 70 30 30");
        check("duplicates total", String.valueOf(r.getTotalHeadMovement()), "60");
    }

    private static void allRequestsOnTheOtherSide() {
        check("up with every request below the head",
                seq(new CLook().schedule(new int[] {40, 10, 25}, 50, 200, "Up")), "50 10 25 40");
        check("down with every request above the head",
                seq(new CLook().schedule(new int[] {60, 90, 75}, 50, 200, "Down")), "50 90 75 60");
    }

    private static void headAtBoundaries() {
        check("head at 0 moving down",
                seq(new CLook().schedule(new int[] {5, 199}, 0, 200, "Down")), "0 199 5");
        check("head at 199 moving up",
                seq(new CLook().schedule(new int[] {0, 198}, 199, 200, "Up")), "199 0 198");
    }

    private static void singleRequest() {
        DiskResult r = new CLook().schedule(new int[] {7}, 7, 200, "Up");
        check("single request on the head", seq(r) + " / " + r.getTotalHeadMovement(), "7 7 / 0");
    }

    private static void inputArrayIsNotModified() {
        int[] requests = {98, 183, 37};
        new CLook().schedule(requests, 53, 200, "Up");
        check("input array unchanged", Arrays.toString(requests), "[98, 183, 37]");
    }

    private static void invalidInputIsRejected() {
        check("empty queue rejected", rejects(new int[0], 0, 200, "Up"), "rejected");
        check("request outside disk rejected", rejects(new int[] {200}, 0, 200, "Up"), "rejected");
        check("negative head rejected", rejects(new int[] {1}, -1, 200, "Up"), "rejected");
        check("bad direction rejected", rejects(new int[] {1}, 0, 200, "Left"), "rejected");
    }

    // ---- helpers

    private static DiskResult run(CLookSampleData.DiskSample s) {
        return new CLook().schedule(s.getRequests(), s.getHead(), s.getDiskSize(), s.getDirection());
    }

    private static String rejects(int[] requests, int head, int diskSize, String direction) {
        try {
            new CLook().schedule(requests, head, diskSize, direction);
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
