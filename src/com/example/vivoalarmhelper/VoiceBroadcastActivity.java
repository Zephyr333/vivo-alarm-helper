package com.example.vivoalarmhelper;

import android.app.Activity;
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
import java.util.HashSet;
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
        page.addView(Ui.toolbar(this, "语音播报", "‹",
                view -> finishWithResult(), "", null));

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 36));

        LinearLayout master = Ui.card(this);
        master.addView(Ui.switchRow(this, "语音播报",
                "闹钟响起后，播报当天的实用信息。",
                alarm.voiceBroadcast, (button, checked) -> {
                    alarm = alarm.buildUpon().voiceBroadcast(checked).build();
                    render();
                }));
        root.addView(master, Ui.pageCardParams(this));

        TextView title = Ui.text(this, "播报内容", 15, Ui.COLOR_SUBTEXT);
        title.setPadding(Ui.dp(this, 24), 0, 0, Ui.dp(this, 10));
        root.addView(title);
        LinearLayout enabledList = Ui.card(this);
        int enabledIndex = 0;
        for (int index = 0; index < items.size(); index++) {
            if (!items.get(index).enabled) continue;
            if (enabledIndex++ > 0) enabledList.addView(Ui.divider(this));
            enabledList.addView(enabledRow(index));
        }
        if (enabledIndex == 0) {
            TextView empty = Ui.text(this, "未选择播报内容", 15, Ui.COLOR_SUBTEXT);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(this, 26), 0, Ui.dp(this, 26));
            enabledList.addView(empty);
        }
        root.addView(enabledList, Ui.pageCardParams(this));

        int disabledCount = 0;
        for (Item item : items) if (!item.enabled) disabledCount++;
        if (disabledCount > 0) {
            TextView addTitle = Ui.text(this, "可添加内容", 15, Ui.COLOR_SUBTEXT);
            addTitle.setPadding(Ui.dp(this, 24), 0, 0, Ui.dp(this, 10));
            root.addView(addTitle);
            LinearLayout disabledList = Ui.card(this);
            int added = 0;
            for (int index = 0; index < items.size(); index++) {
                if (items.get(index).enabled) continue;
                if (added++ > 0) disabledList.addView(Ui.divider(this));
                final int position = index;
                LinearLayout row = Ui.row(this, title(items.get(index).code),
                        itemHint(items.get(index).code), false,
                        view -> setEnabled(position, true));
                TextView plus = Ui.text(this, "+", 28, Ui.COLOR_PRIMARY);
                plus.setGravity(Gravity.CENTER);
                plus.setContentDescription("添加" + title(items.get(index).code));
                row.addView(plus, new LinearLayout.LayoutParams(
                        Ui.dp(this, 52), Ui.dp(this, 52)));
                disabledList.addView(row);
            }
            root.addView(disabledList, Ui.pageCardParams(this));
        }

        scroll.addView(root);
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.setContentView(this, page);
    }

    private View enabledRow(int position) {
        Item item = items.get(position);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Ui.dp(this, 72));
        row.setPadding(Ui.dp(this, 2), Ui.dp(this, 8),
                Ui.dp(this, 2), Ui.dp(this, 8));
        row.setTag(String.valueOf(item.code));
        row.setOnDragListener((view, event) -> handleDrop(position, view, event));

        TextView remove = Ui.text(this, "−", 23, android.graphics.Color.WHITE);
        remove.setGravity(Gravity.CENTER);
        remove.setContentDescription("移除" + title(item.code));
        remove.setClickable(true);
        remove.setFocusable(true);
        remove.setBackground(Ui.roundedBackground(this, Ui.COLOR_PRIMARY,
                android.graphics.Color.TRANSPARENT, 17));
        remove.setOnClickListener(view -> setEnabled(position, false));
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                Ui.dp(this, 34), Ui.dp(this, 34));
        removeParams.setMarginEnd(Ui.dp(this, 16));
        row.addView(remove, removeParams);

        TextView label = Ui.text(this, title(item.code), 17, Ui.COLOR_TEXT);
        row.addView(label, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView handle = Ui.text(this, "≡", 30, Ui.COLOR_TEXT);
        handle.setGravity(Gravity.CENTER);
        handle.setContentDescription("按住拖动" + title(item.code));
        handle.setOnLongClickListener(view -> view.startDragAndDrop(
                ClipData.newPlainText("broadcast_code", String.valueOf(item.code)),
                new View.DragShadowBuilder(row), null, 0));
        row.addView(handle, new LinearLayout.LayoutParams(
                Ui.dp(this, 54), Ui.dp(this, 52)));
        return row;
    }

    private boolean handleDrop(int target, View view, DragEvent event) {
        if (event.getAction() == DragEvent.ACTION_DRAG_ENTERED) {
            view.setAlpha(0.72f);
        } else if (event.getAction() == DragEvent.ACTION_DRAG_EXITED
                || event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
            view.setAlpha(1f);
        } else if (event.getAction() == DragEvent.ACTION_DROP) {
            view.setAlpha(1f);
            ClipData data = event.getClipData();
            if (data == null || data.getItemCount() == 0) return false;
            String code = data.getItemAt(0).coerceToText(this).toString();
            int from = -1;
            for (int index = 0; index < items.size(); index++) {
                if (String.valueOf(items.get(index).code).equals(code)) from = index;
            }
            if (from >= 0 && from != target) move(from, target);
            return true;
        }
        return true;
    }

    private void setEnabled(int position, boolean enabled) {
        if (!enabled && alarm.voiceBroadcast && enabledCount() <= 1) {
            Toast.makeText(this, "语音播报开启时至少保留一项",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        items.get(position).enabled = enabled;
        storeEncoding();
        render();
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
        if (code == 'A') return "需要天气服务和本地城市";
        if (code == 'B') return "需要允许时钟读取日历";
        if (code == 'C') return "部分系统版本可能不提供";
        return "需要允许时钟联网";
    }

    @Override
    public void onBackPressed() {
        finishWithResult();
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
