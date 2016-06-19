package com.pimpimmobile.librealarm.shareddata.settings;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.pimpimmobile.librealarm.shareddata.GlucoseData;
import com.pimpimmobile.librealarm.shareddata.R;

public class LowGlucoseSettings extends Settings implements AlertRule {

    private EditText mLowGlucoseEditText;
    private float glucose = 3.5F;

    @Override
    public String getSettingsValue() {
        if (mLowGlucoseEditText != null) setSettingsValue(mLowGlucoseEditText.getText().toString());
        return String.valueOf(glucose);
    }

    @Override
    public void setSettingsValue(String data) {
        if (!TextUtils.isEmpty(data)) {
            glucose = Float.valueOf(data);
            if (mLowGlucoseEditText != null) mLowGlucoseEditText.setText(String.valueOf(glucose));
        }
    }

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.settings_edit_text, parent, false);
        ((TextView)v.findViewById(R.id.title)).setText(R.string.settings_low_glucose_text);
        mLowGlucoseEditText = (EditText) v.findViewById(R.id.settings_value);
        mLowGlucoseEditText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        boolean isMmol = ((GlucoseUnitSettings)SettingsUtils
                .getSettings(v.getContext(), GlucoseUnitSettings.class.getSimpleName())).isMmol();
        mLowGlucoseEditText.setHint(isMmol ? "mmol/l" : "mg/dl");
        mLowGlucoseEditText.setText(String.valueOf(glucose));
        return v;
    }

    @Override
    public AlertResult doFilter(Context context, GlucoseData prediction) {
        return prediction.glucoseLevel > glucose * 18 ? AlertResult.NOTHING : AlertResult.ALERT;
    }

    @Override
    public void afterFilter(AlertResult result) {

    }
}
