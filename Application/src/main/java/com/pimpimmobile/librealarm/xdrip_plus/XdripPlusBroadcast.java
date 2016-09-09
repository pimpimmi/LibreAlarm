package com.pimpimmobile.librealarm.xdrip_plus;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;

import com.pimpimmobile.librealarm.shareddata.ReadingData;

import java.util.List;

/**
 * Created by jamorham on 08/09/2016.
 */

public class XdripPlusBroadcast {

    final static String TAG = "LibAlrm+xDrip+bcast";

    // send broadcast to xDrip-plus
    public static void syncXdripPlus(final Context context, final String data, final ReadingData.TransferObject object, final int battery_level) {
        final boolean check_receivers = true;
        new Thread() {
            @Override
            public void run() {
                final String LIBRE_ALARM_TO_XDRIP_PLUS = "com.eveningoutpost.dexdrip.FROM_LIBRE_ALARM";
                final Bundle bundle = new Bundle();

                bundle.putInt("bridge_battery", battery_level);
                bundle.putString("data", data);

                final Intent intent = new Intent(LIBRE_ALARM_TO_XDRIP_PLUS);
                intent.putExtras(bundle);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                context.sendBroadcast(intent);
                List<ResolveInfo> q = context.getPackageManager().queryBroadcastReceivers(intent, 0);
                // can use this to generate an alert if nothing there to receive the data
                if (check_receivers) {
                    if (q.size() < 1) {
                        Log.e(TAG, "xDrip-plus No receivers! - xDrip-plus not running or out of date version?");
                    } else Log.e(TAG, "Sent to xDrip-plus " + q.size() + " receivers");
                }
            }
        }.start();
    }
}
