/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pyload.android.client.components;

import java.util.ArrayList;

import org.pyload.android.client.R;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

/**
 * Demonstrates combining a TabLayout with a ViewPager to implement a tab UI that
 * switches between tabs and also allows the user to perform horizontal flicks
 * to move between the tabs.
 */
public class FragmentTabsPager extends AppCompatActivity {
    protected TabLayout mTabLayout;
    protected ViewPager mViewPager;
    protected TabsAdapter mTabsAdapter;

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

        setContentView(R.layout.fragment_tabs_pager);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTabLayout = findViewById(R.id.tabs);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, mTabLayout, mViewPager);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        if (savedInstanceState != null) {
            TabLayout.Tab tab = mTabLayout.getTabAt(savedInstanceState.getInt("tab", 0));
            if (tab != null) {
                tab.select();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", mTabLayout.getSelectedTabPosition());
    }

    public int getCurrentTab() {
        return mTabLayout.getSelectedTabPosition();
    }

    public Fragment getCurrentFragment() {
        return mTabsAdapter.getFragment(getCurrentTab());
    }


    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabLayout.
     */
    public static class TabsAdapter extends FragmentPagerAdapter implements
            TabLayout.OnTabSelectedListener, ViewPager.OnPageChangeListener {
        private final FragmentActivity mContext;
        private final TabLayout mTabLayout;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
        private int selected = 0;

        static final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(Class<?> _class, Bundle _args) {
                clss = _class;
                args = _args;
            }
        }

        public TabsAdapter(FragmentActivity activity, TabLayout tabLayout,
                           ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mTabLayout = tabLayout;
            mViewPager = pager;
            this.container = pager.getId();
            mTabLayout.addOnTabSelectedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.addOnPageChangeListener(this);
        }

        public void addTab(String title, Drawable icon, Class<?> clss, Bundle args) {
            TabInfo info = new TabInfo(clss, args);
            mTabs.add(info);
            mTabLayout.addTab(mTabLayout.newTab().setText(title).setIcon(icon));
            notifyDataSetChanged();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object obj = super.instantiateItem(container, position);
            if (obj instanceof TabHandler) {
                ((TabHandler) obj).setPosition(position);
            }
            return obj;
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);

            Fragment frag = mContext.getSupportFragmentManager().getFragmentFactory().instantiate(mContext.getClassLoader(), info.clss.getName());
            if (info.args != null) {
                frag.setArguments(info.args);
            }

            ((TabHandler) frag).setPosition(position);

            return frag;
        }

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            mViewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                                   int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {

            Fragment pos = getFragment(position);
            if (pos != null)
                ((TabHandler) pos).onSelected();

            Fragment old = getFragment(selected);
            if (old != null && selected != position)
                ((TabHandler) old).onDeselected();

            TabLayout.Tab tab = mTabLayout.getTabAt(position);
            if (tab != null) {
                tab.select();
            }
            selected = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }
}
