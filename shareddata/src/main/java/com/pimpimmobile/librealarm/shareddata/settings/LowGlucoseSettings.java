package com.pimpimmobile.librealarm.shareddata.settings;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.pimpimmobile.librealarm.shareddata.GlucoseData;
import com.pimpimmobile.librealarm.shareddata.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class LowGlucoseSettings extends Settings implements AlertRule, GlucoseUnitSettings.GlucoseUnitSettingsListener {

    private static final DecimalFormat mFormat = new DecimalFormat("##.0", new DecimalFormatSymbols(Locale.UK));

    private EditText mLowGlucoseEditText;
    private float mGlucose = 63f;
    private boolean mIsMmol;

    public LowGlucoseSettings(Context context) {
        mIsMmol = "1".equals(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(GlucoseUnitSettings.class.getSimpleName(), "1"));
    }

    @Override
    public String getSettingsValue() {
        if (mLowGlucoseEditText != null) {
            float glucose = Float.valueOf(mLowGlucoseEditText.getText().toString()) * (mIsMmol ? 18 : 1);
            setSettingsValue(String.valueOf(glucose));
        }
        return String.valueOf(mGlucose);
    }

    @Override
    public void setSettingsValue(String data) {
        if (!TextUtils.isEmpty(data)) {
            mGlucose = Float.valueOf(data);
            updateView();
        }
    }

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.settings_edit_text, parent, false);
        ((TextView)v.findViewById(R.id.title)).setText(R.string.settings_low_glucose_text);
        mLowGlucoseEditText = (EditText) v.findViewById(R.id.settings_value);
        mLowGlucoseEditText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        mLowGlucoseEditText.setHint(mIsMmol ? "mmol/l" : "mg/dl");
        updateView();
        return v;
    }

    @Override
    public AlertResult doFilter(Context context, GlucoseData prediction) {
        return prediction.glucoseLevel > mGlucose ? AlertResult.NOTHING : AlertResult.ALERT_LOW;
    }

    @Override
    public void setIsMmol(boolean isMmol) {
        mIsMmol = isMmol;
        updateView();
    }

    private void updateView() {
        if (mLowGlucoseEditText != null) {
            mLowGlucoseEditText.setText(mIsMmol ? mFormat.format(mGlucose / 18) : String.valueOf(mGlucose));
        }
    }
}
