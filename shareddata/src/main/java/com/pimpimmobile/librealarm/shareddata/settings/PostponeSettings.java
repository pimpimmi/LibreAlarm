package com.pimpimmobile.librealarm.shareddata.settings;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.pimpimmobile.librealarm.shareddata.GlucoseData;
import com.pimpimmobile.librealarm.shareddata.R;

public class PostponeSettings extends Settings implements AlertRule {

    private EditText mMinutesView;
    private long mAlarmTime = -1;

    @Override
    public String getSettingsValue() {
        return String.valueOf(mAlarmTime);
    }

    @Override
    public void setSettingsValue(String data) {
        if (!TextUtils.isEmpty(data)) {
            mAlarmTime = Long.valueOf(data);
        } else {
            mAlarmTime = -1;
        }
    }

    public void setValueFromView() {
        if (mMinutesView != null && !TextUtils.isEmpty(mMinutesView.getText())) {
            setSettingsValue("" + (((long) (Float.valueOf(mMinutesView.getText().toString()) * 60000)) + System.currentTimeMillis()));
        }
    }

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.settings_edit_text, parent, false);
        ((TextView)v.findViewById(R.id.title)).setText(R.string.settings_postpone_text);
        mMinutesView = (EditText) v.findViewById(R.id.settings_value);
        mMinutesView.setHint(R.string.minutes);
        mMinutesView.setInputType(EditorInfo.TYPE_NUMBER_FLAG_SIGNED);
        return v;
    }

    @Override
    public AlertResult doFilter(Context context, GlucoseData prediction) {
        return mAlarmTime < System.currentTimeMillis() ? AlertResult.NOTHING : AlertResult.NO_ALERTS;
    }

}
