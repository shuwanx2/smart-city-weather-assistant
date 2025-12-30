package com.example.group316weatherappproject.database;

// User model class representing a user in the database
public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String themeColor;
    private String textColor;

    public User() {
    }

    // Parameterized constructor
    public User(int id, String username, String passwordHash, String themeColor, String textColor) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.themeColor = themeColor;
        this.textColor = textColor;
    }

    // Get id
    public int getId() {
        return id;
    }

    // Set id
    public void setId(int id) {
        this.id = id;
    }

    // Get username
    public String getUsername() {
        return username;
    }

    // Set username     
    public void setUsername(String username) {
        this.username = username;
    }

    // Get password hash
    public String getPasswordHash() {
        return passwordHash;
    }

    // Set password hash
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // Get theme color
    public String getThemeColor() {
        return themeColor;
    }

    // Set theme color
    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }

    // Get text color
    public String getTextColor() {
        return textColor;
    }

    // Set text color
    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }
}
