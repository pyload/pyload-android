package org.pyload.android.client;

import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.MenuItem;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.KeyEvent;
import androidx.appcompat.widget.SearchView;
import android.widget.ImageView;
import androidx.fragment.app.Fragment;
import org.pyload.android.client.components.TabHandler;
import android.content.Context;
import androidx.fragment.app.FragmentManager;

public class RemoteSettings extends AppCompatActivity {

    private MenuItem searchItem;
    private OnBackPressedCallback onBackPressedCallback;

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
        setContentView(R.layout.remote_settings);

        onBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (searchItem != null && searchItem.isActionViewExpanded()) {
                    searchItem.collapseActionView();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.serverSettings, org.pyload.android.client.fragments.SettingsFragment.class, null)
                    .commit();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.remote_settings_container), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean isMainPage = getSupportFragmentManager().getBackStackEntryCount() == 0;
            if (isMainPage && searchItem != null) {
                SearchView searchView = (SearchView) searchItem.getActionView();
                if (searchView != null && !searchView.isIconified()) {
                    searchView.setQuery("", false);
                    searchView.setIconified(true);
                }
                
                Fragment frag = getSupportFragmentManager().findFragmentById(R.id.serverSettings);
                if (frag instanceof org.pyload.android.client.fragments.SettingsFragment) {
                    ((org.pyload.android.client.fragments.SettingsFragment) frag).resetSearch();
                }
            }
            invalidateOptionsMenu();
        });
    }

    @Override
    protected void onDestroy() {
        android.util.Log.d("pyLoad", "RemoteSettings.onDestroy()");
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        searchItem = menu.findItem(R.id.search);

        // Hide all items except search
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() != R.id.search) {
                item.setVisible(false);
            }
        }

        if (searchItem != null) {
            // Only show search on the main settings page (backstack is empty)
            boolean isMainPage = getSupportFragmentManager().getBackStackEntryCount() == 0;
            searchItem.setVisible(isMainPage);

            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    onBackPressedCallback.setEnabled(true);
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    onBackPressedCallback.setEnabled(false);
                    return true;
                }
            });

            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint(getString(R.string.filter_hint));
                searchView.setIconifiedByDefault(true);

                // Remove the underline from the search box
                int searchPlateId = androidx.appcompat.R.id.search_plate;
                View searchPlate = searchView.findViewById(searchPlateId);
                if (searchPlate != null) {
                    searchPlate.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                }

                searchView.setSubmitButtonEnabled(false);

                // Access the inner EditText to handle focus and clicks
                View searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
                if (searchAutoComplete != null) {
                    searchAutoComplete.setOnTouchListener((v, event) -> {
                        if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                            // Force focus reset to trigger keyboard if it was dismissed
                            if (v.isFocused()) {
                                v.clearFocus();
                                v.requestFocus();
                            }
                            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.showSoftInput(v, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                            }
                        }
                        return false;
                    });

                    searchAutoComplete.setOnKeyListener((v, keyCode, event) -> {
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            if (event.getAction() == KeyEvent.ACTION_UP) {
                                if (searchItem != null && searchItem.isActionViewExpanded()) {
                                    searchItem.collapseActionView();
                                }
                            }
                            // Always consume back key when search is expanded to prevent default SearchView behavior
                            return searchItem != null && searchItem.isActionViewExpanded();
                        }
                        return false;
                    });
                }

                // Immediate focus and open keyboard
                searchView.setOnSearchClickListener(v -> {
                    searchView.requestFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(searchView.findFocus(), android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                    }
                });

                // Force the 'X' button to close the search view entirely
                int closeBtnId = androidx.appcompat.R.id.search_close_btn;
                View closeBtn = searchView.findViewById(closeBtnId);
                if (closeBtn != null) {
                    closeBtn.setAlpha(1.0f);
                    closeBtn.setEnabled(true);

                    if (closeBtn instanceof ImageView) {
                        android.util.TypedValue typedValue = new android.util.TypedValue();
                        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
                        int color = typedValue.data;
                        ((ImageView) closeBtn).setColorFilter(color);
                    }

                    closeBtn.setOnClickListener(v -> {
                        searchItem.collapseActionView();
                    });
                }

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        if (query != null && !query.trim().isEmpty()) {
                            searchView.clearFocus();
                        }
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        Fragment frag = getSupportFragmentManager().findFragmentById(R.id.serverSettings);
                        if (frag instanceof TabHandler) {
                            ((TabHandler) frag).onSearch(newText);
                        }
                        return true;
                    }
                });
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
