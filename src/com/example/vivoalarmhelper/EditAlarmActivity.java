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
    public static final String EXTRA_IS_NEW = "is_new_alarm";
    public static final String EXTRA_SEQUENCE_MODE = "sequence_mode";
    public static final String EXTRA_SEQUENCE_INDEX = "sequence_index";
    private static final int REQUEST_RINGTONE = 51;
    private static final int REQUEST_SHIFT = 52;
    private static final int REQUEST_VOICE = 53;
    private static final int REQUEST_REPEAT = 54;

    private AlarmConfig alarm;
    private LinearLayout page;
    private boolean creating;
    private boolean sequenceMode;
    private int sequenceIndex;
    private String initialSignature;

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
        creating = getIntent().getBooleanExtra(EXTRA_IS_NEW, false);
        sequenceMode = getIntent().getBooleanExtra(EXTRA_SEQUENCE_MODE, false);
        sequenceIndex = Math.max(0,
                getIntent().getIntExtra(EXTRA_SEQUENCE_INDEX, 0));
        initialSignature = alarm.toJsonString();
        render();
    }

    private void render() {
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Ui.COLOR_BACKGROUND);
        page.addView(Ui.toolbar(this,
                creating ? "新建闹钟" : "编辑闹钟",
                creating ? "取消" : "‹", view -> handleBack(),
                "完成", view -> finishWithResult()));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 40));

        root.addView(modeCard(), Ui.pageCardParams(this));
        root.addView(timeHeader(), Ui.pageCardParams(this));

        LinearLayout timeCard = Ui.card(this);
        timeCard.addView(Ui.navigationRow(this, "重复", repeatSummary(), true,
                view -> chooseRepeat()));
        timeCard.addView(Ui.divider(this));
        boolean dateEnabled = alarm.timeMode == AlarmConfig.TIME_ABSOLUTE
                && alarm.repeatType == AlarmConfig.REPEAT_ONCE;
        timeCard.addView(Ui.navigationRow(this, "日期", dateSummary(),
                dateEnabled,
                view -> {
                    if (dateEnabled) chooseDate();
                }));
        root.addView(timeCard, Ui.pageCardParams(this));

        LinearLayout baseCard = Ui.card(this);
        baseCard.addView(Ui.navigationRow(this, "闹钟名称", labelSummary(), true,
                view -> editLabel()));
        baseCard.addView(Ui.divider(this));
        baseCard.addView(Ui.navigationRow(this, "铃声与振动", ringtoneSummary(), true,
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
        snoozeCard.addView(Ui.navigationRow(this, "稍后提醒", snoozeSummary(), true,
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
        voiceCard.addView(Ui.navigationRow(this, "语音播报", voiceSummary(), true,
                view -> editVoice()));
        root.addView(voiceCard, Ui.pageCardParams(this));

        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.setContentView(this, page);
    }

    private View modeCard() {
        LinearLayout card = Ui.card(this);
        TextView title = Ui.text(this, "时间模式", 13, Ui.COLOR_SUBTEXT);
        title.setPadding(Ui.dp(this, 4), Ui.dp(this, 14), 0, Ui.dp(this, 8));
        card.addView(title);
        if (sequenceMode) {
            card.addView(Ui.row(this,
                    sequenceIndex == 0 ? "第一个闹钟时间" : "接在前一个闹钟之后",
                    sequenceIndex == 0
                            ? "每次执行方案时选择，不预先保存在方案中"
                            : "设置与前一个闹钟之间的间隔",
                    false, null));
            TextView help = Ui.text(this,
                    "执行时可以选择“从现在起”或“指定时间”，后续闹钟按已保存的间隔接续。",
                    12, Ui.COLOR_SUBTEXT);
            help.setPadding(Ui.dp(this, 4), 0,
                    Ui.dp(this, 4), Ui.dp(this, 14));
            card.addView(help);
            return card;
        }
        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.setPadding(0, 0, 0, Ui.dp(this, 14));
        Button relative = modeButton("从现在起", alarm.timeMode == AlarmConfig.TIME_RELATIVE);
        relative.setOnClickListener(view -> {
            if (alarm.repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY) {
                Toast.makeText(this,
                        "轮班制的周期日有独立时分，不能使用“从现在起”",
                        Toast.LENGTH_LONG).show();
                return;
            }
            alarm = alarm.buildUpon().timeMode(AlarmConfig.TIME_RELATIVE).build();
            render();
        });
        modes.addView(relative, modeParams());
        Button absolute = modeButton("指定时间", alarm.timeMode == AlarmConfig.TIME_ABSOLUTE);
        absolute.setOnClickListener(view -> {
            alarm = alarm.buildUpon().timeMode(AlarmConfig.TIME_ABSOLUTE).build();
            render();
        });
        modes.addView(absolute, modeParams());
        card.addView(modes);
        TextView help = Ui.text(this,
                alarm.timeMode == AlarmConfig.TIME_RELATIVE
                        ? "执行方案时开始计算；同一方案中的所有相对闹钟使用同一个当前时间。"
                        : "按指定时分提醒；仅一次时还会使用下面选择的日期。",
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
            main = sequenceMode
                    ? sequenceIndex == 0 ? "执行时选择"
                            : "间隔 " + TimeText.duration(alarm.offsetMinutes)
                    : TimeText.durationAfter(alarm.offsetMinutes);
            sub = sequenceMode
                    ? sequenceIndex == 0
                            ? "本方案不保存固定的第一个时间"
                            : "从前一个闹钟继续计算"
                    : "执行方案后开始计算";
        } else {
            main = String.format(Locale.CHINA, "%02d:%02d", alarm.hour, alarm.minute);
            sub = alarm.repeatType == AlarmConfig.REPEAT_ONCE
                    ? TimeText.date(alarm.date) : "按重复规则计算下一次";
        }
        TextView time = Ui.heading(this, main, 40);
        time.setGravity(Gravity.CENTER);
        time.setPadding(0, Ui.dp(this, 28), 0, Ui.dp(this, 4));
        card.addView(time);
        TextView detail = Ui.text(this, sub, 14, Ui.COLOR_SUBTEXT);
        detail.setGravity(Gravity.CENTER);
        card.addView(detail);
        if (sequenceMode && sequenceIndex == 0) {
            detail.setPadding(0, 0, 0, Ui.dp(this, 28));
            return card;
        }
        Button change = Ui.textButton(this,
                alarm.repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY
                        ? "设置周期日时分"
                        : alarm.timeMode == AlarmConfig.TIME_RELATIVE
                                ? sequenceMode
                                        ? "调整间隔"
                                        : "调整时长"
                                : "设置时间");
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
        String title = sequenceMode
                ? sequenceIndex == 0
                        ? "第一个闹钟多久以后" : "与前一个闹钟间隔"
                : "多久以后提醒";
        VivoDialogs.showDuration(this, title, alarm.offsetMinutes, value -> {
            alarm = alarm.buildUpon().offsetMinutes(value).build();
            render();
        });
    }

    private void chooseTime() {
        VivoDialogs.showClock(this, alarm.hour, alarm.minute, (hour, minute) -> {
            alarm = alarm.buildUpon().hour(hour).minute(minute).build();
            render();
        });
    }

    private void chooseDate() {
        VivoDialogs.showDate(this, alarm.date, value -> {
            alarm = alarm.buildUpon().date(value).build();
            render();
        });
    }

    private void chooseRepeat() {
        Intent intent = new Intent(this, RepeatActivity.class);
        intent.putExtra(EXTRA_ALARM_JSON, alarm.toJsonString());
        intent.putExtra(EXTRA_SEQUENCE_MODE, sequenceMode);
        startActivityForResult(intent, REQUEST_REPEAT);
    }

    private void editLabel() {
        VivoDialogs.showTextInput(this, "闹钟名称", alarm.label,
                "闹钟", 56, value -> {
                    alarm = alarm.buildUpon().label(value).build();
                    render();
                });
    }

    private void editSnooze() {
        VivoDialogs.showSnooze(this, alarm.snoozeEnabled,
                alarm.snoozeMinutes, alarm.snoozeCount,
                (enabled, minutes, count) -> {
                    alarm = alarm.buildUpon()
                            .snoozeEnabled(enabled)
                            .snoozeMinutes(minutes)
                            .snoozeCount(count).build();
                    render();
                });
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
                || requestCode == REQUEST_VOICE || requestCode == REQUEST_REPEAT) {
            try {
                alarm = AlarmConfig.fromJsonString(data.getStringExtra(EXTRA_ALARM_JSON));
                if (sequenceMode && (alarm.timeMode != AlarmConfig.TIME_RELATIVE
                        || alarm.repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY)) {
                    Toast.makeText(this,
                            "执行时选择时间＋间隔不能使用轮班制时间",
                            Toast.LENGTH_LONG).show();
                    alarm = alarm.buildUpon()
                            .timeMode(AlarmConfig.TIME_RELATIVE)
                            .repeatType(AlarmConfig.REPEAT_ONCE).build();
                }
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
        if (alarm.timeMode == AlarmConfig.TIME_RELATIVE) return "执行时自动计算";
        return TimeText.date(alarm.date);
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

    @Override
    public void onBackPressed() {
        handleBack();
    }

    private void handleBack() {
        if (alarm.toJsonString().equals(initialSignature)) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("放弃未保存的修改？")
                .setMessage("返回后，这次对闹钟的修改不会保留。")
                .setNegativeButton("继续编辑", null)
                .setPositiveButton("放弃修改", (dialog, which) -> finish())
                .show();
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

}
