package com.example.vivoalarmhelper;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

final class AccessibilityUtils {
    private AccessibilityUtils() {
    }

    static boolean isEnabled(Context context) {
        return isEnabledInSystem(context);
    }

    static AccessibilityStatus getStatus(Context context) {
        return AccessibilityStatus.resolve(isEnabledInSystem(context),
                AlarmAccessibilityService.getConnectedInstance() != null);
    }

    private static boolean isEnabledInSystem(Context context) {
        ComponentName component = new ComponentName(context, AlarmAccessibilityService.class);
        AccessibilityManager manager = (AccessibilityManager)
                context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager != null) {
            try {
                List<AccessibilityServiceInfo> services =
                        manager.getEnabledAccessibilityServiceList(
                                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
                if (services != null) {
                    for (AccessibilityServiceInfo service : services) {
                        if (service != null && AccessibilityServiceIds.contains(
                                service.getId(), component.getPackageName(),
                                component.getClassName())) {
                            return true;
                        }
                    }
                }
            } catch (RuntimeException ignored) {
                // Some OEM builds can briefly fail this query while settings change.
            }
        }

        String enabled = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return AccessibilityServiceIds.contains(enabled, component.getPackageName(),
                component.getClassName());
    }
}
