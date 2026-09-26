package org.pyload.android.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import org.pyload.android.client.models.Server;
import org.pyload.android.client.module.LanguageUtils;
import org.pyload.android.client.module.ServerManager;

import java.util.Locale;

public class ServerEditActivity extends AppCompatActivity {

    public static final String EXTRA_SERVER_ID = "server_id";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageUtils.attachBaseContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        String serverId = getIntent().getStringExtra(EXTRA_SERVER_ID);
        boolean isEdit = serverId != null;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(isEdit ? R.string.edit_server : R.string.add_server);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (savedInstanceState == null) {
            ServerEditFragment fragment = new ServerEditFragment();
            Bundle args = new Bundle();
            if (serverId != null) {
                args.putString(EXTRA_SERVER_ID, serverId);
            }
            fragment.setArguments(args);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.preferences_container, fragment)
                    .commit();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.preferences_container), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.server_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_save) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.preferences_container);
            if (fragment instanceof ServerEditFragment editFragment) {
                editFragment.saveAndFinish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class ServerEditFragment extends PreferenceFragmentCompat {
        private Server server;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.server_edit_preferences, rootKey);

            String sId = getArguments() != null ? getArguments().getString(EXTRA_SERVER_ID) : null;
            if (sId != null) {
                server = ServerManager.getInstance(requireContext()).getServer(sId);
            }
            if (server == null) {
                server = new Server(null, "", "", "8000", "", false, true, "");
            }

            EditTextPreference namePref = findPreference("server_name");
            EditTextPreference hostPref = findPreference("host");
            EditTextPreference portPref = findPreference("port");
            EditTextPreference pathPrefixPref = findPreference("path_prefix");
            CheckBoxPreference sslPref = findPreference("ssl");
            CheckBoxPreference sslValidatePref = findPreference("ssl_validate");
            EditTextPreference apiKeyPref = findPreference("api_key");

            if (namePref != null) {
                namePref.setText(server.getName());
                namePref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
            }
            if (hostPref != null) {
                hostPref.setText(server.getHost());
                hostPref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
            }
            if (portPref != null) {
                portPref.setText(server.getPort());
                portPref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
            }
            if (pathPrefixPref != null) {
                pathPrefixPref.setText(server.getPathPrefix());
                pathPrefixPref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
            }
            if (sslPref != null) {
                sslPref.setChecked(server.isSsl());
            }
            if (sslValidatePref != null) {
                sslValidatePref.setChecked(server.isSslValidate());
            }
            if (apiKeyPref != null) {
                apiKeyPref.setText(server.getApiKey());
                apiKeyPref.setSummaryProvider(preference -> {
                    String val = ((EditTextPreference) preference).getText();
                    if (val == null || val.isEmpty()) {
                        return getString(R.string.api_key_desc);
                    }
                    return "••••••••";
                });
            }
        }

        public void saveAndFinish() {
            if (getContext() == null) return;
            EditTextPreference namePref = findPreference("server_name");
            EditTextPreference hostPref = findPreference("host");
            EditTextPreference portPref = findPreference("port");
            EditTextPreference pathPrefixPref = findPreference("path_prefix");
            CheckBoxPreference sslPref = findPreference("ssl");
            CheckBoxPreference sslValidatePref = findPreference("ssl_validate");
            EditTextPreference apiKeyPref = findPreference("api_key");

            String serverName = namePref != null && namePref.getText() != null ? namePref.getText().trim() : "";
            String host = hostPref != null && hostPref.getText() != null ? hostPref.getText().trim() : "";
            String port = portPref != null && portPref.getText() != null ? portPref.getText().trim() : "";
            String pathPrefix = pathPrefixPref != null && pathPrefixPref.getText() != null ? pathPrefixPref.getText().trim() : "";
            boolean ssl = sslPref != null && sslPref.isChecked();
            boolean sslValidate = sslValidatePref == null || sslValidatePref.isChecked();
            String apiKey = apiKeyPref != null && apiKeyPref.getText() != null ? apiKeyPref.getText().trim() : "";

            if (serverName.isEmpty() || host.isEmpty() || port.isEmpty() || apiKey.isEmpty()) {
                pyLoadApp app = (pyLoadApp) requireActivity().getApplicationContext();
                app.showCenteredSnackbar(R.string.fill_required_fields, Snackbar.LENGTH_SHORT);
                return;
            }

            ServerManager sm = ServerManager.getInstance(getContext());
            if (server.getId() != null && sm.getServer(server.getId()) != null) {
                server.setName(serverName);
                server.setHost(host);
                server.setPort(port);
                server.setPathPrefix(pathPrefix);
                server.setSsl(ssl);
                server.setSslValidate(sslValidate);
                server.setApiKey(apiKey);
                sm.updateServer(server);
            } else {
                server = new Server(null, serverName, host, port, pathPrefix, ssl, sslValidate, apiKey);
                sm.addServer(server);
            }

            requireActivity().finish();
        }
    }
}
