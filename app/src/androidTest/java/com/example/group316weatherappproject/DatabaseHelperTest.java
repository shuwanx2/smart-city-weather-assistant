package com.example.group316weatherappproject;

import com.example.group316weatherappproject.database.User;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for DatabaseHelper that validate user registration,
 * authentication logic, and data consistency in the app database.
 * These tests interact directly with the database layer to verify correct
 * behavior for user creation, case-insensitive usernames, duplicate handling,
 * password validation, hashing, and default theme assignment. Assertions confirm
 * that database operations produce the expected outcomes and reject invalid ones.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseHelperTest {

    private DatabaseHelper databaseHelper;
    private Context context;

    /**
     * Sets up a fresh DatabaseHelper instance before each test
     * and removes any leftover accounts to ensure deterministic,
     * isolated test results.
     */
    @Before
    public void setUp() {
        // Get the application context
        context = ApplicationProvider.getApplicationContext();

        // Initialize DatabaseHelper
        databaseHelper = new DatabaseHelper(context);

        // Clean up any existing test data before each test
        cleanupTestUsers();
    }

    /**
     * Cleans test data and closes the database connection after each test.
     * Prevents test interference and ensures the in-app database remains clean.
     */
    @After
    public void tearDown() {
        // Clean up test data after each test
        cleanupTestUsers();

        // Close the database
        if (databaseHelper != null) {
            databaseHelper.getWritableDatabase().close();
            databaseHelper.close();
        }
    }

    /**
     * Deletes all usernames used in these tests to avoid conflicts
     * between test executions.
     */
    private void cleanupTestUsers() {
        if (databaseHelper != null) {
            databaseHelper.getWritableDatabase().execSQL(
                    "DELETE FROM users WHERE username IN (?, ?, ?, ?)",
                    new String[]{"dbtestuser", "duplicateuser", "casetest", "normaluser"}
            );
        }
    }

    /**
     * Verifies that a new user can be registered and authenticated successfully,
     * while authentication with an incorrect password correctly fails.
     */
    @Test
    public void testRegisterAndAuthenticate_Success() {
        // Register a new user
        boolean registerResult = databaseHelper.registerUser("dbTestUser", "securePass");
        assertTrue("Registration should succeed for new user", registerResult);

        // Authenticate with correct credentials
        User authenticatedUser = databaseHelper.authenticateUser("dbTestUser", "securePass");
        assertNotNull("Authentication should succeed with correct password", authenticatedUser);
        assertEquals("Username should match (lowercase)", "dbtestuser", authenticatedUser.getUsername());
        assertNotNull("User should have a theme color", authenticatedUser.getThemeColor());
        assertNotNull("User should have a text color", authenticatedUser.getTextColor());

        // Authenticate with wrong password
        User failedAuth = databaseHelper.authenticateUser("dbTestUser", "WRONGpass");
        assertNull("Authentication should fail with incorrect password", failedAuth);
    }

    /**
     * Ensures that registering the same username twice is not allowed.
     */
    @Test
    public void testDuplicateRegistration_Fails() {
        // Register user first time
        boolean firstRegistration = databaseHelper.registerUser("duplicateUser", "pass");
        assertTrue("First registration should succeed", firstRegistration);

        // Attempt to register same user again
        boolean secondRegistration = databaseHelper.registerUser("duplicateUser", "pass");
        assertFalse("Second registration with same username should fail", secondRegistration);
    }

    /**
     * Tests that usernames are stored and compared in a case-insensitive manner.
     */
    @Test
    public void testRegisterUser_CaseInsensitive() {
        // Register user with mixed case username
        boolean registerResult = databaseHelper.registerUser("CaseTest", "password");
        assertTrue("Registration should succeed", registerResult);

        // Verify username is stored in lowercase
        User user = databaseHelper.authenticateUser("casetest", "password");
        assertNotNull("Should authenticate with lowercase username", user);
        assertEquals("Username should be stored in lowercase", "casetest", user.getUsername());

        // Verify authentication works with different cases
        User userMixedCase = databaseHelper.authenticateUser("CaseTEST", "password");
        assertNotNull("Should authenticate with mixed case username", userMixedCase);
    }

    /**
     * Validates that case-insensitive duplicate detection prevents registering
     * usernames that differ only by letter casing.
     */
    @Test
    public void testRegisterUser_DuplicateCaseInsensitive() {
        // Register user with lowercase username
        boolean firstReg = databaseHelper.registerUser("normaluser", "pass123");
        assertTrue("First registration should succeed", firstReg);

        // Try to register same username with different case
        boolean secondReg = databaseHelper.registerUser("NormalUser", "pass456");
        assertFalse("Registration should fail for username with different case", secondReg);

        // Verify original user still authenticates correctly
        User user = databaseHelper.authenticateUser("normaluser", "pass123");
        assertNotNull("Original user should still authenticate", user);
    }

    /**
     * Ensures authentication fails for users that do not exist.
     */
    @Test
    public void testAuthenticateUser_NonExistentUser() {
        // Try to authenticate a user that doesn't exist
        User user = databaseHelper.authenticateUser("nonexistent", "password");
        assertNull("Authentication should fail for non-existent user", user);
    }

    /**
     * Verifies that authentication fails when an existing user
     * attempts login with an empty password.
     */
    @Test
    public void testAuthenticateUser_EmptyPassword() {
        // Register user with a password
        boolean registered = databaseHelper.registerUser("dbTestUser", "securePass");
        assertTrue("Registration should succeed", registered);

        // Try to authenticate with empty password
        User user = databaseHelper.authenticateUser("dbTestUser", "");
        assertNull("Authentication should fail with empty password", user);
    }

    /**
     * Confirms that new users receive default theme and text colors
     * when none are provided at registration time.
     */
    @Test
    public void testRegisterUser_DefaultThemeColors() {
        // Register a new user
        boolean registered = databaseHelper.registerUser("dbTestUser", "password");
        assertTrue("Registration should succeed", registered);

        // Authenticate and check default theme colors
        User user = databaseHelper.authenticateUser("dbTestUser", "password");
        assertNotNull("User should be authenticated", user);
        assertEquals("Default theme color should be set", "#6750a5", user.getThemeColor());
        assertEquals("Default text color should be set", "#000000", user.getTextColor());
    }

    /**
     * Verifies that passwords are securely hashed and never stored as plaintext.
     */
    @Test
    public void testRegisterUser_PasswordHashing() {
        // Register a user
        boolean registered = databaseHelper.registerUser("dbTestUser", "myPassword123");
        assertTrue("Registration should succeed", registered);

        // Authenticate successfully
        User user = databaseHelper.authenticateUser("dbTestUser", "myPassword123");
        assertNotNull("User should authenticate with correct password", user);

        // Verify password hash is not the plain text password
        assertNotNull("Password hash should exist", user.getPasswordHash());
        assertFalse("Password hash should not equal plain text password",
                "myPassword123".equals(user.getPasswordHash()));
        assertTrue("Password hash should start with BCrypt prefix",
                user.getPasswordHash().startsWith("$2"));
    }

    /**
     * Confirms that the system supports multiple independent users
     * and validates passwords for each individually.
     */
    @Test
    public void testRegisterUser_MultipleUsers() {
        // Register multiple users
        assertTrue("First user registration should succeed",
                databaseHelper.registerUser("dbTestUser", "pass1"));
        assertTrue("Second user registration should succeed",
                databaseHelper.registerUser("duplicateUser", "pass2"));
        assertTrue("Third user registration should succeed",
                databaseHelper.registerUser("caseTest", "pass3"));

        // Verify all users can authenticate
        assertNotNull("First user should authenticate",
                databaseHelper.authenticateUser("dbTestUser", "pass1"));
        assertNotNull("Second user should authenticate",
                databaseHelper.authenticateUser("duplicateUser", "pass2"));
        assertNotNull("Third user should authenticate",
                databaseHelper.authenticateUser("caseTest", "pass3"));

        // Verify users don't authenticate with wrong passwords
        assertNull("First user should not authenticate with wrong password",
                databaseHelper.authenticateUser("dbTestUser", "wrongpass"));
        assertNull("Second user should not authenticate with wrong password",
                databaseHelper.authenticateUser("duplicateUser", "wrongpass"));
    }
}
