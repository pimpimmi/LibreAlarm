package com.pimpimmobile.librealarm;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class AlarmIntentService extends IntentService {

    private static final String TAG = "GLUCOSE::" + AlarmIntentService.class.getSimpleName();

    public AlarmIntentService() {
        super("MyAlarmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "AlarmIntentService");
        Intent i = new Intent(this, WearActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // If activity doesn't start for some reason, try again in 10 seconds.
        if (PreferencesUtil.getIsStarted(this)) AlarmReceiver.post(getBaseContext(), 10000);
        startActivity(i);

        AlarmReceiver.completeWakefulIntent(intent);
    }
}
