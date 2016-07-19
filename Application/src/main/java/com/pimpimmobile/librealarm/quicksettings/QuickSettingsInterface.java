package com.pimpimmobile.librealarm.quicksettings;

public interface QuickSettingsInterface {
    void setUpdateListener(QuickSettingsView.QuickSettingsChangeListener listener);
    void setItem(QuickSettingsItem item);
    QuickSettingsItem getItem();
    void updateWatchValues();
    boolean saveSettings();
    String getKey();
    String getValue();
}
