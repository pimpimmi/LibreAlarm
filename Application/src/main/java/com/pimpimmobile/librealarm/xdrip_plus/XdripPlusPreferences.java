package com.pimpimmobile.librealarm.xdrip_plus;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.pimpimmobile.librealarm.R;

import java.util.HashMap;

/**
 * Created by jamorham on 06/09/2016.
 */

public class XdripPlusPreferences extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private HashMap<String, String> mChanged = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.xdrip_plus_preferences);
        }
    }

    @Override
    protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        setResult();
        super.onBackPressed();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mChanged.put(key, sharedPreferences.getAll().get(key).toString());
    }

    private void setResult() {
        Intent result = new Intent();
        Bundle bundle = new Bundle();
        for (String key : mChanged.keySet()) {
            bundle.putString(key, mChanged.get(key));
        }
        result.putExtra("result", bundle);
        setResult(RESULT_OK, result);
    }
}

