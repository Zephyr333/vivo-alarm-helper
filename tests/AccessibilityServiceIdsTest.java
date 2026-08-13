package com.example.vivoalarmhelper;

public final class AccessibilityServiceIdsTest {
    public static void main(String[] args) {
        String packageName = "com.example.vivoalarmhelper";
        String className = packageName + ".AlarmAccessibilityService";

        assertMatch(packageName + "/" + className, packageName, className);
        assertMatch(packageName + "/.AlarmAccessibilityService", packageName, className);
        assertMatch("other/.Service:" + packageName + "/.AlarmAccessibilityService",
                packageName, className);
        assertNoMatch("other/.AlarmAccessibilityService", packageName, className);
        assertNoMatch("", packageName, className);

        System.out.println("Accessibility service id tests passed");
    }

    private static void assertMatch(String enabled, String packageName, String className) {
        if (!AccessibilityServiceIds.contains(enabled, packageName, className)) {
            throw new AssertionError("Expected a match: " + enabled);
        }
    }

    private static void assertNoMatch(String enabled, String packageName, String className) {
        if (AccessibilityServiceIds.contains(enabled, packageName, className)) {
            throw new AssertionError("Expected no match: " + enabled);
        }
    }
}
