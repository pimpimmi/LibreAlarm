package com.pimpimmobile.librealarm;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.pimpimmobile.librealarm.shareddata.AlgorithmUtil;
import com.pimpimmobile.librealarm.shareddata.ReadingData;
import com.pimpimmobile.librealarm.shareddata.Status;
import com.pimpimmobile.librealarm.shareddata.WearableApi;
import com.pimpimmobile.librealarm.shareddata.settings.PostponeSettings;
import com.pimpimmobile.librealarm.shareddata.settings.SettingsUtils;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;

/**
 * Service which keeps the phone connected to the watch.
 * TODO: Perhaps this is unnecessary and drains battery. Needed to get notified about alarms though.
 */
public class WearService extends Service implements DataApi.DataListener, MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GLUCOSE::" + WearService.class.getSimpleName();

    private final WearServiceBinder binder = new WearServiceBinder();

    private Activity mActivity;

    private GoogleApiClient mGoogleApiClient;

    private boolean mResolvingError;

    public WearServiceListener mListener;

    // TODO: Crashes for some reason.
//    private MediaPlayer mAlarmPlayer;

    private Status mReadingStatus;

    private SimpleDatabase mDatabase = new SimpleDatabase(this);

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(TAG, "Message receiver: " + messageEvent.getPath() + ", " +
                new String(messageEvent.getData(), Charset.forName("UTF-8")));
        switch (messageEvent.getPath()) {
            case WearableApi.CANCEL_ALARM:
                stopAlarm();
                break;
            case WearableApi.SETTINGS: // ACK
                Toast.makeText(this, "Settings updated on watch", Toast.LENGTH_LONG).show();
                SettingsUtils.saveSettings(this, PostponeSettings.class.getSimpleName(), null);
                break;
            case WearableApi.STATUS:
                Log.i("UITest", "Gson: " + new String(messageEvent.getData()));
                mReadingStatus = new Gson().fromJson(new String(messageEvent.getData()), Status.class);
                if (mListener != null) mListener.onDataUpdated();
                break;
            case WearableApi.GLUCOSE:
                ReadingData.TransferObject object = new Gson().fromJson(
                        new String(messageEvent.getData()), ReadingData.TransferObject.class);
                mDatabase.storeReading(object.data);
                WearableApi.sendMessage(mGoogleApiClient, WearableApi.GLUCOSE, String.valueOf(object.id), null);
                if (mListener != null) mListener.onDataUpdated();
                break;
        }
    }

    public void start() {
        WearableApi.sendMessage(mGoogleApiClient, WearableApi.START, "", null);
    }

    public void stop() {
        WearableApi.sendMessage(mGoogleApiClient, WearableApi.STOP, "", null);
    }

    public class WearServiceBinder extends Binder {
        public WearService getService() {
            return WearService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (!mResolvingError) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
            stopAlarm();
        }
        mDatabase.close();
        super.onDestroy();
    }

    public boolean isConnected(){
        return mGoogleApiClient != null && mGoogleApiClient.isConnected();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Wear connected");
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        WearableApi.sendMessage(mGoogleApiClient, WearableApi.GET_UPDATE, "", null);
        mResolvingError = false;
        if (mListener != null) mListener.onDataUpdated();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        if (mListener != null) mListener.onDataUpdated();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution() && mActivity != null) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(mActivity, 1000);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            mResolvingError = false;
            if (mListener != null) mListener.onDataUpdated();
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
        }
    }

    public SimpleDatabase getDatabase() {
        return mDatabase;
    }

    public Status getReadingStatus() {
        return mReadingStatus;
    }

    private void startAlarm() {
        stopAlarm();
//        mAlarmPlayer =
//                MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
//        mAlarmPlayer.start();
        alarmisplaying = true;
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private boolean alarmisplaying = false;
    public void stopAlarm() {
        alarmisplaying = false;
        if (mListener != null) mListener.onDataUpdated();
//        if (mAlarmPlayer != null && mAlarmPlayer.isPlaying()) {
//            mAlarmPlayer.stop();
//            mAlarmPlayer.release();
//        }
    }

    public boolean isAlarmPlaying() {
        return alarmisplaying; //return mAlarmPlayer != null && mAlarmPlayer.isPlaying();
    }

    public String getStatusString() {
        if (isConnected() && mReadingStatus != null) {
            switch (mReadingStatus.status) {
                case ALARM:
                    return "ALARM!!";
                case ATTEMPTING:
                    return "Attempt " + mReadingStatus.attempt + "/" + mReadingStatus.maxAttempts;
                case ATTENPT_FAILED:
                    return "Attempt " + mReadingStatus.attempt + "/" + mReadingStatus.maxAttempts + " failed";
                case WAITING:
                    return "Next check: " + AlgorithmUtil.format(new Date(mReadingStatus.nextCheck));
                case NOT_RUNNING:
                    return "Not running";
                default:
                    return "";
            }
        } else {
            return "Not connected";
        }
    }

    public void sendData(String command, HashMap<String, String> data, ResultCallback<DataApi.DataItemResult> listener) {
        if (isConnected()) WearableApi.sendData(mGoogleApiClient, command, data, listener);
    }

    public void sendMessage(String command, String message, ResultCallback<MessageApi.SendMessageResult> listener) {
        if (isConnected()) WearableApi.sendMessage(mGoogleApiClient, command, message, listener);
    }

    public void setListener(Activity activity, WearServiceListener listener) {
        mActivity = activity;
        mListener = listener;
    }

    public interface WearServiceListener {
        void onDataUpdated();
    }
}
