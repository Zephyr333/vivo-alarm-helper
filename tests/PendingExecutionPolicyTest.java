package com.example.vivoalarmhelper;

public final class PendingExecutionPolicyTest {
    public static void main(String[] args) {
        long createdAt = 1_000_000L;
        long lifetime = 30_000L;

        assertFresh(createdAt, createdAt, lifetime);
        assertFresh(createdAt, createdAt + lifetime, lifetime);
        assertExpired(createdAt, createdAt + lifetime + 1L, lifetime);
        assertExpired(0L, createdAt, lifetime);
        assertExpired(createdAt + 6_000L, createdAt, lifetime);

        System.out.println("Pending execution policy tests passed");
    }

    private static void assertFresh(long createdAt, long now, long lifetime) {
        if (!PendingExecutionPolicy.isFresh(createdAt, now, lifetime)) {
            throw new AssertionError("Expected pending execution to be fresh");
        }
    }

    private static void assertExpired(long createdAt, long now, long lifetime) {
        if (PendingExecutionPolicy.isFresh(createdAt, now, lifetime)) {
            throw new AssertionError("Expected pending execution to be expired");
        }
    }
}
