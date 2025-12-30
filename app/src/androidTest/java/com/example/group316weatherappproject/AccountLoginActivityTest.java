package com.example.group316weatherappproject;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

/**
 * Instrumented UI tests for LoginActivity.
 * These tests simulate login attempts with different inputs and assert
 * whether the app remains on the login screen or navigates to the home
 * screen, validating correct authentication behavior.
 */

@RunWith(AndroidJUnit4.class)
public class AccountLoginActivityTest {

    /**
     * Launches LoginActivity before each test,
     * providing an isolated ActivityScenario environment.
     */
    @Rule
    public ActivityScenarioRule<LoginActivity> activityScenarioRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    private DatabaseHelper databaseHelper;
    private Context context;

    /**
     * Ensures a clean test state before each run by clearing any session
     * data and inserting a known test user. This guarantees consistent
     * and repeatable login results.
     */
    @Before
    public void setUp() {
        // Get the application context

        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(context);

        // Clear session to ensure we start logged out
        SessionManager sessionManager = new SessionManager(context);
        sessionManager.logoutUser();  // Changed from logout() to logoutUser()

        databaseHelper.getWritableDatabase().execSQL("DELETE FROM users WHERE username = 'testuser'");

        // Insert a valid test user into the database
        boolean registered = databaseHelper.registerUser("testuser", "password123");

        // Verify registration was successful
        if (!registered) {
            throw new RuntimeException("Failed to register test user in setUp()");
        }
    }

    /**
     * Removes the test user and clears the session, preventing data from
     * carrying over into subsequent tests.
     */
    @After
    public void tearDown() {
        // Clean up test data
        if (databaseHelper != null) {
            databaseHelper.getWritableDatabase().execSQL("DELETE FROM users WHERE username = 'testuser'");
            databaseHelper.close();
        }

        // Clear session after test
        SessionManager sessionManager = new SessionManager(context);
        sessionManager.logoutUser();  // Changed from logout() to logoutUser()
    }

    /**
     * Verifies that a user can log in successfully using valid credentials.
     */
    @Test
    public void testAttemptLogin_Successful() throws InterruptedException {
        // Type username
        onView(withId(R.id.editUsername))
                .perform(typeText("testuser"), closeSoftKeyboard());

        // Type password
        onView(withId(R.id.editPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Click login button
        onView(withId(R.id.LoginButton))
                .perform(click());

        // Wait for 2 seconds for video recording visibility
        Thread.sleep(2000);

        onView(withId(R.id.titleText)).check(matches(withText("Team 316 - " + "testuser")));

    }

    /**
     * Ensures login fails when an incorrect password is entered.
     * The button remains visible, indicating no navigation occurred.
     */
    @Test
    public void testAttemptLogin_Failure() throws InterruptedException {
        // Type username
        onView(withId(R.id.editUsername))
                .perform(typeText("testuser"), closeSoftKeyboard());

        // Type incorrect password
        onView(withId(R.id.editPassword))
                .perform(typeText("wrongpass"), closeSoftKeyboard());

        // Click login button
        onView(withId(R.id.LoginButton))
                .perform(click());

        // Wait for 2 seconds for video recording visibility
        Thread.sleep(2000);

        // Verify failure toast is displayed
        onView(withId(R.id.LoginButton))
                .check(matches(isDisplayed()));
    }

    /**
     * Verifies login validation logic when the username field is empty.
     * The login should not proceed and the user should remain on the page.
     */
    @Test
    public void testAttemptLogin_EmptyUsername() throws InterruptedException {
        // Leave username empty, type password
        onView(withId(R.id.editPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Click login button
        onView(withId(R.id.LoginButton))
                .perform(click());

        // Wait for 2 seconds for video recording visibility
        Thread.sleep(4000);

        onView(withId(R.id.LoginButton))
                .check(matches(isDisplayed()));
        Thread.sleep(1000);

    }

    /**
     * Verifies login validation logic when the password field is empty.
     */
    @Test
    public void testAttemptLogin_EmptyPassword() throws InterruptedException {
        // Type username, leave password empty
        onView(withId(R.id.editUsername))
                .perform(typeText("testuser"), closeSoftKeyboard());

        // Click login button
        onView(withId(R.id.LoginButton))
                .perform(click());

        // Wait for 2 seconds for video recording visibility
        Thread.sleep(2000);

        onView(withId(R.id.LoginButton))
                .check(matches(isDisplayed()));
    }
}
