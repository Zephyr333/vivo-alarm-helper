package com.example.vivoalarmhelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

public final class ShortcutHelper {
    private static final String ID_PREFIX = "profile_";

    private ShortcutHelper() {
    }

    public static void pinProfile(Activity activity, AlarmProfile profile) {
        ShortcutManager manager = activity.getSystemService(ShortcutManager.class);
        if (manager == null || !manager.isRequestPinShortcutSupported()) {
            Toast.makeText(activity, "当前桌面不支持由应用添加快捷方式",
                    Toast.LENGTH_LONG).show();
            return;
        }
        try {
            ShortcutInfo shortcut = buildShortcut(activity, profile);
            if (updateIfAlreadyPinned(manager, shortcut)) {
                Toast.makeText(activity, "原桌面快捷方式已更新",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (manager.requestPinShortcut(shortcut, null)) {
                Toast.makeText(activity,
                        "请在系统窗口中确认添加；以后点击该快捷方式会直接创建闹钟",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(activity, "桌面没有接受快捷方式请求",
                        Toast.LENGTH_LONG).show();
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            Toast.makeText(activity, "添加快捷方式失败："
                    + exception.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    public static void updatePinnedProfile(Context context, AlarmProfile profile) {
        ShortcutManager manager = context.getSystemService(ShortcutManager.class);
        if (manager == null) return;
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            try {
                manager.updateShortcuts(Collections.singletonList(
                        buildShortcut(appContext, profile)));
            } catch (IllegalArgumentException | IllegalStateException ignored) {
            }
        }, "update-alarm-shortcut").start();
    }

    public static void disableProfileShortcut(Context context, String profileId) {
        ShortcutManager manager = context.getSystemService(ShortcutManager.class);
        if (manager == null) return;
        new Thread(() -> {
            try {
                manager.disableShortcuts(
                        Collections.singletonList(shortcutId(profileId)),
                        "对应的闹钟方案已删除");
            } catch (IllegalArgumentException | IllegalStateException ignored) {
            }
        }, "disable-alarm-shortcut").start();
    }

    private static boolean updateIfAlreadyPinned(
            ShortcutManager manager, ShortcutInfo shortcut) {
        List<ShortcutInfo> pinned = manager.getPinnedShortcuts();
        for (ShortcutInfo item : pinned) {
            if (shortcut.getId().equals(item.getId())) {
                manager.updateShortcuts(Collections.singletonList(shortcut));
                return true;
            }
        }
        return false;
    }

    private static ShortcutInfo buildShortcut(Context context, AlarmProfile profile) {
        Intent intent = new Intent(context, CreateAlarmsActivity.class);
        intent.setAction(CreateAlarmsActivity.ACTION_RUN_PROFILE);
        intent.putExtra(CreateAlarmsActivity.EXTRA_PROFILE_ID, profile.getId());
        int iconId = context.getResources().getIdentifier(
                "ic_launcher", "mipmap", context.getPackageName());
        return new ShortcutInfo.Builder(context, shortcutId(profile.getId()))
                .setShortLabel(shortLabel(profile.getName()))
                .setLongLabel("创建闹钟：" + profile.getName())
                .setIcon(Icon.createWithResource(context, iconId))
                .setIntent(intent)
                .build();
    }

    private static String shortcutId(String profileId) {
        return ID_PREFIX + profileId;
    }

    private static String shortLabel(String name) {
        int count = name.codePointCount(0, name.length());
        if (count <= 10) return name;
        return name.substring(0, name.offsetByCodePoints(0, 10));
    }
}
