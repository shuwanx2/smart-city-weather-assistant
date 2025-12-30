package com.example.group316weatherappproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.group316weatherappproject.database.User;

// Login activity that handles user authentication
public class LoginActivity extends AppCompatActivity {

    private EditText editUsername, editPassword;
    private Button loginButton;
    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    // onCreate method to initialize the activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize session manager and check if already logged in
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navigateToHome();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        // Get UI elements
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        loginButton = findViewById(R.id.LoginButton);

        // Set up login button click listener
        loginButton.setOnClickListener(v -> attemptLogin());

        // Set up click listener for "Create an account here" text
        TextView createAccountText = findViewById(R.id.createAccountText);
        createAccountText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, CreateAccountActivity.class);
            startActivity(intent);
        });
    }

    // Attempts to log in the user
    private void attemptLogin() {
        String username = editUsername.getText().toString().trim().toLowerCase();
        String password = editPassword.getText().toString().trim();

        // Validate input
        if (username.isEmpty()) {
            editUsername.setError("Username is required");
            editUsername.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editPassword.setError("Password is required");
            editPassword.requestFocus();
            return;
        }

        // Authenticate user
        User user = databaseHelper.authenticateUser(username, password);

        if (user != null) {
            // Persist user's theme so HomeActivity immediately uses it
            Theme current = ThemeManager.loadTheme(this);
            Theme toSave = new Theme(user.getThemeColor(), user.getTextColor(),
                    current != null ? current.accent : Theme.defaultTheme().accent);
            ThemeManager.saveTheme(this, toSave);

            // Login successful
            sessionManager.createLoginSession(user.getUsername(), user.getThemeColor(), user.getTextColor());
            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
            navigateToHome();
        } else {
            // Login failed
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        }
    }

    // Navigates to the home screen
    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}