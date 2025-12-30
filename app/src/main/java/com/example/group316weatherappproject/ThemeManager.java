package com.example.group316weatherappproject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

/**
 * This class manages saving, loading, and recursively applying color themes
 */
public class ThemeManager {

    private static final String TAG = "ThemeManager";
    private static final String prefs = "my_llm_theme_prefs";
    private static final String bg_key = "my_theme_background";
    private static final String text_key = "my_theme_text";
    private static final String accent_key = "my_theme_accent";
    /**
     * Saves the given theme to SharedPreferences.
     */
    public static void saveTheme(Context ctx, Theme spec) {
        Log.d(TAG, "saveTheme -> bg=" + spec.background + " text=" + spec.text + " accent=" + spec.accent);
        SharedPreferences sp = getPrefs(ctx);
        writeThemeColors(sp, spec);
    }
    /**
     * Gets the SharedPreferences object for theme storage.
     */
    private static SharedPreferences getPrefs(Context ctx) {
        return ctx.getSharedPreferences(prefs, Context.MODE_PRIVATE);
    }
    /**
     * Writes the theme colors to SharedPreferences.
     */
    private static void writeThemeColors(SharedPreferences sp, Theme spec) {
        sp.edit()
                .putString(bg_key, spec.background)
                .putString(text_key, spec.text)
                .putString(accent_key, spec.accent)
                .apply();
    }
    /**
     * Loads the theme from SharedPreferences, or returns default if missing.
     */
    public static Theme loadTheme(Context ctx) {
        SharedPreferences sp = getPrefs(ctx);
        String bg = sp.getString(bg_key, null);
        String txt = sp.getString(text_key, null);
        String acc = sp.getString(accent_key, null);
        Theme t = buildTheme(bg, txt, acc);
        Log.d(TAG, "loadTheme -> bg=" + t.background + " text=" + t.text + " accent=" + t.accent);
        return t;
    }
    /**
     * Builds a Theme object from color strings, using defaults if not provided.
     */
    private static Theme buildTheme(String bg, String txt, String acc) {
        if (bg == null || txt == null) {
            return Theme.defaultTheme();
        }
        if (acc == null) acc = Theme.defaultTheme().accent;
        return new Theme(bg, txt, acc);
    }
    /**
     * Applies the given theme to the root view and its children.
     */
    public static void applyTheme(Activity activity, int rootViewId, Theme spec) {
        if (spec == null) {
            Log.w(TAG, "applyTheme called with null spec");
            return;
        }
        Log.d(TAG, "applyTheme -> applying bg=" + spec.background + " text=" + spec.text + " accent=" + spec.accent + " to rootId=" + rootViewId);
        View root = activity.findViewById(rootViewId);
        if (root == null) {
            Log.w(TAG, "applyTheme: root view not found for id " + rootViewId);
            return;
        }

        int[] colors = parseColors(spec);
        root.setBackgroundColor(colors[0]);
        applyRecursively(root, colors[1], colors[2]);
    }
    /**
     * Converts theme color strings to integer color values.
     */
    private static int[] parseColors(Theme spec) {
        int bg = toColor(spec.background, Color.WHITE);
        int txt = toColor(spec.text, Color.BLACK);
        int acc = toColor(spec.accent, 0xFF1976D2);
        return new int[]{bg, txt, acc};
    }
    /**
     * Recursively applies text and accent colors to views
     */
    private static void applyRecursively(View v, int txtColor, int accColor) {
        applyTextColor(v, txtColor);
        applyButtonStyle(v, accColor);
        applyAccentBackgroundIfTagged(v, accColor);
        processChildren(v, txtColor, accColor);
    }

    /**
     * Sets the text color for TextView elements.
     * Respects tag "fixedText" or "fixed-text" to keep layout-defined color.
     */
    private static void applyTextColor(View v, int c) {
        if (v instanceof TextView) {
            Object tag = v.getTag();
            if (tag != null) {
                String t = String.valueOf(tag);
                if ("fixedText".equals(t) || "fixed-text".equals(t)) {
                    return; // keep existing color
                }
            }
            ((TextView) v).setTextColor(c);
        }
    }
    /**
     * Sets the accent color for Button elements.
     */
    private static void applyButtonStyle(View v, int acc) {
        if (v instanceof Button) {
            Button btn = (Button) v;
            // btn.setTextColor(Color.WHITE);
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(acc));
        }
    }
    /**
     * If a view is tagged to use the accent as background, apply it.
     * Supported tags: "accentBg", "accent-bg"
     */
    private static void applyAccentBackgroundIfTagged(View v, int accColor) {
        Object tag = v.getTag();
        if (tag == null) return;
        String t = String.valueOf(tag);
        if ("accentBg".equals(t) || "accent-bg".equals(t)) {
            v.setBackgroundColor(accColor);
        }
    }
    /**
     * Processes all child views in a ViewGroup recursively.
     */
    private static void processChildren(View v, int txt, int acc) {
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            int count = vg.getChildCount();
            for (int i = 0; i < count; i++) {
                applyRecursively(vg.getChildAt(i), txt, acc);
            }
        }
    }
    /**
     * Converts a hex color string to an int or returns default if invalid.
     */
    private static int toColor(String hex, int def) {
        try {
            return Color.parseColor(hex);
        } catch (Exception e) {
            Log.w(TAG, "toColor: failed to parse '" + hex + "', using default", e);
            return def;
        }
    }

    /**
     * load the saved theme and apply it to the given root view id.
     */
    public static void applySavedTheme(Activity activity, int rootViewId) {
        Theme saved = loadTheme(activity);
        applyTheme(activity, rootViewId, saved);
    }
}
