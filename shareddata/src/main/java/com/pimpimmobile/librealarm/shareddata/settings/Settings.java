package com.pimpimmobile.librealarm.shareddata.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class Settings {

    public abstract String getSettingsValue();
    public abstract void setSettingsValue(String data);
    public abstract View getView(LayoutInflater inflater, ViewGroup parent);

}
