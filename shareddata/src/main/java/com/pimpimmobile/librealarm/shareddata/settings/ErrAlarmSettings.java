package com.pimpimmobile.librealarm.shareddata.settings;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import com.pimpimmobile.librealarm.shareddata.R;

import org.json.JSONException;
import org.json.JSONObject;

public class ErrAlarmSettings extends Settings {

    private CheckBox mCheckbox;
    private EditText mMinCountEditText;
    private boolean mChecked;
    private int mMinCount = 2; // Default value
    private String mJsonObject;

    @Override
    public String getSettingsValue() {
        if (mCheckbox != null) {
            JSONObject object = new JSONObject();
            try {
                object.put("checked", mCheckbox.isChecked());
                object.put("min_count", Long.valueOf(mMinCountEditText.getText().toString()));
            } catch (JSONException e) {
                throw new RuntimeException("Json exception: " + e.getMessage());
            }
            setSettingsValue(object.toString());
        }
        return mJsonObject;
    }

    @Override
    public void setSettingsValue(String data) {
        if (!TextUtils.isEmpty(data)) {
            mJsonObject = data;
            try {
                JSONObject object = new JSONObject(mJsonObject);
                mChecked = object.getBoolean("checked");
                mMinCount = object.getInt("min_count");
            } catch (JSONException e) {
                throw new RuntimeException("Json exception: " + e.getMessage());
            }
        }
    }

    public int getErrCount() {
        return !mChecked ? Integer.MAX_VALUE : mMinCount;
    }

    @Override
    public View getView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.settings_err, parent, false);
        mCheckbox = (CheckBox) v.findViewById(R.id.checkbox);
        mCheckbox.setChecked(mChecked);
        mMinCountEditText = (EditText) v.findViewById(R.id.value);
        mMinCountEditText.setText("" + mMinCount);
        return v;
    }

}
