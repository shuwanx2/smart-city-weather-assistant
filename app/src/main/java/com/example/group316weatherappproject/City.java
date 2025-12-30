package com.example.group316weatherappproject;

/**
 * Represents a city object added by the user.
 * Stores the city name AND optional latitude/longitude coordinates
 * for use with Google Maps and weather-related features.
 */
public class City {

    public String name;    // stored in lowercase
    public double lat;     // latitude
    public double lon;     // longitude

    /**
     * Constructor for a city WITHOUT coordinates (backwards compatible).
     * Coordinates can be set later when available.
     */
    public City(String name) {
        this.name = name;
        this.lat = 0.0;
        this.lon = 0.0;
    }

    /**
     * Constructor for a city WITH coordinates.
     */
    public City(String name, double lat, double lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * Helper: returns display-ready city name
     * Example: "new york" -> "New York"
     */
    public String getDisplayName() {
        if (name == null || name.isEmpty()) {
            return name;
        }

        String[] words = name.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }

        return result.toString();
    }
}
