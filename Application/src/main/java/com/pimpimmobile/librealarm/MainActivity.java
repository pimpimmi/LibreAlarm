package com.pimpimmobile.librealarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pimpimmobile.librealarm.shareddata.AlgorithmUtil;
import com.pimpimmobile.librealarm.shareddata.GlucoseData;
import com.pimpimmobile.librealarm.shareddata.PredictionData;
import com.pimpimmobile.librealarm.shareddata.Status;
import com.pimpimmobile.librealarm.shareddata.Status.Type;
import com.pimpimmobile.librealarm.shareddata.WearableApi;
import com.pimpimmobile.librealarm.shareddata.settings.GlucoseUnitSettings;
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
    private SettingsView mSettingsView;
    private GlucoseUnitSettings mGlucoseUnitSettings;
    private boolean mIsFirstStartup;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((WearService.WearServiceBinder) service).getService();
            mService.setListener(MainActivity.this, MainActivity.this);
            onDataUpdated();
            mService.getDatabase().setListener(MainActivity.this);
            mService.getUpdate();
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
        mSettingsView = (SettingsView) findViewById(R.id.settings);
        mGlucoseUnitSettings = (GlucoseUnitSettings)
                mSettingsView.settingsMap.get(GlucoseUnitSettings.class.getSimpleName());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mStatusTextView = (TextView) layout.findViewById(R.id.status_view);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("first_startup", true)) {
            showDisclaimer(true);
            mIsFirstStartup = true;
        }

        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_close,
                R.string.drawer_open) {
            public void onDrawerClosed(View view) {
                HashMap<String, String> settings = SettingsUtils.getTransferHashMap(mSettingsView.settingsMap);
                mService.sendData(WearableApi.SETTINGS, settings, null);
                mSettingsView.settingsMap.get(PostponeSettings.class.getSimpleName()).setSettingsValue("");
                SettingsUtils.saveSettings(MainActivity.this, settings);
            }

            public void onDrawerOpened(View drawerView) {
            }
        };
        drawerLayout.setDrawerListener(mDrawerToggle);

        bindService(new Intent(this, WearService.class), mConnection, BIND_AUTO_CREATE);
        startService(new Intent(this, WearService.class));

        mProgressBar = (ProgressBar) layout.findViewById(R.id.progress);
        mTriggerGlucoseButton = layout.findViewById(R.id.trigger_glucose);
        mTriggerGlucoseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.sendMessage(WearableApi.TRIGGER_GLUCOSE, "", null);
            }
        });
        mActionButton = (Button) layout.findViewById(R.id.action);
        mActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService != null) {
                    if (mService.isAlarmRunning()) {
                        mService.sendMessage(WearableApi.CANCEL_ALARM, "", null);
                        mService.stopAlarm();
                        mActionButton.setText(R.string.button_stop);
                    } else if (mService.getReadingStatus().status == Type.NOT_RUNNING) {
                        mService.start();
                    } else {
                        mService.stop();
                    }
                }
            }
        });

        mAdapter = new HistoryAdapter(this, this, mGlucoseUnitSettings);
        RecyclerView recyclerView = (RecyclerView) layout.findViewById(R.id.history);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter.setHistory(null);
    }

    private void showDisclaimer(final boolean mustBePositive) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mustBePositive) {
                            PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit()
                                    .putBoolean("first_startup", false).apply();
                        }
                    }
                })
                .setTitle(R.string.disclaimer_title)
                .setMessage(R.string.disclaimer_message);
        if (mustBePositive) {
            dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            dialogBuilder.setOnKeyListener(new Dialog.OnKeyListener() {

                @Override
                public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        finish();
                    }
                    return true;
                }
            });
        }
        AlertDialog dialog = dialogBuilder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == R.id.disclaimer) {
            showDisclaimer(false);
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
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
        mProgressBar.setVisibility((status != null && status.status == Type.ATTEMPTING) ? View.VISIBLE : View.GONE);
        if (status != null && mService.isConnected()) {
            switch (status.status) {
                case ALARM:
                    mActionButton.setText(R.string.button_alarm);
                    mTriggerGlucoseButton.setVisibility(View.GONE);
                    break;
                case ATTEMPTING:
                case ATTENPT_FAILED:
                case WAITING:
                    mActionButton.setText(android.R.string.cancel);
                    mTriggerGlucoseButton.setVisibility(View.VISIBLE);
                    break;
                case NOT_RUNNING:
                    mActionButton.setText(R.string.button_start);
                    mTriggerGlucoseButton.setVisibility(View.GONE);
                    break;
            }
        } else {
            mActionButton.setText(R.string.button_wait);
            mTriggerGlucoseButton.setVisibility(View.GONE);
        }
        if (mIsFirstStartup && status == null) {
            mStatusTextView.setText(R.string.status_message_first_startup);
        } else {
            mStatusTextView.setText(mService.getStatusString());
        }
    }

    @Override
    public void onDatabaseChange() {
        mAdapter.setHistory(mService.getDatabase().getPredictions());
    }

    @Override
    public void onAdapterItemClicked(PredictionData predictionData) {
        String s = "";
        if (predictionData.glucoseLevel == -1) { // ERR
            s = getString(R.string.err_explanation);
        } else {
            for (GlucoseData data : mService.getDatabase().getTrend(predictionData.phoneDatabaseId)) {
                s += AlgorithmUtil.format(new Date(data.realDate)) +
                        ": " + data.glucose(mGlucoseUnitSettings.isMmol()) + "\n";
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this).setPositiveButton(android.R.string.ok, null)
                .setTitle("").setMessage(s).create();
        dialog.show();
    }
}
