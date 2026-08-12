package com.example.vivoalarmhelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.File;

public final class RingtoneActivity extends Activity {
    private static final int REQUEST_SYSTEM_RINGTONE = 61;
    private static final int REQUEST_CUSTOM_AUDIO = 62;
    private static final int REQUEST_VIBRATION = 63;
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
        page.addView(Ui.toolbar(this, "铃声与振动", "‹",
                view -> finishWithResult(), "", null));

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 36));

        LinearLayout settings = Ui.card(this);
        settings.addView(Ui.navigationRow(this, "振动效果",
                vibrationName(alarm.vibrateMode), true, view -> chooseVibration()));
        settings.addView(Ui.divider(this));
        settings.addView(Ui.navigationRow(this, "自定义铃声",
                alarm.ringtoneType == AlarmConfig.RING_CUSTOM
                        ? alarm.ringtoneName : "", true,
                view -> pickCustomAudio()));
        root.addView(settings, Ui.pageCardParams(this));

        TextView ringTitle = Ui.text(this, "系统铃声", 15, Ui.COLOR_SUBTEXT);
        ringTitle.setPadding(Ui.dp(this, 22), 0, 0, Ui.dp(this, 10));
        root.addView(ringTitle);
        LinearLayout rings = Ui.card(this);
        rings.addView(optionRow("无铃声", null,
                alarm.ringtoneType == AlarmConfig.RING_SILENT,
                () -> setRingtone(AlarmConfig.RING_SILENT,
                        "vivo.ring.silence", "无铃声")));
        rings.addView(Ui.divider(this));
        rings.addView(optionRow("跟随默认铃声", "使用系统当前默认闹钟铃声",
                alarm.ringtoneType == AlarmConfig.RING_FOLLOW_DEFAULT,
                () -> setRingtone(AlarmConfig.RING_FOLLOW_DEFAULT, "", "默认铃声")));
        if (hasWeatherRingtone()) {
            rings.addView(Ui.divider(this));
            rings.addView(optionRow("随天气响铃", "vivo 根据天气选择系统铃声",
                    alarm.ringtoneType == AlarmConfig.RING_WEATHER,
                    () -> setRingtone(AlarmConfig.RING_WEATHER, "", "随天气响铃")));
        }
        rings.addView(Ui.divider(this));
        rings.addView(optionRow("选择其他系统铃声", "打开系统闹钟铃声列表",
                alarm.ringtoneType == AlarmConfig.RING_INTERNAL,
                this::pickSystemRingtone));
        root.addView(rings, Ui.pageCardParams(this));

        scroll.addView(root);
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.setContentView(this, page);
    }

    private LinearLayout optionRow(String title, String summary,
            boolean selected, Runnable click) {
        return Ui.radioRow(this, title, summary, selected, view -> click.run());
    }

    private void setRingtone(int type, String uri, String name) {
        alarm = alarm.buildUpon().ringtoneType(type).alertUri(uri)
                .ringtoneName(name).build();
        render();
    }

    private void pickSystemRingtone() {
        Intent picker = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        picker.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_ALARM);
        picker.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        picker.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        if (!alarm.alertUri.isEmpty()) {
            picker.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    Uri.parse(alarm.alertUri));
        }
        try {
            startActivityForResult(picker, REQUEST_SYSTEM_RINGTONE);
        } catch (RuntimeException exception) {
            Toast.makeText(this, "无法打开系统铃声选择器",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void pickCustomAudio() {
        // vivo's own custom-ringtone page stores a MediaStore content URI. Using
        // ACTION_PICK here produces the same durable URI shape instead of a
        // document-provider URI whose temporary grant could expire later.
        Intent picker = new Intent(Intent.ACTION_PICK,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        picker.setType("audio/*");
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivityForResult(picker, REQUEST_CUSTOM_AUDIO);
        } catch (RuntimeException exception) {
            Toast.makeText(this, "无法打开本地音频选择器",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == REQUEST_SYSTEM_RINGTONE) {
            Uri uri = data.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri == null) {
                setRingtone(AlarmConfig.RING_SILENT,
                        "vivo.ring.silence", "无铃声");
                return;
            }
            if (RingtoneManager.isDefault(uri)) {
                setRingtone(AlarmConfig.RING_FOLLOW_DEFAULT,
                        uri.toString(), "默认铃声");
                return;
            }
            String name = titleForRingtone(uri);
            setRingtone(AlarmConfig.RING_INTERNAL, uri.toString(), name);
        } else if (requestCode == REQUEST_CUSTOM_AUDIO) {
            Uri uri = data.getData();
            if (uri == null) return;
            setRingtone(AlarmConfig.RING_CUSTOM, uri.toString(),
                    displayName(uri));
        } else if (requestCode == REQUEST_VIBRATION) {
            try {
                alarm = AlarmConfig.fromJsonString(data.getStringExtra(
                        EditAlarmActivity.EXTRA_ALARM_JSON));
                render();
            } catch (JSONException | NullPointerException exception) {
                Toast.makeText(this, "振动设置读取失败", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String titleForRingtone(Uri uri) {
        try {
            Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
            String title = ringtone == null ? null : ringtone.getTitle(this);
            return title == null || title.trim().isEmpty() ? "系统铃声" : title;
        } catch (RuntimeException exception) {
            return "系统铃声";
        }
    }

    private String displayName(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty()) return name;
            }
        } catch (RuntimeException ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return "自定义音频";
    }

    private void chooseVibration() {
        Intent intent = new Intent(this, VibrationActivity.class);
        intent.putExtra(EditAlarmActivity.EXTRA_ALARM_JSON, alarm.toJsonString());
        startActivityForResult(intent, REQUEST_VIBRATION);
    }

    private String vibrationName(int mode) {
        if (mode == 0) return "无振动";
        if (mode == 2) return "跟随音乐节奏";
        if (mode == 1) return "标准";
        String[] custom = {"舒缓", "心动", "蓝调", "SOS", "弹跳", "呼唤",
                "热舞", "紧急", "快速"};
        int index = mode - 10;
        return index >= 0 && index < custom.length ? custom[index] : "标准";
    }

    private boolean hasWeatherRingtone() {
        File directory = new File("/system/media/audio/alarms");
        File[] files = directory.listFiles();
        if (files == null) return false;
        for (File file : files) {
            if (file.getName().startsWith("weather_")) return true;
        }
        return false;
    }

    private void finishWithResult() {
        Intent result = new Intent();
        result.putExtra(EditAlarmActivity.EXTRA_ALARM_JSON, alarm.toJsonString());
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onBackPressed() {
        finishWithResult();
    }
}
