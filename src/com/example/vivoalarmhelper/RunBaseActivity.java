package com.example.vivoalarmhelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Collects the first alarm time for one execution; it never edits the profile. */
public final class RunBaseActivity extends Activity {
    private static final int MODE_RELATIVE = 0;
    private static final int MODE_ABSOLUTE = 1;

    private String profileId;
    private AlarmProfile profile;
    private int mode = MODE_RELATIVE;
    private int durationMinutes = 60;
    private int hour;
    private int minute;
    private String date;

    public static Intent createIntent(Context context, String profileId) {
        Intent intent = new Intent(context, RunBaseActivity.class);
        intent.putExtra(CreateAlarmsActivity.EXTRA_PROFILE_ID, profileId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileId = getIntent().getStringExtra(
                CreateAlarmsActivity.EXTRA_PROFILE_ID);
        profile = ProfileStore.getProfile(this, profileId);
        if (profile == null || !profile.isSequence()) {
            Toast.makeText(this, "这套方案已不存在或时间方式已改变",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Calendar initial = Calendar.getInstance();
        initial.add(Calendar.HOUR_OF_DAY, 1);
        hour = initial.get(Calendar.HOUR_OF_DAY);
        minute = initial.get(Calendar.MINUTE);
        date = formatDate(initial.getTime());
        render();
    }

    private void render() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Ui.COLOR_BACKGROUND);
        page.addView(Ui.toolbar(this, "选择本次时间",
                "取消", view -> finish(),
                "开始", view -> startExecution()));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 36));

        root.addView(modeCard(), Ui.pageCardParams(this));
        root.addView(timeCard(), Ui.pageCardParams(this));

        if (mode == MODE_ABSOLUTE) {
            LinearLayout dateCard = Ui.card(this);
            dateCard.addView(Ui.navigationRow(this, "日期",
                    TimeText.date(date), true, view -> chooseDate()));
            root.addView(dateCard, Ui.pageCardParams(this));
        }

        LinearLayout summaryCard = Ui.card(this);
        summaryCard.addView(Ui.row(this,
                "本次将创建 " + profile.getAlarms().size() + " 个闹钟",
                sequenceSummary(), false, null));
        root.addView(summaryCard, Ui.pageCardParams(this));

        TextView note = Ui.text(this,
                "这里选择的时间只用于本次执行，不会保存为方案的固定时间。",
                13, Ui.COLOR_SUBTEXT);
        note.setGravity(Gravity.CENTER);
        note.setPadding(Ui.dp(this, 24), 0,
                Ui.dp(this, 24), Ui.dp(this, 8));
        root.addView(note);

        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.setContentView(this, page);
    }

    private View modeCard() {
        LinearLayout card = Ui.card(this);
        TextView title = Ui.text(this, "第一个闹钟", 13, Ui.COLOR_SUBTEXT);
        title.setPadding(Ui.dp(this, 4), Ui.dp(this, 14),
                0, Ui.dp(this, 8));
        card.addView(title);

        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.setPadding(0, 0, 0, Ui.dp(this, 14));
        Button relative = modeButton("从现在起", mode == MODE_RELATIVE);
        relative.setOnClickListener(view -> {
            mode = MODE_RELATIVE;
            render();
        });
        modes.addView(relative, modeParams());
        Button absolute = modeButton("指定时间", mode == MODE_ABSOLUTE);
        absolute.setOnClickListener(view -> {
            mode = MODE_ABSOLUTE;
            render();
        });
        modes.addView(absolute, modeParams());
        card.addView(modes);
        return card;
    }

    private View timeCard() {
        LinearLayout card = Ui.card(this);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        String main = mode == MODE_RELATIVE
                ? TimeText.durationAfter(durationMinutes)
                : TimeText.clock(hour, minute);
        String sub = mode == MODE_RELATIVE
                ? "从点击“开始”时计算"
                : TimeText.date(date);
        TextView time = Ui.heading(this, main, 40);
        time.setGravity(Gravity.CENTER);
        time.setPadding(0, Ui.dp(this, 28), 0, Ui.dp(this, 4));
        card.addView(time);
        TextView detail = Ui.text(this, sub, 14, Ui.COLOR_SUBTEXT);
        detail.setGravity(Gravity.CENTER);
        card.addView(detail);
        Button change = Ui.textButton(this,
                mode == MODE_RELATIVE ? "调整时长" : "设置时间");
        change.setOnClickListener(view -> {
            if (mode == MODE_RELATIVE) chooseDuration();
            else chooseClock();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 58));
        params.setMargins(0, Ui.dp(this, 8), 0, Ui.dp(this, 5));
        card.addView(change, params);
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

    private void chooseDuration() {
        VivoDialogs.showDuration(this, "第一个闹钟多久以后",
                durationMinutes, value -> {
                    durationMinutes = value;
                    render();
                });
    }

    private void chooseClock() {
        VivoDialogs.showClock(this, hour, minute, (selectedHour, selectedMinute) -> {
            hour = selectedHour;
            minute = selectedMinute;
            moveDateToTomorrowWhenTodayHasPassed();
            render();
        });
    }

    private void chooseDate() {
        VivoDialogs.showDate(this, date, value -> {
            date = value;
            render();
        });
    }

    private void startExecution() {
        long baseTargetAt;
        if (mode == MODE_RELATIVE) {
            Calendar target = Calendar.getInstance();
            target.add(Calendar.MINUTE, durationMinutes);
            clearSeconds(target);
            baseTargetAt = target.getTimeInMillis();
        } else {
            Calendar target = absoluteTarget();
            if (target == null || target.getTimeInMillis()
                    <= System.currentTimeMillis()) {
                Toast.makeText(this,
                        "指定的日期和时间必须晚于现在",
                        Toast.LENGTH_LONG).show();
                return;
            }
            baseTargetAt = target.getTimeInMillis();
        }
        startActivity(CreateAlarmsActivity.createRuntimeRunIntent(
                this, profileId, baseTargetAt));
        finish();
    }

    private Calendar absoluteTarget() {
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .parse(date);
            if (parsed == null) return null;
            Calendar target = Calendar.getInstance();
            target.setTime(parsed);
            target.set(Calendar.HOUR_OF_DAY, hour);
            target.set(Calendar.MINUTE, minute);
            clearSeconds(target);
            return target;
        } catch (ParseException exception) {
            return null;
        }
    }

    private void moveDateToTomorrowWhenTodayHasPassed() {
        Calendar target = absoluteTarget();
        if (target == null || target.getTimeInMillis()
                > System.currentTimeMillis()) return;
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        date = formatDate(tomorrow.getTime());
    }

    private String sequenceSummary() {
        if (profile.getAlarms().size() == 1) {
            return "只创建你本次选择的第一个闹钟";
        }
        StringBuilder value = new StringBuilder("第一个闹钟之后");
        for (int index = 1; index < profile.getAlarms().size(); index++) {
            value.append(index == 1 ? "，间隔 " : "，再间隔 ")
                    .append(TimeText.duration(
                            profile.getAlarms().get(index).offsetMinutes));
        }
        return value.toString();
    }

    private String formatDate(Date value) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(value);
    }

    private void clearSeconds(Calendar value) {
        value.set(Calendar.SECOND, 0);
        value.set(Calendar.MILLISECOND, 0);
    }
}
