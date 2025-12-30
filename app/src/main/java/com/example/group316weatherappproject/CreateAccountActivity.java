package com.example.group316weatherappproject;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.example.group316weatherappproject.ThemeErrorHandler;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// Activity for creating new user accounts with personalized theme generation using Gemini AI
public class CreateAccountActivity extends AppCompatActivity {

    private static final String TAG = "CreateAccount";
    private EditText editUsername, editPassword, editTheme;
    private Button createAccountButton;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    // LLM helpers
    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();
    private static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=";
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    // Initializes the activity, sets up UI components and event listeners
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.create), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize database helper and session manager
        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        // Get UI elements
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        editTheme = findViewById(R.id.theme);
        createAccountButton = findViewById(R.id.CreateAccountButton);

        // Enhanced hint with more examples
        editTheme.setHint("Describe your theme: 'summer beach', 'cyberpunk nightscape', 'forest sunrise', or hex color");

        // Set up create account button click listener
        createAccountButton.setOnClickListener(v -> attemptCreateAccount());

        // Set up click listener for "Already have an account? Login here" text
        TextView loginText = findViewById(R.id.loginText);
        loginText.setOnClickListener(v -> {
            Intent intent = new Intent(CreateAccountActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    // Validates user input, creates account, generates personalized theme, and navigates to home
    private void attemptCreateAccount() {
        String username = editUsername.getText().toString().trim().toLowerCase();
        String password = editPassword.getText().toString().trim();
        String themeInput = editTheme.getText().toString().trim();

        Log.d(TAG, "attemptCreateAccount: username=" + username + " themeInput=\"" + themeInput + "\"");

        // Validate input
        if (username.isEmpty()) {
            editUsername.setError("Username is required");
            editUsername.requestFocus();
            Log.d(TAG, "validation failed: username empty");
            return;
        }

        if (password.isEmpty()) {
            editPassword.setError("Password is required");
            editPassword.requestFocus();
            Log.d(TAG, "validation failed: password empty");
            return;
        }

        if (password.length() < 4) {
            editPassword.setError("Password must be at least 4 characters");
            editPassword.requestFocus();
            Log.d(TAG, "validation failed: password too short");
            return;
        }

        // Register user
        Log.d(TAG, "registering user: " + username);
        boolean success = databaseHelper.registerUser(username, password);
        Log.d(TAG, "registerUser returned: " + success);

        if (!success) {
            Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button while generating/applying theme
        createAccountButton.setEnabled(false);
        Toast.makeText(this, "Generating personalized theme...", Toast.LENGTH_SHORT).show();

        // Do theme generation/update off the UI thread
        new Thread(() -> {
            Theme spec = null;

            // If input looks like a hex color, build a simple theme immediately
            boolean isHex = themeInput.startsWith("#") && (themeInput.length() == 7 || themeInput.length() == 9);
            if (isHex) {
                Log.d(TAG, "themeInput detected as hex color: " + themeInput);
                String background = themeInput;
                String text = pickReadableTextColor(background);
                String accent = generateComplementaryColor(background); // Enhanced accent generation
                spec = new Theme(background, text, accent);
                Log.d(TAG, "Built enhanced theme from hex -> bg=" + background + " text=" + text + " accent=" + accent);
            } else if (!themeInput.isEmpty()) {
                // Try to request a theme from Gemini using the description
                Log.d(TAG, "Sending description to Gemini: \"" + themeInput + "\"");
                ThemeErrorHandler.logThemeGenerationAttempt(themeInput);
                try {
                    spec = requestThemeFromGemini(themeInput);
                    if (spec != null) {
                        ThemeErrorHandler.logThemeGenerationSuccess(themeInput, spec);
                    }
                    Log.d(TAG, "Gemini returned spec: " + (spec == null ? "null" : ("bg=" + spec.background + " text=" + spec.text + " accent=" + spec.accent)));
                } catch (Exception e) {
                    ThemeErrorHandler.ThemeError error = ThemeErrorHandler.classifyError(e);
                    ThemeErrorHandler.logThemeGenerationFailure(themeInput, error, e);

                    runOnUiThread(() -> {
                        Toast.makeText(this, ThemeErrorHandler.getUserFriendlyMessage(error),
                                Toast.LENGTH_LONG).show();
                    });

                    Log.e(TAG, "Exception requesting theme from Gemini", e);
                    spec = null;
                }

                // Enhanced fallback system - create intelligent fallback based on description
                if (spec == null) {
                    spec = createIntelligentFallbackTheme(themeInput);
                    Log.d(TAG, "Using intelligent fallback theme for: " + themeInput);
                }
            } else {
                Log.d(TAG, "No theme input provided, will use default theme");
            }

            if (spec == null) {
                spec = Theme.defaultTheme();
                Log.d(TAG, "Using default theme");
            }

            // Persist theme to SharedPreferences and update DB + session on UI thread
            Theme finalSpec = spec;
            runOnUiThread(() -> {
                try {
                    Log.d(TAG, "Saving theme via ThemeManager");
                    ThemeManager.saveTheme(CreateAccountActivity.this, finalSpec);

                    Log.d(TAG, "Applying theme to CreateAccountActivity root");
                    ThemeManager.applyTheme(CreateAccountActivity.this, R.id.create, finalSpec);

                    // Enhanced theme application with visual effects
                    applyEnhancedThemeEffects(finalSpec);

                    Log.d(TAG, "Updating DB for user " + username);
                    boolean dbBg = databaseHelper.updateThemeColor(username, finalSpec.background);
                    boolean dbText = databaseHelper.updateTextColor(username, finalSpec.text);
                    Log.d(TAG, "DB update themeColor=" + dbBg + " textColor=" + dbText);

                    Log.d(TAG, "Creating session for user " + username);
                    sessionManager.createLoginSession(username, finalSpec.background, finalSpec.text);

                    createAccountButton.setEnabled(true);
                    Toast.makeText(CreateAccountActivity.this, "Account created with personalized theme!", Toast.LENGTH_SHORT).show();

                    // Navigate to Home
                    Intent intent = new Intent(CreateAccountActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error applying/saving theme after signup", e);
                    createAccountButton.setEnabled(true);
                    Toast.makeText(CreateAccountActivity.this, "Account created but theme apply failed", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(CreateAccountActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        }).start();
    }

    // Creates a fallback theme based on keyword matching when Gemini API is unavailable
    private Theme createIntelligentFallbackTheme(String description) {
        String desc = description.toLowerCase();

        if (desc.contains("summer") || desc.contains("beach")) {
            return new Theme("#FFF8E1", "#D84315", "#FF7043"); // Warm beach colors
        } else if (desc.contains("cyberpunk") || desc.contains("neon") || desc.contains("nightscape")) {
            return new Theme("#0A0A0A", "#00FF41", "#FF00FF"); // Dark with neon accents
        } else if (desc.contains("forest") || desc.contains("nature") || desc.contains("green")) {
            return new Theme("#E8F5E8", "#1B5E20", "#4CAF50"); // Natural forest colors
        } else if (desc.contains("ocean") || desc.contains("sea") || desc.contains("blue")) {
            return new Theme("#E3F2FD", "#0D47A1", "#2196F3"); // Ocean blues
        } else if (desc.contains("sunset") || desc.contains("sunrise") || desc.contains("orange")) {
            return new Theme("#FFF3E0", "#E65100", "#FF9800"); // Warm sunset colors
        } else if (desc.contains("fire") || desc.contains("red")) {
            return new Theme("#FFEBEE", "#B71C1C", "#F44336"); // Fire reds
        } else if (desc.contains("space") || desc.contains("galaxy") || desc.contains("star")) {
            return new Theme("#1A1A2E", "#E0E0E0", "#BB86FC"); // Space theme
        } else if (desc.contains("winter") || desc.contains("snow") || desc.contains("ice")) {
            return new Theme("#F3F8FF", "#0D47A1", "#03DAC6"); // Winter blues
        } else {
            return Theme.defaultTheme();
        }
    }

    // Applies enhanced visual effects to UI elements using the generated theme colors
    private void applyEnhancedThemeEffects(Theme theme) {
        try {
            // Force background application
            int bgColor = Color.parseColor(theme.background);
            findViewById(R.id.create).setBackgroundColor(bgColor);

            // Enhanced button styling with accent color
            int accentColor = Color.parseColor(theme.accent);
            createAccountButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(accentColor)
            );

            Log.d(TAG, "Applied enhanced theme effects");
        } catch (Exception e) {
            Log.w(TAG, "Failed to apply enhanced theme effects", e);
        }
    }

    // Generates a complementary accent color by rotating the hue 180 degrees on the color wheel
    private String generateComplementaryColor(String baseColor) {
        try {
            int color = Color.parseColor(baseColor);
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);

            // Create complementary color (opposite on color wheel)
            hsv[0] = (hsv[0] + 180) % 360;
            hsv[1] = Math.min(1.0f, hsv[1] + 0.2f); // Increase saturation
            hsv[2] = Math.min(1.0f, hsv[2] + 0.1f); // Slightly brighter

            int complementaryColor = Color.HSVToColor(hsv);
            return String.format("#%06X", (0xFFFFFF & complementaryColor));
        } catch (Exception e) {
            return Theme.defaultTheme().accent;
        }
    }

    // Determines whether white or black text is more readable on the given background color
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

    // Sends a theme description to the Gemini API and parses the returned theme specification
    private Theme requestThemeFromGemini(String description) throws IOException {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        boolean hasKey = apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR_GEMINI_API_KEY");
        if (!hasKey) {
            Log.w(TAG, "Missing GEMINI_API_KEY, skipping Gemini call");
            return null;
        }
        String prompt = buildEnhancedJsonPrompt(description);
        String bodyJson = "{\"contents\":[{\"parts\":[{\"text\":" + gson.toJson(prompt) + "}]}]}";
        Log.d(TAG, "Gemini request body (truncated): " + (bodyJson.length() > 300 ? bodyJson.substring(0, 300) + "..." : bodyJson));
        Request req = new Request.Builder()
                .url(GEMINI_ENDPOINT + apiKey)
                .post(RequestBody.create(bodyJson, MEDIA_TYPE_JSON))
                .build();
        try (Response resp = http.newCall(req).execute()) {
            String respText = resp.body() != null ? resp.body().string() : "";
            Log.d(TAG, "Gemini HTTP status: " + resp.code() + " response length: " + (respText == null ? 0 : respText.length()));
            if (!resp.isSuccessful() || respText.isEmpty()) {
                Log.w(TAG, "Gemini response unsuccessful or empty");
                return null;
            }
            try {
                JsonObject root = JsonParser.parseString(respText).getAsJsonObject();
                String text = root.getAsJsonArray("candidates").get(0).getAsJsonObject()
                        .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
                        .get("text").getAsString();
                Log.d(TAG, "Gemini candidate text (truncated): " + (text.length() > 300 ? text.substring(0,300) + "..." : text));
                String jsonOnly = extractJson(text);
                if (jsonOnly == null) {
                    Log.w(TAG, "Failed to extract JSON from Gemini text");
                    return null;
                }
                Log.d(TAG, "Extracted JSON: " + jsonOnly);

                // Try to parse as enhanced theme first, fall back to basic theme
                Theme spec = parseEnhancedThemeResponse(jsonOnly);
                if (spec == null) {
                    spec = gson.fromJson(jsonOnly, Theme.class);
                }

                if (spec == null || spec.background == null || spec.text == null) {
                    Log.w(TAG, "Parsed theme missing required fields");
                    return null;
                }
                if (spec.accent == null) spec.accent = generateComplementaryColor(spec.background);
                return spec;
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse Gemini response", e);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "HTTP request to Gemini failed", e);
            throw e;
        }
    }

    // Parses an enhanced theme JSON response with additional properties like emoji, mood, and theme name
    private Theme parseEnhancedThemeResponse(String jsonString) {
        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

            String background = getJsonString(json, "background");
            String text = getJsonString(json, "text");
            String accent = getJsonString(json, "accent");

            // If we have the basic required fields, create the theme
            if (background != null && text != null) {
                if (accent == null) {
                    accent = generateComplementaryColor(background);
                }

                // Log additional theme information if available
                String emoji = getJsonString(json, "emoji");
                String mood = getJsonString(json, "mood");
                String themeName = getJsonString(json, "themeName");

                if (emoji != null || mood != null || themeName != null) {
                    Log.d(TAG, "Enhanced theme properties - emoji: " + emoji + " mood: " + mood + " name: " + themeName);
                }

                return new Theme(background, text, accent);
            }

            return null;
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse enhanced theme response", e);
            return null;
        }
    }

    // Safely extracts a string value from a JSON object, returning null if the key doesn't exist
    private String getJsonString(JsonObject json, String key) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Constructs a detailed prompt for the Gemini API requesting a comprehensive theme with multiple properties
    private String buildEnhancedJsonPrompt(String description) {
        return "You are an expert UI/UX designer and color theorist. Based on the user's description, " +
                "create a comprehensive mobile app theme that captures the essence of their request. " +
                "Return valid JSON with these exact keys (no explanations, no backticks, just JSON):\n\n" +
                "{\n" +
                "  \"background\": \"#HEXCODE\",\n" +
                "  \"text\": \"#HEXCODE\",\n" +
                "  \"accent\": \"#HEXCODE\",\n" +
                "  \"secondary\": \"#HEXCODE\",\n" +
                "  \"cardBackground\": \"#HEXCODE\",\n" +
                "  \"border\": \"#HEXCODE\",\n" +
                "  \"headerText\": \"#HEXCODE\",\n" +
                "  \"emoji\": \"single emoji that represents the theme\",\n" +
                "  \"fontWeight\": \"normal|bold|light\",\n" +
                "  \"mood\": \"one word describing the theme mood\",\n" +
                "  \"themeName\": \"creative theme name\"\n" +
                "}\n\n" +
                "Guidelines:\n" +
                "- Ensure high contrast between background and text for readability\n" +
                "- Make accent colors vibrant but harmonious\n" +
                "- Choose colors that evoke the requested theme/mood\n" +
                "- Select an emoji that perfectly represents the theme\n" +
                "- Consider psychological impact of colors\n" +
                "- Create a cohesive visual experience\n\n" +
                "User description: \"" + description + "\"\n\n" +
                "Return JSON now:";
    }

    // Extracts the JSON object from a text response by finding the first '{' and last '}'
    private String extractJson(String text) {
        if (text == null) return null;
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) return null;
        return text.substring(start, end + 1);
    }
}