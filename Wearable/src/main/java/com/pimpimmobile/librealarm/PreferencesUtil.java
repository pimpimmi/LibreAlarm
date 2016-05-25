package com.pimpimmobile.librealarm;

import android.content.Context;
import android.preference.PreferenceManager;

import com.pimpimmobile.librealarm.shareddata.Status;

public class PreferencesUtil {


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

    public static Status.Type getCurrentType(Context context) {
        return Status.Type.values()[getInt(context, "current_type", Status.Type.NOT_RUNNING.ordinal())];
    }

    public static void setCurrentType(Context context, Status.Type type) {
        setInt(context, "current_type", type.ordinal());
    }

    private static void setBoolean(Context context, String key, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).apply();
    }

    private static boolean getBoolean(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, false);
    }

    private static void setInt(Context context, String key, int value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).apply();
    }

    private static int getInt(Context context, String key, int default_) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, default_);
    }

}
