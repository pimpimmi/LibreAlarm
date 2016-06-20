package com.pimpimmobile.librealarm;

import android.content.Context;
import android.preference.PreferenceManager;

public class PreferencesUtil {

    public static Boolean isNsRestEnabled(Context context) {
        return getBoolean(context, "ns_rest");
    }

    public static String getNsRestUrl(Context context) {
        return getString(context, "ns_rest_uri");
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

    private static void setString(Context context, String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
    }

    private static String getString(Context context, String key) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, "");
    }
}
