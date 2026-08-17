package com.example.vivoalarmhelper;

import java.util.List;

/** Resolves the user-facing profile time arrangement into effective offsets. */
public final class ProfileTiming {
    public static final int MAX_OFFSET_MINUTES = 525600;

    private ProfileTiming() {
    }

    public static int effectiveRelativeOffsetMinutes(
            AlarmProfile profile, int alarmIndex) {
        if (profile.isSequence()) {
            throw new IllegalArgumentException(
                    "Execution-time sequence has no saved base offset");
        }
        List<AlarmConfig> alarms = profile.getAlarms();
        if (alarmIndex < 0 || alarmIndex >= alarms.size()) {
            throw new IllegalArgumentException("Invalid alarm index");
        }
        AlarmConfig selected = alarms.get(alarmIndex);
        if (selected.timeMode != AlarmConfig.TIME_RELATIVE) {
            throw new IllegalArgumentException("Alarm is not relative");
        }
        return SequenceOffsets.independentOffset(
                selected.offsetMinutes, MAX_OFFSET_MINUTES);
    }

    public static int elapsedAfterRuntimeBaseMinutes(
            AlarmProfile profile, int alarmIndex) {
        if (!profile.isSequence()) {
            throw new IllegalArgumentException("Profile is not a sequence");
        }
        int[] intervals = new int[profile.getAlarms().size()];
        for (int index = 1; index < intervals.length; index++) {
            intervals[index] = profile.getAlarms().get(index).offsetMinutes;
        }
        return SequenceOffsets.elapsedAfterBase(
                intervals, alarmIndex, MAX_OFFSET_MINUTES);
    }

    public static String sequenceProblem(AlarmProfile profile) {
        if (!profile.isSequence()) return null;
        int[] storedOffsets = new int[profile.getAlarms().size()];
        for (int index = 0; index < profile.getAlarms().size(); index++) {
            AlarmConfig alarm = profile.getAlarms().get(index);
            if (alarm.timeMode != AlarmConfig.TIME_RELATIVE) {
                return "第 " + (index + 1)
                        + " 个闹钟不是“从现在起”，不能用于时间序列";
            }
            if (alarm.repeatType == AlarmConfig.REPEAT_SHIFT_WORKDAY) {
                return "第 " + (index + 1)
                        + " 个闹钟使用了轮班制，不能用于时间序列";
            }
            if (index > 0) storedOffsets[index] = alarm.offsetMinutes;
        }
        if (SequenceOffsets.sequenceExceedsLimit(
                storedOffsets, MAX_OFFSET_MINUTES)) {
            return "全部间隔超过 8760 小时，请缩短后续间隔";
        }
        return null;
    }
}
