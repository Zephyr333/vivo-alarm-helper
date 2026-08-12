package com.example.vivoalarmhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class EditAlarmActivity extends Activity {
    public static final String EXTRA_ALARM_JSON = "alarm_json";
    private static final int REQUEST_RINGTONE = 51;
    private static final int REQUEST_SHIFT = 52;
    private static final int REQUEST_VOICE = 53;

    private AlarmConfig alarm;
    private LinearLayout page;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            alarm = AlarmConfig.fromJsonString(
                    getIntent().getStringExtra(EXTRA_ALARM_JSON));
        } catch (JSONException | NullPointerException exception) {
            Toast.makeText(this, "闹钟配置已损坏", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        render();
    }

    private void render() {
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Ui.COLOR_BACKGROUND);
        page.addView(Ui.toolbar(this, "编辑闹钟", "取消", view -> finish(),
                "完成", view -> finishWithResult()));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 40));

        root.addView(modeCard(), Ui.pageCardParams(this));
        root.addView(timeHeader(), Ui.pageCardParams(this));

        LinearLayout timeCard = Ui.card(this);
        timeCard.addView(Ui.row(this, "重复", repeatSummary(), true,
                view -> chooseRepeat()));
        timeCard.addView(Ui.divider(this));
        timeCard.addView(Ui.row(this, "日期", dateSummary(),
                alarm.timeMode == AlarmConfig.TIME_ABSOLUTE
                        && alarm.repeatType == AlarmConfig.REPEAT_ONCE,
                view -> {
                    if (alarm.timeMode == AlarmConfig.TIME_ABSOLUTE
                            && alarm.repeatType == AlarmConfig.REPEAT_ONCE) {
                        chooseDate();
                    }
                }));
        root.addView(timeCard, Ui.pageCardParams(this));

        LinearLayout baseCard = Ui.card(this);
        baseCard.addView(Ui.row(this, "闹钟名称", labelSummary(), true,
                view -> editLabel()));
        baseCard.addView(Ui.divider(this));
        baseCard.addView(Ui.row(this, "铃声与振动", ringtoneSummary(), true,
                view -> editRingtone()));
        baseCard.addView(Ui.divider(this));
        LinearLayout deleteRow = Ui.switchRow(this,
                "提醒关闭后删除此闹钟",
                alarm.isRepeating()
                        ? "vivo 对重复闹钟会强制关闭此项"
                        : "闹钟响起并关闭后，此闹钟会被删除。",
                alarm.deleteAfterRing,
                (button, checked) -> {
                    if (alarm.isRepeating()) {
                        button.setChecked(false);
                        Toast.makeText(this,
                                "重复闹钟不能开启“关闭后删除”",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        alarm = alarm.buildUpon().deleteAfterRing(checked).build();
                    }
                });
        deleteRow.setAlpha(alarm.isRepeating() ? 0.58f : 1f);
        baseCard.addView(deleteRow);
        root.addView(baseCard, Ui.pageCardParams(this));

        LinearLayout snoozeCard = Ui.card(this);
        snoozeCard.addView(Ui.row(this, "稍后提醒", snoozeSummary(), true,
                view -> editSnooze()));
        if (alarm.snoozeEnabled) {
            snoozeCard.addView(Ui.divider(this));
            snoozeCard.addView(Ui.switchRow(this, "稍后提醒报时",
                    "点击稍后提醒时自动播报当前时间。",
                    alarm.snoozeTalker,
                    (button, checked) -> alarm = alarm.buildUpon()
                            .snoozeTalker(checked).build()));
        }
        root.addView(snoozeCard, Ui.pageCardParams(this));

        LinearLayout voiceCard = Ui.card(this);
        voiceCard.addView(Ui.row(this, "语音播报", voiceSummary(), true,
                view -> editVoice()));
        root.addView(voiceCard, Ui.pageCardParams(this));

        TextView note = Ui.text(this,
                "此页“完成”只把本闹钟放回方案编辑器，不会打开 vivo 时钟，也不会创建闹钟。",
                13, Ui.COLOR_SUBTEXT);
        note.setPadding(Ui.dp(this, 22), 0, Ui.dp(this, 22), 0);
        root.addView(note);

        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(page);
    }

    private View modeCard() {
        LinearLayout card = Ui.card(this);
        TextView title = Ui.text(this, "时间模式", 13, Ui.COLOR_SUBTEXT);
        title.setPadding(Ui.dp(this, 4), Ui.dp(this, 14), 0, Ui.dp(this, 8));
        card.addView(title);
        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.setPadding(0, 0, 0, Ui.dp(this, 14));
        Button relative = modeButton("相对 t0", alarm.timeMode == AlarmConfig.TIME_RELATIVE);
        relative.setOnClickListener(view -> {
            if (alarm.repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY) {
                Toast.makeText(this,
                        "轮班制的周期日有独立时分，不能使用相对 t0 模式",
                        Toast.LENGTH_LONG).show();
                return;
            }
            alarm = alarm.buildUpon().timeMode(AlarmConfig.TIME_RELATIVE).build();
            render();
        });
        modes.addView(relative, modeParams());
        Button absolute = modeButton("绝对时间", alarm.timeMode == AlarmConfig.TIME_ABSOLUTE);
        absolute.setOnClickListener(view -> {
            alarm = alarm.buildUpon().timeMode(AlarmConfig.TIME_ABSOLUTE).build();
            render();
        });
        modes.addView(absolute, modeParams());
        card.addView(modes);
        TextView help = Ui.text(this,
                alarm.timeMode == AlarmConfig.TIME_RELATIVE
                        ? "点击桌面快捷方式时捕获 t0，再加上偏移量；同一方案中的相对闹钟共用同一个 t0。"
                        : "使用保存的时分；仅一次时还使用指定日期，重复时由 vivo 计算下一次触发。",
                12, Ui.COLOR_SUBTEXT);
        help.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), Ui.dp(this, 14));
        card.addView(help);
        return card;
    }

    private Button modeButton(String label, boolean selected) {
        Button button = Ui.button(this, label);
        button.setTextColor(selected ? Color.WHITE : Ui.COLOR_TEXT);
        button.setBackground(Ui.roundedBackground(this,
                selected ? Ui.COLOR_PRIMARY : Color.rgb(241, 241, 244),
                Color.TRANSPARENT, 18));
        return button;
    }

    private LinearLayout.LayoutParams modeParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, Ui.dp(this, 50), 1f);
        params.setMarginEnd(Ui.dp(this, 6));
        return params;
    }

    private View timeHeader() {
        LinearLayout card = Ui.card(this);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        String main;
        String sub;
        if (alarm.repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY) {
            main = alarm.shiftDaysCount + " 天轮班周期";
            sub = "今天是周期第 " + alarm.shiftToday + " 天";
        } else if (alarm.timeMode == AlarmConfig.TIME_RELATIVE) {
            int hours = alarm.offsetMinutes / 60;
            int minutes = alarm.offsetMinutes % 60;
            main = "+" + alarm.offsetMinutes + " 分钟";
            sub = hours > 0 ? hours + " 小时 " + minutes + " 分钟" : "以点击瞬间为 t0";
        } else {
            main = String.format(Locale.CHINA, "%02d:%02d", alarm.hour, alarm.minute);
            sub = alarm.repeatType == AlarmConfig.REPEAT_ONCE
                    ? alarm.date : "按重复规则计算下一次";
        }
        TextView time = Ui.heading(this, main, 40);
        time.setGravity(Gravity.CENTER);
        time.setPadding(0, Ui.dp(this, 28), 0, Ui.dp(this, 4));
        card.addView(time);
        TextView detail = Ui.text(this, sub, 14, Ui.COLOR_SUBTEXT);
        detail.setGravity(Gravity.CENTER);
        card.addView(detail);
        Button change = Ui.textButton(this,
                alarm.repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY
                        ? "设置周期日时分"
                        : alarm.timeMode == AlarmConfig.TIME_RELATIVE
                                ? "设置偏移" : "设置时间");
        change.setOnClickListener(view -> {
            if (alarm.repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY) editShift();
            else if (alarm.timeMode == AlarmConfig.TIME_RELATIVE) editOffset();
            else chooseTime();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 58));
        params.setMargins(0, Ui.dp(this, 8), 0, Ui.dp(this, 5));
        card.addView(change, params);
        return card;
    }

    private void editOffset() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(alarm.offsetMinutes));
        input.setSelectAllOnFocus(true);
        input.setHint("1～525600");
        int pad = Ui.dp(this, 24);
        input.setPadding(pad, 0, pad, 0);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("相对于 t0 的偏移分钟")
                .setMessage("所有相对闹钟会用同一个 t0 独立计算。")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    try {
                        int value = Integer.parseInt(input.getText().toString().trim());
                        if (value < 1 || value > 525600) throw new NumberFormatException();
                        alarm = alarm.buildUpon().offsetMinutes(value).build();
                        dialog.dismiss();
                        render();
                    } catch (NumberFormatException exception) {
                        input.setError("请输入 1～525600 的整数分钟");
                    }
                }));
        dialog.show();
    }

    private void chooseTime() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            alarm = alarm.buildUpon().hour(hour).minute(minute).build();
            render();
        }, alarm.hour, alarm.minute, true).show();
    }

    private void chooseDate() {
        Calendar selected = Calendar.getInstance();
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(alarm.date);
            if (date != null) selected.setTime(date);
        } catch (ParseException ignored) {
        }
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    Calendar value = Calendar.getInstance();
                    value.set(year, month, day);
                    alarm = alarm.buildUpon().date(new SimpleDateFormat(
                            "yyyy-MM-dd", Locale.US).format(value.getTime())).build();
                    render();
                }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH));
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        Calendar max = (Calendar) today.clone();
        max.add(Calendar.YEAR, 2);
        dialog.getDatePicker().setMinDate(today.getTimeInMillis());
        dialog.getDatePicker().setMaxDate(max.getTimeInMillis());
        dialog.show();
    }

    private void chooseRepeat() {
        String[] choices = {"仅一次", "每天", "法定工作日", "节假日及周末",
                "单双休工作日", "轮班制工作日", "自定义"};
        new AlertDialog.Builder(this)
                .setTitle("重复")
                .setSingleChoiceItems(choices, alarm.repeatType, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == AlarmConfig.REPEAT_SHIFT_WORKDAY
                            && alarm.timeMode == AlarmConfig.TIME_RELATIVE) {
                        confirmShiftMode();
                        return;
                    }
                    alarm = alarm.buildUpon().repeatType(which).build();
                    if (which == AlarmConfig.REPEAT_DAILY) chooseExcludeHolidays();
                    else if (which == AlarmConfig.REPEAT_ODD_EVEN_WORKDAY) chooseOddEven();
                    else if (which == AlarmConfig.REPEAT_SHIFT_WORKDAY) editShift();
                    else if (which == AlarmConfig.REPEAT_CUSTOM) chooseCustomDays();
                    else render();
                })
                .setNegativeButton("取消", null).show();
    }

    private void confirmShiftMode() {
        new AlertDialog.Builder(this)
                .setTitle("切换为绝对时间？")
                .setMessage("轮班制的每个周期日都有独立时分，不能再由 t0 偏移决定。继续后将切换为绝对时间模式。")
                .setNegativeButton("取消", null)
                .setPositiveButton("继续", (dialog, which) -> {
                    alarm = alarm.buildUpon()
                            .timeMode(AlarmConfig.TIME_ABSOLUTE)
                            .repeatType(AlarmConfig.REPEAT_SHIFT_WORKDAY).build();
                    editShift();
                }).show();
    }

    private void chooseOddEven() {
        String[] values = {"本周双休（周一到周五提醒）", "本周单休（周一到周六提醒）"};
        new AlertDialog.Builder(this)
                .setTitle("单双休轮换提醒")
                .setSingleChoiceItems(values, alarm.oddEvenThisWeek - 1,
                        (dialog, which) -> {
                            alarm = alarm.buildUpon().oddEvenThisWeek(which + 1).build();
                            dialog.dismiss();
                            chooseExcludeHolidays();
                        })
                .setNegativeButton("取消", (dialog, which) -> render()).show();
    }

    private void chooseCustomDays() {
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        boolean[] checked = new boolean[7];
        for (int index = 0; index < 7; index++) {
            checked[index] = (alarm.customDays & (1 << index)) != 0;
        }
        new AlertDialog.Builder(this)
                .setTitle("自定义提醒周期")
                .setMultiChoiceItems(days, checked,
                        (dialog, which, value) -> checked[which] = value)
                .setNegativeButton("取消", (dialog, which) -> render())
                .setPositiveButton("下一步", (dialog, which) -> {
                    int mask = 0;
                    for (int index = 0; index < 7; index++) {
                        if (checked[index]) mask |= (1 << index);
                    }
                    if (mask == 0) {
                        Toast.makeText(this, "至少选择一天", Toast.LENGTH_LONG).show();
                        render();
                        return;
                    }
                    alarm = alarm.buildUpon().customDays(mask).build();
                    chooseExcludeHolidays();
                }).show();
    }

    private void chooseExcludeHolidays() {
        String[] option = {"法定节假日不提醒"};
        boolean[] checked = {alarm.excludeHolidays};
        new AlertDialog.Builder(this)
                .setTitle("节假日联动")
                .setMultiChoiceItems(option, checked,
                        (dialog, which, value) -> checked[0] = value)
                .setMessage("该选项对应 vivo 的 china_holiday 字段，需要 vivo 时钟能够获取节假日数据。")
                .setNegativeButton("取消", (dialog, which) -> render())
                .setPositiveButton("完成", (dialog, which) -> {
                    alarm = alarm.buildUpon().excludeHolidays(checked[0]).build();
                    render();
                }).show();
    }

    private void editLabel() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(alarm.label);
        input.setHint("闹钟");
        input.setSelectAllOnFocus(true);
        input.setPadding(Ui.dp(this, 24), 0, Ui.dp(this, 24), 0);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("闹钟名称")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String value = input.getText().toString().trim();
                    if (value.codePointCount(0, value.length()) > 56) {
                        input.setError("vivo 闹钟名称最多 56 个字符");
                        return;
                    }
                    alarm = alarm.buildUpon().label(value).build();
                    dialog.dismiss();
                    render();
                }));
        dialog.show();
    }

    private void editSnooze() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        android.widget.CheckBox enabled = new android.widget.CheckBox(this);
        enabled.setText("开启稍后提醒");
        enabled.setChecked(alarm.snoozeEnabled);
        form.addView(enabled);
        TextView interval = Ui.text(this,
                "间隔：" + alarm.snoozeMinutes + " 分钟", 15, Ui.COLOR_TEXT);
        interval.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 10));
        form.addView(interval);
        android.widget.SeekBar intervalBar = new android.widget.SeekBar(this);
        int[] intervals = {5, 10, 15, 30};
        intervalBar.setMax(3);
        intervalBar.setProgress(indexOf(intervals, alarm.snoozeMinutes));
        intervalBar.setOnSeekBarChangeListener(new SimpleSeekListener(progress ->
                interval.setText("间隔：" + intervals[progress] + " 分钟")));
        form.addView(intervalBar);
        TextView count = Ui.text(this,
                "次数：" + alarm.snoozeCount + " 次", 15, Ui.COLOR_TEXT);
        count.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 10));
        form.addView(count);
        android.widget.SeekBar countBar = new android.widget.SeekBar(this);
        int[] counts = {2, 3, 5, 10};
        countBar.setMax(3);
        countBar.setProgress(indexOf(counts, alarm.snoozeCount));
        countBar.setOnSeekBarChangeListener(new SimpleSeekListener(progress ->
                count.setText("次数：" + counts[progress] + " 次")));
        form.addView(countBar);
        new AlertDialog.Builder(this)
                .setTitle("稍后提醒")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("完成", (dialog, which) -> {
                    alarm = alarm.buildUpon()
                            .snoozeEnabled(enabled.isChecked())
                            .snoozeMinutes(intervals[intervalBar.getProgress()])
                            .snoozeCount(counts[countBar.getProgress()]).build();
                    render();
                }).show();
    }

    private int indexOf(int[] values, int target) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] == target) return index;
        }
        return 0;
    }

    private void editRingtone() {
        Intent intent = new Intent(this, RingtoneActivity.class);
        intent.putExtra(EXTRA_ALARM_JSON, alarm.toJsonString());
        startActivityForResult(intent, REQUEST_RINGTONE);
    }

    private void editShift() {
        Intent intent = new Intent(this, ShiftActivity.class);
        intent.putExtra(EXTRA_ALARM_JSON, alarm.toJsonString());
        startActivityForResult(intent, REQUEST_SHIFT);
    }

    private void editVoice() {
        Intent intent = new Intent(this, VoiceBroadcastActivity.class);
        intent.putExtra(EXTRA_ALARM_JSON, alarm.toJsonString());
        startActivityForResult(intent, REQUEST_VOICE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            render();
            return;
        }
        if (requestCode == REQUEST_RINGTONE || requestCode == REQUEST_SHIFT
                || requestCode == REQUEST_VOICE) {
            try {
                alarm = AlarmConfig.fromJsonString(data.getStringExtra(EXTRA_ALARM_JSON));
            } catch (JSONException | NullPointerException exception) {
                Toast.makeText(this, "二级配置读取失败", Toast.LENGTH_LONG).show();
            }
            render();
        }
    }

    private String repeatSummary() {
        String[] names = {"仅一次", "每天", "法定工作日", "节假日及周末",
                "单双休工作日", "轮班制工作日", "自定义"};
        String value = names[Math.max(0, Math.min(names.length - 1, alarm.repeatType))];
        if (alarm.repeatType == AlarmConfig.REPEAT_DAILY && alarm.excludeHolidays) {
            value += " · 节假日不提醒";
        } else if (alarm.repeatType == AlarmConfig.REPEAT_ODD_EVEN_WORKDAY) {
            value += alarm.oddEvenThisWeek == 2 ? " · 本周单休" : " · 本周双休";
            if (alarm.excludeHolidays) value += " · 节假日不提醒";
        } else if (alarm.repeatType == AlarmConfig.REPEAT_CUSTOM) {
            value += " · " + customDaysSummary();
            if (alarm.excludeHolidays) value += " · 节假日不提醒";
        } else if (alarm.repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY) {
            value += " · " + alarm.shiftDaysCount + " 天周期";
            if (alarm.excludeHolidays) value += " · 节假日不提醒";
        }
        return value;
    }

    private String customDaysSummary() {
        String[] names = {"一", "二", "三", "四", "五", "六", "日"};
        StringBuilder value = new StringBuilder("周");
        for (int index = 0; index < 7; index++) {
            if ((alarm.customDays & (1 << index)) != 0) value.append(names[index]);
        }
        return value.toString();
    }

    private String dateSummary() {
        if (alarm.repeatType != AlarmConfig.REPEAT_ONCE) return "重复闹钟不使用日期";
        if (alarm.timeMode == AlarmConfig.TIME_RELATIVE) return "执行时由 t0 + 偏移自动计算";
        return alarm.date;
    }

    private String labelSummary() {
        return alarm.label.isEmpty() ? "闹钟" : alarm.label;
    }

    private String ringtoneSummary() {
        String[] modes = {"无振动", "标准", "跟随音乐节奏", "", "", "", "",
                "", "", "", "舒缓", "心动", "蓝调", "SOS", "弹跳", "呼唤",
                "热舞", "紧急", "快速"};
        String vibrate = alarm.vibrateMode >= 0 && alarm.vibrateMode < modes.length
                ? modes[alarm.vibrateMode] : "标准";
        return alarm.ringtoneName + " · " + vibrate;
    }

    private String snoozeSummary() {
        return alarm.snoozeEnabled
                ? "间隔 " + alarm.snoozeMinutes + " 分钟，提醒 "
                        + alarm.snoozeCount + " 次"
                : "关闭";
    }

    private String voiceSummary() {
        if (!alarm.voiceBroadcast) return "关闭";
        int count = 0;
        for (int index = 1; index < alarm.broadcastContent.length(); index += 2) {
            if (alarm.broadcastContent.charAt(index) == '1') count++;
        }
        return "开启 · " + count + " 项播报内容";
    }

    private void finishWithResult() {
        if (alarm.timeMode == AlarmConfig.TIME_ABSOLUTE
                && alarm.repeatType == AlarmConfig.REPEAT_ONCE
                && !isFutureAbsolute(alarm)) {
            Toast.makeText(this,
                    "一次性绝对闹钟的日期和时间必须晚于现在",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Intent result = new Intent();
        result.putExtra(EXTRA_ALARM_JSON, alarm.toJsonString());
        setResult(RESULT_OK, result);
        finish();
    }

    private boolean isFutureAbsolute(AlarmConfig value) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                    .parse(value.date + " " + value.hour + ":" + value.minute);
            return date != null && date.getTime() > System.currentTimeMillis();
        } catch (ParseException exception) {
            return false;
        }
    }

    private static final class SimpleSeekListener
            implements android.widget.SeekBar.OnSeekBarChangeListener {
        interface Callback { void onChanged(int progress); }
        private final Callback callback;
        SimpleSeekListener(Callback callback) { this.callback = callback; }
        @Override public void onProgressChanged(android.widget.SeekBar seekBar,
                int progress, boolean fromUser) { callback.onChanged(progress); }
        @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) { }
        @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) { }
    }
}
