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
import com.google.firebase.auth.FirebaseAuth;


import com.llavore.hereoattendance.utils.SessionManager;

public class TeacherLoginActivity extends AppCompatActivity {

    private TextView switchRoles, signUp;
    private TextInputEditText txtEmail, txtPassword;

    private MaterialButton btnLogin;
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        switchRoles = findViewById(R.id.roleSwitch);
        signUp = findViewById(R.id.txtSignUp);
        txtEmail = findViewById(R.id.txtEmail);
        txtPassword = findViewById(R.id.txtPassword);
        btnLogin = findViewById(R.id.btnLogin);

        switchRoles.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
        });

        signUp.setOnClickListener(v -> {
            startActivity(new Intent(this, TeacherSignUpActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            String email = txtEmail.getText().toString().trim();
            String password = txtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Attempt login
            attemptLogin(email, password);
        });
    }
    
    private void attemptLogin(String email, String password) {
        // Show loading state
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");
        
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Save session data
                        String userId = mAuth.getCurrentUser().getUid();
                        sessionManager.setLogin(true, userId, "teacher");
                        
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, APIActivity2.class));
                        finish();
                    } else {
                        // Login failed
                        handleFailedLogin();
                    }
                });
    }
    
    private void handleFailedLogin() {
        // Reset button state
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");

        // Show error message
        Toast.makeText(this, "Login Failed. Please check your credentials.", Toast.LENGTH_LONG).show();
    }
    

}
