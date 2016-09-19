package com.pimpimmobile.librealarm.shareddata;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.Charset;
import java.util.HashMap;

public class WearableApi {

    private static final String TAG = "GLUCOSE::" + WearableApi.class.getSimpleName();

    private static final String PREFIX = "/librealarm";

    public static final String TRIGGER_GLUCOSE = PREFIX + "/trigger_glucose";
    public static final String START = PREFIX + "/start";
    public static final String STOP = PREFIX + "/stop";
    public static final String CANCEL_ALARM = PREFIX + "/cancel_alarm";
    public static final String SETTINGS = PREFIX + "/settings";
    public static final String GLUCOSE = PREFIX + "/glucose";
    public static final String STATUS = PREFIX + "/status_update";
    public static final String GET_UPDATE = PREFIX + "/update";

    public static boolean sendData(GoogleApiClient client, String command, HashMap<String, String> pairs, ResultCallback<DataApi.DataItemResult> listener) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(command);
        for (String key : pairs.keySet()) {
            putDataMapReq.getDataMap().putString(key, pairs.get(key));
        }
        return sendData(client, putDataMapReq, listener);
    }

    public static boolean sendData(GoogleApiClient client, String command, String key, String data, ResultCallback<DataApi.DataItemResult> listener) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(command);
        putDataMapReq.getDataMap().putString(key, data);
        return sendData(client, putDataMapReq, listener);
    }

    private static boolean sendData(GoogleApiClient client, PutDataMapRequest putDataMapReq, ResultCallback<DataApi.DataItemResult> listener) {
        if (client.isConnected()) {
            Log.i(TAG, "update settings");
            putDataMapReq.setUrgent();
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pR =
                    Wearable.DataApi.putDataItem(client, putDataReq);
            if (listener != null) pR.setResultCallback(listener);
            return true;
        }
        return false;
    }

    public static void sendMessage(final GoogleApiClient client, final String command, final String message, ResultCallback<MessageApi.SendMessageResult> listener) {
        sendMessage(client, command, message.getBytes(Charset.forName("UTF-8")), listener);
    }

    public static void sendMessage(final GoogleApiClient client, final String command,
            final byte[] message, final ResultCallback<MessageApi.SendMessageResult> listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes =
                        Wearable.NodeApi.getConnectedNodes( client ).await();
                for(Node node : nodes.getNodes()) {
                    Log.i(TAG, "sending to " + node.getId() + ", command: " + command);
                    PendingResult<MessageApi.SendMessageResult> pR =
                            Wearable.MessageApi.sendMessage(client, node.getId(), command, message);
                    if (listener != null) pR.setResultCallback(listener);
                }
            }
        }).start();
    }
}
