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

import java.util.Locale;

public class GlucoseLevelView extends FrameLayout implements QuickSettingsInterface {

    private EditText mValueView;
    private TextView mTitleView;
    private TextView mSubtitleView;

    private QuickSettingsItem mItem;

    public GlucoseLevelView(Context context) {
        super(context);
        View view = LayoutInflater.from(context).inflate(R.layout.quick_settings_edit_text, this, false);
        mValueView = (EditText) view.findViewById(R.id.value);
        mTitleView = (TextView) view.findViewById(R.id.title);
        mSubtitleView = (TextView) view.findViewById(R.id.subtitle);
        addView(view);
    }

    @Override
    public void setUpdateListener(QuickSettingsView.QuickSettingsChangeListener listener) {
        // NOP
    }

    @Override
    public void setItem(QuickSettingsItem item) {
        mItem = item;
        refreshViews();
    }

    @Override
    public QuickSettingsItem getItem() {
        return mItem;
    }

    @Override
    public void updateWatchValues() {
        mItem.updateValues(getContext());
        refreshViews();
    }

    @Override
    public boolean saveSettings() {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(
                mItem.key + QuickSettingsItem.DEFAULT_VALUE, getValue()).commit();
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putString(mItem.key, getValue()).commit();
        return true;
    }

    private void refreshViews() {
        mValueView.setText(mItem.isMmol ? "" + Float.valueOf(mItem.value) / 18 : mItem.value);
        mValueView.setHint(mItem.isMmol ?
                getContext().getString(R.string.mmol) :
                getContext().getString(R.string.mgdl));
        mTitleView.setText(mItem.title);

        String watchValueString;
        if ("-1".equals(mItem.watchValue)) {
            watchValueString = "?";
        } else {
            watchValueString = String.format(Locale.getDefault(), "%.1f",
                    Float.valueOf(mItem.watchValue) / (mItem.isMmol ? 18 : 1));
        }
        mSubtitleView.setText(getResources().getString(R.string.watch_value, watchValueString));
    }

    @Override
    public String getKey() {
        return mItem.key;
    }

    @Override
    public String getValue() {
        String value = mValueView.getText().toString();
        if (TextUtils.isEmpty(value)) return "0";
        return mItem.isMmol ? "" + Float.valueOf(value) * 18 : value;
    }

}
