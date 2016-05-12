package com.pimpimmobile.librealarm.shareddata.settings;

import android.content.Context;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class SettingsUtils {

    public static HashMap<String, Settings> getSettings(Context context) {
        // Decide which settings to use
        HashMap<String, Settings> settingsMap =  new LinkedHashMap<>();
        settingsMap.put(PostponeSettings.class.getSimpleName(), new PostponeSettings());
        settingsMap.put(HighGlucoseSettings.class.getSimpleName(), new HighGlucoseSettings());
        settingsMap.put(LowGlucoseSettings.class.getSimpleName(), new LowGlucoseSettings());
        settingsMap.put(ConfidenceSettings.class.getSimpleName(), new ConfidenceSettings());
        settingsMap.put(SmsSettings.class.getSimpleName(), new SmsSettings());

        // Load settings saved values
        String string = PreferenceManager.getDefaultSharedPreferences(context).getString("settings", "");
        for (String s : string.split(",")) {
            String[] split = s.split(":");
            if (split.length > 1) {
                Settings settings = settingsMap.get(split[0]);
                settings.setExtraData(split[1]);
                if (split.length > 2) settings.enabled("true".equals(split[2]));
            }
        }

        return settingsMap;
    }

    public static List<AlertRule> getAlertRules(Context context) {
        List<AlertRule> list = new ArrayList<>();
        for (Settings settings : getSettings(context).values()) {
            if (settings instanceof AlertRule) list.add((AlertRule) settings);
        }
        return list;
    }

    public static void saveSettings(Context context, String string) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("settings", string).apply();
    }

    public static String createSettingsTransferString(Collection<Settings> settingsArray) {
        String string = "";
        for (Settings settings : settingsArray) {
            string += settings.getClass().getSimpleName() + ":" + settings.getExtraData() + ":" + settings.isEnabled() + ",";
        }
        return string;
    }

}
