package com.pimpimmobile.librealarm.shareddata.settings;

import android.content.Context;

import com.pimpimmobile.librealarm.shareddata.GlucoseData;

public interface AlertRule {
    enum AlertResult {
        INVALID,
        NO_ALERTS,
        NOTHING,
        ALERT,
        FORCE_ALERT,
    }

    AlertResult doFilter(Context context, GlucoseData prediction);
    void afterFilter(AlertResult result);

}
