package com.example.vivoalarmhelper;

public final class SequenceOffsetsTest {
    private static final int MAX = 525600;

    public static void main(String[] args) {
        independentOffsetsStayIndependent();
        runtimeSequenceIntervalsAccumulateAfterBase();
        sequenceRejectsOverlongTotal();
        System.out.println("SequenceOffsetsTest passed");
    }

    private static void independentOffsetsStayIndependent() {
        int[] offsets = {480, 15, 5};
        assertEquals(480, SequenceOffsets.independentOffset(480, MAX));
        assertEquals(15, SequenceOffsets.independentOffset(15, MAX));
    }

    private static void runtimeSequenceIntervalsAccumulateAfterBase() {
        int[] intervals = {0, 15, 5};
        assertEquals(0, SequenceOffsets.elapsedAfterBase(
                intervals, 0, MAX));
        assertEquals(15, SequenceOffsets.elapsedAfterBase(
                intervals, 1, MAX));
        assertEquals(20, SequenceOffsets.elapsedAfterBase(
                intervals, 2, MAX));
        if (SequenceOffsets.sequenceExceedsLimit(intervals, MAX)) {
            throw new AssertionError("Valid sequence was rejected");
        }
    }

    private static void sequenceRejectsOverlongTotal() {
        int[] intervals = {0, 400000, 200000};
        if (!SequenceOffsets.sequenceExceedsLimit(intervals, MAX)) {
            throw new AssertionError("Overlong sequence was accepted");
        }
        try {
            SequenceOffsets.elapsedAfterBase(intervals, 2, MAX);
            throw new AssertionError("Expected an overlong sequence failure");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
