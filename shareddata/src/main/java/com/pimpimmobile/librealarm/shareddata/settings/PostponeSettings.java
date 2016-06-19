package com.pimpimmobile.librealarm.shareddata.settings;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.pimpimmobile.librealarm.shareddata.R;

public class PostponeSettings extends Settings {

    private EditText mMinutesView;
    private long time = -1;

    @Override
    public String getSettingsValue() {
        if (mMinutesView != null && !TextUtils.isEmpty(mMinutesView.getText())) {
            setSettingsValue("" + Float.valueOf(mMinutesView.getText().toString()) * 60000);
        }
        return String.valueOf(time);
    }

    @Override
    public void setSettingsValue(String data) {
        if (!TextUtils.isEmpty(data)) {
            time = (long) ((float) Float.valueOf(data));
        } else {
            time = -1;
        }
        if (mMinutesView != null && data != null) mMinutesView.setText(data);
    }

    public long getTime() {
        return time;
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

}
