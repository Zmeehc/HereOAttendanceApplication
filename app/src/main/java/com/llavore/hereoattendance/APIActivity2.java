package com.llavore.hereoattendance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.llavore.hereoattendance.teacher.MainActivity;
import com.llavore.hereoattendance.teacher.TeacherDashboard;
import com.llavore.hereoattendance.teacher.TeacherSignUpActivity;
import com.llavore.hereoattendance.utils.LoginSecurityManager;
import com.llavore.hereoattendance.utils.SessionManager;

public class APIActivity2 extends AppCompatActivity {

    // UI Elements
    private TextView roleSwitch, txtSignUp;
    private TextInputEditText txtEmail, txtPassword;
    private MaterialButton btnLogin;
    
    // Security and session management
    private LoginSecurityManager securityManager;
    private SessionManager sessionManager;
    
    // Login attempt counter
    private int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_apiactivity2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements
        initializeViews();
        
        // Initialize managers
        sessionManager = new SessionManager(this);
        
        // Set up click listeners
        setupClickListeners();
    }
    
    private void initializeViews() {
        roleSwitch = findViewById(R.id.roleSwitch);
        txtSignUp = findViewById(R.id.txtSignUp);
        txtEmail = findViewById(R.id.txtEmail);
        txtPassword = findViewById(R.id.txtPassword);
        btnLogin = findViewById(R.id.btnLogin);
    }
    
    private void setupClickListeners() {
        // Role switch (go back to main activity)
        roleSwitch.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // Sign up
        txtSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, TeacherSignUpActivity.class));
        });

        // Login button
        btnLogin.setOnClickListener(v -> {
            String email = txtEmail.getText().toString().trim();
            String password = txtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Initialize security manager for this email
            securityManager = new LoginSecurityManager(this, email);

            // Attempt login
            attemptLogin(email, password);
        });
    }
    
    private void attemptLogin(String email, String password) {
        // Show loading state
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");
        
        // Simple credential check (replace with your actual authentication logic)
        if (checkCredentials(email, password)) {
            // Login successful
            handleSuccessfulLogin(email);
        } else {
            // Login failed
            handleFailedLogin(email);
        }
    }
    
    private boolean checkCredentials(String email, String password) {
        // Replace this with your actual authentication logic
        // For now, using simple hardcoded check
        String correctEmail = "Admin";
        String correctPassword = "Password";
        
        return email.equals(correctEmail) && password.equals(correctPassword);
    }
    
    private void handleSuccessfulLogin(String email) {
        // Reset counter
        if (securityManager != null) {
            securityManager.recordSuccessfulAttempt();
        }
        counter = 0;
        
        // Reset button state
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");
        
        // Show success message
        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
        
        // Navigate to next activity (replace with your target activity)
        // For now, navigate to APIActivity as a placeholder
        startActivity(new Intent(this, TeacherDashboard.class));
        finish();
    }
    
    private void handleFailedLogin(String email) {
        // Increment counter
        counter++;
        if (securityManager != null) {
            securityManager.recordFailedAttempt();
        }
        
        // Reset button state
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");
        
        // Show error message
        Toast.makeText(this, "Login Failed. Please check your credentials.", Toast.LENGTH_LONG).show();
        
        // Check if we should send SMS alert (after 3 failed attempts)
        if (counter >= 3) {
            sendSecurityAlert();
            Toast.makeText(this, "Security alert has been sent", Toast.LENGTH_LONG).show();
            resetLoginAttemptsManually();
        } else {
            int remainingAttempts = 3 - counter;
            Toast.makeText(this, "Login failed. " + remainingAttempts + 
                " attempts remaining before security alert.", Toast.LENGTH_LONG).show();

        }
    }
    
    private void sendSecurityAlert() {
        try {
            // Send SMS alert
            SemaphoreSmsSender.sendSMS(
                "+639663996287", // Replace with actual user's phone number
                "Someone is trying to login to your account"
            );
            
            // Log the alert
            android.util.Log.d("APIActivity2", "Security alert SMS sent to +639663996287");
            
        } catch (Exception e) {
            android.util.Log.e("APIActivity2", "Failed to send security alert SMS", e);
            Toast.makeText(this, "Failed to send security alert. Please contact support.", Toast.LENGTH_LONG).show();
        }
    }
    

    public void resetLoginAttemptsManually() {
        if (securityManager != null) {
            securityManager.resetAllData();
        }
        counter = 0;
        Toast.makeText(this, "Login counter have been reset", Toast.LENGTH_SHORT).show();
    }
}