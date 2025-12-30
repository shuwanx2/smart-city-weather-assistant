package com.example.group316weatherappproject;

import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// Settings activity that allows users to log out, return home, and customize app theme
public class Settings extends AppCompatActivity {

    private static final String TAG = "Settings";
    private SessionManager sessionManager;
    private Button logoutButton, backToHomeButton, applyThemeButton;
    private EditText themeDescriptionEdit;
    private TextView themePreviewText;
    private DatabaseHelper databaseHelper;

    // LLM helpers
    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();
    private static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=";
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    // onCreate method to initialize the activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.settings);

        sessionManager = new SessionManager(this);
        databaseHelper = new DatabaseHelper(this);

        // Initialize UI elements
        initializeViews();

        // Set up window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Apply saved theme to this screen
        ThemeManager.applySavedTheme(this, R.id.settings);

        // Force update background color to ensure it shows correctly
        Theme currentTheme = ThemeManager.loadTheme(this);
        if (currentTheme != null) {
            try {
                // Force set the main background color
                int bgColor = android.graphics.Color.parseColor(currentTheme.background);
                findViewById(R.id.settings).setBackgroundColor(bgColor);
                Log.d(TAG, "Forced background color to: " + currentTheme.background);

                // Apply accent color to top panel
                if (currentTheme.accent != null) {
                    int accentColor = android.graphics.Color.parseColor(currentTheme.accent);
                    findViewById(R.id.topPanelGraphic).setBackgroundColor(accentColor);
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to parse theme colors", e);
            }
        }

        // Set up button listeners
        setupButtonListeners();

        // Set up theme description text watcher for real-time preview
        setupThemePreview();
    }

    // onResume method to refresh the background
    @Override
    protected void onResume() {
        super.onResume();
        forceUpdateBackground();
    }

    // Force update background color based on current theme
    private void forceUpdateBackground() {
        Theme currentTheme = ThemeManager.loadTheme(this);
        if (currentTheme != null) {
            try {
                int bgColor = android.graphics.Color.parseColor(currentTheme.background);
                findViewById(R.id.settings).setBackgroundColor(bgColor);
                Log.d(TAG, "Background updated in onResume: " + currentTheme.background);
            } catch (Exception e) {
                Log.w(TAG, "Failed to update background in onResume", e);
            }
        }
    }

    // Initialize UI elements
    private void initializeViews() {
        logoutButton = findViewById(R.id.logoutButton);
        backToHomeButton = findViewById(R.id.backToHomeButton);

        // Add these UI elements to your settings.xml layout if they don't exist
        themeDescriptionEdit = findViewById(R.id.themeDescriptionEdit);
        applyThemeButton = findViewById(R.id.applyThemeButton);
        themePreviewText = findViewById(R.id.themePreviewText);

        // If the above elements don't exist in your layout, you'll need to add them
        // For now, let's handle the case where they might be null
        if (themeDescriptionEdit == null) {
            Log.w(TAG, "themeDescriptionEdit not found in layout");
        }
        if (applyThemeButton == null) {
            Log.w(TAG, "applyThemeButton not found in layout");
        }
        if (themePreviewText == null) {
            Log.w(TAG, "themePreviewText not found in layout");
        }
    }

    // Set up button listeners
    private void setupButtonListeners() {
        logoutButton.setOnClickListener(v -> logout());

        if (backToHomeButton != null) {
            backToHomeButton.setOnClickListener(v -> navigateToHome());
        }

        if (applyThemeButton != null) {
            applyThemeButton.setOnClickListener(v -> generateAndApplyTheme());
        }
    }

