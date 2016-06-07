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

public class HighGlucoseSettings extends Settings implements AlertRule {

    private static final float DEFAULT = 10F;

    private EditText mGlucoseEditView;
    private float glucose = DEFAULT;

    @Override
    public String getSettingsValue() {
        if (mGlucoseEditView != null) setSettingsValue(mGlucoseEditView.getText().toString());
        return String.valueOf(glucose);
    }

    @Override
    public void setSettingsValue(String data) {
        if (!TextUtils.isEmpty(data)) {
            glucose = Float.valueOf(data);
            if (mGlucoseEditView != null) mGlucoseEditView.setText(String.valueOf(glucose));
        }
    }

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.settings_edit_text, parent, false);
        ((TextView)v.findViewById(R.id.title)).setText("High glucose limit");
        mGlucoseEditView = (EditText) v.findViewById(R.id.settings_value);
        mGlucoseEditView.setHint("mmol/l");
        mGlucoseEditView.setText(String.valueOf(glucose));
        mGlucoseEditView.setInputType(EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);
        return v;
    }

    @Override
    public AlertResult doFilter(Context context, GlucoseData prediction) {
        return prediction.glucoseLevel < glucose * 18 ? AlertResult.NOTHING : AlertResult.ALERT;
    }

    @Override
    public void afterFilter(AlertResult result) {

    }
}
