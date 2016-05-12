package com.pimpimmobile.librealarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "GLUCOSE::" + AlarmReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "alarmreceiver");
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AlarmHandler.setNextCheck(context, System.currentTimeMillis() + 120000);
        } else {
            Log.i(TAG, "alarmreceiver: check glucose!");
            Intent i = new Intent(context, WearActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
