package com.pimpimmobile.librealarm.shareddata.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class Settings {

    private boolean mIsChecked;

    public void enabled(boolean isChecked) {
        mIsChecked = isChecked;
    }

    public boolean isEnabled() {
        return isObligatory() || mIsChecked;
    }

    public abstract String getSettingsValue();
    public abstract void setSettingsValue(String data);
    // Settings return true if the user should be able to disable this setting (not used at the moment)
    public abstract boolean isObligatory();
    public abstract View getView(LayoutInflater inflater, ViewGroup parent);

}
