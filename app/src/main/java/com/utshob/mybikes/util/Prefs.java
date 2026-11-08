package com.utshob.mybikes.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.utshob.mybikes.AppConfig;

/** All SharedPreferences settings of the app in one place. */
public final class Prefs {
    private static SharedPreferences sp;

    private Prefs() {}

    public static void init(Context c) {
        if (sp == null) sp = c.getApplicationContext().getSharedPreferences("mybikes_prefs", Context.MODE_PRIVATE);
    }

    public static String getString(String key, String def) { return sp.getString(key, def); }
    public static void putString(String key, String v) { sp.edit().putString(key, v).apply(); }

    public static boolean rememberMe() { return sp.getBoolean("remember_me", true); }
    public static void setRememberMe(boolean v) { sp.edit().putBoolean("remember_me", v).apply(); }
    public static String savedEmail() { return sp.getString("saved_email", ""); }
    public static void setSavedEmail(String e) { sp.edit().putString("saved_email", e).apply(); }

    public static String selectedBike() { return sp.getString("selected_bike", null); }
    public static void setSelectedBike(String id) { sp.edit().putString("selected_bike", id).apply(); }

    public static String currency() { return sp.getString("currency", AppConfig.DEFAULT_CURRENCY); }
    public static void setCurrency(String c) { sp.edit().putString("currency", c).apply(); }

    public static int themeMode() { return sp.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); }
    public static void setThemeMode(int m) { sp.edit().putInt("theme_mode", m).apply(); }

    public static boolean remindersEnabled() { return sp.getBoolean("reminders", true); }
    public static void setRemindersEnabled(boolean v) { sp.edit().putBoolean("reminders", v).apply(); }

    // Avoids duplicate notifications: each reminder key is notified only once.
    public static boolean isNotified(String key) { return sp.getBoolean("n_" + key, false); }
    public static void markNotified(String key) { sp.edit().putBoolean("n_" + key, true).apply(); }
}
