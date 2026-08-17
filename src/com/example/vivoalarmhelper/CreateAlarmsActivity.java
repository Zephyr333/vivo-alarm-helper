package com.example.vivoalarmhelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;

public final class CreateAlarmsActivity extends Activity {
    public static final String ACTION_RUN_PROFILE =
            "com.example.vivoalarmhelper.action.RUN_PROFILE";
    public static final String EXTRA_PROFILE_ID = "profile_id";
    public static final String EXTRA_RUNTIME_BASE_TARGET_AT =
            "runtime_base_target_at";

    private static final long CONNECT_TIMEOUT_MS = 10000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AlarmAccessibilityService.ConnectionListener connectionListener =
            () -> handler.post(this::handleConnectionState);
    private AlarmProfile profile;
    private PendingExecution pending;
    private boolean listenerRegistered;
    private boolean completed;

    public static Intent createRunIntent(Context context, String profileId) {
        Intent intent = new Intent(context, CreateAlarmsActivity.class);
        intent.setAction(ACTION_RUN_PROFILE);
        intent.putExtra(EXTRA_PROFILE_ID, profileId);
        return intent;
    }

    public static Intent createRuntimeRunIntent(Context context,
            String profileId, long baseTargetAt) {
        Intent intent = createRunIntent(context, profileId);
        intent.putExtra(EXTRA_RUNTIME_BASE_TARGET_AT, baseTargetAt);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ProfileStore.ensureInitialized(this);
        String requestedId = getIntent().getStringExtra(EXTRA_PROFILE_ID);
        if (!ACTION_RUN_PROFILE.equals(getIntent().getAction()) || requestedId == null) {
            openSetup("请从主应用或方案桌面快捷方式执行");
            return;
        }
        profile = ProfileStore.getProfile(this, requestedId);
        if (profile == null) {
            openSetup("这个闹钟方案已删除，原桌面快捷方式不能再执行");
            return;
        }

        long runtimeBaseTargetAt = getIntent().getLongExtra(
                EXTRA_RUNTIME_BASE_TARGET_AT, 0L);
        if (profile.isSequence() && runtimeBaseTargetAt <= 0L) {
            completed = true;
            startActivity(RunBaseActivity.createIntent(
                    this, profile.getId()));
            finish();
            return;
        }

        AccessibilityStatus status = AccessibilityUtils.getStatus(this);
        if (status == AccessibilityStatus.DISABLED) {
            openSetup("无法执行“" + profile.getName() + "”：系统未开启无障碍服务");
            return;
        }

        AlarmAccessibilityService service =
                AlarmAccessibilityService.getConnectedInstance();
        if (service != null && service.isCreatingAlarms()) {
            Toast.makeText(this, "已有一组闹钟正在创建，请勿重复点击",
                    Toast.LENGTH_SHORT).show();
            completed = true;
            finish();
            return;
        }

        try {
            pending = PendingExecutionStore.enqueue(
                    this, profile.getId(), System.currentTimeMillis(),
                    runtimeBaseTargetAt);
        } catch (IllegalStateException exception) {
            openSetup("无法保存待执行任务，请重新点击执行");
            return;
        }
        if (pending == null) {
            Toast.makeText(this, "已有闹钟任务正在等待执行，请勿重复点击",
                    Toast.LENGTH_SHORT).show();
            completed = true;
            finish();
            return;
        }

        if (status == AccessibilityStatus.CONNECTED) {
            executeAndFinish();
        } else {
            waitForConnection();
        }
    }

    private void waitForConnection() {
        AlarmAccessibilityService.addConnectionListener(connectionListener);
        listenerRegistered = true;
        Toast.makeText(this, "正在恢复创建服务，连接后会自动继续",
                Toast.LENGTH_SHORT).show();
        handler.postAtTime(connectTimeoutRunnable,
                SystemClock.uptimeMillis() + CONNECT_TIMEOUT_MS);
        handleConnectionState();
    }

    private void handleConnectionState() {
        if (completed) return;
        AccessibilityStatus status = AccessibilityUtils.getStatus(this);
        if (status == AccessibilityStatus.CONNECTED) {
            executeAndFinish();
        } else if (status == AccessibilityStatus.DISABLED) {
            cancelPending();
            openSetup("无法执行“" + profile.getName() + "”：无障碍服务已关闭");
        }
    }

    private final Runnable connectTimeoutRunnable = () -> {
        if (completed) return;
        AccessibilityStatus status = AccessibilityUtils.getStatus(this);
        if (status == AccessibilityStatus.CONNECTED) {
            executeAndFinish();
        } else {
            cancelPending();
            openSetup(status == AccessibilityStatus.DISABLED
                    ? "无法执行“" + profile.getName() + "”：系统未开启无障碍服务"
                    : "系统中已开启无障碍，但服务长时间没有恢复。请在系统无障碍设置中将本服务关闭后重新开启");
        }
    };

    private void executeAndFinish() {
        if (completed) return;
        AlarmAccessibilityService service =
                AlarmAccessibilityService.getConnectedInstance();
        if (service == null) return;
        completed = true;
        stopWaitingForConnection();
        service.executePendingAlarms();
        finishAndRemoveTask();
    }

    private void cancelPending() {
        if (pending != null) {
            PendingExecutionStore.cancel(this, pending.token);
            pending = null;
        }
    }

    private void openSetup(String message) {
        if (completed) return;
        completed = true;
        stopWaitingForConnection();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Intent main = new Intent(this, MainActivity.class);
        main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(main);
        finish();
    }

    private void stopWaitingForConnection() {
        handler.removeCallbacks(connectTimeoutRunnable);
        if (listenerRegistered) {
            AlarmAccessibilityService.removeConnectionListener(connectionListener);
            listenerRegistered = false;
        }
    }

    @Override
    protected void onDestroy() {
        stopWaitingForConnection();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
