package com.example.group316weatherappproject;

import android.util.Log;
import com.google.gson.JsonSyntaxException;
import com.example.group316weatherappproject.Theme;
import java.io.IOException;

/**
 * Enhanced error handling and logging for theme generation
 * Use this to provide better user feedback when theme generation fails
 */
public class ThemeErrorHandler {
    private static final String TAG = "ThemeErrorHandler";

    // Enum representing different types of theme generation errors
    public enum ThemeError {
        NETWORK_ERROR("Network connection failed"),
        API_KEY_MISSING("Gemini API key not configured"),
        INVALID_JSON("LLM returned invalid JSON"),
        PARSING_ERROR("Failed to parse theme colors"),
        TIMEOUT_ERROR("Request timed out"),
        RATE_LIMIT("API rate limit exceeded"),
        UNKNOWN_ERROR("Unknown error occurred");

        private final String message;

        ThemeError(String message) {
            this.message = message;
        }

        // Returns a readable description for this error type
        public String getMessage() {
            return message;
        }
    }

    /**
     * Classify an exception into a specific theme error type
     */
    public static ThemeError classifyError(Exception e) {
        if (e instanceof IOException) {
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                return ThemeError.TIMEOUT_ERROR;
            }
            return ThemeError.NETWORK_ERROR;
        } else if (e instanceof JsonSyntaxException) {
            return ThemeError.INVALID_JSON;
        } else if (e instanceof IllegalArgumentException &&
                e.getMessage() != null && e.getMessage().contains("color")) {
            return ThemeError.PARSING_ERROR;
        }
        return ThemeError.UNKNOWN_ERROR;
    }

    /**
     * Log when theme generation starts
     */
    public static void logThemeGenerationAttempt(String description) {
        Log.i(TAG, "Attempting theme generation for: '" + description + "'");
    }

    /**
     * Log successful theme generation
     */
    public static void logThemeGenerationSuccess(String description, Theme theme) {
        Log.i(TAG, "Theme generation successful for: '" + description + "'" +
                " -> bg:" + theme.background + " text:" + theme.text + " accent:" + theme.accent);
    }

    /**
     * Log failed theme generation
     */
    public static void logThemeGenerationFailure(String description, ThemeError error, Exception e) {
        Log.w(TAG, "Theme generation failed for: '" + description + "'" +
                " Error: " + error.getMessage(), e);
    }

    /**
     * Get user-friendly error message to show in Toast
     */
    public static String getUserFriendlyMessage(ThemeError error) {
        switch (error) {
            case NETWORK_ERROR:
                return "Unable to connect to theme service. Using default theme.";
            case API_KEY_MISSING:
                return "Theme service not configured. Using default theme.";
            case INVALID_JSON:
            case PARSING_ERROR:
                return "Theme generation incomplete. Using default theme.";
            case TIMEOUT_ERROR:
                return "Theme generation taking too long. Using default theme.";
            case RATE_LIMIT:
                return "Too many theme requests. Please try again later.";
            default:
                return "Theme generation unavailable. Using default theme.";
        }
    }
}
