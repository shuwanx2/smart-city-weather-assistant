package com.example.group316weatherappproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.group316weatherappproject.database.User;

import org.mindrot.jbcrypt.BCrypt;

// Helper class for managing the SQLite database
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "WeatherApp.db";
    private static final int DATABASE_VERSION = 4;

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD_HASH = "password_hash";
    private static final String COLUMN_THEME_COLOR = "theme_color";
    private static final String COLUMN_TEXT_COLOR = "text_color";

    // Cities table
    private static final String TABLE_CITIES = "cities";
    private static final String COLUMN_CITY_ID = "city_id";
    private static final String COLUMN_CITY_NAME = "city_name";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";

    // Default colors
    private static final String DEFAULT_THEME_COLOR = "#6750a5";
    private static final String DEFAULT_TEXT_COLOR = "#000000";

    // Constructs the database helper for the WeatherApp
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create the database tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USERNAME + " TEXT UNIQUE NOT NULL,"
                + COLUMN_PASSWORD_HASH + " TEXT NOT NULL,"
                + COLUMN_THEME_COLOR + " TEXT NOT NULL,"
                + COLUMN_TEXT_COLOR + " TEXT NOT NULL"
                + ")";
        db.execSQL(CREATE_USERS_TABLE);

        String CREATE_CITIES_TABLE = "CREATE TABLE " + TABLE_CITIES + "("
                + COLUMN_CITY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_CITY_NAME + " TEXT NOT NULL,"
                + COLUMN_USER_ID + " INTEGER NOT NULL,"
                + COLUMN_LATITUDE + " REAL NOT NULL DEFAULT 0.0,"
                + COLUMN_LONGITUDE + " REAL NOT NULL DEFAULT 0.0,"
                + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + "),"
                + "UNIQUE(" + COLUMN_USER_ID + ", " + COLUMN_CITY_NAME + ")"
                + ")";
        db.execSQL(CREATE_CITIES_TABLE);
    }

    // Upgrade the database schema
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Create cities table if upgrading from version 2 or lower
            String CREATE_CITIES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CITIES + "("
                    + COLUMN_CITY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_CITY_NAME + " TEXT NOT NULL,"
                    + COLUMN_USER_ID + " INTEGER NOT NULL,"
                    + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_ID + "),"
                    + "UNIQUE(" + COLUMN_USER_ID + ", " + COLUMN_CITY_NAME + ")"
                    + ")";
            db.execSQL(CREATE_CITIES_TABLE);
        }
        if (oldVersion < 4) {
            // Add latitude and longitude columns if upgrading from version 3 or lower
            db.execSQL("ALTER TABLE " + TABLE_CITIES + " ADD COLUMN " + COLUMN_LATITUDE + " REAL NOT NULL DEFAULT 0.0");
            db.execSQL("ALTER TABLE " + TABLE_CITIES + " ADD COLUMN " + COLUMN_LONGITUDE + " REAL NOT NULL DEFAULT 0.0");
        }
    }

    /**
     * Register a new user
     * @param username the username
     * @param password the plain text password
     * @return true if registration successful, false if username already exists
     */
    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Lowercase the username for consistency
        username = username.toLowerCase();

        Log.d(TAG, "registerUser start for: " + username);

        // Check if username already exists
        if (usernameExists(username)) {
            Log.d(TAG, "registerUser: username exists -> " + username);
            return false;
        }

        // Hash the password using BCrypt
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD_HASH, passwordHash);
        values.put(COLUMN_THEME_COLOR, DEFAULT_THEME_COLOR);
        values.put(COLUMN_TEXT_COLOR, DEFAULT_TEXT_COLOR);

        long result = db.insert(TABLE_USERS, null, values);
        boolean ok = result != -1;
        Log.d(TAG, "registerUser finished for: " + username + " result=" + result + " success=" + ok);
        return ok;
    }

    /**
     * Authenticate a user
     * @param username the username
     * @param password the plain text password
     * @return User object if authentication successful, null otherwise
     */
    public User authenticateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Lowercase the username for consistency
        username = username.toLowerCase();

        String[] columns = {COLUMN_ID, COLUMN_USERNAME, COLUMN_PASSWORD_HASH, COLUMN_THEME_COLOR, COLUMN_TEXT_COLOR};
        String selection = COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
            String dbUsername = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME));
            String passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH));
            String themeColor = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_THEME_COLOR));
            String textColor = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT_COLOR));

            // Verify password using BCrypt
            if (BCrypt.checkpw(password, passwordHash)) {
                user = new User(id, dbUsername, passwordHash, themeColor, textColor);
            }
        }

        cursor.close();
        return user;
    }

    /**
     * Check if username already exists
     */
    private boolean usernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Lowercase the username for consistency
        username = username.toLowerCase();
        String[] columns = {COLUMN_ID};
        String selection = COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();

        return exists;
    }

    /**
     * Update user's theme color
     */
    public boolean updateThemeColor(String username, String themeColor) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Lowercase the username for consistency
        username = username.toLowerCase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_THEME_COLOR, themeColor);

        String selection = COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};

        int rowsAffected = db.update(TABLE_USERS, values, selection, selectionArgs);
        Log.d(TAG, "updateThemeColor for " + username + " set " + themeColor + " rowsAffected=" + rowsAffected);
        return rowsAffected > 0;
    }

    /**
     * Update user's text color
     */
    public boolean updateTextColor(String username, String textColor) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Lowercase the username for consistency
        username = username.toLowerCase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TEXT_COLOR, textColor);

        String selection = COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};

        int rowsAffected = db.update(TABLE_USERS, values, selection, selectionArgs);
        Log.d(TAG, "updateTextColor for " + username + " set " + textColor + " rowsAffected=" + rowsAffected);
        return rowsAffected > 0;
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Lowercase the username for consistency
        username = username.toLowerCase();

        String[] columns = {COLUMN_ID, COLUMN_USERNAME, COLUMN_PASSWORD_HASH, COLUMN_THEME_COLOR, COLUMN_TEXT_COLOR};
        String selection = COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
            String dbUsername = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME));
            String passwordHash = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH));
            String themeColor = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_THEME_COLOR));
            String textColor = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEXT_COLOR));

            user = new User(id, dbUsername, passwordHash, themeColor, textColor);
        }

        cursor.close();
        return user;
    }

    /**
     * Add a city for a user
     * @return true if added successfully, false if user not found or city already exists
     */
    public boolean addCity(String username, String cityName, double latitude, double longitude) {
        SQLiteDatabase db = this.getWritableDatabase();
        username = username.toLowerCase();
        // Normalize city name to lowercase for consistent storage and duplicate checking
        cityName = cityName.toLowerCase();

        // Get user ID
        User user = getUserByUsername(username);
        if (user == null) {
            Log.w(TAG, "addCity: user not found -> " + username);
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_CITY_NAME, cityName);
        values.put(COLUMN_USER_ID, user.getId());
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE, longitude);

        try {
            long result = db.insert(TABLE_CITIES, null, values);
            boolean ok = result != -1;
            Log.d(TAG, "addCity for user " + username + " city " + cityName + " at (" + latitude + ", " + longitude + ") result=" + result + " success=" + ok);
            return ok;
        } catch (android.database.sqlite.SQLiteConstraintException e) {
            // This happens when trying to add a duplicate city (UNIQUE constraint violation)
            Log.w(TAG, "addCity: duplicate city for user " + username + " city " + cityName);
            return false;
        }
    }

    /**
     * Get all cities for a user
     */
    public java.util.List<City> getCitiesForUser(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        username = username.toLowerCase();
        java.util.List<City> cities = new java.util.ArrayList<>();

        // Get user ID
        User user = getUserByUsername(username);
        if (user == null) {
            Log.w(TAG, "getCitiesForUser: user not found -> " + username);
            return cities;
        }

        String[] columns = {COLUMN_CITY_NAME, COLUMN_LATITUDE, COLUMN_LONGITUDE};
        String selection = COLUMN_USER_ID + " = ?";
        String[] selectionArgs = {String.valueOf(user.getId())};

        Cursor cursor = db.query(TABLE_CITIES, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                String cityName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CITY_NAME));
                double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE));
                double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE));
                cities.add(new City(cityName, latitude, longitude));
            } while (cursor.moveToNext());
        }

        cursor.close();
        Log.d(TAG, "getCitiesForUser " + username + " found " + cities.size() + " cities");
        return cities;
    }

    /**
     * Delete a city for a user
     */
    public boolean deleteCity(String username, String cityName) {
        SQLiteDatabase db = this.getWritableDatabase();
        username = username.toLowerCase();
        // Normalize city name to lowercase for consistent lookup
        cityName = cityName.toLowerCase();

        // Get user ID
        User user = getUserByUsername(username);
        if (user == null) {
            Log.w(TAG, "deleteCity: user not found -> " + username);
            return false;
        }

        String selection = COLUMN_USER_ID + " = ? AND " + COLUMN_CITY_NAME + " = ?";
        String[] selectionArgs = {String.valueOf(user.getId()), cityName};

        int rowsDeleted = db.delete(TABLE_CITIES, selection, selectionArgs);
        boolean ok = rowsDeleted > 0;
        Log.d(TAG, "deleteCity for user " + username + " city " + cityName + " rowsDeleted=" + rowsDeleted + " success=" + ok);
        return ok;
    }

    /**
     * Delete a user by username
     * @return true if deleted, false if not found
     */
    public boolean deleteUser(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        username = username.toLowerCase();
        String selection = COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};
        int rowsDeleted = db.delete(TABLE_USERS, selection, selectionArgs);
        return rowsDeleted > 0;
    }
}