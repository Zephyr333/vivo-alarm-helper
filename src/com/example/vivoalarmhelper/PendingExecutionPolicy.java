package com.example.vivoalarmhelper;

final class PendingExecutionPolicy {
    private static final long CLOCK_SKEW_TOLERANCE_MS = 5000L;

    private PendingExecutionPolicy() {
    }

    static boolean isFresh(long createdAt, long now, long lifetimeMs) {
        if (createdAt <= 0L || lifetimeMs < 0L) return false;
        if (createdAt - now > CLOCK_SKEW_TOLERANCE_MS) return false;
        return now - createdAt <= lifetimeMs;
    }
}
