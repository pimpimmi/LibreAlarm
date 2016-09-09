package com.pimpimmobile.librealarm.shareddata;

import android.content.Context;
import android.preference.PreferenceManager;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

public class PreferencesUtil {

    // Used on phone
    public static Boolean isNsRestEnabled(Context context) {
        return getBoolean(context, "ns_rest");
    }

    // Used on phone
    public static Boolean isXdripPlusEnabled(Context context) {
        return getBoolean(context, "xdrip_plus_broadcast");
    }

    public static String getNsRestUrl(Context context) {
        return getString(context, "ns_rest_uri");
    }
    // End used on phone

    // Used on watch
    public static void setIsStarted(Context context, boolean started) {
        setBoolean(context, "startstopflag", started);
    }

    public static boolean getIsStarted(Context context) {
        return getBoolean(context, "startstopflag");
    }

    public static void setRetries(Context context, int attempts) {
        setInt(context, "retries", attempts);
    }

    public static int getRetries(Context context) {
        return getInt(context, "retries", 1);
    }

    public static long getLastBoot(Context context) {
        return getLong(context, "last_boot");
    }

    public static void setLastBoot(Context context, long time) {
        setLong(context, "last_boot", time);
    }

    public static Status.Type getCurrentType(Context context) {
        return Status.Type.values()[getInt(context, "current_type", Status.Type.NOT_RUNNING.ordinal())];
    }

    public static void setCurrentType(Context context, Status.Type type) {
        setInt(context, "current_type", type.ordinal());
    }

    public static long errInRowForAlarm(Context context) {
        if ("true".equals(getString(context, context.getString(R.string.pref_key_err_alarm_enabled), "false"))) {
            return Long.valueOf(getString(context, context.getString(R.string.pref_key_err_alarm), "2"));
        }
        return Long.MAX_VALUE;
    }

    public static int increaseErrorsInARow(Context context) {
        int errorsInRow = getInt(context, "errors_in_a_row", 0) + 1;
        setInt(context, "errors_in_a_row", errorsInRow);
        return errorsInRow;
    }

    public static void resetErrorsInARow(Context context) {
        setInt(context, "errors_in_a_row", 0);
    }
    // End used in watch

    public static void setBoolean(Context context, String key, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).apply();
    }

    public static boolean getBoolean(Context context, String key) {
        return getBoolean(context, key, false);
    }

    public static boolean getBoolean(Context context, String key, boolean default_) {
        // booleans get stored on watch as strings due to the way data is synced
        try {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, default_);
        } catch (ClassCastException e) {
            return PreferencesUtil.getString(context, key).equals("true");
        }
    }

    public static void setInt(Context context, String key, int value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).apply();
    }

    public static int getInt(Context context, int id) {
        return getInt(context, context.getString(id), -1);
    }

    public static int getInt(Context context, String key, int default_) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, default_);
    }

    public static void setLong(Context context, String key, long value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(key, value).apply();
    }

    public static long getLong(Context context, int id) {
        return getLong(context, context.getString(id));
    }

    public static long getLong(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(key, -1);
    }

    public static String getString(Context context, int id) {
        return getString(context, context.getString(id));
    }

    public static String getString(Context context, String key) {
        return getString(context, key, "-1");
    }

    public static String getString(Context context, int keyId, String default_) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(keyId), default_);
    }

    public static String getString(Context context, String key, String default_) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, default_);
    }

    public static void putString(Context context, String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
    }

    public static float getFloat(Context context, int id) {
        return getFloat(context, context.getString(id));
    }

    public static float getFloat(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getFloat(key, -1);
    }

    public static String toString(HashMap<String, String> prefs) {
        JsonObject object = new JsonObject();
        for (String key : prefs.keySet()) {
            object.addProperty(key, prefs.get(key));
        }
        return object.toString();
    }

    public static HashMap<String, String> toMap(String prefs) {
        HashMap<String, String> map = new HashMap<>();
        try {
            JSONObject object = new JSONObject(prefs);
            Iterator<String> iterator = object.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                map.put(key, object.getString(key));
            }
        } catch (JSONException e) {
            throw new RuntimeException("Something wrong with JSON");
        }
        return map;
    }
}
