package com.pimpimmobile.librealarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AlarmHandler {

    private static final String TAG = "GLUCOSE::" + AlarmHandler.class.getSimpleName();

    private static final int DEFAULT_INTERVAL = 600000;

    public static void setNextCheck(Context context, long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Log.i(TAG, "set next check: " + millis + " (" + sdf.format(new Date(millis)) + ") "
                + new Exception().getStackTrace()[1]);
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pI = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        manager.cancel(pI);
        manager.setExact(AlarmManager.RTC_WAKEUP, millis, pI);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("next_check", millis).apply();
    }

    public static long getNextCheck(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong("next_check", 0);
    }

    public static long getDefaultInterval(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong("default_interval", DEFAULT_INTERVAL);
    }

    public static void cancelNextCheck(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pI = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        manager.cancel(pI);
    }
}
