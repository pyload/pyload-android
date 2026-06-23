package org.pyload.android.client;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

public class Preferences extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        android.content.SharedPreferences prefs = newBase.getSharedPreferences(newBase.getPackageName() + "_preferences", android.content.Context.MODE_PRIVATE);
        String language = prefs.getString("language", "");
        if (!language.isEmpty()) {
            java.util.Locale locale = new java.util.Locale(language);
            android.content.res.Configuration config = new android.content.res.Configuration(newBase.getResources().getConfiguration());
            config.setLocale(locale);
            newBase = newBase.createConfigurationContext(config);
        }
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.preferences_container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        Fragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.getKey());
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.preferences_container, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            Preference themePreference = findPreference("theme");
            if (themePreference != null) {
                themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    pyLoadApp.applyTheme((String) newValue);
                    return true;
                });
            }

            updateUrlSummary();
        }

        @Override
        public void onResume() {
            super.onResume();
            if (getPreferenceManager().getSharedPreferences() != null) {
                getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            }
            updateUrlSummary();
        }

        @Override
        public void onPause() {
            super.onPause();
            if (getPreferenceManager().getSharedPreferences() != null) {
                getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
            if ("host".equals(key) || "port".equals(key) || "ssl".equals(key) || "path_prefix".equals(key)) {
                updateUrlSummary();
            }
        }

        private void updateUrlSummary() {
            Preference serverConnection = findPreference("server_connection");
            if (serverConnection != null) {
                SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
                if (prefs != null) {
                    String host = prefs.getString("host", "");
                    String port = prefs.getString("port", "8000");
                    String pathPrefix = prefs.getString("path_prefix", "");

                    if (!pathPrefix.startsWith("/") && !pathPrefix.isEmpty()) {
                        pathPrefix = "/" + pathPrefix;
                    }
                    if (pathPrefix.endsWith("/")) {
                        pathPrefix = pathPrefix.substring(0, pathPrefix.length() - 1);
                    }

                    boolean ssl = prefs.getBoolean("ssl", false);
                    String protocol = ssl ? "https://" : "http://";
                    serverConnection.setSummary(protocol + host + ":" + port + pathPrefix);
                }
            }
        }
    }
}
