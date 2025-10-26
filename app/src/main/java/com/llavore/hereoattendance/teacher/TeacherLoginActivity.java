package com.llavore.hereoattendance.teacher;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.utils.SessionManager;

public class TeacherLoginActivity extends AppCompatActivity {

    // Admin credentials - exclusive for admin access
    private static final String ADMIN_EMAIL = "Admin";
    private static final String ADMIN_PASSWORD = "Password123";

    private TextView switchRoles;
    private TextInputEditText txtEmail, txtPassword;

    private MaterialButton btnLogin;
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;
    private DatabaseReference mDatabase;


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
        mDatabase = FirebaseDatabase.getInstance().getReference();
        sessionManager = new SessionManager(this);

        switchRoles = findViewById(R.id.roleSwitch);
        txtEmail = findViewById(R.id.txtEmail);
        txtPassword = findViewById(R.id.txtPassword);
        btnLogin = findViewById(R.id.btnLogin);

        switchRoles.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            String email = txtEmail.getText().toString().trim();
            String password = txtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            } else if(email.equals(ADMIN_EMAIL) && password.equals(ADMIN_PASSWORD)) {
                Toast.makeText(this, "You are now logged in as Admin", Toast.LENGTH_SHORT).show();
                Intent goToAdmin = new Intent(TeacherLoginActivity.this, AdminDashboard.class);
                startActivity(goToAdmin);
                finish();
                return; // Exit early to prevent attempting Firebase login
            }

            // Attempt login with teacher role guard
            attemptLogin(email, password);
        });
    }
    
    private void attemptLogin(String email, String password) {
        // Show loading state
        btnLogin.setEnabled(false);
        btnLogin.setText("Checking...");

        // Sign in first, then verify teacher role under users/teachers/{uid}
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
                        if (userId == null) {
                            handleFailedLogin();
                            return;
                        }

                        mDatabase.child("users").child("teachers").child(userId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            sessionManager.setLogin(true, userId, "teacher");
                                            Toast.makeText(TeacherLoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(TeacherLoginActivity.this, TeacherDashboard.class));
                                            finish();
                                        } else {
                                            // Not a teacher, sign out and inform user
                                            mAuth.signOut();
                                            btnLogin.setEnabled(true);
                                            btnLogin.setText("Login");
                                            Toast.makeText(TeacherLoginActivity.this, "This account is not a teacher. Please use a Teacher Account.", Toast.LENGTH_LONG).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        handleFailedLogin();
                                    }
                                });
                    } else {
                        handleFailedLogin();
                    }
                })
                .addOnFailureListener(e -> handleFailedLogin());
    }
    
    private void handleFailedLogin() {
        // Reset button state
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");

        // Show error message
        Toast.makeText(this, "Login Failed. Please check your credentials.", Toast.LENGTH_LONG).show();
    }
    

}
