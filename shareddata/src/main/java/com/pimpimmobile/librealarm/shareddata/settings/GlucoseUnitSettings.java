package com.pimpimmobile.librealarm.shareddata.settings;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.pimpimmobile.librealarm.shareddata.R;

public class GlucoseUnitSettings extends Settings {

    private Switch mSwitch;
    private boolean mChecked;

    @Override
    public String getSettingsValue() {
        if (mSwitch != null) setSettingsValue(mSwitch.isChecked() ? "1" : "0");
        return mChecked ? "1" : "0";
    }

    @Override
    public void setSettingsValue(String data) {
        if (!TextUtils.isEmpty(data))  {
            mChecked = "1".equals(data);
            if (mSwitch != null) mSwitch.setChecked(mChecked);
        }
    }

    public boolean isMmol() {
        return mChecked;
    }

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.settings_switch, parent, false);
        mSwitch = (Switch) v.findViewById(R.id.settings_value);
        mSwitch.setTextOn("mmol/l");
        mSwitch.setTextOff("mg/dl");
        mSwitch.setText(R.string.settings_glucose_units_text);
        return v;
    }

}

