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
    public long time = -1;

    @Override
    public String getExtraData() {
        if (!TextUtils.isEmpty(mMinutesView.getText().toString())) {
            setExtraData("" + Long.valueOf(mMinutesView.getText().toString()) * 60000);
        }
        return String.valueOf(time);
    }

    @Override
    public void setExtraData(String data) {
        if (TextUtils.isEmpty(data)) {
            time = -1;
        } else {
            time = Long.valueOf(data);
        }
        if (mMinutesView != null && data != null) mMinutesView.setText(data);
    }

    @Override
    public boolean isObligatory() {
        return true;
    }

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.settings_edit_text, parent, false);
        ((TextView)v.findViewById(R.id.title)).setText("Next check in");
        mMinutesView = (EditText) v.findViewById(R.id.value);
        mMinutesView.setHint("Minutes");
        mMinutesView.setInputType(EditorInfo.TYPE_NUMBER_FLAG_SIGNED);
        return v;
    }

}
