package com.example.vivoalarmhelper;

public final class TimeTextTest {
    public static void main(String[] args) {
        expect("8 小时后", TimeText.durationAfter(480));
        expect("8 小时 15 分钟后", TimeText.durationAfter(495));
        expect("8 小时 20 分钟后", TimeText.durationAfter(500));
        expect("25 小时后", TimeText.durationAfter(1500));
        expect("1 分钟后", TimeText.durationAfter(1));
        expect("08:05", TimeText.clock(8, 5));
        System.out.println("TimeTextTest passed");
    }

    private static void expect(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected <" + expected
                    + "> but was <" + actual + ">");
        }
    }
}
