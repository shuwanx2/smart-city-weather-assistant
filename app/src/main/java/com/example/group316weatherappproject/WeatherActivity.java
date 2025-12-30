package com.example.group316weatherappproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

// Activity that shows weather data, generates a city view image, and opens Weather Insights
public class WeatherActivity extends AppCompatActivity {

    private static final String TAG = "WeatherActivity";

    // UI elements
    private TextView titleView;
    private ImageView cityImageView;
    private Button btnGenerateCityView;
    private Button btnBack;
    private TextView loadingStatusText;

    // Weather Insights button
    private Button btnWeatherInsights;

    // Weather UI elements
    private TextView weatherDateTime;
    private TextView weatherTemperature;
    private TextView weatherCondition;
    private TextView weatherHumidity;
    private TextView weatherWind;

    // Data
    private String cityName;
    private double latitude;
    private double longitude;

    // Data to use in the image prompt
    private boolean weatherIsLoaded = false;
    private double currentTempC;
    private String currentCondition;
    private int currentHumidity;
    private double currentWindSpeed;

    // HTTP + JSON helpers
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson json = new Gson();
    private static final String GEMINI_IMAGE_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent";
    private static final String OPENWEATHER_API_ENDPOINT =
            "https://api.openweathermap.org/data/2.5/weather";
    private static final MediaType MEDIA_TYPE_JSON =
            MediaType.parse("application/json; charset=utf-8");
    // ADD a new field to store the weather JSON
    private String weatherJson = null;

    // Initializes the weather screen, receives city data, and kicks off weather loading
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // Receive data from HomeActivity
        cityName = getIntent().getStringExtra("CITY_NAME");
        if (cityName == null) cityName = "Unknown City";

        latitude = getIntent().getDoubleExtra("LAT", 0.0);
        longitude = getIntent().getDoubleExtra("LON", 0.0);

        // Setup UI
        initializeViews();
        setupButtonListeners();

        titleView.setText(cityName);

        // Load weather data
        fetchWeatherData();

