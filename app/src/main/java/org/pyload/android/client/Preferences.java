package org.pyload.android.client;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

public class Preferences extends AppCompatActivity {
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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }
    }
}
