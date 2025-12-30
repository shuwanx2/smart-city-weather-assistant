package com.example.group316weatherappproject;

import android.content.Context;
import android.content.SharedPreferences;

// Manages user session data such as login status and preferences
public class SessionManager {
    private static final String PREF_NAME = "WeatherAppSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_THEME_COLOR = "themeColor";
    private static final String KEY_TEXT_COLOR = "textColor";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    // Constructor to initialize SessionManager with application context
    public SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /**
     * Create login session
     */
    public void createLoginSession(String username, String themeColor, String textColor) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_THEME_COLOR, themeColor);
        editor.putString(KEY_TEXT_COLOR, textColor);
        editor.commit();
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Get logged in username
     */
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    /**
     * Get user's theme color
     */
    public String getThemeColor() {
        return prefs.getString(KEY_THEME_COLOR, "#6750a5");
    }

    /**
     * Get user's text color
     */
    public String getTextColor() {
        return prefs.getString(KEY_TEXT_COLOR, "#000000");
    }

    /**
     * Update theme color
     */
    public void updateThemeColor(String themeColor) {
        editor.putString(KEY_THEME_COLOR, themeColor);
        editor.commit();
    }

    /**
     * Update text color
     */
    public void updateTextColor(String textColor) {
        editor.putString(KEY_TEXT_COLOR, textColor);
        editor.commit();
    }

    /**
     * Logout user
     */
    public void logoutUser() {
        editor.clear();
        editor.commit();
    }
}
