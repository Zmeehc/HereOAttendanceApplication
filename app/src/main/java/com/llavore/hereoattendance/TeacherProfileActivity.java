package com.llavore.hereoattendance;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.content.ContextCompat;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.model.User;
import com.llavore.hereoattendance.utils.SessionManager;
import com.bumptech.glide.Glide;

public class TeacherProfileActivity extends AppCompatActivity {

    private ImageView burgerIcon;
    private SessionManager sessionManager;
    private DatabaseReference mDatabase;
    
    // UI elements for user data
    private TextView emailValue, fullNameValue, genderValue, birthdateValue, contactValue, passwordValue;
    
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
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Initialize UI elements
        initializeViews();
        
        // Setup profile picture click listener
        setupProfilePictureClick();
        
        // Load user data
        loadUserData();

        DrawerLayout drawerLayout = findViewById(R.id.main);
        burgerIcon = findViewById(R.id.burgerIcon);
        burgerIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setCheckedItem(R.id.nav_account);
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        sessionManager.logout();
                        Intent intent = new Intent(TeacherProfileActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_dashboard) {
                // Navigate to dashboard
                Intent intent = new Intent(TeacherProfileActivity.this, TeacherDashboard.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_account) {
                // Already on profile page, just close drawer
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_notifications) {
                // Handle notifications navigation
                // You can add navigation to notifications activity here
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_settings) {
                // Handle settings navigation
                // You can add navigation to settings activity here
                drawerLayout.closeDrawers();
                return true;
            }
            // Handle other navigation items if needed
            return false;
        });

        // getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.green));
    }
    
    private void initializeViews() {
        emailValue = findViewById(R.id.emailValue);
        fullNameValue = findViewById(R.id.fullNameValue);
        genderValue = findViewById(R.id.genderValue);
        birthdateValue = findViewById(R.id.birthdateValue);
        contactValue = findViewById(R.id.contactValue);
        passwordValue = findViewById(R.id.passwordValue);
    }
    
    private void setupProfilePictureClick() {
        ImageView profilePicture = findViewById(R.id.profilePicture);
        profilePicture.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherProfileActivity.this, TeacherEditProfileActivity.class);
            startActivityForResult(intent, 1001); // Request code for edit profile
        });
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
                    }
                });
    }
    
    private void displayUserData(User user) {
        // Display user data in UI
        emailValue.setText(user.getEmail() != null ? user.getEmail() : "Not provided");
        fullNameValue.setText(user.getFullName() != null ? user.getFullName() : "Not provided");
        genderValue.setText(user.getGender() != null ? user.getGender() : "Not provided");
        birthdateValue.setText(user.getBirthdate() != null ? user.getBirthdate() : "Not provided");
        contactValue.setText(user.getIdNumber() != null ? user.getIdNumber() : "Not provided");
        passwordValue.setText("******78"); // Always show masked password
        
        // Load profile image if available
        loadProfileImage(user.getProfileImageUrl());
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Profile was updated, refresh the data
            loadUserData();
        }
    }
}