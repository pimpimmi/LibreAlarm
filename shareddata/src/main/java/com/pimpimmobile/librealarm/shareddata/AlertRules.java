package com.pimpimmobile.librealarm.shareddata;

import android.content.Context;

public class AlertRules {

    public enum Danger {
        HIGH,
        LOW,
        NOTHING
    }

    public static Danger check(Context context, PredictionData data) {
        Danger danger = Danger.NOTHING;
        if (!allAlertsDisabled(context)) {
            if (!alertSnoozeHigh(context)) danger = alertHigh(context, data);
            if (!alertSnoozeLow(context) && danger == Danger.NOTHING)
                danger = alertLow(context, data);
        }
        return danger;
    }

    public static Danger checkDontPostpone(Context context, PredictionData data) {
        Danger danger = alertHigh(context, data);
        if (danger == Danger.NOTHING) danger = alertLow(context, data);
        return danger;
    }

    private static boolean allAlertsDisabled(Context context)
    {
        // Booleans can be either boolean or text depending on whether they are on watch or phone due to how they are synced
        return PreferencesUtil.getBoolean(context, context.getString(R.string.key_all_alarms_disabled), false);
    }

    private static boolean alertSnoozeHigh(Context context) {
        long time = Long.valueOf(PreferencesUtil.getString(context, R.string.key_snooze_high));
        return time > System.currentTimeMillis();
    }

    private static boolean alertSnoozeLow(Context context) {
        long time = Long.valueOf(PreferencesUtil.getString(context, R.string.key_snooze_low));
        return time > System.currentTimeMillis();
    }

    private static Danger alertHigh(Context context, PredictionData data) {
        float value = Float.valueOf(PreferencesUtil.getString(context, R.string.key_alarm_limit_high, "180"));
        return data.glucoseLevel > value ? Danger.HIGH : Danger.NOTHING;
    }

    private static Danger alertLow(Context context, PredictionData data) {
        float value = Float.valueOf(PreferencesUtil.getString(context, R.string.key_alarm_limit_low, "72"));
        return data.glucoseLevel < value ? Danger.LOW : Danger.NOTHING;
    }
}
