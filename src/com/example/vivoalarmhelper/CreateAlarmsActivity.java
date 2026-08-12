package com.example.vivoalarmhelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public final class CreateAlarmsActivity extends Activity {
    public static final String ACTION_RUN_PROFILE =
            "com.example.vivoalarmhelper.action.RUN_PROFILE";
    public static final String EXTRA_PROFILE_ID = "profile_id";

    private static final int MAX_CONNECT_RETRIES = 20;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int retryCount;
    private AlarmProfile profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ProfileStore.ensureInitialized(this);
        String requestedId = getIntent().getStringExtra(EXTRA_PROFILE_ID);
        if (!ACTION_RUN_PROFILE.equals(getIntent().getAction()) || requestedId == null) {
            openSetup("请从主应用为方案创建桌面快捷方式");
            return;
        }
        profile = ProfileStore.getProfile(this, requestedId);
        if (profile == null) {
            openSetup("这个闹钟方案已删除，原桌面快捷方式不能再执行");
            return;
        }
        if (!AccessibilityUtils.isEnabled(this)) {
            openSetup("无法执行“" + profile.getName() + "”：请先启用无障碍服务");
            return;
        }
        connectAndCreate();
    }

    private void connectAndCreate() {
        AlarmAccessibilityService service =
                AlarmAccessibilityService.getConnectedInstance();
        if (service != null) {
            String error = service.createAlarms(profile);
            if (error != null) {
                openSetup(error);
            } else {
                finishAndRemoveTask();
            }
            return;
        }
        retryCount++;
        if (retryCount <= MAX_CONNECT_RETRIES) {
            handler.postDelayed(this::connectAndCreate, 100L);
            return;
        }
        openSetup("无障碍服务没有连接，请关闭后重新启用一次");
    }

    private void openSetup(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
