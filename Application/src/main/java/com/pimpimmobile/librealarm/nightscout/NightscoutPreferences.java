package com.pimpimmobile.librealarm.nightscout;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.pimpimmobile.librealarm.R;

public class NightscoutPreferences extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.nightscout_preferences);
        }
    }
}
