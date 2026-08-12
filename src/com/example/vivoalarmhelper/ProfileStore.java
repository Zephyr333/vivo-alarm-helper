package com.example.vivoalarmhelper;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ProfileStore {
    private static final String PREFS_NAME = "alarm_profiles";
    private static final String KEY_PROFILES = "profiles_json";
    private static final String KEY_INITIALIZED = "profiles_initialized";
    private static final String KEY_CORRUPT_BACKUP = "profiles_json_corrupt_backup";
    private static final int DATA_VERSION = 2;

    private ProfileStore() {
    }

    public static synchronized void ensureInitialized(Context context) {
        SharedPreferences preferences = preferences(context);
        if (!preferences.getBoolean(KEY_INITIALIZED, false)) {
            if (preferences.contains(KEY_PROFILES)) {
                try {
                    List<AlarmProfile> migrated = parseAnyVersion(
                            preferences.getString(KEY_PROFILES, ""));
                    writeProfiles(preferences, migrated);
                    preferences.edit().putBoolean(KEY_INITIALIZED, true).commit();
                    return;
                } catch (JSONException ignored) {
                    preferences.edit().putString(KEY_CORRUPT_BACKUP,
                            preferences.getString(KEY_PROFILES, "")).commit();
                }
            }
            writeProfiles(preferences, defaultProfiles());
            preferences.edit().putBoolean(KEY_INITIALIZED, true).commit();
        }
    }

    public static synchronized List<AlarmProfile> getProfiles(Context context) {
        ensureInitialized(context);
        SharedPreferences preferences = preferences(context);
        try {
            return parseVersion2(preferences.getString(KEY_PROFILES, ""));
        } catch (JSONException exception) {
            String damaged = preferences.getString(KEY_PROFILES, "");
            preferences.edit().putString(KEY_CORRUPT_BACKUP, damaged).commit();
            List<AlarmProfile> defaults = defaultProfiles();
            writeProfiles(preferences, defaults);
            return defaults;
        }
    }

    public static synchronized AlarmProfile getProfile(Context context, String id) {
        if (id == null) return null;
        for (AlarmProfile profile : getProfiles(context)) {
            if (id.equals(profile.getId())) return profile;
        }
        return null;
    }

    public static synchronized boolean saveProfile(Context context, AlarmProfile profile) {
        List<AlarmProfile> profiles = new ArrayList<>(getProfiles(context));
        boolean replaced = false;
        for (int index = 0; index < profiles.size(); index++) {
            if (profiles.get(index).getId().equals(profile.getId())) {
                profiles.set(index, profile);
                replaced = true;
                break;
            }
        }
        if (!replaced) profiles.add(profile);
        return writeProfiles(preferences(context), profiles);
    }

    public static synchronized boolean deleteProfile(Context context, String id) {
        List<AlarmProfile> profiles = new ArrayList<>(getProfiles(context));
        boolean removed = false;
        for (int index = profiles.size() - 1; index >= 0; index--) {
            if (profiles.get(index).getId().equals(id)) {
                profiles.remove(index);
                removed = true;
            }
        }
        return removed && writeProfiles(preferences(context), profiles);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static List<AlarmProfile> defaultProfiles() {
        List<AlarmConfig> alarms = Arrays.asList(
                AlarmConfig.defaultRelative(480),
                AlarmConfig.defaultRelative(495),
                AlarmConfig.defaultRelative(500));
        return new ArrayList<>(Collections.singletonList(new AlarmProfile(
                AlarmProfile.DEFAULT_ID, "默认三闹钟", alarms)));
    }

    private static boolean writeProfiles(SharedPreferences preferences,
            List<AlarmProfile> profiles) {
        JSONObject root = new JSONObject();
        JSONArray items = new JSONArray();
        try {
            root.put("version", DATA_VERSION);
            for (AlarmProfile profile : profiles) items.put(profile.toJson());
            root.put("profiles", items);
        } catch (JSONException exception) {
            return false;
        }
        return preferences.edit().putString(KEY_PROFILES, root.toString()).commit();
    }

    private static List<AlarmProfile> parseAnyVersion(String json)
            throws JSONException {
        JSONObject root = new JSONObject(json);
        int version = root.optInt("version", 1);
        return version >= 2 ? parseVersion2(json) : migrateVersion1(root);
    }

    private static List<AlarmProfile> parseVersion2(String json)
            throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONArray items = root.getJSONArray("profiles");
        List<AlarmProfile> profiles = new ArrayList<>();
        for (int index = 0; index < items.length(); index++) {
            profiles.add(AlarmProfile.fromJson(items.getJSONObject(index)));
        }
        return profiles;
    }

    private static List<AlarmProfile> migrateVersion1(JSONObject root)
            throws JSONException {
        JSONArray items = root.getJSONArray("profiles");
        List<AlarmProfile> profiles = new ArrayList<>();
        for (int index = 0; index < items.length(); index++) {
            JSONObject item = items.getJSONObject(index);
            JSONArray offsets = item.getJSONArray("offsets");
            List<AlarmConfig> alarms = new ArrayList<>();
            for (int offsetIndex = 0; offsetIndex < offsets.length(); offsetIndex++) {
                int offset = offsets.getInt(offsetIndex);
                if (offset > 0) alarms.add(AlarmConfig.defaultRelative(offset));
            }
            if (!alarms.isEmpty()) {
                profiles.add(new AlarmProfile(item.getString("id"),
                        item.getString("name"), alarms));
            }
        }
        return profiles.isEmpty() ? defaultProfiles() : profiles;
    }
}
