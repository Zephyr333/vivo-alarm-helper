package com.example.vivoalarmhelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AlarmProfile {
    public static final String DEFAULT_ID = "default";
    public static final int TIMING_INDEPENDENT = 0;
    public static final int TIMING_SEQUENCE = 1;

    private final String id;
    private final String name;
    private final int timingMode;
    private final List<AlarmConfig> alarms;

    public AlarmProfile(String id, String name, List<AlarmConfig> alarms) {
        this(id, name, TIMING_INDEPENDENT, alarms);
    }

    public AlarmProfile(String id, String name, int timingMode,
            List<AlarmConfig> alarms) {
        this.id = id;
        this.name = name;
        this.timingMode = timingMode == TIMING_SEQUENCE
                ? TIMING_SEQUENCE : TIMING_INDEPENDENT;
        this.alarms = Collections.unmodifiableList(new ArrayList<>(alarms));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTimingMode() {
        return timingMode;
    }

    public boolean isSequence() {
        return timingMode == TIMING_SEQUENCE;
    }

    public List<AlarmConfig> getAlarms() {
        return alarms;
    }

    JSONObject toJson() throws JSONException {
        JSONObject item = new JSONObject();
        item.put("id", id);
        item.put("name", name);
        item.put("timingMode", timingMode);
        JSONArray alarmItems = new JSONArray();
        for (AlarmConfig alarm : alarms) {
            alarmItems.put(alarm.toJson());
        }
        item.put("alarms", alarmItems);
        return item;
    }

    static AlarmProfile fromJson(JSONObject item) throws JSONException {
        String id = item.getString("id");
        String name = item.getString("name");
        JSONArray alarmItems = item.getJSONArray("alarms");
        List<AlarmConfig> alarms = new ArrayList<>();
        for (int index = 0; index < alarmItems.length(); index++) {
            alarms.add(AlarmConfig.fromJson(alarmItems.getJSONObject(index)));
        }
        if (id.trim().isEmpty() || name.trim().isEmpty() || alarms.isEmpty()) {
            throw new JSONException("Invalid profile");
        }
        return new AlarmProfile(id, name,
                item.optInt("timingMode", TIMING_INDEPENDENT), alarms);
    }
}
