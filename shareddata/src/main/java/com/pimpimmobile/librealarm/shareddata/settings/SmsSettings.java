package com.pimpimmobile.librealarm.shareddata.settings;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.pimpimmobile.librealarm.shareddata.GlucoseData;
import com.pimpimmobile.librealarm.shareddata.R;

public class SmsSettings extends Settings implements AlertRule {

    private static final String SMS_ALERT_COUNT = "pref_sms_alert_count";

    private EditText mNumberEditText;

    @Override
    public String getExtraData() {
        return mNumberEditText.getText().toString();
    }

    @Override
    public void setExtraData(String data) {
        if (mNumberEditText != null) mNumberEditText.setText(data);
    }

    @Override
    public boolean isObligatory() {
        return false;
    }

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.settings_edit_text, parent, false);
        ((TextView)v.findViewById(R.id.title)).setText("Send SMS if Glucose below 2.0 mmol/l last hour and alarm hasn't been canceled");
        mNumberEditText = (EditText) v.findViewById(R.id.value);
        mNumberEditText.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
        mNumberEditText.setHint("Phone number");
        return v;
    }

    @Override
    public AlertResult doFilter(Context context, GlucoseData prediction) {
        // TODO
        return AlertResult.NOTHING;
    }

    @Override
    public void afterFilter(AlertResult result) {

    }
}
