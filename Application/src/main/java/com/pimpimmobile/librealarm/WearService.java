package com.pimpimmobile.librealarm;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.IntentSender;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
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
import com.pimpimmobile.librealarm.nightscout.NightscoutUploader;
import com.pimpimmobile.librealarm.quicksettings.QuickSettingsItem;
import com.pimpimmobile.librealarm.shareddata.AlertRules;
import com.pimpimmobile.librealarm.shareddata.AlgorithmUtil;
import com.pimpimmobile.librealarm.shareddata.PredictionData;
import com.pimpimmobile.librealarm.shareddata.PreferencesUtil;
import com.pimpimmobile.librealarm.shareddata.ReadingData;
import com.pimpimmobile.librealarm.shareddata.Status;
import com.pimpimmobile.librealarm.shareddata.WearableApi;
import com.pimpimmobile.librealarm.xdrip_plus.XdripPlusBroadcast;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Service which keeps the phone connected to the watch.
 */
public class WearService extends Service implements DataApi.DataListener, MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GLUCOSE::" + WearService.class.getSimpleName();

    private final WearServiceBinder binder = new WearServiceBinder();

    private Activity mActivity;

    private GoogleApiClient mGoogleApiClient;

    private boolean mResolvingError;

    public WearServiceListener mListener;

    private MediaPlayer mAlarmPlayer;

    private TextToSpeech mTextToSpeech;

    private Status mReadingStatus;

    private SimpleDatabase mDatabase = new SimpleDatabase(this);

    private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            // Nop
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String data = new String(messageEvent.getData(), Charset.forName("UTF-8"));
        Log.i(TAG, "Message receiver: " + messageEvent.getPath() + ", " + data);
        switch (messageEvent.getPath()) {
            case WearableApi.CANCEL_ALARM:
                stopAlarm();
                break;
            case WearableApi.SETTINGS:
                Toast.makeText(this, R.string.settings_updated_on_watch, Toast.LENGTH_LONG).show();
                HashMap<String, String> prefs = PreferencesUtil.toMap(data);
                for (String key : prefs.keySet()) {
                    PreferencesUtil.putString(this, key + QuickSettingsItem.WATCH_VALUE, prefs.get(key));
                }

                if (mListener != null) mListener.onWatchSettingsUpdated();
                break;
            case WearableApi.STATUS:
                mReadingStatus = new Gson().fromJson(data, Status.class);
                Status.Type type = mReadingStatus.status;
                if (type == Status.Type.ALARM_HIGH || type == Status.Type.ALARM_LOW) {
                    startAlarm(mReadingStatus);
                } else {
                    stopAlarm();
                }
                if (mListener != null) mListener.onDataUpdated();
                break;
            case WearableApi.GLUCOSE:
                ReadingData.TransferObject object =
                        new Gson().fromJson(data, ReadingData.TransferObject.class);
                mDatabase.storeReading(object.data);
                WearableApi.sendMessage(mGoogleApiClient, WearableApi.GLUCOSE, String.valueOf(object.id), null);
                if (mListener != null) mListener.onDataUpdated();
                if (PreferencesUtil.isXdripPlusEnabled(this)) XdripPlusBroadcast.syncXdripPlus(getApplicationContext(),data,object,getBatteryLevel());
                if (PreferencesUtil.isNsRestEnabled(this)) syncNightscout();
                runTextToSpeech(object.data.prediction);
                break;
        }
    }


    private void syncNightscout() {
        new Thread() {
            @Override
            public void run() {
                NightscoutUploader uploader = new NightscoutUploader(WearService.this);
                List<PredictionData> result = uploader.upload(mDatabase.getNsSyncData());
                mDatabase.setNsSynced(result);
                super.run();
            }
        }.start();
    }

    public void start() {
        WearableApi.sendMessage(mGoogleApiClient, WearableApi.START, "", null);
    }

    public void stop() {
        WearableApi.sendMessage(mGoogleApiClient, WearableApi.STOP, "", null);
    }

    public void getUpdate() {
        WearableApi.sendMessage(mGoogleApiClient, WearableApi.GET_UPDATE, "", null);
    }

    public void disableAlarm(String key, int minutes) {
        String value = String.valueOf((long) minutes * 60000 + System.currentTimeMillis());
        sendData(WearableApi.SETTINGS, key, value, null);
    }

    public class WearServiceBinder extends Binder {
        public WearService getService() {
            return WearService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupTextToSpeech();
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
        mTextToSpeech.shutdown();
        mAlarmPlayer.release();
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

    private void startAlarm(Status status) {
        boolean usePhoneAlarm = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.pref_key_phone_alarm), false);
        if (usePhoneAlarm) {
            if (mAlarmPlayer == null) {
                mAlarmPlayer = MediaPlayer.create(
                        this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
            }
            mAlarmPlayer.start();
        }

        startActivity(MainActivity.buildAlarmIntent(this, status));
    }

    public void stopAlarm() {
        if (mAlarmPlayer != null && mAlarmPlayer.isPlaying()) {
            mAlarmPlayer.pause();
            mAlarmPlayer.seekTo(0);
        }
    }

    private void setupTextToSpeech() {
        mTextToSpeech = new TextToSpeech(this,new TextToSpeech.OnInitListener() {
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR) {
                    Toast.makeText(WearService.this, R.string.error_text_to_speech_init, Toast.LENGTH_LONG).show();
                }
            }
        });

        mTextToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {

            }

            @Override
            public void onDone(String utteranceId) {
                AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
                manager.abandonAudioFocus(mAudioFocusChangeListener);
            }

            @Override
            public void onError(String utteranceId) {
                AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
                manager.abandonAudioFocus(mAudioFocusChangeListener);
            }
        });
    }

    private void runTextToSpeech(PredictionData data) {

        if (!PreferencesUtil.getBoolean(this, getString(R.string.pref_key_text_to_speech))) return;

        boolean alarmOnly = PreferencesUtil.getBoolean(this, getString(R.string.pref_key_text_to_speech_only_alarm));

        if (alarmOnly && AlertRules.checkDontPostpone(this, data) == AlertRules.Danger.NOTHING) return;

        AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
        manager.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

        String message;

        if (data.errorCode == PredictionData.Result.OK) {
            boolean isMmol = PreferencesUtil.getBoolean(this, getString(R.string.pref_key_mmol), true);
            String glucose = data.glucose(isMmol);

            AlgorithmUtil.TrendArrow arrow = AlgorithmUtil.getTrendArrow(data);
            String trend;
            switch (arrow) {
                case UP:
                    trend = getString(R.string.text_to_speech_trend_up);
                    break;
                case DOWN:
                    trend = getString(R.string.text_to_speech_trend_down);
                    break;
                case SLIGHTLY_UP:
                    trend = getString(R.string.text_to_speech_trend_slightly_up);
                    break;
                case SLIGHTLY_DOWN:
                    trend = getString(R.string.text_to_speech_trend_slightly_down);
                    break;
                case FLAT:
                    trend = getString(R.string.text_to_speech_trend_flat);
                    break;
                case UNKNOWN:
                default:
                    trend = getString(R.string.text_to_speech_trend_unknown);
                    break;
            }
            message = getString(R.string.text_to_speech_message, glucose, trend);
        } else {
            message = getString(R.string.text_to_speech_error);
        }

        if (Build.VERSION.SDK_INT < 21) {
            HashMap<String, String> map = new HashMap<>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "glucose-speech");
            mTextToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, map);
        } else {
            mTextToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "glucose-speech");
        }

    }

    public int getBatteryLevel() {
        if ((mReadingStatus != null) && (mReadingStatus.battery > 0)) {
            return mReadingStatus.battery;
        } else {
            return 0;
        }
    }

    public String getStatusString() {
        if (isConnected() && mReadingStatus != null) {
            switch (mReadingStatus.status) {
                case ALARM_HIGH:
                case ALARM_LOW:
                case ALARM_OTHER:
                    return getString(R.string.status_text_alarm);
                case ATTEMPTING:
                    return getString(R.string.status_check_attempt, mReadingStatus.attempt, mReadingStatus.maxAttempts);
                case ATTENPT_FAILED:
                    return getString(R.string.status_check_attempt_failed, mReadingStatus.attempt, mReadingStatus.maxAttempts);
                case WAITING:
                    return getString(R.string.status_text_next_check, AlgorithmUtil.format(new Date(mReadingStatus.nextCheck)));
                case NOT_RUNNING:
                    return getString(R.string.status_text_not_running);
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

    public void sendData(String command, String key, String data, ResultCallback<DataApi.DataItemResult> listener) {
        if (isConnected()) WearableApi.sendData(mGoogleApiClient, command, key, data, listener);
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
        void onWatchSettingsUpdated();
    }
}
