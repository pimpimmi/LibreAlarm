package com.pimpimmobile.librealarm.shareddata.settings;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.pimpimmobile.librealarm.shareddata.GlucoseData;
import com.pimpimmobile.librealarm.shareddata.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class HighGlucoseSettings extends Settings implements AlertRule, GlucoseUnitSettings.GlucoseUnitSettingsListener {

    private static final DecimalFormat mFormat = new DecimalFormat("##.0", new DecimalFormatSymbols(Locale.UK));

    private EditText mGlucoseEditView;
    private float mGlucose = 180f;

    private boolean mIsMmol;

    public HighGlucoseSettings(Context context) {
        mIsMmol = "1".equals(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(GlucoseUnitSettings.class.getSimpleName(), "1"));
    }

    @Override
    public String getSettingsValue() {
        if (mGlucoseEditView != null) {
            float glucose = Float.valueOf(mGlucoseEditView.getText().toString()) * (mIsMmol ? 18 : 1);
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
        ((TextView)v.findViewById(R.id.title)).setText(R.string.settings_high_glucose_text);
        mGlucoseEditView = (EditText) v.findViewById(R.id.settings_value);
        mGlucoseEditView.setHint(mIsMmol ? "mmol/l" : "mg/dl");
        mGlucoseEditView.setInputType(EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);
        updateView();
        return v;
    }

    @Override
    public AlertResult doFilter(Context context, GlucoseData prediction) {
        return prediction.glucoseLevel < mGlucose ? AlertResult.NOTHING : AlertResult.ALERT;
    }

    @Override
    public void afterFilter(AlertResult result) {

    }

    @Override
    public void setIsMmol(boolean isMmol) {
        mIsMmol = isMmol;
        updateView();
    }

    private void updateView() {
        if (mGlucoseEditView != null) {
            mGlucoseEditView.setText(mIsMmol ? mFormat.format(mGlucose / 18) : String.valueOf(mGlucose));
        }
    }
}
