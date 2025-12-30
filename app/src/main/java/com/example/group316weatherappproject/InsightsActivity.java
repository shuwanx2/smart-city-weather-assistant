package com.example.group316weatherappproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Activity that generates AI-powered weather insights for a selected city.
 * Displays a summary of the current conditions and interactive questions
 * which are answered using the Gemini API, with local fallbacks.
 */
public class InsightsActivity extends AppCompatActivity {

    private static final String TAG = "InsightsActivity";

    private static final String GEMINI_TEXT_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent";
    private static final MediaType MEDIA_TYPE_JSON =
            MediaType.parse("application/json; charset=utf-8");

    // UI Elements
    private TextView titleView;
    private TextView weatherSummaryView;
    private LinearLayout questionsContainer;
    private LinearLayout answerContainer;
    private ProgressBar loadingIndicator;
    private Button backButton;
    private ScrollView scrollView;

    // Data
    private String cityName;
    private String weatherJson;
    private WeatherData weatherData;

    // HTTP client
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson json = new Gson();

    // Weather data holder class

    /**
     * Simple data holder for parsed weather information
     * used when generating prompts and fallback questions.
     */
    private static class WeatherData {
        double temperature;
        String condition;
        int humidity;
        double windSpeed;
        String dateTime;

        // Constructor to initialize all weather fields for this data object
        WeatherData(double temp, String cond, int hum, double wind, String dt) {
            temperature = temp;
            condition = cond;
            humidity = hum;
            windSpeed = wind;
            dateTime = dt;
        }
    }

    /**
     * Initializes the InsightsActivity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insights);

        cityName = getIntent().getStringExtra("cityName");
        weatherJson = getIntent().getStringExtra("weatherJson");

        if (cityName == null) cityName = "Unknown City";

        initializeViews();
        setupListeners();

        ThemeManager.applySavedTheme(this, R.id.insights);

        extractWeatherData();
        generateQuestions();
    }

    /**
     * Finds and initializes all views in the layout,
     * sets the title text, and hides the answer container by default.
     */
    private void initializeViews() {
        titleView = findViewById(R.id.insightsTitleView);
        weatherSummaryView = findViewById(R.id.weatherSummaryView);
        questionsContainer = findViewById(R.id.questionsContainer);
        answerContainer = findViewById(R.id.answerContainer);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        backButton = findViewById(R.id.backButton);
        scrollView = findViewById(R.id.scrollView);

        titleView.setText("Weather Insights - " + cityName);
        answerContainer.setVisibility(View.GONE);
    }

