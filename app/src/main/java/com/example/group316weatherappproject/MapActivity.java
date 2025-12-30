package com.example.group316weatherappproject;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.group316weatherappproject.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

// Activity that displays a Google Map for a selected city with basic zoom and close controls
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Test-only override for coordinates and city name used by instrumented tests)
    private static Double testLat = null;
    private static Double testLon = null;
    private static String testCityName = null;

    public static void setTestCoordinates(Double lat, Double lon) {
        testLat = lat;
        testLon = lon;
    }

    public static void setTestCityName(String cityNameOverride) {
        testCityName = cityNameOverride;
    }

    private GoogleMap mMap;
    double lat, lon;
    String cityName;

    private Button closeMap;
    private Button zoomIn;
    private Button zoomOut;

    private TextView cityDetails;

    // Initializes the map screen and loads the Google Map fragment
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // initialize buttons/views
        initializeViews();

        // Receive data from DetailsActivity
        dataReciever();
        setupListeners();
        setupViews();


        // Load the map fragment
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    // Initializes buttons and text views from layout
    private void initializeViews() {
        closeMap = findViewById(R.id.escapeMap);
        zoomIn = findViewById(R.id.zoom_in);
        zoomOut = findViewById(R.id.zoom_out);
        cityDetails = findViewById(R.id.cityDetailsView);
    }

    // Receives city name and coordinates from the launching activity
    private void dataReciever() {
        cityName = getIntent().getStringExtra("cityName");
        lat = getIntent().getDoubleExtra("lat", 0);
        lon = getIntent().getDoubleExtra("lon", 0);

        // If test coordinates are provided, override the real ones.
        if (testLat != null && testLon != null) {
            lat = testLat;
            lon = testLon;
        }

        // If a test city name is provided, override the real one.
        if (testCityName != null) {
            cityName = testCityName;
        }
    }

    // Sets up button listeners for closing and zooming the map
    private void setupListeners() {
        closeMap.setOnClickListener(v -> closeMapView());
        zoomIn.setOnClickListener(v -> zoomMapIn());
        zoomOut.setOnClickListener(v -> zoomMapOut());
    }

    // Displays formatted city information: name and coordinates
    private void setupViews() {
        double trunc_lat = Math.floor(lat);
        double trunc_lon = Math.floor(lon);
        String city_details_text = cityName + "\n" + trunc_lat + "°N" + ", " + trunc_lon + "°W";
        cityDetails.setText(city_details_text);

    }

    // Called when the Google Map is ready, adds marker and centers camera
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Create location point
        LatLng cityLoc = new LatLng(lat, lon);

        // Add marker
        mMap.addMarker(new MarkerOptions().position(cityLoc).title(cityName));

        // Zoom to city
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cityLoc, 10));
    }

    // closes the map activity
    private void closeMapView() {
        finish();
    }

    // zooms the map in
    private void zoomMapIn() {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());

    }

    // zooms the map out
    private void zoomMapOut() {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }
}
