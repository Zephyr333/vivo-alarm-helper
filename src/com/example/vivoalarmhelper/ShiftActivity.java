package com.example.vivoalarmhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShiftActivity extends Activity {
    private AlarmConfig alarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            alarm = AlarmConfig.fromJsonString(
                    getIntent().getStringExtra(EditAlarmActivity.EXTRA_ALARM_JSON));
        } catch (JSONException | NullPointerException exception) {
            finish();
            return;
        }
        render();
    }

    private void render() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Ui.COLOR_BACKGROUND);
        page.addView(Ui.toolbar(this, "轮班制工作日", "取消", view -> finish(),
                "完成", view -> finishWithResult()));

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 36));

        LinearLayout cycle = Ui.card(this);
        cycle.addView(Ui.row(this, "调整周期",
                alarm.shiftDaysCount + " 天（范围 2～62 天）", true,
                view -> editCycleLength()));
        cycle.addView(Ui.divider(this));
        cycle.addView(Ui.row(this, "今天",
                "周期第 " + alarm.shiftToday + " 天", true,
                view -> editToday()));
        cycle.addView(Ui.divider(this));
        cycle.addView(Ui.switchRow(this, "智能跳过节假日",
                "春节、国庆等节假日不提醒（不包括普通周末）。",
                alarm.excludeHolidays, (button, checked) ->
                        alarm = alarm.buildUpon().excludeHolidays(checked).build()));
        root.addView(cycle, Ui.pageCardParams(this));

        TextView title = Ui.heading(this, "周期日提醒", 18);
        title.setPadding(Ui.dp(this, 22), 0, 0, Ui.dp(this, 8));
        root.addView(title);
        TextView hint = Ui.text(this,
                "每一天可独立设置时分和启用状态，至少启用一天。",
                13, Ui.COLOR_SUBTEXT);
        hint.setPadding(Ui.dp(this, 22), 0, Ui.dp(this, 22), Ui.dp(this, 12));
        root.addView(hint);

        LinearLayout days = Ui.card(this);
        for (int index = 0; index < alarm.shiftDays.size(); index++) {
            final int position = index;
            AlarmConfig.ShiftDay day = alarm.shiftDays.get(index);
            String time = String.format(Locale.CHINA, "%02d:%02d",
                    day.hour, day.minute);
            days.addView(Ui.switchRow(this, "第 " + (index + 1) + " 天",
                    time + " · 点击行可修改时间", day.enabled,
                    (button, checked) -> updateEnabled(position, checked)));
            android.view.View row = days.getChildAt(days.getChildCount() - 1);
            row.setOnClickListener(view -> editDayTime(position));
            if (index < alarm.shiftDays.size() - 1) days.addView(Ui.divider(this));
        }
        root.addView(days, Ui.pageCardParams(this));

        TextView evidence = Ui.text(this,
                "保存编码：shift_today、shift_days_count、shift_days_time、shift_days_enabled。",
                12, Ui.COLOR_SUBTEXT);
        evidence.setPadding(Ui.dp(this, 22), 0, Ui.dp(this, 22), 0);
        root.addView(evidence);

        scroll.addView(root);
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(page);
    }

    private void editCycleLength() {
        editInteger("调整周期", "周期长度（2～62 天）",
                alarm.shiftDaysCount, 2, 62, value -> {
                    alarm = alarm.buildUpon().shiftDaysCount(value).build();
                    render();
                });
    }

    private void editToday() {
        editInteger("今天", "今天是周期第几天（1～"
                        + alarm.shiftDaysCount + "）",
                alarm.shiftToday, 1, alarm.shiftDaysCount, value -> {
                    alarm = alarm.buildUpon().shiftToday(value).build();
                    render();
                });
    }

    private void editInteger(String title, String message, int current,
            int min, int max, IntCallback callback) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current));
        input.setSelectAllOnFocus(true);
        input.setPadding(Ui.dp(this, 24), 0, Ui.dp(this, 24), 0);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title).setMessage(message).setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    try {
                        int value = Integer.parseInt(input.getText().toString());
                        if (value < min || value > max) throw new NumberFormatException();
                        callback.accept(value);
                        dialog.dismiss();
                    } catch (NumberFormatException exception) {
                        input.setError("请输入 " + min + "～" + max);
                    }
                }));
        dialog.show();
    }

    private void editDayTime(int position) {
        AlarmConfig.ShiftDay day = alarm.shiftDays.get(position);
        new TimePickerDialog(this, (view, hour, minute) -> {
            List<AlarmConfig.ShiftDay> values = new ArrayList<>(alarm.shiftDays);
            values.set(position, new AlarmConfig.ShiftDay(
                    hour, minute, values.get(position).enabled));
            alarm = alarm.buildUpon().shiftDays(values).build();
            render();
        }, day.hour, day.minute, true).show();
    }

    private void updateEnabled(int position, boolean checked) {
        if (!checked) {
            int enabledCount = 0;
            for (AlarmConfig.ShiftDay day : alarm.shiftDays) {
                if (day.enabled) enabledCount++;
            }
            if (enabledCount <= 1 && alarm.shiftDays.get(position).enabled) {
                Toast.makeText(this, "轮班周期至少启用一天",
                        Toast.LENGTH_SHORT).show();
                render();
                return;
            }
        }
        List<AlarmConfig.ShiftDay> values = new ArrayList<>(alarm.shiftDays);
        AlarmConfig.ShiftDay current = values.get(position);
        values.set(position, new AlarmConfig.ShiftDay(
                current.hour, current.minute, checked));
        alarm = alarm.buildUpon().shiftDays(values).build();
    }

    private void finishWithResult() {
        boolean anyEnabled = false;
        for (AlarmConfig.ShiftDay day : alarm.shiftDays) anyEnabled |= day.enabled;
        if (!anyEnabled) {
            Toast.makeText(this, "至少启用一个周期日", Toast.LENGTH_LONG).show();
            return;
        }
        Intent result = new Intent();
        result.putExtra(EditAlarmActivity.EXTRA_ALARM_JSON, alarm.toJsonString());
        setResult(RESULT_OK, result);
        finish();
    }

    private interface IntCallback { void accept(int value); }
}
