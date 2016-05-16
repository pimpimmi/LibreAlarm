package com.pimpimmobile.librealarm;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.pimpimmobile.librealarm.shareddata.WearableApi;
import com.pimpimmobile.librealarm.shareddata.settings.PostponeSettings;
import com.pimpimmobile.librealarm.shareddata.settings.Settings;
import com.pimpimmobile.librealarm.shareddata.settings.SettingsUtils;

import java.util.HashMap;

public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = "GLUCOSE::" + DataLayerListenerService.class.getSimpleName();

    GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "create");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "destroy");
        super.onDestroy();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // Check the data path
                String path = event.getDataItem().getUri().getPath();
                if (WearableApi.SETTINGS.equals(path)) {
                    String newSettings = DataMapItem.fromDataItem(event.getDataItem()).getDataMap().getString("data", null);
                    SettingsUtils.saveSettings(this, newSettings);
                    HashMap<String, Settings> settings = SettingsUtils.getSettings(getBaseContext());
                    long nextAlarm = ((PostponeSettings)settings.get(PostponeSettings.class.getSimpleName())).time;
                    if (nextAlarm != -1) {
                        AlarmReceiver.post(this, nextAlarm);
                    }
                    WearableApi.sendMessage(mGoogleApiClient, WearableApi.SETTINGS, WearableApi.MESSAGE_ACK, null);
                    WearableApi.sendMessage(mGoogleApiClient, WearableApi.GET_NEXT_CHECK,
                            Long.toString(AlarmReceiver.getNextCheck(this)), null);
                }
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(TAG, "received message: " + messageEvent.getSourceNodeId() + ", command: " + messageEvent.getPath());
        switch (messageEvent.getPath()) {
            case WearableApi.GET_NEXT_CHECK:
                WearableApi.sendMessage(mGoogleApiClient, WearableApi.GET_NEXT_CHECK,
                        Long.toString(AlarmReceiver.getNextCheck(this)), null);
                break;
            case WearableApi.TRIGGER_GLUCOSE: {
                    Intent i = new Intent(this, WearActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
                break;
            case WearableApi.CANCEL_ALARM: {
                    Intent i = new Intent(this, WearActivity.class);
                    i.putExtra("cancel", true);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
                break;
            case WearableApi.STOP: {
                AlarmReceiver.stop(this);
                WearableApi.sendMessage(mGoogleApiClient, WearableApi.GET_NEXT_CHECK,
                        Long.toString(AlarmReceiver.getNextCheck(this)), null);
                }
                break;
            case WearableApi.START: {
                AlarmReceiver.start(this);
                WearableApi.sendMessage(mGoogleApiClient, WearableApi.GET_NEXT_CHECK,
                        Long.toString(AlarmReceiver.getNextCheck(this)), null);
                }
                break;
        }
    }

    @Override
    public void onPeerConnected(Node peer) {
        Log.d(TAG, "onPeerConnected: " + peer);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        Log.d(TAG, "onPeerDisconnected: " + peer);
    }

}
