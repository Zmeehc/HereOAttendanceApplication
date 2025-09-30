package com.llavore.hereoattendance.teacher;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.utils.SessionManager;

public class NotificationsActivity extends AppCompatActivity {

    private ImageView burgerIcon;
    private DrawerLayout drawerLayout;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        
        // Initialize views and setup
        initializeViews();
        setupNavigationDrawer();
    }

    private void initializeViews() {
        burgerIcon = findViewById(R.id.burgerIcon);
        drawerLayout = findViewById(R.id.main);
        
        // Initialize session manager
        sessionManager = new SessionManager(this);
    }

    private void setupNavigationDrawer() {
        // Setup burger icon click listener
        burgerIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setCheckedItem(R.id.nav_notifications);
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                showLogoutDialog();
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_dashboard) {
                // Navigate to dashboard
                Intent intent = new Intent(NotificationsActivity.this, TeacherDashboard.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_account) {
                // Navigate to profile
                Intent intent = new Intent(NotificationsActivity.this, TeacherProfileActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_settings) {
                // Navigate to settings
                Intent intent = new Intent(NotificationsActivity.this, SettingsActivity.class);
                startActivity(intent);
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_notifications) {
                // Already on notifications page, just close drawer
                drawerLayout.closeDrawers();
                return true;
            }
            return false;
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes", (dialog, which) -> {
                sessionManager.logout();
                Intent intent = new Intent(NotificationsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
            .show();
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
