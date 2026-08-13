package com.example.vivoalarmhelper;

public final class AccessibilityStatusTest {
    public static void main(String[] args) {
        assertStatus(AccessibilityStatus.DISABLED,
                AccessibilityStatus.resolve(false, false));
        assertStatus(AccessibilityStatus.ENABLED_DISCONNECTED,
                AccessibilityStatus.resolve(true, false));
        assertStatus(AccessibilityStatus.CONNECTED,
                AccessibilityStatus.resolve(false, true));
        assertStatus(AccessibilityStatus.CONNECTED,
                AccessibilityStatus.resolve(true, true));
        System.out.println("Accessibility status tests passed");
    }

    private static void assertStatus(
            AccessibilityStatus expected, AccessibilityStatus actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
