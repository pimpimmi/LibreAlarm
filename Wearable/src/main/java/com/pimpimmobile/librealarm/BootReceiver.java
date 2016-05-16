package com.pimpimmobile.librealarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "GLUCOSE::" + BootReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "alarmreceiver");
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AlarmReceiver.start(context);
        }
    }
}
