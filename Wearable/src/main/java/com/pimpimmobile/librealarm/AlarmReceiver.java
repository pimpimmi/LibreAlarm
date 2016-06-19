package com.pimpimmobile.librealarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This receiver will be started
 */
public class AlarmReceiver extends WakefulBroadcastReceiver {

    public static final int DEFAULT_INTERVAL = 600000;
    private static final String TAG = "GLUCOSE::" + AlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "AlarmReceiver");
        Intent i = new Intent(context, AlarmIntentService.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startWakefulService(context, i);
    }

    public static void post(Context context, long delay) {
        Log.i(TAG, "set next check: " + delay + " (" + new SimpleDateFormat("HH:mm:ss")
                .format(new Date(delay + System.currentTimeMillis())) + ") "
                + new Exception().getStackTrace()[1]);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent intent = getAlarmReceiverIntent(context);
        alarmManager.cancel(intent);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay, intent);
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putLong("next_check", System.currentTimeMillis() + delay).apply();
    }

    public static void start(Context context) {
        ComponentName receiver = new ComponentName(context, AlarmReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static long getNextCheck(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong("next_check", 0);
    }

    private static PendingIntent getAlarmReceiverIntent(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public static void stop(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(getAlarmReceiverIntent(context));

        ComponentName receiver = new ComponentName(context, AlarmReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("next_check", -1).apply();
    }
}
