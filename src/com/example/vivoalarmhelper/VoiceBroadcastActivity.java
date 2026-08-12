package com.example.vivoalarmhelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VoiceBroadcastActivity extends Activity {
    private AlarmConfig alarm;
    private final ArrayList<Item> items = new ArrayList<>();

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
        parseItems(alarm.broadcastContent);
        render();
    }

    private void parseItems(String encoded) {
        items.clear();
        Set<Character> seen = new HashSet<>();
        for (int index = 0; index + 1 < encoded.length(); index += 2) {
            char code = encoded.charAt(index);
            if (title(code) != null && seen.add(code)) {
                items.add(new Item(code, encoded.charAt(index + 1) == '1'));
            }
        }
        for (char code : new char[]{'A', 'B', 'C', 'D'}) {
            if (seen.add(code)) items.add(new Item(code, code != 'C'));
        }
    }

    private void render() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Ui.COLOR_BACKGROUND);
        page.addView(Ui.toolbar(this, "语音播报", "取消", view -> finish(),
                "完成", view -> finishWithResult()));

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 36));

        LinearLayout master = Ui.card(this);
        master.addView(Ui.switchRow(this, "语音播报",
                "闹钟响铃时按下面的顺序播报所选内容。",
                alarm.voiceBroadcast, (button, checked) -> {
                    alarm = alarm.buildUpon().voiceBroadcast(checked).build();
                    render();
                }));
        root.addView(master, Ui.pageCardParams(this));

        TextView title = Ui.heading(this, "播报内容", 18);
        title.setPadding(Ui.dp(this, 22), 0, 0, Ui.dp(this, 8));
        root.addView(title);
        TextView hint = Ui.text(this,
                "vivo 支持拖动排序；这里使用 ▲/▼ 调整，开启时至少保留一项。",
                13, Ui.COLOR_SUBTEXT);
        hint.setPadding(Ui.dp(this, 22), 0, Ui.dp(this, 22), Ui.dp(this, 12));
        root.addView(hint);

        LinearLayout list = Ui.card(this);
        for (int index = 0; index < items.size(); index++) {
            final int position = index;
            Item item = items.get(index);
            LinearLayout row = Ui.switchRow(this, title(item.code), itemHint(item.code),
                    item.enabled, (button, checked) -> setEnabled(position, checked));
            LinearLayout controls = new LinearLayout(this);
            controls.setOrientation(LinearLayout.HORIZONTAL);
            controls.setGravity(Gravity.CENTER);
            Button up = Ui.textButton(this, "▲");
            up.setEnabled(index > 0);
            up.setOnClickListener(view -> move(position, position - 1));
            controls.addView(up, new LinearLayout.LayoutParams(
                    Ui.dp(this, 48), Ui.dp(this, 48)));
            Button down = Ui.textButton(this, "▼");
            down.setEnabled(index < items.size() - 1);
            down.setOnClickListener(view -> move(position, position + 1));
            controls.addView(down, new LinearLayout.LayoutParams(
                    Ui.dp(this, 48), Ui.dp(this, 48)));
            row.addView(controls);
            list.addView(row);
            if (index < items.size() - 1) list.addView(Ui.divider(this));
        }
        root.addView(list, Ui.pageCardParams(this));

        TextView permission = Ui.text(this,
                "功能依赖：所在地天气需要 vivo 天气和本地城市；待办事项需要 vivo 时钟的日历权限；待取快递依赖设备的 Jovi 能力；新闻资讯需要 vivo 时钟允许联网。参数会完整保存，但系统能力或权限缺失时对应内容可能不播报。",
                13, Ui.COLOR_SUBTEXT);
        permission.setPadding(Ui.dp(this, 22), 0, Ui.dp(this, 22), 0);
        root.addView(permission);

        scroll.addView(root);
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(page);
    }

    private void setEnabled(int position, boolean enabled) {
        if (!enabled && alarm.voiceBroadcast && enabledCount() <= 1) {
            Toast.makeText(this, "语音播报开启时至少保留一项",
                    Toast.LENGTH_SHORT).show();
            render();
            return;
        }
        items.get(position).enabled = enabled;
        storeEncoding();
    }

    private int enabledCount() {
        int count = 0;
        for (Item item : items) if (item.enabled) count++;
        return count;
    }

    private void move(int from, int to) {
        if (to < 0 || to >= items.size()) return;
        Item item = items.remove(from);
        items.add(to, item);
        storeEncoding();
        render();
    }

    private void storeEncoding() {
        alarm = alarm.buildUpon().broadcastContent(encoding()).build();
    }

    private String encoding() {
        StringBuilder value = new StringBuilder();
        for (Item item : items) {
            value.append(item.code).append(item.enabled ? '1' : '0');
        }
        return value.toString();
    }

    private String title(char code) {
        if (code == 'A') return "所在地天气";
        if (code == 'B') return "待办事项";
        if (code == 'C') return "待取快递";
        if (code == 'D') return "新闻资讯";
        return null;
    }

    private String itemHint(char code) {
        if (code == 'A') return "依赖 vivo 天气和本地城市";
        if (code == 'B') return "依赖日历读取权限";
        if (code == 'C') return "部分 OriginOS 版本会隐藏或强制关闭";
        return "依赖 vivo 时钟联网许可";
    }

    private void finishWithResult() {
        if (alarm.voiceBroadcast && enabledCount() == 0) {
            Toast.makeText(this, "语音播报开启时至少选择一项",
                    Toast.LENGTH_LONG).show();
            return;
        }
        storeEncoding();
        Intent result = new Intent();
        result.putExtra(EditAlarmActivity.EXTRA_ALARM_JSON, alarm.toJsonString());
        setResult(RESULT_OK, result);
        finish();
    }

    private static final class Item {
        final char code;
        boolean enabled;
        Item(char code, boolean enabled) {
            this.code = code;
            this.enabled = enabled;
        }
    }
}
