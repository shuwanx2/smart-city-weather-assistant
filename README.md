# Smart City Weather Assistant App

An Android application that integrates real-time weather data, geolocation services, and large language models (LLMs) to deliver personalized, location-aware weather insights, interactive maps, and AI-enhanced user experiences.

---

## Overview

The **Smart City Weather Assistant App** is a team-developed Android application designed to provide users with intelligent, context-aware weather information. By combining real-time environmental data, geospatial context, and large language models, the system enhances everyday decision-making while demonstrating modern **AI-for-Science** workflows in a user-facing application.

The application integrates environmental data ingestion, AI-driven reasoning, and rigorous software validation, reflecting the full lifecycle of designing, building, testing, and maintaining a data-driven intelligent system.

---

## Core Features

### 🔐 User Authentication & Personal Storage
- User sign-up, login, and logout
- Individual local storage per user
- Personalized user sessions and preferences

### 🌆 City Management
- User-specific city lists
- Add and delete cities dynamically
- Persistent storage of saved cities for each user

### 🌤️ Real-Time Weather Information
- Live weather data retrieved from external APIs
- Displays city name, date & time, temperature, humidity, wind conditions, and weather status
- Real-time updates based on selected city

### 🤖 LLM-Based Weather Insights (Q&A)
- Context-aware weather questions generated dynamically
- Users select questions to receive personalized answers
- Recommendations based on current weather conditions (e.g., outdoor activities, clothing suggestions)

### 🎨 LLM-Driven UI Theme Customization
- Users describe desired UI themes using natural language
- LLM generates structured UI theme specifications (colors, fonts, accents)
- Themes applied dynamically at runtime with robust fallback options

### 🖼️ Weather-Aware City Visualizations
- AI-generated city images reflecting:
  - City name
  - Current weather conditions
  - Time of day and atmospheric context
- Enhances visual understanding of environmental conditions

### 🗺️ Interactive Map Feature
- Embedded interactive maps for selected cities
- Supports zoom, drag, and navigation
- City coordinates passed dynamically to the map service

---

## AI & LLM Integration

Large language models (e.g., **Gemini**) are integrated into multiple components of the application:

### Weather Insights
The application generates weather-related questions and context-aware answers based on real-time environmental data, enabling personalized decision support.

### Natural-Language UI Customization
Users describe UI themes using free-form text (e.g., *"summer beach"* or *"cyberpunk nightscape"*). The LLM parses these descriptions into structured UI specifications that are applied dynamically.

### Weather-Aware City Views
The system generates realistic city images that reflect current weather conditions, time of day, and atmospheric context.

All LLM-generated content is produced dynamically at runtime. No questions, answers, themes, or images are hardcoded.

---

## Software Validation & Testing

A strong emphasis was placed on software reliability and validation. The application includes a comprehensive testing strategy:

- **Espresso instrumented tests**
Espresso is an Android framework for automated UI testing that simulates real user actions.

  - City addition and deletion
  - Weather display validation
  - Interactive map behavior
  - User logout functionality

- **LLM-generated tests**
  - Automated testing for user sign-up and login workflows

- **Mocked location testing**
  - Simulated location changes to validate location-based features

- **Code coverage analysis**
  - JaCoCo integration with HTML coverage reports
  - JaCoCo is a Java code coverage library that tracks which lines and branches of code are executed during tests and produces visual coverage reports.

This testing framework ensures correctness, reproducibility, and robustness in a system that integrates external APIs and AI components.

---

## System Design

The application follows a modular Android architecture with clear separation of concerns:

- **Activities** for authentication, city management, weather display, map view, and AI insights
- **Services** for weather retrieval, LLM communication, and data processing
- **Persistent storage** for user-specific data and preferences
- **Explicit component transitions** to manage navigation and application state

System design was informed by requirements engineering, use-case modeling, UML class diagrams, and component transition diagrams.

---

## Technologies Used

- **Platform:** Android
- **Languages:** Java, XML
- **APIs:** OpenWeatherMap API, Gemini LLM API, Google Maps Android SDK
- **Testing:** Espresso, JaCoCo
- **Tools:** Git, GitHub, Android Studio, VS Code

---

## Team Development

This project was developed collaboratively by a student team as part of **CS 427: Software Engineering** at the University of Illinois Urbana–Champaign. 
---

## Motivation

The **Smart City Weather Assistant App** demonstrates how **AI-for-Science techniques**—including environmental data integration, geospatial context, and large language models—can be combined with rigorous software engineering practices to build reliable, intelligent systems.

The project highlights how AI can enhance environmental information systems while maintaining reproducibility, testability, and software robustness.
