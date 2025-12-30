package com.example.group316weatherappproject;

/**
 * This class represents a color theme with background, text, and accent colors.
 */
public class Theme {
    public String background; 
    public String text;       
    public String accent;     // button color

    /**
     * Default constructor for Theme.
     */
    public Theme() {}

    /**
     * Constructor that sets background, text, and accent colors.
     * background is a Hex string for background color.
     * text is a Hex string for text color.
     * accent is a Hex string for accent color.
     */
    public Theme(String background, String text, String accent) {
        this.background = background;
        this.text = text;
        this.accent = accent;
    }

    /**
     * Returns a default theme
     * Theme is an object with default colors.
     */
    public static Theme defaultTheme() {
        return new Theme("#FFFFFF", "#222222", "#a5bbce");
    }
}
