package com.example.vivoalarmhelper;

enum AccessibilityStatus {
    DISABLED,
    ENABLED_DISCONNECTED,
    CONNECTED;

    static AccessibilityStatus resolve(boolean systemEnabled, boolean connected) {
        if (connected) return CONNECTED;
        return systemEnabled ? ENABLED_DISCONNECTED : DISABLED;
    }
}
