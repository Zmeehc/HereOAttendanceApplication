package com.llavore.hereoattendance.student;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.teacher.MainActivity;
import com.llavore.hereoattendance.teacher.TeacherLoginActivity;
import com.llavore.hereoattendance.utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class StudentLogin extends AppCompatActivity {
    private TextView switchRolesButton, studentSignupButton;
    private EditText studentEmail, studentPassword;
    private MaterialButton btnStudentLogin;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        sessionManager = new SessionManager(this);

        switchRolesButton = findViewById(R.id.studentRoleSwitch);
        studentSignupButton = findViewById(R.id.studentSignUp);
        studentEmail = findViewById(R.id.studentEmail);
        studentPassword = findViewById(R.id.studentPassword);
        btnStudentLogin = findViewById(R.id.btnStudentLogin);
        studentSignupButton.setOnClickListener(v -> {
            Intent intent = new Intent(StudentLogin.this, StudentSignup.class);
            startActivity(intent);
        });
        switchRolesButton.setOnClickListener(v -> {
            Intent intent = new Intent(StudentLogin.this, MainActivity.class);
            startActivity(intent);

        });

        btnStudentLogin.setOnClickListener(v -> {
            String email = studentEmail.getText() != null ? studentEmail.getText().toString().trim() : "";
            String password = studentPassword.getText() != null ? studentPassword.getText().toString().trim() : "";

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            btnStudentLogin.setEnabled(false);
            btnStudentLogin.setText("Signing in...");

            // Sign in first, then verify role under users/students/{uid}
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
                            if (uid == null) {
                                btnStudentLogin.setEnabled(true);
                                btnStudentLogin.setText("Login");
                                Toast.makeText(StudentLogin.this, "Login error: user not available", Toast.LENGTH_LONG).show();
                                return;
                            }

                            // Now allowed by rules: read only own node
                            mDatabase.child("users").child("students").child(uid)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot snapshot) {
                                            btnStudentLogin.setEnabled(true);
                                            btnStudentLogin.setText("Login");
                                            if (snapshot.exists()) {
                                                // Set login session with user ID and type
                                                sessionManager.setLogin(true, uid, "student");
                                                Toast.makeText(StudentLogin.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(StudentLogin.this, StudentHome.class));
                                                finish();
                                            } else {
                                                // Not a student; sign out and inform user
                                                mAuth.signOut();
                                                Toast.makeText(StudentLogin.this, "This account is not a student. Please use a Student Account.", Toast.LENGTH_LONG).show();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError error) {
                                            btnStudentLogin.setEnabled(true);
                                            btnStudentLogin.setText("Login");
                                            Toast.makeText(StudentLogin.this, "Check failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                        } else {
                            btnStudentLogin.setEnabled(true);
                            btnStudentLogin.setText("Login");
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Login failed";
                            Toast.makeText(StudentLogin.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        btnStudentLogin.setEnabled(true);
                        btnStudentLogin.setText("Login");
                        Toast.makeText(StudentLogin.this, "Login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}