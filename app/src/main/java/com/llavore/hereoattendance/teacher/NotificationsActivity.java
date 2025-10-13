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
import com.llavore.hereoattendance.utils.TeacherNavigationManager;

public class NotificationsActivity extends AppCompatActivity {

    private ImageView burgerIcon;
    private DrawerLayout drawerLayout;
    private SessionManager sessionManager;
    private TeacherNavigationManager navigationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        
        // Initialize views and setup
        initializeViews();
        
        // Setup navigation using the common manager
        navigationManager = new TeacherNavigationManager(this);
        navigationManager.setupNavigationDrawer(drawerLayout, burgerIcon, findViewById(R.id.navigationView), "notifications");
    }

    private void initializeViews() {
        burgerIcon = findViewById(R.id.burgerIcon);
        drawerLayout = findViewById(R.id.main);
        
        // Initialize session manager
        sessionManager = new SessionManager(this);
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