    /**
     * Sets up click listeners for UI controls,
     * currently only the back button to close the activity.
     */
    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
    }

    /**
     * Parses the OpenWeather JSON into a WeatherData object.
     * If parsing fails or no JSON is provided, falls back to a mock sample.
     * Also updates the weather summary text shown at the top.
     */
    private void extractWeatherData() {
        if (weatherJson != null && !weatherJson.isEmpty()) {
            try {
                JsonObject root = JsonParser.parseString(weatherJson).getAsJsonObject();

                JsonObject main = root.getAsJsonObject("main");
                double temp = main.get("temp").getAsDouble();
                int humidity = main.get("humidity").getAsInt();

                JsonArray weatherArray = root.getAsJsonArray("weather");
                String condition = weatherArray.get(0).getAsJsonObject().get("description").getAsString();

                JsonObject wind = root.getAsJsonObject("wind");
                double windSpeed = wind.get("speed").getAsDouble();

                long timestamp = root.get("dt").getAsLong();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                String dateTime = sdf.format(new java.util.Date(timestamp * 1000));

                weatherData = new WeatherData(temp, condition, humidity, windSpeed, dateTime);

            } catch (Exception e) {
                Log.e(TAG, "Failed to parse weather JSON", e);
                createMockWeatherData();
            }
        } else {
            createMockWeatherData();
        }

        weatherSummaryView.setText(String.format(Locale.US,
                "Conditions: %.1f°C, %s, Humidity: %d%%, Wind: %.1f m/s",
                weatherData.temperature, weatherData.condition,
                weatherData.humidity, weatherData.windSpeed));
    }

    /**
     * Creates a hard-coded WeatherData sample used when real
     * weather JSON is missing or invalid.
     */
    private void createMockWeatherData() {
        weatherData = new WeatherData(6.2, "few clouds", 46, 3.1, "2024-11-16 18:52");
    }

    /**
     * Builds a natural-language prompt describing the current conditions
     * and sends it to Gemini to generate 4 practical weather-related questions.
     * Falls back to local questions if the API fails.
     */
    private void generateQuestions() {
        loadingIndicator.setVisibility(View.VISIBLE);

        StringBuilder sb = new StringBuilder();
        sb.append("Generate exactly 4 practical weather questions for ")
                .append(cityName)
                .append(" with current conditions: ");
        sb.append(String.format(Locale.US, "%.1f°C, %s, %d%% humidity, %.1f m/s wind. ",
                weatherData.temperature, weatherData.condition, weatherData.humidity, weatherData.windSpeed));
        sb.append("Format as numbered list:");
        sb.append("1. [Question about clothing/comfort]");
        sb.append("2. [Question about outdoor activities]");
        sb.append("3. [Question about health/safety]");
        sb.append("4. [Question about planning/preparation]");
        sb.append("Each question should be practical and actionable for these specific conditions.");

        String prompt = sb.toString();
        Log.d(TAG, "Question generation prompt: " + prompt);

        new Thread(() -> {
            try {
                String apiKey = BuildConfig.GEMINI_API_KEY;
                Request request = prepareGeminiTextRequest(prompt, apiKey);
                String response = executeGeminiTextRequest(request);
                List<String> questions = parseQuestionsFromResponse(response);

                runOnUiThread(() -> {
                    loadingIndicator.setVisibility(View.GONE);

                    if (questions != null && !questions.isEmpty()) {
                        displayQuestions(questions);
                        Toast.makeText(this, "✅ Generated " + questions.size() + " AI questions!", Toast.LENGTH_SHORT).show();
                    } else {
                        showFallbackQuestions();
                        Toast.makeText(this, "Using fallback questions", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to generate questions", e);
                runOnUiThread(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    showFallbackQuestions();
                    Toast.makeText(this, "Error generating questions: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Builds an HTTP Request object for the Gemini text endpoint
     * using the given prompt and API key.
     *
     */
    private Request prepareGeminiTextRequest(String prompt, String apiKey) throws IOException {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_GEMINI_API_KEY")) {
            throw new IOException("Gemini API key missing");
        }

        JsonObject requestJson = buildTextRequestJson(prompt);
        String jsonString = json.toJson(requestJson);

        RequestBody body = RequestBody.create(jsonString, MEDIA_TYPE_JSON);

        return new Request.Builder()
                .url(GEMINI_TEXT_ENDPOINT)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
    }

    /**
     * Constructs the JSON payload for a Gemini text-only request,
     * including contents and generationConfig parameters.
     */
    private JsonObject buildTextRequestJson(String prompt) {
        JsonObject request = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();

        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);

        content.add("parts", parts);
        contents.add(content);

        request.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("maxOutputTokens", 400);
        request.add("generationConfig", generationConfig);

        return request;
    }

    /**
     * Executes the given Gemini request synchronously and returns
     * the raw JSON response body as a string, or throws on failure.
     *
     */
    private String executeGeminiTextRequest(Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                String error = response.body() != null ? response.body().string() : "No error body";
                Log.e(TAG, "API error: " + error);
                throw new IOException("API request failed");
            }

            String responseBody = response.body().string();
            return responseBody;
        }
    }


    /**
     * Parses a Gemini response body and extracts a list of
     * numbered questions from the generated text.
     */
    private List<String> parseQuestionsFromResponse(String responseBody) {
        List<String> questions = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();

            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) return questions;

            JsonObject first = candidates.get(0).getAsJsonObject();
            JsonObject content = first.getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");

            if (parts != null && parts.size() > 0) {
                String text = parts.get(0).getAsJsonObject().get("text").getAsString();
                Log.d(TAG, "Generated text: " + text);

                String[] lines = text.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.matches("^\\d+\\.\\s+.*")) {
                        String question = line.replaceFirst("^\\d+\\.\\s+", "").trim();
                        if (!question.isEmpty() && question.length() > 10) {
                            questions.add(question);
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse questions response", e);
        }
        return questions;
    }

    /**
     * Populates the questions container with a button for each question.
     * Each button, when tapped, triggers AI generated answers.
     */
    private void displayQuestions(List<String> questions) {
        questionsContainer.removeAllViews();

        for (int i = 0; i < questions.size(); i++) {
            String question = questions.get(i);
            Button questionButton = createQuestionButton(question);
            // Assign a stable ID to the first button for testing
            if (i == 0) {
                questionButton.setId(R.id.firstQuestionButton);
            }
            questionsContainer.addView(questionButton);
        }
    }

    /**
     * Creates a styled button for a single question, applying
     * the current accent color and wiring up its click handler.
     */
    private Button createQuestionButton(String question) {
        Button button = new Button(this);
        button.setText(question);
        button.setId(View.generateViewId());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 16, 0, 16);
        button.setLayoutParams(params);

        Theme currentTheme = ThemeManager.loadTheme(this);
        if (currentTheme != null && currentTheme.accent != null) {
            try {
                int accentColor = android.graphics.Color.parseColor(currentTheme.accent);
                button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
            } catch (Exception ignored) {}
        }

        button.setOnClickListener(v -> answerQuestion(question));

        return button;
    }

    /**
     * Sends the selected question plus current weather conditions
     * to Gemini to generate a short, practical answer, then displays it.
     */
    private void answerQuestion(String question) {
        loadingIndicator.setVisibility(View.VISIBLE);
        answerContainer.setVisibility(View.GONE);

        StringBuilder sb = new StringBuilder();
        sb.append("Answer this weather question for ").append(cityName).append(": \"").append(question).append("\"");
        sb.append("\n\nCurrent conditions: ");
        sb.append(String.format(Locale.US, "%.1f°C, %s, %d%% humidity, %.1f m/s wind",
                weatherData.temperature, weatherData.condition, weatherData.humidity, weatherData.windSpeed));
        sb.append("\n\nProvide a helpful, practical answer in 2-3 sentences. Be specific and actionable for these conditions.");

        String prompt = sb.toString();

        new Thread(() -> {
            try {
                String apiKey = BuildConfig.GEMINI_API_KEY;
                Request request = prepareGeminiTextRequest(prompt, apiKey);
                String responseBody = executeGeminiTextRequest(request);
                String answer = parseAnswerFromResponse(responseBody);

                runOnUiThread(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    showAnswer(question, answer);
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to get answer", e);
                runOnUiThread(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    showAnswer(question, "Sorry, I couldn't generate an answer at the moment. Please try again.");
                });
            }
        }).start();
    }

    /**
     * Parses a Gemini answer response and extracts the first
     * text part as the answer. Returns a fallback message if needed.
     */
    private String parseAnswerFromResponse(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();

            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                return "I couldn't generate a proper answer. Please try asking another question.";
            }

            JsonObject first = candidates.get(0).getAsJsonObject();
            JsonObject content = first.getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");

            if (parts != null && parts.size() > 0) {
                return parts.get(0).getAsJsonObject().get("text").getAsString().trim();
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse answer response", e);
        }

        return "I couldn't generate a proper answer. Please try asking another question.";
    }


    /**
     * Renders the selected question and its answer into the
     * answer container and scrolls the view down to show it.
     */
    private void showAnswer(String question, String answer) {
        answerContainer.removeAllViews();

        TextView questionView = new TextView(this);
        questionView.setText("Q: " + question);
        questionView.setTextSize(16);
        questionView.setTypeface(questionView.getTypeface(), android.graphics.Typeface.BOLD);
        questionView.setPadding(0, 0, 0, 16);

        TextView answerView = new TextView(this);
        answerView.setText("A: " + answer);
        answerView.setTextSize(14);
        answerView.setLineSpacing(4, 1.2f);

        answerContainer.addView(questionView);
        answerContainer.addView(answerView);
        answerContainer.setVisibility(View.VISIBLE);

        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    /**
     * Builds a set of generated fallback questions
     * based on the current temperature range when Gemini is unavailable.
     */
    private List<String> getFallbackQuestions() {
        List<String> fallbackQuestions = new ArrayList<>();

        if (weatherData.temperature < 10) {
            fallbackQuestions.add("What should I wear in this cold weather?");
            fallbackQuestions.add("Is it safe to exercise outdoors in these conditions?");
            fallbackQuestions.add("How can I stay warm and comfortable?");
        } else if (weatherData.temperature > 25) {
            fallbackQuestions.add("How can I stay cool in this warm weather?");
            fallbackQuestions.add("What precautions should I take for outdoor activities?");
            fallbackQuestions.add("What's the best clothing for this heat?");
        } else {
            fallbackQuestions.add("Is this good weather for outdoor activities?");
            fallbackQuestions.add("What should I plan for with these conditions?");
            fallbackQuestions.add("What's the most comfortable clothing choice?");
        }

        return fallbackQuestions;
    }

    /**
     * Displays fallback questions in the UI
     * when Gemini fails or returns no usable questions.
     */
    private void showFallbackQuestions() {
        List<String> questions = getFallbackQuestions();
        displayQuestions(questions);
    }
}