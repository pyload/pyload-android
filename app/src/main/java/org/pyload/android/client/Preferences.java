package org.pyload.android.client;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.pyload.android.client.components.ClickNLoadPreferenceScreen;
import org.pyload.android.client.models.Server;
import org.pyload.android.client.module.LanguageUtils;
import org.pyload.android.client.module.ServerManager;
import org.pyload.android.client.services.ClickNLoadService;

import java.util.List;

public class Preferences extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageUtils.attachBaseContext(newBase));
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.preferences_container), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
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

        var ft = getSupportFragmentManager().beginTransaction();
        if (!"about_screen".equals(pref.getKey())) {
            ft.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
            );
        }
        ft.replace(R.id.preferences_container, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            if ("clicknload".equals(rootKey) || "clicknload_screen".equals(rootKey)) {
                setPreferencesFromResource(R.xml.clicknload_preferences, null);
            } else {
                setPreferencesFromResource(R.xml.preferences, rootKey);
            }

            Preference languagePreference = findPreference("language");
            if (languagePreference != null) {
                languagePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    String newLang = (String) newValue;
                    LanguageUtils.applyLanguage(requireContext(), newLang);
                    requireActivity().recreate();
                    return true;
                });
            }

            Preference themePreference = findPreference("theme");
            if (themePreference != null) {
                themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    pyLoadApp.applyTheme((String) newValue);
                    return true;
                });
            }

            Preference manageServersPref = findPreference("manage_servers_pref");
            if (manageServersPref != null) {
                manageServersPref.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(getContext(), ServerListActivity.class);
                    startActivity(intent);
                    return true;
                });
            }

            Preference clicknloadServerPref = findPreference("clicknload_server_option");
            if (clicknloadServerPref != null) {
                clicknloadServerPref.setOnPreferenceClickListener(preference -> {
                    List<Server> servers = ServerManager.getInstance(requireContext()).getServers();
                    SharedPreferences prefs = requireContext().getSharedPreferences(requireContext().getPackageName() + "_preferences", Context.MODE_PRIVATE);
                    String serverOption = prefs.getString("clicknload_server_option", "ask");
                    String currentTargetId = prefs.getString("clicknload_target_server_id", null);

                    String[] items = new String[servers.size() + 1];
                    items[0] = getString(R.string.clicknload_server_ask);

                    int selectedIndex = 0;
                    if (servers.size() == 1) {
                        selectedIndex = 1;
                    } else if ("quick_select".equals(serverOption) && currentTargetId != null) {
                        for (int i = 0; i < servers.size(); i++) {
                            Server s = servers.get(i);
                            if (s.getId().equals(currentTargetId)) {
                                selectedIndex = i + 1;
                                break;
                            }
                        }
                    }

                    for (int i = 0; i < servers.size(); i++) {
                        Server s = servers.get(i);
                        items[i + 1] = s.getName() + " (" + s.getFormattedUrl() + ")";
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(),
                            android.R.layout.select_dialog_singlechoice, items) {
                        @Override
                        public boolean isEnabled(int position) {
                            if (position == 0 && servers.size() == 1) {
                                return false;
                            }
                            return super.isEnabled(position);
                        }

                        @NonNull
                        @Override
                        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                            View view = super.getView(position, convertView, parent);
                            if (position == 0 && servers.size() == 1) {
                                view.setAlpha(0.4f);
                            } else {
                                view.setAlpha(1.0f);
                            }
                            return view;
                        }
                    };

                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.clicknload_destination_server)
                            .setSingleChoiceItems(adapter, selectedIndex, (dialog, which) -> {
                                dialog.dismiss();
                                if (which == 0) {
                                    prefs.edit()
                                            .putString("clicknload_server_option", "ask")
                                            .remove("clicknload_target_server_id")
                                            .remove("clicknload_target_server_name")
                                            .apply();
                                } else {
                                    Server selected = servers.get(which - 1);
                                    prefs.edit()
                                            .putString("clicknload_server_option", "quick_select")
                                            .putString("clicknload_target_server_id", selected.getId())
                                            .putString("clicknload_target_server_name", selected.getName())
                                            .apply();
                                }
                                updateClickNLoadServerSummary();
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                    return true;
                });
            }

            updateUrlSummary();
            updateClickNLoadServerSummary();
            updateAboutInfo();
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (preference instanceof ClickNLoadPreferenceScreen) {
                Fragment fragment = new SettingsFragment();
                Bundle args = new Bundle();
                args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, "clicknload_screen");
                fragment.setArguments(args);
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(
                                R.anim.slide_in_right,
                                R.anim.slide_out_left,
                                R.anim.slide_in_left,
                                R.anim.slide_out_right
                        )
                        .replace(R.id.preferences_container, fragment)
                        .addToBackStack(null)
                        .commit();
                return true;
            }
            return super.onPreferenceTreeClick(preference);
        }

        private void updateAboutInfo() {
            Preference aboutScreen = findPreference("about_screen");
            if (aboutScreen != null) {
                aboutScreen.setSummary("pyLoad " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
            }

            Preference versionPref = findPreference("version");
            if (versionPref != null) {
                versionPref.setSummary("pyLoad " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
            }

            Preference gitPref = findPreference("git_commit");
            if (gitPref != null) {
                gitPref.setSummary(BuildConfig.GIT_COMMIT);
            }

            Preference buildDatePref = findPreference("build_date");
            if (buildDatePref != null) {
                buildDatePref.setSummary(BuildConfig.BUILD_DATE);
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            PreferenceScreen screen = getPreferenceScreen();
            if (screen != null && "about_screen".equals(screen.getKey())) {
                requireActivity().setTitle(R.string.about);
            } else if (screen != null && ("clicknload".equals(screen.getKey()) || "clicknload_screen".equals(screen.getKey()))) {
                requireActivity().setTitle(R.string.clicknload);
            } else if (screen != null && screen.getTitle() != null) {
                requireActivity().setTitle(screen.getTitle());
            } else {
                requireActivity().setTitle(R.string.app_settings);
            }

            if (getPreferenceManager().getSharedPreferences() != null) {
                getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            }
            updateUrlSummary();
            updateClickNLoadServerSummary();
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
            if ("clicknload".equals(key)) {
                boolean enabled = sharedPreferences.getBoolean(key, false);
                Intent intent = new Intent(getContext(), ClickNLoadService.class);
                if (enabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        getContext().startForegroundService(intent);
                    } else {
                        getContext().startService(intent);
                    }
                } else {
                    getContext().stopService(intent);
                }
            }
        }

        private void updateUrlSummary() {
            if (getContext() == null) return;
            Server active = ServerManager.getInstance(getContext()).getActiveServer();

            Preference manageServersPref = findPreference("manage_servers_pref");
            if (manageServersPref != null && active != null) {
                manageServersPref.setSummary(active.getName() + " (" + active.getFormattedUrl() + ")");
            }
        }

        private void updateClickNLoadServerSummary() {
            if (getContext() == null) return;
            Preference clicknloadServerPref = findPreference("clicknload_server_option");
            if (clicknloadServerPref != null) {
                List<Server> servers = ServerManager.getInstance(getContext()).getServers();
                if (servers.size() == 1) {
                    Server singleServer = servers.get(0);
                    clicknloadServerPref.setSummary(singleServer.getName() + " (" + singleServer.getFormattedUrl() + ")");
                    return;
                }
                SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
                String val = prefs != null ? prefs.getString("clicknload_server_option", "ask") : "ask";
                if ("quick_select".equals(val)) {
                    String targetId = prefs != null ? prefs.getString("clicknload_target_server_id", null) : null;
                    Server targetServer = ServerManager.getInstance(getContext()).getServer(targetId);
                    if (targetServer != null) {
                        clicknloadServerPref.setSummary(targetServer.getName() + " (" + targetServer.getFormattedUrl() + ")");
                    } else {
                        clicknloadServerPref.setSummary(R.string.clicknload_server_quick_select);
                    }
                } else {
                    clicknloadServerPref.setSummary(R.string.clicknload_server_ask);
                }
            }
        }
    }
}
