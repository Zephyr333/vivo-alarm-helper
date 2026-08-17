package com.example.vivoalarmhelper;

/** Pure minute arithmetic shared by Android execution code and desktop tests. */
public final class SequenceOffsets {
    private SequenceOffsets() {
    }

    public static int elapsedAfterBase(int[] intervals,
            int index, int maxOffset) {
        if (intervals == null || index < 0 || index >= intervals.length) {
            throw new IllegalArgumentException("Invalid interval index");
        }
        long total = 0L;
        for (int position = 1; position <= index; position++) {
            int interval = intervals[position];
            if (interval < 1) {
                throw new IllegalArgumentException("Invalid interval");
            }
            total += interval;
            if (total > maxOffset) {
                throw new IllegalArgumentException("Sequence is too long");
            }
        }
        return (int) total;
    }

    public static boolean sequenceExceedsLimit(int[] intervals, int maxOffset) {
        if (intervals == null || intervals.length == 0) return true;
        long total = 0L;
        for (int index = 1; index < intervals.length; index++) {
            int interval = intervals[index];
            total += interval;
            if (interval < 1 || total > maxOffset) return true;
        }
        return false;
    }

    public static int independentOffset(int value, int maxOffset) {
        if (value < 1 || value > maxOffset) {
            throw new IllegalArgumentException("Offset is out of range");
        }
        return value;
    }
}
