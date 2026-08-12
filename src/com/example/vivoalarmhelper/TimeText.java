package com.example.vivoalarmhelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class TimeText {
    private TimeText() {
    }

    public static String duration(int totalMinutes) {
        int safe = Math.max(1, totalMinutes);
        int hours = safe / 60;
        int minutes = safe % 60;
        if (hours == 0) return minutes + " 分钟";
        if (minutes == 0) return hours + " 小时";
        return hours + " 小时 " + minutes + " 分钟";
    }

    public static String durationAfter(int totalMinutes) {
        return duration(totalMinutes) + "后";
    }

    public static String clock(int hour, int minute) {
        return String.format(Locale.CHINA, "%02d:%02d", hour, minute);
    }

    public static String date(String storedDate) {
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .parse(storedDate);
            if (parsed == null) return storedDate;
            Calendar value = Calendar.getInstance();
            value.setTime(parsed);
            clearTime(value);
            Calendar today = Calendar.getInstance();
            clearTime(today);
            if (value.equals(today)) return "今天";
            Calendar tomorrow = (Calendar) today.clone();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            if (value.equals(tomorrow)) return "明天";
            if (value.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                return (value.get(Calendar.MONTH) + 1) + " 月 "
                        + value.get(Calendar.DAY_OF_MONTH) + " 日";
            }
            return value.get(Calendar.YEAR) + " 年 "
                    + (value.get(Calendar.MONTH) + 1) + " 月 "
                    + value.get(Calendar.DAY_OF_MONTH) + " 日";
        } catch (ParseException exception) {
            return storedDate;
        }
    }

    private static void clearTime(Calendar value) {
        value.set(Calendar.HOUR_OF_DAY, 0);
        value.set(Calendar.MINUTE, 0);
        value.set(Calendar.SECOND, 0);
        value.set(Calendar.MILLISECOND, 0);
    }
}
