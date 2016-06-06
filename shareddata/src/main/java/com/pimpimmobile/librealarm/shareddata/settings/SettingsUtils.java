package com.pimpimmobile.librealarm.shareddata.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class SettingsUtils {

    public static HashMap<String, Settings> getAllSettings(Context context) {
        // Decide which settings to use
        HashMap<String, Settings> settingsMap =  new LinkedHashMap<>();
        settingsMap.put(PostponeSettings.class.getSimpleName(), new PostponeSettings());
        settingsMap.put(HighGlucoseSettings.class.getSimpleName(), new HighGlucoseSettings());
        settingsMap.put(LowGlucoseSettings.class.getSimpleName(), new LowGlucoseSettings());
        settingsMap.put(ConfidenceSettings.class.getSimpleName(), new ConfidenceSettings());

        // Load settings saved values
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        for (String key : settingsMap.keySet()) {
            if (preferences.contains(key)) {
                settingsMap.get(key).setSettingsValue(preferences.getString(key, null));
            }
        }

        return settingsMap;
    }

    public static Settings getSettings(Context context, String key) {
        return getAllSettings(context).get(key);
    }

    public static List<AlertRule> getAlertRules(Context context) {
        List<AlertRule> list = new ArrayList<>();
        for (Settings settings : getAllSettings(context).values()) {
            if (settings instanceof AlertRule) list.add((AlertRule) settings);
        }
        return list;
    }

    public static void saveSettings(Context context, HashMap<String, String> map) {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        for (String key : map.keySet()) {
            edit.putString(key, map.get(key));
        }
        edit.apply();
    }

    public static void saveSettings(Context context, String key, String value) {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(key, value);
        edit.apply();
    }

    public static HashMap<String, String> getTransferHashMap(HashMap<String, Settings> settingsArray) {
        HashMap<String, String> map = new HashMap<>();
        for (String key : settingsArray.keySet()) {
            map.put(key, settingsArray.get(key).getSettingsValue());
        }
        return map;
    }

}
