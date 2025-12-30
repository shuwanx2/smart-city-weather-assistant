package com.example.group316weatherappproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// Adapter class for displaying a list of cities in a RecyclerView with delete functionality
public class CityAdapter extends RecyclerView.Adapter<CityAdapter.ViewHolder> {

    // Callback interface for delete action
    // Interface for handling city deletion events
    public interface OnDeleteClickListener {
        // Method called when a city should be deleted
        void onDelete(City city);
    }

    // Interface for handling weather button click events
    public interface OnWeatherClickListener {
        // Method called when the weather button is clicked for a city
        void onWeatherClick(City city);
    }

    // Interface for handling map button click events
    public interface OnMapClickListener {
        // Method that is called when the map button is clicked for a city
        void onMapClick(City city);
    }

    private final List<City> cityList;
    private final OnDeleteClickListener deleteListener;
    private final OnWeatherClickListener weatherListener;
    private final OnMapClickListener mapListener;

    // Constructor to initialize the adapter with a city list and delete listener
    public CityAdapter(List<City> cityList, OnDeleteClickListener deleteListener) {
        this.cityList = cityList;
        this.deleteListener = deleteListener;
        this.weatherListener = null;
        this.mapListener = null;
    }

    // Constructor to initialize the adapter with city list, delete listener, and weather listener
    public CityAdapter(List<City> cityList, OnDeleteClickListener deleteListener, OnWeatherClickListener weatherListener) {
        this.cityList = cityList;
        this.deleteListener = deleteListener;
        this.weatherListener = weatherListener;
        this.mapListener = null;
    }

    // Constructor to initialize the adapter with all listeners
    public CityAdapter(List<City> cityList, OnDeleteClickListener deleteListener, OnWeatherClickListener weatherListener, OnMapClickListener mapListener) {
        this.cityList = cityList;
        this.deleteListener = deleteListener;
        this.weatherListener = weatherListener;
        this.mapListener = mapListener;
    }

    // ViewHolder class to hold references to the views for each city item
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView cityName;
        Button deleteButton;
        Button weatherButton;
        Button mapButton;

        // Constructor to initialize the ViewHolder with item view references
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cityName = itemView.findViewById(R.id.cityNameText);
            deleteButton = itemView.findViewById(R.id.deleteCityButton);
            weatherButton = itemView.findViewById(R.id.weatherButton);
            mapButton = itemView.findViewById(R.id.mapButton);
        }
    }

    // Creates and returns a new ViewHolder by inflating the city item layout
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_city, parent, false);
        return new ViewHolder(view);
    }

    // Binds city data to the ViewHolder and sets up the delete button with theme styling
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        City city = cityList.get(position);
        // Display the city name with proper capitalization
        holder.cityName.setText(city.getDisplayName());
        holder.deleteButton.setOnClickListener(v -> deleteListener.onDelete(city));
        
        // Set up weather button click listener
        if (weatherListener != null) {
            holder.weatherButton.setOnClickListener(v -> weatherListener.onWeatherClick(city));
        }
        
        //  map button click listener
        if (mapListener != null) {
            holder.mapButton.setOnClickListener(v -> mapListener.onMapClick(city));
        }
        
        // Apply accent color to buttons
        Theme currentTheme = ThemeManager.loadTheme(holder.itemView.getContext());
        if (currentTheme != null && currentTheme.accent != null) {
            try {
                int accentColor = android.graphics.Color.parseColor(currentTheme.accent);
                holder.deleteButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
                holder.weatherButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
                holder.mapButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
            } catch (Exception e) {
                // If parsing fails, keep default color
            }
        }
    }

    // Returns the total number of cities in the list
    @Override
    public int getItemCount() {
        return cityList.size();
    }
}
