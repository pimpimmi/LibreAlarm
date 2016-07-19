package com.pimpimmobile.librealarm.quicksettings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.pimpimmobile.librealarm.R;

public class QuickSettingsItem {

    public static final String WATCH_VALUE = "_watch_value";
    public static final String DEFAULT_VALUE = "_default_value";

    public final String title;
    public final String key;
    public String watchValue;
    public String value;
    public boolean isMmol;
    private String defaultValue;

    public QuickSettingsItem(Context context, String title, String key, String defaultValue) {
        this.title = title;
        this.key = key;
        this.defaultValue = defaultValue;
        updateValues(context);
    }

    public void updateValues(Context context) {
        Resources res = context.getResources();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.watchValue = prefs.getString(key + WATCH_VALUE, "-1");
        this.value = prefs.getString(key + DEFAULT_VALUE, defaultValue);
        this.isMmol = prefs.getBoolean(res.getString(R.string.pref_key_mmol), true);
    }
}
