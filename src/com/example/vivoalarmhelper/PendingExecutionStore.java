package com.example.vivoalarmhelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.UUID;

final class PendingExecutionStore {
    static final long LIFETIME_MS = 30000L;

    private static final String PREFS = "pending_alarm_execution";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_PROFILE_ID = "profile_id";
    private static final String KEY_REQUESTED_AT = "requested_at";

    private PendingExecutionStore() {
    }

    /** Returns null when another fresh execution is already waiting. */
    static synchronized PendingExecution enqueue(
            Context context, String profileId, long requestedAt) {
        SharedPreferences preferences = preferences(context);
        PendingExecution existing = read(preferences);
        long now = System.currentTimeMillis();
        if (isFresh(existing, now)) return null;
        clear(preferences);

        PendingExecution pending = new PendingExecution(
                UUID.randomUUID().toString(), profileId, requestedAt);
        boolean saved = preferences.edit()
                .putString(KEY_TOKEN, pending.token)
                .putString(KEY_PROFILE_ID, pending.profileId)
                .putLong(KEY_REQUESTED_AT, pending.requestedAt)
                .commit();
        if (!saved) {
            throw new IllegalStateException("Unable to save pending execution");
        }
        return pending;
    }

    /** Removes before returning so a pending execution can be claimed only once. */
    static synchronized PendingExecution claim(Context context) {
        SharedPreferences preferences = preferences(context);
        PendingExecution pending = read(preferences);
        if (!isFresh(pending, System.currentTimeMillis())) {
            clear(preferences);
            return null;
        }
        if (!clear(preferences)) return null;
        return pending;
    }

    static synchronized void cancel(Context context, String token) {
        if (TextUtils.isEmpty(token)) return;
        SharedPreferences preferences = preferences(context);
        PendingExecution pending = read(preferences);
        if (pending != null && token.equals(pending.token)) {
            clear(preferences);
        }
    }

    private static boolean isFresh(PendingExecution pending, long now) {
        return pending != null && !TextUtils.isEmpty(pending.token)
                && !TextUtils.isEmpty(pending.profileId)
                && PendingExecutionPolicy.isFresh(
                        pending.requestedAt, now, LIFETIME_MS);
    }

    private static PendingExecution read(SharedPreferences preferences) {
        String token = preferences.getString(KEY_TOKEN, "");
        String profileId = preferences.getString(KEY_PROFILE_ID, "");
        long requestedAt = preferences.getLong(KEY_REQUESTED_AT, 0L);
        if (TextUtils.isEmpty(token) || TextUtils.isEmpty(profileId)) return null;
        return new PendingExecution(token, profileId, requestedAt);
    }

    private static boolean clear(SharedPreferences preferences) {
        return preferences.edit().clear().commit();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);
    }
}
