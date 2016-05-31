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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.pimpimmobile.librealarm.shareddata.AlgorithmUtil;
import com.pimpimmobile.librealarm.shareddata.GlucoseData;
import com.pimpimmobile.librealarm.shareddata.Status;
import com.pimpimmobile.librealarm.shareddata.Status.Type;
import com.pimpimmobile.librealarm.shareddata.WearableApi;
import com.pimpimmobile.librealarm.shareddata.settings.PostponeSettings;
import com.pimpimmobile.librealarm.shareddata.settings.SettingsUtils;

import java.util.Date;
import java.util.HashMap;

public class MainActivity extends Activity implements WearService.WearServiceListener, SimpleDatabase.DatabaseListener, HistoryAdapter.OnListItemClickedListener {

    private static final String TAG = "GLUCOSE::" + MainActivity.class.getSimpleName();

    private View mTriggerGlucoseButton;
    private TextView mStatusTextView;
    private HistoryAdapter mAdapter;
    private Button mActionButton;
    private ActionBarDrawerToggle mDrawerToggle;
    private WearService mService;
    private ProgressBar mProgressBar;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((WearService.WearServiceBinder) service).getService();
            mService.setListener(MainActivity.this, MainActivity.this);
            onDataUpdated();
            mService.getDatabase().setListener(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            onDataUpdated();
            mService.getDatabase().setListener(MainActivity.this);
            mService.setListener(null, null);
            mService = null;
        }
    };

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.main_activity);

        ViewGroup layout = (ViewGroup) getLayoutInflater().inflate(R.layout.main_content, null);
        ((FrameLayout) findViewById(R.id.content_frame)).addView(layout);

        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final SettingsView settingsView = (SettingsView) findViewById(R.id.drawer);

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_close,
                R.string.drawer_open) {
            public void onDrawerClosed(View view) {
                HashMap<String, String> settings = SettingsUtils.getTransferHashMap(settingsView.settingsMap);
                SettingsUtils.saveSettings(MainActivity.this, settings);
                mService.sendData(WearableApi.SETTINGS, settings, null);
                settingsView.settingsMap.get(PostponeSettings.class.getSimpleName()).setSettingsValue("");
            }

            public void onDrawerOpened(View drawerView) {
            }
        };
        drawerLayout.setDrawerListener(mDrawerToggle);

        bindService(new Intent(this, WearService.class), mConnection, BIND_AUTO_CREATE);

        mStatusTextView = (TextView) layout.findViewById(R.id.status_view);
        mProgressBar = (ProgressBar) layout.findViewById(R.id.progress);
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
        mActionButton = (Button) layout.findViewById(R.id.action);
        mActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (mActionButton.getText().toString().toLowerCase()) {
                    case "alarm":
                        mService.sendMessage(WearableApi.CANCEL_ALARM, "", null);
                        mService.stopAlarm();
                        boolean notRunning = mService.getReadingStatus().status == Type.NOT_RUNNING;
                        mActionButton.setText(notRunning ? "START" : "STOP");
                        break;
                    case "start":
                        mService.start();
                        break;
                    case "stop":
                        mService.stop();
                        break;
                }
            }
        });

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
        Status status = mService.getReadingStatus();
        mProgressBar.setVisibility((status != null && status.status == Type.ATTEMPTING) ? View.VISIBLE : View.INVISIBLE);
        if (status != null && mService.isConnected()) {
            switch (status.status) {
                case ALARM:
                    mActionButton.setText("ALARM");
                    break;
                case ATTEMPTING:
                case ATTENPT_FAILED:
                case WAITING:
                    mActionButton.setText("STOP");
                    break;
                case NOT_RUNNING:
                    mActionButton.setText("START");
            }
        } else {
            mActionButton.setText("WAIT");
        }
        mActionButton.setEnabled(mService != null && mService.isConnected());
        mTriggerGlucoseButton.setEnabled(mService != null && mService.isConnected());
        mStatusTextView.setText(mService.getStatusString());
    }

    @Override
    public void onDatabaseChange() {
        mAdapter.setHistory(mService.getDatabase().getPredictions());
    }

    @Override
    public void onAdapterItemClicked(long id) {
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
