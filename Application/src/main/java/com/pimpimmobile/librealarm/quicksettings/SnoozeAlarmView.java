package com.pimpimmobile.librealarm.quicksettings;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.pimpimmobile.librealarm.R;
import com.pimpimmobile.librealarm.shareddata.AlgorithmUtil;

import java.util.Date;

public class SnoozeAlarmView extends FrameLayout implements QuickSettingsInterface {

    private EditText mValueView;
    private TextView mTitleView;
    private TextView mSubtitleView;

    private QuickSettingsView.QuickSettingsChangeListener mListener;

    private QuickSettingsItem mItem;

    public SnoozeAlarmView(Context context) {
        super(context);
        View view = LayoutInflater.from(context).inflate(R.layout.quick_settings_snooze_edit_text, this, false);
        mValueView = (EditText) view.findViewById(R.id.value);
        mTitleView = (TextView) view.findViewById(R.id.title);
        mSubtitleView = (TextView) view.findViewById(R.id.subtitle);
        view.findViewById(R.id.snooze).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(
                        mItem.key + QuickSettingsItem.DEFAULT_VALUE, getValue()).commit();
                if (mListener != null) {
                    long value = (long) (float) Float.valueOf(getValue());
                    String time = String.valueOf(System.currentTimeMillis() + value * 60000);
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(mItem.key, time).apply();
                    mListener.onQuickSettingsChanged(mItem.key, time);
                }
            }
        });
        addView(view);
    }

    @Override
    public void setItem(QuickSettingsItem item) {
        mItem = item;
        refreshValues();
    }

    @Override
    public QuickSettingsItem getItem() {
        return mItem;
    }

    @Override
    public void updateWatchValues() {
        mItem.updateValues(getContext());
        refreshValues();
    }

    @Override
    public boolean saveSettings() {
        // We only allow the setting to be saved on snooze button click
        return false;
    }

    @Override
    public String getKey() {
        return mItem.key;
    }

    private void refreshValues() {
        mValueView.setText(mItem.value);
        mTitleView.setText(mItem.title);

        String watchValue;
        if ("-1".equals(mItem.watchValue)) {
            watchValue = "?";
        } else {
            long time = Long.valueOf(mItem.watchValue);
            if (time > System.currentTimeMillis()) {
                watchValue = AlgorithmUtil.format(new Date(time));
            } else {
                watchValue = "?";
            }
        }
        mSubtitleView.setText(getResources().getString(R.string.watch_value, watchValue));
    }

    @Override
    public String getValue() {
        String value = mValueView.getText().toString();
        if (TextUtils.isEmpty(value)) return "0";
        return value;
    }

    @Override
    public void setUpdateListener(QuickSettingsView.QuickSettingsChangeListener listener) {
        mListener = listener;
    }
}
