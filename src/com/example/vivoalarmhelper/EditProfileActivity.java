package com.example.vivoalarmhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class EditProfileActivity extends Activity {
    public static final String EXTRA_PROFILE_ID = "profile_id";
    private static final int REQUEST_EDIT_ALARM = 40;

    private final ArrayList<AlarmConfig> alarms = new ArrayList<>();
    private String editingId;
    private EditText nameInput;
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
        if (profile != null) alarms.addAll(profile.getAlarms());
        buildScreen(profile);
    }

    private void buildScreen(AlarmProfile profile) {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Ui.COLOR_BACKGROUND);
        page.addView(Ui.toolbar(this,
                profile == null ? "新建方案" : "编辑方案",
                "取消", view -> finish(), "保存", view -> saveProfile()));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 36));

        LinearLayout nameCard = Ui.card(this);
        TextView nameLabel = Ui.text(this, "方案名称", 14, Ui.COLOR_SUBTEXT);
        nameLabel.setPadding(Ui.dp(this, 4), Ui.dp(this, 14), 0, 0);
        nameCard.addView(nameLabel);
        nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setText(profile == null ? "" : profile.getName());
        nameInput.setHint("例如：夜班后三闹钟");
        nameInput.setTextSize(18);
        nameInput.setBackgroundColor(Color.TRANSPARENT);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        nameInput.setContentDescription("方案名称");
        nameCard.addView(nameInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 58)));
        root.addView(nameCard, Ui.pageCardParams(this));

        TextView section = Ui.heading(this, "方案中的闹钟", 18);
        section.setPadding(Ui.dp(this, 22), 0, 0, Ui.dp(this, 10));
        root.addView(section);
        TextView hint = Ui.text(this,
                "每个闹钟独立设置。▲/▼ 只调整执行顺序；所有相对闹钟仍共用一次点击时捕获的同一个 t0。",
                13, Ui.COLOR_SUBTEXT);
        hint.setPadding(Ui.dp(this, 22), 0, Ui.dp(this, 22), Ui.dp(this, 12));
        root.addView(hint);

        alarmContainer = new LinearLayout(this);
        alarmContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(alarmContainer);
        renderAlarms();

        Button add = Ui.button(this, "＋ 添加一个闹钟");
        add.setTextColor(Ui.COLOR_PRIMARY);
        add.setOnClickListener(view -> addAlarm());
        root.addView(add, Ui.pageCardParams(this));

        TextView saveTip = Ui.text(this,
                "保存只会保存方案，不会创建任何真实闹钟。保存后请在方案列表中选择“添加到桌面”。",
                13, Ui.COLOR_SUBTEXT);
        saveTip.setPadding(Ui.dp(this, 22), 0, Ui.dp(this, 22), 0);
        root.addView(saveTip);

        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(page);
    }

    private void renderAlarms() {
        alarmContainer.removeAllViews();
        if (alarms.isEmpty()) {
            TextView empty = Ui.text(this,
                    "这套方案还没有闹钟。至少添加一个后才能保存。",
                    14, Ui.COLOR_SUBTEXT);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(Ui.dp(this, 20), Ui.dp(this, 22),
                    Ui.dp(this, 20), Ui.dp(this, 28));
            alarmContainer.addView(empty, Ui.pageCardParams(this));
            return;
        }
        for (int index = 0; index < alarms.size(); index++) {
            alarmContainer.addView(alarmCard(index), Ui.pageCardParams(this));
        }
    }

    private View alarmCard(int index) {
        AlarmConfig alarm = alarms.get(index);
        LinearLayout card = Ui.card(this);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(this, 4), Ui.dp(this, 14),
                Ui.dp(this, 4), Ui.dp(this, 5));
        TextView number = Ui.heading(this, "闹钟 " + (index + 1), 18);
        top.addView(number, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView mode = Ui.text(this,
                alarm.timeMode == AlarmConfig.TIME_RELATIVE ? "相对 t0" : "绝对时间",
                12, Ui.COLOR_PRIMARY);
        mode.setPadding(Ui.dp(this, 10), Ui.dp(this, 5),
                Ui.dp(this, 10), Ui.dp(this, 5));
        mode.setBackground(Ui.roundedBackground(this,
                Color.rgb(255, 239, 238), Color.TRANSPARENT, 20));
        top.addView(mode);
        card.addView(top);

        TextView main = Ui.heading(this, alarmTimeSummary(alarm), 24);
        main.setPadding(Ui.dp(this, 4), Ui.dp(this, 2),
                Ui.dp(this, 4), Ui.dp(this, 5));
        card.addView(main);
        TextView details = Ui.text(this,
                repeatSummary(alarm) + " · " + alarm.ringtoneName
                        + (alarm.deleteAfterRing ? " · 关闭后删除" : ""),
                13, Ui.COLOR_SUBTEXT);
        details.setPadding(Ui.dp(this, 4), 0,
                Ui.dp(this, 4), Ui.dp(this, 12));
        card.addView(details);
        card.addView(Ui.divider(this));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 7));
        Button edit = Ui.textButton(this, "编辑");
        edit.setOnClickListener(view -> editAlarm(index));
        actions.addView(edit, smallAction(1f));
        Button up = Ui.textButton(this, "▲");
        up.setEnabled(index > 0);
        up.setOnClickListener(view -> moveAlarm(index, index - 1));
        actions.addView(up, smallAction(0.55f));
        Button down = Ui.textButton(this, "▼");
        down.setEnabled(index < alarms.size() - 1);
        down.setOnClickListener(view -> moveAlarm(index, index + 1));
        actions.addView(down, smallAction(0.55f));
        Button delete = Ui.textButton(this, "删除");
        delete.setTextColor(Color.rgb(194, 55, 51));
        delete.setOnClickListener(view -> deleteAlarm(index));
        actions.addView(delete, smallAction(0.8f));
        card.addView(actions);
        card.setOnClickListener(view -> editAlarm(index));
        return card;
    }

    private LinearLayout.LayoutParams smallAction(float weight) {
        return new LinearLayout.LayoutParams(0, Ui.dp(this, 48), weight);
    }

    private String alarmTimeSummary(AlarmConfig alarm) {
        if (alarm.repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY) {
            return alarm.shiftDaysCount + " 天轮班周期";
        }
        if (alarm.timeMode == AlarmConfig.TIME_RELATIVE) {
            return "+" + alarm.offsetMinutes + " 分钟";
        }
        return String.format(Locale.CHINA, "%02d:%02d", alarm.hour, alarm.minute);
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
        launchAlarmEditor(AlarmConfig.defaultRelative(offset));
    }

    private void editAlarm(int index) {
        pendingAlarmIndex = index;
        launchAlarmEditor(alarms.get(index));
    }

    private void launchAlarmEditor(AlarmConfig alarm) {
        Intent intent = new Intent(this, EditAlarmActivity.class);
        intent.putExtra(EditAlarmActivity.EXTRA_ALARM_JSON, alarm.toJsonString());
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
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            nameInput.setError("请填写方案名称");
            nameInput.requestFocus();
            return;
        }
        if (name.codePointCount(0, name.length()) > 30) {
            nameInput.setError("名称最多 30 个字符");
            nameInput.requestFocus();
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
        Toast.makeText(this, "方案已保存（未创建闹钟）", Toast.LENGTH_SHORT).show();
        finish();
    }
}
