package com.example.vivoalarmhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.UUID;

public final class EditProfileActivity extends Activity {
    public static final String EXTRA_PROFILE_ID = "profile_id";
    private static final int REQUEST_EDIT_ALARM = 40;

    private final ArrayList<AlarmConfig> alarms = new ArrayList<>();
    private String editingId;
    private String profileName = "";
    private String initialSignature;
    private TextView nameValue;
    private LinearLayout alarmContainer;
    private int pendingAlarmIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editingId = getIntent().getStringExtra(EXTRA_PROFILE_ID);
        AlarmProfile profile = editingId == null
                ? null : ProfileStore.getProfile(this, editingId);
        if (editingId != null && profile == null) {
            Toast.makeText(this, "该方案已不存在", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (profile != null) {
            profileName = profile.getName();
            alarms.addAll(profile.getAlarms());
        }
        initialSignature = draftSignature();
        buildScreen(profile == null);
    }

    private void buildScreen(boolean creating) {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Ui.COLOR_BACKGROUND);
        page.addView(Ui.toolbar(this,
                creating ? "新建方案" : "编辑方案",
                creating ? "取消" : "‹", view -> handleBack(),
                "保存", view -> saveProfile()));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 38));

        LinearLayout nameCard = Ui.card(this);
        LinearLayout nameRow = Ui.navigationRow(this, "方案名称",
                profileName.isEmpty() ? "未命名" : profileName,
                true, view -> editProfileName());
        nameValue = (TextView) ((LinearLayout) nameRow).getChildAt(1);
        nameCard.addView(nameRow);
        root.addView(nameCard, Ui.pageCardParams(this));

        TextView section = Ui.heading(this, "方案中的闹钟", 20);
        section.setPadding(Ui.dp(this, 24), Ui.dp(this, 2),
                Ui.dp(this, 20), Ui.dp(this, 8));
        root.addView(section);
        TextView hint = Ui.text(this,
                "点击桌面方案时，所有“从现在起”的闹钟都会以同一个时刻开始计算。按住 ≡ 可以调整顺序。",
                13, Ui.COLOR_SUBTEXT);
        hint.setPadding(Ui.dp(this, 24), 0, Ui.dp(this, 24), Ui.dp(this, 14));
        root.addView(hint);

        alarmContainer = new LinearLayout(this);
        alarmContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(alarmContainer);
        renderAlarms();

        LinearLayout addCard = Ui.card(this);
        TextView addIcon = Ui.text(this, "+", 28, Ui.COLOR_PRIMARY);
        addIcon.setGravity(Gravity.CENTER);
        LinearLayout addRow = Ui.row(this, "添加一个闹钟",
                "每个闹钟都可以单独设置", false, view -> addAlarm());
        addRow.addView(addIcon, 0, new LinearLayout.LayoutParams(
                Ui.dp(this, 44), Ui.dp(this, 52)));
        addCard.addView(addRow);
        root.addView(addCard, Ui.pageCardParams(this));

        TextView saveTip = Ui.text(this,
                "保存只更新这套方案，不会创建真实闹钟。",
                13, Ui.COLOR_SUBTEXT);
        saveTip.setGravity(Gravity.CENTER);
        saveTip.setPadding(Ui.dp(this, 24), 0, Ui.dp(this, 24), 0);
        root.addView(saveTip);

        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.setContentView(this, page);
    }

    private void editProfileName() {
        VivoDialogs.showTextInput(this, "方案名称", profileName,
                "例如：夜班后三闹钟", 30, value -> {
                    profileName = value;
                    if (nameValue != null) {
                        nameValue.setText(value.isEmpty() ? "未命名" : value);
                    }
                });
    }

    private void renderAlarms() {
        alarmContainer.removeAllViews();
        if (alarms.isEmpty()) {
            LinearLayout emptyCard = Ui.card(this);
            TextView empty = Ui.text(this,
                    "这套方案还没有闹钟",
                    15, Ui.COLOR_SUBTEXT);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(Ui.dp(this, 20), Ui.dp(this, 28),
                    Ui.dp(this, 20), Ui.dp(this, 28));
            emptyCard.addView(empty);
            alarmContainer.addView(emptyCard, Ui.pageCardParams(this));
            return;
        }
        for (int index = 0; index < alarms.size(); index++) {
            alarmContainer.addView(alarmCard(index), Ui.pageCardParams(this));
        }
    }

    private View alarmCard(int index) {
        AlarmConfig alarm = alarms.get(index);
        LinearLayout card = Ui.card(this);
        card.setTag(alarm.id);
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("编辑闹钟 " + (index + 1) + "，"
                + alarmTimeSummary(alarm));
        card.setOnClickListener(view -> editAlarm(index));
        card.setOnDragListener((view, event) -> handleDrop(index, view, event));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(this, 4), Ui.dp(this, 16),
                Ui.dp(this, 2), Ui.dp(this, 4));
        TextView number = Ui.heading(this, "闹钟 " + (index + 1), 17);
        top.addView(number, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView mode = Ui.text(this,
                alarm.timeMode == AlarmConfig.TIME_RELATIVE
                        ? "从现在起" : "指定时间",
                13, Ui.COLOR_PRIMARY);
        mode.setPadding(Ui.dp(this, 11), Ui.dp(this, 5),
                Ui.dp(this, 11), Ui.dp(this, 5));
        mode.setBackground(Ui.roundedBackground(this,
                Ui.COLOR_PRIMARY_SOFT, android.graphics.Color.TRANSPARENT, 18));
        top.addView(mode);
        card.addView(top);

        TextView main = Ui.heading(this, alarmTimeSummary(alarm), 27);
        main.setPadding(Ui.dp(this, 4), Ui.dp(this, 3),
                Ui.dp(this, 4), Ui.dp(this, 6));
        card.addView(main);
        TextView details = Ui.text(this,
                repeatSummary(alarm) + " · " + alarm.ringtoneName
                        + (alarm.deleteAfterRing ? " · 关闭后删除" : ""),
                13, Ui.COLOR_SUBTEXT);
        details.setPadding(Ui.dp(this, 4), 0,
                Ui.dp(this, 4), Ui.dp(this, 13));
        card.addView(details);
        card.addView(Ui.divider(this));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 6));
        TextView remove = Ui.text(this, "−", 25, android.graphics.Color.WHITE);
        remove.setGravity(Gravity.CENTER);
        remove.setContentDescription("删除闹钟 " + (index + 1));
        remove.setClickable(true);
        remove.setFocusable(true);
        remove.setBackground(Ui.roundedBackground(this,
                Ui.COLOR_PRIMARY, android.graphics.Color.TRANSPARENT, 18));
        remove.setOnClickListener(view -> deleteAlarm(index));
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                Ui.dp(this, 36), Ui.dp(this, 36));
        removeParams.setMarginStart(Ui.dp(this, 4));
        actions.addView(remove, removeParams);

        TextView editHint = Ui.text(this, "点击卡片编辑", 13, Ui.COLOR_SUBTEXT);
        editHint.setGravity(Gravity.CENTER);
        actions.addView(editHint, new LinearLayout.LayoutParams(
                0, Ui.dp(this, 48), 1f));

        TextView handle = Ui.text(this, "≡", 30, Ui.COLOR_TEXT);
        handle.setGravity(Gravity.CENTER);
        handle.setContentDescription("按住拖动闹钟 " + (index + 1));
        handle.setClickable(true);
        handle.setFocusable(true);
        handle.setOnLongClickListener(view -> {
            ClipData data = ClipData.newPlainText("alarm_id", alarm.id);
            return view.startDragAndDrop(data,
                    new View.DragShadowBuilder(card), null, 0);
        });
        actions.addView(handle, new LinearLayout.LayoutParams(
                Ui.dp(this, 56), Ui.dp(this, 48)));
        card.addView(actions);
        return card;
    }

    private boolean handleDrop(int targetIndex, View view, DragEvent event) {
        if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
            view.setAlpha(0.72f);
        } else if (event.getAction() == DragEvent.ACTION_DRAG_EXITED
                || event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
            view.setAlpha(1f);
        } else if (event.getAction() == DragEvent.ACTION_DROP) {
            view.setAlpha(1f);
            ClipData data = event.getClipData();
            if (data == null || data.getItemCount() == 0) return false;
            String id = data.getItemAt(0).coerceToText(this).toString();
            int from = -1;
            for (int index = 0; index < alarms.size(); index++) {
                if (alarms.get(index).id.equals(id)) {
                    from = index;
                    break;
                }
            }
            if (from >= 0 && from != targetIndex) moveAlarm(from, targetIndex);
            return true;
        }
        return true;
    }

    private String alarmTimeSummary(AlarmConfig alarm) {
        if (alarm.repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY) {
            return alarm.shiftDaysCount + " 天轮班周期";
        }
        if (alarm.timeMode == AlarmConfig.TIME_RELATIVE) {
            return TimeText.durationAfter(alarm.offsetMinutes);
        }
        return TimeText.clock(alarm.hour, alarm.minute);
    }

    private String repeatSummary(AlarmConfig alarm) {
        String[] names = {"仅一次", "每天", "法定工作日", "节假日及周末",
                "单双休工作日", "轮班制工作日", "自定义重复"};
        return names[Math.max(0, Math.min(names.length - 1, alarm.repeatType))];
    }

    private void addAlarm() {
        int offset = alarms.isEmpty() ? 60
                : Math.min(525600, alarms.get(alarms.size() - 1).offsetMinutes + 15);
        pendingAlarmIndex = -1;
        launchAlarmEditor(AlarmConfig.defaultRelative(offset), true);
    }

    private void editAlarm(int index) {
        pendingAlarmIndex = index;
        launchAlarmEditor(alarms.get(index), false);
    }

    private void launchAlarmEditor(AlarmConfig alarm, boolean creating) {
        Intent intent = new Intent(this, EditAlarmActivity.class);
        intent.putExtra(EditAlarmActivity.EXTRA_ALARM_JSON, alarm.toJsonString());
        intent.putExtra(EditAlarmActivity.EXTRA_IS_NEW, creating);
        startActivityForResult(intent, REQUEST_EDIT_ALARM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_EDIT_ALARM || resultCode != RESULT_OK
                || data == null) return;
        try {
            AlarmConfig alarm = AlarmConfig.fromJsonString(
                    data.getStringExtra(EditAlarmActivity.EXTRA_ALARM_JSON));
            if (pendingAlarmIndex >= 0 && pendingAlarmIndex < alarms.size()) {
                alarms.set(pendingAlarmIndex, alarm);
            } else {
                alarms.add(alarm);
            }
            renderAlarms();
        } catch (JSONException | NullPointerException exception) {
            Toast.makeText(this, "闹钟配置读取失败，请重试",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void moveAlarm(int from, int to) {
        if (from < 0 || from >= alarms.size() || to < 0 || to >= alarms.size()) return;
        AlarmConfig item = alarms.remove(from);
        alarms.add(to, item);
        renderAlarms();
    }

    private void deleteAlarm(int index) {
        new AlertDialog.Builder(this)
                .setTitle("删除这个闹钟？")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    alarms.remove(index);
                    renderAlarms();
                }).show();
    }

    private void saveProfile() {
        String name = profileName.trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请先填写方案名称", Toast.LENGTH_LONG).show();
            editProfileName();
            return;
        }
        if (alarms.isEmpty()) {
            Toast.makeText(this, "一套方案至少需要一个闹钟",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String id = editingId == null ? UUID.randomUUID().toString() : editingId;
        AlarmProfile profile = new AlarmProfile(id, name, new ArrayList<>(alarms));
        if (!ProfileStore.saveProfile(this, profile)) {
            Toast.makeText(this, "方案保存失败，请重试", Toast.LENGTH_LONG).show();
            return;
        }
        ShortcutHelper.updatePinnedProfile(this, profile);
        initialSignature = draftSignature();
        Toast.makeText(this, "方案已保存，未创建闹钟", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }

    private void handleBack() {
        if (draftSignature().equals(initialSignature)) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("放弃未保存的修改？")
                .setMessage("返回后，这次对方案的修改不会保留。")
                .setNegativeButton("继续编辑", null)
                .setPositiveButton("放弃修改", (dialog, which) -> finish())
                .show();
    }

    private String draftSignature() {
        StringBuilder value = new StringBuilder(profileName).append('|');
        for (AlarmConfig alarm : alarms) value.append(alarm.toJsonString()).append('|');
        return value.toString();
    }
}
