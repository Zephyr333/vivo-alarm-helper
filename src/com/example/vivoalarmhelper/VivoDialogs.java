package com.example.vivoalarmhelper;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class VivoDialogs {
    public interface IntCallback { void accept(int value); }
    public interface ClockCallback { void accept(int hour, int minute); }
    public interface StringCallback { void accept(String value); }

    private VivoDialogs() {
    }

    public static void showDuration(Activity activity, int totalMinutes,
            IntCallback callback) {
        showDuration(activity, "多久以后提醒", totalMinutes, callback);
    }

    public static void showDuration(Activity activity, String title,
            int totalMinutes, IntCallback callback) {
        Dialog dialog = dialog(activity);
        LinearLayout content = sheet(activity, title);

        LinearLayout pickers = pickerRow(activity);
        NumberPicker hours = numberPicker(activity, 0, 8760,
                totalMinutes / 60, value -> value + " 小时");
        NumberPicker minutes = numberPicker(activity, 0, 59,
                totalMinutes % 60, value -> String.format(
                        Locale.CHINA, "%02d 分钟", value));
        pickers.addView(hours, pickerParams());
        pickers.addView(minutes, pickerParams());
        content.addView(pickers, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(activity, 190)));

        TextView note = Ui.text(activity,
                "vivo 系统闹钟以分钟为单位；超过 23 小时不会归零。",
                13, Ui.COLOR_SUBTEXT);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, 0, 0, Ui.dp(activity, 14));
        content.addView(note);

        addActions(activity, dialog, content, "确定", () -> {
            long value = (long) hours.getValue() * 60L + minutes.getValue();
            if (value < 1 || value > 525600) {
                Toast.makeText(activity, "请设置 1 分钟到 8760 小时",
                        Toast.LENGTH_LONG).show();
                return false;
            }
            callback.accept((int) value);
            return true;
        });
        show(activity, dialog, content);
    }

    public static void showClock(Activity activity, int selectedHour,
            int selectedMinute, ClockCallback callback) {
        Dialog dialog = dialog(activity);
        LinearLayout content = sheet(activity, "选择时间");
        LinearLayout pickers = pickerRow(activity);
        NumberPicker hour = numberPicker(activity, 0, 23, selectedHour,
                value -> String.format(Locale.CHINA, "%02d 时", value));
        NumberPicker minute = numberPicker(activity, 0, 59, selectedMinute,
                value -> String.format(Locale.CHINA, "%02d 分", value));
        pickers.addView(hour, pickerParams());
        pickers.addView(minute, pickerParams());
        content.addView(pickers, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(activity, 200)));
        addActions(activity, dialog, content, "确定", () -> {
            callback.accept(hour.getValue(), minute.getValue());
            return true;
        });
        show(activity, dialog, content);
    }

    public static void showNumber(Activity activity, String title, int current,
            int min, int max, String suffix, IntCallback callback) {
        Dialog dialog = dialog(activity);
        LinearLayout content = sheet(activity, title);
        NumberPicker picker = numberPicker(activity, min, max, current,
                value -> value + suffix);
        content.addView(picker, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(activity, 190)));
        addActions(activity, dialog, content, "确定", () -> {
            callback.accept(picker.getValue());
            return true;
        });
        show(activity, dialog, content);
    }

    public static void showDate(Activity activity, String current,
            StringCallback callback) {
        Calendar today = Calendar.getInstance();
        clearTime(today);
        Calendar selected = (Calendar) today.clone();
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(current);
            if (parsed != null) selected.setTime(parsed);
        } catch (ParseException ignored) {
        }
        if (selected.before(today)) selected = (Calendar) today.clone();
        final Calendar initial = selected;

        Dialog dialog = dialog(activity);
        LinearLayout content = sheet(activity, "选择日期");
        LinearLayout pickers = pickerRow(activity);
        NumberPicker year = numberPicker(activity, today.get(Calendar.YEAR),
                today.get(Calendar.YEAR) + 2, initial.get(Calendar.YEAR),
                value -> value + " 年");
        NumberPicker month = numberPicker(activity, 1, 12,
                initial.get(Calendar.MONTH) + 1, value -> value + " 月");
        NumberPicker day = numberPicker(activity, 1,
                initial.getActualMaximum(Calendar.DAY_OF_MONTH),
                initial.get(Calendar.DAY_OF_MONTH), value -> value + " 日");
        NumberPicker.OnValueChangeListener refreshDays = (picker, oldValue, newValue) ->
                updateDayRange(day, year.getValue(), month.getValue());
        year.setOnValueChangedListener(refreshDays);
        month.setOnValueChangedListener(refreshDays);
        pickers.addView(year, pickerParams());
        pickers.addView(month, pickerParams());
        pickers.addView(day, pickerParams());
        content.addView(pickers, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(activity, 210)));
        addActions(activity, dialog, content, "确定", () -> {
            Calendar value = Calendar.getInstance();
            value.set(year.getValue(), month.getValue() - 1, day.getValue());
            clearTime(value);
            if (value.before(today)) {
                Toast.makeText(activity, "请选择今天或之后的日期",
                        Toast.LENGTH_LONG).show();
                return false;
            }
            callback.accept(new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .format(value.getTime()));
            return true;
        });
        show(activity, dialog, content);
    }

    public static void showTextInput(Activity activity, String title,
            String current, String hint, int maxCodePoints,
            StringCallback callback) {
        Dialog dialog = dialog(activity);
        LinearLayout content = sheet(activity, title);
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setText(current);
        input.setHint(hint);
        input.setTextSize(18);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSelectAllOnFocus(true);
        input.setPadding(Ui.dp(activity, 4), Ui.dp(activity, 10),
                Ui.dp(activity, 4), Ui.dp(activity, 10));
        content.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(activity, 64)));
        addActions(activity, dialog, content, "确定", () -> {
            String value = input.getText().toString().trim();
            if (value.codePointCount(0, value.length()) > maxCodePoints) {
                input.setError("最多 " + maxCodePoints + " 个字符");
                return false;
            }
            callback.accept(value);
            return true;
        });
        dialog.setOnShowListener(ignored -> {
            input.requestFocus();
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        });
        show(activity, dialog, content);
    }

    public static void showSnooze(Activity activity, boolean enabled,
            int selectedMinutes, int selectedCount, SnoozeCallback callback) {
        Dialog dialog = dialog(activity);
        LinearLayout content = sheet(activity, "稍后提醒");
        android.widget.Switch master = new android.widget.Switch(activity);
        master.setText("开启稍后提醒");
        master.setTextSize(17);
        master.setChecked(enabled);
        master.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(master, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(activity, 64)));
        content.addView(Ui.divider(activity));

        LinearLayout pickers = pickerRow(activity);
        String[] intervalNames = {"5 分钟", "10 分钟", "15 分钟", "30 分钟"};
        int[] intervalValues = {5, 10, 15, 30};
        String[] countNames = {"2 次", "3 次", "5 次", "10 次"};
        int[] countValues = {2, 3, 5, 10};
        NumberPicker interval = valuePicker(activity, intervalNames,
                indexOf(intervalValues, selectedMinutes));
        NumberPicker count = valuePicker(activity, countNames,
                indexOf(countValues, selectedCount));
        pickers.addView(interval, pickerParams());
        pickers.addView(count, pickerParams());
        content.addView(pickers, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(activity, 180)));
        addActions(activity, dialog, content, "确定", () -> {
            callback.accept(master.isChecked(), intervalValues[interval.getValue()],
                    countValues[count.getValue()]);
            return true;
        });
        show(activity, dialog, content);
    }

    public interface SnoozeCallback {
        void accept(boolean enabled, int minutes, int count);
    }

    private interface ConfirmAction { boolean run(); }

    private static Dialog dialog(Activity activity) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    private static LinearLayout sheet(Activity activity, String title) {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(activity, 26), Ui.dp(activity, 24),
                Ui.dp(activity, 26), Ui.dp(activity, 18));
        content.setBackground(Ui.roundedBackground(activity,
                Color.WHITE, Color.TRANSPARENT, 42));
        TextView titleView = Ui.heading(activity, title, 20);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, Ui.dp(activity, 4), 0, Ui.dp(activity, 18));
        content.addView(titleView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return content;
    }

    private static LinearLayout pickerRow(Activity activity) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        return row;
    }

    private static LinearLayout.LayoutParams pickerParams() {
        return new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
    }

    private static NumberPicker numberPicker(Activity activity, int min, int max,
            int selected, NumberPicker.Formatter formatter) {
        NumberPicker picker = new NumberPicker(activity);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(Math.max(min, Math.min(max, selected)));
        picker.setWrapSelectorWheel(max - min < 200);
        picker.setFormatter(formatter);
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        return picker;
    }

    private static NumberPicker valuePicker(Activity activity, String[] names,
            int selected) {
        NumberPicker picker = new NumberPicker(activity);
        picker.setMinValue(0);
        picker.setMaxValue(names.length - 1);
        picker.setDisplayedValues(names);
        picker.setValue(Math.max(0, Math.min(names.length - 1, selected)));
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        return picker;
    }

    private static void addActions(Activity activity, Dialog dialog,
            LinearLayout content, String primaryText, ConfirmAction action) {
        Button primary = Ui.button(activity, primaryText);
        primary.setTextColor(Ui.COLOR_TEXT);
        primary.setTextSize(17);
        primary.setBackground(Ui.roundedBackground(activity,
                Color.TRANSPARENT, Color.BLACK, 28));
        primary.setOnClickListener(view -> {
            if (action.run()) dialog.dismiss();
        });
        LinearLayout.LayoutParams primaryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(activity, 58));
        primaryParams.setMargins(Ui.dp(activity, 48), Ui.dp(activity, 14),
                Ui.dp(activity, 48), Ui.dp(activity, 8));
        content.addView(primary, primaryParams);

        Button cancel = Ui.textButton(activity, "取消");
        cancel.setTextColor(Ui.COLOR_TEXT);
        cancel.setOnClickListener(view -> dialog.dismiss());
        content.addView(cancel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(activity, 54)));
    }

    private static void show(Activity activity, Dialog dialog, View content) {
        dialog.setContentView(content);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.width = (int) (activity.getResources()
                    .getDisplayMetrics().widthPixels * 0.90f);
            attributes.height = WindowManager.LayoutParams.WRAP_CONTENT;
            attributes.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            attributes.dimAmount = 0.42f;
            attributes.y = Ui.dp(activity, 20);
            window.setAttributes(attributes);
        }
        dialog.show();
        if (window != null) {
            window.setLayout((int) (activity.getResources()
                    .getDisplayMetrics().widthPixels * 0.90f),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private static void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private static void updateDayRange(NumberPicker day, int year, int month) {
        Calendar value = Calendar.getInstance();
        value.set(year, month - 1, 1);
        int old = day.getValue();
        day.setMaxValue(value.getActualMaximum(Calendar.DAY_OF_MONTH));
        day.setValue(Math.min(old, day.getMaxValue()));
    }

    private static int indexOf(int[] values, int target) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] == target) return index;
        }
        return 0;
    }
}
