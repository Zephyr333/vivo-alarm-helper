package com.example.vivoalarmhelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AlarmConfig {
    public static final int TIME_RELATIVE = 0;
    public static final int TIME_ABSOLUTE = 1;

    public static final int REPEAT_ONCE = 0;
    public static final int REPEAT_DAILY = 1;
    public static final int REPEAT_LEGAL_WORKDAY = 2;
    public static final int REPEAT_HOLIDAY_WEEKEND = 3;
    public static final int REPEAT_ODD_EVEN_WORKDAY = 4;
    public static final int REPEAT_SHIFT_WORKDAY = 5;
    public static final int REPEAT_CUSTOM = 6;

    public static final int RING_INTERNAL = 0;
    public static final int RING_FOLLOW_DEFAULT = 1;
    public static final int RING_WEATHER = 2;
    public static final int RING_CUSTOM = 3;
    public static final int RING_SILENT = 4;

    public static final String DEFAULT_BROADCAST_CONTENT = "A1B1C0D1";

    public final String id;
    public final int timeMode;
    public final int offsetMinutes;
    public final int hour;
    public final int minute;
    public final String date;
    public final int repeatType;
    public final int customDays;
    public final boolean excludeHolidays;
    public final int oddEvenThisWeek;
    public final int shiftToday;
    public final int shiftDaysCount;
    public final List<ShiftDay> shiftDays;
    public final String label;
    public final int ringtoneType;
    public final String alertUri;
    public final String ringtoneName;
    public final int vibrateMode;
    public final boolean deleteAfterRing;
    public final boolean snoozeEnabled;
    public final int snoozeMinutes;
    public final int snoozeCount;
    public final boolean snoozeTalker;
    public final boolean voiceBroadcast;
    public final String broadcastContent;

    private AlarmConfig(Builder builder) {
        id = builder.id;
        timeMode = builder.timeMode;
        offsetMinutes = builder.offsetMinutes;
        hour = builder.hour;
        minute = builder.minute;
        date = builder.date;
        repeatType = builder.repeatType;
        customDays = builder.customDays;
        excludeHolidays = supportsHolidaySkip(builder.repeatType)
                && builder.excludeHolidays;
        oddEvenThisWeek = builder.oddEvenThisWeek;
        shiftToday = builder.shiftToday;
        shiftDaysCount = builder.shiftDaysCount;
        shiftDays = new ArrayList<>(builder.shiftDays);
        label = builder.label;
        ringtoneType = builder.ringtoneType;
        alertUri = builder.alertUri;
        ringtoneName = builder.ringtoneName;
        vibrateMode = normalizeVibration(builder.ringtoneType, builder.vibrateMode);
        deleteAfterRing = repeatType == REPEAT_ONCE && builder.deleteAfterRing;
        snoozeEnabled = builder.snoozeEnabled;
        snoozeMinutes = builder.snoozeMinutes;
        snoozeCount = builder.snoozeCount;
        snoozeTalker = builder.snoozeEnabled && builder.snoozeTalker;
        voiceBroadcast = builder.voiceBroadcast;
        broadcastContent = builder.broadcastContent;
    }

    public static AlarmConfig defaultRelative(int offsetMinutes) {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.DAY_OF_YEAR, 1);
        return new Builder()
                .id(UUID.randomUUID().toString())
                .offsetMinutes(offsetMinutes)
                .hour(now.get(Calendar.HOUR_OF_DAY))
                .minute(now.get(Calendar.MINUTE))
                .date(new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        .format(now.getTime()))
                .build();
    }

    public Builder buildUpon() {
        return new Builder(this);
    }

    public boolean isRepeating() {
        return repeatType != REPEAT_ONCE;
    }

    public int vivoRepeat() {
        if (repeatType == REPEAT_LEGAL_WORKDAY) return 6;
        if (repeatType == REPEAT_HOLIDAY_WEEKEND) return 7;
        if (repeatType == REPEAT_ODD_EVEN_WORKDAY) return 1;
        if (repeatType == REPEAT_SHIFT_WORKDAY) return 5;
        return 0;
    }

    public int vivoDaysOfWeek() {
        if (repeatType == REPEAT_DAILY) return 127;
        if (repeatType == REPEAT_LEGAL_WORKDAY) return 31;
        if (repeatType == REPEAT_HOLIDAY_WEEKEND) return 96;
        if (repeatType == REPEAT_ODD_EVEN_WORKDAY) {
            return oddEvenThisWeek == 2 ? 63 : 31;
        }
        if (repeatType == REPEAT_CUSTOM) return customDays;
        return 0;
    }

    public String shiftTimes() {
        StringBuilder value = new StringBuilder();
        for (int index = 0; index < shiftDays.size(); index++) {
            if (index > 0) value.append(',');
            ShiftDay day = shiftDays.get(index);
            value.append(day.hour).append(':').append(day.minute);
        }
        return value.toString();
    }

    public String shiftEnabled() {
        StringBuilder value = new StringBuilder();
        for (int index = 0; index < shiftDays.size(); index++) {
            if (index > 0) value.append(',');
            value.append(shiftDays.get(index).enabled ? '1' : '0');
        }
        return value.toString();
    }

    JSONObject toJson() throws JSONException {
        JSONObject item = new JSONObject();
        item.put("id", id);
        item.put("timeMode", timeMode);
        item.put("offsetMinutes", offsetMinutes);
        item.put("hour", hour);
        item.put("minute", minute);
        item.put("date", date);
        item.put("repeatType", repeatType);
        item.put("customDays", customDays);
        item.put("excludeHolidays", excludeHolidays);
        item.put("oddEvenThisWeek", oddEvenThisWeek);
        item.put("shiftToday", shiftToday);
        item.put("shiftDaysCount", shiftDaysCount);
        JSONArray shifts = new JSONArray();
        for (ShiftDay day : shiftDays) shifts.put(day.toJson());
        item.put("shiftDays", shifts);
        item.put("label", label);
        item.put("ringtoneType", ringtoneType);
        item.put("alertUri", alertUri);
        item.put("ringtoneName", ringtoneName);
        item.put("vibrateMode", vibrateMode);
        item.put("deleteAfterRing", deleteAfterRing);
        item.put("snoozeEnabled", snoozeEnabled);
        item.put("snoozeMinutes", snoozeMinutes);
        item.put("snoozeCount", snoozeCount);
        item.put("snoozeTalker", snoozeTalker);
        item.put("voiceBroadcast", voiceBroadcast);
        item.put("broadcastContent", broadcastContent);
        return item;
    }

    public String toJsonString() {
        try {
            return toJson().toString();
        } catch (JSONException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static AlarmConfig fromJsonString(String json) throws JSONException {
        return fromJson(new JSONObject(json));
    }

    static AlarmConfig fromJson(JSONObject item) throws JSONException {
        Builder builder = new Builder()
                .id(item.optString("id", UUID.randomUUID().toString()))
                .timeMode(item.optInt("timeMode", TIME_RELATIVE))
                .offsetMinutes(item.optInt("offsetMinutes", 60))
                .hour(item.optInt("hour", 7))
                .minute(item.optInt("minute", 0))
                .date(item.optString("date", nextDate()))
                .repeatType(item.optInt("repeatType", REPEAT_ONCE))
                .customDays(item.optInt("customDays", 31))
                .excludeHolidays(item.optBoolean("excludeHolidays", false))
                .oddEvenThisWeek(item.optInt("oddEvenThisWeek", 1))
                .shiftToday(item.optInt("shiftToday", 1))
                .shiftDaysCount(item.optInt("shiftDaysCount", 4))
                .label(item.optString("label", ""))
                .ringtoneType(item.optInt("ringtoneType", RING_FOLLOW_DEFAULT))
                .alertUri(item.optString("alertUri", ""))
                .ringtoneName(item.optString("ringtoneName", "默认铃声"))
                .vibrateMode(item.optInt("vibrateMode", 2))
                .deleteAfterRing(item.optBoolean("deleteAfterRing", true))
                .snoozeEnabled(item.optBoolean("snoozeEnabled", false))
                .snoozeMinutes(item.optInt("snoozeMinutes", 5))
                .snoozeCount(item.optInt("snoozeCount", 5))
                .snoozeTalker(item.optBoolean("snoozeTalker", false))
                .voiceBroadcast(item.optBoolean("voiceBroadcast", false))
                .broadcastContent(item.optString(
                        "broadcastContent", DEFAULT_BROADCAST_CONTENT));
        JSONArray shifts = item.optJSONArray("shiftDays");
        List<ShiftDay> days = new ArrayList<>();
        if (shifts != null) {
            for (int index = 0; index < shifts.length(); index++) {
                days.add(ShiftDay.fromJson(shifts.getJSONObject(index)));
            }
        }
        builder.shiftDays(days);
        return builder.build();
    }

    private static String nextDate() {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(tomorrow.getTime());
    }

    private static int normalizeVibration(int ringtoneType, int mode) {
        if (mode == 2 && (ringtoneType == RING_CUSTOM
                || ringtoneType == RING_WEATHER
                || ringtoneType == RING_SILENT)) {
            return 1;
        }
        return mode;
    }

    private static boolean supportsHolidaySkip(int repeatType) {
        return repeatType == REPEAT_DAILY
                || repeatType == REPEAT_ODD_EVEN_WORKDAY
                || repeatType == REPEAT_SHIFT_WORKDAY
                || repeatType == REPEAT_CUSTOM;
    }

    public static final class ShiftDay {
        public final int hour;
        public final int minute;
        public final boolean enabled;

        public ShiftDay(int hour, int minute, boolean enabled) {
            this.hour = hour;
            this.minute = minute;
            this.enabled = enabled;
        }

        JSONObject toJson() throws JSONException {
            JSONObject item = new JSONObject();
            item.put("hour", hour);
            item.put("minute", minute);
            item.put("enabled", enabled);
            return item;
        }

        static ShiftDay fromJson(JSONObject item) {
            return new ShiftDay(item.optInt("hour", 7),
                    item.optInt("minute", 0), item.optBoolean("enabled", true));
        }
    }

    public static final class Builder {
        private String id = UUID.randomUUID().toString();
        private int timeMode = TIME_RELATIVE;
        private int offsetMinutes = 60;
        private int hour = 7;
        private int minute;
        private String date = nextDate();
        private int repeatType = REPEAT_ONCE;
        private int customDays = 31;
        private boolean excludeHolidays;
        private int oddEvenThisWeek = 1;
        private int shiftToday = 1;
        private int shiftDaysCount = 4;
        private List<ShiftDay> shiftDays = new ArrayList<>();
        private String label = "";
        private int ringtoneType = RING_FOLLOW_DEFAULT;
        private String alertUri = "";
        private String ringtoneName = "默认铃声";
        private int vibrateMode = 2;
        private boolean deleteAfterRing = true;
        private boolean snoozeEnabled;
        private int snoozeMinutes = 5;
        private int snoozeCount = 5;
        private boolean snoozeTalker;
        private boolean voiceBroadcast;
        private String broadcastContent = DEFAULT_BROADCAST_CONTENT;

        public Builder() {
            ensureShiftDays();
        }

        Builder(AlarmConfig source) {
            id = source.id;
            timeMode = source.timeMode;
            offsetMinutes = source.offsetMinutes;
            hour = source.hour;
            minute = source.minute;
            date = source.date;
            repeatType = source.repeatType;
            customDays = source.customDays;
            excludeHolidays = source.excludeHolidays;
            oddEvenThisWeek = source.oddEvenThisWeek;
            shiftToday = source.shiftToday;
            shiftDaysCount = source.shiftDaysCount;
            shiftDays = new ArrayList<>(source.shiftDays);
            label = source.label;
            ringtoneType = source.ringtoneType;
            alertUri = source.alertUri;
            ringtoneName = source.ringtoneName;
            vibrateMode = source.vibrateMode;
            deleteAfterRing = source.deleteAfterRing;
            snoozeEnabled = source.snoozeEnabled;
            snoozeMinutes = source.snoozeMinutes;
            snoozeCount = source.snoozeCount;
            snoozeTalker = source.snoozeTalker;
            voiceBroadcast = source.voiceBroadcast;
            broadcastContent = source.broadcastContent;
        }

        public Builder id(String value) { id = value; return this; }
        public Builder timeMode(int value) { timeMode = value; return this; }
        public Builder offsetMinutes(int value) { offsetMinutes = value; return this; }
        public Builder hour(int value) { hour = value; return this; }
        public Builder minute(int value) { minute = value; return this; }
        public Builder date(String value) { date = value; return this; }
        public Builder repeatType(int value) { repeatType = value; return this; }
        public Builder customDays(int value) { customDays = value; return this; }
        public Builder excludeHolidays(boolean value) { excludeHolidays = value; return this; }
        public Builder oddEvenThisWeek(int value) { oddEvenThisWeek = value; return this; }
        public Builder shiftToday(int value) { shiftToday = value; return this; }
        public Builder shiftDaysCount(int value) { shiftDaysCount = value; return this; }
        public Builder shiftDays(List<ShiftDay> value) {
            shiftDays = new ArrayList<>(value);
            return this;
        }
        public Builder label(String value) { label = value == null ? "" : value; return this; }
        public Builder ringtoneType(int value) { ringtoneType = value; return this; }
        public Builder alertUri(String value) { alertUri = value == null ? "" : value; return this; }
        public Builder ringtoneName(String value) { ringtoneName = value == null ? "" : value; return this; }
        public Builder vibrateMode(int value) { vibrateMode = value; return this; }
        public Builder deleteAfterRing(boolean value) { deleteAfterRing = value; return this; }
        public Builder snoozeEnabled(boolean value) { snoozeEnabled = value; return this; }
        public Builder snoozeMinutes(int value) { snoozeMinutes = value; return this; }
        public Builder snoozeCount(int value) { snoozeCount = value; return this; }
        public Builder snoozeTalker(boolean value) { snoozeTalker = value; return this; }
        public Builder voiceBroadcast(boolean value) { voiceBroadcast = value; return this; }
        public Builder broadcastContent(String value) {
            broadcastContent = value == null || value.isEmpty()
                    ? DEFAULT_BROADCAST_CONTENT : value;
            return this;
        }

        public AlarmConfig build() {
            if (timeMode != TIME_RELATIVE && timeMode != TIME_ABSOLUTE) {
                timeMode = TIME_RELATIVE;
            }
            offsetMinutes = Math.max(1, Math.min(525600, offsetMinutes));
            hour = Math.max(0, Math.min(23, hour));
            minute = Math.max(0, Math.min(59, minute));
            customDays &= 127;
            oddEvenThisWeek = oddEvenThisWeek == 2 ? 2 : 1;
            shiftDaysCount = Math.max(2, Math.min(62, shiftDaysCount));
            shiftToday = Math.max(1, Math.min(shiftDaysCount, shiftToday));
            ensureShiftDays();
            if (repeatType == REPEAT_CUSTOM && customDays == 0) customDays = 31;
            if (repeatType == REPEAT_SHIFT_WORKDAY) {
                boolean enabled = false;
                for (ShiftDay day : shiftDays) enabled |= day.enabled;
                if (!enabled) {
                    ShiftDay first = shiftDays.get(0);
                    shiftDays.set(0, new ShiftDay(first.hour, first.minute, true));
                }
            }
            if (snoozeMinutes != 5 && snoozeMinutes != 10
                    && snoozeMinutes != 15 && snoozeMinutes != 30) snoozeMinutes = 5;
            if (snoozeCount != 2 && snoozeCount != 3
                    && snoozeCount != 5 && snoozeCount != 10) snoozeCount = 5;
            if (label.codePointCount(0, label.length()) > 56) {
                int end = label.offsetByCodePoints(0, 56);
                label = label.substring(0, end);
            }
            return new AlarmConfig(this);
        }

        private void ensureShiftDays() {
            while (shiftDays.size() < shiftDaysCount) {
                int index = shiftDays.size();
                shiftDays.add(new ShiftDay(hour, minute, index < shiftDaysCount - 1));
            }
            while (shiftDays.size() > shiftDaysCount) {
                shiftDays.remove(shiftDays.size() - 1);
            }
        }
    }
}
