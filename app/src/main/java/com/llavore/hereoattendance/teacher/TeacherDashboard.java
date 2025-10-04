package com.llavore.hereoattendance.teacher;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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
import com.bumptech.glide.Glide;

public class TeacherDashboard extends AppCompatActivity {

    private ImageView burgerIcon;
    private SessionManager sessionManager;
    private DatabaseReference mDatabase;
    private NavigationHeaderManager headerManager;

    private CardView coursesBtn;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        coursesBtn = findViewById(R.id.courseCardView);
        coursesBtn.setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboard.this, ActiveCoursesActivity.class);
            startActivity(intent);
            finish();
        });

        sessionManager = new SessionManager(this);
        headerManager = new NavigationHeaderManager(sessionManager);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        DrawerLayout drawerLayout = findViewById(R.id.main);
        burgerIcon = findViewById(R.id.burgerIcon);
        burgerIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setCheckedItem(R.id.nav_dashboard);
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                new AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            sessionManager.logout();
                            Intent intent = new Intent(TeacherDashboard.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_dashboard) {
                // Reload or redirect to the dashboard page
                Intent intent = new Intent(TeacherDashboard.this, TeacherDashboard.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_account) {
                // Navigate to account page
                Intent intent = new Intent(TeacherDashboard.this, TeacherProfileActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_notifications) {
                // Navigate to notifications
                Intent intent = new Intent(TeacherDashboard.this, NotificationsActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_settings) {
                // Navigate to settings
                Intent intent = new Intent(TeacherDashboard.this, SettingsActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawers();
                return true;
            }
            // Handle other navigation items if needed
            return false;
        });

        // Load user data into navigation header
        loadNavigationHeader(navigationView);

        // Setup dashboard card click listeners
        setupDashboardCards();
        
        // Set current date and day
        setCurrentDateAndDay();
        
        // Start timer to update date at midnight
        startDateUpdateTimer();
        
        // Load user profile picture and data
        loadUserProfilePicture();
        loadUserData();
        loadCourseCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile picture when returning to dashboard
        loadUserProfilePicture();
        
        // Refresh current date and day when returning to dashboard
        setCurrentDateAndDay();
        
        // Refresh navigation header when returning to activity
        NavigationView navigationView = findViewById(R.id.navigationView);
        loadNavigationHeader(navigationView);
    }



    private void loadUserProfilePicture() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        mDatabase.child("users").child("teachers").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                ImageView profilePicture = findViewById(R.id.imageView3);
                                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                    Glide.with(TeacherDashboard.this)
                                            .load(user.getProfileImageUrl())
                                            .placeholder(R.drawable.baseline_person_24)
                                            .error(R.drawable.baseline_person_24)
                                            .circleCrop()
                                            .override(200, 200)
                                            .into(profilePicture);
                                } else {
                                    profilePicture.setImageResource(R.drawable.baseline_person_24);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle error silently
                    }
                });
    }

    private void loadUserData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        mDatabase.child("users").child("teachers").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                displayUserData(user);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle error silently
                    }
                });
    }

    private void displayUserData(User user) {
        TextView welcomeName = findViewById(R.id.welcomeName);
        TextView welcomeEmail = findViewById(R.id.welcomeEmail);

        // Display "Welcome!" in black and first & last name in green
        String firstLastName = getFirstLastName(user);
        if (firstLastName != null && !firstLastName.isEmpty()) {
            // Create spannable string to have different colors
            android.text.SpannableString spannableString = new android.text.SpannableString("Welcome! " + firstLastName);
            // Set black color for "Welcome! "
            spannableString.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.BLACK),
                    0, 9, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Set green color for the name
            spannableString.setSpan(new android.text.style.ForegroundColorSpan(
                            androidx.core.content.ContextCompat.getColor(this, R.color.green)),
                    9, spannableString.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            welcomeName.setText(spannableString);
        } else {
            welcomeName.setText("Welcome! User");
        }

        // Display email
        String email = user.getEmail();
        if (email != null && !email.isEmpty()) {
            welcomeEmail.setText(email);
        } else {
            welcomeEmail.setText("No email available");
        }
    }

    // Helper method to get only first and last name using User model getters
    private String getFirstLastName(User user) {
        StringBuilder name = new StringBuilder();
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            name.append(user.getFirstName());
        }
        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(user.getLastName());
        }
        return name.toString();
    }

    private void setCurrentDateAndDay() {
        TextView todaySessionsText = findViewById(R.id.textView13);
        if (todaySessionsText != null) {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMMM, dd yyyy", java.util.Locale.getDefault());
            java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault());
            
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            String currentDate = dateFormat.format(calendar.getTime());
            String currentDay = dayFormat.format(calendar.getTime());
            
            String formattedText = "Today's Sessions - " + currentDate + " " + currentDay;
            todaySessionsText.setText(formattedText);
        }
    }

    private void startDateUpdateTimer() {
        java.util.Timer timer = new java.util.Timer();
        java.util.TimerTask task = new java.util.TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> setCurrentDateAndDay());
            }
        };
        
        // Calculate time until next midnight
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        
        long delay = calendar.getTimeInMillis() - System.currentTimeMillis();
        
        // Schedule the task to run at midnight and then every 24 hours
        timer.scheduleAtFixedRate(task, delay, 24 * 60 * 60 * 1000);
    }

    private void setupDashboardCards() {
        // Reports card
        findViewById(R.id.reportCardView).setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboard.this, ReportsActivity.class);
            startActivity(intent);
        });

        // Archives card
        findViewById(R.id.archivesCardView).setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboard.this, ArchivesActivity.class);
            startActivity(intent);
        });

        // SMS Alerts card
        findViewById(R.id.smsCardView).setOnClickListener(v -> {
            Intent intent = new Intent(TeacherDashboard.this, SmsAlertsActivity.class);
            startActivity(intent);
        });
    }

    private void loadCourseCount() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        DatabaseReference coursesRef = mDatabase.child("users").child("teachers").child(userId).child("courses");
        coursesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int courseCount = (int) dataSnapshot.getChildrenCount();
                TextView courseCountText = findViewById(R.id.courseCountText);
                if (courseCountText != null) {
                    courseCountText.setText(String.valueOf(courseCount));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error silently
            }
        });
    }
    
    private void loadNavigationHeader(NavigationView navigationView) {
        android.view.View headerView = navigationView.getHeaderView(0);
        ImageView profilePicture = headerView.findViewById(R.id.navProfilePicture);
        TextView userName = headerView.findViewById(R.id.navUserName);
        TextView userEmail = headerView.findViewById(R.id.navUserEmail);
        
        if (profilePicture != null && userName != null && userEmail != null) {
            headerManager.loadUserData(profilePicture, userName, userEmail);
        }
    }
    
    
}