package com.pimpimmobile.librealarm;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.pimpimmobile.librealarm.shareddata.AlgorithmUtil;
import com.pimpimmobile.librealarm.shareddata.PredictionData;
import com.pimpimmobile.librealarm.shareddata.ReadingData;
import com.pimpimmobile.librealarm.shareddata.ReadingStatus;
import com.pimpimmobile.librealarm.shareddata.WearableApi;
import com.pimpimmobile.librealarm.shareddata.settings.SettingsUtils;

import java.io.IOException;
import java.util.Arrays;

public class WearActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener, MessageApi.MessageListener, View.OnClickListener {

    private static final String TAG = "GLUCOSE::" + WearActivity.class.getSimpleName();

    private static final int MAX_ATTEMPTS = 6;

    private GoogleApiClient mGoogleApiClient;

    private NfcAdapter mNfcAdapter;

    private PowerManager.WakeLock mWakeLock;

    private Handler mHandler = new Handler();

    private Vibrator mVibrator;

    private boolean mShouldRetry;

    private Runnable mStopActivityRunnable = new Runnable() {
        @Override
        public void run() {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WearActivity.this);
            int retries = preferences.getInt("retries", 0);
            if (retries >= MAX_ATTEMPTS) {
                preferences.edit().putInt("retries", 0).commit();
                sendResultAndFinish();
            } else {
                sendStatusUpdate(false, new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                        finish();
                    }
                });
                preferences.edit().putInt("retries", ++retries).commit();
                mShouldRetry = true;
            }
        }
    };

    private ReadingData mResult = new ReadingData(PredictionData.Result.ERROR_NO_NFC);

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        Log.i(TAG, "onCreate()");
        setContentView(R.layout.wear_activity);
        AlarmHandler.setNextCheck(this, System.currentTimeMillis() + AlarmHandler.getDefaultInterval(this));
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = manager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");

        mWakeLock.acquire();
        mHandler.postDelayed(mStopActivityRunnable, 10000);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.hasExtra("cancel")) finish();
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG,"onDestroy");
        if (mShouldRetry) AlarmHandler.setNextCheck(this, System.currentTimeMillis() +  10000);
        mHandler.removeCallbacksAndMessages(null);
        if (mWakeLock.isHeld()) mWakeLock.release();
        if (mVibrator != null) mVibrator.cancel();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
        if (mNfcAdapter != null) mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String action = data.getAction();
        Log.i(TAG, "tech discovered");
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)){
            mResult = new ReadingData(PredictionData.Result.ERROR_NFC_READ);
            Tag tag = data.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            new NfcVReaderTask().execute(tag);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "is connected!!");
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        sendStatusUpdate(true, null);

        NfcManager nfcManager =
                (NfcManager) this.getBaseContext().getSystemService(Context.NFC_SERVICE);
        mNfcAdapter = nfcManager.getDefaultAdapter();

        PendingIntent pi = createPendingResult(15, new Intent(), 0);
        try {
            mNfcAdapter.enableForegroundDispatch(this, pi,
                    new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)},
                    new String[][]{new String[]{"android.nfc.tech.NfcV"}});
        } catch (NullPointerException e) {
            Log.e(TAG, "Adapter nullpointer");
        }
    }

    private void sendStatusUpdate(boolean running,
            ResultCallback<MessageApi.SendMessageResult> listener) {
        int attempt = PreferenceManager.getDefaultSharedPreferences(this).getInt("retries", 0);
        WearableApi.sendMessage(mGoogleApiClient, WearableApi.STATUS_UPDATE,
                new ReadingStatus(attempt, MAX_ATTEMPTS, running).toTransferString(), listener);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        Log.i(TAG, "message: " + event.toString());
    }

    @Override
    public void onClick(View v) {

    }

    private void sendResultAndFinish() {
        WearableApi.sendData(mGoogleApiClient, WearableApi.GLUCOSE, mResult.readingToString(), new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                Log.i(TAG, "finish");
                if (!AlgorithmUtil.danger(WearActivity.this, mResult.prediction,
                        SettingsUtils.getAlertRules(WearActivity.this))) finish();
            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + connectionResult);
    }

    private class NfcVReaderTask extends AsyncTask<Tag, Void, Tag> {

        private byte[] data = new byte[360];

        @Override
        protected void onPostExecute(Tag tag) {
            if (tag == null) return;
            String tagId = bytesToHexString(tag.getId());
            Log.i(TAG, "Tag id: " + tagId);
            mHandler.removeCallbacks(mStopActivityRunnable);
            mResult = AlgorithmUtil.parseData(tagId, data);
            PreferenceManager.getDefaultSharedPreferences(WearActivity.this)
                    .edit().putInt("retries", 0).apply();
            sendResultAndFinish();
            if (AlgorithmUtil.danger(WearActivity.this, mResult.prediction, SettingsUtils.getAlertRules(WearActivity.this))) {
                WearableApi.sendMessage(mGoogleApiClient, WearableApi.ALARM, "", null);
                mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                mVibrator.vibrate(1000000);
            }
        }

        @Override
        protected Tag doInBackground(Tag... params) {
            Tag tag = params[0];
            NfcV nfcvTag = NfcV.get(tag);
            try {
                nfcvTag.connect();
                final byte[] uid = tag.getId();
                for(int i=0; i <= 40; i++) {
                    byte[] cmd = new byte[]{0x60, 0x20, 0, 0, 0, 0, 0, 0, 0, 0, (byte)i, 0};
                    System.arraycopy(uid, 0, cmd, 2, 8);
                    byte[] oneBlock;
                    Long time = System.currentTimeMillis();
                    while (true) {
                        try {
                            oneBlock = nfcvTag.transceive(cmd);
                            break;
                        } catch (IOException e) {
                            if ((System.currentTimeMillis() > time + 2000)) {
                                Log.e(TAG, "tag read timeout");
                                return null;
                            }
                        }
                    }

                    oneBlock = Arrays.copyOfRange(oneBlock, 2, oneBlock.length);
                    System.arraycopy(oneBlock, 0, data, i*8, 8);
                }

            } catch (Exception e) {
                Log.i(TAG, e.toString());
                return null;
            } finally {
                try {
                    nfcvTag.close();
                } catch (Exception e) {}
            }

            return tag;
        }
    }

    private String bytesToHexString(byte[] src) {
        StringBuilder builder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return "";
        }

        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            builder.append(buffer);
        }

        return builder.toString();
    }
}
