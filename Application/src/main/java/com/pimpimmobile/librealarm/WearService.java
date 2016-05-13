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
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.pimpimmobile.librealarm.shareddata.AlgorithmUtil;
import com.pimpimmobile.librealarm.shareddata.ReadingData;
import com.pimpimmobile.librealarm.shareddata.ReadingStatus;
import com.pimpimmobile.librealarm.shareddata.WearableApi;

import java.nio.charset.Charset;
import java.util.Date;

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

    private String mNextCheck;

    private ReadingStatus mReadingStatus;

    private SimpleDatabase mDatabase = new SimpleDatabase(this);

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // Check the data path
                String path = event.getDataItem().getUri().getPath();
                if (path.equals(WearableApi.GLUCOSE)) {
                    String data = DataMapItem.fromDataItem(event.getDataItem()).getDataMap().getString("data", null);
                    if (data != null) {
                        mDatabase.storeReading(new ReadingData(data));
                    }
                }
            }
        }
        mReadingStatus = null;
        if (mListener != null) mListener.onDataUpdated();
        WearableApi.sendMessage(mGoogleApiClient, WearableApi.GET_NEXT_CHECK, "", null);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        switch (messageEvent.getPath()) {
            case WearableApi.ALARM:
                startAlarm();
                break;
            case WearableApi.CANCEL_ALARM:
                stopAlarm();
                break;
            case WearableApi.GET_NEXT_CHECK:
                setNextCheck(AlgorithmUtil.format(new Date(Long.valueOf(
                        new String(messageEvent.getData(), Charset.forName("UTF-8"))))));
                break;
            case WearableApi.SETTINGS:
                Toast.makeText(this, "Settings updated on watch", Toast.LENGTH_LONG).show();
                break;
            case WearableApi.STATUS_UPDATE:
                mReadingStatus = new ReadingStatus(
                        new String(messageEvent.getData(), Charset.forName("UTF-8")));
                if (mListener != null) mListener.onDataUpdated();
                break;
        }
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
        WearableApi.sendMessage(mGoogleApiClient, WearableApi.GET_NEXT_CHECK, "", null);
        mResolvingError = false;
        if (mListener != null) mListener.onConnected();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        if (mListener != null) mListener.onDisconnected();
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
            if (mListener != null) mListener.onDisconnected();
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
        }
    }

    public SimpleDatabase getDatabase() {
        return mDatabase;
    }

    public ReadingStatus getReadingStatus() {
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
//        if (mAlarmPlayer != null && mAlarmPlayer.isPlaying()) {
//            mAlarmPlayer.stop();
//            mAlarmPlayer.release();
//        }
    }

    public boolean isAlarmPlaying() {
        return alarmisplaying; //return mAlarmPlayer != null && mAlarmPlayer.isPlaying();
    }

    public String getNextCheck() {
        return mNextCheck;
    }

    public void setNextCheck(String nextCheck) {
        mNextCheck = nextCheck;
        if (mListener != null) mListener.onDataUpdated();
    }

    public void sendData(String command, String data, ResultCallback<DataApi.DataItemResult> listener) {
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
        void onConnected();
        void onDisconnected();
        void onDataUpdated();
    }
}
