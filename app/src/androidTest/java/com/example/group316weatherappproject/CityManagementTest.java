package com.example.group316weatherappproject;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;


/**
 * Instrumented test suite for managing cities and related features in HomeActivity.
 * These tests simulate user interactions to verify that adding/removing cities,
 * logging off, weather retrieval, insight generation, map display, and mocked
 * locations behave correctly, validated through UI assertions.
 */
@RunWith(AndroidJUnit4.class)
public class CityManagementTest {
    // Activity scenario for the home activity
    private ActivityScenario<HomeActivity> homeScenario;

    // Test city names with unique timestamps to avoid duplicates
    private String TEST_CITY_1;
    private String TEST_CITY_2;

    // Displayed city names (as they appear in the UI, assuming no case conversion)
    private String DISPLAYED_CITY_1;
    private String DISPLAYED_CITY_2;

    // Test user credentials
    private static final String TEST_USER = "testuser";
    private static final String TEST_PASS = "testpass";

    // OpenWeather configuration (must match app code)
    private static final String OPENWEATHER_API_ENDPOINT =
            "https://api.openweathermap.org/data/2.5/weather";

    private final OkHttpClient httpClient = new OkHttpClient();

    /**
     * Custom ViewAction to click a child view within a RecyclerView item.
     * @param viewId The ID of the child view to click.
     * @return The ViewAction instance.
     */
    public static ViewAction clickChildView(final int viewId) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click child view with ID";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View child = view.findViewById(viewId);
                if (child != null) child.performClick();
            }
        };
    }

    /**
     * Setup method run before each test.
     * Initializes unique city names, registers a test user, logs in, adds test cities to the database,
     * and launches the HomeActivity.
     * @throws InterruptedException If sleep is interrupted.
     */
    @Before
    public void setup() throws InterruptedException {
        long timestamp = System.currentTimeMillis();
        TEST_CITY_1 = "Chicago";
        TEST_CITY_2 = "Champaign";
        DISPLAYED_CITY_1 = TEST_CITY_1;
        DISPLAYED_CITY_2 = TEST_CITY_2;

        Context ctx = ApplicationProvider.getApplicationContext();
        DatabaseHelper db = new DatabaseHelper(ctx);
        db.registerUser(TEST_USER, TEST_PASS); // Pre-create user
        SessionManager sm = new SessionManager(ctx);
        sm.createLoginSession(TEST_USER, "#6750a5", "#000000");

        // Pre-cleanup and insert test cities
        db.deleteCity(TEST_USER, TEST_CITY_1.toLowerCase());
        db.deleteCity(TEST_USER, TEST_CITY_2.toLowerCase());
        db.addCity(TEST_USER, TEST_CITY_1, 41.8781, -87.6298); // Chicago
        db.addCity(TEST_USER, TEST_CITY_2, 40.1106, -88.2073); // Champaign

        homeScenario = ActivityScenario.launch(HomeActivity.class);
        Thread.sleep(1000); // Wait for activity to load
    }

    /**
     * Teardown method run after each test.
     * Closes the activity scenario and cleans up the database by deleting test cities and user.
     */
    @After
    public void tearDown() {
        if (homeScenario != null) homeScenario.close();
        Context ctx = ApplicationProvider.getApplicationContext();
        DatabaseHelper db = new DatabaseHelper(ctx);
        db.deleteCity(TEST_USER, TEST_CITY_1.toLowerCase());
        db.deleteCity(TEST_USER, TEST_CITY_2.toLowerCase());
        db.deleteUser(TEST_USER); // Cleanup user
    }

    /**
     * Tests adding a new city to the list.
     * Clicks add button, enters city details, submits, and verifies the city appears in the RecyclerView.
     * @throws InterruptedException If sleep is interrupted.
     */
    @Test
    public void testAddNewCity() throws InterruptedException {
        onView(withId(R.id.buttonAddLocation)).perform(click());
        Thread.sleep(500);
        onView(withId(R.id.inputCityName)).perform(click(), replaceText("New York"), closeSoftKeyboard());
        Thread.sleep(500);
        // onView(withId(R.id.inputCityLat)).perform(click(), replaceText("41.8781"), closeSoftKeyboard());
        Thread.sleep(500);
        // onView(withId(R.id.inputCityLon)).perform(click(), replaceText("-87.6298"), closeSoftKeyboard());
        Thread.sleep(500);
        onView(withText("Add")).perform(click());
        Thread.sleep(2000);
        onView(withId(R.id.cityRecyclerView))
                .check(matches(hasDescendant(withText("New York"))));
    }

    /**
     * Tests removing an existing city from the list.
     * Verifies the city exists, deletes it, and checks it no longer exists.
     * @throws InterruptedException If sleep is interrupted.
     */
    @Test
    public void testRemoveExistingCity() throws InterruptedException {
        onView(withId(R.id.cityRecyclerView))
                .check(matches(hasDescendant(withText(DISPLAYED_CITY_1))));
        onView(withId(R.id.cityRecyclerView))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(DISPLAYED_CITY_1)),
                        clickChildView(R.id.deleteCityButton)));
        Thread.sleep(1000);
        onView(withText(DISPLAYED_CITY_1)).check(doesNotExist());
    }

    /**
     * Tests user logoff functionality.
     * Navigates to settings, clicks logout, and verifies return to login screen.
     * @throws InterruptedException If sleep is interrupted.
     */
    @Test
    public void testUserLogOff() throws InterruptedException {
        onView(withId(R.id.settingsButton)).perform(click());
        Thread.sleep(500);
        onView(withId(R.id.logoutButton)).perform(click());
        Thread.sleep(500);
        onView(withId(R.id.LoginButton)).check(matches(isDisplayed()));
    }

    /**
     * Tests the weather insight feature.
     * Navigates to weather for a city, opens insights, clicks a question, and verifies answer displays.
     * @throws InterruptedException If sleep is interrupted.
     */
    @Test
    public void testWeatherInsightFeature() throws InterruptedException {
        Thread.sleep(1000); // Wait for weather data to load

        onView(withId(R.id.cityRecyclerView))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(DISPLAYED_CITY_1)),
                        clickChildView(R.id.weatherButton)));
        Thread.sleep(5000); // Wait for weather data to load
        onView(withId(R.id.weatherInsights)).perform(click());
        Thread.sleep(10000); // Wait for questions to generate
        onView(withId(R.id.firstQuestionButton)).perform(click());
        Thread.sleep(4000);
        onView(withId(R.id.answerContainer)).check(matches(isDisplayed()));
    }

    /**
     * Tests weather feature for the first city.
     * Navigates to weather and verifies temperature is loaded (not "Loading...").
     * @throws InterruptedException If sleep is interrupted.
     */
    @Test
    public void testWeatherFeatureForCity1() throws InterruptedException {
        onView(withId(R.id.cityRecyclerView))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(equalToIgnoringCase(DISPLAYED_CITY_1))),
                        clickChildView(R.id.weatherButton)));
        Thread.sleep(5000); // wait for WeatherActivity to fetch data

        // Read displayed temperature text from the UI thread
        final String[] displayedText = new String[1];
        Espresso.onView(withId(R.id.weatherTemperature)).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Read temperature text";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView tv = (TextView) view;
                displayedText[0] = tv.getText().toString();
            }
        });

        double apiTemp = fetchTemperatureFromApi(41.8781, -87.6298); // Chicago

        // The UI shows something like "Temperature: 5.1°C" – extract the numeric part
        String uiTempPart = displayedText[0].replace("Temperature:", "")
                .replace("°C", "").trim();
        double uiTemp = Double.parseDouble(uiTempPart);

        org.junit.Assert.assertEquals(apiTemp, uiTemp, 1.0);
    }

    /**
     * Tests weather feature for the second city.
     * Navigates to weather and verifies temperature is loaded (not "Loading...").
     * @throws InterruptedException If sleep is interrupted.
     */
    @Test
    public void testWeatherFeatureForCity2() throws InterruptedException {
        onView(withId(R.id.cityRecyclerView))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(equalToIgnoringCase(DISPLAYED_CITY_2))),
                        clickChildView(R.id.weatherButton)));
        Thread.sleep(5000); // wait for WeatherActivity to fetch data

        final String[] displayedText = new String[1];
        Espresso.onView(withId(R.id.weatherTemperature)).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Read temperature text";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView tv = (TextView) view;
                displayedText[0] = tv.getText().toString();
            }
        });

        double apiTemp = fetchTemperatureFromApi(40.1106, -88.2073); // Champaign

        String uiTempPart = displayedText[0].replace("Temperature:", "")
                .replace("°C", "").trim();
        double uiTemp = Double.parseDouble(uiTempPart);

        org.junit.Assert.assertEquals(apiTemp, uiTemp, 1.0);
    }

    /**
     * Helper that queries the OpenWeather API for the given coordinates
     * using the same endpoint and units as the app.
     */
    private double fetchTemperatureFromApi(double lat, double lon) {
        String apiKey = BuildConfig.OPENWEATHER_API_KEY;
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_OPENWEATHER_API_KEY")) {
            throw new AssertionError("OPENWEATHER_API_KEY is not configured for tests");
        }

        String url = OPENWEATHER_API_ENDPOINT
                + "?lat=" + lat
                + "&lon=" + lon
                + "&appid=" + apiKey
                + "&units=metric";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new AssertionError("Weather API call failed with HTTP " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new AssertionError("Weather API response body was null");
            }

            String json = body.string();
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject main = root.getAsJsonObject("main");
            return main.get("temp").getAsDouble();

        } catch (IOException e) {
            throw new AssertionError("Error calling weather API: " + e.getMessage(), e);
        }
    }

    /**
     * Tests location feature for the first city.
     * Navigates to map and verifies city details contain the city name.
     * @throws InterruptedException If sleep is interrupted.
     */
    @Test
    public void testLocationFeatureForCity1() throws InterruptedException {
        onView(withId(R.id.cityRecyclerView))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(equalToIgnoringCase(DISPLAYED_CITY_1))),
                        clickChildView(R.id.mapButton)));
        Thread.sleep(5000);

        onView(withId(R.id.cityDetailsView))
                .check(matches(allOf(
                        isDisplayed(),
                        withText(containsString("chicago")),
                        withText(containsString("41.0°N")),
                        withText(containsString("-88.0°W"))
                )));

    }

    /**
     * Tests location feature for the second city.
     * Navigates to map and verifies city details contain the city name.
     * @throws InterruptedException If sleep is interrupted.
     */
    @Test
    public void testLocationFeatureForCity2() throws InterruptedException {
        onView(withId(R.id.cityRecyclerView))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(equalToIgnoringCase(DISPLAYED_CITY_2))),
                        clickChildView(R.id.mapButton)));
        Thread.sleep(5000);

        onView(withId(R.id.cityDetailsView))
                .check(matches(allOf(
                        isDisplayed(),
                        withText(containsString("champaign")),
                        withText(containsString("40.0°N")),
                        withText(containsString("-89.0°W"))
                )));

    }

    /**
     * Mock location test that overrides the coordinates for Chicago with
     * hardcoded Champaign coordinates. When opening the map for the
     * "Chicago" city entry, the map should actually display Champaign
     * (both name and coordinates).
     * @throws InterruptedException If sleep is interrupted.
     */
    @Test
    public void testMockLocation() throws InterruptedException {
        Thread.sleep(1000);

        onView(withId(R.id.cityRecyclerView))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(DISPLAYED_CITY_2)),
                        clickChildView(R.id.deleteCityButton)));
        Thread.sleep(1000);

        // Inject Champaign coordinates
        MapActivity.setTestCoordinates(40.1106, -88.2073);
        // Inject Champaign name as displayed city name
        MapActivity.setTestCityName("Champaign");

        // Open map for Chicago
        onView(withId(R.id.cityRecyclerView))
                .perform(RecyclerViewActions.actionOnItem(
                        hasDescendant(withText(equalToIgnoringCase(DISPLAYED_CITY_1))),
                        clickChildView(R.id.mapButton)));
        Thread.sleep(5500);

        onView(withId(R.id.cityDetailsView))
                .check(matches(allOf(
                        isDisplayed(),
                        // Name overridden to Champaign
                        withText(containsString("Champaign")),
                        withText(containsString("40.0°N")),
                        withText(containsString("-89"))
                )));

        // Clear overrides so other tests are unaffected
        MapActivity.setTestCoordinates(null, null);
        MapActivity.setTestCityName(null);
    }
}
