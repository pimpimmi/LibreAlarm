package com.pimpimmobile.librealarm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.pimpimmobile.librealarm.shareddata.AlertRules;
import com.pimpimmobile.librealarm.shareddata.AlertRules.Danger;
import com.pimpimmobile.librealarm.shareddata.AlgorithmUtil;
import com.pimpimmobile.librealarm.shareddata.PredictionData;
import com.pimpimmobile.librealarm.shareddata.PreferencesUtil;
import com.pimpimmobile.librealarm.shareddata.ReadingData;
import com.pimpimmobile.librealarm.shareddata.Status;
import com.pimpimmobile.librealarm.shareddata.Status.Type;
import com.pimpimmobile.librealarm.shareddata.WearableApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;

public class WearActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener, MessageApi.MessageListener {

    private static final String TAG = "GLUCOSE::" + WearActivity.class.getSimpleName();

    public static final String EXTRA_CANCEL_ALARM = "cancel_alarm";

    public static final int MAX_ATTEMPTS = 5;

    private GoogleApiClient mGoogleApiClient;

    private NfcAdapter mNfcAdapter;

    private PowerManager.WakeLock mWakeLock;

    private Handler mHandler = new Handler();

    private Vibrator mVibrator;

    private boolean hasRoot = false;
    private boolean checkedForRoot = false;

    private static boolean scripts_created = false;
    private static final boolean d = true; // global debug output flag
    private static long busy = -1;
    private static boolean tag_discovered = false;
    private static int batteryLevel = -1;
    private static Boolean nfcDestinationState = null;

    // We can't finish activity until all messages have been sent.
    private int mMessagesBeingSent;
    private boolean mFinishAfterSentMessages = false;

