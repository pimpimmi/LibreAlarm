package com.pimpimmobile.librealarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.pimpimmobile.librealarm.shareddata.AlgorithmUtil;
import com.pimpimmobile.librealarm.shareddata.GlucoseData;
import com.pimpimmobile.librealarm.shareddata.WearableApi;
import com.pimpimmobile.librealarm.shareddata.settings.PostponeSettings;
import com.pimpimmobile.librealarm.shareddata.settings.SettingsUtils;

import java.util.Date;

public class MainActivity extends Activity implements WearService.WearServiceListener, SimpleDatabase.DatabaseListener, HistoryAdapter.ShowDialogListener {

    private static final String TAG = "GLUCOSE::" + MainActivity.class.getSimpleName();

    private View mTriggerGlucoseButton;
    private TextView mNextGlucoseTextView;
    private HistoryAdapter mAdapter;
    private View mCancelAlarmTextView;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private WearService mService;
    private TextView mErrorView;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((WearService.WearServiceBinder) service).getService();
            mService.setListener(MainActivity.this, MainActivity.this);
            if (mService.isConnected()) {
                onConnected();
                onDataUpdated();
            }
            mService.getDatabase().setListener(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            onDisconnected();
            mService.getDatabase().setListener(MainActivity.this);
            mService.setListener(null, null);
            mService = null;
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.main_activity);

        ViewGroup layout = (ViewGroup) getLayoutInflater().inflate(R.layout.main_content, null);
        ((FrameLayout) findViewById(R.id.content_frame)).addView(layout);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final SettingsView settingsView = (SettingsView) findViewById(R.id.drawer);

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_close,
                R.string.drawer_open) {
            public void onDrawerClosed(View view) {
                String transferString =
                        SettingsUtils.createSettingsTransferString(settingsView.settingsMap.values());
                SettingsUtils.saveSettings(MainActivity.this, transferString);
                mService.sendData(WearableApi.SETTINGS, transferString, null);
                long nextAlarmTime = ((PostponeSettings)
                        settingsView.settingsMap.get(PostponeSettings.class.getSimpleName())).time;
                if (nextAlarmTime != -1) {
                    mService.setNextCheck(AlgorithmUtil.format(new Date(nextAlarmTime)));
                }
            }

            public void onDrawerOpened(View drawerView) {
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        bindService(new Intent(this, WearService.class), mConnection, BIND_AUTO_CREATE);

        mNextGlucoseTextView = (TextView) layout.findViewById(R.id.next_glucose);
        mTriggerGlucoseButton = layout.findViewById(R.id.trigger_glucose);
        mTriggerGlucoseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendMessage(WearableApi.TRIGGER_GLUCOSE, "",
                        new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Toast.makeText(MainActivity.this, "Trigger failed: " + sendMessageResult.
                                    getStatus().getStatusMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        mCancelAlarmTextView = layout.findViewById(R.id.cancel_alarm);
        mCancelAlarmTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendMessage(WearableApi.CANCEL_ALARM, "", null);
                mService.stopAlarm();
                v.setVisibility(View.GONE);
            }
        });

        mErrorView = (TextView) layout.findViewById(R.id.error_text);

        mAdapter = new HistoryAdapter(this, this);
        RecyclerView recyclerView = (RecyclerView) layout.findViewById(R.id.history);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter.setHistory(null);
    }

    @Override
    protected void onResume() {
        if (mService != null) onDataUpdated();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        unbindService(mConnection);
        if (mService != null) mService.setListener(null, null);
        super.onDestroy();
    }

    @Override
    public void onConnected() {
        mTriggerGlucoseButton.setEnabled(true);
        onDataUpdated();
    }

    @Override
    public void onDisconnected() {
        mTriggerGlucoseButton.setEnabled(false);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onDataUpdated() {
        onDatabaseChange();
        mNextGlucoseTextView.setText(mService.getNextCheck());
        if (mService.isAlarmPlaying()) {
            mCancelAlarmTextView.setVisibility(View.VISIBLE);
        } else {
            mCancelAlarmTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDatabaseChange() {
        mAdapter.setHistory(mService.getDatabase().getPredictions());
    }

    @Override
    public void showDialog(long id) {
        String s = "";
        for (GlucoseData data : mService.getDatabase().getTrend(id)) {
            s += AlgorithmUtil.format(new Date(data.realDate)) +
                    ": " + data.mmolGlucose() + "\n";
        }
        AlertDialog dialog = new AlertDialog.Builder(this).setPositiveButton("OK", null)
                .setTitle("").setMessage(s).create();
        dialog.show();
    }
}
