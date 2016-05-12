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

    public abstract String getExtraData();
    public abstract void setExtraData(String data);
    public abstract boolean isObligatory();
    public abstract View getView(LayoutInflater inflater, ViewGroup parent);

}