        // Apply theme
        ThemeManager.applySavedTheme(this, R.id.weather);
    }

    // Finds all views and sets initial state for the weather screen
    private void initializeViews() {
        titleView = findViewById(R.id.cityNameTitle);
        cityImageView = findViewById(R.id.cityViewImage);
        btnGenerateCityView = findViewById(R.id.generateCityViewButton);
        btnBack = findViewById(R.id.backButton);
        loadingStatusText = findViewById(R.id.loadingText);

        btnWeatherInsights = findViewById(R.id.weatherInsights);
        
        weatherDateTime = findViewById(R.id.weatherDateTime);
        weatherTemperature = findViewById(R.id.weatherTemperature);
        weatherCondition = findViewById(R.id.weatherCondition);
        weatherHumidity = findViewById(R.id.weatherHumidity);
        weatherWind = findViewById(R.id.weatherWind);

        cityImageView.setImageDrawable(null);
        cityImageView.setVisibility(View.GONE);


    }

    /**
     * Sets up all button click listeners for the WeatherActivity Screen
    */
    private void setupButtonListeners() {

        btnGenerateCityView.setOnClickListener(v -> generateCityView());

        // Back to Home always navigates back to home
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(WeatherActivity.this, HomeActivity.class);
            // if HomeActivity in the back of the stack bring it to the front
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            // Close WeatherActivity so Back doesn't return here
            finish();

        });

        // WEATHER INSIGHTS BUTTON
        btnWeatherInsights.setOnClickListener(v -> {
            // asks user to wait if the weather is not loaded
            if (!weatherIsLoaded) {
                Toast.makeText(this, "please wait a moment :)", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create intent to launch InsightsActivity
            Intent intent = new Intent(WeatherActivity.this, InsightsActivity.class);
            intent.putExtra("cityName", cityName);

            // Pass the raw weather JSON data for LLM processing
            if (weatherJson != null && !weatherJson.isEmpty()) {
                intent.putExtra("weatherJson", weatherJson);
            }

            startActivity(intent);
        });
    }

    // Builds a prompt and calls Gemini to generate a photorealistic city view image
    private void generateCityView() {
        loadingStatusText.setVisibility(View.VISIBLE);
        btnGenerateCityView.setEnabled(false);

        cityImageView.setVisibility(View.GONE);
        cityImageView.setImageDrawable(null);

        StringBuilder sb = new StringBuilder();
        sb.append("Create a realistic and detailed image of ")
          .append(cityName)
          .append(" showing its iconic views. ");
        if (weatherCondition != null && weatherCondition.getText() != null)
            sb.append(weatherCondition.getText()).append(". ");
        if (weatherTemperature != null && weatherTemperature.getText() != null)
            sb.append(weatherTemperature.getText()).append(". ");
        if (weatherDateTime != null && weatherDateTime.getText() != null)
            sb.append(weatherDateTime.getText()).append(". ");
        if (weatherHumidity != null && weatherHumidity.getText() != null)
            sb.append(weatherHumidity.getText()).append(". ");
        if (weatherWind != null && weatherWind.getText() != null)
            sb.append(weatherWind.getText()).append(". ");
        sb.append("The image should be photorealistic and reflect these conditions.");

        String prompt = sb.toString();
        Log.d(TAG, "Generating city view for: " + cityName + " with prompt: " + prompt);

        new Thread(() -> {
            try {
                String apiKey = BuildConfig.GEMINI_API_KEY;
                Request req = prepareGeminiImageRequest(prompt, apiKey);
                Bitmap generatedImage = executeGeminiImageRequest(req);

                runOnUiThread(() -> {
                    if (generatedImage != null) {
                        cityImageView.setImageBitmap(generatedImage);
                        cityImageView.setVisibility(View.VISIBLE);
                        // Hide the generate button after a successful generation
                        btnGenerateCityView.setVisibility(View.GONE);
                        Toast.makeText(this, "City view generated!", Toast.LENGTH_SHORT).show();
                    } else {
                        cityImageView.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to generate city view", Toast.LENGTH_SHORT).show();
                    }

                    loadingStatusText.setVisibility(View.GONE);
                    btnGenerateCityView.setEnabled(true);
                });

            } catch (IOException e) {
                Log.e(TAG, "Error generating city view", e);
                runOnUiThread(() -> {
                    cityImageView.setVisibility(View.GONE);
                    cityImageView.setImageDrawable(null);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadingStatusText.setVisibility(View.GONE);
                    btnGenerateCityView.setEnabled(true);
                });
            }
        }).start();
    }

    // Builds an HTTP request for the Gemini image generation endpoint
    private Request prepareGeminiImageRequest(String prompt, String apiKey) throws IOException {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_GEMINI_API_KEY")) {
            throw new IOException("Gemini API key missing");
        }

        JsonObject requestJson = buildImageRequestJson(prompt);
        String jsonString = json.toJson(requestJson);

        RequestBody body = RequestBody.create(jsonString, MEDIA_TYPE_JSON);

        return new Request.Builder()
                .url(GEMINI_IMAGE_ENDPOINT)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
    }

    // Constructs the JSON body for a Gemini image generation request
    private JsonObject buildImageRequestJson(String prompt) {
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
        JsonArray responseModalities = new JsonArray();
        responseModalities.add("TEXT");
        responseModalities.add("IMAGE");
        generationConfig.add("responseModalities", responseModalities);

        request.add("generationConfig", generationConfig);

        return request;
    }

    // Executes the Gemini image request and returns a Bitmap if successful
    private Bitmap executeGeminiImageRequest(Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                String error = response.body() != null ? response.body().string() : "No error body";
                Log.e(TAG, "API error: " + error);
                throw new IOException("API request failed");
            }

            String responseBody = response.body().string();
            return parseImageResponse(responseBody);
        }
    }

    // Parses the Gemini image response and decodes the inline base64 image
    private Bitmap parseImageResponse(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();

            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) return null;

            JsonObject first = candidates.get(0).getAsJsonObject();
            JsonObject content = first.getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");

            for (int i = 0; i < parts.size(); i++) {
                JsonObject part = parts.get(i).getAsJsonObject();

                if (part.has("inlineData")) {
                    String base64Data = part.getAsJsonObject("inlineData").get("data").getAsString();
                    byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);

                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse image response", e);
        }
        return null;
    }

    // Fetches weather data from OpenWeatherMap API
    private void fetchWeatherData() {
        new Thread(() -> {
            try {
                String apiKey = BuildConfig.OPENWEATHER_API_KEY;
                if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_OPENWEATHER_API_KEY")) {
                    runOnUiThread(() -> updateWeatherError("Weather API key missing"));
                    return;
                }

                if (latitude == 0.0 && longitude == 0.0) {
                    Log.e(TAG, "Invalid coordinates: (0.0, 0.0)");
                    runOnUiThread(() -> updateWeatherError("Invalid city coordinates"));
                    return;
                }

                String url = OPENWEATHER_API_ENDPOINT + 
                        "?lat=" + latitude + 
                        "&lon=" + longitude + 
                        "&appid=" + apiKey + 
                        "&units=metric";

                Log.d(TAG, "Fetching weather for: " + cityName + " at (" + latitude + ", " + longitude + ")");
                Log.d(TAG, "Weather API URL: " + url);

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String error = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "Weather API error (HTTP " + response.code() + "): " + error);
                        final String errorMsg = "HTTP " + response.code() + ": " + error;
                        runOnUiThread(() -> {
                            updateWeatherError("Failed to fetch weather");
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    String responseBody = response.body().string();
                    weatherJson = responseBody;
                    Log.d(TAG, "Got response, parsing...");
                    parseWeatherData(responseBody);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching weather", e);
                runOnUiThread(() -> updateWeatherError("Error: " + e.getMessage()));
            }
        }).start();
    }

    // Parses JSON response from weather API
    private void parseWeatherData(String jsonResponse) {
        try {
            Log.d(TAG, "Weather API Response: " + jsonResponse);
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();

            JsonObject main = root.getAsJsonObject("main");
            double temp = main.get("temp").getAsDouble();
            int humidity = main.get("humidity").getAsInt();

            JsonArray weatherArray = root.getAsJsonArray("weather");
            String condition = weatherArray.get(0).getAsJsonObject().get("description").getAsString();

            JsonObject wind = root.getAsJsonObject("wind");
            double windSpeed = wind.get("speed").getAsDouble();

            long timestamp = root.get("dt").getAsLong();

            Log.d(TAG, "Successfully parsed weather data");
            runOnUiThread(() -> updateWeatherUI(timestamp, temp, condition, humidity, windSpeed));

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse weather data", e);
            runOnUiThread(() -> updateWeatherError("Failed to parse weather data"));
        }
    }

    // Updates UI with weather information
    private void updateWeatherUI(long timestamp, double temp, String condition, int humidity, double windSpeed) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String dateTime = sdf.format(new java.util.Date(timestamp * 1000));

        String displayCondition = condition.substring(0, 1).toUpperCase() + condition.substring(1);


        weatherDateTime.setText("Date & Time (UTC): " + dateTime);
        weatherTemperature.setText(String.format("Temperature: %.1f°C", temp));
        weatherCondition.setText("Condition: " + condition.substring(0, 1).toUpperCase() + condition.substring(1));
        weatherHumidity.setText("Humidity: " + humidity + "%");
        weatherWind.setText(String.format("Wind: %.1f m/s", windSpeed));

        currentTempC = temp;
        currentCondition = displayCondition;
        currentHumidity = humidity;
        currentWindSpeed = windSpeed;
        weatherIsLoaded = true;
    }

    // Displays error message in weather fields
    private void updateWeatherError(String message) {
        weatherDateTime.setText("Date & Time: " + message);
        weatherTemperature.setText("Temperature: N/A");
        weatherCondition.setText("Condition: N/A");
        weatherHumidity.setText("Humidity: N/A");
        weatherWind.setText("Wind: N/A");
    }
}
