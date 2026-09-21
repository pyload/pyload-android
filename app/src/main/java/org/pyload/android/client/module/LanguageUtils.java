package org.pyload.android.client.module;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

public class LanguageUtils {

    public static void applyLanguage(Context context, String language) {
        if (language == null) language = "";
        LocaleListCompat localeList = language.isEmpty()
                ? LocaleListCompat.getEmptyLocaleList()
                : LocaleListCompat.forLanguageTags(language);

        AppCompatDelegate.setApplicationLocales(localeList);

        if (!language.isEmpty()) {
            Locale locale = Locale.forLanguageTag(language);
            Locale.setDefault(locale);
        }

        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            updateResourcesLocale(appContext, language);
        }
    }

    public static void updateResourcesLocale(Context context, String language) {
        Locale locale = language.isEmpty() ? Locale.getDefault() : Locale.forLanguageTag(language);
        Configuration config = context.getResources().getConfiguration();
        config.setLocale(locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locale));
        }
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    public static Context attachBaseContext(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        String language = prefs.getString("language", "");
        if (language.isEmpty()) {
            return context;
        }

        Locale locale = Locale.forLanguageTag(language);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locale));
        } else {
            config.setLocale(locale);
        }

        return context.createConfigurationContext(config);
    }

    public static String getLocalizedString(Context context, int resId) {
        if (context == null) return "";
        Context localizedContext = attachBaseContext(context);
        return localizedContext.getString(resId);
    }

    public static String getLocalizedString(Context context, int resId, Object... formatArgs) {
        if (context == null) return "";
        Context localizedContext = attachBaseContext(context);
        return localizedContext.getString(resId, formatArgs);
    }
}
