package com.example.vivoalarmhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class MainActivity extends Activity {
    private LinearLayout content;
    private boolean connectionRefreshAttempted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ProfileStore.ensureInitialized(this);
        buildScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectionRefreshAttempted = false;
        populateContent();
    }

    private void buildScreen() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Ui.COLOR_BACKGROUND);
        page.addView(Ui.toolbar(this, "闹钟方案", "", null,
                "新建", view -> editProfile(null)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 36));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(page);
    }

    private void populateContent() {
        if (content == null) return;
        content.removeAllViews();
        content.addView(createStatusCard(), Ui.pageCardParams(this));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPadding(Ui.dp(this, 22), 0, Ui.dp(this, 18), Ui.dp(this, 10));
        TextView savedTitle = Ui.heading(this, "已保存方案", 18);
        heading.addView(savedTitle, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        content.addView(heading);

        List<AlarmProfile> profiles = ProfileStore.getProfiles(this);
        if (profiles.isEmpty()) {
            content.addView(emptyState(), Ui.pageCardParams(this));
            return;
        }
        for (AlarmProfile profile : profiles) {
            content.addView(createProfileCard(profile), Ui.pageCardParams(this));
        }
    }

    private View createStatusCard() {
        boolean clockFound = isPackageInstalled("com.android.BBKClock");
        boolean enabled = AccessibilityUtils.isEnabled(this);
        boolean connected = AlarmAccessibilityService.getConnectedInstance() != null;
        if (enabled && !connected && !connectionRefreshAttempted) {
            connectionRefreshAttempted = true;
            content.postDelayed(() -> {
                if (!isFinishing()) populateContent();
            }, 700L);
        }

        LinearLayout card = Ui.card(this);
        String state = !clockFound ? "未找到 vivo 系统时钟"
                : !enabled ? "无障碍服务未启用"
                : connected ? "已准备好" : "无障碍服务正在连接";
        int stateColor = clockFound && enabled && connected
                ? Ui.COLOR_SUCCESS : Ui.COLOR_WARNING;
        TextView stateView = Ui.heading(this, state, 17);
        stateView.setTextColor(stateColor);
        stateView.setPadding(Ui.dp(this, 4), Ui.dp(this, 16),
                Ui.dp(this, 4), Ui.dp(this, 4));
        card.addView(stateView);
        TextView detail = Ui.text(this,
                "主应用只管理方案；真正创建闹钟由你添加到桌面的方案快捷方式执行。",
                13, Ui.COLOR_SUBTEXT);
        detail.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), Ui.dp(this, 8));
        card.addView(detail);
        Button settings = Ui.textButton(this,
                enabled ? "检查无障碍设置" : "启用无障碍服务");
        settings.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        settings.setOnClickListener(view -> startActivity(
                new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        card.addView(settings, Ui.matchWrap(Ui.dp(this, 5)));
        return card;
    }

    private View emptyState() {
        LinearLayout card = Ui.card(this);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = Ui.heading(this, "还没有闹钟方案", 18);
        title.setPadding(0, Ui.dp(this, 28), 0, Ui.dp(this, 8));
        card.addView(title);
        TextView detail = Ui.text(this,
                "先新建一套方案，再把它添加到桌面。打开或保存主应用都不会创建真实闹钟。",
                14, Ui.COLOR_SUBTEXT);
        detail.setGravity(Gravity.CENTER);
        card.addView(detail);
        Button add = Ui.button(this, "新建方案");
        add.setTextColor(Color.WHITE);
        add.setBackground(Ui.roundedBackground(this,
                Ui.COLOR_PRIMARY, Color.TRANSPARENT, 16));
        add.setOnClickListener(view -> editProfile(null));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 52));
        params.setMargins(0, Ui.dp(this, 20), 0, Ui.dp(this, 22));
        card.addView(add, params);
        return card;
    }

    private View createProfileCard(AlarmProfile profile) {
        LinearLayout card = Ui.card(this);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(this, 4), Ui.dp(this, 16),
                Ui.dp(this, 4), Ui.dp(this, 8));
        TextView name = Ui.heading(this, profile.getName(), 19);
        top.addView(name, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView count = Ui.text(this,
                profile.getAlarms().size() + " 个闹钟", 13, Ui.COLOR_SUBTEXT);
        top.addView(count);
        card.addView(top);

        TextView summary = Ui.text(this, profileSummary(profile),
                14, Ui.COLOR_SUBTEXT);
        summary.setPadding(Ui.dp(this, 4), 0,
                Ui.dp(this, 4), Ui.dp(this, 14));
        card.addView(summary);
        card.addView(Ui.divider(this));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        Button edit = Ui.textButton(this, "编辑");
        edit.setOnClickListener(view -> editProfile(profile));
        actions.addView(edit, actionParams());
        Button pin = Ui.textButton(this, "添加到桌面");
        pin.setOnClickListener(view -> ShortcutHelper.pinProfile(this, profile));
        actions.addView(pin, actionParams());
        Button delete = Ui.textButton(this, "删除");
        delete.setTextColor(Color.rgb(194, 55, 51));
        delete.setOnClickListener(view -> confirmDelete(profile));
        actions.addView(delete, actionParams());
        card.addView(actions);
        return card;
    }

    private LinearLayout.LayoutParams actionParams() {
        return new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f);
    }

    private void editProfile(AlarmProfile profile) {
        Intent intent = new Intent(this, EditProfileActivity.class);
        if (profile != null) {
            intent.putExtra(EditProfileActivity.EXTRA_PROFILE_ID, profile.getId());
        }
        startActivity(intent);
    }

    private void confirmDelete(AlarmProfile profile) {
        new AlertDialog.Builder(this)
                .setTitle("删除方案？")
                .setMessage("将删除“" + profile.getName()
                        + "”。它已固定到桌面的快捷方式会被停用。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    if (ProfileStore.deleteProfile(this, profile.getId())) {
                        ShortcutHelper.disableProfileShortcut(this, profile.getId());
                        Toast.makeText(this, "方案已删除", Toast.LENGTH_SHORT).show();
                        populateContent();
                    } else {
                        Toast.makeText(this, "删除失败，请重试",
                                Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private String profileSummary(AlarmProfile profile) {
        StringBuilder value = new StringBuilder();
        for (int index = 0; index < profile.getAlarms().size(); index++) {
            if (index > 0) value.append(" · ");
            AlarmConfig alarm = profile.getAlarms().get(index);
            if (alarm.timeMode == AlarmConfig.TIME_RELATIVE) {
                value.append('+').append(alarm.offsetMinutes).append(" 分钟");
            } else {
                value.append(String.format(java.util.Locale.CHINA,
                        "%02d:%02d", alarm.hour, alarm.minute));
            }
        }
        return value.toString();
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException exception) {
            return false;
        }
    }
}
