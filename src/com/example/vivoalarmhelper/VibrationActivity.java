package com.example.vivoalarmhelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONException;

public final class VibrationActivity extends Activity {
    private static final String[] NAMES = {"无振动", "跟随音乐节奏", "标准",
            "舒缓", "心动", "蓝调", "SOS", "弹跳", "呼唤", "热舞", "紧急", "快速"};
    private static final int[] VALUES = {0, 2, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18};
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
        page.addView(Ui.toolbar(this, "振动效果", "‹",
                view -> finishWithResult(), "", null));
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 36));
        LinearLayout card = Ui.card(this);
        for (int index = 0; index < VALUES.length; index++) {
            final int value = VALUES[index];
            card.addView(Ui.radioRow(this, NAMES[index], null,
                    alarm.vibrateMode == value, view -> select(value)));
            if (index < VALUES.length - 1) card.addView(Ui.divider(this));
        }
        root.addView(card, Ui.pageCardParams(this));
        scroll.addView(root);
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.setContentView(this, page);
    }

    private void select(int value) {
        if (value == 2 && (alarm.ringtoneType == AlarmConfig.RING_CUSTOM
                || alarm.ringtoneType == AlarmConfig.RING_WEATHER
                || alarm.ringtoneType == AlarmConfig.RING_SILENT)) {
            Toast.makeText(this, "当前铃声不能使用“跟随音乐节奏”",
                    Toast.LENGTH_LONG).show();
            return;
        }
        alarm = alarm.buildUpon().vibrateMode(value).build();
        render();
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