    // Set up theme description text watcher for real-time preview
    private void setupThemePreview() {
        if (themeDescriptionEdit != null && themePreviewText != null) {
            themeDescriptionEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateThemePreview(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            themeDescriptionEdit.setHint("Describe your theme: 'ocean sunset', 'cyberpunk night', 'forest morning'");
        }
    }

    // Update theme preview based on description
    private void updateThemePreview(String description) {
        if (themePreviewText == null) return;

        if (description.isEmpty()) {
            themePreviewText.setText("Theme preview will appear here");
            return;
        }

        // Simple preview based on keywords
        String preview = generateSimplePreview(description.toLowerCase());
        themePreviewText.setText("Preview: " + preview);
    }

    // Generate a simple preview string based on keywords
    private String generateSimplePreview(String description) {
        if (description.contains("ocean") || description.contains("blue") || description.contains("water")) {
            return "🌊 Blue oceanic theme with cool tones";
        } else if (description.contains("sunset") || description.contains("orange") || description.contains("warm")) {
            return "🌅 Warm sunset theme with orange/pink tones";
        } else if (description.contains("forest") || description.contains("green") || description.contains("nature")) {
            return "🌲 Natural forest theme with green tones";
        } else if (description.contains("night") || description.contains("dark") || description.contains("black")) {
            return "🌙 Dark theme with deep colors";
        } else if (description.contains("cyberpunk") || description.contains("neon") || description.contains("purple")) {
            return "🤖 Cyberpunk theme with neon colors";
        } else if (description.contains("light") || description.contains("bright") || description.contains("white")) {
            return "☀️ Light theme with bright colors";
        } else {
            return "🎨 Custom theme based on '" + description + "'";
        }
    }

    // Generate and apply theme based on user description
    private void generateAndApplyTheme() {
        if (themeDescriptionEdit == null) {
            Toast.makeText(this, "Theme customization not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = themeDescriptionEdit.getText().toString().trim();

        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter a theme description", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button and show progress
        if (applyThemeButton != null) {
            applyThemeButton.setEnabled(false);
            applyThemeButton.setText("Generating...");
        }

        Toast.makeText(this, "Generating theme...", Toast.LENGTH_SHORT).show();

        // Generate theme on background thread
        new Thread(() -> {
            Theme newTheme = null;

            try {
                // Check if it's a hex color
                if (description.startsWith("#") && (description.length() == 7 || description.length() == 9)) {
                    newTheme = createThemeFromHexColor(description);
                    Log.d(TAG, "Created theme from hex color: " + description);
                } else {
                    // Try to generate from LLM
                    ThemeErrorHandler.logThemeGenerationAttempt(description);
                    newTheme = requestThemeFromGemini(description);

                    if (newTheme != null) {
                        ThemeErrorHandler.logThemeGenerationSuccess(description, newTheme);
                    }
                }
            } catch (Exception e) {
                ThemeErrorHandler.ThemeError error = ThemeErrorHandler.classifyError(e);
                ThemeErrorHandler.logThemeGenerationFailure(description, error, e);

                runOnUiThread(() -> {
                    Toast.makeText(this, ThemeErrorHandler.getUserFriendlyMessage(error),
                            Toast.LENGTH_LONG).show();
                });
            }

            // If generation failed, use default
            if (newTheme == null) {
                newTheme = Theme.defaultTheme();
                Log.d(TAG, "Using default theme as fallback");
            }

            // Apply theme on UI thread
            Theme finalTheme = newTheme;
            runOnUiThread(() -> {
                applyGeneratedTheme(finalTheme);

                // Re-enable button
                if (applyThemeButton != null) {
                    applyThemeButton.setEnabled(true);
                    applyThemeButton.setText("Apply Theme");
                }
            });
        }).start();
    }

    // Create a theme from a single hex color by deriving text and accent colors
    private Theme createThemeFromHexColor(String hexColor) {
        String background = hexColor;
        String text = pickReadableTextColor(background);
        String accent = Theme.defaultTheme().accent;
        return new Theme(background, text, accent);
    }

    // Pick a readable text color (black or white) based on background color luminance
    private String pickReadableTextColor(String bgHex) {
        try {
            int color = Color.parseColor(bgHex);
            double r = Color.red(color) / 255.0;
            double g = Color.green(color) / 255.0;
            double b = Color.blue(color) / 255.0;
            double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
            return luminance < 0.5 ? "#FFFFFF" : "#000000";
        } catch (Exception e) {
            Log.w(TAG, "pickReadableTextColor failed for " + bgHex + ", defaulting to #000000", e);
            return "#000000";
        }
    }

    // Apply the generated theme to the app and save it
    private void applyGeneratedTheme(Theme theme) {
        try {
            Log.d(TAG, "Applying theme: bg=" + theme.background + " text=" + theme.text + " accent=" + theme.accent);

            // Save theme
            ThemeManager.saveTheme(this, theme);

            // Apply to current activity
            ThemeManager.applyTheme(this, R.id.settings, theme);

            try {
                int bgColor = android.graphics.Color.parseColor(theme.background);
                findViewById(R.id.settings).setBackgroundColor(bgColor);
                Log.d(TAG, "Forced new background color: " + theme.background);

                int accentColor = android.graphics.Color.parseColor(theme.accent);
                findViewById(R.id.topPanelGraphic).setBackgroundColor(accentColor);
                Log.d(TAG, "Updated top panel color: " + theme.accent);
            } catch (Exception e) {
                Log.w(TAG, "Failed to force update colors", e);
            }

            // Update session and database
            String username = sessionManager.getUsername();
            if (username != null) {
                sessionManager.updateThemeColor(theme.background);
                sessionManager.updateTextColor(theme.text);

                databaseHelper.updateThemeColor(username, theme.background);
                databaseHelper.updateTextColor(username, theme.text);
            }

            Toast.makeText(this, "Theme applied successfully!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error applying theme", e);
            Toast.makeText(this, "Error applying theme", Toast.LENGTH_SHORT).show();
        }
    }

    // Logs out the current user and navigates to the login screen
    private void logout() {
        sessionManager.logoutUser();
        navigateToLogin();
    }

    // Navigates to the login screen
    private void navigateToLogin() {
        Intent intent = new Intent(Settings.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // Navigates to the home screen
    private void navigateToHome() {
        Intent intent = new Intent(Settings.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    // Request theme from Gemini LLM based on user description
    private Theme requestThemeFromGemini(String description) throws IOException {
        // First, check for API key
        String apiKey = getGeminiApiKey();
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_GEMINI_API_KEY")) {
            Log.w(TAG, "Missing GEMINI_API_KEY, skipping Gemini call");
            return null;
        }

        String prompt = buildJsonPrompt(description);
        String bodyJson = "{\"contents\":[{\"parts\":[{\"text\":" + gson.toJson(prompt) + "}]}]}";

        Log.d(TAG, "Gemini request body (truncated): " +
                (bodyJson.length() > 300 ? bodyJson.substring(0, 300) + "..." : bodyJson));

        Request req = new Request.Builder()
                .url(GEMINI_ENDPOINT + apiKey)
                .post(RequestBody.create(bodyJson, MEDIA_TYPE_JSON))
                .build();

        try (Response resp = http.newCall(req).execute()) {
            String respText = resp.body() != null ? resp.body().string() : "";
            Log.d(TAG, "Gemini HTTP status: " + resp.code() +
                    " response length: " + (respText == null ? 0 : respText.length()));

            if (!resp.isSuccessful() || respText.isEmpty()) {
                Log.w(TAG, "Gemini response unsuccessful or empty");
                return null;
            }

            try {
                JsonObject root = JsonParser.parseString(respText).getAsJsonObject();
                String text = root.getAsJsonArray("candidates").get(0).getAsJsonObject()
                        .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                        .get("text").getAsString();

                Log.d(TAG, "Gemini candidate text (truncated): " +
                        (text.length() > 300 ? text.substring(0, 300) + "..." : text));

                String jsonOnly = extractJson(text);
                if (jsonOnly == null) {
                    Log.w(TAG, "Failed to extract JSON from Gemini text");
                    return null;
                }

                Log.d(TAG, "Extracted JSON: " + jsonOnly);
                Theme spec = gson.fromJson(jsonOnly, Theme.class);

                if (spec == null || spec.background == null || spec.text == null) {
                    Log.w(TAG, "Parsed theme missing required fields");
                    return null;
                }

                if (spec.accent == null) {
                    spec.accent = Theme.defaultTheme().accent;
                }

                return spec;

            } catch (Exception e) {
                Log.e(TAG, "Failed to parse Gemini response", e);
                return null;
            }
        }
    }

    // Get the Gemini API key
    private String getGeminiApiKey() {
        // Try to get from BuildConfig first
        try {
            return BuildConfig.GEMINI_API_KEY;
        } catch (Exception e) {
            Log.w(TAG, "BuildConfig.GEMINI_API_KEY not available");
        }

        return null;
    }

    // Build the JSON prompt for the Gemini API
    private String buildJsonPrompt(String desc) {
        return "as a theme generator: you will be given a user description, return valid JSON with these keys: " +
                "background (hex), text (hex), accent (hex). Do not include any backticks or explanations. Example: {\n" +
                "  \"background\": \"#101820\",\n" +
                "  \"text\": \"#F2AA4C\",\n" +
                "  \"accent\": \"#5B8BF7\"\n}" +
                "\nUser description: " + desc + "\nReturn JSON now.";
    }

    // Extract JSON substring from text
    private String extractJson(String text) {
        if (text == null) return null;
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) return null;
        return text.substring(start, end + 1);
    }
}