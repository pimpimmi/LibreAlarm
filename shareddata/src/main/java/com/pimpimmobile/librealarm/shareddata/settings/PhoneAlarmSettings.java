package com.pimpimmobile.librealarm.shareddata.settings;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.pimpimmobile.librealarm.shareddata.R;

public class PhoneAlarmSettings extends Settings {

    private CheckBox mPhoneAlarmCheckbox;
    private boolean mValue;

    @Override
    public String getSettingsValue() {
        if (mPhoneAlarmCheckbox != null) setSettingsValue(mPhoneAlarmCheckbox.isChecked() ? "1" : "0");
        return mValue ? "1" : "0";
    }

    @Override
    public void setSettingsValue(String data) {
        if (!TextUtils.isEmpty(data)) {
            mValue = "1".equals(data);
            if (mPhoneAlarmCheckbox != null) mPhoneAlarmCheckbox.setChecked(mValue);
        }
    }

    public boolean isChecked() {
        return mValue;
    }

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.settings_checkbox, parent, false);
        ((TextView)v.findViewById(R.id.title)).setText(R.string.settings_phone_alarm_text);
        mPhoneAlarmCheckbox = (CheckBox) v.findViewById(R.id.settings_value);
        mPhoneAlarmCheckbox.setChecked(mValue);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPhoneAlarmCheckbox.setChecked(!mPhoneAlarmCheckbox.isChecked());
            }
        });
        return v;
    }

}
