package com.pimpimmobile.librealarm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.pimpimmobile.librealarm.shareddata.AlgorithmUtil;
import com.pimpimmobile.librealarm.shareddata.GlucoseData;
import com.pimpimmobile.librealarm.shareddata.settings.GlucoseUnitSettings;

public class AlarmDialogFragment extends DialogFragment {

    public static final String EXTRA_IS_HIGH = "high";
    public static final String EXTRA_TREND_ORDINAL = "trend";
    public static final String EXTRA_VALUE = "value";

    private int mPickerValue;

    private AlarmActionListener mListener;

    public static AlarmDialogFragment build(boolean isHigh, int trendOrdinal, int value) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(EXTRA_IS_HIGH, isHigh);
        bundle.putInt(EXTRA_TREND_ORDINAL, trendOrdinal);
        bundle.putInt(EXTRA_VALUE, value);
        AlarmDialogFragment fragment = new AlarmDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.alarm_dialog_layout, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setView(view);

        if (getActivity() instanceof AlarmActionListener) {
            mListener = (AlarmActionListener) getActivity();
        }

        Bundle bundle = getArguments();
        final boolean isGlucoseHigh = bundle.getBoolean(EXTRA_IS_HIGH, true);
        AlgorithmUtil.TrendArrow trend = AlgorithmUtil.TrendArrow.values()[bundle.getInt(EXTRA_TREND_ORDINAL, 0)];
        int value = bundle.getInt(EXTRA_VALUE, 0);

        boolean isMmol = "1".equals(PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(GlucoseUnitSettings.class.getSimpleName(), "1"));
        TextView glucoseText = (TextView) view.findViewById(R.id.glucose);
        glucoseText.setText(GlucoseData.glucose(value, isMmol));
        glucoseText.setTextColor(Color.RED);

        int trendDrawableResource = HistoryAdapter.getTrendDrawable(trend);
        if (trendDrawableResource != -1) {
            ((ImageView) view.findViewById(R.id.trend_arrow)).setImageResource(trendDrawableResource);
        }

        mPickerValue = isGlucoseHigh ?
                PreferencesUtil.getPreviousAlarmPostponeHigh(getActivity()) :
                PreferencesUtil.getPreviousAlarmPostponeLow(getActivity());

        final Button disableButton = (Button) view.findViewById(R.id.alarm_disable);
        disableButton.setText(getString(R.string.alarm_disable_button, mPickerValue));
        disableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.snooze(mPickerValue, isGlucoseHigh);
                } else {
                    Toast.makeText(getActivity(),
                            getString(R.string.toast_turn_off_alarm_failed), Toast.LENGTH_SHORT).show();
                }
                dismiss();
            }
        });
        final Button turnOffButton = (Button) view.findViewById(R.id.alarm_turn_off);
        turnOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.turnOff();
                } else {
                    Toast.makeText(getActivity(),
                            getString(R.string.toast_turn_off_alarm_failed), Toast.LENGTH_SHORT).show();
                }
                dismiss();
            }
        });

        NumberPicker picker = (NumberPicker) view.findViewById(R.id.minutes_picker);
        picker.setMinValue(1);
        picker.setMaxValue(200);
        picker.setValue(mPickerValue);
        picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                mPickerValue = newVal;
                disableButton.setText(getString(R.string.alarm_disable_button, mPickerValue));
            }
        });
        return builder.create();
    }

    public interface AlarmActionListener {
        void turnOff();
        void snooze(int minutes, boolean isHigh);
    }

}
