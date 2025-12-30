package com.example.group316weatherappproject;

/**
 * Enhanced Theme class that supports comprehensive LLM-generated theme specifications
 * Includes all required elements plus optional decorative elements
 */
public class EnhancedTheme {
    // Required elements
    public String background;     // Background color (hex format)
    public String text;          // Text color (hex format)

    // Highly encouraged personalized elements
    public String accent;        // Accent colors for buttons
    public String secondary;     // Secondary colors for cards or panels
    public String cardBackground; // Card/panel background
    public String border;        // Border colors
    public String headerText;    // Header/title colors
    public String buttonText;    // Button text color
    public String linkColor;     // Link/accent text color

    // Decorative elements
    public String emoji;         // Theme-related emoji
    public String icon;          // Theme-related icon description
    public String mood;          // Overall mood/atmosphere
    public String fontWeight;    // Typography weight (normal, bold, light)
    public String cornerRadius;  // UI element corner radius
    public String shadowColor;   // Shadow color for depth

    // Theme metadata
    public String themeName;     // Generated theme name
    public String description;   // Theme description

    // default constructor for the EnhancedTheme Class
    public EnhancedTheme() {}

    // Full constructor for all properties
    public EnhancedTheme(String background, String text, String accent, String secondary,
                         String cardBackground, String border, String headerText, String buttonText,
                         String linkColor, String emoji, String icon, String mood, String fontWeight,
                         String cornerRadius, String shadowColor, String themeName, String description) {
        this.background = background;
        this.text = text;
        this.accent = accent;
        this.secondary = secondary;
        this.cardBackground = cardBackground;
        this.border = border;
        this.headerText = headerText;
        this.buttonText = buttonText;
        this.linkColor = linkColor;
        this.emoji = emoji;
        this.icon = icon;
        this.mood = mood;
        this.fontWeight = fontWeight;
        this.cornerRadius = cornerRadius;
        this.shadowColor = shadowColor;
        this.themeName = themeName;
        this.description = description;
    }

    // Default theme as a fallback
    public static EnhancedTheme defaultTheme() {
        return new EnhancedTheme(
                "#FFFFFF", "#222222", "#6750A4", "#E3F2FD",
                "#F5F5F5", "#E0E0E0", "#1976D2", "#FFFFFF",
                "#1976D2", "🎨", "palette", "neutral", "normal",
                "8", "#9E9E9E", "Default Theme", "Clean and professional default theme"
        );
    }

    // Convert to basic Theme for backward compatibility
    public Theme toBasicTheme() {
        return new Theme(background, text, accent);
    }

    // Validation method
    public boolean isValid() {
        return background != null && !background.isEmpty() &&
                text != null && !text.isEmpty() &&
                background.startsWith("#") && text.startsWith("#");
    }

    // String representation of the theme
    @Override
    public String toString() {
        return "EnhancedTheme{" +
                "themeName='" + themeName + '\'' +
                ", background='" + background + '\'' +
                ", text='" + text + '\'' +
                ", accent='" + accent + '\'' +
                ", mood='" + mood + '\'' +
                ", emoji='" + emoji + '\'' +
                '}';
    }
}