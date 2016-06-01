package com.pimpimmobile.librealarm;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;
import com.pimpimmobile.librealarm.shareddata.ReadingData;
import com.pimpimmobile.librealarm.shareddata.Status;
import com.pimpimmobile.librealarm.shareddata.Status.Type;
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
                    HashMap<String, String> newSettings = new HashMap<>();
                    DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    for (String key : dataMap.keySet()) {
                        newSettings.put(key, dataMap.getString(key, null));
                    }
                    SettingsUtils.saveSettings(this, newSettings);
                    HashMap<String, Settings> settings = SettingsUtils.getSettings(getBaseContext());
                    long nextAlarm = ((PostponeSettings)settings.get(PostponeSettings.class.getSimpleName())).time;
                    if (nextAlarm > 0) {
                        AlarmReceiver.post(this, nextAlarm);
                    }
                    WearableApi.sendMessage(mGoogleApiClient, WearableApi.SETTINGS, WearableApi.MESSAGE_ACK, null);

                    sendStatus(mGoogleApiClient);
                }
            }
        }
    }

    private static void sendStatus(GoogleApiClient client) {
        int attempt = PreferencesUtil.getRetries(client.getContext());
        Type type = PreferencesUtil.getCurrentType(client.getContext());
        Status status = new Status(type, attempt, WearActivity.MAX_ATTEMPTS,
                AlarmReceiver.getNextCheck(client.getContext()));
        WearableApi.sendMessage(client, WearableApi.STATUS, new Gson().toJson(status), null);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        handleMessage(mGoogleApiClient, messageEvent);
    }

    public static void handleMessage(GoogleApiClient client, MessageEvent messageEvent) {
        Log.i(TAG, "received message: " + messageEvent.getSourceNodeId() + ", command: " + messageEvent.getPath());
        switch (messageEvent.getPath()) {
            case WearableApi.TRIGGER_GLUCOSE: {
                Intent i = new Intent(client.getContext(), WearActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                client.getContext().startActivity(i);
            }
            break;
            case WearableApi.CANCEL_ALARM: {
                PreferencesUtil.setCurrentType(client.getContext(), Type.WAITING);
                Intent i = new Intent(client.getContext(), WearActivity.class);
                i.putExtra(WearActivity.EXTRA_CANCEL_ALARM, true);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                client.getContext().startActivity(i);
                sendStatus(client);
            }
            break;
            case WearableApi.STOP: {
                PreferencesUtil.setCurrentType(client.getContext(), Type.NOT_RUNNING);
                PreferencesUtil.setIsStarted(client.getContext(), false);
                AlarmReceiver.stop(client.getContext());
                sendStatus(client);
            }
            break;
            case WearableApi.START: {
                PreferencesUtil.setCurrentType(client.getContext(), Type.WAITING);
                PreferencesUtil.setIsStarted(client.getContext(), true);
                AlarmReceiver.start(client.getContext());
                AlarmReceiver.post(client.getContext(), 30000);
                sendStatus(client);
            }
            break;
            case WearableApi.GLUCOSE: { // ACK response
                SimpleDatabase database = new SimpleDatabase(client.getContext());
                database.deleteMessage(Long.valueOf(new String(messageEvent.getData())));
                database.close();
            }
            break;
            case WearableApi.GET_UPDATE: {
                SimpleDatabase database = new SimpleDatabase(client.getContext());
                for (ReadingData.TransferObject message : database.getMessages()) {
                    WearableApi.sendMessage(client, WearableApi.GLUCOSE, new Gson().toJson(message), null);
                }
                database.close();
                sendStatus(client);
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
