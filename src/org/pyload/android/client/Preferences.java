package org.pyload.android.client;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

public class Preferences extends PreferenceActivity {

    public static final String CHECK_CAPTCHA_SERVICE_ENABLE = "check_captcha_bg";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            if (pyLoadApp.isActionBarAvailable()) {
                getActionBar().setHomeButtonEnabled(true);
                getActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
            case android.R.id.home:
                finish();
            }
            return true;
        }

}
