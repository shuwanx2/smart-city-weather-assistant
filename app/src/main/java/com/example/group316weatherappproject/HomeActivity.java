package com.example.group316weatherappproject;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

public class HomeActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private DatabaseHelper databaseHelper;

    private Button settingsButton;
    private final ArrayList<City> cityList = new ArrayList<>();
    private CityAdapter adapter;


    // initializes the home screen, enforces login, applies theme,
    // sets up RecycleView, loads saved cities, and wires button callbacks
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Disable back using OnBackPressedDispatcher (no-op handler)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Intentionally no-op to disable back
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize managers
        sessionManager = new SessionManager(this);
        databaseHelper = new DatabaseHelper(this);

        // If not logged in → go to login
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        // Apply theme
        ThemeManager.applySavedTheme(this, R.id.home);

        // Accent color on top panel
        Theme currentTheme = ThemeManager.loadTheme(this);
        if (currentTheme != null && currentTheme.accent != null) {
            try {
                int accentColor = android.graphics.Color.parseColor(currentTheme.accent);
                findViewById(R.id.topPanelGraphic).setBackgroundColor(accentColor);
            } catch (Exception ignored) {}
        }

        // UI widgets
        settingsButton = findViewById(R.id.settingsButton);
        TextView titleText = findViewById(R.id.titleText);

        String username = sessionManager.getUsername();
        titleText.setText("Team 316 - " + username);

        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, Settings.class)));

        // RecyclerView
        RecyclerView recyclerView = findViewById(R.id.cityRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);

        Button addButton = findViewById(R.id.buttonAddLocation);

        // Load saved cities
        loadCitiesFromDatabase();

        // Adapter with delete + weather + map callbacks
        adapter = new CityAdapter(
                cityList,
                city -> {
                    // DELETE CITY
                    if (databaseHelper.deleteCity(username, city.name)) {
                        cityList.remove(city);
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Failed to delete city", Toast.LENGTH_SHORT).show();
                    }
                },
                city -> {
                    // OPEN WEATHER (now with lat/lon)
                    Intent intent = new Intent(HomeActivity.this, WeatherActivity.class);
                    intent.putExtra("CITY_NAME", city.name);
                    intent.putExtra("LAT", city.lat);
                    intent.putExtra("LON", city.lon);
                    startActivity(intent);
                },
                city -> {
                    Intent intent = new Intent(HomeActivity.this, MapActivity.class);
                    intent.putExtra("cityName", city.name);
                    intent.putExtra("lat", city.lat);
                    intent.putExtra("lon", city.lon);
                    startActivity(intent);
                }
        );

        recyclerView.setAdapter(adapter);

        addButton.setOnClickListener(v -> showAddCityDialog());
    }

    // --------------------------------------------------
    // DISPLAYS A DIALOG THAT ALLOWS USERS TO ADD A NEW CITY BY NAME AND SPECIFY LAT AND LON
    // --------------------------------------------------
    private void showAddCityDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_city, null);

        EditText inputName = dialogView.findViewById(R.id.inputCityName);
        EditText inputLat = dialogView.findViewById(R.id.inputCityLat);
        EditText inputLon = dialogView.findViewById(R.id.inputCityLon);

        new AlertDialog.Builder(this)
                .setTitle("Add City")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String cityName = inputName.getText().toString().trim();
                    String latStr = inputLat.getText().toString().trim();
                    String lonStr = inputLon.getText().toString().trim();

                    if (cityName.isEmpty()) {
                        // City name is mandatory
                        Toast.makeText(this, "City name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // If both lat & lon provided, use them directly
                    if (!latStr.isEmpty() && !lonStr.isEmpty()) {
                        try {
                            // Parse coordinates
                            double lat = Double.parseDouble(latStr);
                            double lon = Double.parseDouble(lonStr);
                            addCityWithCoords(cityName, lat, lon);
                        } catch (NumberFormatException nfe) {
                            // Invalid number format
                            Toast.makeText(this, "Invalid coordinates", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    // Otherwise, geocode the city name to get coordinates
                    Toast.makeText(this, "Looking up coordinates…", Toast.LENGTH_SHORT).show();
                    new Thread(() -> {
                        boolean resolved = false;
                        try {
                            // First try Android Geocoder
                            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                            List<Address> results = geocoder.getFromLocationName(cityName, 1);
                            if (results != null && !results.isEmpty()) {
                                // Found via Geocoder
                                Address addr = results.get(0);
                                double lat = addr.getLatitude();
                                double lon = addr.getLongitude();
                                runOnUiThread(() -> addCityWithCoords(cityName, lat, lon));
                                resolved = true;
                            }
                        } catch (IOException ignored) {
                            // fall through to Google HTTP geocoding
                        } catch (Exception ignored) {}

                        if (!resolved) {
                            // Fallback: Google Geocoding API via HTTP
                            try {
                                String apiKey = getGoogleMapsApiKey();
                                // Check if API key is available
                                if (apiKey == null || apiKey.isEmpty()) {
                                    runOnUiThread(() -> Toast.makeText(this, "No Google Maps API key configured", Toast.LENGTH_LONG).show());
                                    return;
                                }
                                // Perform geocoding request
                                double[] coords = geocodeWithGoogleApi(cityName, apiKey);
                                if (coords != null) {
                                    double lat = coords[0];
                                    double lon = coords[1];
                                    runOnUiThread(() -> addCityWithCoords(cityName, lat, lon));
                                } else {
                                    runOnUiThread(() -> Toast.makeText(this, "Couldn't find coordinates. Please enter latitude and longitude.", Toast.LENGTH_LONG).show());
                                }
                            } catch (Exception e) {
                                runOnUiThread(() -> Toast.makeText(this, "Geocoding error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Helper to persist city and refresh UI
    private void addCityWithCoords(String cityName, double lat, double lon) {
        String username = sessionManager.getUsername();
        if (databaseHelper.addCity(username, cityName, lat, lon)) {
            cityList.add(new City(cityName, lat, lon));
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "City added successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "City already exists or failed to add", Toast.LENGTH_SHORT).show();
        }
    }

    // Read the Google Maps API key from manifest meta-data
    private String getGoogleMapsApiKey() {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            if (bundle != null) {
                String key = bundle.getString("com.google.android.geo.API_KEY");
                if (key == null) key = bundle.getString("com.google.android.maps.v2.API_KEY");
                return key;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // Simple HTTP call to Google Geocoding API; returns [lat, lon] or null
    private double[] geocodeWithGoogleApi(String cityName, String apiKey) {
        HttpURLConnection connection = null; // HTTP connection handle
        BufferedReader reader = null; // Reader for the response stream
        try {
            // Encode the city name for use in URL query parameter
            String encoded = URLEncoder.encode(cityName, "UTF-8");

            // Build the Google Geocoding API URL with query parameters
            Uri uri = Uri.parse("https://maps.googleapis.com/maps/api/geocode/json")
                    .buildUpon()
                    .appendQueryParameter("address", encoded) // address parameter
                    .appendQueryParameter("key", apiKey)     // API key parameter
                    .build();

            // Open HTTP connection to the built URL
            URL url = new URL(uri.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.connect();

            // Get response code and choose input stream accordingly
            int code = connection.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? connection.getInputStream() : connection.getErrorStream();

            // Read the entire response into a StringBuilder
            reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString(); // Full JSON response as string

            // Parse JSON and extract lat/lon from first result
            JSONObject root = new JSONObject(json);
            String status = root.optString("status");
            if (!"OK".equals(status)) { // If API did not return OK, treat as failure
                return null;
            }
            JSONArray results = root.optJSONArray("results");
            if (results == null || results.length() == 0) return null;
            JSONObject first = results.getJSONObject(0);
            JSONObject location = first.getJSONObject("geometry").getJSONObject("location");
            double lat = location.getDouble("lat"); // latitude value
            double lon = location.getDouble("lng"); // longitude value
            return new double[]{lat, lon}; // Return coords as array
        } catch (Exception e) {
            // On any error return null to indicate failure
            return null;
        } finally {
            // Clean up: close reader and disconnect connection if they were opened
            if (reader != null) try { reader.close(); } catch (IOException ignored) {}
            if (connection != null) connection.disconnect();
        }
    }

    // --------------------------------------------------
    // LOAD CITIES FROM THE DATABASE
    // --------------------------------------------------
    private void loadCitiesFromDatabase() {
        String username = sessionManager.getUsername();
        List<City> storedCities = databaseHelper.getCitiesForUser(username);

        cityList.clear();
        cityList.addAll(storedCities);
    }

    // --------------------------------------------------
    // NAVIGATES BACK TO THE LOGIN ACTIVITY AND CLEARS THE ACTIVITY STACK
    // --------------------------------------------------
    private void navigateToLogin() {
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
