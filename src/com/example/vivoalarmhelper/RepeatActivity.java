package com.example.vivoalarmhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONException;

public final class RepeatActivity extends Activity {
    private AlarmConfig alarm;
    private boolean sequenceMode;

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
        sequenceMode = getIntent().getBooleanExtra(
                EditAlarmActivity.EXTRA_SEQUENCE_MODE, false);
        render();
    }

    private void render() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Ui.COLOR_BACKGROUND);
        page.addView(Ui.toolbar(this, "重复", "‹", view -> finishWithResult(),
                "", null));

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 36));
        LinearLayout card = Ui.card(this);
        addOption(card, "仅一次", null, AlarmConfig.REPEAT_ONCE);
        addDivider(card);
        addOption(card, "每天", null, AlarmConfig.REPEAT_DAILY);
        addDivider(card);
        addOption(card, "法定工作日",
                "周一到周五（除法定节假日），以及调休上班日。",
                AlarmConfig.REPEAT_LEGAL_WORKDAY);
        addDivider(card);
        addOption(card, "节假日及周末",
                "法定节假日，以及周六和周日（除调休上班日）。",
                AlarmConfig.REPEAT_HOLIDAY_WEEKEND);
        addDivider(card);
        addOption(card, "单双休工作日", null,
                AlarmConfig.REPEAT_ODD_EVEN_WORKDAY);
        addDivider(card);
        addOption(card, "轮班制工作日", null,
                AlarmConfig.REPEAT_SHIFT_WORKDAY);
        addDivider(card);
        addOption(card, "自定义", customDaysSummary(), AlarmConfig.REPEAT_CUSTOM);
        root.addView(card, Ui.pageCardParams(this));

        scroll.addView(root);
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.setContentView(this, page);
    }

    private void addOption(LinearLayout card, String title, String summary,
            int repeatType) {
        card.addView(Ui.radioRow(this, title, summary,
                alarm.repeatType == repeatType,
                view -> selectRepeat(repeatType)));
    }

    private void addDivider(LinearLayout card) {
        card.addView(Ui.divider(this));
    }

    private void selectRepeat(int repeatType) {
        if (sequenceMode && repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY) {
            Toast.makeText(this,
                    "执行时选择时间＋间隔不能使用轮班制时间；如需轮班制，请在方案中选择“每个闹钟分别设置”",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY
                && alarm.timeMode == AlarmConfig.TIME_RELATIVE) {
            new AlertDialog.Builder(this)
                    .setTitle("改为指定时间？")
                    .setMessage("轮班制的每个周期日都有自己的时分，不能使用“从现在起”。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("继续", (dialog, which) -> {
                        alarm = alarm.buildUpon()
                                .timeMode(AlarmConfig.TIME_ABSOLUTE)
                                .repeatType(repeatType).build();
                        render();
                    }).show();
            return;
        }
        alarm = alarm.buildUpon().repeatType(repeatType).build();
        render();
        if (repeatType == AlarmConfig.REPEAT_DAILY) {
            chooseExcludeHolidays();
        } else if (repeatType == AlarmConfig.REPEAT_ODD_EVEN_WORKDAY) {
            chooseOddEven();
        } else if (repeatType == AlarmConfig.REPEAT_CUSTOM) {
            chooseCustomDays();
        }
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
                .setNegativeButton("取消", null).show();
    }

    private void chooseCustomDays() {
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        boolean[] checked = new boolean[7];
        for (int index = 0; index < 7; index++) {
            checked[index] = (alarm.customDays & (1 << index)) != 0;
        }
        new AlertDialog.Builder(this)
                .setTitle("选择提醒日期")
                .setMultiChoiceItems(days, checked,
                        (dialog, which, value) -> checked[which] = value)
                .setNegativeButton("取消", null)
                .setPositiveButton("下一步", (dialog, which) -> {
                    int mask = 0;
                    for (int index = 0; index < 7; index++) {
                        if (checked[index]) mask |= 1 << index;
                    }
                    if (mask == 0) {
                        Toast.makeText(this, "至少选择一天",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    alarm = alarm.buildUpon().customDays(mask).build();
                    render();
                    chooseExcludeHolidays();
                }).show();
    }

    private void chooseExcludeHolidays() {
        String[] option = {"法定节假日不提醒"};
        boolean[] checked = {alarm.excludeHolidays};
        new AlertDialog.Builder(this)
                .setTitle("节假日设置")
                .setMultiChoiceItems(option, checked,
                        (dialog, which, value) -> checked[0] = value)
                .setNegativeButton("取消", null)
                .setPositiveButton("完成", (dialog, which) -> {
                    alarm = alarm.buildUpon().excludeHolidays(checked[0]).build();
                    render();
                }).show();
    }

    private String customDaysSummary() {
        if (alarm.repeatType != AlarmConfig.REPEAT_CUSTOM) return null;
        String[] names = {"一", "二", "三", "四", "五", "六", "日"};
        StringBuilder value = new StringBuilder("周");
        for (int index = 0; index < names.length; index++) {
            if ((alarm.customDays & (1 << index)) != 0) value.append(names[index]);
        }
        return value.toString();
    }

    @Override
    public void onBackPressed() {
        finishWithResult();
    }

    private void finishWithResult() {
        Intent result = new Intent();
        result.putExtra(EditAlarmActivity.EXTRA_ALARM_JSON, alarm.toJsonString());
        setResult(RESULT_OK, result);
        finish();
    }
}
