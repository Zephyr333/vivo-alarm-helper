package com.example.vivoalarmhelper;

import android.accessibilityservice.AccessibilityService;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class AlarmAccessibilityService extends AccessibilityService {
    private static final String CLOCK_PACKAGE = "com.android.BBKClock";
    private static final String SET_ALARM_CLASS =
            "com.android.BBKClock.alarmclock.view.activity.SetAlarm";
    private static final long FIND_TIMEOUT_MS = 5000L;
    private static final long RETRY_DELAY_MS = 120L;
    private static final long NEXT_ALARM_DELAY_MS = 750L;

    private static volatile AlarmAccessibilityService connectedInstance;
    private static final Set<ConnectionListener> connectionListeners =
            new CopyOnWriteArraySet<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayDeque<AlarmSpec> pendingAlarms = new ArrayDeque<>();
    private boolean waitingForEditor;
    private boolean sawSetAlarmWindow;
    private boolean saveTriggered;
    private long launchTime;
    private long sessionT0;
    private int totalAlarmCount;
    private int savedAlarmCount;
    private String activeProfileName;

    public static AlarmAccessibilityService getConnectedInstance() {
        return connectedInstance;
    }

    public static void addConnectionListener(ConnectionListener listener) {
        if (listener != null) connectionListeners.add(listener);
    }

    public static void removeConnectionListener(ConnectionListener listener) {
        if (listener != null) connectionListeners.remove(listener);
    }

    public boolean isCreatingAlarms() {
        return isBusy();
    }

    public void executePendingAlarms() {
        if (isBusy()) return;
        PendingExecution pending = PendingExecutionStore.claim(this);
        if (pending == null) return;

        ProfileStore.ensureInitialized(this);
        AlarmProfile profile = ProfileStore.getProfile(this, pending.profileId);
        if (profile == null) {
            Toast.makeText(this, "待执行的闹钟方案已被删除",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String error = createAlarms(profile, pending.requestedAt,
                pending.runtimeBaseTargetAt);
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        connectedInstance = this;
        notifyConnectionListeners();
        handler.post(this::executePendingAlarms);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        handleServiceDisconnect("无障碍服务连接已中断");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        handleServiceDisconnect("无障碍服务已停止");
        super.onDestroy();
    }

    private void handleServiceDisconnect(String reason) {
        if (isBusy()) {
            failSession(reason);
        } else {
            abortQueue();
        }
        if (connectedInstance == this) {
            connectedInstance = null;
            notifyConnectionListeners();
        }
    }

    private static void notifyConnectionListeners() {
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.onConnectionChanged();
            } catch (RuntimeException ignored) {
            }
        }
    }

    /** Uses the shortcut click time so reconnect latency never shifts relative alarms. */
    private String createAlarms(AlarmProfile profile, long requestedAt,
            long runtimeBaseTargetAt) {
        if (isBusy()) return "已有一组闹钟正在创建，请稍后再试";
        if (profile.getAlarms().isEmpty()) return "这套方案没有闹钟";

        sessionT0 = requestedAt;
        long validationNow = System.currentTimeMillis();
        String sequenceProblem = ProfileTiming.sequenceProblem(profile);
        if (sequenceProblem != null) {
            return "“" + profile.getName() + "”无法执行：" + sequenceProblem;
        }
        List<AlarmSpec> specs = new ArrayList<>();
        for (int index = 0; index < profile.getAlarms().size(); index++) {
            AlarmConfig config = profile.getAlarms().get(index);
            String problem = validate(config, index + 1,
                    validationNow, profile.isSequence());
            if (problem != null) return "“" + profile.getName() + "”无法执行：" + problem;
            if (profile.isSequence()) {
                if (runtimeBaseTargetAt <= 0L) {
                    return "“" + profile.getName()
                            + "”无法执行：没有选择本次第一个闹钟的时间";
                }
                Calendar target = runtimeSequenceTarget(
                        profile, index, runtimeBaseTargetAt);
                if (config.repeatType == AlarmConfig.REPEAT_ONCE
                        && target.getTimeInMillis() <= validationNow) {
                    return "“" + profile.getName() + "”无法执行：第 "
                            + (index + 1) + " 个一次性闹钟时间已经过去";
                }
                specs.add(new AlarmSpec(target, config));
            } else {
                int effectiveOffset = config.timeMode == AlarmConfig.TIME_RELATIVE
                        ? ProfileTiming.effectiveRelativeOffsetMinutes(
                                profile, index)
                        : 0;
                specs.add(createSpec(config, sessionT0, effectiveOffset));
            }
        }
        pendingAlarms.addAll(specs);
        activeProfileName = profile.getName();
        totalAlarmCount = specs.size();
        savedAlarmCount = 0;
        launchNextAlarm();
        return null;
    }

    private String validate(AlarmConfig config, int number, long t0,
            boolean runtimeSequence) {
        if (!runtimeSequence && config.timeMode == AlarmConfig.TIME_RELATIVE
                && config.repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY) {
            return "第 " + number + " 个闹钟的轮班制不能使用“从现在起”";
        }
        if (config.repeatType == AlarmConfig.REPEAT_CUSTOM
                && config.customDays == 0) {
            return "第 " + number + " 个闹钟没有选择重复日期";
        }
        if (config.repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY) {
            boolean enabled = false;
            for (AlarmConfig.ShiftDay day : config.shiftDays) enabled |= day.enabled;
            if (!enabled) return "第 " + number + " 个轮班闹钟没有启用的周期日";
        }
        if (!runtimeSequence && config.timeMode == AlarmConfig.TIME_ABSOLUTE
                && config.repeatType == AlarmConfig.REPEAT_ONCE) {
            Calendar target = absoluteTarget(config);
            if (target == null) return "第 " + number + " 个闹钟的日期格式无效";
            if (target.getTimeInMillis() <= t0) {
                return "第 " + number + " 个一次性闹钟的绝对日期时间已经过去";
            }
        }
        return null;
    }

    private Calendar runtimeSequenceTarget(AlarmProfile profile,
            int alarmIndex, long baseTargetAt) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(baseTargetAt);
        target.add(Calendar.MINUTE,
                ProfileTiming.elapsedAfterRuntimeBaseMinutes(
                        profile, alarmIndex));
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        return target;
    }

    private AlarmSpec createSpec(AlarmConfig config, long t0,
            int effectiveOffsetMinutes) {
        Calendar target;
        if (config.timeMode == AlarmConfig.TIME_RELATIVE) {
            target = Calendar.getInstance();
            target.setTimeInMillis(t0);
            target.add(Calendar.MINUTE, effectiveOffsetMinutes);
        } else if (config.repeatType == AlarmConfig.REPEAT_ONCE) {
            target = absoluteTarget(config);
        } else {
            target = Calendar.getInstance();
            target.set(Calendar.HOUR_OF_DAY, config.hour);
            target.set(Calendar.MINUTE, config.minute);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);
        }
        return new AlarmSpec(target, config);
    }

    private Calendar absoluteTarget(AlarmConfig config) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(config.date);
            if (date == null) return null;
            Calendar target = Calendar.getInstance();
            target.setTime(date);
            target.set(Calendar.HOUR_OF_DAY, config.hour);
            target.set(Calendar.MINUTE, config.minute);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);
            return target;
        } catch (ParseException exception) {
            return null;
        }
    }

    private boolean isBusy() {
        return waitingForEditor || !pendingAlarms.isEmpty();
    }

    private void launchNextAlarm() {
        AlarmSpec spec = pendingAlarms.poll();
        if (spec == null) {
            Toast.makeText(this,
                    "已创建“" + activeProfileName + "”："
                            + totalAlarmCount + " 个闹钟",
                    Toast.LENGTH_LONG).show();
            return;
        }

        AlarmConfig config = spec.config;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName(CLOCK_PACKAGE, SET_ALARM_CLASS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                | Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.putExtra("intent_tag",
                "vivo_alarm_profile_" + sessionT0 + "_" + config.id);
        intent.putExtra("hour", spec.target.get(Calendar.HOUR_OF_DAY));
        intent.putExtra("minutes", spec.target.get(Calendar.MINUTE));
        intent.putExtra("repeat", config.vivoRepeat());
        intent.putExtra("daysofweek", config.vivoDaysOfWeek());
        intent.putExtra("remindway", remindWay(config));
        intent.putExtra("message", config.label);
        if (!config.alertUri.isEmpty()) {
            intent.putExtra("alert", config.alertUri);
            grantRingtoneAccess(intent, config.alertUri);
        }
        intent.putExtra("snooze", String.valueOf(config.snoozeMinutes));
        intent.putExtra("talker", config.voiceBroadcast ? 1 : 0);
        intent.putExtra("shift_today", config.shiftToday);
        intent.putExtra("shift_days_count", config.shiftDaysCount);
        intent.putExtra("shift_days_time", config.shiftTimes());
        intent.putExtra("shift_days_enabled", config.shiftEnabled());
        intent.putExtra("china_holiday", config.excludeHolidays ? 1 : 0);
        intent.putExtra("snooze_talker", config.snoozeTalker ? 1 : 0);
        intent.putExtra("default_ringtone", config.ringtoneType);
        intent.putExtra("broadcast_content", config.broadcastContent);
        intent.putExtra("vibrate_mode", config.vibrateMode);
        intent.putExtra("delete_after_ring_switch",
                config.deleteAfterRing && !config.isRepeating() ? 1 : 0);
        // vivo's public Intent mapping is inverted: 0 means enabled, 1 means disabled.
        intent.putExtra("alarm_remind_later_enable",
                config.snoozeEnabled ? 0 : 1);
        intent.putExtra("alarm_remind_later_max_number", config.snoozeCount);
        intent.putExtra("date_alarm", config.isRepeating()
                ? "" : formatDate(spec.target.getTime()));

        waitingForEditor = true;
        sawSetAlarmWindow = false;
        saveTriggered = false;
        launchTime = System.currentTimeMillis();
        try {
            startActivity(intent);
            handler.postDelayed(scanRunnable, RETRY_DELAY_MS);
        } catch (ActivityNotFoundException | SecurityException exception) {
            failSession("无法打开 vivo 新闹钟编辑页（"
                    + exception.getClass().getSimpleName() + "）");
        }
    }

    private int remindWay(AlarmConfig config) {
        if (config.ringtoneType == AlarmConfig.RING_SILENT) return 3;
        return config.vibrateMode == 0 ? 1 : 2;
    }

    private void grantRingtoneAccess(Intent intent, String value) {
        try {
            Uri uri = Uri.parse(value);
            if ("content".equals(uri.getScheme())) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setClipData(ClipData.newRawUri("alarm ringtone", uri));
                grantUriPermission(CLOCK_PACKAGE, uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!waitingForEditor || event == null) return;
        if (!TextUtils.equals(CLOCK_PACKAGE, event.getPackageName())) return;
        CharSequence className = event.getClassName();
        if (className != null && SET_ALARM_CLASS.contentEquals(className)) {
            sawSetAlarmWindow = true;
        }
        handler.removeCallbacks(scanRunnable);
        handler.postDelayed(scanRunnable, 60L);
    }

    private final Runnable scanRunnable = this::scanAndSave;

    private void scanAndSave() {
        if (!waitingForEditor || saveTriggered) return;
        if (System.currentTimeMillis() - launchTime > FIND_TIMEOUT_MS) {
            failSession("没有找到 vivo 的“完成”控件");
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null
                || !CLOCK_PACKAGE.contentEquals(root.getPackageName())
                || !sawSetAlarmWindow) {
            handler.postDelayed(scanRunnable, RETRY_DELAY_MS);
            return;
        }
        AccessibilityNodeInfo done = findDoneNode(root);
        if (done == null) {
            handler.postDelayed(scanRunnable, RETRY_DELAY_MS);
            return;
        }
        AccessibilityNodeInfo clickable = findClickableAncestor(done);
        if (clickable == null
                || !clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            handler.postDelayed(scanRunnable, RETRY_DELAY_MS);
            return;
        }
        saveTriggered = true;
        waitingForEditor = false;
        savedAlarmCount++;
        Toast.makeText(this, "已保存 " + savedAlarmCount + "/"
                + totalAlarmCount, Toast.LENGTH_SHORT).show();
        handler.postDelayed(this::launchNextAlarm, NEXT_ALARM_DELAY_MS);
    }

    private AccessibilityNodeInfo findDoneNode(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> byText =
                root.findAccessibilityNodeInfosByText("完成");
        if (byText != null) {
            for (AccessibilityNodeInfo node : byText) {
                if (isExactDone(node)) return node;
            }
        }
        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        collectNodes(root, nodes);
        for (AccessibilityNodeInfo node : nodes) {
            if (isExactDone(node)) return node;
        }
        return null;
    }

    private boolean isExactDone(AccessibilityNodeInfo node) {
        if (node == null || !node.isVisibleToUser()) return false;
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        return "完成".contentEquals(text) || "完成".contentEquals(description)
                || "Done".contentEquals(text) || "Done".contentEquals(description);
    }

    private AccessibilityNodeInfo findClickableAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        for (int depth = 0; current != null && depth < 5; depth++) {
            if (current.isVisibleToUser() && current.isEnabled()
                    && current.isClickable()) return current;
            current = current.getParent();
        }
        return null;
    }

    private void collectNodes(AccessibilityNodeInfo node,
            List<AccessibilityNodeInfo> output) {
        if (node == null) return;
        output.add(node);
        for (int index = 0; index < node.getChildCount(); index++) {
            collectNodes(node.getChild(index), output);
        }
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date);
    }

    private void failSession(String reason) {
        String name = activeProfileName;
        int saved = savedAlarmCount;
        int total = totalAlarmCount;
        abortQueue();
        String prefix = saved > 0
                ? "“" + name + "”只完成 " + saved + "/" + total + "："
                : "“" + name + "”创建失败：";
        Toast.makeText(this, prefix + reason + "，已停止后续创建",
                Toast.LENGTH_LONG).show();
    }

    private void abortQueue() {
        waitingForEditor = false;
        sawSetAlarmWindow = false;
        saveTriggered = false;
        pendingAlarms.clear();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onInterrupt() {
        // Interrupting feedback is not the same as disconnecting the service.
        // Shutdown and queue cleanup are handled by onUnbind/onDestroy.
    }

    public interface ConnectionListener {
        void onConnectionChanged();
    }

    private static final class AlarmSpec {
        final Calendar target;
        final AlarmConfig config;
        AlarmSpec(Calendar target, AlarmConfig config) {
            this.target = target;
            this.config = config;
        }
    }
}