    private ResultCallback<MessageApi.SendMessageResult> mMessageListener =
            new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "messagesbeingsent: " + mMessagesBeingSent + ", finish? " + mFinishAfterSentMessages);
                            if (--mMessagesBeingSent <= 0 && mFinishAfterSentMessages) {
                                finish();
                            }
                        }
                    });
                }
            };

    private Runnable mStopActivityRunnable = new Runnable() {
        @Override
        public void run() {
            int retries = PreferencesUtil.getRetries(WearActivity.this);
            mFinishAfterSentMessages = true;
            if (retries >= MAX_ATTEMPTS) {
                PreferencesUtil.setRetries(WearActivity.this, 1);
                setNextAlarm();
                sendResultAndFinish();
                if (shouldDoErrorAlarm()) {
                    doAlarm(Type.ALARM_OTHER, -1, AlgorithmUtil.TrendArrow.UNKNOWN);
                } else {
                    sendStatusUpdate(Type.WAITING);
                }
            } else {
                sendStatusUpdate(Type.ATTENPT_FAILED);
                PreferencesUtil.setRetries(WearActivity.this, ++retries);
            }
            rootSwitchNFC(false);
            clearBusy();
        }
    };

    private boolean shouldDoErrorAlarm() {
        return PreferencesUtil.increaseErrorsInARow(this) >=
                PreferencesUtil.errInRowForAlarm(this);
    }

    private ReadingData mResult = new ReadingData(PredictionData.Result.ERROR_NO_NFC);

    public static void clearBusy() {
        busy = -1;
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        Log.i(TAG, "onCreate()");
        if (getIntent().hasExtra(EXTRA_CANCEL_ALARM)) {
            finish();
            return;
        }
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.wear_activity);

        createScripts();

        final long time_now = System.currentTimeMillis();
        if (time_now > busy) {
            busy =  time_now+120000; // maximum time we can lock for
            PowerManager manager = (PowerManager) getSystemService(POWER_SERVICE);

            // TODO wakelock management needs reviewing - is there possiblity to arrive here with this wakelock already held?
            if ((mWakeLock != null) && (mWakeLock.isHeld())) mWakeLock.release();
            mWakeLock = manager.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");

            mWakeLock.acquire(60000);

            // If attempt fails for some reason, retry in 20 seconds.
            if (PreferencesUtil.getIsStarted(this)) AlarmReceiver.post(this, 20000);

            Log.d(TAG, "busy set true");
            rootSwitchNFC(true); // turn it on

            BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
            batteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            mGoogleApiClient.connect();
            Log.d(TAG, "NFC Initialization");
            NfcManager nfcManager =
                    (NfcManager) WearActivity.this.getSystemService(Context.NFC_SERVICE);
            mNfcAdapter = nfcManager.getDefaultAdapter();
           // mNfcAdapter.disableForegroundDispatch(this);
            if (mNfcAdapter !=null) {
                Log.d(TAG, "Got NFC adpater");
                int counter = 0;
                try {
                    // null pointer can trigger here from the systemapi
                    while ((!mNfcAdapter.isEnabled() && counter < 9)) {
                        Log.d(TAG, "nfc turn on wait: " + counter);
                        try {
                            // quick and very dirty
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            //
                        }
                        counter++;
                    }


                //mNfcAdapter.disableReaderMode(WearActivity.this);
                Log.d(TAG, "About to discover tag");
                tag_discovered = false;
                mNfcAdapter.enableReaderMode(WearActivity.this, new NfcAdapter.ReaderCallback() {
                    @Override
                    public void onTagDiscovered(Tag tag) {
                        if (!tag_discovered) {
                            tag_discovered = true;
                            Log.d(TAG, "NFC tag discovered - going to read data");
                            new NfcVReaderTask().execute(tag);
                            // mNfcAdapter.disableReaderMode(WearActivity.this); // this seems to make it less reliable than multiple discoveries
                        } else {
                            Log.d(TAG, "Tag already discovered!");
                        }
                    }
                }, NfcAdapter.FLAG_READER_NFC_V | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null);
                } catch (NullPointerException e)
                {
                    Log.wtf(TAG,"Null pointer exception from NFC subsystem: "+e.toString());
                    // TODO do we actually need to reboot watch here after some counter of failures without resolution?
                }
            } else {
                Log.e(TAG,"nfcAdapter is NULL!!");
            }
            // If NFC reading hasn't been completed within 10 seconds, close the app.
            mHandler.postDelayed(mStopActivityRunnable, 10000);
        } else {
            Log.d(TAG, "busy is true! not proceeding");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_CANCEL_ALARM)) {
            sendStatusUpdate(Type.WAITING);
            mFinishAfterSentMessages = true;
        }
        super.onNewIntent(intent);
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        if (mHandler != null) mHandler.removeCallbacksAndMessages(null);

        if (mVibrator != null) mVibrator.cancel();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableReaderMode(this);
        }
        if ((mGoogleApiClient != null) && (mGoogleApiClient.isConnected())) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        rootSwitchNFC(false, 15000);
        if ((mWakeLock != null) && (mWakeLock.isHeld())) mWakeLock.release();
        finish();
        super.onStop();
    }

    private void setNextAlarm() {
        if (PreferencesUtil.getIsStarted(this)) {
            AlarmReceiver.post(this, AlarmReceiver.DEFAULT_INTERVAL);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "is connected!!");
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        sendStatusUpdate(Type.ATTEMPTING);
    }

    private void sendAlarmStatusUpdate(Type type, int value, AlgorithmUtil.TrendArrow trendArrow) {
        int attempt = PreferencesUtil.getRetries(this);
        Status status = new Status(type, attempt, WearActivity.MAX_ATTEMPTS,
                AlarmReceiver.getNextCheck(mGoogleApiClient.getContext()), value, trendArrow);
        sendStatusUpdate(type, status);
    }

    private void sendStatusUpdate(Type type) {
        int attempt = PreferencesUtil.getRetries(this);
        Status status = new Status(type, attempt, WearActivity.MAX_ATTEMPTS,
                AlarmReceiver.getNextCheck(mGoogleApiClient.getContext()),batteryLevel,isHasRoot());
        sendStatusUpdate(type, status);
    }

    private void sendStatusUpdate(Type type, Status status) {
        PreferencesUtil.setCurrentType(this, type);
        mMessagesBeingSent++;
        WearableApi.sendMessage(mGoogleApiClient, WearableApi.STATUS, new Gson().toJson(status), mMessageListener);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        DataLayerListenerService.handleMessage(mGoogleApiClient, event);
    }

    /**
     * Disables touch
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return true;
    }

    private void doAlarm(Type type, int value, AlgorithmUtil.TrendArrow arrow) {
        mFinishAfterSentMessages = false;
        sendAlarmStatusUpdate(type, value, arrow);
        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        mVibrator.vibrate(30000);
        // Close app after it has vibrated for 30 seconds.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mFinishAfterSentMessages = true;
                sendStatusUpdate(Type.WAITING);
            }
        }, 30000);
    }

    private void sendResultAndFinish() {
        SimpleDatabase database = new SimpleDatabase(this);
        long id = database.saveMessage(mResult);
        ReadingData.TransferObject transferObject = new ReadingData.TransferObject(id, mResult);
        database.close();
        WearableApi.sendMessage(mGoogleApiClient, WearableApi.GLUCOSE, new Gson().toJson(transferObject), mMessageListener);
        mMessagesBeingSent++;
        mFinishAfterSentMessages = true;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + connectionResult);
    }

    private synchronized boolean detectRoot()
    {
        checkedForRoot=true;
        return (new File("/system/xbin/su").exists());
    }
    private boolean isHasRoot()
    {
        if (!checkedForRoot) hasRoot = detectRoot();
        return hasRoot;
    }

    private void createScripts()
    {
        if (scripts_created) return;
        // switches to lowest possible power levels on cpu
        String script_name = getFilesDir()+"/powersave.sh";
        writeToFile(script_name,"echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor\necho 0 >/sys/devices/system/cpu/cpu1/online\n",getApplicationContext());

        // restore cpu speed somewhat
        script_name = getFilesDir()+"/performance.sh";
        writeToFile(script_name,"echo interactive > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor\necho 1 >/sys/devices/system/cpu/cpu1/online\n",getApplicationContext());


        // disables the touch sense on the keypad by nuking the driver
        script_name=getFilesDir()+"/killtouch.sh";
        writeToFile(script_name,"echo synaptics_dsx.0 >/sys/bus/platform/drivers/synaptics_dsx/unbind\n" +
                "a=`grep -l ^/system/bin/key_sleep_vibrate_service /proc/*/cmdline`\n" +
                "if [ \"$a\" != \"\" ]\n" +
                "then\n" +
                "let p=6\n" +
                "while [ $p -lt 14 ] && [ \"${a:$p:1}\" != \"/\" ] \n" +
                "do\n" +
                "let p=$p+1\n" +
                "done\n" +
                "let l=$p-6\n" +
                "b=\"${a:6:$l}\"\n" +
                "echo \"$b\"\n" +
                "kill \"$b\"\n" +
                "fi\n" +
                "\n",getApplicationContext());


        scripts_created = true;
    }

    private void writeToFile(String filename,String data,Context context) {
        try {
            File the_file = new File(filename);
           // if (!the_file.exists())
          //  {
                FileOutputStream out = new FileOutputStream(the_file);
                out.write(data.getBytes(Charset.forName("UTF-8")));
                out.close();
           // }
        }
        catch (IOException e) {
            Log.e(TAG, "File write failed: " + e.toString());
        }
    }
    private void rootSwitchNFC(boolean state)
    {
        rootSwitchNFC(state,0);
    }
    // platform specific method for enabling/disabling nfc - not sure if there is a better api based method
    private void rootSwitchNFC(final boolean state, final long delay) {
        nfcDestinationState=state;
        Log.d(TAG, "Setting NFC hardware to state: " + (state ? "ON" : "OFF") + ((delay==0) ? " now" : " after: "+delay));
        new Thread() {
            @Override
            public void run() {
                final PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Nfc-control");
                wl.acquire(30000);
                try {
                    if (delay>0)
                    {
                        Log.d(TAG,"Sleeping for "+delay+" ms");
                        Thread.sleep(delay);
                        Log.d(TAG,"Sleep finished");
                    }
                    if ((nfcDestinationState!=null)&&(nfcDestinationState!=state))
                    {
                        Log.e(TAG,"Destination state changed from: "+state+" to "+nfcDestinationState+" .. skipping switch!");
                    } else {
                        //final boolean needs_root = true; // unclear at the moment whether we need root for this

                        if (state)
                        {
                            if (d) Log.d(TAG,"Switching to higher performance cpu speed");
                            final Process execute4 = Runtime.getRuntime().exec("su -c sh "+getFilesDir()+"/performance.sh");
                        }

                        for (int counter=0;counter<5;counter++) {
                            final Process execute = Runtime.getRuntime().exec("su -c service call nfc " + (state ? "6" : "5")); // turn NFC on or off
                            if (showProcessOutput(execute) != null) {
                                Log.e(TAG, "Got error- retrying.."+counter);
                            } else {
                               break;
                            }
                        }

                        if (!state) {
                            if (PreferencesUtil.disableTouchscreen(getApplicationContext())) {
                                // TODO check if already disabled
                                if (d) Log.d(TAG, "Disabling touchscreen!");
                                final Process execute1 = Runtime.getRuntime().exec("su -c sh " + getFilesDir() + "/killtouch.sh");
                                if (d) showProcessOutput(execute1);
                            }
                            if (PreferencesUtil.slowCpu(getApplicationContext())) {
                                if (d) Log.d(TAG, "Switching to lower powersave cpu speed");
                                final Process execute2 = Runtime.getRuntime().exec("su -c sh " + getFilesDir() + "/powersave.sh");
                                if (d) showProcessOutput(execute2);
                            }
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Got exception executing root nfc off: "+e.toString());
                } finally {
                    if (wl.isHeld()) wl.release();
                }
            }
        }.start();
    }

    private static String showProcessOutput(Process execute)
    {
            try {
                Thread.sleep(1000);
                if (d) Log.d(TAG, "PROCESS OUTPUT: " + (new BufferedReader(new InputStreamReader(execute.getInputStream())).readLine()));
                String error = (new BufferedReader(new InputStreamReader(execute.getErrorStream())).readLine());
                if (d) Log.d(TAG, " PROCESS ERROR: " + error);
                return error;
            } catch (InterruptedException | IOException e)
            {
                Log.d(TAG,"Got error showing process output: "+e.toString());
            }
        return "other error";
    }

    private class NfcVReaderTask extends AsyncTask<Tag, Void, Tag> {

        private byte[] data = new byte[360];

        @Override
        protected void onPostExecute(Tag tag) {
            try {
                if (d) Log.d(TAG, "NFC Reader task done - disabling read mode");
                mNfcAdapter.disableReaderMode(WearActivity.this);
                if (d) Log.d(TAG, "NFC read mode disabled");
                if (tag == null) return;
                String tagId = bytesToHexString(tag.getId());
                int attempt = PreferencesUtil.getRetries(WearActivity.this);
                mResult = AlgorithmUtil.parseData(attempt, tagId, data);
                PreferencesUtil.setRetries(WearActivity.this, 1);
                PreferencesUtil.resetErrorsInARow(WearActivity.this);
                sendResultAndFinish();
                setNextAlarm();
                Danger danger = AlertRules.check(WearActivity.this, mResult.prediction);
                if (Danger.NOTHING != danger) {
                    mHandler.removeCallbacks(mStopActivityRunnable);
                    Type type = danger == Danger.LOW ? Type.ALARM_LOW : Type.ALARM_HIGH;
                    doAlarm(type, mResult.prediction.glucoseLevel,
                            AlgorithmUtil.getTrendArrow(mResult.prediction));
                } else {
                    sendStatusUpdate(Type.WAITING);
                }

            } catch (IllegalStateException e) {
                Log.e(TAG, "Illegal state exception in postExecute: " + e);

            } finally {
                rootSwitchNFC(false); // turn it off
                clearBusy();
            }
        }

        @Override
        protected Tag doInBackground(Tag... params) {
            Tag tag = params[0];
            NfcV nfcvTag = NfcV.get(tag);
            if (d) Log.d(TAG, "Attempting to read tag data");
            try {
                nfcvTag.connect();
                final byte[] uid = tag.getId();
                for (int i = 0; i <= 40; i++) {
                    byte[] cmd = new byte[]{0x60, 0x20, 0, 0, 0, 0, 0, 0, 0, 0, (byte) i, 0};
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
                    System.arraycopy(oneBlock, 0, data, i * 8, 8);
                }
                Log.d(TAG, "GOT TAG DATA!");
            } catch (Exception e) {
                Log.i(TAG, e.toString());
                return null;
            } finally {
                try {
                    nfcvTag.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing tag!");
                }
            }
            Log.d(TAG, "Tag data reader exiting");
            return tag;
        }
    }

    private String bytesToHexString(byte[] src) {
        StringBuilder builder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return "";
        }

        char[] buffer = new char[2];
        for (byte b : src) {
            buffer[0] = Character.forDigit((b >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(b & 0x0F, 16);
            builder.append(buffer);
        }

        return builder.toString();
    }
}
