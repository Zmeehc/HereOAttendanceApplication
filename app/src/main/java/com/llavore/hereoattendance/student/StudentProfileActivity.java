package com.llavore.hereoattendance.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.model.User;
import com.llavore.hereoattendance.teacher.MainActivity;
import com.llavore.hereoattendance.utils.SessionManager;
import com.llavore.hereoattendance.utils.NavigationHeaderManager;
import com.llavore.hereoattendance.utils.StudentNavigationManager;

public class StudentProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private DatabaseReference mDatabase;
    private DrawerLayout drawerLayout;
    private NavigationHeaderManager headerManager;
    private StudentNavigationManager navigationManager;

    private TextView edpValue, emailValue, fullNameValue, genderValue, birthdateValue,
            contactValue, programYearValue, guardianNameValue, guardianContactValue;
    private MaterialButton qrCodeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);
        headerManager = new NavigationHeaderManager(sessionManager);
        navigationManager = new StudentNavigationManager(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        drawerLayout = findViewById(R.id.main);
        ImageView burgerIcon = findViewById(R.id.burgerIcon);
        NavigationView navigationView = findViewById(R.id.navigationView);
        
        // Setup navigation using the common manager
        navigationManager.setupNavigationDrawer(drawerLayout, burgerIcon, navigationView, "account");

        // Setup profile picture click listener to open edit profile
        ImageView profilePicture = findViewById(R.id.profilePicture);
        androidx.cardview.widget.CardView profileCard = findViewById(R.id.profilePictureCard);
        
        View.OnClickListener editProfileClickListener = v -> {
            try {
                Intent intent = new Intent(StudentProfileActivity.this, StudentEditProfileActivity.class);
                startActivityForResult(intent, 100); // Use startActivityForResult to refresh data when returning
            } catch (Exception e) {
                Toast.makeText(this, "Error opening edit profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        };
        profilePicture.setOnClickListener(editProfileClickListener);
        profileCard.setOnClickListener(editProfileClickListener);

        bindViews();
        loadData();
        
        // Navigation header is now handled by the navigation manager
    }
    

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data when returning to profile
        loadData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // Profile was updated, refresh the data
            loadData();
        }
    }

    private void bindViews() {
        edpValue = findViewById(R.id.edpValue);
        emailValue = findViewById(R.id.emailValue);
        fullNameValue = findViewById(R.id.fullNameValue);
        genderValue = findViewById(R.id.genderValue);
        birthdateValue = findViewById(R.id.birthdateValue);
        contactValue = findViewById(R.id.contactValue);
        programYearValue = findViewById(R.id.programYearValue);
        guardianNameValue = findViewById(R.id.guardianNameValue);
        guardianContactValue = findViewById(R.id.guardianContactValue);
        qrCodeButton = findViewById(R.id.qrCodeButton);
        
        // Setup QR Code button click listener
        qrCodeButton.setOnClickListener(v -> {
            Intent intent = new Intent(StudentProfileActivity.this, StudentQRCodeActivity.class);
            startActivity(intent);
        });
    }

    private void loadData() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "User ID not found. Please login again.", Toast.LENGTH_LONG).show();
            return;
        }

        // Loading student data...

        mDatabase.child("users").child("students").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(StudentProfileActivity.this, "Student data not found in database", Toast.LENGTH_LONG).show();
                            return;
                        }
                        
                        User user = snapshot.getValue(User.class);
                        if (user == null) {
                            Toast.makeText(StudentProfileActivity.this, "Error parsing user data", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Load and display user data
                        setTextSafe(emailValue, user.getEmail());
                        setTextSafe(fullNameValue, user.getFullName());
                        setTextSafe(genderValue, user.getGender());
                        setTextSafe(birthdateValue, user.getBirthdate());
                        
                        // Format contact number for display
                        String contactNumber = user.getContactNumber();
                        if (contactNumber != null && !contactNumber.isEmpty()) {
                            contactValue.setText(formatContactNumber(contactNumber));
                        } else {
                            contactValue.setText("Not provided");
                        }
                        
                        setTextSafe(programYearValue, combineProgramYear(user.getProgram(), snapshot.child("yearLevel").getValue(String.class)));
                        setTextSafe(guardianNameValue, snapshot.child("guardianName").getValue(String.class));
                        
                        // Format guardian contact for display
                        String guardianContact = snapshot.child("guardianContactNumber").getValue(String.class);
                        if (guardianContact != null && !guardianContact.isEmpty()) {
                            guardianContactValue.setText(formatContactNumber(guardianContact));
                        } else {
                            guardianContactValue.setText("Not provided");
                        }
                        
                        setTextSafe(edpValue, snapshot.child("edpNumber").getValue(String.class));

                        // Load profile image
                        String imageUrl = user.getProfileImageUrl();
                        ImageView profilePicture = findViewById(R.id.profilePicture);
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(StudentProfileActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.baseline_person_24)
                                    .error(R.drawable.baseline_person_24)
                                    .circleCrop()
                                    .override(200, 200)
                                    .into(profilePicture);
                        } else {
                            profilePicture.setImageResource(R.drawable.baseline_person_24);
                        }
                        
                        // Student data loaded successfully
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(StudentProfileActivity.this, "Error loading data: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setTextSafe(TextView view, String value) {
        if (view == null) return;
        view.setText(value != null && !value.isEmpty() ? value : "Not provided");
    }

    private String combineProgramYear(String program, String year) {
        if (program == null) program = "";
        if (year == null) year = "";
        if (program.isEmpty() && year.isEmpty()) return "Not provided";
        if (program.isEmpty()) return year;
        if (year.isEmpty()) return program;
        return program + " • Year " + year;
    }

    private String formatContactNumber(String contactNumber) {
        if (contactNumber == null || contactNumber.isEmpty()) {
            return "Not provided";
        }

        android.util.Log.d("ContactFormat", "Original contact number: '" + contactNumber + "'");

        // Remove all non-digit characters except +
        String cleanNumber = contactNumber.replaceAll("[^+\\d]", "");
        android.util.Log.d("ContactFormat", "After removing non-digits: '" + cleanNumber + "'");

        // Handle multiple +63 prefixes (like "+63+63" or "+63 +63")
        while (cleanNumber.startsWith("+63+63") || cleanNumber.startsWith("+63+")) {
            if (cleanNumber.startsWith("+63+63")) {
                cleanNumber = cleanNumber.substring(6); // Remove "+63+63"
            } else if (cleanNumber.startsWith("+63+")) {
                cleanNumber = cleanNumber.substring(4); // Remove "+63+"
            }
        }

        // Remove single +63 prefix if present
        if (cleanNumber.startsWith("+63")) {
            cleanNumber = cleanNumber.substring(3);
        } else if (cleanNumber.startsWith("63")) {
            cleanNumber = cleanNumber.substring(2);
        }

        // Remove leading zeros if any
        cleanNumber = cleanNumber.replaceFirst("^0+", "");

        android.util.Log.d("ContactFormat", "After cleaning: '" + cleanNumber + "'");

        // Format as +63 000 000 0000 (3-3-4 format)
        if (cleanNumber.length() == 10) {
            String formatted = String.format("+63 %s %s %s",
                    cleanNumber.substring(0, 3),
                    cleanNumber.substring(3, 6),
                    cleanNumber.substring(6, 10));
            android.util.Log.d("ContactFormat", "Final formatted: '" + formatted + "'");
            return formatted;
        } else if (cleanNumber.length() == 9) {
            // Handle case where leading 9 might be missing (old format)
            String formatted = String.format("+63 9%s %s %s",
                    cleanNumber.substring(0, 2),
                    cleanNumber.substring(2, 5),
                    cleanNumber.substring(5, 9));
            android.util.Log.d("ContactFormat", "Final formatted (9 digits): '" + formatted + "'");
            return formatted;
        } else if (cleanNumber.length() >= 7) {
            // For other lengths, try to format as best as possible
            if (cleanNumber.length() >= 10) {
                // Take only first 10 digits
                cleanNumber = cleanNumber.substring(0, 10);
                String formatted = String.format("+63 %s %s %s",
                        cleanNumber.substring(0, 3),
                        cleanNumber.substring(3, 6),
                        cleanNumber.substring(6, 10));
                android.util.Log.d("ContactFormat", "Final formatted (>=10): '" + formatted + "'");
                return formatted;
            } else {
                // Pad with zeros if too short
                while (cleanNumber.length() < 10) {
                    cleanNumber = cleanNumber + "0";
                }
                String formatted = String.format("+63 %s %s %s",
                        cleanNumber.substring(0, 3),
                        cleanNumber.substring(3, 6),
                        cleanNumber.substring(6, 10));
                android.util.Log.d("ContactFormat", "Final formatted (padded): '" + formatted + "'");
                return formatted;
            }
        } else {
            // If number is too short, return as is with +63 prefix
            String formatted = "+63 " + cleanNumber;
            android.util.Log.d("ContactFormat", "Final formatted (short): '" + formatted + "'");
            return formatted;
        }
    }
}


