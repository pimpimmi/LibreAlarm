package com.pimpimmobile.librealarm.quicksettings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.pimpimmobile.librealarm.R;

import java.util.HashMap;

/**
 * View shown in navigation bar.
 */
public class QuickSettingsView extends LinearLayout {

    public QuickSettingsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);

        // High glucose snooze
        addView(context, new SnoozeAlarmView(context), R.string.settings_snooze_high_title,
                R.string.key_snooze_high, R.integer.snooze_high_default);

        // Low glucose snooze
        addView(context, new SnoozeAlarmView(context), R.string.settings_snooze_low_title,
                R.string.key_snooze_low, R.integer.snooze_low_default);

        // High glucose level
        addView(context, new GlucoseLevelView(context), R.string.settings_alarm_high_title,
                R.string.key_alarm_limit_high, R.integer.alarm_high_default);

        // Low glucose level
        addView(context, new GlucoseLevelView(context), R.string.settings_alarm_low_title,
                R.string.key_alarm_limit_low, R.integer.alarm_low_default);
    }

    public void refresh() {
        for (int i = 0; i < getChildCount(); i++) {
            QuickSettingsInterface settings = ((QuickSettingsInterface) getChildAt(i));
            QuickSettingsItem item = settings.getItem();
            item.updateValues(getContext());
            settings.setItem(item);
        }
    }

    public void setUpdateListener(QuickSettingsChangeListener listener) {
        for (int i = 0; i < getChildCount(); i++) {
            ((QuickSettingsInterface) getChildAt(i)).setUpdateListener(listener);
        }
    }

    public void watchValuesUpdated() {
        for (int i = 0; i < getChildCount(); i++) {
            ((QuickSettingsInterface) getChildAt(i)).updateWatchValues();
        }
    }

    public HashMap<String, String> saveSettings() {
        HashMap<String, String> saved = new HashMap<>();
        for (int i = 0; i < getChildCount(); i++) {
            QuickSettingsInterface settings = (QuickSettingsInterface) getChildAt(i);
            if (settings.saveSettings()) {
                saved.put(settings.getKey(), settings.getValue());
            }
        }
        return saved;
    }

    private void addView(Context context, View view, int titleId, int keyId, int defaultValueId) {
        String key = context.getString(keyId);
        String title = context.getString(titleId);
        String defaultValue = String.valueOf(context.getResources().getInteger(defaultValueId));
        QuickSettingsItem item = new QuickSettingsItem(context, title, key, defaultValue);
        ((QuickSettingsInterface)view).setItem(item);
        addView(view);
    }

    public interface QuickSettingsChangeListener {
        void onQuickSettingsChanged(String key, String value);
    }

}
