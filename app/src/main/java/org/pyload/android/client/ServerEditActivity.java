package org.pyload.android.client;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.snackbar.Snackbar;

import org.pyload.android.client.models.Server;
import org.pyload.android.client.module.LanguageUtils;
import org.pyload.android.client.module.ServerManager;

public class ServerEditActivity extends AppCompatActivity {

    public static final String EXTRA_SERVER_ID = "server_id";
    private boolean isFormValid = false;

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
            if (getIntent().getData() != null) {
                args.putParcelable("deep_link_uri", getIntent().getData());
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

    public void setFormValid(boolean valid) {
        if (isFormValid != valid) {
            isFormValid = valid;
            supportInvalidateOptionsMenu();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.server_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem saveItem = menu.findItem(R.id.action_save);
        if (saveItem != null) {
            saveItem.setEnabled(isFormValid);

            String saveText = getString(R.string.save);
            SpannableString spannable = new SpannableString(saveText);

            int color = ContextCompat.getColor(this, isFormValid ? R.color.textPrimary : R.color.textDisabled);

            spannable.setSpan(new ForegroundColorSpan(color), 0, spannable.length(), 0);
            saveItem.setTitle(spannable);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_save) {
            if (!isFormValid) {
                return true;
            }
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

            if (getArguments() != null && getArguments().containsKey("deep_link_uri")) {
                Uri uri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    uri = getArguments().getParcelable("deep_link_uri", Uri.class);
                } else {
                    uri = getArguments().getParcelable("deep_link_uri");
                }
                if (uri != null) {
                    if (uri.getQueryParameter("host") != null) server.setHost(uri.getQueryParameter("host"));
                    if (uri.getQueryParameter("port") != null) server.setPort(uri.getQueryParameter("port"));
                    if (uri.getQueryParameter("path") != null) server.setPathPrefix(uri.getQueryParameter("path"));
                    if (uri.getQueryParameter("ssl") != null) server.setSsl(Boolean.parseBoolean(uri.getQueryParameter("ssl")));
                    if (uri.getQueryParameter("key") != null) server.setApiKey(uri.getQueryParameter("key"));
                }
            }

            EditTextPreference namePref = findPreference("server_name");
            EditTextPreference hostPref = findPreference("host");
            EditTextPreference portPref = findPreference("port");
            EditTextPreference pathPrefixPref = findPreference("path_prefix");
            CheckBoxPreference sslPref = findPreference("ssl");
            CheckBoxPreference sslValidatePref = findPreference("ssl_validate");
            EditTextPreference apiKeyPref = findPreference("api_key");

            Preference.OnPreferenceChangeListener changeListener = (preference, newValue) -> {
                if (getView() != null) {
                    getView().post(this::validateForm);
                }
                return true;
            };

            if (namePref != null) {
                namePref.setText(server.getName());
                namePref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
                namePref.setOnPreferenceChangeListener(changeListener);
            }
            if (hostPref != null) {
                hostPref.setText(server.getHost());
                hostPref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
                hostPref.setOnPreferenceChangeListener(changeListener);
            }
            if (portPref != null) {
                portPref.setText(server.getPort());
                portPref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
                portPref.setOnPreferenceChangeListener(changeListener);
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
                    if (val.length() <= 8) {
                        return "********";
                    }
                    return val.substring(0, 4) + "********" + val.substring(val.length() - 4);
                });
                apiKeyPref.setOnPreferenceChangeListener(changeListener);
            }

            validateForm();
        }

        private void validateForm() {
            EditTextPreference namePref = findPreference("server_name");
            EditTextPreference hostPref = findPreference("host");
            EditTextPreference portPref = findPreference("port");
            EditTextPreference apiKeyPref = findPreference("api_key");

            String name = namePref != null && namePref.getText() != null ? namePref.getText().trim() : "";
            String host = hostPref != null && hostPref.getText() != null ? hostPref.getText().trim() : "";
            String port = portPref != null && portPref.getText() != null ? portPref.getText().trim() : "";
            String apiKey = apiKeyPref != null && apiKeyPref.getText() != null ? apiKeyPref.getText().trim() : "";

            boolean valid = !name.isEmpty() && !host.isEmpty() && !port.isEmpty() && !apiKey.isEmpty();

            if (getActivity() instanceof ServerEditActivity activity) {
                activity.setFormValid(valid);
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
