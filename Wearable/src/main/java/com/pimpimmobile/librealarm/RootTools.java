package com.pimpimmobile.librealarm;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import com.pimpimmobile.librealarm.shareddata.AlgorithmUtil;
import com.pimpimmobile.librealarm.shareddata.PreferencesUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class RootTools {

    private static final String TAG = "GLUCOSE::" + RootTools.class.getSimpleName();
    private static final boolean DEBUG = AlgorithmUtil.DEBUG;

    private static Boolean sHasRoot = null;
    private static Boolean sScriptsCreated = null;

    private Context mContext;

    private PowerManager.WakeLock mWakeLock;

    private RootHandlerThread mRootHandlerThread;

    public RootTools(Context context) {
        mContext = context;
        createScripts();
        mRootHandlerThread = new RootHandlerThread();
    }

    public synchronized boolean isHasRoot() {
        if (sHasRoot == null) sHasRoot = (new File("/system/xbin/su").exists());
        return sHasRoot;
    }

    private void createScripts() {
        if (sScriptsCreated) return;
        // switches to lowest possible power levels on cpu
        String script_name = mContext.getFilesDir()+"/powersave.sh";
        writeToFile(script_name,"echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor\necho 0 >/sys/devices/system/cpu/cpu1/online\n");

        // restore cpu speed somewhat
        script_name = mContext.getFilesDir()+"/performance.sh";
        writeToFile(script_name,"echo interactive > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor\necho 1 >/sys/devices/system/cpu/cpu1/online\n");


        // disables the touch sense on the keypad by nuking the driver
        script_name = mContext.getFilesDir()+"/killtouch.sh";
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
                "\n");

        sScriptsCreated = true;
    }

    private void writeToFile(String filename, String data) {
        try {
            File the_file = new File(filename);
            // if (!the_file.exists())
            //  {
            FileOutputStream out = new FileOutputStream(the_file);
            out.write(data.getBytes(Charset.forName("UTF-8")));
            out.close();
            // }
        } catch (IOException e) {
            Log.e(TAG, "File write failed: " + e.toString());
        }
    }

    public void executeScripts(boolean state) {
        executeScripts(state,0);
    }

    // platform specific method for enabling/disabling nfc - not sure if there is a better api based method
    public void executeScripts(final boolean state, final long delay) {
        mRootHandlerThread.executeScripts(state, delay);
        final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Nfc-control");
        mWakeLock.acquire(30000);
    }

    public void cancelScripts() {
        mRootHandlerThread.mHandler.removeCallbacksAndMessages(null);
        if (mWakeLock.isHeld()) mWakeLock.release();
    }

    private static String showProcessOutput(Process execute)
    {
        try {
            execute.waitFor();
            if (DEBUG) Log.d(TAG, "PROCESS OUTPUT: " + (new BufferedReader(new InputStreamReader(execute.getInputStream())).readLine()));
            String error = (new BufferedReader(new InputStreamReader(execute.getErrorStream())).readLine());
            if (DEBUG) Log.d(TAG, " PROCESS ERROR: " + error);
            return error;
        } catch (InterruptedException | IOException e) {
            Log.d(TAG, "Got error showing process output: "+e.toString());
        }
        return "other error";
    }

    private class RootHandlerThread extends HandlerThread implements Handler.Callback {

        private Handler mHandler;

        private Boolean sNfcDestinationState = null;

        private RootHandlerThread() {
            super("RootHandlerThread");
            start();
            mHandler = new Handler(getLooper(), this);
        }

        // platform specific method for enabling/disabling nfc - not sure if there is a better api based method
        private void executeScripts(final boolean state, final long delay) {
            Message message = new Message();
            message.what = state ? 1 : 0;
            if (delay > 0) {
                mHandler.sendMessageDelayed(message, delay);
            } else {
                mHandler.sendMessage(message);
            }
        }

        @Override
        public boolean handleMessage(Message msg) {
            boolean state = msg.what == 1;

            try {
                if (sNfcDestinationState != null && sNfcDestinationState != state) {
                    Log.e(TAG,"Destination state changed from: "+state+" to "+ sNfcDestinationState +" .. skipping switch!");
                } else {
                    //final boolean needs_root = true; // unclear at the moment whether we need root for this

                    if (state) {
                        if (DEBUG) Log.d(TAG,"Switching to higher performance cpu speed");
                        final Process execute4 = Runtime.getRuntime().exec("su -c sh "+mContext.getFilesDir()+"/performance.sh");
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
                        if (PreferencesUtil.disableTouchscreen(mContext)) {
                            // TODO check if already disabled
                            if (DEBUG) Log.d(TAG, "Disabling touchscreen!");
                            final Process execute1 = Runtime.getRuntime().exec("su -c sh " + mContext.getFilesDir() + "/killtouch.sh");
                            if (DEBUG) showProcessOutput(execute1);
                        }
                        if (PreferencesUtil.slowCpu(mContext)) {
                            if (DEBUG) Log.d(TAG, "Switching to lower powersave cpu speed");
                            final Process execute2 = Runtime.getRuntime().exec("su -c sh " + mContext.getFilesDir() + "/powersave.sh");
                            if (DEBUG) showProcessOutput(execute2);
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Got exception executing root nfc off: "+e.toString());
            } finally {
                if (mWakeLock.isHeld()) mWakeLock.release();
            }

            sNfcDestinationState = state;

            return true;
        }
    }

}
