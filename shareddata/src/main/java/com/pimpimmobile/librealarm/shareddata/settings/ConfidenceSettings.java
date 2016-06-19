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
import com.pimpimmobile.librealarm.shareddata.PredictionData;
import com.pimpimmobile.librealarm.shareddata.R;

public class ConfidenceSettings extends Settings implements AlertRule {

    private EditText mConfidenceEditText;
    private float confidence = 1;

    @Override
    public String getSettingsValue() {
        if (mConfidenceEditText != null) setSettingsValue(mConfidenceEditText.getText().toString());
        return String.valueOf(confidence);
    }

    @Override
    public void setSettingsValue(String data) {
        if (!TextUtils.isEmpty(data)) {
            confidence = Float.valueOf(data);
            if (mConfidenceEditText != null) mConfidenceEditText.setText(String.valueOf(confidence));
        }
    }

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.settings_edit_text, parent, false);
        ((TextView)v.findViewById(R.id.title)).setText(R.string.settings_confidence_text);
        mConfidenceEditText = (EditText) v.findViewById(R.id.settings_value);
        mConfidenceEditText.setText(String.valueOf(confidence));
        mConfidenceEditText.setHint(R.string.settings_confidence_hint);
        mConfidenceEditText.setInputType(EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);
        return v;
    }

    @Override
    public AlertResult doFilter(Context context, GlucoseData prediction) {
        return ((PredictionData) prediction).confidence < confidence
                ? AlertResult.NOTHING : AlertResult.NO_ALERTS;
    }

    @Override
    public void afterFilter(AlertResult result) {

    }
}
