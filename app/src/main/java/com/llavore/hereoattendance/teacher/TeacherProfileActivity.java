// Fixed TeacherEditProfileActivity.java back arrow issue
// In your TeacherEditProfileActivity.java, make sure the back arrow ID matches the layout

package com.llavore.hereoattendance.teacher;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.model.User;
import com.llavore.hereoattendance.utils.SessionManager;
import com.llavore.hereoattendance.utils.NavigationHeaderManager;
import com.llavore.hereoattendance.utils.TeacherNavigationManager;
import com.bumptech.glide.Glide;

public class TeacherProfileActivity extends AppCompatActivity {

    private ImageView burgerIcon;
    private SessionManager sessionManager;
    private DatabaseReference mDatabase;
    private DrawerLayout drawerLayout;
    private NavigationHeaderManager headerManager;
    private TeacherNavigationManager navigationManager;

    // UI elements for user data
    private TextView emailValue, fullNameValue, genderValue, birthdateValue, idNumberValue, contactValue;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase and session manager
        sessionManager = new SessionManager(this);
        headerManager = new NavigationHeaderManager(sessionManager);
        navigationManager = new TeacherNavigationManager(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI elements
        initializeViews();

        // Setup navigation drawer using the common manager
        DrawerLayout drawerLayout = findViewById(R.id.main);
        ImageView burgerIcon = findViewById(R.id.burgerIcon);
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationManager.setupNavigationDrawer(drawerLayout, burgerIcon, navigationView, "account");

        // Setup profile picture click listener
        setupProfilePictureClick();

        // Load user data
        loadUserData();
    }
    

    private void initializeViews() {
        emailValue = findViewById(R.id.emailValue);
        fullNameValue = findViewById(R.id.fullNameValue);
        genderValue = findViewById(R.id.genderValue);
        birthdateValue = findViewById(R.id.birthdateValue);
        idNumberValue = findViewById(R.id.idNumberValue);
        contactValue = findViewById(R.id.contactValue);
        // password removed from UI

        // Initialize drawer layout and burger icon
        drawerLayout = findViewById(R.id.main);
        burgerIcon = findViewById(R.id.burgerIcon);
    }


    private void setupProfilePictureClick() {
        ImageView profilePicture = findViewById(R.id.profilePicture);
        androidx.cardview.widget.CardView profileCard = findViewById(R.id.profilePictureCard);

        // Create the click listener for edit profile
        View.OnClickListener editProfileClickListener = v -> {
            // Ensure drawer is closed first
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }


            try {
                Intent intent = new Intent(TeacherProfileActivity.this, TeacherEditProfileActivity.class);
                startActivityForResult(intent, 100); // Use startActivityForResult to refresh data when returning
            } catch (Exception e) {
                Toast.makeText(this, "Error opening edit profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        };

        // Set click listeners
        if (profilePicture != null) {
            profilePicture.setOnClickListener(editProfileClickListener);
            profilePicture.setClickable(true);
            profilePicture.setFocusable(true);
        }

        if (profileCard != null) {
            profileCard.setOnClickListener(editProfileClickListener);
            profileCard.setClickable(true);
            profileCard.setFocusable(true);
        }
    }

    private void loadUserData() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            // Handle case where user ID is not available
            return;
        }

        mDatabase.child("users").child("teachers").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                displayUserData(user);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Handle error
                        Toast.makeText(TeacherProfileActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayUserData(User user) {
        // Display user data in UI
        emailValue.setText(user.getEmail() != null ? user.getEmail() : "Not provided");
        fullNameValue.setText(user.getFullName() != null ? user.getFullName() : "Not provided");
        genderValue.setText(user.getGender() != null ? user.getGender() : "Not provided");
        birthdateValue.setText(user.getBirthdate() != null ? user.getBirthdate() : "Not provided");
        idNumberValue.setText(user.getIdNumber() != null ? user.getIdNumber() : "Not provided");

        // Format contact number for display in 3-3-4 format
        String contactNumber = user.getContactNumber();
        if (contactNumber != null && !contactNumber.isEmpty()) {
            String formattedContact = formatContactNumber(contactNumber);
            contactValue.setText(formattedContact);
        } else {
            contactValue.setText("Not provided");
        }

        // password no longer displayed

        // Load profile image if available
        loadProfileImage(user.getProfileImageUrl());
    }

    private String formatContactNumber(String contactNumber) {
        if (contactNumber == null || contactNumber.isEmpty()) {
            return "Not provided";
        }

        // Remove all non-digit characters except +
        String cleanNumber = contactNumber.replaceAll("[^+\\d]", "");

        // Remove +63 prefix if present
        if (cleanNumber.startsWith("+63")) {
            cleanNumber = cleanNumber.substring(3);
        } else if (cleanNumber.startsWith("63")) {
            cleanNumber = cleanNumber.substring(2);
        }

        // Remove leading zeros if any
        cleanNumber = cleanNumber.replaceFirst("^0+", "");

        // Ensure we have exactly 10 digits (Philippine mobile numbers)
        if (cleanNumber.length() == 10) {
            // Format as +63 000 000 0000 (3-3-4 format)
            return String.format("+63 %s %s %s",
                    cleanNumber.substring(0, 3),
                    cleanNumber.substring(3, 6),
                    cleanNumber.substring(6, 10));
        } else if (cleanNumber.length() == 9) {
            // Handle case where leading 9 might be missing (old format)
            return String.format("+63 9%s %s %s",
                    cleanNumber.substring(0, 2),
                    cleanNumber.substring(2, 5),
                    cleanNumber.substring(5, 9));
        } else if (cleanNumber.length() >= 7) {
            // For other lengths, try to format as best as possible
            if (cleanNumber.length() >= 10) {
                // Take only first 10 digits
                cleanNumber = cleanNumber.substring(0, 10);
                return String.format("+63 %s %s %s",
                        cleanNumber.substring(0, 3),
                        cleanNumber.substring(3, 6),
                        cleanNumber.substring(6, 10));
            } else {
                // Pad with zeros if too short
                while (cleanNumber.length() < 10) {
                    cleanNumber = cleanNumber + "0";
                }
                return String.format("+63 %s %s %s",
                        cleanNumber.substring(0, 3),
                        cleanNumber.substring(3, 6),
                        cleanNumber.substring(6, 10));
            }
        } else {
            // If number is too short, return as is with +63 prefix
            return "+63 " + cleanNumber;
        }
    }

    private void loadProfileImage(String imageUrl) {
        ImageView profilePicture = findViewById(R.id.profilePicture);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .circleCrop()
                    .override(200, 200) // Ensure consistent size
                    .into(profilePicture);
        } else {
            profilePicture.setImageResource(R.drawable.baseline_person_24);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data when returning to profile
        loadUserData();
        
        // Update navigation drawer to highlight account
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationManager.setCurrentActivity(navigationView, "account");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Profile was updated, refresh the data
            loadUserData();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}