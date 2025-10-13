package com.llavore.hereoattendance.teacher;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.TermsConditionsActivity;
import com.llavore.hereoattendance.utils.TransitionManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class TeacherSignUpActivity extends AppCompatActivity {

    private ImageView backToLogin;
    private TextView SignIn;

    // Form fields
    private TextInputEditText txtIDNo, firstNameSignup, middleNameSignUp, lastNameSignUp;
    private TextInputEditText emailSignUp, bdaySignUp, programSignUp, contactNumberSignUp, passwordSignUp, confirmPassSignUp;
    private AutoCompleteTextView genderSignUp;
    private CheckBox checkBox;
    private MaterialButton btnSignUp;
    private TextInputLayout bdayField;
    private TextInputLayout passwordField;
    private TextInputLayout confirmPassField;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // Gender dropdown options
    private String[] genderOptions = {"Male", "Female", "Prefer not to say"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initializeViews();
        setupClickListeners();
        setupGenderDropdown();
        setupDatePicker();
        setupClickableTermsText();
    }

    private void initializeViews() {
        backToLogin = findViewById(R.id.backArrow);
        SignIn = findViewById(R.id.txtSignIn);

        // Form fields
        txtIDNo = findViewById(R.id.txtIDNo);
        firstNameSignup = findViewById(R.id.firstNameSignup);
        middleNameSignUp = findViewById(R.id.middleNameSignUp);
        lastNameSignUp = findViewById(R.id.lastNameSignUp);
        emailSignUp = findViewById(R.id.emailSignUp);
        genderSignUp = findViewById(R.id.genderSignUp);
        bdaySignUp = findViewById(R.id.bdaySignUp);
        programSignUp = findViewById(R.id.programSignUp);
        contactNumberSignUp = findViewById(R.id.contactNumberSignUp);
        passwordSignUp = findViewById(R.id.passwordSignUp);
        confirmPassSignUp = findViewById(R.id.confirmPassSignUp);
        checkBox = findViewById(R.id.checkBox);
        btnSignUp = findViewById(R.id.btnSignUp);
        bdayField = findViewById(R.id.bdayField);
        passwordField = findViewById(R.id.passwordField);
        confirmPassField = findViewById(R.id.confirmPassField);
    }

    private void setupClickListeners() {
        backToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherSignUpActivity.this, TeacherLoginActivity.class);
            startActivity(intent);
            finish();
        });

        SignIn.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherSignUpActivity.this, TeacherLoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnSignUp.setOnClickListener(v -> {
            // Prevent accidental double taps while already processing
            if (!btnSignUp.isEnabled()) return;

            if (validateForm()) {
                // Hide keyboard to improve UX
                try {
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    View current = getCurrentFocus();
                    if (imm != null && current != null) {
                        imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
                    }
                } catch (Exception ignored) { }

                registerUser();
            }
        });
    }

    private void setupGenderDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, genderOptions);
        genderSignUp.setAdapter(adapter);

        // Prevent keyboard from showing up
        genderSignUp.setKeyListener(null);
        genderSignUp.setOnClickListener(v -> genderSignUp.showDropDown());
    }

    private void setupDatePicker() {
        bdayField.setEndIconOnClickListener(v -> showDatePicker());
        bdaySignUp.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = String.format("%02d/%02d/%d",
                            selectedMonth + 1, selectedDay, selectedYear);
                    bdaySignUp.setText(selectedDate);
                },
                year, month, day
        );

        // Set maximum date to today (user can't select future dates)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Clear previous errors on TextInputLayouts
        txtIDNo.setError(null);
        firstNameSignup.setError(null);
        lastNameSignUp.setError(null);
        emailSignUp.setError(null);
        genderSignUp.setError(null);
        bdaySignUp.setError(null);
        programSignUp.setError(null);
        contactNumberSignUp.setError(null);

        // Clear password errors properly
        passwordField.setError(null);
        passwordField.setErrorEnabled(false);
        confirmPassField.setError(null);
        confirmPassField.setErrorEnabled(false);

        // Validate ID Number
        if (TextUtils.isEmpty(txtIDNo.getText().toString().trim())) {
            txtIDNo.setError("ID Number is required");
            isValid = false;
        }

        // Validate First Name
        if (TextUtils.isEmpty(firstNameSignup.getText().toString().trim())) {
            firstNameSignup.setError("First Name is required");
            isValid = false;
        }

        // Validate Last Name
        if (TextUtils.isEmpty(lastNameSignUp.getText().toString().trim())) {
            lastNameSignUp.setError("Last Name is required");
            isValid = false;
        }

        // Validate Email
        String email = emailSignUp.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            emailSignUp.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailSignUp.setError("Please enter a valid email address");
            isValid = false;
        }

        // Validate Gender
        if (TextUtils.isEmpty(genderSignUp.getText().toString().trim())) {
            genderSignUp.setError("Please select your gender");
            isValid = false;
        }

        // Validate Birthdate
        if (TextUtils.isEmpty(bdaySignUp.getText().toString().trim())) {
            bdaySignUp.setError("Birthdate is required");
            isValid = false;
        }

        // Validate Program
        if (TextUtils.isEmpty(programSignUp.getText().toString().trim())) {
            programSignUp.setError("Program is required");
            isValid = false;
        }

        // Validate Contact Number
        String contactNumber = contactNumberSignUp.getText().toString().trim();
        if (TextUtils.isEmpty(contactNumber) || contactNumber.equals("+63") || contactNumber.equals("+63 ")) {
            contactNumberSignUp.setError("Contact number is required");
            isValid = false;
        } else if (!contactNumber.startsWith("+63")) {
            contactNumberSignUp.setError("Contact number must start with +63");
            isValid = false;
        } else if (contactNumber.length() < 7) { // +63 + at least 1 digit
            contactNumberSignUp.setError("Please enter a valid contact number");
            isValid = false;
        }

        // Validate Password - Get the actual text without trimming first
        String password = passwordSignUp.getText() != null ? passwordSignUp.getText().toString() : "";
        String confirmPassword = confirmPassSignUp.getText() != null ? confirmPassSignUp.getText().toString() : "";

        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Password is required");
            passwordField.setErrorEnabled(true);
            isValid = false;
        } else if (password.length() < 6) {
            passwordField.setError("Password must be at least 6 characters");
            passwordField.setErrorEnabled(true);
            isValid = false;
        }

        // Validate Confirm Password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPassField.setError("Please confirm your password");
            confirmPassField.setErrorEnabled(true);
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPassField.setError("Passwords do not match");
            confirmPassField.setErrorEnabled(true);
            isValid = false;
        }

        // Validate Terms and Conditions
        if (!checkBox.isChecked()) {
            Toast.makeText(this, "Please agree to the Terms and Conditions", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void registerUser() {
        String email = emailSignUp.getText().toString().trim();
        String password = passwordSignUp.getText().toString().trim();

        // Show loading state
        btnSignUp.setEnabled(false);
        btnSignUp.setText("Creating Account...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration successful
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user.getUid());
                        } else {
                            // Fallback if user is unexpectedly null
                            btnSignUp.setEnabled(true);
                            btnSignUp.setText("Sign Up");
                            Toast.makeText(TeacherSignUpActivity.this, "Registration error: user not available", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // Registration failed
                        btnSignUp.setEnabled(true);
                        btnSignUp.setText("Sign Up");

                        String errorMessage = "Registration failed";
                        if (task.getException() != null) {
                            Throwable ex = task.getException();
                            String msg = ex.getMessage();
                            if (msg != null && msg.toLowerCase().contains("email") && msg.toLowerCase().contains("already")) {
                                errorMessage = "Email already in use";
                            } else if (msg != null && msg.toLowerCase().contains("password")) {
                                errorMessage = "Weak password. Use at least 6 characters.";
                            } else {
                                errorMessage = msg;
                            }
                        }
                        Toast.makeText(TeacherSignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Defensive: ensure button state is restored on low-level failures
                    btnSignUp.setEnabled(true);
                    btnSignUp.setText("Sign Up");
                    Toast.makeText(TeacherSignUpActivity.this, "Registration error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveUserToDatabase(String userId) {
        // Create user data map
        Map<String, Object> userData = new HashMap<>();
        userData.put("idNumber", txtIDNo.getText().toString().trim());
        userData.put("firstName", firstNameSignup.getText().toString().trim());
        userData.put("middleName", middleNameSignUp.getText().toString().trim());
        userData.put("lastName", lastNameSignUp.getText().toString().trim());
        userData.put("email", emailSignUp.getText().toString().trim());
        userData.put("gender", genderSignUp.getText().toString().trim());
        userData.put("birthdate", bdaySignUp.getText().toString().trim());
        userData.put("program", programSignUp.getText().toString().trim());
        userData.put("contactNumber", contactNumberSignUp.getText().toString().trim());
        // Store password as requested (note: storing plain text passwords is insecure)
        userData.put("password", passwordSignUp.getText().toString().trim());
        userData.put("userType", "teacher");
        userData.put("createdAt", System.currentTimeMillis());

        // Save to Firebase Realtime Database
        mDatabase.child("users").child("teachers").child(userId).setValue(userData)
                .addOnCompleteListener(task -> {
                    btnSignUp.setEnabled(true);
                    btnSignUp.setText("Sign Up");

                    if (task.isSuccessful()) {
                        Toast.makeText(TeacherSignUpActivity.this,
                                "Account created successfully!", Toast.LENGTH_SHORT).show();

                        // Navigate to login or main activity
                        Intent intent = new Intent(TeacherSignUpActivity.this, TeacherLoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // More detailed error logging
                        String errorMessage = "Failed to save user data";
                        if (task.getException() != null) {
                            errorMessage += ": " + task.getException().getMessage();
                            // Log the full error for debugging
                            android.util.Log.e("SignUpError", "Database save failed", task.getException());
                        }
                        Toast.makeText(TeacherSignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnSignUp.setEnabled(true);
                    btnSignUp.setText("Sign Up");
                    android.util.Log.e("SignUpError", "Database save failed", e);
                    Toast.makeText(TeacherSignUpActivity.this,
                            "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setupClickableTermsText() {
        TextView termsText = findViewById(R.id.termsText);
        String fullText = "I agree to the Terms and Conditions and Privacy Policy.";
        String highlightText = "Terms and Conditions and Privacy Policy.";

        SpannableString spannableString = new SpannableString(fullText);

        int startIndex = fullText.indexOf(highlightText);
        int endIndex = startIndex + highlightText.length();

        // Set custom color
        spannableString.setSpan(
                new ForegroundColorSpan(ContextCompat.getColor(this, R.color.green)),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // Make it clickable
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // Open Terms and Conditions activity
                Intent intent = new Intent(TeacherSignUpActivity.this, TermsConditionsActivity.class);
                startActivityForResult(intent, 1001);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true); // Add underline
            }
        };

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        termsText.setText(spannableString);
        termsText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // User agreed to terms and conditions
            if (data != null && data.getBooleanExtra("terms_accepted", false)) {
                // Automatically check the checkbox
                checkBox.setChecked(true);
                Toast.makeText(this, "Terms and Conditions accepted", Toast.LENGTH_SHORT).show();
            }
        }
    }
}