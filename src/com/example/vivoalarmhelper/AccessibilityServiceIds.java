package com.example.vivoalarmhelper;

final class AccessibilityServiceIds {
    private AccessibilityServiceIds() {
    }

    static boolean contains(String enabledServices, String packageName,
            String serviceClassName) {
        if (enabledServices == null || enabledServices.trim().isEmpty()) {
            return false;
        }
        String[] entries = enabledServices.split(":");
        for (String entry : entries) {
            int separator = entry.indexOf('/');
            if (separator <= 0 || separator == entry.length() - 1) continue;
            String entryPackage = entry.substring(0, separator).trim();
            String entryClass = entry.substring(separator + 1).trim();
            if (entryClass.startsWith(".")) {
                entryClass = entryPackage + entryClass;
            }
            if (packageName.equals(entryPackage)
                    && serviceClassName.equals(entryClass)) {
                return true;
            }
        }
        return false;
    }
}
