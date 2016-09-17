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

import com.pimpimmobile.librealarm.shareddata.PreferencesUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This receiver will be started
 */
public class AlarmReceiver extends WakefulBroadcastReceiver {

    private static final String TAG = "GLUCOSE::" + AlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "AlarmReceiver");
        post(context);
        Intent i = new Intent(context, WearService.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setAction("alarmreceiver");
        startWakefulService(context, i);
    }

    public static void post(Context context) {
        long delay = Integer.valueOf(PreferencesUtil.getCheckGlucoseInterval(context)) * 60000 + 120000;
        Log.i(TAG, "set next check: " + delay + " (" + new SimpleDateFormat("HH:mm:ss")
                .format(new Date(delay + System.currentTimeMillis())) + ") "
                + new Exception().getStackTrace()[1]);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent intent = getAlarmReceiverIntent(context);
        alarmManager.cancel(intent);
        if (android.os.Build.VERSION.SDK_INT <= 18) {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delay, intent);
        } else {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delay, intent);
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putLong("next_check", System.currentTimeMillis() + delay).apply();
    }

    public static void start(Context context) {
        ComponentName receiver = new ComponentName(context, AlarmReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        post(context);
    }

    private static PendingIntent getAlarmReceiverIntent(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public static void stop(Context context) {
        Log.i(TAG, "stopping alarm receiver");
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
