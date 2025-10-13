package com.llavore.hereoattendance.student;

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

public class StudentSignup extends AppCompatActivity {
    private ImageView backButton;
    private TextView backtoSignIn;

    private TextInputEditText edpNumberSignUp;
    private TextInputEditText studentFirstNameSignUp;
    private TextInputEditText studentMiddleNameSignUp;
    private TextInputEditText studentLastNameSignUp;
    private TextInputEditText studentEmailSignUp;
    private AutoCompleteTextView studentGenderSignUp;
    private TextInputEditText studentBdaySignUp;
    private TextInputEditText studentProgramSignUp;
    private TextInputEditText studentYearLevelSignUp;
    private TextInputEditText studentContactNumberSignUp;
    private TextInputEditText studentGuardianNameSignUp;
    private TextInputEditText studentGuardianContactSignUp;
    private TextInputEditText studentPasswordSignUp;
    private TextInputEditText studentConfirmPassSignUp;
    private CheckBox studentTermsCheckBox;
    private MaterialButton btnStudentSignUp;
    private TextInputLayout studentBdayField;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private String[] genderOptions = {"Male", "Female", "Prefer not to say"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_sign_up);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        bindViews();
        setupInsets();
        setupClickListeners();
        setupGenderDropdown();
        setupDatePicker();
        setupClickableTermsText();
    }

    private void bindViews() {
        backButton = findViewById(R.id.backArrow);
        backtoSignIn = findViewById(R.id.studentSignIn);

        edpNumberSignUp = findViewById(R.id.edpNumberSignUp);
        studentFirstNameSignUp = findViewById(R.id.studentFirstNameSignUp);
        studentMiddleNameSignUp = findViewById(R.id.studentMiddleNameSignUp);
        studentLastNameSignUp = findViewById(R.id.studentLastNameSignUp);
        studentEmailSignUp = findViewById(R.id.studentEmailSignUp);
        studentGenderSignUp = findViewById(R.id.studentGenderSignUp);
        studentBdaySignUp = findViewById(R.id.studentBdaySignUp);
        studentProgramSignUp = findViewById(R.id.studentProgramSignUp);
        studentYearLevelSignUp = findViewById(R.id.studentYearLevelSignUp);
        studentContactNumberSignUp = findViewById(R.id.studentContactNumberSignUp);
        studentGuardianNameSignUp = findViewById(R.id.studentGuardianNameSignUp);
        studentGuardianContactSignUp = findViewById(R.id.studentGuardianContactSignUp);
        studentPasswordSignUp = findViewById(R.id.studentPasswordSignUp);
        studentConfirmPassSignUp = findViewById(R.id.studentConfirmPassSignUp);
        studentTermsCheckBox = findViewById(R.id.studentTermsCheckBox);
        btnStudentSignUp = findViewById(R.id.btnStudentSignUp);
        studentBdayField = findViewById(R.id.studentBdayField);
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupClickListeners() {
        backtoSignIn.setOnClickListener(v -> startActivity(new Intent(StudentSignup.this, StudentLogin.class)));
        backButton.setOnClickListener(v -> startActivity(new Intent(StudentSignup.this, StudentLogin.class)));

        btnStudentSignUp.setOnClickListener(v -> {
            if (validateForm()) {
                registerUser();
            }
        });
    }

    private void setupGenderDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genderOptions);
        studentGenderSignUp.setAdapter(adapter);
        studentGenderSignUp.setKeyListener(null);
        studentGenderSignUp.setOnClickListener(v -> studentGenderSignUp.showDropDown());
    }

    private void setupDatePicker() {
        View.OnClickListener showPicker = v -> showDatePicker();
        studentBdayField.setEndIconOnClickListener(showPicker);
        studentBdaySignUp.setOnClickListener(showPicker);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, y, m, d) -> {
            String formatted = String.format("%04d-%02d-%02d", y, m + 1, d);
            studentBdaySignUp.setText(formatted);
        }, year, month, day);
        datePickerDialog.show();
    }

    private boolean validateForm() {
        String edp = textOf(edpNumberSignUp);
        String first = textOf(studentFirstNameSignUp);
        String last = textOf(studentLastNameSignUp);
        String email = textOf(studentEmailSignUp);
        String gender = textOf(studentGenderSignUp);
        String bday = textOf(studentBdaySignUp);
        String program = textOf(studentProgramSignUp);
        String yearLevel = textOf(studentYearLevelSignUp);
        String contact = textOf(studentContactNumberSignUp);
        String guardian = textOf(studentGuardianNameSignUp);
        String guardianContact = textOf(studentGuardianContactSignUp);
        String password = textOf(studentPasswordSignUp);
        String confirm = textOf(studentConfirmPassSignUp);

        if (TextUtils.isEmpty(edp) || TextUtils.isEmpty(first) || TextUtils.isEmpty(last) ||
                TextUtils.isEmpty(email) || TextUtils.isEmpty(gender) || TextUtils.isEmpty(bday) ||
                TextUtils.isEmpty(program) || TextUtils.isEmpty(yearLevel) || TextUtils.isEmpty(contact) ||
                TextUtils.isEmpty(guardian) || TextUtils.isEmpty(guardianContact) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!studentTermsCheckBox.isChecked()) {
            Toast.makeText(this, "Please agree to the terms and conditions", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void registerUser() {
        String email = textOf(studentEmailSignUp);
        String password = textOf(studentPasswordSignUp);

        btnStudentSignUp.setEnabled(false);
        btnStudentSignUp.setText("Creating Account...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user.getUid());
                        } else {
                            btnStudentSignUp.setEnabled(true);
                            btnStudentSignUp.setText("Sign Up");
                            Toast.makeText(StudentSignup.this, "Registration error: user not available", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        btnStudentSignUp.setEnabled(true);
                        btnStudentSignUp.setText("Sign Up");
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(StudentSignup.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnStudentSignUp.setEnabled(true);
                    btnStudentSignUp.setText("Sign Up");
                    Toast.makeText(StudentSignup.this, "Registration error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveUserToDatabase(String userId) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("edpNumber", textOf(edpNumberSignUp));
        userData.put("firstName", textOf(studentFirstNameSignUp));
        userData.put("middleName", textOf(studentMiddleNameSignUp));
        userData.put("lastName", textOf(studentLastNameSignUp));
        userData.put("email", textOf(studentEmailSignUp));
        userData.put("gender", textOf(studentGenderSignUp));
        userData.put("birthdate", textOf(studentBdaySignUp));
        userData.put("program", textOf(studentProgramSignUp));
        userData.put("yearLevel", textOf(studentYearLevelSignUp));
        userData.put("contactNumber", textOf(studentContactNumberSignUp));
        userData.put("guardianName", textOf(studentGuardianNameSignUp));
        userData.put("guardianContactNumber", textOf(studentGuardianContactSignUp));
        userData.put("password", textOf(studentPasswordSignUp));
        userData.put("userType", "student");
        userData.put("createdAt", System.currentTimeMillis());

        mDatabase.child("users").child("students").child(userId).setValue(userData)
                .addOnCompleteListener(task -> {
                    btnStudentSignUp.setEnabled(true);
                    btnStudentSignUp.setText("Sign Up");

                    if (task.isSuccessful()) {
                        Toast.makeText(StudentSignup.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(StudentSignup.this, StudentLogin.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMessage = "Failed to save user data";
                        if (task.getException() != null) {
                            errorMessage += ": " + task.getException().getMessage();
                        }
                        Toast.makeText(StudentSignup.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnStudentSignUp.setEnabled(true);
                    btnStudentSignUp.setText("Sign Up");
                    Toast.makeText(StudentSignup.this, "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String textOf(AutoCompleteTextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    private void setupClickableTermsText() {
        TextView termsText = findViewById(R.id.studentTermsText);
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
                Intent intent = new Intent(StudentSignup.this, TermsConditionsActivity.class);
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
                studentTermsCheckBox.setChecked(true);
                Toast.makeText(this, "Terms and Conditions accepted", Toast.LENGTH_SHORT).show();
            }
        }
    }
}