package org.pyload.android.client;

import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import android.view.MenuItem;

public class RemoteSettings extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remote_settings);

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
