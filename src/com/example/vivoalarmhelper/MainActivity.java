package com.example.vivoalarmhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public final class MainActivity extends Activity {
    private LinearLayout content;
    private final AlarmAccessibilityService.ConnectionListener connectionListener = () ->
            runOnUiThread(() -> {
                if (!isFinishing()) populateContent();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ProfileStore.ensureInitialized(this);
        buildScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AlarmAccessibilityService.addConnectionListener(connectionListener);
        populateContent();
    }

    @Override
    protected void onPause() {
        AlarmAccessibilityService.removeConnectionListener(connectionListener);
        super.onPause();
    }

    private void buildScreen() {
        FrameLayout stage = new FrameLayout(this);
        stage.setBackgroundColor(Ui.COLOR_BACKGROUND);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Ui.COLOR_BACKGROUND);
        page.addView(Ui.toolbar(this, "闹钟方案", "", null, "", null));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 118));
        scroll.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        stage.addView(page, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        TextView add = Ui.fab(this, "新增方案");
        add.setOnClickListener(view -> editProfile(null));
        stage.addView(add, Ui.fabParams(this));
        Ui.setContentView(this, stage);
    }

    private void populateContent() {
        if (content == null) return;
        content.removeAllViews();
        content.addView(createStatusCard(), Ui.pageCardParams(this));

        List<AlarmProfile> profiles = ProfileStore.getProfiles(this);
        TextView savedTitle = Ui.heading(this, "我的方案", 20);
        savedTitle.setPadding(Ui.dp(this, 24), Ui.dp(this, 2),
                Ui.dp(this, 20), Ui.dp(this, profiles.size() > 1 ? 4 : 12));
        content.addView(savedTitle);
        if (profiles.size() > 1) {
            TextView orderHint = Ui.text(this,
                    "按住方案卡片右上角的 ≡，拖到另一套方案的位置即可排序。",
                    13, Ui.COLOR_SUBTEXT);
            orderHint.setPadding(Ui.dp(this, 24), 0,
                    Ui.dp(this, 24), Ui.dp(this, 12));
            content.addView(orderHint);
        }

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
        AccessibilityStatus status = AccessibilityUtils.getStatus(this);
        boolean connected = status == AccessibilityStatus.CONNECTED;

        LinearLayout card = Ui.card(this);
        String state = !clockFound ? "未找到 vivo 系统时钟"
                : status == AccessibilityStatus.DISABLED ? "无障碍服务未开启"
                : "无障碍服务已开启";
        String detail = !clockFound ? "请确认手机已安装 vivo 系统时钟。"
                : status == AccessibilityStatus.DISABLED
                        ? "执行方案需要此服务代你保存 vivo 闹钟。"
                : connected ? "可以在应用内执行，也可以使用桌面快捷方式。"
                : "执行方案时会自动恢复服务并继续。";
        int stateColor = clockFound && status != AccessibilityStatus.DISABLED
                ? Ui.COLOR_SUCCESS : Ui.COLOR_WARNING;
        LinearLayout row = Ui.row(this, state, detail, true, view ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        TextView dot = Ui.text(this, "●", 17, stateColor);
        dot.setGravity(Gravity.CENTER);
        dot.setContentDescription(clockFound
                && status != AccessibilityStatus.DISABLED
                ? "状态正常" : "需要处理");
        row.addView(dot, 0, new LinearLayout.LayoutParams(
                Ui.dp(this, 34), Ui.dp(this, 48)));
        card.addView(row);
        return card;
    }

    private View emptyState() {
        LinearLayout card = Ui.card(this);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView clock = Ui.text(this, "◷", 52, Ui.COLOR_DISABLED);
        clock.setGravity(Gravity.CENTER);
        clock.setPadding(0, Ui.dp(this, 28), 0, Ui.dp(this, 8));
        card.addView(clock);
        TextView title = Ui.heading(this, "还没有闹钟方案", 18);
        card.addView(title);
        TextView detail = Ui.text(this,
                "点击下方红色加号新建方案。",
                14, Ui.COLOR_SUBTEXT);
        detail.setGravity(Gravity.CENTER);
        detail.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 28));
        card.addView(detail);
        return card;
    }

    private View createProfileCard(AlarmProfile profile) {
        LinearLayout card = Ui.card(this);
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("编辑方案 " + profile.getName());
        card.setOnClickListener(view -> editProfile(profile));
        card.setOnDragListener((view, event) ->
                handleProfileDrop(profile.getId(), view, event));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(this, 4), Ui.dp(this, 18),
                Ui.dp(this, 2), Ui.dp(this, 7));
        TextView name = Ui.heading(this, profile.getName(), 20);
        top.addView(name, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView count = Ui.text(this,
                profile.getAlarms().size() + " 个闹钟"
                        + (profile.isSequence() ? " · 执行时选时间" : ""),
                13, Ui.COLOR_SUBTEXT);
        top.addView(count);
        TextView handle = Ui.text(this, "≡", 30, Ui.COLOR_TEXT);
        handle.setGravity(Gravity.CENTER);
        handle.setContentDescription("按住拖动方案 " + profile.getName());
        handle.setClickable(true);
        handle.setFocusable(true);
        handle.setOnClickListener(view -> Toast.makeText(this,
                "请按住并拖动到另一套方案的位置",
                Toast.LENGTH_SHORT).show());
        handle.setOnLongClickListener(view -> {
            ClipData data = ClipData.newPlainText(
                    "profile_id", profile.getId());
            return view.startDragAndDrop(data,
                    new View.DragShadowBuilder(card), null, 0);
        });
        top.addView(handle, new LinearLayout.LayoutParams(
                Ui.dp(this, 52), Ui.dp(this, 48)));
        card.addView(top);

        TextView summary = Ui.text(this, profileSummary(profile),
                14, Ui.COLOR_SUBTEXT);
        summary.setPadding(Ui.dp(this, 4), 0,
                Ui.dp(this, 4), Ui.dp(this, 15));
        card.addView(summary);
        card.addView(Ui.divider(this));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, Ui.dp(this, 7), 0, Ui.dp(this, 8));
        Button run = Ui.button(this, "执行");
        run.setTextColor(android.graphics.Color.WHITE);
        run.setContentDescription("执行方案 " + profile.getName());
        run.setBackground(Ui.roundedBackground(this,
                Ui.COLOR_PRIMARY, android.graphics.Color.TRANSPARENT, 18));
        run.setOnClickListener(view -> runProfile(profile));
        LinearLayout.LayoutParams runParams = new LinearLayout.LayoutParams(
                0, Ui.dp(this, 46), 1f);
        runParams.setMargins(Ui.dp(this, 2), Ui.dp(this, 2),
                Ui.dp(this, 4), Ui.dp(this, 2));
        actions.addView(run, runParams);
        Button pin = Ui.textButton(this, "添加到桌面");
        pin.setOnClickListener(view -> {
            view.getParent().requestDisallowInterceptTouchEvent(true);
            ShortcutHelper.pinProfile(this, profile);
        });
        actions.addView(pin, new LinearLayout.LayoutParams(
                0, Ui.dp(this, 50), 1.45f));
        Button delete = Ui.textButton(this, "删除");
        delete.setTextColor(Ui.COLOR_WARNING);
        delete.setOnClickListener(view -> confirmDelete(profile));
        actions.addView(delete, new LinearLayout.LayoutParams(
                0, Ui.dp(this, 50), 1f));
        card.addView(actions);
        return card;
    }

    private void editProfile(AlarmProfile profile) {
        Intent intent = new Intent(this, EditProfileActivity.class);
        if (profile != null) {
            intent.putExtra(EditProfileActivity.EXTRA_PROFILE_ID, profile.getId());
        }
        startActivity(intent);
    }

    private void runProfile(AlarmProfile profile) {
        startActivity(CreateAlarmsActivity.createRunIntent(
                this, profile.getId()));
    }

    private boolean handleProfileDrop(String targetId, View view,
            DragEvent event) {
        if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
            view.setAlpha(0.72f);
        } else if (event.getAction() == DragEvent.ACTION_DRAG_EXITED
                || event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
            view.setAlpha(1f);
        } else if (event.getAction() == DragEvent.ACTION_DROP) {
            view.setAlpha(1f);
            ClipData data = event.getClipData();
            if (data == null || data.getItemCount() == 0) return false;
            String sourceId = data.getItemAt(0)
                    .coerceToText(this).toString();
            if (sourceId.equals(targetId)) return true;
            if (ProfileStore.moveProfile(this, sourceId, targetId)) {
                populateContent();
            } else {
                Toast.makeText(this, "方案排序失败，请重试",
                        Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return true;
    }

    private void confirmDelete(AlarmProfile profile) {
        new AlertDialog.Builder(this)
                .setTitle("删除方案？")
                .setMessage("将删除“" + profile.getName()
                        + "”。它已添加到桌面的快捷方式会停止执行。")
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
        String sequenceProblem = ProfileTiming.sequenceProblem(profile);
        if (sequenceProblem != null) return "时间设置需要调整";
        if (profile.isSequence()) {
            StringBuilder sequence = new StringBuilder("执行时选择第一个时间");
            for (int index = 1; index < profile.getAlarms().size(); index++) {
                sequence.append(index == 1 ? " · 间隔 " : " · 再间隔 ")
                        .append(TimeText.duration(
                                profile.getAlarms().get(index).offsetMinutes));
            }
            return sequence.toString();
        }
        StringBuilder value = new StringBuilder();
        for (int index = 0; index < profile.getAlarms().size(); index++) {
            if (index > 0) value.append(" · ");
            AlarmConfig alarm = profile.getAlarms().get(index);
            if (alarm.timeMode == AlarmConfig.TIME_RELATIVE) {
                value.append(TimeText.durationAfter(
                        ProfileTiming.effectiveRelativeOffsetMinutes(
                                profile, index)));
            } else {
                value.append(TimeText.clock(alarm.hour, alarm.minute));
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
