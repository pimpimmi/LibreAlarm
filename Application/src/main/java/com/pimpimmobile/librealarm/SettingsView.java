package com.pimpimmobile.librealarm;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.pimpimmobile.librealarm.shareddata.settings.Settings;
import com.pimpimmobile.librealarm.shareddata.settings.SettingsUtils;

import java.util.HashMap;

public class SettingsView extends LinearLayout {

    private LayoutInflater mInflater;

    public HashMap<String, Settings> settingsMap;

    public SettingsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mInflater = LayoutInflater.from(context);
        setDividerDrawable(new ColorDrawable(Color.WHITE));
        setDividerPadding(3);
        setOrientation(VERTICAL);
        settingsMap = SettingsUtils.getSettings(getContext());
        for (Settings settings : settingsMap.values()) {
            addView(new SettingsChild(getContext(), settings));
        }
    }

    private class SettingsChild extends LinearLayout {

        private Settings mSettings;

        public SettingsChild(Context context, Settings settings) {
            super(context);
            setGravity(Gravity.CENTER_VERTICAL);
            setOrientation(HORIZONTAL);
            mSettings = settings;
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
            setPadding(padding, padding, padding, padding);

            if (!mSettings.isObligatory()) {
                CheckBox checkBox = new CheckBox(getContext());
                checkBox.setPadding(padding, padding, padding, padding);
                checkBox.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mSettings.enabled(isChecked);
                    }
                });
                addView(checkBox);
            }

            FrameLayout frameLayout = new FrameLayout(getContext());
            frameLayout.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
            frameLayout.addView(mSettings.getView(mInflater, SettingsView.this));
            addView(frameLayout);
        }
    }
}
