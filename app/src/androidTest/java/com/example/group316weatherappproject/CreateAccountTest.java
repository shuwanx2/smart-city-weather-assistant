package com.example.group316weatherappproject;

import android.content.Context;

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
 * Instrumented UI tests for CreateAccountActivity.
 *
 * These tests simulate user input to validate different account creation
 * scenarios and assert whether the app behaves correctly. Each test
 * provides specific inputs, triggers the Create Account action, and
 * uses Espresso assertions to confirm that either a successful account
 * creation leads to navigation to the home screen or invalid input
 * triggers appropriate validation errors.
 */
@RunWith(AndroidJUnit4.class)
public class CreateAccountTest {
    /**
     * Launches CreateAccountActivity before each test, ensuring
     * a fresh UI state and isolation between executions.
     */
    @Rule
    public ActivityScenarioRule<CreateAccountActivity> activityScenarioRule =
            new ActivityScenarioRule<>(CreateAccountActivity.class);

    private DatabaseHelper databaseHelper;
    private Context context;
    private String testUsername;

    /**
     * Ensures a clean test environment by initializing the database helper
     * and generating a unique username for this run. This prevents tests
     * from interfering with each other or existing user records.
     */
    @Before
    public void setUp() {
        // Get the application context
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(context);

        // Generate unique username for this test run
        testUsername = "newuser_" + System.currentTimeMillis();
    }

    /**
     * Removes test artifacts after execution by deleting the user created
     * during the tests. This guarantees test repeatability and avoids
     * polluting the app database with leftover accounts.
     */
    @After
    public void tearDown() {
        // Clean up test data - delete the test user created during tests
        if (databaseHelper != null && testUsername != null) {
            databaseHelper.getWritableDatabase().execSQL(
                    "DELETE FROM users WHERE username = ?",
                    new String[]{testUsername.toLowerCase()}
            );
            databaseHelper.close();
        }
    }

    /**
     * Validates successful account creation when all fields are filled.
     *
     * The test enters a unique username, password, and theme, clicks
     * the Create Account button, waits for the account setup thread
     * to finish, and asserts that the user is navigated to HomeActivity
     * by checking the title text that reflects the logged-in username.
     */
    @Test
    public void testCreateAccount_Success() throws InterruptedException {
        // Enter unique username
        onView(withId(R.id.editUsername))
                .perform(typeText(testUsername), closeSoftKeyboard());

        // Enter valid password
        onView(withId(R.id.editPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Enter theme description
        onView(withId(R.id.theme))
                .perform(typeText("ocean blue"), closeSoftKeyboard());

        // Click create account button
        onView(withId(R.id.CreateAccountButton))
                .perform(click());

        // Wait for background thread to complete theme generation and runOnUiThread to execute
        Thread.sleep(7000);

        onView(withId(R.id.titleText)).check(matches(withText("Team 316 - " + testUsername)));


    }

    /**
     * Verifies successful account creation when the theme input is left empty.
     *
     * The app should apply a default theme and still allow account creation.
     * Success is confirmed by the presence of the home screen title containing
     * the generated username.
     */
    @Test
    public void testCreateAccount_Success_WithoutThemeInput() throws InterruptedException {
        // Enter unique username
        String uniqueUsername = "newuser2_" + System.currentTimeMillis();

        onView(withId(R.id.editUsername))
                .perform(typeText(uniqueUsername), closeSoftKeyboard());

        // Enter valid password
        onView(withId(R.id.editPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Leave theme input empty (default theme will be used)

        // Click create account button
        onView(withId(R.id.CreateAccountButton))
                .perform(click());

        // Wait for background thread to complete
        Thread.sleep(3000);

        onView(withId(R.id.titleText)).check(matches(withText("Team 316 - " + uniqueUsername)));


        // Clean up this additional test user
        databaseHelper.getWritableDatabase().execSQL(
                "DELETE FROM users WHERE username = ?",
                new String[]{uniqueUsername.toLowerCase()}
        );
    }

    /**
     * Ensures account creation fails when the username is empty.
     *
     * After attempting to create the account with a missing username,
     * the app should not proceed and must display a valid error message
     * on the username field, which this assertion verifies.
     */
    @Test
    public void testCreateAccount_ValidationFail_EmptyUsername() throws InterruptedException {
        // Leave username empty

        // Enter valid password
        onView(withId(R.id.editPassword))
                .perform(typeText("password123"), closeSoftKeyboard());

        // Enter theme description
        onView(withId(R.id.theme))
                .perform(typeText("ocean blue"), closeSoftKeyboard());

        // Click create account button
        onView(withId(R.id.CreateAccountButton))
                .perform(click());

        // Wait for visibility in video recording
        Thread.sleep(2000);

        // Verify error message on username field
        onView(withId(R.id.editUsername))
                .check(matches(ViewMatchers.hasErrorText("Username is required")));
    }

    /**
     * Ensures account creation fails when the password field is empty.
     *
     * The username and theme are provided, but the missing password should
     * trigger a validation error. The test asserts that the password input
     * displays the correct error text.
     */
    @Test
    public void testCreateAccount_ValidationFail_EmptyPassword() throws InterruptedException {
        // Enter username
        onView(withId(R.id.editUsername))
                .perform(typeText(testUsername), closeSoftKeyboard());

        // Leave password empty

        // Enter theme description
        onView(withId(R.id.theme))
                .perform(typeText("ocean blue"), closeSoftKeyboard());

        // Click create account button
        onView(withId(R.id.CreateAccountButton))
                .perform(click());

        // Wait for visibility in video recording
        Thread.sleep(2000);

        // Verify error message on password field
        onView(withId(R.id.editPassword))
                .check(matches(ViewMatchers.hasErrorText("Password is required")));
    }
}
